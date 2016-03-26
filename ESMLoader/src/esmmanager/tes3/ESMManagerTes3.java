package esmmanager.tes3;

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

import esmmanager.common.PluginException;
import esmmanager.common.data.plugin.FormInfo;
import esmmanager.common.data.plugin.IMaster;
import esmmanager.common.data.plugin.PluginGroup;
import esmmanager.common.data.record.Record;
import esmmanager.loader.IESMManager;
import esmmanager.loader.InteriorCELLTopGroup;
import esmmanager.loader.WRLDChildren;
import esmmanager.loader.WRLDTopGroup;
import tools.WeakValueHashMap;

public class ESMManagerTes3 implements IESMManager
{
	private ArrayList<Master> masters = new ArrayList<Master>();

	private static WeakValueHashMap<Integer, Record> loadedRecordsCache = new WeakValueHashMap<Integer, Record>();

	private LinkedHashMap<Integer, FormInfo> idToFormMap = new LinkedHashMap<Integer, FormInfo>();

	private HashMap<String, Integer> edidToFormIdMap = new HashMap<String, Integer>();

	private HashMap<String, List<Integer>> typeToFormIdMap = new HashMap<String, List<Integer>>();

	private float pluginVersion = -1;

	private String pluginName = "";

	public ESMManagerTes3()
	{

	}

	public ESMManagerTes3(String fileName)
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
				System.out.println(
						"Mismatched master version, ESMManager has this " + pluginVersion + " added master has " + master.getVersion());
			}
		}

		if (!masters.contains(master))
		{
			masters.add((Master) master);
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
	 * @see esmmanager.common.data.record.IRecordStore#getRecord(int)
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
				esmmanager.common.data.plugin.PluginRecord pr = getPluginRecord(formID);
				if (pr != null)
				{
					// TODO: do I need to give a real cell id here?
					Record record = new Record(pr);
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
	public esmmanager.common.data.plugin.PluginRecord getInteriorCELL(int formID) throws DataFormatException, IOException, PluginException
	{
		return getMasterForId(formID).getInteriorCELL(formID);
	}

	@Override
	public PluginGroup getInteriorCELLChildren(int formID) throws DataFormatException, IOException, PluginException
	{
		return getMasterForId(formID).getInteriorCELLChildren(formID);
	}

	@Override
	public esmmanager.common.data.plugin.PluginRecord getPluginRecord(int formID) throws PluginException
	{
		return getMasterForId(formID).getPluginRecord(formID);
	}

	@Override
	public esmmanager.common.data.plugin.PluginRecord getWRLD(int formID) throws DataFormatException, IOException, PluginException
	{
		return getMasterForId(formID).getWRLD(formID);
	}

	@Override
	public WRLDChildren getWRLDChildren(int formID)
	{
		return getMasterForId(formID).getWRLDChildren(formID);
	}

	@Override
	public esmmanager.common.data.plugin.PluginRecord getWRLDExtBlockCELL(int wrldFormId, int x, int y)
			throws DataFormatException, IOException, PluginException
	{
		IMaster master = getMasterForId(wrldFormId);
		return master.getWRLDExtBlockCELL(wrldFormId, x, y);
	}

	@Override
	public esmmanager.common.data.plugin.PluginGroup getWRLDExtBlockCELLChildren(int wrldFormId, int x, int y)
			throws DataFormatException, IOException, PluginException
	{
		IMaster master = getMasterForId(wrldFormId);
		return master.getWRLDExtBlockCELLChildren(wrldFormId, x, y);
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

	public int convertNameRefToId(String str)
	{
		for (Master m : masters)
		{
			int id = m.convertNameRefToId(str);
			if (id != -1)
				return id;
		}

		return -1;
	}

}
