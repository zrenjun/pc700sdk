package com.Carewell.OmniEcg.jni

object Const {
    //过滤波数据导联数
    @JvmField
    var FILTER_LEAD_MUN_8 = 8

    //高通
    @JvmField
    var FILTER_HIGH_LOWPASS = 0.0f //高通滤波开
    @JvmField
    var FILTER_BASELINE_001 = 0.01f
    @JvmField
    var FILTER_BASELINE_005 = 0.05f
    @JvmField
    var FILTER_BASELINE_032 = 0.32f
    @JvmField
    var FILTER_BASELINE_067 = 0.67f

    //肌电/低频
    @JvmField
    var FILTER_LOWPASS_CLOSE = 0
    @JvmField
    var FILTER_EMG_25 = 25
    @JvmField
    var FILTER_EMG_35 = 35
    @JvmField
    var FILTER_EMG_45 = 45
    @JvmField
    var FILTER_LOWPASS_75 = 75
    @JvmField
    var FILTER_LOWPASS_100 = 100
    @JvmField
    var FILTER_LOWPASS_150 = 150
    @JvmField
    var FILTER_LOWPASS_300 = 300

    //工频滤波
    @JvmField
    var FILTER_AC_CLOSE = 0.0f
    @JvmField
    var FILTER_AC_50_HZ = 50.0f
}