package com.castel.obd215b.data;

import com.castel.obd215b.util.LogUtil;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class OBDInfoSP {
	private final static String FILE_NAME = "OBD_INFO";

	public static void saveMacAddress(Context context, String macAddress) {
		SharedPreferences sp = context.getSharedPreferences(FILE_NAME,
				Context.MODE_PRIVATE);
		Editor editor = sp.edit();
		editor.putString("MAC_ADDRESS", macAddress);
		editor.commit();
		LogUtil.i("save MAC_ADDRESS:" + macAddress);
	}

	public static String getMacAddress(Context context) {
		SharedPreferences sp = context.getSharedPreferences(FILE_NAME,
				Context.MODE_PRIVATE);
		LogUtil.i("MAC_ADDRESS:" + sp.getString("MAC_ADDRESS", ""));
		return sp.getString("MAC_ADDRESS", "");
	}

}
