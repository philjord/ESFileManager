package TES4Gecko;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class CleanTask extends WorkerTask
{
	private File pluginFile;

	private Plugin plugin;

	private List<PluginGroup> pluginGroupList;

	private List<String> pluginMasterList;

	private Plugin master;

	private Map<Integer, FormInfo> masterFormMap;

	private int masterCount;

	private int masterIndex;

	private boolean pluginModified = false;

	public CleanTask(StatusDialog statusDialog, File pluginFile)
	{
		super(statusDialog);
		this.pluginFile = pluginFile;
	}

	public static void cleanPlugin(JFrame parent, File pluginFile)
	{
		StatusDialog statusDialog = new StatusDialog(parent, "Cleaning " + pluginFile.getName(), "Clean Plugin");

		CleanTask worker = new CleanTask(statusDialog, pluginFile);
		statusDialog.setWorker(worker);

		worker.start();
		statusDialog.showDialog();

		if (statusDialog.getStatus() == 1)
			JOptionPane.showMessageDialog(parent, pluginFile.getName() + " cleaned", "Clean Plugin", 1);
		else
			JOptionPane.showMessageDialog(parent, "Unable to clean " + pluginFile.getName(), "Clean Plugin", 1);
	}

	public void run()
	{
		boolean completed = false;
		try
		{
			this.plugin = new Plugin(this.pluginFile);
			this.plugin.load(this);
			this.pluginGroupList = this.plugin.getGroupList();
			this.pluginMasterList = this.plugin.getMasterList();

			int pluginMasterCount = this.pluginMasterList.size();
			for (this.masterIndex = 0; this.masterIndex < pluginMasterCount; this.masterIndex += 1)
			{
				String masterName = this.pluginMasterList.get(this.masterIndex);

				File masterFile = new File(Main.pluginDirectory + Main.fileSeparator + masterName);
				this.master = new Plugin(masterFile);
				this.master.load(this);
				this.masterFormMap = this.master.getFormMap();
				this.masterCount = this.master.getMasterList().size();

				getStatusDialog().updateMessage("Comparing '" + this.plugin.getName() + "' to '" + masterName + "'");
				int processedCount = 0;
				int currentProgress = 0;

				for (PluginGroup pluginGroup : this.pluginGroupList)
				{
					compareGroup(pluginGroup);
					processedCount++;
					int newProgress = processedCount * 100 / this.pluginGroupList.size();
					if (newProgress >= currentProgress + 5)
					{
						currentProgress = newProgress;
						getStatusDialog().updateProgress(currentProgress);
					}

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
			Main.logException("Exception while cleaning plugin", exc);
		}

		getStatusDialog().closeDialog(completed);
	}

	private void compareGroup(PluginGroup pluginGroup)
	{
		List<PluginRecord> recordList = pluginGroup.getRecordList();

		for (PluginRecord pluginRecord : recordList)
		{
			if ((pluginRecord instanceof PluginGroup))
			{
				compareGroup((PluginGroup) pluginRecord);
			}

		}

		int recordCount = recordList.size();
		for (int recordIndex = 0; recordIndex < recordCount; recordIndex++)
		{
			PluginRecord pluginRecord = recordList.get(recordIndex);
			if ((pluginRecord instanceof PluginGroup))
			{
				continue;
			}
			String recordType = pluginRecord.getRecordType();
			int formID = pluginRecord.getFormID();
			int modIndex = formID >>> 24;

			if ((modIndex == this.masterIndex) && (!pluginRecord.isDeleted()))
			{
				int masterFormID = formID & 0xFFFFFF | this.masterCount << 24;
				Integer formIndex = new Integer(masterFormID);
				FormInfo masterFormInfo = this.masterFormMap.get(formIndex);
				if (masterFormInfo == null)
				{
					if (Main.debugMode)
						System.out.printf("%s: Record %08X not found\n", new Object[]
						{ this.master.getName(), Integer.valueOf(masterFormID) });
				}
				else
				{
					PluginRecord masterRecord = (PluginRecord) masterFormInfo.getSource();
					if (pluginRecord.isIdentical(masterRecord))
					{
						boolean ignoreRecord = true;

						if ((recordType.equals("WRLD")) || (recordType.equals("CELL")) || (recordType.equals("DIAL")))
						{
							int groupIndex = recordIndex + 1;
							if (groupIndex < recordCount)
							{
								PluginRecord cmpRecord = recordList.get(groupIndex);
								if ((cmpRecord instanceof PluginGroup))
								{
									PluginGroup cmpGroup = (PluginGroup) cmpRecord;
									if (cmpGroup.getGroupParentID() == pluginRecord.getFormID())
									{
										cmpGroup.removeIgnoredRecords();
										if (!cmpGroup.isEmpty())
										{
											ignoreRecord = false;
											if (Main.debugMode)
											{
												System.out.printf("Keeping %s record %s (%08X)\n", new Object[]
												{ recordType, pluginRecord.getEditorID(), Integer.valueOf(pluginRecord.getFormID()) });
											}
										}
									}
								}

							}

						}

						if (ignoreRecord)
						{
							pluginRecord.setIgnore(true);
							this.pluginModified = true;
							if (Main.debugMode)
								System.out.printf("Ignoring %s record %s (%08X)\n", new Object[]
								{ recordType, pluginRecord.getEditorID(), Integer.valueOf(pluginRecord.getFormID()) });
						}
					}
				}
			}
		}
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.CleanTask
 * JD-Core Version:    0.6.0
 */