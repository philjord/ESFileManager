package TES4Gecko;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class ConvertTask extends WorkerTask
{
	private File inFile;

	private File outFile;

	public ConvertTask(StatusDialog statusDialog, File inFile, File outFile)
	{
		super(statusDialog);
		this.inFile = inFile;
		this.outFile = outFile;
	}

	public static void convertFile(JFrame parent, File inFile, File outFile)
	{
		StatusDialog statusDialog = new StatusDialog(parent, "Converting '" + inFile.getName() + "' to '" + outFile.getName() + "'",
				"Convert File");

		ConvertTask worker = new ConvertTask(statusDialog, inFile, outFile);
		statusDialog.setWorker(worker);

		worker.start();
		statusDialog.showDialog();

		if (statusDialog.getStatus() == 1)
			JOptionPane.showMessageDialog(parent, "'" + inFile.getName() + "' converted to '" + outFile.getName() + "'", "Convert File", 1);
		else
			JOptionPane.showMessageDialog(parent, "Unable to convert " + inFile.getName(), "Convert File", 1);
	}

	public void run()
	{
		FileInputStream in = null;
		FileOutputStream out = null;
		byte[] buffer = new byte[4096];
		boolean completed = false;
		try
		{
			boolean headerSet = false;

			if ((!this.inFile.exists()) || (!this.inFile.isFile()))
			{
				throw new IOException("'" + this.inFile.getName() + "' does not exist");
			}
			if (this.outFile.exists())
			{
				this.outFile.delete();
			}
			String name = this.outFile.getName();
			int sep = name.lastIndexOf('.');
			int flagValue;
			// int flagValue;
			if (name.substring(sep).equalsIgnoreCase(".esm"))
				flagValue = 1;
			else
			{
				flagValue = 0;
			}
			in = new FileInputStream(this.inFile);
			out = new FileOutputStream(this.outFile);
			long fileSize = this.inFile.length();
			long processedCount = 0L;
			int currentProgress = 0;
			while (true)
			{
				int count = in.read(buffer, 0, 4096);
				if (count < 0)
				{
					break;
				}
				if (count > 0)
				{
					if (!headerSet)
					{
						if (count < 20)
						{
							throw new PluginException("'" + this.inFile.getName() + "' is not a TES4 file");
						}
						String type = new String(buffer, 0, 4);
						if (!type.equals("TES4"))
						{
							throw new PluginException("'" + this.inFile.getName() + "' is not a TES4 file");
						}
						buffer[8] = (byte) flagValue;
						headerSet = true;
					}

					out.write(buffer, 0, count);
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
			}
			 
			out.close();
			out = null;
			in.close();
			in = null;
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
			Main.logException("Exception while converting file", exc);
		}
		finally
		{
			try
			{
				if (in != null)
					in.close();
				if (out != null)
					out.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
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
				if (this.outFile.exists())
					this.outFile.delete();
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
 * Qualified Name:     TES4Gecko.ConvertTask
 * JD-Core Version:    0.6.0
 */