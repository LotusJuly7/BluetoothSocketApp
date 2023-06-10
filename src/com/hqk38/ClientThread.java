package com.hqk38;

import java.io.IOException;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;

public class ClientThread extends Thread {
	/** 客户端socket*/    
	private BluetoothSocket mmSocket;
	/** 要连接的设备*/    
	private BluetoothDevice mmDevice;
    private BluetoothAdapter mBluetoothAdapter;
	/** 主线程通信的Handler*/   
    private Handler mHandler;
    /** 发送和接收数据的处理类*/   
	ConnectedThread mConnectedThread;
	
    public ClientThread(BluetoothDevice device, BluetoothAdapter bluetoothAdapter, Handler mUIhandler) {
        mmDevice = device;
        mBluetoothAdapter = bluetoothAdapter;
        mHandler = mUIhandler;
        BluetoothSocket tmp = null;
        try {
            // 创建客户端Socket
            tmp = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        mmSocket = tmp;
    }
    
	@Override    
	public void run() {
    	super.run();
    	// 关闭正在发现设备（如果此时又在查找设备，又在发送数据，会有冲突，影响传输效率）
    	mBluetoothAdapter.cancelDiscovery();
    	try {
        	// 连接服务器
        	mmSocket.connect();
	    	manageConnectedSocket(mmSocket);
    	} catch (IOException e) {
    		// 连接异常就关闭
    		mHandler.sendEmptyMessage(Constant.MSG_ERROR);
    		try {
    			mmSocket.close();
	            mBluetoothAdapter.startDiscovery(); // 重新打开正在发现设备
    		} catch (IOException e1) {
    		}
    	}
    }

    private void manageConnectedSocket(BluetoothSocket mmSoket) {
        // 通知主线程连接上了服务端socket，更新UI
        Message message = mHandler.obtainMessage(Constant.MSG_CONNECTED_TO_SERVER, mmDevice); 
 		mHandler.sendMessage(message);
        // 新建一个线程进行通讯，不然会发现线程堵塞
        mConnectedThread = new ConnectedThread(mmSoket, mHandler);
        mConnectedThread.start();
    }

    // 关闭当前客户端
    public void cancel() {
        try {
            mmSocket.close();
            mBluetoothAdapter.startDiscovery(); // 重新打开正在发现设备
	    	//在ConnectedThread里会调用mHandler.sendEmptyMessage(Constant.MSG_DISCONNECT)，handler处理消息之后会重启AcceptThread
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
}
