package esmLoader.common.data.plugin;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.DataFormatException;

import esmLoader.common.PluginException;
import esmLoader.loader.WRLDChildren;

public interface IMaster
{
	public PluginRecord getWRLD(int formID) throws DataFormatException, IOException, PluginException;

	public WRLDChildren getWRLDChildren(int formID);

	public int getWRLDExtBlockCELLId(int wrldFormId, int x, int y);

	public PluginRecord getWRLDExtBlockCELL(int formID) throws DataFormatException, IOException, PluginException;

	public PluginGroup getWRLDExtBlockCELLChildren(int formID) throws DataFormatException, IOException, PluginException;

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

	public Set<Integer> getWRLDExtBlockCELLFormIds();

	public String getName();

	public float getVersion();

}
