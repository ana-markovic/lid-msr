package markovic.ana.techrank;

import markovic.ana.techrank.functions.*;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.jetbrains.annotations.NotNull;
import scala.Tuple2;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TechRank {

    private static final String large80Percent = "80_percent_large_10_10_100";
    private static final String small80percent = "80_percent_small_100_10_10";
    private static final String allDiff404040 = "all_diff_40_40_40";
    private static final String allDiff10_10_100 = "all_diff_10_10_100";
    private static final String allDiff_100_10_10 = "all_diff_100_10_10";

    public static final String RESOURCES_PATH = "resources/";

    public static void main(String[] args) throws Exception {
        long startTime = System.currentTimeMillis();
        String jobConf = allDiff10_10_100;
        String workerConf = "conf_one_slow";

        Counter.initializeWorkers(workerConf);
        RepositoriesWarehouse.loadWorkerRepositories();

//        String jobConf = "all_diff_100_10_10";

        Counter.initializeWorkers(workerConf);
        System.out.println(Counter.getWorkers());

        Logger.getLogger("org").setLevel(Level.SEVERE);
        SparkConf conf = new SparkConf()
                .setAppName("techrank")
                .setMaster("local[5]");

        JavaSparkContext sc = new JavaSparkContext(conf);

        List<RepositorySearch> collect = getRepositorySearches(jobConf);

        JavaRDD<RepositorySearch> parallelize = sc.parallelize(collect, 5);

        List<Tuple2<String, Integer>> technologiesCount = parallelize
                .flatMap(new FakeRepositorySearcher())
                .mapToPair(t -> new Tuple2<>(t._1, t._2))
                .groupByKey()
                .flatMap(new TechnologiesReducer())
                .mapToPair(t -> new Tuple2<>(t._1, t._2))
                .reduceByKey(Integer::sum)
                .collect();

        for (Tuple2<String, Integer> item : technologiesCount) {
            System.out.println(item._1 + " : " + item._2);
        }


        sc.close();

        System.out.println("Total time: " + (System.currentTimeMillis() - startTime));
        RepositoriesWarehouse.saveWorkerRepositories();
        RepositoriesWarehouse.printDataLoad();
    }

    private static List<RepositorySearch> getRepositorySearches(String jobConf) {
        List<Technology> technologiesList = getTechnologies();
        List<RepositorySearch> collect = technologiesList.stream()
                .map(Technology::getExtension)
                .map(extension -> {
                    try {
                        return RepositoriesWarehouse.loadRepositoriesForTechnology(jobConf, extension);

                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .flatMap(List::stream)
                .collect(Collectors.toList());
        return collect;
    }

    public static List<Technology> getTechnologies() {
        return Arrays.asList(
                new Technology("eugenia", "gmf.node", "ecore"),
                new Technology("eol", "var", "eol")
        );
    }

    public static synchronized void addExecTime(long time) {
    }
}
