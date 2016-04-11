package esmmanager.tes3;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.zip.DataFormatException;

import com.frostwire.util.SparseArray;

import esmmanager.common.PluginException;
import esmmanager.common.data.plugin.FormInfo;
import esmmanager.common.data.plugin.PluginSubrecord;
import esmmanager.loader.CELLDIALPointer;
import esmmanager.loader.ESMManager;
import esmmanager.loader.InteriorCELLTopGroup;
import esmmanager.loader.WRLDChildren;
import esmmanager.loader.WRLDTopGroup;
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
public class Master implements IMasterTes3
{
	private File masterFile;

	private RandomAccessFile in;

	private PluginHeader masterHeader;

	//Note NO CELLs or DIALs in the following 3 all CELLs in the Cells sets below
	private SparseArray<FormInfo> idToFormMap;

	private LinkedHashMap<String, Integer> edidToFormIdMap;

	// waay over sized just in case!
	private CELLPluginGroup[][] exteriorCells = new CELLPluginGroup[100][100];

	private LinkedHashMap<String, DIALRecord> dials;

	private LinkedHashMap<String, CELLPluginGroup> interiorCellsByEdid = new LinkedHashMap<String, CELLPluginGroup>();
	private LinkedHashMap<Integer, CELLPluginGroup> interiorCellsByFormId = new LinkedHashMap<Integer, CELLPluginGroup>();

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
	public int[] getAllFormIds()
	{
		return idToFormMap.keySet();
	}

	@Override
	public SparseArray<FormInfo> getFormMap()
	{
		return idToFormMap;
	}

	public void load() throws PluginException, IOException
	{

		if (!masterFile.exists() || !masterFile.isFile())
			throw new IOException("Master file '" + masterFile.getAbsolutePath() + "' does not exist");

		if (masterFile.length() > Integer.MAX_VALUE || !ESMManager.USE_FILE_MAPS)
			in = new RandomAccessFile(masterFile, "r");
		else
			in = new MappedByteBufferRAF(masterFile, "r");

		synchronized (in)
		{
			masterHeader.load(masterFile.getName(), in);

			idToFormMap = new SparseArray<FormInfo>();
			edidToFormIdMap = new LinkedHashMap<String, Integer>();
			dials = new LinkedHashMap<String, DIALRecord>();

			//add a single wrld indicator, to indicate the single morrowind world, id MUST be wrldFormId (0)!
			PluginRecord wrldRecord = new PluginRecord(currentFormId++, "WRLD", "MorrowindWorld");
			idToFormMap.put(wrldRecord.getFormID(), new FormInfo(wrldRecord.getRecordType(), wrldRecord.getFormID(), wrldRecord));

			while (in.getFilePointer() < in.length())
			{
				// pull the prefix data so we know what sort of record we need to load
				byte[] prefix = new byte[16];
				int count = in.read(prefix);
				if (count != 16)
					throw new PluginException(": record prefix is incomplete");

				String recordType = new String(prefix, 0, 4);
				//recordSize = ESMByteConvert.extractInt(prefix, 4);
				//unknownInt = ESMByteConvert.extractInt(prefix, 8);
				//recordFlags1 = ESMByteConvert.extractInt(prefix, 12);

				int formID = getNextFormId();

				if (recordType.equals("CELL"))
				{
					//	looks like x = 23 to -18 y is 27 to -17  so 50 wide with an x off of +25 and y of +20
					CELLPluginGroup cellPluginGroup = new CELLPluginGroup(prefix, in);

					if (cellPluginGroup.isExterior)
					{
						int xIdx = cellPluginGroup.cellX + 50;
						int yIdx = cellPluginGroup.cellY + 50;
						exteriorCells[xIdx][yIdx] = cellPluginGroup;
					}
					else
					{
						interiorCellsByEdid.put(cellPluginGroup.getEditorID(), cellPluginGroup);
						interiorCellsByFormId.put(cellPluginGroup.getFormID(), cellPluginGroup);
					}
				}
				else if (recordType.equals("LAND"))
				{
					//land are fully skipped as they get loaded with the owner cell at cell load time later
					int recordSize = ESMByteConvert.extractInt(prefix, 4);
					in.skipBytes(recordSize);
				}
				else if (recordType.equals("DIAL"))
				{
					DIALRecord dial = new DIALRecord(formID, prefix, in);
					dials.put(dial.getEditorID(), dial);
				}
				else
				{
					PluginRecord record = new PluginRecord(formID, prefix);
					record.load("", in, -1);

					// 1 length are single 0's
					if (record.getEditorID() != null && record.getEditorID().length() > 1)
					{
						edidToFormIdMap.put(record.getEditorID(), formID);
					}

					// every thing else gets stored as a record
					FormInfo info = new FormInfo(record.getRecordType(), formID, record);
					idToFormMap.put(formID, info);
				}

			}

			// now establish min and max form id range
			minFormId = 0;
			maxFormId = currentFormId - 1;
		}
	}

	/**
	 * Not for CELLs
	 */
	@Override
	public PluginRecord getPluginRecord(int formID) throws PluginException
	{
		FormInfo formInfo = idToFormMap.get(formID);

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
	public PluginRecord getWRLDExtBlockCELL(int wrldFormId2, int x, int y) throws DataFormatException, IOException, PluginException
	{
		if (wrldFormId2 != wrldFormId)
		{
			new Throwable("bad morrowind world id! " + wrldFormId2).printStackTrace();
		}
		int xIdx = x + 50;
		int yIdx = y + 50;
		CELLPluginGroup cellPluginGroup = exteriorCells[xIdx][yIdx];
		if (cellPluginGroup != null)
		{
			// make sure no one else asks for it while we check load state
			synchronized (cellPluginGroup)
			{
				if (!cellPluginGroup.isLoaded())
				{
					cellPluginGroup.load(in);
				}
			}

			return cellPluginGroup.createPluginRecord();
		}
		return null;

	}

	@Override
	public PluginGroup getWRLDExtBlockCELLChildren(int wrldFormId2, int x, int y) throws DataFormatException, IOException, PluginException
	{
		if (wrldFormId2 != wrldFormId)
		{
			new Throwable("bad morrowind world id! " + wrldFormId2).printStackTrace();
		}
		int xIdx = x + 50;
		int yIdx = y + 50;
		CELLPluginGroup cellPluginGroup = exteriorCells[xIdx][yIdx];
		if (cellPluginGroup != null)
		{
			// make sure no one else asks for it while we check load state
			synchronized (cellPluginGroup)
			{
				if (!cellPluginGroup.isLoaded())
				{
					cellPluginGroup.load(in);
				}
			}

			return cellPluginGroup;
		}
		return null;
	}

	@Override
	public PluginRecord getInteriorCELL(int formID) throws DataFormatException, IOException, PluginException
	{
		CELLPluginGroup cellPluginGroup = interiorCellsByFormId.get(formID);
		if (cellPluginGroup != null)
		{
			// make sure no one else asks for it while we check load state
			synchronized (cellPluginGroup)
			{
				if (!cellPluginGroup.isLoaded())
				{
					cellPluginGroup.load(in);
				}
			}
			return cellPluginGroup.createPluginRecord();
		}
		return null;
	}

	@Override
	public PluginGroup getInteriorCELLChildren(int formID) throws DataFormatException, IOException, PluginException
	{
		CELLPluginGroup cellPluginGroup = interiorCellsByFormId.get(formID);
		if (cellPluginGroup != null)
		{
			// make sure no one else asks for it while we check load state
			synchronized (cellPluginGroup)
			{
				if (!cellPluginGroup.isLoaded())
				{
					cellPluginGroup.load(in);
				}
			}
			return cellPluginGroup;
		}
		return null;

	}

	@Override
	public PluginGroup getInteriorCELLPersistentChildren(int formID) throws DataFormatException, IOException, PluginException
	{
		//To my knowledge these don't exist in any real manner
		throw new UnsupportedOperationException();
	}

	@Override
	public List<CELLDIALPointer> getAllInteriorCELLFormIds()
	{
		ArrayList<CELLDIALPointer> ret = new ArrayList<CELLDIALPointer>();
		for (Integer formId : interiorCellsByFormId.keySet())
		{
			ret.add(new CELLDIALPointer(formId, -1));
		}

		return ret;
	}

	@Override
	public int[] getAllWRLDTopGroupFormIds()
	{
		return new int[] { wrldFormId };
	}

	public int convertNameRefToId(String key)
	{
		Integer id = edidToFormIdMap.get(key);
		if (id != null)
		{
			return id.intValue();
		}
		else
		{
			CELLPluginGroup cpg = interiorCellsByEdid.get(key);
			if (cpg != null)
				return cpg.getFormID();
			else
				return -1;
		}
	}

}
