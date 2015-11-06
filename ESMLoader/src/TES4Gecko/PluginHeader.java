package TES4Gecko;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

public class PluginHeader extends SerializedElement
{
	private File pluginFile;

	private float pluginVersion;

	private boolean master;

	private String creator;

	private String summary;

	private int recordCount;

	private List<String> masterList;

	public PluginHeader(File pluginFile)
	{
		this.pluginFile = pluginFile;
		this.pluginVersion = 0.8F;
		this.master = false;
		this.creator = "DEFAULT";
		this.summary = new String();
		this.masterList = new ArrayList<String>();
	}

	public float getVersion()
	{
		return this.pluginVersion;
	}

	public void setVersion(float version)
	{
		this.pluginVersion = version;
	}

	public boolean isMaster()
	{
		return this.master;
	}

	public void setMaster(boolean master)
	{
		this.master = master;
	}

	public String getCreator()
	{
		return this.creator;
	}

	public void setCreator(String creator)
	{
		this.creator = creator;
	}

	public String getSummary()
	{
		return this.summary;
	}

	public void setSummary(String summary)
	{
		this.summary = summary;
	}

	public int getRecordCount()
	{
		return this.recordCount;
	}

	public void setRecordCount(int recordCount)
	{
		this.recordCount = recordCount;
	}

	public List<String> getMasterList()
	{
		return this.masterList;
	}

	public void setMasterList(List<String> masterList)
	{
		this.masterList = masterList;
	}

	public void read(RandomAccessFile in) throws PluginException, IOException
	{
		byte[] prefix = new byte[20];
		byte[] buffer = new byte[1024];

		int count = in.read(prefix, 0, 20);
		if (count != 20)
		{
			throw new PluginException(this.pluginFile.getName() + ": File is not a TES4 file");
		}
		String type = new String(prefix, 0, 4);
		if (!type.equals("TES4"))
		{
			throw new PluginException(this.pluginFile.getName() + ": File is not a TES4 file");
		}
		if ((prefix[8] & 0x1) != 0)
			this.master = true;
		else
		{
			this.master = false;
		}
		int headerLength = getInteger(prefix, 4);

		while (headerLength >= 6)
		{
			count = in.read(prefix, 0, 6);
			if (count != 6)
			{
				throw new PluginException(this.pluginFile.getName() + ": Header subrecord prefix truncated");
			}
			headerLength -= 6;
			int length = getShort(prefix, 4);
			if (length > headerLength)
			{
				throw new PluginException(this.pluginFile.getName() + ": Subrecord length exceeds header length");
			}
			if (length > buffer.length)
			{
				buffer = new byte[length];
			}
			count = in.read(buffer, 0, length);
			if (count != length)
			{
				throw new PluginException(this.pluginFile.getName() + ": Header subrecord data truncated");
			}
			headerLength -= count;
			type = new String(prefix, 0, 4);
			if (type.equals("HEDR"))
			{
				if (length < 8)
				{
					throw new PluginException(this.pluginFile.getName() + ": HEDR subrecord is too small");
				}
				int pluginIntVersion = getInteger(buffer, 0);
				this.pluginVersion = Float.intBitsToFloat(pluginIntVersion);
				if (Main.debugMode)
				{
					System.out.printf("%s: Version %f\n", new Object[]
					{ this.pluginFile.getName(), Float.valueOf(this.pluginVersion) });
				}
				this.recordCount = getInteger(buffer, 4);
				if (Main.debugMode)
					System.out.printf("%s: %d records\n", new Object[]
					{ this.pluginFile.getName(), Integer.valueOf(this.recordCount) });
			}
			else if (type.equals("CNAM"))
			{
				if (length > 1)
					this.creator = new String(buffer, 0, length - 1);
			}
			else if (type.equals("SNAM"))
			{
				if (length > 1)
					this.summary = new String(buffer, 0, length - 1);
			}
			else
			{
				if ((!type.equals("MAST")) || (length <= 1))
					continue;
				this.masterList.add(new String(buffer, 0, length - 1));
			}
		}

		if (headerLength != 0)
			throw new PluginException(this.pluginFile.getName() + ": Header is incomplete");
	}

	public void write(FileOutputStream out) throws IOException
	{
		byte[] headerRecord = buildHeader();
		out.write(headerRecord);
	}

	public void write(RandomAccessFile out) throws IOException
	{
		byte[] headerRecord = buildHeader();
		out.write(headerRecord);
	}

	private byte[] buildHeader()
	{
		int pluginIntVersion = Float.floatToIntBits(this.pluginVersion);
		byte[] hedrSubrecord = new byte[18];
		System.arraycopy("HEDR".getBytes(), 0, hedrSubrecord, 0, 4);
		setShort(12, hedrSubrecord, 4);
		setInteger(pluginIntVersion, hedrSubrecord, 6);
		setInteger(this.recordCount, hedrSubrecord, 10);

		byte[] creatorBytes = this.creator.getBytes();
		int length = creatorBytes.length + 1;
		byte[] cnamSubrecord = new byte[6 + length];
		System.arraycopy("CNAM".getBytes(), 0, cnamSubrecord, 0, 4);
		setShort(length, cnamSubrecord, 4);
		if (length > 1)
			System.arraycopy(creatorBytes, 0, cnamSubrecord, 6, creatorBytes.length);
		cnamSubrecord[(6 + creatorBytes.length)] = 0;

		byte[] summaryBytes = this.summary.getBytes();
		length = summaryBytes.length + 1;
		byte[] snamSubrecord;
		if (length > 1)
		{
			snamSubrecord = new byte[6 + length];
			System.arraycopy("SNAM".getBytes(), 0, snamSubrecord, 0, 4);
			setShort(length, snamSubrecord, 4);
			System.arraycopy(summaryBytes, 0, snamSubrecord, 6, summaryBytes.length);
			snamSubrecord[(6 + summaryBytes.length)] = 0;
		}
		else
		{
			snamSubrecord = new byte[0];
		}

		byte[][] masterSubrecords = new byte[this.masterList.size()][];
		int count = 0;
		for (String master : this.masterList)
		{
			byte[] masterBytes = master.getBytes();
			length = masterBytes.length + 1;
			byte[] masterSubrecord = new byte[6 + length];
			System.arraycopy("MAST".getBytes(), 0, masterSubrecord, 0, 4);
			setShort(length, masterSubrecord, 4);
			if (length > 1)
				System.arraycopy(masterBytes, 0, masterSubrecord, 6, masterBytes.length);
			masterSubrecord[(6 + masterBytes.length)] = 0;
			masterSubrecords[(count++)] = masterSubrecord;
		}

		length = hedrSubrecord.length + cnamSubrecord.length + snamSubrecord.length;
		for (int i = 0; i < masterSubrecords.length; i++)
		{
			length += masterSubrecords[i].length + 14;
		}
		byte[] headerRecord = new byte[20 + length];
		System.arraycopy("TES4".getBytes(), 0, headerRecord, 0, 4);
		setInteger(length, headerRecord, 4);
		headerRecord[8] = (byte) (this.master ? 1 : 0);
		int offset = 20;
		System.arraycopy(hedrSubrecord, 0, headerRecord, offset, hedrSubrecord.length);
		offset += hedrSubrecord.length;
		System.arraycopy(cnamSubrecord, 0, headerRecord, offset, cnamSubrecord.length);
		offset += cnamSubrecord.length;

		if (snamSubrecord.length != 0)
		{
			System.arraycopy(snamSubrecord, 0, headerRecord, offset, snamSubrecord.length);
			offset += snamSubrecord.length;
		}

		for (int i = 0; i < masterSubrecords.length; i++)
		{
			System.arraycopy(masterSubrecords[i], 0, headerRecord, offset, masterSubrecords[i].length);
			offset += masterSubrecords[i].length;
			System.arraycopy("DATA".getBytes(), 0, headerRecord, offset, 4);
			headerRecord[(offset + 4)] = 8;
			offset += 14;
		}

		return headerRecord;
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.PluginHeader
 * JD-Core Version:    0.6.0
 */