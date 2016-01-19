package com.castel.obd.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import android.content.Context;

public class FileUtil {
	private static FileUtil instance;
	public static List<String> DTC_list = new ArrayList<String>();

	public static FileUtil getInstance() {
		if (instance == null)
			instance = new FileUtil();
		return instance;
	}

	public void readDTC(Context ctx) {
		try {
			InputStream is = ctx.getAssets().open("DTC.bak");
			BufferedReader bf = new BufferedReader(new InputStreamReader(is));

			String line = null;
			try {
				line = bf.readLine();
				DTC_list.add(line);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			while (line != null) {
				try {
					line = bf.readLine();
					DTC_list.add(line);
				} catch (IOException e2) {
					e2.printStackTrace();
				}
			}
			is.close();
			bf.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String readProperties(Context ctx, String filename, String key) {
		String value = "";
		try {
			Properties pro = new Properties();
			InputStream is = ctx.getAssets().open(filename);
			pro.load(is);
			String str = pro.getProperty(key);
			if (null != str && !str.equals(""))
				value = new String(str.getBytes("ISO8859-1"), "UTF-8");

			is.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return value;
	}
}
