package com.pitstop.interactors.get;

import android.os.Handler;

import com.pitstop.models.Dealership;
import com.pitstop.models.DebugMessage;
import com.pitstop.models.User;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.ShopRepository;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.Logger;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * Created by Matthew on 2017-07-18.
 */

public class GetShopHoursUseCaseImpl implements GetShopHoursUseCase {

    private final String TAG = getClass().getSimpleName();

    private static final String API_KEY = "AIzaSyAjUxXRoOW21-c-LDudqgOZLvBQpiXp58k";
    private static final String PLACES_DETAILS_URL = "https://maps.googleapis.com/maps/api/place/details/json?";

    private final int DEFAULT_OPEN_HOUR = 900;
    private final int DEFAULT_CLOSE_HOUR = 1700;

    private ShopRepository shopRepository;
    private UserRepository userRepository;
    private NetworkHelper networkHelper;
    private Callback callback;
    private Handler useCaseHandler;
    private Handler mainHandler;

    private int shopId;
    private int dayInWeek;
    private int day;
    private int month;
    private int year;

    public GetShopHoursUseCaseImpl(ShopRepository shopRepository, UserRepository userRepository
            , NetworkHelper networkHelper, Handler useCaseHandler, Handler mainHandler){
        this.shopRepository = shopRepository;
        this.userRepository = userRepository;
        this.networkHelper = networkHelper;
        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
    }

    private void onHoursGot(List<String> hours){
        Logger.getInstance().logI(TAG, "Use case finished execution: hours="+hours
                , false, DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onHoursGot(hours));
    }

    private void onNoHoursAvailable(List<String> defaultHours){
        Logger.getInstance().logI(TAG, "Use case finished execution: no hours available, default="+defaultHours
                , false, DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onNoHoursAvailable(defaultHours));
    }

    private void onNotOpen(){
        Logger.getInstance().logI(TAG, "Use case finished execution: not open!"
                , false, DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onNotOpen());
    }

    private void onError(RequestError error){
        Logger.getInstance().logI(TAG, "Use case returned error: err="+error
                , false, DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onError(error));
    }

    @Override
    public void run() {
        userRepository.getCurrentUser(new UserRepository.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                shopRepository.get(shopId, new Repository.Callback<Dealership>() {
                    @Override
                    public void onSuccess(Dealership dealership) {
                        getHours(dealership);
                        return;
                    }

                    @Override
                    public void onError(RequestError error) {
                        GetShopHoursUseCaseImpl.this.onError(error);
                    }
                });
            }

            @Override
            public void onError(RequestError error) {
                GetShopHoursUseCaseImpl.this.onError(error);
            }
        });
    }
    private void getHours(Dealership dealership){
        networkHelper.get("shop/" + dealership.getId() + "/calendar", (response, requestError) -> {
            List<String> timesToRemove = new ArrayList<String>();
            if(requestError == null && response != null){
                try{
                    JSONObject responsesJson = new JSONObject(response);
                    JSONObject dates = responsesJson.getJSONObject("dates");
                    timesToRemove.addAll(getRemoveTimes(dates.getJSONArray("requested"),year+"-"+fixMonth(month)+"-"+day));
                    timesToRemove.addAll(getRemoveTimes(dates.getJSONArray("tentative"),year+"-"+fixMonth(month)+"-"+day));
                    timesToRemove.addAll(getRemoveTimes(dates.getJSONArray("dealership"),year+"-"+fixMonth(month)+"-"+day));
                }catch (JSONException e){
                    GetShopHoursUseCaseImpl.this.onError(RequestError.getUnknownError());
                }
            }

            if(dealership.getHours() == null){
                GetShopHoursUseCaseImpl.this.onNoHoursAvailable(
                        makeTimes(DEFAULT_OPEN_HOUR,DEFAULT_CLOSE_HOUR,timesToRemove));
                return;
            }
            JSONArray hours = dealership.getHours();
            try{
                JSONObject hour = hours.getJSONObject(dayInWeek);
                String open =  hour.getString("open");
                String close = hour.getString("close");
                if(open.equals("") || close.equals("")){
                    GetShopHoursUseCaseImpl.this.onNotOpen();
                    return;
                }
                GetShopHoursUseCaseImpl.this
                        .onHoursGot(makeTimes(Integer.parseInt(hour.getString("open"))
                                ,Integer.parseInt(hour.getString("close")),timesToRemove));
            }catch (JSONException e){
                GetShopHoursUseCaseImpl.this.onError(RequestError.getUnknownError());
            }
        });
    }

    private List<String> makeTimes(int start, int end,List<String> remove){
        List<String> times = new ArrayList<>();
        for(int i = start;i<end;i+=100){
            String t1 = Integer.toString(i);
            String t2 = Integer.toString(i+30);
            if(t1.length()<4){
                t1 = "0"+t1;
            }
            if(t2.length()<4){
                t2 = "0"+t2;
            }
            if(!remove.contains(t1)){
                times.add(timeFormat(t1));
            }
            if(!remove.contains(t2)){
                times.add(timeFormat(t2));
            }
        }
        return times;
    }
    public String fixMonth(int month){
        if(month<10){
            return  "0"+month;
        }else{
            return  ""+month;
        }
    }

    public List<String> getRemoveTimes(JSONArray dates,String date){
        List<String> datesToRemove = new ArrayList<>();
        for(int i = 0 ;i < dates.length();i++){
            try{
                String currentDate = dates.getString(i);
                if(currentDate.contains(date)){
                    datesToRemove.add(currentDate.substring(11,13)+currentDate.substring(14,16));
                }
            }catch (JSONException e){
                GetShopHoursUseCaseImpl.this.onError(RequestError.getUnknownError());
            }
        }
        return datesToRemove;
    }

    private String timeFormat(String time){
        String date = time;
        SimpleDateFormat in = new SimpleDateFormat("HHmm");
        SimpleDateFormat out = new SimpleDateFormat("hh:mm aa");
        try{
            Date inDate = in.parse(date);
            String outDate = out.format(inDate);
            return outDate;
        }catch (ParseException e){
            return date;
        }
    }

    @Override
    public void execute(int year, int month, int day, int shopId, String dayInWeek, Callback callback) {
        Logger.getInstance().logI(TAG, "Use case started execution: y="+year+", m="
                        +month+",day="+day+",shopId="+shopId+", dayInWeek="+dayInWeek
                , false, DebugMessage.TYPE_USE_CASE);
        this.year = year;
        this.month = month;
        this.day = day;
        this.callback = callback;
        this.shopId = shopId;

        switch (dayInWeek){
            case("Mon"):
                this.dayInWeek = 1;
                break;
            case("Tue"):
                this.dayInWeek = 2;
                break;
            case("Wed"):
                this.dayInWeek = 3;
                break;
            case("Thu"):
                this.dayInWeek = 4;
                break;
            case("Fri"):
                this.dayInWeek = 5;
                break;
            case("Sat"):
                this.dayInWeek = 6;
                break;
            case("Sun"):
                this.dayInWeek = 0;
                break;
        }
        useCaseHandler.post(this);
    }
}
