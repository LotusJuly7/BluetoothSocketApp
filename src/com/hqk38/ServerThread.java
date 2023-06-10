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
	 /** �߳���ͨ�ŵĸ���UI��Handler*/    
	private Handler mHandler;
	 /** �������пͻ������ӣ��½�һ���̵߳���������Ȼ�ڴ��߳��л����*/   
	ConnectedThread mConnectedThread;
	private boolean listening = true;

	public ServerThread(BluetoothAdapter adapter, Handler handler) throws IOException {
  	 	mBluetoothAdapter = adapter;
  		mHandler = handler;
    	// ��ȡ���������socket       
		mmServerSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(mBluetoothAdapter.getName(), UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
	}
	 
	@Override
	public void run() {
		super.run();
		BluetoothSocket socket = null; // ���ӿͻ��˵�socket
		mHandler.sendEmptyMessage(Constant.MSG_START_LISTENING); // ֪ͨ���̸߳���UI���ͻ��˿�ʼ����
		while (listening){ // ������ǲ��˳��ģ�Ҫһֱ�������ӽ����Ŀͻ��ˣ���������ѭ��
			if (listening) {
    			try {
    				socket = mmServerSocket.accept(); // ��ȡ���ӵĿͻ���socket
      		  	} catch (IOException e) {
      		  		mHandler.sendEmptyMessage(Constant.MSG_FINISH_LISTENING); // ֪ͨ���̸߳���UI����ȡ�쳣
      		  		e.printStackTrace();
      		  		break; // ������˳�һֱ�����߳�
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
				manageConnectSocket(socket); // �������ӵĿͻ���socket
				// ����Ӧ�����ֶ��Ͽ�������Ӧ����ֻ��֤����һ���ͻ��ˣ������������Ժ󣬹ر��˷����socket
				try {
					if (mmServerSocket != null) {
						mmServerSocket.close();
						mmServerSocket = null;
					}
					mHandler.sendEmptyMessage(Constant.MSG_FINISH_LISTENING);
				} catch (IOException e) {
					e.printStackTrace();           
				}
				break; // �����˿ͻ��ˣ�ֹͣ����
			}
		}
	}
	// �������ӵĿͻ���socket
	private void manageConnectSocket(BluetoothSocket socket) {
		// ֻ֧��ͬʱ����һ������
		if (mConnectedThread != null) {
			// mConnectedThread��Ϊ�գ��ߵ�֮ǰ�Ŀͻ���
			mConnectedThread.cancel();
			mConnectedThread = null;
		}
		// ���̸߳���UI�����ӵ���һ���ͻ���
		Message message = mHandler.obtainMessage(Constant.MSG_GOT_A_CLIENT, socket.getRemoteDevice()); 
 		mHandler.sendMessage(message);
		// �½�һ���̣߳�����ͻ��˷���������
		mConnectedThread = new ConnectedThread(socket, mHandler);
		mConnectedThread.start();
	}
	
	// �Ͽ�����ˣ���������
	public void cancel() {
		try {
			if (mmServerSocket != null) {
				mmServerSocket.close();
				mmServerSocket = null;
			}
			if (mConnectedThread != null) {
				// mConnectedThread��Ϊ�գ��ߵ�֮ǰ�Ŀͻ���
				mConnectedThread.cancel();
				mConnectedThread = null;
			}
            mBluetoothAdapter.startDiscovery(); // ���´����ڷ����豸
	    	// ��ConnectedThread������mHandler.sendEmptyMessage(Constant.MSG_DISCONNECT)��handler������Ϣ֮�������AcceptThread
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	// ��������
	public boolean sendData(byte[] data) throws IOException {
		if (mConnectedThread != null) {
			mConnectedThread.write(data);
			return true;
		} else {
			return false;
		}
	}
	
	public void stopListening() { // �˳���ѭ����ֹͣ�����ͻ���
		listening = false;
		try {
			if (mmServerSocket != null) {
				mmServerSocket.close(); // �����������֮ǰ���߳̾ͻ��������ӣ����޷���������
				mmServerSocket = null;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
