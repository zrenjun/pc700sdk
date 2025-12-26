package com.lepu.pc700.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.Carewell.OmniEcg.jni.toJson
import com.Carewell.ecg700.port.ECGData
import com.Carewell.ecg700.port.GetSingleECGRealTime
import com.Carewell.ecg700.port.GetSingleECGResult
import com.Carewell.ecg700.port.LogUtil
import com.Carewell.ecg700.ParseData
import com.Carewell.ecg700.port.GetSingleECGGain
import com.Carewell.ecg700.port.Wave
import com.Carewell.ecg700.port.observeEvent
import com.Carewell.ecg700.port.postEvent
import com.Carewell.ecg700.port.toInt
import com.Carewell.view.ecg12.Const
import com.Carewell.view.ecg12.EcgConfig
import com.Carewell.view.ecg12.LeadType
import com.Carewell.view.ecg12.MainEcgManager
import com.Carewell.view.ecg12.PreviewManager
import com.Carewell.view.other.LoadingForView
import com.lepu.pc700.App
import com.lepu.pc700.MainActivity
import com.lepu.pc700.R
import com.lepu.pc700.databinding.FragmentEcgSingleBinding
import com.lepu.pc700.delayOnLifecycle
import com.lepu.pc700.net.util.Constant
import com.lepu.pc700.singleClick
import com.lepu.pc700.toast
import com.lepu.pc700.viewBinding
import com.lepu.pc_700.widget.dialog.EcgPlaybackFragemntDialog
import kotlinx.coroutines.*
import java.io.File
import java.util.*
import kotlin.collections.get
import kotlin.collections.toShortArray
import kotlin.properties.Delegates

/**
 *
 *  说明: 单导心电
 *  zrj 2022/2/7 19:23
 *
 */
@SuppressLint("SetTextI18n")
class ECGSingleFragment : Fragment(R.layout.fragment_ecg_single) {
    private val binding by viewBinding(FragmentEcgSingleBinding::bind)

    private var dataEcg = mutableListOf<Int>() // 保存心电数据的列表
    private val allData = LinkedList<Int>()
    private val allData2 = LinkedList<Int>()
    private var time = 0
    private var countdown = 120 // 10s
    private var num = 0
    private var isStart: Boolean by Delegates.observable(false) { _, _, newValue ->
        if (newValue) {
            if (dataEcg.isNotEmpty()) { //清除上一次的数据
                dataEcg.clear()
                allData.clear()
                allData2.clear()
            }
            ParseData.resetFilter()
            time = 0
            countdown = 120
            binding.tvCountdown.text = "${(countdown * 0.084f).toInt()}"
            binding.ecg1Surfaceview.screenClear()
            App.serial.mAPI?.startSingleEcgMeasure()
            ParseData.hpf05(0, 1)
            setPeriod()
        } else {
            App.serial.mAPI?.stopSingleEcgMeasure()
            binding.tvOff.isVisible = false
            binding.tvHr.text = "${getString(R.string.heart)}--"
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
        binding.switchFilter.isEnabled = !newValue
    }
    private var updateTimer = Timer()


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as MainActivity).setMainTitle("单导心电")
        if (!App.serialStart) {
            App.serial.start()
            LogUtil.v("App.serialStart")
        }
        binding.spinnerGain.onItemSelectedListener = onItemSelectedListener
        binding.spinnerGain.setSelection(2, true) //设置Spinner默认选中的值
        binding.spinnerSpeed.onItemSelectedListener = onItemSelectedListener
        binding.spinnerSpeed.setSelection(4, true)
        binding.tvStart.singleClick {
            isStart = !isStart
        }

        MainEcgManager.getInstance().init()
        MainEcgManager.getInstance().updateMainEcgShowStyle(LeadType.LEAD_I)
        initData()
    }

    private var max = 3

    @OptIn(InternalCoroutinesApi::class)
    private fun initData() {
        observeEvent<GetSingleECGResult> { onECG1Result(it.nResult, it.nHR) }
        observeEvent<GetSingleECGRealTime> { onECG1RealTimeData(it.data, it.leadOff) }
        observeEvent<GetSingleECGGain> {
            LogUtil.e(it.toJson())
        }

    }

    private fun setPeriod() {
        updateTimer.cancel()
        updateTimer = Timer()
        updateTimer.schedule(object : TimerTask() {
            override fun run() {
                if (isStart) {
                    for (i in 0 until max) {
                        binding.ecg1Surfaceview.delayOnLifecycle {
                            binding.ecg1Surfaceview.addWaveDate(
                                if (allData.isNotEmpty()) allData.removeAt(
                                    0
                                ) else 0
                            )
                        }
                    }
                }
            }
        }, 200, 21)
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
                    MainEcgManager.getInstance().updateMainGain(position)
                }

                R.id.spinnerSpeed -> {
                    binding.ecg1Surfaceview.setSpeed(
                        when (position) {
                            0 -> 0.245f
                            1 -> 0.491f
                            2 -> 0.982f
                            else -> 1.964f
                        }
                    )
                    MainEcgManager.getInstance().updateMainSpeed(position)
                }
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
                if (binding.switchFilter.isChecked) {
                    if (index % 2 == 0) {
                        val filter150 = ParseData.filter150(i, if (countdown == 120) 1 else 0)
                        val filterHp = ParseData.hpFilter(filter150.toInt(), 0)
                        val filter = ParseData.offlineFilter(filterHp.toDouble(), countdown == 120)
                        if (filter.isNotEmpty() && countdown <= 60) {
                            val result = filter.map { item -> ParseData.hpf05(item.toInt(), 0) }
                            allData.addAll(result)
                            allData2.addAll(result)
                        }
                    }
                } else { //新算法
                    val filterData =
                        ParseData.traditionalSingleEcg(i, if (countdown == 120) 1 else 0)
                    if (index % 2 == 0) {
                        if (countdown <= 60) {//前5秒数据丢弃
                            allData.add(filterData - 2048)
                            allData2.add(filterData - 2048)
                        }
                    }
                }
            }

            if (ecgData.frameNum == 0) {
                countdown--
                binding.tvCountdown.text = "${(countdown * 0.084f).toInt()}"
            } else { //30s正式开始后的心电数据
                time++
                binding.tvCountdown.isVisible = false
                binding.tvStart.text = "${(time * 0.075f).toInt()}s/30s"
                dataEcg.addAll(data)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun onECG1Result(result: Int, hr: Int) {
        if (result == 0xee) {
            binding.tvHr.text = "${getString(R.string.heart)}${if (hr in 30..250) hr else "--"}"
            return
        }
        LogUtil.e(dataEcg.size)
        LogUtil.e(allData2.size)
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
        PreviewManager.SAMPLE_RATE = 150
        EcgConfig.SPEED = 150
        val data = allData2.takeLast(4500)
        LogUtil.e(data.size)
        val dataList = Array(12) { ShortArray(4500) }
        dataList[0] =
            data.map { ((it / 355f) / Const.SHORT_MV_GAIN).toInt().toShort() }.toShortArray()
        //回顾
        EcgPlaybackFragemntDialog.newInstance(dataList).show(childFragmentManager, "")
    }

    override fun onPause() {
        super.onPause()
        LogUtil.e("onPause")
        isStart = false
        updateTimer.cancel()
    }
}

