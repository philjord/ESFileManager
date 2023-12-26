package esfilemanager.tes3;

import esfilemanager.common.data.record.IRecordStore;
import esfilemanager.common.data.record.Record;

public interface IRecordStoreTes3 extends IRecordStore
{
	public Record getRecord(String edidId);

}
