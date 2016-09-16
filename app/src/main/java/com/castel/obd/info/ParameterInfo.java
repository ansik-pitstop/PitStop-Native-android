package com.castel.obd.info;

public class ParameterInfo {
	public String tlvTag;
	public String value;

	public ParameterInfo(String tlvTag, String value) {
		this.tlvTag = tlvTag;
		this.value = value;
	}
}
