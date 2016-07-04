package esmmanager.tes3;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import esmmanager.common.PluginException;
import esmmanager.common.data.plugin.PluginSubrecord;
import esmmanager.common.data.record.Subrecord;
import tools.io.ESMByteConvert;

public class PluginRecord extends esmmanager.common.data.plugin.PluginRecord
{
	protected String editorID = "";
	protected int recordSize;

	/**
	 * FormId is auto generated at load, to simulate form ids in the esm
	 * @param formId
	 */
	public PluginRecord(int formId, byte[] prefix)
	{
		this.formID = formId;

		recordType = new String(prefix, 0, 4);
		recordSize = ESMByteConvert.extractInt(prefix, 4);
		unknownInt = ESMByteConvert.extractInt(prefix, 8);
		recordFlags1 = ESMByteConvert.extractInt(prefix, 12);
	}

	/**
	 * For making the fake wrld  and cell group records for morrowind
	 * @param formId
	 */
	public PluginRecord(int formId, String recordType, String name)
	{
		this.formID = formId;
		this.recordType = recordType;
		this.editorID = name;
		subrecordList = new ArrayList<Subrecord>();
	}

	public String getEditorID()
	{
		return editorID;
	}

	//Note recordLength is not used for tes3 (we have record size already in constructor
	public void load(String fileName, RandomAccessFile in, int recordLength) throws PluginException, IOException
	{
		filePositionPointer = in.getFilePointer();
		recordData = new byte[recordSize];

		int count = in.read(recordData);
		if (count != recordSize)
			throw new PluginException(fileName + ": " + recordType + " record bad length, asked for " + recordSize + " got " + count);

		//attempt to find and set editor id
		if (!nonEdidRecordSet.contains(recordType))
		{
			Subrecord s0 = getSubrecords().get(0);
			if (s0.getSubrecordType().equals("NAME"))
			{
				byte[] bs = s0.getSubrecordData();
				int len = bs.length - 1;

				// GMST are not null terminated!!
				if (recordType.equals("GMST"))
					len = bs.length;

				editorID = new String(bs, 0, len);
			}
			else
			{
				new Throwable("sub record 0 is not NAME! " + s0.getRecordType()).printStackTrace();
			}
		}

		// exterior cells have the x and y as the name (some are blank some are region name)
		if (recordType.equals("LTEX"))
		{
			//LTEX must have edid swapped to unique key system
			Subrecord s1 = getSubrecords().get(1);
			if (s1.getSubrecordType().equals("INTV"))
			{
				byte[] bs = s1.getSubrecordData();
				editorID = "LTEX_" + ESMByteConvert.extractInt(bs, 0);

			}
			else
			{
				new Throwable("LTEX sub record 1 is not INTV! " + s1.getRecordType()).printStackTrace();
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
					if (n.trim().startsWith("CharGen"))
					{
						outScript = true;
						System.out.println("Name = " + n);
					}
				}					
				else if (sr.getSubrecordType().equals("SCTX") && outScript)
				{
					System.out.println(" " + new String(sr.getSubrecordData()));
				}

			}
		}*/

	}

	@Override
	public List<Subrecord> getSubrecords()
	{
		// must fill it up before anyone can get it asynch!
		synchronized (this)
		{
			if (subrecordList == null)
			{
				subrecordList = new ArrayList<Subrecord>();
				getFillSubrecords(recordType, subrecordList, recordData);
				recordData = null;
			}
			return subrecordList;
		}
	}

	public static void getFillSubrecords(String recordType, List<Subrecord> subrecordList, byte[] recordData)
	{
		int offset = 0;

		if (recordData != null)
		{
			while (offset < recordData.length)
			{
				String subrecordType = new String(recordData, offset + 0, 4);
				int subrecordLength = ESMByteConvert.extractInt(recordData, offset + 4);
				byte subrecordData[] = new byte[subrecordLength];
				System.arraycopy(recordData, offset + 8, subrecordData, 0, subrecordLength);

				subrecordList.add(new PluginSubrecord(recordType, subrecordType, subrecordData));

				offset += 8 + subrecordLength;
			}
		}

	}

	/**
	 * Can't be compressed ever
	 * @see esmmanager.common.data.plugin.PluginRecord#isCompressed()
	 */
	public boolean isCompressed()
	{
		return false;
	}

	/**
	 * just a dummy flags
	 * @see esmmanager.common.data.plugin.PluginRecord#getRecordFlags2()
	 */
	public int getRecordFlags2()
	{
		return 0;
	}

	private static String[] edidRecords = new String[] { "GMST", "GLOB", "CLAS", "FACT", "RACE", "SOUN", //1-6
			"REGN", "BSGN", "STAT", "DOOR", "MISC", "WEAP", "CONT", "SPEL", "CREA", "BODY", //10-20
			"LIGH", "ENCH", "NPC_", "ARMO", "CLOT", "REPA", "ACTI", "APPA", "LOCK", "PROB", //21-30
			"INGR", "BOOK", "ALCH", "LEVI", "LEVC", //
			"SNDG", "DIAL" };

	private static String[] nonEdidRecords = new String[] { "TES3", "SKIL", "MGEF", "SCPT", "INFO", "LAND", //
			"PGRD" };

	private static HashSet<String> edidRecordSet = new HashSet<String>();
	private static HashSet<String> nonEdidRecordSet = new HashSet<String>();

	static
	{
		for (String edidRecord : edidRecords)
			edidRecordSet.add(edidRecord);

		for (String nonEdidRecord : nonEdidRecords)
			nonEdidRecordSet.add(nonEdidRecord);
	}
	//"PGRD", "DIAL", over lap with other names
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
		
		38: PGRD NAME = Path Grid ID string - ???
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
