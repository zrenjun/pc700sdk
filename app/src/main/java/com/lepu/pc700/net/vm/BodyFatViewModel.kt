package com.lepu.pc700.net.vm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import cn.icomon.icdevicemanager.ICDeviceManager
import cn.icomon.icdevicemanager.ICDeviceManagerDelegate
import cn.icomon.icdevicemanager.callback.ICScanDeviceDelegate
import cn.icomon.icdevicemanager.model.data.ICCoordData
import cn.icomon.icdevicemanager.model.data.ICKitchenScaleData
import cn.icomon.icdevicemanager.model.data.ICRulerData
import cn.icomon.icdevicemanager.model.data.ICSkipData
import cn.icomon.icdevicemanager.model.data.ICWeightCenterData
import cn.icomon.icdevicemanager.model.data.ICWeightData
import cn.icomon.icdevicemanager.model.data.ICWeightHistoryData
import cn.icomon.icdevicemanager.model.device.ICDevice
import cn.icomon.icdevicemanager.model.device.ICDeviceInfo
import cn.icomon.icdevicemanager.model.device.ICUserInfo
import cn.icomon.icdevicemanager.model.other.ICConstant
import cn.icomon.icdevicemanager.model.other.ICDeviceManagerConfig
import com.Carewell.OmniEcg.jni.toJson
import com.Carewell.ecg700.port.LogUtil
import com.lepu.pc700.App
import com.lepu.pc700.net.remote.Repository

/**
 *
 *  说明:
 *  zrj 2022/3/17 14:14
 *
 */
class BodyFatViewModel(private val repository: Repository) : BaseViewModel(), ICDeviceManagerDelegate {
    fun init() {
        val icDeviceManagerConfig = ICDeviceManagerConfig()
        icDeviceManagerConfig.context = App.context
        ICDeviceManager.shared().delegate = this
        ICDeviceManager.shared().initMgrWithConfig(icDeviceManagerConfig)
    }

    fun scanDevice(delegate: ICScanDeviceDelegate) {
        ICDeviceManager.shared().scanDevice(delegate)
    }

    fun stopScan() {
        ICDeviceManager.shared().stopScan()
    }

    fun updateUserInfo(height: Int, age: Int, sex: Int) {
        // 如果没有身高信息，不应该传给秤， 否则秤SDK会使用172去分析成分
        LogUtil.e("updateUserInfo height == $height | age == $age | sex == $sex")
        val userInfo = ICUserInfo()
        userInfo.setHeight(height)
        userInfo.setAge(age)
        userInfo.setWeightUnit(ICConstant.ICWeightUnit.ICWeightUnitKg)
        when (sex) {
            0 -> userInfo.sex = ICConstant.ICSexType.ICSexTypeMale
            1 -> userInfo.sex = ICConstant.ICSexType.ICSexTypeFemal
            2 -> userInfo.sex = ICConstant.ICSexType.ICSexTypeUnknown
        }
        userInfo.peopleType = ICConstant.ICPeopleType.ICPeopleTypeNormal
        ICDeviceManager.shared().updateUserInfo(userInfo)
    }

    /**
     * 移除设备， 成功移除设备后，设备连接会自动断开
     */
    fun removeDevice(macAddress: String, onSuccess: () -> Unit = {}) {
        if (macAddress.isEmpty()) {
            return
        }
        val device = ICDevice()
        device.macAddr = macAddress
        ICDeviceManager.shared().removeDevice(device) { device, code ->
            LogUtil.e("removeDevice  == ${device.macAddr} | code == $code")
            if (code == ICConstant.ICRemoveDeviceCallBackCode.ICRemoveDeviceCallBackCodeSuccess) {
                onSuccess.invoke()
            }
        }
    }

    fun addDevice(macAddress: String, onSuccess: () -> Unit = {}) {
        if (macAddress.isEmpty()) {
            return
        }
        val device = ICDevice()
        device.macAddr = macAddress
        ICDeviceManager.shared().addDevice(device) { device, code ->
            LogUtil.e("addDevice == ${device.macAddr} | code == $code")
            if (code == ICConstant.ICAddDeviceCallBackCode.ICAddDeviceCallBackCodeSuccess) {
                onSuccess.invoke()
            }
        }
    }


    private val _bluetoothState = MutableLiveData<Int>(0)

    /**
     * 蓝牙状态, 0未打开, 1未连接, 2已连接
     */
    val bluetoothState: LiveData<Int>
        get() = _bluetoothState


    //测量结束时的体重数据
    val lastWeightData: LiveData<ICWeightData?>
        get() = _lastWeightData

    private val _lastWeightData = MutableLiveData<ICWeightData?>()

    //实时体重数据
    val relWeightData: LiveData<ICWeightData?>
        get() = _relWeightData
    private val _relWeightData = MutableLiveData<ICWeightData?>()

    override fun onInitFinish(bSuccess: Boolean) {
        LogUtil.e("$bSuccess")
    }

    override fun onBleState(state: ICConstant.ICBleState?) {
        LogUtil.e("onBleState ICBleState == $state")
        if (state == ICConstant.ICBleState.ICBleStatePoweredOn) {
            _bluetoothState.value = 1
        } else if (state == ICConstant.ICBleState.ICBleStatePoweredOff) {
            _bluetoothState.value = 0
        }
    }

    override fun onDeviceConnectionChanged(
        device: ICDevice?,
        state: ICConstant.ICDeviceConnectState?
    ) {
        if (state == ICConstant.ICDeviceConnectState.ICDeviceConnectStateConnected) {
            _bluetoothState.value = 2
            LogUtil.e("ICDeviceConnectStateConnected")
        } else if (state == ICConstant.ICDeviceConnectState.ICDeviceConnectStateDisconnected) {
            _bluetoothState.value = 1
            LogUtil.e("ICDeviceConnectStateDisconnected")
        }
    }

    override fun onNodeConnectionChanged(
        device: ICDevice?,
        nodeId: Int,
        state: ICConstant.ICDeviceConnectState?
    ) {
    }

    /**
     * 实时测量体重数据返回
     */
    override fun onReceiveWeightData(device: ICDevice?, data: ICWeightData) {
        LogUtil.e("OnReceiveWeightData, weightData = $data")
        if (data.isStabilized) {
            _lastWeightData.value = data
        } else {
            _relWeightData.value = data
        }
        _bluetoothState.value = 2
    }

    override fun onReceiveKitchenScaleData(device: ICDevice?, data: ICKitchenScaleData?) {
    }

    override fun onReceiveKitchenScaleUnitChanged(
        device: ICDevice?,
        unit: ICConstant.ICKitchenScaleUnit?
    ) {
    }

    override fun onReceiveCoordData(device: ICDevice?, data: ICCoordData?) {
    }

    override fun onReceiveRulerData(device: ICDevice?, data: ICRulerData?) {
    }

    override fun onReceiveRulerHistoryData(device: ICDevice?, data: ICRulerData?) {
    }

    override fun onReceiveWeightCenterData(device: ICDevice?, data: ICWeightCenterData?) {
    }

    override fun onReceiveWeightUnitChanged(device: ICDevice?, unit: ICConstant.ICWeightUnit?) {
        LogUtil.e("$unit")
    }

    override fun onReceiveRulerUnitChanged(device: ICDevice?, unit: ICConstant.ICRulerUnit?) {
    }

    override fun onReceiveRulerMeasureModeChanged(
        device: ICDevice?,
        mode: ICConstant.ICRulerMeasureMode?
    ) {
    }

    override fun onReceiveMeasureStepData(
        device: ICDevice?,
        step: ICConstant.ICMeasureStep?,
        data: Any
    ) {
        LogUtil.e("device=$device ICMeasureStep=$step")
        step?.let {
            when (it) {
                ICConstant.ICMeasureStep.ICMeasureStepMeasureWeightData -> {
                    val weightData = data as ICWeightData
                    onReceiveWeightData(device, weightData)
                }

                ICConstant.ICMeasureStep.ICMeasureStepMeasureOver -> { // 测量结束
                    val weightData = data as ICWeightData
                    weightData.isStabilized = true
                    onReceiveWeightData(device, weightData)
                }

                else -> {}
            }
        }
    }

    override fun onReceiveWeightHistoryData(device: ICDevice?, data: ICWeightHistoryData?) {
    }

    override fun onReceiveSkipData(device: ICDevice?, data: ICSkipData?) {
    }

    override fun onReceiveHistorySkipData(device: ICDevice?, data: ICSkipData?) {
    }

    override fun onReceiveBattery(device: ICDevice?, battery: Int, ext: Any?) {
        LogUtil.e("$battery")
    }

    override fun onReceiveUpgradePercent(
        device: ICDevice?,
        status: ICConstant.ICUpgradeStatus?,
        percent: Int
    ) {
    }

    //固件升级
    override fun onReceiveDeviceInfo(device: ICDevice?, deviceInfo: ICDeviceInfo) {
        LogUtil.e(deviceInfo.toJson())
    }

    override fun onReceiveDebugData(device: ICDevice?, type: Int, obj: Any?) {
    }

    override fun onReceiveConfigWifiResult(
        device: ICDevice?,
        state: ICConstant.ICConfigWifiState?
    ) {
    }

    override fun onReceiveHR(device: ICDevice?, hr: Int) {
        LogUtil.e("$hr")
    }

    override fun onReceiveUserInfo(device: ICDevice?, userInfo: ICUserInfo) {
        LogUtil.e(userInfo.toJson())
    }

    override fun onReceiveUserInfoList(p0: ICDevice?, p1: MutableList<ICUserInfo>) {
        LogUtil.e(p1.toJson())
    }

    override fun onReceiveRSSI(device: ICDevice?, rssi: Int) {
        LogUtil.e("$rssi")
    }
}