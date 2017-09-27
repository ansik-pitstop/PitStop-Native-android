package com.pitstop.models.report;

/**
 * Created by Karol Zdebel on 9/20/2017.
 */

public abstract class CarHealthItem {
    private int id;
    private int priority;
    private String item;
    private String description;

    public CarHealthItem(int id, int priority, String item, String description) {
        this.id = id;
        this.priority = priority;
        this.item = item;
        this.description = description;
    }

    public int getId() {
        return id;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString(){
        return "item: "+getItem()+", priority: "+getPriority() +", description: "+description;
    }
}
