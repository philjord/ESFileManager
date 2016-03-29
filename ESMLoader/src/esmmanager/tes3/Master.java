package esmmanager.tes3;

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

import esmmanager.Point;
import esmmanager.common.PluginException;
import esmmanager.common.data.plugin.FormInfo;
import esmmanager.common.data.plugin.IMaster;
import esmmanager.common.data.plugin.PluginSubrecord;
import esmmanager.loader.CELLPointer;
import esmmanager.loader.ESMManager;
import esmmanager.loader.InteriorCELLTopGroup;
import esmmanager.loader.WRLDChildren;
import esmmanager.loader.WRLDTopGroup;
import tools.WeakValueHashMap;
import tools.io.ESMByteConvert;
import tools.io.MappedByteBufferRAF;

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

	// attmpting to index cells and only load when requested
	//private ArrayList<PluginRecord> records = new ArrayList<PluginRecord>();

	private LinkedHashMap<Integer, FormInfo> idToFormMap;

	private HashMap<String, Integer> edidToFormIdMap;

	private HashMap<String, List<Integer>> typeToFormIdMap;

	private HashMap<Point, Integer> extCellXYToFormIdMap;
	private HashMap<Point, Integer> extLandXYToFormIdMap;

	// each cellRecord will have a faked up set of children refrs attached to it, must record
	private WeakValueHashMap<Integer, CELLPluginGroup> cellChildren = new WeakValueHashMap<Integer, CELLPluginGroup>();

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

	/*	public ArrayList<PluginRecord> getRecords()
		{
			return records;
		}*/

	public synchronized void load() throws PluginException, IOException
	{

		//TODO: implement a extblock sub block system for much faster loading! just like the others

		if (!masterFile.exists() || !masterFile.isFile())
			throw new IOException("Master file '" + masterFile.getAbsolutePath() + "' does not exist");

		if (masterFile.length() > Integer.MAX_VALUE || !ESMManager.USE_FILE_MAPS)
			in = new RandomAccessFile(masterFile, "r");
		else
			in = new MappedByteBufferRAF(masterFile, "r");

		synchronized (in)
		{
			masterHeader.load(masterFile.getName(), in);

			idToFormMap = new LinkedHashMap<Integer, FormInfo>();
			edidToFormIdMap = new HashMap<String, Integer>();
			typeToFormIdMap = new HashMap<String, List<Integer>>();

			extCellXYToFormIdMap = new HashMap<Point, Integer>();
			extLandXYToFormIdMap = new HashMap<Point, Integer>();

			//add a single wrld indicator, to indicate the single morrowind world, id MUST be wrldFormId (0)!
			PluginRecord wrldRecord = new PluginRecord(currentFormId++, "WRLD", "MorrowindWorld");
			idToFormMap.put(new Integer(wrldRecord.getFormID()),
					new FormInfo(wrldRecord.getRecordType(), wrldRecord.getFormID(), wrldRecord.getEditorID(), wrldRecord));

			while (in.getFilePointer() < in.length())
			{
				int formID = getNextFormId();
				long pointerToStart = in.getFilePointer();// needed later
				PluginRecord record = new PluginRecord(formID);
				record.load(masterFile.getName(), in);
				// 1 length are single 0's
				if (record.getEditorID() != null && record.getEditorID().length() > 1)
				{
					edidToFormIdMap.put(record.getEditorID(), formID);
				}

				List<Integer> typeList = typeToFormIdMap.get(record.getRecordType());
				if (typeList == null)
				{
					typeList = new ArrayList<Integer>();
					typeToFormIdMap.put(record.getRecordType(), typeList);
				}
				typeList.add(formID);

				if (record.getRecordType().equals("CELL"))
				{
					// cells get pointers not records
					PluginSubrecord data = record.getSubrecords().get(1);
					long flags = getDATAFlags(data);
					// not interior marker
					if ((flags & 0x1) == 0)
					{
						Point xy = new Point(getDATAx(data), getDATAy(data));
						extCellXYToFormIdMap.put(xy, record.getFormID());
					}

					FormInfo info = new FormInfo(record.getRecordType(), formID, record.getEditorID(), pointerToStart);
					idToFormMap.put(new Integer(formID), info);
				}
				// In fact I notice that LAND records always follow the cell they are for
				else if (record.getRecordType().equals("LAND"))
				{
					PluginSubrecord intv = record.getSubrecords().get(0);

					int cellX = ESMByteConvert.extractInt(intv.getSubrecordData(), 0);
					int cellY = ESMByteConvert.extractInt(intv.getSubrecordData(), 4);
					Point xy = new Point(cellX, cellY);
					extLandXYToFormIdMap.put(xy, formID);

					FormInfo info = new FormInfo(record.getRecordType(), formID, record.getEditorID(), pointerToStart);
					idToFormMap.put(new Integer(formID), info);
				}
				else
				{
					// every thing else gets stored as a record
					FormInfo info = new FormInfo(record.getRecordType(), formID, record.getEditorID(), record);
					idToFormMap.put(new Integer(formID), info);
				}

			}

			// now establish min and max form id range
			minFormId = 0;
			maxFormId = currentFormId - 1;
		}
	}

	@Override
	public PluginRecord getPluginRecord(int formID) throws PluginException
	{
		FormInfo formInfo = idToFormMap.get(new Integer(formID));

		if (formInfo == null)
		{
			throw new PluginException("" + masterFile.getName() + ": Record " + formID + " not found, it may be a CELL or WRLD record");
		}

		if (formInfo.isPointerOnly())
		{
			synchronized (in)
			{
				try
				{
					in.seek(formInfo.getPointer());

					PluginRecord record = new PluginRecord(formInfo.getFormID());
					record.load(masterFile.getName(), in);

					return record;
				}
				catch (IOException e)
				{
					e.printStackTrace();
					return null;
				}
			}
		}
		else
		{
			return (PluginRecord) formInfo.getPluginRecord();
		}
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
	public PluginRecord getWRLDExtBlockCELL(int wrldFormId2, int x, int y) throws DataFormatException, IOException, PluginException
	{
		if (wrldFormId2 != wrldFormId)
		{
			new Throwable("bad morrowind world id! " + wrldFormId2).printStackTrace();
		}
		Integer formID = extCellXYToFormIdMap.get(new Point(x, y));
		if (formID != null)
			return getPluginRecord(formID);
		return null;
	}

	@Override
	public PluginGroup getWRLDExtBlockCELLChildren(int wrldFormId2, int x, int y) throws DataFormatException, IOException, PluginException
	{
		if (wrldFormId2 != wrldFormId)
		{
			new Throwable("bad morrowind world id! " + wrldFormId2).printStackTrace();
		}
		Integer formID = extCellXYToFormIdMap.get(new Point(x, y));
		CELLPluginGroup cell = cellChildren.get(formID);
		if (cell == null)
		{
			// make up a fake group and add all children from the cell
			PluginRecord cellRecord = getPluginRecord(formID);
			cell = new CELLPluginGroup(cellRecord);

			PluginSubrecord data = cellRecord.getSubrecords().get(1);

			long flags = getDATAFlags(data);
			// not interior marker
			if ((flags & 0x1) == 0)
			{
				Point xy = new Point(getDATAx(data), getDATAy(data));
				Integer landId = extLandXYToFormIdMap.get(xy);
				if (landId != null)
				{
					PluginRecord land = getPluginRecord(landId);
					cell.addPluginRecord(land);
				}
				cellChildren.put(formID, cell);
			}
			else
			{
				System.out.println("why the hell is an interior being asked for? " + formID);
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
		CELLPluginGroup cell = cellChildren.get(formID);

		if (cell == null)
		{
			// make up a fake group and add all children from the cell
			PluginRecord cellRecord = getPluginRecord(formID);
			cell = new CELLPluginGroup(cellRecord);
			cellChildren.put(formID, cell);
		}
		return cell;

	}

	@Override
	public PluginGroup getInteriorCELLPersistentChildren(int formID) throws DataFormatException, IOException, PluginException
	{
		//To my knowledge these don't exist in any real manner
		throw new UnsupportedOperationException();
	}

	@Override
	public List<CELLPointer> getAllInteriorCELLFormIds()
	{
		ArrayList<CELLPointer> ret = new ArrayList<CELLPointer>();
		List<Integer> cells = typeToFormIdMap.get("CELL");
		// only those marked interior
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
					ret.add(new CELLPointer(id, -1));
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

	public int convertNameRefToId(String key)
	{
		if (edidToFormIdMap.containsKey(key))
			return edidToFormIdMap.get(key);
		return -1;
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

}
