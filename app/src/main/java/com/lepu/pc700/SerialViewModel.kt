package com.lepu.pc700

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.Carewell.ecg700.port.LogUtil
import com.Carewell.ecg700.port.SerialPortHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


/**
 * java类作用描述
 * zrj
 * 2021/8/12 15:37
 */
class SerialViewModel(app: App) : AndroidViewModel(app) {

    var mAPI: SerialPortHelper? = null

    override fun onCleared() {
        super.onCleared()
        mAPI?.stop()
        App.serialStart = false
    }

    fun start() {
        // 幂等守卫：串口/消费协程随进程常驻，全局只应启动一次。
        // App.onCreate 已调用一次 start()，而其真正置位 App.serialStart 是在下面的异步 IO 块内完成，
        // 存在竞态窗口——若某个心电页 onViewCreated 在此窗口内判断 !App.serialStart 又调一次 start()，
        // 旧逻辑会再 new 一个 SerialPortHelper 覆盖旧引用，旧 helper 的消费协程失去引用却未 stop，
        // 造成 "多条消费协程抢同一个静态 queue + 旧协程钩住旧 Fragment/View 泄漏"，长时间运行必然 OOM。
        // 这里用同步的 mAPI != null 提前拦截，杜绝重复启动。
        if (mAPI != null) {
            LogUtil.v("串口已启动，跳过重复 start")
            return
        }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    // 双检：异步块真正执行时再次确认，防止两次 start() 的 launch 都越过上面的同步检查。
                    if (mAPI != null) return@withContext
                    mAPI = SerialPortHelper()
                    mAPI?.start()
                    App.serialStart = true
                    LogUtil.v("App.serialStart")
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    LogUtil.v(ex.message ?: "")
                }
            }
        }
    }

    fun stop() {
        mAPI?.stop()
        mAPI = null
    }
}

