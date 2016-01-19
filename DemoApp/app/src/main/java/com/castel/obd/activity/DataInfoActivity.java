package com.castel.obd.activity;

import java.util.List;

import com.castel.obd.R;
import com.castel.obd.bluetooth.BluetoothManage;
import com.castel.obd.info.AlarmInfo;
import com.castel.obd.info.DataPackageInfo;
import com.castel.obd.info.FualtInfo;
import com.castel.obd.info.GPSDataInfo;
import com.castel.obd.info.PIDInfo;
import com.castel.obd.parse.AlarmParse;
import com.castel.obd.parse.FaultParse;
import com.castel.obd.parse.PIDParse;
import com.castel.obd.util.Utils;

import android.app.Activity;
import android.os.Bundle;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.RadioGroup.OnCheckedChangeListener;

public class DataInfoActivity extends Activity {
	private TextView tvResult;
	private RadioGroup group;
	private List<DataPackageInfo> dataPackages;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_data_info);

		init();

		showResult(0);
	}

	private void init() {
		tvResult = (TextView) findViewById(R.id.tv_result);
		group = (RadioGroup) findViewById(R.id.radioGroup);
		group.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(RadioGroup arg0, int arg1) {
				int radioButtonId = arg0.getCheckedRadioButtonId();
				if (radioButtonId == R.id.trip_0) {
					showResult(0);
				} else if (radioButtonId == R.id.trip_1) {
					showResult(1);
				} else if (radioButtonId == R.id.trip_2) {
					showResult(2);
				} else if (radioButtonId == R.id.trip_3) {
					showResult(3);
				} else if (radioButtonId == R.id.trip_4) {
					showResult(4);
				} else if (radioButtonId == R.id.trip_5) {
					showResult(5);
				} else if (radioButtonId == R.id.trip_6) {
					showResult(6);
				} else if (radioButtonId == R.id.trip_7) {
					showResult(7);
				} else if (radioButtonId == R.id.trip_8) {
					showResult(8);
				} else if (radioButtonId == R.id.trip_9) {
					showResult(9);
				}
			}
		});
	}

	private void showResult(int type) {
		dataPackages = BluetoothManage.getInstance(DataInfoActivity.this).dataPackages;
		if (null != dataPackages && dataPackages.size() > 0) {
			for (int i = dataPackages.size() - 1; i > 0; i--) {
				if (4 == dataPackages.get(i).result
						&& null != dataPackages.get(i).tripFlag
						&& type == Integer
								.parseInt(dataPackages.get(i).tripFlag)) {
					tvResult.setText(getData(dataPackages.get(i)));
					return;
				} else {
					tvResult.setText("no data");
				}
			}
		}
	}

	private String getData(DataPackageInfo dataPackage) {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(getString(R.string.data_travel) + "\n");
		stringBuffer.append(getString(R.string.trip_flag) + ": "
				+ dataPackage.tripFlag + "\n");
		stringBuffer.append(getString(R.string.trip_id) + ": "
				+ dataPackage.tripId + "\n");
		stringBuffer.append(getString(R.string.data_umber) + ": "
				+ dataPackage.dataNumber + "\n");
		stringBuffer.append(getString(R.string.protocol_type) + ": "
				+ dataPackage.protocolType + "\n");
		stringBuffer.append(getString(R.string.trip_mileage) + ": "
				+ dataPackage.tripMileage + getString(R.string.pid_unit_metre)
				+ "\n");
		stringBuffer.append(getString(R.string.trip_fuel) + ": "
				+ dataPackage.tripfuel + getString(R.string.pid_unit_litre)
				+ "\n");
		stringBuffer.append(getString(R.string.car_state) + ": "
				+ dataPackage.vState + "\n");

		stringBuffer.append(getString(R.string.gps_lists) + "\n");
		if (null != dataPackage.gpsData) {
			List<GPSDataInfo> gpsDataValues = dataPackage.gpsData;
			for (int i = 0; i < gpsDataValues.size(); i++) {
				GPSDataInfo gpsDataValue = gpsDataValues.get(i);
				stringBuffer.append(getString(R.string.gps_time)
						+ ": "
						+ Utils.getTime((long) Integer
								.parseInt(gpsDataValue.dateTime) * 1000) + " "
						+ "\n" + getString(R.string.gps_lat) + ": "
						+ gpsDataValue.gpsLat + "\n"
						+ getString(R.string.gps_long) + ": "
						+ gpsDataValue.gpsLong + "\n"
						+ getString(R.string.speed) + ": "
						+ gpsDataValue.speed + "\n"
						+ getString(R.string.direction) + ": "
						+ gpsDataValue.direction + "\n"
						+ getString(R.string.lat_flag) + ": "
						+ gpsDataValue.latFlag + "\n"
						+ getString(R.string.long_flag) + ": "
						+ gpsDataValue.longFlag + "\n"
						+ getString(R.string.location_flag) + ": "
						+ gpsDataValue.locationFlag + "\n"
						+ getString(R.string.starts) + ": "
						+ gpsDataValue.starts + "\n");
			}
		}

		stringBuffer.append(getString(R.string.obd_data_list) + "\n");
		if (null != dataPackage.obdData) {
			getPIDData(stringBuffer, dataPackage.obdData);
		}

		stringBuffer.append(getString(R.string.freeze_data) + "\n");
		if (null != dataPackage.freezeData) {
			getPIDData(stringBuffer, dataPackage.freezeData);
		}

		stringBuffer.append(getString(R.string.surport_pid) + ": "
				+ dataPackage.surportPid + "\n");

		stringBuffer.append(getString(R.string.dtc_list) + ": "
				+ dataPackage.dtcData + "\n");
		if (!Utils.isEmpty(dataPackage.dtcData)) {
			List<FualtInfo> fualtValues = FaultParse.parse(this,
					dataPackage.dtcData.split(","));
			for (int i = 0; i < fualtValues.size(); i++) {
				FualtInfo fualtValue = fualtValues.get(i);
				stringBuffer.append(fualtValue.code + " " + fualtValue.meaning
						+ "\n");
			}
		}

		stringBuffer.append(getString(R.string.dtc_data) + "\n");
		if (null != dataPackage.alarmData
				&& !Utils.isEmpty(dataPackage.alarmData.alarmType)) {
			AlarmInfo alarmDataValue = AlarmParse.parse(this,
					dataPackage.alarmData);
			stringBuffer.append(getString(R.string.alarm_type) + ": "
					+ alarmDataValue.alarmType + " " + alarmDataValue.meaning
					+ "\n");
			stringBuffer.append(getString(R.string.curent_value) + ": "
					+ alarmDataValue.curentValue + " " + alarmDataValue.unit
					+ "\n");
			stringBuffer.append(getString(R.string.threshold) + ": "
					+ alarmDataValue.threshold + " " + alarmDataValue.unit
					+ "\n");
		}

		return stringBuffer.toString();
	}

	private StringBuffer getPIDData(StringBuffer stringBuffer,
			List<PIDInfo> lists) {
		for (int i = 0; i < lists.size(); i++) {
			PIDInfo pidValue = PIDParse.parse(DataInfoActivity.this,
					lists.get(i));

			if (null == pidValue.meaning || null == pidValue.unit) {
				stringBuffer.append(getString(R.string.type) + ": 0x"
						+ pidValue.pidType + "\n");
				stringBuffer.append(getString(R.string.value) + ": "
						+ pidValue.value + "\n");
			} else {
				stringBuffer.append(getString(R.string.type) + ": 0x"
						+ pidValue.pidType + "(" + pidValue.meaning + ")\n");
				stringBuffer.append(getString(R.string.value) + ": "
						+ pidValue.value + "(" + pidValue.intValues[0]
						+ pidValue.unit + ")\n");
			}
		}
		return stringBuffer;
	}

}
