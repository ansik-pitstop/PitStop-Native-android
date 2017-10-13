package com.pitstop.models.service;

import com.pitstop.models.issue.Issue;

/**
 *
 * This class is responsible for modifying any issue data
 * such that it is appropriate in the context of a service.
 *
 * Created by Karol Zdebel on 6/7/2017.
 */

public abstract class CarService {

    private Issue realIssue;

    public CarService(Issue issue) {
        this.realIssue = issue;
    }

    public String getDescription(){
        return realIssue.getDescription();
    }

    public int getPriority(){
        return realIssue.getPriority();
    }

    public String getAction(){
        return realIssue.getAction();
    }

    public String getItem(){
        return realIssue.getItem();
    }

    public int getId() { return realIssue.getIssueId(); }

}
