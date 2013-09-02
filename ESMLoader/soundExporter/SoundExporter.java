package soundExporter;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;

import esmLoader.EsmFileLocations;
import esmLoader.common.data.record.Record;
import esmLoader.common.data.record.Subrecord;
import esmLoader.loader.ESMManager;

public class SoundExporter
{

	public static DefaultMutableTreeNode root = new DefaultMutableTreeNode();

	public static JTree tree = new JTree(root);

	public static ArrayList<Record> allSoundRecords = new ArrayList<Record>();

	public static void main(String args[])
	{

		String esmFile = EsmFileLocations.getGeneralEsmFile();

		System.out.println("loading file " + esmFile);
		long start = System.currentTimeMillis();

		try
		{
			Thread.currentThread().setPriority(4);
			ESMManager esmManager = ESMManager.getESMManager(esmFile);
			Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
			System.out.println("Done in " + (System.currentTimeMillis() - start) + " analyzing...");

			BufferedWriter outputFile = new BufferedWriter(new FileWriter(esmFile + "Sounds.txt"));
			exportSounds(esmManager, outputFile);
			outputFile.flush();
			outputFile.close();

		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		System.out.println("Done");
	}

	private static void exportSounds(ESMManager esmManager, BufferedWriter outputFile) throws IOException
	{
		int idx = 0;
		for (Integer formId : esmManager.getTypeToFormIdMap().get("SOUN"))
		{
			Record rec = esmManager.getRecord(formId);
			if (rec.getRecordType().equals("SOUN"))
			{
				String out = idx + ":" + rec.getFormID();
				for (Subrecord sub : rec.getSubrecords())
				{
					if (sub.getSubrecordType().equals("EDID"))
					{
						out += ":" + new String(sub.getSubrecordData()).trim();
					}
					else if (sub.getSubrecordType().equals("FNAM"))// If not exact name presumably a random sound is selected, dictated by SNDX data
					{
						out += ":" + new String(sub.getSubrecordData()).trim();
					}
					else if (sub.getSubrecordType().equals("SNDX"))//12 bytes
					{
						// do nothing, sexy sound info see SNDX in esmj3dfo3 subrecords
					}
					else if (sub.getSubrecordType().equals("SNDD"))//36 bytes
					{
						// no idea
					}
					else if (sub.getSubrecordType().equals("OBND"))//12 bytes
					{
						// bounds info
					}
					else if (sub.getSubrecordType().equals("ANAM"))//10 bytes
					{

					}
					else if (sub.getSubrecordType().equals("GNAM"))// 2 bytes
					{

					}
					else if (sub.getSubrecordType().equals("HNAM"))// 4 bytes
					{

					}
				}

				outputFile.write(out);
				outputFile.newLine();
				idx++;
			}
		}

	}
}