package com.pitstop.utils;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.pitstop.BuildConfig;
import com.pitstop.database.LocalDebugMessageStorage;
import com.pitstop.models.DebugMessage;

import org.graylog2.gelfclient.GelfConfiguration;
import org.graylog2.gelfclient.GelfMessage;
import org.graylog2.gelfclient.GelfMessageBuilder;
import org.graylog2.gelfclient.GelfMessageLevel;
import org.graylog2.gelfclient.GelfTransportResultListener;
import org.graylog2.gelfclient.GelfTransports;
import org.graylog2.gelfclient.transport.GelfTransport;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import rx.schedulers.Schedulers;

public class Logger {

    private final static String TAG = Logger.class.getSimpleName();

    private static boolean DEBUG = BuildConfig.DEBUG;
    private static boolean NOT_RELEASE = !BuildConfig.BUILD_TYPE.equals(BuildConfig.BUILD_TYPE_RELEASE);
    private static boolean NOT_BETA = !BuildConfig.BUILD_TYPE.equals(BuildConfig.BUILD_TYPE_BETA);
    private static Logger INSTANCE = null;

    private GelfTransport gelfTransport = null;
    private Context context;

    public static Logger getInstance(){
        if (INSTANCE == null){
            return null;
        }else{
            return INSTANCE;
        }
    }

    public Logger(Context context){
        this.context = context;
        LocalDebugMessageStorage localDebugMessageStorage = new LocalDebugMessageStorage(context);
        localDebugMessageStorage.getQueryObservable(DebugMessage.TYPE_BLUETOOTH)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .onErrorReturn(err -> {
                    Log.d(TAG,"error");
                    return null;
                }).doOnError(err -> Log.d(TAG,"err: "+err))
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
                }).filter(messageList -> !messageList.isEmpty())
                .subscribe(messageList -> {
                    Log.d(TAG,"messages received: "+messageList);
                    if (gelfTransport == null){
                        gelfTransport = GelfTransports.create(
                                new GelfConfiguration(new InetSocketAddress(
                                        "graylog.backend-service.getpitstop.io", 12900))
                                        .transport(GelfTransports.TCP)
                                        .tcpKeepAlive(false)
                                        .queueSize(512)
                                        .connectTimeout(12000)
                                        .reconnectDelay(1000)
                                        .sendBufferSize(-1)
                                        .resultListener(new GelfTransportResultListener() {
                                            @Override
                                            public void onMessageSent(GelfMessage gelfMessage) {
                                                Log.d(TAG,"resultListener.onMessageSent() gelfMessage: "+gelfMessage);
                                                localDebugMessageStorage.removeAllMessages();
                                            }

                                            @Override
                                            public void onFailedToSend(GelfMessage gelfMessage) {
                                                Log.d(TAG,"resultListener.onFailedToSend() gelfMessage: "+gelfMessage);

                                            }

                                            @Override
                                            public void onFailedToConnect(List<GelfMessage> list) {
                                                Log.d(TAG,"resultListener.onFailedToConnect() gelfMessageList: "+list);

                                            }
                                        }));
                    }

                    Log.d(TAG,"Logger, received debug message list: "+messageList);

                    for (DebugMessage d: messageList){
                        GelfMessageLevel gelfLevel;
                        switch(d.getLevel()){
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
                        final GelfMessage gelfMessage = new GelfMessageBuilder(d.getMessage(),"com.android.pitstop")
                                .additionalField("Tag",d.getTag())
                                .level(gelfLevel)
                                .build();
                        boolean trySend = gelfTransport.trySend(gelfMessage);
                        Log.d(TAG,"log dispatched? "+trySend);
                    }
                });
    }

    public static void initLogger(Context context){
        INSTANCE = new Logger(context);
    }

    public void logV(String tag, String message, boolean showLogcat, int type) {
        if(NOT_RELEASE && NOT_BETA) {
            if (showLogcat) {
                Log.v(tag, message);
            }
            new LocalDebugMessageStorage(context).addMessage(
                    new DebugMessage(System.currentTimeMillis(), message, tag, type, DebugMessage.LEVEL_V));
        }
    }

    public void logD(String tag, String message, boolean showLogcat, int type) {
        if(NOT_RELEASE && NOT_BETA) {
            if (showLogcat) {
                Log.d(tag, message);
            }
            new LocalDebugMessageStorage(context).addMessage(
                    new DebugMessage(System.currentTimeMillis(), message, tag, type, DebugMessage.LEVEL_D));
        }
    }

    public void logI(String tag, String message, boolean showLogcat, int type) {
        if(NOT_RELEASE && NOT_BETA) {
            if (showLogcat) {
                Log.i(tag, message);
            }
            new LocalDebugMessageStorage(context).addMessage(
                    new DebugMessage(System.currentTimeMillis(), message, tag, type, DebugMessage.LEVEL_I));
        }
    }

    public void logW(String tag, String message, boolean showLogcat, int type) {
        if(NOT_RELEASE && NOT_BETA) {
            if (showLogcat) {
                Log.w(tag, message);
            }
            new LocalDebugMessageStorage(context).addMessage(
                    new DebugMessage(System.currentTimeMillis(), message, tag, type, DebugMessage.LEVEL_W));
        }
    }

    public void logE(String tag, String message, boolean showLogcat, int type) {
        if(NOT_RELEASE && NOT_BETA) {
            if (showLogcat) {
                Log.e(tag, message);
            }
            new LocalDebugMessageStorage(context).addMessage(
                    new DebugMessage(System.currentTimeMillis(), message, tag, type, DebugMessage.LEVEL_E));
        }
    }

}