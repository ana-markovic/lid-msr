package org.crossflow.tests.techrank;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class TechnologySource extends TechnologySourceBase {

    private List<Technology> technologies = List.of(
            new Technology("eugenia", "gmf.node", "ecore"),
            new Technology("eol", "var", "eol")
    );


    @Override
    public void produce() throws Exception {
        long start = System.currentTimeMillis();

		for (Technology technology : technologies) {
			loadRepositoriesForTechnology(technology)
					.forEach(this::sendToRepositories);
		}

        long execTimeMs = System.currentTimeMillis() - start;
        getWorkflow().addWorkTime(execTimeMs);
    }

    private List<Repository> loadRepositoriesForTechnology(Technology technology) throws Exception {
        List<Repository> jobs = new ArrayList<>();
		String fileName = ((TechrankWorkflowExt) getWorkflow()).repoInputFile;
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