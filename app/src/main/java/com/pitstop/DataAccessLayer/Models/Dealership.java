package com.pitstop.DataAccessLayer.Models;

import com.google.gson.annotations.SerializedName;

/**
 * Created by psola on 3/14/2016.
 */
public class Dealership {
    private long id;
    private String name;
    private String address;
    private String phone;
    private String email;

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
