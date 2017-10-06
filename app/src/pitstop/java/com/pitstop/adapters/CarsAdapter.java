package com.pitstop.adapters;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pitstop.R;
import com.pitstop.models.Car;
import com.pitstop.ui.my_garage.MyGarageView;

import java.util.List;

/**
 * Created by ishan on 2017-09-22.
 */

public class CarsAdapter extends RecyclerView.Adapter<CarsAdapter.CarViewHolder> {

    private final String TAG = CarsAdapter.class.getSimpleName();

    private MyGarageView myGarageView;
    private List<Car> carList;

    public CarsAdapter (MyGarageView view, List<Car> list){
        this.myGarageView = view;
        this.carList = list;
    }

    @Override
    public CarsAdapter.CarViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_car_garage, parent, false);
        CarsAdapter.CarViewHolder carViewHolder = new CarViewHolder((view));
        int position = getItemViewType(viewType);
        view.setOnClickListener(v -> myGarageView
                .onCarClicked(carList.get(position), position));
        return carViewHolder;
    }

    @Override
    public void onBindViewHolder(CarsAdapter.CarViewHolder holder, int position) {
        holder.bind(carList.get(position));
    }

    @Override
    public int getItemCount() {
        return carList.size();
    }

    @Override
    public int getItemViewType(int position){
        return position;
    }

    public class CarViewHolder extends RecyclerView.ViewHolder{
        TextView carNameView;
        TextView scanner;
        TextView dealershipName;
        public CarViewHolder(View itemView) {
            super(itemView);
            this.carNameView = itemView.findViewById(R.id.car_name);
            this.scanner = itemView.findViewById(R.id.device_paired_id);
            this.dealershipName = itemView.findViewById(R.id.car_dealership_id);
        }
        public void bind(Car car){
            carNameView.setText(car.getYear() + " " + car.getMake() + " " + car.getModel());
            if(car.getScannerId() == null){
                scanner.setText("No Paired Device");
            }
            else {
                scanner.setText(car.getScannerId());
            }
            if (!car.getDealership().getName().equalsIgnoreCase("No Dealership") || !car.getDealership().getName().equalsIgnoreCase("No Shop") ) {
                dealershipName.setText(car.getDealership().getName());
            }
            else {
                dealershipName.setText("No Associated Shop");
            }
        }
    }
}
