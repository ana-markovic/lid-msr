package org.crossflow.runtime;

import java.io.Serializable;

public class WorkCost implements Serializable {
    public static final WorkCost EMPTY = new WorkCost(0, 0, 0, 1.0, 1.0);
    private final double networkCost;
    private final double ioCost;
    private final double workloadCost;
    private final double processingCost;
    private final double totalCost;
    private final double netCorrectionFactor;
    private final double ioCorrectionFactor;


    public WorkCost(double networkCost, double ioCost, double workloadCost, double netCorrectionFactor, double ioCorrectionFactor) {
        this.networkCost = Math.round(networkCost);
        this.ioCost = Math.round(ioCost);
        this.workloadCost = Math.round(workloadCost);
        this.netCorrectionFactor = netCorrectionFactor;
        this.ioCorrectionFactor = ioCorrectionFactor;
        this.processingCost = networkCost + ioCost;
//        this.totalCost = (processingCost + workloadCost) * netCorrectionFactor;
        this.totalCost = (processingCost + workloadCost);
    }

    public double getNetCorrectionFactor() {
        return netCorrectionFactor;
    }

    public double getNetworkCost() {
        return networkCost;
    }

    public double getIoCost() {
        return ioCost;
    }

    public double getWorkloadCost() {
        return workloadCost;
    }

    public double getProcessingCost() {
        return processingCost;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public double getIoCorrectionFactor() {
        return ioCorrectionFactor;
    }

    @Override
    public String toString() {
        return "WorkCost{" +
                "networkCost=" + networkCost +
                ", ioCost=" + ioCost +
                ", workloadCost=" + workloadCost +
                '}';
    }
}
