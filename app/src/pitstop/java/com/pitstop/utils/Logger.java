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
import org.graylog2.gelfclient.GelfTransports;
import org.graylog2.gelfclient.transport.GelfTransport;

import java.net.InetSocketAddress;

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
                .subscribe(query -> {
                    if (gelfTransport == null){
                        gelfTransport = GelfTransports.create(
                                new GelfConfiguration(new InetSocketAddress(
                                        "graylog.backend-service.getpitstop.io", 12900))
                                        .transport(GelfTransports.TCP)
                                        .tcpKeepAlive(false)
                                        .queueSize(512)
                                        .connectTimeout(12000)
                                        .reconnectDelay(1000)
                                        .sendBufferSize(-1));
                    }

                    Cursor cursor = query.run();
                    DebugMessage message = DebugMessage.fromCursor(cursor);
                    Log.d(TAG,"Logger, received debug message: "+message.getMessage());

                    GelfMessageLevel gelfLevel;
                    switch(message.getLevel()){
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
                    final GelfMessage gelfMessage = new GelfMessageBuilder(message.getMessage(),"com.android.pitstop")
                            .additionalField("Tag",message.getTag())
                            .level(gelfLevel)
                            .build();
                    boolean trySend = gelfTransport.trySend(gelfMessage);
                    Log.d(TAG,"log dispatched? "+trySend);
                });
    }

    public static void initLogger(Context context){
        INSTANCE = new Logger(context);
    }

    public void debugLogV(String tag, String message, boolean showLogcat, int type) {
        if(NOT_RELEASE && NOT_BETA) {
            if (showLogcat) {
                Log.v(tag, message);
            }
            new LocalDebugMessageStorage(context).addMessage(
                    new DebugMessage(System.currentTimeMillis(), message, tag, type, DebugMessage.LEVEL_V));
        }
    }

    public void debugLogD(String tag, String message, boolean showLogcat, int type) {
        if(NOT_RELEASE && NOT_BETA) {
            if (showLogcat) {
                Log.d(tag, message);
            }
            new LocalDebugMessageStorage(context).addMessage(
                    new DebugMessage(System.currentTimeMillis(), message, tag, type, DebugMessage.LEVEL_D));
        }
    }

    public void debugLogI(String tag, String message, boolean showLogcat, int type) {
        if(NOT_RELEASE && NOT_BETA) {
            if (showLogcat) {
                Log.i(tag, message);
            }
            new LocalDebugMessageStorage(context).addMessage(
                    new DebugMessage(System.currentTimeMillis(), message, tag, type, DebugMessage.LEVEL_I));
        }
    }

    public void debugLogW(String tag, String message, boolean showLogcat, int type) {
        if(NOT_RELEASE && NOT_BETA) {
            if (showLogcat) {
                Log.w(tag, message);
            }
            new LocalDebugMessageStorage(context).addMessage(
                    new DebugMessage(System.currentTimeMillis(), message, tag, type, DebugMessage.LEVEL_W));
        }
    }

    public void debugLogE(String tag, String message, boolean showLogcat, int type, Context context) {
        if(NOT_RELEASE && NOT_BETA) {
            if (showLogcat) {
                Log.e(tag, message);
            }
            new LocalDebugMessageStorage(context).addMessage(
                    new DebugMessage(System.currentTimeMillis(), message, tag, type, DebugMessage.LEVEL_E));
        }
    }

    public void LOGV(String tag, String message) {
        if(DEBUG) {
            Log.v(tag, message);
        }
    }

    public void LOGD(String tag, String message) {
        if(DEBUG) {
            Log.d(tag, message);
        }
    }

    public void LOGI(String tag, String message) {
        if(DEBUG) {
            Log.i(tag, message);
        }
    }

    public void LOGW(String tag, String message) {
        if(DEBUG) {
            Log.w(tag, message);
        }
    }

    public void LOGE(String tag, String message) {
        if(DEBUG) {
            Log.e(tag, message);
        }
    }

    public void LOGA(String tag, String message) {
        if(DEBUG) {
            Log.wtf(tag, message);
        }
    }

}