package TES4Gecko;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Vector;
import java.util.zip.DataFormatException;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class MergeTask extends WorkerTask
{
	private String[] pluginNames;

	private PluginInfo pluginInfo;

	private boolean masterMerge;

	private boolean yesToAll = false;

	private Plugin[] plugins;

	private Plugin mergedPlugin;

	private List<String> mergedMasterList;

	private int mergedMasterCount;

	private Master[] masters;

	private Map<Integer, FormInfo> mergedFormMap;

	private Map<String, FormInfo> mergedEditorMap;

	private int highFormID;

	private boolean useFloorFormID = false;

	private int floorFormID;

	Map<Integer, List<PluginRecord>> deletedINFOMap;

	private static JFrame parentWindow;

	public MergeTask(StatusDialog statusDialog, String[] pluginNames, PluginInfo pluginInfo)
	{
		super(statusDialog);
		this.pluginNames = pluginNames;
		this.pluginInfo = pluginInfo;

		if (pluginInfo == null)
			this.masterMerge = true;
		else
			this.masterMerge = false;
	}

	public static void mergeToMaster(JFrame parent, File masterFile, File pluginFile)
	{
		parentWindow = parent;
		String[] pluginNames = new String[2];
		pluginNames[0] = masterFile.getName();
		pluginNames[1] = pluginFile.getName();
		mergePlugins(parent, pluginNames, null);
	}

	public static void mergePlugins(JFrame parent, String[] pluginNames, PluginInfo pluginInfo)
	{
		parentWindow = parent;

		StatusDialog statusDialog = new StatusDialog(parent, " ", "Merge Plugin");

		MergeTask worker = new MergeTask(statusDialog, pluginNames, pluginInfo);
		statusDialog.setWorker(worker);

		worker.start();
		statusDialog.showDialog();

		if (statusDialog.getStatus() == 1)
		{
			if (worker.masterMerge)
				JOptionPane.showMessageDialog(parent, pluginNames[0] + " updated", "Merge Plugin", 1);
			else
				JOptionPane.showMessageDialog(parent, pluginInfo.getName() + " created", "Merge Plugin", 1);
		}
		else
			JOptionPane.showMessageDialog(parent, "Merge failed", "Merge Plugin", 1);
	}

	public void run()
	{
		boolean completed = false;

		Plugin plugin = null;
		Master master = null;
		float version = 0.0F;

		boolean manageMerge = false;
		if (this.masterMerge)
		{
			int selection = JOptionPane
					.showConfirmDialog(
							parentWindow,
							"<html>Do you wish to manage the merge? Choosing <i>yes</i>\nwill allow for user intervention at points within the merge;\notherwise default merge behavior will take place.",
							"Manage Merge?", 0, 3);
			if (selection == 0)
				manageMerge = true;

		}

		try
		{
			this.plugins = new Plugin[this.pluginNames.length];
			for (int i = 0; i < this.pluginNames.length; i++)
			{
				File inFile = new File(Main.pluginDirectory + Main.fileSeparator + this.pluginNames[i]);
				plugin = new Plugin(inFile);
				plugin.load(this);
				this.plugins[i] = plugin;
				version = Math.max(version, plugin.getVersion());
			}
			int count = 0;
			for (int i = 0; i < this.plugins.length; i++)
			{
				count += this.plugins[i].getMasterList().size();
			}
			this.mergedMasterList = new ArrayList<String>(count);

			for (int i = 0; i < this.plugins.length; i++)
			{
				String masterName;
				List<String> masterList = this.plugins[i].getMasterList();
				for (Iterator<String> localIterator1 = masterList.iterator(); localIterator1.hasNext();)
				{
					masterName = localIterator1.next();
					if ((this.mergedMasterList.contains(masterName)) || ((this.masterMerge) && (masterName.equals(this.pluginNames[0]))))
						continue;
					this.mergedMasterList.add(masterName);
				}
			}
			this.mergedMasterCount = this.mergedMasterList.size();
			this.masters = new Master[this.mergedMasterCount];

			int i = 0;
			for (String masterName : this.mergedMasterList)
			{
				File masterFile = new File(Main.pluginDirectory + Main.fileSeparator + masterName);
				master = new Master(masterFile);
				master.load(this);
				this.masters[(i++)] = master;
				version = Math.max(version, master.getVersion());
			}
			int worldspaceID;
			if ((this.masterMerge) && (manageMerge))
			{
				List<?> cellRegionList = this.plugins[1].getCellRegionsUsed();
				if (cellRegionList.size() > 0)
				{
					Vector<String[]> regionData = new Vector<String[]>();
					List<?> pluginMasterList = this.plugins[1].getMasterList();

					for (Iterator<?> localIterator2 = cellRegionList.iterator(); localIterator2.hasNext();)
					{
						int region = ((Integer) localIterator2.next()).intValue();

						String pluginName = "<html><i>Plugin Not Found</i><html>";
						String regionName = "<html><i>Region Not Found</i><html>";
						String worldspaceName = "<html><i>Worldspace Not Found</i><html>";
						if (region == 65535)
						{
							String[] regionInfoArray = new String[4];
							regionInfoArray[0] = String.format("%08X", new Object[]
							{ Integer.valueOf(65535) });
							regionInfoArray[1] = pluginName;
							regionInfoArray[2] = worldspaceName;
							regionInfoArray[3] = regionName;
							regionData.add(regionInfoArray);
						}
						else
						{
							int masterIdx = region >>> 24;
							worldspaceID = -1;
							Object pluginToSearch = null;
							if (masterIdx == pluginMasterList.size())
							{
								pluginToSearch = this.plugins[1];
								pluginName = this.plugins[1].getName();
							}
							else
							{
								pluginName = (String) pluginMasterList.get(masterIdx);
								int mergedMasterIdx = this.mergedMasterList.indexOf(pluginName);
								if (mergedMasterIdx != -1)
								{
									pluginToSearch = this.masters[mergedMasterIdx];
								}
								else if (pluginName.equalsIgnoreCase(this.plugins[0].getName()))
								{
									pluginToSearch = this.plugins[0];
								}
								else
								{
									if (!Main.debugMode)
										continue;
									System.out.print("MergeTask: Plugin " + pluginName + " for region "
											+ String.format("%08X", new Object[]
											{ Integer.valueOf(region) }) + " not found in merge list\n");

									continue;
								}
							}

							PluginRecord pluginRec = null;
							if ((pluginToSearch instanceof Plugin))
							{
								FormInfo formInfo = ((Plugin) pluginToSearch).getFormMap().get(Integer.valueOf(region));
								if (formInfo != null)
									pluginRec = (PluginRecord) formInfo.getSource();
							}
							else if ((pluginToSearch instanceof Master))
							{
								pluginRec = ((Master) pluginToSearch).getRecord(region);
							}
							if (pluginRec == null)
							{
								if (Main.debugMode)
								{
									System.out.print("MergeTask: Region " + String.format("%08X", new Object[]
									{ Integer.valueOf(region) }) + " not found in plugin " + pluginName + "\n");
								}
							}
							if (pluginRec != null)
							{
								regionName = pluginRec.getEditorID();
								try
								{
									PluginSubrecord WSRec = pluginRec.getSubrecord("WNAM");
									String WSStr = WSRec.getDisplayData();
									worldspaceID = Integer.parseInt(WSStr, 16);
								}
								catch (Exception ex)
								{
									if (Main.debugMode)
									{
										System.out.print("MergeTask: WNAM not found for region " + String.format("%08X", new Object[]
										{ Integer.valueOf(region) }) + " in plugin " + pluginName + "\n");
									}

								}

								int WSIndex = worldspaceID == -1 ? worldspaceID : worldspaceID >>> 24;
								String WSPluginName = "";
								if ((WSIndex != masterIdx) && (WSIndex != -1))
								{
									if (WSIndex == pluginMasterList.size())
									{
										pluginToSearch = this.plugins[1];
										WSPluginName = this.plugins[1].getName();
									}
									else
									{
										WSPluginName = (String) pluginMasterList.get(WSIndex);
										int mergedMasterIdx = this.mergedMasterList.indexOf(WSPluginName);
										if (mergedMasterIdx != -1)
										{
											pluginToSearch = this.masters[mergedMasterIdx];
										}
										else if (WSPluginName.equalsIgnoreCase(this.plugins[0].getName()))
										{
											pluginToSearch = this.plugins[0];
										}
										else
										{
											if (!Main.debugMode)
												continue;
											System.out.print("MergeTask: Plugin " + WSPluginName + " for worldspace "
													+ String.format("%08X", new Object[]
													{ Integer.valueOf(worldspaceID) }) + " not found in merge list\n");

											continue;
										}
									}
								}
								else if (WSIndex == masterIdx)
								{
									WSPluginName = pluginName;
								}
								if (WSIndex == -1)
								{
									pluginToSearch = new String();
								}

								FormInfo WSFormInfo = null;
								if ((pluginToSearch instanceof Plugin))
								{
									WSFormInfo = ((Plugin) pluginToSearch).getFormMap().get(Integer.valueOf(worldspaceID));
								}
								else if ((pluginToSearch instanceof Master))
								{
									WSFormInfo = ((Master) pluginToSearch).getFormMap().get(Integer.valueOf(worldspaceID));
								}
								if ((WSFormInfo == null) && (WSIndex != -1))
								{
									if (WSIndex == 0)
									{
										WSFormInfo = this.plugins[1].getFormMap().get(Integer.valueOf(worldspaceID));
										if (WSFormInfo == null)
										{
											WSFormInfo = this.plugins[0].getFormMap().get(Integer.valueOf(worldspaceID));
											if (WSFormInfo == null)
											{
												for (int j = this.masters.length; j >= 0; j--)
												{
													WSFormInfo = this.masters[j].getFormMap().get(Integer.valueOf(worldspaceID));
													if (WSFormInfo != null)
														break;
												}
											}
										}
									}
								}
								if (WSFormInfo != null)
								{
									worldspaceName = WSFormInfo.getEditorID();
								}
								else if (Main.debugMode)
								{
									System.out.print("MergeTask: Worldspace " + String.format("%08X", new Object[]
									{ Integer.valueOf(worldspaceID) }) + " not found in plugin " + WSPluginName + "\n");
								}

							}

							String[] regionInfoArray = new String[4];
							regionInfoArray[0] = String.format("%08X", new Object[]
							{ Integer.valueOf(region) });
							regionInfoArray[1] = pluginName;
							regionInfoArray[2] = worldspaceName;
							regionInfoArray[3] = regionName;
							regionData.add(regionInfoArray);
						}
					}
					String retStr = RegionCellDialog.showDialog(parentWindow, regionData);

					String[] resultArray = retStr.split(":");

					if ((!resultArray[0].equals("All")) || (resultArray[0].equals("None")))
						this.plugins[1].ignoreAllExteriorCells();
					if (resultArray[0].equals("CancelMerge"))
						throw new InterruptedException("Merge canceled");
					if (resultArray[0].equals("Some"))
					{
						ArrayList<Integer> regionIDs = new ArrayList<Integer>();
						for (int j = 1; j < resultArray.length; j++)
						{
							int regionID = Integer.parseInt(resultArray[j], 16);
							regionIDs.add(Integer.valueOf(regionID));
						}
						this.plugins[1].ignoreAllExteriorCellsExcept(regionIDs);
					}
					if (resultArray[0].equals("Except"))
					{
						ArrayList<Integer> regionIDs = new ArrayList<Integer>();
						for (int j = 1; j < resultArray.length; j++)
						{
							int regionID = Integer.parseInt(resultArray[j], 16);
							regionIDs.add(Integer.valueOf(regionID));
						}
						this.plugins[1].ignoreAllExteriorCells(regionIDs);
					}

				}

			}

			if ((this.masterMerge) && (manageMerge))
			{
				int selection2 = JOptionPane
						.showConfirmDialog(
								parentWindow,
								"Do you wish to select a starting start form ID to new objects merged into the master?\n<html>Selecting <i>No</i> or destroying this window will continue the merge as normal.</html>",
								"Select Starting Form ID", 0, 3);
				if (selection2 == 0)
				{
					this.useFloorFormID = true;
					this.floorFormID = getStartFormID(highestFormID(this.plugins[0]));
					if (this.floorFormID == -1)
					{
						this.useFloorFormID = false;
						throw new InterruptedException("Merge canceled");
					}
					this.floorFormID += 1;
				}
				else
				{
					this.useFloorFormID = false;
				}

			}

			i = 0;
			for (count = 0; i < this.plugins.length; i++)
			{
				count += this.plugins[i].getFormList().size();
			}
			this.mergedFormMap = new HashMap<Integer, FormInfo>(count);
			this.mergedEditorMap = new HashMap<String, FormInfo>(count);
			this.highFormID = (this.mergedMasterCount << 24);

			for (i = 0; i < this.plugins.length; i++)
			{
				mapPluginRecords(this.plugins[i]);
				if (interrupted())
				{
					throw new InterruptedException("Merge canceled");
				}

			}

			for (i = 0; i < this.plugins.length; i++)
			{
				getStatusDialog().updateMessage("Updating references for " + this.pluginNames[i]);
				plugin = this.plugins[i];

				List<?> masterList = plugin.getMasterList();
				int[] masterMap = new int[masterList.size()];
				for (int index = 0; index < masterMap.length; index++)
				{
					String masterName = (String) masterList.get(index);
					int mergedIndex = this.mergedMasterList.indexOf(masterName);
					masterMap[index] = (mergedIndex >= 0 ? mergedIndex : this.mergedMasterCount);
				}

				Map<Integer, FormInfo> formMap = plugin.getFormMap();
				FormAdjust formAdjust = new FormAdjust(masterMap, this.mergedMasterCount, formMap);

				List<PluginGroup> groupList = plugin.getGroupList();
				int groupCount = groupList.size();
				int processedCount = 0;
				int currentProgress = 0;
				for (PluginGroup group : groupList)
				{
					updateGroup(group, formAdjust);
					if (interrupted())
					{
						throw new InterruptedException("Merge canceled");
					}
					processedCount++;
					int newProgress = processedCount * 100 / groupCount;
					if (newProgress >= currentProgress + 5)
					{
						currentProgress = newProgress;
						getStatusDialog().updateProgress(currentProgress);
					}
				}
			}
			File mergedFile;
			if (this.masterMerge)
			{
				plugin = this.plugins[0];
				mergedFile = new File(Main.pluginDirectory + Main.fileSeparator + plugin.getName());
				this.mergedPlugin = new Plugin(mergedFile, plugin.getCreator(), plugin.getSummary(), this.mergedMasterList);
				this.mergedPlugin.setVersion(version);
				this.mergedPlugin.setMaster(true);
			}
			else
			{
				mergedFile = new File(Main.pluginDirectory + Main.fileSeparator + this.pluginInfo.getName());
				this.mergedPlugin = new Plugin(mergedFile, this.pluginInfo.getCreator(), this.pluginInfo.getSummary(),
						this.mergedMasterList);
				this.mergedPlugin.setVersion(version);
				this.mergedPlugin.setMaster(false);
			}

			this.mergedPlugin.createInitialGroups();

			this.deletedINFOMap = new HashMap<Integer, List<PluginRecord>>();
			for (i = this.plugins.length - 1; i >= 0; i--)
			{
				buildDeletedINFOMap(this.plugins[i], this.deletedINFOMap);
			}

			for (i = 0; i < this.plugins.length; i++)
			{
				getStatusDialog().updateMessage("Merging " + this.pluginNames[i]);
				mergePlugin(this.plugins[i]);
			}

			if (!this.deletedINFOMap.isEmpty())
			{
				PluginGroup mergedGroup = this.mergedPlugin.getTopGroup("DIAL");
				if (mergedGroup != null)
					mergedGroup.suturePNAMs(this.deletedINFOMap);

			}

			this.mergedPlugin.store(this);

			getStatusDialog().updateMessage("Merging voice files");
			String mergedPath = String.format("%s%sSound%sVoice%s%s", new Object[]
			{ mergedFile.getParent(), Main.fileSeparator, Main.fileSeparator, Main.fileSeparator, mergedFile.getName() });
			if (!this.masterMerge)
			{
				String mergedName = this.mergedPlugin.getName();
				boolean deleteFiles = true;
				for (i = 0; i < this.pluginNames.length; i++)
				{
					if (mergedName.equalsIgnoreCase(this.pluginNames[i]))
					{
						deleteFiles = false;
						break;
					}
				}

				if (deleteFiles)
				{
					File voiceDirectory = new File(mergedPath);
					if (voiceDirectory.exists())
					{
						if (voiceDirectory.isDirectory())
							deleteDirectoryTree(voiceDirectory);
						else
						{
							voiceDirectory.delete();
						}
					}

				}

			}

			for (i = 0; i < this.plugins.length; i++)
			{
				plugin = this.plugins[i];
				if (!this.mergedPlugin.getName().equalsIgnoreCase(plugin.getName()))
				{
					String voicePath = String.format("%s%sSound%sVoice%s%s", new Object[]
					{ mergedFile.getParent(), Main.fileSeparator, Main.fileSeparator, Main.fileSeparator, plugin.getName() });
					File voiceDirectory = new File(voicePath);
					if ((voiceDirectory.exists()) && (voiceDirectory.isDirectory()))
					{
						copyVoiceFiles(plugin, voiceDirectory, mergedPath);
					}
				}
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
			Main.logException("Exception while merging plugins", exc);
		}

		if (this.mergedPlugin != null)
		{
			this.mergedPlugin.resetFormList();
			this.mergedPlugin.resetFormMap();
		}
		for (int i = 0; i < this.plugins.length; i++)
			this.plugins[i].resetFormList();
		for (int i = 0; i < this.masters.length; i++)
			this.masters[i].resetFormList();
		for (int i = 0; i < this.plugins.length; i++)
			this.plugins[i].resetFormMap();
		for (int i = 0; i < this.masters.length; i++)
			this.masters[i].resetFormMap();
		getStatusDialog().closeDialog(completed);
	}

	private void mapPluginRecords(Plugin plugin) throws DataFormatException, IOException, PluginException
	{
		List<FormInfo> formList = plugin.getFormList();
		List<?> masterList = plugin.getMasterList();
		String pluginName = plugin.getName();
		int masterCount = masterList.size();

		for (FormInfo info : formList)
		{
			int formID = info.getFormID();
			String editorID = info.getEditorID();
			int masterID = formID >>> 24;
			formID &= 16777215;
			boolean masterReference = false;
			boolean updateMap = true;

			if (((PluginRecord) info.getSource()).isIgnored())
			{
				continue;
			}

			if (masterID < masterCount)
			{
				String masterName = (String) masterList.get(masterID);
				if ((!this.masterMerge) || (!masterName.equals(this.pluginNames[0])))
				{
					masterReference = true;
					masterID = this.mergedMasterList.indexOf(masterName);
				}
			}

			if (masterReference)
			{
				formID |= masterID << 24;
				FormInfo mergedInfo = this.mergedFormMap.get(new Integer(formID));
				if (mergedInfo != null)
				{
					updateMap = false;
					PluginRecord record = (PluginRecord) info.getSource();
					PluginRecord mergedRecord = (PluginRecord) mergedInfo.getSource();
					if (record.isIdentical(mergedRecord))
					{
						String recordType = record.getRecordType();
						if ((!recordType.equals("WRLD")) && (!recordType.equals("CELL")) && (!recordType.equals("DIAL")))
							record.setIgnore(true);
					}
					else if ((!record.isDeleted()) && (editorID.length() != 0) && (!mergedInfo.getMergedEditorID().equals(editorID)))
					{
						String text = String.format("Plugin %s changes merged master record editor ID from %s to %s", new Object[]
						{ plugin.getName(), mergedInfo.getMergedEditorID(), editorID });
						throw new PluginException(text);
					}

				}

			}
			else
			{
				formID |= this.mergedMasterCount << 24;

				FormInfo mergedInfo = this.mergedFormMap.get(new Integer(formID));
				if (mergedInfo != null)
				{
					if ((this.masterMerge) && (masterID < masterCount))
					{
						PluginRecord record = (PluginRecord) info.getSource();
						String recordType = record.getRecordType();
						if ((!recordType.equals("WRLD")) && (!recordType.equals("CELL")) && (!recordType.equals("DIAL")))
						{
							PluginRecord mergedRecord = (PluginRecord) mergedInfo.getSource();
							this.mergedFormMap.remove(new Integer(formID));
							this.mergedEditorMap.remove(mergedInfo.getMergedEditorID().toLowerCase());
							mergedRecord.setIgnore(true);
							if (record.isDeleted())
							{
								record.setIgnore(true);
								updateMap = false;
							}
						}
						else
						{
							updateMap = false;
							if ((!record.isDeleted()) && (editorID.length() != 0))
							{
								if (!mergedInfo.getMergedEditorID().equals(editorID))
								{
									mergedInfo.setMergedEditorID(editorID);
									this.mergedEditorMap.put(editorID.toLowerCase(), info);
								}
							}
							else if (record.isDeleted())
							{
								PluginRecord mergedRecord = (PluginRecord) mergedInfo.getSource();
								mergedRecord.setIgnore(true);
								record.setIgnore(true);
							}
						}

					}
					else if ((this.masterMerge) && (this.useFloorFormID) && (!plugin.isMaster()))
					{
						formID = this.floorFormID++;
					}
					else
					{
						formID = this.highFormID + 1;
					}
				}
				else if ((this.masterMerge) && (this.useFloorFormID) && (!plugin.isMaster()))
				{
					formID = this.floorFormID++;
				}

				if ((updateMap) && (editorID.length() != 0))
				{
					mergedInfo = this.mergedEditorMap.get(editorID.toLowerCase());
					if (mergedInfo != null)
					{
						PluginRecord record = (PluginRecord) info.getSource();
						PluginRecord mergedRecord = (PluginRecord) mergedInfo.getSource();
						String recordType = record.getRecordType();
						if (record.isIdentical(mergedRecord))
						{
							record.setIgnore(true);
							formID = mergedInfo.getMergedFormID();
							updateMap = false;
						}
						else if (recordType.equals("GMST"))
						{
							if (!mergedRecord.getRecordType().equals("GMST"))
							{
								String text = String.format(
										"Conflict between GMST record %s (%08X) in plugin %s and %s record %s (%08X) in plugin %s",
										new Object[]
										{ editorID, Integer.valueOf(record.getFormID()), pluginName, mergedRecord.getRecordType(),
												editorID, Integer.valueOf(mergedRecord.getFormID()),
												((Plugin) mergedInfo.getPlugin()).getName() });
								throw new PluginException(text);
							}

							formID = mergedInfo.getMergedFormID();
							updateMap = false;
						}
						else if (recordType.equals("MGEF"))
						{
							if (!mergedRecord.getRecordType().equals("MGEF"))
							{
								String text = String.format(
										"Conflict between MGEF record %s (%08X) in plugin %s and %s record %s (%08X) in plugin %s",
										new Object[]
										{ editorID, Integer.valueOf(record.getFormID()), pluginName, mergedRecord.getRecordType(),
												editorID, Integer.valueOf(mergedRecord.getFormID()),
												((Plugin) mergedInfo.getPlugin()).getName() });
								throw new PluginException(text);
							}

							formID = mergedInfo.getMergedFormID();
							updateMap = false;
						}
						else
						{
							String newEditorID = editorID.concat("X");
							while (this.mergedEditorMap.get(newEditorID.toLowerCase()) != null)
							{
								newEditorID = newEditorID.concat("X");
							}
							if (!this.yesToAll)
							{
								String text = String.format(
										"%s record %s (%08X) in '%s' has the same name as %s record %s (%08X) in '%s'",
										new Object[]
										{ info.getRecordType(), editorID, Integer.valueOf(record.getFormID()), pluginName,
												mergedInfo.getRecordType(), editorID, Integer.valueOf(mergedRecord.getFormID()),
												((Plugin) mergedInfo.getPlugin()).getName() });
								int selection = WorkerDialog.showConfirmDialog(getParent(), text + ". Do you want to rename it to "
										+ newEditorID + "?", "Error", 0, 0, true);
								if (selection == 1)
								{
									throw new PluginException(text);
								}
								if (selection == 2)
								{
									this.yesToAll = true;
								}
							}
							editorID = newEditorID;
						}

					}

				}

			}

			info.setMergedFormID(formID);
			info.setMergedEditorID(editorID);

			if (updateMap)
			{
				this.mergedFormMap.put(new Integer(formID), info);
				if (editorID.length() != 0)
				{
					this.mergedEditorMap.put(editorID.toLowerCase(), info);
				}

			}

			if (formID > this.highFormID)
				this.highFormID = formID;
		}
	}

	private void updateGroup(PluginGroup group, FormAdjust formAdjust) throws DataFormatException, IOException, PluginException
	{
		int groupType = group.getGroupType();
		if ((groupType == 1) || (groupType == 6) || (groupType == 8) || (groupType == 9) || (groupType == 10) || (groupType == 7))
		{
			group.setGroupParentID(formAdjust.adjustFormID(group.getGroupParentID()));
		}

		List<PluginRecord> recordList = group.getRecordList();
		for (PluginRecord record : recordList)
			if ((record instanceof PluginGroup))
				updateGroup((PluginGroup) record, formAdjust);
			else if (!record.isIgnored())
				updateRecord(record, formAdjust);
	}

	private void updateRecord(PluginRecord record, FormAdjust formAdjust) throws DataFormatException, IOException, PluginException
	{
		String recordType = record.getRecordType();
		int formID = record.getFormID();
		List<PluginSubrecord> subrecordList = record.getSubrecords();

		FormInfo info = formAdjust.getFormMap().get(new Integer(formID));
		if ((info == null) && (formID != 0))
		{
			throw new PluginException(String.format("No mapping for %s record %s (%08X)", new Object[]
			{ recordType, record.getEditorID(), Integer.valueOf(formID) }));
		}

		for (PluginSubrecord subrecord : subrecordList)
		{
			String subrecordType = subrecord.getSubrecordType();
			if (subrecordType.equals("EDID"))
			{
				if (info != null)
				{
					byte[] idString = info.getMergedEditorID().getBytes();
					byte[] subrecordData = new byte[idString.length + 1];
					System.arraycopy(idString, 0, subrecordData, 0, idString.length);
					subrecordData[idString.length] = 0;
					subrecord.setSubrecordData(subrecordData);
				}
			}
			else
			{
				int[][] references = subrecord.getReferences();
				if (references != null)
				{
					byte[] subrecordData = subrecord.getSubrecordData();
					for (int i = 0; i < references.length; i++)
					{
						int refOffset = references[i][0];
						int refFormID = references[i][1];
						if (refFormID != 0)
						{
							int mergedFormID = formAdjust.adjustFormID(refFormID);
							SerializedElement.setInteger(mergedFormID, subrecordData, refOffset);
						}
					}

					subrecord.setSubrecordData(subrecordData);
				}

			}

		}

		record.setSubrecords(subrecordList);

		if (info != null)
			record.setFormID(info.getMergedFormID());
	}

	private void mergePlugin(Plugin plugin) throws DataFormatException, IOException, PluginException
	{
		List<PluginGroup> mergedGroupList = this.mergedPlugin.getGroupList();
		List<PluginGroup> groupList = plugin.getGroupList();
		boolean editLeveledLists;

		if (this.masterMerge)
			editLeveledLists = false;
		else
		{
			editLeveledLists = this.pluginInfo.shouldEditConflicts();
		}

		for (PluginGroup group : groupList)
		{
			String groupRecordType = group.getGroupRecordType();
			group.removeIgnoredRecords();
			boolean groupMerged = false;

			for (PluginGroup mergedGroup : mergedGroupList)
			{
				if (mergedGroup.getGroupRecordType().equals(groupRecordType))
				{
					List<PluginRecord> recordList = group.getRecordList();
					List<PluginRecord> mergedRecordList = mergedGroup.getRecordList();

					for (PluginRecord record : recordList)
					{
						if ((record instanceof PluginGroup))
						{
							PluginGroup subgroup = (PluginGroup) record;
							int subgroupType = subgroup.getGroupType();
							byte[] subgroupLabel = subgroup.getGroupLabel();
							boolean subgroupMerged = false;
							for (PluginRecord mergedRecord : mergedRecordList)
							{
								if ((mergedRecord instanceof PluginGroup))
								{
									PluginGroup mergedSubgroup = (PluginGroup) mergedRecord;
									if (mergedSubgroup.getGroupType() == subgroupType)
									{
										byte[] mergedSubgroupLabel = mergedSubgroup.getGroupLabel();
										if ((mergedSubgroupLabel[0] != subgroupLabel[0]) || (mergedSubgroupLabel[1] != subgroupLabel[1])
												|| (mergedSubgroupLabel[2] != subgroupLabel[2])
												|| (mergedSubgroupLabel[3] != subgroupLabel[3]))
											continue;
										mergeGroup(mergedSubgroup, subgroup);
										subgroupMerged = true;
										break;
									}

								}

							}

							if (!subgroupMerged)
							{
								if (subgroupType == 2)
								{
									PluginGroup mergedSubgroup = new PluginGroup(subgroup.getGroupType(), subgroup.getGroupLabel());
									mergedRecordList.add(mergedSubgroup);
									mergedSubgroup.setParent(mergedGroup);
									mergeGroup(mergedSubgroup, subgroup);
								}
								else
								{
									mergedRecordList.add(record);
									record.setParent(mergedGroup);
								}

							}

						}
						else
						{
							String recordType = record.getRecordType();
							int index = mergedRecordList.indexOf(record);
							if (index < 0)
							{
								mergedRecordList.add(record);
								record.setParent(mergedGroup);
							}
							else
							{
								if (record.isDeleted())
									continue;
								PluginRecord mergedRecord = mergedRecordList.get(index);
								if ((recordType.equals("ACTI")) || (recordType.equals("AMMO")) || (recordType.equals("APPA"))
										|| (recordType.equals("ARMO")) || (recordType.equals("BOOK")) || (recordType.equals("CLAS"))
										|| (recordType.equals("CLOT")) || (recordType.equals("DOOR")) || (recordType.equals("EYES"))
										|| (recordType.equals("HAIR")) || (recordType.equals("LIGH")) || (recordType.equals("MISC"))
										|| (recordType.equals("SKIL")) || (recordType.equals("WEAP")))
								{
									mergeSubrecords(mergedRecord, record, null);
								}
								else if (recordType.equals("BSGN"))
								{
									mergeBirthsign(mergedRecord, record);
								}
								else if (recordType.equals("CONT"))
								{
									mergeContainer(mergedRecord, record);
								}
								else if ((recordType.equals("ALCH")) || (recordType.equals("ENCH")) || (recordType.equals("INGR"))
										|| (recordType.equals("SPEL")))
								{
									mergeEnchantment(mergedRecord, record);
								}
								else if (recordType.equals("FACT"))
								{
									mergeFaction(mergedRecord, record);
								}
								else if ((recordType.equals("LVLC")) || (recordType.equals("LVLI")) || (recordType.equals("LVSP")))
								{
									if (editLeveledLists)
										EditLeveledList.showWorkerDialog((JDialog) getParent(), mergedRecord, record, this.mergedFormMap,
												this.masters);
									else
										mergeLeveledList(mergedRecord, record);
								}
								else if ((recordType.equals("NPC_")) || (recordType.equals("CREA")))
								{
									mergeNPC(mergedRecord, record);
								}
								else if (recordType.equals("RACE"))
								{
									mergeRace(mergedRecord, record);
								}
								else if (recordType.equals("DIAL"))
								{
									mergeDialogTopic(mergedRecord, record);
								}
								else if ((this.masterMerge) || (!this.pluginInfo.shouldDeleteLastConflict()))
								{
									mergedRecordList.remove(index);
									mergedRecordList.add(index, record);
									record.setParent(mergedGroup);
								}
							}
						}

					}

					groupMerged = true;
					break;
				}
			}

			if (!groupMerged)
				throw new PluginException("Merge group " + groupRecordType + " not found");
		}
	}

	private void mergeGroup(PluginGroup mergedGroup, PluginGroup group) throws DataFormatException, IOException, PluginException
	{
		List<PluginRecord> mergedRecordList = mergedGroup.getRecordList();
		List<?> recordList = group.getRecordList();

		ListIterator<?> lit = recordList.listIterator();
		while (lit.hasNext())
		{
			PluginGroup mergedSubgroup;

			int index;

			PluginRecord record = (PluginRecord) lit.next();
			String recordType = record.getRecordType();
			if ((record instanceof PluginGroup))
			{

				PluginGroup subgroup = (PluginGroup) record;
				int subgroupType = subgroup.getGroupType();
				byte[] subgroupLabel = subgroup.getGroupLabel();
				boolean subgroupMerged = false;
				index = 0;
				for (PluginRecord mergedRecord : mergedRecordList)
				{
					if ((mergedRecord instanceof PluginGroup))
					{
						mergedSubgroup = (PluginGroup) mergedRecord;
						if (mergedSubgroup.getGroupType() == subgroupType)
						{
							byte[] mergedSubgroupLabel = mergedSubgroup.getGroupLabel();
							if ((mergedSubgroupLabel[0] == subgroupLabel[0]) && (mergedSubgroupLabel[1] == subgroupLabel[1])
									&& (mergedSubgroupLabel[2] == subgroupLabel[2]) && (mergedSubgroupLabel[3] == subgroupLabel[3]))
							{
								if ((index > 0) && ((subgroupType == 10) || (subgroupType == 8)))
								{
									mergedRecordList.remove(index);
									mergedRecordList.add(0, mergedSubgroup);
								}

								mergeGroup(mergedSubgroup, subgroup);
								subgroupMerged = true;
								break;
							}
						}
					}

					index++;
				}

				if (subgroupMerged)
					continue;

				if (subgroupType == 3)
				{
					mergedSubgroup = new PluginGroup(subgroup.getGroupType(), subgroup.getGroupLabel());
					mergedRecordList.add(mergedSubgroup);
					mergedSubgroup.setParent(mergedGroup);
					mergeGroup(mergedSubgroup, subgroup);
				}
				else if (subgroupType == 10)
				{
					if (mergedRecordList.size() > 0)
					{
						int insertIndex;
						insertIndex = 0;
						PluginGroup firstGroup;
						firstGroup = (PluginGroup) mergedRecordList.get(0);
						if (firstGroup.getGroupType() == 8)
							insertIndex = 1;
						mergedRecordList.add(insertIndex, record);
					}
					else
					{
						mergedRecordList.add(0, record);
					}
					record.setParent(mergedGroup);
				}
				else if (subgroupType == 8)
				{
					mergedRecordList.add(0, record);
					record.setParent(mergedGroup);
				}
				else
				{
					mergedRecordList.add(record);
					record.setParent(mergedGroup);
				}

			}
			else if ((recordType.equals("CELL")) && (((PluginGroup) record.getParent()).getGroupType() == 3))
			{
				PluginGroup parentGroup;
				List<PluginRecord> parentRecordList;
				PluginGroup subgroup;
				boolean foundGroup;
				int formID;
				FormInfo formInfo;

				formID = record.getFormID();
				formInfo = this.mergedFormMap.get(new Integer(formID));
				PluginRecord mergedRecord = (PluginRecord) formInfo.getSource();
				if (mergedRecord == record)
				{
					int baseFormID;
					int block;
					int subblock;
					baseFormID = formID & 0xFFFFFF;
					block = baseFormID % 10;
					subblock = baseFormID / 10 % 10;

					parentGroup = this.mergedPlugin.createTopGroup("CELL");
					parentRecordList = parentGroup.getRecordList();
					subgroup = null;

					foundGroup = false;
					for (PluginRecord groupRecord : parentRecordList)
					{
						subgroup = (PluginGroup) groupRecord;
						if (SerializedElement.getInteger(subgroup.getGroupLabel(), 0) == block)
						{
							foundGroup = true;
							break;
						}

					}

					if (!foundGroup)
					{
						byte[] groupLabel = new byte[4];
						SerializedElement.setInteger(block, groupLabel, 0);
						subgroup = new PluginGroup(2, groupLabel);
						subgroup.setParent(parentGroup);
						parentRecordList.add(subgroup);
					}

					parentGroup = subgroup;
					parentRecordList = parentGroup.getRecordList();

					foundGroup = false;
					for (PluginRecord groupRecord : parentRecordList)
					{
						subgroup = (PluginGroup) groupRecord;
						if (SerializedElement.getInteger(subgroup.getGroupLabel(), 0) == subblock)
						{
							foundGroup = true;
							break;
						}

					}

					if (!foundGroup)
					{
						byte[] groupLabel = new byte[4];
						SerializedElement.setInteger(subblock, groupLabel, 0);
						subgroup = new PluginGroup(3, groupLabel);
						subgroup.setParent(parentGroup);
						parentRecordList.add(subgroup);
					}

					parentGroup = subgroup;
					parentRecordList = parentGroup.getRecordList();

					index = parentRecordList.size();
					record.setParent(parentGroup);
					parentRecordList.add(record);

					subgroup = new PluginGroup(6, formID);
					subgroup.setParent(parentGroup);
					parentRecordList.add(subgroup);
				}
				else
				{
					parentGroup = (PluginGroup) mergedRecord.getParent();
					parentRecordList = parentGroup.getRecordList();
					index = parentRecordList.indexOf(record);
					if ((!record.isDeleted()) && ((this.masterMerge) || (!this.pluginInfo.shouldDeleteLastConflict())))
					{
						parentRecordList.remove(index);
						parentRecordList.add(index, record);
						record.setParent(parentGroup);
					}

				}

				if (lit.hasNext())
				{
					record = (PluginRecord) lit.next();
					index++;
					if (((record instanceof PluginGroup)) && (index < parentRecordList.size()))
					{
						mergedRecord = parentRecordList.get(index);
						if ((mergedRecord instanceof PluginGroup))
						{
							subgroup = (PluginGroup) record;
							mergedSubgroup = (PluginGroup) mergedRecord;
							mergeGroup(mergedSubgroup, subgroup);
						}
						else
						{
							lit.previous();
						}
					}
					else
					{
						lit.previous();
					}
				}
			}
			else
			{
				index = mergedRecordList.indexOf(record);

				if ((recordType.equals("INFO")) && (!record.isDeleted()) && (!this.deletedINFOMap.isEmpty())
						&& (this.deletedINFOMap.containsKey(Integer.valueOf(group.getGroupParentID()))))
				{
					List<PluginRecord> deletedList;
					deletedList = this.deletedINFOMap.get(Integer.valueOf(group.getGroupParentID()));
					Collections.reverse(deletedList);

					for (PluginRecord delRec : deletedList)
					{
						int delPNAMID = 0;
						int delFormID = delRec.getFormID();
						try
						{
							PluginSubrecord plSubrec = delRec.getSubrecord("PNAM");
							byte[] subrecordData = plSubrec.getSubrecordData();
							delPNAMID = SerializedElement.getInteger(subrecordData, 0);
						}
						catch (Exception ex)
						{
							continue;
						}
						try
						{
							//byte[] subrecordData;
							if (!record.changeSubrecord("PNAM", Integer.valueOf(delFormID), Integer.valueOf(delPNAMID)))
								continue;
						}
						catch (Exception localException1)
						{
						}
					}
					Collections.reverse(deletedList);
				}

				if (index < 0)
				{
					if (recordType.equals("INFO"))
						addTopic(mergedRecordList, record);
					else
					{
						mergedRecordList.add(record);
					}
					record.setParent(mergedGroup);
				}
				else
				{
					if (record.isDeleted())
					{
						continue;
					}

					if ((this.masterMerge) || (!this.pluginInfo.shouldDeleteLastConflict()))
					{
						mergedRecordList.remove(index);
						if (recordType.equals("INFO"))
						{
							addTopic(mergedRecordList, record);
						}
						else
						{
							mergedRecordList.add(index, record);
						}
						record.setParent(mergedGroup);
					}
				}
			}
		}
	}

	private void addTopic(List<PluginRecord> recordList, PluginRecord record) throws DataFormatException, IOException, PluginException
	{
		List<PluginSubrecord> subrecords = record.getSubrecords();
		int prevFormID = 0;
		for (PluginSubrecord subrecord : subrecords)
		{
			if (subrecord.getSubrecordType().equals("PNAM"))
			{
				prevFormID = SerializedElement.getInteger(subrecord.getSubrecordData(), 0);
				break;
			}

		}

		if (prevFormID == 0)
		{
			recordList.add(0, record);
		}
		else
		{
			ListIterator<PluginRecord> lit = recordList.listIterator();
			boolean recordInserted = false;
			while (lit.hasNext())
			{
				PluginRecord checkRecord = lit.next();
				if (checkRecord.getFormID() == prevFormID)
				{
					lit.add(record);
					if (lit.hasNext())
					{
						checkRecord = lit.next();
						subrecords = checkRecord.getSubrecords();
						for (PluginSubrecord subrecord : subrecords)
						{
							if (subrecord.getSubrecordType().equals("PNAM"))
							{
								byte[] subrecordData = subrecord.getSubrecordData();
								int checkFormID = SerializedElement.getInteger(subrecordData, 0);
								if (checkFormID != prevFormID)
									break;
								SerializedElement.setInteger(record.getFormID(), subrecordData, 0);
								subrecord.setSubrecordData(subrecordData);
								checkRecord.setSubrecords(subrecords);

								break;
							}
						}
					}

					recordInserted = true;
					break;
				}
			}

			if (!recordInserted)
				recordList.add(record);
		}
	}

	private void mergeSubrecords(PluginRecord mergedRecord, PluginRecord record, String[] exclusionList) throws DataFormatException,
			IOException, PluginException
	{
		boolean modifiedMergedList = false;
		boolean skipName = false;

		int formID = mergedRecord.getFormID();
		int masterID = formID >>> 24;
		if (masterID >= this.mergedMasterCount)
		{
			throw new PluginException("Merged master ID " + masterID + " is not valid");
		}
		Master master = this.masters[masterID];
		PluginRecord masterRecord = master.getRecord(formID);
		List<?> masterSubrecordList = masterRecord.getSubrecords();
		List<PluginSubrecord> mergedSubrecordList = mergedRecord.getSubrecords();
		List<?> subrecordList = record.getSubrecords();

		ListIterator<?> lit = subrecordList.listIterator();
		while (lit.hasNext())
		{
			PluginSubrecord subrecord = (PluginSubrecord) lit.next();
			boolean foundSubrecord = false;
			String subrecordType = subrecord.getSubrecordType();
			if ((subrecordType.equals("MODB")) || (subrecordType.equals("MODT")) || (subrecordType.equals("MO2B"))
					|| (subrecordType.equals("MO2T")) || (subrecordType.equals("MO3B")) || (subrecordType.equals("MO3T"))
					|| (subrecordType.equals("MO4B")) || (subrecordType.equals("MO4T")))
			{
				continue;
			}
			if ((skipName) && (subrecordType.equals("FULL")))
			{
				continue;
			}
			if (subrecordType.equals("EFID"))
			{
				skipName = true;
			}
			if (exclusionList != null)
			{
				boolean mergeSubrecord = true;
				for (String exclusion : exclusionList)
				{
					if (exclusion.equals(subrecordType))
					{
						mergeSubrecord = false;
						break;
					}
				}

				if (!mergeSubrecord)
				{
					continue;
				}

			}

			ListIterator<PluginSubrecord> mlit = mergedSubrecordList.listIterator();
			boolean skipMergedName = false;
			while (mlit.hasNext())
			{
				PluginSubrecord mergedSubrecord = mlit.next();
				if (mergedSubrecord.getSubrecordType().equals(subrecordType))
				{
					if ((skipMergedName) && (subrecordType.equals("FULL")))
					{
						break;
					}
					if (subrecordType.equals("EFID"))
					{
						skipMergedName = true;
					}
					foundSubrecord = true;
					boolean replaceSubrecord = false;
					if (!mergedSubrecord.equals(subrecord))
					{
						int mergedIndex = masterSubrecordList.indexOf(mergedSubrecord);
						int index = masterSubrecordList.indexOf(subrecord);
						if (mergedIndex < 0)
						{
							if (index < 0)
							{
								if ((this.masterMerge) || (!this.pluginInfo.shouldDeleteLastConflict()))
								{
									replaceSubrecord = true;
								}

							}

						}
						else
						{
							replaceSubrecord = true;
						}
					}

					if (!replaceSubrecord)
					{
						break;
					}

					String modb = null;
					String modt = null;
					if (subrecordType.equals("MODL"))
					{
						modb = "MODB";
						modt = "MODT";
					}
					else if (subrecordType.equals("MOD2"))
					{
						modb = "MO2B";
						modt = "MO2T";
					}
					else if (subrecordType.equals("MOD3"))
					{
						modb = "MO3B";
						modt = "MO3T";
					}
					else if (subrecordType.equals("MOD4"))
					{
						modb = "MO4B";
						modt = "MO4T";
					}

					mlit.set(subrecord);

					if ((modb != null) && (lit.hasNext()))
					{
						subrecord = (PluginSubrecord) lit.next();
						if (subrecord.getSubrecordType().equals(modb))
						{
							if (mlit.hasNext())
							{
								mergedSubrecord = mlit.next();
								if (mergedSubrecord.getSubrecordType().equals(modb))
								{
									mlit.set(subrecord);
								}
								else
								{
									mlit.previous();
									mlit.add(subrecord);
								}
							}
							else
							{
								mlit.add(subrecord);
							}
						}
						else
							lit.previous();

					}

					if ((modt != null) && (lit.hasNext()))
					{
						subrecord = (PluginSubrecord) lit.next();
						if (subrecord.getSubrecordType().equals(modt))
						{
							if (mlit.hasNext())
							{
								mergedSubrecord = mlit.next();
								if (mergedSubrecord.getSubrecordType().equals(modt))
								{
									mlit.set(subrecord);
								}
								else
								{
									mlit.previous();
									mlit.add(subrecord);
								}
							}
							else
							{
								mlit.add(subrecord);
							}
						}
						else
							lit.previous();

					}

					modifiedMergedList = true;

					break;
				}

			}

			if (foundSubrecord)
			{
				continue;
			}

			String modb = null;
			String modt = null;
			if (subrecordType.equals("MODL"))
			{
				modb = "MODB";
				modt = "MODT";
			}
			else if (subrecordType.equals("MOD2"))
			{
				modb = "MO2B";
				modt = "MO2T";
			}
			else if (subrecordType.equals("MOD3"))
			{
				modb = "MO3B";
				modt = "MO3T";
			}
			else if (subrecordType.equals("MOD4"))
			{
				modb = "MO4B";
				modt = "MO4T";
			}

			if (subrecordType.equals("FULL"))
				mergedSubrecordList.add(1, subrecord);
			else
			{
				mergedSubrecordList.add(subrecord);
			}

			if ((modb != null) && (lit.hasNext()))
			{
				subrecord = (PluginSubrecord) lit.next();
				if (subrecord.getSubrecordType().equals(modb))
					mergedSubrecordList.add(subrecord);
				else
				{
					lit.previous();
				}

			}

			if ((modt != null) && (lit.hasNext()))
			{
				subrecord = (PluginSubrecord) lit.next();
				if (subrecord.getSubrecordType().equals(modt))
					mergedSubrecordList.add(subrecord);
				else
				{
					lit.previous();
				}
			}
			modifiedMergedList = true;
		}

		if (modifiedMergedList)
			mergedRecord.setSubrecords(mergedSubrecordList);
	}

	private boolean mergeArray(String subrecordType, int entrySize, List<PluginSubrecord> mergedSubrecordList,
			List<PluginSubrecord> subrecordList) throws IOException
	{
		boolean modifiedMergedList = false;

		for (PluginSubrecord subrecord : subrecordList)
		{
			if (!subrecord.getSubrecordType().equals(subrecordType))
			{
				continue;
			}
			boolean addSubrecord = true;
			boolean replaceSubrecordData = false;
			byte[] subrecordData = subrecord.getSubrecordData();

			for (PluginSubrecord mergedSubrecord : mergedSubrecordList)
			{
				if (!mergedSubrecord.getSubrecordType().equals(subrecordType))
				{
					continue;
				}
				addSubrecord = false;
				byte[] mergedSubrecordData = mergedSubrecord.getSubrecordData();

				for (int offset = 0; offset < subrecordData.length; offset += entrySize)
				{
					boolean addReference = true;
					int reference = SerializedElement.getInteger(subrecordData, offset);
					for (int mergedOffset = 0; mergedOffset < mergedSubrecordData.length; mergedOffset += entrySize)
					{
						int mergedReference = SerializedElement.getInteger(mergedSubrecordData, mergedOffset);
						if (reference == mergedReference)
						{
							addReference = false;
							break;
						}
					}

					if (addReference)
					{
						int length = mergedSubrecordData.length;
						byte[] newSubrecordData = new byte[length + entrySize];
						System.arraycopy(mergedSubrecordData, 0, newSubrecordData, 0, length);
						System.arraycopy(subrecordData, offset, newSubrecordData, length, entrySize);
						mergedSubrecordData = newSubrecordData;
						replaceSubrecordData = true;
					}
				}

				if (!replaceSubrecordData)
					break;
				mergedSubrecord.setSubrecordData(mergedSubrecordData);
				modifiedMergedList = true;

				break;
			}

			if (!addSubrecord)
				break;
			mergedSubrecordList.add(subrecord);
			modifiedMergedList = true;

			break;
		}

		return modifiedMergedList;
	}

	private boolean mergeList(String subrecordType, List<PluginSubrecord> mergedSubrecordList, List<PluginSubrecord> subrecordList)
			throws IOException
	{
		boolean modifiedMergedList = false;

		for (PluginSubrecord subrecord : subrecordList)
		{
			if (!subrecord.getSubrecordType().equals(subrecordType))
			{
				continue;
			}
			byte[] subrecordData = subrecord.getSubrecordData();
			int subrecordID = SerializedElement.getInteger(subrecordData, 0);
			boolean insertSubrecord = true;
			boolean foundSubrecord = false;
			int index = 0;

			for (PluginSubrecord mergedSubrecord : mergedSubrecordList)
			{
				if (mergedSubrecord.getSubrecordType().equals(subrecordType))
				{
					foundSubrecord = true;
					subrecordData = mergedSubrecord.getSubrecordData();
					int mergedSubrecordID = SerializedElement.getInteger(subrecordData, 0);
					if (subrecordID == mergedSubrecordID)
					{
						insertSubrecord = false;
						break;
					}
				}
				else
				{
					if (foundSubrecord)
					{
						break;
					}
				}
				index++;
			}

			if (insertSubrecord)
			{
				mergedSubrecordList.add(index, subrecord);
				modifiedMergedList = true;
			}
		}

		return modifiedMergedList;
	}

	private void mergeBirthsign(PluginRecord mergedRecord, PluginRecord record) throws DataFormatException, IOException, PluginException
	{
		boolean modifiedMergedList = false;

		String[] exclusionList =
		{ "SPLO" };
		mergeSubrecords(mergedRecord, record, exclusionList);

		List<PluginSubrecord> mergedSubrecordList = mergedRecord.getSubrecords();
		List<PluginSubrecord> subrecordList = record.getSubrecords();

		if (mergeList("SPLO", mergedSubrecordList, subrecordList))
		{
			modifiedMergedList = true;
		}

		if (modifiedMergedList)
			mergedRecord.setSubrecords(mergedSubrecordList);
	}

	private void mergeDialogTopic(PluginRecord mergedRecord, PluginRecord record) throws DataFormatException, IOException, PluginException
	{
		boolean modifiedMergedList = false;

		List<PluginSubrecord> mergedSubrecordList = mergedRecord.getSubrecords();
		List<PluginSubrecord> subrecordList = record.getSubrecords();

		if ((this.masterMerge) || (!this.pluginInfo.shouldDeleteLastConflict()))
		{
			String oldEDID = mergedRecord.getEditorID();
			String newEDID = record.getEditorID();
			if (!oldEDID.equals(newEDID))
			{
				modifiedMergedList = true;
				mergedRecord.setEditorID(record.getEditorID());
			}
			PluginSubrecord oldSubFULL = mergedRecord.getSubrecord("FULL");
			PluginSubrecord newSubFULL = record.getSubrecord("FULL");
			if (!oldSubFULL.equals(newSubFULL))
			{
				modifiedMergedList = true;
				mergedRecord.changeSubrecord("FULL", oldSubFULL.getSubrecordData(), newSubFULL.getSubrecordData());
			}

		}

		if (mergeList("QSTI", mergedSubrecordList, subrecordList))
		{
			modifiedMergedList = true;
		}

		if (modifiedMergedList)
			mergedRecord.setSubrecords(mergedSubrecordList);
	}

	private void mergeContainer(PluginRecord mergedRecord, PluginRecord record) throws DataFormatException, IOException, PluginException
	{
		boolean modifiedMergedList = false;

		String[] exclusionList =
		{ "CNTO" };
		mergeSubrecords(mergedRecord, record, exclusionList);

		List<PluginSubrecord> mergedSubrecordList = mergedRecord.getSubrecords();
		List<PluginSubrecord> subrecordList = record.getSubrecords();

		if (mergeList("CNTO", mergedSubrecordList, subrecordList))
		{
			modifiedMergedList = true;
		}

		if (modifiedMergedList)
			mergedRecord.setSubrecords(mergedSubrecordList);
	}

	private void mergeEnchantment(PluginRecord mergedRecord, PluginRecord record) throws DataFormatException, IOException, PluginException
	{
		String recordType = record.getRecordType();
		boolean modifiedMergedList = false;
		int pluginField1 = 0;
		int pluginField2 = 0;
		boolean autoCalculate = true;

		String[] exclusionList =
		{ "EFID", "EFIT", "ENIT", "SCIT", "SPIT" };
		mergeSubrecords(mergedRecord, record, exclusionList);

		List<PluginSubrecord> mergedSubrecordList = mergedRecord.getSubrecords();
		List<?> subrecordList = record.getSubrecords();

		ListIterator<?> lit = subrecordList.listIterator();

		while (lit.hasNext())
		{
			PluginSubrecord subrecord = (PluginSubrecord) lit.next();
			String subrecordType = subrecord.getSubrecordType();
			if (subrecordType.equals("ENIT"))
			{
				byte[] subrecordData = subrecord.getSubrecordData();
				if ((recordType.equals("ALCH")) || (recordType.equals("INGR")))
				{
					pluginField1 = SerializedElement.getInteger(subrecordData, 0);
					if ((subrecordData[4] & 0x1) != 0)
						autoCalculate = false;
				}
				else
				{
					pluginField1 = SerializedElement.getInteger(subrecordData, 4);
					pluginField2 = SerializedElement.getInteger(subrecordData, 8);
					if ((subrecordData[12] & 0x1) != 0)
						autoCalculate = false;
				}
			}
			else if (subrecordType.equals("SPIT"))
			{
				byte[] subrecordData = subrecord.getSubrecordData();
				pluginField1 = SerializedElement.getInteger(subrecordData, 4);
				pluginField2 = SerializedElement.getInteger(subrecordData, 8);
				if ((subrecordData[12] & 0x1) != 0)
					autoCalculate = false;
			}
			else if (subrecordType.equals("EFIT"))
			{
				byte[] subrecordData = subrecord.getSubrecordData();
				boolean addEffect = true;
				String effectName = new String(subrecordData, 0, 4);
				int effectSubtype = 0;
				int scriptID = 0;
				PluginSubrecord scitSubrecord = null;
				PluginSubrecord fullSubrecord = null;
				if ((effectName.equals("DGAT")) || (effectName.equals("DRAT")) || (effectName.equals("DRSK"))
						|| (effectName.equals("FOAT")) || (effectName.equals("FOSK")) || (effectName.equals("REAT"))
						|| (effectName.equals("ABAT")) || (effectName.equals("ABSK")))
				{
					effectSubtype = SerializedElement.getInteger(subrecordData, 20);
				}
				if (effectName.equals("SEFF"))
				{
					if (lit.hasNext())
					{
						scitSubrecord = (PluginSubrecord) lit.next();
						if (scitSubrecord.getSubrecordType().equals("SCIT"))
						{
							byte[] scitSubrecordData = scitSubrecord.getSubrecordData();
							scriptID = SerializedElement.getInteger(scitSubrecordData, 0);
							if (lit.hasNext())
							{
								fullSubrecord = (PluginSubrecord) lit.next();
								if (!fullSubrecord.getSubrecordType().equals("FULL"))
								{
									lit.previous();
									fullSubrecord = null;
								}
							}
						}
						else
						{
							scitSubrecord = null;
						}
					}

					if (scitSubrecord == null)
					{
						throw new PluginException("SCIT subrecord missing for script effect");
					}
				}
				ListIterator<PluginSubrecord> mlit = mergedSubrecordList.listIterator();
				while (mlit.hasNext())
				{
					PluginSubrecord mergedSubrecord = mlit.next();
					if (mergedSubrecord.getSubrecordType().equals("EFIT"))
					{
						byte[] mergedSubrecordData = mergedSubrecord.getSubrecordData();
						String mergedEffectName = new String(mergedSubrecordData, 0, 4);
						int mergedEffectSubtype = 0;

						if (mergedEffectName.equals(effectName))
						{
							if ((mergedEffectName.equals("DGAT")) || (mergedEffectName.equals("DRAT")) || (mergedEffectName.equals("DRSK"))
									|| (mergedEffectName.equals("FOAT")) || (mergedEffectName.equals("FOSK"))
									|| (mergedEffectName.equals("REAT")) || (mergedEffectName.equals("ABAT"))
									|| (mergedEffectName.equals("ABSK")))
								mergedEffectSubtype = SerializedElement.getInteger(mergedSubrecordData, 20);
							if (effectName.equals("SEFF"))
							{
								PluginSubrecord mscitSubrecord = null;
								if (mlit.hasNext())
								{
									mscitSubrecord = mlit.next();
									if (mscitSubrecord.getSubrecordType().equals("SCIT"))
									{
										mergedSubrecordData = mscitSubrecord.getSubrecordData();
										int mergedScriptID = SerializedElement.getInteger(mergedSubrecordData, 0);
										if (mergedScriptID == scriptID)
										{
											addEffect = false;
											break;
										}
									}
									else
									{
										mscitSubrecord = null;
									}
								}

								if (mscitSubrecord == null)
									throw new PluginException("SCIT subrecord missing for script effect");
							}
							else if (mergedEffectSubtype == effectSubtype)
							{
								addEffect = false;
								break;
							}

						}

					}

				}

				if (addEffect)
				{
					byte[] mergedSubrecordData = new byte[4];
					mergedSubrecordData[0] = subrecordData[0];
					mergedSubrecordData[1] = subrecordData[1];
					mergedSubrecordData[2] = subrecordData[2];
					mergedSubrecordData[3] = subrecordData[3];
					mergedSubrecordList.add(new PluginSubrecord(recordType, "EFID", mergedSubrecordData));
					mergedSubrecordList.add(subrecord);
					if (scitSubrecord != null)
					{
						mergedSubrecordList.add(scitSubrecord);
						if (fullSubrecord != null)
						{
							mergedSubrecordList.add(fullSubrecord);
						}
					}
					modifiedMergedList = true;
				}

			}

		}

		if (modifiedMergedList)
		{
			for (PluginSubrecord subrecord : mergedSubrecordList)
			{
				String subrecordType = subrecord.getSubrecordType();
				if ((!subrecordType.equals("ENIT")) && (!subrecordType.equals("SPIT")))
				{
					continue;
				}
				byte[] subrecordData = subrecord.getSubrecordData();
				if ((recordType.equals("ALCH")) || (recordType.equals("INGR")))
				{
					int mergedField1 = SerializedElement.getInteger(subrecordData, 0);
					mergedField1 = Math.max(pluginField1, mergedField1);
					SerializedElement.setInteger(mergedField1, subrecordData, 0);
					if (autoCalculate)
						break;
					subrecordData[4] = (byte) (subrecordData[4] | 0x1);
					break;
				}
				int mergedField1 = SerializedElement.getInteger(subrecordData, 4);
				int mergedField2 = SerializedElement.getInteger(subrecordData, 8);
				mergedField1 = Math.max(pluginField1, mergedField1);
				mergedField2 = Math.max(pluginField2, mergedField2);
				SerializedElement.setInteger(mergedField1, subrecordData, 4);
				SerializedElement.setInteger(mergedField2, subrecordData, 8);
				if (autoCalculate)
					break;
				subrecordData[12] = (byte) (subrecordData[12] | 0x1);

				break;
			}

		}

		if (modifiedMergedList)
			mergedRecord.setSubrecords(mergedSubrecordList);
	}

	private void mergeFaction(PluginRecord mergedRecord, PluginRecord record) throws DataFormatException, IOException, PluginException
	{
		boolean modifiedMergedList = false;

		String[] exclusionList =
		{ "FNAM", "INAM", "MNAM", "RNAM", "XNAM" };
		mergeSubrecords(mergedRecord, record, exclusionList);

		int formID = mergedRecord.getFormID();
		int masterID = formID >>> 24;
		if (masterID >= this.mergedMasterCount)
		{
			throw new PluginException("Merged master ID " + masterID + " is not valid");
		}
		Master master = this.masters[masterID];
		PluginRecord masterRecord = master.getRecord(formID);
		List<PluginSubrecord> masterSubrecordList = masterRecord.getSubrecords();
		List<PluginSubrecord> mergedSubrecordList = mergedRecord.getSubrecords();
		List<PluginSubrecord> subrecordList = record.getSubrecords();

		int ranks = 0;
		int mergedRanks = 0;
		int masterRanks = 0;
		for (PluginSubrecord subrecord : subrecordList)
		{
			if (subrecord.getSubrecordType().equals("RNAM"))
				ranks++;
		}
		for (PluginSubrecord subrecord : mergedSubrecordList)
		{
			if (subrecord.getSubrecordType().equals("RNAM"))
				mergedRanks++;
		}
		for (PluginSubrecord subrecord : masterSubrecordList)
		{
			if (subrecord.getSubrecordType().equals("RNAM"))
				masterRanks++;
		}
		boolean keepFirst = true;
		if (mergedRanks == masterRanks)
		{
			if (ranks != masterRanks)
				keepFirst = false;
			else if ((this.masterMerge) || (!this.pluginInfo.shouldDeleteLastConflict()))
				keepFirst = false;
		}
		else if ((ranks != masterRanks) && ((this.masterMerge) || (!this.pluginInfo.shouldDeleteLastConflict())))
			keepFirst = false;

		if (!keepFirst)
		{
			modifiedMergedList = true;
			ListIterator<PluginSubrecord> lit = mergedSubrecordList.listIterator();
			while (lit.hasNext())
			{
				PluginSubrecord subrecord = lit.next();
				String subrecordType = subrecord.getSubrecordType();
				if ((!subrecordType.equals("RNAM")) && (!subrecordType.equals("MNAM")) && (!subrecordType.equals("FNAM"))
						&& (!subrecordType.equals("INAM")))
					continue;
				lit.remove();
			}

			for (Iterator<PluginSubrecord> subrecordTypeIt = subrecordList.iterator(); subrecordTypeIt.hasNext();)
			{
				PluginSubrecord subrecord = subrecordTypeIt.next();
				String subrecordType = subrecord.getSubrecordType();
				if ((!subrecordType.equals("RNAM")) && (!subrecordType.equals("MNAM")) && (!subrecordType.equals("FNAM"))
						&& (!subrecordType.equals("INAM")))
					continue;
				mergedSubrecordList.add(subrecord);
			}

		}

		for (PluginSubrecord subrecord : subrecordList)
		{
			if (!subrecord.getSubrecordType().equals("XNAM"))
			{
				continue;
			}
			byte[] subrecordData = subrecord.getSubrecordData();
			int faction = SerializedElement.getInteger(subrecordData, 0);
			boolean insertSubrecord = true;
			boolean foundSubrecord = false;
			int index = 0;
			for (PluginSubrecord mergedSubrecord : mergedSubrecordList)
			{
				if (mergedSubrecord.getSubrecordType().equals("XNAM"))
				{
					foundSubrecord = true;
					subrecordData = mergedSubrecord.getSubrecordData();
					int mergedFaction = SerializedElement.getInteger(subrecordData, 0);
					if (faction == mergedFaction)
					{
						if (subrecord.equals(mergedSubrecord))
						{
							insertSubrecord = false;
							break;
						}
						if (masterSubrecordList.indexOf(mergedSubrecord) < 0)
						{
							if (masterSubrecordList.indexOf(subrecord) < 0)
							{
								if ((this.masterMerge) || (!this.pluginInfo.shouldDeleteLastConflict()))
								{
									mergedSubrecordList.remove(index);
									break;
								}
								insertSubrecord = false;
								break;
							}
							insertSubrecord = false;
							break;
						}
						mergedSubrecordList.remove(index);

						break;
					}
				}
				else
				{
					if (foundSubrecord)
					{
						break;
					}
				}
				index++;
			}

			if (insertSubrecord)
			{
				mergedSubrecordList.add(index, subrecord);
				modifiedMergedList = true;
			}

		}

		if (modifiedMergedList)
			mergedRecord.setSubrecords(mergedSubrecordList);
	}

	private void mergeLeveledList(PluginRecord mergedRecord, PluginRecord record) throws DataFormatException, IOException, PluginException
	{
		String recordType = record.getRecordType();

		int formID = mergedRecord.getFormID();
		int masterID = formID >>> 24;
		if (masterID >= this.mergedMasterCount)
		{
			throw new PluginException("Merged leveled list master ID " + masterID + " is not valid");
		}
		Master master = this.masters[masterID];
		PluginRecord masterRecord = master.getRecord(formID);
		List<PluginSubrecord> masterSubrecordList = masterRecord.getSubrecords();
		List<PluginSubrecord> mergedSubrecordList = mergedRecord.getSubrecords();
		List<PluginSubrecord> subrecordList = record.getSubrecords();

		boolean foundFlags = false;
		int chanceIndex = -1;
		int subrecordIndex = 0;
		for (PluginSubrecord masterSubrecord : masterSubrecordList)
		{
			if (masterSubrecord.getSubrecordType().equals("LVLF"))
			{
				foundFlags = true;
				break;
			}

			if (masterSubrecord.getSubrecordType().equals("LVLD"))
			{
				chanceIndex = subrecordIndex;
			}
			subrecordIndex++;
		}

		if (!foundFlags)
			if (chanceIndex < 0)
			{
				byte[] flagsData = new byte[1];
				flagsData[0] = 0;
				masterSubrecordList.add(new PluginSubrecord(recordType, "LVLF", flagsData));
				byte[] chanceData = new byte[1];
				chanceData[0] = 0;
				masterSubrecordList.add(new PluginSubrecord(recordType, "LVLD", chanceData));
			}
			else
			{
				PluginSubrecord chanceSubrecord = masterSubrecordList.get(chanceIndex);
				byte[] chanceData = chanceSubrecord.getSubrecordData();
				byte[] flagsData = new byte[1];
				flagsData[0] = (byte) ((chanceData[0] & 0xFF) >>> 7);
				chanceData[0] = (byte) (chanceData[0] & 0x7F);
				masterSubrecordList.add(new PluginSubrecord(recordType, "LVLF", flagsData));
			}

		for (Object chanceData = subrecordList.iterator(); ((Iterator<?>) chanceData).hasNext();)
		{
			PluginSubrecord subrecord = (PluginSubrecord) ((Iterator<?>) chanceData).next();
			boolean foundSubrecord = false;
			String subrecordType = subrecord.getSubrecordType();
			if (subrecordType.equals("LVLO"))
			{
				continue;
			}

			for (PluginSubrecord mergedSubrecord : mergedSubrecordList)
			{
				if (mergedSubrecord.getSubrecordType().equals(subrecordType))
				{
					foundSubrecord = true;
					boolean replaceSubrecord = false;
					if (!mergedSubrecord.equals(subrecord))
					{
						int mergedIndex = masterSubrecordList.indexOf(mergedSubrecord);
						int index = masterSubrecordList.indexOf(subrecord);
						if (mergedIndex < 0)
						{
							if (index < 0)
							{
								if ((this.masterMerge) || (!this.pluginInfo.shouldDeleteLastConflict()))
								{
									replaceSubrecord = true;
								}

							}

						}
						else
						{
							replaceSubrecord = true;
						}
					}

					if (!replaceSubrecord)
						break;
					int index = mergedSubrecordList.indexOf(mergedSubrecord);
					mergedSubrecordList.remove(index);
					mergedSubrecordList.add(index, subrecord);

					break;
				}

			}

			if (!foundSubrecord)
			{
				mergedSubrecordList.add(subrecord);
			}

		}

		for (Iterator<PluginSubrecord> chanceData = subrecordList.iterator(); chanceData.hasNext();)
		{
			PluginSubrecord subrecord = chanceData.next();
			if (subrecord.getSubrecordType().equals("LVLO"))
			{
				byte[] subrecordData = subrecord.getSubrecordData();
				int itemLevel = SerializedElement.getShort(subrecordData, 0);
				int itemID = SerializedElement.getInteger(subrecordData, 4);
				boolean duplicate = false;
				int index = 0;
				for (PluginSubrecord mergedSubrecord : mergedSubrecordList)
				{
					if (mergedSubrecord.getSubrecordType().equals("LVLO"))
					{
						byte[] mergedSubrecordData = mergedSubrecord.getSubrecordData();
						int mergedItemLevel = SerializedElement.getShort(mergedSubrecordData, 0);
						int mergedItemID = SerializedElement.getInteger(mergedSubrecordData, 4);
						if (mergedItemLevel > itemLevel)
						{
							break;
						}
						if ((mergedItemLevel == itemLevel) && (mergedItemID == itemID))
						{
							duplicate = true;
							break;
						}
					}

					index++;
				}

				if (!duplicate)
				{
					mergedSubrecordList.add(index, subrecord);
				}

			}

		}

		mergedRecord.setSubrecords(mergedSubrecordList);
	}

	private void mergeNPC(PluginRecord mergedRecord, PluginRecord record) throws DataFormatException, IOException, PluginException
	{
		boolean modifiedMergedList = false;

		String[] exclusionList =
		{ "CNTO", "CSDC", "CSDI", "CSDT", "PKID", "SNAM", "SPLO" };
		mergeSubrecords(mergedRecord, record, exclusionList);

		List<PluginSubrecord> mergedSubrecordList = mergedRecord.getSubrecords();
		List<PluginSubrecord> subrecordList = record.getSubrecords();

		if (mergeList("CNTO", mergedSubrecordList, subrecordList))
		{
			modifiedMergedList = true;
		}

		if (mergeList("PKID", mergedSubrecordList, subrecordList))
		{
			modifiedMergedList = true;
		}

		if (mergeList("SNAM", mergedSubrecordList, subrecordList))
		{
			modifiedMergedList = true;
		}

		if (mergeList("SPLO", mergedSubrecordList, subrecordList))
		{
			modifiedMergedList = true;
		}

		if ((record.getRecordType().equals("CREA")) && ((this.masterMerge) || (!this.pluginInfo.shouldDeleteLastConflict())))
		{
			ListIterator<PluginSubrecord> lit = subrecordList.listIterator();
			while (lit.hasNext())
			{
				PluginSubrecord subrecord = lit.next();
				if (subrecord.getSubrecordType().equals("CSDT"))
				{
					int soundIndex = SerializedElement.getInteger(subrecord.getSubrecordData(), 0);
					boolean foundSound = false;
					ListIterator<PluginSubrecord> mlit = mergedSubrecordList.listIterator();
					while (mlit.hasNext())
					{
						PluginSubrecord mergedSubrecord = mlit.next();
						if (mergedSubrecord.getSubrecordType().equals("CSDT"))
						{
							int mergedIndex = SerializedElement.getInteger(mergedSubrecord.getSubrecordData(), 0);
							if (mergedIndex == soundIndex)
							{
								foundSound = true;
								mlit.set(subrecord);
								mlit.next();
								mlit.set(lit.next());
								mlit.next();
								mlit.set(lit.next());
								break;
							}
						}
					}

					if (!foundSound)
					{
						mergedSubrecordList.add(subrecord);
						mergedSubrecordList.add(lit.next());
						mergedSubrecordList.add(lit.next());
					}

					modifiedMergedList = true;
				}

			}

		}

		if (modifiedMergedList)
			mergedRecord.setSubrecords(mergedSubrecordList);
	}

	private void mergeRace(PluginRecord mergedRecord, PluginRecord record) throws DataFormatException, IOException, PluginException
	{
		boolean modifiedMergedList = false;

		String[] exclusionList =
		{ "ENAM", "FNAM", "HNAM", "ICON", "INDX", "MNAM", "MODB", "MODL", "NAM0", "NAM1", "SPLO", "XNAM" };
		mergeSubrecords(mergedRecord, record, exclusionList);

		int formID = mergedRecord.getFormID();
		int masterID = formID >>> 24;
		if (masterID >= this.mergedMasterCount)
		{
			throw new PluginException("Merged master ID " + masterID + " is not valid");
		}
		Master master = this.masters[masterID];
		PluginRecord masterRecord = master.getRecord(formID);
		List<PluginSubrecord> masterSubrecordList = masterRecord.getSubrecords();
		List<PluginSubrecord> mergedSubrecordList = mergedRecord.getSubrecords();
		List<PluginSubrecord> subrecordList = record.getSubrecords();

		if (mergeList("SPLO", mergedSubrecordList, subrecordList))
		{
			modifiedMergedList = true;
		}

		for (PluginSubrecord subrecord : subrecordList)
		{
			if (!subrecord.getSubrecordType().equals("XNAM"))
			{
				continue;
			}
			byte[] subrecordData = subrecord.getSubrecordData();
			int raceID = SerializedElement.getInteger(subrecordData, 0);
			int modifier = SerializedElement.getInteger(subrecordData, 4);
			boolean insertSubrecord = true;
			boolean foundSubrecord = false;
			int index = 0;
			for (PluginSubrecord mergedSubrecord : mergedSubrecordList)
			{
				if (mergedSubrecord.getSubrecordType().equals("XNAM"))
				{
					foundSubrecord = true;
					subrecordData = mergedSubrecord.getSubrecordData();
					int mergedRaceID = SerializedElement.getInteger(subrecordData, 0);
					int mergedModifier = SerializedElement.getInteger(subrecordData, 4);
					if (raceID == mergedRaceID)
					{
						if (subrecord.equals(mergedSubrecord))
						{
							insertSubrecord = false;
							break;
						}
						if (masterSubrecordList.indexOf(mergedSubrecord) < 0)
						{
							if (masterSubrecordList.indexOf(subrecord) < 0)
							{
								if ((this.masterMerge) || (!this.pluginInfo.shouldDeleteLastConflict()))
								{
									mergedSubrecordList.remove(index);
									break;
								}
								insertSubrecord = false;
								break;
							}
							insertSubrecord = false;
							break;
						}
						mergedSubrecordList.remove(index);

						break;
					}
				}
				else
				{
					if (foundSubrecord)
					{
						break;
					}
				}
				index++;
			}

			if (insertSubrecord)
			{
				mergedSubrecordList.add(index, subrecord);
				modifiedMergedList = true;
			}

		}

		if (mergeArray("ENAM", 4, mergedSubrecordList, subrecordList))
		{
			modifiedMergedList = true;
		}

		if (mergeArray("HNAM", 4, mergedSubrecordList, subrecordList))
		{
			modifiedMergedList = true;
		}

		if (mergeFaceData(subrecordList, mergedSubrecordList, masterSubrecordList))
		{
			modifiedMergedList = true;
		}

		if (mergeBodyData(subrecordList, mergedSubrecordList, masterSubrecordList))
		{
			modifiedMergedList = true;
		}

		if (modifiedMergedList)
			mergedRecord.setSubrecords(mergedSubrecordList);
	}

	private boolean mergeFaceData(List<PluginSubrecord> subrecordList, List<PluginSubrecord> mergedSubrecordList,
			List<PluginSubrecord> masterSubrecordList) throws PluginException
	{
		boolean modifiedMergedList = false;

		boolean foundList = false;
		int count = 0;
		for (PluginSubrecord subrecord : subrecordList)
		{
			String subrecordType = subrecord.getSubrecordType();
			if (subrecordType.equals("NAM1"))
			{
				if (foundList)
					break;
				throw new PluginException("Face data does not precede body data");
			}

			if (!foundList)
			{
				if (subrecordType.equals("NAM0"))
					foundList = true;
			}
			else if (subrecordType.equals("INDX"))
			{
				count++;
			}
		}

		PluginSubrecord[] indx = new PluginSubrecord[count];
		PluginSubrecord[] modl = new PluginSubrecord[count];
		PluginSubrecord[] modb = new PluginSubrecord[count];
		PluginSubrecord[] icon = new PluginSubrecord[count];
		int index = -1;
		foundList = false;
		for (PluginSubrecord subrecord : subrecordList)
		{
			String subrecordType = subrecord.getSubrecordType();
			if (!foundList)
			{
				if (subrecordType.equals("NAM0"))
					foundList = true;
			}
			else if (subrecordType.equals("INDX"))
			{
				index++;
				indx[index] = subrecord;
			}
			else if (subrecordType.equals("MODL"))
			{
				modl[index] = subrecord;
			}
			else if (subrecordType.equals("MODB"))
			{
				modb[index] = subrecord;
			}
			else
			{
				if (!subrecordType.equals("ICON"))
					break;
				icon[index] = subrecord;
			}

		}

		foundList = false;
		count = 0;
		for (PluginSubrecord subrecord : mergedSubrecordList)
		{
			String subrecordType = subrecord.getSubrecordType();
			if (subrecordType.equals("NAM1"))
			{
				if (foundList)
					break;
				throw new PluginException("Face data does not precede body data");
			}

			if (!foundList)
			{
				if (subrecordType.equals("NAM0"))
					foundList = true;
			}
			else if (subrecordType.equals("INDX"))
			{
				count++;
			}
		}

		PluginSubrecord[] mindx = new PluginSubrecord[count];
		PluginSubrecord[] mmodl = new PluginSubrecord[count];
		PluginSubrecord[] mmodb = new PluginSubrecord[count];
		PluginSubrecord[] micon = new PluginSubrecord[count];
		index = -1;
		foundList = false;
		for (PluginSubrecord subrecord : mergedSubrecordList)
		{
			String subrecordType = subrecord.getSubrecordType();
			if (!foundList)
			{
				if (subrecordType.equals("NAM0"))
					foundList = true;
			}
			else if (subrecordType.equals("INDX"))
			{
				index++;
				mindx[index] = subrecord;
			}
			else if (subrecordType.equals("MODL"))
			{
				mmodl[index] = subrecord;
			}
			else if (subrecordType.equals("MODB"))
			{
				mmodb[index] = subrecord;
			}
			else
			{
				if (!subrecordType.equals("ICON"))
					break;
				micon[index] = subrecord;
			}

		}

		foundList = false;
		count = 0;
		for (PluginSubrecord subrecord : masterSubrecordList)
		{
			String subrecordType = subrecord.getSubrecordType();
			if (subrecordType.equals("NAM1"))
			{
				if (foundList)
					break;
				throw new PluginException("Face data does not precede body data");
			}

			if (!foundList)
			{
				if (subrecordType.equals("NAM0"))
					foundList = true;
			}
			else if (subrecordType.equals("INDX"))
			{
				count++;
			}
		}

		PluginSubrecord[] xindx = new PluginSubrecord[count];
		PluginSubrecord[] xmodl = new PluginSubrecord[count];
		PluginSubrecord[] xmodb = new PluginSubrecord[count];
		PluginSubrecord[] xicon = new PluginSubrecord[count];
		index = -1;
		foundList = false;
		for (PluginSubrecord subrecord : masterSubrecordList)
		{
			String subrecordType = subrecord.getSubrecordType();
			if (!foundList)
			{
				if (subrecordType.equals("NAM0"))
					foundList = true;
			}
			else if (subrecordType.equals("INDX"))
			{
				index++;
				xindx[index] = subrecord;
			}
			else if (subrecordType.equals("MODL"))
			{
				xmodl[index] = subrecord;
			}
			else if (subrecordType.equals("MODB"))
			{
				xmodb[index] = subrecord;
			}
			else
			{
				if (!subrecordType.equals("ICON"))
					break;
				xicon[index] = subrecord;
			}

		}

		if ((indx.length != mindx.length) || (indx.length != xindx.length))
		{
			throw new PluginException("Face data maximum index values are not the same");
		}

		boolean rebuildMergedData = false;
		for (int i = 0; i < indx.length; i++)
		{
			if ((!indx[i].equals(mindx[i])) || (!indx[i].equals(xindx[i])))
			{
				throw new PluginException("Incorrect face data index progression");
			}
			if (modl[i] != null)
			{
				if (mmodl[i] != null)
				{
					if (!modl[i].equals(mmodl[i]))
						if (xmodl[i] != null)
						{
							if (!modl[i].equals(xmodl[i]))
								if (!mmodl[i].equals(xmodl[i]))
								{
									if ((this.masterMerge) || (!this.pluginInfo.shouldDeleteLastConflict()))
									{
										mmodl[i] = modl[i];
										rebuildMergedData = true;
									}
								}
								else
								{
									mmodl[i] = modl[i];
									rebuildMergedData = true;
								}
						}
						else if ((this.masterMerge) || (!this.pluginInfo.shouldDeleteLastConflict()))
						{
							mmodl[i] = modl[i];
							rebuildMergedData = true;
						}
				}
				else
				{
					mmodl[i] = modl[i];
					rebuildMergedData = true;
				}
			}

			if (icon[i] != null)
			{
				if (micon[i] != null)
				{
					if (!icon[i].equals(micon[i]))
						if (xicon[i] != null)
						{
							if (!icon[i].equals(xicon[i]))
								if (!micon[i].equals(xicon[i]))
								{
									if ((this.masterMerge) || (!this.pluginInfo.shouldDeleteLastConflict()))
									{
										micon[i] = icon[i];
										rebuildMergedData = true;
									}
								}
								else
								{
									micon[i] = icon[i];
									rebuildMergedData = true;
								}
						}
						else if ((this.masterMerge) || (!this.pluginInfo.shouldDeleteLastConflict()))
						{
							micon[i] = icon[i];
							rebuildMergedData = true;
						}
				}
				else
				{
					micon[i] = icon[i];
					rebuildMergedData = true;
				}

			}

		}

		if (rebuildMergedData)
		{
			modifiedMergedList = true;
			index = 0;
			foundList = false;
			ListIterator<PluginSubrecord> lit = mergedSubrecordList.listIterator();
			while (lit.hasNext())
			{
				PluginSubrecord subrecord = lit.next();
				String subrecordType = subrecord.getSubrecordType();
				if (foundList)
				{
					if ((!subrecordType.equals("INDX")) && (!subrecordType.equals("MODL")) && (!subrecordType.equals("MODB"))
							&& (!subrecordType.equals("ICON")))
						break;
					lit.remove();
				}
				else if (subrecordType.equals("NAM0"))
				{
					foundList = true;
					lit.remove();
				}
				else
				{
					index++;
				}
			}

			for (int i = mindx.length - 1; i >= 0; i--)
			{
				if (micon[i] != null)
				{
					mergedSubrecordList.add(index, micon[i]);
				}
				if (mmodb[i] != null)
				{
					mergedSubrecordList.add(index, mmodb[i]);
				}
				if (mmodl[i] != null)
				{
					mergedSubrecordList.add(index, mmodl[i]);
				}
				mergedSubrecordList.add(index, mindx[i]);
			}

			byte[] subrecordData = new byte[0];
			mergedSubrecordList.add(index, new PluginSubrecord("RACE", "NAM0", subrecordData));
		}

		return modifiedMergedList;
	}

	private boolean mergeBodyData(List<PluginSubrecord> subrecordList, List<PluginSubrecord> mergedSubrecordList,
			List<PluginSubrecord> masterSubrecordList) throws PluginException
	{
		boolean modifiedMergedList = false;

		boolean foundList = false;
		boolean foundFace = false;
		int count = 0;

		for (PluginSubrecord subrecord : subrecordList)
		{
			String subrecordType = subrecord.getSubrecordType();
			if (!foundList)
			{
				if (subrecordType.equals("NAM0"))
				{
					foundFace = true;
				}
				else if (subrecordType.equals("NAM1"))
				{
					if (!foundFace)
					{
						throw new PluginException("Face data does not precede body data");
					}
					foundList = true;
				}
			}
			else if (subrecordType.equals("INDX"))
			{
				count++;
			}
		}

		PluginSubrecord[] indx = new PluginSubrecord[count];
		PluginSubrecord[] icon = new PluginSubrecord[count];
		PluginSubrecord[] modl = new PluginSubrecord[2];
		PluginSubrecord[] modb = new PluginSubrecord[2];
		int index = -1;
		int femaleIndex = -1;
		boolean foundFemale = false;
		foundList = false;

		for (PluginSubrecord subrecord : subrecordList)
		{
			String subrecordType = subrecord.getSubrecordType();
			if (!foundList)
			{
				if (subrecordType.equals("NAM1"))
					foundList = true;
			}
			else if (subrecordType.equals("INDX"))
			{
				index++;
				indx[index] = subrecord;
			}
			else if (subrecordType.equals("ICON"))
			{
				icon[index] = subrecord;
			}
			else if (subrecordType.equals("MNAM"))
			{
				if (foundFemale)
					throw new PluginException("Female body data does not follow male body data");
			}
			else if (subrecordType.equals("FNAM"))
			{
				foundFemale = true;
				femaleIndex = index + 1;
			}
			else if (subrecordType.equals("MODL"))
			{
				if (!foundFemale)
					modl[0] = subrecord;
				else
					modl[1] = subrecord;
			}
			else
			{
				if (!subrecordType.equals("MODB"))
					break;
				if (!foundFemale)
					modb[0] = subrecord;
				else
				{
					modb[1] = subrecord;
				}
			}

		}

		if (!foundFemale)
		{
			throw new PluginException("No female body data");
		}

		foundList = false;
		foundFace = false;
		count = 0;

		for (PluginSubrecord subrecord : mergedSubrecordList)
		{
			String subrecordType = subrecord.getSubrecordType();
			if (!foundList)
			{
				if (subrecordType.equals("NAM0"))
				{
					foundFace = true;
				}
				else if (subrecordType.equals("NAM1"))
				{
					if (!foundFace)
					{
						throw new PluginException("Face data does not precede body data");
					}
					foundList = true;
				}
			}
			else if (subrecordType.equals("INDX"))
			{
				count++;
			}
		}

		PluginSubrecord[] mindx = new PluginSubrecord[count];
		PluginSubrecord[] micon = new PluginSubrecord[count];
		PluginSubrecord[] mmodl = new PluginSubrecord[2];
		PluginSubrecord[] mmodb = new PluginSubrecord[2];
		index = -1;
		foundList = false;
		foundFemale = false;

		for (PluginSubrecord subrecord : mergedSubrecordList)
		{
			String subrecordType = subrecord.getSubrecordType();
			if (!foundList)
			{
				if (subrecordType.equals("NAM1"))
					foundList = true;
			}
			else if (subrecordType.equals("INDX"))
			{
				index++;
				mindx[index] = subrecord;
			}
			else if (subrecordType.equals("ICON"))
			{
				micon[index] = subrecord;
			}
			else if (subrecordType.equals("MNAM"))
			{
				if (foundFemale)
					throw new PluginException("Female body data does not follow male body data");
			}
			else if (subrecordType.equals("FNAM"))
			{
				foundFemale = true;
			}
			else if (subrecordType.equals("MODL"))
			{
				if (!foundFemale)
					mmodl[0] = subrecord;
				else
					mmodl[1] = subrecord;
			}
			else
			{
				if (!subrecordType.equals("MODB"))
					break;
				if (!foundFemale)
					mmodb[0] = subrecord;
				else
				{
					mmodb[1] = subrecord;
				}

			}

		}

		foundList = false;
		foundFace = false;
		count = 0;

		for (PluginSubrecord subrecord : masterSubrecordList)
		{
			String subrecordType = subrecord.getSubrecordType();
			if (!foundList)
			{
				if (subrecordType.equals("NAM0"))
				{
					foundFace = true;
				}
				else if (subrecordType.equals("NAM1"))
				{
					if (!foundFace)
					{
						throw new PluginException("Face data does not precede body data");
					}
					foundList = true;
				}
			}
			else if (subrecordType.equals("INDX"))
			{
				count++;
			}
		}

		PluginSubrecord[] xindx = new PluginSubrecord[count];
		PluginSubrecord[] xicon = new PluginSubrecord[count];
		PluginSubrecord[] xmodl = new PluginSubrecord[2];
		PluginSubrecord[] xmodb = new PluginSubrecord[2];
		index = -1;
		foundList = false;
		foundFemale = false;

		for (PluginSubrecord subrecord : masterSubrecordList)
		{
			String subrecordType = subrecord.getSubrecordType();
			if (!foundList)
			{
				if (subrecordType.equals("NAM1"))
					foundList = true;
			}
			else if (subrecordType.equals("INDX"))
			{
				index++;
				xindx[index] = subrecord;
			}
			else if (subrecordType.equals("ICON"))
			{
				xicon[index] = subrecord;
			}
			else if (subrecordType.equals("MNAM"))
			{
				if (foundFemale)
					throw new PluginException("Female body data does not follow male body data");
			}
			else if (subrecordType.equals("FNAM"))
			{
				foundFemale = true;
			}
			else if (subrecordType.equals("MODL"))
			{
				if (!foundFemale)
					xmodl[0] = subrecord;
				else
					xmodl[1] = subrecord;
			}
			else
			{
				if (!subrecordType.equals("MODB"))
					break;
				if (!foundFemale)
					xmodb[0] = subrecord;
				else
				{
					xmodb[1] = subrecord;
				}

			}

		}

		if ((indx.length != mindx.length) || (indx.length != xindx.length))
		{
			throw new PluginException("Body data maximum index values are not the same");
		}

		boolean rebuildMergedData = false;

		for (int i = 0; i < indx.length; i++)
		{
			if ((!indx[i].equals(mindx[i])) || (!indx[i].equals(xindx[i])))
			{
				throw new PluginException("Incorrect body data index progression");
			}
			if (icon[i] != null)
			{
				if (micon[i] != null)
				{
					if (!icon[i].equals(micon[i]))
						if (xicon[i] != null)
						{
							if (!icon[i].equals(xicon[i]))
								if (!micon[i].equals(xicon[i]))
								{
									if ((this.masterMerge) || (!this.pluginInfo.shouldDeleteLastConflict()))
									{
										micon[i] = icon[i];
										rebuildMergedData = true;
									}
								}
								else
								{
									micon[i] = icon[i];
									rebuildMergedData = true;
								}
						}
						else if ((this.masterMerge) || (!this.pluginInfo.shouldDeleteLastConflict()))
						{
							micon[i] = icon[i];
							rebuildMergedData = true;
						}
				}
				else
				{
					micon[i] = icon[i];
					rebuildMergedData = true;
				}
			}
		}

		for (int i = 0; i < 2; i++)
		{
			if (modl[i] != null)
			{
				if (mmodl[i] != null)
				{
					if (xmodl[i] != null)
					{
						if (!modl[i].equals(xmodl[i]))
							if (!mmodl[i].equals(xmodl[i]))
							{
								if ((this.masterMerge) || (!this.pluginInfo.shouldDeleteLastConflict()))
								{
									mmodl[i] = modl[i];
									mmodb[i] = modb[i];
									rebuildMergedData = true;
								}
							}
							else
							{
								mmodl[i] = modl[i];
								mmodb[i] = modb[i];
								rebuildMergedData = true;
							}
					}
					else if ((this.masterMerge) || (!this.pluginInfo.shouldDeleteLastConflict()))
					{
						mmodl[i] = modl[i];
						mmodb[i] = modb[i];
						rebuildMergedData = true;
					}
				}
				else
				{
					mmodl[i] = modl[i];
					mmodb[i] = modb[i];
					rebuildMergedData = true;
				}

			}

		}

		if (rebuildMergedData)
		{
			modifiedMergedList = true;
			index = 0;
			foundList = false;
			ListIterator<PluginSubrecord> lit = mergedSubrecordList.listIterator();
			while (lit.hasNext())
			{
				PluginSubrecord subrecord = lit.next();
				String subrecordType = subrecord.getSubrecordType();
				if (foundList)
				{
					if ((!subrecordType.equals("INDX")) && (!subrecordType.equals("MNAM")) && (!subrecordType.equals("FNAM"))
							&& (!subrecordType.equals("MODL")) && (!subrecordType.equals("MODB")) && (!subrecordType.equals("ICON")))
						break;
					lit.remove();
				}
				else if (subrecordType.equals("NAM1"))
				{
					foundList = true;
					lit.remove();
				}
				else
				{
					index++;
				}
			}

			byte[] subrecordData = new byte[0];
			for (int i = mindx.length - 1; i >= 0; i--)
			{
				if (micon[i] != null)
				{
					mergedSubrecordList.add(index, micon[i]);
				}
				mergedSubrecordList.add(index, mindx[i]);
				if (i == femaleIndex)
				{
					if (mmodb[1] != null)
					{
						mergedSubrecordList.add(index, mmodb[1]);
					}
					if (mmodl[1] != null)
					{
						mergedSubrecordList.add(index, mmodl[1]);
					}
					mergedSubrecordList.add(index, new PluginSubrecord("RACE", "FNAM", subrecordData));
				}
			}

			if (mmodb[0] != null)
			{
				mergedSubrecordList.add(index, mmodb[0]);
			}
			if (mmodl[0] != null)
			{
				mergedSubrecordList.add(index, mmodl[0]);
			}
			mergedSubrecordList.add(index, new PluginSubrecord("RACE", "MNAM", subrecordData));
			mergedSubrecordList.add(index, new PluginSubrecord("RACE", "NAM1", subrecordData));
		}

		return modifiedMergedList;
	}

	private void copyVoiceFiles(Plugin plugin, File voiceDirectory, String mergedPath) throws DataFormatException, IOException,
			PluginException
	{
		File[] files = voiceDirectory.listFiles();
		List<?> formList = plugin.getFormList();
		Map<?, ?> formMap = plugin.getFormMap();
		int masterID = plugin.getMasterList().size();

		for (File file : files)
			if (file.isDirectory())
			{
				String path = mergedPath + Main.fileSeparator + file.getName();
				copyVoiceFiles(plugin, file, path);
			}
			else
			{
				String name = file.getName();
				int questSep = name.indexOf('_');
				if (questSep < 1)
				{
					continue;
				}
				int topicSep = name.indexOf('_', questSep + 1);
				if (topicSep < questSep + 2)
				{
					continue;
				}
				int infoSep = name.indexOf('_', topicSep + 1);
				if (infoSep < topicSep + 2)
					continue;
				int infoID;
				try
				{
					infoID = Integer.parseInt(name.substring(topicSep + 1, infoSep), 16);
				}
				catch (NumberFormatException exc)
				{

					infoID = 0;
				}

				if (infoID == 0)
				{
					continue;
				}

				infoID |= masterID << 24;
				FormInfo infoForm = (FormInfo) formMap.get(new Integer(infoID));
				if (infoForm == null)
				{
					continue;
				}
				PluginRecord infoRecord = (PluginRecord) infoForm.getSource();

				int topicID = infoForm.getParentFormID();
				if (topicID == 0)
				{
					continue;
				}
				FormInfo topicForm = (FormInfo) formMap.get(new Integer(topicID));
				if (topicForm == null)
				{
					continue;
				}
				String topicName = topicForm.getMergedEditorID();

				int questID = 0;
				List<PluginSubrecord> subrecordList = infoRecord.getSubrecords();
				for (PluginSubrecord subrecord : subrecordList)
				{
					if (subrecord.getSubrecordType().equals("QSTI"))
					{
						byte[] subrecordData = subrecord.getSubrecordData();
						questID = SerializedElement.getInteger(subrecordData, 0);
						break;
					}
				}

				if (questID == 0)
				{
					continue;
				}
				FormInfo questForm = (FormInfo) formMap.get(new Integer(questID));
				String questName;

				if (questForm == null)
					questName = name.substring(0, questSep);
				else
				{
					questName = questForm.getMergedEditorID();
				}

				File mergedDirectory = new File(mergedPath);
				if (!mergedDirectory.exists())
				{
					mergedDirectory.mkdirs();
				}
				String mergedName = String.format("%s%s%s_%s_%08X_%s",
						new Object[]
						{ mergedPath, Main.fileSeparator, questName, topicName, Integer.valueOf(infoForm.getMergedFormID() & 0xFFFFFF),
								name.substring(infoSep + 1) });
				File mergedFile = new File(mergedName);
				FileInputStream in = new FileInputStream(file);
				FileOutputStream out = new FileOutputStream(mergedFile);
				int length = (int) file.length();
				byte[] buffer = new byte[length];
				in.read(buffer);
				out.write(buffer);
				out.close();
				in.close();
			}
	}

	private void deleteDirectoryTree(File directory)
	{
		File[] files = directory.listFiles();
		for (File file : files)
		{
			if (file.isDirectory())
				deleteDirectoryTree(file);
			else
			{
				file.delete();
			}

		}

		directory.delete();
	}

	public void buildDeletedINFOMap(Plugin plugin, Map<Integer, List<PluginRecord>> deletedMap)
	{
		PluginGroup dialogueGroup = plugin.getTopGroup("DIAL");
		if ((dialogueGroup == null) || (deletedMap == null))
			return;
		List<PluginRecord> groupList = dialogueGroup.getRecordList();
		ListIterator<?> lit2;
		for (PluginRecord dialOrInfo : groupList)
		{
			if (!(dialOrInfo instanceof PluginGroup))
				continue;
			int topicID = ((PluginGroup) dialOrInfo).getGroupParentID();
			if (deletedMap.containsKey(Integer.valueOf(topicID)))
			{
				List<?> infoList = ((PluginGroup) dialOrInfo).getRecordList();
				List<PluginRecord> deletedList = deletedMap.get(Integer.valueOf(topicID));
				ListIterator<PluginRecord> lit1 = deletedList.listIterator();
				while (lit1.hasNext())
				{
					PluginRecord delRec = lit1.next();
					lit2 = infoList.listIterator();
					while (lit2.hasNext())
					{
						PluginRecord rec = (PluginRecord) lit2.next();
						if ((rec.isDeleted()) || (rec.getFormID() != delRec.getFormID()))
							continue;
						lit1.set(rec);
						break;
					}
				}
			}
		}

		for (PluginRecord dialOrInfo : groupList)
		{
			if (!(dialOrInfo instanceof PluginGroup))
				continue;
			PluginGroup topicGroup = (PluginGroup) dialOrInfo;
			int topicID = topicGroup.getGroupParentID();
			List<PluginRecord> newDelList = topicGroup.getDeletedPluginRecords();
			if ((!newDelList.isEmpty()) && (!deletedMap.containsKey(Integer.valueOf(topicID))))
			{
				deletedMap.put(Integer.valueOf(topicID), newDelList);
			}
			else
			{
				List<PluginRecord> deletedList = deletedMap.get(Integer.valueOf(topicID));
				for (PluginRecord delRec : newDelList)
				{
					ListIterator<PluginRecord> lit = deletedList.listIterator();
					boolean alreadyPresent = false;
					while (lit.hasNext())
					{
						PluginRecord rec = lit.next();
						if (rec.getFormID() == delRec.getFormID())
						{
							alreadyPresent = true;
							break;
						}
						if (alreadyPresent)
							continue;
						lit.add(delRec);
					}
				}
			}
		}
	}

	private int getStartFormID(int baseID)
	{
		int retVal = -1;
		String inputID = (String) JOptionPane.showInputDialog(parentWindow,
				"<html>Please enter the starting form ID <i>in hex</i>\n(Numbering will start at this number plus one):",
				"New Starting Form ID", -1, null, null, String.format("%08X", new Object[]
				{ Integer.valueOf(baseID) }));
		if (inputID == null)
			return retVal;
		try
		{
			retVal = Integer.parseInt(inputID, 16);
		}
		catch (Exception ex)
		{
			JOptionPane.showMessageDialog(parentWindow, "Value entered: \"" + inputID + "\" is not a valid number.", "Entry Error", 0);
			return -1;
		}
		if (retVal < baseID)
		{
			JOptionPane.showMessageDialog(parentWindow, "Number entered: \"" + String.format("%08X", new Object[]
			{ Integer.valueOf(retVal) }) + "\" is too small.", "Entry Error", 0);
			return -1;
		}
		return retVal;
	}

	private int highestFormID(Plugin pl)
	{
		List<FormInfo> allForms = pl.getFormList();
		int highFormID = allForms.get(0).getFormID();
		for (FormInfo form : allForms)
		{
			int formID = form.getFormID();
			if (formID <= highFormID)
				continue;
			highFormID = formID;
		}
		return highFormID;
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.MergeTask
 * JD-Core Version:    0.6.0
 */