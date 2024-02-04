package esfilemanager.common.data.plugin;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import esfilemanager.common.PluginException;
import esfilemanager.common.data.record.Subrecord;
import tools.io.ESMByteConvert;

public class PluginHeader extends PluginRecord {

	private String			pluginFileName;
	
	private float			pluginVersion	= -1;
	
	public static enum GAME{TES3, TES4, FO3, TESV, FO4, UNKNOWN};
	private GAME			game			= GAME.UNKNOWN;

	public static enum FILE_FORMAT{TES3, TES4, UNKNOWN};
	private FILE_FORMAT		fileFormat		= FILE_FORMAT.UNKNOWN;
	
	public static enum FILE_PRIORITY{PLUGIN, MASTER, SAVE, UNKNOWN};
	private FILE_PRIORITY		filePriority		= FILE_PRIORITY.UNKNOWN;

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
		return filePriority == FILE_PRIORITY.MASTER;
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
	
	public String getPluginFileName() {
		return pluginFileName;
	}

	public float getPluginVersion() {
		return pluginVersion;
	}

	public GAME getGame() {
		return game;
	}

	public FILE_PRIORITY getFileFormat() {
		return filePriority;
	}
	 
	//https://en.uesp.net/wiki/Skyrim_Mod:Mod_File_Format/TES4
	//http://www.uesp.net/wiki/Tes4Mod:Mod_File_Format
	// return bytes read
	public int load(String fileName, FileChannel ch, long pos) throws PluginException, IOException  {
		byte[] tmp = new byte[28];
		ByteBuffer bb = ByteBuffer.wrap(tmp);
		int count = ch.read(bb, pos);		
		//NOTE don't move pos forward as that was just a peek

				
		String startChars = new String(tmp);			
		
		// check this before TES4 due to overlap
		if (startChars.startsWith("TES4SAVEGAME")) {
			//https://en.uesp.net/wiki/Oblivion_Mod:Save_File_Format
			fileFormat = FILE_FORMAT.TES4;
			game		= GAME.TES4;
			tesRecordLen = 20;			
			filePriority = FILE_PRIORITY.SAVE;			
		} else if (startChars.startsWith("TES4")) {
			fileFormat = FILE_FORMAT.TES4;
			// master or plugin file
			if (new String(tmp,20,4).equals("HEDR")) {
				game = GAME.TES4;
				tesRecordLen = 20; // oblivion				
			} else  if (new String(tmp,24,4).equals("HEDR")) {
				game = GAME.UNKNOWN;
				tesRecordLen = 24; //fallout3, skyrim				
				//TODO: work out the game?						
			} else {
				throw new PluginException(pluginFileName + ": HEDR not found");
			}
			// fileFormat figured out below
		} else  if (startChars.startsWith("TES3")) {
			// master, plugin 
			//TES3<- ess files have a HEDR at 16 like esp and esm (so the save file indicator is in the HEDR as flag 32)
			fileFormat = FILE_FORMAT.TES3;
			tesRecordLen = 16; //Morrowind
			game		= GAME.TES3;			
			// fileFormat not known yet
		} else if (startChars.startsWith("FO3SAVEGAME")) {
			fileFormat = FILE_FORMAT.TES4;
			game = GAME.FO3;
			tesRecordLen = 24;
			filePriority = FILE_PRIORITY.SAVE;			
		} else if (startChars.startsWith("TESV_SAVEGAME")) {
			//https://en.uesp.net/wiki/Skyrim_Mod:Save_File_Format#Header
			fileFormat = FILE_FORMAT.TES4;
			game = GAME.TESV;
			tesRecordLen = 24;
			filePriority = FILE_PRIORITY.SAVE;			
		} else if (startChars.startsWith("FO4_SAVEGAME")) {
			fileFormat = FILE_FORMAT.TES4;
			game = GAME.FO4;
			tesRecordLen = 24;
			filePriority = FILE_PRIORITY.SAVE;			
		} else {
			throw new PluginException("File is not a ElderScrolls file format (" + fileName + ")");
		}
	
		// are we dealing with ESM/ESP type files?
		if(fileFormat == FILE_FORMAT.TES3 || (fileFormat == FILE_FORMAT.TES4 && filePriority != FILE_PRIORITY.SAVE	) ) {		
			byte[] tesRecordData = new byte[tesRecordLen];
			count = ch.read(ByteBuffer.wrap(tesRecordData), pos);	
			pos += tesRecordLen;
			recordType = new String(tesRecordData, 0, 4);
			int headerLength = ESMByteConvert.extractInt(tesRecordData, 4);
			if(tesRecordLen == 20) {
				recordFlags1 = ESMByteConvert.extractInt(tesRecordData, 8);
			} else if(tesRecordLen == 24) {
				unknownInt = ESMByteConvert.extractInt(tesRecordData, 8);
				recordFlags1 = ESMByteConvert.extractInt(tesRecordData, 12);
			}		
			
			subrecordList = new ArrayList<Subrecord>();
			
			if (fileFormat == FILE_FORMAT.TES4) {			
				filePriority = (recordFlags1 & 0x01) != 0 ? FILE_PRIORITY.MASTER : FILE_PRIORITY.PLUGIN;
				
				count += readTES4(ch, pos, headerLength);
			} else  if (fileFormat == FILE_FORMAT.TES3) {				
				count += readTES3(ch, pos, headerLength);
			} 	
		} else {
			// save files for tes4 are binary and not at all like the esm/esp, in fact our editor won't even display them so let's not really bother 
			if (startChars.startsWith("TES4SAVEGAME")) {
				//https://en.uesp.net/wiki/Oblivion_Mod:Save_File_Format							
			} else if (startChars.startsWith("FO3SAVEGAME")) {	
				//https://falloutmods.fandom.com/wiki/FOS_file_format
			} else if (startChars.startsWith("TESV_SAVEGAME")) {
				//https://en.uesp.net/wiki/Skyrim_Mod:Save_File_Format#Header				
			} else if (startChars.startsWith("FO4_SAVEGAME")) {	
				
			}
		}
		return count;
	}
		
	//return bytes read off
	private int readTES4(FileChannel ch, long pos, int headerLength) throws PluginException, IOException {
			
		int bytesRead = 0;
		byte buffer[] = new byte[1024];
		do {
			if (headerLength < 6)
				break;

			// read a sub record as 4 char type and short data length(len not incl. 6 bytes)
			byte[] subrecordHeader = new byte[6];
			int count = ch.read(ByteBuffer.wrap(subrecordHeader), pos);	
			bytesRead += count;
			pos += subrecordHeader.length;

			if (count != 6)
				throw new PluginException(pluginFileName + ": Header subrecord prefix truncated");
			headerLength -= 6;

			int dataLen = ESMByteConvert.extractShort(subrecordHeader, 4);

			if (dataLen > headerLength)
				throw new PluginException(pluginFileName + ": Subrecord length exceeds header length");
			if (dataLen > buffer.length)
				buffer = new byte[dataLen];

			count = ch.read(ByteBuffer.wrap(buffer,0,dataLen), pos);	
			bytesRead += count;
			pos += dataLen;
			if (count != dataLen)
				throw new PluginException(pluginFileName + ": Header subrecord data truncated");
			headerLength -= count;

			String type = new String(subrecordHeader, 0, 4);
			
			// throw the raw data in so the editor can show it
			byte subrecordData[] = new byte[dataLen];
			System.arraycopy(buffer, 0, subrecordData, 0, dataLen);
			subrecordList.add(new PluginSubrecord(type, subrecordData));

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
			} else if (type.equals("DATA")) {
				// must follow MAST   
				int masterLength = ESMByteConvert.extractInt64(buffer, 0);
			} else if (type.equals("ONAM")) {
				 
			} else {
				System.out.println(pluginFileName + ": " + type + " : unregistered subrecord in header");
			}
		} while (true);

		if (headerLength != 0)
			throw new PluginException(pluginFileName + ": Header is incomplete");
		else
			return bytesRead;
	}

	//http://www.uesp.net/morrow/tech/mw_esm.txt
	//https://www.mwmythicmods.com/argent/tech/es_format.html	
	//return bytes read off
	private int readTES3(FileChannel ch, long pos, int headerLength) throws PluginException, IOException {
		// set up our record data so the editor can see the tes record as well
		
		int bytesRead = 0;
		byte buffer[] = new byte[1024];
		do {
			 if (headerLength < 8)
				break;
			
			byte[] recordHeader = new byte[8]; 
			int count = ch.read(ByteBuffer.wrap(recordHeader), pos);	
			bytesRead += count;
			pos += recordHeader.length;

			if (count != 8)
				throw new PluginException(pluginFileName + ": Header subrecord prefix truncated");
			headerLength -= count;
			
			String type = new String(recordHeader, 0, 4);
			//notice TES3 subrecord len is 4 bytes not 2 like TES4
			int dataLen = ESMByteConvert.extractInt(recordHeader, 4);

			if (dataLen > headerLength)
				throw new PluginException(pluginFileName + ": Subrecord length exceeds header length " +  dataLen + " > " + headerLength);
			if (dataLen > buffer.length)
				buffer = new byte[dataLen];

			count = ch.read(ByteBuffer.wrap(buffer,0,dataLen), pos);	
			bytesRead += count;
			pos += dataLen;
			
			if (count != dataLen)
				throw new PluginException(pluginFileName + ": Header subrecord data truncated");
			headerLength -= count;
			
			
			// throw the raw data in so the editor can show it
			byte subrecordData[] = new byte[dataLen];
			System.arraycopy(buffer, 0, subrecordData, 0, dataLen);
			subrecordList.add(new PluginSubrecord(type, subrecordData));
			

			if (type.equals("HEDR")) {
				if (dataLen < 8)
					throw new PluginException(pluginFileName + ": HEDR subrecord is too small");

				pluginVersion = Float.intBitsToFloat(ESMByteConvert.extractInt(buffer, 0));
				int fileType = ESMByteConvert.extractInt(buffer, 4); // (0=esp file; 1=esm file; 32=ess file)		
				filePriority = fileType == 0 ? FILE_PRIORITY.PLUGIN : fileType == 1 ? FILE_PRIORITY.MASTER : fileType == 32 ? FILE_PRIORITY.SAVE : FILE_PRIORITY.UNKNOWN;
				creator = new String(buffer, 8, 32);
				summary = new String(buffer, 40, 256);				
				
				recordCount = ESMByteConvert.extractInt(buffer, 296);				
				
			}  else if (type.equals("MAST") && dataLen > 1) {
				masterList.add(new String(buffer, 0, dataLen - 1));
				//System.out.println("MAST " + new String(buffer, 0, length - 1));
			} else if (type.equals("DATA")) {
				// must follow MAST   
				int masterLength = ESMByteConvert.extractInt64(buffer, 0);
			}
			
			//ESS files also have
	/*		GMDT (124 bytes)
			float Unknown[6]
				- Unknown values loc rot?
			char  CellName[64]
				- Current cell name of character?
			float Unknown
			char CharacterName[32]
		SCRD (20 bytes)			unknown combination of short/longs? Related to SCRS?
		SCRS (65536 bytes)			Looks like an array of byte data.		Possible the save game screenshot.*/
			else if (type.equals("GMDT")) {
		
			} else if (type.equals("SCRD")) {
		
			} else if (type.equals("SCRS")) {
		
			} else {
				System.out.println(pluginFileName + ": " + type + " : unregistered subrecord in header");
			}
			
		} while (true);

		if (headerLength != 0)
			throw new PluginException(pluginFileName + ": Header is incomplete");
		else
			return bytesRead;

	}
}
