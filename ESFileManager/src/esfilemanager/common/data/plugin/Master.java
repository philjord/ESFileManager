package esfilemanager.common.data.plugin;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;

import com.frostwire.util.SparseArray;

import esfilemanager.Point;
import esfilemanager.common.PluginException;
import esfilemanager.common.data.record.Record;
import esfilemanager.loader.DIALTopGroup;
import esfilemanager.loader.FormToFilePointer;
import esfilemanager.loader.InteriorCELLTopGroup;
import esfilemanager.loader.WRLDChildren;
import esfilemanager.loader.WRLDTopGroup;
import tools.io.ESMByteConvert;
import tools.io.FileChannelRAF;

/**
 * This is a copy of the master file in data package, however it holds onto a copy of all loaded data for everything
 * other than the WRLD and CELL values, which is simply indexes down to the subblock level
 *
 * @author Administrator
 *
 */
public abstract class Master implements IMaster {
	private static int				headerByteCount	= -1;

	private FileChannelRAF			in;

	private PluginHeader			masterHeader;

	private SparseArray<FormInfo>	idToFormMap;

	private int						minFormId		= Integer.MAX_VALUE;

	private int						maxFormId		= Integer.MIN_VALUE;

	private WRLDTopGroup			wRLDTopGroup;

	private InteriorCELLTopGroup	interiorCELLTopGroup;

	private DIALTopGroup			dIALTopGroup;

	private int						masterID		= 0;

	/**
	 * Master id must represent the load order of this master file, this is used as an offset to all formids and needs
	 * to be treated with care if saves are made against those ids
	 * @param masterFile
	 * @param masterID
	 */
	public Master(String masterFileName) {
		masterHeader = new PluginHeader(masterFileName);
	}

	@Override
	public int getMinFormId() {
		return minFormId;
	}

	@Override
	public int getMaxFormId() {
		return maxFormId;
	}

	@Override
	public int[] getAllFormIds() {
		return idToFormMap.keySet();
	}

	@Override
	public SparseArray<FormInfo> getFormMap() {
		return idToFormMap;
	}

	private PluginRecord getRecordFromFile(long pointer) throws PluginException, IOException, DataFormatException {
		FileChannel ch = in.getChannel();
		// use this non sync call for speed
		byte[] prefix = new byte[headerByteCount];
		ByteBuffer bb = ByteBuffer.wrap(prefix);
		int count = ch.read(bb, pointer);
		if (count != headerByteCount)
			throw new PluginException(" : " + this + " record header is incomplete");
		
		PluginRecord cellRecord = new PluginRecord(prefix);
		cellRecord.load(in, pointer + headerByteCount);

		return cellRecord;				
	}

	private PluginGroup getChildrenFromFile(long pointer) throws PluginException, IOException, DataFormatException {
		FileChannel ch = in.getChannel();
		// use this non sync call for speed
		byte[] prefix = new byte[headerByteCount];
		int count = ch.read(ByteBuffer.wrap(prefix), pointer);	
		if (count != headerByteCount)
			throw new PluginException(" : " + this + " record header is incomplete");
		
		int length = ESMByteConvert.extractInt(prefix, 4);
		length -= headerByteCount;
		PluginGroup childrenGroup = new PluginGroup(prefix);

		childrenGroup.load(in, pointer + headerByteCount, length, -1);

		return childrenGroup;
	}

	/**
	 * To use this the pointer MUST be a "children" group of a CELL interior or exterior, if not death
	 * @param pointer
	 * @return
	 * @throws PluginException
	 * @throws IOException
	 * @throws DataFormatException
	 */

	private PluginGroup getChildrenFromFile(long pos, int childGroupType)
			throws PluginException, IOException, DataFormatException {
		FileChannel ch = in.getChannel();
		// use this non sync call for speed
		byte[] prefix = new byte[headerByteCount];
		int count = ch.read(ByteBuffer.wrap(prefix), pos);	
		pos += headerByteCount;
		if (count != headerByteCount)
			throw new PluginException(" : " + this + " record header is incomplete");
		
		int length = ESMByteConvert.extractInt(prefix, 4);
		length -= headerByteCount;
		PluginGroup childrenGroup = new PluginGroup(prefix);

		childrenGroup.load(in, pos, length, childGroupType);
			 
		// Now pull out the right type like the persister guy and return it
		if (childrenGroup.getRecordList() != null) {
			for (Record pgr : childrenGroup.getRecordList()) {
				PluginGroup pg = (PluginGroup)pgr;
				if (pg.getGroupType() == childGroupType) {
					return pg;
				}
			}
		}

		return null;
	}

	@Override
	public WRLDTopGroup getWRLDTopGroup() {
		return wRLDTopGroup;
	}

	@Override
	public InteriorCELLTopGroup getInteriorCELLTopGroup() {
		return interiorCELLTopGroup;
	}

	@Override
	public PluginRecord getWRLD(int formID) throws DataFormatException, IOException, PluginException {
		return wRLDTopGroup.WRLDByFormId.get(formID);
	}

	@Override
	public WRLDChildren getWRLDChildren(int formID) {
		return wRLDTopGroup.WRLDChildrenByFormId.get(formID);
	}

	@Override
	public PluginRecord getWRLDExtBlockCELL(int wrldFormId, int x, int y)
			throws DataFormatException, IOException, PluginException {
		WRLDChildren children = wRLDTopGroup.WRLDChildrenByFormId.get(wrldFormId);
		if (children != null) {
			FormToFilePointer cellPointer = children.getWRLDExtBlockCELLByXY(new Point(x, y));

			if (cellPointer == null || cellPointer.cellFilePointer == -1) {
				// normally this is fine just means we are off the edge of the map
				//System.out.println("null cellPointer! " + new Point(x, y) + " " + cellPointer);
				children.getWRLDExtBlockCELLByXY(new Point(x, y));
				return null;
			}

			PluginRecord cellRecord = getRecordFromFile(cellPointer.cellFilePointer);

			if (!cellRecord.getRecordType().equals("CELL")) {
				System.out.println("Non CELL found " + wrldFormId + " x " + x + " y " + y);
				return null;
			}

			return cellRecord;
		} else {
			System.out.println("WRLDChildren == null, very suspicious, unlikely to be a good thing " + wrldFormId);
		}
		return null;
	}

	@Override
	public PluginGroup getWRLDExtBlockCELLChildren(int wrldFormId, int x, int y)
			throws DataFormatException, IOException, PluginException {
		WRLDChildren children = wRLDTopGroup.WRLDChildrenByFormId.get(wrldFormId);
		if (children != null) {
			FormToFilePointer cellPointer = children.getWRLDExtBlockCELLByXY(new Point(x, y));

			if (cellPointer == null || cellPointer.cellChildrenFilePointer == -1) {
				return null;
			}

			PluginGroup childrenGroup = getChildrenFromFile(cellPointer.cellChildrenFilePointer);
			if (!childrenGroup.getRecordType().equals("GRUP")) {
				System.out.println("Non GRUP found " + wrldFormId + " x " + x + " y " + y);
				return null;
			}

			return childrenGroup;
		}
		return null;
	}

	@Override
	public List<FormToFilePointer> getAllInteriorCELLFormIds() {
		//just for hunter sneaker cut down esm
		if (interiorCELLTopGroup != null)
			return interiorCELLTopGroup.getAllInteriorCELLFormIds();
		else
			return new ArrayList<FormToFilePointer>();
	}

	@Override
	public PluginRecord getInteriorCELL(int formID) throws DataFormatException, IOException, PluginException {
		FormToFilePointer cellPointer = interiorCELLTopGroup.getInteriorCELL(formID);
		if (cellPointer == null || cellPointer.cellFilePointer == -1) {
			return null;
		}

		PluginRecord cellRecord = getRecordFromFile(cellPointer.cellFilePointer);

		if (!cellRecord.getRecordType().equals("CELL")) {
			System.out.println("Non CELL requested " + formID);
			return null;
		}

		return cellRecord;
	}

	@Override
	public PluginGroup getInteriorCELLChildren(int formID) throws DataFormatException, IOException, PluginException {
		FormToFilePointer cellPointer = interiorCELLTopGroup.getInteriorCELL(formID);
		if (cellPointer == null || cellPointer.cellChildrenFilePointer == -1) {
			return null;
		}

		PluginGroup childrenGroup = getChildrenFromFile(cellPointer.cellChildrenFilePointer);
		if (!childrenGroup.getRecordType().equals("GRUP")) {
			System.out.println("Non GRUP requested " + formID);
			return null;
		}

		return childrenGroup;

	}

	@Override
	public PluginGroup getInteriorCELLPersistentChildren(int formID)
			throws DataFormatException, IOException, PluginException {
		FormToFilePointer cellPointer = interiorCELLTopGroup.getInteriorCELL(formID);
		if (cellPointer == null || cellPointer.cellChildrenFilePointer == -1) {
			return null;
		}

		return getChildrenFromFile(cellPointer.cellChildrenFilePointer, PluginGroup.CELL_PERSISTENT);
	}

	@Override
	public PluginRecord getPluginRecord(int formID) {
		 
		//TODO: sort out the multiple esm file form id pointers properly, recall it is parent pointers only, no cross references
		int masterFormID = formID & 0xffffff | masterID << 24;
		FormInfo formInfo = idToFormMap.get(masterFormID);
		try {
			if (formInfo != null) {
				if (formInfo.isPointerOnly()) {
					if (formInfo.getPluginRecordWR() != null) {
						return formInfo.getPluginRecordWR();
					}
					long filePositionPointer = formInfo.getPointer();					
					PluginRecord record = new PluginRecord(in, filePositionPointer, headerByteCount);
					if (record.isDeleted() || record.isIgnored() || formID == 0 || formID >>> 24 < masterID) {
						return null;
					} else {
						record.load(in, filePositionPointer + headerByteCount);
						formInfo.setPluginRecordWR(record);
						return record;
					}
				} else {
					return formInfo.getPluginRecord();
				}
			}
		} catch (PluginException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		//possibly from an unloaded cell etc.
		return null;

	}

	public abstract boolean load() throws PluginException, DataFormatException, IOException;
	
	protected boolean load(FileChannelRAF in) throws PluginException, DataFormatException, IOException {
		System.out.println("Loading ESM file " + masterHeader.getName());
		long start = System.currentTimeMillis();

		this.in = in;
		FileChannel ch = in.getChannel();
		long pos = 0;// keep track of the pos in the file, so we don't use any file pointers

		int count = masterHeader.load(masterHeader.getName(), ch, pos);
		pos += count;

		//bad header means bad file
		if (masterHeader.getVersion() <= 0 || masterHeader.getRecordCount() == 0) {
			System.out.println("Bad Header skip load for file " + masterHeader.getName());
			return false;
		}

		headerByteCount = masterHeader.getHeaderByteCount();

		// now load an index into memory
		byte prefix[] = new byte[headerByteCount];
		ByteBuffer pbb = ByteBuffer.wrap(prefix); //reused to avoid allocation of object, all bytes of array are refilled or error thrown
		masterID = masterHeader.getMasterList().size();
		int recordCount = masterHeader.getRecordCount();
		idToFormMap = new SparseArray<FormInfo>(recordCount / 10);// enough to kick off with, but we aren't loading all of it
		 
		count = ch.read((ByteBuffer)pbb.rewind(), pos);	
		pos += prefix.length;
		if (count != headerByteCount)
			throw new PluginException("Record prefix is incomplete");
 
		while (count != -1) {
			if (count != headerByteCount)
				throw new PluginException(masterHeader.getName() + ": Group record prefix is too short");

			String recordType = new String(prefix, 0, 4);
			int groupLength = ESMByteConvert.extractInt(prefix, 4);

			if (recordType.equals("TES4")) {
				System.out.println("WHAT THE HELL TES4 record seen but I've loaded header already");
				pos += groupLength;
			} else {
				if (!recordType.equals("GRUP"))
					throw new PluginException(masterHeader.getName() + ": Top-level record is not a group");

				String groupRecordType = new String(prefix, 8, 4);
				if (prefix[12] != 0)
					throw new PluginException(masterHeader.getName() + ": Top-level group type " + groupRecordType + " is not 0");
				
				groupLength -= headerByteCount;

				if (groupRecordType.equals("WRLD")) {
					wRLDTopGroup = new WRLDTopGroup(prefix);
					wRLDTopGroup.loadAndIndex(masterHeader.getName(), in, pos, groupLength);
					pos += groupLength;
				} else if (groupRecordType.equals("CELL")) {
					interiorCELLTopGroup = new InteriorCELLTopGroup(prefix);
					interiorCELLTopGroup.loadAndIndex(masterHeader.getName(), in, pos, groupLength);
					pos += groupLength;
				} else if (groupRecordType.equals("DIAL")) {
					dIALTopGroup = new DIALTopGroup(prefix);
					dIALTopGroup.loadAndIndex(masterHeader.getName(), in, pos, groupLength);
					pos += groupLength;
				} else {
					while (groupLength >= headerByteCount) {
						count = ch.read((ByteBuffer)pbb.rewind(), pos);	
						pos += prefix.length;
						if (count != headerByteCount) 
							throw new PluginException(masterHeader.getName() + ": Group " + groupRecordType + " is incomplete");
						 
						recordType = new String(prefix, 0, 4);
						int recordLength = ESMByteConvert.extractInt(prefix, 4);
						if (recordType.equals("GRUP")) {
							groupLength -= recordLength;
							pos += (recordLength - headerByteCount);//skip it
						} else {
							// some should only be indexed rather than loaded now
							boolean indexedOnly = false;
							indexedOnly = (recordType.equals("QUST")	|| recordType.equals("PACK")
											|| recordType.equals("SCEN") || recordType.equals("NPC_")
											|| recordType.equals("RACE") || recordType.equals("STAT")
											|| recordType.equals("WEAP") || recordType.equals("NAVI")
											|| recordType.equals("SCOL") || recordType.equals("SNDR"));

							if (indexedOnly) {
								int formID = ESMByteConvert.extractInt3(prefix, 12);
								formID = formID & 0xffffff | masterID << 24;
								long filePositionPointer = pos - headerByteCount;// go back to the start of the header
								idToFormMap.put(formID, new FormInfo(recordType, formID, filePositionPointer));
								int length = ESMByteConvert.extractInt(prefix, 4);
								pos += length;
							} else {
								PluginRecord record = new PluginRecord(prefix);
								int formID = record.getFormID();

								if (record.isDeleted()	|| record.isIgnored() || formID == 0
									|| formID >>> 24 < masterID) {
									pos += recordLength;
								} else {
									record.load(in, pos);
									pos += recordLength;
									formID = formID & 0xffffff | masterID << 24;
									idToFormMap.put(formID, new FormInfo(recordType, formID, record));
								}

							}
							groupLength -= recordLength + headerByteCount;
						}
					}

					if (groupLength != 0) {
						throw new PluginException(
								masterHeader.getName() + ": Group " + groupRecordType + " is incomplete");
					}
				}
			}

			// prep for the next loop
			count = ch.read((ByteBuffer)pbb.rewind(), pos);	
			pos += prefix.length;
		}

		addGeckDefaultObjects();

		// now establish min and max form id range
		for (int formId : idToFormMap.keySet()) {
			minFormId = formId < minFormId ? formId : minFormId;
			maxFormId = formId > maxFormId ? formId : maxFormId;
		}
		for (FormToFilePointer cp : getAllInteriorCELLFormIds()) {
			int formId = cp.formId;
			minFormId = formId < minFormId ? formId : minFormId;
			maxFormId = formId > maxFormId ? formId : maxFormId;
		}

		for (int formId : getAllWRLDTopGroupFormIds()) {
			minFormId = formId < minFormId ? formId : minFormId;
			maxFormId = formId > maxFormId ? formId : maxFormId;
		}
	

		System.out.println(
				"Finished loading ESM file " + masterHeader.getName() + " in " + (System.currentTimeMillis() - start));
		return true;
	}

	@Override
	public int[] getAllWRLDTopGroupFormIds() {
		if (wRLDTopGroup != null) {
			return wRLDTopGroup.WRLDByFormId.keySet();
		} else {
			//happens if the esp file is bum
			System.out.println("no wRLDTopGroup in  ESM file " + masterHeader.getName());
			return new int[0];
		}
	}

	/**
	 * I have found that when the GECK is opened with no data file, many many object already exist, inside the exe
	 * possibly? Once the Fallout3.esm file is loaded some of these are overwritten with new data.
	 * 
	 * This loader that only load the esm and has not the other data, appears to find most of these magic records (that
	 * is they are in the esm as well) But at least one isn't: decimal 32 (hex 20) PortalMarker STAT is not in my
	 * version of the data The STATs RoomMarker and PortalMarker are in the magic list, but Fallout3.esm only loads the
	 * RoomMarker If I find any others (usually by a request to get PluginRecord returning null), I should work out what
	 * they are and add them here
	 * 
	 * @param idToFormMap2
	 */
	private void addGeckDefaultObjects() {
		//fallout3 formId 32 stat PortalMarker
		if (masterHeader.getName().equals("Fallout3.esm")) {
			PluginRecord pr = new PluginRecord(24, "STAT", 32);
			PluginSubrecord psr = new PluginSubrecord("EDID", "PortalMarkerZ".getBytes()); //note the null termination padding
			pr.getSubrecords().add(psr);
			idToFormMap.put(32, new FormInfo("STAT", 32, pr));
		}
	}

	@Override
	public String getName() {
		return masterHeader.getName();
	}

	@Override
	public float getVersion() {
		return masterHeader.getVersion();
	}

}
