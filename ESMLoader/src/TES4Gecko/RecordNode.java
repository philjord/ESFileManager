package TES4Gecko;

import javax.swing.tree.DefaultMutableTreeNode;

public class RecordNode extends DefaultMutableTreeNode implements Comparable<RecordNode>
{
	private boolean distinct = false;

	public RecordNode(PluginRecord record)
	{
		super(record);
	}

	public PluginRecord getRecord()
	{
		return (PluginRecord) getUserObject();
	}

	public boolean isDistinct()
	{
		return this.distinct;
	}

	public void setDistinct(boolean distinct)
	{
		this.distinct = distinct;
	}

	public int hashCode()
	{
		return getRecord().hashCode();
	}

	public boolean equals(Object obj)
	{
		boolean areEqual = false;
		if ((obj instanceof RecordNode))
		{
			areEqual = getRecord().isIdentical(((RecordNode) obj).getRecord());
		}
		return areEqual;
	}

	public int compareTo(RecordNode node)
	{
		PluginRecord record = getRecord();
		PluginRecord cmpRecord = node.getRecord();
		int diff = record.getRecordType().compareTo(cmpRecord.getRecordType());
		if (diff == 0)
		{
			diff = record.getEditorID().compareTo(cmpRecord.getEditorID());
			if (diff == 0)
			{
				int formID = record.getFormID();
				int cmpFormID = cmpRecord.getFormID();
				if (formID < cmpFormID)
					diff = -1;
				else if (formID > cmpFormID)
				{
					diff = 1;
				}
			}
		}
		return diff;
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.RecordNode
 * JD-Core Version:    0.6.0
 */