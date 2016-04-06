package com.pitstop.parse;

import android.app.Application;
import android.util.Log;

import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.parse.Parse;
import com.parse.ParseInstallation;
import com.parse.ParseUser;
import com.pitstop.MainActivity;
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

    public static void setUpMixPanel(){
        if(ParseUser.getCurrentUser()!=null) {
            Log.i(MainActivity.TAG, "Current parse user is not null");
            Log.i(MainActivity.TAG, "Current user's name: "+ ParseUser.getCurrentUser().getString("name"));
            mixpanelAPI.identify(ParseUser.getCurrentUser().getObjectId());
            mixpanelAPI.getPeople().identify(ParseUser.getCurrentUser().getObjectId());
            mixpanelAPI.getPeople().set("$phone", ParseUser.getCurrentUser().get("phoneNumber"));
            mixpanelAPI.getPeople().set("$name", ParseUser.getCurrentUser().get("name"));
            mixpanelAPI.getPeople().set("$email", ParseUser.getCurrentUser().getEmail());
        }
    }

    public MixpanelAPI getMixpanelAPI() {
        if(mixpanelAPI == null) {
            mixpanelAPI = MixpanelAPI.getInstance(this, getString(R.string.mixpanel_api_token));
        }
        return mixpanelAPI;
    }
}
