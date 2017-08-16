package com.pitstop.bluetooth.handler;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import com.pitstop.EventBus.CarDataChangedEvent;
import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.EventBus.EventType;
import com.pitstop.EventBus.EventTypeImpl;
import com.pitstop.bluetooth.dataPackages.TripInfoPackage;
import com.pitstop.database.LocalCarAdapter;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerTempNetworkComponent;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.TempNetworkComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.other.Trip215EndUseCase;
import com.pitstop.interactors.other.Trip215StartUseCase;
import com.pitstop.models.Car;
import com.pitstop.models.DebugMessage;
import com.pitstop.models.TripEnd;
import com.pitstop.models.TripIndicator;
import com.pitstop.models.TripStart;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.utils.LogUtils;
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.utils.NetworkHelper;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static com.facebook.FacebookSdk.getApplicationContext;

/**
 * Created by Karol Zdebel on 8/15/2017.
 */

public class TripDataHandler implements BluetoothDataHandler{

    private final String TAG = getClass().getSimpleName();
    private final String pfTripId = "lastTripId";

    private static final int TRIP_END_DELAY = 5000;
    private static final EventSource EVENT_SOURCE
            = new EventSourceImpl(EventSource.SOURCE_BLUETOOTH_AUTO_CONNECT);

    private UseCaseComponent useCaseComponent;
    private BluetoothDataHandlerManager bluetoothDataHandlerManager;
    private NetworkHelper networkHelper;
    private Handler handler;
    private SharedPreferences sharedPreferences;
    private LocalCarAdapter localCarStorage;
    private Context context;

    final private LinkedList<TripIndicator> tripRequestQueue = new LinkedList<>();
    private List<TripInfoPackage> pendingTripInfoPackages = new ArrayList<>();
    private int lastTripId;
    private boolean isSendingTripRequest;
    private String currentDeviceId = "";

    public TripDataHandler(BluetoothDataHandlerManager bluetoothDataHandlerManager, Context context){

        this.bluetoothDataHandlerManager = bluetoothDataHandlerManager;
        this.handler = new Handler();
        this.useCaseComponent = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(context))
                .build();

        TempNetworkComponent tempNetworkComponent = DaggerTempNetworkComponent.builder()
                .contextModule(new ContextModule(context))
                .build();
        networkHelper = tempNetworkComponent.networkHelper();
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.localCarStorage = new LocalCarAdapter(context);
        this.context = context;
    }

    public void clearPendingData(){
        pendingTripInfoPackages.clear();
    }

    public void handleTripData(TripInfoPackage tripInfoPackage, boolean connectedTo215
            , long terminalRTCTime){

        boolean deviceIsVerified
                = bluetoothDataHandlerManager.isDeviceVerified();

        //Not handling trip updates anymore since live mileage has been removed
        if (tripInfoPackage.flag.equals(TripInfoPackage.TripFlag.UPDATE)){
            LogUtils.debugLogD(TAG, "trip update received. "
                    , true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
        }
        else if (tripInfoPackage.flag.equals(TripInfoPackage.TripFlag.END)){
            LogUtils.debugLogD(TAG, "Trip end received: " + tripInfoPackage.toString()
                    , true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
            bluetoothDataHandlerManager.trackBluetoothEvent(MixpanelHelper.BT_TRIP_END_RECEIVED);
        }
        else if (tripInfoPackage.flag.equals(TripInfoPackage.TripFlag.START)){
            LogUtils.debugLogD(TAG, "Trip start received: " + tripInfoPackage.toString()
                    , true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
            bluetoothDataHandlerManager.trackBluetoothEvent(MixpanelHelper.BT_TRIP_START_RECEIVED);
        }

        //Set TripInfo deviceId if its not set, but we have it someplace else
        if ((tripInfoPackage.deviceId == null || tripInfoPackage.deviceId.isEmpty())
                && deviceIsVerified
                && !bluetoothDataHandlerManager.getDeviceId().isEmpty()){

            tripInfoPackage.deviceId = bluetoothDataHandlerManager.getDeviceId();

        }

        /*Code for handling 212 trip logic, moved to private method since its being
          phased out and won't be maintained*/
        if (!tripInfoPackage.flag.equals(TripInfoPackage.TripFlag.UPDATE) && connectedTo215
                && deviceIsVerified){

            LogUtils.debugLogD(TAG, "handling 212 trip rtcTime:"+tripInfoPackage.rtcTime, true, DebugMessage.TYPE_BLUETOOTH
                    , getApplicationContext());
            handle212Trip(tripInfoPackage);
            return;
        }

        boolean deviceIdMissing = (tripInfoPackage.deviceId == null
                || tripInfoPackage.deviceId.isEmpty());

        //Check to see if we received current RTC time from device upon the app detecting device
        //If not received yet store the trip for once it is received
        if (!tripInfoPackage.flag.equals(TripInfoPackage.TripFlag.UPDATE)){
            Log.d(TAG,"Adding pending trip.");
            pendingTripInfoPackages.add(tripInfoPackage);
        }
        if (terminalRTCTime == -1 || !deviceIsVerified || deviceIdMissing){

            LogUtils.debugLogD(TAG, "Cannot process pending trips yet, terminalRtcSet?"
                            +(terminalRTCTime != -1)+", deviceVerified?"+deviceIsVerified
                            +", deviceIdMissing?"+deviceIdMissing
                    , true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());

            //Only send mixpanel event for non-update trip events
            if (!tripInfoPackage.flag.equals(TripInfoPackage.TripFlag.UPDATE)){
                bluetoothDataHandlerManager.trackBluetoothEvent(MixpanelHelper.BT_TRIP_NOT_PROCESSED);
            }
            return;
        }

        //Go through all pending trip info packages including the one just passed in parameter
        List<TripInfoPackage> toRemove = new ArrayList<>();
        for (TripInfoPackage trip: pendingTripInfoPackages){

            /*Set the device id for any trips that were received while a device was broken
            /** prior to an overwrite*/
            boolean tripHasNoId = trip.deviceId == null || trip.deviceId.isEmpty();
            if (tripHasNoId){
                //TripInfoPackage must have trip id or it would have returned due to deviceIdMissing flag
                trip.deviceId = tripInfoPackage.deviceId;
            }

            if (trip.flag.equals(TripInfoPackage.TripFlag.END) && connectedTo215){

                LogUtils.debugLogD(TAG, "Executing trip end use case", true
                        , DebugMessage.TYPE_BLUETOOTH, getApplicationContext());

                useCaseComponent.trip215EndUseCase().execute(trip, terminalRTCTime
                        , new Trip215EndUseCase.Callback() {
                            @Override
                            public void onHistoricalTripEndSuccess() {
                                bluetoothDataHandlerManager.trackBluetoothEvent(MixpanelHelper.BT_TRIP_END_HT_SUCCESS);
                                LogUtils.debugLogD(TAG, "Historical trip END saved successfully", true
                                        , DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
                            }

                            @Override
                            public void onRealTimeTripEndSuccess() {
                                bluetoothDataHandlerManager.trackBluetoothEvent(MixpanelHelper.BT_TRIP_END_RT_SUCCESS);

                                LogUtils.debugLogD(TAG, "Real-time END trip end saved successfully", true
                                        , DebugMessage.TYPE_BLUETOOTH, getApplicationContext());

                                //Send update mileage notification after 5 seconds to allow back-end to process mileage
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        notifyEventBus(new EventTypeImpl(EventType.EVENT_MILEAGE));
                                    }
                                },TRIP_END_DELAY);

                            }

                            @Override
                            public void onStartTripNotFound() {
                                bluetoothDataHandlerManager.trackBluetoothEvent(MixpanelHelper.BT_TRIP_END_FAILED);
                                LogUtils.debugLogD(TAG, "Trip start not found, mileage will update on "
                                                +"next trip start", true
                                        , DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
                            }

                            @Override
                            public void onError(RequestError error) {
                                bluetoothDataHandlerManager.trackBluetoothEvent(MixpanelHelper.BT_TRIP_END_FAILED);
                                LogUtils.debugLogD(TAG,"TRIP END Use case returned error", true
                                        , DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
                            }
                        });

            }
            else if (trip.flag.equals(TripInfoPackage.TripFlag.START) && connectedTo215){

                LogUtils.debugLogD(TAG, "Executing trip start use case", true
                        , DebugMessage.TYPE_BLUETOOTH, getApplicationContext());

                useCaseComponent.trip215StartUseCase().execute(trip, terminalRTCTime
                        , new Trip215StartUseCase.Callback() {
                            @Override
                            public void onRealTimeTripStartSuccess() {
                                bluetoothDataHandlerManager.trackBluetoothEvent(MixpanelHelper.BT_TRIP_START_RT_SUCCESS);
                                LogUtils.debugLogD(TAG, "Real-time trip START saved successfully", true
                                        , DebugMessage.TYPE_BLUETOOTH, getApplicationContext());

                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        notifyEventBus(new EventTypeImpl(EventType.EVENT_MILEAGE));
                                    }
                                },TRIP_END_DELAY);
                            }

                            @Override
                            public void onHistoricalTripStartSuccess(){
                                bluetoothDataHandlerManager.trackBluetoothEvent(MixpanelHelper.BT_TRIP_START_HT_SUCCESS);
                                LogUtils.debugLogD(TAG, "Historical trip START saved successfully", true
                                        , DebugMessage.TYPE_BLUETOOTH, getApplicationContext());

                            }

                            @Override
                            public void onError(RequestError error) {
                                bluetoothDataHandlerManager.trackBluetoothEvent(MixpanelHelper.BT_TRIP_START_FAILED);
                                LogUtils.debugLogD(TAG,"Error saving trip start", true
                                        , DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
                            }
                        });

            }
            toRemove.add(trip);

        }
        pendingTripInfoPackages.removeAll(toRemove);

        LogUtils.debugLogD(TAG, "rtcTime: "+tripInfoPackage.rtcTime
                        +" Completed running all use cases on all pending trips"
                        +" pendingTripList.size() after removing:"
                        +pendingTripInfoPackages.size(), true
                , DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
    }

    private void notifyEventBus(EventType eventType){
        CarDataChangedEvent carDataChangedEvent
                = new CarDataChangedEvent(eventType,EVENT_SOURCE);
        EventBus.getDefault().post(carDataChangedEvent);
    }

    //212 trip logic, no longer maintained
    private void handle212Trip(TripInfoPackage tripInfoPackage){

        if(tripInfoPackage.tripId != 0) {
            lastTripId = tripInfoPackage.tripId;
            sharedPreferences.edit().putInt(pfTripId, lastTripId).apply();

            if(tripInfoPackage.flag == TripInfoPackage.TripFlag.START) {
                LogUtils.debugLogI(TAG, "Trip start flag received", true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
            } else if(tripInfoPackage.flag == TripInfoPackage.TripFlag.END) {
                LogUtils.debugLogI(TAG, "Trip end flag received", true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
                if(lastTripId == -1) {
                    networkHelper.getLatestTrip(tripInfoPackage.deviceId, new RequestCallback() {
                        @Override
                        public void done(String response, RequestError requestError) {
                            if(requestError == null && !response.equals("{}")) {
                                try {
                                    lastTripId = new JSONObject(response).getInt("id");
                                    sharedPreferences.edit().putInt(pfTripId, lastTripId).apply();
                                    tripRequestQueue.add(new TripEnd(lastTripId, String.valueOf(tripInfoPackage.rtcTime),
                                            String.valueOf(tripInfoPackage.mileage)));
                                    executeTripRequests();
                                } catch(JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                } else {
                    tripRequestQueue.add(new TripEnd(lastTripId, String.valueOf(tripInfoPackage.rtcTime),
                            String.valueOf(tripInfoPackage.mileage)));
                    executeTripRequests();
                }
                Car car = localCarStorage.getCarByScanner(tripInfoPackage.deviceId);
                if(car != null) {
                    double newMileage = car.getTotalMileage() + tripInfoPackage.mileage;
                    car.setTotalMileage(newMileage);
                    localCarStorage.updateCar(car);
                }
            }
        }
    }

    public void executeTripRequests() {
        if (!isSendingTripRequest && !tripRequestQueue.isEmpty() && networkHelper.isConnected()) {
            Log.i(TAG, "Executing trip request");
            isSendingTripRequest = true;
            final TripIndicator nextAction = tripRequestQueue.peekFirst();
            RequestCallback callback = null;
            if (nextAction instanceof TripStart) {
                if (((TripStart) nextAction).getScannerId() == null) {
                    tripRequestQueue.pop();
                }
                if (localCarStorage.getCarByScanner(((TripStart) nextAction).getScannerId()) == null) {
                    isSendingTripRequest = false;
                    return;
                }
                callback = new RequestCallback() {
                    @Override
                    public void done(String response, RequestError requestError) {
                        if (requestError == null) {
                            try {
                                lastTripId = new JSONObject(response).getInt("id");
                                sharedPreferences.edit().putInt(pfTripId, lastTripId).apply();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            tripRequestQueue.pop();
                            isSendingTripRequest = false;
                            executeTripRequests();
                        } else {
                            networkHelper.getLatestTrip(((TripStart) nextAction).getScannerId(), new RequestCallback() {
                                @Override
                                public void done(String response, RequestError requestError) {
                                    if (requestError == null) {
                                        try {
                                            lastTripId = new JSONObject(response).getInt("id");
                                            sharedPreferences.edit().putInt(pfTripId, lastTripId).apply();
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                        tripRequestQueue.pop();
                                    } else if (requestError.getMessage().contains("no car")) {
                                        tripRequestQueue.pop();
                                    }
                                    isSendingTripRequest = false;
                                    executeTripRequests();
                                }
                            });
                        }
                    }
                };
            } else if (nextAction instanceof TripEnd) {
                callback = new RequestCallback() {
                    @Override
                    public void done(String response, RequestError requestError) {
                        if (requestError == null) {
                            Log.i(TAG, "trip data sent: " + ((TripEnd) nextAction).getMileage());
                        }
                        tripRequestQueue.pop();
                        isSendingTripRequest = false;
                        lastTripId = -1;
                        sharedPreferences.edit().putInt(pfTripId, lastTripId).apply();
                        executeTripRequests();
                    }
                };
            }
            nextAction.execute(context, callback);
        }
    }

    @Override
    public void setDeviceId(String deviceId) {
        this.currentDeviceId = deviceId;
    }
}
