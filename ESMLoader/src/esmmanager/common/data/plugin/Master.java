package esmmanager.common.data.plugin;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;

import com.frostwire.util.SparseArray;

import esmmanager.Point;
import esmmanager.common.PluginException;
import esmmanager.common.data.record.Record;
import esmmanager.loader.CELLDIALPointer;
import esmmanager.loader.DIALTopGroup;
import esmmanager.loader.ESMManager;
import esmmanager.loader.InteriorCELLTopGroup;
import esmmanager.loader.WRLDChildren;
import esmmanager.loader.WRLDTopGroup;
import tools.io.ESMByteConvert;
import tools.io.MappedByteBufferRAF;

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

	private SparseArray<FormInfo> idToFormMap;

	private int minFormId = Integer.MAX_VALUE;

	private int maxFormId = Integer.MIN_VALUE;

	private WRLDTopGroup wRLDTopGroup;

	private InteriorCELLTopGroup interiorCELLTopGroup;

	private DIALTopGroup dIALTopGroup;

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

	@Override
	public int getMinFormId()
	{
		return minFormId;
	}

	@Override
	public int getMaxFormId()
	{
		return maxFormId;
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

	private PluginRecord getRecordFromFile(long pointer) throws PluginException, IOException, DataFormatException
	{
		synchronized (in)
		{
			in.seek(pointer);

			byte prefix[] = new byte[headerByteCount];
			in.read(prefix);

			int length = ESMByteConvert.extractInt(prefix, 4);

			PluginRecord cellRecord = new PluginRecord(prefix);

			//cellRecord.load(masterFile.getName(), in, length);
			cellRecord.load("", in, length);

			return cellRecord;
		}
	}

	private PluginGroup getChildrenFromFile(long pointer) throws PluginException, IOException, DataFormatException
	{
		synchronized (in)
		{
			in.seek(pointer);

			byte prefix[] = new byte[headerByteCount];
			in.read(prefix);

			int length = ESMByteConvert.extractInt(prefix, 4);
			length -= headerByteCount;
			PluginGroup childrenGroup = new PluginGroup(prefix);
			//Dear god this String fileName appears to do something magical without it failures!
			childrenGroup.load("", in, length);

			return childrenGroup;
		}
	}

	/**
	 * To use this the pointer MUST be a "children" group of a CELL interior or exterior, if not death
	 * @param pointer
	 * @return
	 * @throws PluginException
	 * @throws IOException
	 * @throws DataFormatException
	 */

	private PluginGroup getChildrenFromFile(long pointer, int childGroupType) throws PluginException, IOException, DataFormatException
	{
		synchronized (in)
		{
			in.seek(pointer);

			byte prefix[] = new byte[headerByteCount];
			in.read(prefix);

			int length = ESMByteConvert.extractInt(prefix, 4);
			length -= headerByteCount;
			PluginGroup childrenGroup = new PluginGroup(prefix);
			//Dear god this String fileName appears to do something magical without it failures!
			childrenGroup.load("", in, length, childGroupType);

			// Now pull out the right type like the persister guy and return it
			if (childrenGroup.getRecordList() != null)
			{
				for (Record pgr : childrenGroup.getRecordList())
				{
					PluginGroup pg = (PluginGroup) pgr;
					if (pg.getGroupType() == childGroupType)
					{
						return pg;
					}
				}
			}

		}

		return null;
	}

	@Override
	public WRLDTopGroup getWRLDTopGroup()
	{
		return wRLDTopGroup;
	}

	@Override
	public InteriorCELLTopGroup getInteriorCELLTopGroup()
	{
		return interiorCELLTopGroup;
	}

	@Override
	public PluginRecord getWRLD(int formID) throws DataFormatException, IOException, PluginException
	{
		return wRLDTopGroup.WRLDByFormId.get(formID);
	}

	@Override
	public WRLDChildren getWRLDChildren(int formID)
	{
		return wRLDTopGroup.WRLDChildrenByFormId.get(formID);
	}

	@Override
	public PluginRecord getWRLDExtBlockCELL(int wrldFormId, int x, int y) throws DataFormatException, IOException, PluginException
	{
		WRLDChildren children = wRLDTopGroup.WRLDChildrenByFormId.get(wrldFormId);
		if (children != null)
		{
			CELLDIALPointer cellPointer = children.getWRLDExtBlockCELLByXY(new Point(x, y));

			if (cellPointer == null || cellPointer.cellFilePointer == -1)
			{
				// normally this is fine just means we are off the edge of the map
				//System.out.println("null cellPointer! " + new Point(x, y) + " " + cellPointer);
				children.getWRLDExtBlockCELLByXY(new Point(x, y));
				return null;
			}

			PluginRecord cellRecord = getRecordFromFile(cellPointer.cellFilePointer);

			if (!cellRecord.getRecordType().equals("CELL"))
			{
				System.out.println("Non CELL found " + wrldFormId + " x " + x + " y " + y);
				return null;
			}

			return cellRecord;
		}
		else
		{
			System.out.println("WRLDChildren == null, very suspicious, unlikely to be a good thing " + wrldFormId);
		}
		return null;
	}

	@Override
	public PluginGroup getWRLDExtBlockCELLChildren(int wrldFormId, int x, int y) throws DataFormatException, IOException, PluginException
	{
		WRLDChildren children = wRLDTopGroup.WRLDChildrenByFormId.get(wrldFormId);
		if (children != null)
		{
			CELLDIALPointer cellPointer = children.getWRLDExtBlockCELLByXY(new Point(x, y));

			if (cellPointer == null || cellPointer.cellChildrenFilePointer == -1)
			{
				return null;
			}

			PluginGroup childrenGroup = getChildrenFromFile(cellPointer.cellChildrenFilePointer);
			if (!childrenGroup.getRecordType().equals("GRUP"))
			{
				System.out.println("Non GRUP found " + wrldFormId + " x " + x + " y " + y);
				return null;
			}

			return childrenGroup;
		}
		return null;
	}

	@Override
	public List<CELLDIALPointer> getAllInteriorCELLFormIds()
	{
		//just for hunter sneaker cut down esm
		if (interiorCELLTopGroup != null)
			return interiorCELLTopGroup.getAllInteriorCELLFormIds();
		else
			return new ArrayList<CELLDIALPointer>();
	}

	@Override
	public PluginRecord getInteriorCELL(int formID) throws DataFormatException, IOException, PluginException
	{
		CELLDIALPointer cellPointer = interiorCELLTopGroup.getInteriorCELL(formID);
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
	public PluginGroup getInteriorCELLChildren(int formID) throws DataFormatException, IOException, PluginException
	{
		CELLDIALPointer cellPointer = interiorCELLTopGroup.getInteriorCELL(formID);
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
	public PluginGroup getInteriorCELLPersistentChildren(int formID) throws DataFormatException, IOException, PluginException
	{
		CELLDIALPointer cellPointer = interiorCELLTopGroup.getInteriorCELL(formID);
		if (cellPointer == null || cellPointer.cellChildrenFilePointer == -1)
		{
			return null;
		}

		return getChildrenFromFile(cellPointer.cellChildrenFilePointer, PluginGroup.CELL_PERSISTENT);
	}

	@Override
	public PluginRecord getPluginRecord(int formID)
	{
		//TODO: sort out the multiple esm file form id pointers properly, recall it is paretn pointers only, no cross references
		int masterFormID = formID & 0xffffff | masterID << 24;
		FormInfo formInfo = idToFormMap.get(masterFormID);

		if (formInfo != null)
			return formInfo.getPluginRecord();

		//possibly from an unloaded cell etc.
		return null;
	}

	public void load() throws PluginException, DataFormatException, IOException
	{

		if (!masterFile.exists() || !masterFile.isFile())
			throw new IOException("Master file '" + masterFile.getAbsolutePath() + "' does not exist");

		if (masterFile.length() > Integer.MAX_VALUE || !ESMManager.USE_FILE_MAPS)
			in = new RandomAccessFile(masterFile, "r");
		else
			in = new MappedByteBufferRAF(masterFile, "r");

		System.out.println("Loading ESM file " + masterFile.getName());
		long start = System.currentTimeMillis();

		synchronized (in)
		{
			long fp = in.getFilePointer();

			in.seek(fp);

			masterHeader.read(in);

			headerByteCount = masterHeader.getHeaderByteCount();

			// now load an index into memory
			byte prefix[] = new byte[headerByteCount];
			masterID = masterHeader.getMasterList().size();
			int recordCount = masterHeader.getRecordCount();
			idToFormMap = new SparseArray<FormInfo>(recordCount);

			int count = in.read(prefix);
			while (count != -1)
			{
				if (count != headerByteCount)
					throw new PluginException(masterFile.getName() + ": Group record prefix is too short");

				String recordType = new String(prefix, 0, 4);
				int groupLength = ESMByteConvert.extractInt(prefix, 4);

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
					else if (groupRecordType.equals("DIAL"))
					{
						dIALTopGroup = new DIALTopGroup(prefix);
						dIALTopGroup.loadAndIndex(in, groupLength);
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
									record.load("", in, recordLength);
									formID = formID & 0xffffff | masterID << 24;
									idToFormMap.put(formID, new FormInfo(recordType, formID, record));

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

			addGeckDefaultObjects();

			// now establish min and max form id range
			for (int formId : idToFormMap.keySet())
			{
				minFormId = formId < minFormId ? formId : minFormId;
				maxFormId = formId > maxFormId ? formId : maxFormId;
			}
			for (CELLDIALPointer cp : getAllInteriorCELLFormIds())
			{
				int formId = cp.formId;
				minFormId = formId < minFormId ? formId : minFormId;
				maxFormId = formId > maxFormId ? formId : maxFormId;
			}

			for (int formId : getAllWRLDTopGroupFormIds())
			{
				minFormId = formId < minFormId ? formId : minFormId;
				maxFormId = formId > maxFormId ? formId : maxFormId;
			}
		}

		System.out.println("Finished loading ESM file " + masterFile.getName() + " in " + (System.currentTimeMillis() - start));

	}

	@Override
	public int[] getAllWRLDTopGroupFormIds()
	{
		return wRLDTopGroup.WRLDByFormId.keySet();
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
	 * @param idToFormMap2
	 */
	private void addGeckDefaultObjects()
	{
		//fallout3 formId 32 stat PortalMarker
		if (masterFile.getName().equals("Fallout3.esm"))
		{
			PluginRecord pr = new PluginRecord(24, "STAT", 32);
			PluginSubrecord psr = new PluginSubrecord("EDID", "PortalMarkerZ".getBytes()); //note the null termination padding
			pr.getSubrecords().add(psr);
			idToFormMap.put(32, new FormInfo("STAT", 32, pr));
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
