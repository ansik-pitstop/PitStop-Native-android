package com.castel.obd.activity;

import java.util.List;

import com.castel.obd.R;
import com.castel.obd.bluetooth.BluetoothManage;
import com.castel.obd.bluetooth.BluetoothManage.BluetoothDataListener;
import com.castel.obd.info.DataPackageInfo;
import com.castel.obd.info.ParameterInfo;
import com.castel.obd.info.ParameterPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;
import com.castel.obd.util.LogUtil;
import com.castel.obd.util.ProgressDialogUtil;
import com.castel.obd.util.Utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RadioGroup.OnCheckedChangeListener;

public class RegularParameterInfoActivity extends Activity implements
		OnClickListener, BluetoothDataListener, OnCheckedChangeListener {
	private final String REGULAR_TYPE = "1201,1202,1203,1204,1205,1206";//�̶��ϴ���������
	private final String REGULAR_TYPE_QUERY = "12";//��ѯ�̶��ϴ���������
	private TextView tvResult;
	private Button btnSet;
	private Button btnQuery;
	private RadioGroup groupGPS;
	private RadioGroup groupOBDData;
	private RadioGroup groupGSensor;
	private EditText etTime;
	private EditText etDataTime;
	private EditText etDataType;
	private Button btnChoose;

	private String onOffGPS = "00";
	private String onOffOBDData = "00";
	private String onOffGSensor = "00";

	String time = "";
	String dataTime = "";
	String dataType = "2105,210B,210C,210D,210F,2110";//Ĭ�ϵ�OBD������ݿ������ͣ����Ϳ��ټ�

	private String[] pids;
	private String[] pidTypes;

	private int[] regularType = { R.string.regular_gps,
			R.string.regular_obd_data, R.string.regular_gsensor,
			R.string.regular_time, R.string.regular_obd_data_time,
			R.string.regular_obd_data_type };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_regular_parameter_info);

		BluetoothManage.getInstance(this).setBluetoothDataListener(this);

		init();
	}

	private void init() {
		tvResult = (TextView) findViewById(R.id.tv_result);
		btnSet = (Button) findViewById(R.id.btn_set);
		btnQuery = (Button) findViewById(R.id.btn_query);
		btnChoose = (Button) findViewById(R.id.btn_choose);
		btnSet.setOnClickListener(this);
		btnQuery.setOnClickListener(this);
		btnChoose.setOnClickListener(this);
		groupGPS = (RadioGroup) findViewById(R.id.rg_gps);
		groupOBDData = (RadioGroup) findViewById(R.id.rg_obd);
		groupGSensor = (RadioGroup) findViewById(R.id.rg_gsensor);
		groupGPS.setOnCheckedChangeListener(this);
		groupOBDData.setOnCheckedChangeListener(this);
		groupGSensor.setOnCheckedChangeListener(this);
		etTime = (EditText) findViewById(R.id.et_time);
		etDataTime = (EditText) findViewById(R.id.et_data_time);
		etDataType = (EditText) findViewById(R.id.et_data_type);
		etDataType.setText(dataType);

		pids = getResources().getStringArray(R.array.pid);
		pidTypes = getResources().getStringArray(R.array.pid_type);
	}

	@Override
	public void onCheckedChanged(RadioGroup arg0, int arg1) {
		switch (arg0.getCheckedRadioButtonId()) {
		case R.id.rb_gps_on:
			onOffGPS = "01";
			break;
		case R.id.rb_gps_off:
			onOffGPS = "00";
			break;
		case R.id.rb_obd_on:
			onOffOBDData = "01";
			break;
		case R.id.rb_obd_off:
			onOffOBDData = "00";
			break;
		case R.id.rb_gsensor_on:
			onOffGSensor = "01";
			break;
		case R.id.rb_gsensor_off:
			onOffGSensor = "00";
			break;
		}
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.btn_set:
			String time = etTime.getText().toString().trim();
			String dataTime = etDataTime.getText().toString().trim();
			String dataType = etDataType.getText().toString().trim();
			if (Utils.isEmpty(time) || Utils.isEmpty(dataTime)
					|| Utils.isEmpty(dataType)) {
				Toast.makeText(RegularParameterInfoActivity.this,
						R.string.input_value, Toast.LENGTH_LONG).show();
				return;
			}

			StringBuffer bufferValue = new StringBuffer();
			bufferValue.append(onOffGPS);
			bufferValue.append(";" + onOffOBDData);
			bufferValue.append(";" + onOffGSensor);
			bufferValue.append(";" + time);
			bufferValue.append(";" + dataTime);
			bufferValue.append(";" + dataType);

			BluetoothManage.getInstance(this).obdSetParameter(REGULAR_TYPE,
					bufferValue.toString());
			break;
		case R.id.btn_query:
			BluetoothManage.getInstance(this).obdGetParameter(
					REGULAR_TYPE_QUERY);
			break;
		case R.id.btn_choose:
			showMultiChoiceDialog();
			break;
		}
	}

	private void showMultiChoiceDialog() {
		final StringBuffer stringBuffer = new StringBuffer();

		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder.setIcon(R.drawable.ic_launcher);
		builder.setTitle(R.string.regular_obd_data_type);
		builder.setMultiChoiceItems(pids, new boolean[] { false, false, false,
				false, false, false }, new OnMultiChoiceClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1, boolean arg2) {
				stringBuffer.append("," + pidTypes[arg1]);
			}
		});
		builder.setPositiveButton(R.string.submit,
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						if (!Utils.isEmpty(stringBuffer.toString())) {
							dataType = stringBuffer.toString().substring(1,
									stringBuffer.toString().length());
							etDataType.setText(dataType);
						}
					}
				});
		builder.setNegativeButton(R.string.cancel,
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface arg0, int arg1) {

					}
				});
		builder.create().show();
	}

	@Override
	public void setCtrlResponse(ResponsePackageInfo responsePackageInfo) {

	}

	@Override
	public void setParamaterResponse(ResponsePackageInfo responsePackageInfo) {
		ProgressDialogUtil.dismiss();

		if (null == responsePackageInfo) {
			tvResult.setText(R.string.setup_fail);
			return;
		}

		if ("12".equals(responsePackageInfo.type)) {
			tvResult.setText(R.string.setup_success);
		}
	}

	@Override
	public void getParamaterData(ParameterPackageInfo parameterPackageInfo) {
		ProgressDialogUtil.dismiss();

		getResultShow(parameterPackageInfo);
	}

	@Override
	public void getIOData(DataPackageInfo dataPackage) {

	}

	@Override
	public void getBluetoothState(int state) {

	}

	/**
	 * �̶��ϴ�������ʾ
	 * @param parameterPackage
	 */
	private void getResultShow(ParameterPackageInfo parameterPackageInfo) {
		if (null == parameterPackageInfo) {
			tvResult.setText(R.string.query_fail);
			return;
		}

		LogUtil.i("parameterPackage.size():" + parameterPackageInfo.value.size());

		StringBuffer buffer = new StringBuffer();

		List<ParameterInfo> parameterValues = parameterPackageInfo.value;
		for (int i = 0; i < parameterValues.size(); i++) {
			ParameterInfo value = parameterValues.get(i);
			buffer.append(getTypeString(value.tlvTag, value.value,
					regularType[i]));
		}

		tvResult.setText(buffer.toString());
	}

	private String getTypeString(String type, String value, int titleId) {
		LogUtil.i(getString(titleId));
		String result = getString(R.string.type) + ": 0x" + type + " "
				+ getString(titleId) + "\n" + getString(R.string.value) + ": "
				+ value + "\n";

		return result;
	}

}
