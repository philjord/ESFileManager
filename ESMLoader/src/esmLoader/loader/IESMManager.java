package esmLoader.loader;

import java.util.List;

import esmLoader.common.data.plugin.IMaster;
import esmLoader.common.data.plugin.Master;
import esmLoader.common.data.record.IRecordStore;

public interface IESMManager extends IMaster,IRecordStore
{
	void addMaster(Master master);

	void clearMasters();
	
	//FIXME: used by ESMManager itself to look for doors only, probably not needed once I know what sort of 
	//ESM I am using (tes3 or tes4)

	List<WRLDTopGroup> getWRLDTopGroups();

	List<InteriorCELLTopGroup> getInteriorCELLTopGroups();

	int getCellIdOfPersistentTarget(int doorFormId);

}
