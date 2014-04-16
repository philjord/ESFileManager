package ca.davidfuchs.jessedit.ess;

public class StructUnknown3Table {
    private long count;
    private String unknown;

    public long getCount() {
        return count;
    }

    void setCount(long count) {
        this.count = count;
    }

    public String getUnknown() {
        return unknown;
    }

    public void setUnknown(String unknown) {
        this.unknown = unknown;
    }

    @Override
    public String toString() {
        return "StructUnknown3Table{" +
                "count=" + count +
                ", unknown='" + unknown + '\'' +
                '}';
    }
}
