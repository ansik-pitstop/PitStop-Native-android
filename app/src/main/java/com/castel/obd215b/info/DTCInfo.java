package com.castel.obd215b.info;

import java.io.Serializable;
import java.util.Arrays;

public class DTCInfo implements Serializable {

	public String terminalId;
	public int dtcType;
	public String diagnosisProtocol;
	public int dtcNumber;
	public String[] dtcs;

	@Override
	public String toString() {
		return "DTCInfo [terminalId=" + terminalId + ", dtcType=" + dtcType + ", diagnosisProtocol="
				+ diagnosisProtocol + ", dtcNumber=" + dtcNumber + ", dtcs="
				+ Arrays.toString(dtcs) + "]";
	}

}
