package esfilemanager.loader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.DataFormatException;

import com.frostwire.util.SparseArray;

import esfilemanager.common.PluginException;
import esfilemanager.common.data.plugin.FormInfo;
import esfilemanager.common.data.plugin.IMaster;
import esfilemanager.common.data.plugin.PluginGroup;
import esfilemanager.common.data.plugin.PluginRecord;
import esfilemanager.common.data.record.Record;


// TODO: this is really an ESMMaster manager (or master plus plugin? esp? for morrowind)

// also the multi-master part ( and cacher) is really very separate from the ensuremaster
// and get esm manager bit so perhaps time for 2?

public abstract class ESMManager implements IESMManager {

	private ArrayList<IMaster>	masters					= new ArrayList<IMaster>();

	private float				pluginVersion			= -999;

	private String				pluginName				= "";

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
			masters.add(master);
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

	public List<WRLDTopGroup> getWRLDTopGroups() {
		ArrayList<WRLDTopGroup> ret = new ArrayList<WRLDTopGroup>();
		for (IMaster m : masters) {
			ret.add(m.getWRLDTopGroup());
		}
		return ret;
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

	public List<InteriorCELLTopGroup> getInteriorCELLTopGroups() {
		ArrayList<InteriorCELLTopGroup> ret = new ArrayList<InteriorCELLTopGroup>();
		for (IMaster m : masters) {
			ret.add(m.getInteriorCELLTopGroup());
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
	
	
	
	private HashMap<Integer,Integer> doublesReported = new HashMap<Integer,Integer>();

	@Override
	public PluginRecord getInteriorCELL(int formID) throws DataFormatException, IOException, PluginException {
		PluginRecord pr = null;
		for (IMaster m : masters) {
			PluginRecord pr2 = m.getInteriorCELL(formID);
			if(pr2 != null) {
				if(pr != null && doublesReported.get(formID) == null) {
					System.out.println("getInteriorCELL(int formID) found more than once " + formID);
					doublesReported.put(formID,formID);
				}
				pr = pr2;
			}
		}
				
		return pr;
	}

	@Override
	public PluginGroup getInteriorCELLChildren(int formID) throws DataFormatException, IOException, PluginException {
		PluginGroup pg = null;
		for (IMaster m : masters) {
			PluginGroup pg2 = m.getInteriorCELLChildren(formID);
			if(pg2 != null) {
				if(pg != null && doublesReported.get(formID) == null) {
					System.out.println("getInteriorCELLChildren(int formID) found more than once " + formID);
					doublesReported.put(formID,formID);
				}
				pg = pg2;
			}
		}
		return pg;	
	}

	@Override
	public PluginGroup getInteriorCELLPersistentChildren(int formID)
			throws DataFormatException, IOException, PluginException {
		PluginGroup pg = null;
		for (IMaster m : masters) {
			PluginGroup pg2 = m.getInteriorCELLPersistentChildren(formID);
			if(pg2 != null) {
				if(pg != null && doublesReported.get(formID) == null) {
					System.out.println("getInteriorCELLPersistentChildren(int formID) found twice " + formID );	
					doublesReported.put(formID,formID);
				}
				pg = pg2;
			}
		}
		return pg;	
	}
	
	

	@Override
	public PluginRecord getWRLD(int formID) throws DataFormatException, IOException, PluginException {
		PluginRecord pr = null;
		for (IMaster m : masters) {
			PluginRecord pr2 = m.getWRLD(formID);
			if(pr2 != null) {
				if(pr != null && doublesReported.get(formID) == null) {
					System.out.println("getWRLD(int formID) found twice " + formID);		
					doublesReported.put(formID,formID);
				}
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
				if(pr != null && doublesReported.get(formID) == null) {
					System.out.println("getWRLDChildren(int formID) found twice " + formID );	
					doublesReported.put(formID,formID);
				}
				pr = pr2;
			}
		}
		return pr;
	}

	@Override
	public PluginRecord getWRLDExtBlockCELL(int wrldFormId, int x, int y)
			throws DataFormatException, IOException, PluginException {
		PluginRecord pr = null;
		for (IMaster m : masters) {
			PluginRecord pr2 = m.getWRLDExtBlockCELL(wrldFormId, x, y);
			if(pr2 != null) {
				if(pr != null ) 
					System.out.println("getWRLDExtBlockCELL(int formID) found twice " + wrldFormId + " " + x + "x" + y);	
					 
				pr = pr2;
			}
		}
		return pr;
	}

	@Override
	public PluginGroup getWRLDExtBlockCELLChildren(int wrldFormId, int x, int y)
			throws DataFormatException, IOException, PluginException {
		PluginGroup pg = null;
		for (IMaster m : masters) {
			PluginGroup pg2 = m.getWRLDExtBlockCELLChildren(wrldFormId, x, y);
			if(pg2 != null) {
				if(pg != null ) 
					System.out.println("getWRLDExtBlockCELLChildren(int formID) found twice " + wrldFormId + " " + x + "x" + y);	
				 
				pg = pg2;
			}
		}
		return pg;
	}
	
	@Override
	public PluginRecord getPluginRecord(int formID) throws PluginException {
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
}
