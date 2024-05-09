package com.lepu.pc700

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Html
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.lepu.pc700.databinding.FragmentBloodBinding
import com.Carewell.bluetooth.BLEManager
import com.Carewell.bluetooth.BluetoothLeService
import net.litcare.lplibrary.bf.BFRecordHelper
import net.litcare.lplibrary.bf.BFType


/**
 *
 *  说明: 血脂检测分析
 *  zrj 2024/5/8 11:38
 *
 */
class BloodFragment : Fragment(R.layout.fragment_blood) {
    private val binding by viewBinding(FragmentBloodBinding::bind)
    private var intUnit = 0 //0="mmol/L",1= "mg/dL"
    private var mManager: BLEManager? = null
    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as MainActivity).setMainTitle("血脂测量")

        with(binding) {
            tvCholHdl.text = "≥0  ,<3.5"
            btnBlueScan.singleClick {
                mManager?.scanLeDevice(true)
            }
            btnClear.singleClick {
                clearTextView()
                mManager?.disconnect()
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
                    Html.fromHtml("说明:<br><br>" +
                            "总胆固醇（CHOL）：测量范围：2.59~12.93mmol/L<br>" +
                            "甘油三酯（TRIG）：测量范围：0.51~7.34mmol/L<br>" +
                            "高密度脂蛋白（HDL）：测量范围：0.39~2.59mmol/L<br>" +
                            "低密度脂蛋白（LDL）：测量范围：1.29~4.91mmol/L<br><br>" +
                            "绑定MAC地址：绑定血脂仪的MAC地址，将血脂仪与生理参数检测仪绑定连接。（MAC地址为自动绑定）<br><br>" +
                            "解绑MAC地址：解绑血脂仪与生理参数检测仪的绑定连接。<br><br>" +
                            "蓝牙连接：当血脂仪测量完并显示结果后，点击此按钮连接血脂仪与生理参数检测仪，可获取数据至生理参数检测仪，数据将显示在检测仪的屏幕上。<br><br>" +
                            "清屏：清除显示在生理参数检测仪屏幕上的数据。<br><br>" +
                            "操作视频：血脂分析仪简介、操作说明视频。<br><br>" +
                            "手动输入：手动输入血脂仪测得的相应数据。<br>")
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
        initBLE()
        android6_RequestLocation(requireContext())
        requireActivity().registerReceiver(mGattUpdateReceiver, BLEManager.makeGattUpdateIntentFilter())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().unregisterReceiver(mGattUpdateReceiver)
        mManager?.scanLeDevice(false)
    }

    private val mGattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            when {
                BluetoothLeService.ACTION_GATT_CONNECTED == action -> {
                    binding.tvBlueStatus.text = getString(R.string.bluetooth_connected)
                }

                BluetoothLeService.ACTION_GATT_DISCONNECTED == action -> {
                    mManager?.closeService()
                    binding.tvBlueStatus.text = getString(R.string.bluetooth_discon)
                }

                BluetoothLeService.ACTION_SPO2_DATA_AVAILABLE == action -> {
                    intent.getStringExtra(BluetoothLeService.EXTRA_DATA)?.let {
                        getBloodFatByJar(it)
                    }
                }

                BluetoothLeService.ACTION_FIND_DEVICE == action -> {
                    binding.tvBlueStatus.text = getString(R.string.find_device_and_connect)
                }

                BluetoothLeService.ACTION_SEARCH_TIME_OUT == action -> {
                    binding.tvBlueStatus.text = getString(R.string.bluetooth_discovery_time_out)
                }

                BluetoothLeService.ACTION_START_SCAN == action -> {
                    binding.tvBlueStatus.text = getString(R.string.bluetooth_discoverying)
                }
            }
        }
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

    @SuppressLint("MissingPermission")
    private fun initBLE() {
        val bluetoothManager =
            requireActivity().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val mBluetoothAdapter = bluetoothManager.adapter
        mBluetoothAdapter.enable()
        mManager = BLEManager(requireContext(), mBluetoothAdapter)
    }

    @SuppressLint("SetTextI18n")
    private fun getBloodFatByJar(data: String): String {
        if (data.length == 44) {
            try {
                val day = data.substring(0, 14)
                val unit = data.substring(40, 41) //0为mmol/L 1为mg/dL
                val time = (day.substring(0, 4) + "-" + day.substring(4, 6) + "-" + day.substring(
                    6,
                    8
                ) + " "
                        + day.substring(8, 10) + ":" + day.substring(10, 12) + ":" + day.substring(
                    12,
                    14
                ))
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
                toast(getString(R.string.data_exception))
            }
        }
        return "error"
    }

    /**
     * android6.0 Bluetooth, need to open location for bluetooth scanning
     * android6.0 蓝牙扫描需要打开位置信息
     */
    private fun android6_RequestLocation(context: Context) {
        if (requireContext().packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
            && !isGpsEnable(context)
        ) {
            val builder = AlertDialog.Builder(context)
            builder.setCancelable(false)
            builder.setTitle("Prompt")
                .setIcon(android.R.drawable.ic_menu_info_details)
                .setMessage("Android6.0 need to open location for bluetooth scanning")
                .setNegativeButton(
                    "CANCEL"
                ) { dialog, _ -> dialog.dismiss() }.setPositiveButton(
                    "OK"
                ) { _, _ ->
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    context.startActivity(intent)
                }
            builder.show()
        }

        //request permissions
        val checkCallPhonePermission =
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        if (checkCallPhonePermission != PackageManager.PERMISSION_GRANTED) {
            //判断是否需要 向用户解释，为什么要申请该权限
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    requireActivity(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            ) Toast.makeText(
                context,
                "need to open location info for discovery bluetooth device in android6.0 version，otherwise find not！",
                Toast.LENGTH_LONG
            ).show()
            //请求权限
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                0
            )
        }
    }

    // whether or not location is open, 位置是否打开
    private fun isGpsEnable(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        return gps || network
    }
}


