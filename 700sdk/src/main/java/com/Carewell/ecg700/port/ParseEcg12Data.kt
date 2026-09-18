package com.Carewell.ecg700.port

import com.Carewell.OmniEcg.jni.ConfigBean
import com.Carewell.OmniEcg.jni.PaceClearArr.feed
import com.Carewell.OmniEcg.jni.WaveFilter
import java.util.concurrent.LinkedBlockingQueue

/**
 * 说明: 12导解析
 * zrj 2022/4/7 15:09
 */
class ParseEcg12Data {

    private var onECGDataListener: OnECG12DataListener? = null

    fun setOnECGDataListener(onECGDataListener: OnECG12DataListener?) {
        this.onECGDataListener = onECGDataListener
    }

    // 消费线程。改用专用线程直接阻塞在 queue.take()，而不是协程里 runInterruptible(Dispatchers.IO) 逐批
    // 从 Dispatchers.Default 跳到 Dispatchers.IO 再跳回。
    // 原因：1000Hz 下批次很小、循环极频繁，每批一次线程切换 + runInterruptible 的中断处理器安装/拆卸
    // 是纯开销，实测导致消费长期比生产慢约 10%（日志中队列长期贴近满、约 90 帧/秒被丢弃）。
    // 专用线程整段跑在同一线程上，直接 take() 阻塞，取消靠 interrupt()，消除逐批切换开销。
    @Volatile
    private var consumeThread: Thread? = null

    // 注意：start()/stop() 统一用 companion(Companion) 作为锁对象，而非实例锁。
    // 因为“顶掉旧的全局活跃消费者”和“启动本实例消费协程”必须对同一把锁原子完成，
    // 否则两个实例并发 start 时，会出现“旧实例已被顶掉、但其协程 launch 语句已执行”的窄窗口，
    // 短暂产生两条消费协程抢同一个静态 queue。用 Companion 单锁把整段串行化即可根治。
    fun start() {
        synchronized(Companion) {
            // 定位日志：打印本实例标识，便于在日志中核对“是否存在多实例反复 start”。
            LogUtil.e("ParseEcg12Data.start 实例=${System.identityHashCode(this)} 队列当前=${queue.size} 累计丢帧=$droppedFrames", "EcgLife")
            // 把“当前待处理队列大小”提供给 LogUtil 资源采样器（此处 queue 已初始化，无前向引用问题）。
            // 采样日志里就能看到队列随时间的变化，配合线程数/内存判断 OOM 类型。多次注册是幂等的。
            LogUtil.setQueueSizeProvider { queue.size }
            // 消费者全局唯一：queue 是静态全局的，而消费协程是实例级。APP 每次进页面 new 一个新的
            // SerialPortHelper → 新的 ParseEcg12Data 实例，若旧实例未被 stop（APP 侧 start/stop 常不配对），
            // 多个实例的消费协程会同时抢同一个静态 queue，并各自钩住旧监听器/UI 造成泄漏。
            // 这里在启动本实例消费协程前，先顶掉上一个活跃实例，保证全局只有一条消费协程。
            registerActiveConsumer(this)
            // 幂等：若本实例已有活跃消费线程，先彻底停掉旧的，避免重复 start() 累积出多条消费者。
            stopInternal()
            clearQueue()
            val t = Thread({ consumeLoop() }, "ecg12-consumer").apply { isDaemon = true }
            consumeThread = t
            t.start()
        }
    }

    // 消费主循环：整段跑在专用线程上，直接阻塞在 queue.take()，无逐批线程切换。
    // 取消方式：stopInternal() 调用 interrupt()，take()/drainTo 抛出 InterruptedException 或线程中断标志置位后退出循环。
    private fun consumeLoop() {
        val batch = ArrayList<ByteArray>(128)
        // 消费吞吐统计：每 10 秒打印一次这段时间实际处理的帧数换算成"帧/秒"。
        // 生产端约 1000 帧/秒，若这里长期明显低于 1000(且队列不为 0)，即消费追不上生产，
        // 是高温降频下定位"到底还差多少算力"的直接判据，无需再靠丢帧数反推。
        var framesInWindow = 0L
        var windowStart = System.currentTimeMillis()
        while (!Thread.currentThread().isInterrupted) {
            try {
                val first = queue.take() // 阻塞直到有帧；被 interrupt() 时抛 InterruptedException 退出
                batch.add(first)
                // 把队列中当前所有可用帧全部取出，一次性处理
                queue.drainTo(batch)
                framesInWindow += batch.size
                // 批量处理
                processBatch(batch)
                batch.clear()

                val now = System.currentTimeMillis()
                val elapsed = now - windowStart
                if (elapsed >= STAT_INTERVAL_MS) {
                    val fps = framesInWindow * 1000.0 / elapsed
                    LogUtil.e(
                        "12导消费吞吐: ${"%.1f".format(fps)}帧/秒 (窗口${elapsed}ms内处理${framesInWindow}帧) " +
                            "队列=${queue.size} 累计丢帧=$droppedFrames",
                        "EcgStat"
                    )
                    framesInWindow = 0
                    windowStart = now
                }
            } catch (e: InterruptedException) {
                // 收到取消信号：恢复中断标志并退出循环
                Thread.currentThread().interrupt()
                batch.clear()
                break
            } catch (e: Exception) {
                LogUtil.e(e.message ?: "")
                e.printStackTrace()
                batch.clear()
            }
        }
    }

    fun stop() {
        synchronized(Companion) {
            LogUtil.e("ParseEcg12Data.stop 实例=${System.identityHashCode(this)} 队列当前=${queue.size} 累计丢帧=$droppedFrames", "EcgLife")
            unregisterActiveConsumer(this)
            stopInternal()
            clearQueue()
            onECGDataListener = null
        }
    }

    // 仅负责停掉当前消费线程，可被 start()/stop() 复用。
    // 幂等：多次调用安全，不会抛异常。interrupt() 会中断阻塞中的 queue.take() 使循环退出。
    private fun stopInternal() {
        consumeThread?.interrupt()
        consumeThread = null
    }

    private val leadData = ShortArray(8)
    private val ecgData = IntArray(12)

    @Volatile
    private var count = 2
    // 复用数组，避免每帧分配
    private val filterWave = Array(8) { ShortArray(1) }
    private val hrWave = IntArray(1)
    private val leadOffArr = IntArray(8)
    private val fallFlags = BooleanArray(8)

    // ---- 结果对象池，避免高频小对象分配造成 GC 压力 ----
    // 按需增长；同时支持在负载降低后动态收缩，避免峰值内存永久占用
    private var ecgDataPool: Array<IntArray> = Array(INITIAL_POOL_SIZE) { IntArray(12) }
    // 复用的批量结果列表，避免每批新建 ArrayList
    private val batchEcgData = ArrayList<IntArray>(INITIAL_POOL_SIZE)

    // ---- 动态收缩相关状态 ----
    // 当前统计窗口内观察到的最大实际使用量
    private var windowMaxUsage = 0
    private var windowStartTime = System.currentTimeMillis()
    // 连续多少个窗口都处于低使用量，用于消抖，避免频繁扩容/收缩抖动
    private var lowUsageWindowCount = 0

    private fun ensurePoolCapacity(requiredSize: Int) {
        if (requiredSize > ecgDataPool.size) {
            val old = ecgDataPool
            // 容量倍增而不是刚好等于需求，减少后续再次触发扩容的次数
            val newSize = maxOf(requiredSize, old.size * 2)
            ecgDataPool = Array(newSize) { i -> if (i < old.size) old[i] else IntArray(12) }
        }
    }

    /**
     * 根据最近一个统计窗口内的实际使用峰值，决定是否收缩对象池。
     * 收缩目标为「窗口内峰值 * 2」和「初始容量」中的较大值，保留一定余量，
     * 避免刚收缩完就因为下一批数据量稍大而立刻又扩容。
     * 需要连续 [REQUIRED_LOW_WINDOWS] 个窗口都判定为低使用量才会真正收缩，
     * 防止负载在临界值附近抖动时反复扩容/收缩。
     */
    private fun maybeShrinkPool(actualUsage: Int) {
        if (actualUsage > windowMaxUsage) windowMaxUsage = actualUsage

        val now = System.currentTimeMillis()
        if (now - windowStartTime < SHRINK_CHECK_INTERVAL_MS) return

        val target = maxOf(INITIAL_POOL_SIZE, windowMaxUsage * 2)
        if (target < ecgDataPool.size) {
            lowUsageWindowCount++
            if (lowUsageWindowCount >= REQUIRED_LOW_WINDOWS) {
                // 保留前 target 个已有对象，丢弃多余部分交给 GC 回收，
                // 而不是重新分配新对象（那样会失去对象池的意义）
                val old = ecgDataPool
                ecgDataPool = Array(target) { i -> old[i] }
                // 释放批量列表底层数组的多余容量，避免其容量一直停留在历史峰值
                batchEcgData.trimToSize()
                lowUsageWindowCount = 0
            }
        } else {
            lowUsageWindowCount = 0
        }
        windowMaxUsage = 0
        windowStartTime = now
    }

    /**
     * 批量处理多帧数据，一次性回调给 UI 减少回调开销
     *
     * 注意：batchEcgData 中的数组来自复用对象池，
     * 监听方必须在回调内同步处理完数据，不能跨批次持有引用。
     */
    private fun processBatch(batch: List<ByteArray>) {
        ensurePoolCapacity(batch.size)
        batchEcgData.clear()
        var lastHr = -1

        for (frame in batch) {
            if (!processFrame(frame)) continue
            val pooled = ecgDataPool[batchEcgData.size]
            System.arraycopy(ecgData, 0, pooled, 0, ecgData.size)
            batchEcgData.add(pooled)
            // 心率检测需要逐帧喂数据，hrWave 已在 processFrame 中更新
            WaveFilter.instance?.let { lastHr = it.getRate(hrWave) }
        }

        // 无论本批是否有效帧，都参与收缩统计，保证空闲期能被感知到
        maybeShrinkPool(batchEcgData.size)

        if (batchEcgData.isEmpty()) return

        // 导联脱落展示字符串只在状态发生变化时才重新拼接
        buildLeadFailStringIfNeeded()

        // 批量回调：一次性发送所有点
        onECGDataListener?.onECG12BatchDataReceived(batchEcgData)
        // 心率和导联状态只需要回调一次最新值
        onECGDataListener?.onHrReceived(lastHr)
        onECGDataListener?.onLeadFailReceived(cachedLeadStr, cachedLeadFall)
    }

    private fun processFrame(curByteBuffer: ByteArray): Boolean {
        if (curByteBuffer.size < 22) return false
        val frameHead = curByteBuffer[0].toInt() and 0xff
        val frameType = curByteBuffer[1].toInt() and 0xff
        if (frameHead != 0x7f || frameType != TYPE1) return false

        for (i in 0 until 8) {
            val index = 3 + i * 2
            // 内联 toInt，避免每帧创建临时 ByteArray
            val low = curByteBuffer[index].toInt() and 0xFF
            val high = curByteBuffer[index + 1].toInt() and 0xFF
            leadData[i] = ((high shl 8) or low).toShort()
        }

        var leadOff = curByteBuffer[19].toInt() and 0xFF
        var pace = curByteBuffer[20].toInt() and 0xFF

        val arr = feed(leadData, leadOff, pace) ?: return false
        if (arr.size < leadData.size + 2) return false // feed 返回异常数据，跳过该帧
        System.arraycopy(arr, 0, leadData, 0, leadData.size)
        leadOff = arr[arr.size - 2].toInt()
        pace = arr[arr.size - 1].toInt()

        checkLeadOff(leadOff)
        // 记录最后一帧的导联签名，供批次结束后按需重建展示字符串
        lastLeadOffSignature = leadOff

        // 复用 filterWave 数组
        for (i in 0 until 8) {
            filterWave[i][0] = leadData[i]
        }
        hrWave[0] = if (isLeadII) leadData[1].toInt() else leadData[0].toInt()

        // 复用 fallFlags 和 leadOffArr
        fallFlags[0] = iFall
        fallFlags[1] = iiFall
        fallFlags[2] = v1Fall
        fallFlags[3] = v2Fall
        fallFlags[4] = v3Fall
        fallFlags[5] = v4Fall
        fallFlags[6] = v5Fall
        fallFlags[7] = v6Fall
        for (i in 0 until 8) {
            leadOffArr[i] = if (fallFlags[i]) 1 else 0
        }

        val filtered =  WaveFilter.instance?.filterControl(configBean, filterWave, leadOffArr) ?: filterWave

        if (pace == 1 && count == 0) {
            count = 2
        }

        if (isAddPacemaker && count > 0) {
            for (i in 0 until 8) {
                filtered[i][0] = if (!fallFlags[i]) PACE_MAKER_VALUE else filtered[i][0]
            }
            count--
        }

        // III/AVR/AVL/AVF 由 I、II 共同计算得出，只要 I 或 II 任意一个脱落，
        // 这几个导联的计算结果就不可信，需要展示为直线（置零）
        val limbDerivedFall = iFall || iiFall

        val filterWaveSize = filtered[0].size
        for (k in 0 until filterWaveSize) {
            ecgData[0] = if (iFall) 0 else filtered[0][k].toInt() // I
            ecgData[1] = if (iiFall) 0 else filtered[1][k].toInt() // II
            if (limbDerivedFall) {
                ecgData[2] = 0 // III
                ecgData[3] = 0 // AVR
                ecgData[4] = 0 // AVL
                ecgData[5] = 0 // AVF
            } else {
                ecgData[2] = filtered[1][k] - filtered[0][k] // III
                ecgData[3] = -(filtered[0][k] + filtered[1][k]) shr 1 // AVR
                ecgData[4] = filtered[0][k] - (filtered[1][k].toInt() shr 1) // AVL
                ecgData[5] = filtered[1][k] - (filtered[0][k].toInt() shr 1) // AVF
            }
            ecgData[6] = filtered[2][k].toInt()
            ecgData[7] = filtered[3][k].toInt()
            ecgData[8] = filtered[4][k].toInt()
            ecgData[9] = filtered[5][k].toInt()
            ecgData[10] = filtered[6][k].toInt()
            ecgData[11] = filtered[7][k].toInt()
        }

        return true
    }

    private var iFall = false
    private var iiFall = false
    private var v1Fall = false
    private var v2Fall = false
    private var v3Fall = false
    private var v4Fall = false
    private var v5Fall = false
    private var v6Fall = false

    // 仅更新脱落标志位，不做任何分配
    private fun checkLeadOff(leadOff: Int) {
        iFall = (leadOff and 0b00000001) != 0
        iiFall = (leadOff and 0b00000010) != 0
        v1Fall = (leadOff and 0b00000100) != 0
        v2Fall = (leadOff and 0b00001000) != 0
        v3Fall = (leadOff and 0b00010000) != 0
        v4Fall = (leadOff and 0b00100000) != 0
        v5Fall = (leadOff and 0b01000000) != 0
        v6Fall = (leadOff and 0b10000000) != 0
    }

    // 批次内最后一帧的导联脱落位模式（-1 表示本批次没有有效帧）
    private var lastLeadOffSignature = -1
    // 上次已生成展示字符串对应的签名，用于判断是否需要重建
    private var cachedLeadOffSignature = -2
    private var cachedLeadStr = ""
    private var cachedLeadFall = false
    private val leadStrBuilder = StringBuilder(32)
    // 提升为字段而非局部变量，避免局部函数捕获可变局部变量时
    // 被编译器装箱为 Ref.BooleanRef 造成的额外分配
    private var hasAppendedLead = false

    private fun appendLead(name: String) {
        if (hasAppendedLead) leadStrBuilder.append(' ')
        leadStrBuilder.append(name)
        hasAppendedLead = true
    }

    /**
     * 仅当导联脱落状态相比上次回调发生变化时，才重新拼接展示字符串，
     * 避免每帧/每批都创建 List<String> 和字符串对象。
     */
    private fun buildLeadFailStringIfNeeded() {
        val signature = lastLeadOffSignature
        if (signature == cachedLeadOffSignature) return
        cachedLeadOffSignature = signature

        leadStrBuilder.setLength(0)
        hasAppendedLead = false
        if (iFall) appendLead("LA")
        if (iiFall) appendLead("LL")
        if (v1Fall) appendLead("V1")
        if (v2Fall) appendLead("V2")
        if (v3Fall) appendLead("V3")
        if (v4Fall) appendLead("V4")
        if (v5Fall) appendLead("V5")
        if (v6Fall) appendLead("V6")
        if (iFall && iiFall && v1Fall && v2Fall && v3Fall && v4Fall && v5Fall && v6Fall) {
            appendLead("RA")
            appendLead("RL")
        }
        cachedLeadStr = leadStrBuilder.toString()
        cachedLeadFall = hasAppendedLead
    }

    companion object {
        private var time = 0

        // 当前全局唯一的活跃消费者实例。理由同 SphThreads 的生产者唯一：
        // queue 是静态全局单例，消费协程却是实例级，多实例并存会抢队列 + 泄漏旧监听器/UI。
        @Volatile
        private var activeConsumer: ParseEcg12Data? = null

        @Synchronized
        private fun registerActiveConsumer(instance: ParseEcg12Data) {
            activeConsumer?.let {
                if (it !== instance) {
                    // 定位日志：出现这条即说明 APP 侧 start/stop 未配对，旧消费者被新实例顶替。
                    // 频繁出现 = 存在实例反复创建（潜在泄漏/积压诱因）。
                    LogUtil.e(
                        "检测到已有活跃12导消费者，顶替：旧实例=${System.identityHashCode(it)} → 新实例=${System.identityHashCode(instance)}",
                        "EcgLife"
                    )
                    it.stopInternal()
                    it.onECGDataListener = null
                }
            }
            activeConsumer = instance
        }

        @Synchronized
        private fun unregisterActiveConsumer(instance: ParseEcg12Data) {
            if (activeConsumer === instance) activeConsumer = null
        }

        // 入队帧率实测约 1000 帧/秒(1000Hz)。队列仅用于吸收短时处理抖动(GC、热节流、UI 卡顿)，
        // 4096 约等于 4 秒缓冲，稳态下长期贴近 0；满载内存也仅约 240KB，
        // 彻底杜绝高温长测时无界队列积压到百万帧拖垮内存/实时性的问题。
        private const val QUEUE_CAPACITY = 4096
        private val queue = LinkedBlockingQueue<ByteArray>(QUEUE_CAPACITY)

        // 消费吞吐统计打印间隔：每 10 秒一次。仅一条日志，开销可忽略。
        private const val STAT_INTERVAL_MS = 10_000L

        // 累计丢帧数，用于观测过载程度
        private var droppedFrames = 0L
        // 是否已打过“首次丢帧”告警。队列首次被打满(开始丢帧)是消费追不上生产的决定性信号，
        // 单独打一条，便于在日志里精确定位积压“从何时开始”。
        @Volatile
        private var everDropped = false

        fun clearQueue() {
            queue.clear()
        }

        fun addData(bytes: ByteArray) {
            // 有界队列：未满(size < QUEUE_CAPACITY)时 offer 直接成功入队，绝不丢弃；
            // 仅当队列已满时才丢弃队头(最旧)帧，保证总是保留最新波形，且绝不阻塞串口读取线程。
            // 用 while 循环重试：若在 poll 与再次 offer 之间被其它生产者重新填满，会继续丢队头重试，
            // 保证本帧最终一定入队，且每丢弃一帧都被准确计数，避免静默丢帧/计数遗漏的并发缺陷。
            // ECG 波形丢弃过时采样点对实时显示可接受，远好过无限积压。
            while (!queue.offer(bytes)) {
                if (queue.poll() != null) {   // 丢弃最旧，仅在确实移除了一帧时才计数
                    droppedFrames++
                    if (!everDropped) {
                        everDropped = true
                        // 首次丢帧：积压已顶到队列上限(4096)。出现这条=消费开始追不上生产，是根因排查的关键时间点。
                        LogUtil.e("12导队列首次打满开始丢帧(容量=$QUEUE_CAPACITY)，消费已追不上生产，请关注该时刻前后的消费端表现", "EcgLife")
                    }
                    if (droppedFrames % 1000 == 0L) {
                        LogUtil.e("12导队列已满,累计丢帧:$droppedFrames (队列容量:$QUEUE_CAPACITY)")
                    }
                }
            }
            time++
            // 仅在积压偏大时才周期性告警，稳态(队列贴近0)下完全不打日志，
            // 避免生产热路径上的无谓日志 I/O 反过来拖慢消费、诱发积压。
            if (time % 5000 == 0) {
                time = 0
                val size = queue.size
                if (size > QUEUE_CAPACITY / 4) {
                    LogUtil.v("待处理队列大小:$size")
                }
            }
        }

        private const val TYPE1 = 0x81 //12导联数据帧
        private const val PACE_MAKER_VALUE: Short = 1000
        // 批处理结果对象池初始大小/收缩下限，按运行期实际 batch 大小自动增长
        private const val INITIAL_POOL_SIZE = 256
        // 收缩判定的统计窗口时长
        private const val SHRINK_CHECK_INTERVAL_MS = 2000L
        // 需要连续多少个低使用量窗口才真正收缩，避免抖动
        private const val REQUIRED_LOW_WINDOWS = 3
        private var isLeadII = true
        fun setLeadHrMode(leadII: Boolean) {
            isLeadII = leadII
        }

        private val configBean = ConfigBean()
        fun setFilterParam(
            highPassSmooth: Float,
            lowPassSmooth: Int,
            emgSmooth: Int,
            acSmooth: Float
        ) {
            configBean.highPassSmooth = highPassSmooth
            configBean.lowPassSmooth = lowPassSmooth
            configBean.emgSmooth = emgSmooth
            configBean.aCSmooth = acSmooth
        }

        var isAddPacemaker = false
        fun setIsAddPacemaker(isAddPaceMaker: Boolean) {
            isAddPacemaker = isAddPaceMaker
        }
    }
}

