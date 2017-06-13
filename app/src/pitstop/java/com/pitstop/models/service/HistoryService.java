package com.pitstop.models.service;

import com.pitstop.models.issue.CarIssue;

/**
 * Created by Karol Zdebel on 6/7/2017.
 */

public class HistoryService extends CarService {

    private CarIssue issue;

    public HistoryService(CarIssue issue) {
        super(issue);
        this.issue = issue;
    }

    public String getDoneAt(){
        return issue.getDoneAt();
    }

    public int daysSinceServiced(){
        return issue.getDaysAgo();
    }
}
