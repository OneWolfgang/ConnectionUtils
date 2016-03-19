package org.qq.common.socket;

import java.util.Arrays;

import org.qq.common.eventbus.Event;

import android.R.integer;

public class ReadSocketDataEvent extends Event {
	
	private byte[] data;

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}
	
	public static ReadSocketDataEvent createEvent(byte[] data, int start, int end) {
		byte[] resultData = new byte[end - start];
		ReadSocketDataEvent event = new ReadSocketDataEvent();
		System.arraycopy(data, start, resultData, 0, end - start);
		event.data = resultData;
		return event;
	}

}
