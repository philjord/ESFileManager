package TES4Gecko;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

public class PluginDialog extends JDialog implements ActionListener
{
	private String[] pluginNames;

	private String[] columnNames =
	{ "Priority", "Plugin" };

	private Class<?>[] columnClasses =
	{ Integer.class, String.class };

	private PluginTableModel tableModel;

	private JTable table;

	private JScrollPane scrollPane;

	public PluginDialog(JFrame parent)
	{
		super(parent, "Select Plugins", true);
		setDefaultCloseOperation(2);

		String pluginDirectory = Main.properties.getProperty("plugin.directory");
		this.tableModel = new PluginTableModel(pluginDirectory);
		this.table = new JTable(this.tableModel);
		this.table.setCellSelectionEnabled(true);
		this.table.setSelectionMode(0);
		this.table.setAutoResizeMode(3);

		TableCellRenderer headRenderer = this.table.getTableHeader().getDefaultRenderer();
		if ((headRenderer instanceof DefaultTableCellRenderer))
		{
			((DefaultTableCellRenderer) headRenderer).setHorizontalAlignment(0);
		}
		TableColumnModel columnModel = this.table.getColumnModel();
		columnModel.getColumn(0).setPreferredWidth(50);
		columnModel.getColumn(1).setPreferredWidth(350);

		TableCellRenderer renderer = columnModel.getColumn(1).getCellRenderer();
		if (renderer == null)
			renderer = this.table.getDefaultRenderer(this.columnClasses[1]);
		((DefaultTableCellRenderer) renderer).setBackground(Main.backgroundColor);

		this.scrollPane = new JScrollPane(this.table);

		JPanel buttonPane = new JPanel();
		buttonPane.setBackground(Main.backgroundColor);

		JButton button = new JButton("OK");
		button.setActionCommand("done");
		button.addActionListener(this);
		buttonPane.add(button);

		button = new JButton("Cancel");
		button.setActionCommand("cancel");
		button.addActionListener(this);
		buttonPane.add(button);

		JPanel contentPane = new JPanel(new BorderLayout());
		contentPane.setOpaque(true);
		contentPane.setBackground(Main.backgroundColor);
		contentPane.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
		contentPane
				.add(new JLabel(
						"<html><b>Set the merge priority for two or more plugins.  Plugins with a blank or zero priority will not merged.</b><br><br></html>"),
						"North");
		contentPane.add(this.scrollPane, "Center");
		contentPane.add(buttonPane, "South");
		setContentPane(contentPane);
	}

	public String[] getPluginNames()
	{
		return this.pluginNames;
	}

	public static String[] showDialog(JFrame parent)
	{
		PluginDialog dialog = new PluginDialog(parent);
		dialog.pack();
		dialog.setLocationRelativeTo(parent);
		dialog.setVisible(true);
		return dialog.getPluginNames();
	}

	public void actionPerformed(ActionEvent ae)
	{
		try
		{
			boolean doAction = true;

			if ((this.table.isEditing()) && (!this.table.getCellEditor().stopCellEditing()))
			{
				doAction = false;
			}

			if (doAction)
			{
				String action = ae.getActionCommand();
				if (action.equals("done"))
				{
					int rows = this.tableModel.getRowCount();
					List<PluginTableEntry> entryList = new ArrayList<PluginTableEntry>(rows);

					for (int row = 0; row < rows; row++)
					{
						PluginTableEntry entry = this.tableModel.getPluginTableEntry(row);
						int priority = entry.getMergePriority();
						if (priority > 0)
						{
							int index = 0;
							for (PluginTableEntry checkEntry : entryList)
							{
								if (priority < checkEntry.getMergePriority())
								{
									break;
								}
								index++;
							}

							entryList.add(index, entry);
						}
					}

					int count = entryList.size();
					if (count < 2)
					{
						JOptionPane.showMessageDialog(this, "You must select at least two plugins to merge", "Error", 0);
					}
					else
					{
						this.pluginNames = new String[count];
						int index = 0;
						for (PluginTableEntry entry : entryList)
						{
							this.pluginNames[(index++)] = entry.getPluginName();
						}
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
		}
		catch (Throwable exc)
		{
			Main.logException("Exception while processing action event", exc);
		}
	}

	private class PluginMergeFilter implements FileFilter
	{
		public PluginMergeFilter()
		{
		}

		public boolean accept(File file)
		{
			boolean acceptFile = false;
			if (file.isFile())
			{
				String fileName = file.getName();
				int sep = fileName.lastIndexOf('.');
				if ((sep > 0) && (fileName.substring(sep).equalsIgnoreCase(".esp")))
				{
					acceptFile = true;
				}
			}
			return acceptFile;
		}
	}

	private class PluginTableEntry
	{
		private String pluginName;

		private long lastModified;

		private int mergePriority;

		public PluginTableEntry(String pluginName, long lastModified)
		{
			this.pluginName = pluginName;
			this.lastModified = lastModified;
			this.mergePriority = 0;
		}

		public String getPluginName()
		{
			return this.pluginName;
		}

		public long getLastModified()
		{
			return this.lastModified;
		}

		public int getMergePriority()
		{
			return this.mergePriority;
		}

		public void setMergePriority(int priority)
		{
			this.mergePriority = priority;
		}
	}

	private class PluginTableModel extends AbstractTableModel
	{
		private List<PluginDialog.PluginTableEntry> tableData = new ArrayList<PluginTableEntry>();

		public PluginTableModel(String pluginDirectory)
		{
			File directory;
			if ((pluginDirectory != null) && (pluginDirectory.length() != 0))
			{
				directory = new File(pluginDirectory);
				if (directory.isDirectory())
				{
					File[] fileList = directory.listFiles(new PluginMergeFilter());
					if ((fileList != null) && (fileList.length != 0))
						for (File file : fileList)
						{
							long lastModified = file.lastModified();
							int index = 0;
							for (PluginDialog.PluginTableEntry entry : this.tableData)
							{
								if (lastModified < entry.getLastModified())
								{
									break;
								}
								index++;
							}

							this.tableData.add(index, new PluginTableEntry(file.getName(), lastModified));
						}
				}
			}
		}

		public int getColumnCount()
		{
			return PluginDialog.this.columnNames.length;
		}

		public Class<?> getColumnClass(int column)
		{
			return PluginDialog.this.columnClasses[column];
		}

		public String getColumnName(int column)
		{
			return PluginDialog.this.columnNames[column];
		}

		public int getRowCount()
		{
			return this.tableData.size();
		}

		public boolean isCellEditable(int row, int column)
		{
			return column == 0;
		}

		public Object getValueAt(int row, int column)
		{
			if (row >= this.tableData.size())
			{
				throw new IndexOutOfBoundsException("Table row " + row + " is not valid");
			}
			Object value = null;
			PluginDialog.PluginTableEntry entry = this.tableData.get(row);
			switch (column)
			{
				case 0:
					int priority = entry.getMergePriority();
					if (priority <= 0)
						break;
					value = new Integer(priority);
					break;
				case 1:
					value = entry.getPluginName();
					break;
				default:
					throw new IndexOutOfBoundsException("Table column " + column + " is not valid");
			}

			return value;
		}

		public void setValueAt(Object value, int row, int column)
		{
			if (row >= this.tableData.size())
			{
				throw new IndexOutOfBoundsException("Table row " + row + " is not valid");
			}
			if (column == 0)
			{
				int priority;

				if (value == null)
				{
					priority = 0;
				}
				else
				{
					priority = ((Integer) value).intValue();
					if (priority < 1)
					{
						priority = 0;
					}
				}
				this.tableData.get(row).setMergePriority(priority);
				fireTableCellUpdated(row, column);
			}
		}

		public PluginDialog.PluginTableEntry getPluginTableEntry(int row)
		{
			if (row >= this.tableData.size())
			{
				throw new IndexOutOfBoundsException("Table row " + row + " is not valid");
			}
			return this.tableData.get(row);
		}
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.PluginDialog
 * JD-Core Version:    0.6.0
 */