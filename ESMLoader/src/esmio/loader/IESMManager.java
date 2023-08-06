package esmio.loader;

import java.util.ArrayList;

import esmio.common.data.plugin.IMaster;
import esmio.common.data.record.IRecordStore;

public interface IESMManager extends IMaster, IRecordStore
{
	void addMaster(IMaster master);

	void addMaster(String absolutePath);

	void clearMasters();

	ArrayList<IMaster> getMasters();

}
