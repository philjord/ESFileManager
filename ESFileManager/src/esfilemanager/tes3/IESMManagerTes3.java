package esfilemanager.tes3;

import esfilemanager.common.data.plugin.IMaster;
import esfilemanager.loader.IESMManager;

public interface IESMManagerTes3 extends IESMManager, IMasterTes3, IRecordStoreTes3
{
	void addMaster(IMaster master);

	void addMaster(String absolutePath);

	void clearMasters();
}
