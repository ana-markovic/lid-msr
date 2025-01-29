package markovic.ana.techrank.functions;

import markovic.ana.techrank.GithubSearcher;
import markovic.ana.techrank.RepositorySearch;
import markovic.ana.techrank.TechRank;
import markovic.ana.techrank.Technology;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHRateLimit;
import org.kohsuke.github.PagedIterator;
import org.kohsuke.github.PagedSearchIterable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GithubCodeSearcher implements FlatMapFunction<Technology, RepositorySearch> {
    @Override
    public Iterator<RepositorySearch> call(Technology technology) throws Exception {
        PagedSearchIterable<GHContent> it = GithubSearcher
                .searchContent()
                .extension(technology.getExtension())
                .q(technology.getKeyword())
                .size("10000..11000")
                .list();
        PagedIterator<GHContent> pit = it._iterator(100);

        List<GHContent> contents;

        List<RepositorySearch> list = new ArrayList<>();

        do {
            GHRateLimit rateLimit = GithubSearcher.getRateLimit();
            long epochSecond = Instant.now().getEpochSecond();
            long sleepTime = rateLimit.getSearch().getResetEpochSeconds() - epochSecond;
            long sleepTimeMillis = (sleepTime + 2) * 1000;

            if (!pit.hasNext()) {
                break;
            }
            contents = pit.nextPage();
            for (GHContent content : contents) {
                System.out.println("Searched: " + content.getOwner().getFullName());
                RepositorySearch repositorySearch = new RepositorySearch(content.getOwner().getFullName());
                repositorySearch.setTechnologies(TechRank.getTechnologies());
                list.add(repositorySearch);
            }

            Thread.sleep(sleepTimeMillis);
        } while (contents.size() != 0);
        return list.listIterator();
    }
}
