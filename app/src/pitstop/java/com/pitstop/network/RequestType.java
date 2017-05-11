package com.pitstop.network;

/**
 * Created by Paul Soladoye on 28/04/2016.
 */
public enum RequestType {
    GET("GET"), PUT("PUT"), POST("POST"), DELETE("DELETE");

    private String mType;

    RequestType(String type) {
        mType = type;
    }

    public String type() {
        return mType;
    }
}
