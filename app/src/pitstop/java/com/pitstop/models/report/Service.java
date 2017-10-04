package com.pitstop.models.report;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Karol Zdebel on 9/20/2017.
 */

public class Service extends CarHealthItem implements Parcelable{
    private String action;

    public Service(int id, int priority, String item, String action, String description) {
        super(id,priority,item,description);
        this.action = action;
    }

    protected Service(Parcel in) {
        super(in);
        action = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(action);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Service> CREATOR = new Creator<Service>() {
        @Override
        public Service createFromParcel(Parcel in) {
            return new Service(in);
        }

        @Override
        public Service[] newArray(int size) {
            return new Service[size];
        }
    };

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
