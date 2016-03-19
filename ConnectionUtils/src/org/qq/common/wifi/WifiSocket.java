package org.qq.common.wifi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.qq.common.socket.ISocket;

public class WifiSocket implements ISocket {
	
	private Socket mSocket;
	
	public WifiSocket(Socket socket) {
		mSocket = socket;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return mSocket.getInputStream();
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return mSocket.getOutputStream();
	}

	@Override
	public boolean isClosed() {
		return mSocket.isClosed();
	}

	@Override
	public void close() throws IOException {
		mSocket.close();
	}

}
