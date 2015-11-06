package TES4Gecko;

import java.io.File;

public class PluginFileFilter extends javax.swing.filechooser.FileFilter implements java.io.FileFilter
{
	private boolean selectMasterFiles;

	private boolean selectPluginFiles;

	private boolean selectPatchFiles;

	public PluginFileFilter()
	{
		this.selectMasterFiles = true;
		this.selectPluginFiles = true;
		this.selectPatchFiles = true;
	}

	public PluginFileFilter(boolean selectMasterFiles)
	{
		this.selectMasterFiles = selectMasterFiles;
		this.selectPluginFiles = (!selectMasterFiles);
		this.selectPatchFiles = false;
	}

	public PluginFileFilter(boolean selectMasterFiles, boolean selectPluginFiles, boolean selectPatchFiles)
	{
		this.selectMasterFiles = selectMasterFiles;
		this.selectPluginFiles = selectPluginFiles;
		this.selectPatchFiles = selectPatchFiles;
	}

	public String getDescription()
	{
		String text = null;
		if (this.selectMasterFiles)
		{
			text = "TES Files (*.esm";
		}
		if (this.selectPluginFiles)
		{
			if (text == null)
				text = "TES Files (*.esp";
			else
				text = text.concat(", *.esp");
		}
		if (this.selectPatchFiles)
		{
			if (text == null)
				text = "TES Files (*.esu";
			else
				text = text.concat(", *.esu");
		}
		return text.concat(")");
	}

	public boolean accept(File file)
	{
		boolean accept = false;

		if (!file.isFile())
		{
			accept = true;
		}
		else
		{
			String name = file.getName();
			int sep = name.lastIndexOf('.');
			if (sep > 0)
			{
				if (name.substring(sep).equalsIgnoreCase(".esm"))
				{
					if (this.selectMasterFiles)
						accept = true;
				}
				else if (name.substring(sep).equalsIgnoreCase(".esp"))
				{
					if (this.selectPluginFiles)
						accept = true;
				}
				else if ((name.substring(sep).equalsIgnoreCase(".esu")) && (this.selectPatchFiles))
				{
					accept = true;
				}
			}
		}

		return accept;
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.PluginFileFilter
 * JD-Core Version:    0.6.0
 */