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
    }


    override fun onCreate() {
        super.onCreate()
        //初始化日志
        LogUtil.isSaveLog(applicationContext)
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
