package com.pitstop.models.report;

import java.util.List;

/**
 * Created by Karol Zdebel on 9/19/2017.
 */

public class VehicleHealthReport {
    private List<EngineIssue> engineIssues;
    private List<Recall> recalls;
    private List<Service> services;

    public VehicleHealthReport(List<EngineIssue> engineIssues, List<Recall> recalls, List<Service> services) {
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

    @Override
    public String toString(){
        return "engineIssues: "+getEngineIssues() +", recalls: "+getRecalls()
                +", services: "+getServices();
    }
}
