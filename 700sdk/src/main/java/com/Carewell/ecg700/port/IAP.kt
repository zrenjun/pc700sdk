package com.Carewell.ecg700.port

import com.Carewell.ecg700.port.ParseData.getH4
import com.Carewell.ecg700.port.ParseData.getL4
import com.Carewell.ecg700.port.IAPFile.SeekOrigin
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.Exception
import java.util.*

/**
 * IAP 固件升级，适用于PC700,新PC600,900
 */
class IAP(serialIs: InputStream, serialOs: OutputStream) : Thread() {
    interface IAPState {
        companion object {
            const val enter = 0
            const val start = 1
            const val programing = 2
            const val checking = 3
            const val end = 4
            const val error = 0xE0
            const val idle = 0xFF //空闲状态
            const val getVer = 10
        }
    }

    /**
     * 发送命令的初始状态
     */
    private var iapState: Int

    interface IAPCmd {
        companion object {
            const val enter = 0xF001 //令牌F0+类型01
            const val start = 0xF002
            const val programing = 0xF101
            const val check = 0xF102
            const val end = 0xF003
            const val idle = 0xFF
        }
    }

    /**
     * MCU固件升级   MAIN:主MCU，SUB:从MCU
     */
    enum class MCUType {
        MAIN, SUB
    }

    private var mInputStream: InputStream = serialIs
    private var mOutputStream: OutputStream = serialOs
    private var mFileIs: InputStream? = null
    private var bStopIAP = false
    private var bStopRecv = false

    @Volatile
    private var mCmdType: CmdType
    private var mRecvThread: RecvThread
    private var mBufferVec = Vector<Byte>()
    private var mMcuType: MCUType? = null


    /**
     * 获取系统版本号
     *
     * @param mcuType MCU固件升级类型   MAIN:主MCU，SUB:子MCU
     */
    fun getIAPVer(mcuType: MCUType) {
        LogUtil.v("获取系统版本号")
        mMcuType = mcuType
        iapState = IAPState.getVer
    }

    /**
     * 设置升级文件
     *
     * @param fileIs  文件输入流
     * @param mcuType MCU固件升级类型   MAIN:主MCU，SUB:子MCU
     */
    fun setIAPFile(fileIs: InputStream, mcuType: MCUType?) {
        LogUtil.v("设置升级文件")
        mFileIs = fileIs
        mMcuType = mcuType
        iapState = IAPState.enter
        frameFlags = 0
        page = 0
    }

    /**
     * 设置IAP 空闲状态
     */
    fun iapIdle() {
        iapState = IAPState.idle
    }

    override fun run() {
        while (!bStopIAP) {
            synchronized(this) {
                send()
                if (mBufferVec.size > 0) {
                    analyse()
                }
            }
        }
    }

    var preTime: Long = 0

    /**
     * 发送指令
     */
    private fun send() {
        val curTime = System.currentTimeMillis()
        when (iapState) {
            IAPState.enter -> if (preTime < curTime) {
                preTime = curTime + 300
                mCmdType.code = IAPCmd.enter
                mCmdType.length = 3
                mCmdType.buf[0] = if (mMcuType == MCUType.MAIN) 0x01 else 0x11
                sendCmd(mCmdType)
                LogUtil.v("send----> enter")
            }

            IAPState.start -> if (preTime < curTime) {
                preTime = curTime + 200
                mCmdType.code = IAPCmd.start
                mCmdType.length = 3
                mCmdType.buf[0] = 0
                sendCmd(mCmdType)
                LogUtil.v("send----> start")
            }

            IAPState.programing -> if (preTime < curTime) {
                preTime = curTime + 25
                LogUtil.v("frameFlags----> $frameFlags")
                if (frameFlags != 0xFFFF) { // 512字节
                    for (fr in 0..15) { //16帧
                        if (frameFlags and (1 shl fr) == 0) { //帧数据 =0
                            frameFlags = frameFlags or (1 shl fr) //发包后，置帧数据 =1
                            mCmdType.length = 36
                            mCmdType.code = IAPCmd.programing
                            mCmdType.buf[0] = (page * 16 + fr shr 8).toByte().toInt() //H字节
                            mCmdType.buf[1] = (page * 16 + fr and 0xFF).toByte().toInt() //L字节
                            mIapFile?.seek((page * 16 + fr) * 32, SeekOrigin.Begin)
                            mIapFile?.read(mCmdType.buf, 2, 32)
                            sendCmd(mCmdType)
                            LogUtil.v("send----> programing   frameFlags----> $frameFlags")
                            break
                        }
                    }
                }
                //每发送完16帧（512字节固件数据）后，需发送iap_check检查并补发缺失帧
                if (frameFlags == 0xFFFF) {
                    iapState = IAPState.checking
                }
            }

            IAPState.checking -> if (preTime < curTime) {
                preTime = curTime + 100
                mCmdType.length = 5
                mCmdType.code = IAPCmd.check
                mCmdType.buf[0] = 0 // 保留
                mCmdType.buf[1] = (page shr 8).toByte().toInt() // H字节
                mCmdType.buf[2] = (page and 0xFF).toByte().toInt() // L字节
                sendCmd(mCmdType)
                LogUtil.v("send----> checking")
            }

            IAPState.end -> if (preTime < curTime) {
                preTime = curTime + 50
                mCmdType.length = 3
                mCmdType.code = IAPCmd.end
                mCmdType.buf[0] = 0x01 //模式
                sendCmd(mCmdType)
                LogUtil.v("send----> end")
            }

            IAPState.idle -> mCmdType.code = IAPCmd.idle

            IAPState.getVer -> if (preTime < curTime) {
                preTime = curTime + 200
                mCmdType.code = IAPCmd.enter
                mCmdType.length = 3
                mCmdType.buf[0] = if (mMcuType == MCUType.MAIN) 0x02 else 0x12
                sendCmd(mCmdType)
                LogUtil.v("send----> getVer")
            }
        }
    }

    /**
     * 解析命令
     */
    private var mVerifier: BaseProtocol
    private var mIapFile: IAPFile? = null
    private fun analyse() {
        val cnt = mVerifier.checkIntactCnt(mBufferVec)
        var token: Int
        var len: Int
        var type: Int
        for (i in 0 until cnt) {
            mBufferVec.removeAt(0) //包头
            mBufferVec.removeAt(0)
            token = mBufferVec.removeAt(0).toInt() and 0xff //令牌
            len = mBufferVec.removeAt(0).toInt() and 0xff //长度
            type = mBufferVec.removeAt(0).toInt() and 0xff //类型
            when (mCmdType.code) {
                IAPCmd.enter -> {
                    LogUtil.v("enter")
                    val state = mBufferVec.removeAt(0) //状态
                    if (len > 3 && type == 1 && token == 0xf0) { //设备信息
                        //高4位MCU序号,单个MCU,主MCU  aa, 55, f0, 07, 01, 02, 00, 00, 13, 17, 88,
                        //高4位第2个MCU序号,子MCU     aa, 55, f0, 07, 01, 12, 00, 00, 13, 28, 0b,
                        if (state.toInt() and 0xf0 == 0x00 || state.toInt() shr 4 and 0x01 == 0x01) {
                            analyseVersion(state)
                        }
                    }
                }

                IAPCmd.start -> {
                    LogUtil.v("start")
                    mBufferVec.removeAt(0) //状态
                    iapState = IAPState.programing
                    //加载文件
                    mIapFile?.close()
                    mFileIs?.let {
                        mIapFile = IAPFile(it)
                    }
                }

                IAPCmd.check -> {
                    LogUtil.v("check")
                    if (len != 9) {
                        return
                    }
                    val hPage: Int = mBufferVec.removeAt(0).toInt() and 0xff
                    val lPage: Int = mBufferVec.removeAt(0).toInt() and 0xff
                    val rPage = hPage shl 8 or lPage
                    //保留
                    mBufferVec.removeAt(0)
                    mBufferVec.removeAt(0)
                    mBufferVec.removeAt(0)
                    if (rPage != page) {
                        iapState = IAPState.error
                        postEvent(IAPCheckEvent("烧录第" + rPage + "页出错"))
                        break
                    }
                    val hFlags: Int = mBufferVec.removeAt(0).toInt() and 0xff
                    val lFlags: Int = mBufferVec.removeAt(0).toInt() and 0xff
                    //帧数据完整性标志，数据完好则为1，否则为0
                    frameFlags = hFlags shl 8 or lFlags
                    iapState = IAPState.programing //继续
                    //0xFFFF -> 二进制 1111 1111 1111 1111
                    if (frameFlags == 0xFFFF) {
                        page++ // 帧完整，页面++
                        frameFlags = 0
                        val per = page.toFloat() * pageSize / (mIapFile?.length ?: 1) * 100
                        postEvent(IAPProgramingEvent(per.toInt()))
                        if (page * pageSize >= (mIapFile?.length ?: 0)) {
                            iapState = IAPState.end //结束
                        }
                    }
                }

                IAPCmd.end -> {
                    LogUtil.v("end")
                    iapState = IAPState.idle
                    postEvent(IAPEndEvent())
                    mFileIs = null
                    frameFlags = 0
                    page = 0
                }
            }
            mBufferVec.removeAt(0) //crc
        }
    }

    private fun analyseVersion(state: Byte) {

        var temp1: Int
        var temp2: Int
        //硬件版本
        val h1 = mBufferVec.removeAt(0)
        val h2 = mBufferVec.removeAt(0)
        temp1 = getH4(h1)
        temp2 = getL4(h1)
        var verHard = temp1 * 1000 + temp2 * 100
        temp1 = getH4(h2)
        temp2 = getL4(h2)
        verHard += temp1 * 10 + temp2
        //软件版本
        val s1 = mBufferVec.removeAt(0)
        val s2 = mBufferVec.removeAt(0)
        temp1 = getH4(s1)
        temp2 = getL4(s1)
        var verSoft = temp1 * 1000 + temp2 * 100
        temp1 = getH4(s2)
        temp2 = getL4(s2)
        verSoft += temp1 * 10 + temp2
        val response: Byte = (state.toInt() and 0x0f).toByte() //低4位
        LogUtil.v("analyseVersion----> ${response.toInt()}")
        if (response.toInt() == 0x01) { //下位机已经准备好
            LogUtil.v("analyseVersion---->start")
            iapState = IAPState.start
        } else if (response.toInt() == 0x02) { //只获取版本号
            iapState = IAPState.idle
            LogUtil.v("analyseVersion---->idle")
        }
        postEvent(IAPVersionEvent(verHard, verSoft, response))
    }

    /**
     * 发送命令
     */
    private fun sendCmd(outCmd: CmdType) {
        val bytes = ByteArray(outCmd.length + 4)
        bytes[0] = 0xAA.toByte()
        bytes[1] = 0x55.toByte()
        bytes[2] = (outCmd.code shr 8).toByte() //令牌
        bytes[3] = outCmd.length.toByte()
        bytes[4] = (outCmd.code and 0xFF).toByte() //类型
        var i = 0
        while (i < outCmd.length - 2) { //内容长度
            bytes[5 + i] = outCmd.buf[i].toByte()
            i++
        }
        bytes[5 + i] = CRC.crc8Check(bytes, outCmd.length + 3).toByte()
        try {
            LogUtil.v("write  ---->  " + HexUtil.bytesToHexString(bytes))
            mOutputStream.write(bytes)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 接收数据线程
     */
    private val buffer = ByteArray(64)

    internal inner class RecvThread : Thread() {
        override fun run() {
            synchronized(this) {
                while (!bStopRecv) {
                    try {
                        if (mInputStream.available() > 0) {
                            val len = mInputStream.read(buffer)
                            for (i in 0 until len) {
                                mBufferVec.add(buffer[i])
                            }
                            LogUtil.v("receive  ---->  " + HexUtil.bytesToHexString(buffer))
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun stopIAP() {
        bStopRecv = true
        bStopIAP = true
        frameFlags = 0
        page = 0
        mBufferVec.clear()
        if (mIapFile != null) {
            mIapFile?.close()
            mIapFile = null
        }
        mFileIs = null
    }

    companion object {
        /**
         * 帧数据完整性标志，数据完好则为1，否则为0
         */
        var frameFlags = 0
        var page = 0
        const val pageSize = 512
    }

    init {
        iapState = IAPState.idle
        mVerifier = Verifier()
        mCmdType = CmdType()
        mRecvThread = RecvThread()
        mRecvThread.start()
    }
} //--------------- 命令 类型 -------

class CmdType {
    var code = -1
    private var mLength: Int
    var buf = IntArray(1)

    //帧号+固件数据
    var length: Int
        get() = mLength
        set(length) {
            mLength = length
            buf = IntArray(length - 2) //帧号+固件数据
        }

    init {
        //状态机状态
        //定时器; tick->记录时间，超时检测用
        mLength = 0
    }
} //---------------文件操作 ---------

internal class IAPFile(input: InputStream) {
    enum class SeekOrigin {
        Begin, Current, End
    }

    var length = 0
        private set
    private var position = 0
    private lateinit var buffer: ByteArray
    fun read(buf: IntArray, offset: Int, size: Int): Int {
        if (!this::buffer.isInitialized) {
            buffer = ByteArray(length)
        }
        for (i in 0 until size) {
            buf[offset + i] = buffer[position++].toInt() and 0xFF
        }
        return size
    }

    fun seek(offset: Int, origin: SeekOrigin) {
        when (origin) {
            SeekOrigin.Begin -> position = offset
            SeekOrigin.Current -> position += offset
            SeekOrigin.End -> position = length - offset
        }
    }

    fun close() {
        length = 0
        position = 0
    }

    init {
        try {
            length = input.available()
            buffer = ByteArray(length)
            input.read(buffer)
            input.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
} //------------CRC 校验 --

internal object CRC {
    private val crc_8_table = intArrayOf(
        0x00,
        0x5E,
        0xBC,
        0xE2,
        0x61,
        0x3F,
        0xDD,
        0x83,
        0xC2,
        0x9C,
        0x7E,
        0x20,
        0xA3,
        0xFD,
        0x1F,
        0x41,
        0x9D,
        0xC3,
        0x21,
        0x7F,
        0xFC,
        0xA2,
        0x40,
        0x1E,
        0x5F,
        0x01,
        0xE3,
        0xBD,
        0x3E,
        0x60,
        0x82,
        0xDC,
        0x23,
        0x7D,
        0x9F,
        0xC1,
        0x42,
        0x1C,
        0xFE,
        0xA0,
        0xE1,
        0xBF,
        0x5D,
        0x03,
        0x80,
        0xDE,
        0x3C,
        0x62,
        0xBE,
        0xE0,
        0x02,
        0x5C,
        0xDF,
        0x81,
        0x63,
        0x3D,
        0x7C,
        0x22,
        0xC0,
        0x9E,
        0x1D,
        0x43,
        0xA1,
        0xFF,
        0x46,
        0x18,
        0xFA,
        0xA4,
        0x27,
        0x79,
        0x9B,
        0xC5,
        0x84,
        0xDA,
        0x38,
        0x66,
        0xE5,
        0xBB,
        0x59,
        0x07,
        0xDB,
        0x85,
        0x67,
        0x39,
        0xBA,
        0xE4,
        0x06,
        0x58,
        0x19,
        0x47,
        0xA5,
        0xFB,
        0x78,
        0x26,
        0xC4,
        0x9A,
        0x65,
        0x3B,
        0xD9,
        0x87,
        0x04,
        0x5A,
        0xB8,
        0xE6,
        0xA7,
        0xF9,
        0x1B,
        0x45,
        0xC6,
        0x98,
        0x7A,
        0x24,
        0xF8,
        0xA6,
        0x44,
        0x1A,
        0x99,
        0xC7,
        0x25,
        0x7B,
        0x3A,
        0x64,
        0x86,
        0xD8,
        0x5B,
        0x05,
        0xE7,
        0xB9,
        0x8C,
        0xD2,
        0x30,
        0x6E,
        0xED,
        0xB3,
        0x51,
        0x0F,
        0x4E,
        0x10,
        0xF2,
        0xAC,
        0x2F,
        0x71,
        0x93,
        0xCD,
        0x11,
        0x4F,
        0xAD,
        0xF3,
        0x70,
        0x2E,
        0xCC,
        0x92,
        0xD3,
        0x8D,
        0x6F,
        0x31,
        0xB2,
        0xEC,
        0x0E,
        0x50,
        0xAF,
        0xF1,
        0x13,
        0x4D,
        0xCE,
        0x90,
        0x72,
        0x2C,
        0x6D,
        0x33,
        0xD1,
        0x8F,
        0x0C,
        0x52,
        0xB0,
        0xEE,
        0x32,
        0x6C,
        0x8E,
        0xD0,
        0x53,
        0x0D,
        0xEF,
        0xB1,
        0xF0,
        0xAE,
        0x4C,
        0x12,
        0x91,
        0xCF,
        0x2D,
        0x73,
        0xCA,
        0x94,
        0x76,
        0x28,
        0xAB,
        0xF5,
        0x17,
        0x49,
        0x08,
        0x56,
        0xB4,
        0xEA,
        0x69,
        0x37,
        0xD5,
        0x8B,
        0x57,
        0x09,
        0xEB,
        0xB5,
        0x36,
        0x68,
        0x8A,
        0xD4,
        0x95,
        0xCB,
        0x29,
        0x77,
        0xF4,
        0xAA,
        0x48,
        0x16,
        0xE9,
        0xB7,
        0x55,
        0x0B,
        0x88,
        0xD6,
        0x34,
        0x6A,
        0x2B,
        0x75,
        0x97,
        0xC9,
        0x4A,
        0x14,
        0xF6,
        0xA8,
        0x74,
        0x2A,
        0xC8,
        0x96,
        0x15,
        0x4B,
        0xA9,
        0xF7,
        0xB6,
        0xE8,
        0x0A,
        0x54,
        0xD7,
        0x89,
        0x6B,
        0x35
    )

    fun crc8Check(data_buf: ByteArray, count: Int): Int {
        var crc = 0
        for (i in 0 until count) {
            crc = crc_8_table[crc xor (data_buf[i].toInt() and 0xFF)]
        }
        return crc
    }
}