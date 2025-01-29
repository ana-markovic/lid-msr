package org.crossflow.runtime;

import java.io.Serializable;
import java.time.LocalDateTime;

public class WinningBid implements Serializable {
    private final String jobId;
    private String jobName;
    private final String workerId;

    private final LocalDateTime winTime;

    private final WorkCost workCost;


    public WinningBid(String jobId, String jobName, String workerId, WorkCost workCost) {
        this.jobId = jobId;
        this.jobName = jobName;
        this.workerId = workerId;
        this.workCost = workCost;
        winTime = LocalDateTime.now();
    }

    public String getJobId() {
        return jobId;
    }

    public String getWorkerId() {
        return workerId;
    }

    public LocalDateTime getWinTime() {
        return winTime;
    }

    public String toCsv() {
        return jobId + "," + jobName + "," + workerId + "," + winTime + "," + workCost.getTotalCost();
    }

    public WorkCost getWorkCost() {
        return workCost;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }
}
