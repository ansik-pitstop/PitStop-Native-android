package com.castel.obd215b.info;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PIDInfo implements Serializable {
	public String terminalId;
	public String diagnoseProtocol;
	public int pidNumber;
	public List<String> pids = new ArrayList<String>();
	public List<String> pidValues = new ArrayList<String>();
}
