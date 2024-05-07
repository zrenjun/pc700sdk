package com.lepu.pc700

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.lepu.pc700.databinding.FragmentSettingBinding
import com.lepu.pc700.utils.singleClick
import com.lepu.pc700.utils.toast
import com.lepu.pc700.utils.viewBinding

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
    }
}