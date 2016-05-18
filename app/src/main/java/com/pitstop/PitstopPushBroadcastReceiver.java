package com.pitstop;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.admin.SystemUpdatePolicy;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import com.parse.Parse;
import com.parse.ParseAnalytics;
import com.parse.ParsePushBroadcastReceiver;
import com.pitstop.DataAccessLayer.DTOs.ParseNotification;
import com.pitstop.DataAccessLayer.DataAdapters.ParseNotificationStore;
import com.pitstop.parse.ParseApplication;
import com.pitstop.utils.MixpanelHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * TODO Move to UTIL folder
 */
public class PitstopPushBroadcastReceiver extends ParsePushBroadcastReceiver {


    public static final String EXTRA_ACTION = "action";
    public static final String EXTRA_CAR_ID = "carId";

    public static final String ACTION_UPDATE_MILEAGE = "update_mileage";
    private final static String GROUP_KEY_ALERTS = "group_key_alerts";
    private final static String KEY_PARSE_ID = "parsePushId";
    public final static String ACTIVITY_NAME = "PUSH";
    private final String TAG = "PushParse";


    @Override
    protected void onPushReceive(Context context, Intent intent) {
        Log.i(TAG, "PUSH RECEIVED!!");
        JSONObject pushData = getPushData(intent);
        if (pushData == null || (!pushData.has("alert") && !pushData.has("title"))) {
            return;
        }

        ParseNotificationStore localStore = new ParseNotificationStore(context);
        ParseNotification parseNotification = new ParseNotification();

        ApplicationInfo appInfo = context.getApplicationInfo();
        String fallbackTitle = context.getPackageManager().getApplicationLabel(appInfo).toString();
        parseNotification.setTitle(pushData.optString("title", fallbackTitle));
        parseNotification.setAlert(pushData.optString("alert", "Notification received."));
        parseNotification.setParsePushId(pushData.optString(KEY_PARSE_ID, ""));
        String tickerText = String.format(Locale.getDefault(), "%s: %s", parseNotification.getTitle(),
                parseNotification.getAlert());
        localStore.storeNotification(parseNotification);

        Bundle extras = intent.getExtras();

        Random random = new Random();
        int contentIntentRequestCode = random.nextInt();
        int deleteIntentRequestCode = random.nextInt();

        // Security consideration: To protect the app from tampering, we require that intent filters
        // not be exported. To protect the app from information leaks, we restrict the packages which
        // may intercept the push intents.
        String packageName = context.getPackageName();

        Intent contentIntent = new Intent(ParsePushBroadcastReceiver.ACTION_PUSH_OPEN);
        contentIntent.putExtras(extras);
        contentIntent.setPackage(packageName);

        Intent deleteIntent = new Intent(ParsePushBroadcastReceiver.ACTION_PUSH_DELETE);
        deleteIntent.putExtras(extras);
        deleteIntent.setPackage(packageName);

        PendingIntent pContentIntent = PendingIntent.getBroadcast(context, contentIntentRequestCode,
                contentIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent pDeleteIntent = PendingIntent.getBroadcast(context, deleteIntentRequestCode,
                deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Bitmap icon = BitmapFactory.decodeResource(context.getResources(),
                R.mipmap.ic_push);




        List<ParseNotification> notificationList = localStore.getAllNotifications();
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context);
        notification
                .setSmallIcon(R.drawable.ic_directions_car_white_24dp)
                .setColor(context.getResources().getColor(R.color.highlight))
                .setLargeIcon(icon)
                .setAutoCancel(true)
                .setContentIntent(pContentIntent)
                .setDeleteIntent(pDeleteIntent)
                .setDefaults(Notification.DEFAULT_ALL);

        NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        if(notificationList.size() == 1) {
            notification
                    .setContentTitle(parseNotification.getTitle())
                    .setContentText(parseNotification.getAlert());
            nm.notify(0, notification.build());

        } else {
            NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
            for(ParseNotification notification1 : notificationList) {
                inboxStyle.addLine(notification1.getAlert());
            }
            inboxStyle.setBigContentTitle("Pitstop").setSummaryText("Alerts");
            notification
                    .setContentTitle("Pitstop")
                    .setContentText("New Alert")
                    .setStyle(inboxStyle)
                    .setNumber(notificationList.size())
                    .setGroup(GROUP_KEY_ALERTS)
                    .setGroupSummary(true);

            nm.notify(0, notification.build());
        }
    }

    @Override
    public void onPushOpen(Context context, Intent intent) {
        Log.i(TAG, "Push open");
        boolean openedActivity = false;
        // Send a Parse Analytics "push opened" event
        ParseAnalytics.trackAppOpenedInBackground(intent);
        ParseNotificationStore localNotificationStore = new ParseNotificationStore(context);
        localNotificationStore.deleteAllNotifications();

        try {
            JSONObject pushData = new JSONObject(intent.getStringExtra(KEY_PUSH_DATA));
            Log.i(TAG, "Push data: "+pushData.toString());
            Intent target = new Intent(context, MainActivity.class);
            target.putExtras(intent.getExtras());
            target.putExtra(MainActivity.FROM_ACTIVITY, ACTIVITY_NAME);
            target.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(target);
            openedActivity = true;

            ParseApplication application = (ParseApplication) context.getApplicationContext();
            new MixpanelHelper(application).trackAppStatus(MixpanelHelper.APP_LAUNCHED_FROM_PUSH);
        } catch (JSONException e) {
            Log.e(TAG, "Unexpected JSONException when receiving push data: ", e);
        }

        if (!openedActivity) {
            super.onPushOpen(context, intent);
        }
    }

    @Override
    protected void onPushDismiss(Context context, Intent intent) {
        ParseNotificationStore localNotificationStore = new ParseNotificationStore(context);
        localNotificationStore.deleteAllNotifications();
        super.onPushDismiss(context, intent);
    }

    private JSONObject getPushData(Intent intent) {
        try {
            return new JSONObject(intent.getStringExtra(KEY_PUSH_DATA));
        } catch (JSONException e) {
            Log.e(TAG, "Unexpected JSONException when receiving push data: ", e);
            return null;
        }
    }
}
