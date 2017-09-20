package com.pitstop.models.report;

import java.util.List;

/**
 * Created by Karol Zdebel on 9/19/2017.
 */

public class VehicleHealthReport {

    private int id;
    private List<EngineIssue> engineIssues;
    private List<Recall> recalls;
    private List<Service> services;

    public VehicleHealthReport(int id, List<EngineIssue> engineIssues, List<Recall> recalls
            , List<Service> services) {
        this.id = id;
        this.engineIssues = engineIssues;
        this.recalls = recalls;
        this.services = services;
    }

    public List<EngineIssue> getEngineIssues() {
        return engineIssues;
    }

    public void setEngineIssues(List<EngineIssue> engineIssues) {
        this.engineIssues = engineIssues;
    }

    public List<Recall> getRecalls() {
        return recalls;
    }

    public void setRecalls(List<Recall> recalls) {
        this.recalls = recalls;
    }

    public List<Service> getServices() {
        return services;
    }

    public void setServices(List<Service> services) {
        this.services = services;
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString(){
        return "id: "+id+", engineIssues: "+getEngineIssues() +", recalls: "+getRecalls()
                +", services: "+getServices();
    }
}
