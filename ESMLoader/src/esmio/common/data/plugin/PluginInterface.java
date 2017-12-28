package esmio.common.data.plugin;

import java.util.List;
import java.util.Map;

public interface PluginInterface
{
	public String getName();

	public PluginHeader getPluginHeader();

	public List<FormInfo> getFormList();

	public Map<Integer, FormInfo> getFormMap();

	public List<PluginGroup> getGroupList();

}
