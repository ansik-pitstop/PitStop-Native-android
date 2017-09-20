package com.pitstop.models.report;

/**
 * Created by Karol Zdebel on 9/20/2017.
 */

public class EngineIssue {
    private int id;
    private int priority;
    private boolean isPending;
    private String item;
    private String symptoms;
    private String description;

    public EngineIssue(int id, int priorty, boolean isPending, String item
            , String symptoms, String description) {
        this.id = id;
        this.priority = priorty;
        this.isPending = isPending;
        this.item = item;
        this.symptoms = symptoms;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isPending() {
        return isPending;
    }

    public void setPending(boolean pending) {
        isPending = pending;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public String getSymptoms() {
        return symptoms;
    }

    public void setSymptoms(String symptoms) {
        this.symptoms = symptoms;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString(){
        return "item: "+getItem()+", priority: "+getPriority()
                +", isPending: "+isPending+", description: "+description+", symptoms: "+symptoms;
    }
}
