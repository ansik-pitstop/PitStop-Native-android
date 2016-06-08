package com.pitstop.DataAccessLayer.DTOs;

import android.widget.Toast;

import com.castel.obd.util.JsonUtil;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.parse.ParseObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Paul Soladoye on 3/14/2016.
 */
public class Dealership implements Serializable {

    private int id;
    private String name;
    private String address;
    private String phone;
    private String email;
    private String longitude;
    private String latitude;


    public Dealership(){}

    public int getId() {
        return id;
    }

    public void setId(int id) { this.id = id; }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phone = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public static List<Dealership> createDealershipList(String shopsListJson) throws JSONException {
        ArrayList<Dealership> dealerships = new ArrayList<>();

        JSONArray shopArr = new JSONArray(shopsListJson);

        for (int i = 0; i < shopArr.length() ; i++) {
            dealerships.add(jsonToDealershipObject(shopArr.getJSONObject(i).toString()));
        }
        return dealerships;
    }

    public static Dealership jsonToDealershipObject(String json) {
        Dealership dealership = null;
        try {
            dealership = JsonUtil.json2object(json, Dealership.class);

            if(dealership.getId() == 0) {
                dealership = new Dealership();
                JSONObject dealershipJson = new JSONObject(json).getJSONObject("dealership");
                dealership.setId(dealershipJson.getInt("id"));
                dealership.setName(dealershipJson.getString("name"));
                dealership.setAddress(dealershipJson.getString("address"));
                dealership.setEmail(dealershipJson.getString("email"));
                dealership.setPhoneNumber(dealershipJson.getString("phone"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return dealership;
    }
}