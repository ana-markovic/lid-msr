package markovic.ana.techrank.functions;


import markovic.ana.techrank.Technology;
import org.apache.spark.api.java.function.FlatMapFunction;
import scala.Tuple2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class TechnologiesReducer implements FlatMapFunction<Tuple2<String, Iterable<Technology>>, Tuple2<String, Integer>> {
    @Override
    public Iterator<Tuple2<String, Integer>> call(Tuple2<String, Iterable<Technology>> repositoryIterableTuple2) throws Exception {
        Iterator<Technology> iterator = repositoryIterableTuple2._2().iterator();
        List<Technology> list = new ArrayList<>();
        iterator.forEachRemaining(list::add);

        List<Technology> distinctList = list.stream().distinct().collect(Collectors.toList());
        List<Tuple2<String, Integer>> techList = getTechnologiesPairsFromList(distinctList);

        return techList.listIterator();
    }

    private List<Tuple2<String, Integer>> getTechnologiesPairsFromList(List<Technology> list) {
        List<Tuple2<String, Integer>> countList = new ArrayList<>();

        if (list.size() <= 1) {
            return countList;
        }

        for (int i = 0; i < list.size(); i++) {
            for (int j = i + 1; j < list.size(); j++) {
                Technology t1 = list.get(i);
                Technology t2 = list.get(j);
                countList.add(new Tuple2<>(convertTechnologyNamesToKey(t1, t2), 1));
            }
        }

        return countList;
    }

    private String convertTechnologyNamesToKey(Technology t1, Technology t2) {
        return t1.getName().compareTo(t2.getName()) < 0 ?
                t1.getName() + "-" + t2.getName() :
                t2.getName() + "-" + t1.getName();
    }

}
