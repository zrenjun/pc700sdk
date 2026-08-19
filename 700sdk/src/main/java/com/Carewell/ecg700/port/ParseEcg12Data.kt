package com.Carewell.ecg700.port

import com.Carewell.OmniEcg.jni.ConfigBean
import com.Carewell.OmniEcg.jni.PaceClearArr.feed
import com.Carewell.OmniEcg.jni.WaveFilter.Companion.instance
import kotlinx.coroutines.*
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

    private var scope = CoroutineScope(Dispatchers.Default + Job())

    fun start() {
        clearQueue()
        scope.launch {
            val batch = ArrayList<ByteArray>(128)
            while (isActive) {
                try {
                    // runInterruptible 让 take() 在协程取消时可被中断
                    val first = runInterruptible(Dispatchers.IO) { queue.take() }
                    batch.add(first)
                    // 把队列中当前所有可用帧全部取出，一次性处理
                    queue.drainTo(batch)
                    // 批量处理
                    processBatch(batch)
                    batch.clear()
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    LogUtil.e(e.message ?: "")
                    e.printStackTrace()
                    batch.clear()
                }
            }
        }
    }

    fun stop() {
        scope.cancel()
        clearQueue()
        onECGDataListener = null
    }

    private val leadData = ShortArray(8)
    private val ecgData = IntArray(12)
    private val waveFilter = instance

    @Volatile
    private var count = 2
    // 复用数组，避免每帧分配
    private val filterWave = Array(8) { ShortArray(1) }
    private val hrWave = ShortArray(1)
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
            waveFilter?.let { lastHr = it.getRate(hrWave) }
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
        hrWave[0] = if (isLeadII) leadData[1] else leadData[0]

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

        val filtered = waveFilter?.filterControl(configBean, filterWave, leadOffArr) ?: filterWave

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
        private val queue = LinkedBlockingQueue<ByteArray>()

        fun clearQueue() {
            queue.clear()
        }

        fun addData(bytes: ByteArray) {
            queue.put(bytes)
            time++
            if (time % 3000 == 0) {
                time = 0
                LogUtil.v("待处理队列大小:${queue.size}")
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

