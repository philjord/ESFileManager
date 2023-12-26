package esmio.common.data.plugin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import esmio.common.PluginException;
import tools.io.ESMByteConvert;
import tools.io.FileChannelRAF;

public class PluginHeader  {

	private String			pluginFileName;
	
	private String			pluginFileFormat; // ONLY TES3 and TES4 have ever been seen (and can be handled!)
	//https://en.uesp.net/wiki/Oblivion_Mod:Mod_File_Format/Vs_Morrowind


	private float			pluginVersion	= -1;

	public static enum FILE_TYPE{PLUGIN, MASTER, SAVE, UNKNOWN};//plugin == 0 master==1 save==32
	private FILE_TYPE		master			= FILE_TYPE.UNKNOWN;

	private String			creator			= "";

	private String			summary			= "";

	private int				recordCount		= 0;

	private List<String>	masterList		= new ArrayList<String>();

	private int				tesRecordLen	= -1;

	public PluginHeader(String pluginFileName) {
		this.pluginFileName = pluginFileName;
	}

	public String getName() {
		return pluginFileName;
	}

	public float getVersion() {
		return pluginVersion;
	}

	public boolean isMaster() {
		return master == FILE_TYPE.MASTER;
	}

	public String getCreator() {
		return creator;
	}

	public String getSummary() {
		return summary;
	}

	public int getRecordCount() {
		return recordCount;
	}

	public List<String> getMasterList() {
		return masterList;
	}

	public int getHeaderByteCount() {
		return tesRecordLen;
	}
	

	public String getPluginFileFormat() {
		return pluginFileFormat;
	}
	
	//https://en.uesp.net/wiki/Skyrim_Mod:Mod_File_Format/TES4
	//http://www.uesp.net/wiki/Tes4Mod:Mod_File_Format
	public void load(String fileName, FileChannelRAF in) throws PluginException, IOException  {
		long fp = in.getFilePointer();
		// check to how much data the TES? REcord have in it's header possibly 16, 20 or 24 , 
		// nothing indicates this other than reading the file, this size is used by TES? and GRUP
		byte[] tmp = new byte[12];
		in.seek(fp + 16);
		in.read(tmp);
		
		if (new String(tmp,0,4).equals("HEDR")) {
			tesRecordLen = 16; //Morrowind
		} else  if (new String(tmp,4,4).equals("HEDR")) {
			tesRecordLen = 20; // oblivion
		} else  if (new String(tmp,8,4).equals("HEDR")) {
			tesRecordLen = 24; //fallout3, skyrim
		} else {
			throw new PluginException(pluginFileName + ": HEDR not found");
		}
		in.seek(fp);
		

		byte tesRecordData[] = new byte[tesRecordLen];
		int count = in.read(tesRecordData, 0, tesRecordLen);
		
		int headerLength = ESMByteConvert.extractInt(tesRecordData, 4);
		
		if (count != tesRecordLen)
			throw new PluginException(pluginFileName + ": header read failed");
		pluginFileFormat = new String(tesRecordData, 0, 4);	

		if (!pluginFileFormat.equals("TES4") && !pluginFileFormat.equals("TES3"))
			throw new PluginException(pluginFileName + ": File is not a TES3/TES4 file (" + pluginFileFormat + ")");

		if (pluginFileFormat.equals("TES3")) {
			readTes3(in, headerLength);
			return;
		}	
		
		
		//FIXME: is this accurate? it would seem no
		int fileType = ESMByteConvert.extractInt(tesRecordData, 12);// (0=esp file; 1=esm file; 32=ess file)	
		master = fileType == 0 ? FILE_TYPE.PLUGIN : fileType == 1 ? FILE_TYPE.MASTER : fileType == 32 ? FILE_TYPE.SAVE : FILE_TYPE.UNKNOWN;	
		System.out.println("fileType " +fileType);
		
		
		byte buffer[] = new byte[1024];
		do {
			if (headerLength < 6)
				break;

			// read a sub record as 4 char type and short data length(len not incl. 6 bytes)
			byte[] subrecordHeader = new byte[6];
			count = in.read(subrecordHeader);

			if (count != 6)
				throw new PluginException(pluginFileName + ": Header subrecord prefix truncated");
			headerLength -= 6;

			int dataLen = ESMByteConvert.extractShort(subrecordHeader, 4);

			if (dataLen > headerLength)
				throw new PluginException(pluginFileName + ": Subrecord length exceeds header length");
			if (dataLen > buffer.length)
				buffer = new byte[dataLen];

			count = in.read(buffer, 0, dataLen);
			if (count != dataLen)
				throw new PluginException(pluginFileName + ": Header subrecord data truncated");
			headerLength -= count;

			String type = new String(subrecordHeader, 0, 4);

			if (type.equals("HEDR")) {
				if (dataLen < 8)
					throw new PluginException(pluginFileName + ": HEDR subrecord is too small");
				pluginVersion = Float.intBitsToFloat(ESMByteConvert.extractInt(buffer, 0));
				recordCount = ESMByteConvert.extractInt(buffer, 4);
				 
				

			} else if (type.equals("CNAM")) {
				if (dataLen > 1)
					creator = new String(buffer, 0, dataLen - 1);
			} else if (type.equals("SNAM")) {
				if (dataLen > 1)
					summary = new String(buffer, 0, dataLen - 1);
			} else if (type.equals("OFST")) {
				// what is this one?				 
			} else if (type.equals("DELE")) {
				// what is this one?
			} else if (type.equals("MAST") && dataLen > 1) {
				masterList.add(new String(buffer, 0, dataLen - 1));
				//System.out.println("MAST " + new String(buffer, 0, length - 1));
			}
		} while (true);

		if (headerLength != 0)
			throw new PluginException(pluginFileName + ": Header is incomplete");
		else
			return;
	}

	// need a new format for records and sub records as the intro data is slightly different

	//http://www.uesp.net/morrow/tech/mw_esm.txt

	//https://www.mwmythicmods.com/argent/tech/es_format.html	
	private void readTes3(FileChannelRAF in, int headerLength) throws PluginException, IOException {
		byte buffer[] = new byte[1024];
		do {
			 if (headerLength < 6)
				break;
			
			byte[] recordHeader = new byte[8];
			int count = in.read(recordHeader);

			if (count != 8)
				throw new PluginException(pluginFileName + ": Header subrecord prefix truncated");
			headerLength -= count;
			
			String type = new String(recordHeader, 0, 4);
			int length = ESMByteConvert.extractShort(recordHeader, 4);

			if (length > headerLength)
				throw new PluginException(pluginFileName + ": Subrecord length exceeds header length");
			if (length > buffer.length)
				buffer = new byte[length];

			count = in.read(buffer, 0, length);
			if (count != length)
				throw new PluginException(pluginFileName + ": Header subrecord data truncated");
			headerLength -= count;



			/*
 		HEDR (300 bytes)
			4 bytes, float Version (1.2)
			4 bytes, file type (0=esp file; 1=esm file; 32=ess file)
			32 Bytes, Company Name string
			256 Bytes, ESM file description?
			4 bytes, long NumRecords (48227)
	  	MAST = 	string, variable length
	    		Only found in ESP plugins and specifies a master file that the
			plugin requires. Can occur multiple times.  Usually found
	    		just after the TES3 record.
	  	DATA =  8 Bytes  long64 MasterSize
	    		Size of the previous master file in bytes (used for version
			tracking  of plugin).  The MAST and DATA records are always
			found together, the DATA following the MAST record that
			it refers to.

			 */
			if (type.equals("HEDR")) {
				if (length < 8)
					throw new PluginException(pluginFileName + ": HEDR subrecord is too small");

				pluginVersion = Float.intBitsToFloat(ESMByteConvert.extractInt(buffer, 0));
				int fileType = ESMByteConvert.extractInt(buffer, 4); // (0=esp file; 1=esm file; 32=ess file)		
				master = fileType == 0 ? FILE_TYPE.PLUGIN : fileType == 1 ? FILE_TYPE.MASTER : fileType == 32 ? FILE_TYPE.SAVE : FILE_TYPE.UNKNOWN;
				creator = new String(buffer, 8, 32);
				summary = new String(buffer, 40, 256);				
				
				recordCount = ESMByteConvert.extractInt(buffer, 296);
			}  else if (type.equals("MAST") && length > 1) {
				masterList.add(new String(buffer, 0, length - 1));
				//System.out.println("MAST " + new String(buffer, 0, length - 1));
			} else if (type.equals("DATA")) {
				// must follow MAST   
				int masterLength = ESMByteConvert.extractInt64(buffer, 0);
			}
		} while (true);

		if (headerLength != 0)
			throw new PluginException(pluginFileName + ": Header is incomplete");
		else
			return;

	}
}
