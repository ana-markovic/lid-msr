package markovic.ana.techrank;

import java.io.Serializable;
import java.util.Collection;

public class RepositorySearch implements Serializable {

    private final String repository;
    private Collection<Technology> technologies;

    private long size;

    public RepositorySearch(String repository) {
        this.repository = repository;
    }

    public RepositorySearch(String repository, long size, Collection<Technology> technologies) {
        this.repository = repository;
        this.technologies = technologies;
        this.size = size;
    }



    public String getRepository() {
        return repository;
    }

    public Collection<Technology> getTechnologies() {
        return technologies;
    }

    public void setTechnologies(Collection<Technology> technologies) {
        this.technologies = technologies;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RepositorySearch)) return false;

        RepositorySearch that = (RepositorySearch) o;

        return getRepository() != null ? getRepository().equals(that.getRepository()) : that.getRepository() == null;
    }

    @Override
    public int hashCode() {
        return getRepository() != null ? getRepository().hashCode() : 0;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    @Override
    public String toString() {
        return "RepositorySearch{" +
                "repository='" + repository + '\'' +
                ", technologies=" + technologies +
                ", size=" + size +
                '}';
    }
}
