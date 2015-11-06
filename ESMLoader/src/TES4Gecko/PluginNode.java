package TES4Gecko;

import java.util.List;
import java.util.Map;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

public class PluginNode extends DefaultMutableTreeNode
{
	private List<TreePath> distinctPaths;

	private FormAdjust formAdjust;

	public PluginNode(Plugin plugin)
	{
		super(plugin);
	}

	public Plugin getPlugin()
	{
		return (Plugin) getUserObject();
	}

	public List<TreePath> getDistinctPaths()
	{
		return this.distinctPaths;
	}

	public void setDistinctPaths(List<TreePath> distinctPaths)
	{
		this.distinctPaths = distinctPaths;
	}

	public FormAdjust getFormAdjust()
	{
		return this.formAdjust;
	}

	public void setFormAdjust(FormAdjust formAdjust)
	{
		this.formAdjust = formAdjust;
	}

	public void insert(GroupNode groupNode)
	{
		int count = getChildCount();
		int index;
		for (index = 0; index < count; index++)
		{
			if (groupNode.compareTo((GroupNode) getChildAt(index)) < 0)
				break;
		}
		insert(groupNode, index);
	}

	public void buildNodes(WorkerTask task) throws InterruptedException
	{
		StatusDialog statusDialog = task != null ? task.getStatusDialog() : null;
		List<PluginGroup> groupList = getPlugin().getGroupList();
		int groupCount = groupList.size();
		int processedCount = 0;
		int currentProgress = 0;
		if (statusDialog != null)
		{
			statusDialog.updateMessage("Creating tree for " + getPlugin().getName());
		}

		removeAllChildren();

		for (PluginGroup group : groupList)
		{
			GroupNode groupNode = new GroupNode(group);
			createGroupChildren(groupNode, group);
			insert(groupNode);
			if ((task != null) && (WorkerTask.interrupted()))
			{
				throw new InterruptedException("Request canceled");
			}
			processedCount++;
			int newProgress = processedCount * 100 / groupCount;
			if (newProgress >= currentProgress + 5)
			{
				currentProgress = newProgress;
				if (statusDialog != null)
					statusDialog.updateProgress(currentProgress);
			}
		}
	}

	private void createGroupChildren(GroupNode groupNode, PluginGroup group)
	{
		Map<Integer, FormInfo> formMap = getPlugin().getFormMap();

		List<PluginRecord> recordList = group.getRecordList();
		for (PluginRecord record : recordList)
			if ((record instanceof PluginGroup))
			{
				PluginGroup subgroup = (PluginGroup) record;
				GroupNode subgroupNode = new GroupNode(subgroup);
				createGroupChildren(subgroupNode, subgroup);
				groupNode.add(subgroupNode);
			}
			else
			{
				RecordNode recordNode = new RecordNode(record);

				if (record.getRecordLength() != 0)
				{
					recordNode.add(new DefaultMutableTreeNode(null));
				}

				FormInfo formInfo = formMap.get(new Integer(record.getFormID()));
				if (formInfo != null)
				{
					formInfo.setRecordNode(recordNode);
				}

				int groupType = group.getGroupType();
				if (groupType == 0)
				{
					String recordType = group.getGroupRecordType();
					if ((recordType.equals("CELL")) || (recordType.equals("DIAL")) || (recordType.equals("WRLD")))
						groupNode.add(recordNode);
					else
						groupNode.insert(recordNode);
				}
				else if ((groupType == 10) || (groupType == 8) || (groupType == 9))
				{
					groupNode.insert(recordNode);
				}
				else
				{
					groupNode.add(recordNode);
				}
			}
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.PluginNode
 * JD-Core Version:    0.6.0
 */