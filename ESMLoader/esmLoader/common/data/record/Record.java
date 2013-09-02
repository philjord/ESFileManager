package esmLoader.common.data.record;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import esmLoader.common.data.plugin.PluginRecord;
import esmLoader.common.data.plugin.PluginSubrecord;

public class Record
{
	private String recordType;

	private int formID;

	private int cellFormID;

	private int recordFlags1;

	private int recordFlags2;

	private boolean updated = false;

	private ArrayList<Subrecord> subrecordList = new ArrayList<Subrecord>();

	public Record(PluginRecord pluginRecord, int cellFormID)
	{
		this(pluginRecord.getRecordType(), pluginRecord.getFormID(), cellFormID, pluginRecord.getRecordFlags1(), pluginRecord.getRecordFlags2(),
				pluginRecord.getSubrecords());
	}

	public Record(String recordType, int formID, int cellFormID, int recordFlags1, int recordFlags2)
	{
		this.recordType = recordType;
		this.formID = formID;
		this.cellFormID = cellFormID;
		this.recordFlags1 = recordFlags1;
		this.recordFlags2 = recordFlags2;
	}

	public Record(String recordType, int formID, int cellFormID, int recordFlags1, int recordFlags2, List<PluginSubrecord> subs)//, byte[] recordData)
	{
		this(recordType, formID, cellFormID, recordFlags1, recordFlags2);
		//	if (recordData != null)
		//		decodeSubrecords(recordData);
		for (PluginSubrecord sub : subs)
		{
			subrecordList.add(new Subrecord(recordType, sub.getSubrecordType(), sub.getSubrecordData()));
		}
	}

	public boolean isDeleted()
	{
		return (recordFlags1 & 0x20) != 0;
	}

	public boolean isIgnored()
	{
		return (recordFlags1 & 0x1000) != 0;
	}

	public String getRecordType()
	{
		return recordType;
	}

	public int getFormID()
	{
		return formID;
	}

	public String toString()
	{
		return recordType + " record: " + formID + " " + (isIgnored() ? "(Ignore) " : "") + (isDeleted() ? "(Deleted) " : "");

	}

	public void displaySubs()
	{
		for (Iterator<Subrecord> i$ = subrecordList.iterator(); i$.hasNext();)
		{
			Subrecord subrec = i$.next();
			System.out.println("subrec " + subrec);
		}
	}

	public int getRecordFlags1()
	{
		return recordFlags1;
	}

	public int getRecordFlags2()
	{
		return recordFlags2;
	}

	public ArrayList<Subrecord> getSubrecords()
	{
		return subrecordList;
	}

	public void addSubrecord(Subrecord subrecord)
	{
		subrecordList.add(subrecord);
		updated = true;
	}

	public void setFormID(int i)
	{
		formID = i;
		updated = true;
	}

	public void updateFrom(Record record)
	{
		subrecordList.clear();
		subrecordList.addAll(record.getSubrecords());
		updated = true;
	}

	/**
	 * Currently this only overrides the first matching subrecord type and breaks, for multiple entries
	 * like CNTOs there needs to be a much better system
	 * @param subrecord
	 */
	public void updateSubrecord(Subrecord newSubrecord)
	{
		for (int i = 0; i < subrecordList.size(); i++)
		{
			Subrecord sub = subrecordList.get(i);
			if (sub.getSubrecordType().equals(newSubrecord.getSubrecordType()))
			{
				sub.setSubrecordData(newSubrecord.getSubrecordData());
			}
		}
		updated = true;
	}

	public boolean isUpdated()
	{
		return updated;
	}

	public void clearUpdated()
	{
		updated = false;
	}

	public int getCellFormID()
	{
		return cellFormID;
	}

	public void setCellFormID(int i)
	{
		cellFormID = i;
	}

}
