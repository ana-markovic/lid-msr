package org.crossflow.runtime.utils;

import org.crossflow.runtime.WorkCost;

import java.io.Serializable;
import java.time.LocalDateTime;

public class ExtendedEstimationMetric implements Serializable {
    public static String CSV_HEADER = "jobId,workerId,estimatedNetCost,estimatedIOCost,estimatedProcessingCost,currentWorkloadCost,netCorrectionFactor,ioCorrectionFactor,estimatedTotalCost,actualNetCost,actualIOCost,actualProcessingCost,endTime,jobName,repoSize,fileCount,lineCount,intercept,slope";
    private final String jobId;
    private final String workerId;
    private final WorkCost estimatedWorkCost;
    private final WorkCost actualWorkCost;
    private final LocalDateTime startTime;
    private final String jobName;
    private final long repoSize;
    private final int fileCount;
    private final int lineCount;
    private final double intercept;
    private final double slope;



    public ExtendedEstimationMetric(String jobId, String workerId, WorkCost estimatedWorkCost, WorkCost actualWorkCost,
                                    LocalDateTime startTime, String jobName, long repoSize, int fileCount, int lineCount,
                                    double intercept, double slope) {
        this.jobId = jobId;
        this.workerId = workerId;
        this.estimatedWorkCost = estimatedWorkCost;
        this.actualWorkCost = actualWorkCost;
        this.startTime = startTime;
        this.jobName = jobName;
        this.repoSize = repoSize;
        this.fileCount = fileCount;
        this.lineCount = lineCount;
        this.intercept = intercept;
        this.slope = slope;
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
        builder.append(estimatedWorkCost.getIoCorrectionFactor()).append(",");
        builder.append(estimatedWorkCost.getTotalCost()).append(",");
        builder.append(actualWorkCost.getNetworkCost()).append(",");
        builder.append(actualWorkCost.getIoCost()).append(",");
        builder.append(actualWorkCost.getProcessingCost()).append(",");
        builder.append(startTime).append(",");
        builder.append(jobName).append(",");
        builder.append(repoSize).append(",");
        builder.append(fileCount).append(",");
        builder.append(lineCount).append(",");
        builder.append(intercept).append(",");
        builder.append(slope);
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

    public String getJobName() {
        return jobName;
    }

    public long getRepoSize() {
        return repoSize;
    }

    @Override
    public String toString() {
        return "ExtendedEstimationMetric{" +
                "jobId='" + jobId + '\'' +
                ", workerId='" + workerId + '\'' +
                ", estimatedWorkCost=" + estimatedWorkCost +
                ", actualWorkCost=" + actualWorkCost +
                ", startTime=" + startTime +
                ", jobName='" + jobName + '\'' +
                ", repoSize=" + repoSize +
                ", fileCount=" + fileCount +
                ", lineCount=" + lineCount +
                ", intercept=" + intercept +
                ", slope=" + slope +
                '}';
    }
}
