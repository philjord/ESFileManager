package esmmanager.common.data.plugin;

import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.DataFormatException;

import tools.io.ESMByteConvert;
import tools.io.MappedByteBufferRAF;
import esmmanager.common.PluginException;
import esmmanager.loader.CELLPointer;
import esmmanager.loader.InteriorCELLTopGroup;
import esmmanager.loader.WRLDChildren;
import esmmanager.loader.WRLDTopGroup;

/**
 * This is a copy of the master file in data package, however 
 * it holds onto a copy of all loaded data for everything other than the 
 * WRLD and CELL values, which is simply indexes down to the subblock level
 *
 * @author Administrator
 *
 */
public class Master implements IMaster
{
	private static int headerByteCount = -1;

	private File masterFile;

	private RandomAccessFile in;

	private PluginHeader masterHeader;

	private LinkedHashMap<Integer, FormInfo> idToFormMap;

	private HashMap<String, Integer> edidToFormIdMap;

	private HashMap<String, List<Integer>> typeToFormIdMap;

	private int minFormId = Integer.MAX_VALUE;

	private int maxFormId = Integer.MIN_VALUE;

	private WRLDTopGroup wRLDTopGroup;

	private InteriorCELLTopGroup interiorCELLTopGroup;

	private int masterID = 0;

	/**
	 * Master id must represent the load order of this master file, this is used as an offset to all formids 
	 * and needs to be treated with care if saves are made against those ids
	 * @param masterFile
	 * @param masterID
	 */
	public Master(File masterFile)
	{
		this.masterFile = masterFile;
		masterHeader = new PluginHeader(masterFile.getName());
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

	private PluginRecord getRecordFromFile(long pointer) throws PluginException, IOException, DataFormatException
	{
		in.seek(pointer);

		byte prefix[] = new byte[headerByteCount];
		in.read(prefix);

		int length = ESMByteConvert.extractInt(prefix, 4);

		PluginRecord cellRecord = new PluginRecord(prefix);
		cellRecord.load(masterFile.getName(), in, length);

		return cellRecord;
	}

	private PluginGroup getChildrenFromFile(long pointer) throws PluginException, IOException, DataFormatException
	{
		in.seek(pointer);

		byte prefix[] = new byte[headerByteCount];
		in.read(prefix);

		int length = ESMByteConvert.extractInt(prefix, 4);
		length -= headerByteCount;
		PluginGroup childrenGroup = new PluginGroup(prefix);
		childrenGroup.load(masterFile.getName(), in, length);

		return childrenGroup;
	}

	public WRLDTopGroup getWRLDTopGroup()
	{
		return wRLDTopGroup;
	}

	public InteriorCELLTopGroup getInteriorCELLTopGroup()
	{
		return interiorCELLTopGroup;
	}

	@Override
	public synchronized PluginRecord getWRLD(int formID) throws DataFormatException, IOException, PluginException
	{
		return wRLDTopGroup.WRLDByFormId.get(formID);
	}

	@Override
	public synchronized WRLDChildren getWRLDChildren(int formID)
	{
		return wRLDTopGroup.WRLDChildrenByFormId.get(formID);
	}

	@Override
	public synchronized int getWRLDExtBlockCELLId(int wrldFormId, int x, int y)
	{
		WRLDChildren children = wRLDTopGroup.WRLDChildrenByFormId.get(new Integer(wrldFormId));
		if (children != null)
		{
			CELLPointer pointer = children.WRLDExtBlockCELLByXY.get(new Point(x, y));
			if (pointer != null)
			{
				return pointer.formId;
			}
		}

		return -1;
	}

	@Override
	public synchronized PluginRecord getWRLDExtBlockCELL(int formID) throws DataFormatException, IOException, PluginException
	{
		CELLPointer cellPointer = wRLDTopGroup.WRLDExtBlockCELLByFormId.get(new Integer(formID));
		if (cellPointer == null || cellPointer.cellFilePointer == -1)
		{
			return null;
		}

		PluginRecord cellRecord = getRecordFromFile(cellPointer.cellFilePointer);

		if (!cellRecord.getRecordType().equals("CELL"))
		{
			System.out.println("Non CELL requested " + formID);
			return null;
		}

		return cellRecord;
	}

	@Override
	public synchronized PluginGroup getWRLDExtBlockCELLChildren(int formID) throws DataFormatException, IOException, PluginException
	{
		CELLPointer cellPointer = wRLDTopGroup.WRLDExtBlockCELLByFormId.get(new Integer(formID));
		if (cellPointer == null || cellPointer.cellChildrenFilePointer == -1)
		{
			return null;
		}

		PluginGroup childrenGroup = getChildrenFromFile(cellPointer.cellChildrenFilePointer);
		if (!childrenGroup.getRecordType().equals("GRUP"))
		{
			System.out.println("Non GRUP requested " + formID);
			return null;
		}

		return childrenGroup;
	}

	@Override
	public Set<Integer> getAllInteriorCELLFormIds()
	{
		//just for hunter sneaker cut down esm
		if (interiorCELLTopGroup != null)
			return interiorCELLTopGroup.interiorCELLByFormId.keySet();
		else
			return new HashSet<Integer>();
	}

	@Override
	public synchronized PluginRecord getInteriorCELL(int formID) throws DataFormatException, IOException, PluginException
	{
		CELLPointer cellPointer = interiorCELLTopGroup.interiorCELLByFormId.get(new Integer(formID));
		if (cellPointer == null || cellPointer.cellFilePointer == -1)
		{
			return null;
		}

		PluginRecord cellRecord = getRecordFromFile(cellPointer.cellFilePointer);

		if (!cellRecord.getRecordType().equals("CELL"))
		{
			System.out.println("Non CELL requested " + formID);
			return null;
		}

		return cellRecord;
	}

	@Override
	public synchronized PluginGroup getInteriorCELLChildren(int formID) throws DataFormatException, IOException, PluginException
	{
		CELLPointer cellPointer = interiorCELLTopGroup.interiorCELLByFormId.get(new Integer(formID));
		if (cellPointer == null || cellPointer.cellChildrenFilePointer == -1)
		{
			return null;
		}

		PluginGroup childrenGroup = getChildrenFromFile(cellPointer.cellChildrenFilePointer);
		if (!childrenGroup.getRecordType().equals("GRUP"))
		{
			System.out.println("Non GRUP requested " + formID);
			return null;
		}

		return childrenGroup;

	}

	@Override
	public synchronized PluginRecord getPluginRecord(int formID) throws PluginException
	{
		//TODO: sort out the multiple esm file form id pointers properly, recall it is paretn pointers only, no cross references
		int masterFormID = formID & 0xffffff | masterID << 24;
		FormInfo formInfo = idToFormMap.get(new Integer(masterFormID));

		if (formInfo == null)
		{
			throw new PluginException("" + masterFile.getName() + ": Record " + masterFormID
					+ " not found, it may be a CELL or WRLD record");
		}

		return formInfo.getPluginRecord();
	}

	public synchronized void load() throws PluginException, DataFormatException, IOException
	{
		if (!masterFile.exists() || !masterFile.isFile())
			throw new IOException("Master file '" + masterFile.getAbsolutePath() + "' does not exist");

		//in = new RandomAccessFile(masterFile, "r");
		in = new MappedByteBufferRAF(masterFile, "r");

		long fp = in.getFilePointer();

		in.seek(fp);

		masterHeader.read(in);

		headerByteCount = masterHeader.getHeaderByteCount();

		// now load an index into memory
		byte prefix[] = new byte[headerByteCount];
		masterID = masterHeader.getMasterList().size();
		int recordCount = masterHeader.getRecordCount();
		List<FormInfo> formList = new ArrayList<FormInfo>(recordCount);

		int count = in.read(prefix);
		while (count != -1)
		{
			if (count != headerByteCount)
				throw new PluginException(masterFile.getName() + ": Group record prefix is too short");

			String recordType = new String(prefix, 0, 4);
			int groupLength = ESMByteConvert.extractInt(prefix, 4);

			//String groupRecordType2 = new String(prefix, 8, 4);

			if (recordType.equals("TES4"))
			{
				in.skipBytes(groupLength);
			}
			else
			{
				if (!recordType.equals("GRUP"))
					throw new PluginException(masterFile.getName() + ": Top-level record is not a group");
				if (prefix[12] != 0)
					throw new PluginException(masterFile.getName() + ": Top-level group type is not 0");

				String groupRecordType = new String(prefix, 8, 4);
				groupLength -= headerByteCount;

				if (groupRecordType.equals("WRLD"))
				{
					wRLDTopGroup = new WRLDTopGroup(prefix);
					wRLDTopGroup.loadAndIndex(masterFile.getName(), in, groupLength);
				}
				else if (groupRecordType.equals("CELL"))
				{
					interiorCELLTopGroup = new InteriorCELLTopGroup(prefix);
					interiorCELLTopGroup.loadAndIndex(masterFile.getName(), in, groupLength);
				}
				else
				{
					while (groupLength >= headerByteCount)
					{
						count = in.read(prefix);
						if (count != headerByteCount)
						{
							throw new PluginException(masterFile.getName() + ": Group " + groupRecordType + " is incomplete");
						}

						recordType = new String(prefix, 0, 4);
						int recordLength = ESMByteConvert.extractInt(prefix, 4);
						if (recordType.equals("GRUP"))
						{
							groupLength -= recordLength;
							in.skipBytes(recordLength - headerByteCount);
						}
						else
						{
							PluginRecord record = new PluginRecord(prefix);
							int formID = record.getFormID();

							if (record.isDeleted() || record.isIgnored() || formID == 0 || formID >>> 24 < masterID)
							{
								in.skipBytes(recordLength);
							}
							else
							{
								record.load(masterFile.getName(), in, recordLength);
								formList.add(new FormInfo(recordType, formID, record.getEditorID(), record));

							}
							groupLength -= recordLength + headerByteCount;
						}
					}

					if (groupLength != 0)
					{
						throw new PluginException(masterFile.getName() + ": Group " + groupRecordType + " is incomplete");
					}
				}
			}

			// prep for the next loop
			count = in.read(prefix);
		}

		addGeckDefaultObjects(formList);

		recordCount = formList.size();
		idToFormMap = new LinkedHashMap<Integer, FormInfo>(recordCount);
		edidToFormIdMap = new HashMap<String, Integer>();
		typeToFormIdMap = new HashMap<String, List<Integer>>();

		for (FormInfo info : formList)
		{
			int formID = info.getFormID();
			formID = formID & 0xffffff | masterID << 24;
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
		for (Integer formId : idToFormMap.keySet())
		{
			minFormId = formId < minFormId ? formId : minFormId;
			maxFormId = formId > maxFormId ? formId : maxFormId;
		}
		for (Integer formId : getAllInteriorCELLFormIds())
		{
			minFormId = formId < minFormId ? formId : minFormId;
			maxFormId = formId > maxFormId ? formId : maxFormId;
		}

		for (Integer formId : getAllWRLDTopGroupFormIds())
		{
			minFormId = formId < minFormId ? formId : minFormId;
			maxFormId = formId > maxFormId ? formId : maxFormId;
		}

	}

	@Override
	public Set<Integer> getAllWRLDTopGroupFormIds()
	{
		return wRLDTopGroup.WRLDByFormId.keySet();
	}

	@Override
	public Set<Integer> getWRLDExtBlockCELLFormIds()
	{
		return wRLDTopGroup.WRLDExtBlockCELLByFormId.keySet();
	}

	/**
	 * I have found that when the GECK is opened with no data file, many many object already exist, inside the exe possibly?
	 * Once the Fallout3.esm file is loaded some of these are overwritten with new data.
	 * 
	 *  This loader that only load the esm and has not the other data, appears to find most of these magic records (that is they are in the esm as well)
	 *  But at least one isn't: decimal 32 (hex 20) PortalMarker STAT is not in my version of the data
	 *  The STATs RoomMarker and PortalMarker are in the magic list, but Fallout3.esm only loads the RoomMarker
	 *  If I find any others (usually by a request to get PluginRecord returning null), I should work out what they are and add them here
	 *  
	 * @param formList
	 */
	private void addGeckDefaultObjects(List<FormInfo> formList)
	{
		//fallout3 formId 32 stat PortalMarker
		if (masterFile.getName().equals("Fallout3.esm"))
		{
			PluginRecord pr = new PluginRecord(24, "STAT", 32, "PortalMarker");
			PluginSubrecord psr = new PluginSubrecord("STAT", "EDID", "PortalMarkerZ".getBytes()); //note the null termination padding
			pr.getSubrecords().add(psr);
			formList.add(new FormInfo("STAT", 32, "PortalMarker", pr));
		}
	}

	@Override
	public String getName()
	{
		return masterFile.getName();
	}

	@Override
	public float getVersion()
	{
		return masterHeader.getVersion();
	}

}
