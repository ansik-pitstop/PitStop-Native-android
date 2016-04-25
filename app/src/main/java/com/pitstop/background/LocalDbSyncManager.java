package com.pitstop.background;

import android.content.Context;
import android.os.Handler;

import com.pitstop.DataAccessLayer.DataAdapters.LocalShopAdapter;

/**
 * Created by Paul Soladoye on 18/04/2016.
 */
public class LocalDbSyncManager {
    private LocalShopAdapter dealershipAdapter;
    private Handler mHandler;

    public LocalDbSyncManager(Context context) {
        dealershipAdapter = new LocalShopAdapter(context);
        mHandler = new Handler();
    }

    public void startSync() {

    }

    public void stopSync() {

    }

    Runnable dealershipSyncRunnable = new Runnable() {
        @Override
        public void run() {

        }
    };
}
