package com.pitstop.DataAccessLayer.DTOs;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.parse.ParseObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Paul Soladoye on 3/14/2016.
 */
public class Dealership implements Serializable {

    @Expose(serialize = false, deserialize = false)
    private int id;
    private long dealershipId;
    private String parseId;
    private String name;
    private String address;
    private String phone;
    private String email;
    private double latitude;
    private double longitude;

    public Dealership(){}

    public int getId() {
        return id;
    }

    public void setId(int id) { this.id = id; }

    public long getDealershipId() {
        return dealershipId;
    }

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

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getParseId() {
        return parseId;
    }

    public void setParseId(String parseId) {
        this.parseId = parseId;
    }

    public static Dealership createDealership(ParseObject parseObject, String carId) {
        Dealership dealership = new Dealership();
        dealership.setParseId(parseObject.getObjectId());
        dealership.setName(parseObject.getString("name"));
        dealership.setPhoneNumber(parseObject.getString("phoneNumber"));
        dealership.setEmail(parseObject.getString("email"));
        dealership.setAddress(parseObject.getString("addressText"));
        return dealership;
    }

    public static List<Dealership> createDealershipList(List<ParseObject> parseObjects) {
        List<Dealership> dealerships = new ArrayList<>();
        for( ParseObject parseObject : parseObjects) {
            dealerships.add(createDealership(parseObject,""));
        }
        return dealerships;
    }
}