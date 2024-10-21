package com.lepu.pc700.net.remote

import com.lepu.pc700.net.remote.gson.GsonUtil
import com.lepu.pc700.net.util.Constant
import io.nerdythings.okhttp.profiler.OkHttpProfilerInterceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import okhttp3.Protocol
import org.koin.android.BuildConfig
import java.util.*


fun getOkHttpClient(): OkHttpClient {
    val builder = OkHttpClient().newBuilder()
    val basicParamsInterceptor = BasicParamsInterceptor.Builder()
        .addHeaderParam("secret", Constant.secret)
        .addHeaderParam("access-token", Constant.token)
        .addHeaderParam("language", "zh-CN")
        .build()

    builder.run {
        addInterceptor(basicParamsInterceptor)
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(OkHttpProfilerInterceptor())
        }
        connectTimeout(120, TimeUnit.SECONDS)
        readTimeout(120, TimeUnit.SECONDS)
        writeTimeout(120, TimeUnit.SECONDS)
        retryOnConnectionFailure(true) // 错误重连
        hostnameVerifier(UnSafeHostnameVerifier())
    }

    val trustAllCert = UnSafeTrustManager()
    builder.sslSocketFactory(SSL(trustAllCert), trustAllCert)
        .protocols(Collections.singletonList(Protocol.HTTP_1_1))
    return builder.build()
}

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
inline fun <reified T> createWebService(okHttpClient: OkHttpClient, url: String): T {
    val retrofit = Retrofit.Builder()
        .baseUrl(url)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(GsonUtil.gson))
        .build()
    return retrofit.create(T::class.java)
}

