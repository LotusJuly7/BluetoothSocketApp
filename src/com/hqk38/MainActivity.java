package com.hqk38;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    BluetoothAdapter bluetoothAdapter; // ����������
	Button bluetooth, bluetooth_list, bluetooth_disconnect; // �������ذ�ť
	AlertDialog.Builder bluetoothDevice_list_dialogBuilder;
	AlertDialog bluetoothDevice_list_dialog;
	ScrollView bluetoothDevice_list_scroll;
	LinearLayout bluetoothDevice_list_linear;
	BluetoothDevice[] bluetoothDevices;
	ProgressDialog bluetoothDevice_connecting_dialog;
	LinearLayout listView1;
	LinearLayout.LayoutParams messageItemLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
	Button send_btn;
	EditText input;
	TextView bluetooth_connectedDevice; // ������ʾ��Ϣ���ı�
	String bluetoothDevice_name; // ���ӵ����豸������
	Handler bluetooth_acceptHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case Constant.MSG_START_LISTENING:
				Toast.makeText(MainActivity.this, "ServerThread ��ʼ����", Toast.LENGTH_SHORT).show();
				break;
			case Constant.MSG_FINISH_LISTENING:
				Toast.makeText(MainActivity.this, "ServerThread ��������", Toast.LENGTH_SHORT).show();
				break;
			case Constant.MSG_GOT_A_CLIENT:
				Toast.makeText(MainActivity.this, "ServerThread ���ӳɹ�", Toast.LENGTH_SHORT).show();
				connectSuccessful((BluetoothDevice) msg.obj);
				clientThread = null;
				break;
			case Constant.MSG_GOT_DATA:
				//Toast.makeText(MainActivity.this, "ServerThread �յ� " + msg.obj, Toast.LENGTH_SHORT).show();
				showMessage(bluetoothDevice_name, (String) msg.obj, false);
				break;
			case Constant.MSG_DISCONNECT:
				Toast.makeText(MainActivity.this, "ServerThread ���ӶϿ�", Toast.LENGTH_SHORT).show();
				bluetooth_disconnect_action(); // ����ServerThread������UI
			}
		}
	};
	Handler bluetooth_connectHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case Constant.MSG_ERROR:
				Toast.makeText(MainActivity.this, "ClientThread ����ʧ��", Toast.LENGTH_SHORT).show();
				bluetoothDevice_connecting_dialog_dismiss();
				break;
			case Constant.MSG_CONNECTED_TO_SERVER:
				Toast.makeText(MainActivity.this, "ClientThread ���ӳɹ�", Toast.LENGTH_SHORT).show();
				connectSuccessful((BluetoothDevice) msg.obj);
				serverThread = null;
				break;
			case Constant.MSG_GOT_DATA:
				//Toast.makeText(MainActivity.this, "ClientThread �յ� " + msg.obj, Toast.LENGTH_SHORT).show();
				showMessage(bluetoothDevice_name, (String) msg.obj, false);
				break;
			case Constant.MSG_DISCONNECT:
				Toast.makeText(MainActivity.this, "ClientThread ���ӶϿ�", Toast.LENGTH_SHORT).show();
				clientThread = null;
				bluetooth_disconnect_action(); // ����ServerThread������UI
			}
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		bluetooth = (Button) findViewById(R.id.bluetooth);
		bluetooth_list = (Button) findViewById(R.id.bluetooth_list);
		bluetooth_disconnect = (Button) findViewById(R.id.bluetooth_disconnect);
		listView1 = (LinearLayout) findViewById(R.id.listView1);
		send_btn = (Button) findViewById(R.id.fun_btn);
		send_btn.setOnClickListener(send_btn_onClick);
		input = (EditText) findViewById(R.id.input);
		bluetooth_connectedDevice = (TextView) findViewById(R.id.bluetooth_connectedDevice);
		
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		bluetooth.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (bluetoothAdapter.isEnabled()) { // ��������ǿ���
					bluetoothAdapter.disable();	// ֱ�ӹر�
				} else {
					bluetoothAdapter.enable();
				}
			}
		});
		bluetooth_list.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				bluetoothDevice_list_linear.removeAllViews();
				// ��������ݲ���
				Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
				if (pairedDevices.size() > 0) {
					View list_item = null;
					bluetoothDevices = new BluetoothDevice[pairedDevices.size()];
					int index = 0;
					for (BluetoothDevice device : pairedDevices) {
						String deviceName = device.getName();
						String deviceHardwareAddress = device.getAddress(); // MAC address
						list_item = LayoutInflater.from(MainActivity.this).inflate(R.layout.list_item, null, false);
						bluetoothDevice_list_linear.addView(list_item);
						list_item.findViewById(R.id.bluetooth_item).setId(index);
						bluetoothDevices[index] = device;
						((TextView) list_item.findViewById(R.id.name)).setText(deviceName);
						((TextView) list_item.findViewById(R.id.address)).setText(deviceHardwareAddress);
						index++;
					}
				} else {
					bluetoothDevices = null;
				}
				TextView tail = (TextView) LayoutInflater.from(MainActivity.this).inflate(R.layout.list_tail, null, false);
				tail.setText("�˴�����ʾ����Ե��豸\n����ɨ������豸����ǰ��ϵͳ����");
				bluetoothDevice_list_linear.addView(tail, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
				// ���������ݲ��֣�����ʾ�Ի���
				ViewParent parent = bluetoothDevice_list_scroll.getParent();
				if (parent != null) {
					((ViewGroup) parent).removeAllViews();
				}
				bluetoothDevice_list_dialog = bluetoothDevice_list_dialogBuilder.show(); 
			}
		});
		bluetoothDevice_list_scroll = new ScrollView(MainActivity.this);
		bluetoothDevice_list_linear = new LinearLayout(MainActivity.this);
		bluetoothDevice_list_linear.setOrientation(LinearLayout.VERTICAL);
		bluetoothDevice_list_scroll.addView(bluetoothDevice_list_linear);
		bluetoothDevice_list_dialogBuilder = new AlertDialog.Builder(MainActivity.this)
				.setTitle("ѡ��Ҫ���ӵ��豸")
				.setView(bluetoothDevice_list_scroll)
				.setNegativeButton("ȡ��", null);
		registerReceiver(bluetoothStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

		if (bluetoothAdapter.isEnabled()) { // ��������ǿ���
			bluetooth.setText("�ر�����");
			bluetooth_list.setVisibility(View.VISIBLE);
			startServer();
		}
	}
	
	ServerThread serverThread;
    ClientThread clientThread;
    
    public void bluetooth_item_click(View v) {
		bluetoothDevice_connecting_dialog = ProgressDialog.show(MainActivity.this, "", "�������ӡ�");
		clientThread = new ClientThread(bluetoothDevices[v.getId()], bluetoothAdapter, bluetooth_connectHandler);
		clientThread.start();
    }
    
    public void bluetooth_disconnect_click(View v) { // ����ˡ��Ͽ�����ť
    	if (serverThread != null) {
    		if (serverThread.mConnectedThread != null) {
        		serverThread.cancel(); // handler������Ϣ֮�������ServerThread
        	}
    	}
    	if (clientThread != null) {
    		if (clientThread.mConnectedThread != null) {
        		clientThread.cancel(); // handler������Ϣ֮�������ServerThread����clientThread��Ϊnull
        	}
    	}
    }
	
	void startServer() { // ��������������
    	try {
			serverThread = new ServerThread(bluetoothAdapter, bluetooth_acceptHandler);
			serverThread.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
	
	BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	if (intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
				if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0) == BluetoothAdapter.STATE_TURNING_ON) {
					bluetooth.setText("���ڴ�");
					bluetooth.setEnabled(false);
					bluetooth_list.setVisibility(View.GONE);
				} else if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0) == BluetoothAdapter.STATE_ON) {
					bluetooth.setText("�ر�����");
					bluetooth.setEnabled(true);
					bluetooth_list.setVisibility(View.VISIBLE);
					startServer();
				} else if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0) == BluetoothAdapter.STATE_TURNING_OFF) {
					bluetooth.setText("���ڹر�");
					bluetooth.setEnabled(false);
					bluetooth_list.setVisibility(View.GONE);
					bluetooth_connectedDevice.setText("");
					bluetoothDevice_name = null;
			    	bluetooth_disconnect.setVisibility(View.GONE);
			    	send_btn.setEnabled(false);
					if (serverThread != null) {
						serverThread.stopListening();
			    		if (serverThread.mConnectedThread != null) {
			        		serverThread.mConnectedThread.cancel();
			        		serverThread.mConnectedThread = null;
			        	}
			    		serverThread = null;
			    	}
			    	if (clientThread != null) {
			    		if (clientThread.mConnectedThread != null) {
			        		clientThread.mConnectedThread.cancel(); // handler������Ϣ֮����������AcceptThread�������ڼ�����������״̬�жϣ��Ͳ���
			        	}
			    		clientThread = null;
			    	}
				} else if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0) == BluetoothAdapter.STATE_OFF) {
					bluetooth.setText("������");
					bluetooth.setEnabled(true);
					bluetooth_list.setVisibility(View.GONE);
				}
				
			}
        }
    };
    
    void connectSuccessful(BluetoothDevice device) {
    	serverThread.stopListening(); // ��������Ϊ�ͻ��˻��Ƿ�������ֻҪ���ӳɹ������˳�����������
    	bluetoothDevice_connecting_dialog_dismiss();
    	bluetoothDevice_name = device.getName();
		bluetooth_connectedDevice.setText("�����ӵ�" + bluetoothDevice_name + "  " + device.getAddress());
		bluetooth_list.setVisibility(View.GONE);
		bluetooth_disconnect.setVisibility(View.VISIBLE);
		send_btn.setEnabled(true);
		bluetoothDevice_list_dialog_dismiss();
    }
    void bluetooth_disconnect_action() { // ����AcceptThread������UI
    	if (bluetoothAdapter.isEnabled() && isRunning) { // ��������ǿ��ģ���Activityû������
    		startServer();
    	}
		bluetooth_connectedDevice.setText("");
		bluetoothDevice_name = null;
    	bluetooth_disconnect.setVisibility(View.GONE);
    	send_btn.setEnabled(false);
		bluetooth_list.setVisibility(View.VISIBLE);
    }
    
    void bluetoothDevice_connecting_dialog_dismiss() {
    	if (bluetoothDevice_connecting_dialog != null) {
    		bluetoothDevice_connecting_dialog.dismiss();
    		bluetoothDevice_connecting_dialog = null;
    	}
    }
    void bluetoothDevice_list_dialog_dismiss() {
    	if (bluetoothDevice_list_dialog != null) {
    		bluetoothDevice_list_dialog.dismiss();
    		bluetoothDevice_list_dialog = null;
    	}
    }
    
    View.OnClickListener send_btn_onClick = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			String message = input.getText().toString();
			if (serverThread != null) { // ��Ϊ����������ͻ��˷���
	    		try {
					if (serverThread.sendData(message.getBytes())) {
						showMessage(bluetoothDevice_name, message, true);
						input.setText("");
					} else {
						Toast.makeText(MainActivity.this, "����ʧ�ܣ���Clientͨ���̲߳�����", Toast.LENGTH_SHORT).show();
					}
				} catch (IOException e) {
					e.printStackTrace();
					Toast.makeText(MainActivity.this, "����ʧ�ܣ�" + e.getMessage(), Toast.LENGTH_SHORT).show();
				}
	    	}
			if (clientThread != null) { // ��Ϊ�ͻ��ˣ������������
	        	try {
					if (clientThread.sendData(message.getBytes())) {
						showMessage(bluetoothDevice_name, message, true);
						input.setText("");
					} else {
						Toast.makeText(MainActivity.this, "����ʧ�ܣ���Serverͨ���̲߳�����", Toast.LENGTH_SHORT).show();
					}
				} catch (IOException e) {
					e.printStackTrace();
					Toast.makeText(MainActivity.this, "����ʧ�ܣ�" + e.getMessage(), Toast.LENGTH_SHORT).show();
				}
	    	}
		}
	};
	
	SimpleDateFormat dateFormat = new SimpleDateFormat("  HH:mm:ss");
	void showMessage(String nickname, String message, boolean isSend) {
		long time = System.currentTimeMillis();
		if (isSend) {
			listView1.addView(new MessageLayout(this, nickname + "  <-" + dateFormat.format(time), message, isSend), messageItemLp);
		} else {
			listView1.addView(new MessageLayout(this, nickname + dateFormat.format(time), message, isSend), messageItemLp);
		}
	}
	
	boolean isRunning = true;
	@Override
	protected void onDestroy() {
		isRunning = false;
		unregisterReceiver(bluetoothStateReceiver);
		if (serverThread != null) {
			serverThread.stopListening();
    		if (serverThread.mConnectedThread != null) {
        		serverThread.mConnectedThread.cancel();
        		serverThread.mConnectedThread = null;
        	}
    		serverThread = null;
    	}
    	if (clientThread != null) {
    		if (clientThread.mConnectedThread != null) {
        		clientThread.mConnectedThread.cancel(); // handler������Ϣ֮����������AcceptThread��������isRunning��Ϊfalse���Ͳ�����
        	}
    		clientThread = null;
    	}
		super.onDestroy();
	}
}
