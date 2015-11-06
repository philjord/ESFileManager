package TES4Gecko;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.util.zip.DataFormatException;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class SaveTask extends WorkerTask
{
	private File pluginFile;

	private Plugin plugin;

	public SaveTask(StatusDialog statusDialog, File pluginFile, Plugin plugin)
	{
		super(statusDialog);
		this.pluginFile = pluginFile;
		this.plugin = plugin;
		this.plugin.setPluginFile(this.pluginFile);
	}

	public static boolean savePlugin(Component parent, File pluginFile, Plugin plugin)
	{
		StatusDialog statusDialog;

		if ((parent instanceof JFrame))
			statusDialog = new StatusDialog((JFrame) parent, "Saving plugin", "Save Plugin");
		else
		{
			statusDialog = new StatusDialog((JDialog) parent, "Saving plugin", "Save Plugin");
		}

		SaveTask worker = new SaveTask(statusDialog, pluginFile, plugin);
		statusDialog.setWorker(worker);

		worker.start();
		statusDialog.showDialog();

		boolean saved = statusDialog.getStatus() == 1;
		if (!saved)
		{
			JOptionPane.showMessageDialog(parent, "Unable to save " + pluginFile.getName(), "Save Plugin", 1);
		}

		return saved;
	}

	public void run()
	{
		boolean completed = false;
		try
		{
			this.plugin.store(this);
			completed = true;
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
			Main.logException("Exception while saving plugin", exc);
		}

		getStatusDialog().closeDialog(completed);
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.SaveTask
 * JD-Core Version:    0.6.0
 */