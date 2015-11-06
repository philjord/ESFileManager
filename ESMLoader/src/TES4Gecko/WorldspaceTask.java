package TES4Gecko;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.DataFormatException;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class WorldspaceTask extends WorkerTask
{
	private boolean insertPlaceholders = false;

	private boolean pluginModified = false;

	private File pluginFile;

	private Plugin plugin;

	private int pluginIndex;

	private Master[] masters;

	private List<FormInfo> worldspaceList;

	private List<String> worldspaceNames;

	private Map<Integer, Integer> worldspaceMap;

	private List<Integer> distantList;

	private int baseFormID;

	public WorldspaceTask(StatusDialog statusDialog, File pluginFile, int options)
	{
		super(statusDialog);
		this.pluginFile = pluginFile;
		if ((options & 0x1) == 1)
			this.insertPlaceholders = true;
	}

	public static void moveWorldspaces(JFrame parent, File pluginFile, int options)
	{
		StatusDialog statusDialog = new StatusDialog(parent, "Moving worldspaces for " + pluginFile.getName(), "Move Worldspaces");

		WorldspaceTask worker = new WorldspaceTask(statusDialog, pluginFile, options);
		statusDialog.setWorker(worker);

		worker.start();
		statusDialog.showDialog();

		if (statusDialog.getStatus() == 1)
			JOptionPane.showMessageDialog(parent, pluginFile.getName() + " updated", "Move Worldspaces", 1);
		else
			JOptionPane.showMessageDialog(parent, "Unable to move worldspaces for " + pluginFile.getName(), "Move Worldspaces", 1);
	}

	public void run()
	{
		boolean completed = false;
		this.worldspaceList = new ArrayList<FormInfo>(50);
		this.worldspaceNames = new ArrayList<String>(50);
		this.worldspaceMap = new HashMap<Integer, Integer>(50);
		this.distantList = new ArrayList<Integer>(50);
		try
		{
			this.plugin = new Plugin(this.pluginFile);
			this.plugin.load(this);

			List<?> masterList = this.plugin.getMasterList();
			this.pluginIndex = masterList.size();
			if (this.pluginIndex > 0)
			{
				this.masters = new Master[this.pluginIndex];
				for (int i = 0; i < this.pluginIndex; i++)
				{
					String masterName = (String) masterList.get(i);
					File masterFile = new File(Main.pluginDirectory + Main.fileSeparator + masterName);
					this.masters[i] = new Master(masterFile);
					this.masters[i].load(this);
				}

			}

			Date date = new Date();
			this.baseFormID = ((int) (date.getTime() / 1000L - 1176047100L) % 14680064 + 2097152);
			if (Main.debugMode)
			{
				System.out.printf("Base worldspace form ID is %08X\n", new Object[]
				{ Integer.valueOf(this.baseFormID) });
			}

			getStatusDialog().updateMessage("Moving worldspaces for " + this.plugin.getName());

			if (this.pluginIndex > 0)
			{
				mapWorldspaces();

				String lodPath = String.format("%s%sDistantLOD", new Object[]
				{ Main.pluginDirectory, Main.fileSeparator });
				File lodDir = new File(lodPath);
				if ((lodDir.exists()) && (lodDir.isDirectory()))
				{
					mapDistantStatics(lodDir);
				}

				relocateWorldspaces();

				updateReferences();

				if ((lodDir.exists()) && (lodDir.isDirectory()))
				{
					updateDistantStatics(lodDir);
				}

				if (this.insertPlaceholders)
				{
					createPlaceholders();
				}

				String dirPath = String.format("%s%sMeshes%sLandscape%sLOD", new Object[]
				{ Main.pluginDirectory, Main.fileSeparator, Main.fileSeparator, Main.fileSeparator });
				File dirFile = new File(dirPath);
				if ((dirFile.exists()) && (dirFile.isDirectory()))
				{
					renameFiles(dirFile);
				}

				dirPath = String.format("%s%sTextures%sLandscapeLOD%sGenerated", new Object[]
				{ Main.pluginDirectory, Main.fileSeparator, Main.fileSeparator, Main.fileSeparator });
				dirFile = new File(dirPath);
				if ((dirFile.exists()) && (dirFile.isDirectory()))
				{
					renameFiles(dirFile);
				}

			}

			if (this.pluginModified)
			{
				this.plugin.store(this);
			}
			completed = true;
		}
		catch (PluginException exc)
		{
			Main.logException("Plugin Error", exc);
		}
		catch (DataFormatException exc)
		{
			Main.logException("Compression Error", exc);
		}
		catch (IOException exc)
		{
			Main.logException("I/O Error", exc);
		}
		catch (InterruptedException exc)
		{
			WorkerDialog.showMessageDialog(getStatusDialog(), "Request canceled", "Interrupted", 0);
		}
		catch (Throwable exc)
		{
			Main.logException("Exception while moving worldspaces", exc);
		}

		getStatusDialog().closeDialog(completed);
	}

	private void mapWorldspaces() throws DataFormatException, IOException, PluginException
	{
		List<FormInfo> formList = this.plugin.getFormList();
		Map<?, ?> formMap = this.plugin.getFormMap();
		Map<?, ?> masterFormMap = this.masters[0].getFormMap();

		for (FormInfo formInfo : formList)
		{
			String recordType = formInfo.getRecordType();
			int formID = formInfo.getFormID();
			int modIndex = formID >>> 24;

			if (recordType.equals("WRLD"))
			{
				if (modIndex >= this.pluginIndex)
				{
					this.worldspaceList.add(formInfo);
				}
				else if (modIndex > 0)
				{
					Master checkMaster = this.masters[modIndex];
					int checkFormID = formID & 0xFFFFFF | checkMaster.getMasterList().size() << 24;
					FormInfo checkFormInfo = checkMaster.getFormMap().get(new Integer(checkFormID));
					if (checkFormInfo != null)
					{
						PluginRecord checkRecord = checkMaster.getRecord(checkFormID);
						if (checkRecord.getRecordType().equals("BOOK"))
						{
							String editorID = checkRecord.getEditorID();
							if ((editorID.length() == 17) && (editorID.substring(0, 9).equals("TES4Gecko")))
							{
								this.worldspaceList.add(formInfo);
							}
						}

					}

				}

				this.worldspaceNames.add(formInfo.getEditorID().toLowerCase());
			}
			else
			{
				if ((!recordType.equals("REFR")) && (!recordType.equals("ACHR")) && (!recordType.equals("ACRE")))
				{
					continue;
				}

				PluginRecord record = (PluginRecord) formInfo.getSource();
				int recordFlags = record.getRecordFlags();
				int baseFormID = 0;
				List<PluginSubrecord> subrecords = record.getSubrecords();
				for (PluginSubrecord subrecord : subrecords)
				{
					if (subrecord.getSubrecordType().equals("NAME"))
					{
						byte[] subrecordData = subrecord.getSubrecordData();
						baseFormID = SerializedElement.getInteger(subrecordData, 0);
						break;
					}
				}

				int baseModIndex = baseFormID >>> 24;
				if ((baseFormID != 0) && (baseModIndex > 0))
				{
					Integer lookupFormID = new Integer(baseFormID);
					FormInfo baseFormInfo = (FormInfo) formMap.get(lookupFormID);
					if (baseFormInfo == null)
					{
						if (baseModIndex < this.pluginIndex)
						{
							Master checkMaster = this.masters[baseModIndex];
							int checkFormID = baseFormID & 0xFFFFFF | checkMaster.getMasterList().size() << 24;
							baseFormInfo = checkMaster.getFormMap().get(new Integer(checkFormID));
							if (baseFormInfo != null)
							{
								PluginRecord checkRecord = checkMaster.getRecord(checkFormID);
								boolean relocated = false;
								if (checkRecord.getRecordType().equals("BOOK"))
								{
									String editorID = checkRecord.getEditorID();
									if ((editorID.length() == 17) && (editorID.substring(0, 9).equals("TES4Gecko")))
									{
										relocated = true;
									}
								}
								if ((relocated) || ((recordFlags & 0x8000) != 0))
								{
									FormInfo relocFormInfo = new FormInfo(baseFormInfo.getSource(), baseFormInfo.getRecordType(),
											baseFormID, baseFormInfo.getEditorID());
									relocFormInfo.setPlugin(baseFormInfo.getPlugin());
									if (!this.worldspaceList.contains(relocFormInfo))
									{
										this.worldspaceList.add(relocFormInfo);
										if (Main.debugMode)
											System.out.printf(
													"Relocation required for reference %08X to %s base item %s (%08X)\n",
													new Object[]
													{ Integer.valueOf(formInfo.getFormID()), baseFormInfo.getRecordType(),
															baseFormInfo.getEditorID(), Integer.valueOf(baseFormID) });
									}
								}
							}
						}
					}
					else if (((recordFlags & 0x8000) != 0) && (!this.worldspaceList.contains(baseFormInfo)))
					{
						this.worldspaceList.add(baseFormInfo);
						if (Main.debugMode)
							System.out.printf("Relocation required for reference %08X to %s base item %s (%08X)\n",
									new Object[]
									{ Integer.valueOf(formInfo.getFormID()), baseFormInfo.getRecordType(), baseFormInfo.getEditorID(),
											Integer.valueOf(baseFormID) });
					}
					else if ((baseModIndex < this.pluginIndex) && (!this.worldspaceList.contains(baseFormInfo)))
					{
						Master checkMaster = this.masters[baseModIndex];
						int checkFormID = baseFormID & 0xFFFFFF | checkMaster.getMasterList().size() << 24;
						FormInfo checkFormInfo = checkMaster.getFormMap().get(new Integer(checkFormID));
						if (checkFormInfo != null)
						{
							PluginRecord checkRecord = checkMaster.getRecord(checkFormID);
							if (checkRecord.getRecordType().equals("BOOK"))
							{
								String editorID = checkRecord.getEditorID();
								if ((editorID.length() == 17) && (editorID.substring(0, 9).equals("TES4Gecko")))
								{
									this.worldspaceList.add(baseFormInfo);
									if (Main.debugMode)
										System.out.printf(
												"Relocation required for reference %08X to %s base item %s (%08X)\n",
												new Object[]
												{ Integer.valueOf(formInfo.getFormID()), baseFormInfo.getRecordType(),
														baseFormInfo.getEditorID(), Integer.valueOf(baseFormID) });
								}
							}
						}
					}
				}
			}
		}
	}

	private void mapDistantStatics(File dirFile) throws IOException
	{
		File[] files = dirFile.listFiles();
		String name;
		byte[] buffer;
		for (File file : files)
		{
			name = file.getName();
			int length = (int) file.length();
			int pos = name.lastIndexOf('.');
			if ((pos > 0) && (name.substring(pos).equalsIgnoreCase(".lod")) && (length > 0))
			{
				pos = name.indexOf('_');
				if ((pos > 0) && (this.worldspaceNames.contains(name.substring(0, pos).toLowerCase())))
				{
					FileInputStream in = null;
					try
					{
						in = new FileInputStream(file);
						buffer = new byte[length];
						int count = in.read(buffer);
						if (count != length)
						{
							throw new EOFException("Unexpected end-of-file on " + name);
						}
						in.close();
						in = null;

						int referenceCount = SerializedElement.getInteger(buffer, 0);
						int offset = 4;
						for (int i = 0; i < referenceCount; i++)
						{
							int formID = SerializedElement.getInteger(buffer, offset);
							int modIndex = formID >>> 24;
							count = SerializedElement.getInteger(buffer, offset + 4);
							offset += 8 + 28 * count;
							if (modIndex > 0)
							{
								Integer objFormID = new Integer(formID);
								if (!this.distantList.contains(objFormID))
									this.distantList.add(new Integer(objFormID.intValue()));
							}
						}
					}
					finally
					{
						if (in != null)
						{
							in.close();
						}
					}
				}

			}

		}

		List<FormInfo> formList = this.plugin.getFormList();
		Map<?, ?> formMap = this.plugin.getFormMap();
		Map<?, ?> masterFormMap = this.masters[0].getFormMap();
		for (FormInfo formInfo : formList)
		{
			int formID = formInfo.getFormID();
			int modIndex = formID >>> 24;
			if (modIndex >= this.pluginIndex)
				for (Integer distantFormID : this.distantList)
					if (distantFormID.intValue() == formID)
					{
						if (this.worldspaceList.contains(formInfo))
							break;
						this.worldspaceList.add(formInfo);
						if (!Main.debugMode)
							break;
						System.out.printf("DistantLOD entry maps to %s record %s (%08X)\n", new Object[]
						{ formInfo.getRecordType(), formInfo.getEditorID(), Integer.valueOf(formID) });

						break;
					}
		}
	}

	private void relocateWorldspaces() throws DataFormatException, IOException, PluginException
	{
		for (FormInfo formInfo : this.worldspaceList)
		{
			int oldFormID = formInfo.getFormID();
			int modIndex = oldFormID >>> 24;
			int formID;

			if (modIndex >= this.pluginIndex)
			{
				String recordType = formInfo.getRecordType();
				String editorID = formInfo.getEditorID();

				if ((recordType.equals("BOOK")) && (editorID.length() == 17) && (editorID.substring(0, 9).equals("TES4Gecko")))
					formID = Integer.parseInt(editorID.substring(9), 16);
				else
					formID = getBaseFormID();
			}
			else
			{
				PluginRecord checkRecord = this.masters[modIndex].getRecord(oldFormID);
				String recordType = checkRecord.getRecordType();
				String editorID = checkRecord.getEditorID();

				if ((recordType.equals("BOOK")) && (editorID.length() == 17) && (editorID.substring(0, 9).equals("TES4Gecko")))
				{
					formID = Integer.parseInt(editorID.substring(9), 16);
				}
				else if ((formInfo.getPlugin() instanceof Master))
				{
					formID = getBaseFormID();
					PluginRecord record = (PluginRecord) checkRecord.clone();
					editorID = String.format("TES4Gecko%08X", new Object[]
					{ Integer.valueOf(formID) });
					PluginGroup group = this.plugin.createTopGroup(recordType);
					record.setFormID(formID);
					record.setEditorID(editorID);
					record.setParent(group);
					group.getRecordList().add(record);
					FormInfo cloneFormInfo = new FormInfo(record, recordType, formID, editorID);
					cloneFormInfo.setPlugin(this.plugin);
					this.plugin.getFormList().add(cloneFormInfo);
					this.plugin.getFormMap().put(new Integer(formID), cloneFormInfo);
					this.pluginModified = true;
				}
				else
				{
					formID = getBaseFormID();
				}
			}

			this.worldspaceMap.put(new Integer(oldFormID), new Integer(formID));
			if (Main.debugMode)
				System.out.printf("Relocating %s record %s from %08X to %08X\n", new Object[]
				{ formInfo.getRecordType(), formInfo.getEditorID(), Integer.valueOf(oldFormID), Integer.valueOf(formID) });
		}
	}

	private int getBaseFormID()  
	{
		int formID = 0;
		boolean haveFormID = false;
		while (!haveFormID)
		{
			if (this.baseFormID >= 16777216)
			{
				this.baseFormID = 2097152;
			}
			formID = this.baseFormID++;
			haveFormID = true;

			Integer checkObj = new Integer(formID);
			for (int i = 0; i < this.masters.length; i++)
			{
				if (this.masters[i].getFormMap().get(checkObj) != null)
				{
					haveFormID = false;
					break;
				}
			}
		}

		return formID;
	}

	private void updateReferences() throws DataFormatException, IOException, PluginException
	{
		List<PluginGroup> groupList = this.plugin.getGroupList();
		int totalCount = groupList.size();
		int processedCount = 0;
		int currentProgress = 0;

		for (PluginGroup group : groupList)
		{
			updateGroupReferences(group);
			processedCount++;
			int newProgress = processedCount * 100 / totalCount;
			if (newProgress > currentProgress + 5)
			{
				currentProgress = newProgress;
				getStatusDialog().updateProgress(currentProgress);
			}
		}
	}

	void updateGroupReferences(PluginGroup group) throws DataFormatException, IOException, PluginException
	{
		List<PluginRecord> recordList = group.getRecordList();

		for (PluginRecord record : recordList)
			if ((record instanceof PluginGroup))
			{
				PluginGroup subgroup = (PluginGroup) record;
				updateGroupReferences(subgroup);

				if (subgroup.getGroupType() == 1)
				{
					int groupParentID = subgroup.getGroupParentID();
					Integer movedFormID = this.worldspaceMap.get(new Integer(groupParentID));
					if (movedFormID != null)
					{
						subgroup.setGroupParentID(movedFormID.intValue());
						this.pluginModified = true;
					}

				}

			}
			else
			{
				List<PluginSubrecord> subrecords = record.getSubrecords();
				boolean recordModified = false;
				for (PluginSubrecord subrecord : subrecords)
				{
					boolean subrecordModified = false;
					byte[] subrecordData = subrecord.getSubrecordData();
					int[][] references = subrecord.getReferences();
					if ((references == null) || (references.length == 0))
					{
						continue;
					}

					for (int i = 0; i < references.length; i++)
					{
						int offset = references[i][0];
						int formID = references[i][1];
						if (formID == 0)
						{
							continue;
						}
						Integer movedFormID = this.worldspaceMap.get(new Integer(formID));
						if (movedFormID != null)
						{
							SerializedElement.setInteger(movedFormID.intValue(), subrecordData, offset);
							subrecordModified = true;
						}

					}

					if (subrecordModified)
					{
						subrecord.setSubrecordData(subrecordData);
						recordModified = true;
					}

				}

				if (recordModified)
				{
					record.setSubrecords(subrecords);
					this.pluginModified = true;
				}

				String recordType = record.getRecordType();
				String editorID = record.getEditorID();
				if ((!recordType.equals("BOOK")) || (editorID.length() != 17) || (!editorID.substring(0, 9).equals("TES4Gecko")))
				{
					int formID = record.getFormID();
					Integer movedFormID = this.worldspaceMap.get(new Integer(formID));
					if (movedFormID != null)
					{
						record.setFormID(movedFormID.intValue());
						this.pluginModified = true;
					}
				}
			}
	}

	private void updateDistantStatics(File dirFile) throws IOException
	{
		File[] files = dirFile.listFiles();

		for (File file : files)
		{
			String name = file.getName();
			int length = (int) file.length();
			int pos = name.lastIndexOf('.');
			if ((pos > 0) && (name.substring(pos).equalsIgnoreCase(".lod")) && (length > 0))
			{
				pos = name.indexOf('_');
				if ((pos > 0) && (this.worldspaceNames.contains(name.substring(0, pos).toLowerCase())))
				{
					FileInputStream in = null;
					FileOutputStream out = null;
					try
					{
						in = new FileInputStream(file);
						byte[] buffer = new byte[length];
						int count = in.read(buffer);
						if (count != length)
						{
							throw new EOFException("Unexpected end-of-file on " + name);
						}
						in.close();
						in = null;

						boolean fileUpdated = false;
						int referenceCount = SerializedElement.getInteger(buffer, 0);
						int offset = 4;
						for (int i = 0; i < referenceCount; i++)
						{
							int formID = SerializedElement.getInteger(buffer, offset);
							Integer movedFormID = this.worldspaceMap.get(new Integer(formID));
							if (movedFormID != null)
							{
								SerializedElement.setInteger(movedFormID.intValue(), buffer, offset);
								fileUpdated = true;
							}

							count = SerializedElement.getInteger(buffer, offset + 4);
							offset += 8 + 28 * count;
						}

						if (fileUpdated)
						{
							out = new FileOutputStream(file);
							out.write(buffer);
							out.close();
							out = null;
						}
					}
					finally
					{
						if (out != null)
						{
							out.close();
						}
						if (in != null)
							in.close();
					}
				}
			}
		}
	}

	private void createPlaceholders() throws DataFormatException, IOException, PluginException
	{
		Map<?, ?> formMap = this.plugin.getFormMap();
		PluginGroup group = this.plugin.createTopGroup("BOOK");
		List<PluginRecord> groupList = group.getRecordList();
		Set<Entry<Integer, Integer>> mapSet = this.worldspaceMap.entrySet();

		for (Map.Entry<Integer, Integer> entry : mapSet)
		{
			int oldFormID = entry.getKey().intValue();
			int newFormID = entry.getValue().intValue();
			if (oldFormID >>> 24 < this.pluginIndex)
			{
				continue;
			}
			FormInfo formInfo = (FormInfo) formMap.get(entry.getKey());
			if (formInfo != null)
			{
				String recordType = formInfo.getRecordType();
				String editorID = formInfo.getEditorID();
				if ((recordType.equals("BOOK")) && (editorID.length() == 17) && (editorID.substring(0, 9).equals("TES4Gecko")))
				{
					continue;
				}

			}

			List<PluginSubrecord> subrecords = new ArrayList<PluginSubrecord>(10);

			byte[] fullData = String.format("", new Object[]
			{ Integer.valueOf(oldFormID) }).getBytes();
			PluginSubrecord fullSubrecord = new PluginSubrecord("BOOK", "FULL", fullData);
			subrecords.add(fullSubrecord);

			byte[] descData = String.format("", new Object[]
			{ Integer.valueOf(oldFormID), Integer.valueOf(newFormID) }).getBytes();
			PluginSubrecord descSubrecord = new PluginSubrecord("BOOK", "DESC", descData);
			subrecords.add(descSubrecord);

			byte[] dataData = new byte[10];
			dataData[0] = 1;
			dataData[1] = -1;
			dataData[2] = 0;
			dataData[3] = 0;
			dataData[4] = 0;
			dataData[5] = 0;
			dataData[6] = 0;
			dataData[7] = 0;
			dataData[8] = 0;
			dataData[9] = 0;
			PluginSubrecord dataSubrecord = new PluginSubrecord("BOOK", "DATA", dataData);
			subrecords.add(dataSubrecord);

			byte[] modlData = "".getBytes();
			PluginSubrecord modlSubrecord = new PluginSubrecord("BOOK", "MODL", modlData);
			subrecords.add(modlSubrecord);

			byte[] modbData = new byte[4];
			modbData[0] = -83;
			modbData[1] = 60;
			modbData[2] = -37;
			modbData[3] = 65;
			PluginSubrecord modbSubrecord = new PluginSubrecord("BOOK", "MODB", modbData);
			subrecords.add(modbSubrecord);

			byte[] iconData = "".getBytes();
			PluginSubrecord iconSubrecord = new PluginSubrecord("BOOK", "ICON", iconData);
			subrecords.add(iconSubrecord);

			PluginRecord record = new PluginRecord("BOOK", oldFormID);
			record.setSubrecords(subrecords);
			record.setEditorID(String.format("TES4Gecko%08X", new Object[]
			{ Integer.valueOf(newFormID) }));
			groupList.add(record);
			this.pluginModified = true;
			if (Main.debugMode)
				System.out.printf("Added %s record %s (%08X)\n", new Object[]
				{ record.getRecordType(), record.getEditorID(), Integer.valueOf(record.getFormID()) });
		}
	}

	private void renameFiles(File dirFile)
	{
		File[] files = dirFile.listFiles();

		for (File file : files)
			if (file.isFile())
			{
				String fileName = file.getName();
				if (Character.isDigit(fileName.charAt(0)))
				{
					int sep = fileName.indexOf('.');
					if (sep > 0)
					{
						String prefix = fileName.substring(0, sep);
						try
						{
							Integer checkID = new Integer(prefix);
							Integer mappedID = this.worldspaceMap.get(checkID);
							if (mappedID != null)
							{
								String mappedFileName = mappedID.toString() + fileName.substring(sep);
								File mappedFile = new File(dirFile.getPath() + Main.fileSeparator + mappedFileName);
								if (!mappedFile.exists())
								{
									file.renameTo(mappedFile);
									if (Main.debugMode)
										System.out.printf("Renamed %s to %s\n", new Object[]
										{ file.getPath(), mappedFile.getPath() });
								}
							}
						}
						catch (NumberFormatException localNumberFormatException)
						{
						}
					}
				}
			}
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.WorldspaceTask
 * JD-Core Version:    0.6.0
 */