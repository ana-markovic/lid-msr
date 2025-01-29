package markovic.ana;


import markovic.ana.model.RepositorySearch;
import markovic.ana.model.Technology;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.util.Collector;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

public class RepositorySearcher implements FlatMapFunction<RepositorySearch, Tuple2<String, Technology>> {

    private final String parentDirectoryFormat = "~/Desktop/clones/%s/";

    @Override
    public void flatMap(RepositorySearch repositorySearch, Collector<Tuple2<String, Technology>> collector) throws Exception {
        String workerId = Counter.getWorkerId();
        File parentDirectory = new File(String.format(parentDirectoryFormat, workerId));

        if (!parentDirectory.exists()) {
            parentDirectory.mkdir();
        }

        File clone = new File(parentDirectory.getAbsolutePath() + "/" + UUID.nameUUIDFromBytes(repositorySearch.getRepository().getBytes()));


        if (!clone.exists()) {

            try {
                // Try the command-line option first as it supports --depth 1
                Process process = Runtime.getRuntime().exec("git clone --depth 1 " + "https://github.com/" +
                        repositorySearch.getRepository() + ".git " + clone.getAbsolutePath());
                process.waitFor();
            } catch (Exception ex) {
                System.out.println("Falling back to JGit because " + ex.getMessage());
            }
        }

        for (Technology technology : repositorySearch.getTechnologies()) {
            if (technologyFound(clone, technology)) {
                collector.collect(new Tuple2<>(repositorySearch.getRepository(), technology));
            }
        }

        Counter.finishWorker(workerId);
    }

    private boolean technologyFound(File directory, Technology technology) {
        return countFiles(directory, technology) > 0;
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
            return file.getName().endsWith(technology.getExtension()) && new String(Files.readAllBytes(Paths.get(file.toURI()))).contains(technology.getKeyword());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
