package esmmanager.tes3;

import esmmanager.common.data.record.IRecordStore;
import esmmanager.common.data.record.Record;

public interface IRecordStoreTes3 extends IRecordStore
{
	public Record getRecord(String edidId);

}
