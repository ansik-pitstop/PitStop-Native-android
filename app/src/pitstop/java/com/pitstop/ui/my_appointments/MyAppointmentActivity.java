package com.pitstop.ui.my_appointments;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.database.LocalAppointmentAdapter;
import com.pitstop.models.Appointment;
import com.pitstop.models.Car;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.ui.MainActivity;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;



/**
 * Created by Matthew on 2017-05-02.
 */

public class MyAppointmentActivity extends AppCompatActivity {

    public static final String EXTRA_CAR = "extra_car";

    private RecyclerView mApptsList;
    private AppointmentsAdapter mAppointmentAdapter;

    private GlobalApplication application;
    private NetworkHelper networkHelper;
    private LocalAppointmentAdapter localAppointmentAdapter;

    private Car dashboardCar;
    private List<Appointment> mAppts;
    private ProgressBar mLoadingSpinner;




    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_appointments);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        application = (GlobalApplication) getApplicationContext();
        networkHelper = new NetworkHelper(application);
        localAppointmentAdapter = new LocalAppointmentAdapter(application);
        dashboardCar = getIntent().getParcelableExtra(EXTRA_CAR);
        mLoadingSpinner = (ProgressBar)findViewById(R.id.progress_spinner1);
        mAppts = new ArrayList<Appointment>();
        fetchAppointments();
    }

    private void fetchAppointments(){
        mLoadingSpinner.setVisibility(View.VISIBLE);

        if(!networkHelper.isConnected(this)) {
             GrabLocal grabLocal = new GrabLocal();
             grabLocal.execute();
        }
        networkHelper.getAppointments(dashboardCar.getId(),  new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                JSONObject jObject  = null;
                try {
                    mAppts.clear();
                    jObject = new JSONObject(response);
                    JSONArray responseArray = jObject.getJSONArray("results");
                    for(int i=0; i<responseArray.length(); i++){
                        JSONObject jAppoiontment = responseArray.getJSONObject(i);
                        Appointment addAppt = new Appointment();
                        addAppt.setDate(jAppoiontment.getString("appointmentDate"));
                        addAppt.setComments(jAppoiontment.getString("comments"));
                        addAppt.setState(jAppoiontment.getString("state"));
                        mAppts.add(addAppt);
                    }
                    setupList();
                    localAppointmentAdapter.deleteAllAppointments();
                    localAppointmentAdapter.storeAppointments(mAppts);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    private class GrabLocal extends AsyncTask<Void, Void, Void>{
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params) {
            mAppts = localAppointmentAdapter.getAllAppointments();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            setupList();

        }

    }

    private void setupList() {// find a way to run via async task or something
        mApptsList = (RecyclerView) findViewById(R.id.appointments_recyclerview);
        mAppointmentAdapter = new AppointmentsAdapter(this, mAppts);
        mApptsList.setAdapter(mAppointmentAdapter);
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mApptsList.setLayoutManager(linearLayoutManager);
        mLoadingSpinner.setVisibility(View.GONE);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == android.R.id.home){
            Intent intent = new Intent();
            intent.putExtra(MainActivity.REFRESH_FROM_SERVER, false);
            intent.putExtra(MainActivity.REMOVE_TUTORIAL_EXTRA, false);
            setResult(RESULT_CANCELED, intent);
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra(MainActivity.REFRESH_FROM_SERVER, false);
        intent.putExtra(MainActivity.REMOVE_TUTORIAL_EXTRA, false);
        setResult(RESULT_CANCELED, intent);
        super.onBackPressed();
    }
    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.activity_bottom_down_in, R.anim.activity_bottom_down_out);
    }


}
