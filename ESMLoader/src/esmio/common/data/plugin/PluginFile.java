package esmio.common.data.plugin;

import java.io.File;
import java.io.IOException;
import java.util.zip.DataFormatException;

import esmio.common.PluginException;
import tools.io.FileChannelRAF;

/**
 * For desktop to swap File path strings into ByteBuffer or randomaccessfile or io streams Android will have the same
 * for SAF Uri starting points
 */

public class PluginFile extends Plugin {
	private File pluginFile;

	public PluginFile(File pluginFile) {
		super(pluginFile.getName());
		this.pluginFile = pluginFile;
	}

	/**
	 * This method assumes an index only version is required
	 * @throws PluginException
	 * @throws DataFormatException
	 * @throws IOException
	 */
	@Override
	public void load() throws PluginException, DataFormatException, IOException {
		load(true);
	}

	@Override
	public void load(boolean indexCellsOnly) throws PluginException, DataFormatException, IOException {
		if (!pluginFile.exists() || !pluginFile.isFile())
			throw new IOException("Plugin file '" + pluginFile.getName() + "' does not exist");

		//in = new RandomAccessFile(pluginFile, "r");
		in = new FileChannelRAF(pluginFile, "r");
		super.load(indexCellsOnly, in);
	}
}
