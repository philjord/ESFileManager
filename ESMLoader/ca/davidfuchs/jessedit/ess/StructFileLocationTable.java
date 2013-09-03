package ca.davidfuchs.jessedit.ess;

import java.util.Arrays;

public class StructFileLocationTable {
    private long globalDataTable1Count;
    private long globalDataTable2Count;
    private long globalDataTable3Count;
    private long globalDataTable1Offset;
    private long globalDataTable2Offset;
    private long globalDataTable3Offset;

    private long changeFormsCount;
    private long changeFormsOffset;

    private long formIdArrayCountOffset;

    private long unknownTable3Offset;

    private long[] unused = new long[15];

    public long getGlobalDataTable1Count() {
        return globalDataTable1Count;
    }

    void setGlobalDataTable1Count(long globalDataTable1Count) {
        this.globalDataTable1Count = globalDataTable1Count;
    }

    public long getGlobalDataTable2Count() {
        return globalDataTable2Count;
    }

    void setGlobalDataTable2Count(long globalDataTable2Count) {
        this.globalDataTable2Count = globalDataTable2Count;
    }

    public long getGlobalDataTable3Count() {
        return globalDataTable3Count;
    }

    void setGlobalDataTable3Count(long globalDataTable3Count) {
        this.globalDataTable3Count = globalDataTable3Count;
    }

    public long getGlobalDataTable1Offset() {
        return globalDataTable1Offset;
    }

    void setGlobalDataTable1Offset(long globalDataTable1Offset) {
        this.globalDataTable1Offset = globalDataTable1Offset;
    }

    public long getGlobalDataTable2Offset() {
        return globalDataTable2Offset;
    }

    void setGlobalDataTable2Offset(long globalDataTable2Offset) {
        this.globalDataTable2Offset = globalDataTable2Offset;
    }

    public long getGlobalDataTable3Offset() {
        return globalDataTable3Offset;
    }

    void setGlobalDataTable3Offset(long globalDataTable3Offset) {
        this.globalDataTable3Offset = globalDataTable3Offset;
    }

    public long getChangeFormsCount() {
        return changeFormsCount;
    }

    void setChangeFormsCount(long changeFormsCount) {
        this.changeFormsCount = changeFormsCount;
    }

    public long getChangeFormsOffset() {
        return changeFormsOffset;
    }

    void setChangeFormsOffset(long changeFormsOffset) {
        this.changeFormsOffset = changeFormsOffset;
    }

    public long getFormIdArrayCountOffset() {
        return formIdArrayCountOffset;
    }

    void setFormIdArrayCountOffset(long formIdArrayCountOffset) {
        this.formIdArrayCountOffset = formIdArrayCountOffset;
    }

    public long getUnknownTable3Offset() {
        return unknownTable3Offset;
    }

    void setUnknownTable3Offset(long unknownTable3Offset) {
        this.unknownTable3Offset = unknownTable3Offset;
    }

    public long[] getUnused() {
        return unused;
    }

    void setUnused(long[] unused) {
        this.unused = unused;
    }

    @Override
    public String toString() {
        return "StructFileLocationTable{" +
                "globalDataTable1Count=" + globalDataTable1Count +
                ", globalDataTable2Count=" + globalDataTable2Count +
                ", globalDataTable3Count=" + globalDataTable3Count +
                ", globalDataTable1Offset=" + globalDataTable1Offset +
                ", globalDataTable2Offset=" + globalDataTable2Offset +
                ", globalDataTable3Offset=" + globalDataTable3Offset +
                ", changeFormsCount=" + changeFormsCount +
                ", changeFormsOffset=" + changeFormsOffset +
                ", formIdArrayCountOffset=" + formIdArrayCountOffset +
                ", unknownTable3Offset=" + unknownTable3Offset +
                ", unused=" + Arrays.toString(unused) +
                '}';
    }
}