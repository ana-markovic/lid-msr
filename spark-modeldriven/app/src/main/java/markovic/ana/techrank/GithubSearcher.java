package markovic.ana.techrank;

import org.kohsuke.github.GHContentSearchBuilder;
import org.kohsuke.github.GHRateLimit;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

import java.io.IOException;
import java.io.Serializable;

public class GithubSearcher implements Serializable {
    private static GitHub github;

    public static final String GITHUB_TOKEN = "";

    public static final String GITHUB_USER = "";

    static {
        try {
            github = new GitHubBuilder()
                    .withOAuthToken(GITHUB_TOKEN,
                            GITHUB_USER).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static GHContentSearchBuilder searchContent() {
        return github.searchContent();
    }

    public static GHRateLimit getRateLimit() throws IOException {
        return github.getRateLimit();
    }
}
