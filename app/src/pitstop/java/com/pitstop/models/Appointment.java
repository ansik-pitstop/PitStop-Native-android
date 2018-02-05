package com.pitstop.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Date;


/**
 * Created by Matthew on 2017-05-03.
 */

public class Appointment {
    @SerializedName("comments")
    @Expose
    private String comments;
    @SerializedName("appointmentDate")
    @Expose
    private Date date;
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

    public Appointment(int shopId, String state, Date date, String comments){
        this.shopId = shopId;
        this.state = state;
        this.date = date;
        this.comments = comments;
    }

    public String getComments(){ return comments;}

    public Date getDate() { return date;}

    public String getState() { return state;}

    public int getShopId() { return shopId;}

    public int getId() { return id;}

    public void setComments(String comments) { this.comments = comments;}

    public void setState(String state) {this.state = state;}

    public void setDate(Date date) {
        this.date = date;
    }

    public void setShopId(int shopId) { this.shopId = shopId;}

    public void setId(int id){ this.id = id;}

    @Override
    public String toString(){
        try{
            return String.format("{ comments: %s, date: %s, state: %s, shopId: %d, id: %d }"
                    , comments, date, state, shopId, id);
        }catch(NullPointerException e){
            return "null";
        }
    }

    @Override
    public boolean equals(Object o){
        if (o instanceof Appointment){
            Appointment otherApp = (Appointment)o;
            String otherAppComments = "";
            String thisComments = "";
            if (otherApp.comments != null) otherAppComments = otherApp.comments;
            if (comments != null) thisComments = comments;
            return otherAppComments.equalsIgnoreCase(thisComments)
                    && otherApp.getDate().toString().equals(date.toString())
                    && otherApp.getState().equalsIgnoreCase(state)
                    && otherApp.getShopId() == shopId;
        }else{
            return false;
        }
    }

}
