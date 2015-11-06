package TES4Gecko;

public class PluginRace
{
	private int formID;

	private String editorID;

	private String fullName;

	private boolean deleted;

	private boolean playableRace;

	private int maleVoiceID;

	private int femaleVoiceID;

	public PluginRace(int formID)
	{
		this(formID, new String(), new String(), false, formID, formID);
	}

	public PluginRace(int formID, String editorID, String fullName, boolean playableRace, int maleVoiceID, int femaleVoiceID)
	{
		this.formID = formID;
		this.editorID = editorID;
		this.fullName = fullName;
		this.playableRace = playableRace;
		this.maleVoiceID = maleVoiceID;
		this.femaleVoiceID = femaleVoiceID;
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

	public String getName()
	{
		return this.fullName;
	}

	public boolean isDeleted()
	{
		return this.deleted;
	}

	public void setDelete(boolean deleted)
	{
		this.deleted = deleted;
	}

	public boolean isPlayableRace()
	{
		return this.playableRace;
	}

	public int getMaleVoiceID()
	{
		return this.maleVoiceID;
	}

	public int getFemaleVoiceID()
	{
		return this.femaleVoiceID;
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.PluginRace
 * JD-Core Version:    0.6.0
 */