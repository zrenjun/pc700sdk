package com.lepu.pc700

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.Carewell.ecg700.LogUtil
import com.Carewell.ecg700.SerialPortHelper
import com.lepu.pc700.App
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
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
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
}

