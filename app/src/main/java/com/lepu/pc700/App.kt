package com.lepu.pc700

import android.app.Application
import com.Carewell.ecg700.port.CommonApp
import com.Carewell.ecg700.port.LogUtil
import com.lepu.pc700.net.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.logger.AndroidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.logger.Level
import kotlin.properties.Delegates

/**
 * 说明: java
 * zrj 2022/3/24 15:18
 */
class App : Application() {

    companion object {
        var serial: SerialViewModel by Delegates.notNull()
            private set
        var serialStart = false

        var context: App by Delegates.notNull()
            private set

        var isSetGluType = false
        var mcuMainVer = 0
    }


    override fun onCreate() {
        super.onCreate()
        context = this
        //初始化日志
        LogUtil.isSaveLog(applicationContext)
        // 让日志重新带上(类名/文件名:行号).方法名。注意：会对每条日志抓一次调用栈，有性能开销，
        // 仅建议调试/测试期开启；发布版如需极致性能，改为 false 或用 BuildConfig.DEBUG 包裹。
        LogUtil.setTraceLocation(true)
        //初始化bus
        CommonApp.init(this)
        //初始化串口
        serial = SerialViewModel(this)
        serial.start()

        startKoin {
            AndroidLogger(Level.DEBUG)
            androidContext(this@App)
            modules(appModule)
        }
    }
}
