package org.crossflow.tests.techrankHistoric;

import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class RepositorySource extends RepositorySourceBase {
    public static final String REPOSITORIES_FILE_PATH = "repositories_shuffled.txt";
    private GitHub github;

    private final List<String> forbiddenRepositories = List.of(
            "SuperMap/iClient-JavaScript".toLowerCase(),
            "dataarts/3-dreams-of-black".toLowerCase(),
            "bytedeco/javacpp-presets".toLowerCase(),
            "apache/hive".toLowerCase(),
            "deeplearning4j/deeplearning4j".toLowerCase()
    );

    @Override
    public void produce() throws Exception {
        while (!workflow.workflowReady()) {
            System.out.println("Not enough workers connected, waiting...");
            Thread.sleep(1_000);
        }
        long taskStart = System.currentTimeMillis();
        fakeProduce();
        getWorkflow().addWorkTime(System.currentTimeMillis() - taskStart);
    }

    private void actuallyProduce() throws Exception {
//        connectToGitHub();

//        PagedSearchIterable<GHRepository> repositories = github.searchRepositories()
//                .language("java")
//                .size(":>500000")
//                .stars(":>1700")
//                .list();
//
//        for (GHRepository repository : repositories) {
//            if (repository.getFullName().contains("jdk")) {
//                continue;
//            }
//            if (repository.getSize() > 1_200_000 || forbiddenRepositories.contains(repository.getFullName().toLowerCase())) {
//                continue;
//            }
//            sendToRepositories(new Repository(repository.getFullName(), repository.getSize()));
//        }

        loadRepositories()
                .forEach(this::sendToRepositories);
    }

    private List<Repository> loadRepositories() {
        try (var reader = Files.newBufferedReader(Path.of(REPOSITORIES_FILE_PATH))) {

            List<Repository> repositories = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isEmpty()) {
                    String[] strings = line.split(",");
                    repositories.add(new Repository(strings[0], Long.parseLong(strings[1])));
                }
            }
            return repositories;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void fakeProduce() throws Exception {
        HistoryTechrankWorkflowExt wf = (HistoryTechrankWorkflowExt) getWorkflow();

        wf.fakeCommitHashes
                .keySet()
                .forEach(repository -> {
                    Repository repo = new Repository(repository, wf.getFakeRepositorySize(repository));
                    sendToRepositories(repo);
                });

    }

    protected void connectToGitHub() throws Exception {
        github = new GitHubBuilder()
                .withOAuthToken("-", "-")
                .build();
    }
}
