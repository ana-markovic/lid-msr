package org.crossflow.runtime;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Arrays;

public class Bid implements Serializable, Comparable<Bid> {
    private Job job;
    private String jobName;
    private final String workerId;
    private final LocalDateTime bidTime;
    private final WorkCost workCost;

    public Bid(Job job, String workerId, WorkCost workCost, String jobName) {
        this.job = job;
        this.workerId = workerId;
        this.bidTime = LocalDateTime.now();
        this.workCost = workCost;
        this.jobName = jobName;
    }

    public void decreaseCost(double cost) {
//        this.cost = Math.max(0, this.cost - cost);
    }

    public void setJob(Job job) {
        this.job = job;
    }

    public LocalDateTime getBidTime() {
        return bidTime;
    }

    public Job getJob() {
        return job;
    }

    public double getCost() {
        return getWorkCost().getTotalCost();
    }

    public String getWorkerId() {
        return workerId;
    }

    public WorkCost getWorkCost() {
        return workCost;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Bid)) return false;

        Bid bid = (Bid) o;

        if (Double.compare(bid.getCost(), getCost()) != 0) return false;
        if (!getJob().equals(bid.getJob())) return false;
        return getWorkerId().equals(bid.getWorkerId());
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = getJob().hashCode();
        temp = Double.doubleToLongBits(getCost());
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + getWorkerId().hashCode();
        return result;
    }

    @Override
    public int compareTo(Bid o) {
        return Double.compare(this.getWorkCost().getTotalCost(), o.getWorkCost().getTotalCost());
    }

    @Override
    public String toString() {
        return "Bid{" +
                "job=" + job +
                ", cost=" + getWorkCost().getTotalCost() +
                ", workerId='" + workerId + '\'' +
                ", jobName='" + jobName + '\'' +
                '}';
    }

    public String toCsv() {
        return String.join(",", Arrays.asList(
                job.getJobId(),
                jobName,
                workerId,
                String.valueOf(getWorkCost().getTotalCost()),
                String.valueOf(getWorkCost().getProcessingCost()),
                String.valueOf(bidTime)
        ));
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }
}
