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

    private boolean dateSelected;
    private boolean timeSelected;
    private String date;
    private String time;
    private String state;

    private Dealership localDealership;

    private List<CarIssue> issues;


    public ServiceFormPresenter(RequestServiceCallback callback, UseCaseComponent component){
        this.component = component;
        this.callback = callback;
    }


    public void subscribe(ServiceFormView view ){
        dateSelected = false;
        timeSelected = false;
        issues = new ArrayList<>();
        this.view = view;
    }

    public void timeButtonClicked(){
        if(!dateSelected){
            view.showReminder("Please select a date first");
            return;
        }
        view.toggleTimeList();
    }
    public void dateButtonClicked(){
        view.toggleCalender();
    }

    public void dateSelected(int year, int month, int dayOfMonth, CalendarView calendar){
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
                   view.setupTimeList(hours);
                   view.showLoading(false);
               }

               @Override
               public void onNoHoursAvailable(List<String> defaultHours) {
                   view.setupTimeList(defaultHours);
                   view.showLoading(false);
               }

                @Override
                public void onNotOpen() {
                    resetDate(calendar,"There are no times available for this date");
                }

                @Override
               public void onError() {
                    resetDate(calendar,"There was an error loading these times");
               }
            });
            finalizeDate(outDate);
        }catch (ParseException e){
            finalizeDate(date);
        }
    }
    private void finalizeDate(String sendDate){
        view.hideCalender();
        view.showDate(sendDate);
        date = sendDate;
        dateSelected = true;
        view.showTime("Tap to select time");
        timeSelected = false;
    }

    @Override
    public void onTimeClicked(String time) {
        view.showTime(time);
        view.hideTimeList();
        this.time = time;
        timeSelected = true;
    }

    private void resetDate( CalendarView calendar, String message){
        calendar.setDate(System.currentTimeMillis());
        view.showDate("Tap to select date");
        view.showCalender();
        view.showLoading(false);
        view.showReminder(message);
    }

    public void onSubmitClicked(){
        if(!dateSelected){
            view.showReminder("Please choose a date");
            return;
        }
        else if(!timeSelected){
            view.showReminder("Please choose a time");
            return;
        }
        String outDate = date+" "+time;
        component.getRequestServiceUseCase().execute(callback.checkTentative(), timeStamp(outDate), view.getComments(), new RequestServiceUseCase.Callback() {
            @Override
            public void onServicesRequested() {
               component.getAddServicesUseCase().execute(issues, new AddServicesUseCase.Callback() {
                   @Override
                   public void onServicesAdded() {
                       callback.finishActivity();
                   }

                   @Override
                   public void onError() {
                      view.toast("There was an error adding your services");
                   }
               });
            }

            @Override
            public void onError() {
              view.toast("There was an error requesting this service");
            }
        });
    }

    @Override
    public void onIssueClicked(CarIssue issue) {
        if(!issues.contains(issue)){
            issues.add(issue);
            view.setupSelectedIssues(issues);
        }
    }

    @Override
    public void onRemoveClicked(CarIssue issue) {
        issues.remove(issue);
        view.setupSelectedIssues(issues);
    }




    public void setIssues(){
        view.setupPresetIssues(view.getPresetList());
    }

    public void addButtonClicked(){
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
        if(car.getDealership() == null){return;}
        Dealership dealership = car.getDealership();
        if(dealership.getName() == null || dealership.getAddress() == null){return;}
        localDealership = car.getDealership();
        view.showShop(dealership.getName(),dealership.getAddress());
    }
}
