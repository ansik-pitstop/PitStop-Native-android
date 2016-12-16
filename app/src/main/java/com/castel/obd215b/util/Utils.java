package com.castel.obd215b.util;

import java.text.NumberFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
	public static boolean bCommonVehicle;

	public static boolean isEmpty(String str) {
		if (null == str || "".equals(str) || "null".equals(str)) {
			return true;
		}
		return false;
	}

	public static String fraction(int numerator, int denominator) {
		// ¡ä¡ä?¡§¨°???¨ºy?¦Ì??¨º??¡¥???¨®
		NumberFormat numberFormat = NumberFormat.getInstance();
		// ¨¦¨¨????¨¨¡¤¦Ì?D?¨ºy¦Ì?o¨®2??
		numberFormat.setMaximumFractionDigits(2);
		String result = numberFormat.format((float) numerator
				/ (float) denominator * 100);
		return result + "%";
	}

	public static String bytesToHexString(byte[] src) {
		StringBuilder stringBuilder = new StringBuilder("");
		if (src == null || src.length <= 0) {
			return null;
		}
		for (int i = 0; i < src.length; i++) {
			int v = src[i] & 0xFF;
			String hv = Integer.toHexString(v);
			if (hv.length() < 2) {
				stringBuilder.append(0);
			}
			stringBuilder.append(hv);
		}
		return stringBuilder.toString();
	}

	/**
	 * Convert hex string to byte[]
	 * 
	 * @param hexString
	 *            the hex string
	 * @return byte[]
	 */
	public static byte[] hexStringToBytes(String hexString) {
		if (hexString == null || hexString.equals("")) {
			return null;
		}
		hexString = hexString.toUpperCase();
		int length = hexString.length() / 2;
		char[] hexChars = hexString.toCharArray();
		byte[] d = new byte[length];
		for (int i = 0; i < length; i++) {
			int pos = i * 2;
			d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
		}
		return d;
	}

	/**
	 * Convert char to byte
	 * 
	 * @param c
	 *            char
	 * @return byte
	 */
	public static byte charToByte(char c) {
		return (byte) "0123456789ABCDEF".indexOf(c);
	}

	public static String toHexString(int number) {
		String ret = Integer.toHexString(number).toUpperCase();
		switch (ret.length()) {
		case 1:
			ret = "000" + ret;
			break;
		case 2:
			ret = "00" + ret;
			break;
		case 3:
			ret = "0" + ret;
			break;

		default:
			break;
		}

		return ret.toUpperCase();
	}

	public static boolean isInteger(String value) {
		try {
			Integer.parseInt(value);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}
	
	public static boolean isFloat(String value) {
		try {
			Float.parseFloat(value);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}
	
	/**判断字符串是否为数字
	 * @param str
	 * @return
	 */
	public boolean isNumeric(String str){ 
		   Pattern pattern = Pattern.compile("[0-9]*"); 
		   Matcher isNum = pattern.matcher(str);
		   if( !isNum.matches() ){
		       return false; 
		   } 
		   return true; 
		}
}
