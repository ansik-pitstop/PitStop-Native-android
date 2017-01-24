package com.pitstop.models;

import java.io.Serializable;
import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by zohaibhussain on 2017-01-11.
 */

public class Timeline implements Serializable {
    @SerializedName("results")
    @Expose
    private List<TimelineResult> results = null;

    public List<TimelineResult> getResults() {
        return results;
    }

    public void setResults(List<TimelineResult> results) {
        this.results = results;
    }
}
