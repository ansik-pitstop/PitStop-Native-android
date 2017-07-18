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
        System.out.println("Testing does the dealership have hours "+dealership.getHours());
        networkHelper.get("shop/" + dealership.getId() + "/calendar/?from="+year+"-"+month+"-"+day+"&to="+year+"-"+month+"-"+(day+1), new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                System.out.println("Testing response "+response);
            }
        });
        if(dealership.getHours() == null){
            callback.onNoHoursAvailable(makeTimes(DEFAULT_OPEN_HOUR,DEFAULT_CLOSE_HOUR));
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
            callback.onHoursGot(makeTimes(Integer.parseInt(hour.getString("open")),Integer.parseInt(hour.getString("close"))));
        }catch (JSONException e){
         callback.onError();
        }
    }

    private List<String> makeTimes(int start, int end){
        List<String> times = new ArrayList<>();
        for(int i = start;i<end;i+=100){
            times.add(timeFormat(i));
            times.add(timeFormat(i+30));
        }
        return times;
    }
    private String timeFormat(int time){
        String date = Integer.toString(time);
        if(date.length()<4){
            date = "0"+date;
        }
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
