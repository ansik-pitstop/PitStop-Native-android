package com.pitstop.auto;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by davidl on 4/21/16.
 */
public class AndroidAutoReplyReceiver  extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent){
        Log.d("NotificaitonReciever","Heard");
    }
}
