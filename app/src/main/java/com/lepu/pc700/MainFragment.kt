package com.lepu.pc700

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.lepu.pc700.databinding.FragmentMainBinding

/**
 * 主页
 * zrj
 * 2021/8/3 18:40
 */
class MainFragment : Fragment(R.layout.fragment_main) {

    private val binding by viewBinding(FragmentMainBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tv1.singleClick {findNavController().navigate(R.id.routineExaminationFragment)}
        binding.tv2.singleClick { findNavController().navigate(R.id.ecg12Fragment) }
        binding.tv3.singleClick { findNavController().navigate(R.id.eCGSingleFragment) }
        binding.tv4.singleClick { findNavController().navigate(R.id.idCardInputFragment) }
        binding.tv5.singleClick { findNavController().navigate(R.id.settingFragment) }
    }
}

