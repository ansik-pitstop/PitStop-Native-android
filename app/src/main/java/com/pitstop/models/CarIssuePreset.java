package com.pitstop.models;

import com.castel.obd.util.JsonUtil;
import com.google.gson.annotations.Expose;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yifan on 16/9/28.
 */
public class CarIssuePreset {

    public static final String TYPE_USER_INPUT = "userInput";
    public static final String TYPE_PRESET = "preset";

    @Expose
    private String type;
    @Expose
    private int id;
    @Expose
    private String item;
    @Expose
    private String action;
    @Expose
    private String description;

    private int priority;

    public CarIssuePreset() {
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

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

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    private static CarIssuePreset createCustomCarIssue(JSONObject issue) {
        return JsonUtil.json2object(issue.toString(), CarIssuePreset.class);
    }

    public static List<CarIssuePreset> createCustomCarIssues(String response) throws JSONException {
        JSONObject body = new JSONObject(response);
        JSONArray results = body.getJSONArray("results");

        ArrayList<CarIssuePreset> customCarIssues = new ArrayList<>();

        for (int i = 0; i < results.length(); i++) {
            JSONObject issue = results.getJSONObject(i);
            customCarIssues.add(createCustomCarIssue(issue));
        }

        return customCarIssues;
    }

    public CarIssuePreset(Builder builder){
        type = builder.type;
        id = builder.id;
        item = builder.item;
        action = builder.action;
        description = builder.description;
        priority = builder.priority;
    }

    public static class Builder {
        private String type;
        private int id;
        private String item = "No item";
        private String action = "No action";
        private String description = "No description";
        private int priority;

        public Builder() {}

        public Builder(String type, int id) {
            this.type = type;
            this.id = id;
        }

        public Builder setId(int id){
            this.id = id;
            return this;
        }

        public Builder setType(String type){
            this.type = type;
            return this;
        }

        public Builder setItem(String item) {
            this.item = item;
            return this;
        }

        public Builder setAction(String action) {
            this.action = action;
            return this;
        }

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder setPriority(int priority){
            this.priority = priority;
            return this;
        }

        public CarIssuePreset build(){
            return new CarIssuePreset(this);
        }
    }

}
