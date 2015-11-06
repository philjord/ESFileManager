package TES4Gecko;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class WorldspaceDialog extends JDialog implements ActionListener
{
	private int option = -1;

	private boolean insertPlaceholders = false;

	private JCheckBox placeholdersField;

	public WorldspaceDialog(JFrame parent)
	{
		super(parent, "Move Worldspaces", true);
		setDefaultCloseOperation(2);

		JPanel buttonPane = new JPanel(new GridLayout(0, 1, 10, 10));
		buttonPane.setOpaque(true);
		buttonPane.setBackground(Main.backgroundColor);
		buttonPane.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

		this.placeholdersField = new JCheckBox("Insert worldspace placeholders", this.insertPlaceholders);
		this.placeholdersField.setBackground(Main.backgroundColor);
		buttonPane.add(this.placeholdersField);

		buttonPane.add(Box.createGlue());

		JButton button = new JButton("Move to master index");
		button.setActionCommand("move to master index");
		button.addActionListener(this);
		buttonPane.add(button);

		button = new JButton("Cancel");
		button.setActionCommand("cancel");
		button.addActionListener(this);
		buttonPane.add(button);

		setContentPane(buttonPane);
	}

	public static int showDialog(JFrame parent)
	{
		WorldspaceDialog dialog = new WorldspaceDialog(parent);
		dialog.pack();
		dialog.setLocationRelativeTo(parent);
		dialog.setVisible(true);
		int result = dialog.option;
		if ((result == 0) && (dialog.insertPlaceholders))
		{
			result = 1;
		}
		return result;
	}

	public void actionPerformed(ActionEvent ae)
	{
		try
		{
			String action = ae.getActionCommand();
			this.insertPlaceholders = this.placeholdersField.isSelected();
			if (action.equals("move to master index"))
			{
				this.option = 0;
				setVisible(false);
				dispose();
			}
			else if (action.equals("cancel"))
			{
				setVisible(false);
				dispose();
			}
		}
		catch (Throwable exc)
		{
			Main.logException("Exception while processing action event", exc);
		}
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.WorldspaceDialog
 * JD-Core Version:    0.6.0
 */