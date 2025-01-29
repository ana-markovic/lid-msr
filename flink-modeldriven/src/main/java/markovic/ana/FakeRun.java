package markovic.ana;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.util.Arrays;

public class FakeRun {
    public static void main(String[] args) throws Exception {
        Counter.initializeWorkers("conf_slow_fast");
        Counter.workerMap.forEach((s, aBoolean) -> System.out.println(s + " " + aBoolean));


        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(5);
        env.fromCollection(Arrays.asList(5, 12, 21, 32, 45, 50, 60, 70, 80, 90, 100))
                .map((MapFunction<Integer, Integer>) integer -> integer * 21)
                .flatMap((FlatMapFunction<Integer, String>) (integer, collector) -> {

                    if (integer > 100) {
                        collector.collect("High Value: " + integer);
                    }
                    collector.collect("Normal Value: " + integer);
                })
                .print();


        env.execute("techrank-fake");

    }
}
