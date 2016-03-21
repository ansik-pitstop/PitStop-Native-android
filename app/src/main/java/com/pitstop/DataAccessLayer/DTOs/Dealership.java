package com.pitstop.DataAccessLayer.DTOs;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Paul Soladoye on 3/14/2016.
 */
public class Dealership {
    private long id;
    private String name;
    private String address;
    private String phone;
    private String email;
    private double latitude;
    private double longitude;

    public Dealership(){}

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }
}
