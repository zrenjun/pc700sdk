package com.lepu.pc700

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.view.View
import androidx.bluetooth.BluetoothDevice
import androidx.bluetooth.BluetoothLe
import androidx.bluetooth.GattCharacteristic
import androidx.bluetooth.GattClientScope
import androidx.bluetooth.ScanFilter
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.Carewell.ecg700.LogUtil
import com.lepu.pc700.databinding.FragmentBloodBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.litcare.lplibrary.bf.BFRecordHelper
import net.litcare.lplibrary.bf.BFType
import java.util.UUID


/**
 *
 *  说明: 血脂检测分析
 *  zrj 2024/5/8 11:38
 *
 */
class BloodFragment : Fragment(R.layout.fragment_blood) {
    private val binding by viewBinding(FragmentBloodBinding::bind)
    private var intUnit = 0 //0="mmol/L",1= "mg/dL"

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as MainActivity).setMainTitle("血脂测量")
        bluetoothLe = BluetoothLe(requireContext())
        with(binding) {
            tvCholHdl.text = "≥0  ,<3.5"
            btnBlueScan.singleClick { startScanning() }
            btnClear.singleClick {
                clearTextView()
                stopScanning()
                disconnect()
            }
            btnVideo.singleClick {
                val uri = Uri.parse("https://mp.weixin.qq.com/s/f0J6Q49hmowmkZ5mncc23A")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            }
            tvBloodfatUnit.singleClick {
                val items = arrayOf("mmol/L", "mg/dL")
                AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.setbloodfatuntil))
                    .setItems(items) { _, which ->
                        intUnit = which
                        tvBloodfatUnit.text = items[which]
                        switchUnit(which)
                    }.show()
            }
            btnInfo.singleClick {
                val bloodMsg =
                    Html.fromHtml(
                        "说明:<br><br>" +
                                "总胆固醇（CHOL）：测量范围：2.59~12.93mmol/L<br>" +
                                "甘油三酯（TRIG）：测量范围：0.51~7.34mmol/L<br>" +
                                "高密度脂蛋白（HDL）：测量范围：0.39~2.59mmol/L<br>" +
                                "低密度脂蛋白（LDL）：测量范围：1.29~4.91mmol/L<br><br>" +
                                "绑定MAC地址：绑定血脂仪的MAC地址，将血脂仪与生理参数检测仪绑定连接。（MAC地址为自动绑定）<br><br>" +
                                "解绑MAC地址：解绑血脂仪与生理参数检测仪的绑定连接。<br><br>" +
                                "蓝牙连接：当血脂仪测量完并显示结果后，点击此按钮连接血脂仪与生理参数检测仪，可获取数据至生理参数检测仪，数据将显示在检测仪的屏幕上。<br><br>" +
                                "清屏：清除显示在生理参数检测仪屏幕上的数据。<br><br>" +
                                "操作视频：血脂分析仪简介、操作说明视频。<br><br>" +
                                "手动输入：手动输入血脂仪测得的相应数据。<br>"
                    )
                val alertDialog = AlertDialog.Builder(requireContext()).also {
                    it.setMessage(bloodMsg)
                        .setTitle("提示")
                    it.setPositiveButton("确定") { dialog, _ ->
                        dialog.dismiss()
                    }
                }.create()
                alertDialog.setCanceledOnTouchOutside(false)
                alertDialog.setCancelable(false)
                alertDialog.show()
            }
        }
        switchUnit(0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopScanning()
        disconnect()
    }


    @SuppressLint("SetTextI18n")
    private fun switchUnit(index: Int) {
        if (index == 0) {
            binding.tvCholScope.text = "2.59~12.93 ，<5.17" //总胆固醇 <5.17
            binding.tvHdlScope.text = "0.39~2.59 ，>1.03" //高密度 1.03~1.55
            binding.tvTrigScope.text = "0.51~7.34 ，<1.7" //甘油  <1.7
            binding.tvLdlScope.text = "0~12.93 ，<3.37" //低密度
        } else {
            binding.tvCholScope.text = "100~500 ，<200" // <200
            binding.tvHdlScope.text = "15~100 ，>40" // 40~60
            binding.tvTrigScope.text = "45~650 ，<150" //<150
            binding.tvLdlScope.text = "0~500 ，<130"
        }
    }

    private fun clearTextView() {
        binding.etCHOL.setText("")
        binding.etHDL.setText("")
        binding.etLDL.setText("")
        binding.etTRIG.setText("")
        binding.etRate.setText("")
        binding.tvBloodfatUnit.text = "- -"
        binding.tvTime.text = "- -"
        binding.tvBlueStatus.text = "- -"
    }


    @SuppressLint("SetTextI18n")
    private fun getBloodFatByJar(data: String) {
        LogUtil.e(data)
        if (data.length == 44) {
            try {
                val day = data.substring(0, 14)
                val unit = data.substring(40, 41) //0为mmol/L 1为mg/dL
                val time = day.substring(0, 4) + "-" + day.substring(4, 6) + "-" + day.substring(
                    6,
                    8
                ) + " " + day.substring(8, 10) + ":" + day.substring(10, 12) + ":" + day.substring(
                    12,
                    14
                )
                val bfRecordHelper: BFRecordHelper = BFRecordHelper.parseFromBTResult(data)
                binding.tvTime.text = time
                val sChol = bfRecordHelper.getValueString(BFType.CHOL)
                val sTrig = bfRecordHelper.getValueString(BFType.TRIG)
                val sHdl = bfRecordHelper.getValueString(BFType.HDL)
                val sLdl = bfRecordHelper.getValueString(BFType.LDL)
                val rate = bfRecordHelper.getValueString(BFType.CHOL_HDL)
                binding.etCHOL.setText(sChol)
                binding.etTRIG.setText(sTrig)
                binding.etHDL.setText(sHdl)
                binding.etLDL.setText(sLdl)
                binding.etRate.setText(rate)
                if ("0" == unit) {
                    binding.tvBloodfatUnit.text = "mmol/L"
                    switchUnit(0)
                } else {
                    binding.tvBloodfatUnit.text = "mg/dL"
                    switchUnit(1)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                toast(e.message.toString())
            }
        }
    }


    /** service-> uuid  */
    private val UUID_SERVICE_DATA = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")

    /** write uuid  */
    private var UUID_CHARACTER_WRITE = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
    private var bluetoothLe: BluetoothLe? = null

    private var scanJob: Job? = null

    @SuppressLint("MissingPermission")
    private fun startScanning() {
        binding.tvBlueStatus.text = getString(R.string.bluetooth_discoverying)
        scanJob = lifecycleScope.launch {
            try {
                bluetoothLe?.scan(listOf(ScanFilter(deviceName = "LPM311")))?.collect {
                    binding.tvBlueStatus.text = getString(R.string.find_device_and_connect)
                    connect(it.device)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun stopScanning() {
        scanJob?.cancel()
        scanJob = null
    }

    private var connectJob: Job? = null

    //一旦设备连接，每个通信都会通过这个对象
    private var gattClient: GattClientScope? = null
    private var gattCharacteristic: GattCharacteristic? = null

    @SuppressLint("MissingPermission")
    private fun connect(device: BluetoothDevice) {
        connectJob = lifecycleScope.launch {
            try {
                bluetoothLe?.connectGatt(device) {
                    gattClient = this
                    gattCharacteristic = getService(UUID_SERVICE_DATA)?.getCharacteristic(UUID_CHARACTER_WRITE)
                    binding.tvBlueStatus.text = getString(R.string.bluetooth_connected)
                    read()
                    delay(300)
                    write()
                    awaitCancellation()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun disconnect() {
        connectJob?.cancel()
        connectJob = null
    }

    private fun read() {
        launchWhenResumed {
            gattClient?.apply {
                val stringBuilder = StringBuilder()
                var recCnt = 0
                gattCharacteristic?.let {
                    subscribeToCharacteristic(it).collect { bytes ->
                        LogUtil.e(String(bytes))
                        stringBuilder.append(String(bytes))
                        if (recCnt == 2) {
                            getBloodFatByJar(stringBuilder.toString())
                        }
                        recCnt++
                    }
                }
            }
        }
    }

    private fun write() {
        launchWhenResumed {
            gattClient?.apply {
                gattCharacteristic?.let {
                    val result = writeCharacteristic(it, "connect".toByteArray()) //获取血脂命令
                    if (result.isSuccess) {
                        LogUtil.e("write success")
                    }
                }
            }
        }
    }
}


