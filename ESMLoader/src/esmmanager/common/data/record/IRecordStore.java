package esmmanager.common.data.record;

public interface IRecordStore
{
	public Record getRecord(int formID);

	public Record getRecord(String edidId);

}
