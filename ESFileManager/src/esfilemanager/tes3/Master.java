package esfilemanager.tes3;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
 

import com.frostwire.util.SparseArray;

import esfilemanager.common.PluginException;
import esfilemanager.common.data.plugin.FormInfo;
import esfilemanager.common.data.plugin.PluginSubrecord;
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
public class Master implements IMasterTes3 {
	private String									fileName;

	private FileChannelRAF							in;

	private PluginHeader							masterHeader;

	//Note NO CELLs or DIALs in the following 3 all CELLs in the Cells sets below
	private SparseArray<FormInfo>					idToFormMap;

	private LinkedHashMap<String, Integer>			edidToFormIdMap;

	// waay over sized just in case!
	private CELLPluginGroup[][]						exteriorCells			= new CELLPluginGroup[100][100];

	private LinkedHashMap<String, DIALRecord>		dials;

	private LinkedHashMap<String, CELLPluginGroup>	interiorCellsByEdid		= new LinkedHashMap<String, CELLPluginGroup>();
	private SparseArray<CELLPluginGroup>			interiorCellsByFormId	= new SparseArray<CELLPluginGroup>();

	private static int								currentFormId			= 0;

	private int										minFormId				= Integer.MAX_VALUE;

	private int										maxFormId				= Integer.MIN_VALUE;

	// used to indicate the single morrowind world, added first
	public static int								wrldFormId				= 0;

	public Master(FileChannelRAF in, String fileName) {
		this.in = in;
		this.fileName = fileName;
		masterHeader = new PluginHeader();
		// in case we've loaded a master already and this is a new load, in that case start again
		// so the form id's correctly start from 0 again
		resetFormId();
	}

	@Override
	public String getName() {
		return masterHeader.getName();
	}

	@Override
	public float getVersion() {
		return masterHeader.getVersion();
	}

	@Override
	public int getMinFormId() {
		return minFormId;
	}

	@Override
	public int getMaxFormId() {
		return maxFormId;
	}

	public static void resetFormId() {
		currentFormId = 0;
	}

	public static int getNextFormId() {
		return currentFormId++;
	}

	@Override
	public int[] getAllFormIds() {
		return idToFormMap.keySet();
	}

	@Override
	public SparseArray<FormInfo> getFormMap() {
		return idToFormMap;
	}


	public void load() throws PluginException, IOException {
		System.out.println("Loading ESM file " + fileName);
		long start = System.currentTimeMillis();

		FileChannel ch = in.getChannel();
		long pos = 0;// keep track of the pos in the file, so we don't use any file pointers

		int count = masterHeader.load(fileName, in, pos);
		pos += count;
		
		idToFormMap = new SparseArray<FormInfo>();
		edidToFormIdMap = new LinkedHashMap<String, Integer>();
		dials = new LinkedHashMap<String, DIALRecord>();

		//add a single wrld indicator, to indicate the single morrowind world, id MUST be wrldFormId (0)!
		PluginRecord wrldRecord = new PluginRecord(currentFormId++, "WRLD", "MorrowindWorld");
		idToFormMap.put(wrldRecord.getFormID(),
				new FormInfo(wrldRecord.getRecordType(), wrldRecord.getFormID(), wrldRecord));

		while (pos < ch.size()) {
			// pull the prefix data so we know what sort of record we need to load
			byte[] prefix = new byte[16];
			count = ch.read(ByteBuffer.wrap(prefix), pos);	
			pos += prefix.length;
			if (count != 16)
				throw new PluginException(": record prefix is incomplete");

			String recordType = new String(prefix, 0, 4);
			//recordSize = ESMByteConvert.extractInt(prefix, 4);
			//unknownInt = ESMByteConvert.extractInt(prefix, 8);
			//recordFlags1 = ESMByteConvert.extractInt(prefix, 12);

			int formID = getNextFormId();

			if (recordType.equals("CELL")) {
				//	looks like x = 23 to -18 y is 27 to -17  so 50 wide with an x off of +25 and y of +20
				CELLPluginGroup cellPluginGroup = new CELLPluginGroup(prefix, in, pos);
				pos += cellPluginGroup.getRecordSize();

				if (cellPluginGroup.isExterior) {
					int xIdx = cellPluginGroup.cellX + 50;
					int yIdx = cellPluginGroup.cellY + 50;
					exteriorCells [xIdx] [yIdx] = cellPluginGroup;
				} else {
					interiorCellsByEdid.put(cellPluginGroup.getEditorID(), cellPluginGroup);
					interiorCellsByFormId.put(cellPluginGroup.getFormID(), cellPluginGroup);
				}
			} else if (recordType.equals("LAND")) {
				//land are fully skipped as they get loaded with the owner cell at cell load time later
				int recordSize = ESMByteConvert.extractInt(prefix, 4);
				pos += recordSize;
			} else if (recordType.equals("DIAL")) {
				DIALRecord dial = new DIALRecord(formID, prefix, in, pos);
				pos += dial.recordSize;
				dials.put(dial.getEditorID(), dial);
			} else {
				PluginRecord record = new PluginRecord(formID, prefix);
				record.load(in, pos, record.recordSize);
				pos += record.recordSize;

				// 1 length are single 0's
				if (record.getEditorID() != null && record.getEditorID().length() > 1) {
					edidToFormIdMap.put(record.getEditorID(), new Integer(formID));
				}

				// every thing else gets stored as a record
				FormInfo info = new FormInfo(record.getRecordType(), formID, record);
				idToFormMap.put(formID, info);
			}

		}

		// now establish min and max form id range
		minFormId = 0;
		maxFormId = currentFormId - 1;
		
		System.out.println(
				"Finished loading ESM file " + masterHeader.getName() + " in " + (System.currentTimeMillis() - start));
	}

	/**
	 * Not for CELLs
	 */
	@Override
	public PluginRecord getPluginRecord(int formID) throws PluginException {
		FormInfo formInfo = idToFormMap.get(formID);

		if (formInfo == null) {
			throw new PluginException(
					"" + fileName + ": Record " + formID + " not found, it may be a CELL or WRLD record");
		}
		return (PluginRecord)formInfo.getPluginRecord();
	}

	@Override
	public WRLDTopGroup getWRLDTopGroup() {
		throw new UnsupportedOperationException();
	}

	@Override
	public InteriorCELLTopGroup getInteriorCELLTopGroup() {
		throw new UnsupportedOperationException();
	}

	@Override
	public PluginRecord getWRLD(int formID) throws  IOException, PluginException {
		if (formID == wrldFormId) {
			PluginRecord wrld = getPluginRecord(formID);
			// loaded as a cell so we'll fake it up
			wrld.getSubrecords().add(new PluginSubrecord("NAME", "Morrowind".getBytes()));
			wrld.getSubrecords().add(new PluginSubrecord("DATA", new byte[12]));
			return wrld;
		}
		// no message as null indicates a non world formid
		return null;
	}

	@Override
	public WRLDChildren getWRLDChildren(int formID) {
		return null;
	}

	@Override
	public PluginRecord getWRLDExtBlockCELL(int wrldFormId2, int x, int y)
			throws  IOException, PluginException {
		if (wrldFormId2 != wrldFormId) {
			new Throwable("bad morrowind world id! " + wrldFormId2).printStackTrace();
		}
		int xIdx = x + 50;
		int yIdx = y + 50;
		if (xIdx > 0 && yIdx > 0 && xIdx < 100 && yIdx < 100) {
			CELLPluginGroup cellPluginGroup = exteriorCells [xIdx] [yIdx];
			if (cellPluginGroup != null) {
				// make sure no one else asks for it while we check load state
				synchronized (cellPluginGroup) {
					if (!cellPluginGroup.isLoaded()) {
						cellPluginGroup.load(in);
					}
				}

				return cellPluginGroup.createPluginRecord();
			}
		}
		return null;

	}

	@Override
	public PluginGroup getWRLDExtBlockCELLChildren(int wrldFormId2, int x, int y)
			throws  IOException, PluginException {
		if (wrldFormId2 != wrldFormId) {
			new Throwable("bad morrowind world id! " + wrldFormId2).printStackTrace();
		}
		int xIdx = x + 50;
		int yIdx = y + 50;
		CELLPluginGroup cellPluginGroup = exteriorCells [xIdx] [yIdx];
		if (cellPluginGroup != null) {
			// make sure no one else asks for it while we check load state
			synchronized (cellPluginGroup) {
				if (!cellPluginGroup.isLoaded()) {
					cellPluginGroup.load(in);
				}
			}

			return cellPluginGroup;
		}
		return null;
	}

	@Override
	public PluginRecord getInteriorCELL(int formID) throws IOException, PluginException {
		CELLPluginGroup cellPluginGroup = interiorCellsByFormId.get(formID);
		if (cellPluginGroup != null) {
			// make sure no one else asks for it while we check load state
			synchronized (cellPluginGroup) {
				if (!cellPluginGroup.isLoaded()) {
					cellPluginGroup.load(in);
				}
			}
			return cellPluginGroup.createPluginRecord();
		}
		return null;
	}

	@Override
	public PluginGroup getInteriorCELLChildren(int formID) throws IOException, PluginException {
		CELLPluginGroup cellPluginGroup = interiorCellsByFormId.get(formID);
		if (cellPluginGroup != null) {
			// make sure no one else asks for it while we check load state
			synchronized (cellPluginGroup) {
				if (!cellPluginGroup.isLoaded()) {
					cellPluginGroup.load(in);
				}
			}
			return cellPluginGroup;
		}
		return null;

	}

	@Override
	public PluginGroup getInteriorCELLPersistentChildren(int formID)
			throws IOException, PluginException {
		//To my knowledge these don't exist in any real manner
		throw new UnsupportedOperationException();
	}

	@Override
	public List<FormToFilePointer> getAllInteriorCELLFormIds() {
		ArrayList<FormToFilePointer> ret = new ArrayList<FormToFilePointer>();
		for (int formId : interiorCellsByFormId.keySet()) {
			ret.add(new FormToFilePointer(formId, -1));
		}

		return ret;
	}

	@Override
	public int[] getAllWRLDTopGroupFormIds() {
		return new int[] {wrldFormId};
	}

	@Override
	public int convertNameRefToId(String key) {
		Integer id = edidToFormIdMap.get(key);
		if (id != null) {
			return id.intValue();
		} else {
			CELLPluginGroup cpg = interiorCellsByEdid.get(key);
			if (cpg != null)
				return cpg.getFormID();
			else
				return -1;
		}
	}

}
