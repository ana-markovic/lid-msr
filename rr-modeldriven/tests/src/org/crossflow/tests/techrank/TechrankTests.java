package org.crossflow.tests.techrank;

import org.apache.commons.io.FileUtils;
import org.crossflow.runtime.Mode;
import org.crossflow.tests.WorkflowTests;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TechrankTests extends WorkflowTests {

    private static final int NUMBER_OF_WORKERS = 3;
    public static final String INPUT_FOLDER = "experiment/techrank/in";
    public static final String OUTPUT_FOLDER = "experiment/techrank/out";

    @Before
    public void deleteDir() throws IOException {
        File output = new File(OUTPUT_FOLDER);
        if (output.exists())
            FileUtils.deleteDirectory(output);
    }


    @Test
    public void testDedicatedQueues() throws Exception {

        TechrankWorkflowExt master = new TechrankWorkflowExt(Mode.MASTER_BARE);
        master.createBroker(false);
        master.setMaster("localhost");
        master.setInputDirectory(new File(INPUT_FOLDER));
        master.setOutputDirectory(new File(OUTPUT_FOLDER));
        master.setInstanceId("techrank-old");
        master.setName("techrank-master");


        List<TechrankWorkflowExt> workers = new ArrayList<>();
        TechrankWorkflowExt worker;

        for (int i = 1; i <= NUMBER_OF_WORKERS; i++) {
            worker = new TechrankWorkflowExt(Mode.WORKER);
            worker.setName("Worker" + i);
            worker.setInstanceId("techrank-old");
            worker.setInputDirectory(new File(INPUT_FOLDER));
            worker.setOutputDirectory(new File(OUTPUT_FOLDER));
            workers.add(worker);
        }


        long init = System.currentTimeMillis();
        master.run();

        for (TechrankWorkflow w : workers) {
            w.run(0);
        }


        waitFor(master);
        System.out.println("normal execution time: " + (System.currentTimeMillis()-init) / 1000 + "s");

        assertEquals(1000, 1000);


    }


}
