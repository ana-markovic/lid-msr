package markovic.ana.techrank.functions;

import markovic.ana.techrank.RepositorySearch;
import markovic.ana.techrank.Technology;
import org.apache.spark.api.java.function.FlatMapFunction;

import java.util.Iterator;

public class FakeGithubCodeSearcher implements FlatMapFunction<Technology, RepositorySearch> {

    private String jobConf;

    public FakeGithubCodeSearcher(String jobConf) {
        this.jobConf = jobConf;
    }

    @Override
    public Iterator<RepositorySearch> call(Technology technology) throws Exception {
        return RepositoriesWarehouse.loadRepositoriesForTechnology(jobConf, technology.getExtension())
                .listIterator();
    }
}
