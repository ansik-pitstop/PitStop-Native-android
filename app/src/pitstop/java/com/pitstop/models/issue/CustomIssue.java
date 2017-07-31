package com.pitstop.models.issue;

/**
 * name: "TEST CUSTOM ISSUE 2",
 action: "Fix Stuff",
 priority: 3,
 description: "description two",
 userId: 1

 * Created by Matt on 2017-07-31.
 */

public class CustomIssue {
    private String name;
    private String action;
    private int priority;
    private String description;
    private int userId;

    public CustomIssue(String name,String action, int priority, String description, int userId) {
        this.name = name;
        this.action = action;
        this.priority = priority;
        this.description = description;
        this.userId = userId;
    }
    public CustomIssue(){

    }

        public void setName(String name) {
        this.name = name;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getPriority() {
        return priority;
    }

    public int getUserId() {
        return userId;
    }

    public String getAction() {
        return action;
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }
}
