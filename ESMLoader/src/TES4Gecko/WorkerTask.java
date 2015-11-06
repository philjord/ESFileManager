package TES4Gecko;

import java.awt.Component;

public class WorkerTask extends Thread
{
	private StatusDialog statusDialog;

	public WorkerTask(StatusDialog statusDialog)
	{
		this.statusDialog = statusDialog;
	}

	public StatusDialog getStatusDialog()
	{
		return this.statusDialog;
	}

	public Component getParent()
	{
		return this.statusDialog;
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.WorkerTask
 * JD-Core Version:    0.6.0
 */