package com.pitstop.repositories;

/**
 * Created by Karol Zdebel on 7/6/2017.
 */

public interface Repository {

    interface Callback<T>{
        void onSuccess(T data);
        void onError(int error);
        void onError(String error);
    }

}
