package esmLoader.tes3;

import java.io.IOException;
import java.io.RandomAccessFile;

import tools.io.ESMByteConvert;
import esmLoader.common.PluginException;

public class PluginHeader extends PluginRecord
{

	private String pluginFileName;

	private float pluginVersion = -1;

	private boolean master = false;

	private String creator = "";

	private String summary = "";

	private int numRecords = 0;

	public PluginHeader()
	{
		super(-1);
	}

	public void load(String fileName, RandomAccessFile in) throws PluginException, IOException
	{
		super.load(fileName, in);
		pluginFileName = fileName;
		for (esmLoader.common.data.plugin.PluginSubrecord sub : getSubrecords())
		{
			if (sub.getSubrecordType().equals("HEDR"))
			{
				// 4 bytes, float Version (1.2)
				pluginVersion = ESMByteConvert.extractFloat(sub.getSubrecordData(), 0);
				// 4 bytes, long Unknown (1)
				// 32 Bytes, Company Name string
				creator = new String(sub.getSubrecordData(), 8, 32);
				// 256 Bytes, ESM file description?
				summary = new String(sub.getSubrecordData(), 40, 256);
				// 4 bytes, long NumRecords (48227)
				numRecords = ESMByteConvert.extractInt(sub.getSubrecordData(), 296);
			}
			else if (sub.getSubrecordType().equals("MAST"))
			{
				/*
				 * MAST = string, variable length Only found in ESP plugins and
				 * specifies a master file that the plugin requires. Can occur
				 * multiple times. Usually found just after the TES3 record.
				 */
			}
			else if (sub.getSubrecordType().equals("DATA"))
			{
				/*
				 * DATA = 8 Bytes long64 MasterSize Size of the previous master
				 * file in bytes (used for version tracking of plugin). The MAST
				 * and DATA records are always found together, the DATA
				 * following the MAST record that it refers to.
				 */
			}
			else
			{
				throw new PluginException("Unknown subrecord in TES3 Record " + sub.getRecordType());
			}
		}
	}

	public String getPluginFileName()
	{
		return pluginFileName;
	}

	public float getPluginVersion()
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

	public int getNumRecords()
	{
		return numRecords;
	}
}
