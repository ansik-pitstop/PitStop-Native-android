package com.pitstop.interactors.emissions;

import android.os.CountDownTimer;
import android.os.Handler;
import android.os.SystemClock;

import com.pitstop.interactors.MacroUseCases.EmissionsMacroUseCase;
import com.pitstop.models.Pid;
import com.pitstop.models.Trip215;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.Device215TripRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.utils.DateTimeFormatUtil;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.jar.JarInputStream;

/**
 * Created by Matt on 2017-08-28.
 */

public class Post2141UseCaseImpl implements Post2141UseCase {

    private Callback callback;
    private Handler handler;
    private NetworkHelper networkHelper;
    private Device215TripRepository device215TripRepository;
    private String pid;
    private String deviceId;
    private long rtcTime;

    public Post2141UseCaseImpl(Device215TripRepository device215TripRepository, NetworkHelper networkHelper, Handler handler) {
        this.handler = handler;
        this.networkHelper = networkHelper;
        this.device215TripRepository = device215TripRepository;
    }

    @Override
    public void execute(String pid,String deviceId, Callback callback) {
        this.callback = callback;
        this.pid = pid;
        this.deviceId = deviceId;
        handler.post(this);
    }

    @Override
    public void run() {

        JSONObject body = new JSONObject();
        try{
            body.put("data",pid);
        }catch (JSONException e){
            e.printStackTrace();
        }
        networkHelper.post("demo/emissions/pid", new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {


                if(response != null && requestError == null){
                    System.out.println("Testing response " + response);
                    try{
                        JSONObject responseJson = new JSONObject(response);
                        callback.onPIDPosted(responseJson);// I know this is bad, but its temporary
                    }catch (JSONException e){
                        callback.onError(RequestError.getUnknownError());
                    }

                }else if(response == null && requestError != null){
                    System.out.println("Testing error "+requestError.getMessage());
                    callback.onError(requestError);
                }
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
                    rtcTime = System.currentTimeMillis()/1000;
                    pidObject.put("rtcTime", rtcTime);
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
                            System.out.println("Testing get "+"scan/pids?from=" + DateTimeFormatUtil.rtcToIso(rtcTime*1000-200000) + "&to="+DateTimeFormatUtil.rtcToIso(rtcTime*1000+200000)+"&limit=10");
                            networkHelper.get("scan/pids?from=" + DateTimeFormatUtil.rtcToIso(rtcTime-10000) + "&to="+DateTimeFormatUtil.rtcToIso(rtcTime+10000)+"&limit=10", new RequestCallback() {
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
