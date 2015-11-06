package TES4Gecko;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.util.zip.DataFormatException;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class CreateTreeTask extends WorkerTask
{
	private File pluginFile;

	private Plugin plugin;

	private PluginNode pluginNode;

	public CreateTreeTask(StatusDialog statusDialog, File pluginFile)
	{
		super(statusDialog);
		this.pluginFile = pluginFile;
	}

	public static PluginNode createTree(Component parent, File pluginFile)
	{
		StatusDialog statusDialog;
		//StatusDialog statusDialog;
		if ((parent instanceof JFrame))
			statusDialog = new StatusDialog((JFrame) parent, "Creating tree", "Create Tree");
		else
		{
			statusDialog = new StatusDialog((JDialog) parent, "Creating tree", "Create Tree");
		}

		CreateTreeTask worker = new CreateTreeTask(statusDialog, pluginFile);
		statusDialog.setWorker(worker);

		worker.start();
		statusDialog.showDialog();

		if (statusDialog.getStatus() != 1)
		{
			worker.pluginNode = null;
			JOptionPane.showMessageDialog(parent, "Unable to create tree for " + pluginFile.getName(), "Create Patch", 1);
		}

		return worker.pluginNode;
	}

	public void run()
	{
		boolean completed = false;
		try
		{
			this.plugin = new Plugin(this.pluginFile);
			this.plugin.load(this);

			this.pluginNode = new PluginNode(this.plugin);
			this.pluginNode.buildNodes(this);

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
			Main.logException("Exception while creating tree", exc);
		}

		getStatusDialog().closeDialog(completed);
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.CreateTreeTask
 * JD-Core Version:    0.6.0
 */