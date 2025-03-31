package com.Carewell.OmniEcg.jni

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.text.TextUtils
import android.util.Log
import com.Carewell.ecg700.entity.AiResultBean
import com.Carewell.ecg700.entity.MacureResultBean
import com.Carewell.ecg700.entity.PatientInfoBean
import com.Carewell.view.ecg12.EcgReportTemplateRoutine
import com.Carewell.view.ecg12.LeadSpeedType
import com.Carewell.view.ecg12.PdfUtil
import com.creative.sdkpack.R
import java.io.File
import java.text.SimpleDateFormat


/**
 * 心电数据管理
 */
class EcgDataManager private constructor() {
    /**
     * 导出图片 走速最大支持25mm/s
     */
    fun exportBmp(
        context: Context, patientInfoBean: PatientInfoBean, resultBean: MacureResultBean,
        ecgDataArray: Array<ShortArray>, checkTimeStamp: Long,
        lowPassHz: String,
        hpHz: String,
        acHz: String
    ): Bitmap {
        val ecgSrcDataLen = ecgDataArray[0].size
        val needDataLen = 10 * 1000 //截取最后10秒数据
        val beginPos = ecgSrcDataLen - needDataLen
        val ecgDataArrayTemp = Array(ecgDataArray.size) { ShortArray(needDataLen) }
        for (i in ecgDataArrayTemp.indices) {
            System.arraycopy(ecgDataArray[i], beginPos, ecgDataArrayTemp[i], 0, needDataLen)
        }
        return makeReportRoutine(
            context,
            patientInfoBean,
            resultBean,
            checkTimeStamp,
            floatArrayOf(1f, 1f), //增益 10mm/mv
            LeadSpeedType.FORMFEED_12_P_5,
            ecgDataArrayTemp,
            lowPassHz, hpHz, acHz
        )
    }

    /**
     * 导出pdf
     */
    fun exportPdf(
        context: Context, patientInfoBean: PatientInfoBean, resultBean: MacureResultBean,
        ecgDataArray: Array<ShortArray>, checkTimeStamp: Long,
        resultFilePath: String, lowPassHz: String, hpHz: String,
        acHz: String
    ): File? {
        val imageList = exportBmp(
            context,
            patientInfoBean,
            resultBean,
            ecgDataArray,
            checkTimeStamp,
            lowPassHz, hpHz, acHz
        )
        Log.d("pc700", "save pdf path = $resultFilePath")
        PdfUtil.saveBitmapForPdf(imageList, resultFilePath, false)
        val outFile = File(resultFilePath)
        return if (outFile.exists()) outFile else null
    }

    /**
     * 导出pdf
     */
    fun exportPdf(bitmap: Bitmap, outfilePath: String): File? {
        Log.d("pc700", "save pdf path = $outfilePath")
        PdfUtil.saveBitmapForPdf(bitmap, outfilePath, false)
        val outFile = File(outfilePath)
        return if (outFile.exists()) outFile else null
    }

    private fun makeReportRoutine(
        context: Context, patientInfoBean: PatientInfoBean, resultBean: MacureResultBean,
        reportTimeStamp: Long, gainArray: FloatArray, leadSpeedType: LeadSpeedType,
        ecgDataArrayAll: Array<ShortArray>,
        lowPassHz: String,
        hpHz: String,
        acHz: String,
    ): Bitmap {
        val ecgReportTemplateRoutine = EcgReportTemplateRoutine(context, false, true)
        ecgReportTemplateRoutine.drawTitleInfo(context.getString(R.string.print_export_title))
        ecgReportTemplateRoutine.drawPatientInfoTop(patientInfoBean)
        ecgReportTemplateRoutine.drawMacureResult(resultBean)
        ecgReportTemplateRoutine.drawEcgImage(gainArray, leadSpeedType,ecgDataArrayAll, false, null)
        ecgReportTemplateRoutine.drawBottomEcgParamInfo(
            getPrintBottomInfo(context,leadSpeedType, reportTimeStamp, lowPassHz, hpHz, acHz)
        )
        ecgReportTemplateRoutine.drawBottomOtherInfo(context.getString(R.string.print_export_bottom_info))
        return ecgReportTemplateRoutine.bgEcg
    }

    @SuppressLint("SimpleDateFormat")
    private fun getPrintBottomInfo(
        context: Context,
        leadSpeedType: LeadSpeedType,
        checkTimeStamp: Long,
        lowPassHz: String,
        hpHz: String,
        acHz: String
    ): String {
        val space = "\t \t"
        val sb = StringBuilder()
        //走速 增益
        sb.append("${leadSpeedType.value} mm/s").append(space)
        sb.append("10 mm/mV").append(space)
        //滤波
        sb.append(String.format("工频滤波: %s", "$acHz Hz")).append(space)
        sb.append(String.format("高通滤波: %s", "$hpHz Hz")).append(space)
        sb.append(String.format("低通滤波: %s", "$lowPassHz Hz")).append(space)
        //检查时间
        if (checkTimeStamp > 0) {
            sb.append(
                String.format(
                    "%s %s",
                    context.getString(R.string.print_export_bottom_info_checktime),
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(checkTimeStamp)
                )
            ).append(space)
        }
        return sb.toString()
    }

    companion object {
        var instance: EcgDataManager? = null
            get() {
                if (field == null) {
                    field = EcgDataManager()
                }
                return field
            }
            private set

        /**
         * 检查是否是起搏器
         */
        @JvmStatic
        fun checkIfPacemaker(aiResultBean: AiResultBean): Boolean {
            var pacemaker = false
            val aiResultDiagnosisBean = aiResultBean.aiResultDiagnosisBean
            if (aiResultDiagnosisBean != null) {
                val minnesotaCodeItemBeanList = aiResultDiagnosisBean.diagnosis
                if (minnesotaCodeItemBeanList != null && minnesotaCodeItemBeanList.isNotEmpty()) {
                    for (item in minnesotaCodeItemBeanList) {
                        if (!TextUtils.isEmpty(item.code)) {
                            if ("421" == item.code) {
                                pacemaker = true
                                break
                            }
                        }
                    }
                }
            }
            return pacemaker
        }
    }
}
