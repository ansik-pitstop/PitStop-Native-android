package com.pitstop.interactors.get;

import android.os.Handler;

import com.pitstop.models.Settings;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.CarIssueRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.DateTimeFormatUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by Karol Zdebel on 9/1/2017.
 */

public class GetHistoricalServicesSortedByDateUseCaseImpl implements GetHistoryServicesSortedByDateUseCase {

    private UserRepository userRepository;
    private CarIssueRepository carIssueRepository;
    private Callback callback;
    private Handler useCaseHandler;
    private Handler mainHandler;

    public GetHistoricalServicesSortedByDateUseCaseImpl(UserRepository userRepository
            , CarIssueRepository carIssueRepository, Handler useCaseHandler, Handler mainHandler) {

        this.userRepository = userRepository;
        this.carIssueRepository = carIssueRepository;
        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
    }

    @Override
    public void execute(Callback callback) {
        this.callback = callback;
        useCaseHandler.post(this);
    }

    private void onGotDoneServices(LinkedHashMap<String, ArrayList<CarIssue>> sortedIssues
            , ArrayList<String> headers){
        mainHandler.post(() -> callback.onGotDoneServices(sortedIssues,headers));
    }

    private void onError(RequestError error){
        mainHandler.post(() -> callback.onError(error));
    }

    @Override
    public void run() {
        userRepository.getCurrentUserSettings(new Repository.Callback<Settings>() {
            @Override
            public void onSuccess(Settings data) {

                if (!data.hasMainCar()){
                    callback.onError(RequestError.getUnknownError());
                    return;
                }

                //Use the current users car to get all the current issues
                carIssueRepository.getDoneCarIssues(data.getCarId()
                        , new CarIssueRepository.Callback<List<CarIssue>>() {

                            @Override
                            public void onSuccess(List<CarIssue> doneServices) {

                                LinkedHashMap<String, ArrayList<CarIssue>> sortedIssues = new LinkedHashMap<>();
                                ArrayList<String> headers = new ArrayList<>();

                                CarIssue[] doneServicesOrdered = new CarIssue[doneServices.size()];
                                doneServices.toArray(doneServicesOrdered);
                                Arrays.sort(doneServicesOrdered, (lhs, rhs)
                                        -> DateTimeFormatUtil.getHistoryDateToCompare(rhs.getDoneAt())
                                                - DateTimeFormatUtil.getHistoryDateToCompare(lhs.getDoneAt()));

                                for (CarIssue issue: doneServicesOrdered){
                                    addIssue(sortedIssues,headers,issue);
                                }

                                GetHistoricalServicesSortedByDateUseCaseImpl
                                        .this.onGotDoneServices(sortedIssues,headers);

                            }

                            @Override
                            public void onError(RequestError error) {
                                GetHistoricalServicesSortedByDateUseCaseImpl.this.onError(error);
                            }
                        });
            }

            @Override
            public void onError(RequestError error) {
                GetHistoricalServicesSortedByDateUseCaseImpl.this.onError(error);
            }
        });
    }

    private void addIssue(LinkedHashMap<String, ArrayList<CarIssue>> sortedIssues
            ,List<String> headers, CarIssue issue){

        String dateHeader;
        if(issue.getDoneAt() == null || issue.getDoneAt().equals("")) {
            dateHeader = "";
        } else {
            String formattedDate = DateTimeFormatUtil.formatDateToHistoryFormat(issue.getDoneAt());
            dateHeader = formattedDate.substring(0, 3) + " " + formattedDate.substring(9, 13);
        }

        ArrayList<CarIssue> issues = sortedIssues.get(dateHeader);

        //Check if header already exists
        if(issues == null) {
            headers.add(dateHeader);
            issues = new ArrayList<>();
            issues.add(issue);
        }
        else {
            //Add issue to appropriate position within list, in order of date
            int issueSize = issues.size();
            for (int i = 0; i < issueSize; i++) {
                if (!(DateTimeFormatUtil.getHistoryDateToCompare(issues.get(i).getDoneAt())
                        - DateTimeFormatUtil.getHistoryDateToCompare(issue.getDoneAt()) >= 0)) {
                    issues.add(i, issue);
                    break;
                }
                if (i == issueSize -1){
                    issues.add(issue);
                    break;
                }
            }
        }

        sortedIssues.put(dateHeader, issues);
    }
}
