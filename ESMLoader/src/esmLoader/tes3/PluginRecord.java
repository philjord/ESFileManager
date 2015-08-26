package esmLoader.tes3;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import tools.io.ESMByteConvert;
import esmLoader.common.PluginException;
import esmLoader.common.data.plugin.PluginSubrecord;

public class PluginRecord extends esmLoader.common.data.plugin.PluginRecord
{
	private int recordSize;

	/**
	 * FormId is auto generated at load, to simulate form ids in the esm
	 * @param formId
	 */
	public PluginRecord(int formId)
	{
		this.formID = formId;
	}

	public void load(String fileName, RandomAccessFile in) throws PluginException, IOException
	{
		filePositionPointer = in.getFilePointer();
		byte[] prefix = new byte[16];
		int count = in.read(prefix);
		if (count != 16)
			throw new PluginException(fileName + ": record prefix is incomplete");

		recordType = new String(prefix, 0, 4);
		recordSize = ESMByteConvert.extractInt(prefix, 4);
		unknownInt = ESMByteConvert.extractInt(prefix, 8);
		recordFlags1 = ESMByteConvert.extractInt(prefix, 12);

		recordData = new byte[recordSize];

		count = in.read(recordData);
		if (count != recordSize)
			throw new PluginException(fileName + ": " + recordType + " record bad length, asked for " + recordSize + " got " + count);

		//attempt to find and set editor id
		for (String edidRecord : edidRecords)
		{
			if (recordType.equals(edidRecord))
			{
				for (PluginSubrecord sub : getSubrecords())
				{
					if (sub.getSubrecordType().equals("NAME"))
					{
						editorID = new String(sub.getSubrecordData());
						break;
					}
				}
				break;
			}
		}

	}

	@Override
	public List<PluginSubrecord> getSubrecords()
	{
		// must fill it up before anyone can get it asynch!
		synchronized (this)
		{
			if (subrecordList == null)
			{
				subrecordList = new ArrayList<PluginSubrecord>();
				int offset = 0;

				byte[] rd = getRecordData();
				if (rd != null)
				{
					while (offset < rd.length)
					{
						String subrecordType = new String(rd, offset + 0, 4);
						int subrecordLength = ESMByteConvert.extractInt(rd, offset + 4);
						byte subrecordData[] = new byte[subrecordLength];
						System.arraycopy(rd, offset + 8, subrecordData, 0, subrecordLength);

						subrecordList.add(new PluginSubrecord(recordType, subrecordType, subrecordData));

						offset += 8 + subrecordLength;
					}
				}
				// TODO: can I discard the raw data now?
			}
			return subrecordList;
		}
	}

	/**
	 * Can't be compressed ever
	 * @see esmLoader.common.data.plugin.PluginRecord#isCompressed()
	 */
	public boolean isCompressed()
	{
		return false;
	}

	/**
	 * just a dummy flags
	 * @see esmLoader.common.data.plugin.PluginRecord#getRecordFlags2()
	 */
	public int getRecordFlags2()
	{
		return 0;
	}

	private static String[] edidRecords = new String[]
	{ "GMST", "GLOB", "CLAS", "FACT", "RACE", "SOUN", "REGN", "BSGN", "LTEX", "STAT", "DOOR", "MISC", "WEAP", "CONT", "SPEL", "CREA",
			"BODY", "LIGH", "ENCH", "NPC_", "ARMO", "CLOT", "REPA", "ACTI", "APPA", "LOCK", "PROB", "INGR", "BOOK", "ALCH", "LEVI", "LEVC",
			"PGRD", "SNDG", "DIAL" };

	/*	
	 * 1: GMST NAME = Setting ID string			
		2: GLOB NAME = Global ID string		
		3: CLAS NAME = Class ID string
		4: FACT NAME = Faction ID string
		5: RACE NAME = Race ID string	
		6: SOUN NAME = Sound ID	string	
		
		10: REGN NAME = Region ID string	
		11: BSGN NAME = Sign ID string		
		12: LTEX NAME = Texture ID string		
		13: STAT NAME = Static ID string					
		14: DOOR NAME = Door ID string			
		15: MISC NAME = Misc ID string
		16: WEAP NAME = Weapon ID string	
		17: CONT NAME = Container ID string				
		18: SPEL NAME = Spell ID string		
		19: CREA NAME = Creature ID string		
		20: BODY NAME = Body ID string					
		21: LIGH NAME = Light ID string		
		22: ENCH NAME = Enchantment ID string		
		23: NPC_ NAME = NPC ID string	
		24: ARMO NAME = Item ID, required
		25: CLOT NAME = Item ID, required
		26: REPA NAME = Item ID, required
		27: ACTI NAME = Item ID, required	
		28: APPA NAME = Item ID, required		
		29: LOCK NAME = Item ID, required	
		30: PROB NAME = Item ID, required		
		31: INGR NAME = Item ID, required		
		32: BOOK NAME = Item ID, required			
		33: ALCH NAME = Item ID, required			
		34: LEVI NAME = levelled list ID string		
		35: LEVC NAME = levelled list ID string
		
		38: PGRD NAME = Path Grid ID string
		39: SNDG NAME = Sound Generator ID string  
		40: DIAL NAME = Dialogue ID string		
		
			
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
