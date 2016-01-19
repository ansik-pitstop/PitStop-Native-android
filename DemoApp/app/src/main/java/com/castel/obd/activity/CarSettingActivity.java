package com.castel.obd.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.castel.obd.R;
import com.castel.obd.bluetooth.BluetoothManage;
import com.castel.obd.bluetooth.BluetoothManage.BluetoothDataListener;
import com.castel.obd.data.OBDInfoSP;
import com.castel.obd.info.DataPackageInfo;
import com.castel.obd.info.ParameterInfo;
import com.castel.obd.info.ParameterPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;
import com.castel.obd.util.LogUtil;
import com.castel.obd.util.ProgressDialogUtil;

public class CarSettingActivity extends Activity implements
		OnItemSelectedListener, OnClickListener, BluetoothDataListener {
	private TextView tvResult;
	private Button btnSet;
	private Button btnQuery;
	private Spinner spinnerCarType;
	private Spinner spinnerDisplacement;
	private Spinner spinnerFuelType;

	private final String TYPE = "1401";

	private int[] carTypes;
	private String[] cars;
	private String[] displacements = { "1.5", "1.6", "1.7", "1.8", "1.9",
			"2.0", "2.1", "2.2", "2.3", "2.4", "2.5", "2.6", "2.7", "2.8",
			"2.9", "3.0" };
	private String[] fuelTypes = { "10", "20" };
	private String[] fuels;

	private int carType = 0x00;
	private String displacement = "1.5";
	private String fuelType = "10";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_car_setting);

		carTypes = getResources().getIntArray(R.array.car_type);
		cars = getResources().getStringArray(R.array.car);

		fuels = new String[] { getString(R.string.gasoline),
				getString(R.string.diesel_oil) };

		BluetoothManage.getInstance(this).setBluetoothDataListener(this);

		init();
	}

	private void init() {
		tvResult = (TextView) findViewById(R.id.tv_result);
		btnSet = (Button) findViewById(R.id.btn_set);
		btnQuery = (Button) findViewById(R.id.btn_query);
		btnSet.setOnClickListener(this);
		btnQuery.setOnClickListener(this);

		spinnerCarType = (Spinner) findViewById(R.id.spinner_car_type);
		spinnerDisplacement = (Spinner) findViewById(R.id.spinner_displacement);
		spinnerFuelType = (Spinner) findViewById(R.id.spinner_fuel_type);
		spinnerCarType.setOnItemSelectedListener(this);
		spinnerDisplacement.setOnItemSelectedListener(this);
		spinnerFuelType.setOnItemSelectedListener(this);

		ArrayAdapter<String> adapterCar = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, cars);
		adapterCar
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerCarType.setAdapter(adapterCar);
		spinnerCarType.setVisibility(View.VISIBLE);

		ArrayAdapter<String> adapterDisplacement = new ArrayAdapter<String>(
				this, android.R.layout.simple_spinner_item, displacements);
		adapterCar
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerDisplacement.setAdapter(adapterDisplacement);
		spinnerDisplacement.setVisibility(View.VISIBLE);

		ArrayAdapter<String> adapterFuel = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, fuels);
		adapterCar
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerFuelType.setAdapter(adapterFuel);
		spinnerFuelType.setVisibility(View.VISIBLE);
	}

	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
			long arg3) {
		switch (arg0.getId()) {
		case R.id.spinner_car_type:
			carType = carTypes[arg2];
			break;

		case R.id.spinner_displacement:
			displacement = displacements[arg2];
			break;
		case R.id.spinner_fuel_type:
			fuelType = fuelTypes[arg2];
			break;
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {

	}

	@Override
	public void onClick(View arg0) {
		switch (arg0.getId()) {
		case R.id.btn_set:
			String value = displacement + "," + fuelType;
			LogUtil.i(value);
			BluetoothManage.getInstance(this).obdSetParameter(TYPE, value);
			break;
		case R.id.btn_query:
			BluetoothManage.getInstance(this).obdGetParameter(TYPE);
			break;
		}
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
		ProgressDialogUtil.dismiss();

		if (null == responsePackageInfo) {
			tvResult.setText(R.string.fail);
			return;
		}

		if ("14".equals(responsePackageInfo.type)) {
			tvResult.setText(R.string.success);
			OBDInfoSP.saveCarType(CarSettingActivity.this, carType);
		}
	}

	@Override
	public void getParamaterData(ParameterPackageInfo parameterPackageInfo) {
		ProgressDialogUtil.dismiss();

		if (null == parameterPackageInfo) {
			tvResult.setText(R.string.query_fail);
			return;
		}

		if (null != parameterPackageInfo.value
				&& TYPE.equals(parameterPackageInfo.value.get(0).tlvTag)) {
			ParameterInfo parameterValue = parameterPackageInfo.value.get(0);
			String[] values = parameterValue.value.split(",");
			String fuelType = getString(R.string.gasoline);
			if (fuelTypes[0].equals(values[1])) {
				fuelType = getString(R.string.gasoline);
			} else if (fuelTypes[1].equals(values[1])) {
				fuelType = getString(R.string.diesel_oil);
			}

			String msg = getString(R.string.type) + ": 0x"
					+ parameterValue.tlvTag + " "
					+ getString(R.string.setting_oil_wear) + "\n"
					+ getString(R.string.value) + ": " + parameterValue.value
					+ " " + getString(R.string.displecement) + values[0] + " "
					+ fuelType + "\n";

			tvResult.setText(msg);
		}

	}

	@Override
	public void getIOData(DataPackageInfo dataPackage) {
		// TODO Auto-generated method stub

	}

}
