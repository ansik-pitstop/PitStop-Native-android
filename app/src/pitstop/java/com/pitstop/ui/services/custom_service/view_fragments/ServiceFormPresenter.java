package com.pitstop.ui.services.custom_service.view_fragments;

import com.pitstop.EventBus.EventSource;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.add.AddCustomServiceUseCase;
import com.pitstop.models.issue.CustomIssue;
import com.pitstop.models.service.CustomIssueListItem;
import com.pitstop.network.RequestError;
import com.pitstop.ui.services.custom_service.CustomServiceActivityCallback;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Matt on 2017-07-25.
 */

public class ServiceFormPresenter implements PresenterCallback {

    private ServiceFormView view;

    private CustomServiceActivityCallback callback;
    private UseCaseComponent component;

    public ServiceFormPresenter(UseCaseComponent component,CustomServiceActivityCallback callback){
        this.component = component;
        this.callback = callback;
    }
    public void subscribe(ServiceFormView view){
        this.view = view;
        setActionList();
        setPartNameList();
        setPriorityList();
    }
    public void unsubscribe(){

    }


    public void setActionList(){
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
        view.togglePriorityList();
    }

    public void onPartNameClicked(){
        view.togglePartNameList();
    }

    public void onActionClicked(){
        view.toggleActionList();
    }

    @Override
    public void onActionItemClicked(CustomIssueListItem item) {
        view.showActionText(item);
        view.toggleActionList();
    }

    @Override
    public void onPartNameItemClicked(CustomIssueListItem item) {
        view.showPartNameText(item);
        view.togglePartNameList();
    }

    @Override
    public void onPriorityItemClicked(CustomIssueListItem item) {
        view.showPriorityText(item);
        view.togglePriorityList();
    }

    @Override
    public void onActionOther() {
        view.showActionText();
        view.toggleActionList();
    }

    @Override
    public void onPartNameOther() {
        view.showPartNameText();
        view.togglePriorityList();
    }

    public void onCreateButton(){
        view.disableCreateButton(false);
        CustomIssue customIssue = new CustomIssue();
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
        customIssue.setName(view.getPartName());
        String priority = view.getPriority();
        if(priority.equals("Low")){
            customIssue.setPriority(1);
        }else if(priority.equals("High")){
            customIssue.setPriority(3);
        }else if(priority.equals("Critical")){
            customIssue.setPriority(4);
        }else {
            customIssue.setPriority(2);
        }
        component.getAddCustomServiceUseCase().execute(customIssue, EventSource.SOURCE_REQUEST_SERVICE, new AddCustomServiceUseCase.Callback() {
            @Override
            public void onIssueAdded() {
                System.out.println("Testing issue added");
                callback.finishForm();
            }

            @Override
            public void onError(RequestError error) {
                view.disableCreateButton(true);
            }
        });
    }
}
