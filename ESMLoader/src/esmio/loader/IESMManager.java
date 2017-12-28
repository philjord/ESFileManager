package esmio.loader;

import esmio.common.data.plugin.IMaster;
import esmio.common.data.record.IRecordStore;

public interface IESMManager extends IMaster, IRecordStore
{
	void addMaster(IMaster master);

	void addMaster(String absolutePath);

	void clearMasters();



}
