package esmLoader.tes3;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.zip.DataFormatException;

import javax.swing.JFrame;

import display.PluginDisplayDialog;
import esmLoader.EsmFileLocations;
import esmLoader.common.PluginException;
import esmLoader.common.data.plugin.Plugin;

/**
 * This is a copy of the master file in data package, however it holds onto a
 * copy of all loaded data for everything other than the WRLD and CELL values,
 * which is simply indexes down to the subblock level
 *
 * @author Administrator
 *
 */
public class Master
{
	private static int headerByteCount = -1;

	private File masterFile;

	private RandomAccessFile in;

	private PluginHeader masterHeader;

	private int masterID = 0;
	private ArrayList<PluginRecord> records = new ArrayList<PluginRecord>();

	public Master(File masterFile)
	{
		this.masterFile = masterFile;
		masterHeader = new PluginHeader();
	}

	public PluginHeader getMasterHeader()
	{
		return masterHeader;
	}

	public ArrayList<PluginRecord> getRecords()
	{
		return records;
	}

	public synchronized void load() throws PluginException, DataFormatException, IOException
	{
		if (!masterFile.exists() || !masterFile.isFile())
			throw new IOException("Master file '" + masterFile.getAbsolutePath() + "' does not exist");

		in = new RandomAccessFile(masterFile, "r");

		masterHeader.load(masterFile.getName(), in);

		while (in.getFilePointer() < in.length())
		{
			PluginRecord r = new PluginRecord();
			r.load(masterFile.getName(), in);
			records.add(r);
		}

	}

	public static void main(String[] args)
	{
		String generalEsmFile = EsmFileLocations.getGeneralEsmFile();
		
		System.out.println("loading file " + generalEsmFile);

		File pluginFile = new File(generalEsmFile);
		Master plugin = new Master(pluginFile);
		try
		{
			plugin.load();

			for(PluginRecord r: plugin.getRecords())
			{
				System.out.println(""+r);
				for(PluginSubrecord sr: r.getSubrecords())
				{
					System.out.println("\t"+sr);
				}
			}
			System.out.println("done");
		}
		catch (PluginException e)
		{
			e.printStackTrace();
		}
		catch (DataFormatException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

}
