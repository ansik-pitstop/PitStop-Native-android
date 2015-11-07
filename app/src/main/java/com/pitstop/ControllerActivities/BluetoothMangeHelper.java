package com.pitstop.ControllerActivities;

import android.content.Context;

import com.pitstop.R;
import com.castel.obd.bluetooth.BluetoothManage;
import com.castel.obd.info.DataPackageInfo;
import com.castel.obd.info.PIDInfo;
import com.castel.obd.info.ParameterPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;
import com.castel.obd.parse.PIDParse;
import com.castel.obd.util.ProgressDialogUtil;

import java.util.List;

public class BluetoothMangeHelper implements BluetoothManage.BluetoothDataListener{

    Context currContext;

    BluetoothMangeHelper(Context instance){
        currContext = instance;
    }

    public void startRequest(){
        BluetoothManage.getInstance(currContext).setBluetoothDataListener(this);
        BluetoothManage.getInstance(currContext)
                .obdGetParameter("2401");
    }

    public void getResponse(){

    }

    public void getTimer(){

    }

    @Override
    public void setCtrlResponse(ResponsePackageInfo responsePackageInfo) {

    }

    @Override
    public void setParamaterResponse(ResponsePackageInfo responsePackageInfo) {

    }

    @Override
    public void getParamaterData(ParameterPackageInfo parameterPackageInfo) {
        if (null == parameterPackageInfo) {
//            ProgressDialogUtil.dismiss();
//            tvResult.setText(R.string.fail);
            return;
        }

        if (null != parameterPackageInfo.value
                && null != parameterPackageInfo.value.get(0)
                && "2401".equals(parameterPackageInfo.value.get(0).tlvTag)) {
            BluetoothManage.getInstance(currContext).obdSetMonitor();
        } else {
//            ProgressDialogUtil.dismiss();
//            tvResult.setText(R.string.fail);
        }
    }

    @Override
    public void getIOData(DataPackageInfo dataPackage) {
        if (null == dataPackage) {
            return;
        }

        if (6 != dataPackage.result) {
            return;
        }

        if (null == dataPackage.obdData) {
//            tvResult.setText(R.string.fail);
            return;
        }

        ProgressDialogUtil.dismiss();

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(currContext.getString(R.string.data_monitor) + "\n");

        List<PIDInfo> lists = dataPackage.obdData;
        for (int i = 0; i < lists.size(); i++) {
            PIDInfo pidValue = PIDParse.parse(currContext,
                    lists.get(i));

            if (null == pidValue.meaning || null == pidValue.unit) {
                stringBuffer.append(currContext.getString(R.string.type) + ": 0x"
                        + pidValue.pidType + "\n");
                stringBuffer.append(currContext.getString(R.string.value) + ": "
                        + pidValue.value + "\n");
            } else {
                stringBuffer.append(currContext.getString(R.string.type) + ": 0x"
                        + pidValue.pidType + "(" + pidValue.meaning + ")\n");
                stringBuffer.append(currContext.getString(R.string.value) + ": "
                        + pidValue.value + "(" + pidValue.intValues[0]
                        + pidValue.unit + ")\n");
            }
        }
//        tvResult.setText(stringBuffer.toString());
    }

    @Override
    public void getBluetoothState(int state) {

    }
}
