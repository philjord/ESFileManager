package TES4Gecko;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.zip.DataFormatException;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class GenerateTask extends WorkerTask
{
	private File pluginFile;

	private Plugin plugin;

	private byte[] voiceData;

	private Map<Integer, byte[]> voiceDataMap;

	private Map<Integer, byte[]> lipSynchDataMap;

	private static final int maxSecondsSilence = 20;

	private static final int wordsPerSecondSilence = 3;

	private static final int bogusNPCFormID = 66947;

	private List<PluginRace> raceList;

	private Map<Integer, PluginRace> raceMap;

	private List<PluginNPC> npcList;

	private Map<Integer, PluginNPC> npcMap;

	private List<PluginRecord> questList;

	private Map<Integer, PluginRecord> questMap;

	private List<PluginTopic> topicList;

	private Map<Integer, PluginTopic> topicMap;

	private static JFrame parentWindow;

	private static int numInfosProcessed = 0;

	private static int numFilesCreated = 0;

	public GenerateTask(StatusDialog statusDialog, File pluginFile)
	{
		super(statusDialog);
		this.pluginFile = pluginFile;
	}

	public static void generateResponses(JFrame parent, File pluginFile)
	{
		parentWindow = parent;

		StatusDialog statusDialog = new StatusDialog(parent, " ", "Create Silent Voice Files");

		GenerateTask worker = new GenerateTask(statusDialog, pluginFile);
		statusDialog.setWorker(worker);

		worker.start();
		statusDialog.showDialog();

		if (statusDialog.getStatus() == 1)
			JOptionPane.showMessageDialog(parent, "Silent voice files created for " + pluginFile.getName() + "\n" + numFilesCreated
					+ " files created for " + numInfosProcessed + " INFO records.", "Create Silent Voice Files", 1);
		else
			JOptionPane.showMessageDialog(parent, "Unable to create silent voice files for " + pluginFile.getName(),
					"Create Silent Voice Files", 1);
	}

	public void run()
	{
		boolean completed = false;
		numInfosProcessed = 0;
		numFilesCreated = 0;
		try
		{
			int selection = JOptionPane
					.showConfirmDialog(
							parentWindow,
							"<html>This operation will remove <b>all</b> of the files currently in the voice \ndirectory of this plugin. Even if this is the desired behavior, that\nvoice directory should be backed up before executing this action.\nDo you still want to do this?",
							"Create Silent Voice Files: " + this.pluginFile.getName(), 0, 3);
			if (selection != 0)
				return;

			URL fileURL = Main.class.getResource("Main.class");
			String geckoPath = "Gecko path: " + fileURL.getFile() + "\n";
			if (fileURL == null)
			{
				throw new IOException("Unable to locate Main class");
			}
			String filePath = fileURL.getPath();
			int sep = filePath.indexOf(':');
			if (sep < 1)
			{
				throw new IOException("Main class path is not valid");
			}
			String protocol = fileURL.getProtocol();
			if ((!protocol.equals("file")) && (!protocol.equals("jar")))
			{
				throw new IOException("Main class path protocol is not valid");
			}

			String firstCut = filePath.substring(0, filePath.indexOf("/Main.class"));
			String uriString = firstCut.substring(firstCut.indexOf('/'));
			if (uriString.contains("!"))
			{
				uriString = uriString.substring(0, uriString.lastIndexOf('!'));
				uriString = uriString.substring(0, uriString.lastIndexOf('/'));
			}
			URI fileURI = new URI(uriString);
			filePath = fileURI.getPath().substring(1);
			File voiceFile = new File(filePath + Main.fileSeparator + "TES4Gecko-Silence.mp3");
			if ((!voiceFile.exists()) || (!voiceFile.isFile()))
			{
				throw new IOException("'" + voiceFile.getPath() + "' does not exist");
			}
			this.voiceData = new byte[(int) voiceFile.length()];
			FileInputStream in = new FileInputStream(voiceFile);
			in.read(this.voiceData);
			in.close();

			this.voiceDataMap = new HashMap<Integer, byte[]>();
			this.lipSynchDataMap = new HashMap<Integer, byte[]>();
			FileInputStream inLipSynch;
			for (int i = 1; i <= 20; i++)
			{
				String silenceFilePath = String.format(filePath + "/TES4Gecko-Silence%02d.mp3", new Object[]
				{ Integer.valueOf(i) });
				File silenceFile = new File(silenceFilePath);
				String lipSynchFilePath = String.format(filePath + "/TES4Gecko-Silence%02d.lip", new Object[]
				{ Integer.valueOf(i) });
				File lipSynchFile = new File(lipSynchFilePath);
				if ((!silenceFile.exists()) || (!silenceFile.isFile()))
				{
					this.voiceDataMap.clear();
					this.lipSynchDataMap.clear();
					break;
				}

				byte[] silenceData = new byte[(int) silenceFile.length()];
				FileInputStream inSilence = new FileInputStream(silenceFile);
				inSilence.read(silenceData);
				inSilence.close();
				this.voiceDataMap.put(Integer.valueOf(i), silenceData);

				byte[] lipSynchData = new byte[(lipSynchFile.exists()) && (lipSynchFile.isFile()) ? (int) lipSynchFile.length() : 1];
				if (lipSynchData.length > 1)
				{
					inLipSynch = new FileInputStream(lipSynchFile);
					inLipSynch.read(lipSynchData);
					inLipSynch.close();
				}
				this.lipSynchDataMap.put(Integer.valueOf(i), lipSynchData);
			}

			this.plugin = new Plugin(this.pluginFile);
			this.plugin.load(this);
			int numInfos = getNumberInfos(this.plugin);
			List<PluginGroup> groupList = this.plugin.getGroupList();
			List<String> masterList = this.plugin.getMasterList();
			Master[] masters = new Master[masterList.size()];

			int index = 0;
			for (String masterName : masterList)
			{
				File masterFile = new File(this.pluginFile.getParent() + Main.fileSeparator + masterName);
				Master master = new Master(masterFile);
				master.load(this);
				masters[(index++)] = master;
			}

			this.raceList = new ArrayList<PluginRace>(32);
			this.raceMap = new HashMap<Integer, PluginRace>(32);
			this.npcList = new ArrayList<PluginNPC>(3072);
			this.npcMap = new HashMap<Integer, PluginNPC>(3072);
			this.questList = new ArrayList<PluginRecord>(512);
			this.questMap = new HashMap<Integer, PluginRecord>(512);
			this.topicList = new ArrayList<PluginTopic>(4096);
			this.topicMap = new HashMap<Integer, PluginTopic>(4096);

			File voiceBase = new File(this.pluginFile.getParent() + Main.fileSeparator + "Sound" + Main.fileSeparator + "Voice"
					+ Main.fileSeparator + this.plugin.getName());
			if (voiceBase.exists())
			{
				getStatusDialog().updateMessage("Deleting existing voice files");
				if (voiceBase.isDirectory())
					deleteDirectoryTree(voiceBase);
				else
				{
					voiceBase.delete();
				}

			}

			getStatusDialog().updateMessage("Building cross-reference lists");

			for (PluginGroup group : groupList)
			{
				String groupRecordType = group.getGroupRecordType();
				List<PluginRecord> recordList = group.getRecordList();
				if (groupRecordType.equals("RACE"))
				{
					for (PluginRecord record : recordList)
						if ((record.getRecordType().equals("RACE")) && (!record.isIgnored()))
						{
							PluginRace race;
							if (record.isDeleted())
							{
								race = new PluginRace(record.getFormID());
								race.setDelete(true);
							}
							else
							{
								race = buildRaceEntry(record);
							}

							this.raceList.add(race);
							this.raceMap.put(new Integer(race.getFormID()), race);
						}
				}
				else if (groupRecordType.equals("NPC_"))
				{
					for (PluginRecord record : recordList)
						if ((record.getRecordType().equals("NPC_")) && (!record.isIgnored()))
						{
							PluginNPC npc;
							if (record.isDeleted())
							{
								npc = new PluginNPC(record.getFormID());
								npc.setDelete(true);
							}
							else
							{
								npc = buildNPCEntry(record);
							}

							this.npcList.add(npc);
							this.npcMap.put(new Integer(npc.getFormID()), npc);
						}
				}
				else if (groupRecordType.equals("QUST"))
				{
					for (PluginRecord record : recordList)
						if ((record.getRecordType().equals("QUST")) && (!record.isIgnored()))
						{
							PluginQuest quest;
							if (record.isDeleted())
							{
								quest = new PluginQuest(record.getFormID());
								quest.setDelete(true);
							}
							else
							{
								quest = buildQuestEntry(record);
							}

							this.questList.add(record);
							this.questMap.put(new Integer(quest.getFormID()), record);
						}
				}
				else if (groupRecordType.equals("DIAL"))
				{
					for (PluginRecord record : recordList)
					{
						if ((record.getRecordType().equals("DIAL")) && (!record.isIgnored()))
						{
							PluginTopic topic;
							if (record.isDeleted())
							{
								topic = new PluginTopic(record.getFormID());
								topic.setDelete(true);
							}
							else
							{
								topic = buildTopicEntry(record);
							}

							this.topicList.add(topic);
							this.topicMap.put(new Integer(topic.getFormID()), topic);
						}

					}

				}

			}

			List<String> masterListInCaps = this.plugin.getMasterList();
			ListIterator<String> lit = masterListInCaps.listIterator();
			while (lit.hasNext())
			{
				String tmpMaster = lit.next();
				lit.set(tmpMaster.toUpperCase());
			}
			Master master;

			for (int masterID = masters.length - 1; masterID > -1; masterID--)
			{
				master = masters[masterID];
				List<FormInfo> formList = master.getFormList();
				List<String> masterListforMaster = master.getMasterList();

				for (FormInfo formInfo : formList)
				{
					int newMasterID = masterID;
					String recordType = formInfo.getRecordType();
					if ((recordType.equals("RACE")) || (recordType.equals("NPC_")) || (recordType.equals("QUST"))
							|| (recordType.equals("DIAL")))
					{
						int formMasterID = (formInfo.getFormID() & 0xFF000000) >> 24;
						if (formMasterID < masterListforMaster.size())
						{
							String formMasterName = masterListforMaster.get(formMasterID);
							String raceFormID = String.format("%08X", new Object[]
							{ Integer.valueOf(formInfo.getFormID()) });
							int pluginMasterIdx = masterListInCaps.indexOf(formMasterName.toUpperCase());
							if (pluginMasterIdx == -1)
							{
								if (Main.debugMode)
								{
									System.out
											.printf("GenerateTask: Form ID %08X is modified in <%s> from the original in <%s>;  but <%s> is not in the master list for plugin <%s>.\n",
													new Object[]
													{ Integer.valueOf(formInfo.getFormID()), master.getName(), formMasterName,
															formMasterName, this.plugin.getName() });
								}
							}
							else
								newMasterID = pluginMasterIdx;
						}
					}
					if (recordType.equals("RACE"))
					{
						PluginRecord record = master.getRecord(formInfo.getFormID());
						PluginRace race = buildRaceEntry(record);
						int formID = race.getFormID() & 0xFFFFFF | newMasterID << 24;
						Integer objFormID = new Integer(formID);
						if (this.raceMap.get(objFormID) == null)
						{
							this.raceList.add(race);
							this.raceMap.put(objFormID, race);
						}
					}
					else if (recordType.equals("NPC_"))
					{
						PluginRecord record = master.getRecord(formInfo.getFormID());
						PluginNPC npc = buildNPCEntry(record);
						int formID = npc.getFormID() & 0xFFFFFF | newMasterID << 24;
						Integer objFormID = new Integer(formID);
						if (this.npcMap.get(objFormID) == null)
						{
							this.npcList.add(npc);
							this.npcMap.put(objFormID, npc);
						}
					}
					else if (recordType.equals("QUST"))
					{
						PluginRecord record = master.getRecord(formInfo.getFormID());
						PluginQuest quest = buildQuestEntry(record);
						int formID = quest.getFormID() & 0xFFFFFF | newMasterID << 24;
						Integer objFormID = new Integer(formID);
						if (this.questMap.get(objFormID) == null)
						{
							this.questList.add(record);
							this.questMap.put(objFormID, record);
						}
					}
					else if (recordType.equals("DIAL"))
					{
						PluginRecord record = master.getRecord(formInfo.getFormID());
						PluginTopic topic = buildTopicEntry(record);
						int formID = topic.getFormID() & 0xFFFFFF | newMasterID << 24;
						Integer objFormID = new Integer(formID);
						if (this.topicMap.get(objFormID) == null)
						{
							this.topicList.add(topic);
							this.topicMap.put(objFormID, topic);
						}

					}

				}

			}

			getStatusDialog().updateMessage("Creating silent voice files for " + this.pluginFile.getName());
			for (PluginGroup group : groupList)
			{
				String groupRecordType = group.getGroupRecordType();
				if (groupRecordType.equals("DIAL"))
				{
					List<PluginRecord> recordList = group.getRecordList();
					for (PluginRecord record : recordList)
					{
						if (record.getRecordType().equals("GRUP"))
						{
							PluginGroup infoGroup = (PluginGroup) record;
							int topicID = infoGroup.getGroupParentID();
							List<PluginRecord> infoList = infoGroup.getRecordList();
							for (PluginRecord infoRecord : infoList)
							{
								if ((!infoRecord.getRecordType().equals("INFO")) || (infoRecord.isIgnored()) || (infoRecord.isDeleted()))
									continue;
								int masterID = infoRecord.getFormID() >>> 24;
								if (masterID < masterList.size())
									continue;
								numFilesCreated += createResponseFiles(topicID, infoRecord);
								numInfosProcessed += 1;
								if (numInfosProcessed % 100 == 0)
								{
									getStatusDialog().updateProgress(100 * numInfosProcessed / numInfos);
								}
							}
						}
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
			Main.logException("Exception while generating responses", exc);
		}

		getStatusDialog().closeDialog(completed);
	}

	private PluginRace buildRaceEntry(PluginRecord record) throws DataFormatException, IOException, PluginException
	{
		int raceFormID = record.getFormID();
		int maleFormID = raceFormID;
		int femaleFormID = raceFormID;
		boolean playableRace = false;
		String raceName = null;

		List<PluginSubrecord> subrecordList = record.getSubrecords();
		for (PluginSubrecord subrecord : subrecordList)
		{
			String subrecordType = subrecord.getSubrecordType();
			if (subrecordType.equals("FULL"))
			{
				byte[] subrecordData = subrecord.getSubrecordData();
				if (subrecordData.length > 1)
					raceName = new String(subrecordData, 0, subrecordData.length - 1);
			}
			else if (subrecordType.equals("VNAM"))
			{
				byte[] subrecordData = subrecord.getSubrecordData();
				if (subrecordData.length >= 4)
				{
					int formID = SerializedElement.getInteger(subrecordData, 0);
					if (formID != 0)
					{
						maleFormID = formID;
					}
				}
				if (subrecordData.length >= 8)
				{
					int formID = SerializedElement.getInteger(subrecordData, 4);
					if (formID != 0)
						femaleFormID = formID;
				}
			}
			else if (subrecordType.equals("DATA"))
			{
				byte[] subrecordData = subrecord.getSubrecordData();
				if ((subrecordData.length >= 33) && ((subrecordData[32] & 0x1) != 0))
				{
					playableRace = true;
				}
			}
		}
		if (raceName == null)
		{
			raceName = record.getEditorID();
		}
		return new PluginRace(raceFormID, record.getEditorID(), raceName, playableRace, maleFormID, femaleFormID);
	}

	private PluginNPC buildNPCEntry(PluginRecord record) throws DataFormatException, IOException, PluginException
	{
		int formID = record.getFormID();
		int raceID = 0;
		boolean female = false;

		List<PluginSubrecord> subrecordList = record.getSubrecords();
		for (PluginSubrecord subrecord : subrecordList)
		{
			String subrecordType = subrecord.getSubrecordType();
			if (subrecordType.equals("RNAM"))
			{
				byte[] subrecordData = subrecord.getSubrecordData();
				if (subrecordData.length >= 4)
					raceID = SerializedElement.getInteger(subrecordData, 0);
			}
			else if (subrecordType.equals("ACBS"))
			{
				byte[] subrecordData = subrecord.getSubrecordData();
				if ((subrecordData.length >= 1) && ((subrecordData[0] & 0x1) != 0))
				{
					female = true;
				}
			}
		}
		return new PluginNPC(formID, record.getEditorID(), raceID, female);
	}

	private PluginQuest buildQuestEntry(PluginRecord record)
	{
		return new PluginQuest(record.getFormID(), record.getEditorID());
	}

	private PluginTopic buildTopicEntry(PluginRecord record)
	{
		return new PluginTopic(record.getFormID(), record.getEditorID());
	}

	private int createResponseFiles(int topicID, PluginRecord infoRecord) throws DataFormatException, IOException, PluginException
	{
		List<PluginSubrecord> subrecordList = infoRecord.getSubrecords();
		int infoID = infoRecord.getFormID() & 0xFFFFFF;
		int responseCount = 0;
		int questID = 0;
		int npcID = 0;
		int raceID = 0;
		List<Integer> respNums = new ArrayList<Integer>();
		List<Integer> voiceIdx = new ArrayList<Integer>();

		Map<?, ?> responseNPCs = new HashMap<Object, Object>();
		List<Integer> responseIncludedNPCs = new ArrayList<Integer>();
		List<Integer> responseExcludedRaces = new ArrayList<Integer>();
		List<Integer> responseIncludedRaces = new ArrayList<Integer>();
		for (PluginSubrecord subrecord : subrecordList)
		{
			String subrecordType = subrecord.getSubrecordType();
			if (subrecordType.equals("QSTI"))
			{
				byte[] subrecordData = subrecord.getSubrecordData();
				questID = SerializedElement.getInteger(subrecordData, 0);
			}
			else if (subrecordType.equals("CTDA"))
			{
				byte[] subrecordData = subrecord.getSubrecordData();
				int code = SerializedElement.getInteger(subrecordData, 8);
				if ((subrecordData[0] == 0) && ((code == 72) || (code == 69)))
				{
					int bits = SerializedElement.getInteger(subrecordData, 4);
					float value = Float.intBitsToFloat(bits);
					int formID = SerializedElement.getInteger(subrecordData, 12);
					if (value == 1.0D)
					{
						if (code == 72)
						{
							npcID = formID;
							responseIncludedNPCs.add(Integer.valueOf(npcID));
						}
						else
						{
							raceID = formID;
							responseIncludedRaces.add(Integer.valueOf(raceID));
						}
					}
					else
					{
						if (value != 0.0D)
							continue;
						if (code != 1157)
							continue;
						int raceIDEx = formID;
						responseExcludedRaces.add(Integer.valueOf(raceIDEx));
					}
				}

			}
			else if (subrecordType.equals("TRDT"))
			{
				responseCount++;
				byte[] subrecordData = subrecord.getSubrecordData();
				respNums.add(new Integer(subrecordData[12]));
			}
			else
			{
				if (!subrecordType.equals("NAM1"))
					continue;
				if (this.voiceDataMap.size() == 0)
				{
					voiceIdx.add(Integer.valueOf(0));
				}
				else
				{
					byte[] subrecordData = subrecord.getSubrecordData();
					String respLine = new String(subrecordData, 0, subrecordData.length - 1).trim();
					if (respLine.equals(""))
					{
						voiceIdx.add(Integer.valueOf(1));
					}
					else
					{
						String[] numWords = respLine.split("[^\\w'\\-]+");
						int secsOfSilence = numWords.length / 3;
						secsOfSilence = secsOfSilence + 1 < 20 ? secsOfSilence + 1 : 20;
						voiceIdx.add(Integer.valueOf(secsOfSilence));
					}
				}
			}

		}

		if ((responseCount == 0) || (questID == 0))
		{
			return 0;
		}

		PluginTopic topic = this.topicMap.get(new Integer(topicID));
		if (topic == null)
		{
			String text = String.format("Topic %08X not found", new Object[0]);
			throw new PluginException(text);
		}

		if (topic.isDeleted())
		{
			String text = String.format("Topic %08X is deleted", new Object[]
			{ Integer.valueOf(topicID) });
			throw new PluginException(text);
		}

		String topicName = topic.getEditorID();

		PluginRecord quest = this.questMap.get(new Integer(questID));
		if (quest == null)
		{
			String text = String.format("Quest %08X not found for dialog topic %s", new Object[]
			{ Integer.valueOf(questID), topicName });
			throw new PluginException(text);
		}

		if (quest.isDeleted())
		{
			String text = String.format("Quest %08X is deleted", new Object[]
			{ Integer.valueOf(questID) });
			throw new PluginException(text);
		}

		int filesCreated = 0;
		String questName = quest.getEditorID();

		List<PluginSubrecord> questSubrecordList = quest.getSubrecords();
		List<Integer> questIncludedNPCs = new ArrayList<Integer>();
		List<Integer> questExcludedRaces = new ArrayList<Integer>();
		List<Integer> questIncludedRaces = new ArrayList<Integer>();
		String subrecordType;
		byte[] subrecordData;
		for (PluginSubrecord subrecord : questSubrecordList)
		{
			subrecordType = subrecord.getSubrecordType();
			if (subrecordType.equals("CTDA"))
			{
				subrecordData = subrecord.getSubrecordData();
				int code = SerializedElement.getInteger(subrecordData, 8);
				if ((subrecordData[0] == 0) && ((code == 72) || (code == 69)))
				{
					int bits = SerializedElement.getInteger(subrecordData, 4);
					float value = Float.intBitsToFloat(bits);
					int formID = SerializedElement.getInteger(subrecordData, 12);
					if (value == 1.0D)
					{
						if (code == 72)
						{
							questIncludedNPCs.add(Integer.valueOf(formID));
						}
						else
						{
							questIncludedRaces.add(Integer.valueOf(formID));
						}
					}
					else
					{
						if (value != 0.0D)
							continue;
						if (code != 1157)
							continue;
						questExcludedRaces.add(Integer.valueOf(formID));
					}
				}

			}

		}

		List<PluginRace> racesQuestLevel = new ArrayList<PluginRace>();

		if (questIncludedNPCs.size() == 0)
		{
			if (questIncludedRaces.size() > 0)
			{
				for (PluginRace race : this.raceList)
				{
					if (race.isDeleted())
						continue;
					if (!questIncludedRaces.contains(Integer.valueOf(race.getFormID())))
						continue;
					racesQuestLevel.add(race);
				}

			}
			else if (questExcludedRaces.size() > 0)
			{
				for (PluginRace race : this.raceList)
				{
					if ((!race.isPlayableRace()) || (race.isDeleted()))
						continue;
					if (questExcludedRaces.contains(Integer.valueOf(race.getFormID())))
						continue;
					racesQuestLevel.add(race);
				}

			}
			else
			{
				for (PluginRace race : this.raceList)
				{
					if ((!race.isPlayableRace()) || (race.isDeleted()))
						continue;
					racesQuestLevel.add(race);
				}

			}

		}

		List<PluginRace> racesResponseLevel = new ArrayList<PluginRace>();
		if (questIncludedNPCs.size() == 0)
		{
			if (responseIncludedNPCs.size() == 0)
			{
				if (responseIncludedRaces.size() > 0)
				{
					for (PluginRace race : racesQuestLevel)
					{
						if (!responseIncludedRaces.contains(Integer.valueOf(race.getFormID())))
							continue;
						racesResponseLevel.add(race);
					}

				}
				else if (responseExcludedRaces.size() > 0)
				{
					for (PluginRace race : racesQuestLevel)
					{
						if (responseExcludedRaces.contains(Integer.valueOf(race.getFormID())))
							continue;
						racesResponseLevel.add(race);
					}

				}
				else
				{
					racesResponseLevel = racesQuestLevel;
				}

			}

		}

		List<Integer> intersectNPCList = new ArrayList<Integer>();
		if ((questIncludedNPCs.size() == 0) && (responseIncludedNPCs.size() > 0))
			intersectNPCList = responseIncludedNPCs;
		else if ((questIncludedNPCs.size() > 0) && (responseIncludedNPCs.size() == 0))
		{
			intersectNPCList = questIncludedNPCs;
		}
		else
		{
			for (Iterator<Integer> code = questIncludedNPCs.iterator(); code.hasNext();)
			{
				int npc = code.next().intValue();

				if (!responseIncludedNPCs.contains(Integer.valueOf(npc)))
					continue;
				intersectNPCList.add(Integer.valueOf(npc));
			}

		}

		for (PluginRace race : racesResponseLevel)
		{
			filesCreated += copyVoiceData(questName, topicName, infoID, race, false, respNums, voiceIdx);
			filesCreated += copyVoiceData(questName, topicName, infoID, race, true, respNums, voiceIdx);
		}

		for (Iterator<Integer> code = intersectNPCList.iterator(); code.hasNext();)
		{
			int tmpNPCID = code.next().intValue();

			PluginNPC npc = this.npcMap.get(new Integer(tmpNPCID));
			if (npc == null)
			{
				String text = "";
				if (npcID != 66947)
				{
					text = String.format("NPC %08X not found for dialog topic %s (%08X) in quest %s (%08X)\n", new Object[]
					{ Integer.valueOf(tmpNPCID), topicName, Integer.valueOf(infoID), questName, Integer.valueOf(questID) });
					System.out.printf(text, new Object[0]);
				}
				if (npcID == 66947)
				{
					continue;
				}
				throw new PluginException(text);
			}

			if (npc.isDeleted())
			{
				String text = String.format("NPC %08X is deleted", new Object[]
				{ Integer.valueOf(npcID) });
				throw new PluginException(text);
			}
			raceID = npc.getRaceID();
			PluginRace race = this.raceMap.get(Integer.valueOf(raceID));
			if (race == null)
			{
				String text = String.format("Race %08X not found for NPC %s", new Object[]
				{ Integer.valueOf(raceID), npc.getEditorID() });
				throw new PluginException(text);
			}

			if (race.isDeleted())
			{
				String text = String.format("Race %08X is deleted", new Object[]
				{ Integer.valueOf(raceID) });
				throw new PluginException(text);
			}

			if ((racesQuestLevel.size() > 0) && (responseIncludedNPCs.size() > 0))
			{
				if ((!racesQuestLevel.contains(race)) && (race.isPlayableRace()))
					continue;
				filesCreated += copyVoiceData(questName, topicName, infoID, race, npc.isFemale(), respNums, voiceIdx);
			}
			else
			{
				filesCreated += copyVoiceData(questName, topicName, infoID, race, npc.isFemale(), respNums, voiceIdx);
			}

		}

		return filesCreated;
	}

	private void copyVoiceData(String questName, String topicName, int infoID, PluginRace race, boolean female, int count)
			throws IOException, PluginException
	{
		int voiceID = female ? race.getFemaleVoiceID() : race.getMaleVoiceID();
		PluginRace voiceRace = this.raceMap.get(new Integer(voiceID));
		if (voiceRace == null)
		{
			String text = String.format("Voice race %08X not found for race %s", new Object[]
			{ Integer.valueOf(voiceID), race.getName() });
			throw new PluginException(text);
		}

		if (voiceRace.isDeleted())
		{
			String text = String.format("Voice race %08X is deleted", new Object[]
			{ Integer.valueOf(voiceID) });
			throw new PluginException(text);
		}

		String filePath = String.format("%s\\Sound\\Voice\\%s\\%s\\%s", new Object[]
		{ this.pluginFile.getParent(), this.plugin.getName(), voiceRace.getName(), female ? "F" : "M" });
		File voiceDirectory = new File(filePath);
		if (!voiceDirectory.exists())
		{
			voiceDirectory.mkdirs();
		}

		for (int response = 1; response <= count; response++)
		{
			String fileName = String.format("%s_%s_%08X_%d.mp3", new Object[]
			{ questName, topicName, Integer.valueOf(infoID), Integer.valueOf(response) });
			File voiceFile = new File(filePath + "\\" + fileName);
			FileOutputStream out = null;
			try
			{
				out = new FileOutputStream(voiceFile);
				out.write(this.voiceData);
			}
			finally
			{
				if (out != null)
					out.close();
			}
		}
	}

	private int copyVoiceData(String questName, String topicName, int infoID, PluginRace race, boolean female, List<Integer> respNums,
			List<Integer> voiceIdx) throws IOException, PluginException
	{
		int voiceID = female ? race.getFemaleVoiceID() : race.getMaleVoiceID();
		PluginRace voiceRace = this.raceMap.get(new Integer(voiceID));
		if (voiceRace == null)
		{
			String text = String.format("Voice race %08X not found for race %s", new Object[]
			{ Integer.valueOf(voiceID), race.getName() });
			throw new PluginException(text);
		}

		if (voiceRace.isDeleted())
		{
			String text = String.format("Voice race %08X is deleted", new Object[]
			{ Integer.valueOf(voiceID) });
			throw new PluginException(text);
		}
		int filesCreated = 0;

		String filePath = String.format("%s\\Sound\\Voice\\%s\\%s\\%s", new Object[]
		{ this.pluginFile.getParent(), this.plugin.getName(), voiceRace.getName(), female ? "F" : "M" });
		File voiceDirectory = new File(filePath);
		if (!voiceDirectory.exists())
		{
			voiceDirectory.mkdirs();
		}

		for (int i = 0; i < respNums.size(); i++)
		{
			int response = respNums.get(i).intValue();
			int silenceIdx = voiceIdx.get(i).intValue();
			String fileName = String.format("%s_%s_%08X_%d.mp3", new Object[]
			{ questName, topicName, Integer.valueOf(infoID), Integer.valueOf(response) });
			String lipSynchFileName = String.format("%s_%s_%08X_%d.lip", new Object[]
			{ questName, topicName, Integer.valueOf(infoID), Integer.valueOf(response) });
			File voiceFile = new File(filePath + "\\" + fileName);
			if (!voiceFile.exists())
			{
				File lipSynchFile = new File(filePath + "\\" + lipSynchFileName);
				FileOutputStream out = null;
				FileOutputStream lipSynchOut = null;

				label545: try
				{
					out = new FileOutputStream(voiceFile);
					out.write(silenceIdx == 0 ? this.voiceData : (byte[]) this.voiceDataMap.get(Integer.valueOf(silenceIdx)));
					filesCreated++;
					if (silenceIdx > 0)
					{
						if (this.lipSynchDataMap.get(Integer.valueOf(silenceIdx)).length <= 1)
							break label545;
						lipSynchOut = new FileOutputStream(lipSynchFile);
						lipSynchOut.write(this.lipSynchDataMap.get(Integer.valueOf(silenceIdx)));
						filesCreated++;
					}
				}
				finally
				{
					if (out != null)
						out.close();
					if (lipSynchOut != null)
						lipSynchOut.close();
				}
			}
		}
		return filesCreated;
	}

	private int getNumberInfos(Plugin pl)
	{
		int numInfos = 0;
		List<FormInfo> allForms = pl.getFormList();

		for (FormInfo form : allForms)
		{
			if (!form.getRecordType().equals("INFO"))
				continue;
			numInfos++;
		}
		return numInfos;
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
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.GenerateTask
 * JD-Core Version:    0.6.0
 */