package esfilemanager.common.data.record;

import java.util.List;

/**
 * https://en.m.uesp.net/wiki/Skyrim_Mod:Mod_File_Format#Groups
 */
public class Record {
	protected String			recordType;

	protected int				formID;
	protected int				masterID;

	protected int				recordFlags;

	// this is described as time stampe and version id in the https://en.uesp.net/wiki/Oblivion_Mod:Mod_File_Format
	// and https://en.m.uesp.net/wiki/Skyrim_Mod:Mod_File_Format#Groups 
	protected int				timeStamp = 0;
	protected int				versionControl = 0;

	//https://en.m.uesp.net/wiki/Skyrim_Mod:Mod_File_Format#Groups has it as a 16 of version control, and a 16 of version format
	protected int				internalVersion	= 0;	// only in 24 byte skyrim+
	protected int				unknownShort	= 0;

	protected boolean			updated			= false;

	protected List<Subrecord>	subrecordList;

	public Record() {
	}

	public boolean isDeleted() {
		return (recordFlags & 0x20) != 0;
	}

	public boolean isIgnored() {
		return (recordFlags & 0x1000) != 0;
	}

	public boolean isCompressed() {
		return (recordFlags & 0x40000) != 0;
	}

	public String getRecordType() {
		return recordType;
	}

	public int getFormID() {
		return formID;
	}

	@Override
	public String toString() {
		return recordType	+ " record: " + formID + " " + (isIgnored() ? "(Ignore) " : "")
				+ (isDeleted() ? "(Deleted) " : "");

	}

	public void displaySubs() {
		List<Subrecord> subrecords = getSubrecords();
		for (Subrecord subrec : subrecords) {
			System.out.println("Subrecord " + subrec);
		}
	}

	public int getRecordFlags() {
		return recordFlags;
	}
	
	public int getMasterID() {
		return masterID;
	}
	
	public int getTimeStamp() {
		return timeStamp;
	}

	public int geVersionControl() {
		return versionControl;
	}

	public int getInternalVersion() {
		return internalVersion;
	}
	
	public int getUnknownShort() {
		return unknownShort;
	}
	

	public List<Subrecord> getSubrecords() {
		return subrecordList;
	}

	public void addSubrecord(Subrecord subrecord) {
		subrecordList.add(subrecord);
		updated = true;
	}

	public void setFormID(int i) {
		formID = i;
		updated = true;
	}

	public void updateFrom(Record record) {
		subrecordList.clear();
		subrecordList.addAll(record.getSubrecords());
		updated = true;
	}

	/**
	 * Currently this only overrides the first matching subrecord type and breaks, for multiple entries like CNTOs there
	 * needs to be a much better system
	 * @param subrecord
	 */
	public void updateSubrecord(Subrecord newSubrecord) {
		for (int i = 0; i < subrecordList.size(); i++) {
			Subrecord sub = subrecordList.get(i);
			if (sub.getSubrecordType().equals(newSubrecord.getSubrecordType())) {
				sub.setSubrecordData(newSubrecord.getSubrecordData());
			}
		}
		updated = true;
	}

	public boolean isUpdated() {
		return updated;
	}

	public void clearUpdated() {
		updated = false;
	}

	//ONLY for ESMConverter.ConverterFO3
	private int cellFormID = -1;

	public int getESMConverterCellFormID() {
		return cellFormID;
	}

	public void setESMConverterCellFormID(int i) {
		cellFormID = i;
	}

}
