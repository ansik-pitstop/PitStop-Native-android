package com.pitstop.network;

import com.castel.obd.util.JsonUtil;

import java.io.IOException;
import java.net.SocketTimeoutException;

/**
 * Created by Paul Soladoye on 14/04/2016.
 */
public class RequestError {

    public final static String ERR_UNKNOWN = "unknown_error";
    public final static String ERR_OFFLINE = "offline_error";

    private String error = "";
    private String message = "";
    private int statusCode = -1;

    public RequestError(Throwable t){
        if (t instanceof SocketTimeoutException || t instanceof IOException){
            error = ERR_OFFLINE;
            message = "Please check your internet connection";
        }else{
            error = ERR_UNKNOWN;
            message = "Unknown error";
        }
    }

    public RequestError() { }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public RequestError setStatusCode(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public static RequestError jsonToRequestErrorObject(String json) {
        RequestError requestError = null;

        try {
            requestError = JsonUtil.json2object(json, RequestError.class);
        }catch (Exception e) {
            e.printStackTrace();
        }

        return requestError;
    }

    public static RequestError getUnknownError() {
        RequestError requestError = new RequestError();
        requestError.setError(ERR_UNKNOWN);
        requestError.setMessage("Unknown error");

        return requestError;
    }

    public static RequestError getOfflineError(){
        RequestError requestError = new RequestError();
        requestError.setError(ERR_OFFLINE);
        requestError.setMessage("Please check your internet connection");

        return requestError;
    }

    @Override
    public String toString(){
        try{
            return String.format("{ error: %s, message: %s, statusCode: %d }"
                    ,getError(),getMessage(),getStatusCode());
        }catch(NullPointerException){
            return "null";
        }

    }

}
