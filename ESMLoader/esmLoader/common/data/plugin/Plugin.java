package esmLoader.common.data.plugin;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;

import tools.io.ESMByteConvert;
import esmLoader.common.PluginException;

public class Plugin implements PluginInterface
{
	private File pluginFile;

	private RandomAccessFile in;

	private PluginHeader pluginHeader;

	private List<PluginGroup> groupList = new ArrayList<PluginGroup>();

	private List<FormInfo> formList;

	private Map<Integer, FormInfo> formMap;

	public Plugin(File pluginFile)
	{
		this.pluginFile = pluginFile;
		pluginHeader = new PluginHeader(pluginFile.getName());
	}

	public String getName()
	{
		return pluginFile.getName();
	}

	public PluginHeader getPluginHeader()
	{
		return pluginHeader;
	}

	public List<FormInfo> getFormList()
	{
		return formList;
	}

	public Map<Integer, FormInfo> getFormMap()
	{
		return formMap;
	}

	public List<PluginGroup> getGroupList()
	{
		return groupList;
	}

	public String toString()
	{
		return pluginFile.getName();
	}

	/**
	 * This method assumes an index only version is required
	 * @throws PluginException
	 * @throws DataFormatException
	 * @throws IOException
	 */
	public void load() throws PluginException, DataFormatException, IOException
	{
		load(true);
	}

	public void load(boolean indexCellsOnly) throws PluginException, DataFormatException, IOException
	{
		if (!pluginFile.exists() || !pluginFile.isFile())
			throw new IOException("Plugin file '" + pluginFile.getName() + "' does not exist");

		in = new RandomAccessFile(pluginFile, "r");
		pluginHeader.read(in);
		int recordCount = pluginHeader.getRecordCount();
		formList = new ArrayList<FormInfo>(recordCount);
		formMap = new HashMap<Integer, FormInfo>(recordCount);
		byte prefix[] = new byte[pluginHeader.getHeaderByteCount()];
		int loadCount = 0;
		int count = in.read(prefix);
		while (count != -1)
		{
			if (count != pluginHeader.getHeaderByteCount())
				throw new PluginException(pluginFile.getName() + ": Group record prefix is too short");

			String type = new String(prefix, 0, 4);
			if (!type.equals("GRUP"))
				throw new PluginException(pluginFile.getName() + ": Top-level record is not a group");
			if (prefix[12] != 0)
				throw new PluginException(pluginFile.getName() + ": Top-level group type is not 0");

			int length = ESMByteConvert.extractInt(prefix, 4);
			length -= pluginHeader.getHeaderByteCount();

			PluginGroup group = new PluginGroup(prefix);
			group.load(pluginFile.getName(), in, length);

			// if requested we only index the wrld and cell records, as that is 99% of the file size
			if (indexCellsOnly && (group.getGroupRecordType().equals("WRLD") || group.getGroupRecordType().equals("CELL")))
			{
				updateFormListPointersOnly(group, formList);
			}
			else
			{
				updateFormList(group, formList);
				groupList.add(group);
				loadCount += group.getRecordCount() + 1;
			}

			//prep for the next iter
			count = in.read(prefix);
		}

		if (loadCount != recordCount)
		{
			System.out.println(pluginFile.getName() + ": Load count " + loadCount + " does not match header count " + recordCount
					+ ", presumably it is only indexed");
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

	public static void updateFormList(PluginGroup pg, List<FormInfo> formList)
	{
		for (PluginRecord record : pg.getRecordList())
		{
			if (!record.isIgnored() || (record instanceof PluginGroup))
			{
				record.setParent(pg);
				FormInfo formInfo = new FormInfo(record.getRecordType(), record.getFormID(), record.getEditorID(), record);
				formInfo.setParentFormID(pg.getGroupParentID());
				formList.add(formInfo);

				if (record instanceof PluginGroup)
				{
					updateFormList((PluginGroup) record, formList);
				}
			}
		}
	}

	public static void updateFormListPointersOnly(PluginGroup pg, List<FormInfo> formList)
	{
		for (PluginRecord record : pg.getRecordList())
		{
			if (!record.isIgnored() || (record instanceof PluginGroup))
			{
				//record.setParent(this);
				FormInfo formInfo = new FormInfo(record.getRecordType(), record.getFormID(), record.getEditorID(), record.getFilePositionPointer());
				formInfo.setParentFormID(pg.getGroupParentID());
				formList.add(formInfo);
				if (record instanceof PluginGroup)
				{
					updateFormListPointersOnly((PluginGroup) record, formList);
				}
			}
		}
	}
}
