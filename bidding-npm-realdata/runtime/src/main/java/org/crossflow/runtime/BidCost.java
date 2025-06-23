package org.crossflow.runtime;

public class BidCost {
    private double cost;
    private WorkCost workCost;
    private String jobName;

    public BidCost(double cost, String jobName) {
        this.cost = cost;
        this.jobName = jobName;
    }

    public BidCost(WorkCost workCost, String jobName) {
        this.workCost = workCost;
        this.jobName = jobName;
    }

    public WorkCost getWorkCost() {
        return workCost;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }
}
