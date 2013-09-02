package analyzer;

import java.util.LinkedHashMap;

import esmLoader.common.data.record.Subrecord;

public class SubrecordStatsList extends LinkedHashMap<String, SubrecordStats>
{
	public void applySub(Subrecord sub, String inRec, int orderNo)
	{
		// are we updating or creating
		SubrecordStats subrecordStats = get(sub.getSubrecordType());
		if (subrecordStats == null)
		{
			subrecordStats = new SubrecordStats(sub.getSubrecordType());
			put(sub.getSubrecordType(), subrecordStats);
		}

		subrecordStats.applySub(sub, inRec, orderNo);

	}
}
