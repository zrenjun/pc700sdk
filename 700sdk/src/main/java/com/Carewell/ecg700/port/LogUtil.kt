package com.Carewell.ecg700.port

import android.annotation.SuppressLint
import android.content.Context
import android.os.Debug
import android.util.Log
import co.nedim.maildroidx.MaildroidXType
import co.nedim.maildroidx.sendEmail
import io.getstream.log.StreamLog
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit


/**
 * Created by zrj 2017/6/10.
 */
@SuppressLint("StaticFieldLeak")
object LogUtil {
    private var logEnabled = true
    private var tag = "zrj"
    private var isSaveLog = true  //是否保存日志
    private lateinit var context: Context

    // 是否在每条日志抓取调用栈以获取文件名/行号。抓栈(Thread.stackTrace)是极昂贵的同步操作，
    // 在 1000Hz 心电消费/生产热路径上逐条抓栈会严重拖慢线程，导致队列积压。默认关闭。
    @Volatile
    private var traceLocation = false

    // ---- 日志文件滚动检查降频 ----
    // 原实现每写一条日志都 File.listFiles() 扫描目录并判断是否需要删除旧文件，
    // 这是每条日志一次文件系统元数据 I/O，在热路径上是主要卡顿源。
    // 改为“每 CHECK_EVERY 条或距上次超过 CHECK_INTERVAL_MS 才检查一次”，把目录 I/O 移出逐条路径。
    private const val ROLL_CHECK_EVERY = 500
    private const val ROLL_CHECK_INTERVAL_MS = 10_000L
    private var logCountSinceCheck = 0
    private var lastRollCheckTime = 0L

    // 邮件附件大小上限：超过此值的文件(典型是几十~几百 MB 的 .hprof)不作为附件发送，
    // 只在正文里写明其设备路径，由人工去设备拉取，避免 SMTP 发送大附件失败/超时。
    private const val MAIL_ATTACH_MAX_BYTES = 20L * 1024 * 1024

    // “待随下一封邮件带出的” heap dump。仅当采样检测到堆逼近 OOM 而触发了一次 dump 时才置位；
    // 平时为 null，发普通日志邮件不会附带任何快照。随邮件带出(或注明路径)后即清空，只带一次。
    @Volatile
    private var pendingHprof: File? = null

    /**
     * 发送日志邮件。
     * @param filePath 主日志文件(向后兼容旧调用)。
     * @param attachHeapDump 是否在“最近发生过 OOM 迹象(触发过 heap dump)”时附带该快照。
     *                       只有 [pendingHprof] 非空(即刚发生疑似 OOM)才附带；普通日志邮件不带。
     *                       hprof 通常很大，超过 [MAIL_ATTACH_MAX_BYTES] 时不作为附件，改在正文注明路径。
     */
    fun sendDsl(
        subject: String,
        text: String,
        filePath: String = "",
        attachHeapDump: Boolean = true
    ) {
        try {
            // 收集要附带的文件。顺序讲究：hprof 放前面、主日志放最后，
            // 因为部分 maildroidx 版本 attachment() 多次调用只保留“最后一个”，
            // 主日志(含 RES 采样、可读性强)最重要，必须保证一定被带上。
            val attachPaths = ArrayList<String>()

            val bodyBuilder = StringBuilder(text)
            // 仅当最近发生过疑似 OOM(pendingHprof 非空)时才附带快照；平时发日志不带。
            val hprof = if (attachHeapDump) pendingHprof else null
            if (hprof != null && hprof.exists()) {
                bodyBuilder.append("\n\n---- heap dump (最近一次疑似OOM) ----\n")
                bodyBuilder.append("path: ").append(hprof.absolutePath).append('\n')
                bodyBuilder.append("size: ").append(hprof.length() / (1024 * 1024)).append(" MB\n")
                if (hprof.length() <= MAIL_ATTACH_MAX_BYTES) {
                    attachPaths.add(hprof.absolutePath)
                    bodyBuilder.append("(已尝试作为附件发送；若邮件中缺失，请按上述路径到设备拉取)\n")
                } else {
                    bodyBuilder.append("(文件过大未作附件，请到设备上述路径拉取后用 MAT/Android Studio 分析)\n")
                }
                // 只带一次，带出后清空标志，避免后续普通邮件重复附带
                pendingHprof = null
            }
            // 主日志放最后，确保在“只留最后一个附件”的库版本下它一定被发送
            if (filePath.isNotEmpty()) attachPaths.add(filePath)
            val finalBody = bodyBuilder.toString()

            sendEmail {
                smtp("smtp.163.com")
                smtpUsername("zrj15347281367@163.com")
                smtpPassword("NZPJJMYLPTHEMKDL")
                port("25")
                type(MaildroidXType.PLAIN)
                to("zhourenjun1@lepucloud.com")
                from("zrj15347281367@163.com")
                subject(subject)
                body(finalBody)
                // 逐个附加。注意：部分 maildroidx 版本 attachment() 多次调用只保留最后一个，
                // 为稳妥，主日志优先放最后一个 attachment 以保证它一定被带上。
                for (p in attachPaths) {
                    attachment(p)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isSaveLog(context: Context, logEnabled: Boolean = true, isSaveLog: Boolean = true)  {
        LogUtil.context = context
        LogUtil.logEnabled = logEnabled
        LogUtil.isSaveLog = isSaveLog
        // 注意：不再自行安装 UncaughtExceptionHandler。
        // APP 先于 SDK 初始化 Bugly，Bugly 已接管崩溃上报且平时崩溃都能收到；
        // 若 SDK 再抢占 handler 并在崩溃时做重活(dump 全线程栈/写文件)，
        // 可能延迟或干扰 Bugly 的上报，得不偿失。
        // 能被 Java 崩溃回调捕获的异常交给 Bugly；Bugly 抓不到的那类 OOM
        // (线程/FD 耗尽、native OOM、被系统 LMK 直接杀)本就不走 Java 回调，
        // 改由下面的资源采样器留下的“崩溃前最后一条采样”来判定类型。
        startResourceSampler()
    }

    // ---- 运行时资源采样 ----
    // 目的：很多 OOM(线程/FD 耗尽、native OOM、被系统 LMK 直接杀)不会走 Java 崩溃回调，
    // Bugly 和 UncaughtExceptionHandler 都抓不到。改为周期性记录“线程数/Java堆/native堆/FD数/队列大小”，
    // 崩溃前最后一条采样即可区分是内存涨、线程涨还是 FD 涨，锁定 OOM 类型。
    // 用单独的调度线程异步执行，绝不占用心电消费/生产热路径。
    private const val SAMPLE_INTERVAL_SEC = 30L
    private var sampler: ScheduledExecutorService? = null

    // Java 堆使用率超过此阈值时，自动 dump 一次 heap(.hprof)，供事后用 MAT/Android Studio 分析
    // 到底是哪类对象撑满堆。dump 文件很大且过程耗时，因此全程只触发一次。
    private const val HPROF_DUMP_THRESHOLD = 0.90
    @Volatile
    private var hprofDumped = false

    // 由数据消费方(如 ParseEcg12Data)注入的“当前待处理队列大小”提供者，避免 LogUtil 反向依赖具体类。
    @Volatile
    private var queueSizeProvider: (() -> Int)? = null
    fun setQueueSizeProvider(provider: (() -> Int)?) {
        queueSizeProvider = provider
    }

    @Synchronized
    private fun startResourceSampler() {
        if (sampler != null) return
        sampler = Executors.newSingleThreadScheduledExecutor { r ->
            Thread(r, "res-sampler").apply { isDaemon = true }
        }.also {
            it.scheduleWithFixedDelay(
                { sampleResourceOnce() },
                SAMPLE_INTERVAL_SEC, SAMPLE_INTERVAL_SEC, TimeUnit.SECONDS
            )
        }
    }

    private fun sampleResourceOnce() {
        try {
            val mb = 1024L * 1024L
            val rt = Runtime.getRuntime()
            val javaUsed = (rt.totalMemory() - rt.freeMemory()) / mb
            val javaMax = rt.maxMemory() / mb
            val nativeUsed = Debug.getNativeHeapAllocatedSize() / mb
            val threadCount = Thread.activeCount()
            val fdCount = countOpenFds()
            val q = try { queueSizeProvider?.invoke() ?: -1 } catch (e: Exception) { -1 }
            // 线程名前缀聚合：让单独一条采样就能暴露“协程/线程泄漏”(如 DefaultDispatcher-worker x N)，
            // 不必依赖多条趋势对比。只取线程名、不取栈，开销可控(30 秒一次)。
            val topThreads = topThreadPrefixes()
            // 走普通日志通道(异步/降负后的)，tag 固定便于过滤
            e(
                "RES java=${javaUsed}/${javaMax}MB native=${nativeUsed}MB " +
                    "threads=$threadCount fd=$fdCount queue=$q  $topThreads",
                "ResSampler"
            )
            // Java 堆逼近上限时，抓一份 heap dump 供事后点名是哪类对象撑满(只抓一次)
            if (!hprofDumped && javaMax > 0 && javaUsed.toDouble() / javaMax >= HPROF_DUMP_THRESHOLD) {
                dumpHprofOnce(javaUsed, javaMax, threadCount)
            }
        } catch (t: Throwable) {
            // 采样失败绝不能影响主流程
            try { Log.e("ResSampler", "采样失败: ${t.message}") } catch (_: Throwable) {}
        }
    }

    /**
     * Java 堆逼近上限时抓一份 .hprof 堆快照落盘。全程仅一次。
     * 用于事后用 MAT/Android Studio Profiler 打开，直接看是哪类对象(ByteArray? Fragment? IntArray?)占满堆，
     * 把"采样只能指方向"升级为"点名具体对象"。
     * 注意：dump 会短暂冻结进程且文件较大(与堆大小相当)，因此只在真正逼近 OOM 时触发一次。
     */
    @Synchronized
    private fun dumpHprofOnce(javaUsedMb: Long, javaMaxMb: Long, threadCount: Int) {
        if (hprofDumped) return
        hprofDumped = true
        try {
            val base = context.getExternalFilesDir(null)?.path ?: return
            val dir = File(base, "hprof")
            if (!dir.exists()) dir.mkdirs()
            val name = "heap_" + try {
                java.text.SimpleDateFormat("yyMMdd_HHmmss").format(Date())
            } catch (t: Throwable) { System.currentTimeMillis().toString() } + ".hprof"
            val file = File(dir, name)
            e("检测到Java堆逼近上限(${javaUsedMb}/${javaMaxMb}MB, threads=$threadCount)，开始 dump heap: ${file.absolutePath}", "ResSampler")
            Debug.dumpHprofData(file.absolutePath)
            // 标记：本次疑似 OOM 抓到的快照，待随下一封日志邮件带出(只带一次)。
            pendingHprof = file
            e("heap dump 完成: ${file.absolutePath} (大小 ${file.length() / (1024 * 1024)}MB)，将随下次日志邮件附带/注明路径", "ResSampler")
        } catch (t: Throwable) {
            // dump 本身可能因内存/存储不足失败，失败不影响主流程；允许后续再试一次
            hprofDumped = false
            try { Log.e("ResSampler", "dump heap 失败: ${t.message}") } catch (_: Throwable) {}
        }
    }

    // 统计当前进程打开的文件描述符数量(线程/FD 泄漏是这类 OOM 的常见根因)。
    private fun countOpenFds(): Int {
        return try {
            val fdDir = File("/proc/self/fd")
            fdDir.list()?.size ?: -1
        } catch (e: Exception) {
            -1
        }
    }

    // 按线程名前缀聚合，输出数量最多的前几类(数量>=3 才列)，用于一眼看出协程/线程池是否泄漏。
    // 例如输出 "[DefaultDispatcher x120 res-sampler x1 ...]"，DefaultDispatcher 暴涨即协程泄漏。
    private fun topThreadPrefixes(): String {
        return try {
            val names = Thread.getAllStackTraces().keys // 只取线程对象，不遍历栈，开销较低
            val byPrefix = HashMap<String, Int>()
            for (t in names) {
                // 取到第一个 '-' 或空格前的前缀，如 DefaultDispatcher-worker-3 → DefaultDispatcher
                val prefix = t.name.substringBefore('-').substringBefore(' ')
                byPrefix[prefix] = (byPrefix[prefix] ?: 0) + 1
            }
            val sb = StringBuilder("threadTop[")
            byPrefix.entries.sortedByDescending { it.value }
                .filter { it.value >= 3 }
                .take(5)
                .forEach { sb.append(it.key).append('x').append(it.value).append(' ') }
            sb.append(']')
            sb.toString()
        } catch (e: Exception) {
            "threadTop[?]"
        }
    }

    /**
     * 是否在每条日志抓取调用栈以打印文件名/行号。
     * 抓栈开销极高，仅建议在排查问题时临时开启；生产环境保持关闭以避免拖慢热路径。
     */
    fun setTraceLocation(enabled: Boolean) {
        traceLocation = enabled
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
        // 默认路径：不抓调用栈，直接使用调用方传入的 tag 与消息，避免每条日志一次昂贵的 stackTrace。
        if (!traceLocation) {
            val tag = if (customTag.isNotBlank()) customTag else this.tag
            point(tag, msg)
            return
        }
        // 仅在显式开启定位时才抓栈打印文件名/行号（调试用）。
        val elements = Thread.currentThread().stackTrace
        val index = findIndex(elements)
        // 保护：findIndex 找不到合适帧时返回 -1，直接用 elements[-1] 会数组越界崩溃。
        // 此时降级为不带位置信息，保证日志本身不会因为定位失败而抛异常。
        if (index < 0 || index >= elements.size) {
            val tag = if (customTag.isNotBlank()) customTag else this.tag
            point(tag, msg)
            return
        }
        val element = elements[index]
        val tag = handleTag(element, customTag)
        val content = "(${element.fileName}:${element.lineNumber}).${element.methodName}:  $msg"
        point(tag, content)
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
                // 文件滚动检查降频：不再每条日志都 listFiles() 扫描目录，
                // 而是累计 ROLL_CHECK_EVERY 条或距上次超过 ROLL_CHECK_INTERVAL_MS 才检查一次，
                // 把昂贵的目录元数据 I/O 移出逐条日志热路径。
                maybeRollLogFiles()
                StreamLog.e(tag = tag) { msg }
            }else{
                Log.e(tag, msg)
            }
        }catch (e:Exception){
            e.printStackTrace()
        }
    }

    /**
     * 按频率检查并滚动日志文件（超过 7 个文件或 50MB 时删除最旧文件）。
     * 仅在累计足够条数或超过时间间隔时才真正扫描目录，降低每条日志的 I/O 开销。
     */
    private fun maybeRollLogFiles() {
        logCountSinceCheck++
        val now = System.currentTimeMillis()
        if (logCountSinceCheck < ROLL_CHECK_EVERY && now - lastRollCheckTime < ROLL_CHECK_INTERVAL_MS) {
            return
        }
        logCountSinceCheck = 0
        lastRollCheckTime = now

        val log = File("${context.getExternalFilesDir(null)?.path}")
        val dir = log.listFiles()
        if (dir != null && (dir.size > 7 || log.length() > 50 * 1024 * 1024)) {//5*4 5天  50M
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
    }
}



