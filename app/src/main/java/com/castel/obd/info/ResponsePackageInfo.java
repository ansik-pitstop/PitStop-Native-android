package com.castel.obd.info;

public class ResponsePackageInfo {
	public int result;
	public String deviceId;
	public String flag;
	public String type;
	public String value;

	@Override
	public String toString() {
		return "ResponsePackageInfo{" +
				"result=" + result +
				", deviceId='" + deviceId + '\'' +
				", flag='" + flag + '\'' +
				", type='" + type + '\'' +
				", value='" + value + '\'' +
				'}';
	}
}
