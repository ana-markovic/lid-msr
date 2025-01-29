package markovic.ana;


import markovic.ana.model.RepositorySearch;
import markovic.ana.model.Technology;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.util.Collector;

public class FakeGithubCodeSearcher extends RichFlatMapFunction<Technology, RepositorySearch> {

    private String jobConf;

    public FakeGithubCodeSearcher(String jobConf) {
        this.jobConf = jobConf;
    }

    @Override
    public void flatMap(Technology technology, Collector<RepositorySearch> collector) throws Exception {
        RepositoriesWarehouse
                .loadRepositoriesForTechnology(jobConf, technology.getExtension())
                .forEach(collector::collect);
    }
}
