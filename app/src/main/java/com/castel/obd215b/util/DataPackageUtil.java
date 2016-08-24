package com.castel.obd215b.util;

import com.castel.obd.OBD;
import com.castel.obd215b.info.SettingInfo;

public class DataPackageUtil {

	// parameter IDs
	public static final String TERMINAL_ID_PARAM = "A01";
	public static final String BT_NAME_PARAM = "A02";
	public static final String RTC_TIME_PARAM = "A03";
	public static final String VIN_PARAM = "A07";

	public static String ciPackage(String controlEventID, String terminalSN) {
		String crcData = Constants.INSTRUCTION_HEAD + terminalSN + ","
				+ Constants.INSTRUCTION_CI + "," + controlEventID + ","
				+ Constants.INSTRUCTION_STAR;

		// String crc = Integer.toHexString(OBD.CRC(crcData)).toUpperCase();
		String crc = Utils.toHexString(OBD.CRC(crcData));

		String msg = crcData + crc + Constants.INSTRUCTION_FOOD;

		return msg;
	}

	public static String dtcPackage(String controlEventID, String terminalSN) {
		String crcData = Constants.INSTRUCTION_HEAD + terminalSN + ","
				+ Constants.INSTRUCTION_DTC + "," + controlEventID + ","
				+ Constants.INSTRUCTION_STAR;

		// String crc = Integer.toHexString(OBD.CRC(crcData)).toUpperCase();
		String crc = Utils.toHexString(OBD.CRC(crcData));

		String msg = crcData + crc + Constants.INSTRUCTION_FOOD;

		return msg;
	}

	public static String pidtPackage(String terminalSN) {
		String crcData = Constants.INSTRUCTION_HEAD + terminalSN + ","
				+ Constants.INSTRUCTION_PIDT + "," + Constants.INSTRUCTION_STAR;

		// String crc = Integer.toHexString(OBD.CRC(crcData)).toUpperCase();
		String crc = Utils.toHexString(OBD.CRC(crcData));

		String msg = crcData + crc + Constants.INSTRUCTION_FOOD;

		return msg;
	}

	public static String pidPackage(String controlEventID, int pidNum,
			String pids, String terminalSN) {
		String crcData = "";
		if (0 == pidNum) {
			crcData = Constants.INSTRUCTION_HEAD + terminalSN + ","
					+ Constants.INSTRUCTION_PID + "," + controlEventID + ","
					+ Constants.INSTRUCTION_STAR;
		} else {
			crcData = Constants.INSTRUCTION_HEAD + terminalSN + ","
					+ Constants.INSTRUCTION_PID + "," + controlEventID + ","
					+ pidNum + "," + pids + "," + Constants.INSTRUCTION_STAR;
		}

		// String crc = Integer.toHexString(OBD.CRC(crcData)).toUpperCase();
		String crc = Utils.toHexString(OBD.CRC(crcData));

		String msg = crcData + crc + Constants.INSTRUCTION_FOOD;

		return msg;
	}

	public static String qiPackage(int type, String terminalSN) {
		String crcData = "";
		if (0 == type) {
			crcData = Constants.INSTRUCTION_HEAD
					+ terminalSN
					+ ","
					+ Constants.INSTRUCTION_QI
					+ ",18,A01,A02,A03,A04,A05,A06,A07,A08,A09,A10,A11,A12,A13,A14,A15,A16,A17,A18,"
					+ Constants.INSTRUCTION_STAR;
		} else if (1 == type) {
			crcData = Constants.INSTRUCTION_HEAD
					+ terminalSN
					+ ","
					+ Constants.INSTRUCTION_QI
					+ ",27,A01,B01,B02,B03,B04,B05,B06,B07,B08,B09,B10,B11,B12,B13,B14,B15,B16,B17,B18,B19,B20,B21,B22,B23,B24,B25,B26,"
					+ Constants.INSTRUCTION_STAR;
		}

		// String crc = Integer.toHexString(OBD.CRC(crcData)).toUpperCase();
		String crc = Utils.toHexString(OBD.CRC(crcData));

		String msg = crcData + crc + Constants.INSTRUCTION_FOOD;

		return msg;
	}

	public static String getParameter(String param) {
		String crcData = Constants.INSTRUCTION_HEAD
				+ "0"
				+ ","
				+ Constants.INSTRUCTION_QI
				+ ",1,"
				+ param
				+ ","
				+ Constants.INSTRUCTION_STAR;

		String crc = Utils.toHexString(OBD.CRC(crcData));

		String msg = crcData + crc + Constants.INSTRUCTION_FOOD;

		return msg;
	}

	public static String setParameter(String param, String value) {
		String crcData = Constants.INSTRUCTION_HEAD
				+ "0"
				+ ","
				+ Constants.INSTRUCTION_SI
				+ ",1,"
				+ param
				+ ","
				+ value
				+ ","
				+ Constants.INSTRUCTION_STAR;

		String crc = Utils.toHexString(OBD.CRC(crcData));

		String msg = crcData + crc + Constants.INSTRUCTION_FOOD;

		return msg;
	}

	public static String siPackage(int type, SettingInfo settingInfo,
			String terminalSN) {
		String crcData = "";

		int num = 0;
		StringBuffer value = new StringBuffer();
		if (0 == type) {
			if (!Utils.isEmpty(settingInfo.terminalSN)
					&& !settingInfo.terminalSN.equals(terminalSN)) {
				num = num + 1;
				value.append("A01," + settingInfo.terminalSN + ",");
			}
			if (!Utils.isEmpty(settingInfo.bluetoothDeviceName)) {
				num = num + 1;
				value.append("A02," + settingInfo.bluetoothDeviceName + ",");
			}
			if (!Utils.isEmpty(settingInfo.terminalRTCTime)) {
				num = num + 1;
				value.append("A03," + settingInfo.terminalRTCTime + ",");
			}
			if (!Utils.isEmpty(settingInfo.terminalWorkMode)) {
				num = num + 1;
				value.append("A04," + settingInfo.terminalWorkMode + ",");
			}
			if (!Utils.isEmpty(settingInfo.theFuelConsumptionType)) {
				num = num + 1;
				value.append("A05," + settingInfo.theFuelConsumptionType + ",");
			}
			if (!Utils.isEmpty(settingInfo.theengineEmission)) {
				num = num + 1;
				value.append("A06," + settingInfo.theengineEmission + ",");
			}
			if (!Utils.isEmpty(settingInfo.licensePlateNumber)) {
				num = num + 1;
				value.append("A08," + settingInfo.licensePlateNumber + ",");
			}
			if (!Utils.isEmpty(settingInfo.totalMileage)) {
				num = num + 1;
				value.append("A09," + settingInfo.totalMileage + ",");
			}
			if (!Utils.isEmpty(settingInfo.totalFuelConsumption)) {
				num = num + 1;
				value.append("A10," + settingInfo.totalFuelConsumption + ",");
			}
			if (!Utils.isEmpty(settingInfo.engineFlameOurDelayTime)) {
				num = num + 1;
				value.append("A11," + settingInfo.engineFlameOurDelayTime + ",");
			}
			/**
			 * 只读 if (!Utils.isEmpty(settingInfo.swVersion)) { num = num + 1;
			 * value.append("A12," + settingInfo.swVersion + ","); } if
			 * (!Utils.isEmpty(settingInfo.hardwareVersion)) { num = num + 1;
			 * value.append("A13," + settingInfo.hardwareVersion + ","); }
			 */
			if (!Utils.isEmpty(settingInfo.pidCollectType)) {
				num = num + 1;
				value.append("A14," + settingInfo.pidCollectType + ",");
			}
			if (!Utils.isEmpty(settingInfo.comprehensiveDataIntervalTime)) {
				num = num + 1;
				value.append("A15," + settingInfo.comprehensiveDataIntervalTime
						+ ",");
			}
			/**
			 * 只读 if (!Utils.isEmpty(settingInfo.obdswVersion)) { num = num + 1;
			 * value.append("A16," + settingInfo.obdswVersion + ","); } if
			 * (!Utils.isEmpty(settingInfo.obdhardwareVersion)) { num = num + 1;
			 * value.append("A17," + settingInfo.obdhardwareVersion + ","); }
			 */
			if (!Utils.isEmpty(settingInfo.historyCompletionSwitch)) {
				num = num + 1;
				value.append("A18," + settingInfo.historyCompletionSwitch + ",");
			}

		} else if (1 == type) {
			if (!Utils.isEmpty(settingInfo.terminalSN)
					&& !settingInfo.terminalSN.equals(terminalSN)) {
				num = num + 1;
				value.append("A01," + settingInfo.terminalSN + ",");
			}
			if (!Utils.isEmpty(settingInfo.powerOnAlarmSwitch)) {
				num = num + 1;
				value.append("B01," + settingInfo.powerOnAlarmSwitch + ",");
			}
			if (!Utils.isEmpty(settingInfo.ignitiOnAlarmSwitch)) {
				num = num + 1;
				value.append("B02," + settingInfo.ignitiOnAlarmSwitch + ",");
			}
			if (!Utils.isEmpty(settingInfo.flameOutAlarmSwitch)) {
				num = num + 1;
				value.append("B03," + settingInfo.flameOutAlarmSwitch + ",");
			}
			if (!Utils.isEmpty(settingInfo.overSpeedAlarmSwitch)) {
				num = num + 1;
				value.append("B04," + settingInfo.overSpeedAlarmSwitch + ",");
			}
			if (!Utils.isEmpty(settingInfo.overSpeedThresholdValue)) {
				num = num + 1;
				value.append("B05," + settingInfo.overSpeedThresholdValue + ",");
			}
			if (!Utils.isEmpty(settingInfo.packingWithoutFlameOutSwitch)) {
				num = num + 1;
				value.append("B06," + settingInfo.packingWithoutFlameOutSwitch
						+ ",");
			}
			if (!Utils
					.isEmpty(settingInfo.packingWithoutFlameOurTimeThresholdValue)) {
				num = num + 1;
				value.append("B07,"
						+ settingInfo.packingWithoutFlameOurTimeThresholdValue
						+ ",");
			}
			if (!Utils.isEmpty(settingInfo.lowVoltageAlarmSwitch)) {
				num = num + 1;
				value.append("B08," + settingInfo.lowVoltageAlarmSwitch + ",");
			}
			if (!Utils.isEmpty(settingInfo.lowVoltageAlarmThresholdValue)) {
				num = num + 1;
				value.append("B09," + settingInfo.lowVoltageAlarmThresholdValue
						+ ",");
			}
			if (!Utils
					.isEmpty(settingInfo.engineCoolantTemperatureTooHighAlarmSwitch)) {
				num = num + 1;
				value.append("B10,"
						+ settingInfo.engineCoolantTemperatureTooHighAlarmSwitch
						+ ",");
			}
			if (!Utils
					.isEmpty(settingInfo.engineCoolantTemperatureTooHighAlarmThresholdValue)) {
				num = num + 1;
				value.append("B11,"
						+ settingInfo.engineCoolantTemperatureTooHighAlarmThresholdValue
						+ ",");
			}
			if (!Utils.isEmpty(settingInfo.engineRevolutionTooHighAlarmSwitch)) {
				num = num + 1;
				value.append("B12,"
						+ settingInfo.engineRevolutionTooHighAlarmSwitch + ",");
			}
			if (!Utils
					.isEmpty(settingInfo.engineRevolutionTooHighAlarmThresholdValue)) {
				num = num + 1;
				value.append("B13,"
						+ settingInfo.engineRevolutionTooHighAlarmThresholdValue
						+ ",");
			}
			if (!Utils.isEmpty(settingInfo.collisionAlarmSwitch)) {
				num = num + 1;
				value.append("B14," + settingInfo.collisionAlarmSwitch + ",");
			}
			if (!Utils.isEmpty(settingInfo.collisionAlarmThresholdValue)) {
				num = num + 1;
				value.append("B15," + settingInfo.collisionAlarmThresholdValue
						+ ",");
			}
			if (!Utils.isEmpty(settingInfo.shakeEventSwitch)) {
				num = num + 1;
				value.append("B16," + settingInfo.shakeEventSwitch + ",");
			}
			if (!Utils.isEmpty(settingInfo.shakeThresholdValue)) {
				num = num + 1;
				value.append("B17," + settingInfo.shakeThresholdValue + ",");
			}
			if (!Utils.isEmpty(settingInfo.towAlarmSwitch)) {
				num = num + 1;
				value.append("B18," + settingInfo.towAlarmSwitch + ",");
			}
			if (!Utils.isEmpty(settingInfo.dangerousDriving)) {
				num = num + 1;
				value.append("B19," + settingInfo.dangerousDriving + ",");
			}
			if (!Utils.isEmpty(settingInfo.dangerousDrivingThresholdValue)) {
				num = num + 1;
				value.append("B20,"
						+ settingInfo.dangerousDrivingThresholdValue + ",");
			}
			if (!Utils.isEmpty(settingInfo.fatigueDrivingSwitch)) {
				num = num + 1;
				value.append("B21," + settingInfo.fatigueDrivingSwitch + ",");
			}
			if (!Utils.isEmpty(settingInfo.fatigueDrivingThresholdValue)) {
				num = num + 1;
				value.append("B22," + settingInfo.fatigueDrivingThresholdValue
						+ ",");
			}
			if (!Utils.isEmpty(settingInfo.accelerationAlarmSwitch)) {
				num = num + 1;
				value.append("B23," + settingInfo.accelerationAlarmSwitch + ",");
			}
			if (!Utils.isEmpty(settingInfo.accelerationAlarmThresholdValue)) {
				num = num + 1;
				value.append("B24,"
						+ settingInfo.accelerationAlarmThresholdValue + ",");
			}
			if (!Utils.isEmpty(settingInfo.slowdownAlarmSwitch)) {
				num = num + 1;
				value.append("B25," + settingInfo.slowdownAlarmSwitch + ",");
			}
			if (!Utils.isEmpty(settingInfo.slowdownAlarmThresholdValue)) {
				num = num + 1;
				value.append("B26," + settingInfo.slowdownAlarmThresholdValue
						+ ",");
			}
		}

		crcData = Constants.INSTRUCTION_HEAD + terminalSN + ","
				+ Constants.INSTRUCTION_SI + "," + num + "," + value
				+ Constants.INSTRUCTION_STAR;

		// String crc = Integer.toHexString(OBD.CRC(crcData)).toUpperCase();
		String crc = Utils.toHexString(OBD.CRC(crcData));

		String msg = crcData + crc + Constants.INSTRUCTION_FOOD;

		return msg;
	}

	public static String hardwareTestPackage(String testValue) {
		String crcData = "";
		crcData = "@@IDD,TEST," + testValue + ",*";
		String crc = Utils.toHexString(OBD.CRC(crcData));

		String msg = crcData + crc + Constants.INSTRUCTION_FOOD;

		return msg;
	}

	public static String replyIDRPackage() {
		String crcData = "";
		crcData = "@@IDD,IDR,0,*";
		String crc = Utils.toHexString(OBD.CRC(crcData));

		String msg = crcData + crc + Constants.INSTRUCTION_FOOD;
		// LogUtil.e("发送数据:" + crcData);
		// LogUtil.e("校验码:" + crc);

		return msg;
	}
}
