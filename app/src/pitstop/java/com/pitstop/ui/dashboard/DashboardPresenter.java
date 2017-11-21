package com.pitstop.ui.dashboard;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.pitstop.BuildConfig;
import com.pitstop.EventBus.CarDataChangedEvent;
import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.EventBus.EventType;
import com.pitstop.EventBus.EventTypeImpl;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.get.GetAlarmCountUseCase;
import com.pitstop.interactors.get.GetFuelConsumedUseCase;
import com.pitstop.interactors.get.GetFuelPricesUseCase;
import com.pitstop.interactors.get.GetUserCarUseCase;
import com.pitstop.interactors.update.UpdateCarMileageUseCase;
import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.network.RequestError;
import com.pitstop.ui.mainFragments.TabPresenter;
import com.pitstop.utils.MixpanelHelper;

import org.greenrobot.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Karol Zdebel on 9/7/2017.
 */

public class DashboardPresenter extends TabPresenter<DashboardView>{

    public static final String GAS_PRICE_SHARED_PREF = "gasPrices";
    public static final String LAST_UPDATED_DATE = "lastUpdatedDate";
    public static final String PRICE_AT_UPDATE = "priceAtUpdate";
    public static final String TOTAL_FUEL_CONSUMED_AT_UPDATE = "totalFuel";
    public static final String TOTAL_MONEY_SPENT_AT_UPDATE = "totalMoney";



    private final String TAG = getClass().getSimpleName();
    public final EventSource EVENT_SOURCE = new EventSourceImpl(EventSource.SOURCE_DASHBOARD);
    private UseCaseComponent useCaseComponent;
    private MixpanelHelper mixpanelHelper;

    private boolean isDealershipMercedes;
    private boolean updating = false;
    private int numAlarms = 0;
    private int carID = 0;
    private Car car = null;

    private boolean carHasScanner  = false;

    public DashboardPresenter(UseCaseComponent useCaseComponent
            , MixpanelHelper mixpanelHelper){
        this.useCaseComponent = useCaseComponent;
        this.mixpanelHelper = mixpanelHelper;
    }

    void onOfflineTryAgainClicked(){
        Log.d(TAG,"onOfflineTryAgainClicked()");
        if (getView() != null)
            onUpdateNeeded();
    }

    void onAddCarButtonClicked(){
        Log.d(TAG,"onAddCarButtonClicked()");
        if (getView() != null)
            getView().startAddCarActivity();
    }

    void onUnknownErrorTryAgainClicked(){
        Log.d(TAG,"onUnknownErrorTryAgainClicked()");
        onUpdateNeeded();
    }

    void onUpdateNeeded(){
        Log.d(TAG,"onUpdateNeeded()");
        if (updating || getView() == null) return;
        updating = true;
        getView().showLoading();

        useCaseComponent.getUserCarUseCase().execute(new GetUserCarUseCase.Callback() {
            @Override
            public void onCarRetrieved(Car car, Dealership dealership, boolean isLocal) {
                Log.d(TAG, "onCarRetrieved(): " + car.getId());
                if (!isLocal)
                    updating = false;
                if (getView() == null) return;

                if (!isLocal){
                    DashboardPresenter.this.carID = car.getId();
                    DashboardPresenter.this.car = car;
                    carHasScanner = !(car.getScanner() == null);
                    getFuelConsumed();
                    getAmountSpent();
                    useCaseComponent.getGetAlarmCountUseCase().execute(car.getId()
                            , new GetAlarmCountUseCase.Callback() {
                        @Override
                        public void onAlarmCountGot(int alarmCount) {
                            numAlarms = alarmCount;
                            if (alarmCount == 0){
                                if (getView()==null) return;
                                getView().hideBadge();
                            }
                            else {
                                getView().showBadges(alarmCount);
                            }
                        }
                        @Override
                        public void onError(@NotNull RequestError error) {
                            if (getView() == null )return;
                            getView().hideBadge();
                        }
                    });
                }

                getView().displayOnlineView();
                Log.d(TAG, Integer.toString(car.getId()));
                isDealershipMercedes = (dealership.getId() == 4
                        || dealership.getId() == 18);

                if (BuildConfig.DEBUG && (dealership.getId() == 4
                        || dealership.getId() == 18)){
                    getView().displayMercedesDealershipVisuals(dealership);
                } else if (!BuildConfig.DEBUG && dealership.getId() == 14){
                    getView().displayMercedesDealershipVisuals(dealership);
                } else {
                    getView().displayDefaultDealershipVisuals(dealership);
                }
                if (car.getScannerId()==null || car.getScannerId().equalsIgnoreCase("null")) {
                    getView().noScanner();
                    carHasScanner = false;
                }
                else carHasScanner= true;

                getView().displayCarDetails(car);
                if (!isLocal)
                    getView().hideLoading();
            }

            @Override
            public void onNoCarSet(boolean isLocal) {
                if (!isLocal){
                    updating = false;
                    carHasScanner = false;
                    if (getView() == null) return;
                    getView().displayNoCarView();
                    getView().hideLoading();
                }
            }

            @Override
            public void onError(RequestError error) {
                Log.d(TAG,"getUserCar() error: "+error);
                updating = false;
                if (getView() == null) return;
                if (error.getError()!=null) {
                    if (error.getError().equals(RequestError.ERR_OFFLINE)) {
                        if (getView().hasBeenPopulated()) {
                            getView().displayOfflineErrorDialog();
                        } else {
                            getView().displayOfflineView();
                        }
                    }else{
                        if (getView().hasBeenPopulated()) {
                            getView().displayUnknownErrorDialog();
                        } else {
                            getView().displayUnknownErrorView();
                        }
                    }
                }
                else{
                    getView().displayUnknownErrorView();
                }
                getView().hideLoading();
            }
        });

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
                getView().displayNoCarView();
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
                        getView().displayOfflineView();
                    }
                }
                else{
                    getView().displayOnlineView();
                    getView().displayUnknownErrorDialog();
                }

                getView().hideLoading();
            }
        });
    }

    void onMileageClicked(){
        Log.d(TAG,"onMileageClicked()");
        if (getView() != null)
            getView().displayUpdateMileageDialog();
    }

    void onRefresh(){
        Log.d(TAG,"onRefresh()");

        mixpanelHelper.trackViewRefreshed(MixpanelHelper.SERVICE_UPCOMING_VIEW);
        if (getView() != null && getView().isRefreshing() && updating){
            getView().hideRefreshing();
        }else{
            onUpdateNeeded();
        }

    }

    void onMyTripsButtonClicked(){
        Log.d(TAG,"onMyTripsButtonClicked()");
        if (getView() != null)
            getView().startMyTripsActivity();
    }

    @Override
    public EventType[] getIgnoredEventTypes() {
        Log.d(TAG,"getIgnoredEventTypes()");
        return new EventType[0];
    }

    @Override
    public void onAppStateChanged() {
        Log.d(TAG,"onAppStateChanged()");
        onUpdateNeeded();
    }

    @Override
    public EventSource getSourceType() {
        Log.d(TAG,"getSourceType()");
        return EVENT_SOURCE;
    }

    public void onTotalAlarmsClicked() {
        Log.d(TAG,"onTotalAlarmsClicked()");
        if (updating)return;
        if (getView() == null) return;
        if (carHasScanner){
            getView().openAlarmsActivity();
        }
        else {
            getView().displayBuyDeviceDialog();
        }
    }

    public void getAmountSpent(){
        Log.d(TAG, "getAmountSpent();");
        if (getView() == null) return;
        if (this.car.getScannerId()==null || this.car.getScannerId() == ""){
            getView().showFuelExpense((float) 0.0);
            return;
        }
        SharedPreferences sharedPreferences = ((android.support.v4.app.Fragment)getView()).
                                            getActivity().getSharedPreferences(GAS_PRICE_SHARED_PREF, Context.MODE_PRIVATE);
        if (!sharedPreferences.contains(LAST_UPDATED_DATE+car.getVin())){
            updatePrice(sharedPreferences);
            return;
        }
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String currentDate = Integer.toString(Calendar.getInstance().getTime().getYear())+ Integer.toString(Calendar.getInstance().getTime().getMonth()) +
                Integer.toString(Calendar.getInstance().getTime().getDate());
        Log.d(TAG, "current date: " + currentDate);
        Log.d(TAG, "last update date: " +sharedPreferences.getString(LAST_UPDATED_DATE+car.getVin(), "0000") );
        if (Integer.parseInt(currentDate) > Integer.parseInt(sharedPreferences.getString(LAST_UPDATED_DATE+car.getVin(), "0000"))){
            updatePrice(sharedPreferences);
            return;
        }

        else {
            useCaseComponent.getGetFuelConsumedUseCase().execute(car.getScannerId(), new GetFuelConsumedUseCase.Callback() {
                @Override
                public void onFuelConsumedGot(double fuelConsumed) {
                    if (getView() == null) return;
                    float oldMoneySpent = sharedPreferences.getFloat(TOTAL_MONEY_SPENT_AT_UPDATE + car.getVin(), (float) 0.0);
                    float oldFuelConsumed = sharedPreferences.getFloat(TOTAL_FUEL_CONSUMED_AT_UPDATE + car.getVin(), (float) 0.0);
                    float price = sharedPreferences.getFloat(PRICE_AT_UPDATE + car.getVin(), (float) 1.09);
                    if (getView() == null) return;
                    Log.d(TAG, "new fuel consumed: " + Double.toString(fuelConsumed));
                    Log.d(TAG, "old money spent: " + Float.toString(oldMoneySpent));
                    Log.d(TAG, "old fuel consumed: " + Float.toString(oldMoneySpent));
                    Log.d(TAG, "price: " + Float.toString(sharedPreferences.getFloat(PRICE_AT_UPDATE + car.getVin(), (float) 1.09)));

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
                useCaseComponent.getGetFuelConsumedUseCase().execute(car.getScannerId(), new GetFuelConsumedUseCase.Callback() {
                    @Override
                    public void onFuelConsumedGot(double fuelConsumed) {
                        if (getView() == null) return;
                        Log.d(TAG, "fuel consumed got: "  + Double.toString(fuelConsumed));
                        float oldConsumed = sharedPreferences.getFloat(TOTAL_FUEL_CONSUMED_AT_UPDATE+car.getVin(), 0);
                        float oldMoneySpent = sharedPreferences.getFloat(TOTAL_MONEY_SPENT_AT_UPDATE+car.getVin(), 0);
                        editor.putFloat(PRICE_AT_UPDATE+car.getVin(), (float)fuelPrice);
                        String date = Integer.toString(Calendar.getInstance().getTime().getYear()) + Integer.toString(Calendar.getInstance().getTime().getMonth()) +
                                Integer.toString(Calendar.getInstance().getTime().getDate());
                        editor.putString(LAST_UPDATED_DATE+car.getVin(), date);
                        editor.putFloat(TOTAL_FUEL_CONSUMED_AT_UPDATE+car.getVin(), (float)fuelConsumed);
                        float newMoneySpent = (oldMoneySpent) + ((float) fuelConsumed-oldConsumed)*(float) fuelPrice;
                        Log.d(TAG, "old fuel consumed "  + Double.toString(oldConsumed));
                        Log.d(TAG, "old moeny total: "  + Double.toString(oldMoneySpent));
                        Log.d(TAG, "new Money spent: "  + Double.toString(newMoneySpent));

                        editor.putFloat(TOTAL_MONEY_SPENT_AT_UPDATE+car.getVin(), newMoneySpent);
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


    public boolean isDealershipMercedes(){
        return this.isDealershipMercedes;
    }

    public void setNumAlarms(int alarms){
        this.numAlarms = alarms;
    }

    public int getNumAlarms(){
        return this.numAlarms;
    }

    public void onFuelConsumptionClicked() {
        if (carHasScanner)
            getView().showFuelConsumptionExplanationDialog();
        else
            getView().displayBuyDeviceDialog();


    }

    public void getFuelConsumed() {
        if (this.car.getScannerId()==null || this.car.getScannerId() == ""){
            if (getView() ==null) return;
            getView().showFuelConsumed(0.0);
            return;
        }
        useCaseComponent.getGetFuelConsumedUseCase().execute(car.getScannerId(), new GetFuelConsumedUseCase.Callback() {
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
        if (!carHasScanner){
            getView().displayBuyDeviceDialog();

        }
        else {
            getView().showFuelExpensesDialog();
        }

    }
}
