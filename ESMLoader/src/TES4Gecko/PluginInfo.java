package TES4Gecko;

public class PluginInfo
{
	private String pluginName;

	private float pluginVersion;

	private String pluginCreator;

	private String pluginSummary;

	private boolean deleteLastConflict = false;

	private boolean editConflicts = false;

	public PluginInfo(String name, String creator, String summary)
	{
		this(name, creator, summary, 0.8F);
	}

	public PluginInfo(String name, String creator, String summary, float version)
	{
		this.pluginName = name;
		this.pluginCreator = (creator != null ? creator : new String());
		this.pluginSummary = (summary != null ? summary : new String());
		this.pluginVersion = version;
	}

	public String getName()
	{
		return this.pluginName;
	}

	public float getVersion()
	{
		return this.pluginVersion;
	}

	public void setVersion(float version)
	{
		this.pluginVersion = version;
	}

	public String getCreator()
	{
		return this.pluginCreator;
	}

	public void setCreator(String creator)
	{
		this.pluginCreator = creator;
	}

	public String getSummary()
	{
		return this.pluginSummary;
	}

	public void setSummary(String summary)
	{
		this.pluginSummary = summary;
	}

	public boolean shouldDeleteLastConflict()
	{
		return this.deleteLastConflict;
	}

	public void setDeleteLastConflict(boolean deleteLastConflict)
	{
		this.deleteLastConflict = deleteLastConflict;
	}

	public boolean shouldEditConflicts()
	{
		return this.editConflicts;
	}

	public void setEditConflicts(boolean editConflicts)
	{
		this.editConflicts = editConflicts;
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.PluginInfo
 * JD-Core Version:    0.6.0
 */