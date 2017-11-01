package com.pitstop.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pitstop.R;
import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.ui.main_activity.MainView;
import com.pitstop.ui.my_garage.MyGarageView;

import java.util.List;

/**
 * Created by ishan on 2017-09-22.
 */

public class CarsAdapter extends RecyclerView.Adapter<CarsAdapter.CarViewHolder> {

    private final String TAG = CarsAdapter.class.getSimpleName();


    private List<Car> carList;
    private List<Dealership> dealershipList;
    private MainView mainView;

    public CarsAdapter (MainView view, List<Dealership> dealershipList, List<Car> carList){
        this.mainView = view;
        this.carList = carList;
        this.dealershipList = dealershipList;
    }

    @Override
    public CarsAdapter.CarViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_car_garage, parent, false);
        CarsAdapter.CarViewHolder carViewHolder = new CarViewHolder((view));
        int position = getItemViewType(viewType);

        view.setOnClickListener(v -> mainView.onCarClicked(carList.get(position)));

        return carViewHolder;
    }

    @Override
    public void onBindViewHolder(CarsAdapter.CarViewHolder holder, int position) {
        holder.bind(carList.get(position),dealershipList.get(position));
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
        public void bind(Car car, Dealership dealership){
            boolean isCarCurrent  = car.isCurrentCar();
            Log.d(TAG, car.getModel() + isCarCurrent);
            carNameView.setText(car.getYear() + " " + car.getMake() + " " + car.getModel());
            if (isCarCurrent){
                carNameView.setTextColor(Color.rgb(43,131,226));
                scanner.setTextColor(Color.rgb(43,131,226));
                dealershipName.setTextColor(Color.rgb(43,131,226));
            }else{
                carNameView.setTextColor(Color.BLACK);
                scanner.setTextColor(Color.GRAY);
                dealershipName.setTextColor(Color.GRAY);
            }
            if(car.getScanner() == null){
                scanner.setText("No Paired Device");
            }
            else {
                scanner.setText(car.getScanner());
            }
            if (dealership.getName().equalsIgnoreCase("No Dealership")
                    || !dealership.getName().equalsIgnoreCase("No Shop") ) {
                dealershipName.setText(dealership.getName());
            }
            else {
                dealershipName.setText("No Associated Shop");
            }
        }
    }
}
