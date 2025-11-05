@file:Suppress("PackageName")

package com.Carewell.ecg700.port

import android.os.Handler
import android.os.Looper
import android_serialport_api.SerialPort
import androidx.annotation.IntDef
import com.Carewell.OmniEcg.jni.JniFilterNew
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import java.util.concurrent.PriorityBlockingQueue


/**
 *
 *  说明: 串口入口
 *  zrj 2022/3/25 15:13
 *
 */
class SerialPortHelper private constructor() : OnSerialPortDataListener {
    //命令队列
    private val pendingQueue = PriorityBlockingQueue(20, compareBy<WriteData> { it.priority })

    // 发送串口命令的协程
    private var sendScope = CoroutineScope(Dispatchers.IO)

    companion object {
        @Volatile
        private var INSTANCE: SerialPortHelper? = null

        fun getInstance(): SerialPortHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SerialPortHelper().also { INSTANCE = it }
            }
        }
    }

    fun setEcgListener(listener: OnECG12DataListener?) {
        parseEcg12Data.setOnECGDataListener(listener)
    }

    private lateinit var serialPort: SerialPort
    private var sphThreads: SphThreads? = null
    private var parseEcg12Data = ParseEcg12Data()

    private var isStart = false

    /**
     * 开启读写线程
     */
    fun start() {
        if (isStart) {
            return
        }
        serialPort = SerialPort(File("/dev/ttyMT1"), 460800, 0)
        sphThreads = SphThreads(serialPort.inputStream, this)
        parseEcg12Data.start()
        wakeUp()
        JniFilterNew.getInstance().InitDCRecover(0)
        isStart = true
    }

    fun pause() {
        sphThreads?.pause()
    }

    fun reStart() {
        sphThreads?.reStart()
    }

    //当前是否可以发送命令
    @Volatile
    private var canSend = true

    @Volatile
    private var cmd: WriteData? = null
    private val mTimeoutHandler = Handler(Looper.getMainLooper())
    private val mCommandTimeoutRunnable = CommandTimeoutRunnable()

    private inner class CommandTimeoutRunnable : Runnable {
        override fun run() {
            if ((cmd?.retry ?: -1) > -1) {
                LogUtil.v("超时重发")
                sendData()
            } else {
                mTimeoutHandler.removeCallbacksAndMessages(null)
                canSend = true
                processCommand()
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
                if (canSend) {
                    processCommand()
                }
            }
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
    }

    private fun processCommand() {
        try {
            if (pendingQueue.isEmpty()) {
                return
            }
            canSend = false
            cmd = pendingQueue.poll()
            sendData()
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
        isStart = false
        LogUtil.v("关闭串口")
    }

    override fun onDataReceived(bytes: ByteArray) {
        LogUtil.v("received  ---->  " + HexUtil.bytesToHexString(bytes))
        cmd?.let {
            if (bytes.size >= 3) {
                // 保持正常队列命令一发一收再发下一个，超时不算
                if (it.bytes[0] == 0xaa.toByte() && it.bytes[1] == 0x55.toByte()) {
                    if (it.bytes[2] == bytes[2]) {
                        mTimeoutHandler.removeCallbacksAndMessages(null)
                        canSend = true
                        processCommand()
                    }
                }
                // 心电命令
                if (it.bytes[0] == 0x7f.toByte() && it.bytes[1] == 0xc1.toByte()) {
                    if (it.bytes[3] == bytes[3]) {
                        mTimeoutHandler.removeCallbacksAndMessages(null)
                        canSend = true
                        processCommand()
                    }
                }
            }
        }
    }

    /**
     * 发送
     */
    private fun sendData() {
        cmd?.let {
            sendScope.launch {
                it.retry--
                delay(it.delay)
                if (it.isCRC) {
                    BaseProtocol.getCRC(it.bytes, it.bytes.size)
                }
                mTimeoutHandler.postDelayed(mCommandTimeoutRunnable, it.commandTimeoutMill)
                try {
                    LogUtil.v("${it.method} ----> " + HexUtil.bytesToHexString(it.bytes))
                    serialPort.outputStream.write(it.bytes)
                    serialPort.outputStream.flush()
                } catch (e: IOException) {
                    e.printStackTrace()
                    // 重置标志位并尝试重新处理命令
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
        synchronized(pendingQueue) {
            pendingQueue.removeIf { it.bytes.contentEquals(Cmd.sleepMachine) }
        }
        send(
            WriteData(
                Cmd.handShake,
                method = "握手",
                delay = 1000,
                priority = Priority.HIGH,
                retry = 0,
                isCRC = false
            )
        )
    }

    fun sleep() {
        send(WriteData(Cmd.sleepMachine, method = "休眠下位机", retry = 0, isCRC = false))
    }

    fun queryBattery() {
        send(WriteData(Cmd.queryBattery, method = "查询电量", retry = 0, isCRC = false))
    }

    fun setPressureMode(mode: Int) {
        Cmd.bNIBP_SetPressureMode[5] = mode.toByte()
        send(WriteData(Cmd.bNIBP_SetPressureMode, method = "设置血压模块类型"))
    }

    fun getPressureMode() {
        send(WriteData(Cmd.bNIBP_GetPressureMode, method = "获取血压模块类型"))
    }

    fun setGluType(mode: Int) {
        Cmd.bGLU_SetType[5] = mode.toByte()
        send(WriteData(Cmd.bGLU_SetType, method = "设置血糖设备类型"))
    }

    fun getGluType() {
        send(WriteData(Cmd.bGLU_GetType, method = "获取血糖设备类型", isCRC = false))
    }

    /**
     * usb-mode  0-device, 1-host
     */
    fun setUsb_Mode(mode: Int) {
        Cmd.usb_Mode[5] = mode.toByte()
        send(WriteData(Cmd.usb_Mode, method = "USB模式设置"))
    }

    /**
     * 卡/证命令————类型设置
     */
    fun setIDCard_Type(type: Int) {
        Cmd.bIDCard_Type[5] = type.toByte()
        send(WriteData(Cmd.bIDCard_Type, method = "IDCard类型设置"))
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

    fun setNIBPAdult() {
        send(WriteData(Cmd.bNIBP_SetAdult, method = "血压设置成人", isCRC = false))
    }

    fun setNIBPChild() {
        send(WriteData(Cmd.bNIBP_SetChild, method = "血压设置儿童", isCRC = false))
    }

    fun setNIBPInfant() {
        send(WriteData(Cmd.bNIBP_SetInfant, method = "血压设置婴儿", isCRC = false))
    }

    fun startTransfer() {
        pendingQueue.removeIf { it.bytes.contentEquals(Cmd.stopECG12Measure) }
        pendingQueue.removeIf { it.bytes.contentEquals(Cmd.stopTransfer) }
        JniFilterNew.getInstance().resetFilter()
        send(
            WriteData(
                Cmd.startTransfer,
                method = "开始12导透传",
                priority = Priority.DEFAULT,
                isCRC = false,
                commandTimeoutMill = 3000L
            )
        )
    }

    fun startECG12Measure() {
        pendingQueue.removeIf { it.bytes.contentEquals(Cmd.stopECG12Measure) }
        pendingQueue.removeIf { it.bytes.contentEquals(Cmd.stopTransfer) }
        send(
            WriteData(
                Cmd.startECG12Measure,
                method = "开始12导测量",
                priority = Priority.IMMEDIATELY,
                isCRC = false,
                commandTimeoutMill = 2000L
            )
        )
    }

    fun stopECG12Measure() {
        pendingQueue.removeIf { it.bytes.contentEquals(Cmd.startTransfer) }
        pendingQueue.removeIf { it.bytes.contentEquals(Cmd.startECG12Measure) }
        send(
            WriteData(
                Cmd.stopECG12Measure,
                method = "停止12导测量",
                priority = Priority.HIGH,
                isCRC = false,
                commandTimeoutMill = 2000L
            )
        )
    }

    fun stopTransfer() {
        pendingQueue.removeIf { it.bytes.contentEquals(Cmd.startTransfer) }
        pendingQueue.removeIf { it.bytes.contentEquals(Cmd.startECG12Measure) }
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

    fun startStaticAdjusting() {
        send(WriteData(Cmd.bNIBP_StaticAdjustingStart, method = "开始静态压校验"))
    }

    fun stopStaticAdjusting() {
        send(WriteData(Cmd.bNIBP_StaticAdjustingStop, method = "停止静态压校验"))
    }

    fun startDynamicAdjusting() {
        send(WriteData(Cmd.bNIBP_DynamicAdjustingStart, method = "开始动态压校验"))
    }

    fun stopDynamicAdjusting() {
        send(WriteData(Cmd.bNIBP_DynamicAdjustingStop, method = "停止动态压校验"))
    }

    fun startCheckLeakage() {
        send(WriteData(Cmd.bNIBP_CheckLeakageStart, method = "开始漏气检测"))
    }

    fun stopCheckLeakage() {
        send(WriteData(Cmd.bNIBP_CheckLeakageStop, method = "停止漏气检测"))
    }

    /**
     * 获取固件版本号
     */
    fun getVer(isMain: Boolean) {
        send(
            WriteData(
                if (isMain) Cmd.getMainVer else Cmd.getSubVer,
                method = if (isMain) "获取主固件版本" else "获取子固件版本",
                isCRC = false
            )
        )
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
    var delay: Long = 10L,
    var retry: Int = 2,
    var isCRC: Boolean = true,
    var commandTimeoutMill: Long = 500L,
) {
    override fun equals(other: Any?): Boolean {
        return when (other) {
            !is WriteData -> false
            else -> method == other.method && bytes.contentEquals(other.bytes)
        }
    }

    override fun hashCode(): Int {
        return method.hashCode() + bytes.contentHashCode()
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