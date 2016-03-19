package org.qq.connection;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.qq.common.bluetooth.BTDeviceFoundEvent;
import org.qq.common.bluetooth.BTEnableStateEvent;
import org.qq.common.bluetooth.BluetoothConnectionManager;
import org.qq.common.connect.interfaces.IConnectManager;
import org.qq.common.eventbus.Event;
import org.qq.common.eventbus.EventBus;
import org.qq.common.util.CommonUtils;
import org.qq.common.util.UIUtil;
import org.qq.common.wifi.WifiConnectionManager;

public class MainActivity extends Activity {

	private static final String TAG = "MainActivity";

	private IConnectManager mConnectManager;

	private List<BluetoothDevice> mDeviceList = new ArrayList<BluetoothDevice>();
	
	private List<String> mDeviceNameList = new ArrayList<String>();
	
	private BaseAdapter mDevicesAdapter;
	
	private void showToast(final String tip) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(MainActivity.this, tip, Toast.LENGTH_SHORT)
						.show();
			}
		});
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		EventBus.getInstance().register(this);
		mConnectManager = WifiConnectionManager.getInstance();

		mConnectManager.init(this);

		findViewById(R.id.build).setOnClickListener(mOnClickListener);
		findViewById(R.id.connect).setOnClickListener(mOnClickListener);
//		findViewById(R.id.connect).setEnabled(false);
		findViewById(R.id.senddata).setOnClickListener(mOnClickListener);
		findViewById(R.id.readdata).setOnClickListener(mOnClickListener);
		
		ListView deviceListView = (ListView) findViewById(R.id.lstview_devices);
		mDevicesAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mDeviceNameList);
		deviceListView.setAdapter(mDevicesAdapter);
		
		deviceListView.setOnItemClickListener(mOnListItemClickListener);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		EventBus.getInstance().unregister(this);
		
		mConnectManager.destroy();
		
	}

	private View.OnClickListener mOnClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View view) {
			switch (view.getId()) {
			case R.id.build:
				mConnectManager.listen();
				break;
			case R.id.connect:
				mConnectManager.connect("");
				break;
			case R.id.senddata:
				List<String> ipList = mConnectManager.getConnectedDevices();
				if (CommonUtils.isEmpty(ipList)) {
					UIUtil.showToast(getApplicationContext(), "no available connection");
					return;
				}
				mConnectManager.writeData(ipList.get(0), "This is client".getBytes());
				break;
			case R.id.readdata:
				List<String> ipListRead = mConnectManager.getConnectedDevices();
				if (CommonUtils.isEmpty(ipListRead)) {
					UIUtil.showToast(getApplicationContext(), "no available connection");
					return;
				}
				byte[] data = mConnectManager.readData(ipListRead.get(0));
				try {
					showToast(new String(data, "utf8"));
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			}
		}
	};

	public void handleEvent(Event event) {
		if(null == event) {
			return;
		}
		if(event instanceof BTDeviceFoundEvent) {
			BTDeviceFoundEvent deviceFoundEvent = (BTDeviceFoundEvent) event;
			BluetoothDevice device = deviceFoundEvent.getDevice();
			mDeviceList.add(device);
			mDeviceNameList.add(device.getName() + ":"+ device.getAddress());
			mDevicesAdapter.notifyDataSetChanged();
		} else if(event instanceof BTEnableStateEvent) {
			BTEnableStateEvent stateEvent = (BTEnableStateEvent) event;
			if(stateEvent.isEnabled()) {
				mConnectManager.startScan();
			}
		}
	}
	
	private OnItemClickListener mOnListItemClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			BluetoothDevice device = mDeviceList.get(position);
			mConnectManager.connect(device.getAddress());
		}
	};
}
