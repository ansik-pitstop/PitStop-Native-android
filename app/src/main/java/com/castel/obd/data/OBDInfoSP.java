package com.castel.obd.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.castel.obd.util.LogUtil;

public class OBDInfoSP {
	private final static String FILE_NAME = "OBD_INFO";

	/**
	 * ???????????OBD?ıÙID??????????????ID
	 * @param context
	 * @param deviceId
	 * @param dataNum
	 */
	public static void saveInfo(Context context, String deviceId, String dataNum) {
		SharedPreferences sp = context.getSharedPreferences(FILE_NAME,
				Context.MODE_PRIVATE);
		Editor editor = sp.edit();
		editor.putString("DEVICE_ID", deviceId);
		editor.putString("DATA_NUM", dataNum);
		editor.commit();
	}

	public static void saveMacAddress(Context context, String macAddress) {
		SharedPreferences sp = context.getSharedPreferences(FILE_NAME,
				Context.MODE_PRIVATE);
		Editor editor = sp.edit();
		editor.putString("MAC_ADDRESS", macAddress);
		editor.commit();
		LogUtil.i("save MAC_ADDRESS:" + macAddress);
	}

	public static void saveCarType(Context context, int carType) {
		SharedPreferences sp = context.getSharedPreferences(FILE_NAME,
				Context.MODE_PRIVATE);
		Editor editor = sp.edit();
		editor.putInt("CAR_TYPE", carType);
		editor.commit();
		LogUtil.i("save CarType:" + carType);
	}

	public static String getDeviceId(Context context) {
		SharedPreferences sp = context.getSharedPreferences(FILE_NAME,
				Context.MODE_PRIVATE);
		return sp.getString("DEVICE_ID", "");
	}

	public static String getDataNum(Context context) {
		SharedPreferences sp = context.getSharedPreferences(FILE_NAME,
				Context.MODE_PRIVATE);
		return sp.getString("DATA_NUM", "");
	}

	public static String getMacAddress(Context context) {
		SharedPreferences sp = context.getSharedPreferences(FILE_NAME,
				Context.MODE_PRIVATE);
		LogUtil.i("MAC_ADDRESS:" + sp.getString("MAC_ADDRESS", ""));
		return sp.getString("MAC_ADDRESS", "");
	}

	public static int getCarType(Context context) {
		SharedPreferences sp = context.getSharedPreferences(FILE_NAME,
				Context.MODE_PRIVATE);
		LogUtil.i("CAR_TYPE:" + sp.getInt("CAR_TYPE", 0));
		return sp.getInt("CAR_TYPE", 0);
	}

}
