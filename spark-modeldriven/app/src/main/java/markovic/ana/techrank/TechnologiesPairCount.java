package markovic.ana.techrank;

import java.io.Serializable;

public class TechnologiesPairCount implements Serializable {
    private Technology technology1;
    private Technology technology2;
    private int count;

    public TechnologiesPairCount() {
    }

    public TechnologiesPairCount(Technology technology1, Technology technology2, int count) {
        this.technology1 = technology1;
        this.technology2 = technology2;
        this.count = count;
    }

    public Technology getTechnology1() {
        return technology1;
    }

    public void setTechnology1(Technology technology1) {
        this.technology1 = technology1;
    }

    public Technology getTechnology2() {
        return technology2;
    }

    public void setTechnology2(Technology technology2) {
        this.technology2 = technology2;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    @Override
    public String toString() {
        return "TechnologiesPairCount{" +
                "technology1=" + technology1 +
                ", technology2=" + technology2 +
                ", count=" + count +
                '}';
    }
}
