package com.Carewell.OmniEcg.jni

enum class LeadSignalEnum(//基线漂移4
     val leadName: String, val value: Int
) {
    NOISE_NORMAL("无噪声", 0),  //无噪声0
    NOISE_50HZ60HZ("工频噪声", 1),  //工频噪声1
    NOISE_EMG("工频噪声", 2),  //肌电噪声2
    NOISE_BASELINE("基线漂移", 4);

    companion object {
        fun getLeadSignalEnumByValue(value: Any): LeadSignalEnum {
            var type = NOISE_NORMAL
            for (item in values()) {
                if (item.value == value) {
                    type = item
                    break
                }
            }
            return type
        }
    }
}