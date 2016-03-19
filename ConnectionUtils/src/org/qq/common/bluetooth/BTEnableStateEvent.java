package org.qq.common.bluetooth;

import org.qq.common.eventbus.Event;

/***
 * event indicate the device's state
 * @author qq
 *
 */
public class BTEnableStateEvent extends Event {

	private boolean isEnabled;

	public boolean isEnabled() {
		return isEnabled;
	}

	public void setEnabled(boolean isEnabled) {
		this.isEnabled = isEnabled;
	}

	public static BTEnableStateEvent createEvent(boolean isEnabled) {
		BTEnableStateEvent event = new BTEnableStateEvent();
		event.isEnabled = isEnabled;
		return event;
	}
}
