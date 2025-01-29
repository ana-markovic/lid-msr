package org.crossflow.runtime.utils;

import org.crossflow.runtime.WorkCost;

import java.io.Serializable;
import java.time.LocalDateTime;

public class EstimationMetrics implements Serializable {
    //    public static String CSV_HEADER = "jobId,workerId,estimatedNetCost,estimatedIOCost,estimatedProcessingCost,currentWorkloadCost,currentCorrectionFactor,estimatedTotalCost,actualNetCost,actualIOCost,actualProcessingCost,startTime";
    public static String CSV_HEADER = "jobId,workerId,estimatedNetCost,estimatedIOCost,estimatedProcessingCost,currentWorkloadCost,currentCorrectionFactor,estimatedTotalCost,actualNetCost,actualIOCost,actualProcessingCost,startTime,repoName, repoSize";
    private final String jobId;
    private final String workerId;
    private final WorkCost estimatedWorkCost;
    private final WorkCost actualWorkCost;
    private final LocalDateTime startTime;
    private final String repoName;
    private final long repoSize;

    public EstimationMetrics(String jobId, String workerId, WorkCost estimatedWorkCost, WorkCost actualWorkCost, LocalDateTime startTime, String repoName, long repoSize) {
        this.jobId = jobId;
        this.workerId = workerId;
        this.estimatedWorkCost = estimatedWorkCost;
        this.actualWorkCost = actualWorkCost;
        this.startTime = startTime;
        this.repoName = repoName;
        this.repoSize = repoSize;
    }

    public static EstimationMetrics emptyWithJobId(String jobId, String workerId) {
        return new EstimationMetrics(jobId, workerId, WorkCost.EMPTY, WorkCost.EMPTY, LocalDateTime.now(), "", 0);
    }

    public String toCSV() {
        StringBuilder builder = new StringBuilder();
        builder.append(jobId).append(",");
        builder.append(workerId).append(",");
        builder.append(estimatedWorkCost.getNetworkCost()).append(",");
        builder.append(estimatedWorkCost.getIoCost()).append(",");
        builder.append(estimatedWorkCost.getProcessingCost()).append(",");
        builder.append(estimatedWorkCost.getWorkloadCost()).append(",");
        builder.append(estimatedWorkCost.getNetCorrectionFactor()).append(",");
        builder.append(estimatedWorkCost.getTotalCost()).append(",");
        builder.append(actualWorkCost.getNetworkCost()).append(",");
        builder.append(actualWorkCost.getIoCost()).append(",");
        builder.append(actualWorkCost.getProcessingCost()).append(",");
        builder.append(startTime).append(",");
        builder.append(repoName).append(",");
        builder.append(repoSize);
        return builder.toString();
    }

    public String getJobId() {
        return jobId;
    }

    public String getWorkerId() {
        return workerId;
    }

    public WorkCost getEstimatedWorkCost() {
        return estimatedWorkCost;
    }

    public WorkCost getActualWorkCost() {
        return actualWorkCost;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public String getRepoName() {
        return repoName;
    }

    public long getRepoSize() {
        return repoSize;
    }
}
