package com.pitstop.ui.service_request.view_fragment.main_from_view;


import com.pitstop.dependency.UseCaseComponent;
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

    private ServiceFormView view;
    private RequestServiceCallback callback;
    private UseCaseComponent component;

    private boolean dateSelected;

    private Dealership localDealership;

    private List<CarIssue> issues;

    public ServiceFormPresenter(RequestServiceCallback callback, UseCaseComponent component){
        this.component = component;
        this.callback = callback;
    }


    public void subscribe(ServiceFormView view ){
        dateSelected = false;
        issues = new ArrayList<>();
        this.view = view;
    }

    public void timeButtonClicked(){
        if(!dateSelected){
            return;//add a prompt here
        }
        view.toggleTimeList();
    }
    public void dateButtonClicked(){
        view.toggleCalender();
    }

    public void dateSelected(int year, int month, int dayOfMonth){
        String date = year+"/"+month+"/"+dayOfMonth;
        SimpleDateFormat oldFormat = new SimpleDateFormat("yyyy/MM/dd");
        SimpleDateFormat newFormat = new SimpleDateFormat("EEEE dd MMM yyyy");
        try{
            Date inDate = oldFormat.parse(date);
            String outDate = newFormat.format(inDate);
            finalizeDate(outDate);
        }catch (ParseException e){
            finalizeDate(date);
        }
    }
    private void finalizeDate(String sendDate){
        view.hideCalender();
        view.showDate(sendDate);
        dateSelected = true;
    }

    @Override
    public void onTimeClicked(String time) {
        view.showTime(time);
        view.hideTimeList();
    }

    public void onSubmitClicked(){

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


    public void setTimes(int start, int end){
        List<String> times = new ArrayList<>();
        for(int i = start;i<end;i+=100){
            times.add(timeFormat(i));
            times.add(timeFormat(i+30));
        }
        view.setupTimeList(times);
    }

    public void setIssues(){
        view.setupPresetIssues(view.getPresetList());
    }

    public void addButtonClicked(){
        view.toggleServiceList();
    }

    private String timeFormat(int time){
        String date = Integer.toString(time);
        if(date.length()<4){
            date = "0"+date;
        }

        SimpleDateFormat in = new SimpleDateFormat("HHmm");
        SimpleDateFormat out = new SimpleDateFormat("hh:mm aa");
        try{
            Date inDate = in.parse(date);
            String outDate = out.format(inDate);
            return outDate;
        }catch (ParseException e){
            return date;
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
