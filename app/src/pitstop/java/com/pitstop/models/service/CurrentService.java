package com.pitstop.models.service;

import com.pitstop.models.issue.CarIssue;

/**
 * Class responsible for any functionality/data related to current
 * services.
 *
 * Created by Karol Zdebel on 6/7/2017.
 */

public class CurrentService extends CarService {

    private CarIssue issue;

    public CurrentService(CarIssue issue) {
        super(issue);
        this.issue = issue;
    }



}
