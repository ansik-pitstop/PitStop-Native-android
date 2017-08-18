package com.pitstop.interactors.other;

import com.pitstop.dependency.UseCaseComponent;

/**
 * Created by Karol Zdebel on 8/18/2017.
 */

public interface PeriodicCachedTripSendUseCase extends Runnable {

    void execute(UseCaseComponent useCaseComponent);
}
