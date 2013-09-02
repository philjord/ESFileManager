package analyzer;

import java.util.ArrayList;

import esmLoader.common.data.record.Record;
import esmLoader.common.data.record.Subrecord;

public class RecordStats
{
	public String type = "";

	public boolean appearsInExtCELL = false;

	public boolean appearsInIntCELL = false;

	public int count = 0;

	public SubrecordStatsList subrecordStatsList = new SubrecordStatsList();

	public RecordStats(String t)
	{
		this.type = t;
	}

	public void applyRecord(Record rec, boolean interior, boolean exterior, SubrecordStatsList allSubrecordStatsList)
	{
		appearsInIntCELL = appearsInIntCELL || interior;
		appearsInExtCELL = appearsInExtCELL || exterior;
		count++;

		ArrayList<Subrecord> subs = rec.getSubrecords();
		for (int i = 0; i < subs.size(); i++)
		{
			Subrecord sub = subs.get(i);

			// heaps of madness in some records
			if (sub.getSubrecordType().endsWith("0TX") || sub.getSubrecordType().endsWith("IAD"))
				continue;

			subrecordStatsList.applySub(sub, rec.getRecordType(), i);
			// also put into the global sub stats list
			allSubrecordStatsList.applySub(sub, rec.getRecordType(), i);
		}

		EsmFormatAnalyzer.loadRecord(rec);
	}
}
