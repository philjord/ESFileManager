package TES4Gecko;

import java.io.File;

public class ESPFileFilter extends javax.swing.filechooser.FileFilter implements java.io.FileFilter
{
	public String getDescription()
	{
		return "Oblivion Plugin Files (*.esp)";
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
				if (name.substring(sep).equalsIgnoreCase(".esp"))
					accept = true;
			}
			else
			{
				accept = true;
			}
		}

		return accept;
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.ESPFileFilter
 * JD-Core Version:    0.6.0
 */