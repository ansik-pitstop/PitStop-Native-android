package com.castel.obd215b.info;

import java.io.Serializable;

public class IDRInfo implements Serializable {
	public String terminalSN = "0";
	public String idr;
	public String ignitionTime;
	public String runTime;
	public String locateStatus;
	public String latitude;
	public String longitude;
	public String gpsDataAndTime;
	public String satelliteNumber;
	public String gpsSpeed;
	public String direction;
	public String altitude;
	public String horizontalPositionPrecision;
	public String vehicleStatusBit;
	public String alarmEvents;
	public String alarmValues;
	public String mileage;
	public String fuelConsumption;
//	public String transmitterSpeed;
//	public String speed;
//	public String engineCoolantTemperature;
	public String vehicleVoltage;
	public String mil;
	public String diagnosisProtocol;
	public String pid;
	public String dtc;
	public String freezeFrame;
	public String snapshot;
//	public String checkingCode;
	public String time;   // 时间(由APP生成)

	@Override
	public String toString() {
		return "IDRInfo [terminalSN=" + terminalSN + ", idr=" + idr
				+ ", ignitionTime=" + ignitionTime + ", runTime=" + runTime
				+ ", locateStatus=" + locateStatus + ", latitude=" + latitude
				+ ", longitude=" + longitude + ", gpsDataAndTime="
				+ gpsDataAndTime + ", satelliteNumber=" + satelliteNumber
				+ ", gpsSpeed=" + gpsSpeed + ", direction=" + direction
				+ ", altitude=" + altitude + ", horizontalPositionPrecision="
				+ horizontalPositionPrecision + ", vehicleStatusBit="
				+ vehicleStatusBit + ", alarmEvents=" + alarmEvents
				+ ", alarmValues=" + alarmValues + ", mileage=" + mileage
				+ ", fuelConsumption=" + fuelConsumption
				+ ", vehicleVoltage=" + vehicleVoltage + ", mil=" + mil
				+ ", diagnosisProtocol=" + diagnosisProtocol + ", pid=" + pid
				+ ", dtc=" + dtc + ", freezeFrame=" + freezeFrame
				+ ", snapshot=" + snapshot
				+ "]";
	}

}
