package esfilemanager.common.data.display;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;

import esfilemanager.common.PluginException;
import esfilemanager.common.data.plugin.FormInfo;
import esfilemanager.common.data.plugin.PluginGroup;
import esfilemanager.common.data.plugin.PluginHeader;
import esfilemanager.common.data.plugin.PluginRecord;
import esfilemanager.common.data.plugin.PluginHeader.GAME;
import esfilemanager.common.data.record.Record;
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
		FileChannel ch = in.getChannel();
		long pos = 0;// keep track of the pos in the file, so we don't use any file pointers
		
		int count = pluginHeader.load(pluginHeader.getName(), ch, pos);
		pos += count;
		
		if(pluginHeader.getGame() == GAME.TES3)
		{
			loadTES3(indexCellsOnly, in, pos);
			return;
		}
		
		// fake top group for the header to live in
		PluginGroup topPG = new esfilemanager.tes3.PluginGroup(PluginGroup.TOP, "0HED");
		groupList.add(topPG);
		topPG.getRecordList().add(pluginHeader);

		
		int recordCount = pluginHeader.getRecordCount();
		formList = new ArrayList<FormInfo>(recordCount);
		formMap = new HashMap<Integer, FormInfo>(recordCount);
		byte prefix[] = new byte[pluginHeader.getHeaderByteCount()];
		int loadCount = 0; 
		count = ch.read(ByteBuffer.wrap(prefix), pos);	
		pos += prefix.length;
		
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
			group.load(in, pos, length);
			pos += length;
			
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
			count = ch.read(ByteBuffer.wrap(prefix), pos);	
			pos += prefix.length;
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
	
	
	private void loadTES3(boolean indexCellsOnly, FileChannelRAF in, long pos) throws IOException, PluginException, DataFormatException {
		FileChannel ch = in.getChannel();
		// fake top groups for now, to butcher with later, cells and everything else
		PluginGroup topPG = new esfilemanager.tes3.PluginGroup(PluginGroup.TOP);
		groupList.add(topPG);
		topPG.getRecordList().add(pluginHeader);
		PluginGroup cellPG = new esfilemanager.tes3.PluginGroup(PluginGroup.CELL);
		topPG.getRecordList().add(cellPG);
		
		LinkedHashMap<String, PluginGroup> typeToGroup = new LinkedHashMap<String, PluginGroup>();
		
		int recordCount = pluginHeader.getRecordCount();
		formList = new ArrayList<FormInfo>(recordCount);
		formMap = new HashMap<Integer, FormInfo>(recordCount);
		byte prefix[] = new byte[pluginHeader.getHeaderByteCount()];
		int loadCount = 0;
		int count = ch.read(ByteBuffer.wrap(prefix), pos);	
		pos += prefix.length;
		while (count != -1) {
			if (count != pluginHeader.getHeaderByteCount())
				throw new PluginException(pluginHeader.getName() + ": Group record prefix is too short");

			String type = new String(prefix, 0, 4);
			int length = ESMByteConvert.extractInt(prefix, 4);//Notice for TES3 header length not taken off record length			

			PluginRecord record = new esfilemanager.tes3.PluginRecord(-1, prefix);
			record.load(in, pos);
			pos += length;
			loadCount++;

			if(type.equals("CELL")) {
				cellPG.getRecordList().add(record);
			} else {
				if(!esfilemanager.tes3.PluginRecord.edidRecordSet.contains(type) 
						&& !esfilemanager.tes3.PluginRecord.nonEdidRecordSet.contains(type) )
					System.out.println("unseen type " + type);
				
				PluginGroup pg = typeToGroup.get(type);
				if(pg == null) {
					pg = new esfilemanager.tes3.PluginGroup(PluginGroup.TOP, type);
					topPG.getRecordList().add(pg);
					typeToGroup.put(type, pg);
				}
				
				pg.getRecordList().add(record);
			}				

			//prep for the next iter
			count = ch.read(ByteBuffer.wrap(prefix), pos);	
			pos += prefix.length;
		}

		if (loadCount != recordCount) {
			System.out.println(pluginHeader.getName()	+ ": TES3 Load count " + loadCount + " does not match header count "
								+ recordCount + ", presumably it is only indexed");
		}		
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
