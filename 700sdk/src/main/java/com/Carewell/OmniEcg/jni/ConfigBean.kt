package com.Carewell.OmniEcg.jni

import java.io.Serializable

class ConfigBean : Serializable {
    //高通滤波频率设置值
    var highPassSmooth = 0f
    //低通滤波频率设置值 0（关闭）75\100\150\300
    var lowPassSmooth = 0
    //肌电滤波频率设置值 0（关闭）25\35\45
    var emgSmooth = 0
    //工频滤波参数设置 浮点型  0.0F、50.0f 60.0f 两种 频率，其他频率均无效;
    var aCSmooth = 0f
    override fun toString(): String {
        return "ConfigBean{" +
                ", HighPassSmooth=" + highPassSmooth +
                ", LowPassSmooth=" + lowPassSmooth +
                ", EmgSmooth=" + emgSmooth +
                ", ACSmooth=" + aCSmooth +
                '}'
    }
}