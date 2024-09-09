package com.lepu.pc700.net.remote

import com.lepu.pc700.net.bean.AnalysisId
import com.lepu.pc700.net.bean.HttpResult
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.TreeMap


class Repository(private val api: Api) : BaseRepository() {

    suspend fun uploadECG(ecgInfo: String, file: File): HttpResult<AnalysisId> {
        return apiCall {
            api.uploadECG(
                MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart(
                    "ecg_file",
                    file.name,
                    file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
                ).addFormDataPart("ecg_info", ecgInfo).build()
            )
        }
    }

    suspend fun getECGReport(id: String) = apiCall {
        api.getECGReport(TreeMap<String, Any?>().apply { this["analysis_id"] = id })
    }
}
