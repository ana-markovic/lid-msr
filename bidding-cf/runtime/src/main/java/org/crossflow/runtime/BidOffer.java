package org.crossflow.runtime;

import java.io.Serializable;

public class BidOffer implements Serializable {
    private Job job;
    private String jobName;

    public BidOffer(Job job) {
        this.job = job;
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }

    @Override
    public String toString() {
        return "BidOffer{" +
                "job=" + job +
                '}';
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }
}
