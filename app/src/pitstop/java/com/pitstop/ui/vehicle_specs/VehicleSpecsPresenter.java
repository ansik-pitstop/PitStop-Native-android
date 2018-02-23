package com.pitstop.ui.vehicle_specs;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.pitstop.EventBus.CarDataChangedEvent;
import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.EventBus.EventType;
import com.pitstop.EventBus.EventTypeImpl;
import com.pitstop.bluetooth.BluetoothAutoConnectService;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.add.AddLicensePlateUseCase;
import com.pitstop.interactors.add.AddScannerUseCase;
import com.pitstop.interactors.get.GetAlarmCountUseCase;
import com.pitstop.interactors.get.GetFuelConsumedAndPriceUseCase;
import com.pitstop.interactors.get.GetFuelConsumedUseCase;
import com.pitstop.interactors.get.GetLicensePlateUseCase;
import com.pitstop.interactors.get.GetUserCarUseCase;
import com.pitstop.interactors.remove.RemoveCarUseCase;
import com.pitstop.models.Alarm;
import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.network.RequestError;
import com.pitstop.observer.AlarmObservable;
import com.pitstop.observer.AlarmObserver;
import com.pitstop.observer.FuelObservable;
import com.pitstop.observer.FuelObserver;
import com.pitstop.ui.mainFragments.TabPresenter;
import com.pitstop.utils.MixpanelHelper;

import org.greenrobot.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;

import java.util.Calendar;

/**
 * Created by ishan on 2017-09-25.
 */

public class VehicleSpecsPresenter extends TabPresenter<VehicleSpecsView> implements FuelObserver, AlarmObserver {

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
    private boolean carHasScanner = false;
    private FuelObservable fuelObservable;
    private AlarmObservable alarmObservable;


    private Dealership mdealership;
    public final EventSource EVENT_SOURCE = new EventSourceImpl(EventSource.SOURCE_MY_CAR);
    public final EventType[] ignoredEvents = {
            new EventTypeImpl(EventType.EVENT_SERVICES_HISTORY),
            new EventTypeImpl(EventType.EVENT_DTC_NEW),
            new EventTypeImpl(EventType.EVENT_SERVICES_NEW)
    };
    private int numAlarms;


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
        if (getView() == null || updating) return;
        updating = true;
        useCaseComponent.addLicensePlateUseCase().execute(carID, s, new AddLicensePlateUseCase.Callback() {
            @Override
            public void onLicensePlateStored(String licensePlate) {
                updating = false;
                if (getView() == null) return;
                getView().showLicensePlate(licensePlate);
            }

            @Override
            public void onError(RequestError error) {
            }
        });
    }

    public void getLicensePlate(int carID) {
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

    public void deleteCar() {
        if (getView() == null || updating) return;
        updating = true;
        getView().showLoadingDialog("Loading...");
        Log.d(TAG, "deleteCar()");
        useCaseComponent.removeCarUseCase().execute(this.mCar.getId(), EventSource.SOURCE_MY_GARAGE, new RemoveCarUseCase.Callback() {
            @Override
            public void onCarRemoved() {
                updating = false;
                if (getView() == null) return;
                getView().hideLoadingDialog();
                onUpdateNeeded();

            }

            @Override
            public void onError(RequestError error) {
                updating = false;
                if (getView() == null) return;
                getView().hideLoadingDialog();
                getView().displayOfflineErrorDialog();
            }
        });
    }

    public Car getCar() {
        Log.d(TAG, "getCar()");
        if (this.mCar != null) {
            return this.mCar;
        } else
            return null;
    }

    public Dealership getDealership() {
        Log.d(TAG, "getDealership()");
        return this.mdealership;
    }

    public void onScannerViewClicked() {
        Log.d(TAG, "onScannerViewCLicked()");
        if (getView() == null) return;
        getView().showPairScannerDialog();
    }

    public void onUpdateNeeded() {
        Log.d(TAG, "onUdateNeeded()");
        if (getView() == null || updating) return;
        updating = true;
        getView().showLoading();
        useCaseComponent.getGetAlarmCountUseCase().execute(new GetAlarmCountUseCase.Callback() {
            @Override
            public void onAlarmCountGot(int alarmCount) {
                numAlarms = alarmCount;
                if (alarmCount == 0) {
                    if (getView() == null) return;
                    getView().hideBadge();
                } else {
                    getView().showBadges(alarmCount);
                }
            }

            @Override
            public void onError(@NotNull RequestError error) {
                if (getView() == null) return;
                getView().hideBadge();
            }
        });

        useCaseComponent.getUserCarUseCase().execute(new GetUserCarUseCase.Callback() {
            @Override
            public void onCarRetrieved(Car car, Dealership dealership, boolean isLocal) {
                mCar = car;
                mdealership = car.getShop();
                if (!isLocal)
                    updating = false;
                if (getView() != null) {
                    if (!isLocal) {
                        getView().hideLoading();
                    }
                    getView().setCarView(mCar);

                    getFuelConsumed(car.getScannerId());
                    String scannerId = car.getScannerId();
                    carHasScanner = (scannerId != null && !scannerId.equalsIgnoreCase(""));
                    getAmountSpent(scannerId);

                    getView().displayCarDetails(car);
                    getView().showNormalLayout();

                    getView().displayDefaultDealershipVisuals(dealership);
                    //getCarImage(mCar.getVin());
                }
            }

            @Override
            public void onNoCarSet(boolean isLocal) {
                if (getView() != null && !isLocal) {
                    updating = false;
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
                } else {
                    if (getView().hasBeenPopulated())
                        getView().displayUnknownErrorDialog();
                    else
                        getView().showUnknownErrorView();
                }
                getView().hideLoading();
            }
        });
    }

    public void onRefresh() {
        Log.d(TAG, "onRefresh()");
        onUpdateNeeded();
    }

    private void getAmountSpent(String scannerId) {
        Log.d(TAG, "getAmountSpent();");
        if (getView() == null) return;

        if (scannerId == null || scannerId.equalsIgnoreCase("")) {
            getView().showFuelExpense((float) 0.0);
            return;
        }
        SharedPreferences sharedPreferences = ((android.support.v4.app.Fragment) getView()).
                getActivity().getSharedPreferences(GAS_PRICE_SHARED_PREF, Context.MODE_PRIVATE);
        if (!sharedPreferences.contains(LAST_UPDATED_DATE + mCar.getVin())) {
            updatePrice(scannerId, sharedPreferences);
            return;
        }
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String currentDate = Integer.toString(Calendar.getInstance().getTime().getYear()) + Integer.toString(Calendar.getInstance().getTime().getMonth()) +
                Integer.toString(Calendar.getInstance().getTime().getDate());
        Log.d(TAG, "current date: " + currentDate);
        Log.d(TAG, "last update date: " + sharedPreferences.getString(LAST_UPDATED_DATE + mCar.getVin(), "0000"));
        if (Integer.parseInt(currentDate) > Integer.parseInt(sharedPreferences.getString(LAST_UPDATED_DATE + mCar.getVin(), "0000"))) {
            updatePrice(scannerId, sharedPreferences);
            return;
        } else {
            useCaseComponent.getGetFuelConsumedUseCase().execute(scannerId, new GetFuelConsumedUseCase.Callback() {
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

    private void updatePrice(String scannerId, final SharedPreferences sharedPreferences) {
        Log.d(TAG, "updatePrice();");
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String lastKnownLocation = getView().getLastKnowLocation();
        if (lastKnownLocation == null) return;
        useCaseComponent.getGetFuelConsumedAndPriceUseCase().execute(lastKnownLocation, scannerId, new GetFuelConsumedAndPriceUseCase.Callback() {
            @Override
            public void onGotFuelConsumedAndPrice(double price, double fuelConsumed) {
                if (getView() == null) return;
                Log.d(TAG, "onGotFuelConsumedAndPrice, Price: " + Double.toString(price) + " fuelConsumed: " + Double.toString(fuelConsumed));
                if (getView() == null) return;
                Log.d(TAG, "fuel consumed got: " + Double.toString(fuelConsumed));
                float oldConsumed = sharedPreferences.getFloat(TOTAL_FUEL_CONSUMED_AT_UPDATE + mCar.getVin(), 0);
                float oldMoneySpent = sharedPreferences.getFloat(TOTAL_MONEY_SPENT_AT_UPDATE + mCar.getVin(), 0);
                editor.putFloat(PRICE_AT_UPDATE + mCar.getVin(), (float) price);
                String date = Integer.toString(Calendar.getInstance().getTime().getYear()) + Integer.toString(Calendar.getInstance().getTime().getMonth()) +
                        Integer.toString(Calendar.getInstance().getTime().getDate());
                editor.putString(LAST_UPDATED_DATE + mCar.getVin(), date);
                editor.putFloat(TOTAL_FUEL_CONSUMED_AT_UPDATE + mCar.getVin(), (float) fuelConsumed);
                float newMoneySpent = (oldMoneySpent) + ((float) fuelConsumed - oldConsumed) * (float) price;
                Log.d(TAG, "old fuel consumed " + Double.toString(oldConsumed));
                Log.d(TAG, "old moeny total: " + Double.toString(oldMoneySpent));
                Log.d(TAG, "new Money spent: " + Double.toString(newMoneySpent));
                editor.putFloat(TOTAL_MONEY_SPENT_AT_UPDATE + mCar.getVin(), newMoneySpent);
                editor.commit();
                getView().showFuelExpense(newMoneySpent);
            }

            @Override
            public void onError(@NotNull RequestError error) {
                Log.d(TAG, "couldnt update price");
            }
        });

    }

    public void onFuelConsumptionClicked() {
        Log.d(TAG,"onFuelConsumptionClicked() car: "+mCar);
        if (this.mCar.getScannerId() == null || this.mCar.getScannerId().isEmpty())
            getView().showBuyDeviceDialog();
        else
            getView().showFuelConsumptionExplanationDialog();
    }

    public void getFuelConsumed(String scannerId) {

        if (scannerId == null || scannerId.equals("")) {
            if (getView() == null) return;
            getView().showFuelConsumed(0.0);
            return;
        }
        useCaseComponent.getGetFuelConsumedUseCase().execute(scannerId, new GetFuelConsumedUseCase.Callback() {
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
        if (updating) return;
        if (getView() == null) return;
        if (mCar.getScannerId() == null) {
            getView().showBuyDeviceDialog();
        } else {
            getView().showFuelExpensesDialog();
        }
    }

    public void onTotalAlarmsClicked() {
        Log.d(TAG, "onTotalAlarmsClicked()");
        if (updating) return;
        if (getView() == null) return;
        if (carHasScanner) {
            getView().openAlarmsActivity();
        } else {
            getView().showBuyDeviceDialog();
        }
    }

    @Override
    public void onFuelConsumedUpdated() {
        if (mCar == null) return;
        getFuelConsumed(mCar.getScannerId());
    }


    @Override
    public void onAlarmAdded(Alarm alarm) {
        numAlarms++;
        if (getView() != null)
            getView().showBadges(numAlarms);

    }

    public void onServiceBound(BluetoothAutoConnectService bluetoothAutoConnectService) {
        this.fuelObservable = (FuelObservable) bluetoothAutoConnectService;
        fuelObservable.subscribe(this);
        this.alarmObservable = (AlarmObservable) bluetoothAutoConnectService;
        alarmObservable.subscribe(this);
    }

    public void onMyTripsButtonClicked() {
        Log.d(TAG, "onMyTripsButtonClicked()");
        if (getView() != null)
            getView().startMyTripsActivity();
    }

    public void onAddCarClicked() {
        Log.d(TAG, "onAddCarClicked()");
        getView().startAddCarActivity();
    }

    public void onPairScannerConfirmClicked(String s) {
        Log.d(TAG, "onPairScannerConfirmClicked()");
        String newScannerID = s.contains("b") ? s.replace("b", "B") : s;
        if (updating) return;
        updating = true;
        boolean carHasScanner = getCar().getScannerId() != null && !getCar().getScannerId().equalsIgnoreCase("");
        getView().showLoadingDialog("Updating Device ID");
        useCaseComponent.getAddScannerUseCase().execute(carHasScanner, getCar().getScannerId(), getCar().getId(), newScannerID, new AddScannerUseCase.Callback() {
            @Override
            public void onDeviceAlreadyActive() {
                updating = false;
                EventBus.getDefault().post(new CarDataChangedEvent(
                        new EventTypeImpl(EventType.EVENT_SCANNER), EVENT_SOURCE));
                if (getView() == null) return;
                getView().hideLoadingDialog();
                getView().showScannerAlreadyActiveDialog();
            }

            @Override
            public void onScannerCreated() {

                updating = false;
                if (getView() == null) return;
                getView().hideLoadingDialog();
                getCar().setScannerId(newScannerID);
                getView().showScannerID(newScannerID);
                getView().toast("Scanner ID successfully updated");
            }

            @Override
            public void onError(RequestError error) {
                updating = false;
                if (getView() == null) return;
                getView().hideLoadingDialog();
                getView().displayUnknownErrorDialog();
            }
        });

    }
    public void onUpdateScannerClicked(String s) {
        Log.d(TAG, "new new scanner id" + s);
        Log.d(TAG, "onUpdateScannerClicked");
        if (s.length() != 10) {
            getView().toast("Invalid device ID");
            return;
        }
        if (!(s.substring(0, 4).equalsIgnoreCase("215B"))) {
            getView().toast("Invalid device ID");
            return;
        }
        try {
            int k = Integer.parseInt(s.substring(4));
        } catch (NumberFormatException e) {
            e.printStackTrace();
            getView().toast("Invalid device ID");
            return;
        }
        getView().showConfirmUpdateScannerDialog(s);
    }
}