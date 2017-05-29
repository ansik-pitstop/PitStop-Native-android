package com.pitstop.repositories;

import com.pitstop.network.RequestCallback;

/**
 * Created by Karol Zdebel on 5/26/2017.
 */

public interface Repository<T> {
    boolean insert(T model, RequestCallback callback);

    boolean update(T model, RequestCallback callback);

    T get(int id, RequestCallback callback);

    boolean delete(T model, RequestCallback callback);
}
