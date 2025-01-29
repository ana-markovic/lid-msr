package org.crossflow.tests.techrankHistoric;


import org.crossflow.runtime.WorkCost;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class RepositorySearcher extends RepositorySearcherBase {

    private static final String GET_COMMIT_HASH_COMMAND = "git rev-parse HEAD";
    private static final int NUM_COMMITS_TO_INVESTIGATE = 4;

    private static final long DOWNLOAD_SPEED_MBS = 12;

    @Override
    public RepositorySearchResult consumeRepositories(Repository repository) throws Exception {
        HistoryTechrankWorkflowExt wf = (HistoryTechrankWorkflowExt) getWorkflow();
        long taskStart = System.currentTimeMillis();

        long netStart = System.currentTimeMillis();
        fakeDownloadRepository(repository);

        long netDuration = System.currentTimeMillis() - netStart;

        List<String> commitHashes = new ArrayList<>();
        List<Integer> fileCounts = new ArrayList<>();
        List<Integer> lineCounts = new ArrayList<>();
        List<Integer> repoSizes = new ArrayList<>();

        long ioStart = System.currentTimeMillis();

        for (int i = 0; i < NUM_COMMITS_TO_INVESTIGATE; i++) {
//            actualLoadCommit(repository, i, commitHashes, fileCounts, lineCounts, repoSizes);
            fakeLoadCommit(repository, i, commitHashes, fileCounts, lineCounts, repoSizes);
        }

        long ioDuration = System.currentTimeMillis() - ioStart;

        RepositorySearchResult result = new RepositorySearchResult();
        result.setRepository(repository);
        result.setCommitHash(commitHashes);
        result.setFileCount(fileCounts);
        result.setLineCount(lineCounts);
        result.setRepoSizeKB(repoSizes);

        WorkCost cost = new WorkCost(netDuration, ioDuration, 0, 0, 0);

        ((HistoryTechrankWorkflowExt) getWorkflow()).reportJobFinish(repository, cost, repository.path, repository.size, 0, 0, "");

        getWorkflow().addWorkTime(System.currentTimeMillis() - taskStart);
        wf.addLocalWorkTime(ioDuration + netDuration);
//         TODO: Disable this
//        if (tooManyLines(result)) {
//            return null;
//        }

        return result;
    }

    private void fakeLoadCommit(Repository repository, int i, List<String> commitHashes, List<Integer> fileCounts, List<Integer> lineCounts, List<Integer> repoSizes) {
        String fakeCommitHash = getFakeCommitHash(repository, i);
        commitHashes.add(fakeCommitHash);
        fileCounts.add(0);
        lineCounts.add(getFakeLineCount(repository, fakeCommitHash));
        repoSizes.add((int) repository.size);
    }

    private int getFakeLineCount(Repository repository, String fakeCommitHash) {
        HistoryTechrankWorkflowExt wf = (HistoryTechrankWorkflowExt) getWorkflow();
        return wf.getFakeLineCount(repository, fakeCommitHash);
    }

    private String getFakeCommitHash(Repository repository, int i) {
        HistoryTechrankWorkflowExt wf = (HistoryTechrankWorkflowExt) getWorkflow();
        return wf.getFakeCommitHash(repository, i);
    }

    private boolean tooManyLines(RepositorySearchResult result) {
        boolean tooMany = result.lineCount
                .stream()
                .anyMatch(lineCount -> lineCount > 1_000_000);
        if (tooMany) {
            System.out.println("Too many lines in " + result.repository.path);
        }
        return tooMany;
    }

    private void actualLoadCommit(Repository repository, int i, List<String> commitHashes, List<Integer> fileCounts, List<Integer> lineCounts, List<Integer> repoSizes) {
        rewindToNthCommit(repository, i);
        commitHashes.add(getCommitHash(repository));
        fileCounts.add(getFileCount(repository));
        lineCounts.add(getLineCount(repository));
//        repoSizes.add(getSizeKB(repository));
        repoSizes.add((int) repository.size);
    }

    private void fakeDownloadRepository(Repository repository) {
        String storageDirPath = ((HistoryTechrankWorkflowExt) getWorkflow()).getStorageDirPath(repository);
        long repoSizeKB = repository.size;

        long downloadTime = repoSizeKB / DOWNLOAD_SPEED_MBS; // mb/ms

        Path path = Path.of(storageDirPath);

        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
                Thread.sleep(downloadTime);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void actualDownloadRepository(Repository repository) {
        String storageDirPath = ((HistoryTechrankWorkflowExt) getWorkflow()).getStorageDirPath(repository);

        Path path = Path.of(storageDirPath);

        if (Files.exists(path)) {
            System.out.println("Skipping " + storageDirPath);
        } else {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            String command = "git clone https://github.com/" + repository.path + ".git " + storageDirPath;
            ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
            try {
                Process process = processBuilder.start();
                process.waitFor();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private int getLineCount(Repository repository) {
        String storageDirPath = ((HistoryTechrankWorkflowExt) getWorkflow()).getStorageDirPath(repository);

        try (var stream = Files.walk(Path.of(storageDirPath))) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(isValidJavaPath())
                    .mapToInt(path -> {
                        try {
                            return Files.readAllLines(path).size();
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .sum();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Predicate<Path> isValidJavascriptPath() {
        return path -> {
            String pathString = path.toString();
            return pathString.endsWith(".js")
                    && !pathString.contains("node_modules")
                    && !pathString.contains("test")
                    && !pathString.contains("tests")
                    && !pathString.contains("spec");
        };
    }

    private Predicate<Path> isValidJavaPath() {
        return path -> path.toString().endsWith(".java");
    }

    private int getFileCount(Repository repository) {
        String storageDirPath = ((HistoryTechrankWorkflowExt) getWorkflow()).getStorageDirPath(repository);

        try (var stream = Files.walk(Path.of(storageDirPath))) {
            return (int) stream
                    .filter(Files::isRegularFile)
                    .filter(isValidJavaPath())
                    .count();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getCommitHash(Repository repository) {
        String storageDirPath = ((HistoryTechrankWorkflowExt) getWorkflow()).getStorageDirPath(repository);

        ProcessBuilder processBuilder = new ProcessBuilder(GET_COMMIT_HASH_COMMAND.split(" "));
        processBuilder.directory(new File(storageDirPath));
        try {
            Process process = processBuilder.start();
            process.waitFor();
            return new String(process.getInputStream().readAllBytes()).trim();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void rewindToNthCommit(Repository repository, int n) {
        int commitNumber = NUM_COMMITS_TO_INVESTIGATE - n - 1;
        String command = "git checkout HEAD~" + commitNumber;

        HistoryTechrankWorkflowExt wf = (HistoryTechrankWorkflowExt) getWorkflow();
        String storageDirPath = wf.getStorageDirPath(repository);

        ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
        processBuilder.directory(new File(storageDirPath));
        try {
            Process process = processBuilder.start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private int getSizeKB(Repository repository) {
        String storageDirPath = ((HistoryTechrankWorkflowExt) getWorkflow()).getStorageDirPath(repository);

        try (var stream = Files.walk(Path.of(storageDirPath))) {
            long totalSize = stream
                    .mapToLong(path -> {
                        try {
                            return Files.size(path);
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .sum();

            return (int) (totalSize / 1000);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
