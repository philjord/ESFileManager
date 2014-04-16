package ca.davidfuchs.jessedit.ess;

import java.util.Arrays;

public class StructBasicData {
    private String magic;
    private short formVersion;
    private short[] screenShotData;

    private long headerSize;
    private long pluginInfoSize;
    private long formIdArrayCount;
    private long visitedWorldspaceArrayCount;
    private long unknown3TableSize;

    public String getMagic() {
        return magic;
    }

    void setMagic(String magic) {
        this.magic = magic;
    }

    public short getFormVersion() {
        return formVersion;
    }

    void setFormVersion(short formVersion) {
        this.formVersion = formVersion;
    }

    public short[] getScreenShotData() {
        return screenShotData;
    }

    void setScreenShotData(short[] screenShotData) {
        this.screenShotData = screenShotData;
    }

    public long getHeaderSize() {
        return headerSize;
    }

    void setHeaderSize(long headerSize) {
        this.headerSize = headerSize;
    }

    public long getPluginInfoSize() {
        return pluginInfoSize;
    }

    void setPluginInfoSize(long pluginInfoSize) {
        this.pluginInfoSize = pluginInfoSize;
    }

    public long getFormIdArrayCount() {
        return formIdArrayCount;
    }

    void setFormIdArrayCount(long formIdArrayCount) {
        this.formIdArrayCount = formIdArrayCount;
    }

    public long getVisitedWorldspaceArrayCount() {
        return visitedWorldspaceArrayCount;
    }

    void setVisitedWorldspaceArrayCount(long visitedWorldspaceArrayCount) {
        this.visitedWorldspaceArrayCount = visitedWorldspaceArrayCount;
    }

    public long getUnknown3TableSize() {
        return unknown3TableSize;
    }

    void setUnknown3TableSize(long unknown3TableSize) {
        this.unknown3TableSize = unknown3TableSize;
    }

    @Override
    public String toString() {
        return "StructBasicData{" +
                "magic='" + magic + '\'' +
                ", formVersion=" + formVersion +
                ", screenShotData=" + Arrays.toString(screenShotData) +
                ", headerSize=" + headerSize +
                ", pluginInfoSize=" + pluginInfoSize +
                ", formIdArrayCount=" + formIdArrayCount +
                ", visitedWorldspaceArrayCount=" + visitedWorldspaceArrayCount +
                ", unknown3TableSize=" + unknown3TableSize +
                '}';
    }
}
