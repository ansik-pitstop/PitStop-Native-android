package com.pitstop.models.report;

/**
 * Created by Karol Zdebel on 9/20/2017.
 */

public class EngineIssue extends CarHealthItem{
    private boolean isPending;
    private String symptoms;

    public EngineIssue(int id, int priority, boolean isPending, String item
            , String symptoms, String description) {
        super(id,priority,item,description);
        this.isPending = isPending;
        this.symptoms = symptoms;
    }

    public boolean isPending() {
        return isPending;
    }

    public void setPending(boolean pending) {
        isPending = pending;
    }

    public String getSymptoms() {
        return symptoms;
    }

    public void setSymptoms(String symptoms) {
        this.symptoms = symptoms;
    }

    @Override
    public String toString(){
        return "item: "+getItem()+", priority: "+getPriority() +", isPending: "+isPending
                +", description: "+getDescription()+", symptoms: "+getSymptoms();
    }
}
