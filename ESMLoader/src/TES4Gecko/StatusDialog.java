package TES4Gecko;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

public class StatusDialog extends JDialog implements ActionListener
{
	private Component parent;

	private Thread worker;

	private JLabel messageText;

	private JProgressBar progressBar;

	private int status = -1;

	private String deferredText;

	private int deferredProgress;

	public StatusDialog(JFrame parent, String text, String title)
	{
		super(parent, title, true);
		this.parent = parent;
		initFields(text);
	}

	public StatusDialog(JDialog parent, String text, String title)
	{
		super(parent, title, true);
		this.parent = parent;
		initFields(text);
	}

	private void initFields(String text)
	{
		JPanel progressPane = new JPanel();
		progressPane.setLayout(new BoxLayout(progressPane, 1));
		progressPane.add(Box.createVerticalStrut(15));
		this.messageText = new JLabel(text);
		progressPane.add(this.messageText);
		progressPane.add(Box.createVerticalStrut(15));
		this.progressBar = new JProgressBar(0, 100);
		this.progressBar.setStringPainted(true);
		progressPane.add(this.progressBar);
		progressPane.add(Box.createVerticalStrut(15));

		JPanel buttonPane = new JPanel();
		buttonPane.setOpaque(false);
		JButton button = new JButton("Cancel");
		button.setActionCommand("cancel");
		button.addActionListener(this);
		buttonPane.add(button);

		JPanel contentPane = new JPanel();
		contentPane.setLayout(new BoxLayout(contentPane, 1));
		contentPane.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
		contentPane.add(progressPane);
		contentPane.add(Box.createVerticalStrut(10));
		contentPane.add(buttonPane);
		setContentPane(contentPane);
	}

	public void setWorker(Thread worker)
	{
		this.worker = worker;
	}

	public void actionPerformed(ActionEvent ae)
	{
		try
		{
			String action = ae.getActionCommand();
			if ((action.equals("cancel")) && (this.worker != null))
				this.worker.interrupt();
		}
		catch (Throwable exc)
		{
			Main.logException("Exception while processing action event", exc);
		}
	}

	public int showDialog()
	{
		pack();
		setLocationRelativeTo(this.parent);

		while (this.status == -1)
		{
			setVisible(true);
		}
		return this.status;
	}

	public int getStatus()
	{
		return this.status;
	}

	public void closeDialog(boolean completed)
	{
		this.status = (completed ? 1 : 0);
		if (SwingUtilities.isEventDispatchThread())
		{
			setVisible(false);
			dispose();
		}
		else
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					StatusDialog.this.setVisible(false);
					StatusDialog.this.dispose();
				}
			});
		}
	}

	public void updateMessage(String text)
	{
		if (SwingUtilities.isEventDispatchThread())
		{
			this.messageText.setText(text);
			this.progressBar.setValue(0);
			pack();
			setLocationRelativeTo(this.parent);
		}
		else
		{
			this.deferredText = text;
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					StatusDialog.this.messageText.setText(StatusDialog.this.deferredText);
					StatusDialog.this.progressBar.setValue(0);
					StatusDialog.this.pack();
					StatusDialog.this.setLocationRelativeTo(StatusDialog.this.parent);
				}
			});
		}
	}

	public void updateProgress(int progress)
	{
		if (SwingUtilities.isEventDispatchThread())
		{
			this.progressBar.setValue(progress);
		}
		else
		{
			this.deferredProgress = progress;
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					StatusDialog.this.progressBar.setValue(StatusDialog.this.deferredProgress);
				}
			});
		}
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.StatusDialog
 * JD-Core Version:    0.6.0
 */