package markovic.ana.techrank.functions;

import markovic.ana.techrank.*;
import org.apache.spark.api.java.function.FlatMapFunction;
import scala.Tuple2;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class RepositorySearcher implements FlatMapFunction<RepositorySearch, Tuple2<String, Technology>> {

    private final String parentDirectoryFormat = "/Users/ana/Desktop/clones/%s/";

    @Override
    public Iterator<Tuple2<String, Technology>> call(RepositorySearch repositorySearch) throws Exception {

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
            }
            catch (Exception ex) {
                System.out.println("Falling back to JGit because " + ex.getMessage());
            }
        }

        List<Tuple2<String, Technology>> results = new ArrayList<>();

        for (Technology technology : repositorySearch.getTechnologies()) {
            if (technologyFound(clone, technology)) {
                results.add(new Tuple2<>(repositorySearch.getRepository(), technology));
            }
        }

        Counter.finishWorker(workerId);
        return results.listIterator();
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
        }
        else return 0;
    }

    protected boolean conforms(File file, Technology technology) {
        try {
            return file.getName().endsWith(technology.getExtension()) && new String(Files.readAllBytes(Paths.get(file.toURI()))).contains(technology.getKeyword());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
