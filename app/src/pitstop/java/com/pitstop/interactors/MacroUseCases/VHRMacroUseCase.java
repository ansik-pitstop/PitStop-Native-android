package com.pitstop.interactors.MacroUseCases;

import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.Interactor;
import com.pitstop.interactors.get.GetCurrentServicesUseCase;
import com.pitstop.interactors.get.GetCurrentServicesUseCaseImpl;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.network.RequestError;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Created by Matt on 2017-08-23.
 */

public class VHRMacroUseCase {

    public interface Callback{
        void onStartGetServices();
        void onServicesGot(List<CarIssue> issues, List<CarIssue> recalls);
        void onServiceError();
        void onStartGetDTC();
        void onGotDTC();
        void onStartPID();
        void onGotPID();
        void onFinish();
    }

    private Callback callback;

    public Queue<Interactor> interactorQueue;

    private UseCaseComponent component;



    public VHRMacroUseCase(UseCaseComponent component,Callback callback){
        this.callback = callback;
        this.component = component;
        interactorQueue = new LinkedList<Interactor>();
        interactorQueue.add(component.getCurrentServicesUseCase());
    }
    public void start(){
        next();
    }
    private void next(){
        if(interactorQueue.isEmpty()){finish();}
        Interactor current = interactorQueue.peek();
        interactorQueue.remove(current);

        if(current instanceof GetCurrentServicesUseCaseImpl){
            System.out.println("Testing here");
            callback.onStartGetServices();
            ((GetCurrentServicesUseCaseImpl) current).execute(new GetCurrentServicesUseCase.Callback() {
                @Override
                public void onGotCurrentServices(List<CarIssue> currentServices, List<CarIssue> customIssues) {
                    List<CarIssue> recalls = new ArrayList<CarIssue>();
                    for(CarIssue c: currentServices){
                        if(c.getIssueType().equals(CarIssue.RECALL)){
                            recalls.add(c);
                        }else if(c.getIssueType().equals(CarIssue.DTC)){
                            currentServices.remove(c);
                        }
                    }
                    currentServices.addAll(customIssues);
                    callback.onServicesGot(currentServices,recalls);
                    next();
                }

                @Override
                public void onError(RequestError error) {
                    callback.onServiceError();
                }
            });
        }
    }

    private void finish(){
        callback.onFinish();
    }


}
