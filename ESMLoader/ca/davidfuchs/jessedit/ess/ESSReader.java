package ca.davidfuchs.jessedit.ess;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ESSReader {
    private ESSInputStream essInputStream;
    private ESSFile essFile;

    private ESSReader(ESSFile essFile, InputStream inputStream) {
        this.essFile = essFile;
        this.essInputStream = new ESSInputStream(inputStream);
    }

    public static ESSFile readESSFile(InputStream inputStream) throws IOException {
        ESSFile essFile = new ESSFile();

        ESSReader essReader = new ESSReader(essFile, inputStream);
        essReader.readESSFile();

        return essFile;
    }

    public void readESSFile() throws IOException {
        StructBasicData basicData = essFile.getBasicData();

        {
            basicData.setMagic(String.valueOf(essInputStream.readUTF8Array(13)));
            basicData.setHeaderSize(essInputStream.readUnsignedInt());
        }

        StructHeader header = essFile.getHeader();

        {
            header.setVersion(essInputStream.readUnsignedInt());
            header.setSaveNumber(essInputStream.readUnsignedInt());
            header.setPlayerName(essInputStream.readWideString());
            header.setPlayerLevel(essInputStream.readUnsignedInt());
            header.setPlayerLocation(essInputStream.readWideString());
            header.setGameDate(essInputStream.readWideString());
            header.setPlayerRaceEditorId(essInputStream.readWideString());

            header.setPlayerSex(essInputStream.readUnsignedShort());

            header.setPlayerCurExp(essInputStream.readFloat());
            header.setPlayerLvlUpExp(essInputStream.readFloat());
            header.setFileTime(essInputStream.readFileTime());

            header.setShotWidth(essInputStream.readUnsignedInt());
            header.setShotHeight(essInputStream.readUnsignedInt());
        }

        {
            long screenShotLength = 3 * header.getShotWidth() * header.getShotHeight();

            basicData.setScreenShotData(essInputStream.readUnsignedByteArray((int) screenShotLength));
        }

        {
            basicData.setFormVersion(essInputStream.readUnsignedByte());
            basicData.setPluginInfoSize(essInputStream.readUnsignedInt());
        }

        StructPluginInfo pluginInfo = essFile.getPluginInfo();

        {
            pluginInfo.setPluginCount(essInputStream.readUnsignedByte());

            ArrayList<String> plugins = new ArrayList<String>(pluginInfo.getPluginCount());

            for (int index = 0; index < pluginInfo.getPluginCount(); index++) {
                plugins.add(essInputStream.readWideString());
            }

            pluginInfo.setPlugins(plugins);
        }

        StructFileLocationTable fileLocationTable = essFile.getFileLocationTable();

        {
            fileLocationTable.setFormIdArrayCountOffset(essInputStream.readUnsignedInt());
            fileLocationTable.setUnknownTable3Offset(essInputStream.readUnsignedInt());
            fileLocationTable.setGlobalDataTable1Offset(essInputStream.readUnsignedInt());
            fileLocationTable.setGlobalDataTable2Offset(essInputStream.readUnsignedInt());
            fileLocationTable.setChangeFormsOffset(essInputStream.readUnsignedInt());
            fileLocationTable.setGlobalDataTable3Offset(essInputStream.readUnsignedInt());
            fileLocationTable.setGlobalDataTable1Count(essInputStream.readUnsignedInt());
            fileLocationTable.setGlobalDataTable2Count(essInputStream.readUnsignedInt());
            fileLocationTable.setGlobalDataTable3Count(essInputStream.readUnsignedInt() + 1);
            fileLocationTable.setChangeFormsCount(essInputStream.readUnsignedInt());
            fileLocationTable.setUnused(essInputStream.readUnsignedIntArray(15));
        }

        {
            populateGlobalDataTable(essFile.getGlobalDataTable1(), fileLocationTable.getGlobalDataTable1Count());
            populateGlobalDataTable(essFile.getGlobalDataTable2(), fileLocationTable.getGlobalDataTable2Count());
            populateChangeForms(essFile.getChangeForms(), fileLocationTable.getChangeFormsCount());
            populateGlobalDataTable(essFile.getGlobalDataTable3(), fileLocationTable.getGlobalDataTable3Count());
        }

        {
            basicData.setFormIdArrayCount(essInputStream.readUnsignedInt());
            populateFormIdArray(essFile.getFormIdArray(), basicData.getFormIdArrayCount());
        }

        {
            basicData.setVisitedWorldspaceArrayCount(essInputStream.readUnsignedInt());
            populateFormIdArray(essFile.getVisitedWorldspaceArray(), basicData.getVisitedWorldspaceArrayCount());
        }

        {
            basicData.setUnknown3TableSize(essInputStream.readUnsignedInt());
        }

        StructUnknown3Table structUnknown3Table = essFile.getUnknown3Table();

        {
            structUnknown3Table.setCount(basicData.getUnknown3TableSize());
            structUnknown3Table.setUnknown(String.valueOf(essInputStream.readUTF8Array((int) basicData.getUnknown3TableSize())));
        }
    }

    private void populateGlobalDataTable(List<StructGlobalData> target, long size) throws IOException {
        for (int index = 0; index < size; index++) {
            target.add(readStructGlobalData());
        }
    }

    private void populateChangeForms(List<StructChangeForms> target, long size) throws IOException {
        for (int index = 0; index < size; index++) {
            target.add(readChangeForms());
        }
    }

    private void populateFormIdArray(List<StructFormId> target, long size) throws IOException {
        for (int index = 0; index < size; index++) {
            target.add(readFormId());
        }
    }

    private StructGlobalData readStructGlobalData() throws IOException {
        StructGlobalData structGlobalData = new StructGlobalData();

        structGlobalData.setType(essInputStream.readUnsignedInt());
        structGlobalData.setLength(essInputStream.readUnsignedInt());
        structGlobalData.setData(essInputStream.readUnsignedByteArray((int) structGlobalData.getLength()));

        return structGlobalData;
    }

    private StructChangeForms readChangeForms() throws IOException {
        StructChangeForms structChangeForms = new StructChangeForms();
        structChangeForms.setRefId(readRefId());

        structChangeForms.setChangeFlags(essInputStream.readUnsignedInt());
        structChangeForms.setType(essInputStream.readUnsignedByte());
        structChangeForms.setVersion(essInputStream.readUnsignedByte());

        // The getFieldLengthType() method operates on the 'type' byte we just read, which actually holds two
        // different pieces of data. See StructChangeForms.getFieldLengthType() for more information.
        short fieldLengthType = structChangeForms.getFieldLengthType();

        if (fieldLengthType == 0) {
            structChangeForms.setLength1(essInputStream.readUnsignedByte());
            structChangeForms.setLength2(essInputStream.readUnsignedByte());
        } else if (fieldLengthType == 1) {
            structChangeForms.setLength1(essInputStream.readUnsignedShort());
            structChangeForms.setLength2(essInputStream.readUnsignedShort());
        } else if (fieldLengthType == 2) {
            structChangeForms.setLength1(essInputStream.readUnsignedInt());
            structChangeForms.setLength2(essInputStream.readUnsignedInt());
        }

        structChangeForms.setData(essInputStream.readBytes((int) structChangeForms.getLength1()));

        return structChangeForms;
    }


    private StructRefId readRefId() throws IOException {
        StructRefId refId = new StructRefId();

        byte[] bytes = essInputStream.readBytes(3);

        refId.setBytes(new byte[]{bytes[2], bytes[1], bytes[0]});

        return refId;
    }

    private StructFormId readFormId() throws IOException {
        StructFormId formId = new StructFormId();

        byte[] bytes = essInputStream.readBytes(3);

        formId.setBytes(new byte[]{bytes[2], bytes[1], bytes[0]});
        formId.setCount(essInputStream.readUnsignedByte());

        return formId;
    }
}
