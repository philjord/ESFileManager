package esmmanager.common.data.plugin;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.DataFormatException;

import esmmanager.common.PluginException;
import esmmanager.loader.InteriorCELLTopGroup;
import esmmanager.loader.WRLDChildren;
import esmmanager.loader.WRLDTopGroup;

public interface IMaster
{
	public WRLDTopGroup getWRLDTopGroup();

	public InteriorCELLTopGroup getInteriorCELLTopGroup();

	public PluginRecord getWRLD(int formID) throws DataFormatException, IOException, PluginException;

	public WRLDChildren getWRLDChildren(int formID);

	public PluginRecord getWRLDExtBlockCELL(int wrldFormId, int x, int y) throws DataFormatException, IOException, PluginException;

	public PluginGroup getWRLDExtBlockCELLChildren(int wrldFormId, int x, int y) throws DataFormatException, IOException, PluginException;

	public PluginRecord getInteriorCELL(int formID) throws DataFormatException, IOException, PluginException;

	public PluginGroup getInteriorCELLChildren(int formID) throws DataFormatException, IOException, PluginException;

	public PluginRecord getPluginRecord(int formID) throws PluginException;

	public Map<Integer, FormInfo> getFormMap();

	public Map<String, Integer> getEdidToFormIdMap();

	public Map<String, List<Integer>> getTypeToFormIdMap();

	public Set<Integer> getAllFormIds();

	public Set<String> getAllEdids();

	public Set<Integer> getAllInteriorCELLFormIds();

	public Set<Integer> getAllWRLDTopGroupFormIds();

	public String getName();

	public float getVersion();

	public int getMinFormId();

	public int getMaxFormId();

}
