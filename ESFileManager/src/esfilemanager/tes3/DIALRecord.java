package esfilemanager.tes3;

import java.io.IOException;
import java.util.ArrayList;

import esfilemanager.common.PluginException;
import tools.io.FileChannelRAF;

public class DIALRecord extends PluginRecord {
	private long					INFOOffset	= -1;

	private boolean					isLoaded	= false;

	private ArrayList<PluginRecord>	infos		= new ArrayList<PluginRecord>();

	/*
	  40: DIAL =   772 (    24,     33.54,     54)
		Dialogue topic (including journals)
		NAME = Dialogue ID string
		DATA = Dialogue Type? (1 byte, 4 bytes for deleted?)
			0 = Regular Topic
			1 = Voice?
			2 = Greeting?
			3 = Persuasion?
			4 = Journal
		What follows in the ESP/ESM are all the INFO records that belong to the
		DIAL record (one of the few cases where order is important).
	 */

	// on construction simply load this record only and record file pointer afterwards

	// later on a load call actually load up the INFO records

	public DIALRecord(int formId, byte[] thisPrefix, FileChannelRAF in) throws PluginException, IOException {
		super(formId, thisPrefix);
		super.load("", in, -1);
		INFOOffset = in.getFilePointer();
	}

	public boolean isLoaded() {
		return isLoaded;
	}

	public ArrayList<PluginRecord> getInfos() {
		return infos;
	}

	public void load(FileChannelRAF in) throws PluginException, IOException {
		infos = new ArrayList<PluginRecord>();
		synchronized (in) {
			in.seek(INFOOffset);
			while (in.getFilePointer() < in.length()) {
				// pull the prefix data so we know what sort of record we have
				byte[] prefix = new byte[16];
				int count = in.read(prefix);
				if (count != 16)
					throw new PluginException(": record prefix is incomplete");

				String recordType = new String(prefix, 0, 4);
				if (recordType.equals("INFO")) {
					PluginRecord record = new PluginRecord(-1, prefix);
					record.load("", in, -1);
					infos.add(record);
				} else {
					// we are finished here
					return;
				}
			}
		}
		isLoaded = true;
	}
}
