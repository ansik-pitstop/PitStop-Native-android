package com.castel.obd215b.util;

import com.castel.obd215b.info.DTCInfo;
import com.castel.obd215b.info.IDRInfo;
import com.castel.obd215b.info.PIDInfo;
import com.castel.obd215b.info.SettingInfo;

public class DataParseUtil {

	public static String parseMsgType(String msg) {
		String[] msgs = msg.split(",");
		if (Constants.INSTRUCTION_IDR.equalsIgnoreCase(msgs[1])) {
			return Constants.INSTRUCTION_IDR;
		} else if (Constants.INSTRUCTION_CI.equalsIgnoreCase(msgs[1])) {
			return Constants.INSTRUCTION_CI;
		} else if (Constants.INSTRUCTION_SI.equalsIgnoreCase(msgs[1])) {
			return Constants.INSTRUCTION_SI;
		} else if (Constants.INSTRUCTION_QI.equalsIgnoreCase(msgs[1])) {
			return Constants.INSTRUCTION_QI;
		} else if (Constants.INSTRUCTION_PIDT.equalsIgnoreCase(msgs[1])) {
			return Constants.INSTRUCTION_PIDT;
		} else if (Constants.INSTRUCTION_PID.equalsIgnoreCase(msgs[1])) {
			return Constants.INSTRUCTION_PID;
		} else if (Constants.INSTRUCTION_DTC.equalsIgnoreCase(msgs[1])) {
			return Constants.INSTRUCTION_DTC;
		} else if (Constants.INSTRUCTION_OTA.equalsIgnoreCase(msgs[1])) {
			return Constants.INSTRUCTION_OTA;
		} else if (Constants.INSTRUCTION_TEST.equalsIgnoreCase(msgs[1])) {
			return Constants.INSTRUCTION_TEST;
		}
		return "";
	}

	public static IDRInfo parseIDR(String msg) {
		IDRInfo idrInfo = new IDRInfo();
		String[] msgs = msg.split(",");

		idrInfo.terminalSN = msgs[0].substring(2);
		idrInfo.idr = msgs[1];
		idrInfo.ignitionTime = msgs[2];
		idrInfo.runTime = msgs[3];
		idrInfo.locateStatus = msgs[4];
		idrInfo.latitude = msgs[5];
		idrInfo.longitude = msgs[6];
		idrInfo.gpsDataAndTime = msgs[7];
		idrInfo.satelliteNumber = msgs[8];
		idrInfo.gpsSpeed = msgs[9];
		idrInfo.direction = msgs[10];
		idrInfo.altitude = msgs[11];
		idrInfo.horizontalPositionPrecision = msgs[12];
		idrInfo.vehicleStatusBit = msgs[13];
		idrInfo.alarmEvents = msgs[14];
		idrInfo.alarmValues = msgs[15];
		idrInfo.mileage = msgs[16];
		idrInfo.fuelConsumption = msgs[17];
//		idrInfo.transmitterSpeed = msgs[18];
//		idrInfo.speed = msgs[19];
//		idrInfo.engineCoolantTemperature = msgs[20];
		idrInfo.vehicleVoltage = msgs[18];
		idrInfo.mil = msgs[19];
		idrInfo.diagnosisProtocol = msgs[20];
		idrInfo.pid = msgs[21];
		idrInfo.dtc = msgs[22];
		idrInfo.freezeFrame = msgs[23];
		idrInfo.snapshot = msgs[24];
//		idrInfo.checkingCode = msgs[28].substring(0, 5);

		return idrInfo;
	}

	public static DTCInfo parseDTC(String msg) {
		DTCInfo dtcInfo = new DTCInfo();
		String[] msgs = msg.split(",");
		dtcInfo.terminalId = msgs[0].substring(2);
		dtcInfo.dtcType = Integer.parseInt(msgs[2]);
		LogUtil.d("故障码："+dtcInfo.dtcType);
		
		if(Integer.parseInt(msgs[2]) > 1)
			return dtcInfo;

//		if (3 != Integer.parseInt(msgs[2])) {
			dtcInfo.diagnosisProtocol = msgs[3];
			dtcInfo.dtcNumber = Integer.parseInt(msgs[4]);
			dtcInfo.dtcs = new String[dtcInfo.dtcNumber];
			for (int i = 0; i < dtcInfo.dtcNumber; i++) {
				dtcInfo.dtcs[i] = msgs[5 + i];
			}
//		}

		return dtcInfo;
	}

	public static boolean parseSetting(String msg) {
		String[] msgs = msg.split(",");
		if ("0".equals(msgs[2])) {
			return true;
		} else {
			return false;
		}
	}

	public static SettingInfo parseQI(String msg) {
		String[] msgs = msg.split(",");
		SettingInfo settingInfo = new SettingInfo();

		int num = Integer.parseInt(msgs[2]);
		for (int i = 0; i < num * 2; i = i + 2) {
			int positionKey = i + 3;
			int positionValue = i + 4;
			if ("A01".equals(msgs[positionKey])) {
				settingInfo.terminalSN = msgs[positionValue];
			} else if ("A02".equals(msgs[positionKey])) {
				settingInfo.bluetoothDeviceName = msgs[positionValue];
			} else if ("A03".equals(msgs[positionKey])) {
				settingInfo.terminalRTCTime = msgs[positionValue];
			} else if ("A04".equals(msgs[positionKey])) {
				settingInfo.terminalWorkMode = msgs[positionValue];
			} else if ("A05".equals(msgs[positionKey])) {
				settingInfo.theFuelConsumptionType = msgs[positionValue];
			} else if ("A06".equals(msgs[positionKey])) {
				settingInfo.theengineEmission = msgs[positionValue];
			} else if ("A07".equals(msgs[positionKey])) {
				settingInfo.vehicleVINCode = msgs[positionValue];
			} else if ("A08".equals(msgs[positionKey])) {
				settingInfo.licensePlateNumber = msgs[positionValue];
			} else if ("A09".equals(msgs[positionKey])) {
				settingInfo.totalMileage = msgs[positionValue];
			} else if ("A10".equals(msgs[positionKey])) {
				settingInfo.totalFuelConsumption = msgs[positionValue];
			} else if ("A11".equals(msgs[positionKey])) {
				settingInfo.engineFlameOurDelayTime = msgs[positionValue];
			} else if ("A12".equals(msgs[positionKey])) {
				settingInfo.swVersion = msgs[positionValue];
			} else if ("A13".equals(msgs[positionKey])) {
				settingInfo.hardwareVersion = msgs[positionValue];
			} else if ("A14".equals(msgs[positionKey])) {
				settingInfo.pidCollectType = msgs[positionValue];
			} else if ("A15".equals(msgs[positionKey])) {
				settingInfo.comprehensiveDataIntervalTime = msgs[positionValue];
			} else if ("A16".equals(msgs[positionKey])) {
				settingInfo.obdswVersion = msgs[positionValue];
			} else if ("A17".equals(msgs[positionKey])) {
				settingInfo.obdhardwareVersion = msgs[positionValue];
			} else if ("A18".equals(msgs[positionKey])) {
				settingInfo.historyCompletionSwitch = msgs[positionValue];
			} else if ("B01".equals(msgs[positionKey])) {
				settingInfo.powerOnAlarmSwitch = msgs[positionValue];
			} else if ("B02".equals(msgs[positionKey])) {
				settingInfo.ignitiOnAlarmSwitch = msgs[positionValue];
			} else if ("B03".equals(msgs[positionKey])) {
				settingInfo.flameOutAlarmSwitch = msgs[positionValue];
			} else if ("B04".equals(msgs[positionKey])) {
				settingInfo.overSpeedAlarmSwitch = msgs[positionValue];
			} else if ("B05".equals(msgs[positionKey])) {
				settingInfo.overSpeedThresholdValue = msgs[positionValue];
			} else if ("B06".equals(msgs[positionKey])) {
				settingInfo.packingWithoutFlameOutSwitch = msgs[positionValue];
			} else if ("B07".equals(msgs[positionKey])) {
				settingInfo.packingWithoutFlameOurTimeThresholdValue = msgs[positionValue];
			} else if ("B08".equals(msgs[positionKey])) {
				settingInfo.lowVoltageAlarmSwitch = msgs[positionValue];
			} else if ("B09".equals(msgs[positionKey])) {
				settingInfo.lowVoltageAlarmThresholdValue = msgs[positionValue];
			} else if ("B10".equals(msgs[positionKey])) {
				settingInfo.engineCoolantTemperatureTooHighAlarmSwitch = msgs[positionValue];
			} else if ("B11".equals(msgs[positionKey])) {
				settingInfo.engineCoolantTemperatureTooHighAlarmThresholdValue = msgs[positionValue];
			} else if ("B12".equals(msgs[positionKey])) {
				settingInfo.engineRevolutionTooHighAlarmSwitch = msgs[positionValue];
			} else if ("B13".equals(msgs[positionKey])) {
				settingInfo.engineRevolutionTooHighAlarmThresholdValue = msgs[positionValue];
			} else if ("B14".equals(msgs[positionKey])) {
				settingInfo.collisionAlarmSwitch = msgs[positionValue];
			} else if ("B15".equals(msgs[positionKey])) {
				settingInfo.collisionAlarmThresholdValue = msgs[positionValue];
			} else if ("B16".equals(msgs[positionKey])) {
				settingInfo.shakeEventSwitch = msgs[positionValue];
			} else if ("B17".equals(msgs[positionKey])) {
				settingInfo.shakeThresholdValue = msgs[positionValue];
			} else if ("B18".equals(msgs[positionKey])) {
				settingInfo.towAlarmSwitch = msgs[positionValue];
			} else if ("B19".equals(msgs[positionKey])) {
				settingInfo.dangerousDriving = msgs[positionValue];
			} else if ("B20".equals(msgs[positionKey])) {
				settingInfo.dangerousDrivingThresholdValue = msgs[positionValue];
			} else if ("B21".equals(msgs[positionKey])) {
				settingInfo.fatigueDrivingSwitch = msgs[positionValue];
			} else if ("B22".equals(msgs[positionKey])) {
				settingInfo.fatigueDrivingThresholdValue = msgs[positionValue];
			} else if ("B23".equals(msgs[positionKey])) {
				settingInfo.accelerationAlarmSwitch = msgs[positionValue];
			} else if ("B24".equals(msgs[positionKey])) {
				settingInfo.accelerationAlarmThresholdValue = msgs[positionValue];
			} else if ("B25".equals(msgs[positionKey])) {
				settingInfo.slowdownAlarmSwitch = msgs[positionValue];
			} else if ("B26".equals(msgs[positionKey])) {
				settingInfo.slowdownAlarmThresholdValue = msgs[positionValue];
			}
		}

		return settingInfo;
	}

	public static PIDInfo parsePIDT(String msg) {
		String[] msgs = msg.split(",");
		PIDInfo pidInfo = new PIDInfo();
		pidInfo.terminalId = msgs[0].substring(2);
		pidInfo.diagnoseProtocol = msgs[2];
		pidInfo.pidNumber = Integer.parseInt(msgs[3]);
		if (0 < pidInfo.pidNumber) {
			for (int i = 0; i < pidInfo.pidNumber; i++) {
				pidInfo.pids.add(msgs[i + 4]);
			}
		}

		return pidInfo;
	}

	public static PIDInfo parsePID(String msg) {
		
		//msg = "$$HT,PID,03,34,2103,0100,2104,35,2105,00,2106,00,2107,00,210C,03B8,210D,5A,210E,3F,210F,D8,2110,01D8,2111,3C,2113,03,2115,3863,211C,01,211F,0000,2121,0000,2124,80D5412E,212E,00,212F,64,2130,00,2131,0000,2132,0000,2133,01,213C,0000,2142,2EE0,2143,00,2144,0000,2145,06,2147,64,2149,00,214A,00,214C,05,214D,0000,214E,0000,*40D5";

		String[] msgs = msg.split(",");
		PIDInfo pidInfo = new PIDInfo();
		pidInfo.diagnoseProtocol = msgs[2];
		pidInfo.pidNumber = Integer.parseInt(msgs[3]);
		if (0 < pidInfo.pidNumber) {
			for (int i = 0; i < pidInfo.pidNumber*2; i = i + 2) {
				pidInfo.pids.add(msgs[i + 4]);
				pidInfo.pidValues.add(msgs[i + 5]);
			}
		}

		return pidInfo;
	}

	/*
	 * 0 ¨¦y??3¨¦1| 1 ¨¦y???D 2 ¨¦y??¨º¡ì¡ã¨¹ 3 ¨¦y??¨¨???
	 */
	public static int parseOTA(String msg) {
		String[] msgs = msg.split(",");
		return Integer.parseInt(msgs[3]);
	}
	
	/*
	 * 获取升级类型  
	 * 1：OBD模块
	 * 0：终端模块
	 */
	public static int parseUpgradeType(String msg) {
		String[] msgs = msg.split(",");
		return Integer.parseInt(msgs[2]);
	}
}
