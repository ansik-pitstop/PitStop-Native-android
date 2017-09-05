package com.pitstop.ui.services.custom_service.view_fragments;

import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
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

    private CarIssue issue;

    private MixpanelHelper mixpanelHelper;

    private final String MIX_VIEW = "CustomServices";


    public ServiceFormPresenter(UseCaseComponent component,CustomServiceActivityCallback callback, MixpanelHelper mixpanelHelper){
        this.component = component;
        this.callback = callback;
        this.mixpanelHelper = mixpanelHelper;
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
        item.setText("Replace");
        item.setCardColor("#194D85");
        item.setKey(CustomServiceListAdapter.SERVICE_ACTION_KEY);
        items.add(item);

        item = new CustomIssueListItem();
        item.setText("Rotate");
        item.setCardColor("#256fc0");
        item.setKey(CustomServiceListAdapter.SERVICE_ACTION_KEY);
        items.add(item);

        item = new CustomIssueListItem();
        item.setText("Perform");
        item.setCardColor("#2b83e2");
        item.setKey(CustomServiceListAdapter.SERVICE_ACTION_KEY);
        items.add(item);

        item = new CustomIssueListItem();
        item.setText("Inspect");
        item.setCardColor("#5FA2EC");
        item.setKey(CustomServiceListAdapter.SERVICE_ACTION_KEY);
        items.add(item);

        item = new CustomIssueListItem();
        item.setText("Other");
        item.setCardColor("#8FBDF2");
        item.setKey(CustomServiceListAdapter.SERVICE_ACTION_OTHER_KEY);
        items.add(item);

        view.setActionList(items);
    }

    public void setPartNameList(){
        if(view == null || callback == null){return;}
        List<CustomIssueListItem> items = new ArrayList<>();
        CustomIssueListItem item = new CustomIssueListItem();
        item.setText("Air Filter");
        item.setCardColor("#194D85");
        item.setKey(CustomServiceListAdapter.SERVICE_PART_KEY);
        items.add(item);

        item = new CustomIssueListItem();
        item.setText("Brakes");
        item.setCardColor("#256fc0");
        item.setKey(CustomServiceListAdapter.SERVICE_PART_KEY);
        items.add(item);

        item = new CustomIssueListItem();
        item.setText("Coolant");
        item.setCardColor("#2b83e2");
        item.setKey(CustomServiceListAdapter.SERVICE_PART_KEY);
        items.add(item);

        item = new CustomIssueListItem();
        item.setText("Exhaust");
        item.setCardColor("#5FA2EC");
        item.setKey(CustomServiceListAdapter.SERVICE_PART_KEY);
        items.add(item);

        item = new CustomIssueListItem();
        item.setText("Lights");
        item.setCardColor("#8FBDF2");
        item.setKey(CustomServiceListAdapter.SERVICE_PART_KEY);
        items.add(item);

        item = new CustomIssueListItem();
        item.setText("Spark Plugs");
        item.setCardColor("#194D85");
        item.setKey(CustomServiceListAdapter.SERVICE_PART_KEY);
        items.add(item);

        item = new CustomIssueListItem();
        item.setText("Suspension");
        item.setCardColor("#256fc0");
        item.setKey(CustomServiceListAdapter.SERVICE_PART_KEY);
        items.add(item);

        item = new CustomIssueListItem();
        item.setText("Tires");
        item.setCardColor("#2b83e2");
        item.setKey(CustomServiceListAdapter.SERVICE_PART_KEY);
        items.add(item);

        item = new CustomIssueListItem();
        item.setText("Transmission");
        item.setCardColor("#5FA2EC");
        item.setKey(CustomServiceListAdapter.SERVICE_PART_KEY);
        items.add(item);

        item = new CustomIssueListItem();
        item.setText("Other");
        item.setCardColor("#8FBDF2");
        item.setKey(CustomServiceListAdapter.SERVICE_PART_OTHER_KEY);
        items.add(item);

        view.setParNameList(items);
    }

    public void setPriorityList(){
        if(view == null || callback == null){return;}
        List<CustomIssueListItem> items = new ArrayList<>();
        CustomIssueListItem item = new CustomIssueListItem();
        item.setText("Low ");
        item.setCardColor("#2b83e2");
        item.setKey(CustomServiceListAdapter.SERVICE_PRIORITY_KEY);
        items.add(item);

        item = new CustomIssueListItem();
        item.setText("Medium");
        item.setCardColor("#FFCE54");
        item.setKey(CustomServiceListAdapter.SERVICE_PRIORITY_KEY);
        items.add(item);

        item = new CustomIssueListItem();
        item.setText("High");
        item.setCardColor("#FF6F00");
        item.setKey(CustomServiceListAdapter.SERVICE_PRIORITY_KEY);
        items.add(item);

        item = new CustomIssueListItem();
        item.setText("Critical");
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
            view.showReminder("Please enter the part name");
            view.disableCreateButton(true);
            return;
        }else if(view.getPriority().equals("")){
            view.showReminder("Please select a priority");
            view.disableCreateButton(true);
            return;
        }
        customIssue.setAction(view.getAction());
        customIssue.setDescription(view.getDescription());
        customIssue.setItem(view.getPartName());
        String priority = view.getPriority();
        if(priority.contains("Low")){
            customIssue.setPriority(1);
        }else if(priority.contains("High")){
            customIssue.setPriority(3);
        }else if(priority.contains("Critical")){
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
        EventSource eventSource;
        if (callback.getHistorical())
            eventSource = new EventSourceImpl(EventSource.SOURCE_SERVICES_HISTORY);
        else
            eventSource = new EventSourceImpl(EventSource.SOURCE_SERVICES_CURRENT);
        component.markServiceDoneUseCase().execute(issue,eventSource
                , new MarkServiceDoneUseCase.Callback() {
            @Override
            public void onServiceMarkedAsDone() {
                if(view == null || callback == null){return;}
                callback.finishForm(issue);
            }

            @Override
            public void onError(RequestError error) {
                if(view == null || callback == null){return;}
                view.showReminder("An error occurred logging your issue "+error.getMessage());
            }
        });
    }
    private void postService(CarIssue customIssue){
        if(view == null || callback == null){return;}
        component.getAddCustomServiceUseCase().execute(customIssue, EventSource.SOURCE_REQUEST_SERVICE, new AddCustomServiceUseCase.Callback() {
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
                view.showReminder("An error occurred adding your service "+error.getMessage());
                view.disableCreateButton(true);
            }
        });
    }
}
