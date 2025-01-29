package org.crossflow.tests.techrank;

import org.crossflow.runtime.CrossflowMetricsBuilder;
import org.crossflow.runtime.utils.EstimationMetrics;

import java.time.LocalDateTime;

public class RepositorySearcher extends CommitmentRepositorySearcherBase {

    private CrossflowMetricsBuilder currentMetricBuilder;

    @Override
    public RepositorySearchResult consumeRepositories(Repository repository) throws Exception {
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
        getWorkflow().sendEstimationMetrics(EstimationMetrics.emptyWithJobId(repository.getJobId(), getWorkflow().getName()));
        ((TechrankWorkflowExt) getWorkflow()).addLocalWorkTime(System.currentTimeMillis() - start);
        return result;
    }

    private void fakeDownload(Repository repository) throws Exception {
        long speed = ((TechrankWorkflowExt) workflow).getNetSpeed();
        long sleepTime = repository.size / speed * 1000;

        System.out.println("Worker " + getWorkflow().getName() + " downloading " + repository.getPath() + " for " + sleepTime + " ms.");

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

}