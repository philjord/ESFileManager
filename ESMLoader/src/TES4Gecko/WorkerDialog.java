package TES4Gecko;

import java.awt.Component;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class WorkerDialog implements Runnable
{
	public static final int CLOSED_OPTION = -1;

	public static final int OK_OPTION = 0;

	public static final int YES_OPTION = 0;

	public static final int NO_OPTION = 1;

	public static final int YES_TO_ALL_OPTION = 2;

	private boolean confirmDialog;

	private Component parent;

	private String message;

	private String title;

	private int optionType;

	private int messageType;

	private int selection;

	private boolean yesToAll;

	public WorkerDialog(Component parent, String message, String title, int messageType)
	{
		this.parent = parent;
		this.message = message;
		this.title = title;
		this.messageType = messageType;
		this.confirmDialog = false;
	}

	public WorkerDialog(Component parent, String message, String title, int optionType, int messageType, boolean yesToAll)
	{
		this.parent = parent;
		this.message = message;
		this.title = title;
		this.optionType = optionType;
		this.messageType = messageType;
		this.yesToAll = yesToAll;
		this.confirmDialog = true;
	}

	public void run()
	{
		if (this.confirmDialog)
		{
			if (this.yesToAll)
			{
				Object[] options =
				{ "Yes", "No", "Yes to All" };
				this.selection = JOptionPane.showOptionDialog(this.parent, this.message, this.title, this.optionType, this.messageType,
						null, options, options[2]);
			}
			else
			{
				this.selection = JOptionPane.showConfirmDialog(this.parent, this.message, this.title, this.optionType, this.messageType);
			}
		}
		else
		{
			JOptionPane.showMessageDialog(this.parent, this.message, this.title, this.messageType);
			this.selection = 0;
		}
	}

	public int getSelection()
	{
		return this.selection;
	}

	public static void showMessageDialog(Component parent, String message, String title, int messageType)
	{
		int selection = -1;
		try
		{
			WorkerDialog messageDialog = new WorkerDialog(parent, message, title, messageType);
			SwingUtilities.invokeAndWait(messageDialog);
		}
		catch (InterruptedException exc)
		{
			Main.logException("Message dialog interrupted", exc);
		}
		catch (Throwable exc)
		{
			Main.logException("Exception while displaying message dialog", exc);
		}
	}

	public static int showConfirmDialog(Component parent, String message, String title, int optionType, int messageType, boolean yesToAll)
	{
		int selection = -1;
		try
		{
			WorkerDialog confirmDialog = new WorkerDialog(parent, message, title, optionType, messageType, yesToAll);
			SwingUtilities.invokeAndWait(confirmDialog);
			selection = confirmDialog.getSelection();
		}
		catch (InterruptedException exc)
		{
			Main.logException("Confirmation dialog interrupted", exc);
		}
		catch (Throwable exc)
		{
			Main.logException("Exception while displaying confirmation dialog", exc);
		}

		return selection;
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.WorkerDialog
 * JD-Core Version:    0.6.0
 */