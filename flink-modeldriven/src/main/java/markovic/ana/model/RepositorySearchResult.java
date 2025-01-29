package markovic.ana.model;

import java.io.Serializable;

public class RepositorySearchResult implements Serializable {
    private String repository;
    private Technology technology;

    public RepositorySearchResult(String repository, Technology technology) {
        this.repository = repository;
        this.technology = technology;
    }

    public String getRepository() {
        return repository;
    }

    public Technology getTechnology() {
        return technology;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RepositorySearchResult)) return false;

        RepositorySearchResult that = (RepositorySearchResult) o;

        if (getRepository() != null ? !getRepository().equals(that.getRepository()) : that.getRepository() != null)
            return false;
        return getTechnology() != null ? getTechnology().equals(that.getTechnology()) : that.getTechnology() == null;
    }

    @Override
    public int hashCode() {
        int result = getRepository() != null ? getRepository().hashCode() : 0;
        result = 31 * result + (getTechnology() != null ? getTechnology().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "RepositorySearchResult{" +
                "repository='" + repository + '\'' +
                ", technology=" + technology +
                '}';
    }
}
