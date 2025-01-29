package markovic.ana;

import com.google.gson.Gson;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Counter {
    public static Map<String, Boolean> workerMap = new HashMap<>();

    private static Map<String, WorkerConfiguration> workers = new HashMap<>();
    private final static String RESOURCES_DIR = "worker_conf/";
    private static Map<Integer, WorkerConfiguration> workersByIndex = new HashMap<>();
    private static Map<Integer, Boolean> workerMapByIndex = new HashMap<>();

    public static void initializeMap(int numWorkers) {
        for (int i = 1; i <= numWorkers; i++) {
            workerMap.put("worker" + i, false);
        }
    }

    public static Map<String, WorkerConfiguration> getWorkers() {
        return workers;
    }

    public static void initializeWorkers(String workerConfiguration) {
        Gson gson = new Gson();
        try {
            WorkerConfiguration[] workerConfigurations = gson.fromJson(new FileReader(RESOURCES_DIR + workerConfiguration + ".json"), WorkerConfiguration[].class);
            for (int i = 0; i < workerConfigurations.length; i++) {
                WorkerConfiguration worker = workerConfigurations[i];
                workers.put(worker.getName(), worker);
                workerMap.put(worker.getName(), false);
                workerMapByIndex.put(i, false);
                workersByIndex.put(i, worker);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static synchronized String getWorkerId() {
        for (Map.Entry<String, Boolean> entry : workerMap.entrySet()) {
            if (!entry.getValue()) {
                entry.setValue(true);
                return entry.getKey();
            }
        }
        throw new IllegalStateException("No free workers");
    }

    public static void finishWorker(int workerIndex) {
        workerMapByIndex.put(workerIndex, false);
    }

    public static void finishWorker(String workerId) {
        workerMap.put(workerId, false);
    }

    public synchronized static WorkerConfiguration getAvailableWorker() {
        Optional<WorkerConfiguration> found = Optional.empty();
        for (WorkerConfiguration worker : workers.values()) {
            if (!workerMap.get(worker.getName())) {
                found = Optional.of(worker);
                break;
            }
        }
        WorkerConfiguration available = found
                .orElseThrow(() -> new IllegalStateException("No available workers"));
        workerMap.put(available.getName(), true);
        return available;
    }

    public synchronized static WorkerConfiguration getWorkerByIndex(int workerIndex) {
        WorkerConfiguration available = workersByIndex.get(workerIndex);
        workerMap.put(available.getName(), true);
        return available;
    }


}