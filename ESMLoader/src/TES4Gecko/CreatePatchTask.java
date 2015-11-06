package TES4Gecko;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class CreatePatchTask extends WorkerTask
{
	private File baseFile;

	private FormAdjust baseFormAdjust;

	private Plugin basePlugin;

	private File modifiedFile;

	private FormAdjust modifiedFormAdjust;

	private Plugin modifiedPlugin;

	private File patchFile;

	private Plugin patchPlugin;

	public CreatePatchTask(StatusDialog statusDialog, File baseFile, File modifiedFile, File patchFile)
	{
		super(statusDialog);
		this.baseFile = baseFile;
		this.modifiedFile = modifiedFile;
		this.patchFile = patchFile;
	}

	public static void createPatch(JFrame parent, File baseFile, File modifiedFile, File patchFile)
	{
		StatusDialog statusDialog = new StatusDialog(parent, "Creating patch", "Create Patch");

		CreatePatchTask worker = new CreatePatchTask(statusDialog, baseFile, modifiedFile, patchFile);
		statusDialog.setWorker(worker);

		worker.start();
		statusDialog.showDialog();

		if (statusDialog.getStatus() == 1)
			JOptionPane.showMessageDialog(parent, "Patch created for " + baseFile.getName(), "Create Patch", 1);
		else
			JOptionPane.showMessageDialog(parent, "Unable to create patch for " + baseFile.getName(), "Create Patch", 1);
	}

	public void run()
	{
		boolean completed = false;
		try
		{
			this.basePlugin = new Plugin(this.baseFile);
			this.basePlugin.load(this);
			List<FormInfo> baseList = this.basePlugin.getFormList();
			Map<Integer, FormInfo> baseMap = this.basePlugin.getFormMap();
			List<String> baseMasterList = this.basePlugin.getMasterList();
			this.baseFormAdjust = new FormAdjust();

			this.modifiedPlugin = new Plugin(this.modifiedFile);
			this.modifiedPlugin.load(this);
			List<FormInfo> modifiedList = this.modifiedPlugin.getFormList();
			Map<Integer, FormInfo> modifiedMap = this.modifiedPlugin.getFormMap();
			List<String> modifiedMasterList = this.modifiedPlugin.getMasterList();
			this.modifiedFormAdjust = new FormAdjust();

			int masterCount = baseMasterList.size();
			if (masterCount != modifiedMasterList.size())
			{
				throw new PluginException("The master list is not the same for both plugins");
			}
			for (int i = 0; i < masterCount; i++)
			{
				if (!baseMasterList.get(i).equals(modifiedMasterList.get(i)))
				{
					throw new PluginException("The master list is not the same for both plugins");
				}

			}

			getStatusDialog().updateMessage("Creating patch for " + this.baseFile.getName());
			this.patchPlugin = new Plugin(this.patchFile, this.modifiedPlugin.getCreator(), this.modifiedPlugin.getSummary(),
					modifiedMasterList);
			this.patchPlugin.setVersion(Math.max(this.basePlugin.getVersion(), this.modifiedPlugin.getVersion()));
			this.patchPlugin.createInitialGroups();

			int formCount = modifiedList.size();
			int processedCount = 0;
			int currentProgress = 0;
			for (FormInfo formInfo : modifiedList)
			{
				PluginRecord record = (PluginRecord) formInfo.getSource();
				if (record != null)
				{
					int formID = record.getFormID();
					FormInfo baseInfo = baseMap.get(new Integer(formID));
					if (baseInfo == null)
					{
						this.patchPlugin.copyRecord(record, this.modifiedFormAdjust);
					}
					else
					{
						PluginRecord baseRecord = (PluginRecord) baseInfo.getSource();
						if (!baseRecord.isIdentical(record))
						{
							this.patchPlugin.copyRecord(record, this.modifiedFormAdjust);
						}
					}
				}
				processedCount++;
				int newProgress = processedCount * 50 / formCount;
				if (newProgress >= currentProgress + 5)
				{
					currentProgress = newProgress;
					getStatusDialog().updateProgress(currentProgress);
				}

			}

			formCount = baseList.size();
			processedCount = 0;
			for (FormInfo formInfo : baseList)
			{
				PluginRecord record = (PluginRecord) formInfo.getSource();
				if (record != null)
				{
					int formID = record.getFormID();
					FormInfo modifiedInfo = modifiedMap.get(new Integer(formID));
					if (modifiedInfo == null)
					{
						PluginGroup patchGroup = this.patchPlugin.createHierarchy(record, this.baseFormAdjust);
						int deletedFormID = this.baseFormAdjust.adjustFormID(formID);
						String recordType = record.getRecordType();
						String editorID = record.getEditorID();
						PluginRecord deletedRecord = new PluginRecord(recordType, deletedFormID);
						deletedRecord.setDelete(true);
						deletedRecord.setParent(patchGroup);
						patchGroup.getRecordList().add(deletedRecord);
						FormInfo deletedFormInfo = new FormInfo(deletedRecord, recordType, deletedFormID, editorID);
						deletedFormInfo.setParentFormID(patchGroup.getGroupParentID());
						this.patchPlugin.getFormList().add(deletedFormInfo);
						this.patchPlugin.getFormMap().put(new Integer(deletedFormID), deletedFormInfo);
						if (Main.debugMode)
						{
							System.out.printf("%s: Deleted %s record %s (%08X)\n", new Object[]
							{ this.patchFile.getName(), editorID, recordType, Integer.valueOf(deletedFormID) });
						}
					}
					processedCount++;
					int newProgress = processedCount * 50 / formCount + 50;
					if (newProgress >= currentProgress + 5)
					{
						currentProgress = newProgress;
						getStatusDialog().updateProgress(currentProgress);
					}

				}

			}

			this.patchPlugin.store(this);
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
			Main.logException("Exception while creating patch", exc);
		}

		getStatusDialog().closeDialog(completed);
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.CreatePatchTask
 * JD-Core Version:    0.6.0
 */