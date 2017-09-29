package com.pitstop.models.report;

/**
 * Created by Karol Zdebel on 9/20/2017.
 */

public class Service extends CarHealthItem{
    private String action;

    public Service(int id, int priority, String item, String action, String description) {
        super(id,priority,item,description);
        this.action = action;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    @Override
    public String toString(){
        return "item: "+getItem()+", priority: "+getPriority() + ", description: "+getDescription();
    }
}
