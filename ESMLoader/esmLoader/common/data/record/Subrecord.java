package esmLoader.common.data.record;

public class Subrecord
{
	private String recordType;

	private String subrecordType;

	private byte subrecordData[];

	public Subrecord(String recordType, String subrecordType, byte subrecordData[])
	{
		this.recordType = recordType;
		this.subrecordType = subrecordType;
		this.subrecordData = subrecordData;
	}

	public String getRecordType()
	{
		return recordType;
	}

	public String getSubrecordType()
	{
		return subrecordType;
	}

	public byte[] getSubrecordData()
	{
		return subrecordData;
	}

	public void setSubrecordData(byte subrecordData[])
	{
		this.subrecordData = subrecordData;
	}

	public String toString()
	{
		return subrecordType + " subrecord";
	}
}
