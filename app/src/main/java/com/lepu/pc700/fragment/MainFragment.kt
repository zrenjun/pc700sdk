package com.lepu.pc700.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.Carewell.OmniEcg.jni.toJson
import com.Carewell.ecg700.port.BatteryStatusEvent
import com.Carewell.ecg700.port.LogUtil
import com.Carewell.ecg700.port.observeEvent
import com.lepu.pc700.App
import com.lepu.pc700.R
import com.lepu.pc700.databinding.FragmentMainBinding
import com.lepu.pc700.singleClick
import com.lepu.pc700.viewBinding
import kotlinx.coroutines.InternalCoroutinesApi

/**
 * 主页
 * zrj
 * 2021/8/3 18:40
 */
class MainFragment : Fragment(R.layout.fragment_main) {

    private val binding by viewBinding(FragmentMainBinding::bind)

    @SuppressLint("SetTextI18n")
    @OptIn(InternalCoroutinesApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tv1.singleClick { findNavController().navigate(R.id.routineExaminationFragment) }
        binding.tv2.singleClick { findNavController().navigate(R.id.ecg12Fragment) }
        binding.tv3.singleClick { findNavController().navigate(R.id.eCGSingleFragment) }
        binding.tv4.singleClick { findNavController().navigate(R.id.idCardInputFragment) }
        binding.tv5.singleClick { findNavController().navigate(R.id.settingFragment) }
        //是否充电
        observeEvent<BatteryStatusEvent> {  //没有百分比 只有1-4的分割 0 - 25% 25% - 50% 50% - 75% 75% - 100%
            LogUtil.e(it.toJson())
            binding.tv6.text = "电量等级:${it.chargeLevel},是否充电:${it.ac == 1}"
        }

        App.serial.mAPI?.queryBattery() //可以间隔轮询,也可以在需要的时候调用,拔插电源会主动上报
    }
}

