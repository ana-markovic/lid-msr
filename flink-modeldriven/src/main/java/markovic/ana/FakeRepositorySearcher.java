package markovic.ana;


import com.google.gson.Gson;
import markovic.ana.model.RepositorySearch;
import markovic.ana.model.Technology;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.util.Collector;

import java.io.FileNotFoundException;
import java.io.FileReader;

public class FakeRepositorySearcher extends RichFlatMapFunction<RepositorySearch, Tuple2<String, Technology>> {

    private static final String RESOURCES_DIR = "worker_conf/";
//    private static final String WORKER_CONFIGURATION = "conf_slow_fast";

    @Override
    public void flatMap(RepositorySearch repository, Collector<Tuple2<String, Technology>> collector) throws Exception {
        if (!alreadyDownloaded(repository)) {
            fakeDownload(repository);
        }

        fakeIOProcess(repository);
//        Counter.finishWorker();
        repository.getTechnologies()
                .forEach(technology -> collector.collect(new Tuple2<>(repository.getRepository(), technology)));
    }

    private WorkerConfiguration worker;


    private void fakeDownload(RepositorySearch repository) {
        worker = getWorker();

        long speed = SpeedUtils.getSpeed(worker.getNetSpeedBps());
        long sleepTime = repository.getSize() / speed * 1000;

        System.out.println("Worker " + worker.getName() + " downloading " + repository.getRepository() + " for " + sleepTime + " ms.");

		/*
		TODO:
		    - Cache misses
    		- Data load (sum of downloaded repositories)
		* */

        try {
            Thread.sleep(sleepTime);
            RepositoriesWarehouse.addDataLoad(worker.getName(), repository.getSize());
            markAsDownloaded(repository);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private WorkerConfiguration getWorker() {
        Gson gson = new Gson();
        try {
            WorkerConfiguration[] workerConfigurations = gson.fromJson(new FileReader(RESOURCES_DIR + TechRank.workerConf.getConfName() + ".json"), WorkerConfiguration[].class);
            int workerIndex = getRuntimeContext().getIndexOfThisSubtask();
            return workerConfigurations[workerIndex];
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void markAsDownloaded(RepositorySearch repository) {
        worker = getWorker();
        RepositoriesWarehouse.add(repository.getRepository(), worker.getName());
    }

    private boolean alreadyDownloaded(RepositorySearch repository) {
        worker = getWorker();
        return RepositoriesWarehouse.has(repository.getRepository(), worker.getName());
    }

    private void fakeIOProcess(RepositorySearch repository) {
        worker = getWorker();
        long speed = SpeedUtils.getSpeed(worker.getIoSpeedBps());
        long sleepTime = repository.getSize() / speed * 1000;

        System.out.println("Worker " + worker.getName() + " io processing " + repository.getRepository() + " for " + sleepTime + " ms.");
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
