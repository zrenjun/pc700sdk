package com.lepu.pc700.net.remote.gson

import android.text.TextUtils
import java.util.regex.Pattern

object NumberUtils {
    fun isIntOrLong(str: String): Boolean {
        return TextUtils.isDigitsOnly(str)
    }

    fun isFloatOrDouble(str: String): Boolean {
        val pattern = Pattern.compile("^[-\\+]?[.\\d]*$")
        return pattern.matcher(str).matches()
    }
}