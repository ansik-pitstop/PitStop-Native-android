package com.pitstop.ui;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import com.castel.obd.bluetooth.ObdManager;
import com.castel.obd.info.DataPackageInfo;
import com.castel.obd.info.LoginPackageInfo;
import com.castel.obd.info.ParameterPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;
import com.pitstop.R;
import com.pitstop.adapters.TestActionAdapter;
import com.pitstop.models.TestAction;
import com.pitstop.utils.ShadowTransformer;

import java.util.ArrayList;

/**
 * Created by BEN! on 15/9/2016.
 */
public class MainActivity extends AppCompatActivity implements ObdManager.IBluetoothDataListener {

    private ViewPager viewPager;
    private TestActionAdapter adapter;
    private ShadowTransformer shadowTransformer;

    ArrayList<TestAction> testActions = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeTestActions();

        viewPager = (ViewPager) findViewById(R.id.viewPager);
        adapter = new TestActionAdapter(this, testActions);
        shadowTransformer = new ShadowTransformer(viewPager, adapter);

        shadowTransformer.enableScaling(true);

        viewPager.setPageTransformer(false, shadowTransformer);
        viewPager.setAdapter(adapter);
    }

    @Override
    public void setCtrlResponse(ResponsePackageInfo responsePackageInfo) {

    }

    @Override
    public void getIOData(DataPackageInfo dataPackageInfo) {

    }

    @Override
    public void setParameterResponse(ResponsePackageInfo responsePackageInfo) {

    }

    @Override
    public void getParameterData(ParameterPackageInfo parameterPackageInfo) {

    }

    @Override
    public void getBluetoothState(int state) {

    }

    @Override
    public void deviceLogin(LoginPackageInfo loginPackageInfo) {

    }

    private void initializeTestActions() {
        testActions.add(new TestAction("Connect", "Connect to the device.", TestAction.Type.CONNECT));
        testActions.add(new TestAction("Device Time", "The device time must be properly set before receiving data. " +
                "This may take up to a minute.", TestAction.Type.CHECK_TIME));
        testActions.add(new TestAction("Sensor Data", "Verify real-time sensor data is working. Received data will be displayed.", TestAction.Type.PID));
        testActions.add(new TestAction("Engine Codes", "Check the vehicle for any engine codes.", TestAction.Type.DTC));
        testActions.add(new TestAction("Get VIN", "Verify the VIN is retrievable.", TestAction.Type.VIN));
        testActions.add(new TestAction("Reset", "Reset the device.", TestAction.Type.RESET));
    }
}
