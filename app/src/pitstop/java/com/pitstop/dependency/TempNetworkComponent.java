package com.pitstop.dependency;

import com.pitstop.utils.NetworkHelper;

import javax.inject.Singleton;

import dagger.Component;

/**
 * For temporary use while use cases(Interactors) are being integrated into different
 * parts of the system.
 *
 * Created by Karol Zdebel on 6/22/2017.
 */

@Singleton
@Component(modules = NetworkModule.class)
public interface TempNetworkComponent {

    NetworkHelper networkHelper();

}
