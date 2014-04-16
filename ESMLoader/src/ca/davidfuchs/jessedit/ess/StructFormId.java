package ca.davidfuchs.jessedit.ess;

public class StructFormId extends StructRefId {
    private short count;

    public short getCount() {
        return count;
    }

    void setCount(short count) {
        this.count = count;
    }

    @Override
    public String toString() {
        return "StructFormId{" +
                "refId=" + super.toString() +
                "count=" + count +
                '}';
    }
}
