package esmLoader.tes3;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.DataFormatException;

import esmLoader.EsmFileLocations;
import esmLoader.common.PluginException;
import esmLoader.common.data.plugin.FormInfo;
import esmLoader.common.data.plugin.IMaster;
import esmLoader.common.data.plugin.PluginGroup;
import esmLoader.common.data.plugin.PluginSubrecord;
import esmLoader.loader.InteriorCELLTopGroup;
import esmLoader.loader.WRLDChildren;
import esmLoader.loader.WRLDTopGroup;

/**
 * This is a copy of the master file in data package, however it holds onto a
 * copy of all loaded data for everything other than the WRLD and CELL values,
 * which is simply indexes down to the subblock level
 *
 * @author Administrator
 *
 */
public class Master implements IMaster
{
	private File masterFile;

	private RandomAccessFile in;

	private PluginHeader masterHeader;

	private ArrayList<PluginRecord> records = new ArrayList<PluginRecord>();

	public Master(File masterFile)
	{
		this.masterFile = masterFile;
		masterHeader = new PluginHeader();
	}

	@Override
	public String getName()
	{
		return masterHeader.getPluginFileName();
	}

	@Override
	public float getVersion()
	{
		return masterHeader.getPluginVersion();
	}

	@Override
	public int getMinFormId()
	{
		return -1;
	}

	@Override
	public int getMaxFormId()
	{
		return -1;
	}

	public ArrayList<PluginRecord> getRecords()
	{
		return records;
	}

	public synchronized void load() throws PluginException, DataFormatException, IOException
	{
		if (!masterFile.exists() || !masterFile.isFile())
			throw new IOException("Master file '" + masterFile.getAbsolutePath() + "' does not exist");

		in = new RandomAccessFile(masterFile, "r");

		masterHeader.load(masterFile.getName(), in);

		while (in.getFilePointer() < in.length())
		{
			PluginRecord r = new PluginRecord();
			r.load(masterFile.getName(), in);
			records.add(r);
		}

	}

	public static void main(String[] args)
	{
		String generalEsmFile = EsmFileLocations.getGeneralEsmFile();

		System.out.println("loading file " + generalEsmFile);

		File pluginFile = new File(generalEsmFile);
		Master plugin = new Master(pluginFile);
		try
		{
			plugin.load();

			for (PluginRecord r : plugin.getRecords())
			{
				System.out.println("" + r);
				for (PluginSubrecord sr : r.getSubrecords())
				{
					System.out.println("\t" + sr);
				}
			}
			System.out.println("done");
		}
		catch (PluginException e)
		{
			e.printStackTrace();
		}
		catch (DataFormatException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public WRLDTopGroup getWRLDTopGroup()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InteriorCELLTopGroup getInteriorCELLTopGroup()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PluginRecord getWRLD(int formID) throws DataFormatException, IOException, PluginException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WRLDChildren getWRLDChildren(int formID)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getWRLDExtBlockCELLId(int wrldFormId, int x, int y)
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public PluginRecord getWRLDExtBlockCELL(int formID) throws DataFormatException, IOException,
			PluginException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PluginGroup getWRLDExtBlockCELLChildren(int formID) throws DataFormatException, IOException, PluginException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PluginRecord getInteriorCELL(int formID) throws DataFormatException, IOException, PluginException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PluginGroup getInteriorCELLChildren(int formID) throws DataFormatException, IOException, PluginException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PluginRecord getPluginRecord(int formID) throws PluginException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Integer, FormInfo> getFormMap()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Integer> getEdidToFormIdMap()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, List<Integer>> getTypeToFormIdMap()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Integer> getAllFormIds()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<String> getAllEdids()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Integer> getAllInteriorCELLFormIds()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Integer> getAllWRLDTopGroupFormIds()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Integer> getWRLDExtBlockCELLFormIds()
	{
		// TODO Auto-generated method stub
		return null;
	}

}
