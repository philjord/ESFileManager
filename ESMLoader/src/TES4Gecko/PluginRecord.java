package TES4Gecko;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class PluginRecord extends SerializedElement implements Cloneable
{
	private static final String dummyEditorID = new String();

	private String recordType;

	private int recordFlags1;

	private int recordFlags2;

	private int formID;

	private String editorID;

	private long recordPosition = -1L;

	private int recordLength;

	private byte[] digest;

	private PluginRecord parentRecord;

	public PluginRecord(String recordType)
	{
		this.recordType = recordType;
		this.editorID = dummyEditorID;
	}

	public PluginRecord(String recordType, int formID)
	{
		this.recordType = recordType;
		this.formID = formID;
		this.editorID = dummyEditorID;
	}

	public PluginRecord(byte[] prefix)
	{
		if (prefix.length != 20)
		{
			throw new IllegalArgumentException("The record prefix is not 20 bytes");
		}

		this.recordType = new String(prefix, 0, 4);
		this.editorID = dummyEditorID;
		this.formID = getInteger(prefix, 12);
		this.recordFlags1 = getInteger(prefix, 8);
		this.recordFlags2 = getInteger(prefix, 16);
	}

	public PluginRecord getParent()
	{
		return this.parentRecord;
	}

	public void setParent(PluginRecord parent)
	{
		this.parentRecord = parent;
	}

	public boolean isDeleted()
	{
		return (this.recordFlags1 & 0x20) != 0;
	}

	public void setDelete(boolean deleted)
	{
		if (deleted)
			this.recordFlags1 |= 32;
		else if ((this.recordFlags1 & 0x20) != 0)
			this.recordFlags1 ^= 32;
	}

	public boolean isIgnored()
	{
		return (this.recordFlags1 & 0x1000) != 0;
	}

	public void setIgnore(boolean ignored)
	{
		if (ignored)
			this.recordFlags1 |= 4096;
		else if ((this.recordFlags1 & 0x1000) != 0)
			this.recordFlags1 ^= 4096;
	}

	public boolean isCompressed()
	{
		return (this.recordFlags1 & 0x40000) != 0;
	}

	public String getRecordType()
	{
		return this.recordType;
	}

	public int getRecordFlags()
	{
		return this.recordFlags1;
	}

	public int getFormID()
	{
		return this.formID;
	}

	public void setFormID(int formID)
	{
		this.formID = formID;
	}

	public String getEditorID()
	{
		return this.editorID;
	}

	public void setEditorID(String editorID) throws DataFormatException, IOException, PluginException
	{
		this.editorID = editorID;

		List<PluginSubrecord> subrecords = getSubrecords();
		ListIterator<PluginSubrecord> lit = subrecords.listIterator();
		while (lit.hasNext())
		{
			PluginSubrecord subrecord = lit.next();
			if (subrecord.getSubrecordType().equals("EDID"))
			{
				lit.remove();
				break;
			}

		}

		byte[] edidData = editorID.getBytes();
		byte[] subrecordData = new byte[edidData.length + 1];
		System.arraycopy(edidData, 0, subrecordData, 0, edidData.length);
		subrecordData[edidData.length] = 0;
		PluginSubrecord edidSubrecord = new PluginSubrecord(this.recordType, "EDID", subrecordData);
		subrecords.add(0, edidSubrecord);
		setSubrecords(subrecords);
	}

	public int getRecordLength()
	{
		return this.recordLength;
	}

	public byte[] getDigest()
	{
		return this.digest;
	}

	public byte[] getRecordData() throws DataFormatException, IOException, PluginException
	{
		if (this.recordLength == 0)
		{
			return new byte[0];
		}

		byte[] recordData = Main.pluginSpill.read(this.recordPosition, this.recordLength);

		if (!isCompressed())
		{
			return recordData;
		}

		if ((recordData.length < 5) || (recordData[3] >= 32))
		{
			throw new PluginException("Compressed data prefix is not valid");
		}

		int length = getInteger(recordData, 0);
		byte[] buffer = new byte[length];
		Inflater expand = new Inflater();
		expand.setInput(recordData, 4, recordData.length - 4);
		int count = expand.inflate(buffer);
		if (count != length)
		{
			throw new PluginException("Expanded data less than data length");
		}
		expand.end();

		return buffer;
	}

	public void setRecordData(byte[] buffer) throws DataFormatException, IOException
	{
		byte[] recordData;
		if (isCompressed())
		{
			int length = buffer.length;
			Deflater comp = new Deflater(6);
			comp.setInput(buffer);
			comp.finish();
			byte[] compBuffer = new byte[length + 20];
			int compLength = comp.deflate(compBuffer);
			if (compLength == 0)
			{
				throw new DataFormatException("Unable to compress " + this.recordType + " record " + this.editorID);
			}
			if (!comp.finished())
			{
				throw new DataFormatException("Compressed buffer is too small");
			}
			comp.end();
			recordData = new byte[4 + compLength];
			setInteger(length, recordData, 0);
			System.arraycopy(compBuffer, 0, recordData, 4, compLength);
		}
		else
		{
			recordData = buffer;
		}

		this.recordPosition = Main.pluginSpill.write(recordData);
		this.recordLength = recordData.length;
		try
		{
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(recordData);
			this.digest = md.digest();
		}
		catch (NoSuchAlgorithmException exc)
		{
			throw new UnsupportedOperationException("MD5 digest algorithm is not supported", exc);
		}
	}

	public List<PluginSubrecord> getSubrecords() throws DataFormatException, IOException, PluginException
	{
		List<PluginSubrecord> subrecordList = new ArrayList<PluginSubrecord>();
		byte[] recordData = getRecordData();
		int offset = 0;
		int overrideLength = 0;

		while (offset < recordData.length)
		{
			String subrecordType = new String(recordData, offset, 4);
			int subrecordLength = getShort(recordData, offset + 4);
			if (subrecordType.equals("XXXX"))
			{
				overrideLength = getInteger(recordData, offset + 6);
			}
			else
			{
				if (subrecordLength == 0)
				{
					subrecordLength = overrideLength;
					overrideLength = 0;
				}

				byte[] subrecordData = new byte[subrecordLength];
				System.arraycopy(recordData, offset + 6, subrecordData, 0, subrecordLength);
				subrecordList.add(new PluginSubrecord(this.recordType, subrecordType, subrecordData));
			}

			offset += 6 + subrecordLength;
		}

		return subrecordList;
	}

	public boolean addAdditionalSubrecord(String subrecordType, Object subrecordData) throws DataFormatException, IOException,
			PluginException
	{
		byte[] dataBytes = convertToByteArray(subrecordData);
		boolean typeFound = false;
		List<PluginSubrecord> subrecordList = getSubrecords();
		PluginSubrecord newSub = new PluginSubrecord(getRecordType(), subrecordType, dataBytes);
		ListIterator<PluginSubrecord> lit = subrecordList.listIterator();
		while (lit.hasNext())
		{
			PluginSubrecord checkSubrecord = lit.next();
			if (checkSubrecord.getSubrecordType().equals(subrecordType))
				continue;
			if (typeFound)
			{
				lit.previous();
				lit.add(newSub);
				setSubrecords(subrecordList);
				return true;
			}

			typeFound = true;
			if (checkSubrecord.equals(newSub))
			{
				return false;
			}
		}

		return false;
	}

	public boolean insertSubrecordAfter(String subrecordType, Object subrecordData, String subrecordAfterType) throws DataFormatException,
			IOException, PluginException
	{
		byte[] dataBytes = convertToByteArray(subrecordData);
		List<PluginSubrecord> subrecordList = getSubrecords();
		PluginSubrecord newSub = new PluginSubrecord(getRecordType(), subrecordType, dataBytes);
		ListIterator<PluginSubrecord> lit = subrecordList.listIterator(subrecordList.size());
		while (lit.hasPrevious())
		{
			PluginSubrecord checkSubrecord = lit.previous();
			if (!checkSubrecord.getSubrecordType().equals(subrecordAfterType))
				continue;
			lit.next();
			lit.add(newSub);
			setSubrecords(subrecordList);
			return true;
		}

		return false;
	}

	public void addSubrecord(String subrecordType, Object subrecordData) throws DataFormatException, IOException, PluginException
	{
		byte[] dataBytes = convertToByteArray(subrecordData);
		boolean typeFound = false;
		List<PluginSubrecord> subrecordList = getSubrecords();
		PluginSubrecord newSub = new PluginSubrecord(getRecordType(), subrecordType, dataBytes);
		subrecordList.add(newSub);
		setSubrecords(subrecordList);
	}

	public boolean changeSubrecord(String subrecordType, Object oldSubData, Object newSubData) throws DataFormatException, IOException,
			PluginException
	{
		if (!oldSubData.getClass().equals(newSubData.getClass()))
		{
			throw new DataFormatException("changeSubrecord: Argument 2 is of class " + oldSubData.getClass()
					+ " while argument 3 is of class " + newSubData.getClass());
		}

		byte[] oldDataBytes = convertToByteArray(oldSubData);
		byte[] newDataBytes = convertToByteArray(newSubData);
		List<PluginSubrecord> subrecordList = getSubrecords();
		PluginSubrecord oldSub = new PluginSubrecord(getRecordType(), subrecordType, oldDataBytes);
		PluginSubrecord newSub = new PluginSubrecord(getRecordType(), subrecordType, newDataBytes);
		ListIterator<PluginSubrecord> lit = subrecordList.listIterator();
		while (lit.hasNext())
		{
			PluginSubrecord checkSubrecord = lit.next();
			if (!checkSubrecord.equals(oldSub))
				continue;
			lit.set(newSub);
			setSubrecords(subrecordList);
			return true;
		}

		return false;
	}

	public boolean hasSubrecordWithData(String subrecordType, Object subrecData) throws DataFormatException, IOException, PluginException
	{
		byte[] dataBytes = convertToByteArray(subrecData);
		List<?> subrecordList = getSubrecords();
		PluginSubrecord checkRec = new PluginSubrecord(getRecordType(), subrecordType, dataBytes);
		ListIterator<?> lit = subrecordList.listIterator();
		while (lit.hasNext())
		{
			PluginSubrecord checkSubrecord = (PluginSubrecord) lit.next();
			if (checkSubrecord.equals(checkRec))
			{
				return true;
			}
		}

		return false;
	}

	public boolean removeSubrecords(HashSet<String> subrecordTypes, boolean notInSet) throws DataFormatException, IOException,
			PluginException
	{
		boolean atLeastOne = false;
		List<PluginSubrecord> subrecordList = getSubrecords();
		ListIterator<PluginSubrecord> lit = subrecordList.listIterator();
		while (lit.hasNext())
		{
			PluginSubrecord subrec = lit.next();
			if (!(notInSet ^ subrecordTypes.contains(subrec.getSubrecordType())))
				continue;
			lit.remove();
			setSubrecords(subrecordList);
			atLeastOne = true;
		}

		if (atLeastOne)
		{
			setSubrecords(subrecordList);
		}
		return atLeastOne;
	}

	public boolean removeCTDASubrecords(HashSet<Integer> functionCodes, boolean notInSet) throws DataFormatException, IOException,
			PluginException
	{
		boolean atLeastOne = false;
		List<PluginSubrecord> subrecordList = getSubrecords();
		ListIterator<PluginSubrecord> lit = subrecordList.listIterator();
		while (lit.hasNext())
		{
			PluginSubrecord subrec = lit.next();
			if (subrec.getSubrecordType().equals("CTDA"))
			{
				byte[] subrecordData = subrec.getSubrecordData();
				int functionCode = getInteger(subrecordData, 8);

				if (!(notInSet ^ functionCodes.contains(Integer.valueOf(functionCode))))
					continue;
				lit.remove();
				setSubrecords(subrecordList);
				atLeastOne = true;
			}

		}

		if (atLeastOne)
		{
			setSubrecords(subrecordList);
		}
		return atLeastOne;
	}

	public boolean removeSubrecords(String subrecordType) throws DataFormatException, IOException, PluginException
	{
		HashSet<String> onlyOne = new HashSet<String>(1);
		onlyOne.add(subrecordType);
		return removeSubrecords(onlyOne, false);
	}

	public List<PluginSubrecord> getAllSubrecords(String subrecordType) throws DataFormatException, IOException, PluginException
	{
		List<PluginSubrecord> subrecordList = getSubrecords();
		List<PluginSubrecord> returnList = new ArrayList<PluginSubrecord>();
		for (PluginSubrecord sub : subrecordList)
		{
			if (sub.getSubrecordType().equals(subrecordType))
				returnList.add(sub);
		}
		return returnList;
	}

	public PluginSubrecord getSubrecord(String subrecordType) throws DataFormatException, IOException, PluginException
	{
		List<PluginSubrecord> subrecordList = getSubrecords();
		for (PluginSubrecord sub : subrecordList)
		{
			if (sub.getSubrecordType().equals(subrecordType))
				return sub;
		}
		return null;
	}

	public boolean hasSubrecordOfType(String subrecordType)
	{
		try
		{
			List<PluginSubrecord> subrecordList = getSubrecords();
			for (PluginSubrecord sub : subrecordList)
			{
				if (sub.getSubrecordType().equals(subrecordType))
					return true;
			}
		}
		catch (Exception ex)
		{
			return false;
		}
		return false;
	}

	public static byte[] convertToByteArray(Object data) throws DataFormatException
	{
		byte[] dataBytes = null;
		if ((data instanceof Byte))
		{
			dataBytes = new byte[1];
			dataBytes[0] = ((Byte) data).byteValue();
		}
		else if ((data instanceof Short))
		{
			dataBytes = new byte[2];
			SerializedElement.setShort(((Short) data).shortValue(), dataBytes, 0);
		}
		else if ((data instanceof Integer))
		{
			dataBytes = new byte[4];
			SerializedElement.setInteger(((Integer) data).intValue(), dataBytes, 0);
		}
		else if ((data instanceof Long))
		{
			dataBytes = new byte[8];
			SerializedElement.setLong(((Long) data).longValue(), dataBytes, 0);
		}
		else if ((data instanceof Float))
		{
			dataBytes = new byte[8];
			int tmp = Float.floatToIntBits(((Float) data).floatValue());
			SerializedElement.setInteger(tmp, dataBytes, 0);
		}
		else if ((data instanceof Double))
		{
			dataBytes = new byte[8];
			long tmp = Double.doubleToLongBits(((Double) data).doubleValue());
			SerializedElement.setLong(tmp, dataBytes, 0);
		}
		else if ((data instanceof String))
		{
			dataBytes = new byte[((String) data).length()];
			System.arraycopy(((String) data).getBytes(), 0, dataBytes, 0, ((String) data).length());
		}
		else if ((data instanceof int[]))
		{
			dataBytes = new byte[((int[]) data).length * 4];
			SerializedElement.setIntegerArray((int[]) data, dataBytes, 0);
		}
		else if ((data instanceof byte[]))
		{
			dataBytes = (byte[]) data;
		}
		else
		{
			throw new DataFormatException("convertToByteArray: Argument is of unrecognized class " + data.getClass());
		}
		return dataBytes;
	}

	public void setSubrecords(List<PluginSubrecord> subrecordList) throws DataFormatException, IOException
	{
		int length = 0;
		for (PluginSubrecord subrecord : subrecordList)
		{
			int subrecordLength = subrecord.getSubrecordData().length;
			length += 6 + subrecordLength;
			if (subrecordLength > 65535)
			{
				length += 10;
			}

		}

		byte[] recordData = new byte[length];
		int offset = 0;
		for (PluginSubrecord subrecord : subrecordList)
		{
			byte[] subrecordData = subrecord.getSubrecordData();
			int subrecordLength = subrecordData.length;
			if (subrecordLength > 65535)
			{
				System.arraycopy("XXXX".getBytes(), 0, recordData, offset, 4);
				setShort(4, recordData, offset + 4);
				setInteger(subrecordLength, recordData, offset + 6);
				offset += 10;
			}

			System.arraycopy(subrecord.getSubrecordType().getBytes(), 0, recordData, offset, 4);

			if (subrecordLength > 65535)
				setShort(0, recordData, offset + 4);
			else
			{
				setShort(subrecordLength, recordData, offset + 4);
			}
			System.arraycopy(subrecordData, 0, recordData, offset + 6, subrecordLength);
			offset += 6 + subrecordLength;
		}

		setRecordData(recordData);
	}

	public void changeFormID(int newFormID)
	{
		if ((this.parentRecord != null) && ((this.parentRecord instanceof PluginGroup)))
		{
			PluginGroup parentGroup = (PluginGroup) this.parentRecord;
			List<?> parentList = parentGroup.getRecordList();
			int index = parentList.indexOf(this);
			if ((index >= 0) && (index < parentList.size() - 1))
			{
				PluginRecord checkRecord = (PluginRecord) parentList.get(index + 1);
				if ((checkRecord instanceof PluginGroup))
				{
					PluginGroup checkGroup = (PluginGroup) checkRecord;
					if (checkGroup.getGroupParentID() == this.formID)
					{
						checkGroup.setGroupParentID(newFormID);

						List<PluginRecord> subgroupList = checkGroup.getRecordList();
						for (PluginRecord subgroupRecord : subgroupList)
						{
							if ((subgroupRecord instanceof PluginGroup))
							{
								checkGroup = (PluginGroup) subgroupRecord;
								if (checkGroup.getGroupParentID() == this.formID)
								{
									checkGroup.setGroupParentID(newFormID);
								}
							}
						}
					}
				}

			}

		}

		this.formID = newFormID;
	}

	public boolean updateReferences(FormAdjust formAdjust) throws DataFormatException, IOException, PluginException
	{
		boolean recordModified = false;
		List<PluginSubrecord> subrecords = getSubrecords();
		for (PluginSubrecord subrecord : subrecords)
		{
			boolean subrecordModified = false;
			byte[] subrecordData = subrecord.getSubrecordData();
			int[][] references = subrecord.getReferences();
			if ((references == null) || (references.length == 0))
			{
				continue;
			}

			for (int i = 0; i < references.length; i++)
			{
				int offset = references[i][0];
				int oldFormID = references[i][1];
				if (oldFormID == 0)
				{
					continue;
				}
				int newFormID = formAdjust.adjustFormID(oldFormID);
				if (newFormID != oldFormID)
				{
					setInteger(newFormID, subrecordData, offset);
					subrecordModified = true;
				}

			}

			if (subrecordModified)
			{
				subrecord.setSubrecordData(subrecordData);
				recordModified = true;
			}

		}

		if (recordModified)
		{
			setSubrecords(subrecords);
		}
		return recordModified;
	}

	List<PluginRecord> getAllPluginRecords()
	{
		ArrayList<PluginRecord> recList = new ArrayList<PluginRecord>();
		recList.add(this);
		return recList;
	}

	List<PluginRecord> getDeletedPluginRecords()
	{
		ArrayList<PluginRecord> recList = new ArrayList<PluginRecord>();
		if (isDeleted())
			recList.add(this);
		return recList;
	}

	public int hashCode()
	{
		return this.formID;
	}

	public boolean equals(Object object)
	{
		boolean areEqual = false;
		if ((object instanceof PluginRecord))
		{
			PluginRecord objRecord = (PluginRecord) object;
			if (objRecord.getRecordType().equals(this.recordType))
			{
				if (this.recordType.equals("GMST"))
				{
					if (objRecord.getEditorID().equals(this.editorID))
						areEqual = true;
				}
				else if (objRecord.getFormID() == this.formID)
				{
					areEqual = true;
				}
			}
		}

		return areEqual;
	}

	public boolean isIdentical(PluginRecord record)
	{
		boolean areIdentical = false;

		boolean areEqual = equals(record);
		if (areEqual)
		{
			int cmpFlags1 = record.getRecordFlags();
			if ((this.recordFlags1 & 0xFFFBFFFE) != (cmpFlags1 & 0xFFFBFFFE))
			{
				areEqual = false;
			}

		}

		if (areEqual)
		{
			areIdentical = true;
			byte[] cmpDigest = record.getDigest();
			if (this.digest == null)
			{
				if (cmpDigest != null)
					areIdentical = false;
			}
			else if (cmpDigest == null)
				areIdentical = false;
			else
			{
				for (int i = 0; i < this.digest.length; i++)
				{
					if (this.digest[i] != cmpDigest[i])
					{
						areIdentical = false;
						break;
					}

				}

			}

		}

		if ((areEqual) && (!areIdentical))
		{
			try
			{
				areIdentical = true;
				List<PluginSubrecord> subrecords = getSubrecords();
				List<?> cmpSubrecords = record.getSubrecords();
				String cmpDisplayValue = "";
				for (PluginSubrecord subrecord : subrecords)
				{
					ListIterator<?> lit = cmpSubrecords.listIterator();
					areIdentical = false;
					while (lit.hasNext())
					{
						PluginSubrecord cmpSubrecord = (PluginSubrecord) lit.next();
						try
						{
							cmpDisplayValue = cmpSubrecord.getDisplayData();
						}
						catch (Throwable exc)
						{
							cmpDisplayValue = "Cannot make display value for subrecord type " + cmpSubrecord.getSubrecordType()
									+ " of record type " + record.getRecordType();
						}
						if (cmpSubrecord.equals(subrecord))
						{
							areIdentical = true;
							lit.remove();
							break;
						}
					}

					if (!areIdentical)
					{
						if (!Main.debugMode)
							break;
						System.out.printf("Miscompare on %s subrecord [%s vs %s] of %s record %s (%08X)\n", new Object[]
						{ subrecord.getSubrecordType(), subrecord.getDisplayData(), cmpDisplayValue, this.recordType, this.editorID,
								Integer.valueOf(this.formID) });

						break;
					}
				}

				if ((areIdentical) && (cmpSubrecords.size() != 0))
					areIdentical = false;
			}
			catch (Throwable exc)
			{
				areIdentical = false;
				Main.logException("Unable to compare record data", exc);
			}
		}

		return areIdentical;
	}

	public String toString()
	{
		String text = null;

		if ((this.recordType.equals("CELL")) && (this.recordLength > 0) && (this.parentRecord != null)
				&& ((this.parentRecord instanceof PluginGroup)))
		{
			PluginGroup parentGroup = (PluginGroup) this.parentRecord;
			if (parentGroup.getGroupType() == 5)
			{
				try
				{
					List<PluginSubrecord> subrecords = getSubrecords();
					for (PluginSubrecord subrecord : subrecords)
						if (subrecord.getSubrecordType().equals("XCLC"))
						{
							byte[] subrecordData = subrecord.getSubrecordData();
							text = String.format("CELL (%d,%d) record: %s (%08X)", new Object[]
							{ Integer.valueOf(getInteger(subrecordData, 0)), Integer.valueOf(getInteger(subrecordData, 4)), this.editorID,
									Integer.valueOf(this.formID) });
							break;
						}
				}
				catch (Exception localException)
				{
				}
			}
		}
		if (text == null)
		{
			text = String.format("%s record: %s (%08X)", new Object[]
			{ this.recordType, this.editorID, Integer.valueOf(this.formID) });
		}
		if (isIgnored())
			text = "(Ignore) " + text;
		else if (isDeleted())
		{
			text = "(Deleted) " + text;
		}
		return text;
	}

	public void load(File file, RandomAccessFile in, int recordLength) throws PluginException, IOException, DataFormatException
	{
		int offset = 0;
		int overrideLength = 0;

		byte[] recordData = new byte[recordLength];
		int count = in.read(recordData);
		if (count != recordLength)
		{
			throw new PluginException(file.getName() + ": " + this.recordType + " record is incomplete");
		}
		this.recordPosition = Main.pluginSpill.write(recordData);
		this.recordLength = recordData.length;
		try
		{
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(recordData);
			this.digest = md.digest();
		}
		catch (NoSuchAlgorithmException exc)
		{
			throw new UnsupportedOperationException("MD5 digest algorithm is not supported", exc);
		}

		byte[] buffer = getRecordData();
		int dataLength = buffer.length;

		while (dataLength >= 6)
		{
			String subrecordType = new String(buffer, offset, 4);
			int length = getShort(buffer, offset + 4);
			if (length == 0)
			{
				length = overrideLength;
				overrideLength = 0;
			}

			if (length > dataLength)
			{
				throw new PluginException(file.getName() + ": " + subrecordType + " subrecord is incomplete");
			}
			if (length > 0)
			{
				if (subrecordType.equals("XXXX"))
				{
					if (length != 4)
					{
						throw new PluginException(file.getName() + ": XXXX subrecord data length is not 4");
					}
					overrideLength = getInteger(buffer, offset + 6);
				}
				else if ((subrecordType.equals("EDID")) && (length > 1))
				{
					this.editorID = new String(buffer, offset + 6, length - 1);
				}
			}

			offset += 6 + length;
			dataLength -= 6 + length;
		}

		if (dataLength != 0)
			throw new PluginException(file.getName() + ": " + this.recordType + " record is incomplete");
	}

	public void store(RandomAccessFile out) throws IOException
	{
		byte[] prefix = new byte[20];
		System.arraycopy(this.recordType.getBytes(), 0, prefix, 0, 4);
		setInteger(this.recordLength, prefix, 4);
		setInteger(this.recordFlags1, prefix, 8);
		setInteger(this.formID, prefix, 12);
		setInteger(this.recordFlags2, prefix, 16);
		out.write(prefix);

		if (this.recordLength != 0)
		{
			byte[] recordData = Main.pluginSpill.read(this.recordPosition, this.recordLength);
			out.write(recordData);
		}
	}

	public Object clone()
	{
		Object clonedObject;
		try
		{
			clonedObject = super.clone();
		}
		catch (CloneNotSupportedException exc)
		{

			throw new UnsupportedOperationException("Unable to clone record", exc);
		}

		return clonedObject;
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.PluginRecord
 * JD-Core Version:    0.6.0
 */