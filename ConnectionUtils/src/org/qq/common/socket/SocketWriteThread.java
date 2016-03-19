package org.qq.common.socket;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.LinkedBlockingQueue;

import org.qq.common.log.Logging;

/***
 * Thread used to write data to socket
 * @author qq
 *
 */
public class SocketWriteThread extends Thread {
    
	private static final String TAG = "SocketWriteThread";

	private LinkedBlockingQueue<byte[]> mWaitSendQueue = new LinkedBlockingQueue<byte[]>();

	ISocket mSocket;

	public SocketWriteThread(ISocket socket) {
        mSocket = socket;
    }

    @Override
    public void run() {
        OutputStream os = null;
        try {
            os = mSocket.getOutputStream();
            //write data to socket from flush queue infinitely
            while(!mSocket.isClosed()) {
                // take data write to socket, take will block util data set
                byte[] data = mWaitSendQueue.take();
                os.write(data);
                os.flush();
            }
        } catch (Exception ex) {
            // error happened
        	Logging.d(TAG, "SocketWriteThread | write data to socket failed", ex);
        } finally {
            if(null != os) {
                try {
                    os.close();
                } catch (Exception e) {
                    // close out stream error happened
                	Logging.d(TAG, "SocketWriteThread | close outputstream failed", e);
                }
                os = null;
            }
            if(null != mSocket) {
                try {
                    mSocket.close();
                } catch (Exception e) {
                    // close socket error happened
                	Logging.d(TAG, "SocketWriteThread | close socket failed", e);
                }
                mSocket = null;
            }
        }
    }

	public boolean writeData(byte[] data) {
		try {
            mWaitSendQueue.put(data);
        } catch (Exception ex) {
            return false;
        }
        return true;
	}
	
	public void close() {
		try {
			mSocket.close();
		} catch (IOException e) {
			Logging.d(TAG, "close()| close socket failed");
		}
	}
}
