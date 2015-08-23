package esmLoader.loader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.DataFormatException;

import esmLoader.common.PluginException;
import esmLoader.common.data.plugin.FormInfo;
import esmLoader.common.data.plugin.Master;
import esmLoader.common.data.plugin.PluginGroup;
import esmLoader.common.data.plugin.PluginRecord;
import esmLoader.common.data.record.Record;
import tools.WeakValueHashMap;

public class ESMManager implements IESMManager
{
	private ArrayList<Master> masters = new ArrayList<Master>();

	private static WeakValueHashMap<Integer, Record> loadedRecordsCache = new WeakValueHashMap<Integer, Record>();

	private LinkedHashMap<Integer, FormInfo> idToFormMap = new LinkedHashMap<Integer, FormInfo>();

	private HashMap<String, Integer> edidToFormIdMap = new HashMap<String, Integer>();

	private HashMap<String, List<Integer>> typeToFormIdMap = new HashMap<String, List<Integer>>();

	private float pluginVersion = -1;

	private String pluginName = "";

	public ESMManager()
	{

	}

	public void addMaster(Master master)
	{
		if (pluginVersion == -1)
		{
			pluginVersion = master.getMasterHeader().getVersion();
			pluginName = master.getMasterHeader().getName();
		}
		else
		{
			if (master.getMasterHeader().getVersion() != pluginVersion)
			{
				System.out.println("Mismatched master version, ESMMAnger has this " + pluginVersion + " added master has "
						+ master.getMasterHeader().getVersion());
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

	public float getVersion()
	{
		return pluginVersion;
	}

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

	public Map<String, List<Integer>> getTypeToFormIdMap()
	{
		return typeToFormIdMap;
	}

	@Override
	public Map<Integer, FormInfo> getFormMap()
	{
		return idToFormMap;
	}

	public List<WRLDTopGroup> getWRLDTopGroups()
	{
		ArrayList<WRLDTopGroup> ret = new ArrayList<WRLDTopGroup>();
		for (Master m : masters)
		{
			ret.add(m.getWRLDTopGroup());
		}
		return ret;
	}

	@Override
	public Set<Integer> getAllWRLDTopGroupFormIds()
	{
		TreeSet<Integer> ret = new TreeSet<Integer>();
		for (Master m : masters)
		{
			ret.addAll(m.getAllWRLDTopGroupFormIds());
		}
		return ret;
	}

	public List<InteriorCELLTopGroup> getInteriorCELLTopGroups()
	{
		ArrayList<InteriorCELLTopGroup> ret = new ArrayList<InteriorCELLTopGroup>();
		for (Master m : masters)
		{
			ret.add(m.getInteriorCELLTopGroup());
		}
		return ret;
	}

	@Override
	public Set<Integer> getAllInteriorCELLFormIds()
	{
		TreeSet<Integer> ret = new TreeSet<Integer>();
		for (Master m : masters)
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
		for (Master m : masters)
		{
			ret.addAll(m.getWRLDExtBlockCELLFormIds());
		}
		return ret;
	}

	@Override
	public PluginRecord getWRLDExtBlockCELL(int formID) throws DataFormatException, IOException, PluginException
	{
		Master master = getMasterForId(formID);
		return master.getWRLDExtBlockCELL(formID);
	}

	@Override
	public PluginGroup getWRLDExtBlockCELLChildren(int formID) throws DataFormatException, IOException, PluginException
	{
		Master master = getMasterForId(formID);
		return master.getWRLDExtBlockCELLChildren(formID);
	}

	@Override
	public int getWRLDExtBlockCELLId(int wrldFormId, int x, int y)
	{
		Master master = getMasterForId(wrldFormId);
		return master.getWRLDExtBlockCELLId(wrldFormId, x, y);
	}

	private Master getMasterForId(int formID)
	{
		for (Master m : masters)
		{
			if (formID >= m.getMinFormId() && formID <= m.getMaxFormId())
			{
				return m;
			}
		}

		System.out.println("no master found for form id " + formID);
		return null;
	}

	/**
	 * THIS IS A UTIL METHOD PUT IT SOMEWHERE ELSE???
	 * PluginGroup.CELL_PERSISTENT
	 * PluginGroup.CELL_TEMPORARY
	 * PluginGroup.CELL_DISTANT
	 * @param cellChildren
	 * @param type
	 * @return
	 */
	public static List<Record> getChildren(PluginGroup cellChildren, int type)
	{
		List<Record> ret = new ArrayList<Record>();
		if (cellChildren != null && cellChildren.getRecordList() != null)
		{
			for (PluginRecord pgr : cellChildren.getRecordList())
			{
				PluginGroup pg = (PluginGroup) pgr;

				if (pg.getGroupType() == type)
				{
					for (PluginRecord pr : pg.getRecordList())
					{
						Record record = new Record(pr, -1);
						ret.add(record);
					}
				}
			}
		}
		return ret;
	}

	//Convinence staic singleton system below 
	private static IESMManager esmManager = null;

	/**
	 * Note synchronized because the new statement and the load statement are two different calls.
	 * Note currently this convience only loads the ESM_FILE file bt others canbe added easily
	 */
	public synchronized static void ensureMasterLoaded(String fileName)
	{
		if (esmManager == null)
		{
			loadMaster(fileName);
		}
	}

	public synchronized static void loadMaster(String fileName)
	{
		File m = new File(fileName);

		try
		{
			Master master = new Master(m);
			esmManager = new ESMManager();
			master.load();
			esmManager.addMaster(master);
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

	/**
	 * This will auto load ESM_FILE first
	 * @param fileNameToAdd
	 */
	public synchronized void addMaster(String fileNameToAdd)
	{

		try
		{
			File m = new File(fileNameToAdd);
			Master master = new Master(m);
			master.load();
			esmManager.addMaster(master);
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

	//TODO this static esm file handing out system is garbage isn't it?
	public static IESMManager getESMManager(String fileName)
	{
		ensureMasterLoaded(fileName);
		return esmManager;
	}

	public static void clearESMManager()
	{
		if (esmManager != null)
		{
			esmManager.clearMasters();
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
		List<WRLDTopGroup> WRLDTopGroups = esmManager.getWRLDTopGroups();
		for (WRLDTopGroup WRLDTopGroup : WRLDTopGroups)
		{
			for (PluginRecord wrld : WRLDTopGroup.WRLDByFormId.values())
			{
				//WRLD wrld = new WRLD(wrld);
				WRLDChildren children = esmManager.getWRLDChildren(wrld.getFormID());
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

		List<InteriorCELLTopGroup> interiorCELLTopGroups = esmManager.getInteriorCELLTopGroups();
		for (InteriorCELLTopGroup interiorCELLTopGroup : interiorCELLTopGroups)
		{
			for (CELLPointer cp : interiorCELLTopGroup.interiorCELLByFormId.values())
			{
				try
				{
					PluginRecord cell = esmManager.getInteriorCELL(cp.formId);

					PluginGroup cellChildGroups = esmManager.getInteriorCELLChildren(cell.getFormID());

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

}
