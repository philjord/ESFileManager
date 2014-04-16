package esmLoader.common.data.plugin;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;

import tools.io.ESMByteConvert;
import esmLoader.common.PluginException;

public class PluginGroup extends PluginRecord
{

	public static final int TOP = 0;

	public static final int WORLDSPACE = 1;

	public static final int INTERIOR_BLOCK = 2;

	public static final int INTERIOR_SUBBLOCK = 3;

	public static final int EXTERIOR_BLOCK = 4;

	public static final int EXTERIOR_SUBBLOCK = 5;

	public static final int CELL = 6;

	public static final int TOPIC = 7;

	public static final int CELL_PERSISTENT = 8;

	public static final int CELL_TEMPORARY = 9;

	public static final int CELL_DISTANT = 10;

	protected byte groupLabel[];

	protected String groupRecordType;

	protected int groupParentID;

	protected int groupType;

	public static Map<String, String> typeMap;

	private List<PluginRecord> recordList = new ArrayList<PluginRecord>();

	public PluginGroup(byte prefix[])
	{
		super("GRUP", prefix);
		groupLabel = new byte[4];
		System.arraycopy(prefix, 8, groupLabel, 0, 4);
		groupType = prefix[12] & 0xff;
		switch (groupType)
		{
			case TOP: // '\0'
				if (groupLabel[0] >= 32)
					groupRecordType = new String(groupLabel);
				else
					groupRecordType = new String();
				break;

			case WORLDSPACE: // '\001'
			case CELL: // '\006'
			case TOPIC: // '\007'
			case CELL_PERSISTENT: // '\b'
			case CELL_TEMPORARY: // '\t'
			case CELL_DISTANT: // '\n'
				groupParentID = ESMByteConvert.extractInt(groupLabel, 0);
				break;
		}

	}

	public List<PluginRecord> getRecordList()
	{
		return recordList;
	}

	public int getRecordCount()
	{
		int recordCount = 0;

		for (Iterator<PluginRecord> i$ = recordList.iterator(); i$.hasNext();)
		{
			PluginRecord record = i$.next();
			recordCount++;
			if (record instanceof PluginGroup)
				recordCount += ((PluginGroup) record).getRecordCount();
		}

		return recordCount;
	}

	public void load(String fileName, RandomAccessFile in, int groupLength) throws IOException, DataFormatException, PluginException
	{
		int dataLength = groupLength;
		byte prefix[] = new byte[headerByteCount];

		while (dataLength >= headerByteCount)
		{
			int count = in.read(prefix);
			if (count != headerByteCount)
				throw new PluginException(fileName + ": Record prefix is incomplete");
			dataLength -= headerByteCount;
			String type = new String(prefix, 0, 4);
			int length = ESMByteConvert.extractInt(prefix, 4);
			PluginRecord record;
			if (type.equals("GRUP"))
			{
				length -= headerByteCount;
				record = new PluginGroup(prefix);
			}
			else
			{
				record = new PluginRecord(prefix);
			}

			try
			{
				record.load(fileName, in, length);
			}
			catch (Exception e)
			{
				System.out.println("Bad PluginGroup.load of reocrd " + type + " : " + e + " " + e.getStackTrace()[0]);
			}
			recordList.add(record);

			// remove this iteration from the total
			dataLength -= length;
		}

		if (dataLength != 0)
		{
			if (groupType == 0)
				throw new PluginException(fileName + ": Group " + groupRecordType + " is incomplete");
			else
				throw new PluginException(fileName + ": Subgroup type " + groupType + " is incomplete");
		}

		return;
	}

	public int getGroupType()
	{
		return groupType;
	}

	public byte[] getGroupLabel()
	{
		return groupLabel;
	}

	public String getGroupRecordType()
	{
		return groupRecordType;
	}

	public int getGroupParentID()
	{
		return groupParentID;
	}

	public String toString()
	{
		int intValue = ESMByteConvert.extractInt(groupLabel, 0);
		String text;

		switch (groupType)
		{
			case TOP: // '\0'
			{
				String type = new String(groupLabel);
				String description = typeMap.get(type);
				if (description != null)
					text = "Group: " + type + " : " + description;
				else
					text = "Group: Type " + new String(groupLabel);
				break;
			}

			case WORLDSPACE: // '\001'
			{
				text = "Group: Worldspace (" + intValue + ") children";
				break;
			}

			case INTERIOR_BLOCK: // '\002'
			{
				text = "Group: Interior cell block " + intValue;
				break;
			}

			case INTERIOR_SUBBLOCK: // '\003'
			{
				text = "Group: Interior cell subblock " + intValue;
				break;
			}

			case EXTERIOR_BLOCK: // '\004'
			{
				int x = intValue >>> 16;
				if ((x & 0x8000) != 0)
					x |= 0xffff0000;
				int y = intValue & 0xffff;
				if ((y & 0x8000) != 0)
					y |= 0xffff0000;

				text = "Group: Exterior cell block " + x + "," + y;
				break;
			}

			case EXTERIOR_SUBBLOCK: // '\005'
			{
				int x = intValue >>> 16;
				if ((x & 0x8000) != 0)
					x |= 0xffff0000;
				int y = intValue & 0xffff;
				if ((y & 0x8000) != 0)
					y |= 0xffff0000;

				text = "Group: Exterior cell subblock " + x + "," + y;
				break;
			}

			case CELL: // '\006'
			{

				text = "Group: Cell (" + intValue + ") children";
				break;
			}

			case TOPIC: // '\007'
			{
				text = "Group: Topic (" + intValue + ") children";
				break;
			}

			case CELL_PERSISTENT: // '\b'
			{
				text = "Group: Cell (" + intValue + ") persistent children";
				break;
			}

			case CELL_TEMPORARY: // '\t'
			{
				text = "Group: Cell (" + intValue + ") temporary children";
				break;
			}

			case CELL_DISTANT: // '\n'
			{
				text = "Group: Cell (" + intValue + ") visible distant children";
				break;
			}

			default:
			{
				text = "Group: Type " + groupType + ", Parent " + intValue;
				break;
			}
		}
		return text;
	}

	private static String groupDescriptions[][] =
	{
	{ "GRUP", "Form Group" },//
			{ "REFR", "Object Reference" },
			{ "ACHR", "Actor Reference" },
			{ "ACTI", "Activators" },
			{ "ALCH", "Potions" },
			{ "AMMO", "Ammunition" },
			{ "ANIO", "Animated Object" },
			{ "APPA", "Apparatus" }, //Not in FO3 
			{ "ARMO", "Armor" },
			{ "BOOK", "Books" },
			{ "BSGN", "Birthsigns" }, //Not in FO3 , Not in Skyrim
			{ "CELL", "Cells" },
			{ "CLAS", "Classes" },
			{ "CLOT", "Clothing" }, //Not in FO3 , Not in Skyrim
			{ "CLMT", "Climate" },
			{ "CONT", "Containers" },
			{ "CREA", "Creatures" }, //Not in Skyrim 
			{ "CSTY", "Combat Styles" },
			{ "DIAL", "Dialog" },
			{ "DOOR", "Doors" },
			{ "EFSH", "Effect Shaders" },
			{ "ENCH", "Enchantments" },
			{ "EYES", "Eyes" },
			{ "FACT", "Factions" },
			{ "FLOR", "Flora" }, //Not in FO3 
			{ "FURN", "Furniture" },
			{ "GLOB", "Global Variables" },
			{ "GMST", "Game Settings" },
			{ "GRAS", "Grass" },
			{ "HAIR", "Hair" }, // Not in Skyrim
			{ "IDLE", "Idle Animations" },
			{ "INGR", "Ingredients" },
			{ "KEYM", "Keys" },
			{ "LIGH", "Lights" },
			{ "LSCR", "Load Screens" },
			{ "LTEX", "Land Textures" },
			{ "LVLC", "Leveled Creatures" }, // Not in Skyrim
			{ "LVLI", "Leveled Items" },
			{ "LVSP", "Leveled Spells" }, //Not in FO3 
			{ "MGEF", "Magic Effects" },
			{ "MISC", "Miscellaneous Items" },
			{ "NPC_", "NPCs" },
			{ "PACK", "Packages" },
			{ "QUST", "Quests" },
			{ "RACE", "Races" },
			{ "REGN", "Regions" },
			{ "SBSP", "Subspaces" }, //Not in FO3, Not in Skyrim
			{ "SCPT", "Scripts" }, // Not in Skyrim
			{ "SGST", "Sigil Stones" }, //Not in FO3, Not in SKyrim 
			{ "SKIL", "Skills" }, //Not in FO3, Not in SKyrim   
			{ "SLGM", "Soul Gems" }, //Not in FO3 
			{ "SOUN", "Sounds" },
			{ "SPEL", "Spells" },
			{ "STAT", "Statics" },
			{ "TREE", "Trees" },
			{ "WATR", "Water" },
			{ "WEAP", "Weapons" },
			{ "WTHR", "Weather" },
			{ "WRLD", "World Spaces" },

			//new in FO3 
			{ "ADDN", "Addon Node" },
			{ "ARMA", "Armor Addon" },
			{ "ASPC", "Acoustic Space" },
			{ "AVIF", "Actor Values/Perk Tree Graphics" },
			{ "BPTD", "Body Part Data" },
			{ "CAMS", "Cameras" },
			{ "COBJ", "Constructible Object (recipes)" },
			{ "CPTH", "Camera Path" },
			{ "DEBR", "Debris" },
			{ "DOBJ", "Default Object Manager" },
			{ "ECZN", "Encounter Zone" },
			{ "EXPL", "Explosion" },
			{ "FLST", "Form List (non-leveled list)" },
			{ "HDPT", "Head Part" },
			{ "IDLM", "Idle Marker" },
			{ "IMAD", "Image Space Modifier" },
			{ "IMGS", "Image Space" },
			{ "IPCT", "Impact" },
			{ "IPDS", "Impact Data Set" },
			{ "LVLN", "LeveledCharacter" },
			{ "LGTM", "Lighting Template" },
			{ "MESG", "Message" },
			{ "MICN", "Menu Icon" }, // Not in Skyrim
			{ "MSTT", "Movable Static" },
			{ "MUSC", "Music" },
			{ "NAVI", "Navigation" },
			{ "NOTE", "Notes" }, // Not in Skyrim
			{ "PERK", "Perk" },
			{ "PROJ", "Projectile" },
			{ "PWAT", "Placeable Water" }, // Not in Skyrim
			{ "RADS", "Unknown RADS ?" },// Not in Skyrim
			{ "RGDL", "Ragdoll" },// Not in Skyrim
			{ "SCOL", "Static Collection" },// Not in Skyrim
			{ "TACT", "Talking Activator" },
			{ "TERM", "Terminal" },// Not in Skyrim
			{ "TXST", "Texture Set" },
			{ "VTYP", "Voice Type" },

			// new in Skyrim
			{ "AACT", "Action" },
			{ "ARTO", "Art Object" },
			{ "ASTP", "Association Type" },
			{ "COLL", "Collision Layer" },
			{ "CLFM", "Color" },
			{ "DLVW", "Dialog View" },
			{ "DLBR", "Dialog Branch" },
			{ "DUAL", "Dual Cast Data" },// (possibly unused)
			{ "EQUP", "Equip Slot" },// (flag-type values)
			{ "FSTP", "Footstep" },
			{ "FSTS", "Footstep Set" },
			{ "HAZD", "Hazard" },
			{ "KYWD", "Keyword" },
			{ "LCRT", "Location Reference Type" },
			{ "LCTN", "Location" },
			{ "MATT", "Material Type" },
			{ "MUST", "Music Track" },
			{ "MATO", "Material Object" },
			{ "MOVT", "Movement Type" },
			{ "OTFT", "Outfit" },
			{ "RELA", "Relationship" },
			{ "RFCT", "Visual Effect" },
			{ "REVB", "Reverb Parameters" },
			{ "SPGD", "Shader Particle Geometry" },
			{ "SCRL", "Scroll" },
			{ "SMBN", "Story Manager Branch Node" },
			{ "SMQN", "Story Manager Quest Node" },
			{ "SMEN", "Story Manager Event Node" },
			{ "SHOU", "Shout" },
			{ "SCEN", "Scene" },
			{ "SNCT", "Sound Category" },
			{ "SOPM", "Sound Output Marker" },
			{ "SNDR", "Sound Reference" },
			{ "WOOP", "Word Of Power" },

	};

	static
	{
		if (typeMap == null)
		{
			typeMap = new HashMap<String, String>(groupDescriptions.length);
			for (int i = 0; i < groupDescriptions.length; i++)
			{
				typeMap.put(groupDescriptions[i][0], groupDescriptions[i][1]);
			}
		}
	}

}