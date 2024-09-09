package com.lepu.pc700.net.vm

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.Carewell.OmniEcg.jni.XmlUtil
import com.Carewell.OmniEcg.jni.toJson
import com.Carewell.view.ecg12.LeadType
import com.lepu.pc700.dialog.PROJECT_DIR
import com.lepu.pc700.net.bean.EcgInfo
import com.lepu.pc700.net.remote.Repository
import com.lepu.pc700.net.remote.UrlDownload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat


class GetPDFViewModel(private val repository: Repository) : BaseViewModel() {

    val mECGPdf: MutableLiveData<String> = MutableLiveData()

    @SuppressLint("SimpleDateFormat")
    fun getAIPdf(
        context: Context,
        ecgDataArray: Array<ShortArray>,
        ecgInfo: EcgInfo,
        startTime: Long,
        endTime: Long,
    ) {
        launch {
            withContext(Dispatchers.IO) {
                val filePath = "$PROJECT_DIR/test"
                XmlUtil.createDir(filePath)
                val fileName = SimpleDateFormat("yyyyMMddHHmmss").format(startTime)
                //1.生成AI心电分析xml
                XmlUtil.makeHl7Xml(context, ecgDataArray, LeadType.LEAD_12, filePath, fileName, startTime, endTime)
                val result = repository.uploadECG(ecgInfo.toJson(), File("$filePath/$fileName.xml"))
                if (result.code == 0) {
                    getAIReport(result.data?.analysis_id ?: "", filePath)
                } else {
                    mECGPdf.postValue(result.message)
                }
            }
        }
    }


    private var ticker: ReceiveChannel<Unit>? = null

    //间隔5秒获取报告
    @OptIn(ObsoleteCoroutinesApi::class)
    private fun getAIReport(id: String, filePath: String) {
        ticker = ticker(5 * 1000L, 0)
        viewModelScope.launch {
            for (event in ticker!!) {
                //4.通过id获取报告url
                val ecgResult = repository.getECGReport(id)
                if (ecgResult.code == 0) {
                    val url = ecgResult.data?.report_url ?: ""
                    if (url.isNotEmpty()) {
                        val arrStr = url.trim().split("/") //文件名后缀不能包含空格
                        val temp = arrStr[arrStr.size - 1]
                        if (UrlDownload.asyncDownload(url, temp, filePath)) {
                            mECGPdf.postValue("${filePath}/${temp}")
                            ticker?.cancel()
                        }
                    }
                } else {
                    mECGPdf.postValue(ecgResult.message)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        ticker?.cancel()
    }
}




