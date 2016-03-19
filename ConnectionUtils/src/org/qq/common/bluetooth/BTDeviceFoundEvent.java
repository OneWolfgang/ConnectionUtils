package org.qq.common.bluetooth;

import org.qq.common.eventbus.Event;

import android.bluetooth.BluetoothDevice;

/***
 * event indicate a device is found
 * @author qq
 *
 */
public class BTDeviceFoundEvent extends Event {
	private BluetoothDevice mDevice;

	public BluetoothDevice getDevice() {
		return mDevice;
	}

	public void setDevice(BluetoothDevice mDevice) {
		this.mDevice = mDevice;
	}
	
	public static BTDeviceFoundEvent createEvent(BluetoothDevice device) {
		BTDeviceFoundEvent event = new BTDeviceFoundEvent();
		event.mDevice = device;
		return event;
	}
}
