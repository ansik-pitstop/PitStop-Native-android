package com.pitstop.ui.my_appointments;

import static com.facebook.FacebookSdk.getApplicationContext;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.application.GlobalVariables;
import com.pitstop.database.LocalAppointmentStorage;
import com.pitstop.database.LocalDatabaseHelper;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerTempNetworkComponent;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.TempNetworkComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.get.GetAllAppointmentsUseCase;
import com.pitstop.models.Appointment;
import com.pitstop.models.Car;
import com.pitstop.network.RequestError;
import com.pitstop.ui.main_activity.MainActivity;
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.utils.NetworkHelper;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;



/**
 * Created by Matthew on 2017-05-02.
 */

public class MyAppointmentActivity extends AppCompatActivity {

    private final String TAG = MyAppointmentActivity.class.getSimpleName();

    private RecyclerView mApptsList;
    private AppointmentsAdapter mAppointmentAdapter;

    private GlobalApplication application;
    private NetworkHelper networkHelper;
    private LocalAppointmentStorage localAppointmentStorage;
    private MixpanelHelper mixpanelHelper;

    private Car dashboardCar;
    private List<Appointment> mAppts;
    private ProgressBar mLoadingSpinner;
    private UseCaseComponent useCaseComponent;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_appointments);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        TempNetworkComponent tempNetworkComponent = DaggerTempNetworkComponent.builder()
                .contextModule(new ContextModule(this))
                .build();

        useCaseComponent = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(this)).build();
        application = (GlobalApplication) getApplicationContext();
        mixpanelHelper = new MixpanelHelper(application);
        networkHelper = tempNetworkComponent.networkHelper();
        localAppointmentStorage = new LocalAppointmentStorage(LocalDatabaseHelper.getInstance(application));
        dashboardCar = getIntent().getParcelableExtra(MainActivity.CAR_EXTRA);
        mLoadingSpinner = (ProgressBar)findViewById(R.id.progress_spinner1);
        mAppts = new ArrayList<Appointment>();
        fetchAppointments();
        mixpanelHelper.trackViewAppeared("My Appointments");
    }

    private Integer getMainCarId() {
        return GlobalVariables.Companion.getMainCarId(getApplicationContext());
    }

    private void fetchAppointments(){
        mLoadingSpinner.setVisibility(View.VISIBLE);

        if(!networkHelper.isConnected(this)) {
             GrabLocal grabLocal = new GrabLocal();
             grabLocal.execute();
        }
        Integer carId = getMainCarId();
        if (carId == null) return;

        useCaseComponent.getAllAppointmentsUseCase().execute(carId, new GetAllAppointmentsUseCase.Callback() {
            @Override
            public void onGotAppointments(@NotNull List<? extends Appointment> appointments) {
                Log.d(TAG,"onGotAppointments() appointments: "+appointments);
                mAppts.clear();
                mAppts.addAll(appointments);
                setupList();
            }

            @Override
            public void onError(@NotNull RequestError error) {
                Log.d(TAG,"onError() error: "+error);
                if (error.getError().equals(RequestError.ERR_OFFLINE)){
                    Toast.makeText(MyAppointmentActivity.this
                            ,"Please connect to the internet to load appointments."
                            ,Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(MyAppointmentActivity.this
                            ,"An error occurred."
                            ,Toast.LENGTH_SHORT).show();
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
