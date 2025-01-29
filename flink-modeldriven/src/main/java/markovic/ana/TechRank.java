/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package markovic.ana;

import markovic.ana.model.RepositorySearch;
import markovic.ana.model.Technology;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public class TechRank {
    private static final JobConfType job_conf = JobConfType.REPETITIVE_SMALL;
    public static final WorkerConfType workerConf = WorkerConfType.SLOW_FAST;
    public static final int NUM_WORKERS = 5;

    public static void main(String[] args) throws Exception {

        long start = System.currentTimeMillis();

        System.out.println("Starting TechRank: " + job_conf.getJobName() + " | " + workerConf.getConfName());

        Counter.initializeWorkers(workerConf.getConfName());

        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(NUM_WORKERS);
        DataStreamSource<Technology> technologies = env.fromCollection(RepositoriesWarehouse.getTechnologies());

        DataStream<RepositorySearch> repositories = technologies
                .flatMap(new FakeGithubCodeSearcher(job_conf.getJobName()));

        DataStream<Tuple2<String, Technology>> searchResults = repositories
                .keyBy(RepositorySearch::getRepository)
                .flatMap(new FakeRepositorySearcher());

        DataStream<Tuple2<String, Integer>> summed = searchResults
                .map(t -> new Tuple2<>(t.f0, 1))
                .returns(Types.TUPLE(Types.STRING, Types.INT))
                .keyBy(t -> t.f0)
                .sum(1);

        summed.map(result -> result.f0 + ": " + result.f1)
                .print();

        env.execute("Flink TechRank");

        System.out.println("Took: " + (System.currentTimeMillis() - start) + " ms");
    }

}
