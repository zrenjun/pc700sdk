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

    private var scope = CoroutineScope(Dispatchers.IO + Job())

    fun start() {
        queue.clear()
        scope.launch {
            while (this.isActive) {
                try {
                    queue.take()?.let { checkPack(it) }
                } catch (e: Exception) {
                    LogUtil.e(e.message ?: "")
                    e.printStackTrace()
                }
            }
        }
    }

    fun stop() {
        scope.cancel()
    }

    private val leadData = ShortArray(8)
    private val ecgData = IntArray(12)
    private val waveFilter = instance

    @Volatile
    private var count = 2

    private fun checkPack(curByteBuffer: ByteArray) {
        if (curByteBuffer.size < 22) return
        val frameHead = curByteBuffer[0].toInt() and 0xff
        val frameType = curByteBuffer[1].toInt() and 0xff
        if (frameHead == 0x7f && frameType == TYPE1) {
            for (i in 0 until 8) {
                val index = 3 + i * 2
                leadData[i] =
                    toInt(byteArrayOf(curByteBuffer[index], curByteBuffer[index + 1])).toShort()
            }

            var leadOff = curByteBuffer[19].toInt() and 0xFF
            var pace = curByteBuffer[20].toInt() and 0xFF

            val arr = feed(leadData, leadOff, pace) ?: return
            System.arraycopy(arr, 0, leadData, 0, leadData.size)
            leadOff = arr[arr.size - 2].toInt()
            pace = arr[arr.size - 1].toInt()

            val leadNames = checkLeadOff(leadOff)

            var filterWave = Array(8) { ShortArray(1) }
            var j = 0
            for (i in 0 until 8) {
                filterWave[j][0] = leadData[i]
                j++
            }
            val hrWave = ShortArray(1) { if (isLeadII) leadData[1] else leadData[0] }

            // 提前定义布尔数组，避免多次访问成员变量
            val fallFlags =
                booleanArrayOf(iFall, iiFall, v1Fall, v2Fall, v3Fall, v4Fall, v5Fall, v6Fall)
            // 直接初始化 Int 数组，避免 map 操作
            val leadOffArr = IntArray(8) { if (fallFlags[it]) 1 else 0 }

            waveFilter?.let {
                filterWave = it.filterControl(configBean, filterWave, leadOffArr)
            }

            if (pace == 1 && count == 0) {
                count = 2
            }

            if (isAddPacemaker && count > 0) {
                for (i in 0 until 8) {
                    filterWave[i][0] = if (!fallFlags[i]) PACE_MAKER_VALUE else filterWave[i][0]
                }
                count--
            }

            val filterWaveSize = filterWave[0].size
            for (k in 0 until filterWaveSize) {
                ecgData[0] = filterWave[0][k].toInt() // I
                ecgData[1] = filterWave[1][k].toInt() // II
                ecgData[2] = filterWave[1][k] - filterWave[0][k] // III
                ecgData[3] = -(filterWave[0][k] + filterWave[1][k]) shr 1 // AVR
                ecgData[4] = filterWave[0][k] - (filterWave[1][k].toInt() shr 1) // AVL
                ecgData[5] = filterWave[1][k] - (filterWave[0][k].toInt() shr 1) // AVF
                ecgData[6] = filterWave[2][k].toInt()
                ecgData[7] = filterWave[3][k].toInt()
                ecgData[8] = filterWave[4][k].toInt()
                ecgData[9] = filterWave[5][k].toInt()
                ecgData[10] = filterWave[6][k].toInt()
                ecgData[11] = filterWave[7][k].toInt()
            }
            //不可切换线程
            onECGDataListener?.onECG12DataReceived(ecgData)
            waveFilter?.let { onECGDataListener?.onHrReceived(it.getRate(hrWave)) }
            val leadStr = leadNames.joinToString(" ")
            onECGDataListener?.onLeadFailReceived(leadStr, leadNames.isNotEmpty())
        }
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
        fun addData(bytes: ByteArray) {
            time++
            if (time % 10000 == 0) {
                time = 0
                LogUtil.v("待处理队列大小:${queue.size}")
            }
            queue.put(bytes)
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

