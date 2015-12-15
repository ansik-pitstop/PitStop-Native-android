package com.castel.obd.activity;

import java.io.UnsupportedEncodingException;
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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class ParameterInfoActivity extends Activity implements OnClickListener,
		BluetoothDataListener {
	private TextView tvTitle;
	private TextView tvResult;
	private LinearLayout llSwitch;
	private Spinner spinner;
	private RadioGroup groupAlarm;
	private RadioGroup groupSound;
	private EditText etValue;
	private Button btnSet;
	private Button btnQuery;

	private String title = "";
	private String type = "";
	private int onOff;
	private String onOffAlarm = "00";
	private String onOffSound = "00";

	private String[] alarms;
	private String[] alarmTypes;

	private long currentTime;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_parameter_info);

		// title = getIntent().getExtras().getString("TYPE_TITLE", "");
		// type = getIntent().getExtras().getString("TYPE", "");
		// onOff = getIntent().getExtras().getInt("TYPE_ON_OFF");

		title = getIntent().getExtras().getString("TYPE_TITLE");
		type = getIntent().getExtras().getString("TYPE");
		onOff = getIntent().getExtras().getInt("TYPE_ON_OFF");

		BluetoothManage.getInstance(this).setBluetoothDataListener(this);

		init();
	}

	private void init() {
		tvResult = (TextView) findViewById(R.id.tv_result);
		tvTitle = (TextView) findViewById(R.id.tv_title);
		tvTitle.setText(title);
		llSwitch = (LinearLayout) findViewById(R.id.ll_switch);
		spinner = (Spinner) findViewById(R.id.spinner);
		groupAlarm = (RadioGroup) findViewById(R.id.rg_alarm);
		groupSound = (RadioGroup) findViewById(R.id.rg_sound);
		etValue = (EditText) findViewById(R.id.editText);
		btnSet = (Button) findViewById(R.id.btn_set);
		btnQuery = (Button) findViewById(R.id.btn_query);
		btnSet.setOnClickListener(this);
		btnQuery.setOnClickListener(this);

		if ("1A01".equals(type)) {
			currentTime = System.currentTimeMillis();
			etValue.setText(getString(R.string.current_time)
					+ Utils.getTime(currentTime));
			etValue.setFocusable(false);
		}

		if (1 == onOff) {
			llSwitch.setVisibility(View.GONE);
		} else if (2 == onOff) {
			llSwitch.setVisibility(View.GONE);
			etValue.setVisibility(View.GONE);
			btnSet.setVisibility(View.GONE);
		} else if (3 == onOff) {
			spinner.setVisibility(View.VISIBLE);
			alarms = getResources().getStringArray(R.array.alarm);
			alarmTypes = getResources().getStringArray(R.array.alarm_type);
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
					android.R.layout.simple_spinner_item, alarms);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			spinner.setAdapter(adapter);
			spinner.setVisibility(View.VISIBLE);
			spinner.setOnItemSelectedListener(new OnItemSelectedListener() {

				@Override
				public void onItemSelected(AdapterView<?> arg0, View arg1,
						int arg2, long arg3) {
					type = alarmTypes[arg2];
					LogUtil.i("type:" + type);
					if ("1007".equals(type)) {
						etValue.setVisibility(View.GONE);
						etValue.setText("15");
					} else if ("1009".equals(type)) {
						etValue.setVisibility(View.GONE);
						etValue.setText("0");
					} else if ("100A".equals(type)) {
						etValue.setVisibility(View.GONE);
						etValue.setText("0");
					} else {
						etValue.setVisibility(View.VISIBLE);
						etValue.setText("");
					}
				}

				@Override
				public void onNothingSelected(AdapterView<?> arg0) {
				}
			});

		}

		groupAlarm.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(RadioGroup arg0, int arg1) {
				int radioButtonId = arg0.getCheckedRadioButtonId();
				if (radioButtonId == R.id.radio_on) {
					onOffAlarm = "01";
				} else {
					onOffAlarm = "00";
				}
			}
		});
		groupSound.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(RadioGroup arg0, int arg1) {
				int radioButtonId = arg0.getCheckedRadioButtonId();
				if (radioButtonId == R.id.radio_on_sound) {
					onOffSound = "01";
				} else {
					onOffSound = "00";
				}
			}
		});

	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.btn_set:
			LogUtil.i("type:" + type);
			StringBuffer buffer = new StringBuffer();
			if (0 == onOff || 3 == onOff) {
				buffer.append(onOffAlarm);
				buffer.append("," + onOffSound + ",");
			}

			if (Utils.isEmpty(etValue.getText().toString().trim())) {
				Toast.makeText(ParameterInfoActivity.this,
						R.string.input_value, Toast.LENGTH_LONG).show();
				return;
			}

			String value = etValue.getText().toString().trim();
			// ���ƺ���ת���ַ�����
			if ("1501".equals(type)) {
				try {
					value = Utils.bytesToHexString(value.getBytes("UTF-16LE"));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			} else if ("1A01".equals(type)) {
				// Java��������8λ��C������4λ�����ʱ��תΪ��
				value = String.valueOf(currentTime / 1000);
			}

			buffer.append(value);
			LogUtil.i("value:" + buffer.toString());
			BluetoothManage.getInstance(this).obdSetParameter(type,
					buffer.toString());
			break;
		case R.id.btn_query:
			if (3 == onOff) {
				BluetoothManage.getInstance(this).obdGetParameter("10");
			}
			BluetoothManage.getInstance(this).obdGetParameter(type);
			break;
		}
	}

	@Override
	public void setCtrlResponse(ResponsePackageInfo responsePackageInfo) {

	}

	@Override
	public void setParamaterResponse(ResponsePackageInfo responsePackageInfo) {
		ProgressDialogUtil.dismiss();

		setResultShow(responsePackageInfo);

	}

	@Override
	public void getParamaterData(ParameterPackageInfo parameterPackageInfo) {
		ProgressDialogUtil.dismiss();
		queryResultShow(parameterPackageInfo);
	}

	@Override
	public void getIOData(DataPackageInfo dataPackage) {

	}

	@Override
	public void getBluetoothState(int state) {

	}

	private void queryResultShow(ParameterPackageInfo parameterPackageInfo) {
		if (null == parameterPackageInfo) {
			tvResult.setText(R.string.query_fail);
			return;
		}

		LogUtil.i("parameterPackage.size():"
				+ parameterPackageInfo.value.size());

		String result = "";

		List<ParameterInfo> parameterValues = parameterPackageInfo.value;
		if (1 == parameterValues.size()) {
			// �������
			result = getOtherParameter(parameterValues.get(0));
		} else if (1 < parameterValues.size()) {
			if ("10".equals(parameterValues.get(0).tlvTag.substring(0, 2))) {
				// �澯����
				result = getAlarmParameter(parameterValues);
			}
		}

		tvResult.setText(result);
	}

	private String getAlarmParameter(List<ParameterInfo> parameterValues) {
		StringBuffer buffer = new StringBuffer();

		for (int i = 0; i < parameterValues.size(); i++) {
			ParameterInfo value = parameterValues.get(i);
			for (int j = 0; j < alarmTypes.length; j++) {
				if (alarmTypes[j].equals(value.tlvTag)) {
					LogUtil.i(alarms[j]);
					buffer.append(getString(R.string.type) + ": 0x"
							+ value.tlvTag + " " + alarms[i] + "\n"
							+ getString(R.string.value) + ": " + value.value
							+ "\n");
				}
			}
		}

		return buffer.toString();
	}

	/**
	 * ��ȡ������Ϣ��ʾ
	 * 
	 * @param parameterValue
	 * @return
	 */
	private String getOtherParameter(ParameterInfo parameterValue) {
		String msg = "";
		if ("1401".equals(parameterValue.tlvTag)) {
			msg = getTypeString(parameterValue.tlvTag, parameterValue.value,
					R.string.setting_oil_wear);
		} else if ("1501".equals(parameterValue.tlvTag)) {
			String plateNumber = parameterValue.value;
			try {
				plateNumber = new String(
						Utils.hexStringToBytes(parameterValue.value),
						"utf-16LE");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			msg = getTypeString(parameterValue.tlvTag, plateNumber,
					R.string.setting_plate_number);
		} else if ("1601".equals(parameterValue.tlvTag)) {
			msg = getTypeString(parameterValue.tlvTag, parameterValue.value,
					R.string.setting_sn);
		} else if ("1A01".equals(parameterValue.tlvTag)) {
			msg = getTypeString(
					parameterValue.tlvTag,
					Utils.getTime((long) Integer.parseInt(parameterValue.value) * 1000),
					R.string.setting_time);
		} else if ("1B01".equals(parameterValue.tlvTag)) {
			msg = getTypeString(parameterValue.tlvTag, parameterValue.value,
					R.string.setting_warning);
		} else if ("1D01".equals(parameterValue.tlvTag)) {
			msg = getTypeString(parameterValue.tlvTag, parameterValue.value,
					R.string.setting_delay);
		} else if ("2201".equals(parameterValue.tlvTag)) {
			msg = getTypeString(parameterValue.tlvTag, parameterValue.value,
					R.string.read_vin);
		} else if ("2301".equals(parameterValue.tlvTag)) {
			msg = getTypeString(parameterValue.tlvTag, parameterValue.value,
					R.string.read_version);
		} else if ("2401".equals(parameterValue.tlvTag)) {
			msg = getTypeString(parameterValue.tlvTag, parameterValue.value,
					R.string.read_data_type);
		}

		return msg;
	}

	private String getTypeString(String type, String value, int titleId) {
		LogUtil.i(getString(titleId));
		String result = getString(R.string.type) + ": 0x" + type + " "
				+ getString(titleId) + "\n" + getString(R.string.value) + ": "
				+ value + "\n";

		return result;
	}

	/**
	 * ���ò���ɹ���ʧ����ʾ
	 * 
	 * @param responsePackage
	 */
	private void setResultShow(ResponsePackageInfo responsePackageInfo) {
		if (null == responsePackageInfo) {
			tvResult.setText(R.string.fail);
			return;
		}

		String result = "";
		if ("10".equals(responsePackageInfo.type)) {
			LogUtil.i("���ø澯����");
			result = getTypeString(responsePackageInfo.value,
					R.string.setting_alarm);
		} else if ("12".equals(responsePackageInfo.type)) {
			LogUtil.i("���ù̶��ϴ�����");
			result = getTypeString(responsePackageInfo.value,
					R.string.setting_regular);
		} else if ("14".equals(responsePackageInfo.type)) {
			LogUtil.i("�����ͺĲ���");
			result = getTypeString(responsePackageInfo.value,
					R.string.setting_oil_wear);
		} else if ("15".equals(responsePackageInfo.type)) {
			LogUtil.i("���ó��ƺ�");
			result = getTypeString(responsePackageInfo.value,
					R.string.setting_plate_number);
		} else if ("16".equals(responsePackageInfo.type)) {
			LogUtil.i("�������к�");
			result = getTypeString(responsePackageInfo.value,
					R.string.setting_sn);
		} else if ("1A".equals(responsePackageInfo.type)) {
			LogUtil.i("����RTCʱ��");
			result = getTypeString(responsePackageInfo.value,
					R.string.setting_time);
		} else if ("1B".equals(responsePackageInfo.type)) {
			LogUtil.i("����ϵͳ��ʾ��״̬");
			result = getTypeString(responsePackageInfo.value,
					R.string.setting_warning);
		} else if ("1D".equals(responsePackageInfo.type)) {
			LogUtil.i("���÷�����Ϩ����ʱ�ж�");
			result = getTypeString(responsePackageInfo.value,
					R.string.setting_delay);
		}

		tvResult.setText(result);
		Toast.makeText(this, result, Toast.LENGTH_LONG).show();
	}

	private String getTypeString(String value, int titleId) {
		StringBuffer msgBuffer = new StringBuffer();
		msgBuffer.append(getString(titleId));
		if (0 == Integer.parseInt(value)) {
			LogUtil.i("ʧ��");
			msgBuffer.append(getString(R.string.fail));
		} else if (1 == Integer.parseInt(value)) {
			LogUtil.i("�ɹ�");
			msgBuffer.append(getString(R.string.success));
		}

		return msgBuffer.toString();
	}

}
