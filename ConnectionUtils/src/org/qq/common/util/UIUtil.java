package org.qq.common.util;

import android.content.Context;
import android.widget.Toast;

public class UIUtil {

	/***
	 * show a short toast
	 * @param context
	 * @param toast
	 */
	public static void showToast(Context context, String toast) {
		Toast.makeText(context, toast, Toast.LENGTH_SHORT).show();
	}
}
