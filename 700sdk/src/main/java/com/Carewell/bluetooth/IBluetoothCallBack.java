package com.Carewell.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.util.List;

/**
 * 蓝牙回调方法
 *
 */
public interface IBluetoothCallBack {

	/**
	 * 搜索到设备
	 */
	 void onFindDevice(BluetoothDevice device);

	/**
	 * 搜索完成 搜索到的设备列表
	 */
	 void onDiscoveryCompleted(List<BluetoothDevice> devices);

	/**
	 * 连接成功 连接的socket 可获取输入/输出流
	 */
	 void onConnected(BluetoothSocket socket);

	/**
	 * 连接失败
	 */
	 void onConnectFail(String err);

	/**
	 * 异常
	 */
	 void onException(int exception);
	
	/**
	 * 自身做服务端，有其他的远程设备主动连接本地终端
	 */
	 void onConnectLocalDevice(BluetoothSocket remoteSocket);

}
