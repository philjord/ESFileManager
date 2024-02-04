package esfilemanager.tes3;

import java.io.File;
import java.io.IOException;

import esfilemanager.common.PluginException;

public class ESMManagerTes3File extends ESMManagerTes3 {
	
	
	public ESMManagerTes3File(String fileName) {
		File m = new File(fileName);

		try {
			Master master = new MasterFile(m);
			master.load();
			
			addMaster(master);
		} catch (PluginException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	@Override
	public void addMaster(String fileNameToAdd) {
		try {
			File m = new File(fileNameToAdd);
			Master master = new MasterFile(m);
			master.load();
			addMaster(master);
		} catch (PluginException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

}
