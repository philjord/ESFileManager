package esmmanager.loader;

import esmmanager.common.data.plugin.IMaster;
import esmmanager.common.data.record.IRecordStore;

public interface IESMManager extends IMaster, IRecordStore
{
	void addMaster(IMaster master);

	void addMaster(String absolutePath);

	void clearMasters();

}
