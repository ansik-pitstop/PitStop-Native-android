package com.pitstop.network;

import android.content.res.Resources;

import com.castel.obd.util.JsonUtil;
import com.pitstop.R;

/**
 * Created by Paul Soladoye on 14/04/2016.
 */
public class RequestError {

    public final static String ERR_UNKNOWN = "unknown_error";
    public final static String ERR_OFFLINE = "offline_error";

    private String error;
    private String message;
    private int statusCode;

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
        requestError.setMessage(Resources.getSystem().getString(R.string.internet_check_error));

        return requestError;
    }

    public static RequestError getOfflineError(){
        RequestError requestError = new RequestError();
        requestError.setError(ERR_OFFLINE);
        requestError.setMessage(Resources.getSystem().getString(R.string.internet_check_error));

        return requestError;
    }

}
