package com.pitstop.ui.service_request.view_fragment.main_from_view;


import android.widget.CalendarView;

import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.AddServicesUseCase;
import com.pitstop.interactors.GetPitstopShopsUseCase;
import com.pitstop.interactors.GetShopHoursUseCase;
import com.pitstop.interactors.RequestServiceUseCase;
import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.ui.service_request.RequestServiceCallback;
import com.pitstop.utils.MixpanelHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Matthew on 2017-07-11.
 */

public class ServiceFormPresenter implements PresenterCallback{
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

    private Dealership localDealership;

    private List<CarIssue> issues;

    private MixpanelHelper mixpanelHelper;


    public ServiceFormPresenter(RequestServiceCallback callback, UseCaseComponent component, MixpanelHelper mixpanelHelper, Car dashCar){
        this.component = component;
        this.callback = callback;
        this.mixpanelHelper = mixpanelHelper;
        this.dashCar = dashCar;

    }

    public void subscribe(ServiceFormView view ){
        mixpanelHelper.trackViewAppeared("RequestServiceForm");
        dateSelected = false;
        timeSelected = false;
        issues = new ArrayList<>();
        this.view = view;
        if(callback.checkTentative().equals(STATE_TENTATIVE)){
            setCommentHint("Salesperson");
        }
        setDealer(dashCar);
        setIssues();
    }

    public void unsubscribe(){
        view = null;
    }

    public void timeButtonClicked(){
        if(view == null || callback == null){return;}
        mixpanelHelper.trackButtonTapped("TimeMenuButton","RequestServiceForm");
        if(!dateSelected){
            view.showReminder("Please select a date first");
            return;
        }
        view.toggleTimeList();
    }
    public void dateButtonClicked(){
        if(view == null || callback == null){return;}
        mixpanelHelper.trackButtonTapped("DateMenuButton","RequestServiceForm");
        view.toggleCalender();
    }

    public void dateSelected(int year, int month, int dayOfMonth, CalendarView calendar){
        if(view == null || callback == null){return;}
        mixpanelHelper.trackButtonTapped("DateItemButton","RequestServiceForm");
        String date = year+"/"+month+"/"+dayOfMonth;
        SimpleDateFormat oldFormat = new SimpleDateFormat("yyyy/MM/dd");
        SimpleDateFormat newFormat = new SimpleDateFormat("EEEE dd MMM yyyy");
        SimpleDateFormat dayInWeek = new SimpleDateFormat("u");
        try{
            Date inDate = oldFormat.parse(date);
            String outDate = newFormat.format(inDate);
            String day = dayInWeek.format(inDate);
            view.showLoading(true);
            component.getGetShopHoursUseCase().execute(year,month,dayOfMonth,localDealership.getId(), day, new GetShopHoursUseCase.Callback() {
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
                    resetDate(calendar,"There are no times available for this date");
                }

                @Override
               public void onError() {
                    if(view == null || callback == null){return;}
                    resetDate(calendar,"There was an error loading these times");
               }
            });
            finalizeDate(outDate);
        }catch (ParseException e){
            finalizeDate(date);
        }
    }
    private void finalizeDate(String sendDate){
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
        if(view == null || callback == null){return;}
        mixpanelHelper.trackButtonTapped("TimeItemButton","RequestServiceForm");
        view.showTime(time);
        view.hideTimeList();
        this.time = time;
        timeSelected = true;
    }

    private void resetDate( CalendarView calendar, String message){
        if(view == null || callback == null){return;}
        calendar.setDate(System.currentTimeMillis());
        view.showDate("Tap to select date");
        view.showCalender();
        view.showLoading(false);
        view.showReminder(message);
    }

    public void onSubmitClicked(){
        if(view == null || callback == null){return;}
        mixpanelHelper.trackButtonTapped("SubmitButton","RequestServiceForm");
        if(localDealership.getEmail().equals("")){
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
        component.getRequestServiceUseCase().execute(callback.checkTentative(), timeStamp(outDate), view.getComments(), new RequestServiceUseCase.Callback() {
            @Override
            public void onServicesRequested() {
                if(view == null || callback == null){return;}
               component.getAddServicesUseCase().execute(issues, new AddServicesUseCase.Callback() {
                   @Override
                   public void onServicesAdded() {
                       if(view == null || callback == null){return;}
                       view.disableButton(false);
                       callback.finishActivity();
                   }

                   @Override
                   public void onError() {
                       if(view == null || callback == null){return;}
                       view.disableButton(false);
                      view.toast("There was an error adding your services");
                   }
               });
            }

            @Override
            public void onError() {
                if(view == null || callback == null){return;}
                view.disableButton(false);
              view.toast("There was an error requesting this service");
            }
        });
    }

    @Override
    public void onIssueClicked(CarIssue issue) {
        if(view == null || callback == null){return;}
        mixpanelHelper.trackButtonTapped("IssueItemButton","RequestServiceForm");
        if(!issues.contains(issue)){
            issues.add(issue);
            view.setupSelectedIssues(issues);
        }
    }
    public void setCommentHint(String hint){
        if(view == null || callback == null){return;}
        view.setCommentHint(hint);
    }

    @Override
    public void onRemoveClicked(CarIssue issue) {
        if(view == null || callback == null){return;}
        mixpanelHelper.trackButtonTapped("RemoveIssueItemButton","RequestServiceForm");
        issues.remove(issue);
        view.setupSelectedIssues(issues);
    }




    public void setIssues(){
        if(view == null || callback == null){return;}
        view.setupPresetIssues(view.getPresetList());
    }

    public void addButtonClicked(){
        if(view == null || callback == null){return;}
        mixpanelHelper.trackButtonTapped("IssueMenuButton","RequestServiceForm");
        view.toggleServiceList();
    }


    public String timeStamp(String inTime){
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

    public void setDealer(Car car){
        if(view == null || callback == null){return;}
        if(car.getDealership() == null){return;}
        Dealership dealership = car.getDealership();
        if(dealership.getName() == null || dealership.getAddress() == null){return;}
        localDealership = car.getDealership();
        view.showShop(dealership.getName(),dealership.getAddress());
    }
}
