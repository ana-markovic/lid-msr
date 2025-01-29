package markovic.ana;


import markovic.ana.model.RepositorySearch;
import markovic.ana.model.Technology;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class RepositoriesWarehouse {

    private static final String REPOSITORIES_FILE = "repositories.txt";

    static ConcurrentMap<String, Set<String>> repositories = new ConcurrentHashMap<>();
    static ConcurrentMap<String, BigInteger> dataLoadPerWorker = new ConcurrentHashMap<>();

    public static List<RepositorySearch> loadRepositoriesForTechnology(String jobConf, String technologyExtension) throws Exception {
        List<RepositorySearch> jobs = new ArrayList<>();
        try (Scanner reader = new Scanner(new File("job_conf/" + jobConf + ".csv"))) {
            reader.nextLine(); // skip header
            while (reader.hasNextLine()) {
                String input = reader.nextLine();
                String[] split = input.split(",");
                if (split[2].equals(technologyExtension)) {
                    jobs.add(new RepositorySearch(split[0], Long.parseLong(split[1]), getTechnologies()));
                }
            }
            return jobs;
        }
    }

    public static boolean has(String repository, String workerName) {
        if (!repositories.containsKey(workerName)) {
            return false;
        }
        return repositories.get(workerName).contains(repository);
    }

    public static void add(String repository, String workerName) {
        repositories.putIfAbsent(workerName, new HashSet<>());
        repositories.get(workerName).add(repository);
    }

    public static void loadWorkerRepositories() {
        try {
            Files.readAllLines(Paths.get(REPOSITORIES_FILE)).forEach(line -> {
                String[] split = line.split(",");
                repositories.putIfAbsent(split[0], new HashSet<>());
                repositories.get(split[0]).add(split[1]);
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void addDataLoad(String workerId, long dataLoad) {
        dataLoadPerWorker.putIfAbsent(workerId, BigInteger.ZERO);
        dataLoadPerWorker.put(workerId, dataLoadPerWorker.get(workerId).add(BigInteger.valueOf(dataLoad)));
    }

    public static void printDataLoad() {
        System.out.println("Data Load:");
        dataLoadPerWorker
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(x -> System.out.println(x.getKey() + " " + x.getValue()));
        BigInteger totalDataLoad = dataLoadPerWorker.values().stream().reduce(BigInteger.ZERO, BigInteger::add);
        System.out.println("Total data load: " + totalDataLoad);
    }

    public static List<Technology> getTechnologies() {
        return Arrays.asList(
                new Technology("eugenia", "gmf.node", "ecore"),
                new Technology("eol", "var", "eol")
        );
    }


    public static void saveWorkerRepositories() {
        try {
            Files.write(Paths.get(REPOSITORIES_FILE),
                    repositories.entrySet().stream()
                            .flatMap(entry -> entry.getValue().stream().map(value -> entry.getKey() + "," + value))
                            .collect(Collectors.toList()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
