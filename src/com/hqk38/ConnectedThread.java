package com.hqk38;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;

public class ConnectedThread extends Thread{
	/** 当前连接的客户端BluetoothSocket*/
	private BluetoothSocket mmSocket;
	private InputStream mmInputStream;
	private OutputStream mmOutputStream;
	/** 与主线程通信Handler*/
	private Handler mHandler;
	public ConnectedThread(BluetoothSocket socket, Handler handler) {
		mmSocket = socket;
		mHandler = handler;
		try { 
			mmInputStream = socket.getInputStream();
			mmOutputStream = socket.getOutputStream(); 
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	@Override
	public void run() {
		super.run();
    	byte[] buffer = new byte[64];
  		while (mmSocket.isConnected()) {
 			try {
    			// 读取数据
                int bytes = mmInputStream.read(buffer);
        	    if (bytes > 0) {
	        		String data = new String(buffer, 0, bytes, "utf-8");
	        		// 把数据发送到主线程, 此处还可以用广播
	        		Message message = mHandler.obtainMessage(Constant.MSG_GOT_DATA, data); 
	         		mHandler.sendMessage(message);
    			}
            } catch (IOException e) {
            	e.printStackTrace();
            	mHandler.sendEmptyMessage(Constant.MSG_DISCONNECT);
            	break;
            }
  		}
	}
	// 只当服务器时使用，踢掉当前客户端
	public void cancel() {
		try {
			mmSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    // 服务端发送数据
    public void write(byte[] data) throws IOException {
         mmOutputStream.write(data);
	}
}
