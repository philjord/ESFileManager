package esmLoader.common.data.plugin;

public class FormInfo
{
	private String recordType;

	private int formID;

	private int parentFormID;

	private String editorID;

	private PluginRecord pluginRecord;

	private boolean pointerOnly = false;

	private long pointer;

	public FormInfo(String recordType, int formID, String editorID, PluginRecord pluginRecord)
	{
		this.recordType = recordType;
		this.formID = formID;
		this.editorID = editorID;
		this.pluginRecord = pluginRecord;
	}

	public FormInfo(String recordType, int formID, String editorID, long pointer)
	{
		this.recordType = recordType;
		this.formID = formID;
		this.editorID = editorID;
		this.pointer = pointer;
		this.pointerOnly = true;
	}

	public PluginRecord getPluginRecord()
	{
		return pluginRecord;
	}

	public long getPointer()
	{
		return pointer;
	}

	public String getRecordType()
	{
		return recordType;
	}

	public int getFormID()
	{
		return formID;
	}

	public void setParentFormID(int formID)
	{
		parentFormID = formID;
	}

	public String getEditorID()
	{
		return editorID;
	}

	public boolean isPointerOnly()
	{
		return pointerOnly;
	}

	public int getParentFormID()
	{
		return parentFormID;
	}

}
