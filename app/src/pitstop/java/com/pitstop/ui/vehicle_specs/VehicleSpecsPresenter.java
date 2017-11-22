package com.pitstop.ui.vehicle_specs;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.pitstop.EventBus.CarDataChangedEvent;
import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.EventBus.EventType;
import com.pitstop.EventBus.EventTypeImpl;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.add.AddLicensePlateUseCase;
import com.pitstop.interactors.get.GetCarImagesArrayUseCase;
import com.pitstop.interactors.get.GetCarStyleIDUseCase;
import com.pitstop.interactors.get.GetFuelConsumedUseCase;
import com.pitstop.interactors.get.GetFuelPricesUseCase;
import com.pitstop.interactors.get.GetLicensePlateUseCase;
import com.pitstop.interactors.get.GetUserCarUseCase;
import com.pitstop.interactors.remove.RemoveCarUseCase;
import com.pitstop.interactors.update.UpdateCarMileageUseCase;
import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.network.RequestError;
import com.pitstop.ui.mainFragments.TabPresenter;
import com.pitstop.utils.MixpanelHelper;

import org.greenrobot.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;

import java.util.Calendar;

/**
 * Created by ishan on 2017-09-25.
 */

public class VehicleSpecsPresenter extends TabPresenter<VehicleSpecsView> {

    public static final String GAS_PRICE_SHARED_PREF = "gasPrices";
    public static final String LAST_UPDATED_DATE = "lastUpdatedDate";
    public static final String PRICE_AT_UPDATE = "priceAtUpdate";
    public static final String TOTAL_FUEL_CONSUMED_AT_UPDATE = "totalFuel";
    public static final String TOTAL_MONEY_SPENT_AT_UPDATE = "totalMoney";

    private final static String TAG = VehicleSpecsPresenter.class.getSimpleName();
    private UseCaseComponent useCaseComponent;
    private MixpanelHelper mixpanelHelper;
    private boolean updating;
    private Car mCar;

    private Dealership mdealership;
    public final EventSource EVENT_SOURCE = new EventSourceImpl(EventSource.SOURCE_MY_CAR);
    public final EventType[] ignoredEvents = {
            new EventTypeImpl(EventType.EVENT_SERVICES_HISTORY),
            new EventTypeImpl(EventType.EVENT_DTC_NEW),
            new EventTypeImpl(EventType.EVENT_SERVICES_NEW)
    };


    @Override
    public EventType[] getIgnoredEventTypes() {
        return ignoredEvents;
    }

    @Override
    public void onAppStateChanged() {
        onUpdateNeeded();
    }

    @Override
    public EventSource getSourceType() {
        return EVENT_SOURCE;
    }

    public static final String BASE_URL_PHOTO = "https://media.ed.edmunds-media.com";

    public VehicleSpecsPresenter(UseCaseComponent useCaseComponent, MixpanelHelper mixpanelHelper) {
        this.useCaseComponent = useCaseComponent;
        this.mixpanelHelper = mixpanelHelper;
    }

    public void onUpdateLicensePlateDialogConfirmClicked(int carID, String s) {
        Log.d(TAG, "onUpdateLicensePlateDialogConfirmClicked()");
        if (getView()== null|| updating)return;
        updating = true;
        useCaseComponent.addLicensePlateUseCase().execute(carID, s, new AddLicensePlateUseCase.Callback() {
            @Override
            public void onLicensePlateStored(String licensePlate) {
                updating = false;
                if (getView()== null)return;
                getView().showLicensePlate(licensePlate);
            }

            @Override
            public void onError(RequestError error) {}
        });
    }

    public void getCarImage(String Vin){
        if (getView() == null || updating)return;
        updating = true;
        Log.d(TAG, "getCarImage()");
        getView().showImageLoading();
        useCaseComponent.getCarStyleIDUseCase().execute(Vin, new GetCarStyleIDUseCase.Callback() {
            @Override
            public void onStyleIDGot(String styleID) {
                if (getView() == null)return;
                Log.d(TAG, styleID);
                useCaseComponent.getCarImagesArrayUseCase().execute(styleID, new GetCarImagesArrayUseCase.Callback() {
                    @Override
                    public void onArrayGot(String imageLink) {
                        updating = false;
                        if (getView() == null) return;
                        getView().hideImageLoading();
                        getView().showImage(BASE_URL_PHOTO + imageLink);
                    }
                    @Override
                    public void onError(RequestError error) {
                        updating = false;
                        if (getView() ==null) return;
                        getView().hideImageLoading();
                        getView().showDealershipBanner();
                       // Log.d(TAG, error.getMessage());
                    }
                });
            }
            @Override
            public void onError(RequestError error) {
                updating = false;
                if (getView() == null) return;
                getView().hideImageLoading();
                getView().showDealershipBanner();
                Log.d(TAG, error.getMessage());
            }
        });
    }

    public void getLicensePlate(int carID){
        Log.d(TAG, "getLicensePlate()");
        if (getView() == null) return;
        useCaseComponent.getLicensePlateUseCase().execute(carID, new GetLicensePlateUseCase.Callback() {
            @Override
            public void onLicensePlateGot(String licensePlate) {
                if (getView() == null) return;
                Log.d(TAG, "licensePlateGot");
                getView().showLicensePlate(licensePlate);
            }
            @Override
            public void onError(RequestError error) {
                if (getView() == null) return;
                Log.d(TAG, "gettingLicensePlateFailed");
                getView().showLicensePlate("");
            }
        });
    }

    public void deleteCar(){
        if(getView() == null||updating)return;
        updating = true;
        getView().showLoadingDialog("Loading...");
        Log.d(TAG, "deleteCar()");
        useCaseComponent.removeCarUseCase().execute(this.mCar.getId(), EventSource.SOURCE_MY_GARAGE, new RemoveCarUseCase.Callback() {
            @Override
            public void onCarRemoved() {
                updating = false;
                if (getView() == null)return;
                getView().hideLoadingDialog();
                onUpdateNeeded();

            }
            @Override
            public void onError(RequestError error) {
                updating = false;
                if (getView() == null) return;
                getView().hideLoadingDialog();
                getView().toast(error.getMessage());
            }
        });
    }

    public void getCurrentCar() {
        Log.d(TAG, "getCurrentCar()");
        if (getView() == null|| updating)return;
        updating = true;
        getView().showLoading();
        useCaseComponent.getUserCarUseCase().execute(new GetUserCarUseCase.Callback() {
            @Override
            public void onCarRetrieved(Car car, Dealership dealership, boolean isLocal) {
                mCar = car;
                mdealership = car.getShop();
                if (!isLocal)
                    updating = false;
                if (getView()!=null) {
                    if (!isLocal)
                        getView().hideLoading();
                    getView().setCarView(mCar);
                    getFuelConsumed();
                    getAmountSpent();
                    getView().showNormalLayout();
                    //getCarImage(mCar.getVin());
                }
            }

            @Override
            public void onNoCarSet(boolean isLocal) {
                updating = false;
                if (getView()!=null && !isLocal){
                    getView().showNoCarView();
                    getView().hideLoading();
                }
            }

            @Override
            public void onError(RequestError error) {
                updating = false;
                if (getView() == null) return;
                if (error.getError().equals(RequestError.ERR_OFFLINE)) {
                    if (getView().hasBeenPopulated())
                        getView().displayOfflineErrorDialog();
                    else
                        getView().showOfflineErrorView();
                }
                else {
                    if (getView().hasBeenPopulated())
                        getView().displayUnknownErrorDialog();
                    else
                        getView().showUnknownErrorView();
                }
                getView().hideLoading();
            }
        });



    }

    public Car getCar() {
        Log.d(TAG, "getCar()");
        if (this.mCar!=null){
            return this.mCar;
        }
        else
            return null;
    }

    public Dealership getDealership(){
        Log.d(TAG, "getDealership()");
        return this.mdealership;
    }

    public void onScannerViewClicked() {
        Log.d(TAG, "onScannerVIewCLicked()");
        if (this.mCar.getScannerId() == null&& getView()!= null)
            getView().showBuyDeviceDialog();

    }

    public void onUpdateNeeded() {
        Log.d(TAG, "onUdateNeeded()");
        getCurrentCar();
    }

    public void onRefresh() {
        Log.d(TAG, "onRefresh()");
        onUpdateNeeded();
    }

    void onUpdateMileageDialogConfirmClicked(String mileageText){
        Log.d(TAG,"onUpdateMileageDialogConfirmClicked()");
        if (updating) return;
        updating = true;
        getView().showLoading();
        double mileage;
        try{
            mileage = Double.valueOf(mileageText);
        }catch(NumberFormatException e){
            e.printStackTrace();
            getView().displayUpdateMileageError();
            getView().hideLoading();
            updating = false;
            return;
        }

        if (mileage < 0 || mileage > 3000000){
            getView().hideLoading();
            getView().displayUpdateMileageError();
            updating = false;
            return;
        }

        useCaseComponent.updateCarMileageUseCase().execute(mileage
                , new UpdateCarMileageUseCase.Callback() {

                    @Override
                    public void onMileageUpdated() {
                        updating = false;
                        EventBus.getDefault().post(new CarDataChangedEvent(
                                new EventTypeImpl(EventType.EVENT_MILEAGE),EVENT_SOURCE));
                        if (getView() == null) return;
                        getView().hideLoading();

                        try{
                            getView().displayMileage(mileage);
                        }catch(NumberFormatException e){
                            e.printStackTrace();
                            getView().displayUnknownErrorDialog();
                        }
                    }

                    @Override
                    public void onNoCarAdded() {
                        updating = false;
                        if (getView() == null) return;
                        getView().hideLoading();
                        getView().showNoCarView();
                    }

                    @Override
                    public void onError(RequestError error) {
                        updating = false;
                        if (getView() == null) return;

                        if (error.getError().equals(RequestError.ERR_OFFLINE)){
                            if (getView().hasBeenPopulated()){
                                getView().displayOfflineErrorDialog();
                            }
                            else{
                                getView().showOfflineErrorView();
                            }
                        }
                        else{
                            getView().hideLoading();
                            getView().displayUnknownErrorDialog();
                        }

                        getView().hideLoading();
                    }
                });
    }


    public void getAmountSpent(){
        Log.d(TAG, "getAmountSpent();");
        if (getView() == null) return;
        if (this.mCar.getScannerId()==null || this.mCar.getScannerId() == ""){
            getView().showFuelExpense((float) 0.0);
            return;
        }
        SharedPreferences sharedPreferences = ((android.support.v4.app.Fragment)getView()).
                getActivity().getSharedPreferences(GAS_PRICE_SHARED_PREF, Context.MODE_PRIVATE);
        if (!sharedPreferences.contains(LAST_UPDATED_DATE+mCar.getVin())){
            updatePrice(sharedPreferences);
            return;
        }
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String currentDate = Integer.toString(Calendar.getInstance().getTime().getYear())+ Integer.toString(Calendar.getInstance().getTime().getMonth()) +
                Integer.toString(Calendar.getInstance().getTime().getDate());
        Log.d(TAG, "current date: " + currentDate);
        Log.d(TAG, "last update date: " +sharedPreferences.getString(LAST_UPDATED_DATE+mCar.getVin(), "0000") );
        if (Integer.parseInt(currentDate) > Integer.parseInt(sharedPreferences.getString(LAST_UPDATED_DATE+mCar.getVin(), "0000"))){
            updatePrice(sharedPreferences);
            return;
        }

        else {
            useCaseComponent.getGetFuelConsumedUseCase().execute(mCar.getScannerId(), new GetFuelConsumedUseCase.Callback() {
                @Override
                public void onFuelConsumedGot(double fuelConsumed) {
                    if (getView() == null) return;
                    float oldMoneySpent = sharedPreferences.getFloat(TOTAL_MONEY_SPENT_AT_UPDATE + mCar.getVin(), (float) 0.0);
                    float oldFuelConsumed = sharedPreferences.getFloat(TOTAL_FUEL_CONSUMED_AT_UPDATE + mCar.getVin(), (float) 0.0);
                    float price = sharedPreferences.getFloat(PRICE_AT_UPDATE + mCar.getVin(), (float) 1.09);
                    if (getView() == null) return;
                    Log.d(TAG, "new fuel consumed: " + Double.toString(fuelConsumed));
                    Log.d(TAG, "old money spent: " + Float.toString(oldMoneySpent));
                    Log.d(TAG, "old fuel consumed: " + Float.toString(oldMoneySpent));
                    Log.d(TAG, "price: " + Float.toString(sharedPreferences.getFloat(PRICE_AT_UPDATE + mCar.getVin(), (float) 1.09)));

                    float moneyInCents = ((float) fuelConsumed - oldFuelConsumed) * price + oldMoneySpent;
                    getView().showFuelExpense(moneyInCents);
                }

                @Override
                public void onError(@NotNull RequestError error) {
                }
            });
        }


    }



    private void updatePrice(final SharedPreferences sharedPreferences) {
        Log.d(TAG, "updatePrice();");
        SharedPreferences.Editor editor = sharedPreferences.edit();
        useCaseComponent.getFuelPriceUseCase().execute(getView().getLastKnowLocation(), new GetFuelPricesUseCase.Callback() {
            @Override
            public void onFuelPriceGot(double fuelPrice) {
                if (getView() == null) return;
                Log.d(TAG, "just got fuel price, its: " + Double.toString(fuelPrice));
                useCaseComponent.getGetFuelConsumedUseCase().execute(mCar.getScannerId(), new GetFuelConsumedUseCase.Callback() {
                    @Override
                    public void onFuelConsumedGot(double fuelConsumed) {
                        if (getView() == null) return;
                        Log.d(TAG, "fuel consumed got: "  + Double.toString(fuelConsumed));
                        float oldConsumed = sharedPreferences.getFloat(TOTAL_FUEL_CONSUMED_AT_UPDATE+mCar.getVin(), 0);
                        float oldMoneySpent = sharedPreferences.getFloat(TOTAL_MONEY_SPENT_AT_UPDATE+mCar.getVin(), 0);
                        editor.putFloat(PRICE_AT_UPDATE+mCar.getVin(), (float)fuelPrice);
                        String date = Integer.toString(Calendar.getInstance().getTime().getYear()) + Integer.toString(Calendar.getInstance().getTime().getMonth()) +
                                Integer.toString(Calendar.getInstance().getTime().getDate());
                        editor.putString(LAST_UPDATED_DATE+mCar.getVin(), date);
                        editor.putFloat(TOTAL_FUEL_CONSUMED_AT_UPDATE+mCar.getVin(), (float)fuelConsumed);
                        float newMoneySpent = (oldMoneySpent) + ((float) fuelConsumed-oldConsumed)*(float) fuelPrice;
                        Log.d(TAG, "old fuel consumed "  + Double.toString(oldConsumed));
                        Log.d(TAG, "old moeny total: "  + Double.toString(oldMoneySpent));
                        Log.d(TAG, "new Money spent: "  + Double.toString(newMoneySpent));

                        editor.putFloat(TOTAL_MONEY_SPENT_AT_UPDATE+mCar.getVin(), newMoneySpent);
                        editor.commit();

                        getView().showFuelExpense(newMoneySpent);
                    }
                    @Override
                    public void onError(@NotNull RequestError error) {
                        Log.d(TAG, "couldnt update price");
                    }
                });
            }
            @Override
            public void onError(@NotNull RequestError error) {
                Log.d(TAG, "couldnt update price");

            }
        });



    }

    public void onFuelConsumptionClicked() {
        if (this.mCar.getScanner()==null || this.mCar.getScannerId() == "" )
            getView().showBuyDeviceDialog();
        else
            getView().showFuelConsumptionExplanationDialog();
    }

    public void getFuelConsumed() {

        if (this.mCar.getScannerId()==null || this.mCar.getScannerId() == ""){
            if (getView() ==null) return;
            getView().showFuelConsumed(0.0);
            return;
        }
        useCaseComponent.getGetFuelConsumedUseCase().execute(this.mCar.getScannerId(), new GetFuelConsumedUseCase.Callback() {
            @Override
            public void onFuelConsumedGot(double fuelConsumed) {
                if (getView() == null) return;
                getView().showFuelConsumed(fuelConsumed);
            }

            @Override
            public void onError(@NotNull RequestError error) {
                Log.d(TAG, "getFuelConsumedError");
            }
        });
    }

    public void onFuelExpensesClicked() {
        if (updating)return;
        if (getView() == null) return;
        if (mCar.getScannerId()==null){
            getView().showBuyDeviceDialog();
        }
        else {
            getView().showFuelExpensesDialog();
        }
    }
}