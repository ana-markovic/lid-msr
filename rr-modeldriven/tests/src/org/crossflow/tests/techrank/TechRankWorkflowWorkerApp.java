package org.crossflow.tests.techrank;

import org.crossflow.runtime.Mode;

import java.io.File;

public class TechRankWorkflowWorkerApp {

    public static final String INPUT_FOLDER = "experiment/techrank/in";
    public static final String OUTPUT_FOLDER = "experiment/techrank/out";

    public static void main(String[] args) throws Exception {
        String workerName = "worker";
        String brokerHost = "localhost";
        String storageDir = "";
        var env = System.getenv();

        if (env.containsKey("WORKER_NAME")) {
            workerName = env.get("WORKER_NAME");
        }

        if (env.containsKey("BROKER_HOST")) {
            brokerHost = env.get("BROKER_HOST");
        }

        if (env.containsKey("STORAGE_DIR")) {
            storageDir = env.get("STORAGE_DIR");
        }

        TechrankWorkflowExt worker = new TechrankWorkflowExt(Mode.WORKER);
        worker.setName(workerName);
        worker.setInstanceId("techrank");
        worker.setMaster(brokerHost);
        worker.setInputDirectory(new File(INPUT_FOLDER));
        worker.setOutputDirectory(new File(OUTPUT_FOLDER));
        worker.setStorageDir(storageDir);
        worker.setupSpeed();
        worker.loadDownloadedRepositories();

        System.out.println("-------------");
        System.out.println("Broker Host: " + worker.getMaster());
        System.out.println("Storage Dir: " + worker.getStorageDir());
        System.out.println("Worker Name: " + worker.getName());
        System.out.println("IO Speed: " + worker.getIOSpeed());
        System.out.println("Net Speed: " + worker.getNetSpeed());
        System.out.println("-------------");

        long init = System.currentTimeMillis();

        worker.run();

        while (!worker.hasTerminated()) {
            Thread.sleep(100);
        }


        System.out.println("completed in " + (System.currentTimeMillis() - init) + " ms.");
    }
}
