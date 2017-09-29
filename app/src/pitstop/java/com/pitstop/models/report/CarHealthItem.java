package com.pitstop.models.report;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Karol Zdebel on 9/20/2017.
 */

public abstract class CarHealthItem implements Parcelable{
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

    protected CarHealthItem(Parcel in) {
        id = in.readInt();
        priority = in.readInt();
        item = in.readString();
        description = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(id);
        parcel.writeInt(priority);
        parcel.writeString(item);
        parcel.writeString(description);
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
