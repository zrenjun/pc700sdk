package com.lepu.pc700.net.remote.gson

import android.util.Log
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.io.IOException


class LongTypeAdapter : TypeAdapter<Long?>() {
    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: Long?) {
        var temp = value
        try {
            if (value == null) {
                temp = 0L
            }
            out.value(temp)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Throws(IOException::class)
    override fun read(jr: JsonReader): Long {
        try {
            val value: Long
            if (jr.peek() == JsonToken.NULL) {
                jr.nextNull()
                Log.e("TypeAdapter", "null is not a number")
                return 0L
            }
            if (jr.peek() == JsonToken.BOOLEAN) {
                val b = jr.nextBoolean()
                Log.e("TypeAdapter", "$b is not a number")
                return 0L
            }
            return if (jr.peek() == JsonToken.STRING) {
                val str = jr.nextString()
                if (NumberUtils.isIntOrLong(str)) {
                    str.toLong()
                } else {
                    Log.e("TypeAdapter", "$str is not a int number")
                    0L
                }
            } else {
                value = jr.nextLong()
                value
            }
        } catch (e: Exception) {
            Log.e("TypeAdapter", "Not a number", e)
        }
        return 0L
    }
}