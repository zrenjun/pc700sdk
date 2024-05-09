package com.Carewell.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.LeScanCallback
import android.bluetooth.BluetoothDevice
import android.content.*
import android.os.Handler
import android.os.IBinder
import com.Carewell.ecg700.LogUtil

class BLEManager(private val mContext: Context, adapter: BluetoothAdapter) {
    private val mBluetoothAdapter: BluetoothAdapter = adapter
    private var mTargetDevice: BluetoothDevice? = null
    private var mBluetoothLeService: BluetoothLeService? = null
    private var bScanning = false
    private val mHandler: Handler = Handler()
    private var bBindServ = false
    private var longtime = 0L

    @SuppressLint("MissingPermission")
    fun scanLeDevice(enable: Boolean) {
        if (enable) {
            mHandler.postDelayed({
                mBluetoothAdapter.stopLeScan(mLeScanCallback)
                if (bScanning) {
                    broadcastUpdate(BluetoothLeService.ACTION_SEARCH_TIME_OUT)
                    LogUtil.json("===============" + "search time out!")
                }
            }, SCAN_PERIOD)
            bScanning = true
            broadcastUpdate(BluetoothLeService.ACTION_START_SCAN)
            mBluetoothAdapter.startLeScan(mLeScanCallback)
        } else {
            bScanning = false
            mBluetoothAdapter.stopLeScan(mLeScanCallback)
        }
    }

    private val mLeScanCallback: LeScanCallback = object : LeScanCallback {
        @SuppressLint("MissingPermission")
        override fun onLeScan(device: BluetoothDevice, rssi: Int, scanRecord: ByteArray) {
            try {
                if (device.name != null) {
                    longtime = System.currentTimeMillis()
                    if (device.name.contains("LPM311")) {
                        LogUtil.d( "find-->${device.name}")
                        if (device.address != null) {
                            mTargetDevice = device
                            scanLeDevice(false)
                            broadcastUpdate(BluetoothLeService.ACTION_FIND_DEVICE)
                            // start BluetoothLeService
                            synchronized(this) {
                                bBindServ = true
                                mContext.bindService(
                                    Intent(mContext, BluetoothLeService::class.java),
                                    mServiceConnection,
                                    Context.BIND_AUTO_CREATE
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun disableEnable() {
        val nowtime = System.currentTimeMillis()
        if (nowtime - longtime > 60 * 1000) {//超过1分钟就重新启动吧
            mBluetoothAdapter.disable()
            mBluetoothAdapter.enable()
        }
    }

    // Code to manage Service lifecycle.
    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            mBluetoothLeService = (service as BluetoothLeService.LocalBinder).service
            if (mBluetoothLeService?.initialize() == false) {
                return
            }
            mBluetoothLeService?.connect(mTargetDevice?.address)
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            mBluetoothLeService = null
        }
    }

    fun closeService() {
        synchronized(this) {
            if (bBindServ) {
                mContext.unbindService(mServiceConnection)
                bBindServ = false
            }
            if (mBluetoothLeService != null) {
                mBluetoothLeService!!.close()
                mBluetoothLeService = null
            }
            LogUtil.d("-- closeService --")
        }
    }

    /**
     * 断开连接
     */
    fun disconnect() {
        mBluetoothLeService?.disconnect()
    }

    private fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        mContext.sendBroadcast(intent)
    }

    companion object {
        private const val SCAN_PERIOD = 10000L

        /**
         * 自定义过滤器
         * custom intentFilter
         */
        fun makeGattUpdateIntentFilter(): IntentFilter {
            val intentFilter = IntentFilter()
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
            intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
            intentFilter.addAction(BluetoothLeService.ACTION_SPO2_DATA_AVAILABLE)
            intentFilter.addAction(BluetoothLeService.ACTION_FIND_DEVICE)
            intentFilter.addAction(BluetoothLeService.ACTION_SEARCH_TIME_OUT)
            intentFilter.addAction(BluetoothLeService.ACTION_START_SCAN)
            return intentFilter
        }
    }
}