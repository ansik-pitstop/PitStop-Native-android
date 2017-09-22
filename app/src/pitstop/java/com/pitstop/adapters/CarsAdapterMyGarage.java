package com.pitstop.adapters;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.pitstop.R;
import com.pitstop.models.Car;

import java.util.List;

/**
 * Created by ishan on 2017-09-22.
 */

public class CarsAdapterMyGarage extends ArrayAdapter<Car> {

    private List<Car> carList;
    private Context context;




    public CarsAdapterMyGarage(@NonNull Context context, @LayoutRes int resource, List<Car> list) {
        super(context, resource);
        this.context = context;
        this.carList = list;
    }

    @Override
    public int getCount() {
        return carList.size();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View View, @NonNull ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View carView = inflater.inflate(R.layout.list_item_car_garage, parent, false);

        TextView carName = (TextView)carView.findViewById(R.id.car_name);
        TextView scanner = (TextView)carView.findViewById(R.id.device_paired_id);
        TextView carShop = (TextView)carView.findViewById(R.id.car_dealership_id);

        Car currCar = carList.get(position);
        carName.setText(currCar.getYear() + " " + currCar.getMake() + " " + currCar.getModel());
        scanner.setText(currCar.getScannerId());
        carShop.setText(currCar.getDealership().getName());
        return carView;
    }


}
