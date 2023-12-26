package esmio.tes3;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;

import com.frostwire.util.SparseArray;

import esmio.common.PluginException;
import esmio.common.data.plugin.FormInfo;
import esmio.common.data.plugin.IMaster;
import esmio.common.data.plugin.PluginGroup;
import esmio.common.data.record.Record;
import esmio.loader.FormToFilePointer;
import esmio.loader.InteriorCELLTopGroup;
import esmio.loader.WRLDChildren;
import esmio.loader.WRLDTopGroup;

public abstract class ESMManagerTes3 implements IESMManagerTes3 {
	private ArrayList<IMasterTes3>	masters			= new ArrayList<IMasterTes3>();
	private float					pluginVersion	= -999;
	private String					pluginName		= "";

	@Override
	public void addMaster(IMaster master) {
		if (pluginVersion == -999) {
			pluginVersion = master.getVersion();
			pluginName = master.getName();
		} else {
			if (master.getVersion() != pluginVersion) {
				System.out.println("Mismatched master version, ESMManager has this "	+ pluginVersion
									+ " added master has " + master.getVersion());
			}
		}

		boolean hasMaster = masters.contains(master);
		for(IMaster m : masters) { 
			if(m.getName().equals(master.getName())) {				
				hasMaster = true;
				break;
			}
				
		}
			
		if (!hasMaster) {		
			masters.add((IMasterTes3)master);
		} else {
			System.out.println("why add same master twice? " + master);
		}
		
		
	}

	@Override
	public float getVersion() {
		return pluginVersion;
	}

	@Override
	public String getName() {
		return pluginName;
	}

	/**
	 * No more cache!!! duplicates happily handed out
	 */
	@Override
	public Record getRecord(int formID) {
		try {
			return getPluginRecord(formID);
		} catch (PluginException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Record getRecord(String edidId) {
		int formId = convertNameRefToId(edidId);
		if (formId != -1) {
			return getRecord(formId);
		} else {
			System.out.println("-1 form id for " + edidId);
			return null;
		}
	}

	/**
	 * Note very slow, but not used often
	 */
	@Override
	public int[] getAllFormIds() {
		SparseArray<FormInfo> idToFormMap = new SparseArray<FormInfo>();
		for (IMaster m : masters) {
			idToFormMap.putAll(m.getFormMap());
		}
		return idToFormMap.keySet();
	}

	/**
	 * Note very slow, but not used often
	 */
	@Override
	public SparseArray<FormInfo> getFormMap() {
		SparseArray<FormInfo> idToFormMap = new SparseArray<FormInfo>();
		for (IMaster m : masters) {
			idToFormMap.putAll(m.getFormMap());
		}
		return idToFormMap;
	}

	@Override
	public int[] getAllWRLDTopGroupFormIds() {
		int totalSize = 0;
		for (IMaster m : masters)
			totalSize += m.getAllWRLDTopGroupFormIds().length;

		int[] ret = new int[totalSize];

		int offset = 0;
		for (IMaster m : masters) {
			System.arraycopy(m.getAllWRLDTopGroupFormIds(), 0, ret, offset, m.getAllWRLDTopGroupFormIds().length);
			offset += m.getAllWRLDTopGroupFormIds().length;
		}
		return ret;
	}

	@Override
	public List<FormToFilePointer> getAllInteriorCELLFormIds() {
		ArrayList<FormToFilePointer> ret = new ArrayList<FormToFilePointer>();
		for (IMaster m : masters) {
			ret.addAll(m.getAllInteriorCELLFormIds());
		}
		return ret;
	}

	
	
	
	@Override
	public esmio.common.data.plugin.PluginRecord getInteriorCELL(int formID)
			throws DataFormatException, IOException, PluginException {
		esmio.common.data.plugin.PluginRecord pr = null;
		for (IMaster m : masters) {
			esmio.common.data.plugin.PluginRecord pr2 = m.getInteriorCELL(formID);
			if(pr2 != null) {
				if(pr != null)
					System.out.println("getInteriorCELL(int formID) found twice " + formID);				
				pr = pr2;
			}
		}
				
		return pr;
	}

	@Override
	public PluginGroup getInteriorCELLChildren(int formID) throws DataFormatException, IOException, PluginException {
		esmio.common.data.plugin.PluginGroup pg = null;
		for (IMaster m : masters) {
			esmio.common.data.plugin.PluginGroup pg2 = m.getInteriorCELLChildren(formID);
			if(pg2 != null) {
				if(pg != null)
					System.out.println("getInteriorCELLChildren(int formID) found twice " + formID );				
				pg = pg2;
			}
		}
		return pg;	
	}

	@Override
	public PluginGroup getInteriorCELLPersistentChildren(int formID)
			throws DataFormatException, IOException, PluginException {
		esmio.common.data.plugin.PluginGroup pg = null;
		for (IMaster m : masters) {
			esmio.common.data.plugin.PluginGroup pg2 = m.getInteriorCELLPersistentChildren(formID);
			if(pg2 != null) {
				if(pg != null)
					System.out.println("getInteriorCELLPersistentChildren(int formID) found twice " + formID );				
				pg = pg2;
			}
		}
		return pg;	
	}

	

	@Override
	public esmio.common.data.plugin.PluginRecord getWRLD(int formID)
			throws DataFormatException, IOException, PluginException {
		esmio.common.data.plugin.PluginRecord pr = null;
		for (IMaster m : masters) {
			esmio.common.data.plugin.PluginRecord pr2 = m.getWRLD(formID);
			if(pr2 != null) {
				if(pr != null)
					System.out.println("getWRLD(int formID) found twice " + formID);				
				pr = pr2;
			}
		}
		return pr;
	}

	@Override
	public WRLDChildren getWRLDChildren(int formID) {
		WRLDChildren pr = null;
		for (IMaster m : masters) {
			WRLDChildren pr2 = m.getWRLDChildren(formID);
			if(pr2 != null) {
				if(pr != null)
					System.out.println("getWRLDChildren(int formID) found twice " + formID );				
				pr = pr2;
			}
		}
		return pr;
	}

	@Override
	public esmio.common.data.plugin.PluginRecord getWRLDExtBlockCELL(int wrldFormId, int x, int y)
			throws DataFormatException, IOException, PluginException {		
		esmio.common.data.plugin.PluginRecord pr = null;
		for (IMaster m : masters) {
			esmio.common.data.plugin.PluginRecord pr2 = m.getWRLDExtBlockCELL(wrldFormId, x, y);
			if(pr2 != null) {
				if(pr != null)
					System.out.println("getWRLDExtBlockCELL(int formID) found twice " + wrldFormId + " " + x + "x" + y);				
				pr = pr2;
			}
		}
		return pr;
	}

	@Override
	public esmio.common.data.plugin.PluginGroup getWRLDExtBlockCELLChildren(int wrldFormId, int x, int y)
			throws DataFormatException, IOException, PluginException {
		esmio.common.data.plugin.PluginGroup pg = null;
		for (IMaster m : masters) {
			esmio.common.data.plugin.PluginGroup pg2 = m.getWRLDExtBlockCELLChildren(wrldFormId, x, y);
			if(pg2 != null) {
				if(pg != null)
					System.out.println("getWRLDExtBlockCELLChildren(int formID) found twice " + wrldFormId + " " + x + "x" + y);				
				pg = pg2;
			}
		}
		return pg;	
	}
	
	
	@Override
	public esmio.common.data.plugin.PluginRecord getPluginRecord(int formID) throws PluginException {
		for (IMaster m : masters) {
			if (formID >= m.getMinFormId() && formID <= m.getMaxFormId()) {
				return m.getPluginRecord(formID);
			}
		}

		System.out.println("no master found for form id " + formID);
		return null;
	}
	
	@Override
	public WRLDTopGroup getWRLDTopGroup() {
		throw new UnsupportedOperationException("ESMManager does not have a single top group");
	}

	@Override
	public InteriorCELLTopGroup getInteriorCELLTopGroup() {
		throw new UnsupportedOperationException("ESMManager does not have a single top group");
	}

	@Override
	public int getMinFormId() {
		throw new UnsupportedOperationException("ESMManager does not have a min form id");
	}

	@Override
	public int getMaxFormId() {
		throw new UnsupportedOperationException("ESMManager does not have a max form id");
	}
	
	

	
	@Override
	public ArrayList<IMaster> getMasters() {
		return new ArrayList<IMaster>(masters);
	}
	
	@Override
	public void clearMasters() {
		masters.clear();
		pluginVersion = -1;
	}

	@Override
	public int convertNameRefToId(String str) {
		for (IMasterTes3 m : masters) {
			int id = m.convertNameRefToId(str);
			if (id != -1)
				return id;
		}

		return -1;
	}

}
