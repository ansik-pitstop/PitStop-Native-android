package com.pitstop.interactors.other;

import com.pitstop.interactors.Interactor;

/**
 * Created by Karol Zdebel on 9/14/2017.
 */

public interface DiscoveryTimeoutUseCase extends Interactor {
    interface Callback{
        void onFinish(int discoveryNum);
    }

    void execute(int discoveryNum, Callback callback);
}
