package org.crossflow.tests.techrank;

import org.crossflow.runtime.BidCost;
import org.crossflow.runtime.Job;
import org.crossflow.runtime.Mode;
import org.crossflow.runtime.WorkCost;
import org.crossflow.runtime.utils.EstimationMetrics;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class TechrankWorkflowExt extends TechrankWorkflow {
    private final String writerFileName;

    public long net_bytesPerSecond = 1 << 21; // 2MB / s
    public long io_bytesPerSecond = 1 << 27; // 128 MB / s
    public Set<String> downloaded = new HashSet<>();

    public String repoInputFile;
    private int cacheMisses = 0;

    public long workTimeMillis;
    private BigInteger bytesLoaded = BigInteger.ZERO;

    public Map<String, Long> repoSizes = new HashMap<>();
    private long workflowStartTime;

    public TechrankWorkflowExt(Mode m) {
        super(m);
        writerFileName = String.format("/Users/ana/Desktop/repositories_%s.txt", getInstanceId());
    }

    @Override
    public synchronized void terminate() {
        if (getResultsSink() != null) {
            this.getResultsSink().printMatrix();
            getResultsSink().printIndividualTechnologiesCount();
        }
        saveDownloadedRepositories();
        if (!isMaster()) printWorkerStatistics();
        if (isMaster()) {
            saveMetricsCsv();
            saveBidsCsv();
            saveWinningBidsCsv();
            printWorkflowExecutionTime();
            printEstimationMetricsCSV();
        }
        super.terminate();
    }

    public long getRepoSize(String repoName) {
        return repoSizes.getOrDefault(repoName, -1L);
    }

    private void printWorkflowExecutionTime() {
        System.out.println("Workflow execution time: " + getWorkflowTime());
    }

    public void printEstimationMetricsCSV() {
        String HEADER = EstimationMetrics.CSV_HEADER;
        String fileName = "estimation_metrics.csv";
        try {
            String csv = estimationMetrics.stream()
                    .map(EstimationMetrics::toCSV)
                    .collect(Collectors.joining("\n"));
            Files.write(Paths.get(fileName), List.of(HEADER, csv));
            System.out.println("Saved " + fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected double calculateJobCost(Job job) {
        if (job instanceof Repository) {
            Repository repository = (Repository) job;
            return calculateRepositoryBidCost(repository);
        }

        if (job instanceof RepositorySearchResult) {
            return 1.0;
        }
        return Double.MAX_VALUE;
    }


    @Override
    protected BidCost calculateProcessingTime(Job job) {
        if (job instanceof Repository) {
            Repository repository = (Repository) job;
            return new BidCost(calculateRepositoryProcessingCost(repository), repository.getPath());
        }

        if (job instanceof RepositorySearchResult) {
            RepositorySearchResult repositorySearchResult = (RepositorySearchResult) job;
            return new BidCost(1.0, repositorySearchResult.getRepository());
        }

        return new BidCost(Double.MAX_VALUE, "error");
    }

    public WorkCost calculateRepositoryProcessingCost(Repository repository) {
        double netCost = 0;

        if (!isLocalResource(repository)) {
            netCost = 1.0 * repository.size / net_bytesPerSecond;
        }

        double ioCost = 1.0 * repository.size / io_bytesPerSecond;

        WorkCost workCost = new WorkCost(netCost, ioCost, queuedJobsTime(), 1.0, 1.0);
        repository.setCost(workCost.getProcessingCost());
        return workCost;
    }


    @Override
    protected double queuedJobsTime() {
        return currentWorkloadCost();
    }

    private double currentWorkloadCost() {
        return jobsWaiting
                .values()
                .stream()
                .mapToDouble(Job::getCost)
                .sum();
    }

    private double calculateRepositoryBidCost(Repository repository) {
        double cost = 1.0 * repository.size / io_bytesPerSecond;
        double workloadCost = currentWorkloadCost();

        if (!isLocalResource(repository)) {
            cost += ((double) repository.size / net_bytesPerSecond);
        }

        repository.setCost(cost);
        return cost + workloadCost;
    }

    private boolean isLocalResource(Repository repository) {
        if (jobNameExists(repository.getPath())) {
            return true;
        }
        String parentFolder = getStorageDir() + "/" + getName();
        File clone = new File(parentFolder + "/" + UUID.nameUUIDFromBytes(repository.getPath().getBytes()));
        return clone.exists();
    }

    public void loadDownloadedRepositories() {
        if (isMaster()) {
            return;
        }

        try {
            File file = new File(String.format("%s_downloaded_repositories.txt", getName()));
            if (file.exists()) {
                Scanner scanner = new Scanner(file);
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    downloaded.add(line);
                }
                scanner.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void saveDownloadedRepositories() {
        try {
            File file = new File(String.format("%s_downloaded_repositories.txt", getName()));
            FileWriter writer = new FileWriter(file);
            for (String path : downloaded) {
                writer.write(path + "\n");
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setNet_bytesPerSecond(long bytesPerSecond) {
        this.net_bytesPerSecond = bytesPerSecond;
    }

    public void setIo_bytesPerSecond(long io_bytesPerSecond) {
        this.io_bytesPerSecond = io_bytesPerSecond;
    }

    public void addLocalWorkTime(long milis) {
        workTimeMillis += milis;
    }

    public void addCacheData(int numMisses, long dataLoaded) {
        cacheMisses += numMisses;
        bytesLoaded = bytesLoaded.add(BigInteger.valueOf(dataLoaded));
    }

    public void printWorkerStatistics() {
        String fileName = String.format("statistics-i%s-%s-%s-%s-%s.txt", iteration, algorithm, jobConf, workerConf, getName());
        try {
            Files.write(Paths.get(fileName), getWorkerStatistics().getBytes());
            System.out.println("Saved " + fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getWorkerStatistics() {
        return String.format("workername: %s, cachemisses: %s, dataloaded: %s, worktime: %s\n", getName(), cacheMisses, bytesLoaded, workTimeMillis);
    }

    public void printMasterStatistics(String masterStatistics) {
        String fileName = String.format("statistics-i%s-%s-%s-%s-master.txt", iteration, algorithm, jobConf, workerConf);
        try {
            Files.write(Paths.get(fileName), masterStatistics.getBytes());
            System.out.println("Saved " + fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public long getNetSpeed() {
        return SpeedUtils.getSpeed(net_bytesPerSecond);
    }

    public long getIOSpeed() {
        return SpeedUtils.getSpeed(io_bytesPerSecond);
    }

    public void setupSpeed() {
        Scanner scanner;
        try {
            String speedLocation = "/home/ubuntu/";
            if (System.getenv().containsKey("SPEED_LOCATION")) {
                speedLocation = System.getenv("SPEED_LOCATION");
            }
            scanner = new Scanner(new File(speedLocation + getName() + "-speed.txt"));
            String netSpeedStr = scanner.nextLine();
            String ioSpeedStr = scanner.nextLine();
            setIo_bytesPerSecond(Long.parseLong(ioSpeedStr));
            setNet_bytesPerSecond(Long.parseLong(netSpeedStr));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void startTimer() {
        workflowStartTime = System.currentTimeMillis();
    }

    public long getWorkflowTime() {
        return System.currentTimeMillis() - workflowStartTime;
    }

}
