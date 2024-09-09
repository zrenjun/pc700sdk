package com.lepu.pc700.net.remote.gson

import android.util.Log
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.io.IOException


class DoubleTypeAdapter : TypeAdapter<Double?>() {
    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: Double?) {
        var temp = value
        try {
            if (temp == null) {
                temp = 0.0
            }
            out.value(temp)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Throws(IOException::class)
    override fun read(jr: JsonReader): Double {
        try {
            if (jr.peek() == JsonToken.NULL) {
                jr.nextNull()
                Log.e("TypeAdapter", "null is not a number")
                return 0.0
            }
            if (jr.peek() == JsonToken.BOOLEAN) {
                val b = jr.nextBoolean()
                Log.e("TypeAdapter", "$b is not a number")
                return 0.0
            }
            return if (jr.peek() == JsonToken.STRING) {
                val str = jr.nextString()
                if (NumberUtils.isFloatOrDouble(str)) {
                    str.toDouble()
                } else {
                    Log.e("TypeAdapter", "$str is not a number")
                    0.0
                }
            } else {
                jr.nextDouble()
            }
        } catch (e: NumberFormatException) {
            Log.e("TypeAdapter", e.message, e)
        } catch (e: Exception) {
            Log.e("TypeAdapter", e.message, e)
        }
        return 0.0
    }
}