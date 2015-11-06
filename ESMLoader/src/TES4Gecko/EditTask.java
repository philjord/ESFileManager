package TES4Gecko;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class EditTask extends WorkerTask
{
	private File inFile;

	private PluginInfo pluginInfo;

	public EditTask(StatusDialog statusDialog, File inFile, PluginInfo pluginInfo)
	{
		super(statusDialog);
		this.inFile = inFile;
		this.pluginInfo = pluginInfo;
	}

	public static void editFile(JFrame parent, File inFile, PluginInfo pluginInfo)
	{
		StatusDialog statusDialog = new StatusDialog(parent, "Updating " + inFile.getName(), "Update Plugin");

		EditTask worker = new EditTask(statusDialog, inFile, pluginInfo);
		statusDialog.setWorker(worker);

		worker.start();
		statusDialog.showDialog();

		if (statusDialog.getStatus() == 1)
			JOptionPane.showMessageDialog(parent, "Updated " + inFile.getName(), "Update Plugin", 1);
		else
			JOptionPane.showMessageDialog(parent, "Unable to update " + inFile.getName(), "Update Plugin", 1);
	}

	public void run()
	{
		File outFile = new File(this.inFile.getParent() + Main.fileSeparator + "Gecko.tmp");
		RandomAccessFile in = null;
		FileOutputStream out = null;
		byte[] buffer = new byte[4096];
		boolean completed = false;
		try
		{
			if ((!this.inFile.exists()) || (!this.inFile.isFile()))
			{
				throw new IOException("'" + this.inFile.getName() + "' does not exist");
			}
			if (outFile.exists())
			{
				outFile.delete();
			}
			in = new RandomAccessFile(this.inFile, "r");
			out = new FileOutputStream(outFile);
			long fileSize = this.inFile.length();
			long processedCount = 0L;
			int currentProgress = 0;

			PluginHeader inHeader = new PluginHeader(this.inFile);
			inHeader.read(in);

			PluginHeader outHeader = new PluginHeader(outFile);
			outHeader.setRecordCount(inHeader.getRecordCount());
			outHeader.setMaster(inHeader.isMaster());
			outHeader.setMasterList(inHeader.getMasterList());
			outHeader.setVersion(this.pluginInfo.getVersion());
			outHeader.setCreator(this.pluginInfo.getCreator());
			outHeader.setSummary(this.pluginInfo.getSummary());
			outHeader.write(out);
			while (true)
			{
				int count = in.read(buffer, 0, 4096);
				if (count < 0)
				{
					break;
				}
				if (count > 0)
				{
					out.write(buffer, 0, count);
				}
				if (interrupted())
				{
					throw new InterruptedException("Request canceled");
				}
				processedCount += count;
				int newProgress = (int) (processedCount * 100L / fileSize);
				if (newProgress >= currentProgress + 5)
				{
					currentProgress = newProgress;
					getStatusDialog().updateProgress(currentProgress);
				}

			}

			out.close();
			out = null;

			in.close();
			in = null;

			this.inFile.delete();
			outFile.renameTo(this.inFile);

			completed = true;
		}
		catch (PluginException exc)
		{
			Main.logException("Plugin Error", exc);
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
			Main.logException("Exception while updating plugin", exc);
		}

		if (!completed)
		{
			try
			{
				if (out != null)
				{
					out.close();
				}
				if (in != null)
				{
					in.close();
				}
				if (outFile.exists())
					outFile.delete();
			}
			catch (IOException exc)
			{
				Main.logException("I/O Error", exc);
			}

		}

		getStatusDialog().closeDialog(completed);
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.EditTask
 * JD-Core Version:    0.6.0
 */