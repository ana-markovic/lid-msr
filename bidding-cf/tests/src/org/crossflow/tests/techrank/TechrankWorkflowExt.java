package org.crossflow.tests.techrank;

import org.crossflow.runtime.BidCost;
import org.crossflow.runtime.Job;
import org.crossflow.runtime.Mode;
import org.crossflow.runtime.WorkCost;
import org.crossflow.runtime.utils.EstimationMetrics;

import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class TechrankWorkflowExt extends TechrankWorkflow {

    private static final int NUM_PAGES = 10;
    private static final int SLEEP_PER_PAGE_SECONDS = 60;
    public static final double GITHUB_API_SEARCH_TIME_SECONDS = NUM_PAGES * SLEEP_PER_PAGE_SECONDS;
    // Median IO Speed: 92ms
    public static final int FIXED_IO_COST = 92;
    public static final String REPOSITORIES_FOLDER_PATH = "";
    private final String writerFileName;
    public long net_bytesPerSecond = 30_000;
    public long io_bytesPerSecond = 1 << 27;
    public Set<String> downloaded = new HashSet<>();
    private double latencySec = 0;
    private double correctionFactor = 1.0;

    private FileWriter writer;
    public String repoInputFile;
    private int cacheMisses = 0;

    public long workTimeMillis;
    private BigInteger bytesLoaded = BigInteger.ZERO;

    private List<Double> correctionFactors = new ArrayList<>();

    private List<Double> localCorrectionFactors = new ArrayList<>();
    private List<Double> nonLocalCorrectionFactors = new ArrayList<>();
    private boolean weightedFactors;
    private long workflowStartTime;

    private List<Long> netSpeed = new ArrayList<>();
    private List<Long> ioSpeeds = new ArrayList<>();
    public Map<String, Long> repoSizes = new HashMap<>();

    public void setWeightedFactors() {
        this.weightedFactors = true;
    }

    public TechrankWorkflowExt(Mode m) {
        super(m);
        writerFileName = String.format(REPOSITORIES_FOLDER_PATH + "repositories_%s.txt", getInstanceId());
        loadRepositorySizes();
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

    public void loadRepositorySizes() {
        try {
            Scanner scanner = new Scanner(new File("repo_speed.txt"));
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] split = line.split(",");
                String repoName = split[0];
                long speed = Long.parseLong(split[1]);
                repoSizes.put(repoName, speed);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
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
//        if (job instanceof Technology) {
//            Technology technology = (Technology) job;
//            return calculateTechnologyCost(technology);
//        }
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
//        if (job instanceof Technology) {
//            Technology technology = (Technology) job;
//            return new BidCost(calculateTechnologyCost(technology), technology.getName());
//        }
        return new BidCost(Double.MAX_VALUE, "error");
    }

    public WorkCost calculateRepositoryProcessingCost(Repository repository) {
        double netCost = 0;
        double cf = 1.0;

        long repositorySize = getRepoSize(repository.getPath());

        if (repositorySize == -1) {
            repositorySize = repository.size;
        }


        if (isLocalResource(repository)) {
//            cf = getLocalCorrectionFactor();
        } else {
                cf = windowedCorrectionFactor();

//            netCost = (double) (repositorySize / getNetSpeed());
            double netSpeed = 33.0 * (Math.log(repositorySize / 1000.0) / Math.log(413));
            if (netSpeed < 0) {
                netSpeed = 0.5;
            }
            netCost = repositorySize / netSpeed;
            netCost *= cf;
        }

//        WorkCost workCost = new WorkCost(netCost, getIOSpeed(), queuedJobsTime(), cf);
        WorkCost workCost = new WorkCost(netCost, getIOSpeed(), queuedJobsTime(), cf, 1.0);
        repository.setCost(workCost.getProcessingCost());
        return workCost;
    }

    private double getLocalCorrectionFactor() {
        return localCorrectionFactors
                .stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(1.0);
    }

    public void addLocalCorrectionFactor(double cf) {
        localCorrectionFactors.add(cf);
    }

    public void addNonLocalCorrectionFactor(double cf) {
        nonLocalCorrectionFactors.add(cf);
    }


    private double getNonLocalCorrectionFactor() {
        return nonLocalCorrectionFactors
                .stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(1.0);
    }


    @Override
    protected double queuedJobsTime() {
        return currentWorkloadCost();
    }

    private double calculateTechnologyCost(Technology technology) {
        return currentWorkloadCost() + GITHUB_API_SEARCH_TIME_SECONDS;
    }

    private double currentWorkloadCost() {
        return jobsWaiting
                .values()
                .stream()
                .mapToDouble(Job::getCost)
                .sum();
    }

    private double calculateRepositoryBidCost(Repository repository) {
//        double cost = ((double) repository.size / getIOSpeed());

        double cost = getIOSpeed();

        double workloadCost = currentWorkloadCost();

        if (isLocalResource(repository)) {
            // TODO check if needed
            repository.setCost(cost);
//            double bidOffer = workloadCost + cost;// * correctionFactor;
            return (workloadCost + cost) * meanCorrectionFactor();
        }

        cost += 1.0 * (repository.size / getNetSpeed());
        // TODO check if needed
        repository.setCost(cost);
//        double bidOffer = cost + workloadCost;// * correctionFactor;
        return cost + workloadCost * meanCorrectionFactor();
    }

//    private boolean isLocalResource(Repository repository) {
//        return downloaded.contains(repository.path);
//    }



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

//    private boolean isLocalResource(Repository repository) {
//        try {
//            TechrankWorkflowContext context = new TechrankWorkflowContext(this);
//            File file = new File(context.getProperties().getProperty("clones") + "/" + getName() + "/" + UUID.nameUUIDFromBytes(repository.getPath().getBytes()));
//            return file.exists();
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }

    public void setNet_bytesPerSecond(long bytesPerSecond) {
        this.net_bytesPerSecond = bytesPerSecond;
    }

    public void setIo_bytesPerSecond(long io_bytesPerSecond) {
        this.io_bytesPerSecond = io_bytesPerSecond;
    }

    public void addIOSpeed(long speed) {
        ioSpeeds.add(speed);
    }

//    public void reportJobFinish(Job job, double actualExecTime) {
//        // TODO: Razmisli o tome da li treba += ili =
//        latencySec += job.getCost() - actualExecTime;
//        correctionFactor *= getAdjustedCorrectionFactor(job.getCost(), actualExecTime);
//        roundCorrectionFactor();
//    }

//    public synchronized void reportJobFinish(Job job, double actualExecTime) {
//        double estimatedCost = estimatedBids.getOrDefault(job.getJobId(), 0.0);
//        estimatedBids.remove(job.getJobId());
//        jobsWaiting.remove(job.getJobId());
//        addCorrectionFactor(getAdjustedCorrectionFactor(estimatedCost, actualExecTime));
//        sendEstimationMetrics(job, estimatedCost, actualExecTime);
//    }

    public synchronized void reportJobFinish(Job job, WorkCost actualWorkCost) {
        WorkCost estimated = estimatedWorkCosts.getOrDefault(job.getJobId(), WorkCost.EMPTY);
        estimatedBids.remove(job.getJobId());
        estimatedWorkCosts.remove(job.getJobId());
        jobsWaiting.remove(job.getJobId());
        if (actualWorkCost.getNetworkCost() == 0) {
            addLocalCorrectionFactor(getAdjustedCorrectionFactor(estimated.getProcessingCost(), actualWorkCost.getProcessingCost()));
        } else {
            addNonLocalCorrectionFactor(getAdjustedCorrectionFactor(estimated.getProcessingCost(), actualWorkCost.getProcessingCost()));
        }
        addCorrectionFactor(getAdjustedCorrectionFactor(estimated.getProcessingCost(), actualWorkCost.getProcessingCost()));
//        sendEstimationMetrics(job, estimated, actualWorkCost);
    }

    public synchronized void reportJobFinish(Job job, WorkCost actualWorkCost, String repoName, long repoSize) {
        WorkCost estimated = estimatedWorkCosts.get(job.getJobId());
        estimatedBids.remove(job.getJobId());
        estimatedWorkCosts.remove(job.getJobId());
        jobsWaiting.remove(job.getJobId());
        if (actualWorkCost.getNetworkCost() == 0) {
            addLocalCorrectionFactor(getAdjustedCorrectionFactor(estimated.getProcessingCost(), actualWorkCost.getProcessingCost()));
        } else {
            addNonLocalCorrectionFactor(getAdjustedCorrectionFactor(estimated.getProcessingCost(), actualWorkCost.getProcessingCost()));
        }
        addCorrectionFactor(getAdjustedCorrectionFactor(estimated.getProcessingCost(), actualWorkCost.getProcessingCost()));
        long repositorySize = getRepoSize(repoName);

        if (repositorySize == -1) {
            repositorySize = repoSize;
        }
        sendEstimationMetrics(job, estimated, actualWorkCost, repoName, repositorySize);
    }


    private void roundCorrectionFactor() {
        if (correctionFactor < 0.5) {
            correctionFactor = 0.5;
        }
        if (correctionFactor > 2) {
            correctionFactor = 2;
        }
    }

    private double getAdjustedCorrectionFactor(double jobCost, double actualExecTime) {

        // Convert to seconds
//        actualExecTime /= 1000;

        // Prevent division by zero
        if (jobCost == 0) {
            jobCost = 0.00001;
        }
        double cf = actualExecTime / jobCost;
        if (cf < 0.025) {
            cf = 0.025;
        }
        if (cf > 4) {
            cf = 4;
        }
        return cf;
    }

    public void appendStringToFile(String text) {

        try {
            writer = new FileWriter(writerFileName, true);
            writer.write(text + "\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


    public double getLatencySec() {
        return latencySec;
    }

    public String repoInputFile() {
        return repoInputFile;
    }

    public void printLocalWorkloadStatistics() {
        System.out.print(getName() + ": cachemisses: " + cacheMisses + ", dataloaded: " + bytesLoaded + ", worktime: " + workTimeMillis + "\n");
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

    public synchronized void addCorrectionFactor(double cf) {
        correctionFactors.add(cf);
    }

    public long getNetSpeed() {
        if (netSpeed.isEmpty()) {
            return net_bytesPerSecond;
        }
        long totalSpeed = netSpeed.stream()
                .mapToLong(Long::longValue)
                .sum();
        return totalSpeed / netSpeed.size();
    }

    public void addNetSpeed(long speed) {
        netSpeed.add(speed);
    }

    public long getIOSpeed() {
//        return 172;
        return SpeedUtils.getSpeed(172);
    }

    public synchronized double weighedLastThreeCF() {
        if (nonLocalCorrectionFactors.size() < 3) {
            return 1.0;
        }

        double third = nonLocalCorrectionFactors.get(nonLocalCorrectionFactors.size() - 1);
        double second = nonLocalCorrectionFactors.get(nonLocalCorrectionFactors.size() - 2);
        double first = nonLocalCorrectionFactors.get(nonLocalCorrectionFactors.size() - 3);

        return third * 0.6 + second * 0.3 + first * 0.1;
    }

    public synchronized double noCF() {
        return 1.0;
    }

    public synchronized double windowedCorrectionFactor() {
        if (nonLocalCorrectionFactors.size() < 2) {
            return 1.0;
        }
        double second = nonLocalCorrectionFactors.get(nonLocalCorrectionFactors.size() - 1);
        double first = nonLocalCorrectionFactors.get(nonLocalCorrectionFactors.size() - 2);

        return (first + second) / 2;
    }

    public synchronized double meanCorrectionFactor() {
//        return 1.0;
//        if (weightedFactors) {
//            return weightedCorrectionFactor();
//        }
//        return weightedCorrectionFactor();
//        return productCorrectionFactor();
        return nonLocalCorrectionFactors
                .stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(1.0);
    }

    private double productCorrectionFactor() {
        if (correctionFactors.isEmpty()) {
            return 1.0;
        }
        double product = 1.0;
        for (Double correctionFactor : correctionFactors) {
            product *= correctionFactor;
        }
        if (product < 0.5) {
            product = 0.5;
        }
        if (product > 2) {
            product = 2;
        }
        return product;
    }

    private double weightedCorrectionFactor() {
        if (correctionFactors.isEmpty()) {
            return 1.0;
        }

        double sum = 0.0;
        double weight = 0.0;
        for (int i = 0; i < correctionFactors.size(); i++) {
            double currentValue = correctionFactors.get(i);
            double currentWeight = i + 1;
            sum += currentValue * currentWeight;
            weight += currentWeight;
        }
        double cf = sum / weight;
//        if (cf < 0.5) {
//            cf = 0.5;
//        }
//        if (cf > 2) {
//            cf = 2;
//        }
        return cf;
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
        this.workflowStartTime = System.currentTimeMillis();
    }

    public long getWorkflowTime() {
        return System.currentTimeMillis() - workflowStartTime;
    }
}
