package org.qq.common.wifi;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.qq.common.connect.interfaces.IConnectManager;
import org.qq.common.log.Logging;
import org.qq.common.socket.SocketReadThread;
import org.qq.common.socket.SocketWriteThread;
import org.qq.common.util.CommonUtils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

/***
 * the wrapped ability of wifi connection
 */
public class WifiConnectionManager implements IConnectManager {

	private static final String TAG = "WifiConnectionManager";

	// *************************singleton******************************//
	private static WifiConnectionManager mInstance;
	
	private WifiConnectionManager() {

	}

	public static WifiConnectionManager getInstance() {
		if (null == mInstance) {
			synchronized (WifiConnectionManager.class) {
				if (null == mInstance) {
					mInstance = new WifiConnectionManager();
				}
			}
		}
		return mInstance;
	}
	// *************************singleton******************************//

	private static final String COMMON_SSID = "WIFI_SSID";

	private static final String COMMON_KEY = "QQ12345678";

	// the wifi manager used for all operations to wifi module
	private WifiManager mWifiManager;

	// the broadcast receiver of Wifi 
	private WifiEventReceiver mWifiEventReceiver;

	private Context mContext;

	// the server socket of the device
	private ServerSocket mServerSocket;

	private static final int INIT_PORT = 17000;

	private static final int MAX_PORT = 17050;

	private int mPortListening = INIT_PORT;
	
	private HashMap<String, SocketReadThread> mReadThreadMap = new HashMap<String, SocketReadThread>();

	private HashMap<String, SocketWriteThread> mWriteThreadMap = new HashMap<String, SocketWriteThread>();

	private static final int CONNECT_ACTION_NONE = 0;
	private static final int CONNECT_ACTION_CREATE_SOCKET = 1;
	private int mNextConnectAction = CONNECT_ACTION_NONE;
	@Override
	public boolean init(Context context) {
		Logging.d(TAG, "init()");
		mContext = context.getApplicationContext();
		mWifiManager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);

		// register for wifi state change
		mWifiEventReceiver = new WifiEventReceiver();
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		mContext.registerReceiver(mWifiEventReceiver, intentFilter);

		return true;
	}

	@Override
	public boolean startScan() {
		Logging.d(TAG, "startScan()");
		// first, enable wifi
		if (!enableWifi(true)) {
			Logging.d(TAG, "startScan() | enable wifi failed");
			return false;
		}
		
		// second, begin scan
		mWifiManager.startScan();
		return true;
	}

	@Override
	public boolean listen() {
		Logging.d(TAG, "listen()");
		// listen for connection, means we need
		// create a wifi hotspot, and first, we need to disable wifi
		if (!enableWifi(false)) {
			Logging.d(TAG, "listen()| enable wifi failed");
			return false;
		}

		boolean isHotspotEnabled = setWifiHotspotEnable(COMMON_SSID,
				COMMON_KEY, true);
		// if enable hotspot failed, return
		if (!isHotspotEnabled) {
			Logging.d(TAG, "listen()| create hotspot failed");
			return false;
		}

		while (null == mServerSocket && mPortListening <= MAX_PORT) {
			try {
				mServerSocket = new ServerSocket(mPortListening++);
			} catch (IOException e) {
				// use another port
				Logging.d(TAG,
						"listen() | create server socket failed at port: "
								+ mPortListening, e);
			}
		}

		if (null == mServerSocket) {
			Logging.d(TAG, "listen() | create servet socket failed");
			return false;
		} else {
			Logging.d(TAG, "listen() | create servet socket success at port "
					+ mServerSocket.getLocalPort());
		}

		Thread listenerThread = new Thread() {
			public void run() {
				while (true) {
					// when a new connect built, create two thread for data
					// operation
					try {
						Logging.d(TAG, "listen() | wait for client connection");
						Socket serverConnSocket = mServerSocket.accept();

						String hostAddress = serverConnSocket.getInetAddress()
								.getHostAddress();

						Logging.d(TAG,
								"listen() | socket created remote host: "
										+ hostAddress);

						SocketWriteThread writeThread = new SocketWriteThread(
								new WifiSocket(serverConnSocket));
						SocketReadThread readThread = new SocketReadThread(
								new WifiSocket(serverConnSocket));
						mReadThreadMap.put(hostAddress, readThread);
						mWriteThreadMap.put(hostAddress, writeThread);
						readThread.start();
						writeThread.start();
					} catch (Exception ex) {
						// do nothing
						Logging.d(TAG,
								"listen() | listen for connection failed", ex);
					}
				}
			}
		};
		listenerThread.setName("ListenerThread");
		listenerThread.start();

		return true;
	}

	@Override
	public boolean connect(String identifier) {
		Logging.d(TAG, "connect()");

		// we need to enable wifi before connect
		if (!enableWifi(true)) {
			Logging.d(TAG, "connect()| enable wifi failed");
			return false;
		}

		WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
		if (null != wifiInfo
				&& ("\"" + COMMON_SSID + "\"").equals(wifiInfo.getSSID())) {
			String ipString = CommonUtils.intToIp(wifiInfo.getIpAddress());
			createSocketWhenConnected();
			return true;
		}

		WifiConfiguration config = new WifiConfiguration();
		config.SSID = "\"" + COMMON_SSID + "\"";// ssid
		config.preSharedKey = "\"" + COMMON_KEY + "\""; // password
		config.hiddenSSID = true;
		config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
		config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
		config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
		config.allowedPairwiseCiphers
				.set(WifiConfiguration.PairwiseCipher.TKIP);
		config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
		config.status = WifiConfiguration.Status.ENABLED;
		int netID = mWifiManager.addNetwork(config);
		boolean result = mWifiManager.enableNetwork(netID, true);

//		if (!result) {
//			Logging.d(TAG, "connect() | connect wifi failed");
//			return false;
//		}

		// after net change to what we want, go to excute
		// method createSocketWhenConnected()
		mNextConnectAction = CONNECT_ACTION_CREATE_SOCKET;
		
		return true;
	}

	private void createSocketWhenConnected() {
		Logging.d(TAG, "createSocketWhenConnected()");
		DhcpInfo info = mWifiManager.getDhcpInfo();
		if (null == info) {
			Logging.d(TAG,
					"createSocketWhenConnected() | no dhcp info for network");
			return;
		}

		int ip = info.gateway;
		final String ipString = CommonUtils.intToIp(ip);
		
		if(mReadThreadMap.get(ipString) == null) {
		    createSocketToServer(ipString);
		} else {
			Logging.d(TAG,
					"createSocketWhenConnected() | created socket, do nothing");
		}
	}

	private void createSocketToServer(final String ipString) {
		Logging.d(TAG, "createSocketToServer()");
		Thread clientThread = new Thread() {
			@Override
			public void run() {
				try {
					// NOTICE: On Android5.1(ZTEB880), after we confirm the wifi is connected,
					// we also can not create a socket to server,
					// waiting at least 400ms can solve this problem
					sleep(400);
					
					Socket clientSocket = new Socket(ipString, INIT_PORT);

					Logging.d(TAG, "createSocketToServer() | create socket success " + ipString);

					SocketWriteThread writeThread = new SocketWriteThread(
							new WifiSocket(clientSocket));
					SocketReadThread readThread = new SocketReadThread(
							new WifiSocket(clientSocket));

					mReadThreadMap.put(ipString, readThread);
					mWriteThreadMap.put(ipString, writeThread);

					readThread.start();
					writeThread.start();
				} catch (Exception ex) {
					// error happen
					Logging.d(
							TAG,
							"createSocketToServer() | connect to server failed",
							ex);
				}
			}
		};
		clientThread.setName("ClientThread");
		clientThread.start();

	}

	@Override
	public List<String> getConnectedDevices() {
		Set<String> ipSet = mReadThreadMap.keySet();
		if (CommonUtils.isEmpty(ipSet)) {
			Logging.d(TAG, "getConnectedDevices()| no connected devices");
			return null;
		}
		return new ArrayList<String>(mReadThreadMap.keySet());
	}

	@Override
	public byte[] readData(String ipAddress) {
		SocketReadThread socketReadThread = mReadThreadMap.get(ipAddress);
		if (null == socketReadThread) {
			Logging.d(TAG, "readData()| no connected devices for : "
					+ ipAddress);
			return null;
		}
		return socketReadThread.readData();
	}

	@Override
	public boolean writeData(String ipAddress, byte[] data) {
		SocketWriteThread socketWriteThread = mWriteThreadMap.get(ipAddress);
		if (null == socketWriteThread) {
			Logging.d(TAG, "writeData()| no connected devices for : "
					+ ipAddress);
			return false;
		}
		return socketWriteThread.writeData(data);
	}

	@Override
	public boolean close() {
		Logging.d(TAG, "close()");
		setWifiHotspotEnable(COMMON_SSID, COMMON_KEY, false);
		enableWifi(false);
		return true;
	}

	@Override
	public void destroy() {
		Logging.d(TAG, "destroy()");
		mInstance = null;
		
		if (null != mWifiEventReceiver) {
			mContext.unregisterReceiver(mWifiEventReceiver);
			mWifiEventReceiver = null;
		}
		
		close();

		mWifiManager = null;

		mContext = null;
	}

	private boolean enableWifi(boolean enable) {
		boolean result = mWifiManager.setWifiEnabled(enable);
		return result;
	}

	private boolean setWifiHotspotEnable(String ssid, String key, boolean enable) {
		try {
			// the configuration of the hotspot
			WifiConfiguration apConfig = new WifiConfiguration();
			// the name of the hotspot
			apConfig.SSID = ssid;

			apConfig.allowedAuthAlgorithms
					.set(WifiConfiguration.AuthAlgorithm.OPEN);
			apConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
			apConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
			apConfig.allowedPairwiseCiphers
					.set(WifiConfiguration.PairwiseCipher.CCMP);
			apConfig.allowedPairwiseCiphers
					.set(WifiConfiguration.PairwiseCipher.TKIP);
			apConfig.allowedGroupCiphers
					.set(WifiConfiguration.GroupCipher.CCMP);
			apConfig.allowedGroupCiphers
					.set(WifiConfiguration.GroupCipher.TKIP);

			// key is null when empty
			if (key != null) {
				if (key.length() < 8) {
					// the length of wifi password must be 8 or longer
					return false;
				}
				// set the password for the wifi hotspot
				apConfig.allowedKeyManagement
						.set(WifiConfiguration.KeyMgmt.WPA_PSK);
				apConfig.preSharedKey = key;
			}

			// set wifi hotspot enabled
			Method method = mWifiManager.getClass().getMethod(
					"setWifiApEnabled", WifiConfiguration.class, Boolean.TYPE);
			// return whether hotspot is open
			return (Boolean) method.invoke(mWifiManager, apConfig, enable);
		} catch (Exception e) {
			return false;
		}
	}

	private class WifiEventReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
				Logging.d(TAG, "onReceive() | SCAN_RESULTS_AVAILABLE_ACTION");

//                List<ScanResult> list = mWifiManager.getScanResults();
//                if(null != mWifiScanResultListener) {
//                    mWifiScanResultListener.onScanResult(list);
//                }
			} else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
				Logging.d(TAG, "onReceive() | NETWORK_STATE_CHANGED_ACTION");
				
				NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
				String bssid = intent.getStringExtra(WifiManager.EXTRA_BSSID);
//				Logging.d(TAG, "onReceive()| networkInfo= " + networkInfo + " bssid= " + bssid);
				if(null != networkInfo && networkInfo.isConnected() && !CommonUtils.isEmpty(bssid)) {
					if(mNextConnectAction == CONNECT_ACTION_CREATE_SOCKET) {
						mNextConnectAction = CONNECT_ACTION_NONE;
						createSocketWhenConnected();
					}
				}
			}
		}

	}
}
