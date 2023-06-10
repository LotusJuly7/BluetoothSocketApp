package com.hqk38;

import java.io.IOException;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;

public class ClientThread extends Thread {
	/** �ͻ���socket*/    
	private BluetoothSocket mmSocket;
	/** Ҫ���ӵ��豸*/    
	private BluetoothDevice mmDevice;
    private BluetoothAdapter mBluetoothAdapter;
	/** ���߳�ͨ�ŵ�Handler*/   
    private Handler mHandler;
    /** ���ͺͽ������ݵĴ�����*/   
	ConnectedThread mConnectedThread;
	
    public ClientThread(BluetoothDevice device, BluetoothAdapter bluetoothAdapter, Handler mUIhandler) {
        mmDevice = device;
        mBluetoothAdapter = bluetoothAdapter;
        mHandler = mUIhandler;
        BluetoothSocket tmp = null;
        try {
            // �����ͻ���Socket
            tmp = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        mmSocket = tmp;
    }
    
	@Override    
	public void run() {
    	super.run();
    	// �ر����ڷ����豸�������ʱ���ڲ����豸�����ڷ������ݣ����г�ͻ��Ӱ�촫��Ч�ʣ�
    	mBluetoothAdapter.cancelDiscovery();
    	try {
        	// ���ӷ�����
        	mmSocket.connect();
	    	manageConnectedSocket(mmSocket);
    	} catch (IOException e) {
    		// �����쳣�͹ر�
    		mHandler.sendEmptyMessage(Constant.MSG_ERROR);
    		try {
    			mmSocket.close();
	            mBluetoothAdapter.startDiscovery(); // ���´����ڷ����豸
    		} catch (IOException e1) {
    		}
    	}
    }

    private void manageConnectedSocket(BluetoothSocket mmSoket) {
        // ֪ͨ���߳��������˷����socket������UI
        Message message = mHandler.obtainMessage(Constant.MSG_CONNECTED_TO_SERVER, mmDevice); 
 		mHandler.sendMessage(message);
        // �½�һ���߳̽���ͨѶ����Ȼ�ᷢ���̶߳���
        mConnectedThread = new ConnectedThread(mmSoket, mHandler);
        mConnectedThread.start();
    }

    // �رյ�ǰ�ͻ���
    public void cancel() {
        try {
            mmSocket.close();
            mBluetoothAdapter.startDiscovery(); // ���´����ڷ����豸
	    	//��ConnectedThread������mHandler.sendEmptyMessage(Constant.MSG_DISCONNECT)��handler������Ϣ֮�������AcceptThread
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
}
