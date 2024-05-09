package com.lepu.pc700

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.TextUtils
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.fragment.app.Fragment
import com.Carewell.ecg700.LogUtil
import com.contec.bc.code.base.ContecDevice
import com.contec.bc.code.bean.ContecBluetoothType
import com.contec.bc.code.callback.BluetoothSearchCallback
import com.contec.bc.code.callback.CommunicateCallback
import com.contec.bc.code.connect.ContecSdk
import com.lepu.pc700.databinding.FragmentUrineBinding
import com.Carewell.bluetooth.MyBluetooth
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 *
 *  说明: 尿液分析
 *  zrj 2024/5/8 10:19
 *
 */
class UrineFragment : Fragment(R.layout.fragment_urine) {

    private val binding by viewBinding(FragmentUrineBinding::bind)
    private var devSN = ""
    private var preTime = 0L

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as MainActivity).setMainTitle(getString(R.string.urinalysis))
        with(binding) {
            tvDevSN.text = devSN
            ivScanBluetooth.singleClick {
                val curTime = System.currentTimeMillis()
                if (curTime - preTime > 3000) {
                    if (TextUtils.isEmpty(devSN)) {
                        showSNInputDialog()
                    } else {
                        tvBluetoothState.text = "--"
                        initBle()
                    }
                    preTime = curTime
                }
            }

            btnUnbond.singleClick {
                //解除SN绑定,可以连接其他尿液分析仪
                if (TextUtils.isEmpty(devSN)) { //没有连接并且  没有输入尿液设备号码
                    toast(resources.getString(R.string.bind_the_device_first))
                } else {
                    toast(resources.getString(R.string.the_device_is_unbound_successfully))
                    tvDevSN.text = ""
                    setText(tvBluetoothState, "--")
                    devSN = ""
                }
            }
            btnClear.singleClick { clearTextView() }
        }
    }

    private var sdk: ContecSdk? = null
    private fun initBle() {
        //初始化蓝牙模块
        sdk = ContecSdk()
        sdk?.init(ContecBluetoothType.TYPE_FF, false)
        //设置设备参数
        sdk?.startBluetoothSearch(bluetoothSearchCallback, 9000)
    }

    private var bluetoothSearchCallback: BluetoothSearchCallback =
        object : BluetoothSearchCallback {
            @SuppressLint("SetTextI18n")
            override fun onSearchError(errorCode: Int) {
                isConnecting = false
                var str = "未知"
                when (errorCode) {
                    0 -> str = "设备不支持 BLE 搜索"
                    1 -> str = "手机蓝牙未开启"
                    2 -> str = "SDK 未初始化"
                }
                launchWhenResumed {
                    binding.tvBluetoothState.text = "搜索错误:$str"
                }
            }

            override fun onContecDeviceFound(contectDevice: ContecDevice) {
                if (contectDevice.name == null) {
                    return
                }
                //示例程序中设备蓝牙名称为BC01打头的字符串
                if (checkName(contectDevice.name, devSN) && contectDevice.type == 2) {
                    LogUtil.e("onContecDeviceFound: ${contectDevice.name}  ${contectDevice.type}")
                    //停止搜索
                    sdk?.stopBluetoothSearch()
                    //设备类型这标注了是单模的还是双模的，经典和双模的就是旧的(1、3)，单模的是新的(2)
                    if (!isConnecting) {
                        isConnecting = true
                        //设置本次通信要获取的数据类型
                        sdk?.setObtainDataType(ContecSdk.ObtainDataType.SINGLE) //ALL, SINGLE
                        //启动连接获取数据
                        sdk?.startCommunicate(requireContext(), contectDevice, communicateCallback)
                        launchWhenResumed {
                            binding.tvBluetoothState.text = getString(R.string.bluetooth_connecting)
                        }
                    }
                }
            }

            override fun onSearchComplete() {
                sdk?.stopBluetoothSearch()
                sdk?.stopCommunicate()
                sdk = null
                initBluetooth()
                launchWhenResumed {
                    binding.tvBluetoothState.text = getString(R.string.bluetooth_discovery_time_out)
                }
            }
        }

    private var myBluetooth: MyBluetooth? = null
    private fun initBluetooth() {
        if (myBluetooth == null){
            myBluetooth = MyBluetooth(requireContext())
        }
        if (!TextUtils.isEmpty(devSN)) {
            myBluetooth?.startDiscovery(devSN)
        }
        myBluetooth?.setHandlerListener(object : MyBluetooth.HandlerListener {
            override fun onHandlerCallback(what: Int, arg1: Int) {
//                val uIData = UIData()
//                uIData.handlerWhat = what
//                uIData.handlerObj = arg1
//                viewModelScope.launch(Dispatchers.Main) {
//                    uiDataMutableLiveData.value = uIData
//                }
            }
        })
    }


    private var jsonArray: JSONArray? = null
    private var isConnecting = false
    private val stringBuffer = StringBuffer()
    var communicateCallback: CommunicateCallback = object : CommunicateCallback {
        override fun onCommunicateIndexSuccess(indexJson: String) {
            //与设备同步数据成功后返回json格式的数据，根据自身需求解析
            stringBuffer.append(indexJson)
            stringBuffer.append("\n")
            isConnecting = false
            //解析Json数据的示例程序
            try {
                jsonArray = JSONObject(indexJson).getJSONArray("IndexData")
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

        override fun onCommunicateSuccess(json: String) {
            //与设备同步数据成功后返回json格式的数据，根据自身需求解析
            stringBuffer.append(json)
            isConnecting = false
            //解析Json数据的示例程序
            try {
                jsonArray = JSONObject(json).getJSONArray("Data")
                for (i in 0 until jsonArray!!.length()) {
                    val jsonObject = jsonArray!!.getJSONObject(i)
                    setText(binding.tvBil, "" + jsonObject.optString("BIL"))
                    setText(binding.tvUro, "" + jsonObject.optString("URO"))
                    setText(binding.tvBld, "" + jsonObject.optString("BLD"))
                    setText(binding.tvKet, "" + jsonObject.optString("KET"))
                    setText(binding.tvLeu, "" + jsonObject.optString("LEU"))
                    setText(binding.tvGlu, "" + jsonObject.optString("GLU"))
                    setText(binding.tvPro, "" + jsonObject.optString("PRO"))
                    setText(binding.tvPh, "" + jsonObject.optString("PH"))
                    setText(binding.tvNit, "" + jsonObject.optString("NIT"))
                    setText(binding.tvSg, "" + jsonObject.optString("SG"))
                    setText(binding.tvVc, "" + jsonObject.optString("VC"))
                    setText(binding.tvTime, "" + jsonObject.optString("Date"))
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

        @SuppressLint("SetTextI18n")
        override fun onCommunicateFailed(errorCode: Int) {
            var str = "连接断开"
            when (errorCode) {
                0 -> str = "不支持的设备类型"
                1 -> str = "创建通信方式失败"
                2 -> str = "重新连接"
                3 -> str = "与设备建立连接失败"
                4 -> str = "获取设备服务失败"
                5 -> str = "开启设备监听失败"
                6 -> str = "数据传输过程中断"
            }
            binding.tvBluetoothState.text = "连接失败-$str"
            isConnecting = false
        }

        override fun onCommunicateProgress(status: Int) {
            when (status) {
                1 -> { //设备连接成功
                    binding.tvBluetoothState.text = getString(R.string.bluetooth_connected)
                    toast(R.string.reading_urine_machine_data)
                }

                5 -> { //设备断开
                    isConnecting = false
                    binding.tvBluetoothState.text = getString(R.string.reconnectinggetdata)
                    sdk?.stopCommunicate()
                    sdk = null
                }
            }
        }
    }

    private fun setText(tv: TextView?, msg: String?) {
        if (tv != null && msg != null) {
            if ("0" == msg || "" == msg || "0.0" == msg) {
                tv.text = "--"
            } else {
                tv.text = msg
            }
        }
    }

    /**
     * 当多台尿液分析仪打开时，一体机不知道会连接哪台，所以加SN，指定连接设备
     */
    private fun showSNInputDialog() {
        val editText = EditText(activity)
        editText.setSingleLine()
        editText.inputType = EditorInfo.TYPE_CLASS_NUMBER
        editText.filters = arrayOf<InputFilter>(LengthFilter(4))
        AlertDialog.Builder(activity)
            .setTitle(getString(R.string.input_urine_sn))
            .setIcon(android.R.drawable.ic_menu_info_details)
            .setView(editText)
            .setPositiveButton(
                getString(R.string.const_ok)
            ) { _, _ ->
                devSN = editText.text.toString()
                if (!TextUtils.isEmpty(devSN) && TextUtils.isDigitsOnly(devSN) && devSN.length == 4) {
                    binding.tvDevSN.text = devSN
                    initBle()
                } else {
                    toast(R.string.input_sn_err)
                }
            }
            .setNegativeButton(getString(R.string.const_cancel), null)
            .show()
    }

    private fun clearTextView() {
        with(binding) {
            setText(tvBil, "--")
            setText(tvUro, "--")
            setText(tvBld, "--")
            setText(tvKet, "--")
            setText(tvLeu, "--")
            setText(tvGlu, "--")
            setText(tvPro, "--")
            setText(tvPh, "--")
            setText(tvNit, "--")
            setText(tvSg, "--")
            setText(tvVc, "--")
            setText(tvTime, "时间--")
            setText(tvBluetoothState, "--")
        }
    }
}


//检索到指定sn号的尿机
fun checkName(devName: String, devSN: String): Boolean {
    return devSN != "" && !TextUtils.isEmpty(devName) && devName.contains("BC01") && devName.length > 4 && devName.substring(
        devName.length - 4,
        devName.length
    ) == devSN
}