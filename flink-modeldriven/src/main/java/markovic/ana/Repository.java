package markovic.ana;

public class Repository {
    public String path;
    public long size;

    public Repository() {
    }

    public Repository(String path, long size) {
        this.path = path;
        this.size = size;
    }
}
