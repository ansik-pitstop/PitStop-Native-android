package com.pitstop.models.issue;

/**
 * Created by Karol Zdebel on 6/7/2017.
 */

public interface Issue {

    int getIssueId();

    int getPriority();

    String getDescription();

    String getItem();

    String getAction();

}
