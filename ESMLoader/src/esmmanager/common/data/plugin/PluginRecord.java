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
	public PluginRecord(int headerByteCount, String recordType, int formID)
	{
		this.headerByteCount = headerByteCount;
		this.recordType = recordType;
		this.formID = formID;
		this.recordFlags1 = 0;
		this.recordFlags2 = 0;
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

	public String getEditorID()
	{
		for (Subrecord sr : getSubrecords())
		{
			if (sr.getSubrecordType().equals("EDID"))
				return new String(sr.getSubrecordData(), 0, sr.getSubrecordData().length - 1);
		}
		return "";
	}

	//Dear god this String fileName appears to do something magical without it failures!	
	public void load(String fileName, RandomAccessFile in, int recordLength) throws PluginException, IOException, DataFormatException
	{
		filePositionPointer = in.getFilePointer();

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
	}

	private boolean uncompressRecordData() throws DataFormatException, PluginException
	{
		if (!isCompressed())
			return false;
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

		return true;
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
				try
				{
					uncompressRecordData();

					int offset = 0;
					int overrideLength = 0;

					if (recordData != null)
					{
						while (offset < recordData.length)
						{
							String subrecordType = new String(recordData, offset, 4);
							int subrecordLength = recordData[offset + 4] & 0xff | (recordData[offset + 5] & 0xff) << 8;
							if (subrecordType.equals("XXXX"))
							{
								overrideLength = ESMByteConvert.extractInt(recordData, offset + 6);
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
							if (offset + 6 + subrecordLength <= recordData.length)
								System.arraycopy(recordData, offset + 6, subrecordData, 0, subrecordLength);

							subrecordList.add(new PluginSubrecord(recordType, subrecordType, subrecordData));

							offset += 6 + subrecordLength;
						}
					}

					// discard the raw data as used now
					recordData = null;
				}
				catch (DataFormatException e)
				{
					e.printStackTrace();
				}
				catch (PluginException e)
				{
					e.printStackTrace();
				}
			}
		}

	}

	public String toString()
	{
		String text = "" + recordType + " record: " + getEditorID() + " (" + formID + ")";
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
