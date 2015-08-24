package esmLoader.tes3;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import tools.io.ESMByteConvert;
import esmLoader.common.PluginException;
import esmLoader.common.data.plugin.PluginSubrecord;

public class PluginRecord extends esmLoader.common.data.plugin.PluginRecord
{
	private String recordType;

	private int recordSize;

	private int unknownInt;

	private int recordFlags;

	private byte[] recordData;

	private long filePositionPointer = -1;

	private List<PluginSubrecord> subrecordList;

	public PluginRecord()
	{
	}

	@Override
	public boolean isDeleted()
	{
		return (recordFlags & 0x20) != 0;
	}

	@Override
	public boolean isIgnored()
	{
		return (recordFlags & 0x1000) != 0;
	}

	@Override
	public String getRecordType()
	{
		return recordType;
	}

	public void load(String fileName, RandomAccessFile in) throws PluginException, IOException
	{
		filePositionPointer = in.getFilePointer();
		byte[] prefix = new byte[16];
		int count = in.read(prefix);
		if (count != 16)
			throw new PluginException(fileName + ": record prefix is incomplete");

		recordType = new String(prefix, 0, 4);
		recordSize = ESMByteConvert.extractInt(prefix, 4);
		unknownInt = ESMByteConvert.extractInt(prefix, 8);
		recordFlags = ESMByteConvert.extractInt(prefix, 12);

		recordData = new byte[recordSize];

		count = in.read(recordData);
		if (count != recordSize)
			throw new PluginException(fileName + ": " + recordType + " record bad length, asked for " + recordSize + " got " + count);

	}

	@Override
	public byte[] getRecordData()
	{
		return recordData;
	}

	@Override
	public List<PluginSubrecord> getSubrecords()
	{
		// must fill it up before anyone can get it asynch!
		synchronized (this)
		{
			if (subrecordList == null)
			{
				subrecordList = new ArrayList<PluginSubrecord>();
				int offset = 0;

				byte[] rd = getRecordData();
				if (rd != null)
				{
					while (offset < rd.length)
					{
						String subrecordType = new String(rd, offset + 0, 4);
						int subrecordLength = ESMByteConvert.extractInt(rd, offset + 4);
						byte subrecordData[] = new byte[subrecordLength];
						System.arraycopy(rd, 8, subrecordData, 0, subrecordLength);

						subrecordList.add(new PluginSubrecord(recordType, subrecordType, subrecordData));

						offset += 8 + subrecordLength;
					}
				}
				// TODO: can I discard the raw data now?
			}
			return subrecordList;
		}
	}

	@Override
	public long getFilePositionPointer()
	{
		return filePositionPointer;
	}

	public int getRecordFlags()
	{
		return recordFlags;
	}

	@Override
	public int getUnknownInt()
	{
		return unknownInt;
	}

	public int getFormID()
	{
		throw new UnsupportedOperationException();
	}

	public String getEditorID()
	{
		throw new UnsupportedOperationException();
	}

	public int getRecordFlags1()
	{
		throw new UnsupportedOperationException();
	}

	public int getRecordFlags2()
	{
		throw new UnsupportedOperationException();
	}

}
