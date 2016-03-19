package org.qq.common.eventbus;

public class EventItem {
	Object object;
	String messageName;
	Event event;
	
	public EventItem(Object object, String messageName, Event event) {
		this.object = object;
		this.messageName = messageName;
		this.event = event;
	}
	
}
