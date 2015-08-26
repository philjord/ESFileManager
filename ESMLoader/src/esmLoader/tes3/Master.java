package esmLoader.tes3;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

	private LinkedHashMap<Integer, FormInfo> idToFormMap;

	private HashMap<String, Integer> edidToFormIdMap;

	private HashMap<String, List<Integer>> typeToFormIdMap;

	private static int currentFormId = 0;

	private int minFormId = Integer.MAX_VALUE;

	private int maxFormId = Integer.MIN_VALUE;

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

	public int getMinFormId()
	{
		return minFormId;
	}

	public int getMaxFormId()
	{
		return maxFormId;
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
	public Map<Integer, FormInfo> getFormMap()
	{
		return idToFormMap;
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

	public ArrayList<PluginRecord> getRecords()
	{
		return records;
	}

	public synchronized void load() throws PluginException, IOException
	{
		if (!masterFile.exists() || !masterFile.isFile())
			throw new IOException("Master file '" + masterFile.getAbsolutePath() + "' does not exist");

		in = new RandomAccessFile(masterFile, "r");

		masterHeader.load(masterFile.getName(), in);

		List<FormInfo> formList = new ArrayList<FormInfo>();

		while (in.getFilePointer() < in.length())
		{
			PluginRecord record = new PluginRecord(currentFormId++);//note ++
			record.load(masterFile.getName(), in);
			records.add(record);
			formList.add(new FormInfo(record.getRecordType(), record.getFormID(), record.getEditorID(), record));
		}

		idToFormMap = new LinkedHashMap<Integer, FormInfo>();
		edidToFormIdMap = new HashMap<String, Integer>();
		typeToFormIdMap = new HashMap<String, List<Integer>>();

		for (FormInfo info : formList)
		{
			int formID = info.getFormID();
			idToFormMap.put(new Integer(formID), info);

			if (info.getEditorID() != null && info.getEditorID().length() > 0)
			{
				edidToFormIdMap.put(info.getEditorID(), formID);
			}

			List<Integer> typeList = typeToFormIdMap.get(info.getRecordType());
			if (typeList == null)
			{
				typeList = new ArrayList<Integer>();
				typeToFormIdMap.put(info.getRecordType(), typeList);
			}
			typeList.add(info.getFormID());

		}

		// now establish min and max form id range
		minFormId = 0;
		maxFormId = currentFormId - 1;
	}

	@Override
	public PluginRecord getPluginRecord(int formID) throws PluginException
	{
		FormInfo formInfo = idToFormMap.get(new Integer(formID));

		if (formInfo == null)
		{
			throw new PluginException("" + masterFile.getName() + ": Record " + formID + " not found, it may be a CELL or WRLD record");
		}

		return (PluginRecord) formInfo.getPluginRecord();
	}

	@Override
	public WRLDTopGroup getWRLDTopGroup()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public InteriorCELLTopGroup getInteriorCELLTopGroup()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public PluginRecord getWRLD(int formID) throws DataFormatException, IOException, PluginException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public WRLDChildren getWRLDChildren(int formID)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int getWRLDExtBlockCELLId(int wrldFormId, int x, int y)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public PluginRecord getWRLDExtBlockCELL(int formID) throws DataFormatException, IOException, PluginException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public PluginGroup getWRLDExtBlockCELLChildren(int formID) throws DataFormatException, IOException, PluginException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public PluginRecord getInteriorCELL(int formID) throws DataFormatException, IOException, PluginException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public PluginGroup getInteriorCELLChildren(int formID) throws DataFormatException, IOException, PluginException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<Integer> getAllInteriorCELLFormIds()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<Integer> getAllWRLDTopGroupFormIds()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<Integer> getWRLDExtBlockCELLFormIds()
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * For quick testing only
	 * @param args
	 */
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
				if (r.getRecordType().equals("LEVI"))
				{
					System.out.println("" + r);
					for (PluginSubrecord sr : r.getSubrecords())
					{
						System.out.println("\t" + sr.displaySubrecord());
					}
				}
			}
			System.out.println("done");
		}
		catch (PluginException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

}
