package com.pitstop.ui.my_garage;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.EventBus.EventType;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.get.GetUserCarUseCase;
import com.pitstop.interactors.get.GetUserShopsUseCase;
import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.network.RequestError;
import com.pitstop.ui.dashboard.DashboardPresenter;
import com.pitstop.ui.mainFragments.TabPresenter;
import com.pitstop.ui.service_request.RequestServiceActivity;
import com.pitstop.utils.MixpanelHelper;

import java.util.HashMap;
import java.util.List;

import io.smooch.core.User;
import io.smooch.ui.ConversationActivity;

import static com.pitstop.ui.main_activity.MainActivity.RC_REQUEST_SERVICE;

/**
 * Created by ishan on 2017-09-19.
 */

public class MyGaragePresenter extends TabPresenter<MyGarageView>{

    private static final String TAG = MyGaragePresenter.class.getSimpleName();

    private HashMap<String, Object> customProperties;

   // public final EventSource EVENT_SOURCE = new EventSourceImpl(EventSource.SOURCE_DASHBOARD);
    private UseCaseComponent useCaseComponent;
    private MixpanelHelper mixpanelHelper;
    private boolean updating = false;

    public MyGaragePresenter (UseCaseComponent useCaseComponent,
                               MixpanelHelper mixpanelHelper){
        this.useCaseComponent = useCaseComponent;
        this.mixpanelHelper = mixpanelHelper;

    }



    @Override
    public EventType[] getIgnoredEventTypes() {
        return new EventType[0];
    }

    @Override
    public void onAppStateChanged() {

    }

    @Override
    public EventSource getSourceType() {
        return null;
    }


    public void onMyAppointmentsClicked() {
        Log.d(TAG, "onMyAppointmentsClicked()");
        getView().openMyAppointments();

    }

    public void onRequestServiceClicked() {
        Log.d(TAG, "onRequestServiceClicked()");
        getView().openRequestService();
    }

    public void onMessageClicked() {
        Log.d(TAG, "onMessageClicked()");
        if (getView() == null||updating )return;
        updating = true;


        if (customProperties == null){
            useCaseComponent.getUserCarUseCase().execute(new GetUserCarUseCase.Callback() {
                @Override
                public void onCarRetrieved(Car car) {
                    updating = false;
                    //mixpanelHelper.trackFabClicked("Message");
                    customProperties = new HashMap<>();
                    customProperties.put("VIN", car.getVin());
                    customProperties.put("Car Make", car.getMake());
                    customProperties.put("Car Model", car.getModel());
                    customProperties.put("Car Year", car.getYear());
                    Log.i(TAG, car.getDealership().getEmail());
                    customProperties.put("Email", car.getDealership().getEmail());
                    User.getCurrentUser().addProperties(customProperties);

                    if (!getView().isUserNull()) {
                        customProperties.put("Phone", getView().getUserPhone());
                        User.getCurrentUser().setFirstName(getView().getUserFirstName());
                        User.getCurrentUser().setEmail(getView().getUserEmail());
                    }

                    getView().openSmooch();
                }

                @Override
                public void onNoCarSet() {
                    getView().toast("Please Select a Car");
                }

                @Override
                public void onError(RequestError error) {
                    getView().toast(error.getMessage());
                }
            });

        }

        else{
            updating = false;
            getView().openSmooch();
        }
    }

    public void onCallClicked() {
        if(getView() == null || updating) return;
        updating = true;
        useCaseComponent.getGetUserShopsUseCase().execute(new GetUserShopsUseCase.Callback() {
            @Override
            public void onShopGot(List<Dealership> dealerships) {
                if(getView() != null){
                    updating =false;
                    if (dealerships.size() == 0)
                        getView().toast("Please add a dealership");
                    else if (dealerships.size() == 1)
                        getView().callDealership(dealerships.get(0));
                    else
                        getView().showDealershipsDialog(dealerships);
                }
            }
            @Override
            public void onError(RequestError error) {
                updating = false;
                if(getView() != null){
                    getView().toast("There was an error loading your shops");
                }
            }
        });

    }
}
