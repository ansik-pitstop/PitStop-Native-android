package com.castel.obd215b.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

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
			InputStream is = ctx.getAssets().open("DTC_215B.bak");
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
	
	public static String getPath(Context context, Uri uri) {
		 
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = { "_data" };
            Cursor cursor = null;
 
            try {
                cursor = context.getContentResolver().query(uri, projection,null, null, null);
                int column_index = cursor.getColumnIndexOrThrow("_data");
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            } catch (Exception e) {
                // Eat it
            }
        }
 
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
 
        return null;
    }

}
