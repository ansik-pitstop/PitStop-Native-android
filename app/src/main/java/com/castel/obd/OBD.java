package com.castel.obd;

public class OBD {
	static {
		System.loadLibrary("212B");
	}

	public static native int init(String deviceId, String dataNum);

	public static native String setCtrl(int inCtrlIndex);

	public static native String setMonitor(int type, String valueList);

	public static native String setParameter(String tlvTagList, String valueList);

	public static native String getParameter(String tlvTag);

	public static native String getIOData(String inData);
}
