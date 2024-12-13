package com.lepu.pc700.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import cn.icomon.icdevicemanager.callback.ICScanDeviceDelegate
import cn.icomon.icdevicemanager.model.data.ICWeightData
import cn.icomon.icdevicemanager.model.device.ICScanDeviceInfo
import cn.icomon.icdevicemanager.model.other.ICConstant
import com.Carewell.OmniEcg.jni.toJson
import com.Carewell.ecg700.port.LogUtil
import com.lepu.pc700.MainActivity
import com.lepu.pc700.R
import com.lepu.pc700.databinding.FragmentBodyfatBinding
import com.lepu.pc700.net.vm.BodyFatViewModel
import com.lepu.pc700.singleClick
import com.lepu.pc700.toast
import com.lepu.pc700.viewBinding
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.properties.Delegates

/**
 *
 *  说明: 体脂检测
 *  zrj 2023/11/6 9:36
 *
 */
class BodyFatFragment : Fragment(R.layout.fragment_bodyfat), ICScanDeviceDelegate {
    private val viewModel: BodyFatViewModel by viewModel()
    private val binding: FragmentBodyfatBinding  by viewBinding(FragmentBodyfatBinding::bind)
    private var decimalFormat = DecimalFormat("0.0", DecimalFormatSymbols(Locale.CHINA))
    private var isMeasured = false //是否测量完成
    private var mac = ""
    private var lastWeightData: ICWeightData? = null
    private var isInput: Boolean by Delegates.observable(false) { _, _, newValue ->
        binding.btnInput.text = getString(if (newValue) R.string.bodyfat_scale_measurement else R.string.input)
        binding.tvState.isVisible = !newValue
        binding.tvMac.isVisible = !newValue
        binding.btnBind.isVisible = !newValue
        binding.tv1.setEtFocusable(newValue)
        if (newValue) {
            binding.ctl1.isVisible = true
            binding.group.isVisible = false
            binding.ctl2.isVisible = false
            binding.tv1.setEtText("")
            binding.tv2.setEtText("")
        } else {
            if (isMeasured) {
                binding.ctl1.isVisible = true
                binding.group.isVisible = true
                binding.ctl2.isVisible = false
                lastWeightData?.let {
                    binding.tv1.setEtText(decimalFormat.format(it.weight_kg))
                    binding.tv2.setEtText(decimalFormat.format(it.bmi))
                }
            } else {
                binding.ctl1.isVisible = false
                binding.ctl2.isVisible = true
            }
        }
    }

    private var height = 160
    private var age = 20
    private var isMan = true
    private var bluetoothState = 0


    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as MainActivity).setMainTitle(getString(R.string.body_composition_analysis))
        viewModel.init()
        viewModel.updateUserInfo(height, age, if (isMan) 0 else 1)
        viewModel.bluetoothState.observe(viewLifecycleOwner) {
            bluetoothState = it
            if (it == 2) {
                binding.tvState.text = getString(R.string.bluetooth_connected2)
            } else {
                binding.tvState.text = getString(R.string.bluetooth_not_connected)
                binding.tvHint.text = getString(R.string.scale_hint1)
                binding.tvHint2.text = getString(R.string.scale_hint2)
            }
        }
        viewModel.lastWeightData.observe(viewLifecycleOwner) {
            if (isInput) return@observe
            isMeasured = true
            it?.let {
                lastWeightData = it
                binding.ctl1.isVisible = true
                binding.group.isVisible = true
                binding.ctl2.isVisible = false
                binding.tv1.setEtText(decimalFormat.format(it.weight_kg))
                binding.tv2.setEtText(decimalFormat.format(it.bmi))
                binding.tv3.setEtText(decimalFormat.format(it.bodyFatPercent))
                binding.tv4.setEtText(decimalFormat.format(it.proteinPercent))
                binding.tv5.setEtText(decimalFormat.format(it.smPercent * it.weight_kg / 100f))
                binding.tv6.setEtText(decimalFormat.format(it.bmr))
                binding.tv7.setEtText(decimalFormat.format(it.musclePercent * it.weight_kg / 100f))
                binding.tv8.setEtText(decimalFormat.format(it.visceralFat))
                binding.tv9.setEtText(decimalFormat.format(it.subcutaneousFatPercent))
                binding.tv10.setEtText(decimalFormat.format(it.weightStandard))
                binding.tv11.setEtText(decimalFormat.format(it.moisturePercent))
                binding.tv12.setEtText(decimalFormat.format(it.physicalAge))
                binding.tv13.setEtText(decimalFormat.format(it.boneMass))
                binding.tv14.setEtText(decimalFormat.format((1 - it.bodyFatPercent / 100f) * it.weight_kg))
            }
        }
        viewModel.relWeightData.observe(viewLifecycleOwner) {
            if (isMeasured || isInput) return@observe
            it?.let {
                binding.ctl1.isVisible = false
                binding.ctl2.isVisible = true
                binding.tvHint.text = getString(R.string.measuring)
                binding.tvHint2.text = decimalFormat.format(it.weight_kg)
            }
        }
//        mac = sp().getString("addDevice_macAddress", "") ?: ""
        if (mac.isNotEmpty()) {
            binding.btnBind.text = getString(R.string.unbind)
            binding.tvMac.text = "${getString(R.string.mac)}$mac"
            viewModel.addDevice(mac)
        }
        binding.btnBind.singleClick {
            if (mac.isNotEmpty()) {
                    viewModel.removeDevice(mac) {
                        toast(R.string.the_device_is_unbound_successfully)
                        binding.btnBind.text = getString(R.string.idCard_scan)
                        binding.tvState.text = getString(R.string.bluetooth_not_connected)
                        binding.tvMac.text = "${getString(R.string.mac)}${getString(R.string.unknown2)}"
                        binding.ctl1.isVisible = false
                        binding.ctl2.isVisible = true
                        binding.tvHint.text = getString(R.string.scale_hint1)
                        binding.tvHint2.text = getString(R.string.scale_hint2)
                        mac = ""
//                        sp().putString("addDevice_macAddress", "")
                    }
            } else {
                viewModel.scanDevice(this)
            }
        }

        binding.btnInput.singleClick {
            isInput = !isInput
        }
        //体重
        binding.tv1.setTextChangeListener {
            if (it.isEmpty()) {
                binding.tv1.setTvValue("")
                binding.tv2.setEtText("")
            } else {
                if (isInput) {
                    binding.tv2.setEtText(decimalFormat.format(it.toFloat() / ((height / 100f) * (height / 100f))))
                }
                if (it.toFloat() == 0.0f) return@setTextChangeListener
                setWeightResult(it.toFloat())
            }
        }
        //BMI
        binding.tv2.setTextChangeListener {
            if (it.isEmpty()) {
                binding.tv2.setTvValue("")
            } else {
                if (it.toFloat() == 0.0f) return@setTextChangeListener
                setBmiResult(it.toFloat())
            }
        }
        //体脂率
        binding.tv3.setTextChangeListener {
            if (it.toFloat() == 0.0f) return@setTextChangeListener
            setBodyFatPercentResult(it.toFloat())
        }
        //蛋白率
        binding.tv4.setTextChangeListener {
            if (it.toFloat() == 0.0f) return@setTextChangeListener
            setProteinPercentResult(it.toFloat())
        }
        //骨骼肌量
        binding.tv5.setTextChangeListener {
            if (it.toFloat() == 0.0f) return@setTextChangeListener
            setSmPercentResult(it.toFloat())
        }
        //基础代谢
        binding.tv6.setTextChangeListener {
            if (it.toFloat() == 0.0f) return@setTextChangeListener
            setBmrResult(it.toFloat())
        }
        //肌肉量
        binding.tv7.setTextChangeListener {
            if (it.toFloat() == 0.0f) return@setTextChangeListener
            setMusclePercentResult(it.toFloat())
        }
        //内脏脂肪等级
        binding.tv8.setTextChangeListener {
            if (it.toFloat() == 0.0f) return@setTextChangeListener
            setVisceralFatResult(it.toFloat())
        }
        //皮下脂肪
        binding.tv9.setTextChangeListener {
            if (it.toFloat() == 0.0f) return@setTextChangeListener
            setSubcutaneousFatPercentResult(it.toFloat())
        }
        //水分率
        binding.tv11.setTextChangeListener {
            if (it.toFloat() == 0.0f) return@setTextChangeListener
            setMoisturePercentResult(it.toFloat())
        }
        //身体年龄
        binding.tv12.setTextChangeListener {
            if (it.toFloat() == 0.0f) return@setTextChangeListener
            setPhysicalAgeResult(it.toFloat())
        }
        //骨量
        binding.tv13.setTextChangeListener {
            if (it.toFloat() == 0.0f) return@setTextChangeListener
            setBoneMassResult(it.toFloat())
        }
    }


    override fun onStop() {
        super.onStop()
        viewModel.stopScan()
        viewModel.removeDevice(mac)
    }


    //体重
    private fun setWeightResult(value: Float) {
        val X = (height / 100f) * (height / 100f)
        when {
            value < 18.5f * X -> {
                binding.tv1.setTvValue(getString(R.string.thin))
                binding.tv1.setTvValueColor(colorCompat(R.color.color178AFE))
            }

            18.5f * X <= value && value < 24f * X -> {
                binding.tv1.setTvValue(getString(R.string.standard))
                binding.tv1.setTvValueColor(colorCompat(R.color.color666666))
            }

            24f * X <= value && value < 28f * X -> {
                binding.tv1.setTvValue(getString(R.string.overweight))
                binding.tv1.setTvValueColor(colorCompat(R.color.red))
            }

            value >= 28f * X -> {
                binding.tv1.setTvValue(getString(R.string.corpulent))
                binding.tv1.setTvValueColor(colorCompat(R.color.red))
            }
        }
    }

    //BMI
    private fun setBmiResult(value: Float) {
        when {
            value < 18.5f -> {
                binding.tv2.setTvValue(getString(R.string.low))
                binding.tv2.setTvValueColor(colorCompat(R.color.color178AFE))
            }

            18.5f <= value && value < 25f -> {
                binding.tv2.setTvValue(getString(R.string.standard))
                binding.tv2.setTvValueColor(colorCompat(R.color.color666666))
            }

            25f <= value && value < 30f -> {
                binding.tv2.setTvValue(getString(R.string.high_side))
                binding.tv2.setTvValueColor(colorCompat(R.color.red))
            }

            value >= 30f -> {
                binding.tv2.setTvValue(getString(R.string.high))
                binding.tv2.setTvValueColor(colorCompat(R.color.red))
            }
        }
    }

    //体脂率
    private fun setBodyFatPercentResult(value: Float) {
        var x1 = 0f
        var x2 = 0f
        var x3 = 0f
        var x4 = 0f
        when {
            age <= 39 -> {
                x1 = if (isMan) 10f else 20f
                x2 = if (isMan) 16f else 27f
                x3 = if (isMan) 21f else 34f
                x4 = if (isMan) 26f else 39f

            }

            age in (40..59) -> {
                x1 = if (isMan) 11f else 21f
                x2 = if (isMan) 17f else 28f
                x3 = if (isMan) 22f else 35f
                x4 = if (isMan) 27f else 40f
            }

            age >= 60 -> {
                x1 = if (isMan) 13f else 22f
                x2 = if (isMan) 19f else 29f
                x3 = if (isMan) 24f else 36f
                x4 = if (isMan) 29f else 41f
            }
        }
        when {
            value < x1 -> {
                binding.tv3.setTvValue(getString(R.string.thin))
                binding.tv3.setTvValueColor(colorCompat(R.color.color178AFE))
            }

            x1 <= value && value < x2 -> {
                binding.tv3.setTvValue(getString(R.string.standard1))
                binding.tv3.setTvValueColor(colorCompat(R.color.color666666))
            }

            x2 <= value && value < x3 -> {
                binding.tv3.setTvValue(getString(R.string.standard2))
                binding.tv3.setTvValueColor(colorCompat(R.color.color666666))
            }

            x3 <= value && value < x4 -> {
                binding.tv3.setTvValue(getString(R.string.overweight))
                binding.tv3.setTvValueColor(colorCompat(R.color.red))
            }

            value >= x4 -> {
                binding.tv3.setTvValue(getString(R.string.corpulent))
                binding.tv3.setTvValueColor(colorCompat(R.color.red))
            }
        }
    }

    //蛋白率
    private fun setProteinPercentResult(value: Float) {
        when {
            value < 16 -> {
                binding.tv4.setTvValue(getString(R.string.low))
                binding.tv4.setTvValueColor(colorCompat(R.color.color178AFE))
            }

            16 <= value && value < 20 -> {
                binding.tv4.setTvValue(getString(R.string.standard))
                binding.tv4.setTvValueColor(colorCompat(R.color.color666666))
            }

            value >= 20 -> {
                binding.tv4.setTvValue(getString(R.string.high_side))
                binding.tv4.setTvValueColor(colorCompat(R.color.red))
            }
        }
    }

    //骨骼肌量
    private fun setSmPercentResult(value: Float) {
        var x1 = 0f
        var x2 = 0f
        val y1 = if (isMan) 160 else 150
        val y2 = if (isMan) 170 else 160
        when {
            height < y1 -> {
                x1 = if (isMan) 21.2f else 16f
                x2 = if (isMan) 26.6f else 20.6f
            }

            height in (y1 until y2) -> {
                x1 = if (isMan) 24.8f else 18.9f
                x2 = if (isMan) 34.6f else 23.7f
            }

            height >= y2 -> {
                x1 = if (isMan) 29.6f else 22.1f
                x2 = if (isMan) 43.2f else 30.3f
            }
        }
        when {
            value < x1 -> {
                binding.tv5.setTvValue(getString(R.string.low))
                binding.tv5.setTvValueColor(colorCompat(R.color.color178AFE))
            }

            x1 <= value && value < x2 -> {
                binding.tv5.setTvValue(getString(R.string.standard))
                binding.tv5.setTvValueColor(colorCompat(R.color.color666666))
            }

            value >= x2 -> {
                binding.tv5.setTvValue(getString(R.string.high_side))
                binding.tv5.setTvValueColor(colorCompat(R.color.red))
            }
        }
    }

    //基础代谢
    private fun setBmrResult(value: Float) {
        val weight = binding.tv1.getEtText().toFloat()
        val x1 = when {
            age in (0 until 3) -> if (isMan) 60.9f * weight - 54 else 61f * weight - 51
            age in (3 until 10) -> if (isMan) 22.7f * weight + 495 else 22.5f * weight + 499
            age in (10 until 18) -> if (isMan) 17.5f * weight + 651 else 12.2f * weight + 746
            age in (18 until 30) -> if (isMan) 15.3f * weight + 679 else 14.7f * weight + 496
            age >= 30 -> if (isMan) 11.6f * weight + 879 else 8.7f * weight + 820
            else -> 0f
        }
        binding.tv6.setTvValue(if (value >= x1) getString(R.string.up_to_par) else getString(R.string.not_standard))
        binding.tv6.setTvValueColor(
            if (value >= x1) colorCompat(R.color.color666666) else colorCompat(
                R.color.red
            )
        )
    }

    //肌肉量
    private fun setMusclePercentResult(value: Float) {
        var x1 = 0f
        var x2 = 0f
        val y1 = if (isMan) 160 else 150
        val y2 = if (isMan) 170 else 160
        when {
            height < y1 -> {
                x1 = if (isMan) 38.5f else 21.9f
                x2 = if (isMan) 46.5f else 34.7f
            }

            height in (y1 until y2) -> {
                x1 = if (isMan) 44f else 32.9f
                x2 = if (isMan) 52.4f else 37.5f
            }

            height >= y2 -> {
                x1 = if (isMan) 49.4f else 36.5f
                x2 = if (isMan) 59.4f else 42.5f
            }
        }
        when {
            value < x1 -> {
                binding.tv7.setTvValue(getString(R.string.low))
                binding.tv7.setTvValueColor(colorCompat(R.color.color178AFE))
            }

            x1 <= value && value < x2 -> {
                binding.tv7.setTvValue(getString(R.string.standard))
                binding.tv7.setTvValueColor(colorCompat(R.color.color666666))
            }

            value >= x2 -> {
                binding.tv7.setTvValue(getString(R.string.high_side))
                binding.tv7.setTvValueColor(colorCompat(R.color.red))
            }
        }
    }

    //内脏脂肪指数
    private fun setVisceralFatResult(value: Float) {
        when {
            value <= 4.5f -> {
                binding.tv8.setTvValue(getString(R.string.healthy_type))
                binding.tv8.setTvValueColor(colorCompat(R.color.color178AFE))
            }

            5.0f <= value && value < 9.5f -> {
                binding.tv8.setTvValue(getString(R.string.slightly_more))
                binding.tv8.setTvValueColor(colorCompat(R.color.color666666))
            }

            10f <= value && value < 14.5f -> {
                binding.tv8.setTvValue(getString(R.string.alert))
                binding.tv8.setTvValueColor(colorCompat(R.color.red))
            }

            value >= 15f -> {
                binding.tv8.setTvValue(getString(R.string.hazardous))
                binding.tv8.setTvValueColor(colorCompat(R.color.red))
            }
        }
    }

    //皮下脂肪率
    private fun setSubcutaneousFatPercentResult(value: Float) {
        val x1 = if (isMan) 7f else 11f
        val x2 = if (isMan) 15f else 17f
        when {
            value < x1 -> {
                binding.tv9.setTvValue(getString(R.string.low))
                binding.tv9.setTvValueColor(colorCompat(R.color.color178AFE))
            }

            x1 <= value && value < x2 -> {
                binding.tv9.setTvValue(getString(R.string.standard))
                binding.tv9.setTvValueColor(colorCompat(R.color.color666666))
            }

            value >= x2 -> {
                binding.tv9.setTvValue(getString(R.string.high_side))
                binding.tv9.setTvValueColor(colorCompat(R.color.red))
            }
        }
    }

    //身体水分
    private fun setMoisturePercentResult(value: Float) {
        val x1 = if (isMan) 55f else 45f
        val x2 = if (isMan) 65f else 60f
        when {
            value < x1 -> {
                binding.tv11.setTvValue(getString(R.string.low))
                binding.tv11.setTvValueColor(colorCompat(R.color.color178AFE))
            }

            x1 <= value && value < x2 -> {
                binding.tv11.setTvValue(getString(R.string.standard))
                binding.tv11.setTvValueColor(colorCompat(R.color.color666666))
            }

            value >= x2 -> {
                binding.tv11.setTvValue(getString(R.string.high_side))
                binding.tv11.setTvValueColor(colorCompat(R.color.red))
            }
        }
    }

    //身体年龄
    private fun setPhysicalAgeResult(value: Float) {
        when {
            value < age -> {
                binding.tv12.setTvValue(getString(R.string.excellent))
                binding.tv12.setTvValueColor(colorCompat(R.color.color178AFE))
            }

            age == value.toInt() -> {
                binding.tv12.setTvValue(getString(R.string.standard))
                binding.tv12.setTvValueColor(colorCompat(R.color.color666666))
            }

            value > age -> {
                binding.tv12.setTvValue(getString(R.string.high_side))
                binding.tv12.setTvValueColor(colorCompat(R.color.red))
            }
        }
    }

    //骨量
    private fun setBoneMassResult(value: Float) {
        var x1 = 0f
        var x2 = 0f
        val y1 = if (isMan) 60f else 45f
        val y2 = if (isMan) 75f else 60f
        val weight = binding.tv1.getEtText().toFloat()
        when {
            weight < y1 -> {
                x1 = if (isMan) 2.4f else 1.7f
                x2 = if (isMan) 2.6f else 1.9f
            }

            y1 <= weight && weight < y2 -> {
                x1 = if (isMan) 2.8f else 2.1f
                x2 = if (isMan) 3.0f else 2.3f
            }

            weight >= y2 -> {
                x1 = if (isMan) 3.1f else 2.4f
                x2 = if (isMan) 3.3f else 2.6f
            }
        }
        when {
            value < x1 -> {
                binding.tv13.setTvValue(getString(R.string.low))
                binding.tv13.setTvValueColor(colorCompat(R.color.color178AFE))
            }

            value in x1..x2 -> {
                binding.tv13.setTvValue(getString(R.string.standard))
                binding.tv13.setTvValueColor(colorCompat(R.color.color666666))
            }

            value > x2 -> {
                binding.tv13.setTvValue(getString(R.string.high_side))
                binding.tv13.setTvValueColor(colorCompat(R.color.red))
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onScanResult(deviceInfo: ICScanDeviceInfo) {
        //{"communicationType":"ICDeviceCommunicationTypeConnect","deviceFlag":0,"macAddr":"C0:FE:D0:E0:67:4C","name":"DuoEK 1588","nodeId":0,"rssi":-54,"services":["0000180d-0000-1000-8000-00805f9b34fb"],"st_no":0,"subType":"ICDeviceSubTypeDefault","type":"ICDeviceTypeHR"}
        //{"communicationType":"ICDeviceCommunicationTypeBroadcast","deviceFlag":0,"macAddr":"A0:75:65:02:0A:2D","name":"AAA05D","nodeId":0,"rssi":-58,"services":["0000ffb0-0000-1000-8000-00805f9b34fb"],"st_no":0,"subType":"ICDeviceSubTypeDefault","type":"ICDeviceTypeFatScale"}
       // {"communicationType":"ICDeviceCommunicationTypeConnect","deviceFlag":0,"macAddr":"D0:4D:00:30:B3:8C","name":"Le-F2 Pro","nodeId":0,"rssi":-62,"services":["0000ffb0-0000-1000-8000-00805f9b34fb"],"st_no":0,"subType":"ICDeviceSubTypeDefault","type":"ICDeviceTypeFatScale"}
        if (mac.isEmpty() && deviceInfo.type == ICConstant.ICDeviceType.ICDeviceTypeFatScale) {
            LogUtil.e(deviceInfo.toJson())
            viewModel.stopScan()
            binding.tvMac.text = "${getString(R.string.mac)}${deviceInfo.macAddr}"
            viewModel.addDevice(deviceInfo.macAddr) {
                binding.btnBind.text = getString(R.string.unbind)
                mac = deviceInfo.macAddr
//                sp().putString("addDevice_macAddress", deviceInfo.macAddr)
            }
        }
    }
}

fun Fragment.colorCompat(color: Int) = ContextCompat.getColor(requireContext(), color)