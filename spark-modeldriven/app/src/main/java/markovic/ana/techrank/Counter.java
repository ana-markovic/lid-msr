package markovic.ana.techrank;

import com.google.gson.Gson;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Counter {
    private static Map<String, Boolean> map = new HashMap<>();

    private static Map<String, WorkerConfiguration> workers = new HashMap<>();
    private final static String RESOURCES_DIR = "worker_conf/";

    public static void initializeMap(int numWorkers) {
        for (int i = 1; i <= numWorkers; i++) {
            map.put("worker" + i, false);
        }
    }

    public static Map<String, WorkerConfiguration> getWorkers() {
        return workers;
    }

    public static void initializeWorkers(String workerConfiguration) {
        Gson gson = new Gson();
        try {
            WorkerConfiguration[] workerConfigurations = gson.fromJson(new FileReader(RESOURCES_DIR + workerConfiguration + ".json"), WorkerConfiguration[].class);
            Stream.of(workerConfigurations).forEach(worker -> {
                workers.put(worker.getName(), worker);
                map.put(worker.getName(), false);
            });
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static synchronized String getWorkerId() {
        for (Map.Entry<String, Boolean> entry : map.entrySet()) {
            if (!entry.getValue()) {
                entry.setValue(true);
                return entry.getKey();
            }
        }
        throw new IllegalStateException("No free workers");
    }

    public static void finishWorker(String workerId) {
        map.put(workerId, false);
    }

    public synchronized static WorkerConfiguration getAvailableWorker() {
        List<WorkerConfiguration> availableWorkers = workers.values()
                .stream()
                .filter(worker -> !map.get(worker.getName()))
                .collect(Collectors.toList());

        if (availableWorkers.isEmpty()) {
            throw new RuntimeException("No available workers");
        }

        // Get random worker
        WorkerConfiguration available = availableWorkers.get((int) (Math.random() * availableWorkers.size()));

        map.put(available.getName(), true);
        return available;
    }
}