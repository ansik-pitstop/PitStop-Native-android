package com.pitstop.repositories;

/**
 * Created by Karol Zdebel on 7/6/2017.
 */

public interface Repository {

    public static final String ERR_NETWORK = "error_network";

    interface Callback<T>{
        void onSuccess(T data);
        void onError(int error);
    }

}
