package com.castel.obd.info;

import java.util.ArrayList;
import java.util.List;

public class ParameterPackageInfo {
	public int result;
	public String deviceId;
	public List<ParameterInfo> value = new ArrayList<ParameterInfo>();

	@Override
	public String toString() {
		return "ParameterPackageInfo{" +
				"result=" + result +
				", deviceId='" + deviceId + '\'' +
				", value=" + value +
				'}';
	}
}
