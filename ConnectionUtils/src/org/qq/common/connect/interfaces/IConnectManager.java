package org.qq.common.connect.interfaces;

import java.util.List;

import android.content.Context;

/**
 * abstract connection manager interface
 * @author zs
 *
 */
public interface IConnectManager {

	/***
	 * init the connect manager
	 * @param context
	 * @return true if action success, false otherwise
	 */
	public boolean init(Context context);
	
	/***
	 * start scan for other devices
	 * @return true if action success, false otherwise
	 */
	public boolean startScan();
	
	/***
	 * when called, this device acts as a server,
	 * wait for other device's connection request
	 * @return true if action success, false otherwise
	 */
	public boolean listen();
	
	/***
	 * when called, this device acts as a client
	 * @param identifier
	 * @return true if action success, false otherwise
	 */
	public boolean connect(String identifier);
	
	/***
	 * return all identifiers of the connected devices,
	 * if acts as server, it return all the clients,
	 * if acts as client, this return the identifier of the server
	 * @return all the identifiers of the connected devices
	 */
	public List<String> getConnectedDevices();
	
	/***
	 * retrieve data sent by other device actively,
	 * you can also receive event for the data passively, see {@link SocketReadThread}
	 * @param identifier
	 * @return the data received from other device
	 */
	public byte[] readData(String identifier);
	
	/***
	 * write data to target device, see also {@link SocketWriteThread}}
	 * @param identifier : the identifier of the target device
	 * @param data
	 * @return true if write success, false otherwise
	 */
	public boolean writeData(String identifier, byte[] data);
	
	/***
	 * close all the connections created for current device
	 * @return true if action success, false otherwise
	 */
	public boolean close();
	
	/***
	 * destroy the manager,
	 * when invoked, all connection is closed, the manager cannot be used
	 */
	public void destroy();
}
