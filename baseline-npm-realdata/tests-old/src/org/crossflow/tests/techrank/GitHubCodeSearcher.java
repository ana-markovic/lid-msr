package org.crossflow.tests.techrank;

import org.kohsuke.github.*;

import java.io.File;
import java.time.Instant;
import java.util.*;

//public class GitHubCodeSearcher extends GitHubCodeSearcherBase {
//
//    protected GitHub github = null;
//
//    List<Technology> technologies = List.of(
//            new Technology("eugenia", "gmf.node", "ecore"),
//            new Technology("eol", "var", "eol")
//    );
//
////    @Override
////    public Repository consumeTechnologies(Technology technology) throws Exception {
////
////        loadRepositoriesForTechnology(technology)
////                .forEach(this::sendToRepositories);
////        return null;
////    }
//
//    @Override
//    public Repository consumeTechnologies(Technology technology) throws Exception {
//        if (github == null) connectToGitHub();
//
//        PagedSearchIterable<GHRepository> it = github.searchRepositories()
//                .stars(">15000")
//                .forks(">15000")
//                .list();
//        PagedIterator<GHRepository> pit = it._iterator(100);
//
//        List<GHRepository> contents;
//
//        do {
//            GHRateLimit rateLimit = github.getRateLimit();
//            long epochSecond = Instant.now().getEpochSecond();
//            long sleepTime = rateLimit.getSearch().getResetEpochSeconds() - epochSecond;
//            long sleepTimeMillis = (sleepTime + 2) * 1000;
//
//            if (!pit.hasNext()) {
//                break;
//            }
//            contents = pit.nextPage();
//            for (GHRepository repository : contents) {
//                Repository repo = new Repository();
//                repo.path = repository.getFullName();
////                repo.path = content.getOwner().getFullName();
////                GHRepository r = github.getRepository(repo.path);
//
//                repo.size = (long) repository.getSize() << 10;
//                repo.setCorrelationId(repo.getPath());
//                repo.setTechnologies(technologies);
//                sendToRepositories(repo);
//            }
//
//            Thread.sleep(sleepTimeMillis);
//        } while (contents.size() != 0);
//
//        return null;
//    }
//
//    protected void connectToGitHub() throws Exception {
//        github = new GitHubBuilder()
//                .withOAuthToken("-", "-")
//                .build();
//    }
//
//    private List<Repository> loadRepositoriesForTechnology(Technology technology) throws Exception {
//        List<Repository> jobs = new ArrayList<>();
////        String fileName = ((TechrankWorkflowExt) getWorkflow()).repoInputFile;
//        String fileName = "repositories.csv";
//        try (Scanner reader = new Scanner(new File(fileName))) {
//            reader.nextLine(); // skip header
//            while (reader.hasNextLine()) {
//                String input = reader.nextLine();
//                String[] split = input.split(",");
//                if (split[2].equals(technology.extension)) {
//                    jobs.add(new Repository(split[0], Long.parseLong(split[1]), technologies));
//                }
//            }
//            return jobs;
//        }
//    }
//}