package terasort;

import org.apache.spark.api.java.function.FlatMapFunction;

import java.util.Iterator;

public class SplitIntoFiles implements FlatMapFunction<String, String> {
    @Override
    public Iterator<String> call(String s) throws Exception {
        return null;
    }
}
