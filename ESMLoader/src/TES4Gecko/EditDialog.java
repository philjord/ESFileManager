package TES4Gecko;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.table.TableColumn;

public class EditDialog extends JDialog implements ActionListener
{
	private PluginInfo pluginInfo;

	private Float[] versions;

	private JComboBox versionField;

	private JTextField creatorField;

	private JTextArea summaryField;

	private boolean descriptionUpdated = false;

	private JButton updateButton;

	private JButton cancelButton;

	public EditDialog(JFrame parent, PluginInfo pluginInfo)
	{
		super(parent, pluginInfo.getName(), true);
		setDefaultCloseOperation(2);
		this.pluginInfo = pluginInfo;

		this.versions = new Float[2];
		this.versions[0] = new Float(0.8F);
		this.versions[1] = new Float(1.0F);

		StringBuilder summary = new StringBuilder(pluginInfo.getSummary());
		int index = 0;
		while (true)
		{
			index = summary.indexOf("\r\n", index);
			if (index < 0)
			{
				break;
			}
			summary.delete(index, index + 1);
			index++;
		}

		Dimension labelSize = new Dimension(70, 12);

		JPanel versionPane = new JPanel();
		versionPane.setOpaque(false);
		JLabel label = new JLabel("Version: ", 10);
		label.setPreferredSize(labelSize);
		this.versionField = new JComboBox(this.versions);
		this.versionField.setSelectedItem(new Float(pluginInfo.getVersion()));
		versionPane.add(label);
		versionPane.add(this.versionField);

		JPanel creatorPane = new JPanel();
		creatorPane.setOpaque(false);
		label = new JLabel("Creator: ", 10);
		label.setPreferredSize(labelSize);
		this.creatorField = new JTextField(pluginInfo.getCreator(), 32);
		creatorPane.add(label);
		creatorPane.add(this.creatorField);

		JPanel summaryPane = new JPanel();
		summaryPane.setOpaque(false);
		label = new JLabel("Summary :", 10);
		label.setPreferredSize(labelSize);
		this.summaryField = new JTextArea(summary.toString(), 8, 32);
		this.summaryField.setLineWrap(true);
		this.summaryField.setWrapStyleWord(true);
		this.summaryField.setFont(this.creatorField.getFont());
		JScrollPane scrollPane = new JScrollPane(this.summaryField);
		summaryPane.add(label);
		summaryPane.add(scrollPane);

		JPanel buttonPane = new JPanel();
		buttonPane.setBackground(Main.backgroundColor);

		this.updateButton = new JButton("Update");
		this.updateButton.setActionCommand("update");
		this.updateButton.addActionListener(this);
		buttonPane.add(this.updateButton);

		this.cancelButton = new JButton("Cancel");
		this.cancelButton.setActionCommand("cancel");
		this.cancelButton.addActionListener(this);
		buttonPane.add(this.cancelButton);

		JPanel contentPane = new JPanel();
		contentPane.setLayout(new BoxLayout(contentPane, 1));
		contentPane.setOpaque(true);
		contentPane.setBackground(Main.backgroundColor);
		contentPane.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
		contentPane.add(creatorPane);
		contentPane.add(summaryPane);
		contentPane.add(versionPane);
		contentPane.add(Box.createVerticalStrut(15));
		contentPane.add(buttonPane);
		setContentPane(contentPane);
	}

	public boolean isUpdated()
	{
		return this.descriptionUpdated;
	}

	public static boolean showDialog(JFrame parent, PluginInfo pluginInfo)
	{
		EditDialog dialog = new EditDialog(parent, pluginInfo);
		dialog.pack();
		dialog.setLocationRelativeTo(parent);
		dialog.setVisible(true);
		return dialog.isUpdated();
	}

	public void actionPerformed(ActionEvent ae)
	{
		try
		{
			String action = ae.getActionCommand();
			if (action.equals("update"))
			{
				Float version = (Float) this.versionField.getSelectedItem();
				String creator = this.creatorField.getText();
				StringBuilder summary = new StringBuilder(this.summaryField.getText());

				if (version == null)
				{
					version = new Float(0.8F);
				}
				if (creator.length() == 0)
				{
					creator = new String("DEFAULT");
				}

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

				this.pluginInfo.setVersion(version.floatValue());
				this.pluginInfo.setCreator(creator);
				this.pluginInfo.setSummary(summary.toString());
				this.descriptionUpdated = true;
				setVisible(false);
				this.cancelButton.removeActionListener(this);
				this.updateButton.removeActionListener(this);
				removeAllComponents(this);
				dispose();
			}
			else if (action.equals("cancel"))
			{
				setVisible(false);

				removeAllComponents(this);
				dispose();
			}
		}
		catch (Throwable exc)
		{
			Main.logException("Exception while processing action event", exc);
		}
	}

	public static void removeAllComponents(Container cont)
	{
		Component[] components = cont.getComponents();

		for (int i = 0; i < components.length; i++)
		{
			Component comp = components[i];
			if (comp == null)
				continue;
			if ((comp instanceof JTree))
			{
				((JTree) comp).setCellRenderer(null);
			}
			else if ((comp instanceof JTable))
			{
				((JTable) comp).setDefaultRenderer(Object.class, null);
				removeTableColumnRenderers((JTable) comp);
			}
			cont.remove(comp);
			if (!(comp instanceof Container))
				continue;
			removeAllComponents((Container) comp);
		}

		if ((cont instanceof Window))
			((Window) cont).dispose();
	}

	private static void removeTableColumnRenderers(JTable table)
	{
		for (Enumeration cols = table.getColumnModel().getColumns(); cols.hasMoreElements();)
		{
			((TableColumn) cols.nextElement()).setCellRenderer(null);
		}
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.EditDialog
 * JD-Core Version:    0.6.0
 */