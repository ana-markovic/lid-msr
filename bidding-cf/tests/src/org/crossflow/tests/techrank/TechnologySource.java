package org.crossflow.tests.techrank;

import java.util.*;

public class TechnologySource extends TechnologySourceBase {

    protected List<Technology> technologies;

    private List<String> libraries = List.of(
            "express",
            "async",
            "react",
            "lodash",
            "jest",
            "axios");

    @Override
    public void produce() throws Exception {

        technologies = Arrays.asList(
                new Technology("eugenia", "gmf.node", "ecore"),
                new Technology("eol", "var", "eol"));


        while (!workflow.workflowReady()) {
            System.out.println("Not enough workers connected, waiting...");
            Thread.sleep(1_000);
        }

        TechrankWorkflowExt workflowExt = (TechrankWorkflowExt) workflow;
        workflowExt.startTimer();

        long start = System.currentTimeMillis();

        for (String library : libraries) {
            TechrankWorkflowExt workflowExt1 = (TechrankWorkflowExt) getWorkflow();
            workflowExt1.repoSizes.forEach((repoName, repoSize) -> {
                Repository repo = new Repository();
                repo.path = repoName;
                repo.size = repoSize;
                repo.setCorrelationId(repo.getPath());
                repo.setTechnologies(technologies);
                repo.setLibrary(library);
                sendToRepositories(repo);
            });
        }

        long execTimeMs = System.currentTimeMillis() - start;
        getWorkflow().addWorkTime(execTimeMs);
    }

}