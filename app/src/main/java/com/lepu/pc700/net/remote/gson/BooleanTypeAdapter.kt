package com.lepu.pc700.net.remote.gson

import android.util.Log
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.io.IOException


class BooleanTypeAdapter : TypeAdapter<Boolean?>() {
    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: Boolean?) {
        var temp = value
        try {
            if (temp == null) {
                temp = false
            }
            out.value(temp)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Throws(IOException::class)
    override fun read(jr: JsonReader): Boolean? {
        try {
            if (jr.peek() == JsonToken.NULL) {
                jr.nextNull()
                Log.e("TypeAdapter", "null is not a boolean")
                return false
            }
            if (jr.peek() == JsonToken.NUMBER) {
                val value = jr.nextDouble()
                Log.e("TypeAdapter", "$value is not a boolean")
                return value == java.lang.Double.valueOf(1.0) //boolean 有时候服务器给的是1，表示true，0可能就是false
            }
            if (jr.peek() == JsonToken.STRING) {
                val str = jr.nextString()
                if (NumberUtils.isFloatOrDouble(str)) {
                    return java.lang.Double.valueOf(str) == java.lang.Double.valueOf(1.0)
                } else {
                    Log.e("TypeAdapter", "$str is not a number")
                }
            }
        } catch (e: NumberFormatException) {
            Log.e("TypeAdapter", e.message, e)
        } catch (e: Exception) {
            Log.e("TypeAdapter", e.message, e)
        }
        return false
    }
}