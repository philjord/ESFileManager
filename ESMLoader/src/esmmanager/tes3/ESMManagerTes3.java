package esmmanager.tes3;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.DataFormatException;

import com.frostwire.util.SparseArray;

import esmmanager.common.PluginException;
import esmmanager.common.data.plugin.FormInfo;
import esmmanager.common.data.plugin.IMaster;
import esmmanager.common.data.plugin.PluginGroup;
import esmmanager.common.data.record.Record;
import esmmanager.loader.CELLDIALPointer;
import esmmanager.loader.InteriorCELLTopGroup;
import esmmanager.loader.WRLDChildren;
import esmmanager.loader.WRLDTopGroup;

public class ESMManagerTes3 implements IESMManagerTes3
{
	private ArrayList<IMasterTes3> masters = new ArrayList<IMasterTes3>();
	private float pluginVersion = -1;
	private String pluginName = "";

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
			masters.add((IMasterTes3) master);
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
	 * No more cache!!! duplicates happily handed out
	 */
	//TODO: is this bad? should a PluinRecord be instantly swapped to a Record somehow?
	// should I dump Record completely now?
	@Override
	public Record getRecord(int formID)
	{

		try
		{
			esmmanager.common.data.plugin.PluginRecord pr = getPluginRecord(formID);
			if (pr != null)
			{
				Record record = new Record(pr);
				return record;
			}
		}
		catch (PluginException e)
		{
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Record getRecord(String edidId)
	{
		int formId = convertNameRefToId(edidId);
		if (formId != -1)
		{
			return getRecord(formId);
		}
		else
		{
			System.out.println("-1 form id for " + edidId);
			return null;
		}
	}

	/** 
	 * Note very slow, but not used often
	 */
	@Override
	public int[] getAllFormIds()
	{
		SparseArray<FormInfo> idToFormMap = new SparseArray<FormInfo>();
		for (IMaster m : masters)
		{
			idToFormMap.putAll(m.getFormMap());
		}
		return idToFormMap.keySet();
	}

	/** 
	 * Note very slow, but not used often
	 */
	@Override
	public SparseArray<FormInfo> getFormMap()
	{
		SparseArray<FormInfo> idToFormMap = new SparseArray<FormInfo>();
		for (IMaster m : masters)
		{
			idToFormMap.putAll(m.getFormMap());
		}
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
	public List<CELLDIALPointer> getAllInteriorCELLFormIds()
	{
		ArrayList<CELLDIALPointer> ret = new ArrayList<CELLDIALPointer>();
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
	public PluginGroup getInteriorCELLPersistentChildren(int formID) throws DataFormatException, IOException, PluginException
	{
		return getMasterForId(formID).getInteriorCELLPersistentChildren(formID);
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

	@Override
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

	@Override
	public void clearMasters()
	{
		masters.clear();
		pluginVersion = -1;
	}

	@Override
	public int convertNameRefToId(String str)
	{
		for (IMasterTes3 m : masters)
		{
			int id = m.convertNameRefToId(str);
			if (id != -1)
				return id;
		}

		return -1;
	}

}
