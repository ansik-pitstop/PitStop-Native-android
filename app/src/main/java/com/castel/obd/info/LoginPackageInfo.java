package com.castel.obd.info;

public class LoginPackageInfo {
	public int result;
	public String instruction;
	public String deviceId;
	public String flag;

	@Override
	public String toString() {
		return "LoginPackageInfo{" +
				"result=" + result +
				", instruction='" + instruction + '\'' +
				", deviceId='" + deviceId + '\'' +
				", flag='" + flag + '\'' +
				'}';
	}
}
