package esfilemanager.tes3;

/**
 * For making up fake children groups for tes3
 * @author phil
 *
 */
public class PluginGroup extends esfilemanager.common.data.plugin.PluginGroup
{
	protected String editorID = "";

	public PluginGroup(int groupType)
	{
		this.recordType = "GRUP";
		this.groupType = groupType;
		groupParentID = -1;
		groupLabel = new byte[4];
	}
	
	public PluginGroup(int groupType, String groupLabel)
	{
		this.recordType = "GRUP";
		this.groupType = groupType;
		groupParentID = -1;
		groupRecordType = groupLabel;
		this.groupLabel = groupLabel.getBytes();
	}

	@Override
	public String getEditorID()
	{
		return editorID;
	}
}