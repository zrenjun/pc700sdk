@file:Suppress("unused")

package com.lepu.pc700.net.remote
import com.Carewell.OmniEcg.jni.XmlUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.*
import java.lang.StringBuilder
import java.net.HttpURLConnection
import java.net.URL


object UrlDownload {
    suspend fun asyncDownload(url: String, fileName: String, path: String): Boolean {
        return withContext(Dispatchers.IO) { download(url, fileName, path) }
    }

    suspend fun asyncDownload(url: String): String {
        return withContext(Dispatchers.IO) { download(url) }
    }

    private fun download(url: String, fileName: String, path: String): Boolean {
        var urlConn: HttpURLConnection? = null
        val outputFile = XmlUtil.createFile(path, fileName)
        try {
            urlConn = URL(url).openConnection() as HttpURLConnection
            urlConn.inputStream.use { input ->
                BufferedOutputStream(FileOutputStream(outputFile)).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            urlConn?.disconnect()
        }
        return true
    }

    private fun download(url: String): String {
        val stringBuilder = StringBuilder()
        var urlConn: HttpURLConnection? = null
        try {
            urlConn = URL(url).openConnection() as HttpURLConnection
            urlConn.inputStream.use { input ->
                stringBuilder.append(getBufferedReader(input).use { it.readText() })
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            urlConn?.disconnect()
        }
        return stringBuilder.toString()
    }

    fun download(url: String, file: File): Flow<DownloadStatus> {
        return flow {
            val request = Request.Builder().url(url).get().build()
            val response = OkHttpClient.Builder().build().newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.let { body ->
                    //文件大小
                    val totalLength = body.contentLength().toDouble()
                    //写文件
                    file.outputStream().run {
                        body.byteStream().copyTo(this) { currentLength ->
                            //当前下载进度
                            val process = currentLength / totalLength * 100
                            emit(DownloadStatus.Progress(process.toInt()))
                        }
                    }
                    emit(DownloadStatus.Done(file))
                }
            } else {
                throw IOException(response.toString())
            }
        }.catch {
            file.delete()
            emit(DownloadStatus.Err(it))
        }.flowOn(Dispatchers.IO)
    }

    //解决android读取中文txt的乱码（自动判断文档类型并转码). 参考http://www.cnblogs.com/tc310/p/4015233.html
    @Throws(IOException::class)
    fun getBufferedReader(input: InputStream): BufferedReader {
        val stream = BufferedInputStream(input)
        stream.mark(4)
        val first3bytes = ByteArray(3)
        stream.read(first3bytes) // 找到文档的前三个字节并自动判断文档类型。
        stream.reset()
        return if (first3bytes[0] == 0xEF.toByte() && first3bytes[1] == 0xBB.toByte() && first3bytes[2] == 0xBF.toByte()) { // utf-8
            BufferedReader(InputStreamReader(stream, "utf-8"))
        } else if (first3bytes[0] == 0xFF.toByte() && first3bytes[1] == 0xFE.toByte()) {
            BufferedReader(InputStreamReader(stream, "unicode"))
        } else if (first3bytes[0] == 0xFE.toByte() && first3bytes[1] == 0xFF.toByte()) {
            BufferedReader(InputStreamReader(stream, "utf-16be"))
        } else if (first3bytes[0] == 0xFF.toByte() && first3bytes[1] == 0xFF.toByte()) {
            BufferedReader(InputStreamReader(stream, "utf-16le"))
        } else {
            BufferedReader(InputStreamReader(stream, "GBK"))
        }
    }
}

inline fun InputStream.copyTo(
    out: OutputStream,
    bufferSize: Int = DEFAULT_BUFFER_SIZE,
    progress: (Long) -> Unit
): Long {
    var bytesCopied: Long = 0
    val buffer = ByteArray(bufferSize)
    var bytes = read(buffer)
    while (bytes >= 0) {
        out.write(buffer, 0, bytes)
        bytesCopied += bytes
        bytes = read(buffer)
        progress(bytesCopied)
    }
    return bytesCopied
}

sealed class DownloadStatus {
    data class Progress(val progress: Int) : DownloadStatus()
    data class Err(val t: Throwable) : DownloadStatus()
    data class Done(val file: File) : DownloadStatus()
}