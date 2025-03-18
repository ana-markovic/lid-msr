package org.crossflow.tests.techrank;

import org.crossflow.runtime.CrossflowMetricsBuilder;
import org.crossflow.runtime.WorkCost;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class RepositorySearcher extends CommitmentRepositorySearcherBase {


    private TechrankWorkflowContext context;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();


    private List<String> libraries = List.of(
            "express",
            "async",
            "react",
            "lodash",
            "cloudinary",
            "axios",
            "karma",
            "moleculer",
            "grunt",
            "pm2",
            "mocha",
            "moment",
            "babel",
            "socket.io",
            "mongoose",
            "bluebird",
            "redux",
            "jest",
            "webpack",
            "graphql",
            "redux-saga",
            "nodemailer",
            "react-router",
            "react-native",
            "cheerio",
            "dotenv",
            "passport",
            "winston",
            "sharp",
            "puppeteer"
    );

    private CrossflowMetricsBuilder currentMetricBuilder;

    private double getFactoredNoise(double speed) {
        double factor = new Random().nextDouble() * 0.3 + 1;
        return speed * factor;
    }

    public RepositorySearchResult fakeSearch(Repository repository) throws Exception {
        LocalDateTime startTime = LocalDateTime.now();

        currentMetricBuilder = new CrossflowMetricsBuilder();
        currentMetricBuilder
                .setWorkerId(getWorkflow().getName())
                .setRepositoryName(repository.path)
                .setRepositorySizeBytes(repository.size)
                .setJobStartTime(startTime)
                .setCurrentLatency(((TechrankWorkflowExt) workflow).getLatencySec());

        RepositorySearchResult result = new RepositorySearchResult();
        result.setRepository(repository.path);

        long netStart = System.currentTimeMillis();
        long netProcessingTime = 0;

        long start = System.currentTimeMillis();
        if (!alreadyDownloaded(repository)) {
            fakeDownload(repository);
            ((TechrankWorkflowExt) getWorkflow()).addCacheData(1, repository.size);
            netProcessingTime = System.currentTimeMillis() - netStart;
        } else {
            netProcessingTime = 0;
        }

        long ioStart = System.currentTimeMillis();

        fakeIOProcess(repository);
        long ioProcessingTime = System.currentTimeMillis() - ioStart;
        getWorkflow().sendMetric(currentMetricBuilder.createCrossflowMetrics());
        ((TechrankWorkflowExt) getWorkflow()).addLocalWorkTime(System.currentTimeMillis() - start);
        long execTime = System.currentTimeMillis() - start;
//        ((TechrankWorkflowExt) workflow).reportJobFinish(repository, execTime);
        WorkCost workCost = new WorkCost(netProcessingTime, ioProcessingTime, 0, 1.0, 1.0);
        ((TechrankWorkflowExt) workflow).reportJobFinish(repository, workCost, repository.path, repository.size);
        return result;
    }

    public RepositorySearchResult actuallySearch(Repository repository) throws Exception {
        context = new TechrankWorkflowContext(workflow);
        String parentFolder = workflow.getStorageDir() + "/" + workflow.getName();
//        String parentFolder = context.getProperties().getProperty("clones") + "/" + workflow.getName();

        if (!new File(parentFolder).exists()) {
            new File(parentFolder).mkdirs();
        }

        long start = System.currentTimeMillis();
        LocalDateTime startTime = LocalDateTime.now();

        long downloadTime = -1;
        int cacheMisses = 0;

        double netCost = 0;

        File clone = new File(parentFolder + "/" + UUID.nameUUIDFromBytes(repository.getPath().getBytes()));

        if (!clone.exists()) {
            Files.createDirectories(clone.toPath());
            cacheMisses++;
            ((TechrankWorkflowExt) getWorkflow()).addCacheData(1, repository.size);

            try {

                try {
                    Process process = Runtime.getRuntime().exec("git clone --depth 1 " + "https://github.com/" +
                            repository.getPath() + ".git " + clone.getAbsolutePath());
                    process.waitFor();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }

            downloadTime = System.currentTimeMillis() - start;
            netCost = downloadTime;
            long dlTimeSeconds = (downloadTime / 1000) > 0 ? (downloadTime / 1000) : 1;
            long netKbps = repository.size / dlTimeSeconds;
//                ((TechrankWorkflowExt) getWorkflow()).setNet_bytesPerSecond(netKbps);
            ((TechrankWorkflowExt) getWorkflow()).addNetSpeed(netKbps);
        }

        currentMetricBuilder = new CrossflowMetricsBuilder();
        currentMetricBuilder
                .setWorkerId(getWorkflow().getName())
                .setRepositoryName(repository.path)
                .setRepositorySizeBytes(repository.size)
                .setCurrentLatency(((TechrankWorkflowExt) workflow).getLatencySec())
                .setJobStartTime(startTime);


        long ioProcessTime = 0;
//        for (Technology technology : repository.getTechnologies()) {
        long ioStartTime = System.currentTimeMillis();
//            if (countFiles(clone, technology) > 0) {
        if (checkForLibrary(clone, repository.library)) {
            ioProcessTime = (System.currentTimeMillis() - ioStartTime);
            ioProcessTime = ioProcessTime == 0 ? 1 : ioProcessTime;

            // Set ioSpeed
//            long speedMilis = repository.size / ioProcessTime;
//            ((TechrankWorkflowExt) getWorkflow()).addIOSpeed(speedMilis * 1000);
//            ((TechrankWorkflowExt) getWorkflow()).setIo_bytesPerSecond(speedMilis * 1000);

            ((TechrankWorkflowExt) getWorkflow()).addIOSpeed(ioProcessTime);

            RepositorySearchResult result = new RepositorySearchResult();
            result.setRepository(repository.getPath());
            RepositorySearchResult repositorySearchResult = new RepositorySearchResult(repository.library, 1, repository.path);
            sendToRepositorySearchResults(repositorySearchResult);



            /* TODO: See why grep has stopped working (it returns 0 results even when the terminal says otherwise
			try {
				String grep = "grep -r -l --include=\"*." + technology.getExtension() + "\" \"" +
						technology.getKeyword() + "\" " + clone.getAbsolutePath();

				System.out.println("Grepping: " + grep);

				Process process = Runtime.getRuntime().exec(grep);


				BufferedReader processInputStream = new BufferedReader(new InputStreamReader(process.getInputStream()));
				BufferedReader processErrorStream = new BufferedReader(new InputStreamReader(process.getErrorStream()));


				int files = 0;
				String s;
				while ((s = processInputStream.readLine()) != null) {
					System.out.println("Found: " + s);
					files++;
				}

				String e;
				while ((e = processErrorStream.readLine()) != null) {
					System.out.println("Error: " + e);
				}



				RepositorySearchResult result = new RepositorySearchResult(technology.getName(), files, repositorySearch);
				sendToRepositorySearchResults(result);

			}
			catch (Exception ex) {
				System.out.println("Falling back to file-by-file searching because " + ex.getMessage());*/
//			RepositorySearchResult repositorySearchResult = new RepositorySearchResult(technology.getName(), 1, repositorySearch.repository);
//			sendToRepositorySearchResults(repositorySearchResult);
            //}
        }

        ioProcessTime = (System.currentTimeMillis() - ioStartTime);
        ioProcessTime = ioProcessTime == 0 ? 1 : ioProcessTime;
//        long speedMilis = repository.size / ioProcessTime;
        ((TechrankWorkflowExt) getWorkflow()).addIOSpeed(ioProcessTime);

        currentMetricBuilder
                .setDownloadDuration(downloadTime)
                .setCacheMiss(cacheMisses > 0)
                .setIoProcessingDuration(ioProcessTime);

        getWorkflow().sendMetric(currentMetricBuilder.createCrossflowMetrics());
        long execTimeMs = System.currentTimeMillis() - start;

        double ioCost = (double) ioProcessTime;

        WorkCost workCost = new WorkCost(netCost, ioCost, 0, 1.0, 1.0);
        ((TechrankWorkflowExt) workflow).reportJobFinish(repository, workCost, repository.path, repository.size);
        ((TechrankWorkflowExt) getWorkflow()).addLocalWorkTime(execTimeMs);

        getWorkflow().addWorkTime(execTimeMs);
        return null;
    }

    @Override
    public RepositorySearchResult consumeRepositories(Repository repository) throws Exception {
        RepositorySearchResult repositorySearchResult = fakeSearch(repository);
//        RepositorySearchResult repositorySearchResult = actuallySearch(repository);
        workflow.getJobsWaiting().remove(repository.getJobId());
        return repositorySearchResult;
    }

    private void fakeDownload(Repository repository) throws Exception {
//        long speed = ((TechrankWorkflowExt) workflow).getNetSpeed();
//        long sleepTime = repository.size / speed * 1000;

        WorkCost workCost = getWorkflow().getWorkCost(repository.getJobId());
//        long speed = (long) getFactoredNoise(workCost.getNetworkCost());
        long speed = (long) workCost.getNetworkCost();

        long sleepTime = speed;

        System.out.println("Worker " + getWorkflow().getName() + " downloading " + repository.getPath() + " for " + sleepTime + " ms.");

        /*
		TODO:
		    - Cache misses
    		- Data load (sum of downloaded repositories)
		* */

        currentMetricBuilder
                .setDownloadDuration(sleepTime)
                .setBytesLoaded(repository.size)
                .setCacheMiss(true);

        Thread.sleep(sleepTime);
        markAsDownloaded(repository);
    }

    private void markAsDownloaded(Repository repository) {
        ((TechrankWorkflowExt) workflow).downloaded.add(repository.path);
    }

    private boolean alreadyDownloaded(Repository repository) {
        return ((TechrankWorkflowExt) workflow).downloaded.contains(repository.path);
    }

    private void fakeIOProcess(Repository repository) throws Exception {
        WorkCost workCost = getWorkflow().getWorkCost(repository.getJobId());
//        long speed = (long) getFactoredNoise(workCost.getIoCost());
        long speed = (long) workCost.getIoCost();

//        long speed = ((TechrankWorkflowExt) workflow).getIOSpeed();
//        long sleepTime = repository.size / speed * 1000;
        long sleepTime = speed;

        System.out.println("Worker " + getWorkflow().getName() + " io processing " + repository.getPath() + " for " + sleepTime + " ms.");

        Thread.sleep(sleepTime);

        currentMetricBuilder.setIoProcessingDuration(sleepTime);
    }

    private int countAllFiles(File clone) {
        if (clone.isDirectory()) {
            return (int) Arrays.stream(Objects.requireNonNull(clone.listFiles()))
                    .filter(f -> !f.isDirectory()).count()
                    + Arrays.stream(Objects.requireNonNull(clone.listFiles()))
                    .filter(f -> f.isDirectory() && !f.getName().equals(".git"))
                    .mapToInt(this::countAllFiles).sum();
        } else return 0;
    }

    @Override
    public boolean hasCommited(Repository input) {

//		try {
//			context = new TechrankWorkflowContext(workflow);
//			File clone = new File(context.getProperties().getProperty("clones") + "/" + workflow.getName() + "/" + UUID.nameUUIDFromBytes(input.getPath().getBytes()));
//			if (clone.exists()) return true;
//
//		} catch (Exception e) {
//			throw new RuntimeException(e);
//		}
//		return commitments.contains(input.getJobHash());

        return true;
    }

    protected int countFiles(File directory, Technology technology) {
        if (directory.isDirectory()) {
            return Arrays.stream(directory.listFiles()).filter(f ->
                    !f.isDirectory() && conforms(f, technology)).collect(Collectors.toList()).size() +
                    Arrays.stream(directory.listFiles()).filter(f -> f.isDirectory() && !f.getName().equals(".git")).
                            mapToInt(f -> countFiles(f, technology)).sum();
        } else return 0;
    }

//    protected int countFiles(File directory, Technology technology) {
//        if (directory.isDirectory()) {
//            return Arrays.asList(directory.listFiles()).stream().filter(f ->
//                    !f.isDirectory() && conforms(f, technology)).collect(Collectors.toList()).size() +
//                    Arrays.asList(directory.listFiles()).stream().filter(f -> f.isDirectory() && !f.getName().equals(".git")).
//                            mapToInt(f -> countFiles(f, technology)).sum();
//        } else return 0;
//    }

    protected boolean conforms(File file, Technology technology) {
        try {
            return file.getName().endsWith(technology.getExtension())
                    && Files.readString(Paths.get(file.toURI())).contains(technology.getKeyword());
        } catch (IOException e) {
            workflow.reportInternalException(e);
            return false;
        }
    }

    public boolean checkForLibOrTimeout(File rootFolder, String library) {
        Future<Boolean> task = executorService.submit(() -> checkForLibrary(rootFolder, library));

        try {
            return task.get(3, TimeUnit.MINUTES);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean checkForLibrary(File rootFolder, String library) {
        System.out.println("Checking for library " + library + " in " + rootFolder.getAbsolutePath());
        try (var stream = Files.walk(Paths.get(rootFolder.getAbsolutePath()))) {
            var found = stream
                    .map(String::valueOf)
                    .filter(path -> path.endsWith("package.json"))
                    .anyMatch(path -> libraryExistsInPackageJson(library, Paths.get(path)));
            System.out.println("Found " + library + " in " + rootFolder.getAbsolutePath() + ": " + found);
            return found;
        } catch (IOException e) {
            return false;
        }
    }

    private boolean libraryExistsInPackageJson(String library, Path packageJson) {
        try {
            String jsonText1 = Files.readString(packageJson);
            JSONObject root = new JSONObject(jsonText1);

            if (!root.has("dependencies")) return false;

            JSONObject dependencies = root.getJSONObject("dependencies");
            for (String key : dependencies.keySet()) {
                if (key.equals(library)) return true;
            }

            if (!root.has("devDependencies")) return false;

            JSONObject devDependencies = root.getJSONObject("devDependencies");
            for (String key : devDependencies.keySet()) {
                if (key.equals(library)) return true;
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }
}