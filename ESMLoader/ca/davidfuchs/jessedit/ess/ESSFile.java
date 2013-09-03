package ca.davidfuchs.jessedit.ess;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ESSFile implements Serializable {
    private StructBasicData basicData = new StructBasicData();
    private StructHeader header = new StructHeader();
    private StructPluginInfo pluginInfo = new StructPluginInfo();
    private StructFileLocationTable fileLocationTable = new StructFileLocationTable();
    private List<StructGlobalData> globalDataTable1 = new ArrayList<>();
    private List<StructGlobalData> globalDataTable2 = new ArrayList<>();
    private List<StructGlobalData> globalDataTable3 = new ArrayList<>();
    private List<StructChangeForms> changeForms = new ArrayList<>();
    private List<StructFormId> formIdArray = new ArrayList<>();
    private List<StructFormId> visitedWorldspaceArray = new ArrayList<>();
    private StructUnknown3Table unknown3Table = new StructUnknown3Table();

    public StructBasicData getBasicData() {
        return basicData;
    }

    public StructHeader getHeader() {
        return header;
    }

    public StructPluginInfo getPluginInfo() {
        return pluginInfo;
    }

    public StructFileLocationTable getFileLocationTable() {
        return fileLocationTable;
    }

    public List<StructGlobalData> getGlobalDataTable1() {
        return globalDataTable1;
    }

    public List<StructGlobalData> getGlobalDataTable2() {
        return globalDataTable2;
    }

    public List<StructGlobalData> getGlobalDataTable3() {
        return globalDataTable3;
    }

    public List<StructChangeForms> getChangeForms() {
        return changeForms;
    }

    public List<StructFormId> getFormIdArray() {
        return formIdArray;
    }

    public List<StructFormId> getVisitedWorldspaceArray() {
        return visitedWorldspaceArray;
    }

    public StructUnknown3Table getUnknown3Table() {
        return unknown3Table;
    }

    @Override
    public String toString() {
        return "ESSFile{" +
                "basicData=" + basicData +
                ", header=" + header +
                ", pluginInfo=" + pluginInfo +
                ", fileLocationTable=" + fileLocationTable +
                ", globalDataTable1=" + globalDataTable1 +
                ", globalDataTable2=" + globalDataTable2 +
                ", globalDataTable3=" + globalDataTable3 +
                ", changeForms=" + changeForms +
                ", formIdArray=" + formIdArray +
                ", visitedWorldspaceArray=" + visitedWorldspaceArray +
                ", unknown3Table=" + unknown3Table +
                '}';
    }
}
