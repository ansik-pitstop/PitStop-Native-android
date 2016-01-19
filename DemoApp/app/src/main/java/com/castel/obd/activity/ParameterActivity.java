package com.castel.obd.activity;

import com.castel.obd.R;
import com.castel.obd.adapter.DemoListAdapter;
import com.castel.obd.info.DemoInfo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class ParameterActivity extends Activity {
	private final DemoInfo[] demos = {
			new DemoInfo(R.string.setting_alarm, null),
			new DemoInfo(R.string.setting_regular, null),
			new DemoInfo(R.string.setting_oil_wear, null),
			new DemoInfo(R.string.setting_plate_number, null),
			new DemoInfo(R.string.setting_sn, null),
			new DemoInfo(R.string.setting_time, null),
			new DemoInfo(R.string.setting_warning, null),
			new DemoInfo(R.string.setting_delay, null),
			new DemoInfo(R.string.read_version, null),
			new DemoInfo(R.string.read_data_type, null),
			new DemoInfo(R.string.read_vin, null), };

	private final String[] types = { "1001", "1201", "1401", "1501", "1601",
			"1A01", "1B01", "1D01", "2301", "2401", "2201" };

	// 0:�п��أ�1���޿��أ�2��ֻ��ѯ�����ã�3���澯��4��ֻ�п���
	private final int[] onOffs = { 3, 0, 1, 1, 1, 1, 1, 1, 2, 2, 2 };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_control_command);

		init();
	}

	private void init() {
		ListView mListView = (ListView) findViewById(R.id.listView);
		mListView.setAdapter(new DemoListAdapter(this, demos));
		mListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View v, int index,
					long arg3) {
				if (1 == index) {
					startActivity(new Intent(ParameterActivity.this,
							RegularParameterInfoActivity.class));
				} else if (2 == index) {
					startActivity(new Intent(ParameterActivity.this,
							CarSettingActivity.class));
				} else {
					Intent intent = new Intent(ParameterActivity.this,
							ParameterInfoActivity.class);
					intent.putExtra("TYPE", types[index]);
					intent.putExtra("TYPE_TITLE", getString(demos[index].title));
					intent.putExtra("TYPE_ON_OFF", onOffs[index]);
					startActivity(intent);
				}
			}
		});
	}

}
