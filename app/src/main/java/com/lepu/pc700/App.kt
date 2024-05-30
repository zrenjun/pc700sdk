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
        LogUtil.isSaveLog(applicationContext)
        CommonApp.init(this) //初始化bus
        serial = SerialViewModel(this)
        serial.start()
    }
}
