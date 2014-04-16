package ca.davidfuchs.jessedit.ess;

import java.util.Arrays;

public class StructGlobalData {
    private long type;
    private long length;
    private short[] data = new short[0];

    public long getType() {
        return type;
    }

    void setType(long type) {
        this.type = type;
    }

    public long getLength() {
        return length;
    }

    void setLength(long length) {
        this.length = length;
    }

    public short[] getData() {
        return data;
    }

    void setData(short[] data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "StructGlobalData{" +
                "type=" + type +
                ", length=" + length +
                ", data=" + Arrays.toString(data) +
                '}';
    }
}