package com.pitstop.parse;

import android.app.Application;
import android.content.Context;

import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.parse.Parse;
import com.parse.ParseInstallation;
import com.pitstop.R;

import io.smooch.core.Smooch;

/**
 * Created by Ansik on 12/28/15.
 */
public class ParseApplication extends Application {

    private static MixpanelAPI mixpanelAPI;
    @Override
    public void onCreate() {
        super.onCreate();

//        ParseCrashReporting.enable(this);
        Parse.enableLocalDatastore(this);
        Parse.initialize(this, getString(R.string.parse_appID), getString(R.string.parse_clientID));
        ParseInstallation.getCurrentInstallation().saveInBackground();
        Smooch.init(this, getString(R.string.smooch_token));
        mixpanelAPI = MixpanelAPI.getInstance(this, getString(R.string.mixpanel_api_token));
    }

    //Multidex support library - Use when Building Apps with Over 65K Methods
    /*@Override
    protected void attachBaseContext (Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }*/

    public MixpanelAPI getMixpanelAPI() {
        if(mixpanelAPI==null) {
            mixpanelAPI = MixpanelAPI.getInstance(this, getString(R.string.mixpanel_api_token));
        }
        return mixpanelAPI;
    }
}
