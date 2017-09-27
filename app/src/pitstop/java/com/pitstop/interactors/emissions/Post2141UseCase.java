package com.pitstop.interactors.emissions;

import com.pitstop.database.TABLES;
import com.pitstop.interactors.Interactor;
import com.pitstop.models.Pid;
import com.pitstop.network.RequestError;

import org.json.JSONObject;

/**
 * Created by Matt on 2017-08-28.
 */

public interface Post2141UseCase extends Interactor {
    interface Callback{
        void onPIDPosted(JSONObject response);
        void onError(RequestError error);
    }

    //Executes usecase
    void execute(String pid,String deviceId, Callback callback);
}
