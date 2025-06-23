package org.crossflow.tests.techrankHistoric;

import net.sourceforge.pmd.PMD;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommitAnalyser extends CommitAnalyserBase {

    public static final String JAVA_FILE_EXTENSION = ".java";
    public static final String JAVASCRIPT_FILE_EXTENSION = ".js";
    public int FAKE_INTERCEPT;
    public double FAKE_SLOPE;

    @Override
    public CommitAnalysisResult consumeAnalysisJobs(CommitAnalysis commitAnalysis) throws Exception {

        HistoryTechrankWorkflowExt wf = (HistoryTechrankWorkflowExt) getWorkflow();
        String storageDirPath = wf.getStorageDirPath(commitAnalysis.repository);

        long netStart = System.currentTimeMillis();

        downloadRepositoryIfNotExists(commitAnalysis.repository, storageDirPath);
        long netDuration = System.currentTimeMillis() - netStart;

        long ioStart = System.currentTimeMillis();

        runFakeIO(commitAnalysis.lineCount);
        int numWarnings = countCheckStyleWarnings();

        long ioDuration = System.currentTimeMillis() - ioStart;

        CommitAnalysisResult result = new CommitAnalysisResult();
        result.setCommitHash(commitAnalysis.commitHash);
        result.setWarningCount(numWarnings);
        result.setRepositoryName(commitAnalysis.repository.path);
        result.setCommitNumber(commitAnalysis.commitNumber);

        wf.reportIOJobFinish(commitAnalysis, ioDuration, netDuration);

        wf.addLocalWorkTime(ioDuration + netDuration);
        return result;
    }

    private void runFakeIO(long lineCount) {
        var processingTimeMs = getProcessingTimeMs(lineCount);
        processingTimeMs = (long) (processingTimeMs * randomByFive());
        try {
            Thread.sleep(processingTimeMs);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private long getProcessingTimeMs(long lineCount) {
        HistoryTechrankWorkflowExt wf = (HistoryTechrankWorkflowExt) getWorkflow();
        switch (wf.getWorkerType()) {
            case "FAST":
                FAKE_INTERCEPT = 40690;
                FAKE_SLOPE = 0.24;
                break;
            case "SLOW":
                FAKE_INTERCEPT = 32481;
                FAKE_SLOPE = 0.85;
                break;
            case "NORMAL":
            default:
                FAKE_INTERCEPT = 44165;
                FAKE_SLOPE = 0.394;
        }
        return (long) (FAKE_SLOPE * lineCount + FAKE_INTERCEPT);
    }

    private double randomByFive() {
        return 0.925 + 0.15 * new Random().nextDouble();
    }

    private int countCheckStyleWarnings() {
        return new Random().nextInt(1000);
    }


    private void checkoutCommit(Repository repository, String commitHash) {
        String storageDirPath = ((HistoryTechrankWorkflowExt) getWorkflow()).getStorageDirPath(repository);

        String command = "git checkout " + commitHash;

        ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
        processBuilder.directory(new File(storageDirPath));
        try {
            Process process = processBuilder.start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void downloadRepositoryIfNotExists(Repository repository, String storageDirPath) {
        if (!Files.exists(Path.of(storageDirPath))) {
            HistoryTechrankWorkflowExt wf = (HistoryTechrankWorkflowExt) getWorkflow();
            wf.addCacheData(1, repository.size);
            downloadRepository(repository);
        }
    }

    private void downloadRepository(Repository repository) {
        String storageDirPath = ((HistoryTechrankWorkflowExt) getWorkflow()).getStorageDirPath(repository);

        try {
            Files.createDirectories(Path.of(storageDirPath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String command = "git clone https://github.com/" + repository.path + ".git " + storageDirPath;

        System.out.println("Downloading repository: " + repository.path);

        ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
        try {
            Process process = processBuilder.start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
//            throw new RuntimeException(e);
        }
    }

    private int countWarnings(String storageDirPath) {
        String pmdReport = storageDirPath + "/pmd-report.xml";
        try (Stream<String> lines = Files.lines(Path.of(pmdReport))) {
            return lines
                    .filter(line -> line.contains("<violation"))
                    .mapToInt(line -> 1)
                    .sum();
        } catch (IOException e) {
            e.printStackTrace();
            return fakeCountWarnings();
        }
    }

    private List<String> getJavaDirectories(String rootPath) {
        Path path = Path.of(rootPath);
        try (var stream = Files.walk(path)) {
            return stream.filter(p -> p.toString().endsWith(JAVA_FILE_EXTENSION))
                    .map(Path::getParent)
                    .distinct()
                    .map(Path::toAbsolutePath)
                    .map(Path::toString)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            return List.of();
        }
    }

    private int fakeCountWarnings() {
        return new Random().nextInt(1000);
    }

    private String createFileList(String repositoryPath) {
        try (var stream = Files.walk(Path.of(repositoryPath))) {
            List<String> collect = stream.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(JAVA_FILE_EXTENSION))
                    .map(Path::toAbsolutePath)
                    .map(Path::toString)
                    .collect(Collectors.toList());

            Path path = Path.of(repositoryPath + "/file-list.txt");
            try (var writer = Files.newBufferedWriter(path)) {
                for (String s : collect) {
                    writer.write(s);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return repositoryPath + "/file-list.txt";
    }

    private void runCheckStyle(String storagePath) {
        String command = "java -jar checkstyle.jar -c google_checks.xml " + storagePath;

        ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
        processBuilder.directory(new File("/home/ubuntu"));
        processBuilder.redirectOutput(new File("/dev/null"));
        processBuilder.redirectError(new File("/dev/null"));
        try {
            Process process = processBuilder.start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void runPMD(String storageDirPath) {

        String fileList = createFileList(storageDirPath);

        String[] pmdArgs = {
//                "-d", repositoryPath,
                "-filelist", fileList,
//                "-R", "rulesets/ecmascript/basic.xml",
//                "-R", "rulesets/java/quickstart.xml",
                "-R", "rulesets/internal/all-java.xml",
                "-f", "xml",
                "-r", "pmd-report.xml"
        };


        System.out.println("Running PMD on " + storageDirPath);


        try {
            PMD.run(pmdArgs);
            System.out.println("PMD finished on " + storageDirPath);
        } catch (Exception | Error e) {
            e.printStackTrace();
        }
    }
}
