package com.pitstop.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.pitstop.models.Car;
import com.pitstop.R;

/**
 * Created by David on 6/9/2016.
 */
public class MainAppSideMenuAdapter extends BaseAdapter {
    Car[] data;
    Context mContext;

    public MainAppSideMenuAdapter(Context context, Car[] cars){
        mContext = context;
        if(data!=null) {
            this.data = cars;
        }else{
            this.data = new Car [0];
        }
    }

    public void setData(Car[] updatedData){
        if(updatedData!=null) {
            this.data =updatedData;
        }else{
            this.data = new Car [0];
        }
    }
    @Override
    public int getCount() {
        return data.length;
    }

    @Override
    public Object getItem(int i) {
        return data[i];
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
        Car car =  data[i];
        titleView.setText(car.getYear()+" "+car.getMake()+" "+car.getModel() );
        if(car.isCurrentCar()){
            view.setBackground(new ColorDrawable(Color.argb(100,0,0,0)));
        }else{
            view.setBackground(null);
        }
        return view;
    }
}
