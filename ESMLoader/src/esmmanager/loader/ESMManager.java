package esmmanager.loader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.DataFormatException;

import com.frostwire.util.SparseArray;

import esmmanager.common.PluginException;
import esmmanager.common.data.plugin.FormInfo;
import esmmanager.common.data.plugin.IMaster;
import esmmanager.common.data.plugin.Master;
import esmmanager.common.data.plugin.PluginGroup;
import esmmanager.common.data.plugin.PluginRecord;
import esmmanager.common.data.record.Record;
import esmmanager.tes3.ESMManagerTes3;

//TODO: this is really an ESMMaster manager (or master plus plugin? esp? for morrowind)

// also the multi master part ( and cacher)  is really very seperate from the ensuremaster
//and get esm manager bit so perhaps time for 2?

public class ESMManager implements IESMManager
{
	public static boolean USE_FILE_MAPS = true;
	public static boolean USE_MINI_CHANNEL_MAPS = false;
	public static boolean USE_NON_NATIVE_ZIP = false;// true=slower but no native calls

	private ArrayList<IMaster> masters = new ArrayList<IMaster>();

	private float pluginVersion = -1;

	private String pluginName = "";

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
				System.out.println(
						"Mismatched master version, ESMManager has this " + pluginVersion + " added master has " + master.getVersion());
			}
		}

		if (!masters.contains(master))
		{
			masters.add(master);
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
	@Override
	public Record getRecord(int formID)
	{
		try
		{
			return getPluginRecord(formID);
		}
		catch (PluginException e)
		{
			e.printStackTrace();
		}
		return null;

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
	public PluginGroup getInteriorCELLPersistentChildren(int formID) throws DataFormatException, IOException, PluginException
	{
		return getMasterForId(formID).getInteriorCELLPersistentChildren(formID);
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
	public PluginRecord getWRLDExtBlockCELL(int wrldFormId, int x, int y) throws DataFormatException, IOException, PluginException
	{
		return getMasterForId(wrldFormId).getWRLDExtBlockCELL(wrldFormId, x, y);
	}

	@Override
	public PluginGroup getWRLDExtBlockCELLChildren(int wrldFormId, int x, int y) throws DataFormatException, IOException, PluginException
	{
		return getMasterForId(wrldFormId).getWRLDExtBlockCELLChildren(wrldFormId, x, y);
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
		pluginVersion = -1;
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
				// Notice as I'm only pulling 16 bytes the mapped byte buffer is a bad idea
				//RandomAccessFile in = new MappedByteBufferRAF(testFile, "r");
				try
				{
					byte[] prefix = new byte[16];
					int count = in.read(prefix);
					if (count == 16)
					{
						String recordType = new String(prefix, 0, 4);
						if (recordType.equals("TES3"))
						{
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
