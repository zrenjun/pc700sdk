package com.lepu.pc700.net.remote.gson

import android.util.Log
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.io.IOException

class FloatTypeAdapter : TypeAdapter<Float?>() {
    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: Float?) {
        var temp = value
        try {
            if (temp == null) {
                temp = 0f
            }
            out.value(temp.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Throws(IOException::class)
    override fun read(jr: JsonReader): Float? {
        try {
            if (jr.peek() == JsonToken.NULL) {
                jr.nextNull()
                Log.e("TypeAdapter", "null is not a number")
                return 0f
            }
            if (jr.peek() == JsonToken.BOOLEAN) {
                val b = jr.nextBoolean()
                Log.e("TypeAdapter", "$b is not a number")
                return 0f
            }
            return if (jr.peek() == JsonToken.STRING) {
                val str = jr.nextString()
                return if (NumberUtils.isFloatOrDouble(str)) {
                    str.toFloat()
                } else {
                    Log.e("TypeAdapter", "$str is not a number")
                    0f
                }
            } else {
                val str = jr.nextString()
                java.lang.Float.valueOf(str)
            }
        } catch (e: Exception) {
            Log.e("TypeAdapter", "Not a number", e)
        }
        return 0f
    }
}