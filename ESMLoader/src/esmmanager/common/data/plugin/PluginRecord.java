package esmmanager.common.data.plugin;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import esmmanager.common.PluginException;
import esmmanager.common.data.record.Record;
import esmmanager.common.data.record.Subrecord;
import esmmanager.loader.ESMManager;
import tools.io.ESMByteConvert;

public class PluginRecord extends Record
{
	protected int headerByteCount = -1;

	protected int unknownInt;

	protected String editorID = "";

	private PluginRecord parentRecord;

	protected byte[] recordData;

	protected long filePositionPointer = -1;

	//for tes3 version
	protected PluginRecord()
	{

	}

	/**
	 * prefix MUST have length of  either 20 (oblivion) or 24 (fallout) headerbyte count
	 * @param prefix
	 */
	public PluginRecord(byte prefix[])
	{
		if (prefix.length != 20 && prefix.length != 24)
		{
			throw new IllegalArgumentException("The record prefix is not 20 or 24 bytes as required");
		}
		else
		{
			headerByteCount = prefix.length;
			recordType = new String(prefix, 0, 4);
			formID = ESMByteConvert.extractInt(prefix, 12);
			recordFlags1 = ESMByteConvert.extractInt(prefix, 8);
			recordFlags2 = ESMByteConvert.extractInt(prefix, 16);
			if (prefix.length == 24)
				unknownInt = ESMByteConvert.extractInt(prefix, 20);
		}
	}

	/**
	 * For forcibly creating these things not from file	 
	 */
	public PluginRecord(int headerByteCount, String recordType, int formID, String editorID)
	{
		this.headerByteCount = headerByteCount;
		this.recordType = recordType;
		this.formID = formID;
		this.recordFlags1 = 0;
		this.recordFlags2 = 0;
		this.editorID = editorID;
		subrecordList = new ArrayList<Subrecord>();
	}

	protected PluginRecord(String recordType, byte prefix[])
	{
		this.recordType = recordType;
		if (prefix.length != 20 && prefix.length != 24)
		{
			throw new IllegalArgumentException("The record prefix is not 20 or 24 bytes as required");
		}
		else
		{
			headerByteCount = prefix.length;
		}
	}

	public PluginRecord getParent()
	{
		return parentRecord;
	}

	public void setParent(PluginRecord parent)
	{
		parentRecord = parent;
	}

	//TODO: If I can avoid this except for display activity then I can keep data compressed for a long time,
	//save space save load time
	public String getEditorID()
	{
		return editorID;
	}

	//Dear god this String fileName appears to do something magical without it failures!	
	public void load(String fileName, RandomAccessFile in, int recordLength) throws PluginException, IOException, DataFormatException
	{
		filePositionPointer = in.getFilePointer();
		int offset = 0;
		int overrideLength = 0;
		recordData = new byte[recordLength];

		if (ESMManager.USE_MINI_CHANNEL_MAPS && filePositionPointer < Integer.MAX_VALUE)
		{
			//Oddly this is hugely slow
			FileChannel.MapMode mm = FileChannel.MapMode.READ_ONLY;
			FileChannel ch = in.getChannel();
			MappedByteBuffer mappedByteBuffer = ch.map(mm, filePositionPointer, recordLength);
			mappedByteBuffer.get(recordData, 0, recordLength);

			//manually move the pointer forward (someone else is readin from this file)
			in.seek(filePositionPointer + recordLength);
		}
		else
		{
			synchronized (in)
			{
				int count = in.read(recordData);
				if (count != recordLength)
					throw new PluginException(fileName + ": " + recordType + " record is incomplete");
			}
		}

		// now uncompress the recordData
		uncompressRecordData();

		int dataLength = recordData.length;
		while (dataLength >= 6)
		{
			String subrecordType = new String(recordData, offset, 4);

			int length = ESMByteConvert.extractShort(recordData, offset + 4);
			if (length == 0)
			{
				length = overrideLength;
				overrideLength = 0;
			}

			if (length > dataLength)
				throw new PluginException(fileName + ": " + subrecordType + " subrecord is incomplete");

			if (length > 0)
			{
				if (subrecordType.equals("XXXX"))
				{
					if (length != 4)
					{
						throw new PluginException(fileName + ": XXXX subrecord data length is not 4");
					}
					overrideLength = ESMByteConvert.extractInt(recordData, offset + 6);
				}
				else if (subrecordType.equals("EDID") && length > 1)
				{
					editorID = new String(recordData, offset + 6, length - 1);
				}
			}
			offset += 6 + length;
			dataLength -= 6 + length;
		}

		if (dataLength != 0)
			throw new PluginException(fileName + ": " + recordType + " record is incomplete");
		else
			return;
	}

	public byte[] getRecordData()
	{
		return recordData;
	}

	//TODO: I only need to uncompress so I can find the EDID in the load above
	// if I didn't need that (and I don't generally) I could leave these bytes compressed until requested
	// by the load statement below
	private void uncompressRecordData() throws DataFormatException, PluginException
	{
		if (!isCompressed())
			return;
		if (recordData.length < 5 || recordData[3] >= 32)
			throw new PluginException("Compressed data prefix is not valid");
		int length = ESMByteConvert.extractInt(recordData, 0);
		byte buffer[] = new byte[length];

		if (ESMManager.USE_NON_NATIVE_ZIP)
		{
			//JCraft version slower - though I wonder about android? seems real slow too
			com.jcraft.jzlib.Inflater inflater = new com.jcraft.jzlib.Inflater();
			inflater.setInput(recordData, 4, recordData.length - 4, false);
			inflater.setOutput(buffer);
			inflater.inflate(4);//Z_FINISH
			inflater.end();
			recordData = buffer;
		}
		else
		{
			Inflater expand = new Inflater();
			expand.setInput(recordData, 4, recordData.length - 4);
			int count = expand.inflate(buffer);
			if (count != length)
			{
				throw new PluginException("Expanded data less than data length");
			}
			else
			{
				expand.end();
				recordData = buffer;
			}
		}

	}

	@Override
	public List<Subrecord> getSubrecords()
	{
		// must fill it up before anyone can get it asynch!
		synchronized (this)
		{
			if (subrecordList == null)
			{
				loadSubRecords();
			}
			return subrecordList;
		}
	}

	/**
	 * pulls the sub records from the raw byte array if required, and dumps the bytes
	 */
	private void loadSubRecords()
	{
		synchronized (this)
		{
			if (subrecordList == null)
			{
				subrecordList = new ArrayList<Subrecord>();
				int offset = 0;
				int overrideLength = 0;

				byte[] rd = getRecordData();
				if (rd != null)
				{
					while (offset < rd.length)
					{
						String subrecordType = new String(rd, offset, 4);
						int subrecordLength = rd[offset + 4] & 0xff | (rd[offset + 5] & 0xff) << 8;
						if (subrecordType.equals("XXXX"))
						{
							overrideLength = ESMByteConvert.extractInt(rd, offset + 6);
							offset += 6 + 4;
							continue;
						}
						if (subrecordLength == 0)
						{
							subrecordLength = overrideLength;
							overrideLength = 0;
						}
						byte subrecordData[] = new byte[subrecordLength];

						// bad decompress can happen (LAND record in falloutNV)
						if (offset + 6 + subrecordLength <= rd.length)
							System.arraycopy(rd, offset + 6, subrecordData, 0, subrecordLength);

						subrecordList.add(new PluginSubrecord(recordType, subrecordType, subrecordData));

						offset += 6 + subrecordLength;
					}
				}
				//TODO: can I discard the raw data now? does this improve memory usage at all? 
				recordData = null;
			}
		}

	}

	public String toString()
	{
		String text = "" + recordType + " record: " + editorID + " (" + formID + ")";
		if (isIgnored())
		{
			text = "(Ignore) " + text;
		}
		else if (isDeleted())
		{
			text = "(Deleted) " + text;
		}
		return text;
	}

	public long getFilePositionPointer()
	{
		return filePositionPointer;
	}

	public int getUnknownInt()
	{
		return unknownInt;
	}

}
