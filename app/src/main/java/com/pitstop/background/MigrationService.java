package com.pitstop.background;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.parse.ParseUser;
import com.pitstop.DataAccessLayer.DTOs.User;
import com.pitstop.DataAccessLayer.ServerAccess.RequestCallback;
import com.pitstop.DataAccessLayer.ServerAccess.RequestError;
import com.pitstop.MainActivity;
import com.pitstop.R;
import com.pitstop.SplashScreen;
import com.pitstop.application.GlobalApplication;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONException;
import org.json.JSONObject;

public class MigrationService extends Service {

    private static final String TAG = MigrationService.class.getSimpleName();

    public static final String MIGRATION_BROADCAST = "com.ansik.pitstop.MIGRATION_BROADCAST";

    public static final int notificationId = 124135;

    public static final String USER_MIGRATION_ID = "USER_MIGRATION_ID";
    public static final String USER_MIGRATION_ACCESS = "USER_MIGRATION_ACCESS";
    public static final String USER_MIGRATION_REFRESH = "USER_MIGRATION_REFRESH";
    public static final String USER_MIGRATION_SUCCESS = "USER_MIGRATION_SUCCESS";

    private int userId;
    private String accessToken;
    private String refreshToken;

    private GlobalApplication application;

    private CountDownTimer timer;

    private NetworkHelper networkHelper;

    @Override
    public void onCreate() {
        super.onCreate();

        application = (GlobalApplication) getApplicationContext();

        networkHelper = new NetworkHelper(getApplicationContext());

        Intent doneIntent = new Intent(this, MainActivity.class);
        doneIntent.putExtra(MainActivity.FROM_NOTIF, true);
        doneIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

        final PendingIntent donePendingIntent = PendingIntent.getActivity(this, 45435, doneIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent failedIntent = new Intent(this, SplashScreen.class);
        failedIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

        final PendingIntent failedPendingIntent = PendingIntent.getActivity(this, 45435, failedIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        final NotificationCompat.Builder notif = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_directions_car_white_24dp)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_push))
                .setContentTitle("Update in progress")
                .setProgress(100, 100, true)
                .setAutoCancel(false)
                .setOngoing(true);

        final NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        notificationManager.notify(notificationId, notif.build());

        application.setTokens(accessToken, refreshToken);

        timer = new CountDownTimer(120000, 6000) {
            @Override
            public void onTick(long millisUntilFinished) {
                networkHelper.getUser(userId, new RequestCallback() {
                    @Override
                    public void done(final String response, RequestError requestError) {
                        if(response == null) {
                            return;
                        }
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            if (requestError == null && jsonResponse.getJSONObject("migration").getBoolean("isMigrationDone")) {
                                Log.i(TAG, "Migration complete");

                                new CountDownTimer(3000, 3000) { // in case not all data is completely migrated
                                    @Override
                                    public void onTick(long millisUntilFinished) {
                                    }

                                    @Override
                                    public void onFinish() {
                                        notificationManager.notify(notificationId,
                                                notif.setContentTitle("Update complete").setContentText("Press here to use the app").setAutoCancel(true)
                                                        .setOngoing(false).setProgress(0, 0, false).setContentIntent(donePendingIntent).build());
                                        ((GlobalApplication) getApplicationContext()).logInUser(accessToken, refreshToken, User.jsonToUserObject(response));
                                        timer.cancel();
                                        ParseUser.logOut();
                                        Intent resultIntent = new Intent(MIGRATION_BROADCAST);
                                        resultIntent.putExtra(USER_MIGRATION_SUCCESS, true);
                                        sendBroadcast(resultIntent);
                                        stopSelf();
                                    }
                                }.start();
                            } else {
                                Log.i(TAG, "Migration in progress");
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }

            @Override
            public void onFinish() {
                Log.i(TAG, "Migration failed");

                application.setTokens(null, null);

                notificationManager.notify(notificationId,
                        notif.setContentTitle("Update failed").setContentText("Press here to try again").setAutoCancel(true)
                                .setProgress(0, 0, false).setOngoing(false).setContentIntent(failedPendingIntent).build());

                Intent resultIntent = new Intent(MIGRATION_BROADCAST);
                resultIntent.putExtra(USER_MIGRATION_SUCCESS, false);
                sendBroadcast(resultIntent);
                stopSelf();
            }
        }.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null) {
            userId = intent.getIntExtra(USER_MIGRATION_ID, -1);
            accessToken = intent.getStringExtra(USER_MIGRATION_ACCESS);
            refreshToken = intent.getStringExtra(USER_MIGRATION_REFRESH);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
