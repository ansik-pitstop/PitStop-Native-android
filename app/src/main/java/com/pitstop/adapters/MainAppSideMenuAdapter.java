package com.pitstop.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.pitstop.R;

import java.util.ArrayList;

/**
 * Created by David on 6/9/2016.
 */
public class MainAppSideMenuAdapter extends BaseAdapter {
    String[] titles;
    Context mContext;

    public MainAppSideMenuAdapter(Context context, String[] titles){
        mContext = context;
        if(titles!=null) {
            this.titles = titles;
        }else{
            this.titles = new String[0];
        }
    }
    @Override
    public int getCount() {
        return titles.length;
    }

    @Override
    public Object getItem(int i) {
        return titles[i];
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {

        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.app_main_drawer_list_item, null);
        }
        TextView titleView = (TextView) view.findViewById(R.id.car_title);

        titleView.setText( titles[i] );

        return view;
    }
}
