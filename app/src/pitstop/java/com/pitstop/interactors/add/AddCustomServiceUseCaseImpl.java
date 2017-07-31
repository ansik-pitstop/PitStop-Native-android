package com.pitstop.interactors.add;

import android.os.Handler;

import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.repositories.CarIssueRepository;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.UserRepository;

/**
 * Created by Matt on 2017-07-31.
 */

public class AddCustomServiceUseCaseImpl implements AddCustomServiceUseCase {
    private CarIssueRepository carIssueRepository;
    private UserRepository userRepository;
    private Handler handler;
    private Callback callback;

    private int carId;

    private EventSource eventSource;

    public AddCustomServiceUseCaseImpl(CarIssueRepository carIssueRepository, UserRepository userRepository, Handler handler){
        this.carIssueRepository = carIssueRepository;
        this.userRepository = userRepository;
        this.handler = handler;
    }

    @Override
    public void execute(int carId, String eventSource, Callback callback) {
        this.eventSource = new EventSourceImpl(eventSource);
        this.carId = carId;
        this.callback = callback;
        handler.post(this);
    }

    @Override
    public void run() {


    }
    /*

     public boolean insertCustom(int carId, CustomIssue issue,CarIssueCustomInsertCallback callback){
        networkHelper.postUserInputIssue(carId,issue.getName(),issue.getAction(),issue.getDescription(),issue.getPriority(),getInsertCustomRequestCallback(callback));
        return true;
    }
    public RequestCallback getInsertCustomRequestCallback(CarIssueCustomInsertCallback callback){
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                if(requestError == null && response != null){
                    callback.onCustomIssueAdded();
                }else{
                    callback.onError();
                }
            }
        };
        return requestCallback;
    }

     */
}
