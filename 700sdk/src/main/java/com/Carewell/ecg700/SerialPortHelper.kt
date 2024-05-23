@file:Suppress("PackageName")

package com.Carewell.ecg700

import android.os.Build
import android.os.Handler
import android.os.Looper
import android_serialport_api.SerialPort
import androidx.annotation.IntDef
import com.lepu.forwarder.util.HexUtil
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import java.util.*


/**
 *
 *  说明: 串口入口
 *  zrj 2022/3/25 15:13
 *
 */
class SerialPortHelper : OnSerialPortDataListener {
    //命令队列
    private val pendingQueue = PriorityQueue(20, compareBy<WriteData> { it.priority })

    // 发送串口命令的协程
    private var sendScope = CoroutineScope(Dispatchers.IO)

    fun setEcgListener(listener: OnECG12DataListener?) {
        parseEcg12Data.setOnECGDataListener(listener)
    }

    val serialPort: SerialPort by lazy { SerialPort(File("/dev/ttyMT1"), 460800, 0) }
    private var sphThreads: SphThreads? = null
    private var parseEcg12Data = ParseEcg12Data()

    /**
     * 开启读写线程
     */
    fun start() {
        sphThreads = SphThreads(serialPort.inputStream, this)
        parseEcg12Data.start()
        wakeUp()
    }

    fun pause() {
        sphThreads?.pause()
    }

    fun reStart() {
        sphThreads?.reStart()
    }

    private var canSend = true  //当前是否可以发送命令
    private var writeData: WriteData? = null
    private val mTimeoutHandler = Handler(Looper.getMainLooper())
    private val mCommandTimeoutRunnable = CommandTimeoutRunnable()
    private var retry = 0

    private inner class CommandTimeoutRunnable : Runnable {
        override fun run() {
            canSend = true
            if (retry > -1) {
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
                    retry = writeData?.retry ?: 0
                    sendData()
                }
            }
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
    }

    /**
     * 关闭串口
     */
    fun stop() {
        try {
            if (sendScope.isActive) {
                sendScope.cancel()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        parseEcg12Data.stop()
        sphThreads?.stop()
        serialPort.inputStream.close()
        serialPort.outputStream.close()
        serialPort.close()
    }


    override fun onDataReceived(bytes: ByteArray) {
        LogUtil.v("receive  ---->  " + HexUtil.bytesToHexString(bytes))
        var flag = true
        writeData?.let {
            // 设置血糖设备类型  ---->  aa, 55, e0, 03, 01, 03, 5f
            if (it.method == "设置血糖设备类型" && bytes[2] != 0xe0.toByte()) {
                flag = false
            }
            //停止12导测量  7f, c2, 00, 02, 00, 81, 08, 01, 00, 56, 31, 2e, 30, 2e, 30, 2e, 30, 00, 00, 00, 00, 6e,
            if (it.method == "停止12导测量" && bytes[1] != 0xc2.toByte() && bytes[3] != 0x02.toByte()) {
                flag = false
            }
            // 停止12导透传 aa, 55, 30, 02, 02, aa
            if (it.method == "停止12导透传" && bytes[2] != 0x30.toByte() && bytes[4] != 0x02.toByte()) {
                flag = false
            }
        }

        if (flag) {
            LogUtil.v("等待发送下一条cmd")
            mTimeoutHandler.removeCallbacksAndMessages(null)
            canSend = true
            processCommand()
        }
    }

    /**
     * 发送
     */
    @Suppress("BlockingMethodInNonBlockingContext")
    private fun sendData() {
        canSend = false
        writeData?.let {
            sendScope.launch {
                retry--
                delay(it.delay)
                if (it.isCRC) {
                    BaseProtocol.getCRC(it.bytes, it.bytes.size)
                }
                if (retry > 0) {
                    mTimeoutHandler.postDelayed(mCommandTimeoutRunnable, it.commandTimeoutMill)
                }
                try {
                    serialPort.outputStream.write(it.bytes)
                    serialPort.outputStream.flush()
                    LogUtil.v(it.method + "  ---->  " + HexUtil.bytesToHexString(it.bytes))
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                if (retry < 0) {
                    canSend = true
                    processCommand()
                }
            }
        }
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
        send(WriteData(Cmd.handShake, method = "握手", delay = 1000, priority = Priority.HIGH))
    }

    fun sleep() {
        send(WriteData(Cmd.sleepMachine, method = "休眠下位机", retry = 0))
    }

    //获取版本来区别温度
    private fun queryVersion() {
        send(WriteData(Cmd.queryVersion, method = "查询版本"))
    }

    //获取电池充电放电标识
    private fun queryBattery() {
        synchronized(this) {
            send(WriteData(Cmd.queryBattery, method = "查询电池状态"))
        }
    }

    fun setPressureMode(mode: Int) {
        Cmd.bNIBP_SetPressureMode[5] = mode.toByte()
        send(WriteData(Cmd.bNIBP_SetPressureMode, method = "设置血压模块"))
    }

    fun setGluType(mode: Int) {
        Cmd.bGLU_SetType[5] = mode.toByte()
        send(WriteData(Cmd.bGLU_SetType, method = "设置血糖设备类型"))
    }

    /**
     * 卡/证命令————开始扫描
     */
    fun setIDCard_StartScan() {
        send(WriteData(Cmd.bIDCard_StartScan, method = "IDCard感应身份证开始"))
    }

    /**
     * 卡/证命令————停止扫描
     */
    fun setIDCard_StopScan() {
        send(WriteData(Cmd.bIDCard_StopScan, method = "IDCard感应身份证停止"))
    }

    fun switchTemperature(position: Int, unit: Int) {
        Cmd.switchTemperature[5] = (position shl 4 or unit).toByte()
        send(WriteData(Cmd.switchTemperature, method = "设置温度模式"))
    }

    fun startSingleEcgMeasure() {
        send(WriteData(Cmd.bStartECG, method = "开始单导测量"))
    }

    fun stopSingleEcgMeasure() {
        send(WriteData(Cmd.bStopECG, method = "停止单导测量"))
    }

    fun startNIBPMeasure() {
        send(WriteData(Cmd.bNIBP_StartMeasureNIBP, method = "开始血压测量"))
    }

    fun stopNIBPMeasure() {
        send(WriteData(Cmd.bNIBP_StopMeasureNIBP, method = "停止血压测量"))
    }
    fun startTransfer() {
        send(
            WriteData(
                Cmd.startTransfer,
                method = "开始12导透传",
                priority = Priority.DEFAULT,
                isCRC = false,
                commandTimeoutMill = 2000L
            )
        )
    }

    fun startECG12Measure() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            pendingQueue.removeIf { it.bytes.contentEquals(Cmd.stopTransfer) }
        }
        send(
            WriteData(
                Cmd.startECG12Measure,
                method = "开始12导测量",
                delay = 2000L,
                priority = Priority.IMMEDIATELY,
                isCRC = false,
                commandTimeoutMill = 2000L
            )
        )
    }

    fun stopECG12Measure() {
        send(
            WriteData(
                Cmd.stopECG12Measure,
                method = "停止12导测量",
                priority = Priority.HIGH,
                isCRC = false,
                commandTimeoutMill = 3000L
            )
        )
    }

    fun stopTransfer() {
        send(
            WriteData(
                Cmd.stopTransfer,
                method = "停止12导透传",
                priority = Priority.HIGH,
                isCRC = false,
                commandTimeoutMill = 3000L
            )
        )
    }

    fun startDynamicAdjusting() {
        send(WriteData(Cmd.bNIBP_DynamicAdjustingStart, method = "开始动态压校验"))
    }

    fun stopDynamicAdjusting() {
        send(WriteData(Cmd.bNIBP_DynamicAdjustingStop, method = "停止动态压校验"))
    }
}


/**
 *
 * 命令包装
 * zrj 2020/7/16
 */
data class WriteData(
    val bytes: ByteArray,
    @Priority.Project
    var priority: Int = Priority.LOW,
    var method: String = "",
    var delay: Long = 0L,
    var retry: Int = 2,
    var isCRC: Boolean = true,
    var commandTimeoutMill: Long = 500L,
) {
    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            !is WriteData -> false
            else -> method == other.method
        }
    }
}

/**
 * 说明: 串口消息监听
 * zrj 2022/3/25 11:44
 */
interface OnSerialPortDataListener {
    /**
     * 数据接收
     */
    fun onDataReceived(bytes: ByteArray)

}

interface OnECG12DataListener {
    fun onECG12DataReceived(ecg12Data: IntArray)
    fun onHrReceived(hr: Int)
    fun onLeadFailReceived(leadFail: String, fall: Boolean)
}


/**
 *
 * 优先级
 * zrj 2020/7/16
 */
class Priority {
    companion object {
        const val LOW = 0
        const val DEFAULT = 1
        const val HIGH = 2
        const val IMMEDIATELY = 3
    }

    @IntDef(LOW, DEFAULT, HIGH, IMMEDIATELY)
    @Retention(AnnotationRetention.SOURCE)
    annotation class Project
}