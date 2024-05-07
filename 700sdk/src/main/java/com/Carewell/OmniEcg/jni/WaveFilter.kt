@file:Suppress("unused")

package com.Carewell.OmniEcg.jni

import com.Carewell.ecg700.LogUtil


/**
 * 滤波控制器
 */
class WaveFilter {
    private val jniHeartRateDetect: JniHeartRateDetect = JniHeartRateDetect.getInstance()

    val HEART_RATE_DETECT_VALUE = 200 //心率检测阀值
    val SAMPLE_RATE = 1000 //采样率

    @Volatile
    private var isHeartRate: Boolean = true

    //	滤波检测
    fun filterControl(
        configBean: ConfigBean?,
        ecgDataArray: Array<ShortArray>,
        leadOffArr: IntArray?
    ): Array<ShortArray> {
        val notifyFilterBean = NotifyFilterBean()
        val inputDataCount = ecgDataArray[0].size
        val jniFilter = JniFilterNew.getInstance()
        notifyFilterBean.outDataLen = inputDataCount
        jniFilter.DCRecover(
            ecgDataArray,
            ecgDataArray[0].size,
            notifyFilterBean,
            Const.FILTER_LEAD_MUN_8,
            leadOffArr
        )
        //DC后数值位数变化，需要用int接收
        var tmpDataArray = notifyFilterBean.intDataArray
        if (configBean == null) {
            return ecgDataArray
        }
        //漂移 高通
        if (configBean.highPassSmooth != Const.FILTER_HIGH_LOWPASS) {
            val smoothH = configBean.highPassSmooth
            when {
                Const.FILTER_BASELINE_001 == smoothH -> {
                    //0.01
                    jniFilter.HP0p01(
                        tmpDataArray,
                        tmpDataArray[0].size,
                        notifyFilterBean,
                        Const.FILTER_LEAD_MUN_8
                    )
                    tmpDataArray = notifyFilterBean.intDataArray
                }

                Const.FILTER_BASELINE_005 == smoothH -> {
                    //0.05
                    jniFilter.HP0p05(
                        tmpDataArray,
                        tmpDataArray[0].size,
                        notifyFilterBean,
                        Const.FILTER_LEAD_MUN_8
                    )
                    tmpDataArray = notifyFilterBean.intDataArray
                }

                Const.FILTER_BASELINE_032 == smoothH -> {
                    //0.32
                    jniFilter.HP0p32(
                        tmpDataArray,
                        tmpDataArray[0].size,
                        notifyFilterBean,
                        Const.FILTER_LEAD_MUN_8
                    )
                    tmpDataArray = notifyFilterBean.intDataArray
                }

                Const.FILTER_BASELINE_067 == smoothH -> {
                    //0.67
                    jniFilter.HP0p67(
                        tmpDataArray,
                        tmpDataArray[0].size,
                        notifyFilterBean,
                        Const.FILTER_LEAD_MUN_8
                    )
                    tmpDataArray = notifyFilterBean.intDataArray
                }
            }
        }
        //肌电 、低通
        val filterLowPass = configBean.lowPassSmooth
        if (filterLowPass != Const.FILTER_LOWPASS_CLOSE) {
            when (filterLowPass) {
                Const.FILTER_EMG_25 -> {
                    jniFilter.electromyography25(
                        tmpDataArray,
                        tmpDataArray[0].size,
                        notifyFilterBean,
                        Const.FILTER_LEAD_MUN_8
                    )
                    tmpDataArray = notifyFilterBean.intDataArray
                }

                Const.FILTER_EMG_35 -> {
                    jniFilter.electromyography35(
                        tmpDataArray,
                        tmpDataArray[0].size,
                        notifyFilterBean,
                        Const.FILTER_LEAD_MUN_8
                    )
                    tmpDataArray = notifyFilterBean.intDataArray
                }

                Const.FILTER_EMG_45 -> {
                    jniFilter.electromyography45(
                        tmpDataArray,
                        tmpDataArray[0].size,
                        notifyFilterBean,
                        Const.FILTER_LEAD_MUN_8
                    )
                    tmpDataArray = notifyFilterBean.intDataArray
                }

                Const.FILTER_LOWPASS_75 -> { //低通滤波
                    jniFilter.lowPass75(
                        tmpDataArray,
                        tmpDataArray[0].size,
                        notifyFilterBean,
                        Const.FILTER_LEAD_MUN_8
                    )
                    tmpDataArray = notifyFilterBean.intDataArray
                }

                Const.FILTER_LOWPASS_100 -> {
                    jniFilter.lowPass100(
                        tmpDataArray,
                        tmpDataArray[0].size,
                        notifyFilterBean,
                        Const.FILTER_LEAD_MUN_8
                    )
                    tmpDataArray = notifyFilterBean.intDataArray
                }

                Const.FILTER_LOWPASS_150 -> {
                    jniFilter.lowPass150(
                        tmpDataArray,
                        tmpDataArray[0].size,
                        notifyFilterBean,
                        Const.FILTER_LEAD_MUN_8
                    )
                    tmpDataArray = notifyFilterBean.intDataArray
                }

                Const.FILTER_LOWPASS_300 -> {
                    jniFilter.lowPass300(
                        tmpDataArray,
                        tmpDataArray[0].size,
                        notifyFilterBean,
                        Const.FILTER_LEAD_MUN_8
                    )
                    tmpDataArray = notifyFilterBean.intDataArray
                }
            }
        }
        //工频滤波
        val filterAc = configBean.aCSmooth
        if (filterAc != Const.FILTER_AC_CLOSE) {
            //工频滤波
            if (filterAc == Const.FILTER_AC_50_HZ) {
                //50hz
                jniFilter.powerFrequency50(
                    tmpDataArray,
                    tmpDataArray[0].size,
                    notifyFilterBean,
                    Const.FILTER_LEAD_MUN_8
                )
            } else {
                //60hz
                jniFilter.powerFrequency60(
                    tmpDataArray,
                    tmpDataArray[0].size,
                    notifyFilterBean,
                    Const.FILTER_LEAD_MUN_8
                )
            }
            tmpDataArray = notifyFilterBean.intDataArray
        }
        //重新转成short类型丢出
        val filterDataArray = Array(8) { ShortArray(1) }
        //这里每次只过滤一个导联点的数据，这里只接一个循环就可以
        //丢入滤波数据变化这里需要修改
        for (i in 0..7) {
            filterDataArray[i][0] = tmpDataArray[i][0].toShort()
        }
        return filterDataArray
    }

    /**
     * 干扰信号检测
     */
    fun checkNoiseDetect(ecgDataArray: Array<ShortArray>): LeadSignalEnum {
        val noiseStateNormal = LeadSignalEnum.NOISE_NORMAL.value as Int
        var noiseStateValue = 0
        var value: Short
        val noiseResetFlag = '0'
        //-2 是由于最后是导联脱落通道，起搏通道。最后2个通道不用添加
        for (i in 0 until ecgDataArray.size - 2) {
            for (element in ecgDataArray[i]) {
                value = element
                noiseStateValue =
                    JniNoiseDetect.getInstance().noiseDetection(value, i.toShort(), noiseResetFlag)
                if (noiseStateValue != noiseStateNormal) {
                    break
                }
            }
        }
        return LeadSignalEnum.getLeadSignalEnumByValue(noiseStateValue)
    }

    fun initHeartRateDetect() {
        isHeartRate = true
        jniHeartRateDetect.initHeartRateDetect(SAMPLE_RATE)
    }

    /**
     * 计算心率
     */
    fun getRate(ecgDataArray: ShortArray): Int {
        return if (isHeartRate) {
            jniHeartRateDetect.getDataHeartRate(ecgDataArray)
        } else {
            -1
        }
    }

    fun closeHeartRateDetect() {
        isHeartRate = false
        jniHeartRateDetect.closeHeartRateDetect()
    }


    companion object {
        private var waveFilter: WaveFilter? = null
        val instance: WaveFilter?
            get() {
                if (waveFilter == null) waveFilter = WaveFilter()
                return waveFilter
            }
    }

    init {
        LogUtil.json("===========jniHeartRateDetect.initHeartRateDetect(SAMPLE_RATE)")
        jniHeartRateDetect.initHeartRateDetect(SAMPLE_RATE)
    }
}