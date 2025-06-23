package org.crossflow.tests.techrank;

import org.kohsuke.github.*;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

public class TechnologySource extends TechnologySourceBase {

  private final String githubToken;
  private final String githubUsername;
  protected List<Technology> technologies;
  private GitHub github;

  private List<String> libraries = List.of(
      "express",
      "async",
      "react",
      "lodash",
      "cloudinary",
      "axios",
      "karma",
      "moleculer",
      "grunt",
      "pm2",
      "mocha",
      "moment",
      "babel",
      "socket.io",
      "mongoose",
      "bluebird",
      "redux",
      "jest",
      "webpack",
      "graphql",
      "redux-saga",
      "nodemailer",
      "react-router",
      "react-native",
      "cheerio",
      "dotenv",
      "passport",
      "winston",
      "sharp",
      "puppeteer"
  );

  public TechnologySource() {
    githubToken = System.getenv("GITHUB_TOKEN");
    githubUsername = System.getenv("GITHUB_USERNAME");
  }


  @Override
  public void produce() throws Exception {

    long start = System.currentTimeMillis();

    if (github == null) connectToGitHub();
    runGithubSearch();
    long execTimeMs = System.currentTimeMillis() - start;
    getWorkflow().addWorkTime(execTimeMs);
  }

  private void runGithubSearch() throws IOException, InterruptedException {

    PagedSearchIterable<GHRepository> it = github
        .searchRepositories()
        .q("stars:>5000 forks:>5000")
        .size(">512000")
        .list();

    PagedIterator<GHRepository> pit = it._iterator(100);
    List<GHRepository> contents;

    do {

      GHRateLimit rateLimit = github.getRateLimit();
      long epochSecond = Instant.now().getEpochSecond();
      long sleepTime = rateLimit.getSearch().getResetEpochSeconds() - epochSecond;
      long sleepTimeMillis = (sleepTime + 2) * 1000;

      if (!pit.hasNext()) {
        break;
      }
      contents = pit.nextPage();
      for (GHRepository repository : contents) {

        for (String library : libraries) {
          Repository repo = new Repository();
          repo.path = repository.getFullName();
          repo.size = repository.getSize();
          repo.setCorrelationId(repo.getPath());
          repo.setTechnologies(technologies);
          repo.setLibrary(library);
          sendToRepositories(repo);
        }
      }

      Thread.sleep(sleepTimeMillis);

    } while (!contents.isEmpty());
  }

  protected void connectToGitHub() throws Exception {
    github = new GitHubBuilder()
        .withOAuthToken(githubToken, githubUsername)
        .build();
  }

}