package com.pitstop.Debug;

import android.app.Activity;
import android.content.Context;
import android.os.Looper;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by David Liu on 11/27/2015.
 */
public class PrintDebugThread extends Thread{

    private Process logcatProc;
    private BufferedReader mReader = null;
    private boolean mRunning = true;
    String cmds = null;
    private String mPID;
    private TextView textView = null;
    private Activity activity;
    public PrintDebugThread(String pid, TextView tv, Activity a) {
        mPID = pid;
        activity = a;
        /**
         *Setup printing Debug
         *
         * */
        textView = tv;

        // cmds = "logcat *:e *:w | grep \"(" + mPID + ")\"";
        // cmds = "logcat  | grep \"(" + mPID + ")\"";//打印??有日志信??
        cmds = "logcat -s gf";// 打印标签过滤信息
        // cmds = "logcat *:e *:i | grep \"(" + mPID + ")\"";
        // cmds = "logcat -s System.out";

    }

    public void stopLogs() {
        mRunning = false;
    }

    @Override
    public void run() {
        try {
            logcatProc = Runtime.getRuntime().exec(cmds);
            mReader = new BufferedReader(new InputStreamReader(
                    logcatProc.getInputStream()), 1024);
            String line = null;
            while (mRunning && (line = mReader.readLine()) != null) {
                if (!mRunning) {
                    break;
                }
                if (line.length() == 0) {
                    continue;
                }
                if (textView != null ) {
                    final String finalLine = line;
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textView.setText(textView.getText() + getDateEN() + "  " + finalLine + "\n");
                        }
                    });
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (logcatProc != null) {
                logcatProc.destroy();
                logcatProc = null;
            }
            if (mReader != null) {
                try {
                    mReader.close();
                    mReader = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (textView != null) {
                textView = null;
            }

        }
    }

    public static String getDateEN() {
        SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String date1 = format1.format(new Date(System.currentTimeMillis()));
        return date1;// 2012-10-03 23:41:31
    }
}
