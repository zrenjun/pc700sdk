package com.lepu.pc700.fragment

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.Carewell.OmniEcg.jni.toJson
import com.Carewell.ecg700.port.LogUtil
import com.lepu.pc700.App
import com.lepu.pc700.MainActivity
import com.lepu.pc700.R
import com.lepu.pc700.databinding.FragmentIdcardinputBinding
import com.lepu.pc700.singleClick
import com.lepu.pc700.viewBinding
import com.routon.plsy.reader.sdk.common.ErrorCode
import com.routon.plsy.reader.sdk.common.Info.IDCardInfo
import com.routon.plsy.reader.sdk.usb.USBImpl
import com.zkteco.android.IDReader.UsbDevPermissionMgr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext


class NewIdCardFragment : Fragment(R.layout.fragment_idcardinput) {

    private val binding by viewBinding(FragmentIdcardinputBinding::bind)
    private var bScan: Boolean = false

    private var mUsbCallBack = object : UsbDevPermissionMgr.UsbDevPermissionMgrCallback {

        override fun onUsbDevReady(device: UsbDevice?) {
            LogUtil.e("onUsbDevReady")
        }

        override fun onUsbDevRemoved(device: UsbDevice?) {
            LogUtil.e("onUsbDevRemoved")
        }

        override fun onUsbRequestPermission() {
            LogUtil.e("onUsbRequestPermission")
        }
    }

    private var mUsbPermissionMgr: UsbDevPermissionMgr? = null
    private var mReader: USBImpl? = null

    @OptIn(InternalCoroutinesApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as MainActivity).setMainTitle("身份证识别")
        App.serial.mAPI?.setIDCard_StartScan()
        with(binding) {
            btnIdcardScan.singleClick {
                bScan = !bScan
                if (bScan) {
                    btnIdcardScan.text = "停止扫描"
                    launchWhenResumed {
                        withContext(Dispatchers.IO) {
                            usbRead()
                        }
                    }
                } else {
                    btnIdcardScan.text = "开始扫描"
                    //读卡结束，关闭读卡器
                    mReader?.SDT_ClosePort()
                }
            }
        }
    }

    private suspend fun usbRead() {
        //初始化USB设备对象
        val mUsbMgr = requireContext().getSystemService(Context.USB_SERVICE) as UsbManager
        mUsbPermissionMgr = UsbDevPermissionMgr(requireContext(), mUsbCallBack)
        //检查是否读卡器是否已连接
        if (mUsbPermissionMgr?.initMgr() == true) {
            mReader = USBImpl()
            mUsbPermissionMgr?.usbDevice?.let {
                if (mReader?.SDT_OpenPort(mUsbMgr, it) == ErrorCode.SUCCESS) {
                    // 打开设备成功，找卡
                    val sn = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
                    val timeout = 10000 // 超时时间，单位：毫秒
                    val startTime = System.currentTimeMillis()
                    // 轮询查找身份证
                    while (mReader?.SDT_FindIDCard(sn) != ErrorCode.SUCCESS) {
                        if (System.currentTimeMillis() - startTime > timeout) {
                            LogUtil.e("查找身份证超时")
                            return
                        }
                        // 短暂延迟，避免 CPU 占用过高
                        LogUtil.e("查找身份证中...")
                        delay(100)
                    }
                    // 重置超时时间
                    val selectStartTime = System.currentTimeMillis()
                    // 轮询选择身份证
                    while (mReader?.SDT_SelectIDCard(sn) != ErrorCode.SUCCESS) {
                        if (System.currentTimeMillis() - selectStartTime > timeout) {
                            LogUtil.e("选择身份证超时")
                            return
                        }
                        LogUtil.e("选择身份证中...")
                        delay(100)
                    }
                    // 重置超时时间
                    val readStartTime = System.currentTimeMillis()
                    val iDCardInfo = IDCardInfo()
                    // 轮询读取身份证基本信息
                    while (mReader?.RTN_ReadBaseMsg(iDCardInfo) != ErrorCode.SUCCESS) {
                        if (System.currentTimeMillis() - readStartTime > timeout) {
                            LogUtil.e("读取身份证信息超时")
                            return
                        }
                        LogUtil.e("读取身份证信息中...")
                        delay(100)
                    }

                    // 读卡成功，卡的基本信息、照片已经解析并存储到iDCardInfo中
                    LogUtil.e(iDCardInfo.toJson())
                    withContext(Dispatchers.Main) {
                        binding.btnIdcardScan.performClick()
                        binding.etName.setText(iDCardInfo.name)
                        binding.etIdCardNo.setText(iDCardInfo.id)
                        binding.iv.setImageBitmap(iDCardInfo.photo)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        App.serial.mAPI?.setIDCard_StopScan()
        //释放USB对象
        mUsbPermissionMgr?.releaseMgr()
    }
}

