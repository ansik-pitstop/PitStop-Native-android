package com.pitstop.ui.services.current;

import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.other.MarkServiceDoneUseCase;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.network.RequestError;

/**
 * Created by Karol Zdebel on 8/30/2017.
 */

public class CurrentServicesPresenter {

    private CurrentServicesView view;
    private UseCaseComponent useCaseComponent;


    public void onRefresh(){

    }

    public void onServiceDoneDatePicked(CarIssue carIssue, int year, int month, int day){
        carIssue.setYear(year);
        carIssue.setMonth(month);
        carIssue.setDay(day);

        //When the date is set, update issue to done on that date
        useCaseComponent.markServiceDoneUseCase().execute(carIssue, new MarkServiceDoneUseCase.Callback() {
            @Override
            public void onServiceMarkedAsDone() {
//                        carIssues.remove(carIssue);
//                        notifyDataSetChanged();
//                        EventType event = new EventTypeImpl(EventType
//                                .EVENT_SERVICES_HISTORY);
//                        EventSource source = new EventSourceImpl(EventSource
//                                .SOURCE_SERVICES_CURRENT);
//                        notifier.notifyCarDataChanged(event,source);
            }

            @Override
            public void onError(RequestError error) {
            }
        });
    }

    public void onServiceMarkedAsDone(CarIssue carIssue){
        view.displayCalendar(carIssue);
    }

}
