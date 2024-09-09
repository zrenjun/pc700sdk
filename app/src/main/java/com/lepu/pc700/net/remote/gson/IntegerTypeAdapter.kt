package com.lepu.pc700.net.remote.gson

import android.util.Log
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.io.IOException


class IntegerTypeAdapter : TypeAdapter<Int?>() {
    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: Int?) {
        var temp = value
        try {
            if (temp == null) {
                temp = 0
            }
            out.value(temp)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Throws(IOException::class)
    override fun read(jr: JsonReader): Int {
        try {
            val value: Int
            if (jr.peek() == JsonToken.NULL) {
                jr.nextNull()
                Log.e("TypeAdapter","null is not a number")
                return 0
            }
            if (jr.peek() == JsonToken.BOOLEAN) {
                val b = jr.nextBoolean()
                Log.e("TypeAdapter","$b is not a number")
                return 0
            }
            return if (jr.peek() == JsonToken.STRING) {
                val str = jr.nextString()
                if (NumberUtils.isIntOrLong(str)) {
                    str.toInt()
                } else {
                    Log.e("TypeAdapter","$str is not a int number")
                    0
                }
            } else {
                value = jr.nextInt()
                value
            }
        } catch (e: Exception) {
            Log.e("TypeAdapter","Not a number ${e.message}" )
        }
        return 0
    }
}