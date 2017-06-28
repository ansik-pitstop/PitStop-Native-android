package com.pitstop.ui.mainFragments;

import com.pitstop.observer.Subject;

/**
 * Created by Karol Zdebel on 6/28/2017.
 */

public interface BluetoothFragmentNotifier extends Subject{
    void notifyDeviceConnected();
    void notifyDeviceDisconnected();
}
