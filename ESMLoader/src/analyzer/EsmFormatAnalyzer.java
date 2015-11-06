package analyzer;

import java.awt.GridLayout;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;

import esmmanager.EsmFileLocations;
import esmmanager.common.PluginException;
import esmmanager.common.data.plugin.PluginGroup;
import esmmanager.common.data.plugin.PluginRecord;
import esmmanager.common.data.record.Record;
import esmmanager.loader.CELLPointer;
import esmmanager.loader.ESMManager;
import esmmanager.loader.IESMManager;
import esmmanager.loader.InteriorCELLTopGroup;
import esmmanager.loader.WRLDTopGroup;

public class EsmFormatAnalyzer
{

	public static final boolean ANALYZE_CELLS = true;

	public static final boolean OUPUT_SUBREC_CODE = false;

	public static final boolean LOAD_J3DCELLS = false;

	public static final boolean LOAD_BSA_FILES = false;

	public static RecordLoader recordLoader = null;

	public static DefaultMutableTreeNode root;

	public static JTree tree;

	public static HashSet<Integer> recordsDone = new HashSet<Integer>();

	public static RecordStatsList recordStatsList = new RecordStatsList();

	// for all subs to be analyzed together, each record has it's own list as well
	public static SubrecordStatsList allSubrecordStatsList = new SubrecordStatsList();

	//	public static ArrayList<Record> allRecords = new ArrayList<Record>();

	public static int maxFormId = 0;

	public static void main(String args[])
	{

		String generalEsmFile = EsmFileLocations.getGeneralEsmFile();

		System.out.println("loading file " + generalEsmFile);
		long start = System.currentTimeMillis();

		try
		{
			Thread.currentThread().setPriority(4);
			IESMManager esmManager = ESMManager.getESMManager(generalEsmFile);
			Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
			System.out.println("Done in " + (System.currentTimeMillis() - start) + " analyzing...");

			analyze(esmManager);
			System.out.println("done analyzing");

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

	public static void analyze(IESMManager esmManager) throws DataFormatException, IOException, PluginException
	{

		int c = esmManager.getAllFormIds().size();
		System.out.println("master getAllFormIds count = " + c);
		for (Integer formId : esmManager.getAllFormIds())
		{
			Record rec = esmManager.getRecord(formId);
			recordStatsList.applyRecord(rec, false, false, allSubrecordStatsList);

			maxFormId = formId > maxFormId ? formId : maxFormId;

			if (c % 1000 == 0)
				System.out.println("analyzed " + c);

			c++;
		}
		if (ANALYZE_CELLS)
		{
			for (InteriorCELLTopGroup interiorCELLTopGroup : esmManager.getInteriorCELLTopGroups())
			{
				c = interiorCELLTopGroup.interiorCELLByFormId.values().size();
				System.out.println("interiorCELLTopGroup.interiorCELLByFormId.values() count = " + c);
				for (CELLPointer cp : interiorCELLTopGroup.interiorCELLByFormId.values())
				{
					PluginRecord pr2 = esmManager.getInteriorCELL(cp.formId);
					applyRecord(pr2, true, false);
					 
					PluginGroup cellChildren = esmManager.getInteriorCELLChildren(cp.formId);
					if (cellChildren != null)
					{
						for (PluginRecord pgs : cellChildren.getRecordList())
						{
							// children are in groups of temp and persist (and dist)
							for (PluginRecord pgr : ((PluginGroup) pgs).getRecordList())
							{
								applyRecord(pgr, true, false);
								if (pgr instanceof PluginGroup)
								{
									for (PluginRecord pr : ((PluginGroup) pgr).getRecordList())
									{
										applyRecord(pr, true, false);
									}
								}
							}
						}
					}

					if (c % 1000 == 0)
						System.out.println("analyzed " + c);

					c++;
				}

			}

			for (WRLDTopGroup wRLDTopGroup : esmManager.getWRLDTopGroups())
			{
				c = wRLDTopGroup.WRLDExtBlockCELLByFormId.values().size();
				System.out.println("wRLDTopGroup.WRLDExtBlockCELLByFormId.values() count = " + c);
				for (CELLPointer cp : wRLDTopGroup.WRLDExtBlockCELLByFormId.values())
				{
					PluginRecord pr2 = esmManager.getWRLDExtBlockCELL(cp.formId);
					applyRecord(pr2, false, true);

					PluginGroup cellChildren = esmManager.getWRLDExtBlockCELLChildren(cp.formId);
					if (cellChildren != null)
					{
						for (PluginRecord pgs : cellChildren.getRecordList())
						{
							// children are in groups of temp and persist (and dist)
							for (PluginRecord pgr : ((PluginGroup) pgs).getRecordList())
							{
								applyRecord(pgr, true, false);
								if (pgr instanceof PluginGroup)
								{
									for (PluginRecord pr : ((PluginGroup) pgr).getRecordList())
									{
										applyRecord(pr, false, true);
									}
								}
							}
						}
					}
					if (c % 1000 == 0)
						System.out.println("analyzed " + c);

					c++;
				}

			}
		}
		printoutStats(esmManager);
	}

	private static void printoutStats(IESMManager esmManager)
	{
		root = new DefaultMutableTreeNode(esmManager.getName());
		tree = new JTree(root);
		System.out.println("Stats " + recordStatsList.size());
		Map<String, RecordStats> sortedRecsMap = getSortedRecsMap();
		for (RecordStats rs : sortedRecsMap.values())
		{
			String desc = PluginGroup.typeMap.get(rs.type);
			String r = "" + rs.type + " n=" + rs.count + " " + (rs.appearsInIntCELL ? "int" : "") + " "
					+ (rs.appearsInExtCELL ? "ext" : "");
			r += " (" + desc + ")";
			DefaultMutableTreeNode recNode = new DefaultMutableTreeNode(r);
			root.add(recNode);
			System.out.println(r);

			for (SubrecordStats srs : rs.subrecordStatsList.values())
			{
				String sr = "\t" + srs.subrecordType + " n=" + srs.count + (srs.count == rs.count ? " M" : "") + " " + srs.minLength + "-"
						+ srs.maxLength + " " + (srs.isString ? "isString" : "");

				if (srs.hasOrderOf.size() == 1)
				{
					sr += " FixedOrd: " + srs.hasOrderOf.iterator().next();
				}
				else if (srs.hasOrderOf.size() > 10)
				{
					sr += " " + srs.hasOrderOf.size() + " different orderNos";
				}
				else
				{
					sr += " Ords:";
					for (Integer inOrd : srs.hasOrderOf)
					{
						sr += " " + inOrd;
					}
				}

				DefaultMutableTreeNode subrecNode = new DefaultMutableTreeNode(sr);
				recNode.add(subrecNode);
				System.out.println(sr);
			}
		}

		System.out.println("");
		System.out.println("");
		System.out.println("ALL SUBS LIST");
		DefaultMutableTreeNode asNode = new DefaultMutableTreeNode("ALL SUBS LIST");
		root.add(asNode);

		DefaultMutableTreeNode stringfsubsNode = new DefaultMutableTreeNode("A String format");
		asNode.add(stringfsubsNode);
		System.out.println("A String format");

		Map<String, SubrecordStats> sortedSubsMap = getSortedSubsMap();

		for (SubrecordStats srs : sortedSubsMap.values())
		{
			if (srs.isString)
			{
				sopAndTree(srs, stringfsubsNode);
			}
		}

		DefaultMutableTreeNode fixedsubsNode = new DefaultMutableTreeNode("Fixed format");
		asNode.add(fixedsubsNode);
		System.out.println("Fixed format");
		for (SubrecordStats srs : sortedSubsMap.values())
		{
			if (srs.minLength == srs.maxLength && !srs.isString)
			{
				sopAndTree(srs, fixedsubsNode);
			}
		}

		DefaultMutableTreeNode varsubsNode = new DefaultMutableTreeNode("Variable format");
		asNode.add(varsubsNode);
		System.out.println("Variable format");
		for (SubrecordStats srs : sortedSubsMap.values())
		{
			if (srs.minLength != srs.maxLength && !srs.isString)
			{
				sopAndTree(srs, varsubsNode);
			}
		}

		if (OUPUT_SUBREC_CODE)
		{
			for (RecordStats rs : sortedRecsMap.values())
			{
				String desc = PluginGroup.typeMap.get(rs.type);
				System.out.println("" + rs.type + " n=" + rs.count + " (" + desc + ")");

				System.out.println("if (sr.getSubrecordType().equals(\"EDID\"))");
				System.out.println("{");
				System.out.println("EDID = new ZString(bs);");
				System.out.println("}");

				for (SubrecordStats srs : rs.subrecordStatsList.values())
				{
					System.out.println("else if (sr.getSubrecordType().equals(\"" + srs.subrecordType + "\")){}");
				}

				System.out.println("else");
				System.out.println("{");
				System.out.println("System.out.println(\"unhandled : \" + sr.getSubrecordType() + \" in \" + recordData);");
				System.out.println("}");
			}
		}
		tree.expandRow(0);
		JFrame f = new JFrame();
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setSize(600, 600);
		f.getContentPane().setLayout(new GridLayout(1, 1));
		f.getContentPane().add(new JScrollPane(tree));
		f.setVisible(true);

	}

	private static void sopAndTree(SubrecordStats srs, DefaultMutableTreeNode rootNode)
	{
		String asStr = "\t" + srs.subrecordType + " n=" + srs.count + " " + srs.minLength + "-" + srs.maxLength;
		asStr += " " + (srs.isString ? "isString" : "");
		asStr += " in ";

		if (srs.appearsIn.size() > 10)
		{
			asStr += " " + srs.appearsIn.size() + " different RECO types";
		}
		else
		{
			for (String inRec : srs.appearsIn)
			{
				asStr += " " + inRec;
			}
		}

		if (srs.hasOrderOf.size() == 1)
		{
			asStr += " FixedOrd: " + srs.hasOrderOf.iterator().next();
		}
		else if (srs.hasOrderOf.size() > 10)
		{
			asStr += " " + srs.hasOrderOf.size() + " different orderNos";
		}
		else
		{
			asStr += " Ords:";
			for (Integer inOrd : srs.hasOrderOf)
			{
				asStr += " " + inOrd;
			}
		}
		if (srs.couldBeFormId)
		{
			asStr += " FId?";
		}

		System.out.println(asStr);

		DefaultMutableTreeNode node = new DefaultMutableTreeNode(asStr);
		rootNode.add(node);
	}

	private static Map<String, SubrecordStats> getSortedSubsMap()
	{
		List<Map.Entry<String, SubrecordStats>> entries = new ArrayList<Map.Entry<String, SubrecordStats>>(allSubrecordStatsList.entrySet());
		Collections.sort(entries, new Comparator<Map.Entry<String, SubrecordStats>>()
		{
			public int compare(Map.Entry<String, SubrecordStats> a, Map.Entry<String, SubrecordStats> b)
			{
				return a.getKey().compareTo(b.getKey());
			}
		});

		Map<String, SubrecordStats> sortedMap = new LinkedHashMap<String, SubrecordStats>();
		for (Map.Entry<String, SubrecordStats> entry : entries)
		{
			sortedMap.put(entry.getKey(), entry.getValue());
		}

		return sortedMap;
	}

	private static Map<String, RecordStats> getSortedRecsMap()
	{
		List<Map.Entry<String, RecordStats>> entries = new ArrayList<Map.Entry<String, RecordStats>>(recordStatsList.entrySet());
		Collections.sort(entries, new Comparator<Map.Entry<String, RecordStats>>()
		{
			public int compare(Map.Entry<String, RecordStats> a, Map.Entry<String, RecordStats> b)
			{
				return a.getKey().compareTo(b.getKey());
			}
		});

		Map<String, RecordStats> sortedMap = new LinkedHashMap<String, RecordStats>();
		for (Map.Entry<String, RecordStats> entry : entries)
		{
			sortedMap.put(entry.getKey(), entry.getValue());
		}

		return sortedMap;
	}

	/**
	 * Note flase, false is for non cell records (type data)
	 * @param pr
	 * @param interior
	 * @param exterior
	 * @throws DataFormatException
	 * @throws PluginException
	 */
	public static void applyRecord(PluginRecord pr, boolean interior, boolean exterior) throws DataFormatException, PluginException
	{
		if (recordsDone.contains(pr.getFormID()))
			return;

		recordsDone.add(pr.getFormID());

		Record rec = new Record(pr, -1);
		//	allRecords.add(rec);

		recordStatsList.applyRecord(rec, interior, exterior, allSubrecordStatsList);

		if (pr instanceof PluginGroup)
		{
			PluginGroup pg = (PluginGroup) pr;
			for (PluginRecord pr2 : pg.getRecordList())
			{
				applyRecord(pr2, interior, exterior);
			}
		}
	}

	public static void loadRecord(Record rec)
	{
		if (recordLoader != null)
		{
			recordLoader.loadRecord(rec);
		}
	}

	public interface RecordLoader
	{
		public void loadRecord(Record rec);
	}

}