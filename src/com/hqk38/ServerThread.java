package com.hqk38;

import java.io.IOException;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;

public class ServerThread extends Thread {
	private BluetoothServerSocket mmServerSocket;
	private BluetoothAdapter mBluetoothAdapter;
	 /** 线程中通信的更新UI的Handler*/    
	private Handler mHandler;
	 /** 监听到有客户端连接，新建一个线程单独处理，不然在此线程中会堵塞*/   
	ConnectedThread mConnectedThread;
	private boolean listening = true;

	public ServerThread(BluetoothAdapter adapter, Handler handler) throws IOException {
  	 	mBluetoothAdapter = adapter;
  		mHandler = handler;
    	// 获取服务端蓝牙socket       
		mmServerSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(mBluetoothAdapter.getName(), UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
	}
	 
	@Override
	public void run() {
		super.run();
		BluetoothSocket socket = null; // 连接客户端的socket
		mHandler.sendEmptyMessage(Constant.MSG_START_LISTENING); // 通知主线程更新UI，客户端开始监听
		while (listening){ // 服务端是不退出的，要一直监听连接进来的客户端，所以是死循环
			if (listening) {
    			try {
    				socket = mmServerSocket.accept(); // 获取连接的客户端socket
      		  	} catch (IOException e) {
      		  		mHandler.sendEmptyMessage(Constant.MSG_FINISH_LISTENING); // 通知主线程更新UI，获取异常
      		  		e.printStackTrace();
      		  		break; // 服务端退出一直监听线程
      		  	}
			} else {
				try {
					if (mmServerSocket != null) {
						mmServerSocket.close();
						mmServerSocket = null;
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				break;
			}
			if (socket != null) {
				mBluetoothAdapter.cancelDiscovery();
				manageConnectSocket(socket); // 管理连接的客户端socket
				// 这里应该是手动断开，案例应该是只保证连接一个客户端，所以连接完以后，关闭了服务端socket
				try {
					if (mmServerSocket != null) {
						mmServerSocket.close();
						mmServerSocket = null;
					}
					mHandler.sendEmptyMessage(Constant.MSG_FINISH_LISTENING);
				} catch (IOException e) {
					e.printStackTrace();           
				}
				break; // 连上了客户端，停止监听
			}
		}
	}
	// 管理连接的客户端socket
	private void manageConnectSocket(BluetoothSocket socket) {
		// 只支持同时处理一个连接
		if (mConnectedThread != null) {
			// mConnectedThread不为空，踢掉之前的客户端
			mConnectedThread.cancel();
			mConnectedThread = null;
		}
		// 主线程更新UI，连接到了一个客户端
		Message message = mHandler.obtainMessage(Constant.MSG_GOT_A_CLIENT, socket.getRemoteDevice()); 
 		mHandler.sendMessage(message);
		// 新建一个线程，处理客户端发来的数据
		mConnectedThread = new ConnectedThread(socket, mHandler);
		mConnectedThread.start();
	}
	
	// 断开服务端，结束监听
	public void cancel() {
		try {
			if (mmServerSocket != null) {
				mmServerSocket.close();
				mmServerSocket = null;
			}
			if (mConnectedThread != null) {
				// mConnectedThread不为空，踢掉之前的客户端
				mConnectedThread.cancel();
				mConnectedThread = null;
			}
            mBluetoothAdapter.startDiscovery(); // 重新打开正在发现设备
	    	// 在ConnectedThread里会调用mHandler.sendEmptyMessage(Constant.MSG_DISCONNECT)，handler处理消息之后会重启AcceptThread
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	// 发送数据
	public boolean sendData(byte[] data) throws IOException {
		if (mConnectedThread != null) {
			mConnectedThread.write(data);
			return true;
		} else {
			return false;
		}
	}
	
	public void stopListening() { // 退出死循环，停止监听客户端
		listening = false;
		try {
			if (mmServerSocket != null) {
				mmServerSocket.close(); // 如果不这样，之前的线程就会抢着连接，就无法发送数据
				mmServerSocket = null;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
