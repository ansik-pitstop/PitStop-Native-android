package com.castel.obd.parse;

import com.pitstop.R;
import com.castel.obd.info.PIDInfo;
import com.castel.obd.util.LogUtil;

import android.content.Context;

public class PIDParse {

	public static PIDInfo parse(Context context, PIDInfo pidInfo) {
		LogUtil.i("case 0x" + pidInfo.pidType);

		PIDInfo mPidValue = new PIDInfo();
		mPidValue.pidType = pidInfo.pidType;
		mPidValue.value = pidInfo.value;

		String[] values = pidInfo.value.split(",");
		mPidValue.intValues = new int[values.length];
		for (int i = 0; i < values.length; i++) {
			mPidValue.intValues[i] = Integer.parseInt(values[i], 16);
		}

		switch (Integer.parseInt(pidInfo.pidType, 16)) {
		case 0x2105:
			mPidValue.meaning = context.getString(R.string.pid_2105);
			mPidValue.unit = context.getString(R.string.pid_unit_temperature);
			for (int i = 0; i < mPidValue.intValues.length; i++) {
				mPidValue.intValues[i] = mPidValue.intValues[i] - 40;
			}
			break;
		case 0x210B:
			mPidValue.meaning = context.getString(R.string.pid_210B);
			mPidValue.unit = context.getString(R.string.pid_unit_pressure);
			break;
		case 0x210C:
			mPidValue.meaning = context.getString(R.string.pid_210C);
			mPidValue.unit = context.getString(R.string.pid_unit_speed);
			break;
		case 0x210D:
			mPidValue.meaning = context.getString(R.string.pid_210D);
			mPidValue.unit = context.getString(R.string.pid_unit_rpm);
			break;
		case 0x210F:
			mPidValue.meaning = context.getString(R.string.pid_210F);
			mPidValue.unit = context.getString(R.string.pid_unit_temperature);
			for (int i = 0; i < mPidValue.intValues.length; i++) {
				mPidValue.intValues[i] = mPidValue.intValues[i] - 40;
			}
			break;
		case 0x2110:
			mPidValue.meaning = context.getString(R.string.pid_2110);
			mPidValue.unit = context.getString(R.string.pid_unit_gallons);
			break;
		}
		return mPidValue;
	}
}
