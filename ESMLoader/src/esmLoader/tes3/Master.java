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
import java.util.TreeSet;
import java.util.zip.DataFormatException;

import tools.io.ESMByteConvert;
import esmLoader.EsmFileLocations;
import esmLoader.common.PluginException;
import esmLoader.common.data.plugin.FormInfo;
import esmLoader.common.data.plugin.IMaster;
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

	// used to indicate the single morrowind world, added first
	public static int wrldFormId = 0;

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

	public static int getNextFormId()
	{
		return currentFormId++;
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

		//add a single wrld indicator, to indicate the single morrowind world, id MUST be wrldFormId (0)!
		PluginRecord wrldRecord = new PluginRecord(currentFormId++, "WRLD", "MorrowindWorld");
		formList.add(new FormInfo(wrldRecord.getRecordType(), wrldRecord.getFormID(), wrldRecord.getEditorID(), wrldRecord));

		while (in.getFilePointer() < in.length())
		{
			PluginRecord record = new PluginRecord(getNextFormId());
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

			// 1 length are single 0's
			if (info.getEditorID() != null && info.getEditorID().length() > 1)
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
		if (formID == wrldFormId)
		{
			PluginRecord wrld = getPluginRecord(formID);
			// loaded as a cell so we'll fake it up
			wrld.getSubrecords().add(new PluginSubrecord("CELL", "NAME", "Morrowind".getBytes()));
			wrld.getSubrecords().add(new PluginSubrecord("CELL", "DATA", new byte[12]));
			return wrld;
		}
		// no message as null indicates a non world formid
		return null;
	}

	@Override
	public WRLDChildren getWRLDChildren(int formID)
	{
		return null;
	}

	@Override
	public int getWRLDExtBlockCELLId(int wrldFormId2, int x, int y)
	{
		if (wrldFormId2 != wrldFormId)
		{
			new Throwable("bad morrowind world id! " + wrldFormId2).printStackTrace();
		}

		List<Integer> cells = typeToFormIdMap.get("CELL");
		// only those marked interor
		for (int id : cells)
		{
			try
			{
				PluginRecord cell = getInteriorCELL(id);

				PluginSubrecord data = cell.getSubrecords().get(1);
				long flags = getDATAFlags(data);
				// interior marker
				if ((flags & 0x1) == 0)
				{
					if (getDATAx(data) == x && getDATAy(data) == y)
						return id;
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

		return -1;
	}

	@Override
	public PluginRecord getWRLDExtBlockCELL(int formID) throws DataFormatException, IOException, PluginException
	{
		return getPluginRecord(formID);
	}

	@Override
	public PluginGroup getWRLDExtBlockCELLChildren(int formID) throws DataFormatException, IOException, PluginException
	{

		// make up a fake group and add all children from the cell
		PluginRecord cellRecord = getPluginRecord(formID);
		CELLPluginGroup cell = new CELLPluginGroup(cellRecord);

		PluginSubrecord data = cellRecord.getSubrecords().get(1);

		int x = getDATAx(data);
		int y = getDATAy(data);

		List<Integer> lands = typeToFormIdMap.get("LAND");
		
		for (int id : lands)
		{

			PluginRecord land = getPluginRecord(id);
			PluginSubrecord intv = land.getSubrecords().get(0);

			int CellX = ESMByteConvert.extractInt(intv.getSubrecordData(), 0);
			int CellY = ESMByteConvert.extractInt(intv.getSubrecordData(), 4);

			if (CellX == x && CellY == y)
			{
				//System.out.println("found a land for a cell! " + x + " " + y);
				cell.addPluginRecord(land);
			}
		}

		return cell;
	}

	@Override
	public PluginRecord getInteriorCELL(int formID) throws DataFormatException, IOException, PluginException
	{
		return getPluginRecord(formID);
	}

	@Override
	public PluginGroup getInteriorCELLChildren(int formID) throws DataFormatException, IOException, PluginException
	{
		// make up a fake group and add all children from the cell
		PluginRecord cellRecord = getPluginRecord(formID);
		CELLPluginGroup cell = new CELLPluginGroup(cellRecord);
		return cell;

	}

	@Override
	public Set<Integer> getAllInteriorCELLFormIds()
	{
		TreeSet<Integer> ret = new TreeSet<Integer>();
		List<Integer> cells = typeToFormIdMap.get("CELL");
		// only those marked interor
		for (int id : cells)
		{
			try
			{
				PluginRecord cell = getInteriorCELL(id);

				PluginSubrecord data = cell.getSubrecords().get(1);
				long flags = getDATAFlags(data);
				// interior marker
				if ((flags & 0x1) != 0)
				{
					ret.add(id);
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

		return ret;
	}

	@Override
	public Set<Integer> getAllWRLDTopGroupFormIds()
	{
		TreeSet<Integer> ret = new TreeSet<Integer>();
		ret.add(wrldFormId);
		return ret;
	}

	@Override
	public Set<Integer> getWRLDExtBlockCELLFormIds()
	{
		throw new UnsupportedOperationException();
	}

	private static long getDATAFlags(PluginSubrecord data)
	{
		return ESMByteConvert.extractInt(data.getSubrecordData(), 0);
	}

	private static int getDATAx(PluginSubrecord data)
	{
		return ESMByteConvert.extractInt(data.getSubrecordData(), 4);
	}

	private static int getDATAy(PluginSubrecord data)
	{
		return ESMByteConvert.extractInt(data.getSubrecordData(), 8);
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
