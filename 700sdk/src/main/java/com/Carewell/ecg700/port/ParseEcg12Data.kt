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

    /**
     * 批量处理多帧数据，一次性回调给 UI 减少回调开销
     */
    private fun processBatch(batch: List<ByteArray>) {
        // 收集所有有效帧的计算结果
        val batchEcgData = ArrayList<IntArray>(batch.size)
        var lastHr = -1
        var lastLeadStr = ""
        var lastLeadFall = false

        for (frame in batch) {
            val result = processFrame(frame) ?: continue
            batchEcgData.add(result.ecgData.clone())
            // 心率检测需要逐帧喂数据，hrWave 已在 processFrame 中更新
            waveFilter?.let { lastHr = it.getRate(hrWave) }
            lastLeadStr = result.leadStr
            lastLeadFall = result.leadFall
        }

        if (batchEcgData.isEmpty()) return

        // 批量回调：一次性发送所有点
        onECGDataListener?.onECG12BatchDataReceived(batchEcgData)
        // 心率和导联状态只需要回调一次最新值
        onECGDataListener?.onHrReceived(lastHr)
        onECGDataListener?.onLeadFailReceived(lastLeadStr, lastLeadFall)
    }

    private class FrameResult(
        val ecgData: IntArray,
        val leadStr: String,
        val leadFall: Boolean
    )

    private fun processFrame(curByteBuffer: ByteArray): FrameResult? {
        if (curByteBuffer.size < 22) return null
        val frameHead = curByteBuffer[0].toInt() and 0xff
        val frameType = curByteBuffer[1].toInt() and 0xff
        if (frameHead != 0x7f || frameType != TYPE1) return null

        for (i in 0 until 8) {
            val index = 3 + i * 2
            // 内联 toInt，避免每帧创建临时 ByteArray
            val low = curByteBuffer[index].toInt() and 0xFF
            val high = curByteBuffer[index + 1].toInt() and 0xFF
            leadData[i] = ((high shl 8) or low).toShort()
        }

        var leadOff = curByteBuffer[19].toInt() and 0xFF
        var pace = curByteBuffer[20].toInt() and 0xFF

        val arr = feed(leadData, leadOff, pace) ?: return null
        System.arraycopy(arr, 0, leadData, 0, leadData.size)
        leadOff = arr[arr.size - 2].toInt()
        pace = arr[arr.size - 1].toInt()

        val leadNames = checkLeadOff(leadOff)

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

        val filterWaveSize = filtered[0].size
        for (k in 0 until filterWaveSize) {
            ecgData[0] = filtered[0][k].toInt() // I
            ecgData[1] = filtered[1][k].toInt() // II
            ecgData[2] = filtered[1][k] - filtered[0][k] // III
            ecgData[3] = -(filtered[0][k] + filtered[1][k]) shr 1 // AVR
            ecgData[4] = filtered[0][k] - (filtered[1][k].toInt() shr 1) // AVL
            ecgData[5] = filtered[1][k] - (filtered[0][k].toInt() shr 1) // AVF
            ecgData[6] = filtered[2][k].toInt()
            ecgData[7] = filtered[3][k].toInt()
            ecgData[8] = filtered[4][k].toInt()
            ecgData[9] = filtered[5][k].toInt()
            ecgData[10] = filtered[6][k].toInt()
            ecgData[11] = filtered[7][k].toInt()
        }

        val leadStr = leadNames.joinToString(" ")
        return FrameResult(ecgData, leadStr, leadNames.isNotEmpty())
    }

    private var iFall = false
    private var iiFall = false
    private var v1Fall = false
    private var v2Fall = false
    private var v3Fall = false
    private var v4Fall = false
    private var v5Fall = false
    private var v6Fall = false
    private fun checkLeadOff(leadOff: Int): List<String> {
        iFall = (leadOff and 0b00000001) != 0
        iiFall = (leadOff and 0b00000010) != 0
        v1Fall = (leadOff and 0b00000100) != 0
        v2Fall = (leadOff and 0b00001000) != 0
        v3Fall = (leadOff and 0b00010000) != 0
        v4Fall = (leadOff and 0b00100000) != 0
        v5Fall = (leadOff and 0b01000000) != 0
        v6Fall = (leadOff and 0b10000000) != 0

        val leadNames = mutableListOf<String>()
        if (iFall) leadNames.add("LA")
        if (iiFall) leadNames.add("LL")
        if (v1Fall) leadNames.add("V1")
        if (v2Fall) leadNames.add("V2")
        if (v3Fall) leadNames.add("V3")
        if (v4Fall) leadNames.add("V4")
        if (v5Fall) leadNames.add("V5")
        if (v6Fall) leadNames.add("V6")
        if (iFall && iiFall && v1Fall && v2Fall && v3Fall && v4Fall && v5Fall && v6Fall) {
            leadNames.add("RA")
            leadNames.add("RL")
        }
        return leadNames
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

