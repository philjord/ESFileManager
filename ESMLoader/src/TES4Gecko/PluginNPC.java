package TES4Gecko;

public class PluginNPC
{
	private int formID;

	private String editorID;

	private int raceID;

	private boolean female;

	private boolean deleted;

	public PluginNPC(int formID)
	{
		this(formID, new String(), 0, false);
	}

	public PluginNPC(int formID, String editorID, int raceID, boolean female)
	{
		this.formID = formID;
		this.editorID = editorID;
		this.raceID = raceID;
		this.female = female;
		this.deleted = false;
	}

	public int getFormID()
	{
		return this.formID;
	}

	public String getEditorID()
	{
		return this.editorID;
	}

	public int getRaceID()
	{
		return this.raceID;
	}

	public boolean isFemale()
	{
		return this.female;
	}

	public boolean isDeleted()
	{
		return this.deleted;
	}

	public void setDelete(boolean deleted)
	{
		this.deleted = deleted;
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.PluginNPC
 * JD-Core Version:    0.6.0
 */