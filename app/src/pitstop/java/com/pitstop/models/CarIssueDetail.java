package com.pitstop.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Paul Soladoye on 3/18/2016.
 */
public class CarIssueDetail implements Parcelable {

    @Expose(serialize = false, deserialize = false)
    private int id;

    private String item;
    private String description;
    private String action;

    public CarIssueDetail() {}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public static CarIssueDetail createCarIssueDetail(String item,
                                                      String description, String action) {
        CarIssueDetail carIssueDetail = new CarIssueDetail();
        carIssueDetail.setItem(item);
        carIssueDetail.setDescription(description);
        carIssueDetail.setAction(action);
        return carIssueDetail;
    }

    public static CarIssueDetail createCarIssueDetail(JSONObject detailObject) throws JSONException {
        CarIssueDetail carIssueDetail = new CarIssueDetail();

        carIssueDetail.setItem(detailObject.getString("item"));
        carIssueDetail.setDescription(detailObject.getString("description"));
        carIssueDetail.setAction(detailObject.optString("action"));

        return carIssueDetail;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.id);
        dest.writeString(this.item);
        dest.writeString(this.description);
        dest.writeString(this.action);
    }

    protected CarIssueDetail(Parcel in) {
        this.id = in.readInt();
        this.item = in.readString();
        this.description = in.readString();
        this.action = in.readString();
    }

    public static final Creator<CarIssueDetail> CREATOR = new Creator<CarIssueDetail>() {
        @Override
        public CarIssueDetail createFromParcel(Parcel source) {
            return new CarIssueDetail(source);
        }

        @Override
        public CarIssueDetail[] newArray(int size) {
            return new CarIssueDetail[size];
        }
    };
}