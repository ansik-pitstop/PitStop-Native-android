package com.castel.obd.activity;

import java.util.List;

import com.castel.obd.R;
import com.castel.obd.bluetooth.BluetoothManage;
import com.castel.obd.bluetooth.BluetoothManage.BluetoothDataListener;
import com.castel.obd.info.AlarmInfo;
import com.castel.obd.info.DataPackageInfo;
import com.castel.obd.info.FualtInfo;
import com.castel.obd.info.GPSDataInfo;
import com.castel.obd.info.PIDInfo;
import com.castel.obd.info.ParameterPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;
import com.castel.obd.parse.AlarmParse;
import com.castel.obd.parse.FaultParse;
import com.castel.obd.parse.PIDParse;
import com.castel.obd.util.LogUtil;
import com.castel.obd.util.ProgressDialogUtil;
import com.castel.obd.util.Utils;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.RadioGroup.OnCheckedChangeListener;

public class DataActivity extends Activity implements BluetoothDataListener {
	private TextView tvResult;
	private Button btnDetails;
	private RadioGroup group;
	private boolean isTravel = true;

	private List<DataPackageInfo> dataPackages;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_data);

		BluetoothManage.getInstance(this).setBluetoothDataListener(this);

		init();

		dataPackages = BluetoothManage.getInstance(this).dataPackages;
		if (null != dataPackages && dataPackages.size() > 0) {
			for (int i = dataPackages.size() - 1; i > 0; i--) {
				if (4 == dataPackages.get(i).result) {
					showResult(dataPackages.get(i));
					return;
				}
			}
		}
	}

	private void init() {
		tvResult = (TextView) findViewById(R.id.tv_result);
		btnDetails = (Button) findViewById(R.id.btn_details);
		group = (RadioGroup) findViewById(R.id.radioGroup);
		btnDetails.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				startActivity(new Intent(DataActivity.this,
						DataInfoActivity.class));
			}
		});
		group.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(RadioGroup arg0, int arg1) {
				int radioButtonId = arg0.getCheckedRadioButtonId();
				if (radioButtonId == R.id.radio_travel) {
					isTravel = true;
					btnDetails.setVisibility(View.VISIBLE);
					for (int i = dataPackages.size() - 1; i > 0; i--) {
						if (4 == dataPackages.get(i).result) {
							showResult(dataPackages.get(i));
							return;
						}
					}
				} else {
					isTravel = false;
					btnDetails.setVisibility(View.GONE);
					for (int i = dataPackages.size() - 1; i > 0; i--) {
						if (5 == dataPackages.get(i).result) {
							LogUtil.i("i:" + i);
							showResult(dataPackages.get(i));
							return;
						}
					}
				}
			}
		});
	}

	@Override
	public void getBluetoothState(int state) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setCtrlResponse(ResponsePackageInfo responsePackageInfo) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setParamaterResponse(ResponsePackageInfo responsePackageInfo) {
		// TODO Auto-generated method stub

	}

	@Override
	public void getParamaterData(ParameterPackageInfo parameterPackageInfo) {
		// TODO Auto-generated method stub

	}

	@Override
	public void getIOData(DataPackageInfo dataPackage) {
		ProgressDialogUtil.dismiss();

		if (!(null != dataPackage && null != dataPackage.obdData)) {
			tvResult.setText(R.string.fail);
			return;
		}

		dataPackages.add(dataPackage);

		showResult(dataPackage);
	}

	private void showResult(DataPackageInfo dataPackage) {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer
				.append(getString(R.string.time)
						+ ": "
						+ Utils.getTime((long) Integer
								.parseInt(dataPackage.rtcTime) * 1000) + "\n");
		stringBuffer.append(getString(R.string.device_id) + ": "
				+ dataPackage.deviceId + "\n");

		if (4 == dataPackage.result) {
			if (!isTravel) {
				return;
			}
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
					+ dataPackage.tripMileage
					+ getString(R.string.pid_unit_metre) + "\n");
			stringBuffer.append(getString(R.string.trip_fuel) + ": "
					+ dataPackage.tripfuel + getString(R.string.pid_unit_litre)
					+ "\n");
			stringBuffer.append(getString(R.string.car_state) + ": "
					+ dataPackage.vState + "\n");

			getTripData(stringBuffer, dataPackage);
		} else if (5 == dataPackage.result) {
			if (isTravel) {
				return;
			}
			stringBuffer.append(getString(R.string.data_operating_condition)
					+ "\n");
			stringBuffer.append(getString(R.string.trip_mileage) + ": "
					+ dataPackage.tripMileage
					+ getString(R.string.pid_unit_metre) + "\n");
			getPIDData(stringBuffer, dataPackage.obdData);
		}

		tvResult.setText(stringBuffer.toString());
	}

	private StringBuffer getTripData(StringBuffer stringBuffer,
			DataPackageInfo dataPackage) {

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

		return stringBuffer;
	}

	private StringBuffer getPIDData(StringBuffer stringBuffer,
			List<PIDInfo> lists) {
		for (int i = 0; i < lists.size(); i++) {
			PIDInfo pidValue = PIDParse.parse(DataActivity.this, lists.get(i));

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
