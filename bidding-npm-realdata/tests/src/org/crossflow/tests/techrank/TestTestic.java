package org.crossflow.tests.techrank;

import org.kohsuke.github.GHContent;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.PagedSearchIterable;

public class TestTestic {

    public static void main(String[] args) throws Exception {
        GitHub github = new GitHubBuilder()
                .withOAuthToken("-", "-")
                .build();
        PagedSearchIterable<GHContent> it = github.searchContent()
                .q("gmf.node,var")
                .extension("eol,ecore")
                .list();

        System.out.println("it = " + it.getTotalCount());
    }
}
