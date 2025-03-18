package org.crossflow.tests.techrank;

import org.kohsuke.github.*;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/*
public class GitHubCodeSearcher extends GitHubCodeSearcherBase {

    public static final long BURST_SLEEP_MILLIS = 2000L;
    public static final int BURST_SIZE = 3;
    protected GitHub github = null;

    List<Technology> technologies = Arrays.asList(
//            new Technology("eugenia", "gmf.node", "ecore"),
//            new Technology("eol", "var", "eol"));
            new Technology("Python", "python", "py"),
            new Technology("JavaScript", "javascript", "js"));

    @Override
    public Repository consumeTechnologies(Technology technology) throws Exception {

//        loadRepositoriesForTechnology(technology)
//                .forEach(this::sendToRepositories);
//        return null;
//    }
//
        long start = System.currentTimeMillis();

        System.out.println(workflow.getName() + " working on technology " + technology.getName());

        if (github == null) connectToGitHub();


        PagedSearchIterable<GHRepository> it = github.searchRepositories()
                .q("stars:>15000 forks:>15000")
                .list();
        PagedIterator<GHRepository> pit = it._iterator(100);

        List<GHRepository> contents;


        do {
//            GHRateLimit rateLimit = github.getRateLimit();
//            long epochSecond = Instant.now().getEpochSecond();
//            long sleepTime = rateLimit.getSearch().getResetEpochSeconds() - epochSecond;
//            long sleepTimeMillis = (sleepTime + 2) * 1000;

            if (!pit.hasNext()) {
                break;
            }
            contents = pit.nextPage();
            for (GHRepository repository : contents) {
                Repository repo = new Repository();
                repo.path = repository.getFullName();
                repo.size = (long) repository.getSize() << 10;
                repo.setCorrelationId(repo.getPath());
                repo.setTechnologies(technologies);
                sendToRepositories(repo);
            }

//            Thread.sleep(sleepTimeMillis);
        } while (contents.size() != 0);


        long execTimeMs = System.currentTimeMillis() - start;
        getWorkflow().addWorkTime(execTimeMs);
        double execTimeSeconds = execTimeMs / 1000.0;
        ((TechrankWorkflowExt) workflow).reportJobFinish(technology, execTimeSeconds);
        return null;
    }

    protected void connectToGitHub() throws Exception {
//        Properties properties = new TechrankWorkflowContext(workflow).getProperties();
        github = new GitHubBuilder()
                .withOAuthToken("-", "-")
                .build();

//        github = GitHubBuilder.fromProperties(properties)
//                .build();
    }

    private List<Repository> loadRepositoriesForTechnology(Technology technology) throws Exception {
        List<Repository> jobs = new ArrayList<>();
//        String fileName = ((TechrankWorkflowExt) getWorkflow()).repoInputFile;
        String fileName = "repositories.csv";
        try (Scanner reader = new Scanner(new File(fileName))) {
            reader.nextLine(); // skip header
            while (reader.hasNextLine()) {
                String input = reader.nextLine();
                String[] split = input.split(",");
                if (split[2].equals(technology.extension)) {
                    jobs.add(new Repository(split[0], Long.parseLong(split[1]), technologies));
                }
            }
            return jobs;
        }
    }

}*/
