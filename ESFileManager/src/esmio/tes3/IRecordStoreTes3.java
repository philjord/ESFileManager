package esmio.tes3;

import esmio.common.data.record.IRecordStore;
import esmio.common.data.record.Record;

public interface IRecordStoreTes3 extends IRecordStore
{
	public Record getRecord(String edidId);

}
