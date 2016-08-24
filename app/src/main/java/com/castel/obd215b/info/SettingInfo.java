package com.castel.obd215b.info;

import java.io.Serializable;

public class SettingInfo implements Serializable {
	public String terminalSN = "";
	public String bluetoothDeviceName = "";
	public String terminalRTCTime = null;
	public String terminalWorkMode = "0";
	public String theFuelConsumptionType = "10";
	public String theengineEmission = "";
	public String vehicleVINCode = null;
	public String licensePlateNumber = "";
	public String totalMileage = "";
	public String totalFuelConsumption = "";
	public String engineFlameOurDelayTime = "";
	public String swVersion = "";
	public String hardwareVersion = "";
	public String pidCollectType = "";
	public String comprehensiveDataIntervalTime = "";
	public String obdswVersion = "";
	public String obdhardwareVersion = "";
	public String historyCompletionSwitch = "";

	public String powerOnAlarmSwitch = "1";
	public String ignitiOnAlarmSwitch = "1";
	public String flameOutAlarmSwitch = "1";
	public String overSpeedAlarmSwitch = "1";
	public String overSpeedThresholdValue = "";
	public String packingWithoutFlameOutSwitch = "1";
	public String packingWithoutFlameOurTimeThresholdValue = "";
	public String lowVoltageAlarmSwitch = "1";
	public String lowVoltageAlarmThresholdValue = "";
	public String engineCoolantTemperatureTooHighAlarmSwitch = "1";
	public String engineCoolantTemperatureTooHighAlarmThresholdValue = "";
	public String engineRevolutionTooHighAlarmSwitch = "1";
	public String engineRevolutionTooHighAlarmThresholdValue = "";
	public String collisionAlarmSwitch = "1";
	public String collisionAlarmThresholdValue = "";
	public String shakeEventSwitch = "1";
	public String shakeThresholdValue = "";
	public String towAlarmSwitch = "1";
	public String dangerousDriving = "1";
	public String dangerousDrivingThresholdValue = "";
	public String fatigueDrivingSwitch = "1";
	public String fatigueDrivingThresholdValue = "";
	public String accelerationAlarmSwitch = "1";
	public String accelerationAlarmThresholdValue = "";
	public String slowdownAlarmSwitch = "1";
	public String slowdownAlarmThresholdValue = "";
	
	@Override
	public String toString() {
		return "SettingInfo [terminalSN=" + terminalSN
				+ ", bluetoothDeviceName=" + bluetoothDeviceName
				+ ", terminalRTCTime=" + terminalRTCTime
				+ ", terminalWorkMode=" + terminalWorkMode
				+ ", theFuelConsumptionType=" + theFuelConsumptionType
				+ ", theengineEmission=" + theengineEmission
				+ ", vehicleVINCode=" + vehicleVINCode
				+ ", licensePlateNumber=" + licensePlateNumber
				+ ", totalMileage=" + totalMileage + ", totalFuelConsumption="
				+ totalFuelConsumption + ", engineFlameOurDelayTime="
				+ engineFlameOurDelayTime + ", swVersion=" + swVersion
				+ ", hardwareVersion=" + hardwareVersion + ", obdswVersion=" 
				+ obdswVersion +", obdhardwareVersion=" + obdhardwareVersion 
				+ ", historyCompletionSwitch=" + historyCompletionSwitch
				+ ", pidCollectType=" + pidCollectType + ", comprehensiveDataIntervalTime="
				+ comprehensiveDataIntervalTime + ", powerOnAlarmSwitch="
				+ powerOnAlarmSwitch + ", ignitiOnAlarmSwitch="
				+ ignitiOnAlarmSwitch + ", flameOutAlarmSwitch="
				+ flameOutAlarmSwitch + ", overSpeedAlarmSwitch="
				+ overSpeedAlarmSwitch + ", overSpeedThresholdValue="
				+ overSpeedThresholdValue + ", packingWithoutFlameOutSwitch="
				+ packingWithoutFlameOutSwitch
				+ ", packingWithoutFlameOurTimeThresholdValue="
				+ packingWithoutFlameOurTimeThresholdValue
				+ ", lowVoltageAlarmSwitch=" + lowVoltageAlarmSwitch
				+ ", lowVoltageAlarmThresholdValue="
				+ lowVoltageAlarmThresholdValue
				+ ", engineCoolantTemperatureTooHighAlarmSwitch="
				+ engineCoolantTemperatureTooHighAlarmSwitch
				+ ", engineCoolantTemperatureTooHighAlarmThresholdValue="
				+ engineCoolantTemperatureTooHighAlarmThresholdValue
				+ ", engineRevolutionTooHighAlarmSwitch="
				+ engineRevolutionTooHighAlarmSwitch
				+ ", engineRevolutionTooHighAlarmThresholdValue="
				+ engineRevolutionTooHighAlarmThresholdValue
				+ ", collisionAlarmSwitch=" + collisionAlarmSwitch
				+ ", collisionAlarmThresholdValue="
				+ collisionAlarmThresholdValue + ", shakeEventSwitch="
				+ shakeEventSwitch + ", shakeThresholdValue="
				+ shakeThresholdValue + ", towAlarmSwitch=" + towAlarmSwitch
				+ ", dangerousDriving=" + dangerousDriving
				+ ", dangerousDrivingThresholdValue="
				+ dangerousDrivingThresholdValue + ", fatigueDrivingSwitch="
				+ fatigueDrivingSwitch + ", fatigueDrivingThresholdValue="
				+ fatigueDrivingThresholdValue + ", accelerationAlarmSwitch="
				+ accelerationAlarmSwitch + ", accelerationAlarmThresholdValue="
				+ accelerationAlarmThresholdValue + ", slowdownAlarmSwitch="
				+ slowdownAlarmSwitch + ", slowdownAlarmThresholdValue="
				+ slowdownAlarmThresholdValue +  "]";
	}

}
