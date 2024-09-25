package com.lepu.pc700.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.Carewell.ecg700.port.ECGData
import com.Carewell.ecg700.port.GetSingleECGRealTime
import com.Carewell.ecg700.port.GetSingleECGResult
import com.Carewell.ecg700.port.LogUtil
import com.Carewell.ecg700.ParseData
import com.Carewell.ecg700.port.observeEvent
import com.lepu.pc700.App
import com.lepu.pc700.MainActivity
import com.lepu.pc700.R
import com.lepu.pc700.databinding.FragmentEcgSingleBinding
import com.lepu.pc700.delayOnLifecycle
import com.lepu.pc700.singleClick
import com.lepu.pc700.toast
import com.lepu.pc700.viewBinding
import kotlinx.coroutines.*
import java.util.*
import kotlin.properties.Delegates

/**
 *
 *  说明: 单导心电
 *  zrj 2022/2/7 19:23
 *
 */
class ECGSingleFragment : Fragment(R.layout.fragment_ecg_single) {
    private val binding by viewBinding(FragmentEcgSingleBinding::bind)

    private var dataEcg = mutableListOf<Int>() // 保存心电数据的列表
    private val allData = LinkedList<Int>()
    private var countdown = 67 //0.150*67 = 10s
    private var num = 0
    private var isStart: Boolean by Delegates.observable(false) { _, _, newValue ->
        if (newValue) {
            if (dataEcg.size > 0) { //清除上一次的数据
                dataEcg.clear()
                allData.clear()
            }
            ParseData.resetFilter()
            countdown = 67
            binding.tvCountdown.text = "${(countdown * 0.15f).toInt()}"
            binding.ecg1Surfaceview.screenClear()
            App.serial.mAPI?.startSingleEcgMeasure()
        } else {
            App.serial.mAPI?.stopSingleEcgMeasure()
        }
        val drawable = ContextCompat.getDrawable(
            requireContext(),
            if (newValue) R.drawable.ic_baseline_pause_24 else R.drawable.ic_baseline_play_arrow_24
        )
        drawable?.setBounds(0, 0, drawable.minimumWidth, drawable.minimumHeight)
        binding.tvStart.setCompoundDrawables(drawable, null, null, null)
        binding.tvStart.text =
            getString(if (newValue) R.string.forecast else R.string.start_measuring)
        binding.tvCountdown.isVisible = newValue
        binding.spinnerGain.isEnabled = !newValue
        binding.spinnerSpeed.isEnabled = !newValue
    }
    private var updateTimer = Timer()
    private var flag = false
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as MainActivity).setMainTitle("单导心电")
        if (!App.serialStart) {
            App.serial.start()
            LogUtil.v("App.serialStart")
        }
        binding.spinnerGain.onItemSelectedListener = onItemSelectedListener
        binding.spinnerGain.setSelection(3, true) //设置Spinner默认选中的值
        binding.spinnerSpeed.onItemSelectedListener = onItemSelectedListener
        binding.spinnerSpeed.setSelection(2, true)
        binding.tvStart.singleClick {
            binding.tvHr.text = getString(R.string.heart)
            isStart = !isStart
        }
      initData()
    }

    @OptIn(InternalCoroutinesApi::class)
    private fun initData() {
        observeEvent<GetSingleECGResult> { onECG1Result(it.nResult, it.nHR) }
        observeEvent<GetSingleECGRealTime> { onECG1RealTimeData(it.data, it.leadOff) }
        updateTimer = Timer()
        updateTimer.schedule(object : TimerTask() {
            override fun run() {
                if (isStart) {
                    val max = if (allData.size > 50) 4 else if (allData.size > 30) 2 else 1
                    for (i in 0..max) {
                        binding.ecg1Surfaceview.delayOnLifecycle {
                            binding.ecg1Surfaceview.addWaveDate(if (allData.size > 0) allData.removeFirst() else 0)
                        }
                    }
                }
            }
        }, 200, 30)
    }

    private val onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(
            parent: AdapterView<*>,
            view: View,
            position: Int,
            id: Long
        ) {
            when (parent.id) {
                R.id.spinnerGain -> {
                    binding.ecg1Surfaceview.mCalScale = when (position) {
                        0 -> 0.5f
                        1 -> 1f
                        2 -> 2f
                        3 -> 4f
                        else -> 8f
                    }
                    binding.ecg1Surfaceview.refreshCal()
                }

                R.id.spinnerSpeed -> binding.ecg1Surfaceview.setSpeed(
                    when (position) {
                        0 -> 0.245f
                        1 -> 0.491f
                        2 -> 0.982f
                        else -> 1.964f
                    }
                )
            }
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {}
    }


    @SuppressLint("SetTextI18n")
    private fun onECG1RealTimeData(ecgData: ECGData, leadOff: Boolean) {
        if (isStart) {
            if (num < 10) { //延时1.5f判断脱落
                num++
            } else {
                //数据的最高位，1 代表导联脱落(false)，0 为不脱落(true)
                binding.tvOff.isVisible = !leadOff
            }

            val data = ecgData.data.map { it.data }
            data.forEachIndexed { index, i ->
                val filter = if (flag) ParseData.filter(
                    i.toDouble(),
                    index == 0 && countdown == 67
                ) else ParseData.offlineFilter(i.toDouble(), index == 0 && countdown == 67)

                if (filter.isNotEmpty()) {
                    if (countdown <= 33) {//前5秒数据丢弃
                        allData.addAll(filter.map { item -> item.toInt() })
                    }
                }
            }
            if (ecgData.frameNum == 0) {
                countdown--
                binding.tvCountdown.text = "${(countdown * 0.15f).toInt()}"
            } else { //30s正式开始后的心电数据
                binding.tvCountdown.isVisible = false
                binding.tvStart.text = "${(ecgData.frameNum * 0.15f).toInt()}s/30s"
                dataEcg.addAll(data)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun onECG1Result(result: Int, hr: Int) {
        if (result == 0xee) {
            binding.tvHr.text = "${getString(R.string.heart)}$hr"
            return
        }
        isStart = false
        val ecgResult = resources.getStringArray(R.array.ecg_result)
        var index = result
        if (result == 0xff) {
            index = 16
        }
        LogUtil.e(result)
        if (index == 16) {
            toast(ecgResult[index])
            return
        }
        var data = dataEcg.take(4500).map { it.toShort() }.toShortArray()
        data = ParseData.newShortFilter(data)
        //回顾
//        SingleEcgUploadDialog.newInstance(data).show(childFragmentManager, "")
    }

    override fun onStop() {
        super.onStop()
        isStart = false
        updateTimer.cancel()
    }
}

