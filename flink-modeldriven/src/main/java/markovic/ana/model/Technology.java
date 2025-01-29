package markovic.ana.model;

import java.io.Serializable;

public class Technology implements Serializable {
    public final String extension;
    public final String name;
    public final String keyword;

    public Technology(String name, String keyword, String extension) {
        this.name = name;
        this.keyword = keyword;
        this.extension = extension;
    }

    public String getExtension() {
        return extension;
    }

    public String getName() {
        return name;
    }

    public String getKeyword() {
        return keyword;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Technology)) return false;

        Technology that = (Technology) o;

        if (getExtension() != null ? !getExtension().equals(that.getExtension()) : that.getExtension() != null)
            return false;
        if (getName() != null ? !getName().equals(that.getName()) : that.getName() != null) return false;
        return getKeyword() != null ? getKeyword().equals(that.getKeyword()) : that.getKeyword() == null;
    }

    @Override
    public int hashCode() {
        int result = getExtension() != null ? getExtension().hashCode() : 0;
        result = 31 * result + (getName() != null ? getName().hashCode() : 0);
        result = 31 * result + (getKeyword() != null ? getKeyword().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Technology{" +
                "extension='" + extension + '\'' +
                ", name='" + name + '\'' +
                ", keyword='" + keyword + '\'' +
                '}';
    }
}
