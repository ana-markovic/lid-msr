package org.crossflow.tests.techrankHistoric;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class RepositorySource extends RepositorySourceBase {

    @Override
    public void produce() throws Exception {

        while (!workflow.workflowReady()) {
            System.out.println("Not enough workers connected, waiting...");
            Thread.sleep(1_000);
        }

        long taskStart = System.currentTimeMillis();
        HistoryTechrankWorkflowExt wf = (HistoryTechrankWorkflowExt) getWorkflow();

        loadRepositories().forEach(this::sendToRepositories);

        getWorkflow().addWorkTime(System.currentTimeMillis() - taskStart);
    }

    private List<Repository> loadRepositories() throws IOException {
        return Files.readAllLines(Path.of("repositories.csv"))
                .stream()
                .map(line -> {
                    String[] parts = line.split(",");
                    return new Repository(parts[0], Integer.parseInt(parts[1]));
                }).collect(Collectors.toList());
    }

}
