package com.hqk38;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;

public class ConnectedThread extends Thread{
	/** ��ǰ���ӵĿͻ���BluetoothSocket*/
	private BluetoothSocket mmSocket;
	private InputStream mmInputStream;
	private OutputStream mmOutputStream;
	/** �����߳�ͨ��Handler*/
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
    			// ��ȡ����
                int bytes = mmInputStream.read(buffer);
        	    if (bytes > 0) {
	        		String data = new String(buffer, 0, bytes, "utf-8");
	        		// �����ݷ��͵����߳�, �˴��������ù㲥
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
	// ֻ��������ʱʹ�ã��ߵ���ǰ�ͻ���
	public void cancel() {
		try {
			mmSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    // ����˷�������
    public void write(byte[] data) throws IOException {
         mmOutputStream.write(data);
	}
}
