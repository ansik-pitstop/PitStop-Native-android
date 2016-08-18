package com.castel.obd215b.util;

import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.util.Log;

public class DateUtil {

	/**毫秒数转换成字符串
	 * @param millionSeconds
	 * @param format
	 * @return
	 */
	public static String millionSeconds2String(long millionSeconds,
			String format) {
		Date date = new Date(millionSeconds);
		SimpleDateFormat sdf;
		sdf = new SimpleDateFormat(format);
		String dateString = sdf.format(date);
		Log.d("guoqz", "毫秒数转换成字符串:"+dateString);
		return dateString;
	}

	/**字符串转化成毫秒数
	 * @param dateString
	 * @param format
	 * @return
	 */
	public static long string2MillionSeconds(String dateString, String format) {
//		dateString = PatternUtil.returnSpecifiedString(dateString, format);
		long millionSeconds = 0;
		Calendar c = Calendar.getInstance();
		try {
			c.setTime(new SimpleDateFormat(format).parse(PatternUtil
					.returnSpecifiedString(dateString, "[0-9]")));
			millionSeconds = c.getTimeInMillis();

		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Log.d("guoqz", "毫秒数转换成字符串:"+millionSeconds);
		
		return millionSeconds;
	}
	
	/** 获取系统时间
	 * @param format 时间格式
	 * @return
	 */
	public static String getSystemTime(String format) {
		SimpleDateFormat sDateFormat = new SimpleDateFormat(format);     
		String date = sDateFormat.format(new java.util.Date());
		return date;
	}
	
	public static String getSystemTime() {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
		Date curDate = new Date(System.currentTimeMillis());//获取当前时间
		return formatter.format(curDate);
	}
	
	
}
