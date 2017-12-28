package esmio.utils;

import java.util.ArrayList;
import java.util.List;

import esmio.common.data.plugin.PluginGroup;
import esmio.common.data.record.Record;

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
		if (cellChildren != null && cellChildren.getRecordList() != null)
		{
			for (Record pgr : cellChildren.getRecordList())
			{
				PluginGroup pg = (PluginGroup) pgr;

				if (pg.getGroupType() == type)
				{
					return pg.getRecordList();
				}
			}
		}
		// can't return null here, just empty is fine
		return new ArrayList<Record>();
	}
}
