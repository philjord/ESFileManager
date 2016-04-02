package esmmanager.common.data.plugin;

import java.util.HashMap;
import java.util.Map;

import esmmanager.common.data.record.Subrecord;
import tools.io.ESMByteConvert;

public class PluginSubrecord extends Subrecord
{

	//for tes3 version
	protected PluginSubrecord()
	{

	}

	public PluginSubrecord(String recordType, String subrecordType, byte subrecordData[])
	{
		super(recordType, subrecordType, subrecordData);
	}

	public int[][] getReferences()
	{
		int[][] references = null;
		if (subrecordType.equals("CTDA"))
		{
			int functionCode = ESMByteConvert.extractInt(subrecordData, 8);

			FunctionInfo functionInfo2 = functionMap.get(new Integer(functionCode));
			if (functionInfo2 != null)
			{
				references = new int[2][2];
				int index = 0;
				if (functionInfo2.isFirstReference() && subrecordData.length >= 16)
				{
					references[index][0] = 12;
					references[index][1] = ESMByteConvert.extractInt(subrecordData, 12);
					index++;
				}
				if (functionInfo2.isSecondReference() && subrecordData.length >= 20)
				{
					references[index][0] = 16;
					references[index][1] = ESMByteConvert.extractInt(subrecordData, 16);
				}
			}
		}
		else if (subrecordType.equals("DATA") && recordType.equals("MGEF"))
		{
			int mgefOffsets[] = { 24, 32, 36, 40, 44, 48, 52 };
			references = new int[mgefOffsets.length][2];
			int index = 0;
			do
			{
				if (index >= mgefOffsets.length)
					break;
				int refOffset = mgefOffsets[index];
				if (refOffset + 4 > subrecordData.length)
					break;
				references[index][0] = refOffset;
				references[index][1] = ESMByteConvert.extractInt(subrecordData, refOffset);
				index++;
			}
			while (true);
		}
		else if (subrecordType.equals("PLDT") && recordType.equals("PACK"))
		{
			int type = ESMByteConvert.extractInt(subrecordData, 0);
			if (type == 0 || type == 1 || type == 4)
			{
				references = new int[1][2];
				references[0][0] = 4;
				references[0][1] = ESMByteConvert.extractInt(subrecordData, 4);
			}
		}
		else if (subrecordType.equals("PTDT") && recordType.equals("PACK"))
		{
			int type = ESMByteConvert.extractInt(subrecordData, 0);
			if (type == 0 || type == 1)
			{
				references = new int[1][2];
				references[0][0] = 4;
				references[0][1] = ESMByteConvert.extractInt(subrecordData, 4);
			}
		}
		else
		{
			boolean returnReferences = false;
			SubrecordInfo subrecordInfo2 = typeMap.get(subrecordType);
			if (subrecordInfo2 != null)
			{
				String recordTypes[] = subrecordInfo2.getRecordTypes();
				if (recordTypes.length == 0)
				{
					returnReferences = true;
				}
				else
				{
					int i = 0;
					do
					{
						if (i >= recordTypes.length)
							break;
						if (recordType.equals(recordTypes[i]))
						{
							returnReferences = true;
							break;
						}
						i++;
					}
					while (true);
				}
			}
			if (returnReferences)
			{

				int refOffsets[] = subrecordInfo2.getReferenceOffsets();
				int refOffset = 0;
				int refSize = 4;
				int i = -1;
				int index = 0;
				boolean repeating;
				if (refOffsets[0] < 0)
				{
					repeating = true;
					refSize = -refOffsets[0];
					refOffset = -refSize;
					references = new int[subrecordData.length / refSize][2];
				}
				else
				{
					repeating = false;
					references = new int[refOffsets.length][2];
				}
				do
				{
					if (repeating)
					{
						refOffset += refSize;
					}
					else
					{
						if (++i == refOffsets.length)
							break;
						refOffset = refOffsets[i];
					}
					if (refOffset + refSize > subrecordData.length)
						break;
					references[index][0] = refOffset;
					references[index][1] = ESMByteConvert.extractInt(subrecordData, refOffset);
					index++;
				}
				while (true);
			}
		}
		return references;
	}

	public String displaySubrecord()
	{
		StringBuffer dumpData = new StringBuffer(128 + 3 * subrecordData.length + 6 * (subrecordData.length / 16));
		dumpData.append("" + getSubrecordType() + " subrecord: Data length x'" + subrecordData.length + "'\n");
		dumpData.append("\n       0  1  2  3  4  5  6  7  8  9  A  B  C  D  E  F\n");
		StringBuffer dumpHex = new StringBuffer(48);
		StringBuffer dumpLine = new StringBuffer(16);
		for (int i = 0; i < subrecordData.length; i += 16)
		{
			for (int j = 0; j < 16; j++)
			{
				int offset = i + j;
				if (offset == subrecordData.length)
					break;
				//dumpHex.append(String.format(" %02X", new Object[] { Byte.valueOf(subrecordData[offset])}));
				dumpHex.append(" " + subrecordData[offset]);
				if (subrecordData[offset] >= 32 && subrecordData[offset] < 127)
					dumpLine.append(new String(subrecordData, offset, 1));
				else
					dumpLine.append(".");
			}

			for (; dumpHex.length() < 48; dumpHex.append("   "))
				;
			for (; dumpLine.length() < 16; dumpLine.append(" "))
				;
			dumpData.append("" + i + ":");
			dumpData.append(dumpHex);
			dumpData.append("  *");
			dumpData.append(dumpLine);
			dumpData.append("*");
			if (i + 16 < subrecordData.length)
				dumpData.append("\n");
			dumpHex.delete(0, 48);
			dumpLine.delete(0, 16);
		}

		return dumpData.toString();

	}

	private static Map<String, SubrecordInfo> typeMap;

	private static Map<Integer, FunctionInfo> functionMap;

	private static final int offsetRepeating4[] = { -4 };

	private static final int offsetRepeating8[] = { -8 };

	private static final int offsetRepeating12[] = { -12 };

	private static final int offsetRepeating52[] = { -52 };

	private static final int offsetZero[] = { 0 };

	private static final int offsetFour[] = { 4 };

	private static final int offsetZeroFour[] = { 0, 4 };

	//	private static final int offsetTwelveSixteen[] = { 12, 16 };
	private static final SubrecordInfo subrecordInfo[] = { new SubrecordInfo("ANAM", offsetZero, new String[] { "DOOR" }),
			new SubrecordInfo("BNAM", offsetZero, new String[] { "DOOR" }), new SubrecordInfo("BTXT", offsetZero, new String[] { "LAND" }),
			new SubrecordInfo("CNAM", offsetZero, new String[] { "NPC_", "WRLD" }), new SubrecordInfo("CNTO", offsetZero),
			new SubrecordInfo("CSCR", offsetZero, new String[] { "CREA" }), new SubrecordInfo("CSDI", offsetZero, new String[] { "CREA" }),
			new SubrecordInfo("DATA", offsetZero, new String[] { "ANIO" }),
			new SubrecordInfo("DNAM", offsetZeroFour, new String[] { "RACE" }),
			new SubrecordInfo("ENAM", offsetRepeating4, new String[] { "AMMO", "ARMO", "BOOK", "CLOT", "NPC_", "RACE", "WEAP" }),
			new SubrecordInfo("GNAM", offsetZero, new String[] { "LTEX" }),
			new SubrecordInfo("HNAM", offsetRepeating4, new String[] { "NPC_", "RACE" }),
			new SubrecordInfo("INAM", offsetZero, new String[] { "CREA", "NPC_" }),
			new SubrecordInfo("LVLO", offsetFour, new String[] { "LVLC", "LVLI", "LVSP" }), new SubrecordInfo("NAME", offsetZero),
			new SubrecordInfo("NAM2", offsetZero, new String[] { "WRLD" }), new SubrecordInfo("PFIG", offsetZero, new String[] { "FLOR" }),
			new SubrecordInfo("PKID", offsetZero), new SubrecordInfo("PNAM", offsetZero, new String[] { "INFO" }),
			new SubrecordInfo("QNAM", offsetZero, new String[] { "CONT" }), new SubrecordInfo("QSTA", offsetZero, new String[] { "QUST" }),
			new SubrecordInfo("QSTI", offsetZero), new SubrecordInfo("RDSD", offsetRepeating12, new String[] { "REGN" }),
			new SubrecordInfo("RDOT", offsetRepeating52, new String[] { "REGN" }),
			new SubrecordInfo("RDWT", offsetRepeating8, new String[] { "REGN" }),
			new SubrecordInfo("RNAM", offsetZero, new String[] { "NPC_" }),
			new SubrecordInfo("SCIT", offsetZero, new String[] { "ENCH", "INGR", "SPEL" }), new SubrecordInfo("SCRI", offsetZero),
			new SubrecordInfo("SCRO", offsetZero),
			new SubrecordInfo("SNAM", offsetZero, new String[] { "ACTI", "CONT", "CREA", "DOOR", "LIGH", "NPC_", "WATR", "WRLD" }),
			new SubrecordInfo("SPLO", offsetZero), new SubrecordInfo("TCLF", offsetZero, new String[] { "INFO" }),
			new SubrecordInfo("TCLT", offsetZero, new String[] { "INFO" }),
			new SubrecordInfo("TNAM", offsetZero, new String[] { "DOOR", "LVLC" }),
			new SubrecordInfo("VNAM", offsetZeroFour, new String[] { "RACE" }),
			new SubrecordInfo("WNAM", offsetZero, new String[] { "REGN", "WRLD" }),
			new SubrecordInfo("XCCM", offsetZero, new String[] { "CELL" }),
			new SubrecordInfo("XCLR", offsetRepeating4, new String[] { "CELL" }),
			new SubrecordInfo("XCWT", offsetZero, new String[] { "CELL" }), new SubrecordInfo("XESP", offsetZero),
			new SubrecordInfo("XGLB", offsetZero), new SubrecordInfo("XHRS", offsetZero, new String[] { "ACHR" }),
			new SubrecordInfo("XLOC", offsetFour, new String[] { "REFR" }), new SubrecordInfo("XMRC", offsetZero, new String[] { "ACHR" }),
			new SubrecordInfo("XNAM", offsetZero, new String[] { "FACT", "RACE" }), new SubrecordInfo("XOWN", offsetZero),
			new SubrecordInfo("XPCI", offsetZero), new SubrecordInfo("XRTM", offsetZero, new String[] { "REFR" }),
			new SubrecordInfo("XTEL", offsetZero, new String[] { "REFR" }), new SubrecordInfo("ZNAM", offsetZero) };

	private static final FunctionInfo functionInfo[] = { new FunctionInfo("GetCrime", 122, true, false),
			new FunctionInfo("GetDeadCount", 84, true, false), new FunctionInfo("GetDetected", 45, true, false),
			new FunctionInfo("GetDetectionLevel", 180, true, false), new FunctionInfo("GetDisposition", 76, true, false),
			new FunctionInfo("GetDistance", 1, true, false), new FunctionInfo("GetEquipped", 182, true, false),
			new FunctionInfo("GetFactionRank", 73, true, false), new FunctionInfo("GetFactionRankDifference", 60, true, true),
			new FunctionInfo("GetFriendHit", 288, true, false), new FunctionInfo("GetGlobalValue", 74, true, false),
			new FunctionInfo("GetHeadingAngle", 99, true, false), new FunctionInfo("GetInCell", 67, true, false),
			new FunctionInfo("GetInCellParam", 230, true, true), new FunctionInfo("GetInFaction", 71, true, false),
			new FunctionInfo("GetInSameCell", 32, true, false), new FunctionInfo("GetInWorldspace", 310, true, false),
			new FunctionInfo("GetIsClass", 68, true, false), new FunctionInfo("GetIsClassDefault", 228, true, false),
			new FunctionInfo("GetIsCurrentPackage", 161, true, false), new FunctionInfo("GetIsCurrentWeather", 149, true, false),
			new FunctionInfo("GetIsID", 72, true, false), new FunctionInfo("GetIsPlayerBirthsign", 224, true, false),
			new FunctionInfo("GetIsRace", 69, true, false), new FunctionInfo("GetIsReference", 136, true, false),
			new FunctionInfo("GetIsUsedItem", 246, true, false), new FunctionInfo("GetItemCount", 47, true, false),
			new FunctionInfo("GetLineOfSight", 27, true, false), new FunctionInfo("GetPCExpelled", 193, true, false),
			new FunctionInfo("GetPCFactionAttack", 199, true, false), new FunctionInfo("GetPCFactionMurder", 195, true, false),
			new FunctionInfo("GetPCFactionSteal", 197, true, false), new FunctionInfo("GetPCFactionSubmitAuthority", 201, true, false),
			new FunctionInfo("GetPCInFaction", 132, true, false), new FunctionInfo("GetPCIsClass", 129, true, false),
			new FunctionInfo("GetPCIsRace", 130, true, false), new FunctionInfo("GetQuestRunning", 56, true, false),
			new FunctionInfo("GetQuestVariable", 79, true, false), new FunctionInfo("GetScriptVariable", 53, true, false),
			new FunctionInfo("GetShouldAttack", 66, true, false), new FunctionInfo("GetStage", 58, true, false),
			new FunctionInfo("GetStageDone", 59, true, false), new FunctionInfo("GetTalkedToPCParam", 172, true, false),
			new FunctionInfo("HasMagicEffect", 214, true, false), new FunctionInfo("IsCellOwner", 280, true, true),
			new FunctionInfo("IsCurrentFurnitureObj", 163, true, false), new FunctionInfo("IsCurrentFurnitureRef", 162, true, false),
			new FunctionInfo("IsOwner", 278, true, false), new FunctionInfo("IsSpellTarget", 223, true, false),
			new FunctionInfo("SameFaction", 42, true, false), new FunctionInfo("SameRace", 43, true, false),
			new FunctionInfo("SameSex", 44, true, false) };

	static
	{
		if (typeMap == null)
		{
			typeMap = new HashMap<String, SubrecordInfo>(subrecordInfo.length);
			SubrecordInfo arr$[] = subrecordInfo;
			int len$ = arr$.length;
			for (int i$ = 0; i$ < len$; i$++)
			{
				SubrecordInfo info = arr$[i$];
				typeMap.put(info.getSubrecordType(), info);
			}

		}
		if (functionMap == null)
		{
			functionMap = new HashMap<Integer, FunctionInfo>(functionInfo.length);
			FunctionInfo arr$[] = functionInfo;
			int len$ = arr$.length;
			for (int i$ = 0; i$ < len$; i$++)
			{
				FunctionInfo info = arr$[i$];
				functionMap.put(new Integer(info.getCode()), info);
			}

		}
	}

	private static class FunctionInfo
	{
		private String functionName;

		private int functionCode;

		private boolean firstReference;

		private boolean secondReference;

		public FunctionInfo(String name, int code, boolean firstParam, boolean secondParam)
		{
			functionName = name;
			functionCode = code;
			firstReference = firstParam;
			secondReference = secondParam;
		}

		@SuppressWarnings("unused")
		public String getName()
		{
			return functionName;
		}

		public int getCode()
		{
			return functionCode;
		}

		public boolean isFirstReference()
		{
			return firstReference;
		}

		public boolean isSecondReference()
		{
			return secondReference;
		}

	}

	private static class SubrecordInfo
	{
		private String subrecordType;

		private String recordTypes[];

		private int referenceOffsets[];

		private static final String allRecordTypes[] = new String[0];

		public SubrecordInfo(String subrecordType, int referenceOffsets[], String recordTypes[])
		{
			this.subrecordType = subrecordType;
			this.referenceOffsets = referenceOffsets;
			this.recordTypes = new String[recordTypes.length];
			for (int i = 0; i < recordTypes.length; i++)
				this.recordTypes[i] = recordTypes[i];
		}

		public SubrecordInfo(String subrecordType, int referenceOffsets[])
		{
			this.subrecordType = subrecordType;
			this.referenceOffsets = referenceOffsets;
			recordTypes = allRecordTypes;
		}

		public String getSubrecordType()
		{
			return subrecordType;
		}

		public String[] getRecordTypes()
		{
			return recordTypes;
		}

		public int[] getReferenceOffsets()
		{
			return referenceOffsets;
		}

	}

}
