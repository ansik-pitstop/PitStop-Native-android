package com.pitstop.models.service;

import com.pitstop.models.issue.UpcomingIssue;

/**
 *
 * Created by Karol Zdebel on 6/7/2017.
 */

public class UpcomingService extends CarService {

    private UpcomingIssue issue;

    public UpcomingService(UpcomingIssue issue) {
        super(issue);
        this.issue = issue;
    }

    public int getMileage(){
        return Integer.valueOf(issue.getIntervalMileage());
    }
}
