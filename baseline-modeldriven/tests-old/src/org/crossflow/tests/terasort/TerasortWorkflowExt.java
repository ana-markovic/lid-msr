package org.crossflow.tests.terasort;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.crossflow.runtime.Mode;

import java.util.Timer;

public class TerasortWorkflowExt extends TerasortWorkflow {

    private static final String HDFSResultFolder = "hdfs://localhost:9000/tera_result";
    private FileSystem fileSystem = HadoopConfiguration.getFileSystem();

    protected Timer timer;

    public TerasortWorkflowExt(Mode m) {
        super(m);
    }


    @Override
    public void run(long delay) throws Exception {
        super.run(delay);
        fileSystem.delete(new Path(HDFSResultFolder), true);
    }

}
