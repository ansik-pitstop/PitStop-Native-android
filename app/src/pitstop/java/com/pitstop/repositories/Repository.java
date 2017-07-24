package com.pitstop.repositories;

/**
 * Created by Karol Zdebel on 7/6/2017.
 */

public interface Repository {

    final int ERR_OFFLINE = 0;
    final int ERR_UNKNOWN = 1;

    interface Callback<T>{
        void onSuccess(T data);
        void onError(int error);
    }

}
