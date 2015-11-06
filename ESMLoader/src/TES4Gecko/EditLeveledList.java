package TES4Gecko;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

public class EditLeveledList implements Runnable
{
	private JDialog parent;

	private Master[] masters;

	private PluginRecord mergedRecord;

	private PluginRecord pluginRecord;

	private Map<Integer, FormInfo> mergedFormMap;

	public EditLeveledList(JDialog parent, PluginRecord mergedRecord, PluginRecord pluginRecord, Map<Integer, FormInfo> mergedFormMap,
			Master[] masters)
	{
		this.parent = parent;
		this.masters = masters;
		this.mergedRecord = mergedRecord;
		this.pluginRecord = pluginRecord;
		this.mergedFormMap = mergedFormMap;
	}

	public static void showWorkerDialog(JDialog parent, PluginRecord mergedRecord, PluginRecord pluginRecord,
			Map<Integer, FormInfo> mergedFormMap, Master[] masters)
	{
		try
		{
			EditLeveledList task = new EditLeveledList(parent, mergedRecord, pluginRecord, mergedFormMap, masters);
			SwingUtilities.invokeAndWait(task);
		}
		catch (InterruptedException exc)
		{
			Main.logException("Edit dialog interrupted", exc);
		}
		catch (Throwable exc)
		{
			Main.logException("Exception while displaying edit dialog", exc);
		}
	}

	public void run()
	{
		EditLeveledListDialog dialog = new EditLeveledListDialog();
		dialog.pack();
		dialog.setLocationRelativeTo(this.parent);
		dialog.setVisible(true);
	}

	private class EditLeveledListDialog extends JDialog implements ActionListener, ListSelectionListener
	{
		private String[] columnNames =
		{ "Level", "Count", "Editor ID" };

		private Class<?>[] columnClasses =
		{ Integer.class, Integer.class, String.class };

		List<PluginSubrecord> mergedSubrecordList;

		List<PluginSubrecord> pluginSubrecordList;

		private JTable mergedTable;

		private LeveledListTableModel mergedTableModel;

		private ListSelectionModel mergedSelectionModel;

		private JTable pluginTable;

		private LeveledListTableModel pluginTableModel;

		private ListSelectionModel pluginSelectionModel;

		private JCheckBox allLevelsField;

		private JCheckBox allItemsField;

		private JCheckBox allSpellsField;

		private JFormattedTextField chanceField;

		public EditLeveledListDialog()
		{
			super(new Frame(), "Edit leveled list: " + EditLeveledList.this.mergedRecord.getEditorID(), true);
			setDefaultCloseOperation(2);
			try
			{
				Color backgroundColor = Main.backgroundColor;
				boolean allLevels = false;
				boolean allItems = false;
				boolean allSpells = false;
				int chanceNone = 0;

				this.mergedSubrecordList = EditLeveledList.this.mergedRecord.getSubrecords();
				this.pluginSubrecordList = EditLeveledList.this.pluginRecord.getSubrecords();

				for (PluginSubrecord subrecord : this.mergedSubrecordList)
				{
					String subrecordType = subrecord.getSubrecordType();
					if (subrecordType.equals("LVLD"))
					{
						byte[] subrecordData = subrecord.getSubrecordData();
						chanceNone = subrecordData[0] & 0xFF;
					}
					else if (subrecordType.equals("LVLF"))
					{
						byte[] subrecordData = subrecord.getSubrecordData();
						if ((subrecordData[0] & 0x1) != 0)
							allLevels = true;
						if ((subrecordData[0] & 0x2) != 0)
							allItems = true;
						if ((subrecordData[0] & 0x4) != 0)
						{
							allSpells = true;
						}

					}

				}

				JPanel attributesPane = new JPanel(new GridLayout(0, 1));
				attributesPane.setBackground(backgroundColor);

				this.allLevelsField = new JCheckBox("Calculate from all levels <= PC's level", allLevels);
				this.allLevelsField.setBackground(backgroundColor);
				attributesPane.add(this.allLevelsField);

				this.allItemsField = new JCheckBox("Calculate for each item in count", allItems);
				this.allItemsField.setBackground(backgroundColor);
				attributesPane.add(this.allItemsField);

				if (EditLeveledList.this.mergedRecord.getRecordType().equals("LVSP"))
				{
					this.allSpellsField = new JCheckBox("Use all spells", allSpells);
					this.allSpellsField.setBackground(backgroundColor);
					attributesPane.add(this.allSpellsField);
				}

				JLabel title = new JLabel("Chance none  ", 2);
				title.setOpaque(false);

				this.chanceField = new JFormattedTextField(new EditNumber(true, false));
				this.chanceField.setInputVerifier(new EditInputVerifier(false));
				this.chanceField.setColumns(3);
				this.chanceField.setValue(new Integer(chanceNone));

				JPanel chancePane = new JPanel();
				chancePane.setBackground(backgroundColor);
				chancePane.add(title);
				chancePane.add(this.chanceField);
				chancePane.add(Box.createGlue());
				attributesPane.add(chancePane);

				JPanel topPane = new JPanel(new BorderLayout());
				topPane.setBackground(backgroundColor);
				topPane.add(attributesPane, "West");
				topPane.add(Box.createGlue(), "Center");

				this.mergedTableModel = new LeveledListTableModel(this.mergedSubrecordList, true);
				this.mergedTable = new JTable(this.mergedTableModel);
				this.mergedTable.setSelectionMode(0);
				this.mergedTable.setAutoResizeMode(3);
				this.mergedTable.setCellSelectionEnabled(true);
				this.mergedSelectionModel = this.mergedTable.getSelectionModel();
				this.mergedSelectionModel.addListSelectionListener(this);

				TableCellRenderer headRenderer = this.mergedTable.getTableHeader().getDefaultRenderer();
				if ((headRenderer instanceof DefaultTableCellRenderer))
				{
					((DefaultTableCellRenderer) headRenderer).setHorizontalAlignment(0);
				}
				TableColumnModel columnModel = this.mergedTable.getColumnModel();
				columnModel.getColumn(0).setPreferredWidth(50);
				columnModel.getColumn(1).setPreferredWidth(50);
				columnModel.getColumn(2).setPreferredWidth(200);

				JScrollPane mergedScrollPane = new JScrollPane(this.mergedTable);
				mergedScrollPane.setVerticalScrollBarPolicy(22);
				Dimension preferredSize = new Dimension(300, mergedScrollPane.getPreferredSize().height);
				mergedScrollPane.setPreferredSize(preferredSize);

				JPanel mergedPane = new JPanel();
				mergedPane.setLayout(new BoxLayout(mergedPane, 1));
				mergedPane.setBackground(backgroundColor);
				title = new JLabel("Merged Leveled List", 0);
				title.setOpaque(false);
				title.setFont(title.getFont().deriveFont(1));
				mergedPane.add(title);
				mergedPane.add(mergedScrollPane);

				this.pluginTableModel = new LeveledListTableModel(this.pluginSubrecordList, false);
				this.pluginTable = new JTable(this.pluginTableModel);
				this.pluginTable.setSelectionMode(0);
				this.pluginTable.setAutoResizeMode(3);
				this.pluginSelectionModel = this.pluginTable.getSelectionModel();
				this.pluginSelectionModel.addListSelectionListener(this);

				headRenderer = this.pluginTable.getTableHeader().getDefaultRenderer();
				if ((headRenderer instanceof DefaultTableCellRenderer))
				{
					((DefaultTableCellRenderer) headRenderer).setHorizontalAlignment(0);
				}
				columnModel = this.pluginTable.getColumnModel();
				columnModel.getColumn(0).setPreferredWidth(50);
				columnModel.getColumn(1).setPreferredWidth(50);
				columnModel.getColumn(2).setPreferredWidth(200);

				JScrollPane pluginScrollPane = new JScrollPane(this.pluginTable);
				pluginScrollPane.setVerticalScrollBarPolicy(22);
				pluginScrollPane.setPreferredSize(preferredSize);

				JPanel pluginPane = new JPanel();
				pluginPane.setLayout(new BoxLayout(pluginPane, 1));
				pluginPane.setBackground(backgroundColor);
				title = new JLabel("Plugin Leveled List", 0);
				title.setOpaque(false);
				title.setFont(title.getFont().deriveFont(1));
				pluginPane.add(title);
				pluginPane.add(pluginScrollPane);

				JPanel tablePane = new JPanel();
				tablePane.setBackground(backgroundColor);
				tablePane.setLayout(new BoxLayout(tablePane, 0));
				tablePane.add(mergedPane);
				tablePane.add(Box.createHorizontalStrut(15));
				tablePane.add(pluginPane);

				JPanel buttonPane = new JPanel();
				buttonPane.setBackground(backgroundColor);

				JButton button = new JButton("Copy Plugin Item");
				button.setActionCommand("copy");
				button.addActionListener(this);
				buttonPane.add(button);

				buttonPane.add(Box.createHorizontalStrut(10));

				button = new JButton("Delete Merged Item");
				button.setActionCommand("delete");
				button.addActionListener(this);
				buttonPane.add(button);

				buttonPane.add(Box.createHorizontalStrut(10));

				button = new JButton("Done");
				button.setActionCommand("done");
				button.addActionListener(this);
				buttonPane.add(button);

				JPanel contentPane = new JPanel();
				contentPane.setLayout(new BoxLayout(contentPane, 1));
				contentPane.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
				contentPane.setBackground(backgroundColor);
				contentPane.add(topPane);
				contentPane.add(Box.createVerticalStrut(20));
				contentPane.add(tablePane);
				contentPane.add(Box.createVerticalStrut(20));
				contentPane.add(buttonPane);
				setContentPane(contentPane);
			}
			catch (Throwable exc)
			{
				//JLabel title;
				Main.logException("Exception while constructing edit dialog", exc);
			}
		}

		public void actionPerformed(ActionEvent ae)
		{
			try
			{
				boolean doAction = true;
				String action = ae.getActionCommand();

				if (!this.chanceField.isEditValid())
				{
					doAction = false;
				}

				if ((this.mergedTable.isEditing()) && (!this.mergedTable.getCellEditor().stopCellEditing()))
				{
					doAction = false;
				}

				if (doAction)
					if (action.equals("copy"))
					{
						copyListItem();
					}
					else if (action.equals("delete"))
					{
						deleteListItem();
					}
					else if ((action.equals("done")) && (updateMergedRecord()))
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

		private void copyListItem()
		{
			int row = this.pluginTable.getSelectedRow();
			if (row < 0)
			{
				JOptionPane.showMessageDialog(this, "You must select a plugin list item to copy", "Error", 0);
				return;
			}

			PluginSubrecord subrecord = this.pluginTableModel.getSubrecord(row);
			this.pluginTableModel.deleteRow(row);
			this.mergedTableModel.addRow(subrecord);
		}

		private void deleteListItem()
		{
			int row = this.mergedTable.getSelectedRow();
			if (row < 0)
			{
				JOptionPane.showMessageDialog(this, "You must select a merged list item to delete", "Error", 0);
				return;
			}

			this.mergedTableModel.deleteRow(row);
		}

		private boolean updateMergedRecord() throws DataFormatException, IOException
		{
			int chanceNone = ((Integer) this.chanceField.getValue()).intValue();
			if ((chanceNone < 0) || (chanceNone > 100))
			{
				JOptionPane.showMessageDialog(this, "You must enter a chance between 0 and 100", "Error", 0);
				return false;
			}

			boolean allLevels = this.allLevelsField.isSelected();
			boolean allItems = this.allItemsField.isSelected();
			boolean allSpells = false;
			if (this.allSpellsField != null)
			{
				allSpells = this.allSpellsField.isSelected();
			}

			for (PluginSubrecord subrecord : this.mergedSubrecordList)
			{
				if (subrecord.getSubrecordType().equals("LVLD"))
				{
					byte[] subrecordData = subrecord.getSubrecordData();
					subrecordData[0] = (byte) chanceNone;
				}
				else if (subrecord.getSubrecordType().equals("LVLF"))
				{
					byte[] subrecordData = subrecord.getSubrecordData();
					int tmp150_149 = 0;
					byte[] tmp150_147 = subrecordData;
					tmp150_147[tmp150_149] = (byte) (tmp150_147[tmp150_149] & 0xFFFFFFF8);
					if (allLevels)
					{
						int tmp164_163 = 0;
						byte[] tmp164_161 = subrecordData;
						tmp164_161[tmp164_163] = (byte) (tmp164_161[tmp164_163] | 0x1);
					}
					if (allItems)
					{
						int tmp177_176 = 0;
						byte[] tmp177_174 = subrecordData;
						tmp177_174[tmp177_176] = (byte) (tmp177_174[tmp177_176] | 0x2);
					}
					if (allSpells)
					{
						int tmp191_190 = 0;
						byte[] tmp191_188 = subrecordData;
						tmp191_188[tmp191_190] = (byte) (tmp191_188[tmp191_190] | 0x4);
					}

				}

			}

			EditLeveledList.this.mergedRecord.setSubrecords(this.mergedSubrecordList);
			return true;
		}

		public void valueChanged(ListSelectionEvent se)
		{
			ListSelectionModel lsm = (ListSelectionModel) se.getSource();

			if ((!lsm.getValueIsAdjusting()) && (!lsm.isSelectionEmpty()))
				if (lsm == this.mergedSelectionModel)
				{
					if (!this.pluginSelectionModel.isSelectionEmpty())
						this.pluginSelectionModel.clearSelection();
				}
				else if (!this.mergedSelectionModel.isSelectionEmpty())
					this.mergedSelectionModel.clearSelection();
		}

		private class LeveledListEntry
		{
			private PluginSubrecord subrecord;

			private String editorID;

			private int level;

			private int count;

			public LeveledListEntry(PluginSubrecord subrecord, String editorID, int level, int count)
			{
				this.subrecord = subrecord;
				this.editorID = editorID;
				this.level = level;
				this.count = count;
			}

			public PluginSubrecord getSubrecord()
			{
				return this.subrecord;
			}

			public String getEditorID()
			{
				return this.editorID;
			}

			public int getItemLevel()
			{
				return this.level;
			}

			public void setItemLevel(int level)
			{
				this.level = level;
				try
				{
					byte[] subrecordData = this.subrecord.getSubrecordData();
					subrecordData[0] = (byte) level;
					subrecordData[1] = (byte) (level >>> 8);
					this.subrecord.setSubrecordData(subrecordData);
				}
				catch (IOException exc)
				{
					Main.logException("Exception while setting subrecord data", exc);
				}
			}

			public int getItemCount()
			{
				return this.count;
			}

			public void setItemCount(int count)
			{
				this.count = count;
				try
				{
					byte[] subrecordData = this.subrecord.getSubrecordData();
					subrecordData[8] = (byte) count;
					subrecordData[9] = (byte) (count >>> 8);
					this.subrecord.setSubrecordData(subrecordData);
				}
				catch (IOException exc)
				{
					Main.logException("Exception while getting subrecord data", exc);
				}
			}
		}

		private class LeveledListTableModel extends AbstractTableModel
		{
			private List<PluginSubrecord> subrecordList;

			private List<LeveledListEntry> tableData;

			private boolean isEditable;

			public LeveledListTableModel(List<PluginSubrecord> subrecordList, boolean isEditable)
			{
				this.subrecordList = subrecordList;
				this.isEditable = isEditable;

				this.tableData = new ArrayList<LeveledListEntry>(subrecordList.size());

				for (PluginSubrecord subrecord : subrecordList)
				{
					if (!subrecord.getSubrecordType().equals("LVLO"))
						continue;

					byte[] subrecordData;
					try
					{
						subrecordData = subrecord.getSubrecordData();
					}
					catch (IOException exc)
					{

						Main.logException("Exception while getting subrecord data", exc);
						subrecordData = new byte[0];
					}

					if (subrecordData.length >= 12)
					{
						int level = subrecordData[0] & 0xFF | (subrecordData[1] & 0xFF) << 8;
						int count = subrecordData[8] & 0xFF | (subrecordData[9] & 0xFF) << 8;
						int formID = subrecordData[4] & 0xFF | (subrecordData[5] & 0xFF) << 8 | (subrecordData[6] & 0xFF) << 16
								| (subrecordData[7] & 0xFF) << 24;
						int masterID = formID >>> 24;
						FormInfo formInfo;
						Integer objFormID;

						if (masterID < EditLeveledList.this.masters.length)
						{
							Master master = EditLeveledList.this.masters[masterID];
							masterID = master.getMasterList().size();
							formID = formID & 0xFFFFFF | masterID << 24;
							objFormID = new Integer(formID);
							formInfo = master.getFormMap().get(objFormID);
						}
						else
						{
							objFormID = new Integer(formID);
							formInfo = EditLeveledList.this.mergedFormMap.get(objFormID);
						}
						String editorID;

						if (formInfo != null)
							editorID = formInfo.getMergedEditorID();
						else
						{
							editorID = String.format("(%08X)", new Object[]
							{ objFormID });
						}
						this.tableData.add(new LeveledListEntry(subrecord, editorID, level, count));
					}
				}
			}

			public int getColumnCount()
			{
				return EditLeveledList.EditLeveledListDialog.this.columnNames.length;
			}

			public Class<?> getColumnClass(int column)
			{
				return EditLeveledList.EditLeveledListDialog.this.columnClasses[column];
			}

			public String getColumnName(int column)
			{
				return EditLeveledList.EditLeveledListDialog.this.columnNames[column];
			}

			public int getRowCount()
			{
				return this.tableData.size();
			}

			public boolean isCellEditable(int row, int column)
			{
				return (this.isEditable) && (column < 2);
			}

			public Object getValueAt(int row, int column)
			{
				if (row >= this.tableData.size())
				{
					throw new IndexOutOfBoundsException("Table row " + row + " is not valid");
				}
				Object value = null;
				LeveledListEntry entry = this.tableData.get(row);
				switch (column)
				{
					case 0:
						value = new Integer(entry.getItemLevel());
						break;
					case 1:
						value = new Integer(entry.getItemCount());
						break;
					case 2:
						value = entry.getEditorID();
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
				LeveledListEntry entry = this.tableData.get(row);
				switch (column)
				{
					case 0:
						int oldLevel = entry.getItemLevel();
						int newLevel = ((Integer) value).intValue();
						if (oldLevel == newLevel)
							break;
						PluginSubrecord subrecord = entry.getSubrecord();

						this.tableData.remove(row);
						this.subrecordList.remove(subrecord);
						fireTableRowsDeleted(row, row);

						entry.setItemLevel(newLevel);

						boolean insert = false;
						int index = 0;
						for (LeveledListEntry checkEntry : this.tableData)
						{
							if (newLevel < checkEntry.getItemLevel())
							{
								insert = true;
								break;
							}

							index++;
						}

						if (insert)
							this.tableData.add(index, entry);
						else
						{
							this.tableData.add(entry);
						}
						fireTableRowsInserted(index, index);

						insert = false;
						index = 0;
						for (PluginSubrecord checkSubrecord : this.subrecordList)
						{
							if (checkSubrecord.getSubrecordType().equals("LVLO"))
							{
								try
								{
									byte[] checkSubrecordData = checkSubrecord.getSubrecordData();
									int checkLevel = checkSubrecordData[0] & 0xFF | (checkSubrecordData[1] & 0xFF) << 8;
									if (newLevel < checkLevel)
										insert = true;
								}
								catch (IOException exc)
								{
									Main.logException("Exception while getting subrecord data", exc);
								}
							}

							index++;
						}

						if (insert)
							this.subrecordList.add(index, subrecord);
						else
						{
							this.subrecordList.add(subrecord);
						}
						break;
					case 1:
						int oldCount = entry.getItemCount();
						int newCount = ((Integer) value).intValue();
						if (oldCount == newCount)
							break;
						entry.setItemCount(newCount);
						fireTableCellUpdated(row, column);
				}
			}

			public PluginSubrecord getSubrecord(int row)
			{
				if (row >= this.tableData.size())
				{
					throw new IndexOutOfBoundsException("Table row " + row + " is not valid");
				}
				return this.tableData.get(row).getSubrecord();
			}

			public void addRow(PluginSubrecord subrecord)
			{
				byte[] subrecordData;
				try
				{
					subrecordData = subrecord.getSubrecordData();
				}
				catch (IOException exc)
				{

					Main.logException("Exception while getting subrecord data", exc);
					subrecordData = new byte[0];
				}

				if (subrecordData.length < 12)
				{
					return;
				}

				int level = subrecordData[0] & 0xFF | (subrecordData[1] & 0xFF) << 8;
				int count = subrecordData[8] & 0xFF | (subrecordData[9] & 0xFF) << 8;
				int formID = subrecordData[4] & 0xFF | (subrecordData[5] & 0xFF) << 8 | (subrecordData[6] & 0xFF) << 16
						| (subrecordData[7] & 0xFF) << 24;
				int masterID = formID >>> 24;
				FormInfo formInfo;
				Integer objFormID;

				if (masterID < EditLeveledList.this.masters.length)
				{
					Master master = EditLeveledList.this.masters[masterID];
					masterID = master.getMasterList().size();
					formID = formID & 0xFFFFFF | masterID << 24;
					objFormID = new Integer(formID);
					formInfo = master.getFormMap().get(objFormID);
				}
				else
				{
					objFormID = new Integer(formID);
					formInfo = EditLeveledList.this.mergedFormMap.get(objFormID);
				}
				String editorID;

				if (formInfo != null)
					editorID = formInfo.getMergedEditorID();
				else
				{
					editorID = String.format("(%08X)", new Object[]
					{ objFormID });
				}
				LeveledListEntry entry = new LeveledListEntry(subrecord, editorID, level, count);

				boolean insert = false;
				int index = 0;
				for (LeveledListEntry checkEntry : this.tableData)
				{
					if (level < checkEntry.getItemLevel())
					{
						insert = true;
						break;
					}

					index++;
				}

				if (insert)
					this.tableData.add(index, entry);
				else
				{
					this.tableData.add(entry);
				}
				fireTableRowsInserted(index, index);

				insert = false;
				index = 0;
				for (PluginSubrecord checkSubrecord : this.subrecordList)
				{
					if (checkSubrecord.getSubrecordType().equals("LVLO"))
					{
						try
						{
							byte[] checkSubrecordData = checkSubrecord.getSubrecordData();
							int checkLevel = checkSubrecordData[0] & 0xFF | (checkSubrecordData[1] & 0xFF) << 8;
							if (level < checkLevel)
								insert = true;
						}
						catch (IOException exc)
						{
							Main.logException("Exception while getting subrecord data", exc);
						}
					}

					index++;
				}

				if (insert)
					this.subrecordList.add(index, subrecord);
				else
					this.subrecordList.add(subrecord);
			}

			public void deleteRow(int row)
			{
				if (row >= this.tableData.size())
				{
					throw new IndexOutOfBoundsException("Table row " + row + " is not valid");
				}
				PluginSubrecord subrecord = this.tableData.get(row).getSubrecord();
				this.tableData.remove(row);
				this.subrecordList.remove(subrecord);
				fireTableRowsDeleted(row, row);
			}
		}
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.EditLeveledList
 * JD-Core Version:    0.6.0
 */