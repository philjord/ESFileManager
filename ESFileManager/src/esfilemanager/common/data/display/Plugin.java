package esmio.common.data.display;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;

import esmio.common.PluginException;
import esmio.common.data.plugin.FormInfo;
import esmio.common.data.plugin.PluginGroup;
import esmio.common.data.plugin.PluginHeader;
import esmio.common.data.plugin.PluginRecord;
import esmio.common.data.record.Record;
import tools.io.ESMByteConvert;
import tools.io.FileChannelRAF;

public abstract class Plugin {
	protected FileChannelRAF		in;											//created by sub class

	private PluginHeader			pluginHeader;

	private List<PluginGroup>		groupList	= new ArrayList<PluginGroup>();

	private List<FormInfo>			formList;

	private Map<Integer, FormInfo>	formMap;

	protected Plugin(String pluginFileName) {
		pluginHeader = new PluginHeader(pluginFileName);
	}

	public String getName() {
		return pluginHeader.getName();
	}

	public PluginHeader getPluginHeader() {
		return pluginHeader;
	}

	public List<FormInfo> getFormList() {
		return formList;
	}

	public Map<Integer, FormInfo> getFormMap() {
		return formMap;
	}

	public List<PluginGroup> getGroupList() {
		return groupList;
	}

	@Override
	public String toString() {
		return pluginHeader.getName();
	}

	/**
	 * This method assumes an index only version is required
	 * @throws PluginException
	 * @throws DataFormatException
	 * @throws IOException
	 */
	public abstract void load() throws PluginException, DataFormatException, IOException;

	public abstract void load(boolean indexCellsOnly) throws PluginException, DataFormatException, IOException;

	public void load(boolean indexCellsOnly, FileChannelRAF in)
			throws PluginException, DataFormatException, IOException {
		this.in = in;
		pluginHeader.load(pluginHeader.getName(), in);
		
		if( pluginHeader.getPluginFileFormat().equals("TES3"))
		{
			loadTES3(indexCellsOnly, in);
			return;
		}
		int recordCount = pluginHeader.getRecordCount();
		formList = new ArrayList<FormInfo>(recordCount);
		formMap = new HashMap<Integer, FormInfo>(recordCount);
		byte prefix[] = new byte[pluginHeader.getHeaderByteCount()];
		int loadCount = 0;
		int count = in.read(prefix);
		while (count != -1) {
			if (count != pluginHeader.getHeaderByteCount())
				throw new PluginException(pluginHeader.getName() + ": Group record prefix is too short");

			String type = new String(prefix, 0, 4);
			if (!type.equals("GRUP"))
				throw new PluginException(pluginHeader.getName() + ": Top-level record is not a group");
			if (prefix [12] != 0)
				throw new PluginException(pluginHeader.getName() + ": Top-level group type is not 0");

			int length = ESMByteConvert.extractInt(prefix, 4);
			length -= pluginHeader.getHeaderByteCount();

			PluginGroup group = new PluginGroup(prefix);
			//Dear god this String fileName appears to do something magical without it failures!			
			group.load("", in, length);

			// if requested we only index the wrld and cell records, as that is 99% of the file size
			if (indexCellsOnly
				&& (group.getGroupRecordType().equals("WRLD") || group.getGroupRecordType().equals("CELL"))) {
				updateFormListPointersOnly(group, formList);
			} else {
				updateFormList(group, formList);
				groupList.add(group);
				loadCount += group.getRecordCount() + 1;
			}

			//prep for the next iter
			count = in.read(prefix);
		}

		if (loadCount != recordCount) {
			System.out.println(pluginHeader.getName()	+ ": Load count " + loadCount + " does not match header count "
								+ recordCount + ", presumably it is only indexed");
		}

		//I think this is for empty esm files?
		/*	if (pluginHeader.getMasterList().size() == 0)
		 {
		 Integer refFormID = new Integer(20);
		 if (formMap.get(refFormID) == null)
		 {
		 FormInfo playerInfo = (FormInfo) formMap.get(new Integer(7));
		 if (playerInfo != null
		 && playerInfo.getRecordType().equals("NPC_")
		 && playerInfo.getEditorID().equals("Player"))
		 {
		 FormInfo playerRefInfo = new FormInfo(null, "REFR", 20, "PlayerREF");
		 formList.add(playerRefInfo);
		 formMap.put(refFormID, playerRefInfo);
		 }
		 }
		 }*/

	}

	private void loadTES3(boolean indexCellsOnly, FileChannelRAF in) throws IOException, PluginException, DataFormatException {
		
		
		// fake top groups for now, to butcher with later, cells and everything else
		PluginGroup topPG = new esmio.tes3.PluginGroup(PluginGroup.TOP);
		groupList.add(topPG);
		PluginGroup cellPG = new esmio.tes3.PluginGroup(PluginGroup.CELL);
		topPG.getRecordList().add(cellPG);
		
		LinkedHashMap<String, PluginGroup> typeToGroup = new LinkedHashMap<String, PluginGroup>();
		
		for(String grup : esmio.tes3.PluginRecord.edidRecords) {
			PluginGroup pg = new esmio.tes3.PluginGroup(PluginGroup.TOP, grup);
			topPG.getRecordList().add(pg);
			typeToGroup.put(grup, pg);
		}
		for(String grup : esmio.tes3.PluginRecord.nonEdidRecords) {
			PluginGroup pg = new esmio.tes3.PluginGroup(PluginGroup.TOP, grup);
			topPG.getRecordList().add(pg);
			typeToGroup.put(grup, pg);
		}
			
		
		int recordCount = pluginHeader.getRecordCount();
		formList = new ArrayList<FormInfo>(recordCount);
		formMap = new HashMap<Integer, FormInfo>(recordCount);
		byte prefix[] = new byte[pluginHeader.getHeaderByteCount()];
		int loadCount = 0;
		int count = in.read(prefix);
		while (count != -1) {
			if (count != pluginHeader.getHeaderByteCount())
				throw new PluginException(pluginHeader.getName() + ": Group record prefix is too short");

			String type = new String(prefix, 0, 4);
			int length = ESMByteConvert.extractInt(prefix, 4);
			length -= pluginHeader.getHeaderByteCount();

			PluginRecord record = new esmio.tes3.PluginRecord(-1, prefix);
			record.load("", in, -1);
			loadCount++;

			if(type.equals("CELL")) {
				cellPG.getRecordList().add(record);
			} else {
				if(typeToGroup.get(type) == null)
					System.out.println("poo type " + type);
				typeToGroup.get(type).getRecordList().add(record);
			}				

			//prep for the next iter
			count = in.read(prefix);
		}

		if (loadCount != recordCount) {
			System.out.println(pluginHeader.getName()	+ ": Load count " + loadCount + " does not match header count "
								+ recordCount + ", presumably it is only indexed");
		}
		
		
		
 /*

		//add a single wrld indicator, to indicate the single morrowind world, id MUST be wrldFormId (0)!
		PluginRecord wrldRecord = new PluginRecord(0, "WRLD", "MorrowindWorld");
		idToFormMap.put(wrldRecord.getFormID(),
				new FormInfo(wrldRecord.getRecordType(), wrldRecord.getFormID(), wrldRecord));

		while (in.getFilePointer() < in.length()) {
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

			if (recordType.equals("CELL")) {
				//	looks like x = 23 to -18 y is 27 to -17  so 50 wide with an x off of +25 and y of +20
				CELLPluginGroup cellPluginGroup = new CELLPluginGroup(prefix, in);

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
				in.skipBytes(recordSize);
			} else if (recordType.equals("DIAL")) {
				DIALRecord dial = new DIALRecord(formID, prefix, in);
				dials.put(dial.getEditorID(), dial);
			} else {
				PluginRecord record = new PluginRecord(formID, prefix);
				record.load("", in, -1);

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
		*/
	}

	public static void updateFormList(PluginGroup pg, List<FormInfo> formList) {
		for (Record r : pg.getRecordList()) {
			PluginRecord record = (PluginRecord)r;
			if (!record.isIgnored() || (record instanceof PluginGroup)) {
				FormInfo formInfo = new FormInfo(record.getRecordType(), record.getFormID(), record);
				formList.add(formInfo);

				if (record instanceof PluginGroup) {
					updateFormList((PluginGroup)record, formList);
				}
			}
		}
	}

	public static void updateFormListPointersOnly(PluginGroup pg, List<FormInfo> formList) {
		for (Record r : pg.getRecordList()) {
			PluginRecord record = (PluginRecord)r;
			if (!record.isIgnored() || (record instanceof PluginGroup)) {
				//record.setParent(this);
				FormInfo formInfo = new FormInfo(record.getRecordType(), record.getFormID(),
						record.getFilePositionPointer());
				formList.add(formInfo);
				if (record instanceof PluginGroup) {
					updateFormListPointersOnly((PluginGroup)record, formList);
				}
			}
		}
	}
}
