package com.Carewell.ecg700.port

import com.Carewell.OmniEcg.jni.ConfigBean
import com.Carewell.OmniEcg.jni.JniFilterNew
import com.Carewell.OmniEcg.jni.PaceClearArr.feed
import com.Carewell.OmniEcg.jni.WaveFilter.Companion.instance
import kotlinx.coroutines.*

/**
 * 说明: 12导解析
 * zrj 2022/4/7 15:09
 */
class ParseEcg12Data {

    private var onECGDataListener: OnECG12DataListener? = null

    fun setOnECGDataListener(onECGDataListener: OnECG12DataListener?) {
        this.onECGDataListener = onECGDataListener
    }

    private var scope = CoroutineScope(Dispatchers.IO)

    fun start() {
        queue.clear()
        scope.launch {
            while (this.isActive) {
                try {
                    queue.dequeue()?.let { checkPack(it) }
                } catch (e: Exception) {
                    LogUtil.e(e.message?:"")
                    e.printStackTrace()
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

    private val leadData = ShortArray(8)
    private val ecgData = IntArray(12)
    private val waveFilter = instance

    @Volatile
    private var count = 2


    private fun checkPack(curByteBuffer: ByteArray) {
        if (curByteBuffer.size < 22) return
        val frameHead = curByteBuffer[0].toInt() and 0xff
        val frameType = curByteBuffer[1].toInt() and 0xff
        if (frameHead == 0x7f) {
            val crc = curByteBuffer[21]
            if (checkSum(crc, curByteBuffer)) {
                when (frameType) {
                    TYPE1 -> {// TYPE1 = 0x81  12导联数据帧
                        leadData[0] = toInt(curByteBuffer.copyOfRange(3, 5)).toShort()
                        leadData[1] = toInt(curByteBuffer.copyOfRange(5, 7)).toShort()
                        leadData[2] = toInt(curByteBuffer.copyOfRange(7, 9)).toShort()
                        leadData[3] = toInt(curByteBuffer.copyOfRange(9, 11)).toShort()
                        leadData[4] = toInt(curByteBuffer.copyOfRange(11, 13)).toShort()
                        leadData[5] = toInt(curByteBuffer.copyOfRange(13, 15)).toShort()
                        leadData[6] = toInt(curByteBuffer.copyOfRange(15, 17)).toShort()
                        leadData[7] = toInt(curByteBuffer.copyOfRange(17, 19)).toShort()
                        var leadOff = curByteBuffer[19].toInt() and 0xff //导联脱落
                        var pace = curByteBuffer[20].toInt() and 0xff //起搏标识
                        val arr = feed(leadData, leadOff, pace) ?: return
                        System.arraycopy(arr, 0, leadData, 0, leadData.size)
                        leadOff = arr[arr.size - 2].toInt()
                        pace = arr[arr.size - 1].toInt()
                        //导联脱落检测,起博信号
                        checkLeadOff(leadOff)
                        var filterWave = Array(8) { ShortArray(1) }

                        val hrWave = ShortArray(1)
                        if (!isLeadII) { //采集I导联数据心率
                            hrWave[0] = leadData[0]
                        } else { //采集II导联心率数据
                            hrWave[0] = leadData[1]
                        }
                        var j = 0
                        while (j < 8) {
                            filterWave[j][0] = leadData[j]
                            j++
                        }
                        val temp = booleanArrayOf(
                            iFall,
                            iiFall,
                            v1Fall,
                            v2Fall,
                            v3Fall,
                            v4Fall,
                            v5Fall,
                            v6Fall
                        )
                        val leadOffArr = IntArray(temp.size)
                        var i = 0
                        while (i < temp.size) {
                            if (temp[i]) {
                                leadOffArr[i] = 1
                            } else {
                                leadOffArr[i] = 0
                            }
                            i++
                        }
                        waveFilter?.let {
                            filterWave = it.filterControl(configBean, filterWave, leadOffArr)
                            onECGDataListener?.onHrReceived(it.getRate(hrWave))
                        }
                        if (pace == 1 && count == 0) {
                            count = 2
                        }
                        if (isAddPacemaker && count > 0) {
                            if (!iFall) {
                                filterWave[0][0] = PACE_MAKER_VALUE
                            }
                            if (!iiFall) {
                                filterWave[1][0] = PACE_MAKER_VALUE
                            }
                            if (!v1Fall) {
                                filterWave[2][0] = PACE_MAKER_VALUE
                            }
                            if (!v2Fall) {
                                filterWave[3][0] = PACE_MAKER_VALUE
                            }
                            if (!v3Fall) {
                                filterWave[4][0] = PACE_MAKER_VALUE
                            }
                            if (!v4Fall) {
                                filterWave[5][0] = PACE_MAKER_VALUE
                            }
                            if (!v5Fall) {
                                filterWave[6][0] = PACE_MAKER_VALUE
                            }
                            if (!v6Fall) {
                                filterWave[7][0] = PACE_MAKER_VALUE
                            }
                            count--
                        }
                        var k = 0
                        while (k < filterWave[0].size) {
                            ecgData[0] = filterWave[0][k].toInt() //I
                            ecgData[1] = filterWave[1][k].toInt() //II
                            ecgData[2] = filterWave[1][k] - filterWave[0][k] //III
                            ecgData[3] = -(filterWave[0][k] + filterWave[1][k]) shr 1 //AVR
                            ecgData[4] = filterWave[0][k] - (filterWave[1][k].toInt() shr 1) //AVL
                            ecgData[5] = filterWave[1][k] - (filterWave[0][k].toInt() shr 1) //AVF
                            ecgData[6] = filterWave[2][k].toInt()
                            ecgData[7] = filterWave[3][k].toInt() //
                            ecgData[8] = filterWave[4][k].toInt() //
                            ecgData[9] = filterWave[5][k].toInt() //
                            ecgData[10] = filterWave[6][k].toInt() //
                            ecgData[11] = filterWave[7][k].toInt() //
                            k++
                        }
                        onECGDataListener?.onECG12DataReceived(ecgData)
                    }

                    TYPE2 -> {
                        //12导联回复帧,帧的总长度22byte
                        System.arraycopy(curByteBuffer, 3, replyData, 0, 18)
                        val version = ByteArray(8)
                        System.arraycopy(replyData, 6, version, 0, 8)
                        val versionStr = String(version)
                        LogUtil.v("回复帧版 本号:$versionStr")
                        //1 old version;0 new version
                        var versionFlag = 0
                        if ("V1.0.0.0" == versionStr) versionFlag = 1
                        //解决基线跳变问题必须在开始滤波器之前
                        JniFilterNew.getInstance().InitDCRecover(versionFlag)
                    }
                }
            }else{
                LogUtil.v("数据校验失败")
            }
        }
    }

    private val stringBuffer = StringBuffer()
    private var iFall = false
    private var iiFall = false
    private var v1Fall = false
    private var v2Fall = false
    private var v3Fall = false
    private var v4Fall = false
    private var v5Fall = false
    private var v6Fall = false
    private val leadOffData = ShortArray(8)
    private val replyData = ByteArray(18)
    private fun checkLeadOff(leadOff: Int) {
        var bFall = false
        leadOffData[0] = (leadOff and 1).toShort()
        val strLA: String
        if (leadOffData[0].toInt() == 1) {
            strLA = "LA "
            bFall = true
            iFall = true
        } else {
            strLA = ""
            iFall = false
        }
        leadOffData[1] = (leadOff ushr 1 and 1).toShort()
        val strLL: String
        if (leadOffData[1].toInt() == 1) {
            strLL = "LL "
            bFall = true
            iiFall = true
        } else {
            strLL = ""
            iiFall = false
        }
        leadOffData[2] = (leadOff ushr 2 and 1).toShort()
        val strV1: String
        if (leadOffData[2].toInt() == 1) {
            strV1 = "V1 "
            bFall = true
            v1Fall = true
        } else {
            strV1 = ""
            v1Fall = false
        }
        leadOffData[3] = (leadOff ushr 3 and 1).toShort()
        val strV2: String
        if (leadOffData[3].toInt() == 1) {
            strV2 = "V2 "
            bFall = true
            v2Fall = true
        } else {
            strV2 = ""
            v2Fall = false
        }
        leadOffData[4] = (leadOff ushr 4 and 1).toShort()
        val strV3: String
        if (leadOffData[4].toInt() == 1) {
            strV3 = "V3 "
            bFall = true
            v3Fall = true
        } else {
            strV3 = ""
            v3Fall = false
        }
        leadOffData[5] = (leadOff ushr 5 and 1).toShort()
        val strV4: String
        if (leadOffData[5].toInt() == 1) {
            strV4 = "V4 "
            bFall = true
            v4Fall = true
        } else {
            strV4 = ""
            v4Fall = false
        }
        leadOffData[6] = (leadOff ushr 6 and 1).toShort()
        val strV5: String
        if (leadOffData[6].toInt() == 1) {
            strV5 = "V5 "
            bFall = true
            v5Fall = true
        } else {
            strV5 = ""
            v5Fall = false
        }
        leadOffData[7] = (leadOff ushr 7 and 1).toShort()
        val strV6: String
        if (leadOffData[7].toInt() == 1) {
            strV6 = "V6 "
            bFall = true
            v6Fall = true
        } else {
            strV6 = ""
            v6Fall = false
        }
        val strRA: String
        val strRL: String
        if (iFall && iiFall && v1Fall && v2Fall && v3Fall && v4Fall && v5Fall && v6Fall) {
            strRA = "RA "
            strRL = "RL "
        } else {
            strRA = ""
            strRL = ""
        }
        stringBuffer.delete(0, stringBuffer.length)
        stringBuffer.append(strLA).append(strLL).append(strRA).append(strRL).append(strV1)
            .append(strV2).append(strV3).append(strV4).append(strV5).append(strV6)
        onECGDataListener?.onLeadFailReceived(stringBuffer.toString(), bFall)
    }

    companion object {
        private var queue = ArrayQueue<ByteArray>(20000)


        fun addData(bytes: ByteArray) {
            queue.enqueue(bytes)
            if (queue.getSize() > 1000) {
                LogUtil.v("receive  ---->  " + HexUtil.bytesToHexString(bytes))
            }
        }

        private const val TYPE1 = 0x81 //12导联数据帧
        private const val TYPE2 = 0xc2 //回复帧
        private const val PACE_MAKER_VALUE: Short = 1000
        private var isLeadII = true
        fun setLeadHrMode(leadII: Boolean) {
            isLeadII = leadII
        }

        private val configBean = ConfigBean()
        fun setFilterParam(highPassSmooth: Float, lowPassSmooth: Int, emgSmooth: Int, acSmooth: Float) {
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

interface Queue<E> {
    fun enqueue(e: E)   //复杂度 O(1)

    /** 移除队首元素 */
    fun dequeue(): E?   //复杂度 O(n)

    /** 获取队首元素 */
    fun getFront(): E?   //复杂度 O(1)

    /** 获取队列大小 */
    fun getSize(): Int  //复杂度 O(1)

    /** 判断队列是否为null */
    fun isEmpty(): Boolean  //复杂度 O(1)

    fun clear()
}

class ArrayQueue<E>(private val initialCapacity: Int) : Queue<E> {

    private var array = ArrayList<E>(initialCapacity)

    override fun enqueue(e: E) {
        array.add(e)
    }

    override fun dequeue(): E? {
        if (isEmpty()) return null
        return array.removeFirst()
    }

    override fun getFront(): E? {
        if (isEmpty()) return null
        return array.first()
    }

    override fun getSize(): Int {
        return array.size
    }

    override fun isEmpty(): Boolean {
        return array.isEmpty()
    }

    override fun clear() {
        array.clear()
    }

    override fun toString(): String {
        val res = StringBuilder()
        res.append("Queue：")
        res.append("front [")
        if (array.isNotEmpty()) {
            array.forEach {
                res.append(it)
                res.append(",")
            }
            res.deleteCharAt(res.length - 1)
        }
        res.append("] tail")
        return res.toString()
    }

    fun getCapacity(): Int {
        return initialCapacity
    }
}


