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

    private Issue issue;

    public CarService(Issue issue) {
        this.issue = issue;
    }

    public String getDescription(){
        return issue.getDescription();
    }

    public int getPriority(){
        return issue.getPriority();
    }

    public String getAction(){
        return issue.getAction();
    }

    public String getItem(){
        return issue.getItem();
    }

    public int getId() { return issue.getIssueId(); }

}
