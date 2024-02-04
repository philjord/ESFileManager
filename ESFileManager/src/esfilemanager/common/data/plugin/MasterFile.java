package esfilemanager.common.data.plugin;

import java.io.File;
import java.io.IOException;
import java.util.zip.DataFormatException;

import esfilemanager.common.PluginException;
import tools.io.FileChannelRAF;

/**
 * For desktop to swap File path strings into ByteBuffer or randomaccessfile or io streams Android will have the same
 * for SAF Uri starting points
 */
public class MasterFile extends Master {

	private File masterFile;

	public MasterFile(File masterFile) {
		super(masterFile.getName());
		this.masterFile = masterFile;
	}

	@Override
	public boolean load() throws PluginException, DataFormatException, IOException {

		if (!masterFile.exists() || !masterFile.isFile())
			throw new IOException("Master file '" + masterFile.getAbsolutePath() + "' does not exist");

		FileChannelRAF in = new FileChannelRAF(masterFile, "r");

		return super.load(in);
	}
}
