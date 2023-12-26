package esfilemanager.loader;

import java.util.ArrayList;

import esfilemanager.common.data.plugin.IMaster;
import esfilemanager.common.data.record.IRecordStore;

public interface IESMManager extends IMaster, IRecordStore
{
	void addMaster(IMaster master);

	void addMaster(String absolutePath);

	void clearMasters();

	ArrayList<IMaster> getMasters();

}
