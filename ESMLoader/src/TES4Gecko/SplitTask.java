package TES4Gecko;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

public class SplitTask extends WorkerTask
{
	private PluginNode pluginNode;

	private File pluginFile;

	private Plugin plugin;

	private List<FormInfo> formList;

	private Map<Integer, FormInfo> formMap;

	private int masterCount;

	private Master[] masters;

	private boolean independentMaster;

	private PluginNode outputMasterNode;

	private Plugin outputMaster;

	private PluginNode outputPluginNode;

	private Plugin outputPlugin;

	private Map<Integer, Boolean> outputMap;

	private List<PluginRecord> pendingList;

	private Map<Integer, PluginRecord> checkMap;

	private FormAdjust formAdjust;

	public SplitTask(StatusDialog statusDialog, File pluginFile, PluginNode pluginNode, boolean independentMaster,
			PluginNode outputMasterNode, PluginNode outputPluginNode)
	{
		super(statusDialog);

		this.pluginFile = pluginFile;
		this.pluginNode = pluginNode;
		this.plugin = pluginNode.getPlugin();
		this.formList = this.plugin.getFormList();
		this.formMap = this.plugin.getFormMap();
		this.masterCount = this.plugin.getMasterList().size();

		this.outputMasterNode = outputMasterNode;
		this.outputMaster = outputMasterNode.getPlugin();

		this.outputPluginNode = outputPluginNode;
		this.outputPlugin = outputPluginNode.getPlugin();

		this.independentMaster = independentMaster;
		this.outputMap = new HashMap<Integer, Boolean>(this.formList.size());
		this.checkMap = new HashMap<Integer, PluginRecord>(this.formList.size());
		this.pendingList = new ArrayList<PluginRecord>(25);
	}

	public static boolean splitPlugin(JDialog parent, File pluginFile, PluginNode pluginNode, boolean independentMaster,
			PluginNode outputMasterNode, PluginNode outputPluginNode)
	{
		boolean completed = false;

		StatusDialog statusDialog = new StatusDialog(parent, "Splitting " + pluginNode.getPlugin().getName(), "Split Plugin");

		SplitTask worker = new SplitTask(statusDialog, pluginFile, pluginNode, independentMaster, outputMasterNode, outputPluginNode);
		statusDialog.setWorker(worker);

		worker.start();
		statusDialog.showDialog();

		if (statusDialog.getStatus() == 1)
			completed = true;
		else
		{
			JOptionPane.showMessageDialog(parent, "Unable to split " + pluginNode.getPlugin().getName(), "Split Plugin", 1);
		}
		return completed;
	}

	public void run()
	{
		boolean completed = false;
		try
		{
			int recordCount = this.formList.size();
			int processedCount = 0;
			int currentProgress = 0;

			int[] masterMap = new int[this.masterCount];
			for (int i = 0; i < this.masterCount; i++)
			{
				masterMap[i] = i;
			}
			this.formAdjust = new FormAdjust(masterMap, this.masterCount, this.formMap);

			this.masters = new Master[this.masterCount];
			List<String> masterList = this.plugin.getMasterList();
			int index = 0;
			for (String masterName : masterList)
			{
				File masterFile = new File(this.pluginFile.getParent() + Main.fileSeparator + masterName);
				Master master = new Master(masterFile);
				master.load(this);
				this.masters[(index++)] = master;
			}

			getStatusDialog().updateMessage("Splitting " + this.plugin.getName());

			//PJPJPJPJPJ remove odd nulls! that damn 20 again!
			for (int i = 0; i < this.formList.size(); i++)
			{
				FormInfo formInfo = this.formList.get(i);
				if (formInfo.getSource() == null)
				{
					System.out.println("formInfo.getSource() == null for formId " + formInfo.getFormID());
					formList.remove(i);
					i--;
				}
			}

			for (FormInfo formInfo : this.formList)
			{
				PluginRecord record = (PluginRecord) formInfo.getSource();
				if (Main.debugMode)
				{
					System.out.printf("Mapping %s record %s (%08X)\n", new Object[]
					{ record.getRecordType(), record.getEditorID(), Integer.valueOf(record.getFormID()) });
				}
				boolean addMaster = checkMaster(record, false);
				int formID = record.getFormID();
				int masterID = formID >>> 24;
				int splitFormID;

				if (masterID < this.masterCount)
				{
					splitFormID = formID;
				}
				else
				{

					if ((addMaster) || (!this.independentMaster))
						splitFormID = formID & 0xFFFFFF | this.masterCount << 24;
					else
						splitFormID = formID & 0xFFFFFF | this.masterCount + 1 << 24;
				}
				formInfo.setMergedFormID(splitFormID);
				this.pendingList.clear();
				this.outputMap.put(new Integer(formID), new Boolean(addMaster));
				if (Main.debugMode)
				{
					System.out.printf("%s record %s (%08X) master status set to %s\n", new Object[]
					{ record.getRecordType(), record.getEditorID(), Integer.valueOf(record.getFormID()), Boolean.valueOf(addMaster) });
				}

				if (interrupted())
				{
					throw new InterruptedException("Request canceled");
				}
				processedCount++;
				int newProgress = processedCount * 50 / recordCount;
				if (newProgress >= currentProgress + 5)
				{
					currentProgress = newProgress;
					getStatusDialog().updateProgress(currentProgress);
				}

			}

			processedCount = 0;
			for (FormInfo formInfo : this.formList)
			{
				PluginRecord record = (PluginRecord) formInfo.getSource();
				if (this.outputMap.get(new Integer(record.getFormID())).booleanValue())
				{
					this.outputMaster.copyRecord(record, this.formAdjust);
					if (record.getRecordType().equals("REFR"))
						cloneDoorReference(record, formInfo);
				}
				else
				{
					this.outputPlugin.copyRecord(record, this.formAdjust);
				}

				if (interrupted())
				{
					throw new InterruptedException("Request canceled");
				}
				processedCount++;
				int newProgress = processedCount * 50 / recordCount + 50;
				if (newProgress >= currentProgress + 5)
				{
					currentProgress = newProgress;
					getStatusDialog().updateProgress(currentProgress);
				}

			}

			this.outputMaster.store(this);
			this.outputPlugin.store(this);

			this.outputMasterNode.buildNodes(this);
			this.outputPluginNode.buildNodes(this);

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
			Main.logException("Exception while splitting plugin", exc);
		}

		getStatusDialog().closeDialog(completed);
	}

	private boolean checkMaster(PluginRecord record, boolean recursive) throws DataFormatException, IOException, PluginException
	{
		int formID = record.getFormID();
		Integer objFormID = new Integer(formID);
		Boolean objAddMaster = this.outputMap.get(objFormID);
		if (objAddMaster != null)
		{
			return objAddMaster.booleanValue();
		}

		boolean addMaster = true;
		boolean addStatus = true;
		String recordType = record.getRecordType();
		int masterID = formID >>> 24;

		PluginGroup group = (PluginGroup) record.getParent();
		int groupType = group.getGroupType();
		int parentFormID = group.getGroupParentID();
		int parentMasterID = parentFormID >>> 24;

		if (masterID < this.masterCount)
		{
			if (groupType == 0)
			{
				if ((recordType.equals("CELL")) || (recordType.equals("DIAL")))
				{
					addMaster = false;
				}
				else
				{
					Master master = this.masters[masterID];
					int masterFormID = formID & 0xFFFFFF | master.getMasterList().size() << 24;
					if (master.getFormMap().get(new Integer(masterFormID)) != null)
						addMaster = false;
				}
			}
			else
				addMaster = false;

		}
		else
		{
			if ((recordType.equals("GMST")) || (recordType.equals("MGEF")))
			{
				addMaster = false;
			}
			else if (groupType == 1)
			{
				if (parentMasterID < this.masterCount)
				{
					Master master = this.masters[parentMasterID];
					int masterFormID = parentFormID & 0xFFFFFF | master.getMasterList().size() << 24;
					if (master.getFormMap().get(new Integer(masterFormID)) != null)
						addMaster = false;
				}
			}
			else if (groupType == 7)
			{
				if (parentMasterID < this.masterCount)
					addMaster = false;
			}
			else if (groupType == 10)
			{
				addMaster = false;
			}
			else if ((groupType == 6) || (groupType == 8) || (groupType == 9))
			{
				if (parentMasterID < this.masterCount)
				{
					addMaster = false;
				}
				else
				{
					FormInfo cellInfo = this.formMap.get(new Integer(parentFormID));
					if (cellInfo != null)
					{
						int cellFormID = cellInfo.getMergedFormID();
						int cellMasterID = cellFormID >>> 24;
						if (cellMasterID != this.masterCount)
						{
							addMaster = false;
						}

					}

				}

			}

			if ((this.independentMaster) && (addMaster))
			{
				int status = checkReferences(record);
				if (status < 0)
					addStatus = false;
				else if (status == 0)
				{
					addMaster = false;
				}

			}

		}

		if (addStatus)
		{
			if (recursive)
			{
				this.outputMap.put(objFormID, new Boolean(addMaster));
				if (Main.debugMode)
					System.out.printf("%s record %s (%08X) master status set to %s\n", new Object[]
					{ record.getRecordType(), record.getEditorID(), objFormID, Boolean.valueOf(addMaster) });
			}
		}
		else if (!this.pendingList.contains(record))
		{
			this.pendingList.add(record);
		}

		return addMaster;
	}

	private int checkReferences(PluginRecord record) throws DataFormatException, IOException, PluginException
	{
		boolean clean = true;
		boolean recursiveCheck = false;
		if (Main.debugMode)
		{
			System.out.printf("Checking references for %s record %s (%08X)\n", new Object[]
			{ record.getRecordType(), record.getEditorID(), Integer.valueOf(record.getFormID()) });
		}

		int recordFormID = record.getFormID();
		Integer objFormID = new Integer(recordFormID);
		if (this.checkMap.get(objFormID) != null)
		{
			if (Main.debugMode)
			{
				System.out.printf("Recursive reference check for %s record %s (%08X)\n", new Object[]
				{ record.getRecordType(), record.getEditorID(), Integer.valueOf(record.getFormID()) });
			}
			return -1;
		}

		this.checkMap.put(objFormID, record);

		List<PluginSubrecord> subrecords = record.getSubrecords();
		for (PluginSubrecord subrecord : subrecords)
		{
			int[][] references = subrecord.getReferences();
			if ((references == null) || (references.length == 0))
			{
				continue;
			}

			for (int i = 0; i < references.length; i++)
			{
				int formID = references[i][1];
				int masterID = formID >>> 24;
				if (formID == 0)
				{
					continue;
				}

				Integer checkFormID = new Integer(formID);
				if (masterID < this.masterCount)
				{
					if (this.formMap.get(checkFormID) != null)
						clean = false;
				}
				else
				{
					Boolean addMaster = this.outputMap.get(checkFormID);
					if (addMaster != null)
					{
						if (!addMaster.booleanValue())
							clean = false;
					}
					else if (this.checkMap.get(checkFormID) == null)
					{
						FormInfo formInfo = this.formMap.get(checkFormID);
						if (formInfo != null)
						{
							PluginRecord checkRecord = (PluginRecord) formInfo.getSource();
							if (!this.pendingList.contains(checkRecord))
								clean = checkMaster((PluginRecord) formInfo.getSource(), true);
						}
						else
						{
							clean = false;
						}
					}
					else
					{
						recursiveCheck = true;
					}
				}

				if (!clean)
				{
					if (!Main.debugMode)
						break;
					System.out.printf("Unclean reference %08X in %s subrecord of record %08X\n", new Object[]
					{ checkFormID, subrecord.getSubrecordType(), Integer.valueOf(recordFormID) });

					break;
				}
			}

			if (!clean)
			{
				break;
			}

		}

		this.checkMap.remove(objFormID);
		return clean ? 1 : recursiveCheck ? -1 : 0;
	}

	private void cloneDoorReference(PluginRecord record, FormInfo formInfo) throws DataFormatException, IOException, PluginException
	{
		boolean cloneReference = false;

		int masterFormID = formInfo.getMergedFormID();
		FormInfo masterFormInfo = this.outputMaster.getFormMap().get(new Integer(masterFormID));
		if (masterFormInfo == null)
		{
			throw new PluginException(String.format("Unable to locate output master record %08X", new Object[]
			{ Integer.valueOf(masterFormID) }));
		}
		PluginRecord masterRecord = (PluginRecord) masterFormInfo.getSource();

		List<PluginSubrecord> subrecords = masterRecord.getSubrecords();
		int count = subrecords.size();
		int index;
		for (index = 0; index < count; index++)
		{
			PluginSubrecord subrecord = subrecords.get(index);
			String subrecordType = subrecord.getSubrecordType();
			if (subrecordType.equals("XTEL"))
			{
				byte[] subrecordData = subrecord.getSubrecordData();
				int formID = SerializedElement.getInteger(subrecordData, 0);
				int masterID = formID >>> 24;
				if (masterID == this.masterCount)
					break;
				cloneReference = true;

				break;
			}

		}

		if (cloneReference)
		{
			subrecords.remove(index);
			masterRecord.setSubrecords(subrecords);

			this.outputPlugin.copyRecord(record, this.formAdjust);
		}
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.SplitTask
 * JD-Core Version:    0.6.0
 */