package TES4Gecko;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
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
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

public class SplitDialog extends JDialog implements ActionListener, TreeExpansionListener
{
	private File pluginFile;

	private Plugin plugin;

	private PluginNode pluginNode;

	private DefaultTreeModel pluginTreeModel;

	private JTree pluginTree;

	private File outputMasterFile;

	private Plugin outputMaster;

	private PluginNode outputMasterNode;

	private DefaultTreeModel outputMasterTreeModel;

	private JTree outputMasterTree;

	private File outputPluginFile;

	private Plugin outputPlugin;

	private PluginNode outputPluginNode;

	private DefaultTreeModel outputPluginTreeModel;

	private JTree outputPluginTree;

	private JCheckBox independentField;

	private JButton splitButton;

	public SplitDialog(JFrame parent, File pluginFile, PluginNode pluginNode)
	{
		super(parent, "Split Plugin", true);

		this.pluginFile = pluginFile;
		this.pluginNode = pluginNode;
		this.plugin = pluginNode.getPlugin();

		List<PluginGroup> pluginGroupList = this.plugin.getGroupList();
		List<String> pluginMasterList = this.plugin.getMasterList();
		String parentDirectory = pluginFile.getParent();
		String baseName = pluginFile.getName();
		int index = baseName.lastIndexOf('.');
		if (index > 0)
		{
			baseName = baseName.substring(0, index);
		}

		this.outputMasterFile = new File(parentDirectory + Main.fileSeparator + "OUTPUT_" + baseName + ".esm");
		this.outputMaster = new Plugin(this.outputMasterFile, this.plugin.getCreator(), this.plugin.getSummary(), pluginMasterList);
		this.outputMaster.setMaster(true);
		this.outputMaster.setVersion(this.plugin.getVersion());
		this.outputMasterNode = new PluginNode(this.outputMaster);

		List<PluginGroup> outputGroupList = this.outputMaster.getGroupList();

		for (PluginGroup group : pluginGroupList)
		{
			PluginGroup outputGroup = new PluginGroup(group.getGroupRecordType());
			outputGroupList.add(outputGroup);
			GroupNode groupNode = new GroupNode(outputGroup);
			this.outputMasterNode.insert(groupNode);
		}

		List<String> outputMasterList = new ArrayList<String>(5);
		for (String masterName : pluginMasterList)
		{
			outputMasterList.add(masterName);
		}
		outputMasterList.add(baseName + ".esm");

		this.outputPluginFile = new File(parentDirectory + Main.fileSeparator + "OUTPUT_" + baseName + ".esp");
		this.outputPlugin = new Plugin(this.outputPluginFile, this.plugin.getCreator(), this.plugin.getSummary(), outputMasterList);
		this.outputPlugin.setVersion(this.plugin.getVersion());
		this.outputPluginNode = new PluginNode(this.outputPlugin);

		outputGroupList = this.outputPlugin.getGroupList();
		for (PluginGroup group : pluginGroupList)
		{
			PluginGroup outputGroup = new PluginGroup(group.getGroupRecordType());
			outputGroupList.add(outputGroup);
			GroupNode groupNode = new GroupNode(outputGroup);
			this.outputPluginNode.insert(groupNode);
		}

		this.pluginTreeModel = new DefaultTreeModel(pluginNode);
		this.pluginTree = new JTree(this.pluginTreeModel);
		this.pluginTree.setScrollsOnExpand(true);
		this.pluginTree.addTreeExpansionListener(this);

		JScrollPane pluginScrollPane = new JScrollPane(this.pluginTree);
		pluginScrollPane.setHorizontalScrollBarPolicy(32);
		pluginScrollPane.setVerticalScrollBarPolicy(22);
		pluginScrollPane.setPreferredSize(new Dimension(380, 380));

		JLabel label = new JLabel("Source plugin: " + this.plugin.getName());
		label.setBackground(Main.backgroundColor);

		JPanel pluginPane = new JPanel();
		pluginPane.setLayout(new BoxLayout(pluginPane, 1));
		pluginPane.setBackground(Main.backgroundColor);
		pluginPane.setBorder(BorderFactory.createEtchedBorder(Color.WHITE, Color.BLACK));
		pluginPane.add(label);
		pluginPane.add(pluginScrollPane);

		this.outputMasterTreeModel = new DefaultTreeModel(this.outputMasterNode);
		this.outputMasterTree = new JTree(this.outputMasterTreeModel);
		this.outputMasterTree.setScrollsOnExpand(true);
		this.outputMasterTree.addTreeExpansionListener(this);

		JScrollPane outputMasterScrollPane = new JScrollPane(this.outputMasterTree);
		outputMasterScrollPane.setHorizontalScrollBarPolicy(32);
		outputMasterScrollPane.setVerticalScrollBarPolicy(22);
		outputMasterScrollPane.setPreferredSize(new Dimension(380, 380));

		label = new JLabel("Output master: " + this.outputMaster.getName());
		label.setBackground(Main.backgroundColor);

		JPanel outputMasterPane = new JPanel();
		outputMasterPane.setLayout(new BoxLayout(outputMasterPane, 1));
		outputMasterPane.setBackground(Main.backgroundColor);
		outputMasterPane.setBorder(BorderFactory.createEtchedBorder(Color.WHITE, Color.BLACK));
		outputMasterPane.add(label);
		outputMasterPane.add(outputMasterScrollPane);

		this.outputPluginTreeModel = new DefaultTreeModel(this.outputPluginNode);
		this.outputPluginTree = new JTree(this.outputPluginTreeModel);
		this.outputPluginTree.setScrollsOnExpand(true);
		this.outputPluginTree.addTreeExpansionListener(this);

		JScrollPane outputPluginScrollPane = new JScrollPane(this.outputPluginTree);
		outputPluginScrollPane.setHorizontalScrollBarPolicy(32);
		outputPluginScrollPane.setVerticalScrollBarPolicy(22);
		outputPluginScrollPane.setPreferredSize(new Dimension(380, 380));

		label = new JLabel("Output plugin: " + this.outputPlugin.getName());
		label.setBackground(Main.backgroundColor);

		JPanel outputPluginPane = new JPanel();
		outputPluginPane.setLayout(new BoxLayout(outputPluginPane, 1));
		outputPluginPane.setBackground(Main.backgroundColor);
		outputPluginPane.setBorder(BorderFactory.createEtchedBorder(Color.WHITE, Color.BLACK));
		outputPluginPane.add(label);
		outputPluginPane.add(outputPluginScrollPane);

		JPanel treePane = new JPanel();
		treePane.setLayout(new BoxLayout(treePane, 0));
		treePane.setBackground(Main.backgroundColor);
		treePane.add(pluginPane);
		treePane.add(Box.createHorizontalStrut(10));
		treePane.add(outputMasterPane);
		treePane.add(Box.createHorizontalStrut(10));
		treePane.add(outputPluginPane);

		JPanel buttonPane = new JPanel();
		buttonPane.setBackground(Main.backgroundColor);

		this.independentField = new JCheckBox("Independent ESM/ESP", false);
		this.independentField.setBackground(Main.backgroundColor);
		buttonPane.add(this.independentField);

		buttonPane.add(Box.createHorizontalStrut(15));

		this.splitButton = new JButton("Split Plugin");
		this.splitButton.setActionCommand("split plugin");
		this.splitButton.addActionListener(this);
		buttonPane.add(this.splitButton);

		buttonPane.add(Box.createHorizontalStrut(15));

		JButton button = new JButton("Done");
		button.setActionCommand("done");
		button.addActionListener(this);
		buttonPane.add(button);

		JPanel contentPane = new JPanel(new BorderLayout());
		contentPane.setOpaque(true);
		contentPane.setBackground(Main.backgroundColor);
		contentPane.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		contentPane.add(treePane, "Center");
		contentPane.add(buttonPane, "South");
		contentPane.setPreferredSize(new Dimension(975, 600));
		setContentPane(contentPane);
	}

	public static void showDialog(JFrame parent, File pluginFile, PluginNode pluginNode)
	{
		SplitDialog dialog = new SplitDialog(parent, pluginFile, pluginNode);
		dialog.pack();
		dialog.setLocationRelativeTo(parent);
		dialog.setVisible(true);
	}

	public void actionPerformed(ActionEvent ae)
	{
		try
		{
			String action = ae.getActionCommand();
			if (action.equals("done"))
			{
				setVisible(false);
				dispose();
			}
			else if (action.equals("split plugin"))
			{
				splitPlugin();
			}
		}
		catch (Throwable exc)
		{
			Main.logException("Exception while processing action event", exc);
		}
	}

	public void treeExpanded(TreeExpansionEvent event)
	{
		JTree tree = (JTree) event.getSource();
		TreePath treePath = event.getPath();
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();

		if ((node instanceof RecordNode))
		{
			RecordNode recordNode = (RecordNode) node;
			DefaultMutableTreeNode subrecordNode = (DefaultMutableTreeNode) recordNode.getFirstChild();
			if (subrecordNode.getUserObject() == null)
				try
				{
					recordNode.removeAllChildren();
					createRecordChildren(recordNode);
					DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
					model.nodeStructureChanged(recordNode);
				}
				catch (Throwable exc)
				{
					Main.logException("Exception while creating subrecords", exc);
				}
		}
	}

	public void treeCollapsed(TreeExpansionEvent event)
	{
	}

	private void createRecordChildren(RecordNode recordNode) throws DataFormatException, IOException, PluginException
	{
		List<PluginSubrecord> subrecordList = recordNode.getRecord().getSubrecords();
		for (PluginSubrecord subrecord : subrecordList)
		{
			subrecord.setSpillMode(true);
			DefaultMutableTreeNode subrecordNode = new DefaultMutableTreeNode(subrecord);
			recordNode.add(subrecordNode);
		}
	}

	private void splitPlugin()
	{
		boolean completed = SplitTask.splitPlugin(this, this.pluginFile, this.pluginNode, this.independentField.isSelected(),
				this.outputMasterNode, this.outputPluginNode);
		if (completed)
		{
			this.splitButton.setEnabled(false);

			this.outputMasterTreeModel = new DefaultTreeModel(this.outputMasterNode);
			this.outputMasterTree.setModel(this.outputMasterTreeModel);

			this.outputPluginTreeModel = new DefaultTreeModel(this.outputPluginNode);
			this.outputPluginTree.setModel(this.outputPluginTreeModel);
		}
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.SplitDialog
 * JD-Core Version:    0.6.0
 */