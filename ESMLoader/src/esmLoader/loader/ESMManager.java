package esmLoader.loader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.DataFormatException;

import tools.WeakValueHashMap;
import esmLoader.common.PluginException;
import esmLoader.common.data.plugin.FormInfo;
import esmLoader.common.data.plugin.IMaster;
import esmLoader.common.data.plugin.Master;
import esmLoader.common.data.plugin.PluginGroup;
import esmLoader.common.data.plugin.PluginRecord;
import esmLoader.common.data.record.Record;
import esmLoader.tes3.ESMManagerTes3;

//TODO: this is really an ESMMaster manager (or master plus plugin? esp? for morrowind)

// also the multi master part ( and cacher)  is really very seperate from the ensuremaster and get esm manager bit so perhaps time for 2?
public class ESMManager implements IESMManager
{
	private ArrayList<IMaster> masters = new ArrayList<IMaster>();

	private static WeakValueHashMap<Integer, Record> loadedRecordsCache = new WeakValueHashMap<Integer, Record>();

	private LinkedHashMap<Integer, FormInfo> idToFormMap = new LinkedHashMap<Integer, FormInfo>();

	private HashMap<String, Integer> edidToFormIdMap = new HashMap<String, Integer>();

	private HashMap<String, List<Integer>> typeToFormIdMap = new HashMap<String, List<Integer>>();

	private float pluginVersion = -1;

	private String pluginName = "";

	public ESMManager()
	{

	}

	public ESMManager(String fileName)
	{
		File m = new File(fileName);

		try
		{
			Master master = new Master(m);
			master.load();
			addMaster(master);
		}
		catch (PluginException e1)
		{
			e1.printStackTrace();
		}
		catch (DataFormatException e1)
		{
			e1.printStackTrace();
		}
		catch (IOException e1)
		{
			e1.printStackTrace();
		}
	}

	@Override
	public void addMaster(IMaster master)
	{
		if (pluginVersion == -1)
		{
			pluginVersion = master.getVersion();
			pluginName = master.getName();
		}
		else
		{
			if (master.getVersion() != pluginVersion)
			{
				System.out.println("Mismatched master version, ESMManager has this " + pluginVersion + " added master has "
						+ master.getVersion());
			}
		}

		if (!masters.contains(master))
		{
			masters.add(master);
			idToFormMap.putAll(master.getFormMap());
			edidToFormIdMap.putAll(master.getEdidToFormIdMap());
			typeToFormIdMap.putAll(master.getTypeToFormIdMap());
		}
		else
		{
			System.out.println("why add same master twice? " + master);
		}
	}

	@Override
	public float getVersion()
	{
		return pluginVersion;
	}

	@Override
	public String getName()
	{
		return pluginName;
	}

	/**
	 * This is a caching call that will hang on to a weak reference of whatever is handed out, it should really be the only
	 * access to records from an esm that is used at all.
	 * @see esmLoader.common.data.record.IRecordStore#getRecord(int)
	 */
	@Override
	public Record getRecord(int formID)
	{
		//Check the cache for an instance first
		Integer key = new Integer(formID);

		Record ret_val = loadedRecordsCache.get(key);

		if (ret_val == null)
		{
			try
			{
				PluginRecord pr = getPluginRecord(formID);
				if (pr != null)
				{
					// TODO: do I need to give a real cell id here?
					Record record = new Record(pr, -1);
					loadedRecordsCache.put(key, record);
					return record;
				}
			}
			catch (PluginException e)
			{
				e.printStackTrace();
			}
			return null;
		}
		return ret_val;

	}

	@Override
	public Record getRecord(String edidId)
	{
		Integer formId = edidToFormIdMap.get(edidId);
		if (formId != null)
		{
			return getRecord(formId);
		}
		else
		{
			System.out.println("null form for " + edidId);
			return null;
		}
	}

	@Override
	public Set<String> getAllEdids()
	{
		return edidToFormIdMap.keySet();
	}

	@Override
	public Set<Integer> getAllFormIds()
	{
		return idToFormMap.keySet();
	}

	@Override
	public Map<String, Integer> getEdidToFormIdMap()
	{
		return edidToFormIdMap;
	}

	@Override
	public Map<String, List<Integer>> getTypeToFormIdMap()
	{
		return typeToFormIdMap;
	}

	@Override
	public Map<Integer, FormInfo> getFormMap()
	{
		return idToFormMap;
	}

	@Override
	public List<WRLDTopGroup> getWRLDTopGroups()
	{
		ArrayList<WRLDTopGroup> ret = new ArrayList<WRLDTopGroup>();
		for (IMaster m : masters)
		{
			ret.add(m.getWRLDTopGroup());
		}
		return ret;
	}

	@Override
	public Set<Integer> getAllWRLDTopGroupFormIds()
	{
		TreeSet<Integer> ret = new TreeSet<Integer>();
		for (IMaster m : masters)
		{
			ret.addAll(m.getAllWRLDTopGroupFormIds());
		}
		return ret;
	}

	@Override
	public List<InteriorCELLTopGroup> getInteriorCELLTopGroups()
	{
		ArrayList<InteriorCELLTopGroup> ret = new ArrayList<InteriorCELLTopGroup>();
		for (IMaster m : masters)
		{
			ret.add(m.getInteriorCELLTopGroup());
		}
		return ret;
	}

	@Override
	public Set<Integer> getAllInteriorCELLFormIds()
	{
		TreeSet<Integer> ret = new TreeSet<Integer>();
		for (IMaster m : masters)
		{
			ret.addAll(m.getAllInteriorCELLFormIds());
		}
		return ret;
	}

	@Override
	public PluginRecord getInteriorCELL(int formID) throws DataFormatException, IOException, PluginException
	{
		return getMasterForId(formID).getInteriorCELL(formID);
	}

	@Override
	public PluginGroup getInteriorCELLChildren(int formID) throws DataFormatException, IOException, PluginException
	{
		return getMasterForId(formID).getInteriorCELLChildren(formID);
	}

	@Override
	public PluginRecord getPluginRecord(int formID) throws PluginException
	{
		return getMasterForId(formID).getPluginRecord(formID);
	}

	@Override
	public PluginRecord getWRLD(int formID) throws DataFormatException, IOException, PluginException
	{
		return getMasterForId(formID).getWRLD(formID);
	}

	@Override
	public WRLDChildren getWRLDChildren(int formID)
	{
		return getMasterForId(formID).getWRLDChildren(formID);
	}

	@Override
	public Set<Integer> getWRLDExtBlockCELLFormIds()
	{
		TreeSet<Integer> ret = new TreeSet<Integer>();
		for (IMaster m : masters)
		{
			ret.addAll(m.getWRLDExtBlockCELLFormIds());
		}
		return ret;
	}

	@Override
	public PluginRecord getWRLDExtBlockCELL(int formID) throws DataFormatException, IOException, PluginException
	{
		IMaster master = getMasterForId(formID);
		return master.getWRLDExtBlockCELL(formID);
	}

	@Override
	public PluginGroup getWRLDExtBlockCELLChildren(int formID) throws DataFormatException, IOException, PluginException
	{
		IMaster master = getMasterForId(formID);
		return master.getWRLDExtBlockCELLChildren(formID);
	}

	@Override
	public int getWRLDExtBlockCELLId(int wrldFormId, int x, int y)
	{
		IMaster master = getMasterForId(wrldFormId);
		return master.getWRLDExtBlockCELLId(wrldFormId, x, y);
	}

	@Override
	public WRLDTopGroup getWRLDTopGroup()
	{
		throw new UnsupportedOperationException("ESMManager does not have a single top group");
	}

	@Override
	public InteriorCELLTopGroup getInteriorCELLTopGroup()
	{
		throw new UnsupportedOperationException("ESMManager does not have a single top group");
	}

	@Override
	public int getMinFormId()
	{
		throw new UnsupportedOperationException("ESMManager does not have a min form id");
	}

	@Override
	public int getMaxFormId()
	{
		throw new UnsupportedOperationException("ESMManager does not have a max form id");
	}

	private IMaster getMasterForId(int formID)
	{
		for (IMaster m : masters)
		{
			if (formID >= m.getMinFormId() && formID <= m.getMaxFormId())
			{
				return m;
			}
		}

		System.out.println("no master found for form id " + formID);
		return null;
	}

	public synchronized void addMaster(String fileNameToAdd)
	{
		try
		{
			File m = new File(fileNameToAdd);
			Master master = new Master(m);
			master.load();
			addMaster(master);
		}
		catch (PluginException e1)
		{
			e1.printStackTrace();
		}
		catch (DataFormatException e1)
		{
			e1.printStackTrace();
		}
		catch (IOException e1)
		{
			e1.printStackTrace();
		}
	}

	public void clearMasters()
	{
		masters.clear();

		loadedRecordsCache.clear();

		idToFormMap.clear();

		edidToFormIdMap.clear();

		pluginVersion = -1;

	}

	/**
	 
	 * @param formInNewCellId
	 */
	public int getCellIdOfPersistentTarget(int formId)
	{
		//I need to pre load ALL persistent children for all CELLS and keep them
		List<WRLDTopGroup> WRLDTopGroups = getWRLDTopGroups();
		for (WRLDTopGroup WRLDTopGroup : WRLDTopGroups)
		{
			for (PluginRecord wrld : WRLDTopGroup.WRLDByFormId.values())
			{
				//WRLD wrld = new WRLD(wrld);
				WRLDChildren children = getWRLDChildren(wrld.getFormID());
				PluginRecord cell = children.getCell();
				if (cell != null)
				{
					PluginGroup cellChildGroups = children.getCellChildren();

					if (cellChildGroups != null && cellChildGroups.getRecordList() != null)
					{
						for (PluginRecord pgr : cellChildGroups.getRecordList())
						{
							PluginGroup pg = (PluginGroup) pgr;

							for (PluginRecord pr : pg.getRecordList())
							{
								if (pr.getFormID() == formId)
								{
									return wrld.getFormID();
								}
							}
						}
					}
				}
			}
		}

		List<InteriorCELLTopGroup> interiorCELLTopGroups = getInteriorCELLTopGroups();
		for (InteriorCELLTopGroup interiorCELLTopGroup : interiorCELLTopGroups)
		{
			for (CELLPointer cp : interiorCELLTopGroup.interiorCELLByFormId.values())
			{
				try
				{
					PluginRecord cell = getInteriorCELL(cp.formId);

					PluginGroup cellChildGroups = getInteriorCELLChildren(cell.getFormID());

					if (cellChildGroups != null && cellChildGroups.getRecordList() != null)
					{
						for (PluginRecord pgr : cellChildGroups.getRecordList())
						{
							PluginGroup pg = (PluginGroup) pgr;

							for (PluginRecord pr : pg.getRecordList())
							{
								if (pr.getFormID() == formId)
								{
									return cell.getFormID();
								}
							}
						}
					}

				}
				catch (DataFormatException e)
				{
					e.printStackTrace();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
				catch (PluginException e)
				{
					e.printStackTrace();
				}
			}
		}
		return -1;
	}

	public static IESMManager getESMManager(String esmFile)
	{
		File testFile = new File(esmFile);
		if (!testFile.exists() || !testFile.isFile())
		{
			System.out.println("Master file '" + testFile.getAbsolutePath() + "' does not exist");

		}
		else
		{
			try
			{
				RandomAccessFile in = new RandomAccessFile(testFile, "r");
				try
				{
					byte[] prefix = new byte[16];
					int count = in.read(prefix);
					if (count == 16)
					{
						String recordType = new String(prefix, 0, 4);
						if (recordType.equals("TES3"))
						{
							System.out.println("tes3 file!");
							in.close();
							return new ESMManagerTes3(esmFile);
						}
					}
					in.close();
				}
				catch (IOException e)
				{
					//fall through, try tes4
				}

				//assume tes4
				return new ESMManager(esmFile);
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
			}
		}

		return null;
	}

}
