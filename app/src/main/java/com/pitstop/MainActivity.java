package com.pitstop;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.castel.obd.bluetooth.BluetoothManage;
import com.castel.obd.info.DataPackageInfo;
import com.castel.obd.info.ParameterPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;
import com.pitstop.background.BluetoothAutoConnectService;
import com.pitstop.database.LocalDataRetriever;

public class MainActivity extends AppCompatActivity implements BluetoothManage.BluetoothDataListener {
    public static Intent serviceIntent;


    private BluetoothAutoConnectService service;
    /** Callbacks for service binding, passed to bindService() */
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service1) {
            // cast the IBinder and get MyService instance
            BluetoothAutoConnectService.BluetoothBinder binder = (BluetoothAutoConnectService.BluetoothBinder) service1;
            service = binder.getService();
            service.setCallbacks(MainActivity.this); // register
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        serviceIntent= new Intent(MainActivity.this, BluetoothAutoConnectService.class);
        startService(new Intent(MainActivity.this, BluetoothAutoConnectService.class));
        setContentView(R.layout.activity_main);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);


    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        if(id==R.id.refresh){
            refreshDatabase();
        }
        if(id==R.id.action_connect){
            Intent i = new Intent(MainActivity.this, ReceiveDebugActivity.class);
            startActivity(i);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode==AddCarActivity.RESULT_ADDED){
            refreshDatabase();
        }
    }

    private void refreshDatabase() {
        final LocalDataRetriever ldr = new LocalDataRetriever(this);
        SharedPreferences settings = getSharedPreferences(MainActivityFragment.pfName, this.MODE_PRIVATE);
        String objectID = settings.getString(MainActivityFragment.pfCodeForObjectID, "NA");
        ldr.deleteData("Cars", "owner", objectID);
        ((MainActivityFragment)getSupportFragmentManager().findFragmentById(R.id.fragment_main)).setUp();
    }

    @Override
    public void onBackPressed() {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);

    }
    public void addCar(View view) {
        Intent intent = new Intent(MainActivity.this, AddCarActivity.class);
        startActivity(intent);
    }

    @Override
    public void getBluetoothState(int state) {

    }

    @Override
    public void setCtrlResponse(ResponsePackageInfo responsePackageInfo) {

    }

    @Override
    public void setParamaterResponse(ResponsePackageInfo responsePackageInfo) {

    }

    @Override
    public void getParamaterData(ParameterPackageInfo parameterPackageInfo) {

    }

    @Override
    public void getIOData(DataPackageInfo dataPackageInfo) {
        ((MainActivityFragment)getSupportFragmentManager().findFragmentById(R.id.fragment_main)).indicateConnected(dataPackageInfo.deviceId);
    }
}
