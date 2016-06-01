package com.pitstop.auto;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.pitstop.R;
import com.pitstop.parse.ParseApplication;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.Timestamp;
import java.util.Date;

/**
 * Created by davidl on 4/21/16.
 */
public class AndroidAutoHeardReceiver extends BroadcastReceiver {
    public static final int conversationID = 123123123;
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("asdfas",intent.getAction());
        if(intent.getAction().equals("com.google.android.c2dm.intent.RECEIVE")){
            Bundle extras = intent.getExtras();
            String msg = intent.getStringExtra("message");

            //heard intent
            Intent msgHeardIntent = new Intent()
                    .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                    .setAction("com.pitstop.auto.MESSAGE_HEARD")
                    .putExtra("conversation_id", conversationID);

            PendingIntent msgHeardPendingIntent =
                    PendingIntent.getBroadcast(context,
                            conversationID,
                            msgHeardIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT);

            //replied intent
            Intent msgReplyIntent = new Intent()
                    .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                    .setAction("com.pitstop.auto.MESSAGE_REPLY")
                    .putExtra("conversation_id", conversationID);

            PendingIntent msgReplyPendingIntent = PendingIntent.getBroadcast(
                    context,
                    conversationID,
                    msgReplyIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);


            // Create an unread conversation object to organize a group of messages
            // from a particular sender.
            NotificationCompat.CarExtender.UnreadConversation.Builder unreadConvBuilder =
                    new NotificationCompat.CarExtender.UnreadConversation.Builder("PITSTOP MSG")
                            .setReadPendingIntent(msgHeardPendingIntent)
                            .setReplyAction(msgReplyPendingIntent, ParseApplication.remoteInput);

            try {
                JSONObject parsed = new JSONObject(msg);
                unreadConvBuilder.addMessage(parsed.getString("text")+" - "+parsed.getString("name"))
                        .setLatestTimestamp(new Date().getTime());
                NotificationCompat.Builder notificationBuilder =
                        new NotificationCompat.Builder(context)
                                .setSmallIcon(R.drawable.logo)
                                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),R.drawable.logo));
                notificationBuilder.extend(new NotificationCompat.CarExtender()
                        .setUnreadConversation(unreadConvBuilder.build()));
                NotificationManagerCompat msgNotificationManager =
                        NotificationManagerCompat.from(context);
                msgNotificationManager.notify("PITSTOP",
                        conversationID, notificationBuilder.build());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        Log.d("NotificaitonReciever","Heard");
    }
}
