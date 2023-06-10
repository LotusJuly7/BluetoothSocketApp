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
    BluetoothAdapter bluetoothAdapter; // 蓝牙适配器
	Button bluetooth, bluetooth_list, bluetooth_disconnect; // 蓝牙开关按钮
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
	TextView bluetooth_connectedDevice; // 用于显示信息的文本
	String bluetoothDevice_name; // 连接到的设备的名称
	Handler bluetooth_acceptHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case Constant.MSG_START_LISTENING:
				Toast.makeText(MainActivity.this, "ServerThread 开始监听", Toast.LENGTH_SHORT).show();
				break;
			case Constant.MSG_FINISH_LISTENING:
				Toast.makeText(MainActivity.this, "ServerThread 结束监听", Toast.LENGTH_SHORT).show();
				break;
			case Constant.MSG_GOT_A_CLIENT:
				Toast.makeText(MainActivity.this, "ServerThread 连接成功", Toast.LENGTH_SHORT).show();
				connectSuccessful((BluetoothDevice) msg.obj);
				clientThread = null;
				break;
			case Constant.MSG_GOT_DATA:
				//Toast.makeText(MainActivity.this, "ServerThread 收到 " + msg.obj, Toast.LENGTH_SHORT).show();
				showMessage(bluetoothDevice_name, (String) msg.obj, false);
				break;
			case Constant.MSG_DISCONNECT:
				Toast.makeText(MainActivity.this, "ServerThread 连接断开", Toast.LENGTH_SHORT).show();
				bluetooth_disconnect_action(); // 重启ServerThread并更新UI
			}
		}
	};
	Handler bluetooth_connectHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case Constant.MSG_ERROR:
				Toast.makeText(MainActivity.this, "ClientThread 连接失败", Toast.LENGTH_SHORT).show();
				bluetoothDevice_connecting_dialog_dismiss();
				break;
			case Constant.MSG_CONNECTED_TO_SERVER:
				Toast.makeText(MainActivity.this, "ClientThread 连接成功", Toast.LENGTH_SHORT).show();
				connectSuccessful((BluetoothDevice) msg.obj);
				serverThread = null;
				break;
			case Constant.MSG_GOT_DATA:
				//Toast.makeText(MainActivity.this, "ClientThread 收到 " + msg.obj, Toast.LENGTH_SHORT).show();
				showMessage(bluetoothDevice_name, (String) msg.obj, false);
				break;
			case Constant.MSG_DISCONNECT:
				Toast.makeText(MainActivity.this, "ClientThread 连接断开", Toast.LENGTH_SHORT).show();
				clientThread = null;
				bluetooth_disconnect_action(); // 重启ServerThread并更新UI
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
				if (bluetoothAdapter.isEnabled()) { // 如果蓝牙是开的
					bluetoothAdapter.disable();	// 直接关闭
				} else {
					bluetoothAdapter.enable();
				}
			}
		});
		bluetooth_list.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				bluetoothDevice_list_linear.removeAllViews();
				// 先清空内容布局
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
				tail.setText("此处仅显示已配对的设备\n如需扫描更多设备，请前往系统设置");
				bluetoothDevice_list_linear.addView(tail, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
				// 加载完内容布局，再显示对话框
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
				.setTitle("选择要连接的设备")
				.setView(bluetoothDevice_list_scroll)
				.setNegativeButton("取消", null);
		registerReceiver(bluetoothStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

		if (bluetoothAdapter.isEnabled()) { // 如果蓝牙是开的
			bluetooth.setText("关闭蓝牙");
			bluetooth_list.setVisibility(View.VISIBLE);
			startServer();
		}
	}
	
	ServerThread serverThread;
    ClientThread clientThread;
    
    public void bluetooth_item_click(View v) {
		bluetoothDevice_connecting_dialog = ProgressDialog.show(MainActivity.this, "", "正在连接…");
		clientThread = new ClientThread(bluetoothDevices[v.getId()], bluetoothAdapter, bluetooth_connectHandler);
		clientThread.start();
    }
    
    public void bluetooth_disconnect_click(View v) { // 点击了“断开”按钮
    	if (serverThread != null) {
    		if (serverThread.mConnectedThread != null) {
        		serverThread.cancel(); // handler处理消息之后会重启ServerThread
        	}
    	}
    	if (clientThread != null) {
    		if (clientThread.mConnectedThread != null) {
        		clientThread.cancel(); // handler处理消息之后会重启ServerThread并将clientThread置为null
        	}
    	}
    }
	
	void startServer() { // 启动蓝牙服务器
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
					bluetooth.setText("正在打开");
					bluetooth.setEnabled(false);
					bluetooth_list.setVisibility(View.GONE);
				} else if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0) == BluetoothAdapter.STATE_ON) {
					bluetooth.setText("关闭蓝牙");
					bluetooth.setEnabled(true);
					bluetooth_list.setVisibility(View.VISIBLE);
					startServer();
				} else if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0) == BluetoothAdapter.STATE_TURNING_OFF) {
					bluetooth.setText("正在关闭");
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
			        		clientThread.mConnectedThread.cancel(); // handler处理消息之后本来会重启AcceptThread，但由于加了蓝牙连接状态判断，就不会
			        	}
			    		clientThread = null;
			    	}
				} else if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0) == BluetoothAdapter.STATE_OFF) {
					bluetooth.setText("打开蓝牙");
					bluetooth.setEnabled(true);
					bluetooth_list.setVisibility(View.GONE);
				}
				
			}
        }
    };
    
    void connectSuccessful(BluetoothDevice device) {
    	serverThread.stopListening(); // 无论是作为客户端还是服务器，只要连接成功，就退出服务器监听
    	bluetoothDevice_connecting_dialog_dismiss();
    	bluetoothDevice_name = device.getName();
		bluetooth_connectedDevice.setText("已连接到" + bluetoothDevice_name + "  " + device.getAddress());
		bluetooth_list.setVisibility(View.GONE);
		bluetooth_disconnect.setVisibility(View.VISIBLE);
		send_btn.setEnabled(true);
		bluetoothDevice_list_dialog_dismiss();
    }
    void bluetooth_disconnect_action() { // 重启AcceptThread并更新UI
    	if (bluetoothAdapter.isEnabled() && isRunning) { // 如果蓝牙是开的，且Activity没被销毁
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
			if (serverThread != null) { // 作为服务器，向客户端发送
	    		try {
					if (serverThread.sendData(message.getBytes())) {
						showMessage(bluetoothDevice_name, message, true);
						input.setText("");
					} else {
						Toast.makeText(MainActivity.this, "发送失败：与Client通信线程不存在", Toast.LENGTH_SHORT).show();
					}
				} catch (IOException e) {
					e.printStackTrace();
					Toast.makeText(MainActivity.this, "发送失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
				}
	    	}
			if (clientThread != null) { // 作为客户端，向服务器发送
	        	try {
					if (clientThread.sendData(message.getBytes())) {
						showMessage(bluetoothDevice_name, message, true);
						input.setText("");
					} else {
						Toast.makeText(MainActivity.this, "发送失败：与Server通信线程不存在", Toast.LENGTH_SHORT).show();
					}
				} catch (IOException e) {
					e.printStackTrace();
					Toast.makeText(MainActivity.this, "发送失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
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
        		clientThread.mConnectedThread.cancel(); // handler处理消息之后本来会重启AcceptThread，但由于isRunning设为false，就不重启
        	}
    		clientThread = null;
    	}
		super.onDestroy();
	}
}
