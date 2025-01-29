package org.crossflow.tests.techrankHistoric;

import org.crossflow.runtime.BidCost;
import org.crossflow.runtime.Job;
import org.crossflow.runtime.Mode;
import org.crossflow.runtime.WorkCost;
import org.crossflow.runtime.utils.ExtendedEstimationMetric;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class HistoryTechrankWorkflowExt extends HistoryTechrankWorkflow {

    private static final long DEFAULT_LINES_PER_SECOND = 19_000;
    public static final int MIN_LINREG_SIZE = 3;
    public static final int NET_WINDOW_SIZE = 3;

    private final String workerType;
    public long net_bytesPerSecond = 30;
    public long io_bytesPerSecond = 1 << 27;
    private double latencySec = 0;
    private double correctionFactor = 1.0;

    public String repoInputFile;
    private int cacheMisses = 0;

    public long workTimeMillis = 0;
    private BigInteger bytesLoaded = BigInteger.ZERO;

    private List<Double> correctionFactors = new ArrayList<>();

    private List<Double> localCorrectionFactors = new ArrayList<>();
    private List<Double> nonLocalCorrectionFactors = new ArrayList<>();
    private long workflowStartTime;

    private List<Long> netSpeed = new ArrayList<>();
    private List<Long> ioSpeeds = new ArrayList<>();
    private Map<String, Integer> analysedRepoCount = new HashMap<>();

    public Map<String, Long> repoSizes = new HashMap<>();
    private Map<String, Integer> netSleepTime = new HashMap<>();
    private Map<String, Integer> ioSleepTime = new HashMap<>();
    private Map<String, Integer> lineCounts = new HashMap<>();

    private LinearRegression myLinearRegression = null;

    private List<LinRegPoint> linRegPoints = new ArrayList<>();

    private String algorithm = "NO_CF";

    private long storageSize; // 180 GB
    private double STORAGE_COST; // 0.0001 per GB / per hour
    private double CPU_COST; // 0.0001 per CPU / per hour

    private double lineCountCf = new Random().nextDouble();
    private Random random = new Random();

    // repository,repositorySize,commitHash,lineCount
    public ConcurrentHashMap<String, List<String>> fakeCommitHashes = new ConcurrentHashMap<>();
    private static final String REPO_CSV = "fake_repositories.csv";


    public HistoryTechrankWorkflowExt(Mode m) {
        super(m);
        algorithm = System.getenv().getOrDefault("CF_TYPE", "NO_CF");
        workerType = System.getenv().getOrDefault("WORKER_TYPE", "NORMAL");
        storageSize = Long.parseLong(System.getenv().getOrDefault("STORAGE_SIZE_GB", "180")) * 1000 * 1000;
        STORAGE_COST = Double.parseDouble(System.getenv().getOrDefault("STORAGE_COST_PER_HOUR", "0.001"));
        CPU_COST = Double.parseDouble(System.getenv().getOrDefault("CPU_COST_PER_HOUR", "0.024"));
        loadRepositories();
    }

    public void addAnalysedRepo(String repoName) {
        int count = analysedRepoCount.getOrDefault(repoName, 0);
        analysedRepoCount.put(repoName, count + 1);
    }

    public int getAnalysedRepoCount(String repoName) {
        return analysedRepoCount.getOrDefault(repoName, 0);
    }

    public void updateLatencySec(double newLatency) {
        this.latencySec = newLatency;
    }

    private double getTotalCost() {
        double storageCost = getStorageCost();
        double cpuCost = getCpuCost();
        return storageCost + cpuCost;
    }

    private double getCpuCost() {
        return workTimeHours() * CPU_COST;
    }

    private double getStorageCost() {
        return storageSize * STORAGE_COST * workTimeHours();
    }

    private double workTimeHours() {
        return getWorkflowTime() / (60 * 60 * 1000.0);
    }

    @Override
    public synchronized void terminate() {
        if (getResultsSink() != null) {
            this.getResultsSink().printMatrix();
            this.getResultsSink().printToCSV();
        }
        if (!isMaster()) printWorkerStatistics();
        if (isMaster()) {
            saveMetricsCsv();
            saveBidsCsv();
            saveWinningBidsCsv();
            printExtendedEstimationMetrics();
        }
        System.out.println("Total work time: " + workTimeMillis);
        super.terminate();
        if (isMaster()) printWorkflowExecutionTime();
    }


    private void printWorkflowExecutionTime() {
        System.out.println("Workflow execution time: " + getWorkflowTime());
    }

    public void printExtendedEstimationMetrics() {
        String HEADER = ExtendedEstimationMetric.CSV_HEADER;
        String fileName = "extended_estimation_metrics.csv";
        try {
            String csv = extendedEstimationMetrics.stream()
                    .map(ExtendedEstimationMetric::toCSV)
                    .collect(Collectors.joining("\n"));
            Files.write(Paths.get(fileName), List.of(HEADER, csv));
            System.out.println("Saved " + fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private WorkCost calculateCommitAnalysisBidCost(CommitAnalysis commitAnalysis) {
        double netCost = commitAnalysisNetCost(commitAnalysis);

        if (Double.MAX_VALUE == netCost) {
            return new WorkCost(Double.MAX_VALUE, 0, 0, 1.0, 1.0);
        }

        double ioCost = commitAnalysisIOCost(commitAnalysis);
        double workloadCost = currentWorkloadCost();

        double netCf = 1.0;
        if (!isLocalResource(commitAnalysis.repository)) {
            netCf = getNonLocalCorrectionFactor();
        }
        double ioCf = getLocalCorrectionFactor();

        WorkCost workCost = new WorkCost(netCost, ioCost, workloadCost, netCf, ioCf);
        commitAnalysis.setCost(workCost.getProcessingCost());
        return workCost;
    }

    private double commitAnalysisIOCost(CommitAnalysis commitAnalysis) {
        if (algorithm.equals("NO_CF")) {
//            return 0.75 * commitAnalysis.lineCount;
            return 3 * commitAnalysis.lineCount;
        }
        if (algorithm.equals("MEAN")) {
            return meanPredict(commitAnalysis.lineCount);
        }
        if (algorithm.equals("LIN_REG")) {
            return linRegPredict(commitAnalysis.lineCount);
        }
        throw new RuntimeException("Unknown algorithm: " + algorithm);
    }

    private double commitAnalysisNetCost(CommitAnalysis commitAnalysis) {
        if (isLocalResource(commitAnalysis.repository)) {
            return 0;
        }
        return calculateRepositoryDownloadCost(commitAnalysis.repository, this::isLocalResource);
    }

    /**
     * Calculate the cost of processing a job. Bids are expressed in milliseconds.
     *
     * @param job Job to be processed
     * @return BidCost with bid in milliseconds and job name
     */
    @Override
    protected BidCost calculateProcessingTime(Job job) {
        if (job instanceof Repository) {
            Repository repository = (Repository) job;
            return new BidCost(calculateRepositoryProcessingCost(repository), repository.getPath());
        }

        if (job instanceof RepositorySearchResult) {
            RepositorySearchResult repositorySearchResult = (RepositorySearchResult) job;
            return new BidCost(1.0, repositorySearchResult.getRepository().getPath());
        }

        if (job instanceof CommitAnalysis) {
            CommitAnalysis commitAnalysis = (CommitAnalysis) job;
            String jobName = commitAnalysis.repository.getPath() + "__" + commitAnalysis.commitHash;
            return new BidCost(calculateCommitAnalysisBidCost(commitAnalysis), jobName);
        }

        if (job instanceof CommitAnalysisResult) {
            CommitAnalysisResult commitAnalysisResult = (CommitAnalysisResult) job;
            return new BidCost(1.0, commitAnalysisResult.repositoryName);
        }

        return new BidCost(Double.MAX_VALUE, "error");
    }

    public WorkCost calculateRepositoryProcessingCost(Repository repository) {
        double netCost = 0;
        double cf = 1.0;

        if (!isLocalResource(repository)) {
            cf = getNonLocalCorrectionFactor();
            netCost = calculateRepositoryDownloadCost(repository, this::isLocalResource);
        }

        WorkCost workCost = new WorkCost(netCost, 0, queuedJobsTime(), cf, 1.0);
        repository.setCost(workCost.getProcessingCost());
        return workCost;
    }

    private boolean notEnoughStorageFor(Repository repository) {
        long storageLeftKb = storageSize - getStorageUsed();
        return storageLeftKb < repository.size;
    }

    private long getStorageUsed() {
        return jobNames.stream()
                .map(repoName -> {
                    String[] split = repoName.split("__");
                    return split[0];
                })
                .distinct()
                .map(this::getFakeRepositorySize)
                .mapToInt(Integer::intValue)
                .sum();
    }

    private double calculateRepositoryDownloadCost(Repository repository, Predicate<Repository> isLocalRepository) {
        if (isLocalRepository.negate().test(repository)
                && notEnoughStorageFor(repository)) {
            return Double.MAX_VALUE;
        }

        double netCost = 0;

        long repoSizeKB = repository.size;

        if (isLocalRepository.negate().test(repository)) {
            double cf = getNonLocalCorrectionFactor();

            var repoSizeMB = repoSizeKB / 1000.0;

            double netSpeed = (33.0 * (Math.log(repoSizeMB) / Math.log(413))) / 3;

//            if (algorithm.equals("LIN_REG")) {
//                netSpeed = getNetSpeed();
//          }

            if (netSpeed < 0) {
                netSpeed = 0.5;
            }
            netCost = (repoSizeKB / netSpeed);
            netCost *= cf;
        }

        if (Double.isNaN(netCost)) {
            netCost = 0;
        }
        return netCost;
    }

    private double getLocalCorrectionFactor() {
        return ioNoCF();
//        return meanIOCorrectionFactor();
    }

    public void addLocalCorrectionFactor(double cf) {
        localCorrectionFactors.add(cf);
    }

    public void addNonLocalCorrectionFactor(double cf) {
        nonLocalCorrectionFactors.add(cf);
    }


    private double getNonLocalCorrectionFactor() {
//        return meanNetCorrectionFactor();
        return noCF();
    }


    @Override
    protected double queuedJobsTime() {
        return currentWorkloadCost();
    }

    private synchronized double currentWorkloadCost() {

        List<Job> jobList = new ArrayList<>();

        for (Map.Entry<String, Job> entry : jobsWaiting.entrySet()) {
            Job job = entry.getValue();
            if (job instanceof Repository) {
                Repository repository = (Repository) job;
                double netCost = 0;
                if (!repositoryFolderExists(repository)) {
                    netCost = calculateRepositoryDownloadCost(repository, this::repositoryFolderExists);
                }
                job.setCost(netCost);
            }
            if (job instanceof CommitAnalysis) {
                CommitAnalysis analysis = (CommitAnalysis) job;
                double netCost = 0;
                if (!isLocalResource(analysis.repository)) {
                    netCost = commitAnalysisNetCost(analysis);
                }
                double totalCost = netCost + commitAnalysisIOCost(analysis);
                job.setCost(totalCost);
            }
            jobList.add(job);
        }

        jobsWaiting = new ConcurrentHashMap<>();
        jobList.forEach(job -> jobsWaiting.put(job.getJobId(), job));
        return jobList.stream()
                .mapToDouble(Job::getCost)
                .sum();
    }

    private boolean isLocalResource(Repository repository) {
        if (jobNameExists(repository.getPath())) {
            return true;
        }
        return repositoryFolderExists(repository);
    }

    private boolean repositoryFolderExists(Repository repository) {
        String parentFolder = getStorageDir() + "/" + getName();
        File clone = new File(parentFolder + "/" + UUID.nameUUIDFromBytes(repository.getPath().getBytes()));
        return clone.exists();
    }

    public void setNet_bytesPerSecond(long bytesPerSecond) {
        this.net_bytesPerSecond = bytesPerSecond;
    }

    public void setIo_bytesPerSecond(long io_bytesPerSecond) {
        this.io_bytesPerSecond = io_bytesPerSecond;
    }

    public void addIOSpeed(long speed) {
        ioSpeeds.add(speed);
    }

    public synchronized void reportJobFinish(Job job, WorkCost actualWorkCost, String repoName, long repoSize, int fileCount, int lineCount, String commitHash) {
        WorkCost estimated = estimatedWorkCosts.get(job.getJobId());
        estimatedWorkCosts.remove(job.getJobId());
        jobsWaiting.remove(job.getJobId());
        if (actualWorkCost.getNetworkCost() == 0) {
            addLocalCorrectionFactor(getAdjustedCorrectionFactor(estimated.getProcessingCost(), actualWorkCost.getProcessingCost()));
        } else {
            addNonLocalCorrectionFactor(getAdjustedCorrectionFactor(estimated.getProcessingCost(), actualWorkCost.getProcessingCost()));
        }
        sendExtendedEstimationMetrics(job, estimated, actualWorkCost, repoName, repoSize, fileCount, lineCount, commitHash, -1, -1);
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


    public double getLatencySec() {
        return latencySec;
    }

    public String repoInputFile() {
        return repoInputFile;
    }

    public void printLocalWorkloadStatistics() {
        System.out.print(getName() + ": cachemisses: " + cacheMisses + ", dataloaded: " + bytesLoaded + ", worktime: " + workTimeMillis + "\n");
    }

    public void addLocalWorkTime(long millis) {
        workTimeMillis += millis;
    }

    public void addCacheData(int numMisses, long dataLoaded) {
        cacheMisses += numMisses;
        bytesLoaded = bytesLoaded.add(BigInteger.valueOf(dataLoaded));
    }

    public void printWorkerStatistics() {
        String fileName = String.format("statistics-i%s-%s-%s-%s-%s.txt", iteration, algorithm, jobConf, workerConf, getName());
        try (PrintWriter writer = new PrintWriter(fileName)) {
            writer.println(getWorkerStatistics());
            writer.println(getCostStatistics());
            System.out.println("Saved " + fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getCostStatistics() {
        return String.format("cpu_cost: %s, storage_cost: %s, total_cost: %s", getCpuCost(), getStorageCost(), getTotalCost());
    }

    private String getWorkerStatistics() {
        return String.format("workername: %s, cachemisses: %s, dataloaded: %s, worktime: %s", getName(), cacheMisses, bytesLoaded, workTimeMillis);

    }

    public synchronized void addCorrectionFactor(double cf) {
        correctionFactors.add(cf);
    }

    public long getNetSpeed() {
        if (netSpeed.isEmpty() || netSpeed.size() < NET_WINDOW_SIZE) {
            return net_bytesPerSecond;
        }

        double sum = 0;
        for (int i = netSpeed.size() - 1; i >= netSpeed.size() - NET_WINDOW_SIZE; i--) {
            sum += netSpeed.get(i);
        }

        return (long) (sum / NET_WINDOW_SIZE);
    }

    public void addNetSpeed(long speed) {
        netSpeed.add(speed);
    }

    public long getIOSpeed() {
        if (ioSpeeds.isEmpty()) {
            return DEFAULT_LINES_PER_SECOND;
        }
        long totalSpeed = ioSpeeds.stream()
                .mapToLong(Long::longValue)
                .sum();

        if (totalSpeed == 0) {
            totalSpeed = 1;
        }

        return totalSpeed / ioSpeeds.size();
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

    public synchronized double ioNoCF() {
        return 1.0;
    }

    public synchronized double meanIOCorrectionFactor() {
        return localCorrectionFactors.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(1.0);
    }

    public synchronized double meanNetCorrectionFactor() {
        return nonLocalCorrectionFactors
                .stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(1.0);
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

    public String getStorageDirPath(Repository repository) {
        return getStorageDir() +
                File.separator +
                getName() +
                File.separator +
                UUID.nameUUIDFromBytes(repository.getPath().getBytes());
    }

    public void reportIOJobFinish(CommitAnalysis commitAnalysis, long ioDuration, long netDuration) {
        WorkCost estimated = estimatedWorkCosts.get(commitAnalysis.getJobId());
        estimatedWorkCosts.remove(commitAnalysis.getJobId());
        jobsWaiting.remove(commitAnalysis.getJobId());

        updateLinearRegression(commitAnalysis, ioDuration);

        WorkCost actual = new WorkCost(netDuration, ioDuration, 0, 1.0, 1.0);
        double intercept = -1;
        double slope = -1;

        if (linRegPoints.size() >= 2
                && !Double.isNaN(myLinearRegression.intercept())
                && !Double.isNaN(myLinearRegression.slope())) {
            intercept = myLinearRegression.intercept();
            slope = myLinearRegression.slope();
        }

        sendExtendedEstimationMetrics(commitAnalysis, estimated, actual, commitAnalysis.repository.path,
                commitAnalysis.repoSizeKB, commitAnalysis.fileCount, commitAnalysis.lineCount,
                commitAnalysis.commitHash, intercept, slope);
    }

    private double getMeanSlope() {
        double sum = 0;
        for (int i = 0; i < linRegPoints.size(); i++) {
            sum += linRegPoints.get(i).getY() / linRegPoints.get(i).getX();
        }
        return sum / linRegPoints.size();
    }

    public void loadSleepTimes() {
        try {
            Scanner scanner = new Scanner(new File("sleep_times.csv"));
            scanner.nextLine();
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] split = line.split(",");
                String repoName = split[0];
                long repoSize = Long.parseLong(split[1]);
                int lineCount = Integer.parseInt(split[2]);
                int ioSleep = Integer.parseInt(split[3]);
                int netSleep = Integer.parseInt(split[4]);
                netSleepTime.put(repoName, netSleep);
                ioSleepTime.put(repoName, ioSleep);
                lineCounts.put(repoName, lineCount);
                repoSizes.put(repoName, repoSize);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateLinearRegression(CommitAnalysis commitAnalysis, double y) {
        double x = commitAnalysis.lineCount;

        if (hasBeenProcessed(commitAnalysis.repository)) {
            return;
        }

        if (duplicateDataPoint(x, y)) {
            return;
        }

        linRegPoints.add(new LinRegPoint(x, y, commitAnalysis.repository.getPath()));

        if (linRegPoints.size() < 2) {
            return;
        }

        double[] xs2 = linRegPoints.stream().mapToDouble(LinRegPoint::getX).toArray();
        double[] ys2 = linRegPoints.stream().mapToDouble(LinRegPoint::getY).toArray();
        myLinearRegression = new LinearRegression(xs2, ys2);
    }

    private boolean hasBeenProcessed(Repository repository) {
        String repositoryPath = repository.getPath();
        return linRegPoints.stream()
                .anyMatch(linRegPoint -> linRegPoint.getRepository().equals(repositoryPath));
    }

    private boolean duplicateDataPoint(double x, double y) {
        for (int i = 0; i < linRegPoints.size(); i++) {
            if (linRegPoints.get(i).getX() == x && linRegPoints.get(i).getY() == y) {
                return true;
            }
        }
        return false;
    }

    public double linRegPredict(int lineCount) {
        if (linRegPoints.size() < MIN_LINREG_SIZE
                || Double.isNaN(myLinearRegression.intercept())
                || Double.isNaN(myLinearRegression.slope())
                || myLinearRegression.slope() <= 0.001) {
            return 3 * lineCount;
        }

        double predict = myLinearRegression.predict(lineCount);
        if (predict < 0) {
            predict -= 2 * myLinearRegression.intercept();
        }
        return predict;
    }

    public double meanPredict(int lineCount) {
        int n = linRegPoints.size();

        if (n < 2) {
            return 0.75 * lineCount;
        }

        double sum = 0;
        for (int i = 0; i < n; i++) {
            sum += (linRegPoints.get(i).getY() / linRegPoints.get(i).getX());
        }

        return sum / n * lineCount;
    }

    public int getIOSleepTime(String repoName) {
        return ioSleepTime.get(repoName);
    }

    public int getNetSleepTime(String repoName) {
        return netSleepTime.get(repoName);
    }

    public int getLineCount(String repoName) {
        return lineCounts.get(repoName);
    }

    public long getRepoSize(String repoName) {
        return repoSizes.get(repoName);
    }

    public String getWorkerType() {
        return workerType;
    }

    public int getFakeLineCount(Repository repository, String fakeCommitHash) {
        Optional<String> commitHashLine = fakeCommitHashes.get(repository.path)
                .stream()
                .filter(line -> line.contains(fakeCommitHash))
                .findFirst();
        String line = commitHashLine.orElseThrow(() -> new RuntimeException("No fake commit hash found"));
        return Integer.parseInt(line.split(",")[3]);
    }

    public Integer getFakeRepositorySize(String repositoryName) {
        List<String> fakeCommits = fakeCommitHashes.get(repositoryName);
        return Integer.parseInt(fakeCommits.get(0).split(",")[1]);
    }


    public String getFakeCommitHash(Repository repository, int i) {
        List<String> fakeCommits = fakeCommitHashes.get(repository.path);
        return fakeCommits.get(i).split(",")[2];
    }

    public void loadRepositories() {
        try (var lines = Files.lines(Path.of(REPO_CSV))) {
            lines.forEach(line -> {
                String[] split = line.split(",");
                String repoName = split[0];
                if (!fakeCommitHashes.containsKey(repoName)) {
                    fakeCommitHashes.put(repoName, new ArrayList<>());
                }
                fakeCommitHashes.get(repoName).add(line);
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
