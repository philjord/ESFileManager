package esmLoader.tes3;

import esmLoader.common.data.plugin.PluginSubrecord;

public class CELLPluginGroup extends PluginGroup
{
	private PluginGroup temps = new PluginGroup(CELL_TEMPORARY);

	public CELLPluginGroup(PluginRecord cellRecord)
	{
		super(CELL);

		// one child for now (can have temp,persist and distant)

		getRecordList().add(temps);

		PluginRecord refr = null;
		for (int i = 0; i < cellRecord.getSubrecords().size(); i++)
		{
			PluginSubrecord sub = cellRecord.getSubrecords().get(i);
			if (sub.getSubrecordType().equals("FRMR"))
			{
				// have we finished a prior now?
				if (refr != null)
					temps.getRecordList().add(refr);

				refr = new PluginRecord(i, "REFR", "REFR:" + i);
			}

			// just chuck it in, if we are building up a refr now
			if (refr != null)
				refr.getSubrecords().add(sub);
		}

		// have we finished a prior now?
		if (refr != null)
			temps.getRecordList().add(refr);
	}

	public void addPluginRecord(PluginRecord pr)
	{
		temps.getRecordList().add(pr);
	}
}
