package esmmanager.tes3;

import esmmanager.common.data.plugin.IMaster;
import esmmanager.loader.IESMManager;

public interface IESMManagerTes3 extends IESMManager, IMasterTes3, IRecordStoreTes3
{
	void addMaster(IMaster master);

	void addMaster(String absolutePath);

	void clearMasters();
}
