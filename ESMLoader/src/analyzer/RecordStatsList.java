package analyzer;

import java.util.LinkedHashMap;

import esmLoader.common.data.record.Record;

public class RecordStatsList extends LinkedHashMap<String, RecordStats>
{	
	public void applyRecord(Record rec, boolean interior, boolean exterior, SubrecordStatsList allSubrecordStatsList)
	{
		// are we updating or creating
		RecordStats recordStats = get(rec.getRecordType());
		if (recordStats == null)
		{
			recordStats = new RecordStats(rec.getRecordType());
			put(rec.getRecordType(), recordStats);
		}
		recordStats.applyRecord(rec, interior, exterior, allSubrecordStatsList);
	}
}
