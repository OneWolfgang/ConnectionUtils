package org.qq.common.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.net.Uri;

/**
 * qq
 * 2016/3/6.
 */
public class CommonUtils {
	
	public static String intToIp(int ip) {
		StringBuilder sb = new StringBuilder();
		sb.append((ip & 0xff)).append('.')
				.append((ip >> 8) & 0xff).append('.')
				.append((ip >> 16) & 0xff).append('.')
				.append(((ip >> 24) & 0xff));
		return sb.toString();
	}

    public static int ipToInt(String ipAddress) {

        int result = 0;

        String[] ipAddressInArray = ipAddress.split("\\.");

        for (int i = 3; i >= 0; i--) {

            int ip = Integer.parseInt(ipAddressInArray[3 - i]);

            //left shifting 24,16,8,0 and bitwise OR

            //1. 192 << 24
            //1. 168 << 16
            //1. 1   << 8
            //1. 2   << 0
            result |= ip << (i * 8);

        }
        return result;
    }
    
    public static int parsePort(Uri uri) {
    	return uri.getPort();
    }
    
    public static boolean isEmpty(Collection collection) {
    	return null == collection || collection.isEmpty();
    }
    
    public static <T> List<T> asList(T t) {
    	if(null == t) {
    		return null;
    	}
    	List<T> tList = new ArrayList<T>();
    	tList.add(t);
    	return tList;
    }
}
