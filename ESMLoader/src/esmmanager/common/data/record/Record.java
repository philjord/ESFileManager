package esmmanager.common.data.record;

import java.util.List;

public class Record
{
	protected String recordType;

	protected int formID;

	protected int recordFlags1;

	protected int recordFlags2;

	protected boolean updated = false;

	protected List<Subrecord> subrecordList;

	public Record()
	{
	}

	public Record(String recordType, int formID, int cellFormID, int recordFlags1, int recordFlags2)
	{
		this.recordType = recordType;
		this.formID = formID;
		this.cellFormID = cellFormID;
		this.recordFlags1 = recordFlags1;
		this.recordFlags2 = recordFlags2;
	}

	public Record(String recordType, int formID, int cellFormID, int recordFlags1, int recordFlags2, List<Subrecord> subs)//, byte[] recordData)
	{
		this(recordType, formID, cellFormID, recordFlags1, recordFlags2);
		subrecordList = subs;
	}

	public boolean isDeleted()
	{
		return (recordFlags1 & 0x20) != 0;
	}

	public boolean isIgnored()
	{
		return (recordFlags1 & 0x1000) != 0;
	}
	
	public boolean isCompressed()
	{
		return (recordFlags1 & 0x40000) != 0;
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
		List<Subrecord> subrecords = getSubrecords();
		for (Subrecord subrec : subrecords)
		{
			System.out.println("Subrecord " + subrec);
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

	public List<Subrecord> getSubrecords()
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

	//ONLY for ESMConverter.ConverterFO3
	private int cellFormID = -1;

	public int getCellFormID()
	{
		return cellFormID;
	}

	public void setCellFormID(int i)
	{
		cellFormID = i;
	}

}
