package com.pitstop.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import com.pitstop.R;
import com.pitstop.ui.main_activity.MainActivity;

import static com.pitstop.bluetooth.BluetoothAutoConnectService.notifID;

/**
 * Created by Karol Zdebel on 8/16/2017.
 */

public class NotificationsHelper {

    private static Handler mainHandler = new Handler(Looper.getMainLooper());
    public static final int TRIPS_FG_NOTIF_ID = 2559;


    public static void cancelConnectedNotification(Context context){
        mainHandler.post(() -> {
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(notifID);
        });
    }

    public static Notification getForegroundTripServiceNotification(boolean confident, Context context){
        Bitmap icon = BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_push);

        String message = "";
        if (confident){
            message = "Pitstop is ready to begin recording your trip";
        }else{
            message = "Pitstop has began recording your trip";
        }

        NotificationCompat.Builder mBuilder =
            new NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.ic_directions_car_white_24dp)
                    .setLargeIcon(icon)
                    .setColor(context.getResources().getColor(R.color.highlight))
                    .setContentTitle("Pitstop")
                    .setContentText(message);

        Intent resultIntent = new Intent(context, MainActivity.class);
        resultIntent.putExtra(MainActivity.Companion.getFROM_NOTIF(), true);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);

        stackBuilder.addParentStack(MainActivity.class);

        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);


        return mBuilder.build();
    }

    public static void sendNotification(Context context, String message, String title){
        mainHandler.post(() -> {
            Bitmap icon = BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_push);

            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(context)
                            .setSmallIcon(R.drawable.ic_directions_car_white_24dp)
                            .setLargeIcon(icon)
                            .setColor(context.getResources().getColor(R.color.highlight))
                            .setContentTitle(title)
                            .setContentText(message);

            Intent resultIntent = new Intent(context, MainActivity.class);
            resultIntent.putExtra(MainActivity.Companion.getFROM_NOTIF(), true);

            TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);

            stackBuilder.addParentStack(MainActivity.class);

            stackBuilder.addNextIntent(resultIntent);
            PendingIntent resultPendingIntent =
                    stackBuilder.getPendingIntent(
                            0,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );
            mBuilder.setContentIntent(resultPendingIntent);
            NotificationManager mNotificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(notifID, mBuilder.build());
        });
    }
}
