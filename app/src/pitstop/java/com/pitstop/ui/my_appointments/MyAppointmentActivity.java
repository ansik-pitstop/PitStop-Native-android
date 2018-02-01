package com.pitstop.ui.my_appointments;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.database.LocalAppointmentStorage;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerTempNetworkComponent;
import com.pitstop.dependency.TempNetworkComponent;
import com.pitstop.models.Appointment;
import com.pitstop.models.Car;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.ui.main_activity.MainActivity;
import com.pitstop.utils.MixpanelHelper;
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

    private RecyclerView mApptsList;
    private AppointmentsAdapter mAppointmentAdapter;

    private GlobalApplication application;
    private NetworkHelper networkHelper;
    private LocalAppointmentStorage localAppointmentStorage;
    private MixpanelHelper mixpanelHelper;

    private Car dashboardCar;
    private List<Appointment> mAppts;
    private ProgressBar mLoadingSpinner;




    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_appointments);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        TempNetworkComponent tempNetworkComponent = DaggerTempNetworkComponent.builder()
                .contextModule(new ContextModule(this))
                .build();

        application = (GlobalApplication) getApplicationContext();
        mixpanelHelper = new MixpanelHelper(application);
        networkHelper = tempNetworkComponent.networkHelper();
        localAppointmentStorage = new LocalAppointmentStorage(application);
        dashboardCar = getIntent().getParcelableExtra(MainActivity.CAR_EXTRA);
        mLoadingSpinner = (ProgressBar)findViewById(R.id.progress_spinner1);
        mAppts = new ArrayList<Appointment>();
        fetchAppointments();
        mixpanelHelper.trackViewAppeared("My Appointments");
    }

    private void fetchAppointments(){
        mLoadingSpinner.setVisibility(View.VISIBLE);

        if(!networkHelper.isConnected(this)) {
             GrabLocal grabLocal = new GrabLocal();
             grabLocal.execute();
        }

        //TODO: refactor
        networkHelper.getAppointments(dashboardCar.getId(),  new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                //Load locally
                if (requestError != null && requestError.getError().equals(RequestError.ERR_OFFLINE)){
                    mAppts = localAppointmentStorage.getAllAppointments();
                    setupList();
                    Toast.makeText(MyAppointmentActivity.this
                            ,"Please connect to the internet to sync your appointments."
                            ,Toast.LENGTH_SHORT).show();
                }
                //Load from remote server
                else if (requestError == null){
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
                        localAppointmentStorage.deleteAllAppointments();
                        localAppointmentStorage.storeAppointments(mAppts);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }else{
                    Toast.makeText(MyAppointmentActivity.this
                            ,"An error occurred."
                            ,Toast.LENGTH_SHORT).show();
                    finish();

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
            mAppts = localAppointmentStorage.getAllAppointments();
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
            mixpanelHelper.trackButtonTapped("Back","My Appointments");
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void finish() {
        super.finish();
    }


}
