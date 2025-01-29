package markovic.ana.techrank.functions;

import markovic.ana.techrank.Counter;
import markovic.ana.techrank.RepositorySearch;
import markovic.ana.techrank.Technology;
import markovic.ana.techrank.WorkerConfiguration;
import org.apache.spark.api.java.function.FlatMapFunction;
import scala.Tuple2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FakeRepositorySearcher implements FlatMapFunction<RepositorySearch, Tuple2<String, Technology>> {

    private WorkerConfiguration worker;

    @Override
    public Iterator<Tuple2<String, Technology>> call(RepositorySearch repository) throws Exception {

        worker = Counter.getAvailableWorker();

        if (!alreadyDownloaded(repository)) {
            fakeDownload(repository);
        }

        fakeIOProcess(repository);
        Counter.finishWorker(worker.getName());
        return findTechnologies(repository).listIterator();
    }

    private List<Tuple2<String, Technology>> findTechnologies(RepositorySearch repository) {
        List<Tuple2<String, Technology>> results = new ArrayList<>();

        for (Technology technology : repository.getTechnologies()) {
            results.add(new Tuple2<>(repository.getRepository(), technology));
        }

        return results;
    }

    private void fakeDownload(RepositorySearch repository) {
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

    private void markAsDownloaded(RepositorySearch repository) {
        RepositoriesWarehouse.add(repository.getRepository(), worker.getName());
    }

    private boolean alreadyDownloaded(RepositorySearch repository) {
        return RepositoriesWarehouse.has(repository.getRepository(), worker.getName());
    }

    private void fakeIOProcess(RepositorySearch repository) {
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
