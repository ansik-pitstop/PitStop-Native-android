package com.pitstop.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.pitstop.BuildConfig;
import com.pitstop.database.LocalDatabaseHelper;
import com.pitstop.database.LocalDebugMessageStorage;
import com.pitstop.database.LocalUserStorage;
import com.pitstop.models.DebugMessage;

import org.graylog2.gelfclient.GelfConfiguration;
import org.graylog2.gelfclient.GelfMessage;
import org.graylog2.gelfclient.GelfMessageBuilder;
import org.graylog2.gelfclient.GelfMessageLevel;
import org.graylog2.gelfclient.GelfTransportResultListener;
import org.graylog2.gelfclient.GelfTransports;
import org.graylog2.gelfclient.transport.GelfTcpTransport;
import org.graylog2.gelfclient.transport.GelfTransport;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import rx.schedulers.Schedulers;

/**
 *
 * More on the formatting of logs can be found
 * at: https://docs.google.com/spreadsheets/d/1R95PWU6Q6R5_FfHKLSIhRbxWf_Eq2ylvGM3vqKkRMyU/edit#gid=0
 *
 * Created by Karol Zdebel
 */
public class Logger {

    private final static String TAG = Logger.class.getSimpleName();
    private static Logger INSTANCE = null;

    private GelfTransport gelfTransport = null;
    private LocalUserStorage localUserStorage;
    private ConnectivityManager connectivityManager;
    private LocalDebugMessageStorage localDebugMessageStorage;
    private GelfConfiguration gelfConfiguration = null;
    private Handler handler;
    private Context context;
    private int userId = -1;

    //Sends messages if a connection to the internet is established
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) { // internet connectivity listener

                try{
                    if (connectivityManager == null)
                        connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
                    //Check for gelfTransport not null because we want to re-instantiate it here, not create the first instance
                    if (connectivityManager != null && connectivityManager.getActiveNetworkInfo() != null
                            && connectivityManager.getActiveNetworkInfo().isConnected()
                            && gelfTransport != null && handler != null) {
                        Log.d(TAG,"Received internet ON, creating new instance of gelf transport");
                        handler.post(() -> {
                            gelfTransport.stop();
                            gelfTransport = new GelfTcpTransport(gelfConfiguration);
                        });
                    }else{
                        Log.d(TAG,"Received internet OFF");
                    }
                }catch(NullPointerException e){
                    e.printStackTrace();
                }

            }
        }
    };

    public static Logger getInstance(){
        if (INSTANCE == null){
            return null;
        }else{
            return INSTANCE;
        }
    }

    public Logger(Context context){
        this.localUserStorage = new LocalUserStorage(LocalDatabaseHelper.getInstance(context));
        this.context = context;
        connectivityManager =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        //Listen for network changes
        IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(broadcastReceiver, intentFilter);

        //Background handler creation (not really background)
        HandlerThread handlerThread = new HandlerThread("LOG_HANDLER");
        handlerThread.start();
        this.handler = new Handler(handlerThread.getLooper());

        handler.post(() -> {
            InetSocketAddress inetSocketAddress
                    = new InetSocketAddress("graylog-lx.backend-service.getpitstop.io",12900); //Port for UDP is different
            gelfConfiguration = new GelfConfiguration(inetSocketAddress)
                    .transport(GelfTransports.TCP)
                    .tcpKeepAlive(false) //Kill TCP connection after every message
                    .queueSize(512)
                    .connectTimeout(1000)
                    .reconnectDelay(500)
                    .sendBufferSize(-1)
                    .resultListener(new GelfTransportResultListener() {
                        @Override
                        public void onMessageSent(GelfMessage gelfMessage) {
                        }

                        @Override
                        public void onFailedToSend(GelfMessage gelfMessage) {
                        }

                        @Override
                        public void onFailedToConnect(List<GelfMessage> list) {
                        }
                    });

            gelfTransport = new GelfTcpTransport(gelfConfiguration); //Create here first, recreated in broadcast receiver
        });

        localDebugMessageStorage = new LocalDebugMessageStorage(LocalDatabaseHelper.getInstance(context));
        localDebugMessageStorage.getUnsentQueryObservable()
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.io())
                .map(query -> {
                    Cursor c = query.run();
                    List<DebugMessage> messageList = new ArrayList<>();
                    if(c.moveToFirst()) {
                        while(!c.isAfterLast()) {
                            messageList.add(DebugMessage.fromCursor(c));
                            c.moveToNext();
                        }
                    }
                    c.close();
                    return messageList;
                }).filter(messageList ->{
                    try{
                        return !messageList.isEmpty()
                                && connectivityManager != null
                                && connectivityManager.getActiveNetworkInfo() != null
                                && connectivityManager.getActiveNetworkInfo().isConnected();
                    }catch(NullPointerException e){
                        return false;
                    }

                }).subscribe(messageList -> {
                    Log.d(TAG, String.format("Received %d messages in subscribe()",messageList.size()));

                    List<DebugMessage> sentList = new ArrayList<>();
                    for (DebugMessage d: messageList){
                        boolean sent = sendMessage(d);
                        if (sent)
                            sentList.add(d);
                    }
                    localDebugMessageStorage.markAsSent(sentList);
                }, err -> {
                    Log.d(TAG," Error sending message");
                    err.printStackTrace();
                }
        );

    }


    //Format message and send through the network if a connection is available
    //mark the message as sent after it is, so that it is NOT re-sent again
    private boolean sendMessage(DebugMessage d) {

        GelfMessageLevel gelfLevel;
        switch (d.getLevel()) {
            case 0:
                gelfLevel = GelfMessageLevel.INFO;
                break;
            case 1:
                gelfLevel = GelfMessageLevel.DEBUG;
                break;
            case 2:
                gelfLevel = GelfMessageLevel.INFO;
                break;
            case 3:
                gelfLevel = GelfMessageLevel.WARNING;
                break;
            case 4:
                gelfLevel = GelfMessageLevel.ERROR;
                break;
            case 5:
                gelfLevel = GelfMessageLevel.CRITICAL;
                break;
            default:
                gelfLevel = GelfMessageLevel.CRITICAL;

        }

        String type = "";
        switch (d.getType()) {
            case DebugMessage.TYPE_NETWORK:
                type = "network";
                break;
            case DebugMessage.TYPE_BLUETOOTH:
                type = "bluetooth";
                break;
            case DebugMessage.TYPE_REPO:
                type = "repository";
                break;
            case DebugMessage.TYPE_USE_CASE:
                type = "use case";
                break;
            case DebugMessage.TYPE_VIEW:
                type = "view";
                break;
            case DebugMessage.TYPE_PRESENTER:
                type = "presenter";
                break;
            case DebugMessage.TYPE_TRIP:
                type = "trip";
                break;
            default:
                type = "other";

        }

        if (userId <= 0 && localUserStorage.getUser() != null)
            userId = localUserStorage.getUser().getId();

        String build = "";
        if (BuildConfig.DEBUG){
            build = "debug";
        }else if (BuildConfig.BUILD_TYPE.equals(BuildConfig.BUILD_TYPE_BETA)){
            build = "beta";
        }else{
            build = "release";
        }

        //Fields added on to every log message
        final GelfMessage gelfMessage = new GelfMessageBuilder(d.getMessage(), "com.pitstop.android")
                .timestamp(d.getTimestamp())
                .additionalField("tag", d.getTag())
                .additionalField("userId", userId)
                .additionalField("type", type)
                .additionalField("build",build)
                .additionalField("version", BuildConfig.VERSION_NAME)
                .additionalField("versionCode",BuildConfig.VERSION_CODE)
                .level(gelfLevel)
                .build();
        try{
            if (connectivityManager == null)
                connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null && connectivityManager.getActiveNetworkInfo() != null
                    && connectivityManager.getActiveNetworkInfo().isConnected()
                    && gelfTransport != null) {
                boolean trySend = gelfTransport.trySend(gelfMessage);
                if (trySend)
                    return true;
            }
        }catch(NullPointerException e){
            e.printStackTrace();
        }
        return false;
    }

    public static void initLogger(Context context){
        INSTANCE = new Logger(context);
    }

    public void logV(String tag, String message, int type) {
        if(BuildConfig.DEBUG) {
            Log.v(tag, message);
            new LocalDebugMessageStorage(LocalDatabaseHelper.getInstance(context)).addMessage(
                    new DebugMessage(System.currentTimeMillis(), message, tag, type, DebugMessage.LEVEL_V));
        }
    }

    public void logD(String tag, String message, int type) {
        if(BuildConfig.DEBUG || BuildConfig.BUILD_TYPE.equals(BuildConfig.BUILD_TYPE_BETA)) {
            Log.d(tag, message);
            DebugMessage debugMessage = new DebugMessage(System.currentTimeMillis(), message, tag, type, DebugMessage.LEVEL_D);
            localDebugMessageStorage.addMessage(debugMessage);
        }
    }

    public void logI(String tag, String message, int type) {
        Log.i(tag, message);
        DebugMessage debugMessage
                = new DebugMessage(System.currentTimeMillis(), message, tag, type, DebugMessage.LEVEL_I);
        localDebugMessageStorage.addMessage(debugMessage);
    }

    public void logW(String tag, String message, int type) {
        Log.w(tag, message);
        DebugMessage debugMessage
                = new DebugMessage(System.currentTimeMillis(), message, tag, type, DebugMessage.LEVEL_W);
        localDebugMessageStorage.addMessage(debugMessage);
    }

    public void logE(String tag, String message, int type) {
        Log.e(tag, message);
        DebugMessage debugMessage
                = new DebugMessage(System.currentTimeMillis(), message, tag, type, DebugMessage.LEVEL_E);
        localDebugMessageStorage.addMessage(debugMessage);
    }

    public void logException(String tag, Exception e, int type){
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String sStackTrace = sw.toString(); // stack trace as a string
        DebugMessage debugMessage
                = new DebugMessage(System.currentTimeMillis(), sStackTrace, tag, type, DebugMessage.LEVEL_E);
        localDebugMessageStorage.addMessage(debugMessage);
        e.printStackTrace();
    }

}