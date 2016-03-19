package org.qq.common.eventbus;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

public class EventBus {
	
	//*******************single instance*************************//
	private static EventBus mInstance;
	
	private EventBus() {
	}
	
	public static EventBus getInstance() {
		if(null == mInstance) {
			synchronized (EventBus.class) {
				if(null == mInstance) {
					mInstance = new EventBus();
				}
			}
		}
		return mInstance;
	}
	
	//*******************single instance*************************//

	//*******************observers******************************//
	private static final String METHOD_HANDLE_EVENT = "handleEvent";
	
	private static final String METHOD_HANDLE_EVENT_UI = "handleEventOnMainThread";
	
	private static final int MSG_HANDLE_EVENT = 1;
	private List<Object> mObservers = new ArrayList<Object>();
	
	private void notifyEvent(Event event) {
		for(Object object : mObservers) {
			if(!notifyEventInternal(object, METHOD_HANDLE_EVENT, event)) {
				Message message = Message.obtain();
				message.what = MSG_HANDLE_EVENT;
				message.obj = new EventItem(object, METHOD_HANDLE_EVENT_UI, event);
				mHandler.sendMessage(message);
			}
		}
	}

	private static boolean notifyEventInternal(Object object, String messageName, Event event) {
		Method method = null;
		try {
			method = object.getClass().getDeclaredMethod(messageName,
					Event.class);
		} catch (NoSuchMethodException ex) {
			return false;
		}
		method.setAccessible(true);
		try {
			method.invoke(object, event);
		} catch(Exception ex) {
			return false;
		}
		return true;
	}
	
	public void register(Object object) {
		mObservers.add(object);
	}
	
	public void unregister(Object object) {
		mObservers.remove(object);
	}
	
	private static Handler mHandler = new Handler(Looper.getMainLooper()) {
		private WeakReference<EventBus> mEventBus;

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_HANDLE_EVENT:
				EventItem eventItem = (EventItem) msg.obj;
				notifyEventInternal(eventItem.object, 
						eventItem.messageName, eventItem.event);
				break;

			default:
				break;
			}
		}
	};
	
	//**************************************************************//
	
	public void sendEvent(Event event) {
		notifyEvent(event);
	}
	
	public void destroy() {
		mObservers.clear();
		mInstance = null;
	}
}
