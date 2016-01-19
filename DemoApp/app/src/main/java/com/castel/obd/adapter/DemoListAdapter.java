package com.castel.obd.adapter;

import com.castel.obd.R;
import com.castel.obd.info.DemoInfo;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class DemoListAdapter extends BaseAdapter {
	private Context mContext;
	private DemoInfo[] demos;

	public DemoListAdapter(Context context, DemoInfo[] demos) {
		mContext = context;
		this.demos = demos;
	}

	@Override
	public View getView(int index, View convertView, ViewGroup parent) {
		convertView = View.inflate(mContext, R.layout.layout_list_item, null);
		TextView title = (TextView) convertView.findViewById(R.id.title);
		title.setText(demos[index].title);
		return convertView;
	}

	@Override
	public int getCount() {
		return demos.length;
	}

	@Override
	public Object getItem(int index) {
		return demos[index];
	}

	@Override
	public long getItemId(int id) {
		return id;
	}
}
