package com.castel.obd.parse;

import com.pitstop.R;
import com.castel.obd.info.AlarmInfo;

import android.content.Context;

public class AlarmParse {

	public static AlarmInfo parse(Context context, AlarmInfo alarmInfo) {
		AlarmInfo mAlarmInfo = alarmInfo;
		int code = Integer.parseInt(alarmInfo.alarmType);
		int index = code - 1;

		String[] alarms = context.getResources().getStringArray(R.array.alarm);

		switch (code) {
		case 1:
			mAlarmInfo.meaning = alarms[index];
			mAlarmInfo.unit = context.getString(R.string.pid_unit_rpm);
			break;
		case 2:
			mAlarmInfo.meaning = alarms[index];
			mAlarmInfo.unit = context.getString(R.string.pid_unit_v);
			break;
		case 3:
			mAlarmInfo.meaning = alarms[index];
			mAlarmInfo.unit = context.getString(R.string.pid_unit_temperature);
			break;
		case 4:
			mAlarmInfo.meaning = alarms[index];
			mAlarmInfo.unit = context.getString(R.string.pid_unit_g);
			break;
		case 5:
			mAlarmInfo.meaning = alarms[index];
			mAlarmInfo.unit = context.getString(R.string.pid_unit_g);
			break;
		case 6:
			mAlarmInfo.meaning = alarms[index];
			mAlarmInfo.unit = context.getString(R.string.pid_unit_min);
			break;
		case 7:
			mAlarmInfo.meaning = alarms[index];
			mAlarmInfo.unit = "";
			break;
		case 8:
			mAlarmInfo.meaning = alarms[index];
			mAlarmInfo.unit = context.getString(R.string.pid_unit_g);
			break;
		case 9:
			mAlarmInfo.meaning = alarms[index];
			mAlarmInfo.unit = "";
			break;
		case 10:
			mAlarmInfo.meaning = alarms[index];
			mAlarmInfo.unit = "";
			break;
		case 11:
			mAlarmInfo.meaning = alarms[index];
			mAlarmInfo.unit = context.getString(R.string.pid_unit_g);
			break;
		case 12:
			mAlarmInfo.meaning = alarms[index];
			mAlarmInfo.unit = context.getString(R.string.pid_unit_g);
			break;
		case 13:
			mAlarmInfo.meaning = alarms[index];
			mAlarmInfo.unit = context.getString(R.string.pid_unit_h);
			break;
		case 14:
			mAlarmInfo.meaning = alarms[index];
			mAlarmInfo.unit = "";
			break;
		}
		return mAlarmInfo;
	}
}
