package org.crossflow.tests.techrank;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class TechnologySource extends TechnologySourceBase {

    protected List<Technology> technologies = List.of(
            new Technology("eugenia", "gmf.node", "ecore"),
            new Technology("eol", "var", "eol")
    );


    @Override
    public void produce() throws Exception {
        long start = System.currentTimeMillis();

        Thread.sleep(3 * 1000);

        for (Technology technology : technologies) {
            List<Repository> repositoriesForTechnology = loadRepositoriesForTechnology(technology);

            for (Repository repository : repositoriesForTechnology) {
                sendToRepositories(repository);
            }
        }
        
        long execTimeMs = System.currentTimeMillis() - start;
        getWorkflow().addWorkTime(execTimeMs);
    }

    private List<Repository> loadRepositoriesForTechnology(Technology technology) throws Exception {
        List<Repository> jobs = new ArrayList<>();
        TechrankWorkflowExt workflow = ((TechrankWorkflowExt) getWorkflow());
        String fileName = workflow.repoInputFile;
        try (Scanner reader = new Scanner(new File(fileName))) {
            while (reader.hasNextLine()) {
                String input = reader.nextLine();
                String[] split = input.split(",");
                if (split[2].equals(technology.extension)) {
                    jobs.add(new Repository(split[0], Long.parseLong(split[1]), technologies, ""));
                }
            }
            return jobs;
        }
    }
}