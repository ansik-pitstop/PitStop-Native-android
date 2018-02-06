package com.pitstop.ui.service_request.view_fragment.main_from_view;


import android.support.v4.app.Fragment;
import android.util.Log;

import com.pitstop.EventBus.EventSource;
import com.pitstop.R;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.add.AddServicesUseCase;
import com.pitstop.interactors.get.GetDealershipWithCarIssuesUseCase;
import com.pitstop.interactors.get.GetShopHoursUseCase;
import com.pitstop.interactors.other.RequestServiceUseCase;
import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.network.RequestError;
import com.pitstop.ui.service_request.RequestServiceCallback;
import com.pitstop.utils.MixpanelHelper;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

import org.jetbrains.annotations.NotNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by Matthew on 2017-07-11.
 */

public class ServiceFormPresenter implements PresenterCallback{

    private final String TAG = getClass().getSimpleName();

    public static final String STATE_TENTATIVE = "tentative";
    public static final String STATE_REQUESTED = "requested";

    private ServiceFormView view;
    private RequestServiceCallback callback;
    private UseCaseComponent component;

    private Car dashCar;

    private boolean dateSelected;
    private boolean timeSelected;
    private String date;
    private String time;
    private String dateFormat;

    private Dealership dealership;

    private List<CarIssue> issues;

    private MixpanelHelper mixpanelHelper;


    public ServiceFormPresenter(RequestServiceCallback callback, UseCaseComponent component
            , MixpanelHelper mixpanelHelper){
        this.component = component;
        this.callback = callback;
        this.mixpanelHelper = mixpanelHelper;

    }

    public void subscribe(ServiceFormView view ){
        Log.d(TAG,"subscribe()");
        if(callback == null){return;}
        mixpanelHelper.trackViewAppeared("RequestServiceForm");
        dateSelected = false;
        timeSelected = false;
        issues = new ArrayList<>();
        this.view = view;
    }

    public void unsubscribe(){
        Log.d(TAG,"unsubscribe()");
        view = null;
    }

    void populateViews(){
        if(callback.checkTentative().equals(STATE_TENTATIVE)){
            setCommentHint("Salesperson");
        }
        view.showLoading(true);
        component.getDealershipWithCarIssuesUseCase().execute(new GetDealershipWithCarIssuesUseCase.Callback() {
            @Override
            public void onGotDealershipAndIssues(@NotNull Dealership dealership, @NotNull List<? extends CarIssue> carIssues) {
                Log.d(TAG,"getDealershipWithCarIssuesUseCase.onGotDealershipAndIssues()" +
                        " dealership: "+dealership+", issues: "+carIssues);
                if (view == null) return;
                view.showLoading(false);
                ServiceFormPresenter.this.dealership = dealership;
                view.showShop(dealership.getName(), dealership.getAddress());
                issues.clear();
                issues.addAll(carIssues);
                view.setupSelectedIssues(issues);
            }

            @Override
            public void onError(@NotNull RequestError error) {
                Log.d(TAG,"getDealershipWithCarIssuesUseCase.onError() err: "+error.getMessage());
                if (view != null){
                    view.showLoading(false);
                    if (error.getError().equals(RequestError.ERR_OFFLINE)){
                        view.toast("Error connecting to server, please check internet connection.");
                    }else{
                        view.toast("Unknown error occurred, please contact support.");
                    }
                    view.finish();
                }
            }
        });

        if(view == null || callback == null){return;}
        view.setupPresetIssues(view.getPresetList());
    }

    public void timeButtonClicked(){
        Log.d(TAG,"timeButtonClicked()");
        mixpanelHelper.trackButtonTapped("TimeMenuButton","RequestServiceForm");
        if(view == null || callback == null || dealership == null){return;}
        if(dealership.getName().equals("No Shop") || dealership == null){
            view.showReminder("Please set a shop for this car first");
            return;
        }
        if(!dateSelected){
            view.showReminder("Please select a date first");
            return;
        }
        view.toggleTimeList();
    }

    public void dateButtonClicked(){
        Log.d(TAG,"dateButtonClicked()");
        mixpanelHelper.trackButtonTapped("DateMenuButton","RequestServiceForm");
        if(view == null || callback == null || dealership == null){return;}
        if(dealership.getName().equals("No Shop") || dealership == null){
            view.showReminder("Please set a shop for this car first");
            return;
        }
        view.toggleCalender();
    }

    public void dateSelected(Date dateArg, int year, int month, int dayOfMonth, MaterialCalendarView calendarView){
        if(view == null || callback == null){return;}
        mixpanelHelper.trackButtonTapped("DateItemButton","RequestServiceForm");
        Log.d(TAG,"dateArg: "+dateArg);
        String date = year+"/"+month+"/"+dayOfMonth;
        this.dateFormat = date;
        SimpleDateFormat oldFormat = new SimpleDateFormat("yyyy/MM/dd");
        SimpleDateFormat newFormat = new SimpleDateFormat("EEEE dd MMM yyyy");
        SimpleDateFormat dayInWeek = new SimpleDateFormat("E");
        try{
            Date inDate = oldFormat.parse(date);
            String outDate = newFormat.format(inDate);
            String day = dayInWeek.format(inDate);
            view.showLoadingTime(true);
            component.getGetShopHoursUseCase().execute(year,month,dayOfMonth, dealership.getId()
                    , day, new GetShopHoursUseCase.Callback() {
               @Override
               public void onHoursGot(List<String> hours) {
                   if(view == null || callback == null){return;}
                   view.setupTimeList(hours);
                   view.showLoadingTime(false);
               }

               @Override
               public void onNoHoursAvailable(List<String> defaultHours) {
                   if(view == null || callback == null){return;}
                   view.setupTimeList(defaultHours);
                   view.showLoadingTime(false);
               }

                @Override
                public void onNotOpen() {
                    if(view == null || callback == null){return;}
                    resetDate(calendarView,((Fragment)view).getString(R.string.no_times_for_date));
                    view.showLoadingTime(false);
                }

                @Override
               public void onError(RequestError error) {
                    if(view == null || callback == null){return;}
                    resetDate(calendarView,((Fragment)view).getString(R.string.error_loading_times));
                    view.showLoadingTime(false);
               }
            });
            finalizeDate(outDate);
        }catch (ParseException e){
            finalizeDate(date);
        }
    }

    private void finalizeDate(String sendDate){
        Log.d(TAG,"finalizeDate() sendDate: "+sendDate);
        if(view == null || callback == null){return;}
        view.hideCalender();
        view.showDate(sendDate);
        date = sendDate;
        dateSelected = true;
        view.showTime(((Fragment)view).getString(R.string.select_appt_time));
        timeSelected = false;
    }

    @Override
    public void onTimeClicked(String time) {
        Log.d(TAG,"onTimeClicked time: "+time);
        mixpanelHelper.trackButtonTapped("TimeItemButton","RequestServiceForm");
        if(view == null || callback == null){return;}
        view.showTime(time);
        view.hideTimeList();
        this.time = time;
        timeSelected = true;
    }

    private void resetDate( MaterialCalendarView calendarView, String message){
        if(view == null || callback == null){return;}
        Calendar calendar = Calendar.getInstance();
        calendarView.setCurrentDate(calendar);
        view.showDate(((Fragment)view).getString(R.string.select_date));
        view.showCalender();
        view.showLoading(false);
        view.showReminder(message);
    }

    void onSubmitClicked(){
        Log.d(TAG,"onSubmitClicked()");
        mixpanelHelper.trackButtonTapped("SubmitButton","RequestServiceForm");
        if(view == null || callback == null || dealership == null){return;}

        if(dealership.getName().equals("No Shop") || dealership == null){
            view.showReminder("Please set a shop for this car first");
            return;
        }
        if(dealership.getEmail().equals("")){
            view.showReminder(((Fragment)view).getString(R.string.select_email_for_shop));
            return;
        }
        if(!dateSelected){
            view.showReminder(((Fragment)view).getString(R.string.select_date));
            return;
        }
        else if(!timeSelected){
            view.showReminder(((Fragment)view).getString(R.string.choose_time));
            return;
        }
        String outDate = dateFormat+" "+time.replace(".","");
        Log.d(TAG,"outDate: "+outDate);
        Date realDate;
        try{
            realDate = new SimpleDateFormat("yyyy/MM/dd hh:mm aa", Locale.CANADA).parse(outDate);
        }catch(ParseException e){
            e.printStackTrace();
            return;
        }
        view.disableButton(true);
        view.showLoading(true);
        component.getRequestServiceUseCase().execute(callback.checkTentative(), realDate
                , view.getComments(), new RequestServiceUseCase.Callback() {
                    @Override
                    public void onServicesRequested() {
                        Log.d(TAG,"onServiceRequested()");
                        ArrayList<CarIssue> toAdd = new ArrayList<>();
                        for (CarIssue c: issues){
                            if (c.getIssueType().equals(CarIssue.TYPE_PRESET))
                                toAdd.add(c);
                        }
                        if(view == null || callback == null){return;}
                       component.getAddServicesUseCase().execute(toAdd
                               , EventSource.SOURCE_REQUEST_SERVICE,new AddServicesUseCase.Callback() {
                           @Override
                           public void onServicesAdded() {
                               Log.d(TAG,"onServicesAdded()");
                               if(view == null || callback == null){return;}
                               view.showLoading(false);
                               view.disableButton(false);
                               callback.finishActivity();
                               view.toast("Service requested successfully.");
                           }

                           @Override
                           public void onError(RequestError error) {
                               Log.d(TAG,"onError() error: "+error.getMessage());
                               if(view == null || callback == null){return;}
                               view.showLoading(false);
                               view.disableButton(false);
                               view.toast(((Fragment)view).getString(R.string.add_service_error));
                           }
                       });
                    }

                    @Override
                    public void onError(RequestError error) {
                        Log.d(TAG,"onServiceRequested() error: "+error.getMessage());
                        if(view == null || callback == null){return;}
                        view.showLoading(false);
                        view.disableButton(false);
                      view.toast(((Fragment)view).getString(R.string.add_service_error));
                    }
                });
    }

    @Override
    public void onIssueClicked(CarIssue issue) {
        Log.d(TAG,"onIssueClicked() issue: "+issue);
        if(view == null || callback == null){return;}
        mixpanelHelper.trackButtonTapped("IssueItemButton","RequestServiceForm");
        if(!issues.contains(issue)){
            issues.add(issue);
            view.setupSelectedIssues(issues);
        }
    }

    void setCommentHint(String hint){
        Log.d(TAG,"setCommentHint() hint: "+hint);
        if(view == null || callback == null){return;}
        view.setCommentHint(hint);
    }

    @Override
    public void onRemoveClicked(CarIssue issue) {
        Log.d(TAG,"onRemoveClicked() ");
        if(view == null || callback == null){return;}
        mixpanelHelper.trackButtonTapped("RemoveIssueItemButton","RequestServiceForm");
        issues.remove(issue);
        view.setupSelectedIssues(issues);
    }

    void addButtonClicked(){
        Log.d(TAG,"addButtonClicked()");
        if(view == null || callback == null){return;}
        mixpanelHelper.trackButtonTapped("IssueMenuButton","RequestServiceForm");
        view.toggleServiceList();
    }
}
