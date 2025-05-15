package com.zkteco.android.IDReader

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import androidx.core.content.ContextCompat

/**
 * USB权限申请辅助类
 */
class UsbDevPermissionMgr(
    private val mContext: Context,
    private val mCb: UsbDevPermissionMgrCallback?
) {
    private val mUsbManager: UsbManager = mContext.getSystemService(Context.USB_SERVICE) as UsbManager
    private var mUsbReceiver: UsbReceiver?
    var usbDevice: UsbDevice? = null
    private val TASK = arrayListOf<UsbDevTask>()

    /**
     * USB权限申请回调接口
     */
    interface UsbDevPermissionMgrCallback {
        fun onUsbDevReady(device: UsbDevice?)

        fun onUsbDevRemoved(device: UsbDevice?)

        fun onUsbRequestPermission()
    }

    init {
        TASK.add(UsbDevTask(0x425, 0x8159, "HIDIDR"))
        TASK.add(UsbDevTask(0x400, 0xc35a, "USBIDR"))
        TASK.add(UsbDevTask(0x483, 0xdf11, "DFUIDR"))
        mUsbReceiver = UsbReceiver()
        val filter = IntentFilter()
        filter.addAction(ACTION_USB_PERMISSION)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        ContextCompat.registerReceiver(mContext, mUsbReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    fun initMgr(): Boolean {
        var hasOuterDevice = false
        for (device in mUsbManager.deviceList.values) {
            val task = isUsbTaskDevice(device)
            if (task != null) {
                hasOuterDevice = true
                if (mUsbManager.hasPermission(device)) {
                    this.usbDevice = device
                    mCb?.onUsbDevReady(device)
                } else {
                    usbDevPermissionRequest(device)
                }
                break
            }
        }
        return hasOuterDevice
    }

    fun releaseMgr() {
        if (mUsbReceiver != null) {
            mContext.unregisterReceiver(mUsbReceiver)
            mUsbReceiver = null
        }
    }


    private fun usbDevPermissionRequest(device: UsbDevice?) {
        mCb?.onUsbRequestPermission()
        val permissionIntent = Intent(ACTION_USB_PERMISSION)
        val mPermissionIntent = PendingIntent.getBroadcast(mContext, 0, permissionIntent, PendingIntent.FLAG_IMMUTABLE)
        mUsbManager.requestPermission(device, mPermissionIntent)
    }

    private inner class UsbReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val device = checkNotNull(intent.getParcelableExtra<UsbDevice?>(UsbManager.EXTRA_DEVICE))
            val task = isUsbTaskDevice(device)
            val action = intent.action
            if (task != null) {
                if (ACTION_USB_PERMISSION == action) {
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        mCb?.onUsbDevReady(device)
                    }
                } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED == action) {
                    if (mUsbManager.hasPermission(device)) {
                        mCb?.onUsbDevReady(device)
                    } else {
                        usbDevPermissionRequest(device)
                    }
                } else if (UsbManager.ACTION_USB_DEVICE_DETACHED == action) {
                    mCb?.onUsbDevRemoved(device)
                }
            }
        }
    }

    fun isUsbTaskDevice(dev: UsbDevice): UsbDevTask? {
        var task: UsbDevTask? = null
        val vid = dev.vendorId
        val pid = dev.productId
        for (t in TASK) {
            if ((vid == t.vid) && (pid == t.pid)) {
                task = t
                break
            }
        }
        return task
    }

    inner class UsbDevTask(
        internal val vid: Int,
        internal val pid: Int,
        private val name: String?
    ) {
        override fun toString(): String {
            return String.format("<%04x,%04x> %s", vid, pid, name)
        }
    }

    companion object {
        private const val ACTION_USB_PERMISSION = "com.routon.idr.USB_PERMISSION"
    }
}
