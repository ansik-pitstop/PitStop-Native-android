package com.pitstop.interactors.emissions;

import android.os.Handler;

import com.pitstop.network.RequestError;
import com.pitstop.repositories.Device215TripRepository;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Matt on 2017-08-28.
 */

public class Post2141UseCaseImpl implements Post2141UseCase {

    private Callback callback;
    private Handler useCaseHandler;
    private Handler mainHandler;
    private NetworkHelper networkHelper;
    private Device215TripRepository device215TripRepository;
    private String pid;
    private String deviceId;
    private long rtcTime;

    public Post2141UseCaseImpl(Device215TripRepository device215TripRepository
            , NetworkHelper networkHelper, Handler useCaseHandler, Handler mainHandler) {
        this.useCaseHandler = useCaseHandler;
        this.networkHelper = networkHelper;
        this.device215TripRepository = device215TripRepository;
        this.mainHandler = mainHandler;
    }

    private void onPIDPosted(JSONObject response){
        mainHandler.post(() -> callback.onPIDPosted(response));
    }

    private void onError(RequestError error){
        mainHandler.post( () -> callback.onError(error));
    }

    @Override
    public void execute(String pid,String deviceId, Callback callback) {
        this.callback = callback;
        this.pid = pid;
        this.deviceId = deviceId;
        useCaseHandler.post(this);
    }

    @Override
    public void run() {

        JSONObject body = new JSONObject();
        try{
            body.put("data",pid);
        }catch (JSONException e){
            e.printStackTrace();
        }
        networkHelper.post("demo/emissions/pid", (response, requestError) -> {

            if(response != null && requestError == null){
                System.out.println("Testing response " + response);
                try{
                    JSONObject responseJson = new JSONObject(response);
                    Post2141UseCaseImpl.this.onPIDPosted(responseJson);// I know this is bad, but its temporary
                }catch (JSONException e){
                    Post2141UseCaseImpl.this.onError(RequestError.getUnknownError());
                }

            }else if(response == null && requestError != null){
                System.out.println("Testing error "+requestError.getMessage());
                Post2141UseCaseImpl.this.onError(requestError);
            }
        },body);



        /*device215TripRepository.retrieveLatestTrip(deviceId, new Repository.Callback<Trip215>() {
            @Override
            public void onSuccess(Trip215 data) {
                JSONObject body = new JSONObject();
                try{
                    body.put("tripId",data.getTripId());
                    body.put("scannerId",deviceId);
                    JSONArray pidArray = new JSONArray();
                    JSONObject pidObject = new JSONObject();
                    JSONArray pidList = new JSONArray();
                    JSONObject pidSingle = new JSONObject();
                    pidSingle.put("id","2141");
                    pidSingle.put("data",pid);
                    pidList.put(pidSingle);
                    pidObject.put("pids",pidList);
                    bluetoothDeviceTime = System.currentTimeMillis()/1000;
                    pidObject.put("bluetoothDeviceTime", bluetoothDeviceTime);
                    pidObject.put("calculatedMileage",6);//fix this later
                    pidObject.put("tripMileage",data.getMileage());
                    pidArray.put(pidObject);
                    body.put("pidArray",pidArray);

                }catch (JSONException e){
                    e.printStackTrace();
                }
                System.out.println("Testing "+body);
                networkHelper.post("scan/pids", new RequestCallback() {
                    @Override
                    public void done(String response, RequestError requestError) {
                        System.out.println("Testing "+response);
                        if(response != null && requestError == null){
                            System.out.println("Testing get "+"scan/pids?from=" + DateTimeFormatUtil.rtcToIso(bluetoothDeviceTime*1000-200000) + "&to="+DateTimeFormatUtil.rtcToIso(bluetoothDeviceTime*1000+200000)+"&limit=10");
                            networkHelper.get("scan/pids?from=" + DateTimeFormatUtil.rtcToIso(bluetoothDeviceTime-10000) + "&to="+DateTimeFormatUtil.rtcToIso(bluetoothDeviceTime+10000)+"&limit=10", new RequestCallback() {
                                @Override
                                public void done(String response, RequestError requestError) {
                                    System.out.println("Testing response from get "+ response);
                                    if(requestError == null && requestError == null){
                                        callback.onPIDPosted();
                                    }else{
                                        callback.onError(requestError);
                                    }
                                }
                            });
                        }else if(requestError != null && response == null){
                            callback.onError(requestError);
                        }
                    }
                },body);
            }

            @Override
            public void onError(RequestError error) {
                callback.onError(error);
            }
        });*/
    }
}
