package org.qq.common.socket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/***
 * abstract socket for connection
 * @author qq
 *
 */
public interface ISocket {
	InputStream getInputStream() throws IOException;
	
	OutputStream getOutputStream() throws IOException;
	
	boolean isClosed();
	
	void close() throws IOException;
}
