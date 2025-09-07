package esfilemanager.tes3;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import esfilemanager.common.PluginException;
import esfilemanager.common.data.record.Subrecord;
import tools.io.ESMByteConvert;
import tools.io.FileChannelRAF;

public class PluginHeader extends PluginRecord {

	private String	pluginFileName;

	private float	pluginVersion	= -1;

	private boolean	master			= false;

	private String	creator			= "";

	private String	summary			= "";

	private int		numRecords		= 0;

	public PluginHeader() {
		super(-1, "TES3", "The header");
		subrecordList = null;// force load below
	}
	
	// return bytes read
	public int load(String fileName, FileChannelRAF in, long pos) throws PluginException, IOException {
		
		FileChannel ch = in.getChannel();
		
		// pull the prefix data so we know what sort of record we need to load
		byte[] prefix = new byte[16];
		int count = ch.read(ByteBuffer.wrap(prefix), pos);	
		pos += prefix.length;
		if (count != 16)
			throw new PluginException(": record prefix is incomplete");

		// memory saving mechanism  https://www.baeldung.com/java-string-pool
		recordType = new String(prefix, 0, 4).intern();
		recordSize = ESMByteConvert.extractInt(prefix, 4);
		unknownInt = ESMByteConvert.extractInt(prefix, 8);
		recordFlags1 = ESMByteConvert.extractInt(prefix, 12);

		super.load(in, pos);
		getSubrecords();// force load of subs
		pluginFileName = fileName;
		for (Subrecord sub : getSubrecords()) {
			if (sub.getSubrecordType().equals("HEDR")) {
				// 4 bytes, float Version (1.2)
				pluginVersion = ESMByteConvert.extractFloat(sub.getSubrecordData(), 0);
				// 4 bytes, long Unknown (1)
				// 32 Bytes, Company Name string
				creator = new String(sub.getSubrecordData(), 8, 32);
				// 256 Bytes, ESM file description?
				summary = new String(sub.getSubrecordData(), 40, 256);
				// 4 bytes, long NumRecords (48227)
				numRecords = ESMByteConvert.extractInt(sub.getSubrecordData(), 296);
			} else if (sub.getSubrecordType().equals("MAST")) {
				/*
				 * MAST = string, variable length Only found in ESP plugins and
				 * specifies a master file that the plugin requires. Can occur
				 * multiple times. Usually found just after the TES3 record.
				 */
			} else if (sub.getSubrecordType().equals("DATA")) {
				/*
				 * DATA = 8 Bytes long64 MasterSize Size of the previous master
				 * file in bytes (used for version tracking of plugin). The MAST
				 * and DATA records are always found together, the DATA
				 * following the MAST record that it refers to.
				 */
			} else {
				System.out.println("Unknown subrecord in TES3 Record " + recordType + " " + sub.getSubrecordType());
			}
		}
		
		return recordSize;
	}

	public String getName() {
		return pluginFileName;
	}

	public float getVersion() {
		return pluginVersion;
	}

	public boolean isMaster() {
		return master;
	}

	public String getCreator() {
		return creator;
	}

	public String getSummary() {
		return summary;
	}

	public int getNumRecords() {
		return numRecords;
	}
}
