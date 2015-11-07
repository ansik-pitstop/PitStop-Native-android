package com.castel.obd.info;

import java.util.ArrayList;
import java.util.List;

public class DataPackageInfo {

	public int result;
	public String deviceId;
	public String tripId;
	public String dataNumber;
	public String tripFlag;
	public String rtcTime;
	public String protocolType;
	public String tripMileage;
	public String tripfuel;
	public String vState;

	public List<GPSDataInfo> gpsData = new ArrayList<GPSDataInfo>();

	public List<PIDInfo> obdData = new ArrayList<PIDInfo>();

	public List<PIDInfo> freezeData = new ArrayList<PIDInfo>();

	public String surportPid;
	public String dtcData;

	public AlarmInfo alarmData;

}
