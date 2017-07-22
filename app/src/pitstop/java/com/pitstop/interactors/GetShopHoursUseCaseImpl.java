package com.pitstop.interactors;

import android.os.Handler;


import com.pitstop.models.Dealership;
import com.pitstop.models.User;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.ShopRepository;
import com.pitstop.repositories.UserRepository;
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
    private static final String API_KEY = "AIzaSyAjUxXRoOW21-c-LDudqgOZLvBQpiXp58k";

    private static final String PLACES_DETAILS_URL = "https://maps.googleapis.com/maps/api/place/details/json?";

    private final int DEFAULT_OPEN_HOUR = 900;
    private final int DEFAULT_CLOSE_HOUR = 1700;

    private ShopRepository shopRepository;
    private UserRepository userRepository;
    private NetworkHelper networkHelper;
    private Callback callback;
    private Handler handler;


    private int shopId;
    private int dayInWeek;
    private int day;
    private int month;
    private int year;

    public GetShopHoursUseCaseImpl(ShopRepository shopRepository, UserRepository userRepository, NetworkHelper networkHelper, Handler handler){
        this.shopRepository = shopRepository;
        this.userRepository = userRepository;
        this.networkHelper = networkHelper;
        this.handler = handler;
    }

    @Override
    public void run() {
        userRepository.getCurrentUser(new UserRepository.UserGetCallback() {
            @Override
            public void onGotUser(User user) {
                shopRepository.getShopsByUserId(user.getId(), new ShopRepository.ShopsGetCallback() {
                    @Override
                    public void onShopsGot(List<Dealership> dealerships) {
                       for(Dealership d:dealerships){
                           if(d.getId() == shopId){
                               getHours(d);
                               return;
                           }
                       }
                       shopRepository.getPitstopShops(new ShopRepository.GetPitstopShopsCallback() {
                           @Override
                           public void onShopsGot(List<Dealership> dealershipList) {
                               for(Dealership d: dealershipList){
                                   if(d.getId() == shopId ){
                                       getHours(d);
                                       return;
                                   }
                               }
                           }

                           @Override
                           public void onError() {
                                callback.onError();
                           }
                       });
                    }

                    @Override
                    public void onError() {
                         callback.onError();
                    }
                });
            }

            @Override
            public void onError() {
                callback.onError();
            }
        });
    }
    private void getHours(Dealership dealership){
        networkHelper.get("shop/" + dealership.getId() + "/calendar", new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                List<String> timesToRemove = new ArrayList<String>();
                if(requestError == null && response != null){
                    try{
                        JSONObject responsesJson = new JSONObject(response);
                        JSONObject dates = responsesJson.getJSONObject("dates");
                        timesToRemove.addAll(getRemoveTimes(dates.getJSONArray("requested"),year+"-"+fixMonth(month)+"-"+day));
                        timesToRemove.addAll(getRemoveTimes(dates.getJSONArray("tentative"),year+"-"+fixMonth(month)+"-"+day));
                        timesToRemove.addAll(getRemoveTimes(dates.getJSONArray("dealership"),year+"-"+fixMonth(month)+"-"+day));
                    }catch (JSONException e){

                    }
                }

                if(dealership.getHours() == null){
                    callback.onNoHoursAvailable(makeTimes(DEFAULT_OPEN_HOUR,DEFAULT_CLOSE_HOUR,timesToRemove));
                    return;
                }
                JSONArray hours = dealership.getHours();
                try{
                    JSONObject hour = hours.getJSONObject(dayInWeek);
                    String open =  hour.getString("open");
                    String close = hour.getString("close");
                    if(open.equals("") || close.equals("")){
                        callback.onNotOpen();
                        return;
                    }
                    callback.onHoursGot(makeTimes(Integer.parseInt(hour.getString("open")),Integer.parseInt(hour.getString("close")),timesToRemove));
                }catch (JSONException e){
                    callback.onError();
                }
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
        this.year = year;
        this.month = month;
        this.day = day;
        this.callback = callback;
        this.shopId = shopId;
        this.dayInWeek = Integer.parseInt(dayInWeek);
        if(this.dayInWeek == 7){
            this.dayInWeek = 0;
        }
        handler.post(this);
    }
}
