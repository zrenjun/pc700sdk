package com.lepu.pc700

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.Carewell.OmniEcg.jni.toJson
import com.Carewell.ecg700.GetSMAInfo
import com.Carewell.ecg700.LogUtil
import com.Carewell.ecg700.observeEvent
import com.lepu.pc700.databinding.FragmentIdcardinputBinding
import com.zkteco.android.IDReader.IDPhotoHelper
import com.zkteco.android.IDReader.WLTService
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

