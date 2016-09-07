package com.castel.obd215b.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.castel.obd215b.info.FaultInfo;

import android.content.Context;
import android.util.Log;

public class FaultParse {

	public static List<FaultInfo> parse(Context context, String[] faults) {
		int[] tt1 = new int[faults.length];
		for (int i = 0; i < faults.length; i++) {
			tt1[i] = Integer.parseInt(faults[i], 16);
		}

		FileUtil.getInstance().readDTC(context);

		List<FaultInfo> faultInfos = new ArrayList<FaultInfo>();
		for (int ii = 0; ii < tt1.length; ii++) {
			FaultInfo fault = new FaultInfo();
			int tt = tt1[ii];
			Log.e("gf", String.valueOf(tt));
			String b = "";
			String a = Integer.toHexString(tt).toString();

			Log.e("gf", a);
			
			if (a.length() == 1) {
				b = "00,0" + a;
			} else if (a.length() == 2) {
				b = "00," + a;
			} else if (a.length() == 4) {
				b = a.substring(0, 2) + "," + a.substring(2, 4);
			} else if (a.length() == 3) {
				b = "0" + a.substring(0, 1) + "," + a.substring(1, 3);
			} else {
				b = "00,00";
			}
			
			Log.e("gf", b);

			String code = "";
			List<String> ll = FileUtil.DTC_list;
			boolean found = false;
			for (int di = 0; di < ll.size() - 1; di++) {

				String tmp = ll.get(di).toString();
				if (tmp.indexOf(b) > 0) {
					code = tmp.substring(tmp.length() - 5, tmp.length());
					LogUtil.i("code" + code);
					fault.code = code;
					di = ll.size();
					found = true;
				}
			}
			if (!found)
				code = "";

			String fileCN = "";
			String fileEN = "";
			
			fileCN = "malfunction_all.properties";
			fileEN = "malfunction_all_en.properties";
			String explain;
			
			LogUtil.i("file name CN:" + fileCN);
			LogUtil.i("file name EN:" + fileEN);
			String explainCN = "";
			String explainEN = "";
			explainCN = FileUtil.getInstance()
					.readProperties(context, fileCN, code).trim();
			explainEN = FileUtil.getInstance()
					.readProperties(context, fileEN, code).trim();
			LogUtil.i("explainCN:" + explainCN);
			LogUtil.i("explainEN:" + explainEN);

			
			Locale locale = context.getResources().getConfiguration().locale;
	        String language = locale.getLanguage();
	        if (language.endsWith("zh"))
	        	explain = explainCN;
	        else
	        	explain = explainEN;
			
			if (!Utils.isEmpty(explainCN)) {
				fault.meaning = explain;
			} else {
				fault.meaning = "";
			}

			faultInfos.add(fault);
		}
		return faultInfos;
	}
	
	public static List<FaultInfo> parseCommercial(Context context, String[] faults) {
		List<FaultInfo> fualtValues = new ArrayList<FaultInfo>();
		String fileCN = "malfunction_commercial.properties";
		LogUtil.i("file name CN:" + fileCN);
		int[] tt1 = new int[faults.length];
		for (int i = 0; i < faults.length; i++) {
			FaultInfo fualtValue = new FaultInfo();
			String explainCN = "";
			fualtValue.code = "0x"+faults[i];
			explainCN = FileUtil.getInstance()
					.readProperties(context, fileCN, fualtValue.code).trim();
			LogUtil.i("explainCN:" + explainCN);

			if (!Utils.isEmpty(explainCN)) {
				fualtValue.meaning = explainCN;
			} else {
				fualtValue.meaning = "";
			}
			fualtValues.add(fualtValue);
		}
			
		return fualtValues;
	}
}
