package terasort;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;

public class TeraSort {

    public static final String DUMP_FILE_PATH = "/Users/ana/Desktop/dump.txt";

    public static void main(String[] args) {
        SparkConf conf = new SparkConf()
                .setAppName("terasort-spark")
                .setMaster("local[*]");
        try (JavaSparkContext sc = new JavaSparkContext(conf)) {

            JavaRDD<String> tbFile = sc.textFile(DUMP_FILE_PATH);
            tbFile.flatMap(new SplitIntoFiles())
                    .map(new FileSorter())
                    .map(new FileMerger())
                    .map(new FileSorter())
                    .collect();


        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
