package TES4Gecko;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class ApplyPatchTask extends WorkerTask
{
	private File pluginFile;

	private Plugin plugin;

	private Map<Integer, FormInfo> pluginMap;

	private List<String> pluginMasterList;

	private int pluginMasterCount;

	private File patchFile;

	private Plugin patch;

	private List<FormInfo> patchList;

	private List<String> patchMasterList;

	private int patchMasterCount;

	private FormAdjust patchFormAdjust;

	public ApplyPatchTask(StatusDialog statusDialog, File pluginFile, File patchFile)
	{
		super(statusDialog);
		this.pluginFile = pluginFile;
		this.patchFile = patchFile;
	}

	public static void applyPatch(JFrame parent, File pluginFile, File patchFile)
	{
		StatusDialog statusDialog = new StatusDialog(parent, "Applying patch", "Apply Patch");

		ApplyPatchTask worker = new ApplyPatchTask(statusDialog, pluginFile, patchFile);
		statusDialog.setWorker(worker);

		worker.start();
		statusDialog.showDialog();

		if (statusDialog.getStatus() == 1)
			JOptionPane.showMessageDialog(parent, "Patch applied to " + pluginFile.getName(), "Apply Patch", 1);
		else
			JOptionPane.showMessageDialog(parent, "Unable to apply patch to " + pluginFile.getName(), "Apply Patch", 1);
	}

	public void run()
	{
		boolean completed = false;
		try
		{
			this.plugin = new Plugin(this.pluginFile);
			this.plugin.load(this);
			this.pluginMap = this.plugin.getFormMap();
			this.pluginMasterList = this.plugin.getMasterList();
			this.pluginMasterCount = this.pluginMasterList.size();

			this.patch = new Plugin(this.patchFile);
			this.patch.load(this);
			this.patchList = this.patch.getFormList();
			this.patchMasterList = this.patch.getMasterList();
			this.patchMasterCount = this.patchMasterList.size();

			if (this.patchMasterCount != this.pluginMasterCount)
			{
				throw new PluginException("The plugin master list does not match the patch master list");
			}
			for (int i = 0; i < this.patchMasterCount; i++)
			{
				if (!this.pluginMasterList.get(i).equals(this.patchMasterList.get(i)))
				{
					throw new PluginException("The plugin master list does not match the patch master list");
				}

			}

			this.patchFormAdjust = new FormAdjust();

			this.plugin.setCreator(this.patch.getCreator());
			this.plugin.setSummary(this.patch.getSummary());

			this.plugin.setVersion(Math.max(this.plugin.getVersion(), this.patch.getVersion()));

			int patchCount = this.patchList.size();
			int processedCount = 0;
			int currentProgress = 0;
			getStatusDialog().updateMessage("Applying patch to " + this.pluginFile.getName());

			for (FormInfo patchInfo : this.patchList)
			{
				PluginRecord patchRecord = (PluginRecord) patchInfo.getSource();
				int formID = patchRecord.getFormID();
				Integer mapFormID = new Integer(formID);
				FormInfo pluginInfo = this.pluginMap.get(mapFormID);
				if (pluginInfo == null)
				{
					this.plugin.copyRecord(patchRecord, this.patchFormAdjust);
					if (Main.debugMode)
						System.out.printf("Added %s record %s (%08X)\n", new Object[]
						{ patchRecord.getRecordType(), patchRecord.getEditorID(), Integer.valueOf(formID) });
				}
				else
				{
					PluginRecord pluginRecord = (PluginRecord) pluginInfo.getSource();
					String recordType = pluginRecord.getRecordType();
					PluginGroup parentGroup = (PluginGroup) pluginRecord.getParent();
					List<PluginRecord> recordList = parentGroup.getRecordList();
					int index = recordList.indexOf(pluginRecord);
					if (index >= 0)
					{
						if (patchRecord.isDeleted())
						{
							this.plugin.removeRecord(pluginRecord);
							if (Main.debugMode)
							{
								System.out.printf("Deleted %s record %s (%08X)\n", new Object[]
								{ recordType, pluginRecord.getEditorID(), Integer.valueOf(formID) });
							}

						}
						else
						{
							pluginRecord = (PluginRecord) patchRecord.clone();
							pluginRecord.setParent(parentGroup);
							recordList.set(index, pluginRecord);
							pluginInfo.setSource(pluginRecord);
							if (Main.debugMode)
							{
								System.out.printf("Updated %s record %s (%08X)\n", new Object[]
								{ recordType, pluginRecord.getEditorID(), Integer.valueOf(formID) });
							}
						}

					}

				}

				processedCount++;
				int newProgress = processedCount * 100 / patchCount;
				if (newProgress >= currentProgress + 5)
				{
					currentProgress = newProgress;
					getStatusDialog().updateProgress(currentProgress);
				}

			}

			this.plugin.store(this);
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
			Main.logException("Exception while applying patch", exc);
		}

		getStatusDialog().closeDialog(completed);
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.ApplyPatchTask
 * JD-Core Version:    0.6.0
 */