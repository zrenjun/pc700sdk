package com.lepu.pc700

import android.app.Application
import com.Carewell.ecg700.CommonApp
import com.Carewell.ecg700.LogUtil
import com.lepu.pc700.SerialViewModel
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
    }
}
