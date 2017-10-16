package com.pitstop.ui.service_request.view_fragment.main_from_view;


import android.util.Log;

import com.pitstop.EventBus.EventSource;
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
    private String state;

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
                //show error
            }
        });

        if(view == null || callback == null){return;}
        view.setupPresetIssues(view.getPresetList());

        if(callback.getIssue() != null){
            onIssueClicked(callback.getIssue());
        }
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

    public void dateSelected(int year, int month, int dayOfMonth, MaterialCalendarView calendarView){
        Log.d(TAG,"dateSelected() year: "+year+", month: "+month+", dayOfMonth: "+dayOfMonth);
        mixpanelHelper.trackButtonTapped("DateItemButton","RequestServiceForm");
        if(view == null || callback == null || dealership == null){return;}

        String date = year+"/"+month+"/"+dayOfMonth;
        SimpleDateFormat oldFormat = new SimpleDateFormat("yyyy/MM/dd");
        SimpleDateFormat newFormat = new SimpleDateFormat("EEEE dd MMM yyyy");
        SimpleDateFormat dayInWeek = new SimpleDateFormat("E");
        try{
            Date inDate = oldFormat.parse(date);
            String outDate = newFormat.format(inDate);
            String day = dayInWeek.format(inDate);
            view.showLoading(true);
            component.getGetShopHoursUseCase().execute(year,month,dayOfMonth, dealership.getId()
                    , day, new GetShopHoursUseCase.Callback() {
               @Override
               public void onHoursGot(List<String> hours) {
                   if(view == null || callback == null){return;}
                   view.setupTimeList(hours);
                   view.showLoading(false);
               }

               @Override
               public void onNoHoursAvailable(List<String> defaultHours) {
                   if(view == null || callback == null){return;}
                   view.setupTimeList(defaultHours);
                   view.showLoading(false);
               }

                @Override
                public void onNotOpen() {
                    if(view == null || callback == null){return;}
                    resetDate(calendarView,"There are no times available for this date");
                    view.showLoading(false);
                }

                @Override
               public void onError(RequestError error) {
                    if(view == null || callback == null){return;}
                    resetDate(calendarView,"There was an error loading these times");
                    view.showLoading(false);
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
        view.showTime("Tap to select time");
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
        view.showDate("Tap to select date");
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
            view.showReminder("Please set an email for this shop");
            return;
        }
        if(!dateSelected){
            view.showReminder("Please choose a date");
            return;
        }
        else if(!timeSelected){
            view.showReminder("Please choose a time");
            return;
        }
        String outDate = date+" "+time;
        view.disableButton(true);
        component.getRequestServiceUseCase().execute(callback.checkTentative(), timeStamp(outDate)
                , view.getComments(), new RequestServiceUseCase.Callback() {
                    @Override
                    public void onServicesRequested() {
                        Log.d(TAG,"onServiceRequested()");
                        if(view == null || callback == null){return;}
                        if(callback.getIssue()!= null){
                            view.disableButton(false);
                            callback.finishActivity();
                            view.toast("Service requested successfully.");
                            return;
                        }
                       component.getAddServicesUseCase().execute(issues
                               , EventSource.SOURCE_REQUEST_SERVICE,new AddServicesUseCase.Callback() {
                           @Override
                           public void onServicesAdded() {
                               Log.d(TAG,"onServicesAdded()");
                               if(view == null || callback == null){return;}
                               view.disableButton(false);
                               callback.finishActivity();
                               view.toast("Service requested successfully.");
                           }

                           @Override
                           public void onError(RequestError error) {
                               Log.d(TAG,"onError() error: "+error.getMessage());
                               if(view == null || callback == null){return;}
                               view.disableButton(false);
                               view.toast("There was an error adding your services");
                           }
                       });
                    }

                    @Override
                    public void onError(RequestError error) {
                        Log.d(TAG,"onServiceRequested() error: "+error.getMessage());
                        if(view == null || callback == null){return;}
                        view.disableButton(false);
                      view.toast("There was an error requesting this service");
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


    String timeStamp(String inTime){
        Log.d(TAG,"timeStamp() inTime(): "+inTime);
        SimpleDateFormat inFormat = new SimpleDateFormat("EEEE dd MMM yyyy hh:mm aa");
        SimpleDateFormat outFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        try{
            Date date = inFormat.parse(inTime);
            String out = outFormat.format(date);
            return out;
        }catch (ParseException e){
            return "0";
        }
    }

}
