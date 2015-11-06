package TES4Gecko;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Master extends SerializedElement
{
	private static final int INDEX_VERSION = 5;

	private File masterFile;

	private PluginHeader masterHeader;

	private List<FormInfo> formList;

	private Map<Integer, FormInfo> formMap;

	public Master(File masterFile)
	{
		this.masterFile = masterFile;
		this.masterHeader = new PluginHeader(masterFile);
	}

	public String getName()
	{
		return this.masterFile.getName();
	}

	public float getVersion()
	{
		return this.masterHeader.getVersion();
	}

	public void setVersion(float version)
	{
		this.masterHeader.setVersion(version);
	}

	public String getCreator()
	{
		return this.masterHeader.getCreator();
	}

	public String getSummary()
	{
		return this.masterHeader.getSummary();
	}

	public int getRecordCount()
	{
		return this.masterHeader.getRecordCount();
	}

	public List<String> getMasterList()
	{
		return this.masterHeader.getMasterList();
	}

	public List<FormInfo> getFormList()
	{
		return this.formList;
	}

	public Map<Integer, FormInfo> getFormMap()
	{
		return this.formMap;
	}

	public String toString()
	{
		return this.masterFile.getName();
	}

	public PluginRecord getRecord(int formID) throws DataFormatException, IOException, PluginException
	{
		PluginRecord record = null;
		RandomAccessFile in = null;

		int masterFormID = formID;
		FormInfo formInfo = this.formMap.get(new Integer(masterFormID));
		if (formInfo == null)
		{
			throw new PluginException(String.format("%s: Record %08X not found", new Object[]
			{ this.masterFile.getName(), Integer.valueOf(masterFormID) }));
		}
		byte[] prefix = new byte[20];
		long fileOffset = ((Long) formInfo.getSource()).longValue();
		try
		{
			in = new RandomAccessFile(this.masterFile, "r");
			in.seek(fileOffset);
			int count = in.read(prefix);
			if (count != 20)
			{
				throw new PluginException(String.format("%s: Record %08X truncated", new Object[]
				{ this.masterFile.getName(), Integer.valueOf(masterFormID) }));
			}
			int recordLength = getInteger(prefix, 4);
			record = new PluginRecord(prefix);
			record.load(this.masterFile, in, recordLength);
		}
		finally
		{
			if (in != null)
			{
				in.close();
			}
		}
		return record;
	}

	public void load(WorkerTask task) throws PluginException, DataFormatException, IOException, InterruptedException
	{
		if (task != null)
		{
			StatusDialog statusDialog = task.getStatusDialog();
			if (statusDialog != null)
			{
				statusDialog.updateMessage("Loading " + this.masterFile.getName());
			}

		}

		if ((!this.masterFile.exists()) || (!this.masterFile.isFile()))
		{
			throw new IOException("Master file '" + this.masterFile.getName() + "' does not exist");
		}

		RandomAccessFile in = null;
		try
		{
			in = new RandomAccessFile(this.masterFile, "r");
			this.masterHeader.read(in);
		}
		finally
		{
			if (in != null)
			{
				in.close();
			}

		}

		String masterName = this.masterFile.getName();

		int sep = masterName.lastIndexOf('.');
		String indexName;

		if (sep > 0)
			indexName = "Gecko-" + masterName.substring(0, sep) + ".index";
		else
		{
			indexName = "Gecko-" + masterName + ".index";
		}
		File indexFile = new File(this.masterFile.getParent() + Main.fileSeparator + indexName);

		if (!indexFile.exists())
			buildIndexFile(task, indexFile);
		else
			readIndexFile(task, indexFile);
	}

	public void resetFormList()
	{
		for (int i = 0; i < this.formList.size(); i++)
		{
			this.formList.set(i, null);
		}
		this.formList = new ArrayList<FormInfo>(1000);
	}

	public void resetFormMap()
	{
		this.formMap.clear();
		this.formMap = new HashMap<Integer, FormInfo>(1000);
	}

	private void readIndexFile(WorkerTask task, File indexFile) throws DataFormatException, InterruptedException, IOException,
			PluginException
	{
		FileInputStream in = null;
		GZIPInputStream inflater = null;
		int masterID = this.masterHeader.getMasterList().size();
		StatusDialog statusDialog = null;
		if (task != null)
			statusDialog = task.getStatusDialog();
		try
		{
			in = new FileInputStream(indexFile);
			byte[] buffer = new byte[4096];
			boolean rebuildIndex = true;

			int count = in.read(buffer, 0, 24);
			if (count == 24)
			{
				String recordType = new String(buffer, 0, 4);
				if (recordType.equals("INDX"))
				{
					int version = getInteger(buffer, 4);
					if (version == 5)
					{
						int length = getInteger(buffer, 12);
						if (length == (int) this.masterFile.length())
						{
							long timestamp = getLong(buffer, 16);
							if (timestamp == this.masterFile.lastModified())
							{
								rebuildIndex = false;
							}
						}
					}
				}
			}
			if (rebuildIndex)
			{
				in.close();
				in = null;
				buildIndexFile(task, indexFile);
			}
			else
			{
				inflater = new GZIPInputStream(in);

				int recordCount = getInteger(buffer, 8);
				this.formList = new ArrayList<FormInfo>(recordCount);
				this.formMap = new HashMap<Integer, FormInfo>(recordCount);
				int offset = 0;
				int residual = 0;

				int processedCount = 0;
				int currentProgress = 0;

				while (residual < 17)
				{
					if ((residual > 0) && (offset > 0))
					{
						System.arraycopy(buffer, offset, buffer, 0, residual);
					}
					offset = 0;
					count = inflater.read(buffer, residual, buffer.length - residual);
					if (count < 0)
					{
						if (residual == 0)
							break;
						throw new PluginException(indexFile.getName() + ": Index file truncated");
					}

					residual += count;
				}

				if (residual != 0)
				{
					long position = getInteger(buffer, offset);
					int formID = getInteger(buffer, offset + 4);
					String recordType = new String(buffer, offset + 8, 4);
					int parentFormID = getInteger(buffer, offset + 12);
					offset += 16;
					residual -= 16;
					int length = 0;

					while (buffer[(offset + length)] != 0)
					{
						length++;
						if (length >= residual)
						{
							System.arraycopy(buffer, offset, buffer, 0, residual);
							offset = 0;
							count = inflater.read(buffer, residual, buffer.length - residual);
							if (count < 0)
							{
								throw new PluginException(indexFile.getName() + ": Index file truncated");
							}
							residual += count;
						}
					}

					String editorID = new String(buffer, offset, length);
					offset += length + 1;
					residual -= length + 1;

					FormInfo info = new FormInfo(new Long(position), recordType, formID, editorID);
					info.setParentFormID(parentFormID);
					info.setPlugin(this);
					this.formList.add(info);

					if (formID >>> 24 > masterID)
					{
						formID = formID & 0xFFFFFF | masterID << 24;
					}
					this.formMap.put(new Integer(formID), info);

					if ((task != null) && (WorkerTask.interrupted()))
					{
						throw new InterruptedException("Request canceled");
					}

					processedCount++;
					if (statusDialog != null)
					{
						int newProgress = processedCount * 100 / recordCount;
						if (newProgress >= currentProgress + 5)
						{
							currentProgress = newProgress;
							statusDialog.updateProgress(currentProgress);
						}
					}
				}
			}
		}
		finally
		{
			if (inflater != null)
				inflater.close();
			else if (in != null)
				in.close();
		}
	}

	private void buildIndexFile(WorkerTask task, File indexFile) throws DataFormatException, InterruptedException, IOException,
			PluginException
	{
		boolean completed = false;
		RandomAccessFile in = null;
		FileOutputStream out = null;
		GZIPOutputStream deflater = null;
		byte[] prefix = new byte[20];
		int masterID = this.masterHeader.getMasterList().size();
		StatusDialog statusDialog = null;
		if (task != null)
		{
			statusDialog = task.getStatusDialog();
		}

		int recordCount = this.masterHeader.getRecordCount();
		this.formList = new ArrayList<FormInfo>(recordCount);
		try
		{
			in = new RandomAccessFile(this.masterFile, "r");
			long fileSize = this.masterFile.length();
			int currentProgress = 0;

			if (indexFile.exists())
			{
				indexFile.delete();
			}
			out = new FileOutputStream(indexFile);
			while (true)
			{
				int count = in.read(prefix);
				if (count == -1)
				{
					break;
				}
				if (count != 20)
				{
					throw new PluginException(this.masterFile.getName() + ": Group record prefix is too short");
				}

				buildGroup(in, prefix, this.formList);

				if (statusDialog != null)
				{
					int newProgress = (int) (in.getFilePointer() * 50L / fileSize);
					if (newProgress >= currentProgress + 5)
					{
						currentProgress = newProgress;
						statusDialog.updateProgress(currentProgress);
					}

				}

				if ((task != null) && (WorkerTask.interrupted()))
				{
					throw new InterruptedException("Request canceled");
				}

			}

			recordCount = this.formList.size();
			int processedCount = 0;
			this.formMap = new HashMap<Integer, FormInfo>(recordCount);
			byte[] buffer = new byte[256];

			System.arraycopy("INDX".getBytes(), 0, buffer, 0, 4);
			setInteger(5, buffer, 4);
			setInteger(recordCount, buffer, 8);
			setInteger((int) this.masterFile.length(), buffer, 12);
			setLong(this.masterFile.lastModified(), buffer, 16);
			out.write(buffer, 0, 24);

			deflater = new GZIPOutputStream(out);

			for (FormInfo info : this.formList)
			{
				int formID = info.getFormID();
				byte[] recordType = info.getRecordType().getBytes();
				byte[] editorID = info.getEditorID().getBytes();
				int parentFormID = info.getParentFormID();
				int position = ((Long) info.getSource()).intValue();

				int length = 16 + editorID.length + 1;
				if (length > buffer.length)
				{
					buffer = new byte[length];
				}
				setInteger(position, buffer, 0);
				setInteger(formID, buffer, 4);
				System.arraycopy(recordType, 0, buffer, 8, 4);
				setInteger(parentFormID, buffer, 12);
				if (editorID.length != 0)
					System.arraycopy(editorID, 0, buffer, 16, editorID.length);
				buffer[(16 + editorID.length)] = 0;
				deflater.write(buffer, 0, 16 + editorID.length + 1);

				if (formID >>> 24 > masterID)
				{
					formID = formID & 0xFFFFFF | masterID << 24;
				}
				this.formMap.put(new Integer(formID), info);

				processedCount++;
				if (statusDialog != null)
				{
					int newProgress = processedCount * 50 / recordCount + 50;
					if (newProgress >= currentProgress + 5)
					{
						currentProgress = newProgress;
						statusDialog.updateProgress(currentProgress);
					}

				}

			}

			completed = true;
		}
		finally
		{
			if (in != null)
			{
				in.close();
			}
			if (deflater != null)
				deflater.close();
			else if (out != null)
			{
				out.close();
			}
			if (!completed)
				indexFile.delete();
		}
	}

	private void buildGroup(RandomAccessFile in, byte[] prefix, List<FormInfo> formList) throws DataFormatException, InterruptedException,
			IOException, PluginException
	{
		int masterID = this.masterHeader.getMasterList().size();

		String recordType = new String(prefix, 0, 4);
		int groupLength = getInteger(prefix, 4);

		if (recordType.equals("TES4"))
		{
			in.skipBytes(groupLength);
			return;
		}

		if (!recordType.equals("GRUP"))
		{
			throw new PluginException(this.masterFile.getName() + ": Top-level record is not a group");
		}
		groupLength -= 20;
		long stopPosition = in.getFilePointer() + groupLength;
		long position;
		while ((position = in.getFilePointer()) < stopPosition)
		{

			int count = in.read(prefix);
			if (count != 20)
			{
				throw new PluginException(this.masterFile.getName() + ": Incomplete record prefix");
			}
			recordType = new String(prefix, 0, 4);
			int recordLength = getInteger(prefix, 4);

			if (recordType.equals("GRUP"))
			{
				in.skipBytes(recordLength - 20);
			}
			else if ((recordType.equals("CELL")) || (recordType.equals("DIAL")))
			{
				in.skipBytes(recordLength);
			}
			else
			{
				PluginRecord record = new PluginRecord(prefix);
				int formID = record.getFormID();
				if ((record.isDeleted()) || (record.isIgnored()) || (formID == 0))
				{
					in.skipBytes(recordLength);
				}
				else
				{
					record.load(this.masterFile, in, recordLength);
					FormInfo formInfo = new FormInfo(new Long(position), recordType, formID, record.getEditorID());
					formInfo.setPlugin(this);
					formList.add(formInfo);
				}
			}
		}
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.Master
 * JD-Core Version:    0.6.0
 */