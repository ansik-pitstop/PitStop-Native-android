package com.castel.obd.activity;

import java.util.List;

import com.castel.obd.OBD;
import com.castel.obd.R;
import com.castel.obd.adapter.DemoListAdapter;
import com.castel.obd.bluetooth.BluetoothManage;
import com.castel.obd.bluetooth.BluetoothManage.BluetoothDataListener;
import com.castel.obd.info.AlarmInfo;
import com.castel.obd.info.DataPackageInfo;
import com.castel.obd.info.DemoInfo;
import com.castel.obd.info.FualtInfo;
import com.castel.obd.info.ParameterPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;
import com.castel.obd.log.LogCatHelper;
import com.castel.obd.parse.AlarmParse;
import com.castel.obd.parse.FaultParse;
import com.castel.obd.util.JsonUtil;
import com.castel.obd.util.LogUtil;

import android.support.v7.app.ActionBarActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class MainActivity extends ActionBarActivity implements OnClickListener,
		BluetoothDataListener {
	private static final DemoInfo[] demos = {
			new DemoInfo(R.string.control_command, ControlActivity.class),
			new DemoInfo(R.string.monitor_command, MonitorActivity.class),
			new DemoInfo(R.string.setting_parameter, ParameterActivity.class),
			new DemoInfo(R.string.data_stream, DataActivity.class) };

	private Button btnBT;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		LogCatHelper.getInstance(this).start();

		//�����������Ӽ���ݶ�ȡ�ļ���
		BluetoothManage.getInstance(this).setBluetoothDataListener(this);

		init();
		
//		LogUtil.i("result:" + OBD.init("212BM1144101221", "4294967295"));
	}

	private void init() {
		btnBT = (Button) findViewById(R.id.btn_bt);
		btnBT.setOnClickListener(this);

		ListView mListView = (ListView) findViewById(R.id.listView);
		mListView.setAdapter(new DemoListAdapter(this, demos));
		mListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View v, int index,
					long arg3) {
				Intent intent = null;
				intent = new Intent(MainActivity.this, demos[index].demoClass);
				startActivity(intent);
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		LogCatHelper.getInstance(this).stop();
		BluetoothManage.getInstance(this).close();
	}

	@Override
	public void onClick(View arg0) {
		switch (arg0.getId()) {
		case R.id.btn_bt:
			if (BluetoothManage.getInstance(MainActivity.this).getState() == BluetoothManage.CONNECTED) {
				btnBT.setText(R.string.bluetooth_connected);
			} else {
				BluetoothManage.getInstance(MainActivity.this)
						.connectBluetooth();
			}
			break;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void getBluetoothState(int state) {
		if (state == BluetoothManage.CONNECTED) {
			btnBT.setText(R.string.bluetooth_connected);
		} else {
			btnBT.setText(R.string.bluetooth_disconnected);
		}
	}

	@Override
	public void setCtrlResponse(ResponsePackageInfo responsePackageInfo) {

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

}
