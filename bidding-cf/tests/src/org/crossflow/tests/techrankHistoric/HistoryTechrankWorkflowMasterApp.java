package org.crossflow.tests.techrankHistoric;

import org.crossflow.runtime.Mode;

import java.io.File;

public class HistoryTechrankWorkflowMasterApp {

    private static final String INPUT_FOLDER = "";
    private static final String OUTPUT_FOLDER = "";


    public static void main(String[] args) throws Exception {
        String workerName = "master";
        String brokerHost = "localhost";
        String storageDir = "";
        int workerCount = 5;

        var env = System.getenv();

        if (env.containsKey("BROKER_HOST")) {
            brokerHost = env.get("BROKER_HOST");
        }

        if (env.containsKey("STORAGE_DIR")) {
            storageDir = env.get("STORAGE_DIR");
        }

        if (env.containsKey("WORKER_COUNT")) {
            workerCount = Integer.parseInt(env.get("WORKER_COUNT"));
        }

        HistoryTechrankWorkflowExt master = new HistoryTechrankWorkflowExt(Mode.MASTER_BARE);
        master.setName(workerName);
        master.createBroker(false);
        master.setMaster(brokerHost);
        master.setInstanceId("techrank");
        master.setInputDirectory(new File(INPUT_FOLDER));
        master.setOutputDirectory(new File(OUTPUT_FOLDER));
        master.setStorageDir(storageDir);
        master.setWorkerCount(workerCount);

        System.out.println("-------------");
        System.out.println("Broker Host: " + master.getMaster());
        System.out.println("Storage Dir: " + master.getStorageDir());
        System.out.println("Worker Count: " + master.getWorkerCount());
        System.out.println("-------------");

        long init = System.currentTimeMillis();
        master.run(5_000);

        master.startTimer();

        master.awaitTermination();

        System.out.println("master completed in " + (System.currentTimeMillis() - init) + " ms.");
        System.exit(0);
    }

}
