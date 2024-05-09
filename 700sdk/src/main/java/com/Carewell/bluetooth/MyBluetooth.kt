package com.Carewell.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.text.TextUtils
import com.Carewell.ecg700.LogUtil
import java.util.*

/**
 */
class MyBluetooth(context: Context?) {
    private var mDevSN = "" //设备名后缀序列号
    private var mConnectedDevice: BluetoothDevice? = null

    //手动取消搜索
    private var bCancelFind = false

    /**
     * 连接指定的设备
     */
    @SuppressLint("WrongConstant")
    fun startDiscovery(devSN: String) {
        mDevSN = devSN
        if (mBluetoothOper?.bluetoothAdapter != null) {
            blueStatus = BLU_STATUS_NORMAL
            startDiscoveryConn()
        }
    }

    /**
     * 搜索蓝牙设备(经典蓝牙 2.0)
     */
    @SuppressLint("MissingPermission", "WrongConstant")
    private fun startDiscoveryConn() {
        if (blueStatus == BLU_STATUS_NORMAL) {
            mLocalSocket = null
            if (!openBluetooth()) {
                return
            }
            val bondDev = mBluetoothOper?.bondedDevices
            if (bondDev != null && bondDev.size > 0) {
                for (blueDevice in bondDev) {
                    if (checkName(blueDevice.name)) {
                        bDiscovery = false
                        blueStatus = BLU_STATUS_CONNECTING
                        mHandlerListener?.onHandlerCallback(MSG_BLUETOOTH_CONNECTING, -1)
                        mBluetoothOper?.connect(blueDevice)
                        return
                    }
                }
            }
            blueStatus = BLU_STATUS_DISCOVERING
            mHandlerListener?.onHandlerCallback(MSG_BLUETOOTH_DISCOVERYING, -1)
            mBluetoothOper?.discovery()
            bDiscovery = true
        }
    }

    //开启蓝牙打开超时的定时器
    private var openBlueTimer: Timer? = null

    //打开蓝牙是否超时
    private var bOpenBlueTimeOut = false

    // 本次是否有过搜索
    private var bDiscovery = false

    /**
     * 打开手机蓝牙
     */
    private fun openBluetooth(): Boolean {
        bOpenBlueTimeOut = false
        if (mBluetoothOper?.isOpen == false) {
            mHandlerListener?.onHandlerCallback(MSG_BLUETOOTH_OPENING, -1)
            mBluetoothOper?.open()
            openBlueTimer = Timer()
            openBlueTimer?.schedule(object : TimerTask() {
                override fun run() {
                    bOpenBlueTimeOut = true
                    mHandlerListener?.onHandlerCallback(MSG_BLUETOOTH_OPENING_FAIL, -1)
                }
            }, (10 * 1000).toLong())
            while (mBluetoothOper?.isOpen == false && !bOpenBlueTimeOut) {
                try {
                    Thread.sleep(100)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
            openBlueTimer?.cancel()
        }
        return !bOpenBlueTimeOut
    }

    /**
     * 取消搜索
     */
    fun stopDiscovery() {
        if (blueStatus == BLU_STATUS_DISCOVERING || blueStatus == BLU_STATUS_CONNECTING) {
            bDiscovery = true
            mBluetoothOper?.stopDiscovery()
            bCancelFind = true
            bConnected = false
            blueStatus = BLU_STATUS_NORMAL
        }
    }

    fun checkName(devName: String): Boolean {
        return !TextUtils.isEmpty(devName)
                && devName.contains("BC01")
                && devName.length > 4
                && devName.substring(devName.length - 4, devName.length) == mDevSN
    }

    private inner class MyBluetoothCallBack : IBluetoothCallBack {
        override fun onFindDevice(device: BluetoothDevice) {
            @SuppressLint("MissingPermission")
            val devName = device.name ?: return
            LogUtil.d("onFindDevice ->$devName")
            if (checkName(devName)) {
                blueStatus = BLU_STATUS_CONNECTING
                mHandlerListener?.onHandlerCallback(MSG_BLUETOOTH_CONNECTING, -1)
                mBluetoothOper?.connect(device)
            }
        }

        override fun onDiscoveryCompleted(devices: List<BluetoothDevice>) {
            if (blueStatus != BLU_STATUS_CONNECTING && blueStatus != BLU_STATUS_CONNECTED) {
                blueStatus = BLU_STATUS_NORMAL
                mHandlerListener?.onHandlerCallback(MSG_BLUETOOTH_DISCOVERYED, -1)
            }
        }

        @SuppressLint("MissingPermission", "WrongConstant")
        override fun onConnected(socket: BluetoothSocket) {
            blueStatus = BLU_STATUS_CONNECTED
            bConnected = true
            LogUtil.d("onConnected: socket")
            mLocalSocket = socket
            mConnectedDevice = mLocalSocket?.remoteDevice

            mBluetoothOper?.listenConnectLoacalDevice(mConnectedDevice?.name)
            if (bCancelFind) {
                blueStatus = BLU_STATUS_NORMAL
                if (mLocalSocket != null) {
                    mBluetoothOper?.disConnect(mLocalSocket)
                }
                bConnected = false
                bCancelFind = false
            } else {
                mHandlerListener?.onHandlerCallback(MSG_BLUETOOTH_CONNECTED, -1)
            }
        }

        override fun onConnectFail(err: String) {
            LogUtil.d("onConnectFail:$bDiscovery")
            mLocalSocket = null
            if (bDiscovery) {
                blueStatus = BLU_STATUS_NORMAL
                mHandlerListener?.onHandlerCallback(MSG_BLUETOOTH_CONNECT_FAIL, -1)
            } else {
                blueStatus = BLU_STATUS_DISCOVERING
                bDiscovery = true
                mHandlerListener?.onHandlerCallback(MSG_BLUETOOTH_DISCOVERYING, -1)
                mBluetoothOper?.discovery()
            }
        }

        override fun onException(exception: Int) {
            LogUtil.d("exception:$exception")
            when (exception) {
                BluetoothOpertion.ExceptionCode.DISCOVERYTIMEOUT -> {
                    blueStatus = BLU_STATUS_NORMAL
                    mHandlerListener?.onHandlerCallback(MSG_BLUETOOTH_SEARCH_TIMEOUT, -1)
                }

                else -> {}
            }
        }

        override fun onConnectLocalDevice(remoteSocket: BluetoothSocket) {
            mRemoteSocket = remoteSocket
            try {
                Thread.sleep(300)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
//            App.serial.mAPI?.getAllTransData()
        }
    }

    //关闭蓝牙
    fun closeBluetooth() {
        mBluetoothOper?.close()
        mBluetoothOper = null
        mLocalSocket = null
        mRemoteSocket = null
    }

    companion object {

        /* SDK中的蓝牙操作类 */
        @SuppressLint("StaticFieldLeak")
        private var mBluetoothOper: BluetoothOpertion? = null

        /* 蓝牙是否连接成功 */
        var bConnected = false

        /* 本地主动连接上设备的socket*/
        var mLocalSocket: BluetoothSocket? = null

        /* 蓝牙设备主动连接本地的socket*/
        var mRemoteSocket: BluetoothSocket? = null

        /* 正在打开蓝牙 */
        const val MSG_BLUETOOTH_OPENING = 0x00

        /* 搜索设备  */
        const val MSG_BLUETOOTH_DISCOVERYING = 0x01

        /* 正在连接设备 */
        const val MSG_BLUETOOTH_CONNECTING = 0x02

        /* 连接成功  */
        const val MSG_BLUETOOTH_CONNECTED = 0x03

        /*连接失败 */
        const val MSG_BLUETOOTH_CONNECT_FAIL = 0x04

        /* 打开蓝牙失败 */
        const val MSG_BLUETOOTH_OPENING_FAIL = 0x05

        /* 搜索完成  */
        const val MSG_BLUETOOTH_DISCOVERYED = 0x06

        /* 搜索超时  */
        const val MSG_BLUETOOTH_SEARCH_TIMEOUT = 0x07

        /* 当前蓝牙状态——正常 */
        const val BLU_STATUS_NORMAL = 0

        /* 当前蓝牙状态——搜索中  */
        const val BLU_STATUS_DISCOVERING = 1

        /* 当前蓝牙状态——连接中  */
        const val BLU_STATUS_CONNECTING = 3

        /* 当前蓝牙状态——连接上 */
        const val BLU_STATUS_CONNECTED = 4

        /* 当前蓝牙状态 */
        var blueStatus = 0

        /**
         * 获取连接成功的设备
         */
        val conDevice: BluetoothDevice?
            get() = if (mLocalSocket != null) mLocalSocket?.remoteDevice else null
    }

    init {
        mBluetoothOper = BluetoothOpertion(context, MyBluetoothCallBack())
    }

    private var mHandlerListener: HandlerListener? = null

    interface HandlerListener {
        fun onHandlerCallback(what: Int, arg1: Int)
    }

    fun setHandlerListener(okClickListener: HandlerListener?) {
        mHandlerListener = okClickListener

    }
}