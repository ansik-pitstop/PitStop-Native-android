package com.castel.obd.util;

import com.pitstop.R;

import android.app.ProgressDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

public class ProgressDialogUtil {
	private static ProgressDialog mProgressDialog;

	public static void show(Context context) {
		if(null != mProgressDialog && mProgressDialog.isShowing()){
			return;
		}
		mProgressDialog = new ProgressDialog(context);
		mProgressDialog.show();

		View view = LayoutInflater.from(context).inflate(
				R.layout.layout_progress_dialog, null);
		mProgressDialog.setContentView(view);
		mProgressDialog.setCanceledOnTouchOutside(false);
	}

	public static void dismiss() {
		if (mProgressDialog != null && mProgressDialog.isShowing()) {
			mProgressDialog.dismiss();
			mProgressDialog = null;
		}
	}
}
