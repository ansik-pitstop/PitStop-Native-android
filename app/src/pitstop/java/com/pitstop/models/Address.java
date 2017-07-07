package com.pitstop.models;

/**
 * Created by Matthew on 2017-07-05.
 */

public class Address {
    private String street;
    private String city;
    private String province;
    private String country;
    private String postal;
    public Address(String address){
        String[] comaBreak = address.split(",");
        if(comaBreak.length>0){
            street = comaBreak[0];
        }
        if(comaBreak.length>1){
            city = comaBreak[1];
        }
        if(comaBreak.length>2){
            province = comaBreak[2];
        }
        if(comaBreak.length>3){
            postal = comaBreak[3];
        }
        if(comaBreak.length>4){
            country = comaBreak[4];
        }
    }

    public String getStreet() {
        return street;
    }

    public String getCity() {
        return city;
    }

    public String getCountry() {
        return country;
    }

    public String getPostal() {
        return postal;
    }

    public String getProvince() {
        return province;
    }
}
