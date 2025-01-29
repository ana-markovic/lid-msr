package org.crossflow.tests.terasort;

import org.apache.hadoop.fs.BlockLocation;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.crossflow.runtime.Mode;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class TerasortWorkflowWorkerExt extends TerasortWorkflow {

    private String hostname;

    public TerasortWorkflowWorkerExt(Mode m) {
        super(m);
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }


}
