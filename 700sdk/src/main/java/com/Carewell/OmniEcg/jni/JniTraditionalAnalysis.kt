package com.Carewell.OmniEcg.jni

import android.text.TextUtils
import com.Carewell.ecg700.entity.AiResultBean
import com.Carewell.ecg700.entity.EcgMacureResultEnum
import com.Carewell.ecg700.entity.EcgSettingConfigEnum
import com.Carewell.ecg700.entity.MacureResultBean
import com.Carewell.ecg700.entity.PatientInfoBean
import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.google.gson.GsonBuilder


/**
 *
 *  说明: 本地AI分析
 *  zrj 2022/2/10 9:13
 *
 */
object JniTraditionalAnalysis {

    init {
        System.loadLibrary("ecg_traditional_analysis")
    }

    /**
     * 传统算法分析
     * @param ecgMvDataArray
     * @param dataLen
     * @param aiResultBean
     * @param archivesBean
     * @param ifSaveAnalysisResult 1保存xml 结果
     * @param analysisResultPath 保存结果的path
     * @param leadMode 数据导联模式
     * @param diagnosisMode 诊断模式   //EcgDataAnalysisDiagnosisEnum   MODE_5000("5000 lib",0), MODE_CSE("CSE",1);
     */
    private external fun traditionalAnalysis(
        ecgMvDataArray: Array<FloatArray>,
        dataLen: Int,
        aiResultBean: AiResultBean,
        archivesBean: PatientInfoBean,
        ifSaveAnalysisResult: Int,
        analysisResultPath: String?,
        leadMode: Int,
        diagnosisMode: Int,
        cfgleadoff: Int
    )

    fun traditionalAnalysis(
        resultFilePath: String,
        leadType: EcgSettingConfigEnum.LeadType,
        patientInfoBean: PatientInfoBean,
        ecgDataArrayPart: Array<ShortArray>
    ): MacureResultBean {
        //这三个字段不能为null,否则分析失败
        if (TextUtils.isEmpty(patientInfoBean.archivesName)) {
            patientInfoBean.archivesName = "w"
        }
        if (TextUtils.isEmpty(patientInfoBean.sex)) {
            patientInfoBean.sex = "0"
        }
        if (TextUtils.isEmpty(patientInfoBean.age)) {
            patientInfoBean.age = "20"
        }
        if (TextUtils.isEmpty(patientInfoBean.birthdate)) {
            patientInfoBean.birthdate = "1990-09-08"
        }
        var aiResultBean = AiResultBean()
        traditionalAnalysis(
            switchEcgDataArray(ecgDataArrayPart),
            ecgDataArrayPart[0].size,
            aiResultBean,
            patientInfoBean,
            0, //传统算法分析结果xml是否保存.1 保存；0 不保存
            resultFilePath,
            leadType.value,
            0,
            patientInfoBean.leadoffstate
        )
        val sb = StringBuilder()
        sb.append("JSON:")
        sb.append(aiResultBean.arrDiagnosis)
        aiResultBean.arrDiagnosis = sb.toString()
        aiResultBean = AiResultBean.manualSetValue(aiResultBean)
        val macureResultBean = MacureResultBean()
        macureResultBean.ecgMacureResultEnum = EcgMacureResultEnum.TYPE_LOCAL_TRADITIONAL_RESULT
        macureResultBean.aiResultBean = aiResultBean
        return macureResultBean
    }

    const val SHORT_MV_GAIN = 0.0009536f //mv
    private fun switchEcgDataArray(dataArray: Array<ShortArray>): Array<FloatArray> {
        val valueDataArray = Array(dataArray.size) { FloatArray(dataArray[0].size) }
        for (i in dataArray.indices) {
            for (j in dataArray[0].indices) {
                val value = dataArray[i][j]
                valueDataArray[i][j] = value * SHORT_MV_GAIN
            }
        }
        return valueDataArray
    }
}

fun Any.toJson(
    dateFormat: String = "yyyy-MM-dd HH:mm:ss",
    lenient: Boolean = false,
    excludeFields: List<String>? = null
) = GsonBuilder().setDateFormat(dateFormat)
    .apply {
        if (lenient) setLenient()
        if (!excludeFields.isNullOrEmpty()) {
            setExclusionStrategies(object : ExclusionStrategy {
                override fun shouldSkipField(f: FieldAttributes?): Boolean {
                    return f != null && excludeFields.contains(f.name)
                }

                override fun shouldSkipClass(clazz: Class<*>?) = false
            })
        }
    }
    .create().toJson(this)


