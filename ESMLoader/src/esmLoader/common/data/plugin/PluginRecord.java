package esmLoader.common.data.plugin;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import tools.io.ESMByteConvert;
import esmLoader.common.PluginException;

public class PluginRecord
{
	protected int headerByteCount = -1;

	private String recordType;

	private int recordFlags1;

	private int recordFlags2;

	private int unknownInt;

	private int formID;

	private String editorID = "";

	private PluginRecord parentRecord;

	private byte[] recordData;

	private long filePositionPointer = -1;

	private List<PluginSubrecord> subrecordList;

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
	 * For forcably creating these things not from file	 
	 */
	public PluginRecord(int headerByteCount, String recordType, int formID, String editorID)
	{
		this.headerByteCount = headerByteCount;
		this.recordType = recordType;
		this.formID = formID;
		this.recordFlags1 = 0;
		this.recordFlags2 = 0;
		this.editorID = editorID;
		subrecordList = new ArrayList<PluginSubrecord>();
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

	public boolean isDeleted()
	{
		return (recordFlags1 & 0x20) != 0;
	}

	public boolean isIgnored()
	{
		return (recordFlags1 & 0x1000) != 0;
	}

	public boolean isCompressed()
	{
		return (recordFlags1 & 0x40000) != 0;
	}

	public String getRecordType()
	{
		return recordType;
	}

	public int getFormID()
	{
		return formID;
	}

	public String getEditorID()
	{
		return editorID;
	}

	public void load(String fileName, RandomAccessFile in, int recordLength) throws PluginException, IOException, DataFormatException
	{
		filePositionPointer = in.getFilePointer();
		int offset = 0;
		int overrideLength = 0;
		recordData = new byte[recordLength];

		int count = in.read(recordData);
		if (count != recordLength)
			throw new PluginException(fileName + ": " + recordType + " record is incomplete");

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

	private void uncompressRecordData() throws DataFormatException, PluginException
	{
		if (!isCompressed())
			return;
		if (recordData.length < 5 || recordData[3] >= 32)
			throw new PluginException("Compressed data prefix is not valid");
		int length = ESMByteConvert.extractInt(recordData, 0);
		byte buffer[] = new byte[length];
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

	public List<PluginSubrecord> getSubrecords()
	{
		// must fill it up before anyone can get it asynch!
		synchronized (this)
		{
			if (subrecordList == null)
			{

				subrecordList = new ArrayList<PluginSubrecord>();
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
				recordData=null;
			}
			return subrecordList;
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

	public void displaySubs()
	{
		List<PluginSubrecord> subrecords = getSubrecords();

		for (PluginSubrecord subrec : subrecords)
		{
			System.out.println("subrec " + subrec);
		}
	}

	public long getFilePositionPointer()
	{
		return filePositionPointer;
	}

	public int getRecordFlags1()
	{
		return recordFlags1;
	}

	public int getRecordFlags2()
	{
		return recordFlags2;
	}

	public int getUnknownInt()
	{
		return unknownInt;
	}

}
