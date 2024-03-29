package esfilemanager.common.data.plugin;

import java.io.IOException;
import java.util.List;
import java.util.zip.DataFormatException;

import com.frostwire.util.SparseArray;

import esfilemanager.common.PluginException;
import esfilemanager.loader.FormToFilePointer;
import esfilemanager.loader.InteriorCELLTopGroup;
import esfilemanager.loader.WRLDChildren;
import esfilemanager.loader.WRLDTopGroup;

public interface IMaster {
	public WRLDTopGroup getWRLDTopGroup();

	public InteriorCELLTopGroup getInteriorCELLTopGroup();

	public PluginRecord getWRLD(int formID) throws DataFormatException, IOException, PluginException;

	public WRLDChildren getWRLDChildren(int formID);

	public PluginRecord getWRLDExtBlockCELL(int wrldFormId, int x, int y)
			throws DataFormatException, IOException, PluginException;

	public PluginGroup getWRLDExtBlockCELLChildren(int wrldFormId, int x, int y)
			throws DataFormatException, IOException, PluginException;

	public PluginRecord getInteriorCELL(int formID) throws DataFormatException, IOException, PluginException;

	public PluginGroup getInteriorCELLChildren(int formID) throws DataFormatException, IOException, PluginException;

	public PluginGroup getInteriorCELLPersistentChildren(int formID)
			throws DataFormatException, IOException, PluginException;

	public PluginRecord getPluginRecord(int formID) throws PluginException;

	public SparseArray<FormInfo> getFormMap();

	public int[] getAllFormIds();

	public List<FormToFilePointer> getAllInteriorCELLFormIds();

	public int[] getAllWRLDTopGroupFormIds();

	public String getName();

	public float getVersion();

	public int getMinFormId();

	public int getMaxFormId();

}
