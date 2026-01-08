package esfilemanager.common.data.plugin;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.DataFormatException;

import esfilemanager.common.PluginException;
import esfilemanager.common.data.record.Record;
import tools.io.ESMByteConvert;
import tools.io.FileChannelRAF;

/** https://en.m.uesp.net/wiki/Skyrim_Mod:Mod_File_Format#Groups
 * 
 * need to make these load without external reference to length
 */
public class PluginGroup extends PluginRecord {

	public static final int				TOP					= 0;

	public static final int				WORLDSPACE			= 1;

	public static final int				INTERIOR_BLOCK		= 2;

	public static final int				INTERIOR_SUBBLOCK	= 3;

	public static final int				EXTERIOR_BLOCK		= 4;

	public static final int				EXTERIOR_SUBBLOCK	= 5;

	public static final int				CELL				= 6;

	public static final int				TOPIC				= 7;

	public static final int				CELL_PERSISTENT		= 8;

	public static final int				CELL_TEMPORARY		= 9;

	public static final int				CELL_DISTANT		= 10;
	
	protected byte						groupLabel[];

	protected String					groupRecordType		= "UNKW";

	protected int						groupParentID;

	protected int						groupType;

	public static Map<String, String>	typeMap;

	public static Set<String>			instTypes;

	protected List<Record>				recordList			= new ArrayList<Record>();

	//for tes3 version
	protected PluginGroup() {

	}

	public PluginGroup(byte prefix[]) {		
		if (prefix.length != 20 && prefix.length != 24) {
			throw new IllegalArgumentException("The record prefix is not 20 or 24 bytes as required");
		} else {
			headerByteCount = prefix.length;
		}
		this.recordType = "GRUP";
		recordLength = ESMByteConvert.extractInt(prefix, 4) - headerByteCount; //notice differs from PluginRecord
		groupLabel = new byte[4];
		System.arraycopy(prefix, 8, groupLabel, 0, 4);
		groupType = prefix[12] & 0xff;
		switch (groupType) {
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

	public List<Record> getRecordList() {
		return recordList;
	}

	public int getRecordCount() {
		int recordCount = 0;

		for (Record record : recordList) {
			recordCount++;
			if (record instanceof PluginGroup)
				recordCount += ((PluginGroup)record).getRecordCount();
		}

		return recordCount;
	}

	public int getRecordDeepDataSize() {
		int dataSize = 0;

		for (Record record : recordList) {
			if (record instanceof PluginRecord)
				dataSize += ((PluginRecord)record).getRecordDataLen();
			if (record instanceof PluginGroup)
				dataSize += ((PluginGroup)record).getRecordDeepDataSize();
		}

		return dataSize;
	}
	

	@Override
	public void load(FileChannelRAF in, long pos)
			throws IOException, PluginException {
		//-1 means load all children groups that exist (only relevant to the "children" of CELLS groups
		load(in, pos, -1);
	}


	public void load(FileChannelRAF in, long pos, int childGroupType)
			throws IOException, PluginException {
		int dataLength = this.getRecordDataLen();
		byte prefix[] = new byte[headerByteCount];
		ByteBuffer pbb = ByteBuffer.wrap(prefix); //reused to avoid allocation of object, all bytes of array are refilled or error thrown

		FileChannel ch = in.getChannel();

		while (dataLength >= headerByteCount) {
			int count = ch.read((ByteBuffer)pbb.rewind(), pos);
			pos += headerByteCount;
			if (count != headerByteCount)
				throw new PluginException("Record prefix is incomplete");

			dataLength -= headerByteCount;
			String type = new String(prefix, 0, 4);
			
			PluginRecord r;
			
			if (type.equals("GRUP")) {
				PluginGroup pg = new PluginGroup(prefix);				
				if (childGroupType == -1 || pg.getGroupType() == childGroupType) {
					pg.load(in, pos);
					recordList.add(pg);
				}
				r = pg;				
			} else {
				r = new PluginRecord(prefix);
				r.load(in, pos);
				recordList.add(r);
			}
			
			pos += r.getRecordDataLen();
			dataLength -= r.getRecordDataLen();
		}

		if (dataLength != 0) {
			if (groupType == 0)
				throw new PluginException(": Group " + groupRecordType + " is incomplete " + dataLength + " != 0");
			else
				throw new PluginException(": Subgroup type " + groupType + " is incomplete " + dataLength + " != 0");
		}

		return;
	}

	public int getGroupType() {
		return groupType;
	}

	public byte[] getGroupLabel() {
		return groupLabel;
	}

	public String getGroupRecordType() {
		return groupRecordType;
	}

	public int getGroupParentID() {
		return groupParentID;
	}

	@Override
	public String toString() {
		int intValue = ESMByteConvert.extractInt3(groupLabel, 0);
		String text;

		switch (groupType) {
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
				int x = intValue >>> 16; //TODO: fix me I've got world x,y that are reporting 255 for x but I thinks it should be -1
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

			default: {
				text = "Group: Type " + groupType + ", Parent " + intValue;
				break;
			}
		}
		return text;
	}
	
	
	//Good FO76 utils with source code here
	//https://forums.nexusmods.com/topic/7181276-made-edits-to-seventysixesm-by-hand-my-experience-so-far/	
	//https://www.nexusmods.com/fallout76/mods/1487

	private static String				groupDescriptions[][]		= {{"0HED", "Header"}, {"GRUP", "Form Group"},

		//Morrowind?
		{"TES3", "Tes3Header"},																																				//morrowind only	
		{"LOCK", "Lock Info"},																																				//morrowind only	
		{"PROB", "Probe? (Lock Picking?)"},																																	//morrowind only	
		{"REPA", "REPA"},																																					//morrowind only	
		{"SNDG", "SNDG"},																																					//morrowind only	

		//Oblivion
		{"TES4", "Tes4Header"},																																				//obliv
		{"ACTI", "Activator"}, {"ALCH", "Potion"}, {"AMMO", "Ammunition"}, {"ANIO", "Animated Object"},
		{"APPA", "Apparatus"},																																				//Not in FO3// Not in FO4// Not in FO76// Not in SF
		{"ARMO", "Armor"}, {"BOOK", "Book"}, {"BSGN", "Birthsign"},																											//Not in FO3 , Not in Skyrim// Not in FO4// Not in FO76// Not in SF
		{"CLAS", "Class"}, {"CLOT", "Clothing"},																															//Not in FO3 , Not in Skyrim// Not in FO4// Not in FO76// Not in SF
		{"CLMT", "Climate"}, {"CONT", "Container"}, {"CREA", "Creature"},																									//Not in Skyrim // Not in FO4// Not in FO76// Not in SF
		{"CSTY", "Combat Style"}, {"DIAL", "Dialog"},																														/// TODO: exists in a groutp type 10 in FO4 check these 2 // Not in FO76 // no entries SF
		{"DOOR", "Door"}, {"EFSH", "Effect Shader"}, {"ENCH", "Enchantment"}, {"EYES", "Eyes"},																				// Not in FO4// Not in FO76// Not in SF
		{"FACT", "Faction"}, {"FLOR", "Flora"},																																//Not in FO3  
		{"FURN", "Furniture"}, {"GLOB", "Global Variable"}, {"GMST", "Game Setting"}, {"GRAS", "Grass"},
		{"HAIR", "Hair"},																																					// Not in Skyrim// Not in FO4// Not in FO76// Not in SF
		{"IDLE", "Idle Animation"}, {"INGR", "Ingredient"},																													// Not in SF
		{"KEYM", "Key"}, {"LIGH", "Light"}, {"LSCR", "Load Screen"}, {"LTEX", "Land Texture"},
		{"LVLC", "Leveled Creature"},																																		// Not in Skyrim// Not in FO4// Not in FO76// Not in SF
		{"LVLI", "Leveled Item"}, {"LVSP", "Leveled Spell"},																												//Not in FO3 // Not in FO4// Not in FO76// Not in SF
		{"MGEF", "Magic Effect"}, {"MISC", "Miscellaneous Item"}, {"NPC_", "NPC"}, {"PACK", "Package (as in AI)"},
		{"QUST", "Quest"}, {"RACE", "Race"}, {"REGN", "Region"}, {"SBSP", "Subspace"},																						//Not in FO3, Not in Skyrim// Not in FO4// Not in FO76// Not in SF
		{"SCPT", "Script"},																																					// Not in Skyrim// Not in FO4// Not in FO76// Not in SF
		{"SGST", "Sigil Stone"},																																			//Not in FO3, Not in Skyrim // Not in FO4// Not in FO76// Not in SF
		{"SKIL", "Skill"},																																					//Not in FO3, Not in Skyrim  // Not in FO4// Not in FO76 // Not in SF
		{"SLGM", "Soul Gem"},																																				//Not in FO3 // Not in FO4// Not in FO76// Not in SF
		{"SOUN", "Sounds"}, {"SPEL", "Spell"}, {"STAT", "Static"}, {"TREE", "Tree"},																						// no entries FO76// Not in SF
		{"WATR", "Water"}, {"WEAP", "Weapon"}, {"WRLD", "World Space"}, {"WTHR", "Weather"},

		//new in FO3 
		{"ADDN", "Addon Node"}, {"ARMA", "Armor Addon"}, {"ASPC", "Acoustic Space"},
		{"AVIF", "Actor Value (Perk Tree Graphics)"}, {"BPTD", "Body Part Data"}, {"CAMS", "Camera"},
		{"COBJ", "Constructible Object (recipes)"}, {"CPTH", "Camera Path"}, {"DEBR", "Debris"},
		{"DOBJ", "Default Object Manager"}, {"ECZN", "Encounter Zone"},																										// Not in FO76 // Not in SF
		{"EXPL", "Explosion"}, {"FLST", "Form List (non-leveled list)"}, {"HDPT", "Head Part"}, {"IDLM", "Idle Marker"},
		{"IMAD", "Image Space Modifier"}, {"IMGS", "Image Space"}, {"IPCT", "Impact"}, {"IPDS", "Impact Data Set"},
		{"LGTM", "Lighting Template"}, {"LVLN", "Leveled Character"}, {"MESG", "Message"}, {"MICN", "Menu Icon"},															// Not in Skyrim// Not in FO4// Not in FO76 // Not in SF
		{"MSTT", "Movable Static"}, {"MUSC", "Music"}, {"NAVI", "Navigation"}, {"NOTE", "Note"},																			// Not in Skyrim// Not in SF
		{"PERK", "Perk"}, {"PROJ", "Projectile"}, {"PWAT", "Placeable Water"},																								// Not in Skyrim// Not in FO4// Not in FO76 // Not in SF
		{"RADS", "RADS?"},																																					// Not in Skyrim// Not in FO4// Not in FO76 // Not in SF
		{"RGDL", "Ragdoll"},																																				// Not in Skyrim// Not in FO4// Not in FO76 // Not in SF
		{"SCOL", "Static Collection"},																																		// Not in Skyrim
		{"TACT", "Talking Activator"},																																		// Not in SF
		{"TERM", "Terminal"},																																				// Not in Skyrim
		{"TXST", "Texture Set"}, {"VTYP", "Voice Type"},

		// Added in Fallout3 NV. Note NONE of these are in Skyrim,FO4 only 1 is in FO76,SF
		{"ALOC", "Location musCtrl (music?)"}, {"AMEF", "Ammo Effect"}, {"CCRD", "Caravan Card"},
		{"CDCK", "Caravan Deck"}, {"CHAL", "Challenge"},																													// IS IN FO76 // IS IN SF
		{"CHIP", "Poker Chip"}, {"CMNY", "Casino Money"}, {"CSNO", "Casino"}, {"DEHY", "Drinking Effect"},
		{"HUNG", "Eating Effect"}, {"IMOD", "Gun Mods"}, {"LSCT", "Loading Tip"}, {"MSET", "Music Set?"},
		{"RCCT", "Recipe (Menus?)"}, {"RCPE", "Recipe"}, {"REPU", "Reputation"}, {"SLPD", "Sleeping Effect"},

		// new in Skyrim
		{"AACT", "Character Action"}, {"ARTO", "Visual FX (Armo?)"}, {"ASTP", "Association Type"},																							// Not in SF
		{"CLFM", "Color"}, {"COLL", "Collision Layer"}, {"DLVW", "Dialog View"},																							// no entries FO76 // Not in SF
		{"DLBR", "Dialog Branch"},																																			// TODO: exists in a groutp type 10 in FO4 check these 2 Not in FO76 // no entries SF
		{"DUAL", "Dual Cast Data"},																																			// (possibly unused)// Not in FO4// Not in FO76// Not in SF
		{"EQUP", "Equip Slot"},																																				// (flag-type values)
		{"FSTP", "Footstep"}, {"FSTS", "Footstep Set"}, {"HAZD", "Hazard"}, {"KYWD", "Keyword"},
		{"LCRT", "Location Reference Type"}, {"LCTN", "Location"}, {"MATO", "Material Object"},																				// Not in SF
		{"MATT", "Material Type"}, {"MUST", "Music Track"}, {"MOVT", "Movement Type"}, {"OTFT", "Outfit"},
		{"RELA", "Relationship"},																																			// Not in SF
		{"REVB", "Reverb Parameters"}, {"RFCT", "Visual Effect"},																											// Not in SF
		{"SCEN", "Scene"},																																					//   FO4 instance?// Not in FO76// no entries SF
		{"SCRL", "Scroll"},																																					// Not in FO4// Not in FO76// Not in SF
		{"SHOU", "Shout"},																																					// Not in FO4// Not in FO76// Not in SF
		{"SMBN", "Story Manager Branch Node"}, {"SMEN", "Story Manager Event Node"},
		{"SMQN", "Story Manager Quest Node"}, {"SNCT", "Sound Category"},																									// Not in SF
		{"SNDR", "Sound Reference"},																																		// Not in SF
		{"SOPM", "Sound Output Marker"},																																	// Not in SF
		{"SPGD", "Shader Particle Geometry"}, {"WOOP", "Word Of Power"},																									// Not in FO4// Not in FO76// Not in SF

		//Added from Fallout 4			
		{"AECH", "Audio Effect Chain"},																																		// Not in SF
		{"AMDL", "Weapon (AM?)"}, {"AORU", "AMbient Animal Rule"}, {"BNDS", "Cable Spline"}, {"CMPO", "Components (of Recipes)"},														// Not in SF
		{"DFOB", "Interface somethings?"}, {"DMGT", "Damage Type"}, {"GDRY", "Sun Rays"},																					// no entries FO76// Not in SF
		{"INNR", "Equipping?"}, {"KSSM", "Weapon something?"}, {"LAYR", "Location something?"}, {"LENS", "Lens Flare"},
		{"MSWP", "Material Swaps"},																																			// Not in SF
		{"NOCM", "NOCM?"}, {"OMOD", "Model something?"}, {"OVIS", "OVIS?"}, {"PKIN", "PackIn?"}, {"RFGP", "RFGP?"},
		{"SCCO", "Views?"},																																					// Not in SF
		{"SCSN", "SCSN?"},																																					// Not in SF
		{"STAG", "Character ATS?"}, {"TRNS", "Related to models?"}, {"ZOOM", "Weapon Zoom"},

		//Added from Fallout 76
		{"AAMD", "Gun Type Template"}, {"AAPD", "Creature Body parts?"}, {"ASTM", "deprecated"},																			// Not in SF
		{"ATXO", "Entitlements and keywords?"},																																// Not in SF
		{"AUVF", "AUVF?"},																																					// Not in SF
		{"AVTR", "PlayerIcon"},																																				// Not in SF
		{"CNCY", "Currency"},																																				// Not in SF
		{"CNDF", "Conditions (for effects/events)"}, {"COEN", "Texture Data (related to icons)"},																			// Not in SF
		{"CPRD", "Reward"},																																					// Not in SF
		{"CSEN", "CSEN?"},																																					// Not in SF
		{"CURV", "Curves (as in Alphas)"}, {"DCGF", "DCGF?"},																												// Not in SF
		{"DIST", "District"},																																				// Not in SF
		{"ECAT", "Emote Category"},																																			// Not in SF
		{"EMOT", "Player Emote"},																																			// Not in SF
		{"ENTM", "Texture Data?"},																																			// Not in SF
		{"GCVR", "Ground Cover?"}, {"GMRW", "Challenge Data?"},																												// Not in SF
		{"LGDI", "Legendary Item"}, {"LOUT", "Load out"},																													// Not in SF
		{"LVLP", "Leveled Projectile?"}, {"LVPC", "Leveled Card?"},																											// Not in SF
		{"MDSP", "Animation data?"},																																		// Not in SF
		{"PACH", "Power Armour data?"},																																		// Not in SF
		{"PCRD", "Perk Card"},																																				// Not in SF
		{"PEPF", "Playlist"},																																				// Not in SF
		{"PMFT", "Photo Mode Frame"}, {"PPAK", "Perk Card Pack"},																											// Not in SF
		{"QMDL", "Quest Module"},																																			// Not in SF
		{"RESO", "Resource"},																																				// Not in SF
		{"SECH", "Marker?"}, {"STHD", "Hunger/Thirst Threshold"},																											// Not in SF
		{"STMP", "STMP? model data?"}, {"STND", "Node data?"},																												// Not in SF
		{"UTIL", "Atomic Exchange data?"},																																	// Not in SF
		{"VOLI", "Weather data?"}, {"WAVE", "Wave Data (as in wave of enemies?)"},																							// Not in SF
		{"WSPR", "Permissions?"},																																			// Not in SF

		//Added from Starfield
		{"AFFE", "Action Choice"}, {"AMBS", "Ambience Set"}, {"AOPF", "Audio Occlusion Primative"},
		{"AOPS", "Optical Sight"}, {"ATMO", "Atmosphere"}, {"AVMD", "AA Animation (Ambient Animal)"}, {"BIOM", "Biome"},
		{"BMMO", "Biome Marker"}, {"BMOD", "BM Animation"}, {"BODY", "BODY?"}, {"CLDF", "Clouds"},
		{"CUR3", "Water PBR"}, {"EFSQ", "Effects data?"}, {"FFKW", "Architecture Keywords"}, {"FOGV", "Fog Data"},
		{"FORC", "Wind Force"}, {"GBFM", "Ship Templates?"}, {"GBFT", "Templates of some sort?"}, {"IRES", "IRES?"}, {"LMSW", "Swaps?"},
		{"LVLB", "Leveled Ship?"}, {"LVSC", "LVSC?"}, {"MAAM", "Melee Data?"}, {"MRPH", "Morphables Data"},
		{"MTPT", "Material Data?"}, {"OSWP", "Swap Data?"}, {"PCBN", "PCBN?"}, {"PCCN", "PackIn Data?"},
		{"PCMT", "PCMT?"}, {"PDCL", "PDCL?"}, {"PNDT", "Planet Data"}, {"PSDC", "PSDC?"}, {"PTST", "Pattern Style"},
		{"RSGD", "RSGD?"}, {"RSPJ", "Research"}, {"SDLT", "SDLT?"}, {"SFBK", "Surface Terrain Data?"},
		{"SFPC", "Surface Pattern Config"}, {"SFPT", "Surface Pattern Data?"}, {"SFTR", "Surface Tree"},
		{"SPCH", "Speech Challenge"}, {"STBH", "STBH?"}, {"STDT", "Star Data"}, {"SUNP", "Sun Data"},
		{"TMLM", "Terminal Menu"}, {"TODD", "More Start Data?"}, {"TRAV", "Traversal (of animaion)"},
		{"WBAR", "Gun Data?"}, {"WKMF", "WKMF?"}, {"WTHS", "Weather"}, {"WWED", "Sound Data?"},

	};

	//instance recos
	private static String				instGroupDescriptions[][]	= {
		//morrowind, morrowind uses the sub FRMR (form refr) to point to instance recos
		{"CELL", "Cell"},

		// Tes4 Oblivion 
		{"ACHR", "Actor Ref"}, {"ACRE", "Creature Ref"},																													//not in Skyrim, FO4, FO76, SF
		{"INFO", "INFO?"}, {"LAND", "Land"},																																//Not in FO76, SF
		{"PGRD", "PGRD"},																																					//Not in FO3, FONV, Skyrim, FO4, FO76, SF
		{"REFR", "Object Ref"}, {"ROAD", "A Road"},																															//Not in FO3,FONV, Skyrim, FO76

		// added FO3
		{"NAVM", "Navigation Grid?"}, {"PGRE", "Placed Grenade"},

		//Skyrim
		{"PHZD", "PHZD"},

		//FO4
		{"PMIS", "PMIS"},																																					//Not in SF

			//FO76

			//SF

	};

	public static Map<String, String>	subRecDesc;
	// known sub record descriptions 
	private static String				subRecDescriptions[][]		= {
		//Oblivion 
		{"EDID", "Editor ID, used only by consturction kit, not loaded at runtime"},
		{"FULL", "Often the Object in game name, not not always"}, {"MODL", "Model Name of Nif file"},
		{"MODB", "Model Bounds"},																																			// changed to OBND FO3+
		{"MODT", "Model Translation (not sure of the contents)"}, {"ICON", "Icon pointer to a dds file"},

		{"OBND", "Object Bounds"},

	};

	static {
		if (typeMap == null) {
			typeMap = new HashMap<String, String>(groupDescriptions.length + instGroupDescriptions.length);
			for (int i = 0; i < groupDescriptions.length; i++) {
				typeMap.put(groupDescriptions[i][0], groupDescriptions[i][1]);
			}
			// notice these are also looked up
			for (int i = 0; i < instGroupDescriptions.length; i++) {
				typeMap.put(instGroupDescriptions[i][0], instGroupDescriptions[i][1]);
			}
		}
		if(instTypes == null) {
			instTypes = new HashSet<String>(instGroupDescriptions.length);
			for (int i = 0; i < instGroupDescriptions.length; i++) {
				instTypes.add(instGroupDescriptions[i][0]);
			}
		}		
		if (subRecDesc == null) {
			subRecDesc = new HashMap<String, String>(subRecDescriptions.length);
			for (int i = 0; i < subRecDescriptions.length; i++) {
				subRecDesc.put(subRecDescriptions[i][0], subRecDescriptions[i][1]);
			}
		}
	}

}