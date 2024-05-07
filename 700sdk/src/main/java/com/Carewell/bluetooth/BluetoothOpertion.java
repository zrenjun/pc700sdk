package com.Carewell.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 蓝牙2.0/3.0操作类
 * 
 * @author zougy
 */
public class BluetoothOpertion {
	private static final String TAG = "BluetoothOpertion";
	
	private BluetoothAdapter mAdapter;
	private Context mContext;
	
	/**
	 * 回调接口
	 */
	private IBluetoothCallBack mCallBack;

	/**
	 * 搜索到的设备列表
	 */
	private List<BluetoothDevice> mDeviceList = new ArrayList<BluetoothDevice>();

	/**
	 * 蓝牙状态————正常
	 */
	public static final int BLUETOOTH_STATUS_NORMAL = 0;

	/**
	 * 蓝牙状态————搜索中
	 */
	public static final int BLUETOOTH_STATUS_DISCOVERING = 1;
	/**
	 * 蓝牙状态————搜索完成
	 */
	public static final int BLUETOOTH_STATUS_DISCOVERYED = 2;

	/**
	 * 蓝牙状态————连接中
	 */
	public static final int BLUETOOTH_STATUS_CONNECTING = 3;
	/**
	 * 蓝牙状态————连接上
	 */
	public static final int BLUETOOTH_STATUS_CONNECTED = 4;

	/**
	 * 蓝牙当前状态
	 */
	private int bluetoothStatus = BLUETOOTH_STATUS_NORMAL;

	public BluetoothOpertion(Context context, IBluetoothCallBack callBack) throws NullPointerException {
		if (context == null || callBack == null) {
			throw new NullPointerException("context or callBack is NULL");
		} else {
			mContext = context;
			mCallBack = callBack;
			mAdapter = BluetoothAdapter.getDefaultAdapter();
			if (mAdapter == null) {
				mCallBack.onException(ExceptionCode.NOBLUETOOTHADAPTER);
			}
		}
	}

	/**
	 * 获取蓝牙适配器
	 * @return
	 */
	public BluetoothAdapter getBluetoothAdapter() {
		return mAdapter;
	}

	/**
	 * 蓝牙是否打开
	 *
	 * @return TRUE 已经打开 FALSE 没有开打
	 */
	public boolean isOpen() {
		if (mAdapter == null)
			return false;
		return mAdapter.isEnabled();
	}

	/**
	 * 打开蓝牙
	 *
	 * @return TRUE 打开成功 FALSE 打开失败
	 */
	public boolean open() {
		if (mAdapter == null)
			return false;
		if (!isOpen()) {
			return mAdapter.enable();
		} else {
			return true;
		}
	}

	/**
	 * 关闭蓝牙
	 *
	 * @return TRUE 关闭成功 FALSE 关闭失败
	 */
	public boolean close() {
		if (mAdapter == null)
			return false;
		if (isOpen()) {
			return mAdapter.disable();
		} else {
			return true;
		}
	}

	/**
	 * 获取已经配对过的蓝牙设备列表
	 */
	public Set<BluetoothDevice> getBondedDevices() {
		if (mAdapter != null) {
			return mAdapter.getBondedDevices();
		}
		return null;
	}

	/**
	 * 开始搜索设备<br>
	 * <li>蓝牙设备开始搜索周围设备，如果搜索到设备则调用
	 * {@link IBluetoothCallBack#OnFindDevice(BluetoothDevice)}<br>
	 * <li>搜索完成后调用该方法 {@link IBluetoothCallBack#OnDiscoveryCompleted(List)}
	 * <li>15秒后如果没有停止搜索则视为超时 {@link IBluetoothCallBack#OnException()}
	 */
	public void discovery() {
		bluetoothStatus = BLUETOOTH_STATUS_NORMAL;
		mDeviceList.clear();
		registerReceiver();
		new Thread(new DiscoveryRunnable(), "discoveryThread").start();
		disTimerOut(true);
	}

	private Thread mDisTimerTh;
	private int mTimerOutCnt = 0;
	private static final int TIMEOUT = 15;
	private boolean bTimeoutIng = true;

	private void disTimerOut(boolean isStart) {
		if (isStart) {
			if (mDisTimerTh == null) {
				bTimeoutIng = true;
				mTimerOutCnt = 0;
				mDisTimerTh = new Thread("discovery timer out") {
					@Override
					public void run() {
						super.run();
						try {
							sleep(1000);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}

						while (bTimeoutIng) {
							if (!mAdapter.isDiscovering() || (mTimerOutCnt >= TIMEOUT)) {
								bTimeoutIng = false;
								break;
							}
							mTimerOutCnt++;
							try {
								sleep(1000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
						unRegisterReceiver();
						mAdapter.cancelDiscovery();
						if (mTimerOutCnt >= TIMEOUT) {
							mCallBack.onException(ExceptionCode.DISCOVERYTIMEOUT);
						} else
							mCallBack.onDiscoveryCompleted(mDeviceList);
						bluetoothStatus = BLUETOOTH_STATUS_NORMAL;
						mDisTimerTh = null;
					}
				};
				mDisTimerTh.start();
			}
		} else {
			if (mDisTimerTh != null) {
				bTimeoutIng = false;
				mTimerOutCnt = 0;
				mDisTimerTh = null;
			}
		}
	}

	/**
	 * 停止搜索
	 */
	public void stopDiscovery() {
		if (bluetoothStatus == BLUETOOTH_STATUS_DISCOVERING
				|| bluetoothStatus == BLUETOOTH_STATUS_CONNECTING ){
			mAdapter.cancelDiscovery();
			bluetoothStatus = BLUETOOTH_STATUS_NORMAL;
		}
	}

	/**
	 * 注册广播接收器
	 */
	private void registerReceiver() {
		if (!bRegister) {
			bRegister = true;
			IntentFilter bluetoothFilter = new IntentFilter();
			bluetoothFilter.addAction(BluetoothDevice.ACTION_FOUND);
			bluetoothFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
			mContext.registerReceiver(bluetoothReceiver, bluetoothFilter);
		}
	}

	/**
	 * 是否注册过
	 */
	private boolean bRegister = false;

	/**
	 * 注销广播接收器
	 */
	private void unRegisterReceiver() {
		if (bRegister) {
			try {
				mContext.unregisterReceiver(bluetoothReceiver);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				bRegister = false;
			}
		}
	}

	/**
	 * 获取搜索到的设备列表
	 *
	 * @return
	 */
	public List<BluetoothDevice> getDeviceList() {
		return mDeviceList;
	}

	/**
	 * 蓝牙广播接收器
	 */
	private BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			//Log.e(TAG, "BluetoothOperation onReceive-->"+action);

			if (action.equals(BluetoothDevice.ACTION_FOUND)) {
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				mDeviceList.add(device);
				//Log.i(TAG, "ACTION_FOUND-->"+device);
				mCallBack.onFindDevice(device);

			} else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
				mCallBack.onDiscoveryCompleted(mDeviceList);
				disTimerOut(false);
				bluetoothStatus = BLUETOOTH_STATUS_DISCOVERYED;
				unRegisterReceiver();
			}
		}
	};

	/**
	 * 搜索蓝牙线程
	 *
	 * @author zougy
	 *
	 */
	private class DiscoveryRunnable implements Runnable {

		@Override
		public void run() {
			if (!isOpen()) {
				mCallBack.onException(ExceptionCode.BLUETOOTHNOTOPEN);
				return;
			}
			bluetoothStatus = BLUETOOTH_STATUS_DISCOVERING;
			mAdapter.startDiscovery();
			return;
		}

	}

	/**
	 * 连接一个蓝牙设备
	 * <p>
	 * 连接成功后调用 {@link IBluetoothCallBack#OnConnected(BluetoothSocket)}
	 * <p>
	 * 连接失败后调用{@link IBluetoothCallBack#OnConnectFail(String)}
	 *
	 * @param device
	 *            需要连接的设备
	 */
	@SuppressLint("NewApi") //connectDevice
	public void connect(BluetoothDevice device) {
		if (bluetoothStatus != BLUETOOTH_STATUS_CONNECTING) {
			if(mSocket!=null){
				//Log.d(TAG, "connectDevice()->mSocket.isConnected():"+mSocket.isConnected());
				if(mSocket.isConnected()){
					try {
						mSocket.close();
					} catch (IOException e) {

						e.printStackTrace();
					}
				}
				mSocket = null;
			}
			new ConnectThread(device).start();
		} else {
			mCallBack.onConnectFail("Connecting");
		}
	}

	/**
	 * 连接设备
	 * <p>
	 * 连接成功后调用 {@link IBluetoothCallBack#OnConnected(BluetoothSocket)}
	 * <p>
	 * 连接失败后调用{@link IBluetoothCallBack#OnConnectFail(String)}
	 * 
	 * @param address
	 *            设备地址
	 */
	public void connect(String address) {
		if (!BluetoothAdapter.checkBluetoothAddress(address)) {
			mCallBack.onConnectFail("the address is invalid");
			return;
		}
		connect(mAdapter.getRemoteDevice(address));
	}

	//蓝牙串口服务
	private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");// ("a60f35f0-b93a-11de-8a39-08002009c666");

	private static BluetoothSocket mSocket;

	/**
	 * 连接线程
	 */
	private class ConnectThread extends Thread {

		private BluetoothDevice mDevice;
		private BluetoothSocket temp = null;
		
		public ConnectThread(BluetoothDevice device) {
			mDevice = device;	

			try {			 
			 /* System.out.println("mDevice.getBondState():" + mDevice.getBondState());
				if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
					ClsUtils.createBond(BluetoothDevice.class, mDevice);
					ClsUtils.cancelPairingUserInput(BluetoothDevice.class, mDevice);
				}  */
				
				
				if (Build.VERSION.SDK_INT >= 10) {
					temp = mDevice.createInsecureRfcommSocketToServiceRecord(MY_UUID);
				} else {
					temp = mDevice.createRfcommSocketToServiceRecord(MY_UUID);
				}
			} catch (Exception e) {
				Log.e(TAG, "ConnectThread exception->"+e.getMessage());
				e.printStackTrace();
			}
			
			// Method m = null;
			// try {
			// m = mDevice.getClass().getMethod("createRfcommSocket",
			// new Class[] { int.class });
			// } catch (NoSuchMethodException e) {
			// e.printStackTrace();
			// }
			//
			// try {
			// temp = (BluetoothSocket) m.invoke(mDevice, Integer.valueOf(1));
			// } catch (IllegalAccessException | IllegalArgumentException
			// | InvocationTargetException e) {
			// e.printStackTrace();
			// }
			
			mSocket = temp;
		}

		@Override
		public void run() {
			super.run();
			setName("Connet thread");
			if (mSocket != null) {
				try {				
					if(mAdapter.isDiscovering()){
						mAdapter.cancelDiscovery();
					}
					//Log.d(TAG, "- mSocket.connect()-");
					bluetoothStatus = BLUETOOTH_STATUS_CONNECTING;		
					mSocket.connect();
					mCallBack.onConnected(mSocket);
					bluetoothStatus = BLUETOOTH_STATUS_CONNECTED;
				} catch (IOException e) {
					e.printStackTrace();
					Log.e(TAG, "mSocket exception->"+e.getMessage());	
					if(mSocket!=null){
						try {
							mSocket.close();
							mSocket = null;
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
					bluetoothStatus = BLUETOOTH_STATUS_NORMAL;
					mCallBack.onConnectFail(e.getMessage());
					
//					//错误：read failed, socket might closed or timeout, read ret: -1 
//					//参考：http://blog.csdn.net/ccc905341846/article/details/52766961
//					try {
//						Log.d(TAG, "sock蓝牙端口异常， 2次反射重连");
//						bluetoothStatus = BLUETOOTH_STATUS_CONNECTING;
//						mSocket =(BluetoothSocket) mDevice.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(mDevice,1);
//						mSocket.connect();
//						bluetoothStatus = BLUETOOTH_STATUS_CONNECTED;
//					} catch (IOException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException e1) {
//						
//						e1.printStackTrace();
//						Log.e(TAG, "sdk mSocket exception->"+e.getMessage());	
//						if(mSocket!=null){
//							try {
//								mSocket.close();
//								mSocket = null;
//							} catch (IOException e2) {
//								e2.printStackTrace();
//							}
//						}
//						bluetoothStatus = BLUETOOTH_STATUS_NORMAL;
//						mCallBack.onConnectFail(e.getMessage());
//					}
					
				}
			}
		}
	}

	/**
	 * 断开一个连接的设备
	 */
	public void disConnect(BluetoothSocket socket) {
		bluetoothStatus = BLUETOOTH_STATUS_NORMAL;
		if (socket != null) {
			try {
				// InputStream is = socket.getInputStream();
				// OutputStream os = socket.getOutputStream();
				// if (is != null) {
				// is.reset();
				// is.close();
				// is = null;
				// }
				// if (os != null) {
				// os.flush();
				// os.close();
				// os = null;
				// }
				socket.close();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				socket = null;
			}
		}
	}

	/**
	 * 获取蓝牙当前的状态
	 * 
	 * @return {@link BluetoothOpertion#BLUETOOTH_STATUS_NORMAL}
	 *         {@link BluetoothOpertion#BLUETOOTH_STATUS_DISCOVERING}
	 *         {@link BluetoothOpertion#BLUETOOTH_STATUS_DISCOVERYED}
	 *         {@link BluetoothOpertion#BLUETOOTH_STATUS_CONNECTING}
	 *         {@link BluetoothOpertion#BLUETOOTH_STATUS_CONNECTED}
	 */
	public int getBluetoothStatus() {
		return bluetoothStatus;
	}
	
	/**
	 * 本地做服务端，监听远程设备主动连接
	 * @author fangrf
	 */
	private AcceptThread mAcceptThread;
	public void listenConnectLoacalDevice(String listenDevName) {
		if(mAcceptThread!=null){
			mAcceptThread.cancel();
			mAcceptThread =null;
		}
		mAcceptThread = new AcceptThread(listenDevName);
		mAcceptThread.start();
	}
		

	/**
	 * 监听远程设备主动连接的线程
	 * @author fangrf
	 */
	private class AcceptThread extends Thread {
		private final BluetoothServerSocket mmServerSocket;  
		   
	    public AcceptThread(String listenDevName) {   
	        BluetoothServerSocket tmp = null;  
	        try {   
	            tmp = mAdapter.listenUsingRfcommWithServiceRecord(listenDevName, MY_UUID);  
	        } catch (IOException e) { 
	        	e.printStackTrace();
	        }  
	        mmServerSocket = tmp;  
	        Log.d(TAG, "sdk 服务已开启");
	    }  
	   
	    public void run() {  
	        BluetoothSocket socket = null;  	         
	        while (true) {  
	            try {  
	                socket = mmServerSocket.accept();  
	            } catch (IOException e) { 
	            	e.printStackTrace();
	            	Log.e(TAG,"sdk ServerSocket:"+ e.getMessage());
	                break;  
	            }  
	            if (socket != null) {  	 
	            	mCallBack.onConnectLocalDevice(socket);
	            }  
	        }  
	    }  
	   
	    public void cancel() {  
	        try {  
	        	if(mmServerSocket!=null){
	        		 mmServerSocket.close(); 
	        	}           
	        } catch (IOException e) { }  
	    }  
	}
	

	/**
	 * 异常代码
	 * 
	 * @author zougy
	 * 
	 */
	public static class ExceptionCode {
		/**
		 * 蓝牙没有打开
		 */
		public static final int BLUETOOTHNOTOPEN = 0;
		/**
		 * 搜索超时
		 */
		public static final int DISCOVERYTIMEOUT = 1;

		/**
		 * 设备不支持蓝牙
		 */
		public static final int NOBLUETOOTHADAPTER = 2;
	}

}
