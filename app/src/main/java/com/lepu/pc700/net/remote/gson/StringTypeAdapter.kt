package com.lepu.pc700.net.remote.gson

import android.util.Log
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.io.IOException


class StringTypeAdapter : TypeAdapter<String?>() {
    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: String?) {
        try {
            if (value == null) {
                out.nullValue()
                return
            }
            out.value(value)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Throws(IOException::class)
    override fun read(jr: JsonReader): String? {
        try {
            if (jr.peek() == JsonToken.NULL) {
                jr.nextNull()
                Log.e("TypeAdapter", "null is not a string")
                return ""
            }
        } catch (e: Exception) {
            Log.e("TypeAdapter", "Not a String", e)
        }
        return jr.nextString()
    }
}