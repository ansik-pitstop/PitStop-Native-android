package com.pitstop.models.report;

/**
 * Created by Karol Zdebel on 9/20/2017.
 */

public class Service {
    private int id;
    private int priority;
    private String item;
    private String action;
    private String description;

    public Service(int id, int priority, String item, String action, String description) {
        this.id = id;
        this.priority = priority;
        this.item = item;
        this.action = action;
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

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString(){
        return "item: "+getItem()+", priority: "+getPriority() + ", description: "+description;
    }
}
