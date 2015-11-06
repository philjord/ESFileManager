package esmmanager.loader;

import java.util.List;

import esmmanager.common.data.plugin.IMaster;
import esmmanager.common.data.record.IRecordStore;

public interface IESMManager extends IMaster, IRecordStore
{
	void addMaster(IMaster master);

	void addMaster(String absolutePath);

	void clearMasters();

	//FIXME: used by ESMManager itself to look for doors only, probably not needed once I know what sort of 
	//ESM I am using (tes3 or tes4)

	List<WRLDTopGroup> getWRLDTopGroups();

	List<InteriorCELLTopGroup> getInteriorCELLTopGroups();

	int getCellIdOfPersistentTarget(int doorFormId);

}
