package TES4Gecko;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

public class CompareTask extends WorkerTask
{
	private PluginNode pluginNodeA;

	private Map<Integer, RecordNode> recordMapA;

	private PluginNode pluginNodeB;

	private Map<Integer, RecordNode> recordMapB;

	public CompareTask(StatusDialog statusDialog, PluginNode pluginNodeA, PluginNode pluginNodeB)
	{
		super(statusDialog);
		this.pluginNodeA = pluginNodeA;
		this.pluginNodeB = pluginNodeB;
	}

	public static boolean comparePlugins(JFrame parent, PluginNode pluginNodeA, PluginNode pluginNodeB)
	{
		boolean completed = false;

		String text = "Comparing '" + pluginNodeA.getPlugin().getName() + "' and '" + pluginNodeB.getPlugin().getName() + "'";
		StatusDialog statusDialog = new StatusDialog(parent, text, "Compare Plugins");

		CompareTask worker = new CompareTask(statusDialog, pluginNodeA, pluginNodeB);
		statusDialog.setWorker(worker);

		worker.start();
		statusDialog.showDialog();

		if (statusDialog.getStatus() == 1)
			completed = true;
		else
		{
			JOptionPane.showMessageDialog(parent, "Unable to compare plugins", "Compare Plugins", 1);
		}
		return completed;
	}

	public void run()
	{
		boolean completed = false;
		int currentProgress = 0;
		int processedCount = 0;
		try
		{
			this.recordMapA = buildRecordMap(this.pluginNodeA);
			int totalCount = this.pluginNodeA.getChildCount();

			this.recordMapB = buildRecordMap(this.pluginNodeB);
			totalCount += this.pluginNodeB.getChildCount();

			this.pluginNodeA.setDistinctPaths(new ArrayList<TreePath>(100));
			int groupCount = this.pluginNodeA.getChildCount();
			for (int i = 0; i < groupCount; i++)
			{
				GroupNode groupNode = (GroupNode) this.pluginNodeA.getChildAt(i);
				compareGroupChildren(this.pluginNodeA, groupNode, this.recordMapB);

				if (interrupted())
				{
					throw new InterruptedException("Request canceled");
				}
				processedCount++;
				int newProgress = processedCount * 100 / totalCount;
				if (newProgress > currentProgress + 5)
				{
					currentProgress = newProgress;
					getStatusDialog().updateProgress(currentProgress);
				}

			}

			this.pluginNodeB.setDistinctPaths(new ArrayList<TreePath>(100));
			groupCount = this.pluginNodeB.getChildCount();
			for (int i = 0; i < groupCount; i++)
			{
				GroupNode groupNode = (GroupNode) this.pluginNodeB.getChildAt(i);
				compareGroupChildren(this.pluginNodeB, groupNode, this.recordMapA);

				if (interrupted())
				{
					throw new InterruptedException("Request canceled");
				}
				processedCount++;
				int newProgress = processedCount * 100 / totalCount;
				if (newProgress > currentProgress + 5)
				{
					currentProgress = newProgress;
					getStatusDialog().updateProgress(currentProgress);
				}

			}

			completed = true;
		}
		catch (InterruptedException exc)
		{
			WorkerDialog.showMessageDialog(getStatusDialog(), "Request canceled", "Interrupted", 0);
		}
		catch (Throwable exc)
		{
			Main.logException("Exception while comparing plugins", exc);
		}

		getStatusDialog().closeDialog(completed);
	}

	private Map<Integer, RecordNode> buildRecordMap(PluginNode pluginNode)
	{
		Plugin plugin = pluginNode.getPlugin();
		Map<Integer, RecordNode> recordMap = new HashMap<Integer, RecordNode>(plugin.getRecordCount());
		int count = pluginNode.getChildCount();

		for (int i = 0; i < count; i++)
		{
			TreeNode node = pluginNode.getChildAt(i);
			if ((node instanceof GroupNode))
				mapGroupRecords((GroupNode) node, recordMap);
			else
			{
				throw new UnsupportedOperationException("Top-level node is not a group");
			}
		}
		return recordMap;
	}

	private void mapGroupRecords(GroupNode groupNode, Map<Integer, RecordNode> recordMap)
	{
		int count = groupNode.getChildCount();

		for (int i = 0; i < count; i++)
		{
			TreeNode node = groupNode.getChildAt(i);
			if ((node instanceof GroupNode))
			{
				mapGroupRecords((GroupNode) node, recordMap);
			}
			else if ((node instanceof RecordNode))
			{
				RecordNode recordNode = (RecordNode) node;
				PluginRecord record = recordNode.getRecord();
				if (!record.isIgnored())
					recordMap.put(new Integer(record.getFormID()), recordNode);
			}
			else
			{
				throw new UnsupportedOperationException("Child node is not a group or record node");
			}
		}
	}

	private void compareGroupChildren(PluginNode pluginNode, GroupNode groupNode, Map<Integer, RecordNode> recordMap)
	{
		boolean expandGroup = false;
		List<TreePath> distinctPaths = pluginNode.getDistinctPaths();
		int distinctCount = distinctPaths.size();
		int childCount = groupNode.getChildCount();

		for (int i = 0; i < childCount; i++)
		{
			TreeNode node = groupNode.getChildAt(i);
			if ((node instanceof GroupNode))
			{
				compareGroupChildren(pluginNode, (GroupNode) node, recordMap);
			}
			else if ((node instanceof RecordNode))
			{
				RecordNode recordNode = (RecordNode) node;
				PluginRecord record = recordNode.getRecord();
				if (record.isIgnored())
				{
					recordNode.setDistinct(true);
				}
				else
				{
					RecordNode cmpNode = recordMap.get(new Integer(record.getFormID()));
					if ((cmpNode == null) || (!recordNode.equals(cmpNode)))
					{
						recordNode.setDistinct(true);
					}
					else
					{
						TreeNode[] path = recordNode.getPath();
						TreeNode[] cmpPath = cmpNode.getPath();
						if (path.length != cmpPath.length)
							recordNode.setDistinct(true);
						else
						{
							for (int j = 0; j < path.length; j++)
							{
								if (!(path[j] instanceof GroupNode))
								{
									break;
								}
								PluginGroup group = ((GroupNode) path[j]).getGroup();
								byte[] groupLabel = group.getGroupLabel();
								PluginGroup cmpGroup = ((GroupNode) cmpPath[j]).getGroup();
								byte[] cmpGroupLabel = cmpGroup.getGroupLabel();
								if ((group.getGroupType() == cmpGroup.getGroupType()) && (groupLabel[0] == cmpGroupLabel[0])
										&& (groupLabel[1] == cmpGroupLabel[1]) && (groupLabel[2] == cmpGroupLabel[2])
										&& (groupLabel[3] == cmpGroupLabel[3]))
									continue;
								recordNode.setDistinct(true);
								break;
							}

						}

					}

				}

				if (recordNode.isDistinct())
				{
					expandGroup = true;
					TreeNode parentNode = recordNode;
					while ((parentNode = parentNode.getParent()) != null)
					{
						if (!(parentNode instanceof GroupNode))
						{
							break;
						}
						GroupNode parentGroup = (GroupNode) parentNode;
						if (parentGroup.isDistinct())
						{
							break;
						}
						parentGroup.setDistinct(true);
					}

				}

			}

		}

		if ((expandGroup) && (distinctPaths.size() == distinctCount))
		{
			TreeNode[] pathNodes = groupNode.getPath();
			TreePath treePath = new TreePath(pathNodes);
			distinctPaths.add(treePath);
		}
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.CompareTask
 * JD-Core Version:    0.6.0
 */