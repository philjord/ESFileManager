package TES4Gecko;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.util.zip.DataFormatException;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class LoadTask extends WorkerTask
{
	private File pluginFile;

	private Plugin plugin;

	public LoadTask(StatusDialog statusDialog, File pluginFile)
	{
		super(statusDialog);
		this.pluginFile = pluginFile;
	}

	public static Plugin loadPlugin(Component parent, File pluginFile)
	{
		StatusDialog statusDialog;

		if ((parent instanceof JFrame))
			statusDialog = new StatusDialog((JFrame) parent, "Loading plugin", "Load Plugin");
		else
		{
			statusDialog = new StatusDialog((JDialog) parent, "Loading plugin", "Load Plugin");
		}

		LoadTask worker = new LoadTask(statusDialog, pluginFile);
		statusDialog.setWorker(worker);

		worker.start();
		statusDialog.showDialog();

		if (statusDialog.getStatus() != 1)
		{
			worker.plugin = null;
			JOptionPane.showMessageDialog(parent, "Unable to load " + pluginFile.getName(), "Load Plugin", 1);
		}

		return worker.plugin;
	}

	public void run()
	{
		boolean completed = false;
		try
		{
			this.plugin = new Plugin(this.pluginFile);
			this.plugin.load(this);
			completed = true;
		}
		catch (PluginException exc)
		{
			Main.logException("Plugin Error", exc);
		}
		catch (DataFormatException exc)
		{
			Main.logException("Compression Error", exc);
		}
		catch (IOException exc)
		{
			Main.logException("I/O Error", exc);
		}
		catch (InterruptedException exc)
		{
			WorkerDialog.showMessageDialog(getStatusDialog(), "Request canceled", "Interrupted", 0);
		}
		catch (Throwable exc)
		{
			Main.logException("Exception while loading plugin", exc);
		}

		getStatusDialog().closeDialog(completed);
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.LoadTask
 * JD-Core Version:    0.6.0
 */