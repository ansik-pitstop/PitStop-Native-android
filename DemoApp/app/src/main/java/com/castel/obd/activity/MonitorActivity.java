package com.castel.obd.activity;

import java.util.List;

import com.castel.obd.R;
import com.castel.obd.bluetooth.BluetoothManage;
import com.castel.obd.bluetooth.BluetoothManage.BluetoothDataListener;
import com.castel.obd.info.DataPackageInfo;
import com.castel.obd.info.FualtInfo;
import com.castel.obd.info.PIDInfo;
import com.castel.obd.info.ParameterPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;
import com.castel.obd.parse.FaultParse;
import com.castel.obd.parse.PIDParse;
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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class MonitorActivity extends Activity implements BluetoothDataListener {
	private Spinner spinner;
	private Button btnReadPID;
	private Button btnStart;
	private Button btnSelectPid;
	private TextView tvResult;
	private TextView tvPID;
	private LinearLayout llMonitor;
	private ListView lv;

	private String[] types = { "0(��ع������)", "1(��ȡSTORE����������Ϣ����ʷ���ϣ�)",
			"2(��ȡPENDING����������Ϣ����ǰ���ϣ�)", "3(��ȡ����֡���)", "4(ָ����������ϴ�)" };
	private int type = 0;

	private String[] pids;
	private String pid = "";
	private String selectPid = "";
	private boolean[] selected;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_monitor);

		BluetoothManage.getInstance(this).setBluetoothDataListener(this);

		spinner = (Spinner) findViewById(R.id.spinner_type);
		btnStart = (Button) findViewById(R.id.btn);
		btnReadPID = (Button) findViewById(R.id.btn_pid);
		btnSelectPid = (Button) findViewById(R.id.btn_select_pid);
		tvResult = (TextView) findViewById(R.id.tv_result);
		tvPID = (TextView) findViewById(R.id.tv_pid);
		llMonitor = (LinearLayout) findViewById(R.id.ll_monitor);
		llMonitor.setVisibility(View.GONE);

		btnReadPID.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				BluetoothManage.getInstance(MonitorActivity.this)
						.obdGetParameter("2401");
			}
		});

		btnSelectPid.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				showMultiChoiceItems();
			}
		});

		btnStart.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// ���֮ǰ���Ȳ�ѯ����֧�ֵ����������
				tvResult.setText("");
				if (4 == type) {
					int num = 0;
					for (int i = 0; i < selected.length; i++) {
						if (selected[i] == true) {
							num = num + 1;
						}
					}
					if(0 < num && num <= 10){
						BluetoothManage.getInstance(MonitorActivity.this)
						.obdSetMonitor(type, selectPid);
					}else{
						Toast.makeText(MonitorActivity.this,
								R.string.select_pid, Toast.LENGTH_SHORT).show();
					}
				} else {
					BluetoothManage.getInstance(MonitorActivity.this)
							.obdSetMonitor(type, "");
				}
			}
		});

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, types);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);

		spinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				// TODO Auto-generated method stub
				type = arg2;
				LogUtil.i("type:" + type);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub

			}
		});
	}

	private void showMultiChoiceItems() {
		selected = new boolean[pids.length];
		for (int i = 0; i < pids.length; i++) {
			selected[i] = false;
		}

		AlertDialog builder = new AlertDialog.Builder(this)
				.setTitle(R.string.select_pid)
				.setMultiChoiceItems(pids, selected,
						new OnMultiChoiceClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which, boolean isChecked) {
								// TODO Auto-generated method stub
								selected[which] = isChecked;
							}
						})
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						selectPid = "";
						for (int i = 0; i < selected.length; i++) {
							if (selected[i] == true) {
								selectPid += pids[i] + ",";
							}
						}
						if (!"".equals(selectPid)) {
							selectPid = selectPid.substring(0,
									selectPid.length() - 1);
						}
						LogUtil.i(selectPid);
					}
				}).setNegativeButton("Cancel", null).create();
		builder.show();

	}

	@Override
	public void setCtrlResponse(ResponsePackageInfo responsePackageInfo) {

	}

	@Override
	public void setParamaterResponse(ResponsePackageInfo responsePackageInfo) {

	}

	@Override
	public void getParamaterData(ParameterPackageInfo parameterPackageInfo) {
		ProgressDialogUtil.dismiss();
		if (null == parameterPackageInfo) {
			tvResult.setText(R.string.fail);
			return;
		}
		// �ɹ���ѯ����֧�ֵ����������֮�����
		if (null != parameterPackageInfo.value
				&& null != parameterPackageInfo.value.get(0)
				&& "2401".equals(parameterPackageInfo.value.get(0).tlvTag)) {

			LogUtil.i(parameterPackageInfo.value.get(0).value);

			pid = parameterPackageInfo.value.get(0).value;
			tvPID.setText(pid);
			llMonitor.setVisibility(View.VISIBLE);

			pids = parameterPackageInfo.value.get(0).value.split(",");
		} else {
			ProgressDialogUtil.dismiss();
			tvResult.setText(R.string.fail);
		}
	}

	@Override
	public void getIOData(DataPackageInfo dataPackage) {
		if (null == dataPackage) {
			return;
		}

		if (6 != dataPackage.result) {
			return;
		}

		ProgressDialogUtil.dismiss();

		StringBuffer stringBuffer = new StringBuffer();
		if (null != dataPackage.obdData && dataPackage.obdData.size() > 0) {
			stringBuffer = getOdbData(stringBuffer, dataPackage.obdData);
		} else if (null != dataPackage.dtcData
				&& !dataPackage.dtcData.equals("")) {
			stringBuffer = getdtcData(stringBuffer, dataPackage.dtcData);
		} else if (null != dataPackage.freezeData
				&& dataPackage.freezeData.size() > 0) {
			stringBuffer = getPIDData(stringBuffer, dataPackage.freezeData);
		} else {
			stringBuffer.append(R.string.fail);

		}

		tvResult.setText(stringBuffer.toString());

	}

	@Override
	public void getBluetoothState(int state) {

	}

	private StringBuffer getOdbData(StringBuffer stringBuffer,
			List<PIDInfo> lists) {
		stringBuffer.append(getString(R.string.data_monitor) + "\n");

		for (int i = 0; i < lists.size(); i++) {
			PIDInfo pidValue = PIDParse.parse(MonitorActivity.this,
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

	private StringBuffer getPIDData(StringBuffer stringBuffer,
			List<PIDInfo> lists) {
		stringBuffer.append(getString(R.string.freeze_data) + "\n");
		for (int i = 0; i < lists.size(); i++) {
			PIDInfo pidValue = PIDParse.parse(this, lists.get(i));

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

	private StringBuffer getdtcData(StringBuffer stringBuffer, String dtcData) {
		stringBuffer.append(getString(R.string.dtc_list) + ": " + dtcData
				+ "\n");
		if (!Utils.isEmpty(dtcData)) {
			List<FualtInfo> fualtValues = FaultParse.parse(this,
					dtcData.split(","));
			for (int i = 0; i < fualtValues.size(); i++) {
				FualtInfo fualtValue = fualtValues.get(i);
				stringBuffer.append(fualtValue.code + " " + fualtValue.meaning
						+ "\n");
			}
		}
		return stringBuffer;
	}
}
