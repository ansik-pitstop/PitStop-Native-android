package com.pitstop.models.report;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Karol Zdebel on 9/20/2017.
 */

public class Recall extends CarHealthItem implements Parcelable{

    public Recall(int id, int priority, String item, String description) {
        super(id,priority,item,description);
    }

    protected Recall(Parcel in) {
        super(in);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Recall> CREATOR = new Creator<Recall>() {
        @Override
        public Recall createFromParcel(Parcel in) {
            return new Recall(in);
        }

        @Override
        public Recall[] newArray(int size) {
            return new Recall[size];
        }
    };
}
