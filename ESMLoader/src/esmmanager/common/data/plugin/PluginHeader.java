package esmmanager.common.data.plugin;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import esmmanager.common.PluginException;
import tools.io.ESMByteConvert;

public class PluginHeader
{

	private String pluginFileName;

	private float pluginVersion = -1;

	private boolean master = false;

	private String creator = "";

	private String summary = "";

	private int recordCount = 0;

	private List<String> masterList = new ArrayList<String>();

	private int headerByteCount = -1;

	public PluginHeader(String pluginFileName)
	{
		this.pluginFileName = pluginFileName;
	}

	public String getName()
	{
		return pluginFileName;
	}

	public float getVersion()
	{
		return pluginVersion;
	}

	public boolean isMaster()
	{
		return master;
	}

	public String getCreator()
	{
		return creator;
	}

	public String getSummary()
	{
		return summary;
	}

	public int getRecordCount()
	{
		return recordCount;
	}

	public List<String> getMasterList()
	{
		return masterList;
	}

	public int getHeaderByteCount()
	{
		return headerByteCount;
	}

	public void read(RandomAccessFile in) throws PluginException, IOException
	{
		long fp = in.getFilePointer();
		// check to see if a redundant 4 bytes is in the file or are we are teh HDR record now
		byte[] tmp = new byte[4];
		in.seek(fp + 20);
		in.read(tmp);
		if (new String(tmp).equals("HEDR"))
		{
			// it is indeed the HEDR record so we want a lenght of 20
			headerByteCount = 20;
		}
		else
		{
			// we need to skip 24 bytes in the header
			headerByteCount = 24;
		}
		in.seek(fp);

		byte prefix[] = new byte[headerByteCount];
		int count = in.read(prefix, 0, headerByteCount);

		if (count != headerByteCount)
			throw new PluginException(pluginFileName + ": File is not a TES4 file");
		String type = new String(prefix, 0, 4);

		if (!type.equals("TES4") && !type.equals("TES3"))
			throw new PluginException(pluginFileName + ": File is not a TES4 file (" + type + ")");

		if ((prefix[8] & 1) != 0)
			master = true;
		else
			master = false;
		int headerLength = ESMByteConvert.extractInt(prefix, 4);

		if (type.equals("TES3"))
		{
			readTes3(in, headerLength);
			return;
		}

		byte buffer[] = new byte[1024];
		do
		{
			if (headerLength < 6)
				break;

			byte[] recordHeader = new byte[6];
			count = in.read(recordHeader);

			if (count != 6)
				throw new PluginException(pluginFileName + ": Header subrecord prefix truncated");
			headerLength -= 6;

			int length = ESMByteConvert.extractShort(recordHeader, 4);

			if (length > headerLength)
				throw new PluginException(pluginFileName + ": Subrecord length exceeds header length");
			if (length > buffer.length)
				buffer = new byte[length];

			count = in.read(buffer, 0, length);
			if (count != length)
				throw new PluginException(pluginFileName + ": Header subrecord data truncated");
			headerLength -= count;

			type = new String(recordHeader, 0, 4);

			if (type.equals("HEDR"))
			{
				if (length < 8)
					throw new PluginException(pluginFileName + ": HEDR subrecord is too small");

				pluginVersion = Float.intBitsToFloat(ESMByteConvert.extractInt(buffer, 0));
				recordCount = ESMByteConvert.extractInt(buffer, 4);
			}
			else if (type.equals("CNAM"))
			{
				if (length > 1)
					creator = new String(buffer, 0, length - 1);
			}
			else if (type.equals("SNAM"))
			{
				if (length > 1)
					summary = new String(buffer, 0, length - 1);
			}
			else if (type.equals("OFST"))
			{
				// what is this one?				 
			}
			else if (type.equals("DELE"))
			{
				// what is this one?
			}
			else if (type.equals("MAST") && length > 1)
			{
				masterList.add(new String(buffer, 0, length - 1));
				//System.out.println("MAST " + new String(buffer, 0, length - 1));
			}
		}
		while (true);

		if (headerLength != 0)
			throw new PluginException(pluginFileName + ": Header is incomplete");
		else
			return;
	}

	// need a new format for records and sub records as the intro data is slightly different
	
	//http://www.uesp.net/morrow/tech/mw_esm.txt
	//http://www.uesp.net/wiki/Tes4Mod:Mod_File_Format
	private void readTes3(RandomAccessFile in, int headerLength) throws PluginException, IOException
	{
		byte buffer[] = new byte[1024];
		do
		{
			if (headerLength < 16)
				break;

			byte[] recordHeader = new byte[16];
			int count = in.read(recordHeader);

			if (count != 16)
				throw new PluginException(pluginFileName + ": Header subrecord prefix truncated");

			int length = ESMByteConvert.extractShort(recordHeader, 4);

			if (length > headerLength)
				throw new PluginException(pluginFileName + ": Subrecord length exceeds header length");
			if (length > buffer.length)
				buffer = new byte[length];

			count = in.read(buffer, 0, length);
			if (count != length)
				throw new PluginException(pluginFileName + ": Header subrecord data truncated");
			headerLength -= count;

			String type = new String(recordHeader, 0, 4);

			if (type.equals("HEDR"))
			{
				if (length < 8)
					throw new PluginException(pluginFileName + ": HEDR subrecord is too small");

				pluginVersion = Float.intBitsToFloat(ESMByteConvert.extractInt(buffer, 0));
				recordCount = ESMByteConvert.extractInt(buffer, 4);
			}
			else if (type.equals("CNAM"))
			{
				if (length > 1)
					creator = new String(buffer, 0, length - 1);
			}
			else if (type.equals("SNAM"))
			{
				if (length > 1)
					summary = new String(buffer, 0, length - 1);
			}
			else if (type.equals("OFST"))
			{
				// what is this one?				 
			}
			else if (type.equals("DELE"))
			{
				// what is this one?
			}
			else if (type.equals("MAST") && length > 1)
			{
				masterList.add(new String(buffer, 0, length - 1));
				//System.out.println("MAST " + new String(buffer, 0, length - 1));
			}
		}
		while (true);

		if (headerLength != 0)
			throw new PluginException(pluginFileName + ": Header is incomplete");
		else
			return;

	}
}
