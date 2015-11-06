package TES4Gecko;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class MasterDialog extends JDialog implements ActionListener
{
	private Plugin plugin;

	private File pluginFile;

	private JList list;

	public MasterDialog(JFrame parent, File pluginFile, Plugin plugin)
	{
		super(parent, pluginFile.getName(), true);
		setDefaultCloseOperation(2);
		this.pluginFile = pluginFile;
		this.plugin = plugin;

		this.list = new JList(plugin.getMasterList().toArray());
		this.list.setSelectionMode(0);
		this.list.setPrototypeCellValue("mmmmmmmmmmmmmmmmmmmmmmmmm");
		JScrollPane listPane = new JScrollPane(this.list);

		JPanel buttonPane = new JPanel(new GridLayout(0, 1, 0, 10));
		buttonPane.setBackground(Main.backgroundColor);

		JButton button = new JButton("Rename");
		button.setActionCommand("rename");
		button.addActionListener(this);
		buttonPane.add(button);

		button = new JButton("Move Up");
		button.setActionCommand("move up");
		button.addActionListener(this);
		buttonPane.add(button);

		button = new JButton("Move Down");
		button.setActionCommand("move down");
		button.addActionListener(this);
		buttonPane.add(button);

		button = new JButton("Remove");
		button.setActionCommand("remove");
		button.addActionListener(this);
		buttonPane.add(button);

		button = new JButton("Done");
		button.setActionCommand("done");
		button.addActionListener(this);
		buttonPane.add(button);

		JPanel contentPane = new JPanel();
		contentPane.setLayout(new BoxLayout(contentPane, 0));
		contentPane.setOpaque(true);
		contentPane.setBackground(Main.backgroundColor);
		contentPane.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
		contentPane.add(listPane);
		contentPane.add(Box.createHorizontalStrut(15));
		contentPane.add(buttonPane);
		setContentPane(contentPane);
	}

	public static void showDialog(JFrame parent, File pluginFile, Plugin plugin)
	{
		MasterDialog dialog = new MasterDialog(parent, pluginFile, plugin);
		dialog.pack();
		dialog.setLocationRelativeTo(parent);
		dialog.setVisible(true);
	}

	public void actionPerformed(ActionEvent ae)
	{
		try
		{
			String action = ae.getActionCommand();
			if (action.equals("rename"))
			{
				renameMaster();
			}
			else if (action.equals("move up"))
			{
				moveMaster(-1);
			}
			else if (action.equals("move down"))
			{
				moveMaster(1);
			}
			else if (action.equals("remove"))
			{
				removeMaster();
			}
			else if (action.equals("done"))
			{
				setVisible(false);
				dispose();
			}

			if ((this.plugin == null) && (isVisible()))
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

	private void renameMaster()
	{
		if (this.list.isSelectionEmpty())
		{
			JOptionPane.showMessageDialog(this, "You must select a master list entry to rename", "Error", 0);
			return;
		}

		int index = this.list.getSelectedIndex();

		JFileChooser chooser = new JFileChooser(Main.pluginDirectory);
		chooser.setDialogTitle("Select Master File");
		chooser.setFileFilter(new PluginFileFilter(true, true, false));
		if (chooser.showDialog(this, "Select") != 0)
		{
			return;
		}
		String newName = chooser.getSelectedFile().getName();

		List<String> masterList = this.plugin.getMasterList();
		String oldName = masterList.get(index);
		masterList.set(index, newName);
		if (SaveTask.savePlugin(this, this.pluginFile, this.plugin))
		{
			this.list.setListData(masterList.toArray());
		}
		else
		{
			masterList.set(index, oldName);
			this.list.clearSelection();
		}
	}

	private void moveMaster(int move)
	{
		boolean moveValid = false;
		List<?> masterList = this.plugin.getMasterList();
		List<FormInfo> formList = this.plugin.getFormList();

		if (this.list.isSelectionEmpty())
		{
			JOptionPane.showMessageDialog(this, "You must select a master list entry to move", "Error", 0);
			return;
		}

		int index = this.list.getSelectedIndex();

		int newIndex = index + move;
		if ((newIndex < 0) || (newIndex >= masterList.size()))
		{
			return;
		}

		int masterCount = masterList.size();
		List<String> newMasterList = new ArrayList<String>(masterCount);
		int[] masterMap = new int[masterCount];
		for (int i = 0; i < masterCount; i++)
		{
			masterMap[i] = i;
			newMasterList.add((String) masterList.get(i));
		}

		masterMap[index] = newIndex;
		newMasterList.set(index, (String) masterList.get(newIndex));
		masterMap[newIndex] = index;
		newMasterList.set(newIndex, (String) masterList.get(index));

		FormAdjust formAdjust = new FormAdjust(masterMap, masterCount);
		try
		{
			for (FormInfo formInfo : formList)
			{
				PluginRecord record = (PluginRecord) formInfo.getSource();
				if ((record == null) || (record.isIgnored()))
				{
					continue;
				}

				record.updateReferences(formAdjust);

				int formID = record.getFormID();
				int masterID = formID >>> 24;
				formID &= 16777215;
				if (masterID < masterCount)
					formID |= masterMap[masterID] << 24;
				else
				{
					formID |= masterCount << 24;
				}
				record.changeFormID(formID);
			}

			moveValid = true;
		}
		catch (PluginException exc)
		{
			JOptionPane.showMessageDialog(this, exc.getMessage(), "Plugin Error", 0);
		}
		catch (DataFormatException exc)
		{
			JOptionPane.showMessageDialog(this, exc.getMessage(), "Compression Error", 0);
		}
		catch (IOException exc)
		{
			JOptionPane.showMessageDialog(this, exc.getMessage(), "I/O Error", 0);
		}
		catch (Throwable exc)
		{
			Main.logException("Master list update failed", exc);
		}

		if (moveValid)
		{
			this.plugin.setMasterList(newMasterList);
			if (SaveTask.savePlugin(this, this.pluginFile, this.plugin))
			{
				this.list.setListData(newMasterList.toArray());
			}

		}

		this.plugin = LoadTask.loadPlugin(this, this.pluginFile);
		this.list.clearSelection();
	}

	private void removeMaster()
	{
		boolean removeValid = false;
		boolean yesToAll = false;
		boolean noToAll = false;

		if (this.list.isSelectionEmpty())
		{
			JOptionPane.showMessageDialog(this, "You must select a master list entry to remove", "Error", 0);
			return;
		}

		int index = this.list.getSelectedIndex();
		List<?> masterList = this.plugin.getMasterList();
		List<FormInfo> formList = this.plugin.getFormList();

		int masterCount = masterList.size();
		List<String> newMasterList = new ArrayList<String>(masterCount - 1);
		int[] masterMap = new int[masterCount];
		for (int i = 0; i < masterCount; i++)
		{
			if (i == index)
			{
				masterMap[i] = -1;
			}
			else
			{
				masterMap[i] = newMasterList.size();
				newMasterList.add((String) masterList.get(i));
			}

		}

		try
		{
			for (FormInfo formInfo : formList)
			{
				PluginRecord record = (PluginRecord) formInfo.getSource();
				if ((record == null) || (record.isIgnored()))
				{
					continue;
				}

				List<PluginSubrecord> subrecords = record.getSubrecords();
				for (PluginSubrecord subrecord : subrecords)
				{
					byte[] subrecordData = subrecord.getSubrecordData();
					int[][] references = subrecord.getReferences();
					if ((references == null) || (references.length == 0))
					{
						continue;
					}

					for (int i = 0; i < references.length; i++)
					{
						int offset = references[i][0];
						int formID = references[i][1];
						if (formID == 0)
						{
							continue;
						}
						int masterID = formID >>> 24;
						formID &= 16777215;

						if (masterID == index)
						{
							if ((!yesToAll) && (!noToAll))
							{
								String text = String.format(
										"%s record %s (%08X) references %s",
										new Object[]
										{ record.getRecordType(), record.getEditorID(), Integer.valueOf(record.getFormID()),
												masterList.get(index) });
								Object[] options =
								{ "Yes", "No", "Yes to All", "No to All" };
								int option = JOptionPane.showOptionDialog(this, text + ".  Do you want to delete this record?", "Error", 1,
										0, null, options, options[2]);
								if (option == 2)
									yesToAll = true;
								if (option == 3)
									noToAll = true;
								else if (option != 0)
								{
									throw new PluginException(text);
								}
							}

							if (noToAll)
								break;
							record.setIgnore(true);
							break;
						}

						if (masterID < masterCount)
							formID |= masterMap[masterID] << 24;
						else
						{
							formID |= masterCount - 1 << 24;
						}
						SerializedElement.setInteger(formID, subrecordData, offset);
					}

					if (record.isIgnored())
					{
						break;
					}

					subrecord.setSubrecordData(subrecordData);
				}

				if (!record.isIgnored())
				{
					record.setSubrecords(subrecords);

					int formID = record.getFormID();
					int masterID = formID >>> 24;
					formID &= 16777215;

					if (masterID == index)
					{
						if (!yesToAll)
						{
							String text = String.format("Plugin modifies %s record %s (%08X) in %s", new Object[]
							{ record.getRecordType(), record.getEditorID(), Integer.valueOf(record.getFormID()), masterList.get(index) });
							Object[] options =
							{ "Yes", "No", "Yes to All" };
							int option = JOptionPane.showOptionDialog(this, text + ".  Do you want to remove this modification?", "Error",
									1, 0, null, options, options[2]);
							if (option == 2)
								yesToAll = true;
							else if (option != 0)
							{
								throw new PluginException(text);
							}
						}

						record.setIgnore(true);
					}
					else
					{
						if (masterID < masterCount)
							formID |= masterMap[masterID] << 24;
						else
						{
							formID |= masterCount - 1 << 24;
						}
						record.changeFormID(formID);
					}
				}
			}

			removeValid = true;
		}
		catch (PluginException exc)
		{
			JOptionPane.showMessageDialog(this, exc.getMessage(), "Plugin Error", 0);
		}
		catch (DataFormatException exc)
		{
			JOptionPane.showMessageDialog(this, exc.getMessage(), "Compression Error", 0);
		}
		catch (IOException exc)
		{
			JOptionPane.showMessageDialog(this, exc.getMessage(), "I/O Error", 0);
		}
		catch (Throwable exc)
		{
			Main.logException("Master list update failed", exc);
		}

		if (removeValid)
		{
			this.plugin.setMasterList(newMasterList);
			if (SaveTask.savePlugin(this, this.pluginFile, this.plugin))
			{
				this.list.setListData(newMasterList.toArray());
			}

		}

		this.plugin = LoadTask.loadPlugin(this, this.pluginFile);
		this.list.clearSelection();
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.MasterDialog
 * JD-Core Version:    0.6.0
 */