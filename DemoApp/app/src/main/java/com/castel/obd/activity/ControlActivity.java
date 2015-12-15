package com.castel.obd.activity;

import com.castel.obd.R;
import com.castel.obd.adapter.DemoListAdapter;
import com.castel.obd.bluetooth.BluetoothManage;
import com.castel.obd.bluetooth.BluetoothManage.BluetoothDataListener;
import com.castel.obd.info.DataPackageInfo;
import com.castel.obd.info.DemoInfo;
import com.castel.obd.info.ParameterPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;
import com.castel.obd.util.LogUtil;
import com.castel.obd.util.ProgressDialogUtil;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class ControlActivity extends Activity implements BluetoothDataListener {
	private static final DemoInfo[] demos = {
			new DemoInfo(R.string.control_clear_dtc, null),
			new DemoInfo(R.string.control_clear_obd, null),
			new DemoInfo(R.string.control_obd_resetting, null),
			new DemoInfo(R.string.control_factory_setting, null) };

	private TextView tvResult;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_control_command);

		BluetoothManage.getInstance(this).setBluetoothDataListener(this);

		init();

	}

	private void init() {
		tvResult = (TextView) findViewById(R.id.tv_result);

		ListView mListView = (ListView) findViewById(R.id.listView);
		mListView.setAdapter(new DemoListAdapter(this, demos));
		mListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View v, int index,
					long arg3) {
				switch (index) {
				case 0:
					// ????????0x01
					BluetoothManage.getInstance(ControlActivity.this)
							.obdSetCtrl(0x01);
					break;
				case 1:
					// ????OBD?υτ0x02
					BluetoothManage.getInstance(ControlActivity.this)
							.obdSetCtrl(0x02);
					break;
				case 2:
					// ???????????0x03
					BluetoothManage.getInstance(ControlActivity.this)
							.obdSetCtrl(0x03);
					break;
				case 3:
					// ???OBD?????0x04
					BluetoothManage.getInstance(ControlActivity.this)
							.obdSetCtrl(0x04);
					break;
				}
			}
		});
	}

	@Override
	public void setCtrlResponse(ResponsePackageInfo responsePackageInfo) {
		ProgressDialogUtil.dismiss();

		setResultShow(responsePackageInfo);
	}

	@Override
	public void setParamaterResponse(ResponsePackageInfo responsePackageInfo) {

	}

	@Override
	public void getParamaterData(ParameterPackageInfo parameterPackageInfo) {

	}

	@Override
	public void getIOData(DataPackageInfo dataPackage) {

	}

	@Override
	public void getBluetoothState(int state) {

	}

	private void setResultShow(ResponsePackageInfo responsePackageInfo) {
		if (null == responsePackageInfo) {
			tvResult.setText(R.string.fail);
			return;
		}

		String result = "";
		if ("01".equals(responsePackageInfo.type)) {
			LogUtil.i("???DTC???");
			result = getTypeString(responsePackageInfo.value,
					R.string.control_clear_dtc);
		} else if ("02".equals(responsePackageInfo.type)) {
			LogUtil.i("???OBD?????");
			result = getTypeString(responsePackageInfo.value,
					R.string.control_clear_obd);
		} else if ("03".equals(responsePackageInfo.type)) {
			LogUtil.i("????OBD?υτ");
			result = getTypeString(responsePackageInfo.value,
					R.string.control_obd_resetting);
		} else if ("04".equals(responsePackageInfo.type)) {
			LogUtil.i("????????");
			result = getTypeString(responsePackageInfo.value,
					R.string.control_factory_setting);
		}

		tvResult.setText(result);
		Toast.makeText(this, result, Toast.LENGTH_LONG).show();
	}

	private String getTypeString(String value, int titleId) {
		StringBuffer msgBuffer = new StringBuffer();
		msgBuffer.append(getString(titleId));
		if (0 == Integer.parseInt(value)) {
			LogUtil.i("???");
			msgBuffer.append(getString(R.string.fail));
		} else if (1 == Integer.parseInt(value)) {
			LogUtil.i("???");
			msgBuffer.append(getString(R.string.success));
		}

		return msgBuffer.toString();
	}
}
