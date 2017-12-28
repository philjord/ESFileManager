package esmio.tes3;

/**
 * For making up fake children groups for tes3
 * @author phil
 *
 */
public class PluginGroup extends esmio.common.data.plugin.PluginGroup
{
	protected String editorID = "";

	public PluginGroup(int groupType)
	{
		this.recordType = "GRUP";
		this.groupType = groupType;
		groupParentID = -1;
		groupLabel = new byte[4];
	}

	public String getEditorID()
	{
		return editorID;
	}
}