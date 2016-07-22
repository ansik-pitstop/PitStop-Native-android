package com.pitstop.utils;

import android.support.v4.app.FragmentActivity;

import com.castel.obd.bluetooth.ObdManager;
import com.pitstop.background.BluetoothAutoConnectService;

/**
 * Created by david on 7/21/2016.
 */
public abstract class BSAbstractedFragmentActivity extends FragmentActivity implements ObdManager.IBluetoothDataListener{
    public BluetoothAutoConnectService autoConnectService;
    public boolean serviceIsBound;
}
