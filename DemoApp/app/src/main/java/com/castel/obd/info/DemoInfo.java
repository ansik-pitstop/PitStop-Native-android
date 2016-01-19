package com.castel.obd.info;

public class DemoInfo {
	public int title;
	public Class<? extends android.app.Activity> demoClass;

	public DemoInfo(int title, Class<? extends android.app.Activity> demoClass) {
		this.title = title;
		this.demoClass = demoClass;
	}
}
