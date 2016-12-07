package com.pitstop.ui;

import android.support.v4.app.FragmentActivity;

import com.pitstop.bluetooth.BluetoothAutoConnectService;

/**
 * Created by david on 7/21/2016.
 */
public abstract class BSAbstractedFragmentActivity extends FragmentActivity{
    public BluetoothAutoConnectService autoConnectService;
    public boolean serviceIsBound;
}
