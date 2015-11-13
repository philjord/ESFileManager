package display;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.DataFormatException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import esmmanager.EsmFileLocations;
import esmmanager.common.PluginException;
import esmmanager.common.data.plugin.Plugin;
import esmmanager.common.data.plugin.PluginGroup;
import esmmanager.common.data.plugin.PluginRecord;
import esmmanager.common.data.plugin.PluginSubrecord;

public class PluginDisplayDialog extends JFrame implements ActionListener, TreeExpansionListener
{
	public static void main(String[] args)
	{
		String generalEsmFile = EsmFileLocations.getGeneralEsmFile();

		System.out.println("loading file " + generalEsmFile);

		File pluginFile = new File(generalEsmFile);
		Plugin plugin = new Plugin(pluginFile);
		try
		{
			plugin.load(true);

			PluginDisplayDialog displayDialog = new PluginDisplayDialog(plugin);
			displayDialog.setTitle("Display of " + pluginFile.getName());
			displayDialog.setSize(800, 800);
			displayDialog.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			displayDialog.setVisible(true);
		}
		catch (PluginException e)
		{
			e.printStackTrace();
		}
		catch (DataFormatException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private JTree pluginTree;

	private JPanel displayPane;

	public PluginDisplayDialog(Plugin plugin)
	{
		pluginTree = new JTree(createPluginNodes(plugin));
		pluginTree.setScrollsOnExpand(true);
		pluginTree.addTreeExpansionListener(this);
		JScrollPane pluginScrollPane = new JScrollPane(pluginTree);
		pluginScrollPane.setHorizontalScrollBarPolicy(32);
		pluginScrollPane.setVerticalScrollBarPolicy(22);
		pluginScrollPane.setPreferredSize(new Dimension(380, 380));

		JPanel treePane = new JPanel();
		treePane.setLayout(new BoxLayout(treePane, 0));
		treePane.setOpaque(true);
		treePane.setBackground(new Color(240, 240, 240));
		treePane.add(pluginScrollPane);
		treePane.add(Box.createHorizontalStrut(5));

		displayPane = new JPanel();
		displayPane.setLayout(new GridLayout(1, 1));
		treePane.add(displayPane);

		pluginTree.addTreeSelectionListener(new TreeSelectionListener()
		{

			@Override
			public void valueChanged(TreeSelectionEvent e)
			{
				displaySubrecordData();
			}

		});

		treePane.add(Box.createHorizontalStrut(5));
		JPanel buttonPane = new JPanel();
		buttonPane.setBackground(new Color(240, 240, 240));

		JButton button = new JButton("Done");
		button.setActionCommand("done");
		button.setHorizontalAlignment(0);
		button.addActionListener(this);
		buttonPane.add(button);
		JPanel contentPane = new JPanel();
		contentPane.setLayout(new BoxLayout(contentPane, 1));
		contentPane.setOpaque(true);
		contentPane.setBackground(new Color(240, 240, 240));
		contentPane.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
		contentPane.add(treePane);
		contentPane.add(Box.createVerticalStrut(15));
		contentPane.add(buttonPane);

		setContentPane(contentPane);

	}

	public void actionPerformed(ActionEvent ae)
	{
		String action = ae.getActionCommand();
		if (action.equals("done"))
		{
			setVisible(false);
			dispose();
		}
	}

	private void displaySubrecordData()
	{
		TreePath treePaths[] = pluginTree.getSelectionPaths();
		if (treePaths != null)
		{
			for (TreePath treePath : treePaths)
			{
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
				Object userObject = node.getUserObject();
				if (userObject instanceof PluginSubrecord)
				{
					displayPane.removeAll();
					displayPane.add(DisplaySubrecordDialog.getTextArea((PluginSubrecord) userObject));

					this.validate();

				}
			}
		}

	}

	private DefaultMutableTreeNode createPluginNodes(Plugin plugin)
	{
		DefaultMutableTreeNode root = new DefaultMutableTreeNode(plugin);
		try
		{
			for (PluginGroup group : plugin.getGroupList())
			{
				DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(group);
				createGroupChildren(groupNode, group);
				root.add(groupNode);
			}
		}
		catch (DataFormatException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		catch (PluginException e)
		{
			e.printStackTrace();
		}

		return root;
	}

	private void createGroupChildren(DefaultMutableTreeNode groupNode, PluginGroup group) throws DataFormatException, IOException,
			PluginException
	{
		for (PluginRecord record : group.getRecordList())
		{
			DefaultMutableTreeNode recordNode = new DefaultMutableTreeNode(record);
			boolean insertNode = false;
			int index = 0;
			if (record instanceof PluginGroup)
			{
				createGroupChildren(recordNode, (PluginGroup) record);
			}
			else
			{
				if (record.getSubrecords().size() > 0)
				{
					recordNode.add(new DefaultMutableTreeNode(null));
				}
				if (group.getGroupType() == 0)
				{
					String groupRecordType = group.getGroupRecordType();
					if (!groupRecordType.equals("WRLD") && !groupRecordType.equals("CELL") && !groupRecordType.equals("DIAL"))
					{
						String editorID = record.getEditorID();
						Enumeration<?> nodes = groupNode.children();
						while (nodes.hasMoreElements())
						{
							DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodes.nextElement();
							PluginRecord nodeRecord = (PluginRecord) node.getUserObject();
							String nodeEditorID = nodeRecord.getEditorID();
							if (nodeEditorID != null && editorID.compareToIgnoreCase(nodeEditorID) < 0)
							{
								insertNode = true;
								break;
							}
							index++;
						}
					}
				}
			}
			if (insertNode)
			{
				groupNode.insert(recordNode, index);
			}
			else
			{
				groupNode.add(recordNode);
			}
		}

	}

	private void createRecordChildren(DefaultMutableTreeNode recordNode, PluginRecord record)
	{
		for (PluginSubrecord subrecord : record.getSubrecords())
		{
			DefaultMutableTreeNode subrecordNode = new DefaultMutableTreeNode(subrecord);
			recordNode.add(subrecordNode);
		}
	}

	public void treeExpanded(TreeExpansionEvent event)
	{
		JTree tree = (JTree) event.getSource();
		TreePath treePath = event.getPath();
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
		Object userObject = node.getUserObject();
		if ((userObject instanceof PluginRecord) && !(userObject instanceof PluginGroup))
		{
			PluginRecord record = (PluginRecord) userObject;
			DefaultMutableTreeNode subrecordNode = (DefaultMutableTreeNode) node.getFirstChild();
			if (subrecordNode.getUserObject() == null)
			{
				node.removeAllChildren();
				createRecordChildren(node, record);
				DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
				model.nodeStructureChanged(node);
			}
		}
	}

	public void treeCollapsed(TreeExpansionEvent treeexpansionevent)
	{
	}

}
