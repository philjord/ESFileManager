package TES4Gecko;

import java.util.HashMap;
import java.util.Map;

public final class SubrecordDataType
{
	public static final int Invalid = -1;

	public static final int ByteArray = 0;

	public static final int String = 1;

	public static final int FormID = 2;

	public static final int Integer = 3;

	public static final int Float = 4;

	public static final int Short = 5;

	public static final int Byte = 6;

	public static final int StringNoNull = 7;

	public static final int XYCoordinates = 8;

	public static final int ContainerItem = 9;

	public static final int Condition = 10;

	public static final int Emotion = 11;

	public static final int PositionRotation = 12;

	public static final int SpellEffectName = 13;

	public static final int SpellEffectData = 14;

	public static final int FormIDArray = 15;

	public static final int LeveledItem = 16;

	public static final int CellLighting = 17;

	public static final int Flags = 18;

	public static final int StringArray = 19;

	public static final int ActorConfig = 20;

	public static final int FactionInfo = 21;

	public static final int AIInfo = 22;

	public static final int PGNodeArray = 23;

	public static final int PGConnsInt = 24;

	public static final int PGConnsExt = 25;

	public static final int LSTexture = 26;

	public static final int DATAforINFO = 100;

	public static final int DATAforCREA = 101;

	public static final int Other = 999;

	public static final int FormatVaries = 1000;

	private static Map<String, Integer> dataTypeMap = new HashMap<String, Integer>();

	private static Map<Integer, String> dataTypeLabelMap;

	static
	{
		dataTypeMap.put("CSCR", 2);
		dataTypeMap.put("CSDI", 2);
		dataTypeMap.put("ENAM", 2);
		dataTypeMap.put("NAME", 2);
		dataTypeMap.put("PFIG", 2);
		dataTypeMap.put("PKID", 2);
		dataTypeMap.put("PNAM", 2);
		dataTypeMap.put("QNAM", 2);
		dataTypeMap.put("QSTI", 2);
		dataTypeMap.put("QSTR", 2);
		dataTypeMap.put("SCRI", 2);
		dataTypeMap.put("SCRO", 2);
		dataTypeMap.put("SPLO", 2);
		dataTypeMap.put("TCLT", 2);
		dataTypeMap.put("XCCM", 2);
		dataTypeMap.put("XCWT", 2);
		dataTypeMap.put("XESP", 2);
		dataTypeMap.put("XGLB", 2);
		dataTypeMap.put("XHRS", 2);
		dataTypeMap.put("XMRC", 2);
		dataTypeMap.put("XOWN", 2);
		dataTypeMap.put("ZNAM", 2);

		dataTypeMap.put("FLTV", 4);
		dataTypeMap.put("XCLW", 4);
		dataTypeMap.put("XSCL", 4);

		dataTypeMap.put("CSDT", 3);
		dataTypeMap.put("XRNK", 3);

		dataTypeMap.put("LVLD", 6);
		dataTypeMap.put("LVLF", 6);
		dataTypeMap.put("XCMT", 6);

		dataTypeMap.put("DESC", 1);
		dataTypeMap.put("EDID", 1);
		dataTypeMap.put("FULL", 1);
		dataTypeMap.put("GNAM", 1);
		dataTypeMap.put("ICO2", 1);
		dataTypeMap.put("ICON", 1);
		dataTypeMap.put("MOD2", 1);
		dataTypeMap.put("MOD3", 1);
		dataTypeMap.put("MOD4", 1);
		dataTypeMap.put("MODL", 1);
		dataTypeMap.put("SCVR", 1);

		dataTypeMap.put("EFID", 7);
		dataTypeMap.put("SCTX", 7);

		dataTypeMap.put("NIFZ", 19);
		dataTypeMap.put("KFFZ", 19);

		dataTypeMap.put("XCLC", 8);

		dataTypeMap.put("CNTO", 9);

		dataTypeMap.put("LVLO", 16);

		dataTypeMap.put("CTDA", 10);

		dataTypeMap.put("TRDT", 11);

		dataTypeMap.put("EFID", 13);

		dataTypeMap.put("EFIT", 14);

		dataTypeMap.put("XCLR", 15);
		dataTypeMap.put("VNAM", 15);

		dataTypeMap.put("XCLL", 17);

		dataTypeMap.put("ACBS", 20);

		dataTypeMap.put("AIDT", 22);

		dataTypeMap.put("PGRP", 23);

		dataTypeMap.put("PGRR", 24);

		dataTypeMap.put("PGRI", 25);

		dataTypeMap.put("ATXT", 26);
		dataTypeMap.put("BTXT", 26);

		dataTypeMap.put("BNAM", 1000);
		dataTypeMap.put("BNAM-CREA", 4);
		dataTypeMap.put("BNAM-DOOR", 2);
		dataTypeMap.put("CNAM", 1000);
		dataTypeMap.put("CNAM-NPC_", 2);
		dataTypeMap.put("CNAM-QUST", 1);
		dataTypeMap.put("CNAM-WRLD", 2);
		dataTypeMap.put("CNAM-WTHR", 1);
		dataTypeMap.put("DATA", 1000);
		dataTypeMap.put("DATA-ACHR", 12);
		dataTypeMap.put("DATA-ACRE", 12);
		dataTypeMap.put("DATA-ALCH", 4);
		dataTypeMap.put("DATA-ANIO", 2);
		dataTypeMap.put("DATA-CELL", 18);
		dataTypeMap.put("DATA-CREA", 101);
		dataTypeMap.put("DATA-GMST", 4);
		dataTypeMap.put("DATA-INFO", 100);
		dataTypeMap.put("DATA-PGRD", 5);
		dataTypeMap.put("DATA-REFR", 12);
		dataTypeMap.put("ENAM", 1000);
		dataTypeMap.put("ENAM-RACE", 15);
		dataTypeMap.put("ENIT", 1000);
		dataTypeMap.put("FNAM", 1000);
		dataTypeMap.put("FNAM-GLOB", 6);
		dataTypeMap.put("FNAM-LIGH", 4);
		dataTypeMap.put("HNAM", 1000);
		dataTypeMap.put("HNAM-RACE", 15);
		dataTypeMap.put("INAM", 1000);
		dataTypeMap.put("INAM-FACT", 1);
		dataTypeMap.put("INAM-CREA", 2);
		dataTypeMap.put("INAM-NPC_", 2);
		dataTypeMap.put("MODB", 1000);
		dataTypeMap.put("MODB-LIGH", 4);
		dataTypeMap.put("NAM0", 1000);
		dataTypeMap.put("NAM0-CREA", 1);
		dataTypeMap.put("NAM1", 1000);
		dataTypeMap.put("NAM1-RACE", 0);
		dataTypeMap.put("NAM1-OTHER", 1);
		dataTypeMap.put("NAM2", 1000);
		dataTypeMap.put("NAM2-INFO", 1);
		dataTypeMap.put("NAM2-WRLD", 2);
		dataTypeMap.put("RNAM", 1000);
		dataTypeMap.put("RNAM-CREA", 6);
		dataTypeMap.put("RNAM-NPC_", 2);
		dataTypeMap.put("SNAM", 1000);
		dataTypeMap.put("SNAM-ACTI", 2);
		dataTypeMap.put("SNAM-CONT", 2);
		dataTypeMap.put("SNAM-CREA", 21);
		dataTypeMap.put("SNAM-DOOR", 2);
		dataTypeMap.put("SNAM-LIGH", 2);
		dataTypeMap.put("SNAM-NPC_", 21);
		dataTypeMap.put("SNAM-WRLD", 2);
		dataTypeMap.put("TNAM", 1000);
		dataTypeMap.put("TNAM-CREA", 4);
		dataTypeMap.put("TNAM-DOOR", 2);
		dataTypeMap.put("TNAM-LVLC", 2);
		dataTypeMap.put("WNAM", 1000);
		dataTypeMap.put("WNAM-CREA", 4);
		dataTypeMap.put("WNAM-OTHER", 2);

		dataTypeLabelMap = new HashMap<Integer, String>();
		dataTypeLabelMap.put(-1, "Unknown Type (Shown As Byte Array)");
		dataTypeLabelMap.put(0, "Byte Array");
		dataTypeLabelMap.put(1, "String");
		dataTypeLabelMap.put(7, "String");
		dataTypeLabelMap.put(19, "String Array");
		dataTypeLabelMap.put(2, "Form ID");
		dataTypeLabelMap.put(3, "Integer");
		dataTypeLabelMap.put(4, "Float");
		dataTypeLabelMap.put(5, "Short");
		dataTypeLabelMap.put(6, "Byte");
		dataTypeLabelMap.put(8, "X,Y Coordinates");
		dataTypeLabelMap.put(9, "Container Item Info");
		dataTypeLabelMap.put(10, "Condition Info");
		dataTypeLabelMap.put(11, "Emotion Info");
		dataTypeLabelMap.put(16, "Leveled Item Info");
		dataTypeLabelMap.put(12, "Position & Rotation Info");
		dataTypeLabelMap.put(13, "Spell Effect Name");
		dataTypeLabelMap.put(14, "Spell Effect Info");
		dataTypeLabelMap.put(15, "Form ID Array");
		dataTypeLabelMap.put(17, "Cell Lighting Info");
		dataTypeLabelMap.put(18, "Binary Flags");
		dataTypeLabelMap.put(20, "Actor Configuration");
		dataTypeLabelMap.put(21, "Faction Info");
		dataTypeLabelMap.put(22, "AI Info");
		dataTypeLabelMap.put(23, "Path Grid Node Array");
		dataTypeLabelMap.put(24, "Path Grid Internal Connections");
		dataTypeLabelMap.put(25, "Path Grid External Connections");
		dataTypeLabelMap.put(26, "Landscape Texture Data");
		dataTypeLabelMap.put(100, "DATA for INFO Type Format");
		dataTypeLabelMap.put(101, "DATA for CREA Type Format");
		dataTypeLabelMap.put(1000, "Record-Dependent Format");
		dataTypeLabelMap.put(999, "Specific");
	}

	public static boolean isValid(int param)
	{
		return (param == -1) || (param == 0) || (param == 1) || (param == 7) || (param == 2) || (param == 3) || (param == 4)
				|| (param == 5) || (param == 6) || (param == 8) || (param == 9) || (param == 10) || (param == 11) || (param == 16)
				|| (param == 12) || (param == 13) || (param == 14) || (param == 15) || (param == 17) || (param == 18) || (param == 19)
				|| (param == 20) || (param == 21) || (param == 22) || (param == 23) || (param == 24) || (param == 25) || (param == 26)
				|| ((param >= 100) && (param <= 101)) || (param == 1000) || (param == 999);
	}

	public static int getDataType(String subrecType)
	{
		if (!dataTypeMap.containsKey(subrecType))
			return -1;
		return dataTypeMap.get(subrecType).intValue();
	}

	public static String getDataTypeLabel(int subrecDataType)
	{
		if (!dataTypeLabelMap.containsKey(subrecDataType))
			return "Invalid";
		return dataTypeLabelMap.get(subrecDataType);
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.SubrecordDataType
 * JD-Core Version:    0.6.0
 */