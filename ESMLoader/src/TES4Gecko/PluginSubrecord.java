package TES4Gecko;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PluginSubrecord extends SerializedElement
{
	private String recordType;

	private String subrecordType;

	private boolean spillMode = false;

	private long subrecordPosition = -1L;

	private int subrecordLength;

	private byte[] subrecordData;

	private static Map<String, SubrecordInfo> typeMap;

	private static Map<Integer, FunctionInfo> functionMap;

	private static final int[] offsetRepeating4 =
	{ -4 };

	private static final int[] offsetRepeating8 =
	{ -8 };

	private static final int[] offsetRepeating12 =
	{ -12 };

	private static final int[] offsetRepeating52 =
	{ -52 };

	private static final int[] offsetZero = new int[1];

	private static final int[] offsetFour =
	{ 4 };

	private static final int[] offsetZeroFour =
	{ 0, 4 };

	private static final int[] offsetTwelveSixteen =
	{ 12, 16 };

	private static final SubrecordInfo[] subrecordInfo =
	{ new SubrecordInfo("ANAM", offsetZero, new String[]
	{ "DOOR" }), new SubrecordInfo("ATXT", offsetZero, new String[]
	{ "LAND" }), new SubrecordInfo("BNAM", offsetZero, new String[]
	{ "DOOR" }), new SubrecordInfo("BTXT", offsetZero, new String[]
	{ "LAND" }), new SubrecordInfo("CNAM", offsetZero, new String[]
	{ "NPC_", "WRLD" }), new SubrecordInfo("CNTO", offsetZero), new SubrecordInfo("CSCR", offsetZero, new String[]
	{ "CREA" }), new SubrecordInfo("CSDI", offsetZero, new String[]
	{ "CREA" }), new SubrecordInfo("DATA", offsetRepeating4, new String[]
	{ "ANIO", "IDLE" }), new SubrecordInfo("DNAM", offsetZeroFour, new String[]
	{ "RACE" }), new SubrecordInfo("VNAM", offsetZeroFour, new String[]
	{ "RACE" }), new SubrecordInfo("ENAM", offsetRepeating4, new String[]
	{ "AMMO", "ARMO", "BOOK", "CLOT", "NPC_", "RACE", "WEAP" }), new SubrecordInfo("GNAM", offsetZero, new String[]
	{ "LTEX" }), new SubrecordInfo("HNAM", offsetRepeating4, new String[]
	{ "NPC_", "RACE" }), new SubrecordInfo("INAM", offsetZero, new String[]
	{ "CREA", "NPC_" }), new SubrecordInfo("LNAM", offsetZero, new String[]
	{ "LSCR" }), new SubrecordInfo("LVLO", offsetFour, new String[]
	{ "LVLC", "LVLI", "LVSP" }), new SubrecordInfo("NAME", offsetZero), new SubrecordInfo("NAM2", offsetZero, new String[]
	{ "WRLD" }), new SubrecordInfo("PFIG", offsetZero, new String[]
	{ "FLOR" }), new SubrecordInfo("PKID", offsetZero), new SubrecordInfo("PNAM", offsetZero, new String[]
	{ "INFO" }), new SubrecordInfo("PGRL", offsetZero, new String[]
	{ "PGRD" }), new SubrecordInfo("QNAM", offsetZero, new String[]
	{ "CONT" }), new SubrecordInfo("QSTA", offsetZero, new String[]
	{ "QUST" }), new SubrecordInfo("QSTI", offsetZero), new SubrecordInfo("RDSD", offsetRepeating12, new String[]
	{ "REGN" }), new SubrecordInfo("RDOT", offsetRepeating52, new String[]
	{ "REGN" }), new SubrecordInfo("RDWT", offsetRepeating8, new String[]
	{ "REGN" }), new SubrecordInfo("RNAM", offsetZero, new String[]
	{ "NPC_" }), new SubrecordInfo("SCIT", offsetZero, new String[]
	{ "ENCH", "INGR", "SPEL" }), new SubrecordInfo("SCRI", offsetZero), new SubrecordInfo("SCRO", offsetZero),
			new SubrecordInfo("SNAM", offsetZero, new String[]
			{ "ACTI", "CONT", "CREA", "DOOR", "LIGH", "NPC_", "WATR", "WRLD", "WTHR" }), new SubrecordInfo("SPLO", offsetZero),
			new SubrecordInfo("TCLF", offsetZero, new String[]
			{ "INFO" }), new SubrecordInfo("TCLT", offsetZero, new String[]
			{ "INFO" }), new SubrecordInfo("TNAM", offsetZero, new String[]
			{ "DOOR", "LVLC" }), new SubrecordInfo("VNAM", offsetZeroFour, new String[]
			{ "RACE" }), new SubrecordInfo("WLST", offsetRepeating8, new String[]
			{ "CLMT" }), new SubrecordInfo("WNAM", offsetZero, new String[]
			{ "REGN", "WRLD" }), new SubrecordInfo("XCCM", offsetZero, new String[]
			{ "CELL" }), new SubrecordInfo("XCLR", offsetRepeating4, new String[]
			{ "CELL" }), new SubrecordInfo("XCWT", offsetZero, new String[]
			{ "CELL" }), new SubrecordInfo("XESP", offsetZero), new SubrecordInfo("XGLB", offsetZero),
			new SubrecordInfo("XHRS", offsetZero, new String[]
			{ "ACHR" }), new SubrecordInfo("XLOC", offsetFour, new String[]
			{ "REFR" }), new SubrecordInfo("XMRC", offsetZero, new String[]
			{ "ACHR" }), new SubrecordInfo("XNAM", offsetZero, new String[]
			{ "FACT", "RACE" }), new SubrecordInfo("XOWN", offsetZero), new SubrecordInfo("XPCI", offsetZero),
			new SubrecordInfo("XRTM", offsetZero, new String[]
			{ "REFR" }), new SubrecordInfo("XTEL", offsetZero, new String[]
			{ "REFR" }), new SubrecordInfo("ZNAM", offsetZero) };

	private static final String[][] subrecordDataTypes = new String[0][];

	private static final FunctionInfo[] functionInfo =
	{ new FunctionInfo("CanHaveFlames", 153, false, false), new FunctionInfo("CanPayCrimeGold", 127, false, false),
			new FunctionInfo("GetActorValue", 14, true, false), new FunctionInfo("GetAlarmed", 61, false, false),
			new FunctionInfo("GetAmountSoldStolen", 190, false, false), new FunctionInfo("GetAngle", 8, true, false),
			new FunctionInfo("GetArmorRating", 81, false, false), new FunctionInfo("GetArmorRatingUpperBody", 274, false, false),
			new FunctionInfo("GetAttacked", 63, false, false), new FunctionInfo("GetBarterGold", 264, false, false),
			new FunctionInfo("GetBaseActorValue", 277, true, false), new FunctionInfo("GetClassDefaultMatch", 229, false, false),
			new FunctionInfo("GetClothingValue", 41, false, false), new FunctionInfo("GetCrime", 122, true, true),
			new FunctionInfo("GetCrimeGold", 116, false, false), new FunctionInfo("GetCurrentAIPackage", 110, false, false),
			new FunctionInfo("GetCurrentAIProcedure", 143, false, false), new FunctionInfo("GetCurrentTime", 18, false, false),
			new FunctionInfo("GetCurrentWeatherPercent", 148, false, false), new FunctionInfo("GetDayOfWeek", 170, false, false),
			new FunctionInfo("GetDead", 46, false, false), new FunctionInfo("GetDeadCount", 84, true, false),
			new FunctionInfo("GetDestroyed", 203, false, false), new FunctionInfo("GetDetected", 45, true, false),
			new FunctionInfo("GetDetectionLevel", 180, true, false), new FunctionInfo("GetDisabled", 35, false, false),
			new FunctionInfo("GetDisease", 39, false, false), new FunctionInfo("GetDisposition", 76, true, false),
			new FunctionInfo("GetDistance", 1, true, false), new FunctionInfo("GetDoorDefaultOpen", 215, false, false),
			new FunctionInfo("GetEquipped", 182, true, false), new FunctionInfo("GetFactionRank", 73, true, false),
			new FunctionInfo("GetFactionRankDifference", 60, true, true), new FunctionInfo("GetFatiguePercentage", 128, false, false),
			new FunctionInfo("GetFriendHit", 288, true, false), new FunctionInfo("GetFurnitureMarkerID", 160, false, false),
			new FunctionInfo("GetGlobalValue", 74, true, false), new FunctionInfo("GetGold", 48, false, false),
			new FunctionInfo("GetHeadingAngle", 99, true, false), new FunctionInfo("GetIdleDoneOnce", 318, false, false),
			new FunctionInfo("GetIgnoreFriendlyHits", 338, false, false), new FunctionInfo("GetInCell", 67, true, false),
			new FunctionInfo("GetInCellParam", 230, true, true), new FunctionInfo("GetInFaction", 71, true, false),
			new FunctionInfo("GetInSameCell", 32, true, false), new FunctionInfo("GetInWorldspace", 310, true, false),
			new FunctionInfo("GetInvestmentGold", 305, false, false), new FunctionInfo("GetIsAlerted", 91, false, false),
			new FunctionInfo("GetIsClass", 68, true, false), new FunctionInfo("GetIsClassDefault", 228, true, false),
			new FunctionInfo("GetIsCreature", 64, false, false), new FunctionInfo("GetIsCurrentPackage", 161, true, false),
			new FunctionInfo("GetIsCurrentWeather", 149, true, false), new FunctionInfo("GetIsGhost", 237, false, false),
			new FunctionInfo("GetIsID", 72, true, false), new FunctionInfo("GetIsPlayableRace", 254, false, false),
			new FunctionInfo("GetIsPlayerBirthsign", 224, true, false), new FunctionInfo("GetIsRace", 69, true, false),
			new FunctionInfo("GetIsReference", 136, true, false), new FunctionInfo("GetIsSex", 70, true, false),
			new FunctionInfo("GetIsUsedItem", 246, true, false), new FunctionInfo("GetIsUsedItemType", 247, true, false),
			new FunctionInfo("GetItemCount", 47, true, false), new FunctionInfo("GetKnockedState", 107, false, false),
			new FunctionInfo("GetLevel", 80, false, false), new FunctionInfo("GetLineOfSight", 27, true, false),
			new FunctionInfo("GetLockLevel", 65, false, false), new FunctionInfo("GetLocked", 5, false, false),
			new FunctionInfo("GetNoRumors", 320, false, false), new FunctionInfo("GetOffersServicesNow", 255, false, false),
			new FunctionInfo("GetOpenState", 157, false, false), new FunctionInfo("GetPCExpelled", 193, true, false),
			new FunctionInfo("GetPCFactionAttack", 199, true, false), new FunctionInfo("GetPCFactionMurder", 195, true, false),
			new FunctionInfo("GetPCFactionSteal", 197, true, false), new FunctionInfo("GetPCFactionSubmitAuthority", 201, true, false),
			new FunctionInfo("GetPCFame", 249, false, false), new FunctionInfo("GetPCInFaction", 132, true, false),
			new FunctionInfo("GetPCInfamy", 251, false, false), new FunctionInfo("GetPCIsClass", 129, true, false),
			new FunctionInfo("GetPCIsRace", 130, true, false), new FunctionInfo("GetPCIsSex", 131, true, false),
			new FunctionInfo("GetPCMiscStat", 312, true, false), new FunctionInfo("GetPersuasionNumber", 225, false, false),
			new FunctionInfo("GetPlayerControlsDisabled", 98, false, false),
			new FunctionInfo("GetPlayerHasLastRiddenHorse", 362, false, false), new FunctionInfo("GetPlayerInSEWorld", 365, false, false),
			new FunctionInfo("GetPos", 6, true, false), new FunctionInfo("GetQuestRunning", 56, true, false),
			new FunctionInfo("GetQuestVariable", 79, true, true), new FunctionInfo("GetRandomPercent", 77, false, false),
			new FunctionInfo("GetRestrained", 244, false, false), new FunctionInfo("GetScale", 24, false, false),
			new FunctionInfo("GetScriptVariable", 53, true, true), new FunctionInfo("GetSecondsPassed", 12, false, false),
			new FunctionInfo("GetShouldAttack", 66, true, false), new FunctionInfo("GetSitting", 159, false, false),
			new FunctionInfo("GetSleeping", 49, false, false), new FunctionInfo("GetStage", 58, true, false),
			new FunctionInfo("GetStageDone", 59, true, true), new FunctionInfo("GetStartingAngle", 11, true, false),
			new FunctionInfo("GetStartingPos", 10, true, false), new FunctionInfo("GetTalkedToPC", 50, false, false),
			new FunctionInfo("GetTalkedToPCParam", 172, true, false), new FunctionInfo("GetTimeDead", 361, false, false),
			new FunctionInfo("GetTotalPersuasionNumber", 315, false, false),
			new FunctionInfo("GetTrespassWarningLevel", 144, false, false), new FunctionInfo("GetUnconscious", 242, false, false),
			new FunctionInfo("GetUsedItemActivate", 259, false, false), new FunctionInfo("GetUsedItemLevel", 258, false, false),
			new FunctionInfo("GetVampire", 40, false, false), new FunctionInfo("GetWalkSpeed", 142, false, false),
			new FunctionInfo("GetWeaponAnimType", 108, false, false), new FunctionInfo("GetWeaponSkillType", 109, false, false),
			new FunctionInfo("GetWindSpeed", 147, false, false), new FunctionInfo("HasFlames", 154, false, false),
			new FunctionInfo("HasMagicEffect", 214, true, false), new FunctionInfo("HasVampireFed", 227, false, false),
			new FunctionInfo("IsActor", 353, false, false), new FunctionInfo("IsActorAVictim", 314, false, false),
			new FunctionInfo("IsActorEvil", 313, false, false), new FunctionInfo("IsActorUsingATorch", 306, false, false),
			new FunctionInfo("IsCellOwner", 280, true, true), new FunctionInfo("IsCloudy", 267, false, false),
			new FunctionInfo("IsContinuingPackagePCNear", 150, false, false), new FunctionInfo("IsCurrentFurnitureObj", 163, true, false),
			new FunctionInfo("IsCurrentFurnitureRef", 162, true, false), new FunctionInfo("IsEssential", 354, false, false),
			new FunctionInfo("IsFacingUp", 106, false, false), new FunctionInfo("IsGuard", 125, false, false),
			new FunctionInfo("IsHorseStolen", 282, false, false), new FunctionInfo("IsIdlePlaying", 112, false, false),
			new FunctionInfo("IsInCombat", 289, false, false), new FunctionInfo("IsInDangerousWater", 332, false, false),
			new FunctionInfo("IsInInterior", 300, false, false), new FunctionInfo("IsInMyOwnedCell", 146, false, false),
			new FunctionInfo("IsLeftUp", 285, false, false), new FunctionInfo("IsOwner", 278, true, false),
			new FunctionInfo("IsPCAMurderer", 176, false, false), new FunctionInfo("IsPCSleeping", 175, false, false),
			new FunctionInfo("IsPlayerInJail", 171, false, false), new FunctionInfo("IsPlayerMovingIntoNewSpace", 358, false, false),
			new FunctionInfo("IsPlayersLastRiddenHorse", 339, false, false), new FunctionInfo("IsPleasant", 266, false, false),
			new FunctionInfo("IsRaining", 62, false, false), new FunctionInfo("IsRidingHorse", 327, false, false),
			new FunctionInfo("IsRunning", 287, false, false), new FunctionInfo("IsShieldOut", 103, false, false),
			new FunctionInfo("IsSneaking", 286, false, false), new FunctionInfo("IsSnowing", 75, false, false),
			new FunctionInfo("IsSpellTarget", 223, true, false), new FunctionInfo("IsSwimming", 185, false, false),
			new FunctionInfo("IsTalking", 141, false, false), new FunctionInfo("IsTimePassing", 265, false, false),
			new FunctionInfo("IsTorchOut", 102, false, false), new FunctionInfo("IsTrespassing", 145, false, false),
			new FunctionInfo("IsTurnArrest", 329, false, false), new FunctionInfo("IsWaiting", 111, false, false),
			new FunctionInfo("IsWeaponOut", 101, false, false), new FunctionInfo("IsXBox", 309, false, false),
			new FunctionInfo("IsYielding", 104, false, false), new FunctionInfo("MenuMode", 36, true, false),
			new FunctionInfo("SameFaction", 42, true, false), new FunctionInfo("SameFactionAsPC", 133, false, false),
			new FunctionInfo("SameRace", 43, true, false), new FunctionInfo("SameRaceAsPC", 134, false, false),
			new FunctionInfo("SameSex", 44, true, false), new FunctionInfo("SameSexAsPC", 135, false, false),
			new FunctionInfo("WhichServiceMenu", 323, false, false) };

	public PluginSubrecord(String recordType, String subrecordType, byte[] subrecordData)
	{
		this.recordType = recordType;
		this.subrecordType = subrecordType;
		this.subrecordData = subrecordData;

		if (typeMap == null)
		{
			typeMap = new HashMap<String, SubrecordInfo>(subrecordInfo.length);
			for (SubrecordInfo info : subrecordInfo)
			{
				typeMap.put(info.getSubrecordType(), info);
			}

		}

		if (functionMap == null)
		{
			functionMap = new HashMap<Integer, FunctionInfo>(functionInfo.length);
			for (FunctionInfo info : functionInfo)
				functionMap.put(new Integer(info.getCode()), info);
		}
	}

	public void setSpillMode(boolean mode) throws IOException
	{
		if (mode != this.spillMode)
		{
			if (this.spillMode)
			{
				this.subrecordData = Main.pluginSpill.read(this.subrecordPosition, this.subrecordLength);
				this.subrecordPosition = -1L;
				this.subrecordLength = 0;
			}
			else if (this.subrecordData != null)
			{
				this.subrecordPosition = Main.pluginSpill.write(this.subrecordData);
				this.subrecordLength = this.subrecordData.length;
				this.subrecordData = null;
			}
			else
			{
				this.subrecordPosition = -1L;
				this.subrecordLength = 0;
			}

			this.spillMode = mode;
		}
	}

	public String getSubrecordType()
	{
		return this.subrecordType;
	}

	public byte[] getSubrecordData() throws IOException
	{
		if (this.spillMode)
		{
			return Main.pluginSpill.read(this.subrecordPosition, this.subrecordLength);
		}
		return this.subrecordData;
	}

	public void setSubrecordData(byte[] subrecordData) throws IOException
	{
		if (this.spillMode)
		{
			this.subrecordPosition = Main.pluginSpill.write(subrecordData);
			this.subrecordLength = subrecordData.length;
		}
		else
		{
			this.subrecordData = subrecordData;
		}
	}

	public int[][] getReferences() throws IOException
	{
		int[][] references = null;

		if (this.subrecordType.equals("CTDA"))
		{
			byte[] subrecordData = getSubrecordData();
			int functionCode = getInteger(subrecordData, 8);
			FunctionInfo functionInfo = functionMap.get(new Integer(functionCode));
			if (functionInfo != null)
			{
				references = new int[2][2];
				int index = 0;

				if ((functionInfo.isFirstReference()) && (subrecordData.length >= 16))
				{
					references[index][0] = 12;
					references[index][1] = getInteger(subrecordData, 12);
					index++;
				}

				if ((functionInfo.isSecondReference()) && (subrecordData.length >= 20))
				{
					references[index][0] = 16;
					references[index][1] = getInteger(subrecordData, 16);
				}
			}
		}

		else if ((this.subrecordType.equals("DATA")) && (this.recordType.equals("MGEF")))
		{
			byte[] subrecordData = getSubrecordData();
			int[] mgefOffsets = new int[]
			{ 24, 32, 36, 40, 44, 48, 52 };
			references = new int[mgefOffsets.length][2];
			int index = 0;
			while (true)
			{
				int refOffset = mgefOffsets[index];
				if (refOffset + 4 > subrecordData.length)
				{
					break;
				}
				references[index][0] = refOffset;
				references[index][1] = getInteger(subrecordData, refOffset);

				index++;
				if (index < mgefOffsets.length)
					continue;
				break;
			}
		}
		else if ((this.subrecordType.equals("PLDT")) && (this.recordType.equals("PACK")))
		{
			byte[] subrecordData = getSubrecordData();
			int type = getInteger(subrecordData, 0);
			if (!((type != 0) && (type != 1) && (type != 4)))
			{
				references = new int[1][2];
				references[0][0] = 4;
				references[0][1] = getInteger(subrecordData, 4);
			}
		}
		else if ((this.subrecordType.equals("PTDT")) && (this.recordType.equals("PACK")))
		{
			byte[] subrecordData = getSubrecordData();
			int type = getInteger(subrecordData, 0);
			if (!((type != 0) && (type != 1)))
			{
				references = new int[1][2];
				references[0][0] = 4;
				references[0][1] = getInteger(subrecordData, 4);
			}
		}
		else
		{
			boolean returnReferences = false;
			SubrecordInfo subrecordInfo = typeMap.get(this.subrecordType);
			if (subrecordInfo != null)
			{
				String[] recordTypes = subrecordInfo.getRecordTypes();
				if (recordTypes.length == 0)
					returnReferences = true;
				else
				{
					for (int i = 0; i < recordTypes.length; i++)
					{
						if (this.recordType.equals(recordTypes[i]))
						{
							returnReferences = true;
							break;
						}

					}

				}

			}

			if (!(!returnReferences))
			{

				byte[] subrecordData = getSubrecordData();
				int[] refOffsets = subrecordInfo.getReferenceOffsets();
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
				while (true)
				{
					if (repeating)
					{
						refOffset += refSize;
					}
					else
					{
						i++;
						if (i == refOffsets.length)
						{
							break;
						}
						refOffset = refOffsets[i];
					}

					if (refOffset + refSize > subrecordData.length)
					{
						break;
					}
					references[index][0] = refOffset;
					references[index][1] = getInteger(subrecordData, refOffset);
					index++;
				}
			}
		}

		return references;
	}

	public static FunctionInfo getFunctionInfo(int funcCode)
	{
		if (functionMap == null)
		{
			functionMap = new HashMap<Integer, FunctionInfo>(functionInfo.length);
			for (FunctionInfo info : functionInfo)
				functionMap.put(new Integer(info.getCode()), info);
		}
		return functionMap.get(new Integer(funcCode));
	}

	public String getDisplayDataTypeLabel()
	{
		int dataType = SubrecordDataType.getDataType(this.subrecordType);
		if (dataType == 1000)
		{
			dataType = SubrecordDataType.getDataType(this.subrecordType + "-" + this.recordType);
			if (dataType == -1)
			{
				dataType = SubrecordDataType.getDataType(this.subrecordType + "-" + "OTHER");
			}
		}
		String retStr = SubrecordDataType.getDataTypeLabel(dataType);
		if (dataType == 999)
		{
			retStr = this.subrecordType + " " + retStr;
		}
		return retStr;
	}

	public String getDisplayData()
	{
		byte[] subrecordData = null;
		int dataType = SubrecordDataType.getDataType(this.subrecordType);
		if (dataType == 1000)
		{
			dataType = SubrecordDataType.getDataType(this.subrecordType + "-" + this.recordType);
			if (dataType == -1)
			{
				dataType = SubrecordDataType.getDataType(this.subrecordType + "-" + "OTHER");
			}
		}
		String retStr = "";
		try
		{
			subrecordData = getSubrecordData();
		}
		catch (IOException exc)
		{
			Main.logException("Exception while getting subrecord data", exc);
			subrecordData = new byte[0];
			dataType = 0;
		}
		switch (dataType)
		{
			case 2:
				retStr = getDisplayDataFormID(subrecordData);
				break;
			case 4:
				retStr = getDisplayDataFloat(subrecordData);
				break;
			case 3:
				retStr = getDisplayDataInteger(subrecordData);
				break;
			case 5:
				retStr = getDisplayDataShort(subrecordData);
				break;
			case 6:
				retStr = getDisplayDataByte(subrecordData);
				break;
			case 1:
				retStr = getDisplayDataString(subrecordData);
				break;
			case 7:
				retStr = getDisplayDataStringNoNull(subrecordData);
				break;
			case 19:
				retStr = getDisplayDataStringArray(subrecordData);
				break;
			case 8:
				retStr = getDisplayDataXYCoordinates(subrecordData);
				break;
			case 9:
				retStr = getDisplayDataContainerItem(subrecordData);
				break;
			case 10:
				retStr = getDisplayDataCondition(subrecordData);
				break;
			case 11:
				retStr = getDisplayDataEmotion(subrecordData);
				break;
			case 16:
				retStr = getDisplayDataLeveledItem(subrecordData);
				break;
			case 12:
				retStr = getDisplayDataPositionRotation(subrecordData);
				break;
			case 13:
				retStr = getDisplayDataSpellEffectName(subrecordData);
				break;
			case 14:
				retStr = getDisplayDataSpellEffectData(subrecordData);
				break;
			case 15:
				retStr = getDisplayDataFormIDArray(subrecordData);
				break;
			case 17:
				retStr = getDisplayDataCellLightingInfo(subrecordData);
				break;
			case 18:
				retStr = getDisplayDataFlags(subrecordData);
				break;
			case 20:
				retStr = getDisplayDataActorConfig(subrecordData);
				break;
			case 21:
				retStr = getDisplayDataFactionInfo(subrecordData);
				break;
			case 22:
				retStr = getDisplayDataAIInfo(subrecordData);
				break;
			case 23:
				retStr = getDisplayDataPGNodeArray(subrecordData);
				break;
			case 24:
				retStr = getDisplayDataPGConnsInt(subrecordData);
				break;
			case 25:
				retStr = getDisplayDataPGConnsExt(subrecordData);
				break;
			case 26:
				retStr = getDisplayDataLSTexture(subrecordData);
				break;
			case 100:
				retStr = getDisplayDataDATAforINFO(subrecordData);
				break;
			case 101:
				retStr = getDisplayDataDATAforCREA(subrecordData);
				break;
			case 0:
			default:
				retStr = getDisplayDataByteArray(subrecordData);
		}
		return retStr;
	}

	public String getDisplayDataAsBytes()
	{
		byte[] subrecordData = null;
		String retStr = "";
		try
		{
			subrecordData = getSubrecordData();
		}
		catch (IOException exc)
		{
			Main.logException("Exception while getting subrecord data", exc);
			subrecordData = new byte[0];
		}
		retStr = getDisplayDataByteArray(subrecordData);
		return retStr;
	}

	private static String getDisplayDataFormID(byte[] subrecordData)
	{
		int formID = SerializedElement.getInteger(subrecordData, 0);
		return String.format("%08X", new Object[]
		{ Integer.valueOf(formID) });
	}

	private static String getDisplayDataFloat(byte[] subrecordData)
	{
		int floatBits = SerializedElement.getInteger(subrecordData, 0);
		return String.format("%.3f", new Object[]
		{ Float.valueOf(Float.intBitsToFloat(floatBits)) });
	}

	private static String getDisplayDataInteger(byte[] subrecordData)
	{
		int intBits = SerializedElement.getInteger(subrecordData, 0);
		return String.format("%d", new Object[]
		{ Integer.valueOf(intBits) });
	}

	private static String getDisplayDataShort(byte[] subrecordData)
	{
		int intBits = SerializedElement.getShort(subrecordData, 0);
		return String.format("%d", new Object[]
		{ Integer.valueOf(intBits) });
	}

	private static String getDisplayDataByte(byte[] subrecordData)
	{
		byte firstByte = subrecordData[0];
		return String.format("%d", new Object[]
		{ Byte.valueOf(firstByte) });
	}

	private static String getDisplayDataString(byte[] subrecordData)
	{
		return new String(subrecordData, 0, subrecordData.length - 1);
	}

	private static String getDisplayDataStringNoNull(byte[] subrecordData)
	{
		return new String(subrecordData, 0, subrecordData.length);
	}

	private static String getDisplayDataStringArray(byte[] subrecordData)
	{
		String firstStr = new String(subrecordData, 0, subrecordData.length - 2);
		String retstr = firstStr.replace('\000', '\n');
		return retstr;
	}

	private String getDisplayDataByteArray(byte[] subrecordData)
	{
		StringBuilder dumpData = new StringBuilder(128 + 3 * subrecordData.length + 6 * (subrecordData.length / 16));
		dumpData.append(String.format("%s subrecord: Data length x'%X'\n", new Object[]
		{ getSubrecordType(), Integer.valueOf(subrecordData.length) }));
		dumpData.append("\n       0  1  2  3  4  5  6  7  8  9  A  B  C  D  E  F\n");
		StringBuilder dumpHex = new StringBuilder(48);
		StringBuilder dumpLine = new StringBuilder(16);

		for (int i = 0; i < subrecordData.length; i += 16)
		{
			for (int j = 0; j < 16; j++)
			{
				int offset = i + j;
				if (offset == subrecordData.length)
				{
					break;
				}
				dumpHex.append(String.format(" %02X", new Object[]
				{ Byte.valueOf(subrecordData[offset]) }));
				if ((subrecordData[offset] >= 32) && (subrecordData[offset] < 127))
					dumpLine.append(new String(subrecordData, offset, 1));
				else
				{
					dumpLine.append(".");
				}
			}
			while (dumpHex.length() < 48)
			{
				dumpHex.append("   ");
			}
			while (dumpLine.length() < 16)
			{
				dumpLine.append(" ");
			}
			dumpData.append(String.format("%04X:", new Object[]
			{ Integer.valueOf(i) }));
			dumpData.append(dumpHex);
			dumpData.append("  *");
			dumpData.append(dumpLine);
			dumpData.append("*");
			if (i + 16 < subrecordData.length)
			{
				dumpData.append("\n");
			}
			dumpHex.delete(0, 48);
			dumpLine.delete(0, 16);
		}
		return dumpData.toString();
	}

	private static String getDisplayDataXYCoordinates(byte[] subrecordData)
	{
		int x = SerializedElement.getInteger(subrecordData, 0);
		int y = SerializedElement.getInteger(subrecordData, 4);
		String retStr = x + ", " + y;
		return retStr;
	}

	private static String getDisplayDataContainerItem(byte[] subrecordData)
	{
		int itemFormID = SerializedElement.getInteger(subrecordData, 0);
		int itemCount = SerializedElement.getInteger(subrecordData, 4);
		return String.format("Item form ID: %08X\nItem count: %d", new Object[]
		{ Integer.valueOf(itemFormID), Integer.valueOf(itemCount) });
	}

	private static String getDisplayDataLeveledItem(byte[] subrecordData)
	{
		int formIDPos = subrecordData.length == 12 ? 4 : 2;
		int countPos = subrecordData.length == 12 ? 8 : 6;
		int itemLevel = SerializedElement.getShort(subrecordData, 0);
		int itemFormID = SerializedElement.getInteger(subrecordData, formIDPos);
		int itemCount = SerializedElement.getShort(subrecordData, countPos);
		return String.format("Item level: %d\nItem form ID: %08X\nItem count: %d", new Object[]
		{ Integer.valueOf(itemLevel), Integer.valueOf(itemFormID), Integer.valueOf(itemCount) });
	}

	private static String getDisplayDataCondition(byte[] subrecordData)
	{
		int subFuncCode = SerializedElement.getInteger(subrecordData, 8);
		FunctionInfo funcInfo = getFunctionInfo(subFuncCode);
		boolean usesFirst = false;
		boolean usesSecond = false;
		if (funcInfo != null)
		{
			usesFirst = funcInfo.isFirstReference();
			usesSecond = funcInfo.isSecondReference();
		}

		int subCompFlags = subrecordData[0] & 0xF;
		int subCompCode = (subrecordData[0] & 0xF0) >>> 4;
		int subCompValueInt = SerializedElement.getInteger(subrecordData, 4);
		float subCompValue = Float.intBitsToFloat(subCompValueInt);
		int param1 = 0;
		int param2 = 0;
		if (usesFirst)
			param1 = SerializedElement.getInteger(subrecordData, 12);
		if (usesSecond)
			param1 = SerializedElement.getInteger(subrecordData, 16);
		String paramList = "()";
		if (usesFirst)
			paramList = String.format("(%08X)", new Object[]
			{ Integer.valueOf(param1) });
		if (usesSecond)
			paramList = String.format("(%08X, %08X)", new Object[]
			{ Integer.valueOf(param1), Integer.valueOf(param2) });
		String retStr = FunctionCode.getFuncCodeName(subFuncCode) + paramList + " " + ComparisonCode.getCompCodeSymbol(subCompCode) + " "
				+ subCompValue;
		if ((subCompFlags & 0x1) != 0)
			retStr = retStr + "\n - Is ORed to next condition";
		if ((subCompFlags & 0x2) != 0)
			retStr = retStr + "\n - Executes on target";
		if ((subCompFlags & 0x4) != 0)
			retStr = retStr + "\n - Uses global variables";
		return retStr;
	}

	private static String getDisplayDataEmotion(byte[] subrecordData)
	{
		int emotionCode = SerializedElement.getInteger(subrecordData, 0);
		int emotionValue = SerializedElement.getInteger(subrecordData, 4);
		int responseNum = subrecordData[12];
		String retStr = "Type: " + EmotionCode.getString(emotionCode) + "\nValue: " + emotionValue + "\nResponse number: " + responseNum;
		return retStr;
	}

	private static String getDisplayDataPositionRotation(byte[] subrecordData)
	{
		int XPosBits = SerializedElement.getInteger(subrecordData, 0);
		int YPosBits = SerializedElement.getInteger(subrecordData, 4);
		int ZPosBits = SerializedElement.getInteger(subrecordData, 8);
		int XRotBits = SerializedElement.getInteger(subrecordData, 12);
		int YRotBits = SerializedElement.getInteger(subrecordData, 16);
		int ZRotBits = SerializedElement.getInteger(subrecordData, 20);
		float XPos = Float.intBitsToFloat(XPosBits);
		float YPos = Float.intBitsToFloat(YPosBits);
		float ZPos = Float.intBitsToFloat(ZPosBits);
		float XRot = Float.intBitsToFloat(XRotBits) * 180.0F / 3.141593F;
		float YRot = Float.intBitsToFloat(YRotBits) * 180.0F / 3.141593F;
		float ZRot = Float.intBitsToFloat(ZRotBits) * 180.0F / 3.141593F;
		String retStr = "Position: (" + XPos + ", " + YPos + ", " + ZPos + ")\n" + "Rotation: (" + XRot + "°, " + YRot + "°, " + ZRot
				+ "°)";
		return retStr;
	}

	private static String getDisplayDataCellLightingInfo(byte[] subrecordData)
	{
		int ambRed = subrecordData[0];
		int ambGreen = subrecordData[1];
		int ambBlue = subrecordData[2];
		int dirRed = subrecordData[4];
		int dirGreen = subrecordData[5];
		int dirBlue = subrecordData[6];
		int fogRed = subrecordData[8];
		int fogGreen = subrecordData[9];
		int fogBlue = subrecordData[10];
		int fogNearBits = SerializedElement.getInteger(subrecordData, 12);
		int fogFarBits = SerializedElement.getInteger(subrecordData, 16);
		float fogNear = Float.intBitsToFloat(fogNearBits);
		float fogFar = Float.intBitsToFloat(fogFarBits);
		int XYRot = SerializedElement.getInteger(subrecordData, 20);
		int ZRot = SerializedElement.getInteger(subrecordData, 24);
		int dirFadeBits = SerializedElement.getInteger(subrecordData, 28);
		int fogClipBits = SerializedElement.getInteger(subrecordData, 32);
		float dirFade = Float.intBitsToFloat(dirFadeBits);
		float fogClip = Float.intBitsToFloat(fogClipBits);
		String retStr = "Ambient RGB: (" + ambRed + ", " + ambGreen + ", " + ambBlue + ")\n" + "Directional RGB: (" + dirRed + ", "
				+ dirGreen + ", " + dirBlue + ")\n" + "Fog RGB: (" + fogRed + ", " + fogGreen + ", " + fogBlue + ")\n" + "Fog Near: "
				+ fogNear + ", Fog Far: " + fogFar + "\n" + "XY Rotation: " + XYRot + "°, Z Rotation: " + ZRot + "°\n"
				+ "Directional Fade: " + dirFade + ", Fog Clip: " + fogClip + "\n";

		return retStr;
	}

	private static String getDisplayDataSpellEffectName(byte[] subrecordData)
	{
		return SpellEffectType.getSpellEffectName(new String(subrecordData, 0, subrecordData.length));
	}

	private static String getDisplayDataSpellEffectData(byte[] subrecordData)
	{
		String effectName = SpellEffectType.getSpellEffectName(new String(subrecordData, 0, 4));
		int effectMagnitude = SerializedElement.getInteger(subrecordData, 4);
		int effectArea = SerializedElement.getInteger(subrecordData, 8);
		int effectDuration = SerializedElement.getInteger(subrecordData, 12);
		int effectType = SerializedElement.getInteger(subrecordData, 16);
		int effectActorValue = SerializedElement.getInteger(subrecordData, 4);
		return "Name: " + effectName + "\nMagnitude: " + effectMagnitude + "\nArea: " + effectArea + "\nDuration: " + effectDuration
				+ "\nType: " + effectType + "\nActor Value: " + effectActorValue;
	}

	private static String getDisplayDataDATAforINFO(byte[] subrecordData)
	{
		int dialogueType = subrecordData[0];
		int dialogueFlags = subrecordData[2];
		String retStr = "Type: " + DialogueTypeCode.getString(dialogueType);
		if ((dialogueFlags & 0x1) != 0)
			retStr = retStr + "\n - Goodbye";
		if ((dialogueFlags & 0x2) != 0)
			retStr = retStr + "\n - Random";
		if ((dialogueFlags & 0x4) != 0)
			retStr = retStr + "\n - Say Once";
		if ((dialogueFlags & 0x10) != 0)
			retStr = retStr + "\n - Info Refusal";
		if ((dialogueFlags & 0x20) != 0)
			retStr = retStr + "\n - Random End";
		if ((dialogueFlags & 0x40) != 0)
			retStr = retStr + "\n - Run for Rumors";
		return retStr;
	}

	private static String getDisplayDataDATAforCREA(byte[] subrecordData)
	{
		String[] soulgemTypes =
		{ "None", "Petty", "Lesser", "Common", "Greater", "Grand" };
		int combatSkill = subrecordData[1];
		int magicSkill = subrecordData[2];
		int stealthSkill = subrecordData[3];
		int soulgemIdx = subrecordData[4];
		int strength = subrecordData[12];
		int intelligence = subrecordData[13];
		int willpower = subrecordData[14];
		int agility = subrecordData[15];
		int speed = subrecordData[16];
		int endurance = subrecordData[17];
		int personality = subrecordData[18];
		int luck = subrecordData[19];
		int healthPoints = SerializedElement.getShort(subrecordData, 6);
		int attackDamage = SerializedElement.getShort(subrecordData, 10);
		return "Combat Skill: " + combatSkill + "\nMagic Skill: " + magicSkill + "\nStealth Skill: " + stealthSkill + "\nSoulgem Type: "
				+ getIndexedString(soulgemIdx, soulgemTypes) + "\nHealth Points: " + healthPoints + "\nAttack Damage: " + attackDamage
				+ "\nStrength: " + strength + "\nIntelligence: " + intelligence + "\nWillpower: " + willpower + "\nAgility: " + agility
				+ "\nSpeed: " + speed + "\nEndurance: " + endurance + "\nPersonality: " + personality + "\nLuck: " + luck;
	}

	private static String getDisplayDataFormIDArray(byte[] subrecordData)
	{
		String retStr = "";
		int numFormIDs = subrecordData.length / 4;
		for (int i = 0; i < numFormIDs; i++)
		{
			int formID = SerializedElement.getInteger(subrecordData, i * 4);
			if (i == 0)
				retStr = retStr + String.format("%08X", new Object[]
				{ Integer.valueOf(formID) });
			else if (i % 5 == 0)
				retStr = retStr + ",\n" + String.format("%08X", new Object[]
				{ Integer.valueOf(formID) });
			else
				retStr = retStr + ", " + String.format("%08X", new Object[]
				{ Integer.valueOf(formID) });
		}
		return retStr;
	}

	private static String getDisplayDataFlags(byte[] subrecordData)
	{
		String retStr = "";
		for (int i = subrecordData.length - 1; i >= 0; i--)
		{
			byte flagByte = subrecordData[i];

			retStr = retStr + ((flagByte & 0x80) == 0 ? "0" : "1");
			retStr = retStr + ((flagByte & 0x40) == 0 ? "0" : "1");
			retStr = retStr + ((flagByte & 0x20) == 0 ? "0" : "1");
			retStr = retStr + ((flagByte & 0x10) == 0 ? "0" : "1");
			retStr = retStr + ((flagByte & 0x8) == 0 ? "0" : "1");
			retStr = retStr + ((flagByte & 0x4) == 0 ? "0" : "1");
			retStr = retStr + ((flagByte & 0x2) == 0 ? "0" : "1");
			retStr = retStr + ((flagByte & 0x1) == 0 ? "0" : "1");
		}
		return retStr;
	}

	private static String getDisplayDataActorConfig(byte[] subrecordData)
	{
		byte[] flagPart = new byte[3];
		System.arraycopy(subrecordData, 0, flagPart, 0, 3);
		String flagBits = getDisplayDataFlags(flagPart);
		int baseSpell = SerializedElement.getShort(subrecordData, 4);
		int fatigue = SerializedElement.getShort(subrecordData, 6);
		int barterGold = SerializedElement.getShort(subrecordData, 8);
		int level = SerializedElement.getShort(subrecordData, 10);
		int calcMin = SerializedElement.getShort(subrecordData, 12);
		int calcMax = SerializedElement.getShort(subrecordData, 14);
		String retStr = "Flags: " + flagBits + "\nBase Spell Points: " + baseSpell + "\nFatigue: " + fatigue + "\nBarter Gold: "
				+ barterGold + "\nLevel/Offset Level: " + level + "\nCalc Min: " + calcMin + "\nCalc Max: " + calcMax;
		return retStr;
	}

	private static String getDisplayDataAIInfo(byte[] subrecordData)
	{
		byte[] flagPart = new byte[3];
		String[] skillNames =
		{ "Armorer", "Athletics", "Blade", "Block", "Blunt", "Hand to Hand", "Heavy Armor", "Alchemy", "Alteration", "Conjuration",
				"Destruction", "Illusion", "Mysticism", "Restoration", "Acrobatics", "Light Armor", "Marskman", "Mercantile", "Security",
				"Sneak", "Speechcraft" };
		System.arraycopy(subrecordData, 4, flagPart, 0, 3);
		String flagBits = getDisplayDataFlags(flagPart);
		int allFlags = SerializedElement.getInteger(subrecordData, 4);
		int aggression = subrecordData[0];
		int confidence = subrecordData[1];
		int energyLevel = subrecordData[2];
		int responsibility = subrecordData[3];
		int trainingSkill = subrecordData[8];
		int trainingLevel = subrecordData[9];
		String retStr = "Aggression: " + aggression + "\nConfidence: " + confidence + "\nEnergy Level: " + energyLevel
				+ "\nResponsibility: " + responsibility + "\nFlags: " + flagBits;
		if ((allFlags & 0x4000) != 0)
		{
			retStr = retStr + "\nTraining Skill: " + getIndexedString(trainingSkill, skillNames) + "\nTraining Level: " + trainingLevel;
		}

		return retStr;
	}

	private static String getDisplayDataFactionInfo(byte[] subrecordData)
	{
		int factionID = SerializedElement.getInteger(subrecordData, 0);
		int factionRank = subrecordData[4];
		return String.format("Faction form ID: %08X\nFaction rank: %d", new Object[]
		{ Integer.valueOf(factionID), Integer.valueOf(factionRank) });
	}

	private static String getDisplayDataPGNodeArray(byte[] subrecordData)
	{
		String retStr = "";
		int numPGNodes = subrecordData.length / 16;
		for (int i = 0; i < numPGNodes; i++)
		{
			int XPosBits = SerializedElement.getInteger(subrecordData, i * 16);
			int YPosBits = SerializedElement.getInteger(subrecordData, i * 16 + 4);
			int ZPosBits = SerializedElement.getInteger(subrecordData, i * 16 + 8);
			float XPos = Float.intBitsToFloat(XPosBits);
			float YPos = Float.intBitsToFloat(YPosBits);
			float ZPos = Float.intBitsToFloat(ZPosBits);
			String nodeIdx = String.format("%02d", new Object[]
			{ Integer.valueOf(i) });
			String flags = String.format("%d", new Object[]
			{ Byte.valueOf(subrecordData[(i * 16 + 12)]) });
			retStr = retStr + "Node " + nodeIdx + " XYZ: (" + XPos + ", " + YPos + ", " + ZPos + "), Connections:  " + flags + "\n";
		}
		return retStr;
	}

	private static String getDisplayDataPGConnsInt(byte[] subrecordData)
	{
		String retStr = "";
		int numPGConns = subrecordData.length / 4;
		for (int i = 0; i < numPGConns; i++)
		{
			int node1 = SerializedElement.getShort(subrecordData, i * 4);
			int node2 = SerializedElement.getShort(subrecordData, i * 4 + 2);
			String node1Idx = String.format("%02d", new Object[]
			{ Integer.valueOf(node1) });
			String node2Idx = String.format("%02d", new Object[]
			{ Integer.valueOf(node2) });
			String connIdx = String.format("%02d", new Object[]
			{ Integer.valueOf(i) });
			retStr = retStr + "PG Connection " + connIdx + ": [" + node1Idx + " to " + node2Idx + "]\n";
		}
		return retStr;
	}

	private static String getDisplayDataPGConnsExt(byte[] subrecordData)
	{
		String retStr = "";
		int numPGConnsExt = subrecordData.length / 16;
		for (int i = 0; i < numPGConnsExt; i++)
		{
			int intNode = subrecordData[(i * 16)];
			int XPosBits = SerializedElement.getInteger(subrecordData, i * 16 + 4);
			int YPosBits = SerializedElement.getInteger(subrecordData, i * 16 + 8);
			int ZPosBits = SerializedElement.getInteger(subrecordData, i * 16 + 12);
			float XPos = Float.intBitsToFloat(XPosBits);
			float YPos = Float.intBitsToFloat(YPosBits);
			float ZPos = Float.intBitsToFloat(ZPosBits);
			String connIdx = String.format("%02d", new Object[]
			{ Integer.valueOf(i) });
			String nodeIdx = String.format("%02d", new Object[]
			{ Integer.valueOf(intNode) });
			String flags = String.format("%d", new Object[]
			{ Byte.valueOf(subrecordData[(i * 16 + 12)]) });
			retStr = retStr + "PG External Connection " + connIdx + ": Node " + nodeIdx + " to (" + XPos + ", " + YPos + ", " + ZPos
					+ ")\n";
		}
		return retStr;
	}

	private static String getDisplayDataLSTexture(byte[] subrecordData)
	{
		String retStr = "";
		int LSTexID = SerializedElement.getInteger(subrecordData, 0);
		String quad = subrecordData[4] > 1 ? "Top " : "Bottom ";
		quad = quad + (subrecordData[4] % 2 == 0 ? "Left" : "Right");
		int layer = SerializedElement.getShort(subrecordData, 6);
		String layerString = layer == 65535 ? "Base" : String.format("%d", new Object[]
		{ Integer.valueOf(layer) });
		retStr = retStr + "Landscape texture: " + String.format("%08X", new Object[]
		{ Integer.valueOf(LSTexID) }) + "\nQuadrant: " + quad + "\nLayer: " + layerString;
		return retStr;
	}

	private static String getIndexedString(int idx, String[] strArray)
	{
		if (idx < 0)
			return "Below lower bound";
		if (idx >= strArray.length)
			return "Above upper bound";
		return strArray[idx];
	}

	public boolean equals(Object object)
	{
		boolean areEqual = false;
		if ((object instanceof PluginSubrecord))
		{
			PluginSubrecord objSubrecord = (PluginSubrecord) object;
			if (objSubrecord.getSubrecordType().equals(this.subrecordType))
			{
				try
				{
					byte[] subrecordData = getSubrecordData();
					byte[] objSubrecordData = objSubrecord.getSubrecordData();
					if (objSubrecordData.length == subrecordData.length)
					{
						if (((this.subrecordType.equals("ATXT")) || (this.subrecordType.equals("BTXT"))) && (subrecordData.length == 8))
						{
							if ((subrecordData[0] == objSubrecordData[0]) && (subrecordData[1] == objSubrecordData[1])
									&& (subrecordData[2] == objSubrecordData[2]) && (subrecordData[3] == objSubrecordData[3])
									&& (subrecordData[4] == objSubrecordData[4]) && (subrecordData[6] == objSubrecordData[6])
									&& (subrecordData[7] == objSubrecordData[7]))
								areEqual = true;
						}
						else if ((this.subrecordType.equals("EFIT")) && (subrecordData.length == 24))
						{
							String effectName = new String(subrecordData, 0, 4);
							int count = 20;
							if ((effectName.equals("DGAT")) || (effectName.equals("DRAT")) || (effectName.equals("DRSK"))
									|| (effectName.equals("FOAT")) || (effectName.equals("FOSK")) || (effectName.equals("REAT"))
									|| (effectName.equals("ABAT")) || (effectName.equals("ABSK")))
							{
								count = 24;
							}
							areEqual = true;
							int i = 0;
							while (true)
								if (subrecordData[i] != objSubrecordData[i])
								{
									areEqual = false;
									i++;
									if (i < count)
										continue;
									break;
								}
						}
						else if ((this.subrecordType.equals("LVLO")) && (subrecordData.length == 12))
						{
							if ((subrecordData[0] != objSubrecordData[0]) || (subrecordData[1] != objSubrecordData[1])
									|| (subrecordData[4] != objSubrecordData[4]) || (subrecordData[5] != objSubrecordData[5])
									|| (subrecordData[6] != objSubrecordData[6]) || (subrecordData[7] != objSubrecordData[7])
									|| (subrecordData[8] != objSubrecordData[8]) || (subrecordData[9] != objSubrecordData[9]))
								areEqual = false;
							else
								areEqual = true;
						}
						else if ((this.subrecordType.equals("PGRP")) && (subrecordData.length % 16 == 0))
						{
							areEqual = true;
							int i = 0;
							while (true)
							{
								for (int j = 0; j < 13; j++)
								{
									if (subrecordData[(i + j)] != objSubrecordData[(i + j)])
									{
										areEqual = false;
										break;
									}
								}
								if (!areEqual)
									break;
								i += 16;
								if (i < subrecordData.length)
									continue;
								break;

							}

						}
						else if ((this.subrecordType.equals("PKDT")) && (subrecordData.length == 8))
						{
							if ((subrecordData[0] != objSubrecordData[0]) || (subrecordData[1] != objSubrecordData[1])
									|| (subrecordData[2] != objSubrecordData[2]) || (subrecordData[3] != objSubrecordData[3])
									|| (subrecordData[4] != objSubrecordData[4]))
								areEqual = false;
							else
								areEqual = true;
						}
						else if ((this.subrecordType.equals("QSTA")) && (subrecordData.length == 8))
						{
							if ((subrecordData[0] != objSubrecordData[0]) || (subrecordData[1] != objSubrecordData[1])
									|| (subrecordData[2] != objSubrecordData[2]) || (subrecordData[3] != objSubrecordData[3])
									|| (subrecordData[4] != objSubrecordData[4]) || (subrecordData[5] != objSubrecordData[5]))
								areEqual = false;
							else
								areEqual = true;
						}
						else

						if ((this.subrecordType.equals("XCLR")) && (subrecordData.length % 4 == 0))
						{
							int i = 0;
							while (true)
							{
								areEqual = false;
								int formID = getInteger(subrecordData, i);
								for (int j = 0; j < subrecordData.length; j += 4)
								{
									int objFormID = getInteger(objSubrecordData, j);
									if (objFormID == formID)
									{
										areEqual = true;
										break;
									}
								}

								if (!areEqual)
									break;
								i += 4;
								if (i < subrecordData.length)
									continue;
								break;
							}
						}
						else if ((this.subrecordType.equals("XLOC")) && (subrecordData.length == 12))
						{
							if ((subrecordData[0] != objSubrecordData[0]) || (subrecordData[1] != objSubrecordData[1])
									|| (subrecordData[2] != objSubrecordData[2]) || (subrecordData[3] != objSubrecordData[3])
									|| (subrecordData[4] != objSubrecordData[4]) || (subrecordData[5] != objSubrecordData[5])
									|| (subrecordData[6] != objSubrecordData[6]) || (subrecordData[7] != objSubrecordData[7])
									|| (subrecordData[8] != objSubrecordData[8]))
								areEqual = false;
							else
								areEqual = true;
						}
						else
						{
							areEqual = true;
							for (int i = 0; i < subrecordData.length; i++)
							{
								if (subrecordData[i] != objSubrecordData[i])
								{
									areEqual = false;
									break;
								}
							}
						}
					}
				}
				catch (IOException exc)
				{
					areEqual = false;
				}
			}
		}

		return areEqual;
	}

	public String toString()
	{
		return this.subrecordType + " subrecord";
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.PluginSubrecord
 * JD-Core Version:    0.6.0
 */