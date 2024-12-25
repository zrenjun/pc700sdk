package com.Carewell.ecg700.port

import android.os.Handler
import android.os.Looper
import com.Carewell.ecg700.ParseData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.PriorityQueue


/**
 *
 *  说明: 固件升级
 *  zrj 2023/2/8 10:45
 *
 */
class IPAThreads(inputStream: InputStream, outputStream: OutputStream) {
    private var scope = CoroutineScope(Dispatchers.IO)
    private val buffer = ByteArray(256)
    private var writeStream = outputStream

    @Volatile
    private var flag = true

    init {
        scope.launch(Dispatchers.IO) {
            while (scope.isActive) {
                if (flag) {
                    try {
                        if (inputStream.available() > 0) {
                            val len = inputStream.read(buffer) // 读取数据
                            val pkg = ByteArray(len)
                            System.arraycopy(buffer, 0, pkg, 0, len)
                            parseData(pkg)
                        } else {
                            delay(3)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun stop() {
        try {
            if (scope.isActive) {
                scope.cancel()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun pause() {
        flag = false
        page = 0
        if (mIapFile != null) {
            mIapFile?.close()
            mIapFile = null
        }
        mFileIs = null
    }

    private fun parseData(bytes: ByteArray) {
        onDataReceived(bytes)
        val len = bytes[3].toInt() and 0xFF //长度
        val type = bytes[4].toInt() and 0xFF //类型
        when (bytes[2]) { //令牌
            (0xF0).toByte() -> {
                //获取主固件版本
                //aa, 55, f0, 07, 01, 02, 00, 00, 13, 17, 88,
                //获取子固件版本
                //aa, 55, f0, 07, 01, 12, 00, 00, 13, 20, c9,
                val state = bytes[5].toInt() and 0xFF
                if (len > 3 && type == 1) {
                    val response: Byte = (state and 0x0f).toByte() //低4位
                    if (response.toInt() == 0x01) { //下位机已经准备好  aa, 55, f0, 07, 01, 01, 00, 00, 13, 17, c6,
                        startUpgradeStatus()
                    }
                    var temp1: Int
                    var temp2: Int
                    //主软件版本
                    var s1 = bytes[8]
                    var s2 = bytes[9]
                    temp1 = ParseData.getH4(s1)
                    temp2 = ParseData.getL4(s1)
                    var verSoft = temp1 * 1000 + temp2 * 100
                    temp1 = ParseData.getH4(s2)
                    temp2 = ParseData.getL4(s2)
                    verSoft += temp1 * 10 + temp2


                    //下位机新修改子固件版本
                    s1 = bytes[6]
                    s2 = bytes[7]
                    temp1 = ParseData.getH4(s1)
                    temp2 = ParseData.getL4(s1)
                    var verSoft2 = temp1 * 1000 + temp2 * 100
                    temp1 = ParseData.getH4(s2)
                    temp2 = ParseData.getL4(s2)
                    verSoft2 += temp1 * 10 + temp2



                    postEvent(IAPVersionEvent(verSoft2, verSoft, bytes[5]))
                }
                //aa, 55, f0, 03, 02, 00, d0
                if (len == 3 && type == 2) {  //开始升级
                    //加载文件
                    mIapFile?.close()
                    mFileIs?.let { mIapFile = IAPFile(it) }
                    sendFile()
                }

                //aa, 55, f0, 05, 03, 00, 00, 00, 4b
                if (len == 5 && type == 3) { //升级结束
                    postEvent(IAPEndEvent())
                    mFileIs = null
                    page = 0
                    frame = 0
                }
            }

            (0xF1).toByte() -> {  //check 回复  aa, 55, f1, 09, 02, 00, 42, 00, 00, 00, ff, ff, a9,
                if (len != 9) {
                    return
                }
                val hPage = bytes[5].toInt() and 0xff
                val lPage = bytes[6].toInt() and 0xff
                val rPage = hPage shl 8 or lPage
                if (rPage != page) {
                    postEvent(IAPCheckEvent("烧录第" + rPage + "页出错"))
                    return
                }
                val hFlags = bytes[10].toInt() and 0xff
                val lFlags = bytes[11].toInt() and 0xff
                //帧数据完整性标志
                val frameFlags = hFlags shl 8 or lFlags
                if (frameFlags == 0xFFFF) {
                    page++ // 帧完整，页面++
                    LogUtil.v("page------> $page")
                    val per = page.toFloat() * pageSize / (mIapFile?.length ?: 1) * 100
                    postEvent(IAPProgramingEvent(per.toInt()))
                    if (page * pageSize >= (mIapFile?.length ?: 0)) {
                        //结束
                        upgradeStatusEnd()
                        return
                    }
                }
                sendFile() //继续
            }
            //唤醒下位机 握手
            (0xFF).toByte() -> { //aa, 55, ff, 08, 01, 50, 43, 37, 30, 30, 00, 48,
                if (len == 8 && type == 1) {
                    postEvent(ShakeHandsEvent())
                }
            }
        }
    }

    @Volatile
    private var frame = 0

    /**
     * 发送文件
     */
    @Synchronized
    private fun sendFile() {
        sendScope.launch {
            if (frame > 15) {
                frame = 0
            }
            val mCmdFile = CmdType()
            mCmdFile.length = 36
            mCmdFile.code = 0xF101 //令牌F0+类型01
            mCmdFile.buf[0] = (page * 16 + frame shr 8).toByte().toInt() //H字节
            mCmdFile.buf[1] = (page * 16 + frame and 0xFF).toByte().toInt() //L字节
            mIapFile?.seek((page * 16 + frame) * 32, IAPFile.SeekOrigin.Begin)
            mIapFile?.read(mCmdFile.buf, 2, 32)
            send(
                WriteData(
                    getCmd(mCmdFile),
                    method = "文件帧 ${page * 16 + frame}",
                    retry = 0
                )
            )
            frame += 1
            if (frame == 16) {
                //每发送完16帧（512字节固件数据）后，需发送iap_check检查并补发缺失帧
                val mCmdCheck = CmdType()
                mCmdCheck.length = 5
                mCmdCheck.code = 0xF102 //令牌F0+类型01
                mCmdCheck.buf[0] = 0 // 保留
                mCmdCheck.buf[1] = (page shr 8).toByte().toInt() // H字节
                mCmdCheck.buf[2] = (page and 0xFF).toByte().toInt() // L字节
                send(
                    WriteData(
                        getCmd(mCmdCheck),
                        method = "checking",
                        delay = 100,
                        retry = 0
                    )
                )
                return@launch
            }
            delay(25)
            sendFile()
        }
    }


    /**
     * 发送命令
     */
    private fun getCmd(outCmd: CmdType): ByteArray {
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
        return bytes
    }


    /**
     * 获取固件版本号
     */
    fun getIAPVer(isMain: Boolean) {
        send(
            WriteData(
                if (isMain) Cmd.getMainVer else Cmd.getSubVer,
                method = if (isMain) "获取主固件版本" else "获取子固件版本", delay = 200
            )
        )
    }

    private var mIapFile: IAPFile? = null
    private var mFileIs: InputStream? = null

    private var page = 0
    private val pageSize = 512

    /**
     * 设置升级文件
     * @param fileIs  文件输入流
     */
    fun setIAPFile(fileIs: InputStream, isMain: Boolean) {
        mFileIs = fileIs
        page = 0
        send(
            WriteData(
                if (isMain) Cmd.enterUpgradeStatusMain else Cmd.enterUpgradeStatusSub,
                method = if (isMain) "主固件进入升级状态" else "子固件进入升级状态", delay = 300
            )
        )
    }

    /**
     * 开始升级
     */
    private fun startUpgradeStatus() {
        send(WriteData(Cmd.startUpgradeStatus, method = "开始升级", delay = 200))
    }

    /**
     * 升级结束
     */
    private fun upgradeStatusEnd() {
        send(WriteData(Cmd.upgradeStatusEnd, method = "升级结束", delay = 200))
    }

    /**
     * 唤醒下位机
     */
    fun wakeUp() {
        send(
            WriteData(
                ByteArray(80),
                method = "唤醒下位机",
                priority = Priority.IMMEDIATELY,
                retry = 0,
                isCRC = false
            )
        )
        //发送握手包,激活下位机
        send(WriteData(Cmd.handShake, method = "握手", delay = 1500, priority = Priority.HIGH, commandTimeoutMill = 1000))
    }


    //命令队列
    private val pendingQueue = PriorityQueue(20, compareBy<WriteData> { it.priority })

    // 发送串口命令的协程
    private var sendScope = CoroutineScope(Dispatchers.IO)
    private var canSend = true  //当前是否可以发送命令
    private var writeData: WriteData? = null
    private val commandTimeoutMill = 500L
    private val mTimeoutHandler = Handler(Looper.getMainLooper())
    private val mCommandTimeoutRunnable = CommandTimeoutRunnable()
    private var retry = 16

    private inner class CommandTimeoutRunnable : Runnable {
        override fun run() {
            canSend = true
            if (retry > -1) {
                retry--
                LogUtil.v("超时重发")
                sendData()
            } else {
                processCommand() //发送下一条
            }
        }
    }

    /**
     * 发送数据
     */
    private fun send(writeData: WriteData) {
        try {
            if (!pendingQueue.contains(writeData)) {
                pendingQueue.add(writeData)
                processCommand()
            }
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
    }

    private fun processCommand() {
        try {
            if (canSend) {
                synchronized(pendingQueue) {
                    if (pendingQueue.isEmpty()) {
                        return
                    }
                    writeData = pendingQueue.poll()
                    retry--
                    sendData()
                }
            }
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
    }

    private fun onDataReceived(bytes: ByteArray) {
        LogUtil.v("receive  ---->  " + HexUtil.bytesToHexString(bytes))
        var flag = true
        writeData?.let {
            if (it.method == "主固件进入升级状态" && bytes[2] != 0xf0.toByte()) {
                flag = false
            }
            if (it.method == "子固件进入升级状态" && bytes[2] != 0xf0.toByte()) {
                flag = false
            }
        }
        if (flag) {
            mTimeoutHandler.removeCallbacksAndMessages(null)
            canSend = true
            retry = 16
            processCommand()
        }
    }

    /**
     * 发送
     */
    private fun sendData() {
        canSend = false
        writeData?.let {
            sendScope.launch {
                delay(it.delay)
                if (it.isCRC) {
                    BaseProtocol.getCRC(it.bytes, it.bytes.size)
                }
                if (it.retry > 0) {
                    mTimeoutHandler.postDelayed(mCommandTimeoutRunnable, commandTimeoutMill)
                }
                try {
                    withContext(Dispatchers.IO) {
                        writeStream.write(it.bytes)
                        writeStream.flush()
                    }
                    LogUtil.v(it.method + "  ---->  " + HexUtil.bytesToHexString(it.bytes))
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                if (it.retry < 1) {
                    canSend = true
                    processCommand()
                }
            }
        }
    }
}
