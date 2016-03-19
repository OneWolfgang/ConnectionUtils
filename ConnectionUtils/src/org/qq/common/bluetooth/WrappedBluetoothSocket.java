package org.qq.common.bluetooth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.qq.common.socket.ISocket;

import android.bluetooth.BluetoothSocket;

public class WrappedBluetoothSocket implements ISocket {

	private BluetoothSocket mBluetoothSocket;

	public WrappedBluetoothSocket(BluetoothSocket socket) {
		mBluetoothSocket = socket;
	}
	
	@Override
	public InputStream getInputStream() throws IOException {
		return mBluetoothSocket.getInputStream();
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return mBluetoothSocket.getOutputStream();
	}

	@Override
	public boolean isClosed() {
		//TODO
		return false;
	}

	@Override
	public void close() throws IOException {
		mBluetoothSocket.close();
	}

}
