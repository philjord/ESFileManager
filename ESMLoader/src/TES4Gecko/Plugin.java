package TES4Gecko;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;

public class Plugin extends SerializedElement
{
	private File pluginFile;

	private PluginHeader pluginHeader;

	private ArrayList<PluginGroup> groupList;

	private ArrayList<FormInfo> formList;

	private HashMap<Integer, FormInfo> formMap;

	private static final String[] initialGroupList =
	{ "GMST", "GLOB", "CLAS", "FACT", "HAIR", "EYES", "RACE", "SOUN", "SKIL", "MGEF", "SCPT", "LTEX", "ENCH", "SPEL", "BSGN", "ACTI",
			"APPA", "ARMO", "BOOK", "CLOT", "CONT", "DOOR", "INGR", "LIGH", "MISC", "STAT", "GRAS", "TREE", "FLOR", "FURN", "WEAP", "AMMO",
			"NPC_", "CREA", "LVLC", "SLGM", "KEYM", "ALCH", "SBSP", "SGST", "LVLI", "WTHR", "CLMT", "REGN", "CELL", "WRLD", "DIAL", "QUST",
			"IDLE", "PACK", "CSTY", "LSCR", "LVSP", "ANIO", "WATR", "EFSH" };

	public static final int NoRegionAssignedToCell = 65535;

	public Plugin(File pluginFile)
	{
		this.pluginFile = pluginFile;
		this.pluginHeader = new PluginHeader(pluginFile);
		this.groupList = new ArrayList<PluginGroup>(initialGroupList.length);
		this.formList = new ArrayList<FormInfo>(1000);
		this.formMap = new HashMap<Integer, FormInfo>(1000);
	}

	public Plugin(File pluginFile, String creator, String summary, List<String> masterList)
	{
		this.pluginFile = pluginFile;
		this.pluginHeader = new PluginHeader(pluginFile);
		this.pluginHeader.setCreator(creator);
		this.pluginHeader.setSummary(summary);
		this.pluginHeader.setMasterList(masterList);
		this.groupList = new ArrayList<PluginGroup>(initialGroupList.length);
		this.formList = new ArrayList<FormInfo>(1000);
		this.formMap = new HashMap<Integer, FormInfo>(1000);
	}

	public String getName()
	{
		return this.pluginFile.getName();
	}

	public File getPluginFile()
	{
		return this.pluginFile;
	}

	public void setPluginFile(File file)
	{
		this.pluginFile = file;
	}

	public float getVersion()
	{
		return this.pluginHeader.getVersion();
	}

	public void setVersion(float version)
	{
		this.pluginHeader.setVersion(version);
	}

	public String getCreator()
	{
		return this.pluginHeader.getCreator();
	}

	public void setCreator(String creator)
	{
		this.pluginHeader.setCreator(creator);
	}

	public String getSummary()
	{
		return this.pluginHeader.getSummary();
	}

	public void setSummary(String summary)
	{
		this.pluginHeader.setSummary(summary);
	}

	public int getRecordCount()
	{
		return this.pluginHeader.getRecordCount();
	}

	public boolean isMaster()
	{
		return this.pluginHeader.isMaster();
	}

	public void setMaster(boolean master)
	{
		this.pluginHeader.setMaster(master);
	}

	public List<String> getMasterList()
	{
		return this.pluginHeader.getMasterList();
	}

	public void setMasterList(List<String> masterList)
	{
		this.pluginHeader.setMasterList(masterList);
	}

	public void resetFormList()
	{
		for (int i = 0; i < this.formList.size(); i++)
		{
			this.formList.set(i, null);
		}
		this.formList = new ArrayList<FormInfo>(1000);
	}

	public void resetFormMap()
	{
		this.formMap.clear();
		this.formMap = new HashMap<Integer, FormInfo>(1000);
	}

	public void repopulateFormList()
	{
		this.formList = new ArrayList<FormInfo>(1000);
		List<PluginGroup> topGroups = getGroupList();
		for (PluginGroup topGroup : topGroups)
		{
			topGroup.updateFormList(this.formList);
		}
	}

	public void repopulateFormMap()
	{
		this.formMap = new HashMap<Integer, FormInfo>(1000);
		for (FormInfo info : this.formList)
		{
			info.setPlugin(this);
			this.formMap.put(new Integer(info.getFormID()), info);
		}
	}

	public List<FormInfo> getFormList()
	{
		return this.formList;
	}

	public Map<Integer, FormInfo> getFormMap()
	{
		return this.formMap;
	}

	public List<PluginGroup> getGroupList()
	{
		return this.groupList;
	}

	public void createInitialGroups()
	{
		this.groupList.clear();
		this.formList.clear();
		this.formMap.clear();

		for (int i = 0; i < initialGroupList.length; i++)
			this.groupList.add(new PluginGroup(initialGroupList[i]));
	}

	public PluginGroup createTopGroup(String recordType) throws PluginException
	{
		PluginGroup group = null;
		boolean foundGroup = false;
		boolean createdGroup = false;
		int index;
		for (index = 0; index < initialGroupList.length; index++)
		{
			if (initialGroupList[index].equals(recordType))
			{
				foundGroup = true;
				break;
			}
		}

		if (!foundGroup)
		{
			throw new PluginException("TOP group type " + recordType + " is not valid");
		}

		int size = this.groupList.size();
		for (int i = 0; i < size; i++)
		{
			group = this.groupList.get(i);
			String groupRecordType = group.getGroupRecordType();
			for (int j = 0; j < initialGroupList.length; j++)
			{
				if (initialGroupList[j].equals(groupRecordType))
				{
					if (j == index)
					{
						createdGroup = true;
						break;
					}
					if (j <= index)
						break;
					group = new PluginGroup(recordType);
					this.groupList.add(i, group);
					createdGroup = true;

					break;
				}
			}

			if (createdGroup)
			{
				break;
			}
		}
		if (!createdGroup)
		{
			group = new PluginGroup(recordType);
			this.groupList.add(group);
		}

		return group;
	}

	public PluginGroup createHierarchy(PluginRecord record, FormAdjust formAdjust) throws DataFormatException, IOException, PluginException
	{
		PluginGroup pluginGroup = null;
		PluginGroup parentGroup = (PluginGroup) record.getParent();
		if (parentGroup == null)
		{
			if ((record instanceof PluginGroup))
			{
				throw new PluginException(String.format("Type %d group does not have a parent", new Object[]
				{ Integer.valueOf(((PluginGroup) record).getGroupType()) }));
			}
			throw new PluginException(String.format("%s record %s (%08X) does not have a parent", new Object[]
			{ record.getRecordType(), record.getEditorID(), Integer.valueOf(record.getFormID()) }));
		}

		int groupType = parentGroup.getGroupType();
		if (groupType == 0)
		{
			pluginGroup = createTopGroup(parentGroup.getGroupRecordType());
		}
		else
		{
			boolean foundGroup = false;

			if ((record instanceof PluginGroup))
			{
				PluginGroup group = (PluginGroup) record;
				if (group.getGroupType() == 6)
				{
					int newFormID = formAdjust.adjustFormID(group.getGroupParentID());
					FormInfo formInfo = this.formMap.get(new Integer(newFormID));
					if (formInfo != null)
					{
						pluginGroup = (PluginGroup) ((PluginRecord) formInfo.getSource()).getParent();
						foundGroup = true;
					}
				}
			}
			else if (record.getRecordType().equals("CELL"))
			{
				int newFormID = formAdjust.adjustFormID(record.getFormID());
				FormInfo formInfo = this.formMap.get(new Integer(newFormID));
				if (formInfo != null)
				{
					pluginGroup = (PluginGroup) ((PluginRecord) formInfo.getSource()).getParent();
					foundGroup = true;
				}
			}

			if (!foundGroup)
			{
				PluginGroup grandparentGroup = createHierarchy(parentGroup, formAdjust);

				byte[] groupLabel = parentGroup.getGroupLabel();
				int groupParentID = 0;
				switch (groupType)
				{
					case 1:
					case 6:
					case 7:
					case 8:
					case 9:
					case 10:
						groupParentID = formAdjust.adjustFormID(parentGroup.getGroupParentID());
					case 2:
					case 3:
					case 4:
					case 5:
				}
				List<PluginRecord> recordList = grandparentGroup.getRecordList();
				for (PluginRecord parentRecord : recordList)
				{
					if ((parentRecord instanceof PluginGroup))
					{
						pluginGroup = (PluginGroup) parentRecord;
						int checkType = pluginGroup.getGroupType();

						//int recordCount;
						//int index;
						if (checkType == groupType)
						{
							if (groupParentID != 0)
							{
								if (pluginGroup.getGroupParentID() == groupParentID)
								{
									foundGroup = true;
									break;
								}
							}
							else
							{
								byte[] checkLabel = pluginGroup.getGroupLabel();
								if ((checkLabel[0] != groupLabel[0]) || (checkLabel[1] != groupLabel[1])
										|| (checkLabel[2] != groupLabel[2]) || (checkLabel[3] != groupLabel[3]))
									continue;
								foundGroup = true;
								break;
							}

						}

					}

				}

				if (!foundGroup)
				{
					if (groupParentID != 0)
						pluginGroup = new PluginGroup(groupType, groupParentID);
					else
					{
						pluginGroup = new PluginGroup(groupType, groupLabel);
					}
					pluginGroup.setParent(grandparentGroup);
					if ((groupType == 10) || (groupType == 8))
						recordList.add(0, pluginGroup);
					else
					{
						recordList.add(pluginGroup);
					}
					if (Main.debugMode)
					{
						System.out.printf("%s: Created type %d parent group %08X\n", new Object[]
						{ this.pluginFile.getName(), Integer.valueOf(groupType), Integer.valueOf(groupParentID) });
					}

				}

			}

		}

		if ((record instanceof PluginGroup))
		{
			PluginGroup group;
			group = (PluginGroup) record;
			groupType = group.getGroupType();
			if ((groupType == 1) || (groupType == 6) || (groupType == 7))
			{

				int groupParentID = formAdjust.adjustFormID(group.getGroupParentID());
				List<PluginRecord> recordList = parentGroup.getRecordList();
				int recordCount = recordList.size();
				for (int index = 1; index < recordCount; index++)
				{
					if (recordList.get(index) == group)
					{
						PluginRecord prevRecord = recordList.get(index - 1);
						String recordType = prevRecord.getRecordType();
						if ((!recordType.equals("WRLD")) && (!recordType.equals("CELL")) && (!recordType.equals("DIAL")))
							break;
						int formID = prevRecord.getFormID();
						if ((group.getGroupParentID() != formID) || (this.formMap.get(new Integer(groupParentID)) != null))
							break;
						copyRecord(prevRecord, formAdjust);

						break;
					}
				}
			}
		}

		return pluginGroup;
	}

	public void copyRecord(PluginRecord record, FormAdjust formAdjust) throws DataFormatException, IOException, PluginException
	{
		PluginGroup pluginGroup = createHierarchy(record, formAdjust);
		List<PluginRecord> groupRecordList = pluginGroup.getRecordList();

		int formID = formAdjust.adjustFormID(record.getFormID());
		String editorID = record.getEditorID();

		String recordType = record.getRecordType();
		PluginRecord pluginRecord = (PluginRecord) record.clone();
		pluginRecord.setFormID(formID);
		pluginRecord.setParent(pluginGroup);
		pluginRecord.updateReferences(formAdjust);
		groupRecordList.add(pluginRecord);

		if (recordType.equals("INFO"))
		{
			Map<Integer, PluginRecord> prevMap = new HashMap<Integer, PluginRecord>(groupRecordList.size());
			for (PluginRecord checkRecord : groupRecordList)
			{
				int prevFormID = 0;
				List<PluginSubrecord> subrecords = checkRecord.getSubrecords();
				for (PluginSubrecord subrecord : subrecords)
				{
					if (subrecord.getSubrecordType().equals("PNAM"))
					{
						prevFormID = getInteger(subrecord.getSubrecordData(), 0);
						break;
					}
				}

				prevMap.put(new Integer(prevFormID), checkRecord);
			}

			List<PluginRecord> sortedList = new ArrayList<PluginRecord>(groupRecordList.size());
			int prevFormID = 0;
			while (true)
			{
				PluginRecord sortedRecord = prevMap.get(new Integer(prevFormID));
				if (sortedRecord == null)
				{
					break;
				}
				groupRecordList.remove(sortedRecord);
				sortedList.add(sortedRecord);
				prevFormID = sortedRecord.getFormID();
			}

			int count = sortedList.size();
			for (int index = 0; index < count; index++)
			{
				groupRecordList.add(index, sortedList.get(index));
			}

		}

		FormInfo formInfo = new FormInfo(pluginRecord, recordType, formID, editorID);
		formInfo.setPlugin(this);
		formInfo.setParentFormID(pluginGroup.getGroupParentID());
		this.formList.add(formInfo);
		this.formMap.put(new Integer(formID), formInfo);
		if (Main.debugMode)
		{
			System.out.printf("%s: Added %s record %s (%08X)\n", new Object[]
			{ this.pluginFile.getName(), recordType, editorID, Integer.valueOf(formID) });
		}

		if (!pluginRecord.isDeleted())
		{
			PluginGroup subgroup = null;
			if (recordType.equals("WRLD"))
				subgroup = new PluginGroup(1, formID);
			else if (recordType.equals("CELL"))
				subgroup = new PluginGroup(6, formID);
			else if (recordType.equals("DIAL"))
			{
				subgroup = new PluginGroup(7, formID);
			}
			if (subgroup != null)
			{
				subgroup.setParent(pluginGroup);
				groupRecordList.add(subgroup);
				if (Main.debugMode)
					System.out.printf("%s: Added type %d group %08X\n", new Object[]
					{ this.pluginFile.getName(), Integer.valueOf(subgroup.getGroupType()), Integer.valueOf(formID) });
			}
		}
	}

	public void removeRecord(PluginRecord record)
	{
		int formID = record.getFormID();
		String recordType = record.getRecordType();
		PluginGroup parentGroup = (PluginGroup) record.getParent();
		List<?> recordList = parentGroup.getRecordList();
		int index = recordList.indexOf(record);

		if (index >= 0)
		{
			recordList.remove(index);
			Integer mapFormID = new Integer(formID);
			FormInfo formInfo = this.formMap.get(mapFormID);
			if (formInfo != null)
			{
				this.formMap.remove(mapFormID);
				this.formList.remove(formInfo);
			}

			if (((recordType.equals("WRLD")) || (recordType.equals("CELL")) || (recordType.equals("DIAL"))) && (index < recordList.size()))
			{
				PluginRecord checkRecord = (PluginRecord) recordList.get(index);
				if ((checkRecord instanceof PluginGroup))
				{
					PluginGroup subgroup = (PluginGroup) checkRecord;
					int groupType = subgroup.getGroupType();
					if (((groupType == 1) || (groupType == 6) || (groupType == 7)) && (subgroup.getGroupParentID() == formID))
					{
						subgroup.removeIgnoredRecords();
						if (subgroup.isEmpty())
							recordList.remove(index);
					}
				}
			}
		}
	}

	public PluginGroup getTopGroup(String groupType)
	{
		PluginGroup group = null;

		List<PluginGroup> topGroups = getGroupList();
		for (PluginGroup topGroup : topGroups)
		{
			String groupRecordType = topGroup.getGroupRecordType();
			if (!groupRecordType.equalsIgnoreCase(groupType))
				continue;
			group = topGroup;
			break;
		}

		return group;
	}

	public List<PluginRecord> getRegionsInWorldspace(int WSID)
	{
		List<PluginRecord> regionList = new ArrayList<PluginRecord>();
		PluginGroup REGNGroup = getTopGroup("REGN");
		if (REGNGroup == null)
			return regionList;
		List<PluginRecord> regList = REGNGroup.getRecordList();
		for (PluginRecord region : regList)
		{
			if (((region instanceof PluginGroup)) || (!region.getRecordType().equals("REGN")))
				continue;
			PluginSubrecord regionWS = null;
			try
			{
				regionWS = region.getSubrecord("WNAM");
				if (regionWS != null)
				{
					String regWSIDstr = regionWS.getDisplayData();
					int regWSID = Integer.parseInt(regWSIDstr, 16);
					if (regWSID != WSID)
						continue;
					regionList.add(region);
				}
			}
			catch (Exception localException)
			{
			}
		}
		return regionList;
	}

	public PluginGroup getWorldspaceGroupForRegion(PluginRecord region)
	{
		PluginGroup WSGroupRegion = null;
		int regWSID = -1;
		if (!region.getRecordType().equals("REGN"))
			return WSGroupRegion;
		try
		{
			String regWSIDstr = region.getSubrecord("WNAM").getDisplayData();

			regWSID = Integer.parseInt(regWSIDstr, 16);
		}
		catch (Exception ex)
		{
			return WSGroupRegion;
		}

		PluginGroup WRLDGroup = getTopGroup("WRLD");
		if (WRLDGroup == null)
			return WSGroupRegion;
		List<?> regList = WRLDGroup.getRecordList();
		for (int i = 0; i < regList.size(); i += 2)
		{
			PluginRecord WSRec = (PluginRecord) regList.get(i);
			PluginGroup WSGroup = (PluginGroup) regList.get(i + 1);
			if (WSRec.getFormID() != regWSID)
				continue;
			WSGroupRegion = WSGroup;
			break;
		}

		return WSGroupRegion;
	}

	public List<PluginRecord> getChildWorldspaces(int WSID)
	{
		List<PluginRecord> WSList = new ArrayList<PluginRecord>();
		PluginGroup WRLDGroup = getTopGroup("WRLD");
		if (WRLDGroup == null)
			return WSList;
		List<PluginRecord> worldList = WRLDGroup.getRecordList();
		for (PluginRecord world : worldList)
		{
			if (((world instanceof PluginGroup)) || (!world.getRecordType().equals("WRLD")))
				continue;
			PluginSubrecord parentWS = null;
			try
			{
				parentWS = world.getSubrecord("WNAM");
				if (parentWS != null)
				{
					String worldIDstr = parentWS.getDisplayData();
					int worldID = Integer.parseInt(worldIDstr, 16);
					if (worldID != WSID)
						continue;
					WSList.add(world);
				}
			}
			catch (Exception localException)
			{
			}
		}
		return WSList;
	}

	public List<Integer> getCellRegionsUsed()
	{
		List<Integer> regionList = new ArrayList<Integer>();
		PluginGroup WRLDGroup = getTopGroup("WRLD");
		if (WRLDGroup == null)
			return regionList;
		List<?> recList = WRLDGroup.getRecordList();
		for (int i = 0; i < recList.size(); i += 2)
		{
			PluginGroup worldGroup = (PluginGroup) recList.get(i + 1);
			List<PluginRecord> worldGroupList = worldGroup.getRecordList();

			List<PluginGroup> blockList = new ArrayList<PluginGroup>();

			for (PluginRecord block : worldGroupList)
			{
				if (!(block instanceof PluginGroup))
					continue;
				switch (((PluginGroup) block).getGroupType())
				{
					case 6:
						break;
					case 4:
						blockList.add((PluginGroup) block);
					case 5:
				}
			}

			for (PluginGroup block : blockList)
			{
				List<PluginRecord> subBlockList = block.getRecordList();
				for (PluginRecord subBlock : subBlockList)
				{
					List<PluginRecord> cellList = ((PluginGroup) subBlock).getRecordList();
					for (PluginRecord cell : cellList)
					{
						if (((cell instanceof PluginGroup)) || (!cell.getRecordType().equals("CELL")))
							continue;
						PluginSubrecord region = null;
						try
						{
							region = cell.getSubrecord("XCLR");
						}
						catch (Exception localException)
						{
						}

						if (region != null)
						{
							String regionIDs = region.getDisplayData();
							String[] regionArray = regionIDs.replace('\n', ' ').split(", ");
							for (int j = 0; j < regionArray.length; j++)
							{
								int regionID = Integer.parseInt(regionArray[j], 16);
								if (regionList.contains(Integer.valueOf(regionID)))
									continue;
								regionList.add(Integer.valueOf(regionID));
							}

						}
						else if (!regionList.contains(Integer.valueOf(65535)))
						{
							regionList.add(Integer.valueOf(65535));
						}
					}
				}
			}
		}

		return regionList;
	}

	public String getCellRegionsUsedStr()
	{
		String retStr = "";
		List<Integer> regionList = getCellRegionsUsed();
		for (Integer i : regionList)
		{
			if (retStr.equals(""))
				retStr = retStr + String.format("%08X", new Object[]
				{ i });
			else
				retStr = retStr + "-" + String.format("%08X", new Object[]
				{ i });
		}
		return retStr;
	}

	public void ignoreAllExteriorCells()
	{
		PluginGroup WRLDGroup = getTopGroup("WRLD");
		if (WRLDGroup == null)
			return;
		List<?> recList = WRLDGroup.getRecordList();
		for (int i = 0; i < recList.size(); i += 2)
		{
			PluginGroup worldGroup = (PluginGroup) recList.get(i + 1);
			List<PluginRecord> worldGroupList = worldGroup.getRecordList();

			for (PluginRecord block : worldGroupList)
			{
				if (!(block instanceof PluginGroup))
					continue;
				switch (((PluginGroup) block).getGroupType())
				{
					case 6:
						break;
					case 4:
						block.setIgnore(true);
					case 5:
				}
			}
		}
	}

	public void ignoreAllExteriorCellsExcept(List<Integer> regionList)
	{
		PluginGroup WRLDGroup = getTopGroup("WRLD");
		if (WRLDGroup == null)
			return;
		List<?> recList = WRLDGroup.getRecordList();
		for (int i = 0; i < recList.size(); i += 2)
		{
			PluginGroup worldGroup = (PluginGroup) recList.get(i + 1);
			List<PluginRecord> worldGroupList = worldGroup.getRecordList();

			List<PluginGroup> blockList = new ArrayList<PluginGroup>();

			for (PluginRecord block : worldGroupList)
			{
				if (!(block instanceof PluginGroup))
					continue;
				switch (((PluginGroup) block).getGroupType())
				{
					case 6:
						break;
					case 4:
						blockList.add((PluginGroup) block);
					case 5:
				}
			}

			for (PluginGroup block : blockList)
			{
				List<PluginRecord> subBlockList = block.getRecordList();
				for (PluginRecord subBlock : subBlockList)
				{
					List<PluginRecord> cellList = ((PluginGroup) subBlock).getRecordList();
					boolean ignoreCell = false;

					for (PluginRecord cell : cellList)
					{
						if ((cell instanceof PluginGroup))
						{
							if (!ignoreCell)
								continue;
							cell.setIgnore(true);
						}
						else
						{
							ignoreCell = false;
							if (cell.getRecordType().equals("CELL"))
							{
								PluginSubrecord region = null;
								PluginSubrecord XYLoc = null;
								try
								{
									XYLoc = cell.getSubrecord("XCLC");
									region = cell.getSubrecord("XCLR");
								}
								catch (Exception ex)
								{
									if ((Main.debugMode) && (XYLoc != null))
									{
										System.out.printf("ignoreAllExteriorCellsExcept: Cell (%s) does not have an assigned region\n",
												new Object[]
												{ XYLoc.getDisplayData() });
									}
									else if (XYLoc == null)
									{
										System.out.printf("ignoreAllExteriorCellsExcept: Cell %08X does not have XY coordinates\n",
												new Object[]
												{ Integer.valueOf(cell.getFormID()) });
									}
									ignoreCell = true;
								}

								if (region != null)
								{
									String regionIDs = region.getDisplayData();
									String[] regionArray = regionIDs.split(", ");

									ignoreCell = true;
									for (int j = 0; j < regionArray.length; j++)
									{
										int regionID = Integer.parseInt(regionArray[j], 16);
										if (!regionList.contains(Integer.valueOf(regionID)))
											continue;
										ignoreCell = false;
										break;
									}

								}
								else
								{
									if ((Main.debugMode) && (XYLoc != null))
									{
										System.out.printf("ignoreAllExteriorCellsExcept: Cell (%s) does not have an assigned region\n",
												new Object[]
												{ XYLoc.getDisplayData() });
									}
									else if (XYLoc == null)
									{
										System.out.printf("ignoreAllExteriorCellsExcept: Cell %08X does not have XY coordinates\n",
												new Object[]
												{ Integer.valueOf(cell.getFormID()) });
									}
									ignoreCell = true;
								}
								if (!ignoreCell)
									continue;
								cell.setIgnore(true);
							}
						}
					}
				}
			}
		}
	}

	public void ignoreAllExteriorCells(List<Integer> regionList)
	{
		PluginGroup WRLDGroup = getTopGroup("WRLD");
		if (WRLDGroup == null)
			return;
		List<?> recList = WRLDGroup.getRecordList();
		for (int i = 0; i < recList.size(); i += 2)
		{
			PluginGroup worldGroup = (PluginGroup) recList.get(i + 1);
			List<PluginRecord> worldGroupList = worldGroup.getRecordList();

			List<PluginGroup> blockList = new ArrayList<PluginGroup>();

			for (PluginRecord block : worldGroupList)
			{
				if (!(block instanceof PluginGroup))
					continue;
				switch (((PluginGroup) block).getGroupType())
				{
					case 6:
						break;
					case 4:
						blockList.add((PluginGroup) block);
					case 5:
				}
			}

			for (PluginGroup block : blockList)
			{
				List<PluginRecord> subBlockList = block.getRecordList();
				for (PluginRecord subBlock : subBlockList)
				{
					List<PluginRecord> cellList = ((PluginGroup) subBlock).getRecordList();
					boolean ignoreCell = false;

					for (PluginRecord cell : cellList)
					{
						if ((cell instanceof PluginGroup))
						{
							if (!ignoreCell)
								continue;
							cell.setIgnore(true);
						}
						else
						{
							ignoreCell = false;
							if (cell.getRecordType().equals("CELL"))
							{
								PluginSubrecord region = null;
								PluginSubrecord XYLoc = null;
								try
								{
									XYLoc = cell.getSubrecord("XCLC");
									region = cell.getSubrecord("XCLR");
								}
								catch (Exception ex)
								{
									if ((Main.debugMode) && (XYLoc != null))
									{
										System.out.printf("ignoreAllExteriorCells: Cell (%s) does not have an assigned region\n",
												new Object[]
												{ XYLoc.getDisplayData() });
									}
									else if (XYLoc == null)
									{
										System.out.printf("ignoreAllExteriorCells: Cell %08X does not have XY coordinates\n", new Object[]
										{ Integer.valueOf(cell.getFormID()) });
									}
									ignoreCell = true;
								}

								if (region != null)
								{
									String regionIDs = region.getDisplayData();
									String[] regionArray = regionIDs.split(", ");

									ignoreCell = false;
									for (int j = 0; j < regionArray.length; j++)
									{
										int regionID = Integer.parseInt(regionArray[j], 16);
										if (!regionList.contains(Integer.valueOf(regionID)))
											continue;
										ignoreCell = true;
										break;
									}

								}
								else
								{
									if ((Main.debugMode) && (XYLoc != null))
									{
										System.out.printf("ignoreAllExteriorCells: Cell (%s) does not have an assigned region\n",
												new Object[]
												{ XYLoc.getDisplayData() });
									}
									else if (XYLoc == null)
									{
										System.out.printf("ignoreAllExteriorCells: Cell %08X does not have XY coordinates\n", new Object[]
										{ Integer.valueOf(cell.getFormID()) });
									}
									ignoreCell = true;
								}
								if (!ignoreCell)
									continue;
								cell.setIgnore(true);
							}
						}
					}
				}
			}
		}
	}

	public String toString()
	{
		return this.pluginFile.getName();
	}

	public void load(WorkerTask task) throws PluginException, DataFormatException, IOException, InterruptedException
	{
		RandomAccessFile in = null;
		StatusDialog statusDialog = null;
		if (task != null)
		{
			statusDialog = task.getStatusDialog();
			if (statusDialog != null)
			{
				statusDialog.updateMessage("Loading " + this.pluginFile.getName());
			}

		}

		try
		{
			if ((!this.pluginFile.exists()) || (!this.pluginFile.isFile()))
			{
				throw new IOException("Plugin file '" + this.pluginFile.getName() + "' does not exist");
			}
			in = new RandomAccessFile(this.pluginFile, "r");

			this.pluginHeader.read(in);
			int recordCount = this.pluginHeader.getRecordCount();
			int processedCount = 0;
			int currentProgress = 0;

			if (recordCount > 1000)
			{
				this.formList.ensureCapacity(recordCount);
				this.formMap = new HashMap<Integer, FormInfo>(recordCount);
			}

			byte[] prefix = new byte[20];

			int loadCount = 0;
			while (true)
			{
				int count = in.read(prefix);
				if (count == -1)
				{
					break;
				}
				if (count != 20)
				{
					throw new PluginException(this.pluginFile.getName() + ": Group record prefix is too short");
				}
				String type = new String(prefix, 0, 4);
				if (!type.equals("GRUP"))
				{
					throw new PluginException(this.pluginFile.getName() + ": Top-level record is not a group");
				}
				if (prefix[12] != 0)
				{
					throw new PluginException(this.pluginFile.getName() + ": Top-level group type is not 0");
				}
				int length = getInteger(prefix, 4);
				length -= 20;

				PluginGroup group = new PluginGroup(prefix);
				if (Main.debugMode)
				{
					System.out.printf("%s: Loading group %s\n", new Object[]
					{ this.pluginFile.getName(), group.getGroupRecordType() });
				}
				group.load(this.pluginFile, in, length);
				group.updateFormList(this.formList);
				this.groupList.add(group);
				loadCount += group.getRecordCount() + 1;

				if ((task != null) && (WorkerTask.interrupted()))
				{
					throw new InterruptedException("Request canceled");
				}

				processedCount++;
				int newProgress = Math.min(processedCount * 100 / initialGroupList.length, 100);
				if ((statusDialog != null) && (newProgress >= currentProgress + 5))
				{
					currentProgress = newProgress;
					statusDialog.updateProgress(currentProgress);
				}
			}
			int count;
			int selection;
			if (loadCount != recordCount)
			{
				String text = this.pluginFile.getName() + ": Load count " + loadCount + " does not match header count " + recordCount;
				selection = WorkerDialog.showConfirmDialog(task.getParent(), text + ". Do you want to continue?", "Error", 0, 0, false);
				if (selection != 0)
				{
					throw new PluginException(text);
				}

			}

			for (FormInfo info : this.formList)
			{
				info.setPlugin(this);
				this.formMap.put(new Integer(info.getFormID()), info);
			}

			if (this.pluginHeader.getMasterList().size() == 0)
			{
				Integer refFormID = new Integer(20);
				if (this.formMap.get(refFormID) == null)
				{
					FormInfo playerInfo = this.formMap.get(new Integer(7));
					if ((playerInfo != null) && (playerInfo.getRecordType().equals("NPC_")) && (playerInfo.getEditorID().equals("Player")))
					{
						FormInfo playerRefInfo = new FormInfo(null, "REFR", 20, "PlayerREF");
						this.formList.add(playerRefInfo);
						this.formMap.put(refFormID, playerRefInfo);
					}
				}
			}
		}
		finally
		{
			if (in != null)
				in.close();
		}
	}

	public void store(WorkerTask task) throws DataFormatException, IOException, InterruptedException
	{
		File outFile = null;
		RandomAccessFile out = null;
		boolean groupsWritten = false;
		StatusDialog statusDialog = null;
		if (task != null)
		{
			statusDialog = task.getStatusDialog();
			if (statusDialog != null)
			{
				statusDialog.updateMessage("Saving " + this.pluginFile.getName());
			}

		}

		int recordCount = 0;
		for (PluginGroup group : this.groupList)
		{
			group.removeIgnoredRecords();
			int count = group.getRecordCount();
			if (count != 0)
			{
				recordCount += count + 1;
			}
		}
		this.pluginHeader.setRecordCount(recordCount);
		try
		{
			outFile = new File(this.pluginFile.getParent() + Main.fileSeparator + "Gecko.tmp");
			if (outFile.exists())
			{
				outFile.delete();
			}
			out = new RandomAccessFile(outFile, "rw");

			this.pluginHeader.write(out);
			int groupCount = this.groupList.size();
			int processedCount = 0;
			int currentProgress = 0;

			for (PluginGroup group : this.groupList)
			{
				if (!group.isEmpty())
				{
					group.store(out);
				}
				if ((task != null) && (WorkerTask.interrupted()))
				{
					throw new InterruptedException("Request canceled");
				}
				processedCount++;
				int newProgress = processedCount * 100 / groupCount;
				if ((statusDialog != null) && (newProgress >= currentProgress + 5))
				{
					currentProgress = newProgress;
					statusDialog.updateProgress(currentProgress);
				}
			}

			groupsWritten = true;
		}
		finally
		{
			if (out != null)
			{
				out.close();
			}
			if (outFile.exists())
				if (groupsWritten)
				{
					if (this.pluginFile.exists())
					{
						this.pluginFile.delete();
					}
					outFile.renameTo(this.pluginFile);
				}
				else
				{
					outFile.delete();
				}
		}
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.Plugin
 * JD-Core Version:    0.6.0
 */