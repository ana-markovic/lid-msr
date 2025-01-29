package org.crossflow.tests.techrankHistoric;


import java.util.ArrayList;
import java.util.List;

public class RepositoryAnalysisDispatcher extends RepositoryAnalysisDispatcherBase {

    @Override
    public CommitAnalysis consumeRepositorySearchResults(RepositorySearchResult repositorySearchResult) throws Exception {
        long taskStart = System.currentTimeMillis();

        List<String> commitHashes = new ArrayList<>(repositorySearchResult.getCommitHash());
        List<Integer> fileCounts = new ArrayList<>(repositorySearchResult.getFileCount());
        List<Integer> lineCounts = new ArrayList<>(repositorySearchResult.getLineCount());
        List<Integer> repoSizes = new ArrayList<>(repositorySearchResult.getRepoSizeKB());

        for (int i = 0; i < commitHashes.size(); i++) {
            CommitAnalysis commitAnalysis = new CommitAnalysis();
            commitAnalysis.setRepository(repositorySearchResult.getRepository());
            commitAnalysis.setCommitHash(commitHashes.get(i));
            commitAnalysis.setFileCount(fileCounts.get(i));
            commitAnalysis.setLineCount(lineCounts.get(i));
            commitAnalysis.setRepoSizeKB(repoSizes.get(i));
            commitAnalysis.setCommitNumber(i);
            sendToAnalysisJobs(commitAnalysis);
        }

        getWorkflow().addWorkTime(System.currentTimeMillis() - taskStart);

        return null;
    }


}
