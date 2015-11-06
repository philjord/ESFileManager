package TES4Gecko;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.zip.DataFormatException;

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

	private byte[] groupLabel;

	private String groupRecordType;

	private int groupParentID;

	private int groupType;

	private List<PluginRecord> recordList;

	private static Map<String, String> typeMap;

	private static String[][] groupDescriptions =
	{
	{ "ACTI", "Activators" },
	{ "ALCH", "Potions" },
	{ "AMMO", "Ammunition" },
	{ "ANIO", "Animated Object" },
	{ "APPA", "Apparatus" },
	{ "ARMO", "Armor" },
	{ "BOOK", "Books" },
	{ "BSGN", "Birthsigns" },
	{ "CELL", "Cells" },
	{ "CLAS", "Classes" },
	{ "CLOT", "Clothing" },
	{ "CLMT", "Climate" },
	{ "CONT", "Containers" },
	{ "CREA", "Creatures" },
	{ "CSTY", "Combat Styles" },
	{ "DIAL", "Dialog" },
	{ "DOOR", "Doors" },
	{ "EFSH", "Effect Shaders" },
	{ "ENCH", "Enchantments" },
	{ "EYES", "Eyes" },
	{ "FACT", "Factions" },
	{ "FLOR", "Flora" },
	{ "FURN", "Furniture" },
	{ "GLOB", "Global Variables" },
	{ "GMST", "Game Settings" },
	{ "GRAS", "Grass" },
	{ "HAIR", "Hair" },
	{ "IDLE", "Idle Animations" },
	{ "INGR", "Ingredients" },
	{ "KEYM", "Keys" },
	{ "LIGH", "Lights" },
	{ "LSCR", "Load Screens" },
	{ "LTEX", "Land Textures" },
	{ "LVLC", "Leveled Creatures" },
	{ "LVLI", "Leveled Items" },
	{ "LVSP", "Leveled Spells" },
	{ "MGEF", "Magic Effects" },
	{ "MISC", "Miscellaneous Items" },
	{ "NPC_", "NPCs" },
	{ "PACK", "Packages" },
	{ "QUST", "Quests" },
	{ "RACE", "Races" },
	{ "REGN", "Regions" },
	{ "SBSP", "Subspaces" },
	{ "SCPT", "Scripts" },
	{ "SGST", "Sigil Stones" },
	{ "SKIL", "Skills" },
	{ "SLGM", "Soul Gems" },
	{ "SOUN", "Sounds" },
	{ "SPEL", "Spells" },
	{ "STAT", "Statics" },
	{ "TREE", "Trees" },
	{ "WATR", "Water" },
	{ "WEAP", "Weapons" },
	{ "WTHR", "Weather" },
	{ "WRLD", "World Spaces" } };

	public Map<String, String> getTypeMap()
	{
		return typeMap;
	}

	public PluginGroup(byte[] prefix)
	{
		super("GRUP");

		this.groupLabel = new byte[4];
		System.arraycopy(prefix, 8, this.groupLabel, 0, 4);
		this.groupType = (prefix[12] & 0xFF);

		switch (this.groupType)
		{
			case 0:
				if (this.groupLabel[0] >= 32)
					this.groupRecordType = new String(this.groupLabel);
				else
					this.groupRecordType = new String();
				break;
			case 1:
			case 6:
			case 7:
			case 8:
			case 9:
			case 10:
				this.groupParentID = getInteger(this.groupLabel, 0);
			case 2:
			case 3:
			case 4:
			case 5:
		}
		this.recordList = new ArrayList<PluginRecord>();
		if (typeMap == null)
			buildTypeMap();
	}

	public PluginGroup(String recordType)
	{
		super("GRUP");
		this.groupType = 0;
		this.groupLabel = recordType.getBytes();
		this.groupRecordType = recordType;

		this.recordList = new ArrayList<PluginRecord>();
		if (typeMap == null)
			buildTypeMap();
	}

	public PluginGroup(int groupType, byte[] groupLabel)
	{
		super("GRUP");
		this.groupType = groupType;
		this.groupLabel = groupLabel;

		switch (groupType)
		{
			case 0:
				if (groupLabel[0] >= 32)
					this.groupRecordType = new String(groupLabel);
				else
					this.groupRecordType = new String();
				break;
			case 1:
			case 6:
			case 7:
			case 8:
			case 9:
			case 10:
				this.groupParentID = getInteger(groupLabel, 0);
			case 2:
			case 3:
			case 4:
			case 5:
		}
		this.recordList = new ArrayList<PluginRecord>();
		if (typeMap == null)
			buildTypeMap();
	}

	public PluginGroup(int groupType, int groupParentID)
	{
		super("GRUP");
		this.groupType = groupType;
		this.groupLabel = new byte[4];
		setInteger(groupParentID, this.groupLabel, 0);

		switch (groupType)
		{
			case 1:
			case 6:
			case 7:
			case 8:
			case 9:
			case 10:
				this.groupParentID = groupParentID;
			case 2:
			case 3:
			case 4:
			case 5:
		}
		this.recordList = new ArrayList<PluginRecord>();
		if (typeMap == null)
			buildTypeMap();
	}

	private void buildTypeMap()
	{
		typeMap = new HashMap<String, String>(groupDescriptions.length);
		for (int i = 0; i < groupDescriptions.length; i++)
			typeMap.put(groupDescriptions[i][0], groupDescriptions[i][1]);
	}

	public int getRecordCount()
	{
		int recordCount = 0;
		for (PluginRecord record : this.recordList)
		{
			recordCount++;
			if ((record instanceof PluginGroup))
			{
				recordCount += ((PluginGroup) record).getRecordCount();
			}
		}
		return recordCount;
	}

	public boolean isEmpty()
	{
		return this.recordList.size() == 0;
	}

	public int getGroupType()
	{
		return this.groupType;
	}

	public byte[] getGroupLabel()
	{
		return this.groupLabel;
	}

	public void setGroupLabel(byte[] label)
	{
		this.groupLabel = label;

		switch (this.groupType)
		{
			case 0:
				if (this.groupLabel[0] >= 32)
					this.groupRecordType = new String(this.groupLabel);
				else
					this.groupRecordType = new String();
				break;
			case 1:
			case 6:
			case 7:
			case 8:
			case 9:
			case 10:
				this.groupParentID = getInteger(this.groupLabel, 0);
			case 2:
			case 3:
			case 4:
			case 5:
		}
	}

	public String getGroupRecordType()
	{
		return this.groupRecordType;
	}

	public int getGroupParentID()
	{
		return this.groupParentID;
	}

	public void setGroupParentID(int parentID)
	{
		this.groupParentID = parentID;
		setInteger(parentID, this.groupLabel, 0);
	}

	public List<PluginRecord> getRecordList()
	{
		return this.recordList;
	}

	public void updateFormList(List<FormInfo> formList)
	{
		for (PluginRecord record : this.recordList)
		{
			record.setParent(this);
			if ((record instanceof PluginGroup))
			{
				PluginGroup subGroup = (PluginGroup) record;
				subGroup.updateFormList(formList);
			}
			else if (!record.isIgnored())
			{
				FormInfo formInfo = new FormInfo(record, record.getRecordType(), record.getFormID(), record.getEditorID());
				formInfo.setParentFormID(this.groupParentID);
				formList.add(formInfo);
			}
		}
	}

	public void setIgnore(boolean ignored)
	{
		ListIterator<PluginRecord> lit = this.recordList.listIterator();
		while (lit.hasNext())
		{
			PluginRecord record = lit.next();
			record.setIgnore(ignored);
		}
	}

	public void removeIgnoredRecords()
	{
		ListIterator<PluginRecord> lit = this.recordList.listIterator();
		PluginRecord prevRecord = null;
		while (lit.hasNext())
		{
			PluginRecord record = lit.next();
			if ((record instanceof PluginGroup))
			{
				PluginGroup group = (PluginGroup) record;
				group.removeIgnoredRecords();
				if (group.isEmpty())
				{
					int groupType = group.getGroupType();
					if ((groupType == 1) || (groupType == 6) || (groupType == 7))
					{
						if ((prevRecord == null) || (prevRecord.getRecordType().equals("GRUP")))
							lit.remove();
						else
							prevRecord = record;
					}
					else
						lit.remove();
				}
				else
				{
					prevRecord = record;
				}
			}
			else if (record.isIgnored())
			{
				lit.remove();
			}
			else
			{
				prevRecord = record;
			}
		}
	}

	public void suturePNAMs(Map<Integer, List<PluginRecord>> deletedMap)
	{
		if ((this.groupType != 0) || (!this.groupRecordType.equals("DIAL")))
			return;
		if (deletedMap.isEmpty())
			return;
		List<PluginRecord> groupList = getRecordList();
		for (PluginRecord dialOrInfo : groupList)
		{
			if (!(dialOrInfo instanceof PluginGroup))
				continue;
			int topicID = ((PluginGroup) dialOrInfo).getGroupParentID();
			if (deletedMap.containsKey(Integer.valueOf(topicID)))
			{
				List<PluginRecord> infoList = ((PluginGroup) dialOrInfo).getRecordList();
				List<PluginRecord> deletedList = deletedMap.get(Integer.valueOf(topicID));
				Collections.reverse(deletedList);

				for (PluginRecord delRec : deletedList)
				{
					int delPNAMID = 0;
					int delFormID = delRec.getFormID();
					try
					{
						PluginSubrecord plSubrec = delRec.getSubrecord("PNAM");
						byte[] subrecordData = plSubrec.getSubrecordData();
						delPNAMID = SerializedElement.getInteger(subrecordData, 0);
					}
					catch (Exception ex)
					{
						continue;
					}
					//byte[] subrecordData;
					for (PluginRecord rec : infoList)
					{
						try
						{
							if (!rec.changeSubrecord("PNAM", Integer.valueOf(delFormID), Integer.valueOf(delPNAMID)))
								continue;
						}
						catch (Exception localException1)
						{
						}
					}
				}
				Collections.reverse(deletedList);
			}
		}
	}

	public Map<Integer, List<PluginRecord>> getDeletedINFOMap()
	{
		Map<Integer, List<PluginRecord>> deletedINFOMap = new HashMap<Integer, List<PluginRecord>>();
		if ((this.groupType != 0) || (!this.groupRecordType.equals("DIAL")))
			return deletedINFOMap;
		List<PluginRecord> groupList = getRecordList();
		for (PluginRecord dialOrInfo : groupList)
		{
			if (!(dialOrInfo instanceof PluginGroup))
				continue;
			List<PluginRecord> infoGroup = ((PluginGroup) dialOrInfo).getDeletedPluginRecords();
			if (infoGroup.size() != 0)
			{
				int topicID = ((PluginGroup) dialOrInfo).getGroupParentID();
				List<PluginRecord> infoList = new ArrayList<PluginRecord>();
				for (PluginRecord rec : infoGroup)
				{
					infoList.add(rec);
				}
				deletedINFOMap.put(Integer.valueOf(topicID), infoList);
			}
		}
		return deletedINFOMap;
	}

	public List<PluginRecord> getAllPluginRecords()
	{
		ArrayList<PluginRecord> recList = new ArrayList<PluginRecord>();
		List<PluginRecord> tmpList = getRecordList();
		for (PluginRecord rec : tmpList)
		{
			recList.addAll(rec.getAllPluginRecords());
		}
		return recList;
	}

	public List<PluginRecord> getDeletedPluginRecords()
	{
		ArrayList<PluginRecord> recList = new ArrayList<PluginRecord>();
		List<PluginRecord> tmpList = getRecordList();
		for (PluginRecord rec : tmpList)
		{
			recList.addAll(rec.getDeletedPluginRecords());
		}
		return recList;
	}

	public int hashCode()
	{
		return getInteger(this.groupLabel, 0) + this.groupType;
	}

	public boolean equals(Object object)
	{
		boolean areEqual = false;
		if ((object instanceof PluginGroup))
		{
			PluginGroup objGroup = (PluginGroup) object;
			if (objGroup.getGroupType() == this.groupType)
			{
				byte[] objGroupLabel = objGroup.getGroupLabel();
				if (compareArrays(this.groupLabel, 0, objGroupLabel, 0, 4) == 0)
				{
					List<?> objRecordList = objGroup.getRecordList();
					if (objRecordList.size() == this.recordList.size())
					{
						areEqual = true;
						for (int i = 0; i < this.recordList.size(); i++)
						{
							if (!((PluginRecord) objRecordList.get(i)).equals(this.recordList.get(i)))
							{
								areEqual = false;
								break;
							}
						}
					}
				}
			}
		}

		return areEqual;
	}

	public boolean isIdentical(PluginGroup group)
	{
		boolean areIdentical = false;
		if (group.getGroupType() == this.groupType)
		{
			byte[] cmpGroupLabel = group.getGroupLabel();
			if (compareArrays(this.groupLabel, 0, cmpGroupLabel, 0, 4) == 0)
			{
				//PJPJPJPJ short cut for giant copy
				if(group.getFormID() == this.getFormID() )
					return true;
				
				
				List<PluginRecord> cmpRecordList = group.getRecordList();
				if (cmpRecordList.size() == this.recordList.size())
				{
					areIdentical = true;
					for (int i = 0; i < this.recordList.size(); i++)
					{
						if (!cmpRecordList.get(i).isIdentical(this.recordList.get(i)))
						{
							areIdentical = false;
							break;
						}
					}
				}
			}
		}

		return areIdentical;
	}

	public String toString()
	{
		int intValue = getInteger(this.groupLabel, 0);
		String text;

		switch (this.groupType)
		{
			case 0:
				String type = new String(this.groupLabel);
				String description = typeMap.get(type);

				if (description != null)
					text = String.format("Group: %s", new Object[]
					{ description });
				else
					text = String.format("Group: Type %s", new Object[]
					{ new String(this.groupLabel) });
				break;
			case 1:
				text = String.format("Group: Worldspace (%08X) children", new Object[]
				{ Integer.valueOf(intValue) });
				break;
			case 2:
				text = String.format("Group: Interior cell block %d", new Object[]
				{ Integer.valueOf(intValue) });
				break;
			case 3:
				text = String.format("Group: Interior cell subblock %d", new Object[]
				{ Integer.valueOf(intValue) });
				break;
			case 4:
				int x2 = intValue >>> 16;
				if ((x2 & 0x8000) != 0)
				{
					x2 |= -65536;
				}
				int y2 = intValue & 0xFFFF;
				if ((y2 & 0x8000) != 0)
				{
					y2 |= -65536;
				}
				text = String.format("Group: Exterior cell block %d,%d", new Object[]
				{ Integer.valueOf(x2), Integer.valueOf(y2) });
				break;
			case 5:
				int x = intValue >>> 16;
				if ((x & 0x8000) != 0)
				{
					x |= -65536;
				}
				int y = intValue & 0xFFFF;
				if ((y & 0x8000) != 0)
				{
					y |= -65536;
				}
				text = String.format("Group: Exterior cell subblock %d,%d", new Object[]
				{ Integer.valueOf(x), Integer.valueOf(y) });
				break;
			case 6:
				text = String.format("Group: Cell (%08X) children", new Object[]
				{ Integer.valueOf(intValue) });
				break;
			case 7:
				text = String.format("Group: Topic (%08X) children", new Object[]
				{ Integer.valueOf(intValue) });
				break;
			case 8:
				text = String.format("Group: Cell (%08X) persistent children", new Object[]
				{ Integer.valueOf(intValue) });
				break;
			case 9:
				text = String.format("Group: Cell (%08X) temporary children", new Object[]
				{ Integer.valueOf(intValue) });
				break;
			case 10:
				text = String.format("Group: Cell (%08X) visible distant children", new Object[]
				{ Integer.valueOf(intValue) });
				break;
			default:
				text = String.format("Group: Type %d, Parent %08X", new Object[]
				{ Integer.valueOf(this.groupType), Integer.valueOf(intValue) });
		}

		return text;
	}

	public void load(File file, RandomAccessFile in, int groupLength) throws PluginException, IOException, DataFormatException
	{
		int dataLength = groupLength;

		byte[] prefix = new byte[20];

		while (dataLength >= 20)
		{
			int count = in.read(prefix);
			if (count != 20)
			{
				throw new PluginException(file.getName() + ": Record prefix is incomplete");
			}
			dataLength -= 20;
			String type = new String(prefix, 0, 4);
			int length = getInteger(prefix, 4);
			PluginRecord record;

			if (type.equals("GRUP"))
			{
				length -= 20;
				record = new PluginGroup(prefix);
			}
			else
			{
				record = new PluginRecord(prefix);
			}

			record.load(file, in, length);
			this.recordList.add(record);
			dataLength -= length;
		}

		if (dataLength != 0)
		{
			if (this.groupType == 0)
			{
				throw new PluginException(file.getName() + ": Group " + this.groupRecordType + " is incomplete");
			}
			throw new PluginException(file.getName() + ": Subgroup type " + this.groupType + " is incomplete");
		}
	}

	public void store(RandomAccessFile out) throws IOException
	{
		byte[] prefix = new byte[20];
		long groupPosition = out.getFilePointer();
		out.write(prefix);

		for (PluginRecord record : this.recordList)
		{
			record.store(out);
		}

		long stopPosition = out.getFilePointer();
		System.arraycopy("GRUP".getBytes(), 0, prefix, 0, 4);
		setInteger((int) (stopPosition - groupPosition), prefix, 4);
		System.arraycopy(this.groupLabel, 0, prefix, 8, 4);
		setInteger(this.groupType, prefix, 12);
		out.seek(groupPosition);
		out.write(prefix);
		out.seek(stopPosition);
	}

	public Object clone()
	{
		Object clonedObject = super.clone();
		PluginGroup clonedGroup = (PluginGroup) clonedObject;
		clonedGroup.recordList = new ArrayList<PluginRecord>(this.recordList.size());
		for (PluginRecord record : this.recordList)
		{
			clonedGroup.recordList.add((PluginRecord) record.clone());
		}
		return clonedObject;
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.PluginGroup
 * JD-Core Version:    0.6.0
 */