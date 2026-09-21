package com.Carewell.ecg700.port

import android.os.Debug
import android.os.Process
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

    // 通知 APP 滤波旁路状态发生变化(true=已自动旁路滤波，波形降级为未滤波；false=已恢复滤波)。
    // 在消费线程回调，与波形/心率回调同线程；APP 可据此提示"高负载，波形已降级"。
    private fun notifyBypassChanged(bypassed: Boolean) {
        try {
            onECGDataListener?.onFilterBypassChanged(bypassed)
        } catch (t: Throwable) {
            LogUtil.e("onFilterBypassChanged 回调异常: ${t.message}", "EcgLife")
        }
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
        // 提升本消费线程的调度优先级。日志显示高温下本线程的 10 秒统计窗口常被拉长到 12~13 秒，
        // 即线程被系统周期性延迟调度(拿不到 CPU 时间片)，导致队列积压、显示延迟不稳定(观感卡顿)。
        // 用 Android 的 Process.setThreadPriority(设置 Linux nice 值)才能真正影响调度——
        // Java 的 Thread.priority 在 Android 上几乎无效。
        // 选用 URGENT_AUDIO(-19)：心电是硬实时的连续采样流，需要与音频同级的调度保障，
        // 确保 CPU 紧张(高温降频/其它线程繁忙)时本线程优先被执行，减少窗口被拉长的情况。
        try {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
        } catch (t: Throwable) {
            // 某些定制系统可能限制该调用，失败不影响功能，仅记录
            LogUtil.e("设置消费线程优先级失败: ${t.message}", "EcgLife")
        }
        val batch = ArrayList<ByteArray>(128)
        // 消费吞吐统计：每 10 秒打印一次这段时间实际处理的帧数换算成"帧/秒"。
        // 生产端约 1000 帧/秒，若这里长期明显低于 1000(且队列不为 0)，即消费追不上生产，
        // 是高温降频下定位"到底还差多少算力"的直接判据，无需再靠丢帧数反推。
        var framesInWindow = 0L
        // 本窗口内 processBatch 的累计纯处理耗时(纳秒)。只计处理、不含 queue.take() 的阻塞等待，
        // 因此能反映"每帧真正花了多少 CPU 时间"——这才是衡量优化效果的指标：
        // 帧/秒被生产端 1000Hz 封顶看不出余量，而"每帧 μs"和"处理占用率"会随代码变快而下降。
        // 高温降频时每帧 μs 会上升，正好量化降频吃掉了多少余量、以及优化补回了多少。
        var processNanosInWindow = 0L
        var windowStart = System.currentTimeMillis()
        // ---- 窗口拉长现场取证：诊断"消费线程为何被延迟" ----
        // 本线程 tid，用于读取 /proc/self/task/<tid>/stat 里本线程累计 CPU 时间(jiffies)。
        val myTid = Process.myTid()
        // 窗口开始时本线程已用 CPU 时间(jiffies)，与墙钟对比可判断"是被抢CPU"还是"自己算得慢"。
        var cpuJiffiesAtWindowStart = readThreadCpuJiffies(myTid)
        // 窗口开始时的 GC 次数，差值反映本窗口内是否频繁 GC(GC 可能抢占/暂停线程)。
        var gcCountAtWindowStart = readGcCount()
        // 各线程在窗口开始时的 CPU 时间快照，用于窗口异常拉长时点名"谁抢了 CPU"。
        var threadCpuSnapshot = snapshotAllThreadCpu()
        // 窗口开始时的累计丢帧数，差值即本窗口新增丢帧，用于判定是否过载。
        var droppedAtWindowStart = droppedFrames
        // ---- 过载自动旁路滤波的探测状态(仅本消费线程读写) ----
        // 下次允许探测恢复滤波的时间戳；进入旁路后需等到该时刻才试探恢复。
        var bypassProbeAtMs = 0L
        // 当前旁路时长(退避)：反复过载时翻倍，负载稳定后重置。
        var bypassBackoffMs = AUTO_BYPASS_BASE_MS
        // 最近一次恢复滤波的时间，用于判断"探测恢复后是否很快又过载"。
        var lastRecoverMs = 0L
        // 进入旁路的统一入口(窗口级判定和实时水位判定复用)：计算退避时长、置位、通知 APP。
        fun enterBypass(nowMs: Long, reason: String) {
            // 距上次恢复很近就又过载 => 上次探测恢复失败，旁路退避时间翻倍(上限封顶)；否则重置为基准。
            bypassBackoffMs = if (nowMs - lastRecoverMs < bypassBackoffMs * 2)
                minOf(bypassBackoffMs * 2, AUTO_BYPASS_MAX_MS) else AUTO_BYPASS_BASE_MS
            autoFilterBypass = true
            bypassProbeAtMs = nowMs + bypassBackoffMs
            notifyBypassChanged(true)
            LogUtil.e("$reason，自动旁路滤波保实时；${bypassBackoffMs / 1000}s 后探测恢复", "EcgLife")
        }
        while (!Thread.currentThread().isInterrupted) {
            try {
                // 记录在 take() 上阻塞等待的时长：队列为空(设备没在发12导数据)时这里会长时间阻塞。
                val waitStart = System.currentTimeMillis()
                val first = queue.take() // 阻塞直到有帧；被 interrupt() 时抛 InterruptedException 退出
                val waitedMs = System.currentTimeMillis() - waitStart
                // 空闲跳过：若本次是等了很久才等到数据(如启动后还没进测量页、设备尚未推送12导)，
                // 这段时间纯粹是空闲阻塞、并非“消费线程被抢CPU/拉长”。若把它算进统计窗口，
                // 会得到 fps≈0、窗口≈几十秒的假“窗口异常拉长”告警(实测启动首窗口 42s/2帧)。
                // 稳态 1000Hz 下每次 take() 阻塞仅约 1ms，远低于该阈值，不受影响。
                if (waitedMs >= IDLE_GAP_MS) {
                    windowStart = System.currentTimeMillis()
                    framesInWindow = 0
                    processNanosInWindow = 0
                    cpuJiffiesAtWindowStart = readThreadCpuJiffies(myTid)
                    gcCountAtWindowStart = readGcCount()
                    threadCpuSnapshot = snapshotAllThreadCpu()
                }
                batch.add(first)
                // 把队列中当前所有可用帧全部取出，一次性处理
                queue.drainTo(batch)
                framesInWindow += batch.size
                // 批量处理(计时：仅纯处理耗时)
                val t0 = System.nanoTime()
                processBatch(batch)
                processNanosInWindow += System.nanoTime() - t0
                batch.clear()

                // 实时水位触发：每处理完一批就查一次队列，逼近满(高水位)即立刻旁路，
                // 不必等到 10s 统计窗口，尽量赶在大量丢帧之前反应。
                if (autoFilterFallbackEnabled && isFilterEnabled && !autoFilterBypass &&
                    queue.size >= AUTO_BYPASS_QUEUE_HIGH
                ) {
                    enterBypass(System.currentTimeMillis(), "队列水位过高(${queue.size}/$QUEUE_CAPACITY)")
                }

                val now = System.currentTimeMillis()
                val elapsed = now - windowStart
                if (elapsed >= STAT_INTERVAL_MS) {
                    val fps = framesInWindow * 1000.0 / elapsed
                    // 每帧平均处理耗时(微秒)
                    val usPerFrame = if (framesInWindow > 0) processNanosInWindow / 1000.0 / framesInWindow else 0.0
                    // 处理占用率：本窗口花在处理上的时间 / 窗口总时长。越低说明消费端越空闲、余量越大。
                    val busyPct = processNanosInWindow / 1_000_000.0 / elapsed * 100.0

                    // ---- 诊断项 ----
                    // 1) 本线程实际拿到的 CPU 时间(ms) vs 窗口墙钟(ms)。
                    //    若 cpuMs 远小于 elapsed(如窗口13000ms但只拿到8000ms CPU)，说明有~5秒被抢走 → 被延迟调度，不是自己慢。
                    val cpuJiffiesNow = readThreadCpuJiffies(myTid)
                    val cpuMs = if (cpuJiffiesAtWindowStart >= 0 && cpuJiffiesNow >= 0)
                        (cpuJiffiesNow - cpuJiffiesAtWindowStart) * MS_PER_JIFFY else -1
                    // 本线程 CPU 占墙钟的比例：接近100%=一直在跑(自己慢)；明显偏低=被抢/被挂起。
                    val cpuOfWallPct = if (cpuMs >= 0) cpuMs * 100.0 / elapsed else -1.0
                    // 2) 本窗口 GC 次数增量
                    val gcCountNow = readGcCount()
                    val gcDelta = if (gcCountAtWindowStart >= 0 && gcCountNow >= 0) gcCountNow - gcCountAtWindowStart else -1L

                    LogUtil.v(
                        "12导消费吞吐: ${"%.1f".format(fps)}帧/秒 每帧${"%.1f".format(usPerFrame)}μs " +
                            "处理占用${"%.1f".format(busyPct)}% (窗口${elapsed}ms内处理${framesInWindow}帧) " +
                            "队列=${queue.size} 累计丢帧=$droppedFrames " +
                            "本线程CPU=${cpuMs}ms(占墙钟${"%.1f".format(cpuOfWallPct)}%) GC次数=$gcDelta"
                    )

                    // 3) 窗口异常拉长(明显超过应有的10秒)时，点名本窗口内 CPU 增量最大的几个线程，抓"谁抢了CPU"的现场。
                    val newSnapshot = snapshotAllThreadCpu()
                    if (elapsed >= WINDOW_LONG_THRESHOLD_MS) {
                        LogUtil.v("12导窗口异常拉长(${elapsed}ms)，本窗口CPU占用TOP线程: ${topCpuThreads(threadCpuSnapshot, newSnapshot)}")
                    }

                    // 4) 过载自动旁路滤波：高温/杂乱波形下每帧处理>1000μs 时，消费追不上 1000Hz 生产，
                    //    会持续丢帧(实测每帧~1150μs、占用~100%、队列顶死4096、丢帧不断上涨)。此时自动关闭
                    //    滤波(每帧回落~185μs)保住实时性与最新波形；负载缓解后再探测性地恢复滤波。
                    //    仅在"用户希望开启滤波(isFilterEnabled)"且"功能未被禁用"时接管，不覆盖用户的手动关闭。
                    if (autoFilterFallbackEnabled && isFilterEnabled) {
                        val dropsInWindow = droppedFrames - droppedAtWindowStart
                        val queueNow = queue.size
                        if (!autoFilterBypass) {
                            // 过载判据：本窗口已丢帧，或(占用逼近满载 且 队列积压达阈值)——后者可在真正大量丢帧前提前旁路。
                            val overloaded = dropsInWindow > AUTO_BYPASS_DROP_TRIGGER ||
                                (busyPct >= AUTO_BYPASS_BUSY_PCT && queueNow >= AUTO_BYPASS_QUEUE)
                            if (overloaded) {
                                enterBypass(
                                    now,
                                    "检测到持续过载(本窗口丢帧=$dropsInWindow 占用=${"%.1f".format(busyPct)}% 队列=$queueNow)"
                                )
                            }
                        } else if (now >= bypassProbeAtMs) {
                            // 冷却结束，探测性恢复滤波；若随即再次过载，上面的退避逻辑会自动延长下次旁路时长。
                            autoFilterBypass = false
                            lastRecoverMs = now
                            notifyBypassChanged(false)
                            LogUtil.e("负载已缓解，尝试恢复滤波(若随即再次过载将自动延长旁路时长)", "EcgLife")
                        }
                    } else if (autoFilterBypass) {
                        // 用户已手动关闭滤波或功能被禁用：清除旁路状态，避免残留。
                        autoFilterBypass = false
                        notifyBypassChanged(false)
                    }

                    framesInWindow = 0
                    processNanosInWindow = 0
                    windowStart = now
                    cpuJiffiesAtWindowStart = cpuJiffiesNow
                    gcCountAtWindowStart = gcCountNow
                    threadCpuSnapshot = newSnapshot
                    droppedAtWindowStart = droppedFrames
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

    // ---- 以下为"窗口拉长取证"诊断辅助，全部只读 /proc 或系统计数，开销小且仅每10秒调用 ----

    /**
     * 读取指定线程累计使用的 CPU 时间(jiffies = utime + stime)。
     * 数据源 /proc/self/task/<tid>/stat 的第 14、15 字段。失败返回 -1。
     * 注意：stat 第 2 字段(comm，线程名)可能含空格/括号，故从最后一个 ')' 之后开始按空格切分。
     */
    private fun readThreadCpuJiffies(tid: Int): Long {
        return try {
            val stat = java.io.File("/proc/self/task/$tid/stat").readText()
            val rp = stat.lastIndexOf(')')
            if (rp < 0) return -1
            // ')' 之后的字段：state(1) ppid(2) ... utime 是整体第14字段，即 ')' 后的第12个 token。
            val rest = stat.substring(rp + 1).trim().split(Regex("\\s+"))
            // rest[0]=state(第3字段)，故 utime(第14)=rest[11]，stime(第15)=rest[12]
            val utime = rest[11].toLong()
            val stime = rest[12].toLong()
            utime + stime
        } catch (t: Throwable) {
            -1
        }
    }

    /** 读取 ART 累计 GC 次数，失败返回 -1。 */
    private fun readGcCount(): Long {
        return try {
            Debug.getRuntimeStat("art.gc.gc-count")?.toLong() ?: -1
        } catch (t: Throwable) {
            -1
        }
    }

    /** 对进程内所有线程做一次 <tid -> CPU jiffies> 快照，用于计算窗口内各线程 CPU 增量。 */
    private fun snapshotAllThreadCpu(): HashMap<Int, Long> {
        val map = HashMap<Int, Long>()
        try {
            val tasks = java.io.File("/proc/self/task").list() ?: return map
            for (t in tasks) {
                val tid = t.toIntOrNull() ?: continue
                val j = readThreadCpuJiffies(tid)
                if (j >= 0) map[tid] = j
            }
        } catch (t: Throwable) {
            // 忽略
        }
        return map
    }

    /** 读取线程名(/proc/self/task/<tid>/comm)，失败返回 tid 字符串。 */
    private fun readThreadName(tid: Int): String {
        return try {
            java.io.File("/proc/self/task/$tid/comm").readText().trim()
        } catch (t: Throwable) {
            "tid$tid"
        }
    }

    /**
     * 对比两次线程 CPU 快照，输出本窗口内 CPU 增量最大的前几个线程(名字x耗时ms)。
     * 这能直接点名"窗口拉长期间到底是谁在占 CPU"，把"被谁抢"从猜测变为证据。
     */
    private fun topCpuThreads(before: HashMap<Int, Long>, after: HashMap<Int, Long>): String {
        return try {
            val deltas = ArrayList<Pair<Int, Long>>()
            for ((tid, jAfter) in after) {
                val jBefore = before[tid] ?: continue
                val d = jAfter - jBefore
                if (d > 0) deltas.add(tid to d)
            }
            deltas.sortByDescending { it.second }
            val sb = StringBuilder("[")
            for ((tid, d) in deltas.take(6)) {
                sb.append(readThreadName(tid)).append('=').append(d * MS_PER_JIFFY).append("ms ")
            }
            sb.append(']')
            sb.toString()
        } catch (t: Throwable) {
            "[取证失败:${t.message}]"
        }
    }

    fun stop() {
        synchronized(Companion) {
            LogUtil.v("ParseEcg12Data.stop 实例=${System.identityHashCode(this)} 队列当前=${queue.size} 累计丢帧=$droppedFrames", "EcgLife")
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

    // ---- 心率批量计算缓冲 ----
    // 原实现每帧调一次 JNI getDataHeartRate(每次只喂 1 个点)，1000Hz 下每秒 1000 次 JNI 跨界。
    // 心率检测是流式有状态算法，只要把整批采样点按原始顺序一个不漏地喂进去，native 内部看到的
    // 采样序列与逐点喂完全一致，心率结果不变——省的纯粹是 JNI 跨界固定开销。
    // hrBatch 收集本批每帧的心率输入点(滤波前，与原 hrWave 赋值时机一致)，批末一次性喂给 native。
    private var hrBatch = IntArray(INITIAL_POOL_SIZE)
    private var hrBatchSize = 0

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

        // 本批心率输入点收集器复位；容量不足时按批大小增长(与对象池同策略)
        if (hrBatch.size < batch.size) hrBatch = IntArray(maxOf(batch.size, hrBatch.size * 2))
        hrBatchSize = 0

        for (frame in batch) {
            if (!processFrame(frame)) continue
            val pooled = ecgDataPool[batchEcgData.size]
            System.arraycopy(ecgData, 0, pooled, 0, ecgData.size)
            batchEcgData.add(pooled)
            // 收集本帧心率输入点(processFrame 已在滤波前写入 hrWave[0])，批末一次性喂给 native
            hrBatch[hrBatchSize++] = hrWave[0]
        }

        // 心率批量计算：把本批所有采样点按原始顺序一次性喂给 native，等价于逐点喂但只跨界一次。
        // 传入精确长度的数组，避免复用缓冲的尾部残留被 native 当作有效数据处理。
        if (hrBatchSize > 0) {
            WaveFilter.instance?.let {
                val input = if (hrBatchSize == hrBatch.size) hrBatch else hrBatch.copyOf(hrBatchSize)
                lastHr = it.getRate(input)
            }
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

        // 滤波总开关：开启则走整套 JNI 滤波；关闭则直接用滤波前的原始值 filterWave(省算力，波形未滤波)。
        // 关闭滤波不影响心率——心率取的是上面 hrWave 里的滤波前原始导联值。
        val filtered = if (isFilterEnabled && !autoFilterBypass) {
            WaveFilter.instance?.filterControl(configBean, filterWave, leadOffArr) ?: filterWave
        } else {
            filterWave
        }

        if (pace == 1 && count == 0) {
            count = 2
        }

        // 本采样点是否需要叠加起搏标记。
        // 注意：起搏标记不能在推算之前写进 filtered[0](I)/filtered[1](II)——因为 III=II-I，
        // 给 I、II 写入相等的 PACE_MAKER_VALUE 会让 III 相减为 0，导致三导(III)看不到起搏标识(测试报的 bug)。
        // 正确做法：推算仍用真实(已清起搏)波形，标记在推算之后统一叠加到 12 个导联上。
        val paceMark = isAddPacemaker && count > 0
        if (paceMark) count--

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

            // 起搏标记：在推算完成后统一叠加，避免 III=II-I 把相等标记值抵消。
            // 仅打在有效(未脱落)导联上；肢导(I/II/III/aVR/aVL/aVF)在 I 或 II 脱落时整体不可信，不打。
            if (paceMark) {
                val v = PACE_MAKER_VALUE.toInt()
                if (!iFall) ecgData[0] = v
                if (!iiFall) ecgData[1] = v
                if (!limbDerivedFall) {
                    ecgData[2] = v // III：关键修复，不再被 II-I 抵消
                    ecgData[3] = v // AVR
                    ecgData[4] = v // AVL
                    ecgData[5] = v // AVF
                }
                if (!v1Fall) ecgData[6] = v // V1
                if (!v2Fall) ecgData[7] = v // V2
                if (!v3Fall) ecgData[8] = v // V3
                if (!v4Fall) ecgData[9] = v // V4
                if (!v5Fall) ecgData[10] = v // V5
                if (!v6Fall) ecgData[11] = v // V6
            }
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
                    LogUtil.v(
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

        // 每个 jiffy 对应的毫秒数。Android 内核 HZ 基本恒为 100，即 1 jiffy = 10ms。
        // 用于把 /proc stat 里的 CPU 时间(jiffies)换算成毫秒。
        private const val MS_PER_JIFFY = 10L

        // 窗口墙钟时长超过此阈值即视为"异常拉长"(正常应≈10000ms)，触发 TOP CPU 线程取证。
        private const val WINDOW_LONG_THRESHOLD_MS = 11_000L

        // ---- 过载自动旁路滤波的阈值 ----
        // 本窗口新增丢帧超过此数即判定过载(留一点余量，避开页面切换等一次性瞬时丢帧)。
        private const val AUTO_BYPASS_DROP_TRIGGER = 100L
        // 处理占用达到此比例视为逼近满载。
        private const val AUTO_BYPASS_BUSY_PCT = 95.0
        // 且队列积压达到此值时(配合占用率，窗口级早触发)，可在大量丢帧前提前旁路。
        private const val AUTO_BYPASS_QUEUE = 1024
        // 实时水位阈值：主循环每处理完一批就检查，队列积压达到此值(接近满)立刻旁路，
        // 不等 10s 统计窗口，尽量赶在大量丢帧之前反应。
        private const val AUTO_BYPASS_QUEUE_HIGH = 3072
        // 旁路基准时长：进入旁路后至少维持这么久再探测恢复。
        private const val AUTO_BYPASS_BASE_MS = 20_000L
        // 旁路退避上限：反复过载时旁路时长翻倍，但不超过此值。
        private const val AUTO_BYPASS_MAX_MS = 160_000L

        // take() 单次阻塞等待超过此阈值即判定为“空闲无数据”(而非消费拉长)，
        // 用于重置统计窗口基线，避免把启动/停测等空闲期误报为“窗口异常拉长”。
        // 稳态 1000Hz 下每次 take() 阻塞约 1ms，2 秒阈值不会误伤正常消费。
        private const val IDLE_GAP_MS = 2_000L

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

        // ---- 滤波总开关(对外) ----
        // true(默认)：正常走整套 JNI 滤波(DC恢复/高通/肌电/低通/工频)，波形为滤波后可用于诊断的波形。
        // false：跳过整套滤波，波形直接输出滤波前的原始导联值。
        // 用途：低端设备(如MT6735)高温降频时，滤波占每帧约70%的处理耗时；若确认卡顿源于算力不足，
        //       可由 APP 临时关闭滤波以大幅降低消费端负载(实测每帧从~770μs降到~215μs)。
        // 重要：关闭后波形含基线漂移/工频干扰，通常不适合临床诊断，仅供性能取证或特殊场景使用，
        //       由调用方(APP)自行决定何时关闭。心率不受影响——心率取的是滤波前的原始导联值。
        @Volatile
        var isFilterEnabled = true
            private set

        /**
         * 设置是否启用滤波。默认启用。
         * @param enabled true=启用滤波(诊断级波形)；false=关闭滤波(原始波形，仅省算力，不建议用于诊断)。
         */
        fun setFilterEnabled(enabled: Boolean) {
            isFilterEnabled = enabled
            LogUtil.v("滤波开关设置为: ${if (enabled) "开启" else "关闭"}", "EcgLife")
        }

        // ---- 过载自动旁路滤波(对外开关) ----
        // true(默认)：当消费端持续过载(高温+杂乱波形导致每帧>1000μs、持续丢帧)时，自动临时旁路滤波
        //             以保住实时性与最新波形，负载缓解后自动探测恢复。不改变 isFilterEnabled 的用户意图。
        // false：完全遵从 isFilterEnabled，不做任何自动降级(过载时将持续丢帧)。
        @Volatile
        var autoFilterFallbackEnabled = true
            private set

        fun setAutoFilterFallbackEnabled(enabled: Boolean) {
            autoFilterFallbackEnabled = enabled
            if (!enabled) autoFilterBypass = false
            LogUtil.v("滤波过载自动旁路: ${if (enabled) "开启" else "关闭"}", "EcgLife")
        }

        // 运行时旁路标志：由消费线程在过载时置位、恢复时清除；processFrame 据此临时跳过滤波。
        // 与 isFilterEnabled 正交——实际是否滤波 = isFilterEnabled && !autoFilterBypass。
        @Volatile
        private var autoFilterBypass = false
    }
}

