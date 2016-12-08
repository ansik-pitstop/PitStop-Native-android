package com.castel.obd215b.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PatternUtil {
	
	/**
	 * @param from
	 * @param format
	 * @return
	 */
	public static String returnSpecifiedString(String from, String format) {
		Pattern pattern = Pattern.compile(format);
		Matcher matcher = pattern.matcher(from);
		StringBuffer buffer = new StringBuffer();
		while(matcher.find()){              
		    buffer.append(matcher.group());        
		}
		return buffer.toString();
	}
}
