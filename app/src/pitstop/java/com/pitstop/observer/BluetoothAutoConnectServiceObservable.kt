package com.pitstop.observer

import com.pitstop.observer.AutoConnectServiceBindingObserver

/**
 * Created by ishan on 2017-11-07.
 */
interface BluetoothAutoConnectServiceObservable {
    fun subscribe(autoConnectBinderObserver: AutoConnectServiceBindingObserver)
}