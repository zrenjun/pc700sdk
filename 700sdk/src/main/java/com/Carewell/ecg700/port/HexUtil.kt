package com.Carewell.ecg700.port

import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import java.util.*
import kotlin.experimental.xor
import kotlin.math.floor
import kotlin.math.pow

object HexUtil {
    fun hexStringToBytes(hexString: String): ByteArray {
        var hex = hexString
        hex = hex.toUpperCase()
        val length = hex.length / 2
        val hexChars = hex.toCharArray()
        val d = ByteArray(length)
        for (i in 0 until length) {
            val pos = i * 2
            d[i] =
                (charToByte(hexChars[pos]).toInt() shl 4 or charToByte(hexChars[pos + 1]).toInt()).toByte()
        }
        return d
    }

    fun charToByte(c: Char): Byte {
        return "0123456789ABCDEF".indexOf(c).toByte()
    }

    /**
     * 二进制字符串转十进制
     */
    fun binaryToAlgorithm(binary: String): Int {
        val max = binary.length
        var result = 0
        for (i in max downTo 1) {
            val c = binary[i - 1]
            val algorithm = c - '0'
            result += (2.0.pow(max - i.toDouble()) * algorithm).toInt()
        }
        return result
    }

    fun bytesToHexString(src: ByteArray?): String? {
        val stringBuilder = StringBuilder("")
        if (src == null || src.isEmpty()) {
            return null
        }
        for (i in src.indices) {
            val v: Int = src[i].toInt() and 0xFF
            val hv = Integer.toHexString(v)
            if (hv.length < 2) {
                stringBuilder.append(0)
            }
            stringBuilder.append(hv)
            stringBuilder.append(", ")
        }
        return stringBuilder.toString()
    }

    fun getXor(data: ByteArray): Byte {
        var temp = data[0]
        for (i in 1 until data.size) {
            temp = temp xor data[i]
        }
        return temp
    }

    /**
     * 十六进制字符串转十进制
     */
    fun hexStringToAlgorithm(hex: String): Int {
        var s = hex
        s = s.toUpperCase()
        val max = s.length
        var result = 0
        for (i in max downTo 1) {
            val c = s[i - 1]
            val algorithm: Int = if (c in '0'..'9') {
                c - '0'
            } else {
                c.toInt() - 55
            }
            result += (16.0.pow(max - i.toDouble()) * algorithm).toInt()
        }
        return result
    }

    //16进制字符串转ascII字符串
    fun hexToASCIIString(hex: String): String {
        var s = hex
        val baKeyword = ByteArray(s.length / 2)
        for (i in baKeyword.indices) {
            try {
                baKeyword[i] = (0xff and s.substring(
                    i * 2, i * 2 + 2
                ).toInt(16)).toByte()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        try {
            s = String(baKeyword, Charset.forName("ASCII"))
        } catch (e1: Exception) {
            e1.printStackTrace()
        }
        return s
    }

    fun hexToGBString(hex: String): String {
        var s = hex
        val baKeyword = ByteArray(s.length / 2)
        for (i in baKeyword.indices) {
            try {
                baKeyword[i] = (0xff and s.substring(
                    i * 2, i * 2 + 2
                ).toInt(16)).toByte()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        try {
            s = String(baKeyword, Charset.forName("GB2312"))
        } catch (e1: Exception) {
            e1.printStackTrace()
        }
        return s
    }

    /**
     * 十进制转换为十六进制字符串
     */
    fun algorithmToHEXString(i: Int): String {
        var result = Integer.toHexString(i)
        if (result.length % 2 == 1) {
            result = "0$result"
        }
        result = result.toUpperCase()
        return result
    }

    /**
     * 按照指定长度切割字符串
     *
     * @param inputString 需要切割的源字符串
     * @param length      指定的长度
     * @return
     */
    fun getDivLines(inputString: String, length: Int): List<String> {
        val divList: MutableList<String> =
            ArrayList()
        val remainder = inputString.length % length
        // 一共要分割成几段
        val number = floor(inputString.length / length.toDouble()).toInt()
        for (index in 0 until number) {
            val childStr =
                inputString.substring(index * length, (index + 1) * length)
            divList.add(childStr)
        }
        if (remainder > 0) {
            val cStr = inputString.substring(number * length, inputString.length)
            divList.add(cStr)
        }
        return divList
    }

    fun string2ASCII(s: String?): IntArray? { // 字符串转换为ASCII码
        if (s == null || "" == s) {
            return null
        }
        val chars = s.toCharArray()
        val asciiArray = IntArray(chars.size)
        for (i in chars.indices) {
            asciiArray[i] = chars[i].toInt()
        }
        return asciiArray
    }

    fun hz2utf(str: String): ByteArray? {
        return try {
            str.toByteArray(charset("utf-8"))
        } catch (ex: UnsupportedEncodingException) {
            null
        }
    }
}