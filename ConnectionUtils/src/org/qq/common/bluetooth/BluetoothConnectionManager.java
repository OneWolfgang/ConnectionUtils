package org.qq.common.bluetooth;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.qq.common.connect.interfaces.IConnectManager;
import org.qq.common.eventbus.EventBus;
import org.qq.common.log.Logging;
import org.qq.common.socket.SocketReadThread;
import org.qq.common.socket.SocketWriteThread;
import org.qq.common.util.CommonUtils;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

/***
 * the wrapped ability of bluetooth communication, more details see
 * "http://blog.csdn.net/u013478336/article/details/50760784"
 * @author qq
 * 2016.3.15
 */
public class BluetoothConnectionManager implements IConnectManager {

	public static final String TAG = "BluetoothConnectionManager";

	// *************************singleton******************************//
	private static BluetoothConnectionManager mInstance;
	
	private BluetoothConnectionManager() {

	}

	public static BluetoothConnectionManager getInstance() {
		if (null == mInstance) {
			synchronized (BluetoothConnectionManager.class) {
				if (null == mInstance) {
					mInstance = new BluetoothConnectionManager();
				}
			}
		}
		return mInstance;
	}
	// *************************singleton******************************//
	
	//define the common visible second when set device to be foundable
	private static final int VISIBLE_SECOND = 200;

	//the socket name for bluetooth communication, usually can be any string
	private static final String BLUETOOTH_SOCKET_NAME = "BluetoothConnect";
	//the UUID for bluetooth communication
	private static final UUID UUID_CONNECTION = UUID
			.fromString("d2ea0fdc-1982-40e1-98e8-9dcd45130b8e");

	private Context mContext;
	
	//the bluetooth manager used for all operations to bluetooth module
	private BluetoothAdapter mBluetoothAdapter;
	//a broadcast receiver registered for bluetooth event
	//such as device on/off, search begin/finish, other device found event and so on
	private BluetoothEventReceiver mBluetoothEventReceiver;
	//manager all the read thread of the connected sockets
	private HashMap<String, SocketReadThread> mReadThreadMap = new HashMap<String, SocketReadThread>();
    //manager all the write thread of the connected sockets
	private HashMap<String, SocketWriteThread> mWriteThreadMap = new HashMap<String, SocketWriteThread>();
	//manager all the founded devices
	private HashMap<String, BluetoothDevice> mFoundedDevicesMap = new HashMap<String, BluetoothDevice>();
	//the server socket of the device
	private BluetoothServerSocket mBTServerSocket;
	
	private static final int NEXT_ACTION_NONE = 0;
	private static final int NEXT_ACTION_CREATE_CONN_THREAD = 1;
	//indicate what to do after device bounded
	private int mNextConnectAction = NEXT_ACTION_NONE;
	
	@Override
	public boolean init(Context context) {
		mContext = context;

		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		if (null == mBluetoothAdapter) {
			return false;
		}

		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
		intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
		mBluetoothEventReceiver = new BluetoothEventReceiver();
		mContext.registerReceiver(mBluetoothEventReceiver, intentFilter);

		if (!mBluetoothAdapter.isEnabled()) {
			//if device not enabled,
			//start enable the device, and all subsequent action must be down
			//when we receive BluetoothAdapter.ACTION_STATE_CHANGED broadcast
			boolean enabled = mBluetoothAdapter.enable();
			if(!enabled) {
				Logging.d(TAG, "init()| enable bluetooth failed");
				return false;
			}
		} else {
			//if device is already on,
			//we send a event back and listen for other device's connection request
			EventBus.getInstance().sendEvent(BTEnableStateEvent.createEvent(true));
			listen();
		}
		return true;
	}

	@Override
	public boolean startScan() {
		if (mBluetoothAdapter.getState() != BluetoothAdapter.STATE_ON) {
			Logging.d(TAG, "startScan()| device not open");
			return false;
		}
		//result will be false when device not enabled
		//follow the previous check, it will not happen mostly
		boolean startSuccess = mBluetoothAdapter.startDiscovery();
		return startSuccess;
	}

	@Override
	public boolean listen() {
		if (mBluetoothAdapter.getState() != BluetoothAdapter.STATE_ON) {
			Logging.d(TAG, "listen()| device not open");
			return false;
		}
		//a thread is created for endless listening of other devices' connection request
		//UNTIL mBTServerSocket is null, which means close() is called
		Thread listenThread = new Thread() {
			public void run() {
				mBTServerSocket = null;
				try {
					mBTServerSocket = mBluetoothAdapter
							.listenUsingRfcommWithServiceRecord(
									BLUETOOTH_SOCKET_NAME, UUID_CONNECTION);
				} catch (IOException ex) {
					Logging.d(TAG, "listen()| listen at uuid failed", ex);
				}
				
				if(null == mBTServerSocket) {
					return;
				}
				
				while (null != mBTServerSocket) {
					// when a new connect built, create two thread for data
					// operation
					try {
						//blocked until a client connection is created
						BluetoothSocket socket = mBTServerSocket.accept();
						//after connection created, we create read/write thread for the socket
						//and manager them in mReadThreadMap & mWriteThreadMap
						SocketWriteThread writeThread = new SocketWriteThread(
								new WrappedBluetoothSocket(socket));
						SocketReadThread readThread = new SocketReadThread(
								new WrappedBluetoothSocket(socket));
						
						mReadThreadMap.put(socket.getRemoteDevice().getAddress(), readThread);
						mWriteThreadMap.put(socket.getRemoteDevice().getAddress(), writeThread);
						
						writeThread.start();
						readThread.start();
					} catch (Exception ex) {
						Logging.d(TAG, "listen()| wait for connection failed", ex);
					}
				}
			}
		};
		listenThread.setName("BTListenThread");
		listenThread.start();
		return true;
	}
	
	/***
	 * make this device visible to other bluetooth device
	 * @param visibilitySeconds : specific duration for discoverability in seconds
	 */
	public void setDeviceVisible(int visibilitySeconds) {
		Intent in = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
		in.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, visibilitySeconds);
		mContext.startActivity(in);
	}

	@Override
	public boolean connect(final String identifier) {
		if (mBluetoothAdapter.getState() != BluetoothAdapter.STATE_ON) {
			Logging.d(TAG, "connect()| device not open");
			return false;
		}
		//the discovery work must be stopped,
		//because it affects the stability of the connections 
		mBluetoothAdapter.cancelDiscovery();
		
		final BluetoothDevice device = mFoundedDevicesMap.get(identifier);
		if(null == device) {
			return false;
		}
		
		//if target device is not bounded, we do the subsequent work after bounded
		switch (device.getBondState()) {
		case BluetoothDevice.BOND_NONE:
			bindToDevices(device);
		case BluetoothDevice.BOND_BONDING:
			mNextConnectAction = NEXT_ACTION_CREATE_CONN_THREAD;
			return false;
		default:
			break;
		}
		
		//after bounded, created connection socket to target device
		createConnectThread(device);
		return true;
	}

	//create connection socket to target device
	private void createConnectThread(final BluetoothDevice device) {
		Thread connectThread = new Thread() {
			public void run() {
				try {
					//we CANNOT close the socket in finally{}, otherwise no data can be translated
					BluetoothSocket socket = device
							.createRfcommSocketToServiceRecord(UUID_CONNECTION);
					//block until connect success
					socket.connect();
					//after connect success, create read/write thread for the socket
					SocketWriteThread writeThread = new SocketWriteThread(
							new WrappedBluetoothSocket(socket));
					SocketReadThread readThread = new SocketReadThread(
							new WrappedBluetoothSocket(socket));
					
					mReadThreadMap.put(device.getAddress(), readThread);
					mWriteThreadMap.put(device.getAddress(), writeThread);
					writeThread.start();
					readThread.start();
				} catch (Exception ex) {
					Logging.d(TAG, "connect()| connect to server failed", ex);
				}
			}
		};
		connectThread.setName("BTConnectThread");
		connectThread.start();
	}

	@Override
	public List<String> getConnectedDevices() {
		if (mBluetoothAdapter.getState() != BluetoothAdapter.STATE_ON) {
			Logging.d(TAG, "getConnectedDevices()| device not open");
			return null;
		}
		Set<String> listIdentifierSet = mReadThreadMap.keySet();
		if(CommonUtils.isEmpty(listIdentifierSet)) {
			return null;
		}
		return new ArrayList<String>(listIdentifierSet);
	}

	@Override
	public byte[] readData(String identifier) {
		if (mBluetoothAdapter.getState() != BluetoothAdapter.STATE_ON) {
			Logging.d(TAG, "readData()| device not open");
			return null;
		}
		SocketReadThread readThread = mReadThreadMap.get(identifier);
		if(null == readThread) {
			return null;
		}
		return readThread.readData();
	}

	@Override
	public boolean writeData(String identifier, byte[] data) {
		if (mBluetoothAdapter.getState() != BluetoothAdapter.STATE_ON) {
			Logging.d(TAG, "writeData()| device not open");
			return false;
		}
		SocketWriteThread writeThread = mWriteThreadMap.get(identifier);
		if(null == writeThread) {
			return false;
		}
		return writeThread.writeData(data);
	}

	@Override
	public boolean close() {
		// close server socket if need
		if(null != mBTServerSocket) {
			try {
				mBTServerSocket.close();
			} catch (IOException e) {
				Logging.d(TAG, "close()| close server socket failed");
			}
			mBTServerSocket = null;
		}
		
		//close all the sockets in read threads
		Iterator<Entry<String, SocketReadThread>> iteratorReadMap =
				mReadThreadMap.entrySet().iterator();
		while (iteratorReadMap.hasNext()) {
			Entry<String, SocketReadThread> entry = iteratorReadMap.next();
			SocketReadThread readThread = entry.getValue();
			readThread.close();
		}
		mReadThreadMap.clear();
		
		// close all the sockets in write threads,
		// maybe not nessesary, because all sockets are close previous
		Iterator<Entry<String, SocketWriteThread>> iteratorWriteMap =
				mWriteThreadMap.entrySet().iterator();
		while (iteratorWriteMap.hasNext()) {
			Entry<String, SocketWriteThread> entry = iteratorWriteMap.next();
			SocketWriteThread writeThread = entry.getValue();
			writeThread.close();
		}
		mWriteThreadMap.clear();
		
		//disable bluetooth to save battery
		mBluetoothAdapter.disable();
		return true;
	}

	@Override
	public void destroy() {
		mInstance = null;
		
		if(null != mBluetoothEventReceiver) {
			mContext.unregisterReceiver(mBluetoothEventReceiver);
			mBluetoothEventReceiver = null;
		}
		
		close();
		
		mContext = null;
	}

	private void bindToDevices(BluetoothDevice device) {
		if (device.getBondState() == BluetoothDevice.BOND_NONE) {
			// request bound match to target device,
			// handle the subsequent work after received BluetoothDevice.ACTION_BOND_STATE_CHANGED
			Method creMethod;
			try {
				creMethod = BluetoothDevice.class.getMethod("createBond");
				creMethod.invoke(device);
			} catch (Exception ex) {
				Logging.d(TAG, "bindToDevices failed", ex);
			}
		}
	}

	private class BluetoothEventReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
				int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
						BluetoothAdapter.STATE_OFF);
				if (BluetoothAdapter.STATE_ON == state) {
					setDeviceVisible(VISIBLE_SECOND);
					
					//start listen for another device connection
					listen();
					
					//send a event out
					EventBus.getInstance().sendEvent(BTEnableStateEvent.createEvent(true));
				}
			} else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
				Logging.d(TAG, "discovery devices started");
			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED
					.equals(action)) {
				Logging.d(TAG, "discovery devices finished");
			} else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				// a device is found
				handleDeviceFound(intent);
			} else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
				// bond device success
				BluetoothDevice device = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				int boundState = device.getBondState();
				if (BluetoothDevice.BOND_BONDED == boundState) {
					if(mNextConnectAction == NEXT_ACTION_CREATE_CONN_THREAD) {
						createConnectThread(device);
					}
				}
			}
		}

		private void handleDeviceFound(Intent intent) {
			BluetoothDevice device = intent
					.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
			if (null == device) {
				Logging.d(TAG, "handleDeviceFound()| device is null");
				return;
			}
			Logging.d(TAG, "handleDeviceFound()| device= " + device.getName() + "("
					+ device.getAddress() + ")");

			mFoundedDevicesMap.put(device.getAddress(), device);
			
			BTDeviceFoundEvent event = BTDeviceFoundEvent.createEvent(device);
			EventBus.getInstance().sendEvent(event);
		}

	}

}
