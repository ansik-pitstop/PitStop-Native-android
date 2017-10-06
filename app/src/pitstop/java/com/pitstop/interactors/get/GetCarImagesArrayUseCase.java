package com.pitstop.interactors.get;

import com.pitstop.interactors.Interactor;
import com.pitstop.network.RequestError;

/**
 * Created by ishan on 2017-09-28.
 */

public interface GetCarImagesArrayUseCase extends Interactor {
    interface Callback{
        void onArrayGot(String imageLink);
        void onError(RequestError error);
    }
    void execute(String stylesID, com.pitstop.interactors.get.GetCarImagesArrayUseCase.Callback callback);

}
