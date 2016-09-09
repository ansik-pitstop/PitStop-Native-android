package com.pitstop.models;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Paul Soladoye on 26/04/2016.
 */
public class ParseNotification implements Parcelable {

    private int id;
    private String parsePushId;
    private String alert;
    private String title;

    public ParseNotification() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getParsePushId() {
        return parsePushId;
    }

    public void setParsePushId(String parsePushId) {
        this.parsePushId = parsePushId;
    }

    public String getAlert() {
        return alert;
    }

    public void setAlert(String alert) {
        this.alert = alert;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.id);
        dest.writeString(this.parsePushId);
        dest.writeString(this.alert);
        dest.writeString(this.title);
    }

    protected ParseNotification(Parcel in) {
        this.id = in.readInt();
        this.parsePushId = in.readString();
        this.alert = in.readString();
        this.title = in.readString();
    }

    public static final Creator<ParseNotification> CREATOR = new Creator<ParseNotification>() {
        @Override
        public ParseNotification createFromParcel(Parcel source) {
            return new ParseNotification(source);
        }

        @Override
        public ParseNotification[] newArray(int size) {
            return new ParseNotification[size];
        }
    };
}
