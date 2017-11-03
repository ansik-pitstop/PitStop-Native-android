package com.pitstop.ui.services.custom_service.view_fragments;

import android.app.Fragment;
import android.content.res.Resources;

import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.R;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.add.AddCustomServiceUseCase;
import com.pitstop.interactors.other.MarkServiceDoneUseCase;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.models.service.CustomIssueListItem;
import com.pitstop.network.RequestError;
import com.pitstop.ui.services.custom_service.CustomServiceActivityCallback;
import com.pitstop.utils.MixpanelHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Matt on 2017-07-25.
 */

public class ServiceFormPresenter implements PresenterCallback {

    private ServiceFormView view;

    private CustomServiceActivityCallback callback;
    private UseCaseComponent component;
    private EventSource eventSource;

    private CarIssue issue;

    private MixpanelHelper mixpanelHelper;

    private final String MIX_VIEW = "CustomServices";


    public ServiceFormPresenter(UseCaseComponent component,CustomServiceActivityCallback callback, MixpanelHelper mixpanelHelper){
        this.component = component;
        this.callback = callback;
        this.mixpanelHelper = mixpanelHelper;

        if (callback.getHistorical())
            eventSource = new EventSourceImpl(EventSource.SOURCE_SERVICES_HISTORY);
        else
            eventSource = new EventSourceImpl(EventSource.SOURCE_SERVICES_CURRENT);
    }
    public void subscribe(ServiceFormView view){
        if(view == null){return;}
        mixpanelHelper.trackViewAppeared(MIX_VIEW);
        this.view = view;
        setActionList();
        setPartNameList();
        setPriorityList();
    }
    public void unsubscribe(){
        view = null;
    }


    public void setActionList(){
        if(view == null || callback == null){return;}
        List<CustomIssueListItem> items = new ArrayList<>();
        CustomIssueListItem item = new CustomIssueListItem();
        item.setText(((android.app.Fragment)view).getString(R.string.preset_issue_service_replace));
        item.setCardColor("#194D85");
        item.setKey(CustomServiceListAdapter.SERVICE_ACTION_KEY);
        items.add(item);

        item = new CustomIssueListItem();
        item.setText(((Fragment)view).getString(R.string.preset_issue_service_rotate));
        item.setCardColor("#256fc0");
        item.setKey(CustomServiceListAdapter.SERVICE_ACTION_KEY);
        items.add(item);

        item = new CustomIssueListItem();
        item.setText(((Fragment)view).getString(R.string.preset_issue_service_perform));
        item.setCardColor("#2b83e2");
        item.setKey(CustomServiceListAdapter.SERVICE_ACTION_KEY);
        items.add(item);

        item = new CustomIssueListItem();
        item.setText(((Fragment)view).getString(R.string.preset_issue_service_inspect));
        item.setCardColor("#5FA2EC");
        item.setKey(CustomServiceListAdapter.SERVICE_ACTION_KEY);
        items.add(item);

        item = new CustomIssueListItem();
        item.setText(((Fragment)view).getString(R.string.preset_issue_service_other));
        item.setCardColor("#8FBDF2");
        item.setKey(CustomServiceListAdapter.SERVICE_ACTION_OTHER_KEY);
        items.add(item);

        view.setActionList(items);
    }

    public void setPartNameList(){
        if(view == null || callback == null){return;}
        List<CustomIssueListItem> items = new ArrayList<>();
        CustomIssueListItem item = new CustomIssueListItem();
        item.setText(((Fragment)view).getString(R.string.air_filter));
        item.setCardColor("#194D85");
        item.setKey(CustomServiceListAdapter.SERVICE_PART_KEY);
        items.add(item);

        item = new CustomIssueListItem();
        item.setText(((Fragment)view).getString(R.string.brakes));
        item.setCardColor("#256fc0");
        item.setKey(CustomServiceListAdapter.SERVICE_PART_KEY);
        items.add(item);

        item = new CustomIssueListItem();
        item.setText(((Fragment)view).getString(R.string.coolant));
        item.setCardColor("#2b83e2");
        item.setKey(CustomServiceListAdapter.SERVICE_PART_KEY);
        items.add(item);

        item = new CustomIssueListItem();
        item.setText(((Fragment)view).getString(R.string.exhaust));
        item.setCardColor("#5FA2EC");
        item.setKey(CustomServiceListAdapter.SERVICE_PART_KEY);
        items.add(item);

        item = new CustomIssueListItem();
        item.setText(((Fragment)view).getString(R.string.lights));
        item.setCardColor("#8FBDF2");
        item.setKey(CustomServiceListAdapter.SERVICE_PART_KEY);
        items.add(item);

        item = new CustomIssueListItem();
        item.setText(((Fragment)view).getString(R.string.spark_plugs));
        item.setCardColor("#194D85");
        item.setKey(CustomServiceListAdapter.SERVICE_PART_KEY);
        items.add(item);

        item = new CustomIssueListItem();
        item.setText(((Fragment)view).getString(R.string.suspension));
        item.setCardColor("#256fc0");
        item.setKey(CustomServiceListAdapter.SERVICE_PART_KEY);
        items.add(item);

        item = new CustomIssueListItem();
        item.setText(((Fragment)view).getString(R.string.tires));
        item.setCardColor("#2b83e2");
        item.setKey(CustomServiceListAdapter.SERVICE_PART_KEY);
        items.add(item);

        item = new CustomIssueListItem();
        item.setText(((Fragment)view).getString(R.string.transmission));
        item.setCardColor("#5FA2EC");
        item.setKey(CustomServiceListAdapter.SERVICE_PART_KEY);
        items.add(item);

        item = new CustomIssueListItem();
        item.setText(((Fragment)view).getString(R.string.preset_issue_service_other));
        item.setCardColor("#8FBDF2");
        item.setKey(CustomServiceListAdapter.SERVICE_PART_OTHER_KEY);
        items.add(item);

        view.setParNameList(items);
    }

    public void setPriorityList(){
        if(view == null || callback == null){return;}
        List<CustomIssueListItem> items = new ArrayList<>();
        CustomIssueListItem item = new CustomIssueListItem();
        item.setText(((Fragment)view).getString(R.string.severity_indicator_low));
        item.setCardColor("#2b83e2");
        item.setKey(CustomServiceListAdapter.SERVICE_PRIORITY_KEY);
        items.add(item);

        item = new CustomIssueListItem();
        item.setText(((Fragment)view).getString(R.string.severity_indicator_medium));
        item.setCardColor("#FFCE54");
        item.setKey(CustomServiceListAdapter.SERVICE_PRIORITY_KEY);
        items.add(item);

        item = new CustomIssueListItem();
        item.setText(((Fragment)view).getString(R.string.severity_indicator_high));
        item.setCardColor("#FF6F00");
        item.setKey(CustomServiceListAdapter.SERVICE_PRIORITY_KEY);
        items.add(item);

        item = new CustomIssueListItem();
        item.setText(((Fragment)view).getString(R.string.severity_indicator_critical));
        item.setCardColor("#FF0000");
        item.setKey(CustomServiceListAdapter.SERVICE_PRIORITY_KEY);
        items.add(item);

        view.setPriorityList(items);

    }

    public void onPriorityClicked(){
        if(view == null || callback == null){return;}
        mixpanelHelper.trackButtonTapped("Priority",MIX_VIEW);
        view.togglePriorityList();
    }

    public void onPartNameClicked(){
        if(view == null || callback == null){return;}
        mixpanelHelper.trackButtonTapped("PartName",MIX_VIEW);
        view.togglePartNameList();
    }

    public void onActionClicked(){
        if(view == null || callback == null){return;}
        mixpanelHelper.trackButtonTapped("Action",MIX_VIEW);
        view.toggleActionList();
    }

    @Override
    public void onActionItemClicked(CustomIssueListItem item) {
        if(view == null || callback == null){return;}
        mixpanelHelper.trackButtonTapped("ActionItem",MIX_VIEW);
        view.showActionText(item);
        view.toggleActionList();
    }

    @Override
    public void onPartNameItemClicked(CustomIssueListItem item) {
        if(view == null || callback == null){return;}
        mixpanelHelper.trackButtonTapped("PartNameItem",MIX_VIEW);
        view.showPartNameText(item);
        view.togglePartNameList();
    }

    @Override
    public void onPriorityItemClicked(CustomIssueListItem item) {
        if(view == null || callback == null){return;}
        mixpanelHelper.trackButtonTapped("PriorityItem",MIX_VIEW);
        view.showPriorityText(item);
        view.togglePriorityList();
    }

    @Override
    public void onActionOther() {
        if(view == null || callback == null){return;}
        mixpanelHelper.trackButtonTapped("ActionOther",MIX_VIEW);
        view.showActionText();
        view.toggleActionList();
    }

    @Override
    public void onPartNameOther() {
        if(view == null || callback == null){return;}
        mixpanelHelper.trackButtonTapped("PartNameOther",MIX_VIEW);
        view.showPartNameText();
        view.togglePriorityList();
    }

    public void onCreateButton(){
        if(view == null || callback == null){return;}
        mixpanelHelper.trackButtonTapped("Create",MIX_VIEW);
        view.disableCreateButton(false);
        CarIssue customIssue = new CarIssue();
        if(view.getPartName().equals("")){
            view.showReminder(((Fragment)view).getString(R.string.what_car_part));
            view.disableCreateButton(true);
            return;
        }else if(view.getPriority().equals("")){
            view.showReminder(((Fragment)view).getString(R.string.select_priority));
            view.disableCreateButton(true);
            return;
        }
        customIssue.setAction(view.getAction());
        customIssue.setDescription(view.getDescription());
        customIssue.setItem(view.getPartName());
        customIssue.setIssueType(CarIssue.SERVICE_USER);
        String priority = view.getPriority();
        if(priority.contains(((Fragment)view).getString(R.string.severity_indicator_low))){
            customIssue.setPriority(1);
        }else if(priority.contains(((Fragment)view).getString(R.string.severity_indicator_high))){
            customIssue.setPriority(3);
        }else if(priority.contains(((Fragment)view).getString(R.string.severity_indicator_critical))){
            customIssue.setPriority(4);
        }else {
            customIssue.setPriority(2);
        }
        postService(customIssue);


    }

    public void datePicked (int year, int month, int day){
        if(issue == null || view == null || callback == null){return;}
        mixpanelHelper.trackButtonTapped("DatePickerDate",MIX_VIEW);
        issue.setYear(year);
        issue.setMonth(month);
        issue.setDay(day);
        issue.setDoneMileage(10);

        component.markServiceDoneUseCase().execute(issue,eventSource
                , new MarkServiceDoneUseCase.Callback() {
            @Override
            public void onServiceMarkedAsDone(CarIssue carIssue) {
                if(view == null || callback == null || carIssue == null){return;}
                callback.finishForm(carIssue);
            }

            @Override
            public void onError(RequestError error) {
                if(view == null || callback == null){return;}
                view.showReminder("An error occurred logging your issue " +error.getMessage());
            }
        });
    }
    private void postService(CarIssue customIssue){
        if(view == null || callback == null){return;}

        component.getAddCustomServiceUseCase().execute(customIssue, eventSource, new AddCustomServiceUseCase.Callback() {
            @Override
            public void onIssueAdded(CarIssue data) {
                if(view == null || callback == null){return;}
                if(callback.getHistorical()){
                    issue = data;
                    mixpanelHelper.trackViewAppeared("LogCustomServiceDatePicker");
                    view.showDatePicker(data);
                }else{
                    callback.finishForm(data);
                }
            }
            @Override
            public void onError(RequestError error) {
                if(view == null || callback == null){return;}
                view.showReminder("There was an error adding your services"+error.getMessage());
                view.disableCreateButton(true);
            }
        });
    }
}
