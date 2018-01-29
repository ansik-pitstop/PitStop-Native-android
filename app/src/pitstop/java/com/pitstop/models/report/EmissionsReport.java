package com.pitstop.models.report;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Karol Zdebel on 9/28/2017.
 */

public class EmissionsReport implements Parcelable{

    private int id;
    private int vhrId = -1;
    private LinkedHashMap<String,String> sensors;
    private Date createdAt;
    private boolean pass;
    private String reason;

    public EmissionsReport(int id, Date createdAt, boolean pass, String reason) {
        this.id = id;
        this.createdAt = createdAt;
        this.pass = pass;
        this.reason = reason;
        this.sensors = new LinkedHashMap<>();
    }

    public EmissionsReport(int id, int vhrId, Date createdAt, boolean pass, String reason) {
        this.id = id;
        this.vhrId = vhrId;
        this.createdAt = createdAt;
        this.pass = pass;
        this.reason = reason;
        this.sensors = new LinkedHashMap<>();
    }

    public EmissionsReport(int id, Date createdAt, boolean pass, String reason, LinkedHashMap<String,String> sensors) {
        this.id = id;
        this.createdAt = createdAt;
        this.pass = pass;
        this.reason = reason;
        this.sensors = sensors;
    }

    public EmissionsReport(int id, int vhrId, Date createdAt, boolean pass, String reason, LinkedHashMap<String,String> sensors) {
        this.id = id;
        this.vhrId = vhrId;
        this.createdAt = createdAt;
        this.pass = pass;
        this.reason = reason;
        this.sensors = sensors;
    }

    protected EmissionsReport(Parcel in) {
        id = in.readInt();
        createdAt = (Date)in.readSerializable();
        pass = in.readByte() != 0;
        reason = in.readString();
        List<String> sensorValues = new ArrayList<>();
        List<String> sensorKeys = new ArrayList<>();
        in.readStringList(sensorValues);
        in.readStringList(sensorKeys);
        for (int i=0;i<sensorValues.size();i++){
            sensors.put(sensorKeys.get(i),sensorValues.get(i));
        }
    }

    public static final Creator<EmissionsReport> CREATOR = new Creator<EmissionsReport>() {
        @Override
        public EmissionsReport createFromParcel(Parcel in) {
            return new EmissionsReport(in);
        }

        @Override
        public EmissionsReport[] newArray(int size) {
            return new EmissionsReport[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {

        parcel.writeInt(id);
        parcel.writeSerializable(createdAt);
        parcel.writeByte((byte) (pass ? 1 : 0));
        parcel.writeString(reason);
        List<String> sensorValues = new ArrayList<>();
        List<String> sensorKeys = new ArrayList<>();
        for (Map.Entry<String,String> e: sensors.entrySet()){
            sensorValues.add(e.getValue());
            sensorKeys.add(e.getKey());
        }
        parcel.writeStringList(sensorValues);
        parcel.writeStringList(sensorKeys);
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isPass() {
        return pass;
    }

    public void setPass(boolean pass) {
        this.pass = pass;
    }

    public int getVhrId() {
        return vhrId;
    }

    public void setVhrId(int vhrId) {
        this.vhrId = vhrId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void addSensor(String key, String value){
        sensors.put(key,value);
    }

    @Override
    public String toString(){
        try{
            return String.format("id:%d, vhrId:%d, sensors: %s, createdAt:%s, pass:%b, reason:%s",getId(), getVhrId(), sensors.toString(), getCreatedAt().toString()
                    , isPass(), getReason());
        }catch(NullPointerException e){
            e.printStackTrace();
            return "null";
        }

    }
}
