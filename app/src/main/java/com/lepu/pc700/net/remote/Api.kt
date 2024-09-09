package com.lepu.pc700.net.remote

import com.lepu.pc700.net.bean.AnalysisId
import com.lepu.pc700.net.bean.EcgResult
import com.lepu.pc700.net.bean.HttpResult
import okhttp3.RequestBody
import retrofit2.http.*

/**
 *
 */
interface Api {
    /**
     * 上传12导数据
     */
    @POST("/api/v1/resting_ecg/analysis/request")
    suspend fun uploadECG(@Body body: RequestBody): HttpResult<AnalysisId>

    /**
     * 获取12导联心电报告
     */
    @POST("/api/v1/ecg/analysis/result/query")
    suspend fun getECGReport(@Body params: Map<String, @JvmSuppressWildcards Any?>): HttpResult<EcgResult>
}