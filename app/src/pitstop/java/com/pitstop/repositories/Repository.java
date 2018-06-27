package com.pitstop.repositories;

import com.pitstop.network.RequestError;

/**
 * Created by Karol Zdebel on 7/6/2017.
 */

public interface Repository {

    final int ERR_OFFLINE = 0;
    final int ERR_UNKNOWN = 1;

    enum DATABASE_TYPE{
        LOCAL, REMOTE, BOTH
    }

    public interface Callback<T>{
        void onSuccess(T data);
        void onError(RequestError error);
    }

}
