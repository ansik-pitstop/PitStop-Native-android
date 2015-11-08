package com.castel.obd.log;

import android.content.Context;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * log??	�D?????????????????�XSD???
 *
 *
 */
// ??�D?????????????????????
// LogcatHelper.getInstance(this).start();
// LogcatHelper.getInstance(this).stop();
public class LogCatHelper {
	private static LogCatHelper INSTANCE = null;
	private static String PATH_LOGCAT;
	private LogDumper mLogDumper = null;
	private int mPId;

	/**
	 *
	 * ????��??????????
	 *
	 * */
	public void init(Context context) {
		if (Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			PATH_LOGCAT = Environment.getExternalStorageDirectory()
					.getAbsolutePath()
					+ File.separator
					+ "212BLOG";
		} else {// ??????SD????????????????�X��????????�X?????????????????????
			PATH_LOGCAT = context.getFilesDir().getAbsolutePath()
					+ File.separator + "Chap";
		}
		File file = new File(PATH_LOGCAT);
		if (!file.exists()) {
			file.mkdirs();
		}
	}

	public static LogCatHelper getInstance(Context context) {
		if (INSTANCE == null) {
			INSTANCE = new LogCatHelper(context);
		}
		return INSTANCE;
	}

	private LogCatHelper(Context context) {
		init(context);
		mPId = android.os.Process.myPid();
	}

	public void start() {
		if (mLogDumper == null)
			mLogDumper = new LogDumper(String.valueOf(mPId), PATH_LOGCAT);
		mLogDumper.start();
	}

	public void stop() {
		if (mLogDumper != null) {
			mLogDumper.stopLogs();
			mLogDumper = null;
		}
	}

	private class LogDumper extends Thread {

		private Process logcatProc;
		private BufferedReader mReader = null;
		private boolean mRunning = true;
		String cmds = null;
		private String mPID;
		private FileOutputStream out = null;

		public LogDumper(String pid, String dir) {
			mPID = pid;
			try {
				out = new FileOutputStream(new File(dir, "log-" + getFileName()
						+ ".log"));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}

			/**
			 *
			 * ??�D????????��???*:v , *:d , *:w , *:e , *:f , *:s
			 *
			 * ????????????mPID????????? E???W?????��?????�D???.
			 *
			 * */

			// cmds = "logcat *:e *:w | grep \"(" + mPID + ")\"";
			// cmds = "logcat  | grep \"(" + mPID + ")\"";//?????�X????????�D?????????
			cmds = "logcat -s gf";// ?????�X??????????????????
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
					if (out != null && line.contains(mPID)) {
						out.write((getDateEN() + "  " + line + "\n").getBytes());
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
				if (out != null) {
					try {
						out.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					out = null;
				}

			}

		}

	}

	public static String getFileName() {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
		String date = format.format(new Date(System.currentTimeMillis()));
		return date;
	}

	public static String getDateEN() {
		SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String date1 = format1.format(new Date(System.currentTimeMillis()));
		return date1;// 2012-10-03 23:41:31
	}

}
