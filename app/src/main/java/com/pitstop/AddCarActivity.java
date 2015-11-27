package com.pitstop;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.castel.obd.bluetooth.BluetoothManage;
import com.castel.obd.info.DataPackageInfo;
import com.castel.obd.info.ParameterInfo;
import com.castel.obd.info.ParameterPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;
import com.castel.obd.util.LogUtil;
import com.parse.ConfigCallback;
import com.parse.ParseConfig;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;


public class AddCarActivity extends AppCompatActivity implements BluetoothManage.BluetoothDataListener{

    private String VIN = "", scannerID = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_car);

        BluetoothManage.getInstance(this).setBluetoothDataListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_add_car, menu);
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

        return super.onOptionsItemSelected(item);
    }

    public void getVIN(View view) {
        if(BluetoothManage.getInstance(AddCarActivity.this).getState() != BluetoothManage.CONNECTED) {
            BluetoothManage.getInstance(AddCarActivity.this).connectBluetooth();
            findViewById(R.id.loading).setVisibility(View.VISIBLE);
        }else {
            BluetoothManage.getInstance(this).obdGetParameter("2201");
            findViewById(R.id.loading).setVisibility(View.VISIBLE);
        }
        //VIN = "YS3FD75Y746007819";
    }

    private void makeCar() {
        final String[] mashapeKey = {""};
        ParseConfig.getInBackground(new ConfigCallback() {
            @Override
            public void done(ParseConfig config, ParseException e) {

                new CallMashapeAsync().execute(config.getString("MashapeAPIKey"));
            }
        });
    }

    @Override
    public void getBluetoothState(int state) {
        findViewById(R.id.loading).setVisibility(View.GONE);
    }

    @Override
    public void setCtrlResponse(ResponsePackageInfo responsePackageInfo) {

    }

    @Override
    public void setParamaterResponse(ResponsePackageInfo responsePackageInfo) {
    }

    @Override
    public void getParamaterData(ParameterPackageInfo parameterPackageInfo) {
        findViewById(R.id.loading).setVisibility(View.GONE);
        LogUtil.i("parameterPackage.size():"
                + parameterPackageInfo.value.size());

        List<ParameterInfo> parameterValues = parameterPackageInfo.value;
        VIN = parameterValues.get(0).value;
        scannerID = parameterPackageInfo.deviceId;
        ((TextView) findViewById(R.id.cardetails)).setText(VIN);
        //makeCar();
    }

    @Override
    public void getIOData(DataPackageInfo dataPackageInfo) {

    }


    private class CallMashapeAsync extends AsyncTask<String, Void,Void>{

        protected Void doInBackground(String... msg) {

            try {

                StringBuilder response  = new StringBuilder();
                URL url = new URL("https://vindecoder.p.mashape.com/decode_vin?vin="+VIN);
                // Starts the query

                HttpURLConnection httpconn = (HttpURLConnection)url.openConnection();
                httpconn.addRequestProperty("Content-Type", "application/x-zip");
                httpconn.addRequestProperty("X-Mashape-Key", msg[0]);
                httpconn.addRequestProperty("Accept", "application/json");
                if (httpconn.getResponseCode() == HttpURLConnection.HTTP_OK)
                {
                    BufferedReader input = new BufferedReader(new InputStreamReader(httpconn.getInputStream()),8192);
                    String strLine = null;
                    while ((strLine = input.readLine()) != null)
                    {
                        response.append(strLine);
                    }
                    input.close();
                }
                if(new JSONObject(response.toString()).getBoolean("success")) {
                    JSONObject jsonObject = new JSONObject(response.toString()).getJSONObject("specification");

                    //Make Car
                    ParseObject newCar = new ParseObject("Car");
                    newCar.put("VIN", VIN);
                    newCar.put("year", jsonObject.getInt("year"));
                    newCar.put("model", jsonObject.getString("model"));
                    newCar.put("make", jsonObject.getString("make"));
                    newCar.put("tank_size", jsonObject.getString("tank_size"));
                    newCar.put("trim_level", jsonObject.getString("trim_level"));
                    newCar.put("engine", jsonObject.getString("engine"));
                    newCar.put("city_mileage", jsonObject.getString("city_mileage"));
                    newCar.put("highway_mileage", jsonObject.getString("highway_mileage"));
                    newCar.put("scannerId", scannerID);
                    newCar.put("owner", ParseUser.getCurrentUser().getObjectId());
                    newCar.put("user", ParseUser.getCurrentUser().getObjectId());
                    newCar.put("baseMileage", 10000);
                    newCar.saveEventually(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e == null) {
                                finish();
                            } else {
                                Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }else{
                    Toast.makeText(getApplicationContext(),"Failed to find by VIN",Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
