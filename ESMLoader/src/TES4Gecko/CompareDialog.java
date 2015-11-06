package TES4Gecko;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

public class CompareDialog extends DisplayPlugin implements ActionListener, TreeExpansionListener
{
	private File pluginFileA;

	private Plugin pluginA;

	private FormAdjust pluginFormAdjustA;

	private PluginNode pluginNodeA;

	private JTree pluginTreeA;

	private DefaultTreeModel pluginTreeModelA;

	private JScrollPane scrollPaneA;

	private JButton expandButtonA;

	private JProgressBar progressBarA;

	private boolean pluginAModified = false;

	private boolean expandingA = false;

	private File pluginFileB;

	private FormAdjust pluginFormAdjustB;

	private Plugin pluginB;

	private PluginNode pluginNodeB;

	private JTree pluginTreeB;

	private DefaultTreeModel pluginTreeModelB;

	private JScrollPane scrollPaneB;

	private JButton expandButtonB;

	private JProgressBar progressBarB;

	private boolean pluginBModified = false;

	private boolean expandingB = false;

	private JScrollPane clipboardScrollPane;

	private boolean synchronizedExpansion = true;

	public CompareDialog(JFrame parent, File pluginFileA, File pluginFileB, PluginNode pluginNodeA, PluginNode pluginNodeB)
	{
		super(parent, "Compare Plugins");

		this.pluginFileA = pluginFileA;
		this.pluginNodeA = pluginNodeA;
		this.pluginA = pluginNodeA.getPlugin();

		this.pluginFileB = pluginFileB;
		this.pluginNodeB = pluginNodeB;
		this.pluginB = pluginNodeB.getPlugin();

		this.pluginTreeModelA = new DefaultTreeModel(pluginNodeA);
		this.pluginTreeA = new JTree(this.pluginTreeModelA);
		this.pluginTreeA.setScrollsOnExpand(true);
		this.pluginTreeA.setCellRenderer(new CompareCellRenderer());
		this.pluginTreeA.addTreeExpansionListener(this);

		this.pluginTreeModelB = new DefaultTreeModel(pluginNodeB);
		this.pluginTreeB = new JTree(this.pluginTreeModelB);
		this.pluginTreeB.setScrollsOnExpand(true);
		this.pluginTreeB.setCellRenderer(new CompareCellRenderer());
		this.pluginTreeB.addTreeExpansionListener(this);

		List<String> pluginMasterListA = this.pluginA.getMasterList();
		List<String> pluginMasterListB = this.pluginB.getMasterList();
		List<String> clipboardMasterList = new ArrayList<String>(pluginMasterListA.size() + pluginMasterListB.size());

		for (String master : pluginMasterListA)
		{
			clipboardMasterList.add(master);
		}
		for (String master : pluginMasterListB)
		{
			if (!clipboardMasterList.contains(master))
			{
				clipboardMasterList.add(master);
			}

		}

		int masterCount = pluginMasterListA.size();
		int[] masterMap = new int[masterCount];
		for (int i = 0; i < masterCount; i++)
		{
			String masterName = pluginMasterListA.get(i);
			masterMap[i] = clipboardMasterList.indexOf(masterName);
		}

		this.pluginFormAdjustA = new FormAdjust(masterMap, clipboardMasterList.size());
		pluginNodeA.setFormAdjust(this.pluginFormAdjustA);

		masterCount = pluginMasterListB.size();
		masterMap = new int[masterCount];
		for (int i = 0; i < masterCount; i++)
		{
			String masterName = pluginMasterListB.get(i);
			masterMap[i] = clipboardMasterList.indexOf(masterName);
		}

		this.pluginFormAdjustB = new FormAdjust(masterMap, clipboardMasterList.size());
		pluginNodeB.setFormAdjust(this.pluginFormAdjustB);

		this.clipboardFile = new File(pluginFileA.getParent() + Main.fileSeparator + "Gecko Clipboard.esp");
		this.clipboard = new Plugin(this.clipboardFile, this.pluginA.getCreator(), this.pluginA.getSummary(), clipboardMasterList);
		this.clipboard.setVersion(Math.max(this.pluginA.getVersion(), this.pluginB.getVersion()));
		this.clipboard.createInitialGroups();

		PluginNode rootNode = new PluginNode(this.clipboard);
		List<PluginGroup> groupList = this.clipboard.getGroupList();
		for (PluginGroup group : groupList)
		{
			GroupNode groupNode = new GroupNode(group);
			rootNode.insert(groupNode);
		}

		this.clipboardTreeModel = new DefaultTreeModel(rootNode);
		this.clipboardTree = new JTree(this.clipboardTreeModel);
		this.clipboardTree.setScrollsOnExpand(true);
		this.clipboardTree.setSelectionModel(null);
		this.clipboardTree.addTreeExpansionListener(this);

		JPanel labelPane = new JPanel();
		labelPane.setBackground(Main.backgroundColor);
		labelPane.add(new JLabel(pluginFileA.getName()));

		this.scrollPaneA = new JScrollPane(this.pluginTreeA);
		this.scrollPaneA.setHorizontalScrollBarPolicy(32);
		this.scrollPaneA.setVerticalScrollBarPolicy(22);
		this.scrollPaneA.setPreferredSize(new Dimension(250, 500));

		JPanel buttonPane = new JPanel(new GridLayout(3, 2, 5, 5));
		buttonPane.setBackground(Main.backgroundColor);

		this.expandButtonA = new JButton("Expand Distinct Nodes");
		this.expandButtonA.setActionCommand("expand distinct A");
		this.expandButtonA.addActionListener(this);
		buttonPane.add(this.expandButtonA);

		JButton button = new JButton("Collapse Top Nodes");
		button.setActionCommand("collapse all A");
		button.addActionListener(this);
		buttonPane.add(button);

		button = new JButton("Toggle Ignore");
		button.setActionCommand("toggle ignore A");
		button.addActionListener(this);
		buttonPane.add(button);

		button = new JButton("Copy to Clipboard");
		button.setActionCommand("copy to clipboard A");
		button.addActionListener(this);
		buttonPane.add(button);

		button = new JButton("Save Plugin");
		button.setActionCommand("save plugin A");
		button.addActionListener(this);
		buttonPane.add(button);

		this.progressBarA = new JProgressBar(0, 100);
		this.progressBarA.setString("Idle");
		this.progressBarA.setStringPainted(true);
		buttonPane.add(this.progressBarA);

		JPanel filePaneA = new JPanel();
		filePaneA.setLayout(new BoxLayout(filePaneA, 1));
		filePaneA.setBackground(Main.backgroundColor);
		filePaneA.setBorder(BorderFactory.createEtchedBorder(Color.WHITE, Color.BLACK));
		filePaneA.add(labelPane);
		filePaneA.add(this.scrollPaneA);
		filePaneA.add(Box.createVerticalStrut(10));
		filePaneA.add(buttonPane);

		labelPane = new JPanel();
		labelPane.setBackground(Main.backgroundColor);
		labelPane.add(new JLabel(pluginFileB.getName()));

		this.scrollPaneB = new JScrollPane(this.pluginTreeB);
		this.scrollPaneB.setHorizontalScrollBarPolicy(32);
		this.scrollPaneB.setVerticalScrollBarPolicy(22);
		this.scrollPaneB.setPreferredSize(new Dimension(250, 500));

		buttonPane = new JPanel(new GridLayout(3, 2, 5, 5));
		buttonPane.setBackground(Main.backgroundColor);

		this.expandButtonB = new JButton("Expand Distinct Nodes");
		this.expandButtonB.setActionCommand("expand distinct B");
		this.expandButtonB.addActionListener(this);
		buttonPane.add(this.expandButtonB);

		button = new JButton("Collapse Top Nodes");
		button.setActionCommand("collapse all B");
		button.addActionListener(this);
		buttonPane.add(button);

		button = new JButton("Toggle Ignore");
		button.setActionCommand("toggle ignore B");
		button.addActionListener(this);
		buttonPane.add(button);

		button = new JButton("Copy to Clipboard");
		button.setActionCommand("copy to clipboard B");
		button.addActionListener(this);
		buttonPane.add(button);

		button = new JButton("Save Plugin");
		button.setActionCommand("save plugin B");
		button.addActionListener(this);
		buttonPane.add(button);

		this.progressBarB = new JProgressBar(0, 100);
		this.progressBarB.setString("Idle");
		this.progressBarB.setStringPainted(true);
		buttonPane.add(this.progressBarB);

		JPanel filePaneB = new JPanel();
		filePaneB.setLayout(new BoxLayout(filePaneB, 1));
		filePaneB.setBackground(Main.backgroundColor);
		filePaneB.setBorder(BorderFactory.createEtchedBorder(Color.WHITE, Color.BLACK));
		filePaneB.add(labelPane);
		filePaneB.add(this.scrollPaneB);
		filePaneB.add(Box.createVerticalStrut(10));
		filePaneB.add(buttonPane);

		labelPane = new JPanel();
		labelPane.setBackground(Main.backgroundColor);
		labelPane.add(new JLabel(this.clipboardFile.getName()));

		this.clipboardScrollPane = new JScrollPane(this.clipboardTree);
		this.clipboardScrollPane.setHorizontalScrollBarPolicy(32);
		this.clipboardScrollPane.setVerticalScrollBarPolicy(22);
		this.clipboardScrollPane.setPreferredSize(new Dimension(250, 500));

		buttonPane = new JPanel(new GridLayout(3, 2, 5, 5));
		buttonPane.setBackground(Main.backgroundColor);

		button = new JButton("Save Clipboard");
		button.setActionCommand("save clipboard");
		button.addActionListener(this);
		buttonPane.add(button);

		buttonPane.add(Box.createGlue());
		buttonPane.add(Box.createGlue());
		buttonPane.add(Box.createGlue());
		buttonPane.add(Box.createGlue());

		JPanel clipboardPane = new JPanel();
		clipboardPane.setLayout(new BoxLayout(clipboardPane, 1));
		clipboardPane.setBackground(Main.backgroundColor);
		clipboardPane.setBorder(BorderFactory.createEtchedBorder(Color.WHITE, Color.BLACK));
		clipboardPane.add(labelPane);
		clipboardPane.add(this.clipboardScrollPane);
		clipboardPane.add(Box.createVerticalStrut(10));
		clipboardPane.add(buttonPane);

		JPanel topPane = new JPanel();
		topPane.setLayout(new BoxLayout(topPane, 0));
		topPane.setBackground(Main.backgroundColor);
		topPane.add(filePaneA);
		topPane.add(Box.createHorizontalStrut(15));
		topPane.add(filePaneB);
		topPane.add(Box.createHorizontalStrut(15));
		topPane.add(clipboardPane);

		JPanel bottomPane = new JPanel();
		bottomPane.setBackground(Main.backgroundColor);

		JCheckBox checkBox = new JCheckBox("Synchronized Expansion", this.synchronizedExpansion);
		checkBox.setBackground(Main.backgroundColor);
		checkBox.setActionCommand("toggle synchronized expansion");
		checkBox.addActionListener(this);
		bottomPane.add(checkBox);

		button = new JButton("Done");
		button.setActionCommand("done");
		button.addActionListener(this);
		bottomPane.add(button);

		JPanel contentPane = new JPanel(new BorderLayout());
		contentPane.setOpaque(true);
		contentPane.setBackground(Main.backgroundColor);
		contentPane.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		contentPane.add(topPane, "Center");
		contentPane.add(bottomPane, "South");
		contentPane.setPreferredSize(new Dimension(975, 650));
		setContentPane(contentPane);

		addWindowListener(new DialogWindowListener());
	}

	public static void showDialog(JFrame parent, File pluginFileA, File pluginFileB, PluginNode pluginNodeA, PluginNode pluginNodeB)
	{
		CompareDialog dialog = new CompareDialog(parent, pluginFileA, pluginFileB, pluginNodeA, pluginNodeB);
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
				closeDialog();
				setVisible(false);
				dispose();
			}
			else if (action.equals("toggle synchronized expansion"))
			{
				this.synchronizedExpansion = (!this.synchronizedExpansion);
			}
			else if (action.equals("expand distinct A"))
			{
				if (this.expandingA)
				{
					this.progressBarA.setValue(0);
					this.progressBarA.setString("Idle");
					this.expandButtonA.setText("Expand Distinct Nodes");
					this.expandingA = false;
				}
				else
				{
					this.expandButtonA.setText("Stop Expanding Nodes");
					this.progressBarA.setString("Expanding");
					this.expandingA = true;
					expandDistinctNodes(this.pluginTreeA, null);
				}
			}
			else if (action.equals("continue expand A"))
			{
				if (this.expandingA)
					expandDistinctNodes(this.pluginTreeA, (Integer) ae.getSource());
			}
			else if (action.equals("expand distinct B"))
			{
				if (this.expandingB)
				{
					this.progressBarB.setValue(0);
					this.progressBarB.setString("Idle");
					this.expandButtonB.setText("Expand Distinct Nodes");
					this.expandingB = false;
				}
				else
				{
					this.expandButtonB.setText("Stop Expanding Nodes");
					this.progressBarB.setString("Expanding");
					this.expandingB = true;
					expandDistinctNodes(this.pluginTreeB, null);
				}
			}
			else if (action.equals("continue expand B"))
			{
				if (this.expandingB)
					expandDistinctNodes(this.pluginTreeB, (Integer) ae.getSource());
			}
			else if (action.equals("collapse all A"))
			{
				collapseTopNodes(this.pluginTreeA);
			}
			else if (action.equals("collapse all B"))
			{
				collapseTopNodes(this.pluginTreeB);
			}
			else if (action.equals("toggle ignore A"))
			{
				if (toggleIgnore(this.pluginTreeA))
					this.pluginAModified = true;
			}
			else if (action.equals("toggle ignore B"))
			{
				if (toggleIgnore(this.pluginTreeB))
					this.pluginBModified = true;
			}
			else if (action.equals("copy to clipboard A"))
			{
				copyRecords(this.pluginTreeA);
			}
			else if (action.equals("copy to clipboard B"))
			{
				copyRecords(this.pluginTreeB);
			}
			else if (action.equals("save plugin A"))
			{
				if ((this.pluginAModified) && (SaveTask.savePlugin(this, this.pluginFileA, this.pluginA)))
				{
					this.pluginAModified = false;
					validateTree(this.pluginTreeA);
				}
			}
			else if (action.equals("save plugin B"))
			{
				if ((this.pluginBModified) && (SaveTask.savePlugin(this, this.pluginFileB, this.pluginB)))
				{
					this.pluginBModified = false;
					validateTree(this.pluginTreeB);
				}
			}
			else if ((action.equals("save clipboard")) && (this.clipboardModified)
					&& (SaveTask.savePlugin(this, this.clipboardFile, this.clipboard)))
			{
				this.clipboardModified = false;
				validateTree(this.clipboardTree);
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
		TreePath path = event.getPath();
		TreeNode node = (TreeNode) path.getLastPathComponent();

		if ((node instanceof RecordNode))
		{
			RecordNode recordNode = (RecordNode) node;
			DefaultMutableTreeNode subrecordNode = (DefaultMutableTreeNode) recordNode.getFirstChild();
			if (subrecordNode.getUserObject() == null)
			{
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

		if ((this.synchronizedExpansion) && ((tree == this.pluginTreeA) || (tree == this.pluginTreeB))
				&& (((node instanceof GroupNode)) || ((node instanceof RecordNode))))
		{
			JTree cmpTree = tree == this.pluginTreeA ? this.pluginTreeB : this.pluginTreeA;
			JScrollPane scrollPane = tree == this.pluginTreeA ? this.scrollPaneB : this.scrollPaneA;
			int scrollValue = scrollPane.getHorizontalScrollBar().getValue();
			TreePath cmpPath = findMatchingPath(cmpTree, path);
			if ((cmpPath != null) && (!cmpTree.isExpanded(cmpPath)))
			{
				cmpTree.expandPath(cmpPath);
				TreeNode cmpNode = (TreeNode) cmpPath.getLastPathComponent();
				int childCount = cmpNode.getChildCount();
				if (childCount > 0)
				{
					int visibleRows = scrollPane.getViewport().getExtentSize().height / cmpTree.getRowHeight();
					int index = Math.min(childCount, visibleRows - 1);
					cmpPath = cmpPath.pathByAddingChild(cmpNode.getChildAt(index - 1));
				}

				cmpTree.scrollPathToVisible(cmpPath);
				scrollPane.getHorizontalScrollBar().setValue(scrollValue);
			}
		}
	}

	public void treeCollapsed(TreeExpansionEvent event)
	{
		JTree tree = (JTree) event.getSource();
		TreePath path = event.getPath();
		TreeNode node = (TreeNode) path.getLastPathComponent();

		if ((this.synchronizedExpansion) && ((tree == this.pluginTreeA) || (tree == this.pluginTreeB))
				&& (((node instanceof GroupNode)) || ((node instanceof RecordNode))))
		{
			JTree cmpTree = tree == this.pluginTreeA ? this.pluginTreeB : this.pluginTreeA;
			JScrollPane scrollPane = tree == this.pluginTreeA ? this.scrollPaneB : this.scrollPaneA;
			int scrollValue = scrollPane.getHorizontalScrollBar().getValue();
			TreePath cmpPath = findMatchingPath(cmpTree, path);
			if ((cmpPath != null) && (!cmpTree.isCollapsed(cmpPath)))
			{
				cmpTree.collapsePath(cmpPath);
				cmpTree.scrollPathToVisible(cmpPath);
				scrollPane.getHorizontalScrollBar().setValue(scrollValue);
			}
		}
	}

	private void closeDialog()
	{
		this.expandingA = false;
		this.expandingB = false;

		if (this.pluginAModified)
		{
			int selection = JOptionPane.showConfirmDialog(this, "The first plugin has been modified. Do you want to save the changes?",
					"Plugin Modified", 0, 3);
			if (selection == 0)
			{
				SaveTask.savePlugin(this, this.pluginFileA, this.pluginA);
			}

		}

		if (this.pluginBModified)
		{
			int selection = JOptionPane.showConfirmDialog(this, "The second plugin has been modified. Do you want to save the changes?",
					"Plugin Modified", 0, 3);
			if (selection == 0)
			{
				SaveTask.savePlugin(this, this.pluginFileB, this.pluginB);
			}

		}

		if (this.clipboardModified)
		{
			int selection = JOptionPane.showConfirmDialog(this, "The clipboard has been modified. Do you want to save the changes?",
					"Clipboard Modified", 0, 3);
			if (selection == 0)
				SaveTask.savePlugin(this, this.clipboardFile, this.clipboard);
		}
	}

	private void collapseTopNodes(JTree tree)
	{
		PluginNode pluginNode = tree == this.pluginTreeA ? this.pluginNodeA : this.pluginNodeB;
		TreeNode[] pathNodes = new TreeNode[2];
		pathNodes[0] = pluginNode;
		int count = pluginNode.getChildCount();
		for (int i = 0; i < count; i++)
		{
			GroupNode groupNode = (GroupNode) pluginNode.getChildAt(i);
			pathNodes[1] = groupNode;
			tree.collapsePath(new TreePath(pathNodes));
		}
	}

	private void expandDistinctNodes(JTree tree, Integer pathIndex)
	{
		JProgressBar progressBar;
		PluginNode pluginNode;
		//JProgressBar progressBar;
		if (tree == this.pluginTreeA)
		{
			pluginNode = this.pluginNodeA;
			progressBar = this.progressBarA;
		}
		else
		{
			pluginNode = this.pluginNodeB;
			progressBar = this.progressBarB;
		}

		int index = pathIndex != null ? pathIndex.intValue() : 0;
		List<TreePath> pathList = pluginNode.getDistinctPaths();
		int count = pathList.size();

		if (index >= count)
		{
			progressBar.setValue(0);
			progressBar.setString("Idle");
			if (tree == this.pluginTreeA)
			{
				this.expandButtonA.setText("Expand Distinct Nodes");
				this.expandingA = false;
			}
			else
			{
				this.expandButtonB.setText("Expand Distinct Nodes");
				this.expandingB = false;
			}

			return;
		}

		tree.expandPath(pathList.get(index));
		index++;
		progressBar.setValue(index * 100 / count);

		String actionCommand = tree == this.pluginTreeA ? "continue expand A" : "continue expand B";
		ActionEvent actionEvent = new ActionEvent(new Integer(index), 1001, actionCommand);
		SwingUtilities.invokeLater(new DeferredActionEvent(this, actionEvent));
	}

	private TreePath findMatchingPath(JTree tree, TreePath path)
	{
		TreePath cmpPath = new TreePath(tree.getModel().getRoot());
		int count = path.getPathCount();
		boolean foundMatch = true;

		for (int i = 1; (i < count) && (foundMatch); i++)
		{
			TreeNode node = (TreeNode) path.getPathComponent(i);
			TreeNode cmpParentNode = (TreeNode) cmpPath.getPathComponent(i - 1);
			int cmpCount = cmpParentNode.getChildCount();
			if ((node instanceof GroupNode))
			{
				foundMatch = false;
				GroupNode groupNode = (GroupNode) node;
				PluginGroup group = groupNode.getGroup();
				int groupType = group.getGroupType();
				byte[] groupLabel = group.getGroupLabel();
				for (int j = 0; j < cmpCount; j++)
				{
					TreeNode cmpNode = cmpParentNode.getChildAt(j);
					if ((cmpNode instanceof GroupNode))
					{
						GroupNode cmpGroupNode = (GroupNode) cmpNode;
						PluginGroup cmpGroup = cmpGroupNode.getGroup();
						int cmpGroupType = cmpGroup.getGroupType();
						byte[] cmpGroupLabel = cmpGroup.getGroupLabel();
						if ((groupType != cmpGroupType) || (groupLabel[0] != cmpGroupLabel[0]) || (groupLabel[1] != cmpGroupLabel[1])
								|| (groupLabel[2] != cmpGroupLabel[2]) || (groupLabel[3] != cmpGroupLabel[3]))
							continue;
						foundMatch = true;
						cmpPath = cmpPath.pathByAddingChild(cmpGroupNode);
						break;
					}
				}
			}
			else if ((node instanceof RecordNode))
			{
				foundMatch = false;
				RecordNode recordNode = (RecordNode) node;
				PluginRecord record = recordNode.getRecord();
				for (int j = 0; j < cmpCount; j++)
				{
					TreeNode cmpNode = cmpParentNode.getChildAt(j);
					if ((cmpNode instanceof RecordNode))
					{
						RecordNode cmpRecordNode = (RecordNode) cmpNode;
						PluginRecord cmpRecord = cmpRecordNode.getRecord();
						if (record.equals(cmpRecord))
						{
							foundMatch = true;
							cmpPath = cmpPath.pathByAddingChild(cmpRecordNode);
							break;
						}
					}
				}
			}
		}

		return foundMatch ? cmpPath : null;
	}

	private class CompareCellRenderer extends DefaultTreeCellRenderer
	{
		public CompareCellRenderer()
		{
			setTextSelectionColor(Color.WHITE);
			setTextNonSelectionColor(Color.BLACK);
			setBackgroundSelectionColor(Color.BLUE);
			setBackgroundNonSelectionColor(Color.WHITE);
		}

		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean isSelected, boolean isExpanded, boolean isLeaf,
				int row, boolean hasFocus)
		{
			Component component = super.getTreeCellRendererComponent(tree, value, isSelected, isExpanded, isLeaf, row, hasFocus);

			if ((value instanceof GroupNode))
				setBackgroundNonSelectionColor(((GroupNode) value).isDistinct() ? Color.YELLOW : Color.WHITE);
			else if ((value instanceof RecordNode))
				setBackgroundNonSelectionColor(((RecordNode) value).isDistinct() ? Color.YELLOW : Color.WHITE);
			else
			{
				setBackgroundNonSelectionColor(Color.WHITE);
			}
			return component;
		}
	}

	private class DeferredActionEvent implements Runnable
	{
		private ActionListener listener;

		private ActionEvent event;

		public DeferredActionEvent(ActionListener listener, ActionEvent event)
		{
			this.listener = listener;
			this.event = event;
		}

		public void run()
		{
			this.listener.actionPerformed(this.event);
		}
	}

	private class DialogWindowListener extends WindowAdapter
	{
		public DialogWindowListener()
		{
		}

		public void windowClosing(WindowEvent we)
		{
			CompareDialog.this.closeDialog();
		}
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.CompareDialog
 * JD-Core Version:    0.6.0
 */