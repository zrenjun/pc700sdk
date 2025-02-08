package com.Carewell.ecg700.port

import android.annotation.SuppressLint
import android.content.Context
import co.nedim.maildroidx.MaildroidXType
import co.nedim.maildroidx.sendEmail
import io.getstream.log.StreamLog
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.util.*


/**
 * Created by zrj 2017/6/10.
 */
@SuppressLint("StaticFieldLeak")
object LogUtil {
    private var logEnabled = true
    private var tag = "zrj"
    private var isSaveLog = true  //是否保存日志
    private lateinit var context: Context

    fun sendDsl(subject: String, text: String, filePath: String = "") {
        try {
            sendEmail {
                smtp("smtp.163.com")
                smtpUsername("zrj15347281367@163.com")
                smtpPassword("NZPJJMYLPTHEMKDL")
                port("25")
                type(MaildroidXType.PLAIN)
                to("zhourenjun1@lepucloud.com")
                from("zrj15347281367@163.com")
                subject(subject)
                body(text)
                if (filePath.isNotEmpty()) {
                    attachment(filePath)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isSaveLog(context: Context) {
        LogUtil.context = context
    }
    fun v(msg: String, customTag: String = "Serial") {
        log(customTag, msg)
    }
    fun e(msg: String, customTag: String = tag) {
        log(customTag, msg)
    }

    fun e(msg: Int, customTag: String = tag) {
        log(customTag, "$msg")
    }

    fun json(msg: String, customTag: String = tag) {
        val json = formatJson(msg)
        log(customTag, json)
    }

    /**
     * 格式化json
     */
    private fun formatJson(json: String): String {
        return try {
            val trimJson = json.trim()
            when {
                trimJson.startsWith("{") -> JSONObject(trimJson).toString(4)
                trimJson.startsWith("[") -> JSONArray(trimJson).toString(4)
                else -> trimJson
            }
        } catch (e: JSONException) {
            e.printStackTrace().toString()
        }
    }

    /**
     * 输出日志
     */
    private fun log(customTag: String, msg: String) {
        if (!logEnabled) return
        val elements = Thread.currentThread().stackTrace
        val index = findIndex(elements)
        val element = elements[index]
        val tag = handleTag(element, customTag)
        val content = "(${element.fileName}:${element.lineNumber}).${element.methodName}:  $msg"
        if (isSaveLog) {
            point(tag, content)
        }
    }


    /**
     * 处理tag逻辑
     */
    private fun handleTag(element: StackTraceElement, customTag: String): String = when {
        customTag.isNotBlank() -> customTag
        else -> element.className.substringAfterLast(".")
    }

    /**
     * 寻找当前调用类在[elements]中的下标
     */
    private fun findIndex(elements: Array<StackTraceElement>): Int {
        var index = 5
        while (index < elements.size) {
            val className = elements[index].className
            if (className != LogUtil::class.java.name && !elements[index].methodName.startsWith("log")) {
                return index
            }
            index++
        }
        return -1
    }

    @SuppressLint("SimpleDateFormat")
    private fun point(tag: String, msg: String) {
        try {
            if (isSaveLog) {
                val log = File("${context.getExternalFilesDir(null)?.path}")
                val dir = log.listFiles()
                if (dir != null && (dir.size > 7 || log.length() > 40 * 1024 * 1024)) {//5*4 5天  40M
                    //文件修改日期：递增
                    Arrays.sort(dir, object : Comparator<File> {
                        override fun compare(f1: File, f2: File): Int {
                            val diff = f1.lastModified() - f2.lastModified()
                            return if (diff > 0) 1 else if (diff == 0L) 0 else -1 //如果 if 中修改为 返回-1 同时此处修改为返回 1  排序就会是递减
                        }

                        override fun equals(other: Any?): Boolean {
                            return true
                        }
                    })
                    dir[0].delete()
                }
                StreamLog.e(tag = tag) { msg }
            }
        }catch (e:Exception){
            e.printStackTrace()
        }
    }
}



