package ca.davidfuchs.jessedit.ess;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class ESSWriter {
    private static final Logger logger = LoggerFactory.getLogger(ESSWriter.class);

    private ESSOutputStream essOutputStream;
    private ESSFile essFile;

    private ESSWriter(OutputStream outputStream, ESSFile essFile) {
        this.essOutputStream = new ESSOutputStream(outputStream);
        this.essFile = essFile;
    }

    public static void writeESSFile(OutputStream outputStream, ESSFile essFile) throws IOException {
        new ESSWriter(outputStream, essFile).writeESSFile();
    }

    public void writeESSFile() throws IOException {
        StructBasicData basicData = essFile.getBasicData();

        {
            essOutputStream.writeUTF8Array(basicData.getMagic());
            essOutputStream.writeUnsignedInt(basicData.getHeaderSize());
        }

        StructHeader header = essFile.getHeader();

        {
            essOutputStream.writeUnsignedInt(header.getVersion());
            essOutputStream.writeUnsignedInt(header.getSaveNumber());
            essOutputStream.writeWideString(header.getPlayerName());
            essOutputStream.writeUnsignedInt(header.getPlayerLevel());
            essOutputStream.writeWideString(header.getPlayerLocation());
            essOutputStream.writeWideString(header.getGameDate());
            essOutputStream.writeWideString(header.getPlayerRaceEditorId());

            essOutputStream.writeUnsignedShort(header.getPlayerSex());

            essOutputStream.writeFloat(header.getPlayerCurExp());
            essOutputStream.writeFloat(header.getPlayerLvlUpExp());
            essOutputStream.writeFileTime(header.getFileTime());

            essOutputStream.writeUnsignedInt(header.getShotWidth());
            essOutputStream.writeUnsignedInt(header.getShotHeight());
        }

        {
            essOutputStream.writeUnsignedByteArray(basicData.getScreenShotData());
        }

        {
            essOutputStream.writeUnsignedByte(basicData.getFormVersion());
            essOutputStream.writeUnsignedInt(basicData.getPluginInfoSize());
        }

        StructPluginInfo pluginInfo = essFile.getPluginInfo();

        {
            essOutputStream.writeUnsignedByte(pluginInfo.getPluginCount());

            for (String pluginName : pluginInfo.getPlugins()) {
                essOutputStream.writeWideString(pluginName);
            }
        }

        StructFileLocationTable fileLocationTable = essFile.getFileLocationTable();

        {
            essOutputStream.writeUnsignedInt(fileLocationTable.getFormIdArrayCountOffset());
            essOutputStream.writeUnsignedInt(fileLocationTable.getUnknownTable3Offset());
            essOutputStream.writeUnsignedInt(fileLocationTable.getGlobalDataTable1Offset());
            essOutputStream.writeUnsignedInt(fileLocationTable.getGlobalDataTable2Offset());
            essOutputStream.writeUnsignedInt(fileLocationTable.getChangeFormsOffset());
            essOutputStream.writeUnsignedInt(fileLocationTable.getGlobalDataTable3Offset());
            essOutputStream.writeUnsignedInt(fileLocationTable.getGlobalDataTable1Count());
            essOutputStream.writeUnsignedInt(fileLocationTable.getGlobalDataTable2Count());
            essOutputStream.writeUnsignedInt(fileLocationTable.getGlobalDataTable3Count() - 1);
            essOutputStream.writeUnsignedInt(fileLocationTable.getChangeFormsCount());
            essOutputStream.writeUnsignedIntArray(fileLocationTable.getUnused());
        }

        writeGlobalDataTable(essFile.getGlobalDataTable1());
        writeGlobalDataTable(essFile.getGlobalDataTable2());
        writeChangeForms(essFile.getChangeForms());
        writeGlobalDataTable(essFile.getGlobalDataTable3());

        {
            essOutputStream.writeUnsignedInt(basicData.getFormIdArrayCount());
            writeFormIdArray(essFile.getFormIdArray());
        }

        {
            essOutputStream.writeUnsignedInt((int) basicData.getVisitedWorldspaceArrayCount());
            writeFormIdArray(essFile.getVisitedWorldspaceArray());
        }

        {
            essOutputStream.writeUnsignedInt(basicData.getUnknown3TableSize());
            essOutputStream.writeUTF8Array(essFile.getUnknown3Table().getUnknown());
        }
    }

    private void writeGlobalDataTable(List<StructGlobalData> target) throws IOException {
        for (StructGlobalData structGlobalData : target) {
            essOutputStream.writeUnsignedInt(structGlobalData.getType());
            essOutputStream.writeUnsignedInt(structGlobalData.getLength());
            essOutputStream.writeUnsignedByteArray(structGlobalData.getData());
        }
    }

    private void writeChangeForms(List<StructChangeForms> target) throws IOException {
        for (StructChangeForms structChangeForms : target) {
            writeRefId(structChangeForms.getRefId());
            essOutputStream.writeUnsignedInt(structChangeForms.getChangeFlags());
            essOutputStream.writeUnsignedByte(structChangeForms.getType());
            essOutputStream.writeUnsignedByte(structChangeForms.getVersion());

            short fieldLengthType = structChangeForms.getFieldLengthType();

            if (fieldLengthType == 0) {
                essOutputStream.writeUnsignedByte((byte) structChangeForms.getLength1());
                essOutputStream.writeUnsignedByte((byte) structChangeForms.getLength2());
            } else if (fieldLengthType == 1) {
                essOutputStream.writeUnsignedShort((int) structChangeForms.getLength1());
                essOutputStream.writeUnsignedShort((int) structChangeForms.getLength2());
            } else if (fieldLengthType == 2) {
                essOutputStream.writeUnsignedInt(structChangeForms.getLength1());
                essOutputStream.writeUnsignedInt(structChangeForms.getLength2());
            }

            essOutputStream.writeBytes(structChangeForms.getData());
        }
    }

    private void writeFormIdArray(List<StructFormId> target) throws IOException {
        for (StructFormId structFormId : target) {
            essOutputStream.writeBytes(new byte[]{structFormId.getBytes()[2], structFormId.getBytes()[1], structFormId.getBytes()[0]});
            essOutputStream.writeUnsignedByte(structFormId.getCount());
        }
    }

    private void writeRefIdArray(List<StructRefId> target) throws IOException {
        for (StructRefId structRefId : target) {
            writeRefId(structRefId);
        }
    }

    private void writeRefIdArray2(List<StructRefId> target) throws IOException {
        for (StructRefId structRefId : target) {
            writeRefId(structRefId);
        }
    }

    private void writeRefId(StructRefId structFormId) throws IOException {
        essOutputStream.writeBytes(new byte[]{structFormId.getBytes()[2], structFormId.getBytes()[1], structFormId.getBytes()[0]});
    }
}
