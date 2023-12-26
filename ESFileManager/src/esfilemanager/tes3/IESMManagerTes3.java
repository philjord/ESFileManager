package esmio.tes3;

import esmio.common.data.plugin.IMaster;
import esmio.loader.IESMManager;

public interface IESMManagerTes3 extends IESMManager, IMasterTes3, IRecordStoreTes3
{
	void addMaster(IMaster master);

	void addMaster(String absolutePath);

	void clearMasters();
}
