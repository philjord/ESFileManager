package TES4Gecko;

public class FormInfo
{
	private Object plugin;

	private Object source;

	private RecordNode recordNode;

	private String recordType;

	private int formID;

	private int parentFormID;

	private String editorID;

	private String voiceName;

	private int mergedFormID;

	private String mergedEditorID;

	public FormInfo(Object source, String recordType, int formID, String editorID)
	{
		this.source = source;
		this.recordType = recordType;
		this.formID = formID;
		this.editorID = editorID;
		this.mergedFormID = formID;
		this.mergedEditorID = editorID;
	}

	public Object getPlugin()
	{
		return this.plugin;
	}

	public void setPlugin(Object plugin)
	{
		this.plugin = plugin;
	}

	public Object getSource()
	{
		return this.source;
	}

	public void setSource(Object source)
	{
		this.source = source;
	}

	public RecordNode getRecordNode()
	{
		return this.recordNode;
	}

	public void setRecordNode(RecordNode recordNode)
	{
		this.recordNode = recordNode;
	}

	public String getRecordType()
	{
		return this.recordType;
	}

	public int getFormID()
	{
		return this.formID;
	}

	public int getParentFormID()
	{
		return this.parentFormID;
	}

	public void setParentFormID(int formID)
	{
		this.parentFormID = formID;
	}

	public String getEditorID()
	{
		return this.editorID;
	}

	public String getVoiceName()
	{
		return this.voiceName;
	}

	public void setVoiceName(String fileName)
	{
		this.voiceName = fileName;
	}

	public int getMergedFormID()
	{
		return this.mergedFormID;
	}

	public void setFormID(int newFormID)
	{
		this.formID = newFormID;
	}

	public void setMergedFormID(int formID)
	{
		this.mergedFormID = formID;
	}

	public String getMergedEditorID()
	{
		return this.mergedEditorID;
	}

	public void setEditorID(String newEditorID)
	{
		this.editorID = newEditorID;
	}

	public void setMergedEditorID(String editorID)
	{
		this.mergedEditorID = editorID;
	}

	public boolean equals(Object object)
	{
		boolean areEqual = false;
		if ((object instanceof FormInfo))
		{
			FormInfo objInfo = (FormInfo) object;
			if ((objInfo.getPlugin() == this.plugin) && (objInfo.getSource() == this.source)
					&& (objInfo.getRecordType().equals(this.recordType)) && (objInfo.getFormID() == this.formID))
			{
				areEqual = true;
			}
		}
		return areEqual;
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.FormInfo
 * JD-Core Version:    0.6.0
 */