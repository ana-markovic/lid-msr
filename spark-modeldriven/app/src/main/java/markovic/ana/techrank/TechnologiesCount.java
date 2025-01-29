package markovic.ana.techrank;

import java.io.Serializable;

public class TechnologiesCount implements Serializable {
    private Technology technology;
    private int count;

    public TechnologiesCount(Technology technology, int count) {
        this.technology = technology;
        this.count = count;
    }

    public Technology getTechnology() {
        return technology;
    }

    public int getCount() {
        return count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TechnologiesCount)) return false;

        TechnologiesCount that = (TechnologiesCount) o;

        if (getCount() != that.getCount()) return false;
        return getTechnology() != null ? getTechnology().getName().equals(that.getTechnology()) : that.getTechnology() == null;
    }

    @Override
    public int hashCode() {
        int result = getTechnology() != null ? getTechnology().hashCode() : 0;
        result = 31 * result + getCount();
        return result;
    }

    @Override
    public String toString() {
        return "TechnologiesCount{" +
                "technology=" + technology +
                ", count=" + count +
                '}';
    }
}
