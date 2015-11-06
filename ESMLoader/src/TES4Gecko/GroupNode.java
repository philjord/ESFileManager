package TES4Gecko;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

public class GroupNode extends DefaultMutableTreeNode implements Comparable<GroupNode>
{
	private boolean distinct = false;

	public GroupNode(PluginGroup group)
	{
		super(group);
	}

	public PluginGroup getGroup()
	{
		return (PluginGroup) getUserObject();
	}

	public boolean isDistinct()
	{
		return this.distinct;
	}

	public void setDistinct(boolean distinct)
	{
		this.distinct = distinct;
	}

	public void insert(GroupNode groupNode)
	{
		int count = getChildCount();
		int index;
		for (index = 0; index < count; index++)
		{
			TreeNode node = getChildAt(index);
			if (!(node instanceof GroupNode))
			{
				break;
			}
			if (groupNode.compareTo((GroupNode) node) < 0)
			{
				break;
			}
		}
		insert(groupNode, index);
	}

	public void insert(RecordNode recordNode)
	{
		int count = getChildCount();
		int index;
		for (index = 0; index < count; index++)
		{
			TreeNode node = getChildAt(index);
			if (((node instanceof RecordNode)) && (recordNode.compareTo((RecordNode) node) < 0))
			{
				break;
			}
		}
		insert(recordNode, index);
	}

	public int hashCode()
	{
		return getGroup().hashCode();
	}

	public boolean equals(Object obj)
	{
		boolean areEqual = false;
		if ((obj instanceof GroupNode))
		{
			areEqual = getGroup().isIdentical(((GroupNode) obj).getGroup());
		}
		return areEqual;
	}

	public int compareTo(GroupNode node)
	{
		return getGroup().toString().compareTo(node.getGroup().toString());
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.GroupNode
 * JD-Core Version:    0.6.0
 */