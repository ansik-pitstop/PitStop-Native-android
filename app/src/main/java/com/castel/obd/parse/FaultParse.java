package com.castel.obd.parse;

import java.util.ArrayList;
import java.util.List;

import com.castel.obd.data.OBDInfoSP;
import com.castel.obd.info.FualtInfo;
import com.castel.obd.util.LogUtil;
import com.castel.obd.util.FileUtil;
import com.castel.obd.util.Utils;

import android.content.Context;
import android.util.Log;

public class FaultParse {

	public static List<FualtInfo> parse(Context context, String[] faults) {
		int[] tt1 = new int[faults.length];
		for (int i = 0; i < faults.length; i++) {
			tt1[i] = Integer.parseInt(faults[i], 16);
		}

		FileUtil.getInstance().readDTC(context);

		List<FualtInfo> fualtValues = new ArrayList<FualtInfo>();
		for (int ii = 0; ii < tt1.length; ii++) {
			FualtInfo fualtValue = new FualtInfo();
			int tt = tt1[ii];
			Log.v("tt", String.valueOf(tt));
			String b = "";
			String a = Integer.toHexString(tt).toString();

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

			String code = "";
			List<String> ll = FileUtil.DTC_list;
			boolean found = false;
			for (int di = 0; di < ll.size() - 1; di++) {

				String tmp = ll.get(di).toString();
				if (tmp.indexOf(b) > 0) {
					code = tmp.substring(tmp.length() - 5, tmp.length());
					LogUtil.i("code" + code);
					fualtValue.code = code;
					di = ll.size();
					found = true;
				}
			}
			if (!found)
				code = "";

			String fileCN = "";
			String fileEN = "";
			int size = 0;
			if (code.startsWith("P")) {
				size = Integer.parseInt(code.substring(1, code.length()));
			}
			if (size < 1000) {
				fileCN = "malfunction_all.properties";
				fileEN = "malfunction_all_en.properties";
			} else {
				int temp_code = OBDInfoSP.getCarType(context);
				if (temp_code != 21) {
					fileCN = "malfunction_" + temp_code + ".properties";
					fileEN = "malfunction_" + temp_code + "_en.properties";
				} else {
					fileCN = "malfunction_all.properties";
					fileEN = "malfunction_all_en.properties";
				}

			}
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
