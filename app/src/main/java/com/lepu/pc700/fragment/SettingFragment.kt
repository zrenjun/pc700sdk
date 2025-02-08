package com.lepu.pc700.fragment

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.Carewell.ecg700.port.LogUtil
import com.lepu.pc700.MainActivity
import com.lepu.pc700.R
import com.lepu.pc700.databinding.FragmentSettingBinding
import com.lepu.pc700.dialog.FirmwareUpgradeDialog
import com.lepu.pc700.singleClick
import com.lepu.pc700.toast
import com.lepu.pc700.viewBinding
import io.getstream.log.android.file.StreamLogFileManager
import java.io.File

/**
 * 设置
 * zrj
 * 2022/1/6 9:11
 */
class SettingFragment : Fragment(R.layout.fragment_setting) {
    private val binding by viewBinding(FragmentSettingBinding::bind)

    private var flag = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as MainActivity).setMainTitle("设置")
        binding.tv1.singleClick {
            requireContext().sendBroadcast(Intent("android.intent.action.${if (flag) "enablehostmode" else "disablehostmode"}"))
            flag = !flag
            toast("${if (flag) "开启" else "关闭"}host模式")
        }
        binding.tv2.singleClick {
            FirmwareUpgradeDialog().show(childFragmentManager, "FirmwareUpgradeDialog")
        }
        binding.tv3.singleClick {
            StreamLogFileManager.share()
            try {
                File("${requireContext().getExternalFilesDir(null)?.path}").listFiles()?.last()?.absolutePath?.let {
                    LogUtil.sendDsl("本地日志","", it)
                }
            }catch (e: Exception){
                e.printStackTrace()
            }

        }
    }
}