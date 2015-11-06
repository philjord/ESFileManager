package TES4Gecko;

import java.io.File;

import javax.swing.filechooser.FileFilter;

public class PluginDirectoryFilter extends FileFilter
{
	public String getDescription()
	{
		return "File Directories";
	}

	public boolean accept(File file)
	{
		return file.isDirectory();
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.PluginDirectoryFilter
 * JD-Core Version:    0.6.0
 */