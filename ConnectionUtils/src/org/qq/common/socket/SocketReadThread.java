package org.qq.common.socket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.qq.common.eventbus.EventBus;
import org.qq.common.log.Logging;

/***
 * Thread used to read data from socket
 * @author lq
 */
public class SocketReadThread extends Thread {
	private static final String TAG = "SocketReadThread";
	ISocket mSocket;
	private ByteArrayOutputStream mOutputStream = new ByteArrayOutputStream();

	public SocketReadThread(ISocket socket) {
		mSocket = socket;
	}

	@Override
	public void run() {
		super.run();
		InputStream is = null;
		byte[] buffer = new byte[256];
		try {
			is = mSocket.getInputStream();
			// read data from socket
			while (!mSocket.isClosed()) {
				int readSize = 0;
				//read() will block until data come
				while ((readSize = is.read(buffer)) > 0) {
					//store in output stream until readData() triggered
					mOutputStream.write(buffer, 0, readSize);
					//send data arrive event
					EventBus.getInstance().sendEvent(
							ReadSocketDataEvent.createEvent(buffer, 0, readSize));
				}
			}
		} catch (Exception ex) {
			// error
			Logging.d(TAG, "SocketReadThread | read data failed", ex);
		} finally {
			if (null != is) {
				try {
					is.close();
				} catch (Exception ex) {
					Logging.d(TAG, "SocketReadThread | close inputstream error happened", ex);
				}
				is = null;
			}
			if (null != mSocket) {
				try {
					mSocket.close();
				} catch (Exception ex) {
					// close socket error happened
					Logging.d(TAG, "SocketReadThread | close socket error happened", ex);
				}
				mSocket = null;
			}
		}
	}

	/***
	 * read data from flushed output stream
	 * @return
	 */
	public byte[] readData() {
		byte[] data =  mOutputStream.toByteArray();
		mOutputStream.reset();
		return data;
	}
	
	public void close() {
		try {
			mSocket.close();
		} catch (IOException e) {
			Logging.d(TAG, "close()| close socket failed");
		}
	}
}