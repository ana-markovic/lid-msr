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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class RepositorySearcher extends CommitmentRepositorySearcherBase {

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



	private TechrankWorkflowContext context;

	private CrossflowMetricsBuilder currentMetricBuilder;

	public RepositorySearchResult fakeSearch(Repository repository) throws Exception {
		LocalDateTime startTime = LocalDateTime.now();

		currentMetricBuilder = new CrossflowMetricsBuilder();
		currentMetricBuilder
				.setWorkerId(getWorkflow().getName())
				.setRepositoryName(repository.path)
				.setRepositorySizeBytes(repository.size)
				.setJobStartTime(startTime);

		RepositorySearchResult result = new RepositorySearchResult();
		result.setRepository(repository.path);

		long start = System.currentTimeMillis();
		if (!alreadyDownloaded(repository)) {
			fakeDownload(repository);
			((TechrankWorkflowExt) getWorkflow()).addCacheData(1, repository.size);
		}

		fakeIOProcess(repository);
		getWorkflow().sendMetric(currentMetricBuilder.createCrossflowMetrics());
		((TechrankWorkflowExt) getWorkflow()).addLocalWorkTime(System.currentTimeMillis() - start);
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
//        RepositorySearchResult repositorySearchResult = fakeSearch(repository);
		RepositorySearchResult repositorySearchResult = actuallySearch(repository);
		return repositorySearchResult;
	}

	private void fakeDownload(Repository repository) throws Exception {
		long speed = ((TechrankWorkflowExt) workflow).getNetSpeed();
		long sleepTime = repository.size / speed * 1000;

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
		long speed = ((TechrankWorkflowExt) workflow).getIOSpeed();
		long sleepTime = repository.size / speed * 1000;

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
			return Arrays.asList(directory.listFiles()).stream().filter(f ->
					!f.isDirectory() && conforms(f, technology)).collect(Collectors.toList()).size() +
					Arrays.asList(directory.listFiles()).stream().filter(f -> f.isDirectory() && !f.getName().equals(".git")).
							mapToInt(f -> countFiles(f, technology)).sum();
		} else return 0;
	}

	protected boolean conforms(File file, Technology technology) {
		try {
			return file.getName().endsWith(technology.getExtension()) && new String(Files.readAllBytes(Paths.get(file.toURI()))).indexOf(technology.getKeyword()) > -1;
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