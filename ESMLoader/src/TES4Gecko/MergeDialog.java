package TES4Gecko;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class MergeDialog extends JDialog implements ActionListener
{
	private PluginInfo pluginInfo;

	private JTextField nameField;

	private JTextField creatorField;

	private JTextArea summaryField;

	private JCheckBox deleteLastConflictField;

	private JCheckBox editConflictsField;

	public MergeDialog(JFrame parent, String creator, String summary)
	{
		super(parent, "Merged Plugin", true);
		setDefaultCloseOperation(2);
		Color backgroundColor = Main.backgroundColor;

		Dimension labelSize = new Dimension(70, 12);
		JPanel namePane = new JPanel();
		namePane.setOpaque(false);
		JLabel label = new JLabel("Name:", 10);
		label.setPreferredSize(labelSize);
		namePane.add(label);
		this.nameField = new JTextField("Merged Plugin.esp", 32);
		namePane.add(this.nameField);

		JPanel creatorPane = new JPanel();
		creatorPane.setOpaque(false);
		label = new JLabel("Creator:", 10);
		label.setPreferredSize(labelSize);
		creatorPane.add(label);
		this.creatorField = new JTextField(creator, 32);
		creatorPane.add(this.creatorField);

		JPanel summaryPane = new JPanel();
		summaryPane.setOpaque(false);
		label = new JLabel("Summary:", 10);
		label.setPreferredSize(labelSize);
		summaryPane.add(label);
		this.summaryField = new JTextArea(summary, 8, 32);
		this.summaryField.setLineWrap(true);
		this.summaryField.setWrapStyleWord(true);
		this.summaryField.setFont(this.creatorField.getFont());
		JScrollPane scrollPane = new JScrollPane(this.summaryField);
		summaryPane.add(scrollPane);

		JPanel checkBoxPane = new JPanel();
		checkBoxPane.setLayout(new BoxLayout(checkBoxPane, 1));
		checkBoxPane.setBackground(backgroundColor);

		this.deleteLastConflictField = new JCheckBox("Delete last master record conflict", false);
		this.deleteLastConflictField.setOpaque(false);
		checkBoxPane.add(this.deleteLastConflictField);

		this.editConflictsField = new JCheckBox("Edit master leveled list conflicts", false);
		this.editConflictsField.setOpaque(false);
		checkBoxPane.add(this.editConflictsField);

		JPanel buttonPane = new JPanel();
		buttonPane.setBackground(backgroundColor);

		JButton button = new JButton("OK");
		button.setActionCommand("done");
		button.addActionListener(this);
		buttonPane.add(button);

		button = new JButton("Cancel");
		button.setActionCommand("cancel");
		button.addActionListener(this);
		buttonPane.add(button);

		JPanel contentPane = new JPanel();
		contentPane.setLayout(new BoxLayout(contentPane, 1));
		contentPane.setOpaque(true);
		contentPane.setBackground(backgroundColor);
		contentPane.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
		contentPane.add(namePane);
		contentPane.add(creatorPane);
		contentPane.add(summaryPane);
		contentPane.add(Box.createVerticalStrut(15));
		contentPane.add(checkBoxPane);
		contentPane.add(Box.createVerticalStrut(15));
		contentPane.add(buttonPane);
		setContentPane(contentPane);
	}

	public PluginInfo getInfo()
	{
		return this.pluginInfo;
	}

	public static PluginInfo showDialog(JFrame parent, String creator, String summary)
	{
		MergeDialog dialog = new MergeDialog(parent, creator, summary);
		dialog.pack();
		dialog.setLocationRelativeTo(parent);
		dialog.setVisible(true);
		return dialog.getInfo();
	}

	public void actionPerformed(ActionEvent ae)
	{
		try
		{
			String action = ae.getActionCommand();
			if (action.equals("done"))
			{
				String name = this.nameField.getText();
				if (name.length() == 0)
				{
					JOptionPane.showMessageDialog(this, "You must specify a name for the merged plugin", "Error", 0);
				}
				else
				{
					String creator = this.creatorField.getText();
					if (creator.length() == 0)
					{
						creator = new String("DEFAULT");
					}
					StringBuilder summary = new StringBuilder(this.summaryField.getText());
					int index = 0;
					while (true)
					{
						index = summary.indexOf("\n", index);
						if (index < 0)
						{
							break;
						}
						if ((index == 0) || (summary.charAt(index - 1) != '\r'))
						{
							summary.insert(index, "\r");
							index += 2;
							continue;
						}
						index++;
					}

					this.pluginInfo = new PluginInfo(name, creator, summary.toString());
					this.pluginInfo.setDeleteLastConflict(this.deleteLastConflictField.isSelected());
					this.pluginInfo.setEditConflicts(this.editConflictsField.isSelected());
					setVisible(false);
					dispose();
				}
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
 * Qualified Name:     TES4Gecko.MergeDialog
 * JD-Core Version:    0.6.0
 */