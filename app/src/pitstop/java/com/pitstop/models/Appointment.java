package com.pitstop.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


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
    @SerializedName("state") //tentative or requested
    @Expose
    private String state;
    @SerializedName("shopId")
    @Expose
    private int shopId;
    @SerializedName("id")
    @Expose
    private int id;

    public Appointment(){

    }

    public Appointment(int shopId, String state, String date, String comments){
        this.shopId = shopId;
        this.state = state;
        this.date = date;
        this.comments = comments;
    }

    public String getComments(){ return comments;}

    public String getDate() { return date;}

    public String getState() { return state;}

    public int getShopId() { return shopId;}

    public int getId() { return id;}

    public void setComments(String comments) { this.comments = comments;}

    public void setState(String state) {this.state = state;}

    public void setDate(String date) {
        this.date = date;
    }

    public void setShopId(int shopId) { this.shopId = shopId;}

    public void setId(int id){ this.id = id;}

    @Override
    public String toString(){
        try{
            return String.format("comments: %s, date: %s, state: %s, shopId: %d, id: %d"
                    , comments, date, state, shopId, id);
        }catch(NullPointerException e){
            return "null";
        }
    }

    @Override
    public boolean equals(Object o){
        if (o instanceof Appointment){
            Appointment otherApp = (Appointment)o;
            return otherApp.getComments().equals(comments)
                    && otherApp.getDate().equals(date)
                    && otherApp.getState().equals(state)
                    && otherApp.getShopId() == shopId;
        }else{
            return false;
        }
    }

}
