package com.pitstop.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Matthew on 2017-05-03.
 */

public class Appointment {
    @SerializedName("comments")
    @Expose
    private String comments;
    @SerializedName("date")
    @Expose
    private String date;
    @SerializedName("state")
    @Expose
    private String state;
    @SerializedName("shopId")
    @Expose
    private int shopId;
    @SerializedName("id")
    @Expose
    private int id;

    public String getComments(){ return comments;}

    public String getDate() { return date;}

    public String getState() { return state;}

    public int getShopId() { return shopId;}

    public int getId() { return id;}

    public void setComments(String comments) { this.comments = comments;}

    public void setState(String state) {this.state = state;}

    public void setDate(String date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.CANADA);
        SimpleDateFormat newFormat = new SimpleDateFormat("EEE dd MMM yyyy - hh:mm aa");
        try {
            Date formDate = sdf.parse(date);
            String newDate = newFormat.format(formDate);
            this.date = newDate;
        }catch (ParseException e){
            e.printStackTrace();
        }

    }


    public void setShopId(int shopId) { this.shopId = shopId;}

    public void setId(int id){ this.id = id;}

}
