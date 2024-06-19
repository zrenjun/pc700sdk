package com.lepu.pc700.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.Carewell.OmniEcg.jni.toJson
import com.Carewell.ecg700.port.GetSMAInfo
import com.Carewell.ecg700.port.LogUtil
import com.Carewell.ecg700.port.observeEvent
import com.lepu.pc700.App
import com.lepu.pc700.MainActivity
import com.lepu.pc700.R
import com.lepu.pc700.databinding.FragmentIdcardinputBinding
import com.lepu.pc700.singleClick
import com.lepu.pc700.viewBinding
import kotlinx.coroutines.InternalCoroutinesApi

/**
 *author yjj
 *
 * 用户信息
 *created 2022/1/5 16:03
 */
class IdCardInputFragment : Fragment(R.layout.fragment_idcardinput) {

    private val binding by viewBinding(FragmentIdcardinputBinding::bind)
    private var bScan: Boolean = false

    @OptIn(InternalCoroutinesApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as MainActivity).setMainTitle("身份证识别")
        with(binding) {
            btnIdcardScan.singleClick {
                bScan = !bScan
                if (bScan) {
                    btnIdcardScan.text = "停止扫描"
                    App.serial.mAPI?.setIDCard_StartScan()

                } else {
                    btnIdcardScan.text = "开始扫描"
                    App.serial.mAPI?.setIDCard_StopScan()
                }
            }
        }

        observeEvent<GetSMAInfo> {
            LogUtil.e(it.toJson())
            it.info?.apply {
                binding.btnIdcardScan.performClick()
                binding.etName.setText(name)
                binding.etIdCardNo.setText(iDCardNo)
                binding.iv.setImageBitmap(headBitmap)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        App.serial.mAPI?.setIDCard_StopScan()
    }
}

