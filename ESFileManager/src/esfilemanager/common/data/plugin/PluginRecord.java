package esfilemanager.common.data.plugin;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import esfilemanager.common.PluginException;
import esfilemanager.common.data.record.Record;
import esfilemanager.common.data.record.Subrecord;
import tools.io.ESMByteConvert;
import tools.io.FileChannelRAF;

/**
 * https://en.m.uesp.net/wiki/Skyrim_Mod:Mod_File_Format#Records
 *  
 *  I notice there is that odd prefix minus issue appearing to be goign on about the place,m so be careful
 *  lots of obliv and morrwind record sub info here
 *   https://gitlab.com/OpenMW/openmw/-/blob/master/apps/esmtool/record.cpp
 * https://gitlab.com/OpenMW/openmw/-/tree/master/components/esm4
 * 
 */

public class PluginRecord extends Record {
	
	protected int		headerByteCount		= -1;

	protected byte[]	recordData;
	
	protected int		recordLength;

	protected long		filePositionPointer	= -1;

	//for tes3 version
	protected PluginRecord() {

	}

	/**
	 * prefix MUST have length of either 20 (oblivion) or 24 (fallout) headerbyte count
	 * @param prefix
	 */
	public PluginRecord(byte prefix[]) {
		if (prefix.length != 20 && prefix.length != 24) {
			throw new IllegalArgumentException("The record prefix is not 20 or 24 bytes as required");
		} else {
			headerByteCount = prefix.length;
			// memory saving mechanism  https://www.baeldung.com/java-string-pool
			recordType = new String(prefix, 0, 4).intern();
			recordLength = ESMByteConvert.extractInt(prefix, 4);			
			recordFlags = ESMByteConvert.extractInt(prefix, 8);
			formID = ESMByteConvert.extractInt3(prefix, 12);		
			masterID = ESMByteConvert.extractByte(prefix, 15);
			timeStamp = ESMByteConvert.extractShort(prefix, 16);
			versionControl = ESMByteConvert.extractShort(prefix, 18);
			if (prefix.length == 24) {
				internalVersion = ESMByteConvert.extractShort(prefix, 20);
				unknownShort = ESMByteConvert.extractShort(prefix, 22);
			}
		}
	}
	

	/**
	 * non sync required load from filechannel
	 * @param in
	 * @param pointer
	 * @param headerByteCount
	 * @throws PluginException
	 */
	public PluginRecord(FileChannelRAF in, long pointer, int headerByteCount) throws PluginException {
		try {
			// read header off
			byte prefix[] = new byte[headerByteCount];
			FileChannel ch = in.getChannel();

			// use this non sync call for speed
			ByteBuffer bb = ByteBuffer.wrap(prefix);
			int count = ch.read(bb, pointer);

			if (count != headerByteCount) {
				throw new PluginException(": PluginRecord prefix is incomplete " + formID);
			}


			if (prefix.length != 20 && prefix.length != 24) {
				throw new IllegalArgumentException("The record prefix is not 20 or 24 bytes as required");
			} else {
				headerByteCount = prefix.length;
				// memory saving mechanism  https://www.baeldung.com/java-string-pool
				recordType = new String(prefix, 0, 4).intern();
				recordLength = ESMByteConvert.extractInt(prefix, 4);
				formID = ESMByteConvert.extractInt3(prefix, 12);
				recordFlags = ESMByteConvert.extractInt(prefix, 8);
				timeStamp = ESMByteConvert.extractShort(prefix, 16);
				versionControl = ESMByteConvert.extractShort(prefix, 18);
				if (prefix.length == 24) {
					internalVersion = ESMByteConvert.extractShort(prefix, 20);
					unknownShort = ESMByteConvert.extractShort(prefix, 22);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * For forcibly creating these things not from file
	 */
	public PluginRecord(int headerByteCount, String recordType, int formID) {
		this.headerByteCount = headerByteCount;
		// memory saving mechanism  https://www.baeldung.com/java-string-pool
		this.recordType = recordType.intern();
		this.formID = formID;
		this.recordFlags = 0;
		subrecordList = new ArrayList<Subrecord>();
	}

	public String getEditorID() {
		for (Subrecord sr : getSubrecords()) {
			if (sr.getSubrecordType().equals("EDID"))
				return new String(sr.getSubrecordData(), 0, sr.getSubrecordData().length - 1);
		}
		return "";
	}
	
	/**
	 * Does not touch file pointer at all, no sync on in required
	 * @param in
	 * @param position
	 * @param recordLength
	 * @throws PluginException
	 * @throws IOException
	 * @throws DataFormatException
	 */
	public void load(FileChannelRAF in, long position)
			throws PluginException, IOException {
		FileChannel ch = in.getChannel();
		filePositionPointer = position;

		recordData = new byte[recordLength];

		// use this non sync call for speed
		int count = ch.read(ByteBuffer.wrap(recordData), filePositionPointer);
		if (count != recordLength)
			throw new PluginException(" : " + recordType + " record is incomplete");

	}

	private boolean uncompressRecordData() throws DataFormatException, PluginException {
		if (!isCompressed())
			return false;
		if (recordData.length < 5 || recordData [3] >= 32)
			throw new PluginException("Compressed data prefix is not valid");
		int length = ESMByteConvert.extractInt(recordData, 0);
		byte buffer[] = new byte[length];

		Inflater expand = new Inflater();
		expand.setInput(recordData, 4, recordData.length - 4);
		int count = expand.inflate(buffer);
		if (count != length) {
			throw new PluginException("Expanded data less than data length");
		} else {
			expand.end();
			recordData = buffer;
		}

		return true;
	}

	@Override
	public List<Subrecord> getSubrecords() {
		// must fill it up before anyone can get it asynch!
		synchronized (this) {
			if (subrecordList == null) {
				loadSubRecords();
			}
			return subrecordList;
		}
	}

	/**
	 * pulls the sub records from the raw byte array if required, and dumps the bytes, synch calls only one time deal!
	 */
	private void loadSubRecords() {
		subrecordList = new ArrayList<Subrecord>();
		try {
			uncompressRecordData();

			int offset = 0;
			int overrideLength = 0;

			if (recordData != null) {
				while (offset < recordData.length) {
					String subrecordType = new String(recordData, offset, 4);
					int subrecordLength = recordData [offset + 4]	& 0xff
											| (recordData [offset + 5] & 0xff) << 8;
					if (subrecordType.equals("XXXX")) {
						overrideLength = ESMByteConvert.extractInt(recordData, offset + 6);
						offset += 6 + 4;
						continue;
					}
					if (subrecordLength == 0) {
						subrecordLength = overrideLength;
						overrideLength = 0;
					}
					byte subrecordData[] = new byte[subrecordLength];

					// bad decompress can happen (LAND record in falloutNV)
					if (offset + 6 + subrecordLength <= recordData.length)
						System.arraycopy(recordData, offset + 6, subrecordData, 0, subrecordLength);

					subrecordList.add(new PluginSubrecord(subrecordType, subrecordData));

					offset += 6 + subrecordLength;
				}
			}

			// discard the raw data as used now
			recordData = null;
		} catch (DataFormatException e) {
			//FalloutNV has one of this
			System.err.println("java.util.zip.DataFormatException: incorrect data check");
		} catch (PluginException e) {
			e.printStackTrace();
		}
			

	}

	@Override
	public String toString() {
		String text = "" + recordType + " record: " + getEditorID() + " (" + formID + ")";
		if (isIgnored()) {
			text = "(Ignore) " + text;
		} else if (isDeleted()) {
			text = "(Deleted) " + text;
		}
		return text;
	}

	public long getFilePositionPointer() {
		return filePositionPointer;
	}

	
	public int getRecordDataLen() {
		return recordLength;
	}



}
