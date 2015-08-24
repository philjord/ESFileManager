package utils;

import java.util.ArrayList;
import java.util.List;

import esmLoader.common.data.plugin.PluginGroup;
import esmLoader.common.data.plugin.PluginRecord;
import esmLoader.common.data.record.Record;

public class ESMUtils
{
	/**
	 * PluginGroup.CELL_PERSISTENT
	 * PluginGroup.CELL_TEMPORARY
	 * PluginGroup.CELL_DISTANT
	 * @param cellChildren
	 * @param type
	 * @return
	 */
	public static List<Record> getChildren(PluginGroup cellChildren, int type)
	{
		List<Record> ret = new ArrayList<Record>();
		if (cellChildren != null && cellChildren.getRecordList() != null)
		{
			for (PluginRecord pgr : cellChildren.getRecordList())
			{
				PluginGroup pg = (PluginGroup) pgr;

				if (pg.getGroupType() == type)
				{
					for (PluginRecord pr : pg.getRecordList())
					{
						Record record = new Record(pr, -1);
						ret.add(record);
					}
				}
			}
		}
		return ret;
	}
}
