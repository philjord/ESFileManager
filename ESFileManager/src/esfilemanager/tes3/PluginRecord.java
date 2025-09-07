package esfilemanager.tes3;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import esfilemanager.common.PluginException;
import esfilemanager.common.data.plugin.PluginSubrecord;
import esfilemanager.common.data.record.Subrecord;
import tools.io.ESMByteConvert;
import tools.io.FileChannelRAF;

public class PluginRecord extends esfilemanager.common.data.plugin.PluginRecord {
	protected String	editorID	= "";
	protected int		recordSize;

	/**
	 * FormId is auto generated at load, to simulate form ids in the esm
	 * @param formId
	 */
	public PluginRecord(int formId, byte[] prefix) {
		this.formID = formId;
		// memory saving mechanism  https://www.baeldung.com/java-string-pool
		recordType = new String(prefix, 0, 4).intern();
		recordSize = ESMByteConvert.extractInt(prefix, 4);
		unknownInt = ESMByteConvert.extractInt(prefix, 8);
		recordFlags1 = ESMByteConvert.extractInt(prefix, 12);
	}

	/**
	 * For making the fake wrld and cell group records for morrowind
	 * @param formId
	 */
	public PluginRecord(int formId, String recordType, String name) {
		this.formID = formId;
		if(recordType.length() != 4)
			System.out.println("PluginRecord recordType not 4 long " + recordType);
		// memory saving mechanism  https://www.baeldung.com/java-string-pool
		this.recordType = recordType.intern();
		this.editorID = name;
		subrecordList = new ArrayList<Subrecord>();
	}

	@Override
	public String getEditorID() {
		return editorID;
	}

	@Override
	public void load(FileChannelRAF in, long pos) throws PluginException, IOException {
		FileChannel ch = in.getChannel();
		
		filePositionPointer = pos;
		recordData = new byte[recordSize];

		int count = ch.read(ByteBuffer.wrap(recordData), pos);	
		pos += recordData.length;
		if (count != recordSize)
			throw new PluginException(": " + recordType + " record bad length, asked for " + recordSize + " got " + count);

		//attempt to find and set editor id
		if (!nonEdidRecordSet.contains(recordType)) {
			for(Subrecord s : getSubrecords()) {
				if (s.getSubrecordType().equals("NAME")) {
					byte[] bs = s.getSubrecordData();
					int len = bs.length - 1;
	
					// GMST are not null terminated!!
					if (recordType.equals("GMST"))
						len = bs.length;
	
					editorID = new String(bs, 0, len);
				} 
			}
		}

		// exterior cells have the x and y as the name (some are blank some are region name)
		if (recordType.equals("LTEX")) {
			//LTEX must have edid swapped to unique key system
			Subrecord s1 = getSubrecords().get(1);
			if (s1.getSubrecordType().equals("INTV")) {
				byte[] bs = s1.getSubrecordData();
				editorID = "LTEX_" + ESMByteConvert.extractInt(bs, 0);

			} else {
				new Throwable("LTEX sub record 1 is not INTV! " + recordType + " " + s1.getSubrecordType())
						.printStackTrace();
			}

		}

		/*		if (recordType.equals("SNDG"))
				{
					System.out.println("SNDG " +editorID);
				}
				if (recordType.equals("CREA"))
				{
					System.out.println("CREA " +editorID);
				}*/

		/*	if (recordType.equals("SCPT"))
			{
				FileWriter fw = new FileWriter("C:\\temp\\MorrowindScriptsCharGen.txt", true);
				String nl =System.getProperty("line.separator");
				//System.out.print("SCPT ");
				boolean outScript = false;
				for (Subrecord sr : getSubrecords())
				{
					if (sr.getSubrecordType().equals("SCHD"))
					{
						//MoveAndTurn?
						//Main
						//Sound_Cave_Drip
						//Startup
						//CharGen*
						String n = new String(sr.getSubrecordData(), 0, 32);
						if (n.toLowerCase().contains("chargen"))
						{
							outScript = true;
							//System.out.println("Name = " + n);
							fw.write("Script: " + n + nl);
						}
					}
					else if (sr.getSubrecordType().equals("SCTX") && outScript)
					{
						//System.out.println(" " + new String(sr.getSubrecordData()));
						fw.write(new String(sr.getSubrecordData()) + nl);
					}
		
				}
				fw.flush();
				fw.close();
			}
			*/

	}
	
	@Override
	public List<Subrecord> getSubrecords() {
		// must fill it up before anyone can get it asynch!
		synchronized (this) {
			if (subrecordList == null) {
				subrecordList = new ArrayList<Subrecord>();
				getFillSubrecords(recordType, subrecordList, recordData);
				recordData = null;
			}
			return subrecordList;
		}
	}

	public static void getFillSubrecords(String recordType, List<Subrecord> subrecordList, byte[] recordData) {
		int offset = 0;

		if (recordData != null) {
			while (offset < recordData.length) {
				String subrecordType = new String(recordData, offset + 0, 4);
				int subrecordLength = ESMByteConvert.extractInt(recordData, offset + 4);
				byte subrecordData[] = new byte[subrecordLength];
				System.arraycopy(recordData, offset + 8, subrecordData, 0, subrecordLength);

				subrecordList.add(new PluginSubrecord(subrecordType, subrecordData));

				offset += 8 + subrecordLength;
			}
		}

	}

	/**
	 * Can't be compressed ever
	 * @see esfilemanager.common.data.plugin.PluginRecord#isCompressed()
	 */
	@Override
	public boolean isCompressed() {
		return false;
	}

	/**
	 * just a dummy flags
	 * @see esfilemanager.common.data.plugin.PluginRecord#getRecordFlags2()
	 */
	@Override
	public int getRecordFlags2() {
		return 0;
	}

	public static String[]			edidRecords			= new String[] {"GMST", "GLOB", "CLAS", "FACT", "RACE", "SOUN",			 
		"REGN", "BSGN", "LTEX", "STAT", "DOOR", "MISC", "WEAP", "CONT", "SPEL", "CREA", "BODY",											 
		"LIGH", "ENCH", "NPC_", "ARMO", "CLOT", "REPA", "ACTI", "APPA", "LOCK", "PROB",											 
		"INGR", "BOOK", "ALCH", "LEVI", "LEVC",																					 
		"SNDG", "DIAL"
		// not in morrowind
		,"SSCR"	
		// change records from save files https://www.mwmythicmods.com/argent/tech/es_format.html
		,"NPCC", "CREC", "CNTC", "REFR", "QUES", "JOUR", "FMAP", "PCDT", "GAME", "SPLM", "STLN"
	};

	public static String[]			nonEdidRecords		= new String[] {"TES3", "SKIL", "MGEF", "SCPT", "INFO", "LAND",			 
		"PGRD"};

	public static HashSet<String>	edidRecordSet		= new HashSet<String>();
	public static HashSet<String>	nonEdidRecordSet	= new HashSet<String>();

	static {
		for (String edidRecord : edidRecords)
			edidRecordSet.add(edidRecord);

		for (String nonEdidRecord : nonEdidRecords)
			nonEdidRecordSet.add(nonEdidRecord);
	}
	//"PGRD" is just the name of the cell it's in, but it's a 1 to 1 with prior cell
	//"DIAL", over laps with other names
	//LTEX has to be swapped out to use INTV int

	/*	
	    1: GMST NAME = Setting ID string			
		2: GLOB NAME = Global ID string		
		3: CLAS NAME = Class ID string
		4: FACT NAME = Faction ID string
		5: RACE NAME = Race ID string	
		6: SOUN NAME = Sound ID	string	
		
		10: REGN NAME = Region ID string	
		11: BSGN NAME = Sign ID string		
		12: LTEX NAME = Texture ID string	*	
		13: STAT NAME = Static ID string					
		14: DOOR NAME = Door ID string			
		15: MISC NAME = Misc ID string
		16: WEAP NAME = Weapon ID string	
		17: CONT NAME = Container ID string				
		18: SPEL NAME = Spell ID string		
		19: CREA NAME = Creature ID string		
		20: BODY NAME = Body ID string		*			
		21: LIGH NAME = Light ID string		
		22: ENCH NAME = Enchantment ID string		
		23: NPC_ NAME = NPC ID string	
		24: ARMO NAME = Item ID, required
		25: CLOT NAME = Item ID, required
		26: REPA NAME = Item ID, required*
		27: ACTI NAME = Item ID, required	
		28: APPA NAME = Item ID, required		
		29: LOCK NAME = Item ID, required	*
		30: PROB NAME = Item ID, required	*	
		31: INGR NAME = Item ID, required		
		32: BOOK NAME = Item ID, required			
		33: ALCH NAME = Item ID, required			
		34: LEVI NAME = leveled list ID string		
		35: LEVC NAME = leveled list ID string
		
		38: PGRD NAME = Path Grid ID string - Name of cell it follows
		39: SNDG NAME = Sound Generator ID string  *
		40: DIAL NAME = Dialogue ID string - ???
		
			
		7: SKIL INDX = Skill ID (4 bytes, long)	The Skill ID (0 to 26) since skills are hardcoded in the game	
		8: MGEF INDX = The Effect ID (0 to 137) (4 bytes, long)	
		9: SCPT SCHD = Script Header (52 bytes)		char Name[32]	long NumShorts...
					
				
		36: CELL NAME = Cell ID string. Can be an empty string for exterior cells in which case
				the region name is used instead.
			DATA = Cell Data
				long Flags
					0x01 = Interior?
					0x02 = Has Water
					0x04 = Illegal to Sleep here
					0x80 = Behave like exterior (Tribunal)
				long GridX
				long GridY
			RGNN = Region name string
			
		37: LAND INTV (8 bytes)
				long CellX
				long CellY
					The cell coordinates of the cell.			
			
		41: INFO INAM = Info name string (unique sequence of #'s), ID
		*/

}
