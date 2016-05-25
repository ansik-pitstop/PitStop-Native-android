package com.pitstop.DataAccessLayer.ServerAccess;

/**
 * Created by Paul Soladoye on 28/04/2016.
 */
public interface RequestCallback {
    void done(String response, RequestError requestError);
}