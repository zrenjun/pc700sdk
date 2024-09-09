package com.lepu.pc700.net.remote

import com.lepu.pc700.net.bean.HttpResult


/**
 * 统一数据获取
 */
open class BaseRepository {
    suspend fun <T : Any> apiCall(call: suspend () -> HttpResult<T>): HttpResult<T> {
        return call.invoke()
    }
}