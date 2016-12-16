package com.castel.obd215b;

public class OBD {
	static {
		System.loadLibrary("CRC");
	}

	public static native int CRC(String msg);
}
