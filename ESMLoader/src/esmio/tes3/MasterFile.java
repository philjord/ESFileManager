package esmio.tes3;

import java.io.File;
import java.io.IOException;

import tools.io.FileChannelRAF;

/**
 * This is a copy of the master file in data package, however it holds onto a copy of all loaded data for everything
 * other than the WRLD and CELL values, which is simply indexes down to the subblock level
 *
 * @author Administrator
 *
 */
public class MasterFile extends Master {

	public MasterFile(File masterFile) throws IOException {
		super(new FileChannelRAF(masterFile, "r"), masterFile.getName());
	}

}
