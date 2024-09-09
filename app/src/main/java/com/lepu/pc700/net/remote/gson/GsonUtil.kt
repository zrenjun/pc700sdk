@file:Suppress("unused")

package com.lepu.pc700.net.remote.gson

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken


object GsonUtil {
    var gson: Gson? = null
        private set

    /**
     * 转成json
     */
    fun bean2String(`object`: Any?): String? {
        return  gson?.toJson(`object`)
    }

    /**
     * 转成bean
     */
    fun <T> gson2Bean(gsonString: String?, cls: Class<T>?): T? {
        return gson?.fromJson(gsonString, cls)
    }

    /**
     * 转成list
     */
    fun <T> gson2List(gsonString: String?): List<T>? {
        return gson?.fromJson(gsonString, object : TypeToken<List<T>?>() {}.type)
    }

    /**
     * 转成list中有map的
     */
    fun <T> gson2ListMaps(gsonString: String?): List<Map<String, T>>? {
        return gson?.fromJson(
            gsonString,
            object : TypeToken<List<Map<String?, T>?>?>() {}.type
        )
    }

    /**
     * 转成map的
     */
    fun <T> gson2Maps(gsonString: String?): Map<String, T>? {
        return  gson?.fromJson(gsonString, object : TypeToken<Map<String?, T>?>() {}.type)
    }

    init {
        if (gson == null) {
            gson = GsonBuilder()
                .registerTypeAdapter(Int::class.java, IntegerTypeAdapter())
                .registerTypeAdapter(Int::class.javaPrimitiveType, IntegerTypeAdapter())
                .registerTypeAdapter(Double::class.java, DoubleTypeAdapter())
                .registerTypeAdapter(Double::class.javaPrimitiveType, DoubleTypeAdapter())
                .registerTypeAdapter(Long::class.java, LongTypeAdapter())
                .registerTypeAdapter(Long::class.javaPrimitiveType, LongTypeAdapter())
                .registerTypeAdapter(Float::class.java, FloatTypeAdapter())
                .registerTypeAdapter(Float::class.javaPrimitiveType, FloatTypeAdapter())
                .registerTypeAdapter(Boolean::class.java, BooleanTypeAdapter())
                .registerTypeAdapter(Boolean::class.javaPrimitiveType, BooleanTypeAdapter())
                .registerTypeAdapter(String::class.java, StringTypeAdapter())
                .create()
        }
    }
}