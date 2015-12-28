package com.pitstop.parse;

import android.app.Application;

import com.parse.Parse;
import com.pitstop.R;

/**
 * Created by Ansik on 12/28/15.
 */
public class ParseApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

//        ParseCrashReporting.enable(this);
        Parse.enableLocalDatastore(this);
        Parse.initialize(this, getString(R.string.parse_appID), getString(R.string.parse_clientID));
    }
}
