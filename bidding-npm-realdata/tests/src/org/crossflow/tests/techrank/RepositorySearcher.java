package org.crossflow.tests.techrank;

import org.crossflow.runtime.CrossflowMetricsBuilder;
import org.eclipse.jgit.api.Git;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.*;

public class RepositorySearcher extends CommitmentRepositorySearcherBase {

  private ExecutorService executorService = Executors.newSingleThreadExecutor();


  private CrossflowMetricsBuilder currentMetricBuilder;

  public RepositorySearchResult actuallySearch(Repository repository) throws Exception {
    String parentFolder = workflow.getStorageDir() + "/" + workflow.getName();

    if (!new File(parentFolder).exists()) {
      new File(parentFolder).mkdirs();
    }

    long start = System.currentTimeMillis();
    LocalDateTime startTime = LocalDateTime.now();

    long downloadTime = 0;
    int cacheMisses = 0;

    File clone = new File(parentFolder + "/" + UUID.nameUUIDFromBytes(repository.getPath().getBytes()));

    if (!clone.exists()) {
      cacheMisses++;
      ((TechrankWorkflowExt) getWorkflow()).addCacheData(1, repository.size);

      try {
        // Try the command-line option first as it supports --depth 1
        Process process = Runtime.getRuntime().exec("git clone --depth 1 " + "https://github.com/" +
            repository.getPath() + ".git " + clone.getAbsolutePath());
        process.waitFor();

        downloadTime = System.currentTimeMillis() - start;
      } catch (Exception ex) {
        System.out.println("Falling back to JGit because " + ex.getMessage());
        Git.cloneRepository()
            .setURI("https://github.com/" + repository.getPath() + ".git")
            .setDirectory(clone)
            .call();
      }
    }

    currentMetricBuilder = new CrossflowMetricsBuilder();
    currentMetricBuilder
        .setWorkerId(getWorkflow().getName())
        .setRepositoryName(repository.path)
        .setRepositorySizeBytes(repository.size)
        .setJobStartTime(startTime);


    long ioProcessTime = 0;

    long ioStartTime = System.currentTimeMillis();
    if (checkForLibOrTimeout(clone, repository.library)) {
      ioProcessTime = System.currentTimeMillis() - ioStartTime;

      RepositorySearchResult result = new RepositorySearchResult();
      result.setRepository(repository.getPath());
      RepositorySearchResult repositorySearchResult = new RepositorySearchResult(repository.library, 1, repository.path);
      sendToRepositorySearchResults(repositorySearchResult);
    }

    currentMetricBuilder
        .setDownloadDuration(downloadTime)
        .setCacheMiss(cacheMisses > 0)
        .setIoProcessingDuration(ioProcessTime);

    getWorkflow().sendMetric(currentMetricBuilder.createCrossflowMetrics());
    long execTimeMs = System.currentTimeMillis() - start;


    ((TechrankWorkflowExt) getWorkflow()).addLocalWorkTime(execTimeMs);

    getWorkflow().addWorkTime(execTimeMs);
    return null;
  }

  @Override
  public RepositorySearchResult consumeRepositories(Repository repository) throws Exception {
    RepositorySearchResult repositorySearchResult = actuallySearch(repository);
    return repositorySearchResult;
  }

  public boolean hasCommited(Repository input) {
    return true;
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
      String jsonText = Files.readString(packageJson);
      JSONObject root = new JSONObject(jsonText);

      if (!root.has("dependencies")) return false;

      JSONObject dependencies = root.getJSONObject("dependencies");
      for (String key : dependencies.keySet()) {
        if (key.equals(library)) return true;
      }
      return false;
    } catch (Exception e) {
      return false;
    }
  }

}