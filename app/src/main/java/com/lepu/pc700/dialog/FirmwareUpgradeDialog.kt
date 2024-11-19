package com.lepu.pc700.dialog

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.os.Environment
import android.text.TextUtils
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.Carewell.ecg700.port.IAPCheckEvent
import com.Carewell.ecg700.port.IAPEndEvent
import com.Carewell.ecg700.port.IAPProgramingEvent
import com.Carewell.ecg700.port.IAPVersionEvent
import com.Carewell.ecg700.port.IPAThreads
import com.Carewell.ecg700.port.LogUtil
import com.Carewell.ecg700.port.ShakeHandsEvent
import com.Carewell.ecg700.port.observeEvent
import com.lepu.pc700.App
import com.lepu.pc700.R
import com.lepu.pc700.databinding.DialogUpgradeBinding
import com.lepu.pc700.singleClick
import com.lepu.pc700.toast
import com.lepu.pc700.viewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.*
import java.lang.StringBuilder

/**
 *
 *  说明: FirmwareUpgradeDialog.kt
 *  zrj 2024/5/7 9:09
 *
 */
class FirmwareUpgradeDialog : DialogFragment(R.layout.dialog_upgrade) {
    private var MAIN_MCU_VER = 0
    private var SUB_MCU_VER = 0
    private var DOWNLOAD_MCU_MAIN_URL = ""
    private var DOWNLOAD_MCU_SUB_URL = ""

    private val binding by viewBinding(DialogUpgradeBinding::bind)
    private var mIAPTh: IPAThreads? = null
    private var systemMainVer = 0
    private var systemSubVer = 0
    private var gujianup_main = false
    private var gujianup_sub = false

    @OptIn(InternalCoroutinesApi::class)
    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            btnUpgradeMain.singleClick {//安装
                if (systemMainVer >= MAIN_MCU_VER) {
                    toast("已经是最新版本")
                    return@singleClick
                }
                if (mIAPTh == null) {
                    toast("串口启动失败，请取消或重启后尝试！")
                    return@singleClick
                }
                btnUpgradeMain.isEnabled = false //防止升级过程中，多次点击升级
                btnUpgradeSub.isEnabled = false
                val maingujian = getMCUFileStream("main_")
                if (maingujian != null) {

                    mIAPTh?.setIAPFile(
                        BufferedInputStream(getMCUFileStream("main_")),
                        true
                    )
                    gujianup_main = true
                    gujianup_sub = false
                } else {
                    btnUpgradeMain.isEnabled = true //防止升级过程中，多次点击升级
                    btnUpgradeSub.isEnabled = true
                    toast("请先下载固件")
                }
            }

            btnUpgradeSub.singleClick {
                if (systemSubVer >= SUB_MCU_VER) {
                    toast("已经是最新版本")
                    return@singleClick
                }
                if (mIAPTh == null) {
                    toast("串口启动失败，请取消或重启后尝试！")
                    return@singleClick
                }
                btnUpgradeMain.isEnabled = false
                btnUpgradeSub.isEnabled = false
                val maingujian = getMCUFileStream("sub_")
                if (maingujian != null) {

                    mIAPTh?.setIAPFile(
                        BufferedInputStream(getMCUFileStream("sub_")),
                        false
                    )
                    gujianup_sub = true
                    gujianup_main = false
                } else {
                    btnUpgradeMain.isEnabled = true
                    btnUpgradeSub.isEnabled = true
                    toast("请先下载固件")
                }
            }
            btnUpgradeCancel.singleClick {
                if (gujianup_main || gujianup_sub) {
                    toast("正在升级，请稍后！")
                    return@singleClick
                }
                mIAPTh?.pause()
                dismiss()
            }
            btnDownloadMainVer.singleClick { download(DOWNLOAD_MCU_MAIN_URL) }
            btnDownloadSubVer.singleClick { download(DOWNLOAD_MCU_SUB_URL) }
            btnUpgradeTips.singleClick {
                val sb = StringBuilder()
                sb.append("固件升级操件步骤：\n").append("1、连接WIFI或4G网络；\n")
                    .append("2、进入“系统设置”-->“固件升级”；\n")
                    .append("3、点“下载主固件”或“下载子固件”；\n")
                    .append("4、点“主固件升级”或“子固件升级”；\n")
                    .append("5、进度条到100%即升级完成。\n")
                    .append("6、重新进入“固件升级”窗口，此时本机系统固件版本号应与服务器固件版本号一致；\n")
                    .append("如果不一致，请按住按键8秒以上重新开机并重复以上操作。")

                val alertDialog = AlertDialog.Builder(requireContext()).also {
                    it.setMessage(sb.toString())
                        .setTitle("升级提示")
                    it.setPositiveButton("确定") { dialog, _ ->
                        dialog.dismiss()
                    }
                }.create()
                alertDialog.setCanceledOnTouchOutside(false)
                alertDialog.setCancelable(false)
                alertDialog.show()
            }
            if (MAIN_MCU_VER != 0) {
                tvNetMcuMainVer.text = MAIN_MCU_VER.toString() + ""
            }
            if (SUB_MCU_VER != 0) {
                tvNetMcuSubVer.text = SUB_MCU_VER.toString() + ""
            }
        }

        this.isCancelable = false
        dialog?.setCanceledOnTouchOutside(false)//点击屏幕不消失

        observeEvent<IAPVersionEvent> {
            if (it.response.toInt() == 2) {
                if (it.softwareVersion != 0) {
                    systemMainVer = it.softwareVersion
                    binding.tvMcuMainVer.text = systemMainVer.toString()

                    if (it.hardwareVersion==0){
                        App.serial.mAPI?.serialPort?.let {
                            mIAPTh?.getIAPVer(false) //获取子固件版本
                        }
                    }else{
                        systemSubVer = it.hardwareVersion
                        binding.tvMcuSubVer.text = systemSubVer.toString()
                    }
                }
            }
            if (it.response.toInt() == 18) {
                if (it.softwareVersion != 0) {
                    systemSubVer = it.softwareVersion
                    binding.tvMcuSubVer.text = systemSubVer.toString()
                }
            }
        }
        observeEvent<IAPEndEvent> {
            LogUtil.v("IAPEndEvent")
            lifecycleScope.launch {
                delay(4000)
                mIAPTh?.wakeUp()
                delay(4000)
                App.serial.mAPI?.serialPort?.let {
                    mIAPTh?.getIAPVer(true) //获取主固件版本
                }
            }
        }

        observeEvent<ShakeHandsEvent> {
            lifecycleScope.launch(Dispatchers.Main) {
                delay(1000)
                toast("升级成功")
                binding.btnUpgradeSub.isEnabled = true
                gujianup_sub = false
                binding.btnUpgradeMain.isEnabled = true
                gujianup_main = false
            }

        }

        observeEvent<IAPCheckEvent> {
            toast("升级异常:${it.err}")
        }

        observeEvent<IAPProgramingEvent> {
            if (it.type < 100) {
                binding.pbUpgrade.progress = it.type
                binding.tvUpgradePro.text = "${it.type}%"
            } else {
                lifecycleScope.launch(Dispatchers.Main) {
                    delay(7000)
                    binding.pbUpgrade.progress = it.type
                    binding.tvUpgradePro.text = "${it.type}%"
                    if (it.type == 100 && gujianup_main) {
                        binding.tvMcuMainVer.text = MAIN_MCU_VER.toString()
                    }
                    if (it.type == 100 && gujianup_sub) {
                        binding.tvMcuSubVer.text = SUB_MCU_VER.toString()
                    }
                }
            }
        }
        if (!App.serialStart) {
            App.serial.start()
            LogUtil.v("App.serialStart")
        }
        iapStart()
    }

    private fun iapStart() {
        App.serial.mAPI?.pause()
        App.serial.mAPI?.serialPort?.let {
            if (mIAPTh != null) {
                mIAPTh?.stop()
                mIAPTh = null
            }
            mIAPTh = IPAThreads(it.inputStream, it.outputStream)
            mIAPTh?.getIAPVer(true) //获取主固件版本
        }
    }


    private fun getIAPVersion() {

    }

    override fun onStop() {
        super.onStop()
        mIAPTh?.pause()
        mIAPTh = null
        App.serial.mAPI?.reStart() //恢复API
    }

    private fun download(url: String) {
        if (TextUtils.isEmpty(url)) {
            toast("获取服务信息错误")
            return
        }
        val arrStr = url.trim { it <= ' ' }.split("/").toTypedArray() //文件名后缀不能包含空格
        val fileName = arrStr[arrStr.size - 1] //main_1301.bin
        val file = File(PROJECT_DIR, fileName)
//        viewModel.downloadApk(url, file)
    }

    /** 从下载的路径获取mcu文件流  */
    private fun getMCUFileStream(mcuPartFileName: String): FileInputStream? {
        val file = File(PROJECT_DIR)
        if (!file.exists()) { //目录不存在
            toast("本地固件不存在，请联网下载")
            return null
        }
        //获取目录下已存在的所有文件
        val mcuFile = file.listFiles()?.find { item -> item.name.contains(mcuPartFileName) }
        if (mcuFile == null) { //文件不存在
            toast("本地固件不存在，请联网下载")
            return null
        }
        try {
            return FileInputStream(mcuFile)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        return null
    }
}

val SDCARD_DIR = Environment.getExternalStorageDirectory().absolutePath

val PROJECT_DIR = "$SDCARD_DIR/PC700" //项目存放地址