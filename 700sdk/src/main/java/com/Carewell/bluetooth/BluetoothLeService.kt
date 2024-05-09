package com.Carewell.bluetooth

import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.*
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.Carewell.ecg700.LogUtil
import java.util.*

@SuppressLint("NewApi")
class BluetoothLeService : Service() {
    private var mBluetoothManager: BluetoothManager? = null
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mBluetoothDeviceAddress: String? = null
    private var mBluetoothGatt: BluetoothGatt? = null
    private var recvCnt = 0
    private val stringBuilder = StringBuilder()

    private val mGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val intentAction: String
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED
                mConnectionState = STATE_CONNECTED
                broadcastUpdate(intentAction)
                LogUtil.d(  "已连接到 GATT 服务器。")
                // 尝试在成功连接后发现服务。
                LogUtil.d( "尝试启动服务发现:" + mBluetoothGatt?.discoverServices())
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED
                mConnectionState = STATE_DISCONNECTED
                LogUtil.d( "与 GATT 服务器断开连接.")
                broadcastUpdate(intentAction)
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
            } else {
                LogUtil.d( "onServicesDiscovered 已收到: $status")
            }
            val gattChar = getGattCharacteristic(UUID_CHARACTER_WRITE)
            recvCnt = 0
            stringBuilder.delete(0, stringBuilder.length)
            LogUtil.d( "通知,获取onCharacteristic()的通知回调")
            mBluetoothGatt?.setCharacteristicNotification(gattChar, true)
            LogUtil.d( "send cmd >>connetct")
            object : Thread() {
                override fun run() {
                    try {
                        sleep(300)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                    gattChar?.setValue("connect") //获取血脂命令
                    mBluetoothGatt?.writeCharacteristic(gattChar)
                }
            }.start()
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            LogUtil.d(recvCnt.toString() + " 接受到数据 service receive data：" + String(characteristic.value))
            stringBuilder.append(String(characteristic.value))
            if (recvCnt == 2) {
                val intent = Intent(ACTION_SPO2_DATA_AVAILABLE)
                intent.putExtra(EXTRA_DATA, stringBuilder.toString())
                LogUtil.d( "发送 stringBuilder.toString():$stringBuilder")
                sendBroadcast(intent)
                stringBuilder.delete(0, stringBuilder.length)
            }
            recvCnt++
        }
    }

    private fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        sendBroadcast(intent)
    }

    inner class LocalBinder : Binder() {
        val service: BluetoothLeService
            get() = this@BluetoothLeService
    }

    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }

    override fun onUnbind(intent: Intent): Boolean {
        //使用给定设备后，应确保调用 BluetoothGatt.close（）这样资源就会得到适当的清理。 在这个特定示例中，close（） 是当 UI 与服务断开连接时调用。
        close()
        return super.onUnbind(intent)
    }

    private val mBinder: IBinder = LocalBinder()

    /**
     * 初始化对本地蓝牙适配器的引用。
     */
    fun initialize(): Boolean {
        if (mBluetoothManager == null) {
            mBluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
            if (mBluetoothManager == null) {
                LogUtil.d( "Unable to initialize BluetoothManager.")
                return false
            }
        }
        mBluetoothAdapter = mBluetoothManager?.adapter
        if (mBluetoothAdapter == null) {
            LogUtil.d( "Unable to obtain a BluetoothAdapter.")
            return false
        }
        return true
    }

    /**
     * 连接到 Bluetooth LE 设备上托管的 GATT 服务器。
     *
     */
    @SuppressLint("MissingPermission")
    fun connect(address: String?): Boolean {
        if (mBluetoothAdapter == null || address == null) {
            LogUtil.d(  "BluetoothAdapter not initialized or unspecified address.")
            return false
        }

        // 以前连接的设备。 尝试重新连接。
        if (mBluetoothDeviceAddress != null && address == mBluetoothDeviceAddress && mBluetoothGatt != null) {
            LogUtil.d( "Trying to use an existing mBluetoothGatt for connection.")
            return if (mBluetoothGatt?.connect() == true) {
                mConnectionState = STATE_CONNECTING
                true
            } else {
                false
            }
        }
        val device = mBluetoothAdapter?.getRemoteDevice(address)
        if (device == null) {
            LogUtil.d( "Device not found.  Unable to connect.")
            return false
        }
        // 直接连接到设备，设置自动连接 false。
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback)
        LogUtil.d( "Trying to create a new connection.")
        mBluetoothDeviceAddress = address
        mConnectionState = STATE_CONNECTING
        return true
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            LogUtil.d( "BluetoothAdapter not initialized->1")
            return
        }
        mBluetoothGatt?.disconnect()
    }

    /**
     * 使用给定的 BLE 设备后，应用必须调用此方法以确保资源是正确释放。
     */
    @SuppressLint("MissingPermission")
    fun close() {
        if (mBluetoothGatt == null) {
            return
        }
        mBluetoothGatt?.close()
        mBluetoothGatt = null
    }


    fun getGattCharacteristic(characterUUID: UUID?): BluetoothGattCharacteristic? {
        if (mBluetoothAdapter == null) {
            LogUtil.d("BluetoothAdapter not initialized->4")
            return null
        } else if (mBluetoothGatt == null) {
            LogUtil.d("BluetoothGatt not initialized->5")
            return null
        }
        val service = mBluetoothGatt?.getService(UUID_SERVICE_DATA)
        if (service == null) {
            LogUtil.d( "Service is not found!")
            return null
        }
        return service.getCharacteristic(characterUUID)
    }

    companion object {
        const val STATE_DISCONNECTED = 0
        const val STATE_CONNECTING = 1
        const val STATE_CONNECTED = 2
        var mConnectionState = STATE_DISCONNECTED

        // BLE action
        const val ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED"
        const val ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED"
        const val ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED"
        const val ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE"
        const val EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA"
        const val ACTION_FIND_DEVICE: String = "find_device"
        const val ACTION_SEARCH_TIME_OUT: String = "search_timeout"
        const val ACTION_START_SCAN: String = "start_scan"
        const val ACTION_SPO2_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_SPO2_DATA_AVAILABLE"
        //-----UUID ---
        /** service-> uuid  */
        val UUID_SERVICE_DATA = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")

        /** write uuid  */
        var UUID_CHARACTER_WRITE = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")

    }
}