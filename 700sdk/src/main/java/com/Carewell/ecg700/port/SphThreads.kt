package com.Carewell.ecg700.port

import com.Carewell.ecg700.ParseData
import kotlinx.coroutines.*
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

/**
 * SphThreads 类用于从输入流中读取数据，对数据进行解析和处理。
 * 该类使用协程进行异步数据读取，并通过缓冲区管理数据。
 */
class SphThreads(private val inputStream: InputStream, private val listener: OnSerialPortDataListener) {
    companion object {
        // 初始缓冲区大小
        private const val INITIAL_BUFFER_SIZE = 4096

        // 最大缓冲区大小，设置为 64KB
        private const val MAX_BUFFER_SIZE = 64 * 1024

        // 常规数据帧头
        private const val ROUTINE_HEADER = 0xAA55

        // 12 导联心电图数据帧头
        private const val ECG12_HEADER = 0x7F81

        // 12 导联心电图响应数据帧头
        private const val ECG12_RESPONSE_HEADER = 0x7FC2

        // 12 导联心电图数据帧大小
        private const val FRAME_SIZE_12LEAD = 22

        // 当前全局唯一的活跃读线程实例。
        // 背景：数据帧最终写入的是 ParseEcg12Data 的【静态全局 queue】，而 SphThreads 是实例级的。
        // APP 侧在页面快速切换时 start()/stop() 可能不配对（start 多、stop 少），
        // 导致多个 SphThreads 读线程同时存活、同时往同一个静态 queue 灌数据，
        // 生产速率变成 N 倍而消费仍是单协程，队列必然爆炸到百万帧 → OOM。
        // 因此这里强制“生产者全局唯一”：每启动一个新的读线程，先把上一个停掉，
        // 不依赖 APP 是否正确配对调用 stop。
        @Volatile
        private var activeInstance: SphThreads? = null

        @Synchronized
        private fun registerActive(instance: SphThreads) {
            // 顶掉上一个仍在运行的读线程，杜绝多生产者并存灌爆静态队列
            activeInstance?.let {
                if (it !== instance) {
                    // 定位日志：出现即说明 APP 侧串口 start/stop 未配对，旧读线程被新实例顶替。
                    // 频繁出现 = 存在读线程反复创建（这正是历史上多生产者灌爆静态队列的信号）。
                    LogUtil.e(
                        "检测到已有活跃串口读线程，顶替：旧实例=${System.identityHashCode(it)} → 新实例=${System.identityHashCode(instance)}",
                        "EcgLife"
                    )
                    it.stop()
                }
            }
            activeInstance = instance
            LogUtil.e("串口读线程注册为活跃：实例=${System.identityHashCode(instance)}", "EcgLife")
        }

        @Synchronized
        private fun unregisterActive(instance: SphThreads) {
            if (activeInstance === instance) activeInstance = null
        }
    }

    // 协程作用域，使用 IO 调度器和 SupervisorJob
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // 用于存储从输入流读取数据的缓冲区
    private var buffer = ByteBuffer.allocate(INITIAL_BUFFER_SIZE)

    // 用于同步操作的对象
    private val syncObject = Any()

    // 原子布尔值，用于控制数据读取循环的运行状态
    private var isRunning = AtomicBoolean(true)

    init {
        // 把“顶掉旧实例(registerActive)”与“启动本实例读协程(scope.launch)”放进同一把 Companion 锁，
        // 原子完成，避免两个实例并发构造时出现“旧实例已被顶掉、但本实例 launch 尚未执行”的窄窗口，
        // 该窗口会短暂产生两条读线程同时往静态 queue 灌数据。registerActive 本身也是 @Synchronized(Companion)，
        // 同一线程重入可重入锁，无死锁。
        synchronized(Companion) {
            // 启动读循环前先注册为全局唯一活跃实例，顶掉上一个可能未被 stop 的旧读线程，
            // 防止多个读线程并存往静态 queue 灌数据导致积压爆炸。
            registerActive(this)
            // 在 IO 线程中启动一个协程，持续读取输入流数据
            scope.launch(Dispatchers.IO) {
            // 临时缓冲区，用于从输入流读取数据
            val tempBuffer = ByteArray(4096)
            // 只要 isRunning 为 true，就持续读取数据
            while (scope.isActive) {
                if (!isRunning.get()) {
                    // 暂停状态下必须让出 CPU，否则空的 while 循环会 100% 占满一个 IO 线程忙等，
                    // 白白发热并抢占 CPU，间接拖慢心电消费协程。休眠一小段后再检查运行标志。
                    delay(50)
                    continue
                }
                if (isRunning.get()){
                    try {
                        // 设置 100 毫秒的超时时间读取输入流数据
                        val bytesRead = withTimeoutOrNull(100) {
                            inputStream.read(tempBuffer)
                        } ?: continue

                        // 如果读取到有效数据
                        if (bytesRead > 0) {
                            // 同步操作，确保线程安全
                            synchronized(syncObject) {
                                // 如果缓冲区剩余空间不足，扩展缓冲区
                                if (buffer.remaining() < bytesRead) {
                                    expandBuffer(buffer.position() + bytesRead)
                                }
                                // 将临时缓冲区的数据写入主缓冲区
                                buffer.put(tempBuffer, 0, bytesRead)
                            }
                            // 处理缓冲区中的数据
                            processBuffer()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // 记录数据处理错误日志
                        LogUtil.e("Data processing error: ${e.message}")
                        // 延迟 10 毫秒后继续尝试
                        delay(10)
                    }
                }
            }
            } // end scope.launch
        } // end synchronized(Companion)
    }

    /**
     * 暂停数据读取操作。
     */
    fun pause() {
        isRunning.set(false)
        LogUtil.v("pause")
    }

    /**
     * 重新启动数据读取操作。
     */
    fun reStart() {
        isRunning.set(true)
        LogUtil.v("reStart")
    }

    /**
     * 停止数据读取操作，取消协程并关闭输入流。
     */
    fun stop() {
        LogUtil.e("串口读线程 stop：实例=${System.identityHashCode(this)}", "EcgLife")
        isRunning.set(false)
        // 从全局活跃引用中注销自己（仅当自己确实是当前活跃实例）
        unregisterActive(this)
        // 取消协程作用域
        scope.cancel()
        try {
            // 关闭输入流
            inputStream.close()
        } catch (e: Exception) {
            // 记录关闭输入流错误日志
            LogUtil.e("Error closing stream: ${e.message}")
        }
    }

    /**
     * 核心处理逻辑，处理缓冲区中的数据。
     * 直接在 IO 协程中执行，避免不必要的线程切换。
     */
    private fun processBuffer() {
        synchronized(syncObject) {
            // 将缓冲区切换到读模式
            buffer.flip()
            try {
                // 只要缓冲区中至少有 2 个字节，就继续处理
                while (buffer.remaining() >= 2) {
                    // 标记当前位置，用于数据不足时回退
                    val mark = buffer.position()
                    // 读取 2 字节作为帧头
                    when (val header = buffer.short.toInt() and 0xFFFF) {
                        ROUTINE_HEADER -> {
                            if (!handleRoutineFrame()) {
                                buffer.position(mark) // 数据不足，回退等待拼接
                                break
                            }
                        }
                        ECG12_HEADER, ECG12_RESPONSE_HEADER -> {
                            if (!handleEcg12Frame(header)) {
                                buffer.position(mark) // 数据不足，回退等待拼接
                                break
                            }
                        }
                        else -> findNextHeader()
                    }
                }
            } finally {
                // 压缩缓冲区，准备下一次写入
                buffer.compact()
            }
        }
    }

    /**
     * 处理常规数据帧。 普通命令 aa, 55, 30, 02, 01, c6,
     * @return true 处理成功，false 数据不足需等待
     */
    private fun handleRoutineFrame(): Boolean {
        // 如果缓冲区剩余数据不足 4 字节，直接返回 等待拼接
        if (buffer.remaining() < 4) {
            return false
        }
        // 记录当前 position
        val currentPos = buffer.position()
        // 读取数据帧长度
        val length = buffer.get(currentPos + 1).toInt() and 0xFF
        // 如果缓冲区剩余数据不足指定长度，等待拼接
        if (buffer.remaining() < length + 2) {  //position 没有移动 + 2 = cmd + length
            return false
        }
        // 从缓冲区中提取数据帧
        val frameData = ByteArray(length + 4).apply {
            buffer.position(currentPos - 2)  // 回退 2 个字节 + 2 个头
            buffer.get(this, 0, length + 4)
        }
        // 分发数据
        ParseData.processingOrdinaryData(frameData)
        // 调用监听器的回调方法，通知数据接收
        listener.onDataReceived(frameData)
        return true
    }

    /**
     * 处理 12 导联心电图数据帧。
     *
     * @param header 数据帧头
     * @return true 处理成功，false 数据不足需等待
     */
    private fun handleEcg12Frame(header: Int): Boolean {
        // 如果缓冲区剩余数据不足指定长度，直接返回 等待拼接
        if (buffer.remaining() < FRAME_SIZE_12LEAD - 2) {
            return false
        }
        // 记录当前 position
        val currentPos = buffer.position()
        // 从缓冲区中提取数据帧
        val frameData = ByteArray(FRAME_SIZE_12LEAD).apply {
            buffer.position(currentPos - 2)
            buffer.get(this)
        }
        // 验证数据帧校验和
        if (verifyChecksum(frameData)) {
            when (header) {
                // 添加数据到解析器
                ECG12_HEADER -> ParseEcg12Data.addData(frameData)
                // 命令回复需要分发数据
                ECG12_RESPONSE_HEADER -> listener.onDataReceived(frameData)
            }
        } else {
            // 校验失败，回退   去掉2个头
            buffer.position(currentPos)
        }
        return true
    }

    /**
     * 扩展缓冲区大小。
     *
     * @param requiredCapacity 所需的缓冲区容量
     */
    private fun expandBuffer(requiredCapacity: Int) {
        // 计算新的缓冲区大小
        val newSize = min(requiredCapacity * 2, MAX_BUFFER_SIZE)
        // 如果新大小大于当前缓冲区容量，则进行扩展
        if (newSize > buffer.capacity()) {
            // 创建新的缓冲区
            val newBuffer = ByteBuffer.allocate(newSize)
            // 将原缓冲区切换到读模式
            buffer.flip()
            // 将原缓冲区的数据复制到新缓冲区
            newBuffer.put(buffer)
            // 清空原缓冲区
            buffer.clear()
            // 使用新缓冲区替换原缓冲区
            buffer = newBuffer
        }
    }

    /**
     * 查找下一个有效的数据帧头。
     */
    private fun findNextHeader() {
        var found = false
        while (buffer.remaining() >= 1 && !found) {
            val mark = buffer.position()
            val first = buffer.get().toInt() and 0xFF
            if (first == 0xAA || first == 0x7F) {
                if (buffer.remaining() >= 1) {
                    val second = buffer.get().toInt() and 0xFF
                    val header = (first shl 8) or second
                    when (header) {
                        ROUTINE_HEADER, ECG12_HEADER, ECG12_RESPONSE_HEADER -> {
                            buffer.position(mark)
                            found = true
                        }
                        else -> buffer.position(mark + 1)
                    }
                } else {
                    buffer.position(mark)
                    break
                }
            }
        }
    }

    /**
     * 验证数据帧的校验和。
     */
    private fun verifyChecksum(frame: ByteArray): Boolean {
        val size = frame.size
        var sum: Byte = 0
        for (i in 0..size - 2) {
            sum = (sum + frame[i]).toByte()
        }
        val check = sum == frame[size - 1]
        if (!check) {
            LogUtil.v(
                "校验失败-->${HexUtil.bytesToHexString(frame)}  校验值-->${
                    HexUtil.bytesToHexString(
                        byteArrayOf(sum)
                    )
                }"
            )
        }
        return check
    }
}