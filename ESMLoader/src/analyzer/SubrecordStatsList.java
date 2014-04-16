package analyzer;

import java.util.LinkedHashMap;

import esmLoader.common.data.record.Subrecord;

public class SubrecordStatsList extends LinkedHashMap<String, SubrecordStats>
{
	public void applySub(Subrecord sub, String inRec, int orderNo)
	{
		// are we updating or creating
		SubrecordStats subrecordStats = get(sub.getType());
		if (subrecordStats == null)
		{
			subrecordStats = new SubrecordStats(sub.getType());
			put(sub.getType(), subrecordStats);
		}

		subrecordStats.applySub(sub, inRec, orderNo);

	}
}
