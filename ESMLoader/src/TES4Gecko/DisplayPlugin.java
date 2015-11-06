package TES4Gecko;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

public abstract class DisplayPlugin extends JDialog
{
	protected Plugin clipboard;

	protected File clipboardFile;

	protected JTree clipboardTree;

	protected DefaultTreeModel clipboardTreeModel;

	protected boolean clipboardModified = false;

	protected boolean clipboardCleared = true;

	protected boolean copyReferences = false;

	public DisplayPlugin(JFrame parent, String title)
	{
		super(parent, title, true);
		setDefaultCloseOperation(2);
	}

	protected void createRecordChildren(RecordNode recordNode) throws DataFormatException, IOException, PluginException
	{
		List<PluginSubrecord> subrecordList = recordNode.getRecord().getSubrecords();
		for (PluginSubrecord subrecord : subrecordList)
		{
			subrecord.setSpillMode(true);
			DefaultMutableTreeNode subrecordNode = new DefaultMutableTreeNode(subrecord);
			recordNode.add(subrecordNode);
		}
	}

	protected boolean toggleIgnore(JTree tree)
	{
		boolean pluginModified = false;
		TreePath[] treePaths = tree.getSelectionPaths();
		if (treePaths == null)
			JOptionPane.showMessageDialog(this, "You must select at least one record.", "Error", 0);
		else
		{
			for (TreePath treePath : treePaths)
			{
				TreeNode node = (TreeNode) treePath.getLastPathComponent();
				if ((node instanceof PluginNode))
					JOptionPane.showMessageDialog(this, "The entire plugin can not be selected.  The selection will be ignored.",
							"Warning", 2);
				else if ((node instanceof GroupNode))
				{
					if (toggleGroupIgnore(tree, treePath, (GroupNode) node))
						pluginModified = true;
				}
				else if ((node instanceof RecordNode))
				{
					if (toggleRecordIgnore(tree, treePath, (RecordNode) node))
					{
						DefaultTreeModel treeModel = (DefaultTreeModel) tree.getModel();
						treeModel.nodeChanged(node);
						pluginModified = true;
					}
				}
				else
					JOptionPane.showMessageDialog(this, "A subrecord can not be ignored.  The selection will be ignored.", "Warning", 2);
			}

		}

		return pluginModified;
	}

	private boolean toggleGroupIgnore(JTree tree, TreePath groupPath, GroupNode groupNode)
	{
		boolean pluginModified = false;

		int childCount = groupNode.getChildCount();
		List<Integer> childList = new ArrayList<Integer>(childCount);
		for (int i = 0; i < childCount; i++)
		{
			TreeNode node = groupNode.getChildAt(i);
			TreePath treePath = groupPath.pathByAddingChild(node);
			if ((node instanceof GroupNode))
			{
				if (toggleGroupIgnore(tree, treePath, (GroupNode) node))
					pluginModified = true;
			}
			else
			{
				if ((!(node instanceof RecordNode)) || (!toggleRecordIgnore(tree, treePath, (RecordNode) node)))
					continue;
				childList.add(new Integer(i));
				pluginModified = true;
			}

		}

		childCount = childList.size();
		if (childCount > 0)
		{
			int[] childIndices = new int[childCount];
			for (int i = 0; i < childCount; i++)
			{
				childIndices[i] = childList.get(i).intValue();
			}
			DefaultTreeModel treeModel = (DefaultTreeModel) tree.getModel();
			treeModel.nodesChanged(groupNode, childIndices);
		}

		return pluginModified;
	}

	protected void updateRecordNode(JTree tree, RecordNode recordNode)
	{
		int childCount = recordNode.getChildCount();
		if (childCount > 0)
		{
			int[] childIndices = new int[childCount];
			for (int i = 0; i < childCount; i++)
			{
				childIndices[i] = i;
			}

			DefaultTreeModel treeModel = (DefaultTreeModel) tree.getModel();
			treeModel.nodesChanged(recordNode, childIndices);
		}
	}

	protected boolean toggleRecordIgnore(JTree tree, TreePath treePath, RecordNode recordNode)
	{
		Plugin plugin = ((PluginNode) treePath.getPathComponent(0)).getPlugin();
		PluginRecord record = recordNode.getRecord();
		record.setIgnore(!record.isIgnored());
		if (!record.isIgnored())
		{
			Map<Integer, FormInfo> formMap = plugin.getFormMap();
			Integer objFormID = new Integer(record.getFormID());
			if (formMap.get(objFormID) == null)
			{
				GroupNode parentNode = (GroupNode) recordNode.getParent();
				PluginGroup parentGroup = parentNode.getGroup();
				FormInfo formInfo = new FormInfo(record, record.getRecordType(), record.getFormID(), record.getEditorID());
				formInfo.setRecordNode(recordNode);
				formInfo.setParentFormID(parentGroup.getGroupParentID());
				plugin.getFormList().add(formInfo);
				formMap.put(objFormID, formInfo);
			}
		}

		return true;
	}

	protected void copyRecords(JTree pluginTree) throws DataFormatException, IOException, PluginException
	{
		PluginNode pluginNode = (PluginNode) pluginTree.getModel().getRoot();
		Plugin plugin = pluginNode.getPlugin();
		FormAdjust formAdjust = pluginNode.getFormAdjust();

		TreePath[] treePaths = pluginTree.getSelectionPaths();
		if (treePaths == null)
		{
			JOptionPane.showMessageDialog(this, "You must select at least one record to copy.", "Error", 0);
			return;
		}

		for (TreePath treePath : treePaths)
		{
			DefaultMutableTreeNode pathNode = (DefaultMutableTreeNode) treePath.getLastPathComponent();
			System.out.println("pathNode " + pathNode);
			if ((pathNode instanceof PluginNode))
			{
				JOptionPane.showMessageDialog(this, "The entire plugin can not be selected.  The selection will be ignored.", "Warning", 2);
			}
			else if ((pathNode instanceof GroupNode))
			{
				PluginGroup group = ((GroupNode) pathNode).getGroup();
				if (group.getGroupType() == 0)
				{
					PluginNode rootNode = (PluginNode) this.clipboardTreeModel.getRoot();
					int childCount = rootNode.getChildCount();
					String groupRecordType = group.getGroupRecordType();
					for (int i = 0; i < childCount; i++)
					{
						GroupNode parentNode = (GroupNode) rootNode.getChildAt(i);
						if (parentNode.getGroup().getGroupRecordType().equals(groupRecordType))
						{
							List<PluginRecord> recordList = group.getRecordList();
							for (PluginRecord record : recordList)
							{
								if ((record instanceof PluginGroup))
									copyGroup(plugin, formAdjust, (PluginGroup) record, parentNode);
								else
								{
									copyRecord(plugin, formAdjust, record, parentNode);
								}
							}
							break;
						}
					}
				}
				else
				{
					GroupNode parentNode = createHierarchy(plugin, formAdjust, group);
					copyGroup(plugin, formAdjust, group, parentNode);
				}
			}
			else if ((pathNode instanceof RecordNode))
			{
				PluginRecord record = ((RecordNode) pathNode).getRecord();
				GroupNode parentNode = createHierarchy(plugin, formAdjust, record);
				copyRecord(plugin, formAdjust, record, parentNode);
			}
			else
			{
				JOptionPane.showMessageDialog(this, "An individual subrecord can not be copied.  The selection will be ignored.",
						"Warning", 2);
			}
		}
	}

	private GroupNode createHierarchy(Plugin plugin, FormAdjust formAdjust, PluginRecord record) throws DataFormatException, IOException,
			PluginException
	{
		GroupNode parentNode = null;

		PluginGroup parentGroup = this.clipboard.createHierarchy(record, formAdjust);

		List<PluginGroup> groupList = new ArrayList<PluginGroup>(10);
		PluginGroup topGroup = parentGroup;
		while (topGroup.getGroupType() != 0)
		{
			groupList.add(topGroup);
			topGroup = (PluginGroup) topGroup.getParent();
		}

		DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) this.clipboardTreeModel.getRoot();
		int childCount = rootNode.getChildCount();
		for (int i = 0; i < childCount; i++)
		{
			parentNode = (GroupNode) rootNode.getChildAt(i);
			if (parentNode.getGroup() == topGroup)
			{
				break;
			}

		}

		for (int i = groupList.size() - 1; i >= 0; i--)
		{
			PluginGroup group = groupList.get(i);
			GroupNode groupNode = null;

			childCount = parentNode.getChildCount();
			boolean foundGroup = false;
			for (int j = 0; j < childCount; j++)
			{
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) parentNode.getChildAt(j);
				if (node.getUserObject() == group)
				{
					groupNode = (GroupNode) node;
					foundGroup = true;
					break;
				}

			}

			if (!foundGroup)
			{
				int groupType = group.getGroupType();
				if ((groupType == 1) || (groupType == 6) || (groupType == 7))
				{
					int formID = group.getGroupParentID();
					parentGroup = parentNode.getGroup();
					List<PluginRecord> recordList = parentGroup.getRecordList();
					int index = recordList.indexOf(group);
					if (index < 1)
					{
						throw new PluginException("Type " + groupType + " subgroup not preceded by WRLD/CELL/DIAL record");
					}
					PluginRecord prevRecord = recordList.get(index - 1);
					String recordType = prevRecord.getRecordType();
					if ((!recordType.equals("WRLD")) && (!recordType.equals("CELL")) && (!recordType.equals("DIAL")))
					{
						throw new PluginException("Type " + groupType + " subgroup not preceded by WRLD/CELL/DIAL record");
					}
					if (prevRecord.getFormID() != formID)
					{
						throw new PluginException("WRLD/CELL/DIAL record form ID mismatch");
					}
					RecordNode recordNode = new RecordNode(prevRecord);
					createRecordChildren(recordNode);
					parentNode.add(recordNode);

					groupNode = new GroupNode(group);
					parentNode.add(groupNode);

					int[] childIndices = new int[2];
					childIndices[0] = childCount;
					childIndices[1] = (childCount + 1);
					this.clipboardTreeModel.nodesWereInserted(parentNode, childIndices);
				}
				else
				{
					groupNode = new GroupNode(group);
					int[] childIndices = new int[1];
					if ((groupType == 10) || (groupType == 8))
					{
						childIndices[0] = 0;
						parentNode.insert(groupNode, 0);
					}
					else
					{
						parentNode.add(groupNode);
						childIndices[0] = childCount;
					}

					this.clipboardTreeModel.nodesWereInserted(parentNode, childIndices);
				}

				this.clipboardModified = true;
			}

			parentNode = groupNode;
		}

		if ((record instanceof PluginGroup))
		{
			PluginGroup group = (PluginGroup) record;
			int groupType = group.getGroupType();
			if ((groupType == 1) || (groupType == 6) || (groupType == 7))
			{
				int formID = formAdjust.adjustFormID(group.getGroupParentID());
				childCount = parentNode.getChildCount();
				boolean foundRecord = false;
				for (int i = 0; i < childCount; i++)
				{
					DefaultMutableTreeNode node = (DefaultMutableTreeNode) parentNode.getChildAt(i);
					if ((!(node instanceof RecordNode)) || (((RecordNode) node).getRecord().getFormID() != formID))
						continue;
					foundRecord = true;
					break;
				}

				if (!foundRecord)
				{
					List<PluginRecord> recordList = parentNode.getGroup().getRecordList();
					int recordCount = recordList.size();
					for (int i = 0; i < recordCount; i++)
					{
						PluginRecord checkRecord = recordList.get(i);
						if (((checkRecord instanceof PluginGroup)) || (checkRecord.getFormID() != formID))
							continue;
						if (i > recordCount - 2)
						{
							throw new PluginException("WRLD/CELL/DIAL record not followed by subgroup");
						}
						RecordNode recordNode = new RecordNode(checkRecord);
						if (checkRecord.getRecordLength() != 0)
						{
							recordNode.add(new DefaultMutableTreeNode(null));
						}
						parentNode.add(recordNode);
						GroupNode groupNode = new GroupNode((PluginGroup) recordList.get(i + 1));
						parentNode.add(groupNode);
						int[] childIndices = new int[2];
						childIndices[0] = childCount;
						childIndices[1] = (childCount + 1);
						this.clipboardTreeModel.nodesWereInserted(parentNode, childIndices);
						this.clipboardModified = true;
						break;
					}
				}

			}

		}

		return parentNode;
	}

	private void copyGroup(Plugin plugin, FormAdjust formAdjust, PluginGroup group, GroupNode parentNode) throws DataFormatException,
			IOException, PluginException
	{
		System.out.println("copy group " + group);
		PluginGroup parentGroup = parentNode.getGroup();
		int groupType = group.getGroupType();
		byte[] groupLabel = group.getGroupLabel();
		switch (groupType)
		{
			case 1:
			case 6:
			case 7:
			case 8:
			case 9:
			case 10:
				int oldFormID = SerializedElement.getInteger(groupLabel, 0);
				int newFormID = formAdjust.adjustFormID(oldFormID);
				SerializedElement.setInteger(newFormID, groupLabel, 0);
			case 2:
			case 3:
			case 4:
			case 5:
		}
		int childCount = parentNode.getChildCount();
		GroupNode clipboardNode = null;
		boolean foundGroup = false;
		for (int index = 0; index < childCount; index++)
		{
			TreeNode node = parentNode.getChildAt(index);
			PluginGroup checkGroup;
			 
			if ((node instanceof GroupNode))
			{
				clipboardNode = (GroupNode) node;
				checkGroup = clipboardNode.getGroup();
				if (checkGroup.getGroupType() == groupType)
				{
					byte[] checkLabel = checkGroup.getGroupLabel();
					if ((groupLabel[0] != checkLabel[0]) || (groupLabel[1] != checkLabel[1]) || (groupLabel[2] != checkLabel[2])
							|| (groupLabel[3] != checkLabel[3]))
						continue;
					foundGroup = true;
					break;
				}
			}

		}

		if (!foundGroup)
		{
			PluginGroup clipboardGroup = new PluginGroup(groupType, groupLabel);
			clipboardGroup.setParent(parentGroup);
			if ((groupType == 10) || (groupType == 8))
				parentGroup.getRecordList().add(0, clipboardGroup);
			else
			{
				parentGroup.getRecordList().add(clipboardGroup);
			}
			clipboardNode = new GroupNode(clipboardGroup);
			int[] childIndices = new int[1];
			if ((groupType == 10) || (groupType == 8))
			{
				childIndices[0] = 0;
				parentNode.insert(clipboardNode, 0);
			}
			else
			{
				childIndices[0] = childCount;
				parentNode.add(clipboardNode);
			}

			this.clipboardTreeModel.nodesWereInserted(parentNode, childIndices);
			this.clipboardModified = true;
		}

		List<PluginRecord> recordList = group.getRecordList();
		for (PluginRecord record : recordList)
			if ((record instanceof PluginGroup))
				copyGroup(plugin, formAdjust, (PluginGroup) record, clipboardNode);
			else
				copyRecord(plugin, formAdjust, record, clipboardNode);
	}

	private void copyRecord(Plugin plugin, FormAdjust formAdjust, PluginRecord record, GroupNode parentNode) throws DataFormatException,
			IOException, PluginException
	{
		PluginGroup parentGroup = parentNode.getGroup();
		String recordType = record.getRecordType();

		int formID = formAdjust.adjustFormID(record.getFormID());
		Integer mapID = new Integer(formID);
		Map<?, ?> formMap = this.clipboard.getFormMap();
		if (formMap.get(mapID) != null)
		{
			return;
		}

		this.clipboard.copyRecord(record, formAdjust);
		PluginRecord clipboardRecord = (PluginRecord) ((FormInfo) formMap.get(mapID)).getSource();
		this.clipboardModified = true;

		List<PluginRecord> recordList = parentGroup.getRecordList();
		int recordCount = recordList.size();
		int childCount = parentNode.getChildCount();
		for (int i = 0; i < recordCount; i++)
		{
			PluginRecord checkRecord = recordList.get(i);
			if (checkRecord == clipboardRecord)
			{
				if ((recordType.equals("WRLD")) || (recordType.equals("CELL")) || (recordType.equals("DIAL")))
				{
					if (i > recordCount - 2)
					{
						throw new PluginException("WRLD/CELL/DIAL record not followed by subgroup");
					}
					checkRecord = recordList.get(i + 1);
					if (!(checkRecord instanceof PluginGroup))
					{
						throw new PluginException("WRLD/CELL/DIAL record not followed by subgroup");
					}
					PluginGroup clipboardGroup = (PluginGroup) checkRecord;
					if (clipboardGroup.getGroupParentID() != formID)
					{
						throw new PluginException("WRLD/CELL/DIAL record not followed by subgroup");
					}
					RecordNode recordNode = new RecordNode(clipboardRecord);
					createRecordChildren(recordNode);
					parentNode.add(recordNode);

					GroupNode groupNode = new GroupNode(clipboardGroup);
					parentNode.add(groupNode);

					int[] childIndices = new int[2];
					childIndices[0] = childCount;
					childIndices[1] = (childCount + 1);
					this.clipboardTreeModel.nodesWereInserted(parentNode, childIndices);
				}
				else
				{
					RecordNode recordNode = new RecordNode(clipboardRecord);
					createRecordChildren(recordNode);
					int index;
					for (index = 0; index < childCount; index++)
					{
						TreeNode node = parentNode.getChildAt(index);
						if (((node instanceof RecordNode)) && (recordNode.compareTo((RecordNode) node) < 0))
						{
							break;
						}
					}
					parentNode.insert(recordNode, index);

					int[] childIndices = new int[1];
					childIndices[0] = index;
					this.clipboardTreeModel.nodesWereInserted(parentNode, childIndices);
				}

			}

		}

		if (this.copyReferences)
			copyRecordReferences(plugin, formAdjust, record);
	}

	private void copyRecordReferences(Plugin plugin, FormAdjust formAdjust, PluginRecord record) throws DataFormatException, IOException,
			PluginException
	{
		Map<?, ?> formMap = plugin.getFormMap();
		int masterCount = plugin.getMasterList().size();
		Map<?, ?> clipboardMap = this.clipboard.getFormMap();

		List<PluginSubrecord> subrecordList = record.getSubrecords();
		for (PluginSubrecord subrecord : subrecordList)
		{
			int[][] references = subrecord.getReferences();
			if (references == null)
			{
				continue;
			}

			for (int i = 0; i < references.length; i++)
			{
				int formID = references[i][1];
				if (formID == 0)
				{
					continue;
				}
				int masterID = formID >>> 24;
				if (masterID < masterCount)
				{
					continue;
				}
				Integer objFormID = new Integer(formID);
				if (clipboardMap.get(objFormID) != null)
				{
					continue;
				}
				FormInfo formInfo = (FormInfo) formMap.get(objFormID);
				if (formInfo == null)
				{
					continue;
				}
				PluginRecord refRecord = (PluginRecord) formInfo.getSource();
				if (refRecord == null)
				{
					continue;
				}

				GroupNode parentNode = createHierarchy(plugin, formAdjust, refRecord);
				copyRecord(plugin, formAdjust, refRecord, parentNode);
			}
		}
	}

	protected void validateTree(JTree tree)
	{
		DefaultTreeModel treeModel = (DefaultTreeModel) tree.getModel();
		PluginNode root = (PluginNode) treeModel.getRoot();
		Enumeration<?> nodes = root.children();
		while (nodes.hasMoreElements())
		{
			GroupNode groupNode = (GroupNode) nodes.nextElement();
			boolean subtreeChanged = validateTreeGroup(treeModel, groupNode);
			if (subtreeChanged)
				treeModel.nodeStructureChanged(groupNode);
		}
	}

	private boolean validateTreeGroup(DefaultTreeModel treeModel, GroupNode parentNode)
	{
		boolean nodeStructureChanged = false;
		PluginGroup parentGroup = parentNode.getGroup();
		int index = 0;
		while (index < parentNode.getChildCount())
		{
			boolean removeNode = false;
			TreeNode node = parentNode.getChildAt(index);
			if ((node instanceof GroupNode))
			{
				GroupNode groupNode = (GroupNode) node;
				PluginGroup group = groupNode.getGroup();
				boolean subtreeChanged = validateTreeGroup(treeModel, groupNode);
				if (subtreeChanged)
				{
					nodeStructureChanged = true;
				}
				if (group.isEmpty())
				{
					List<PluginRecord> recordList = parentGroup.getRecordList();
					removeNode = true;
					for (PluginRecord groupRecord : recordList)
						if (groupRecord == group)
						{
							removeNode = false;
							break;
						}
				}
			}
			else if ((node instanceof RecordNode))
			{
				RecordNode recordNode = (RecordNode) node;
				PluginRecord record = recordNode.getRecord();
				if (record.isIgnored())
				{
					removeNode = true;
				}
				else
				{
					boolean subtreeChanged = validateTreeRecord(treeModel, recordNode);
					if (subtreeChanged)
					{
						nodeStructureChanged = true;
					}
				}
			}
			if (removeNode)
			{
				parentNode.remove(index);
				nodeStructureChanged = true;
			}
			else
			{
				index++;
			}
		}

		return nodeStructureChanged;
	}

	private boolean validateTreeRecord(DefaultTreeModel treeModel, RecordNode parentRecNode)
	{
		boolean nodeStructureChanged = false;
		PluginRecord parentRec = (PluginRecord) parentRecNode.getUserObject();
		int index = 0;
		List<PluginSubrecord> subrecs = new ArrayList<PluginSubrecord>();
		try
		{
			subrecs = parentRec.getSubrecords();
		}
		catch (Exception ex)
		{
			return false;
		}
		do
		{
			boolean removeNode = false;
			DefaultMutableTreeNode subNode = (DefaultMutableTreeNode) parentRecNode.getChildAt(index);
			PluginSubrecord subrec = (PluginSubrecord) subNode.getUserObject();
			if ((subrec == null) && (Main.debugMode))
			{
				String errStr = "Subrec is null at index " + index + " with parent " + parentRec.getEditorID() + "("
						+ parentRec.getFormID() + ")" + " with " + subrecs.size() + " subrecords.\n";
				System.out.printf(errStr, new Object[0]);
			}
			boolean removeSub = true;
			for (PluginSubrecord sub : subrecs)
			{
				if ((subrec == null) || (!subrec.equals(sub)))
					continue;
				removeSub = false;
				break;
			}

			if (removeSub)
			{
				parentRecNode.remove(index);
				nodeStructureChanged = true;
			}
			else
			{
				index++;
			}
		}
		while (index < parentRecNode.getChildCount());

		return nodeStructureChanged;
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.DisplayPlugin
 * JD-Core Version:    0.6.0
 */