package esmLoader.tes3;

public class PluginSubrecord
{
	private String recordType;

	private String subrecordType;

	private byte subrecordData[];

	public PluginSubrecord(String recordType, String subrecordType, byte subrecordData[])
	{
		this.recordType = recordType;
		this.subrecordType = subrecordType;
		this.subrecordData = subrecordData;
	}

	public String getRecordType()
	{
		return recordType;
	}

	public String getSubrecordType()
	{
		return subrecordType;
	}

	public byte[] getSubrecordData()
	{
		return subrecordData;
	}

	public void setSubrecordData(byte subrecordData[])
	{
		this.subrecordData = subrecordData;
	}

	public String toString()
	{
		return subrecordType + " subrecord";
	}

	public String displaySubrecord()
	{
		StringBuffer dumpData = new StringBuffer(128 + 3 * subrecordData.length + 6 * (subrecordData.length / 16));
		dumpData.append("" + getSubrecordType() + " subrecord: Data length x'" + subrecordData.length + "'\n");
		dumpData.append("\n       0  1  2  3  4  5  6  7  8  9  A  B  C  D  E  F\n");
		StringBuffer dumpHex = new StringBuffer(48);
		StringBuffer dumpLine = new StringBuffer(16);
		for (int i = 0; i < subrecordData.length; i += 16)
		{
			for (int j = 0; j < 16; j++)
			{
				int offset = i + j;
				if (offset == subrecordData.length)
					break;
				// dumpHex.append(String.format(" %02X", new Object[] {
				// Byte.valueOf(subrecordData[offset])}));
				dumpHex.append(" " + subrecordData[offset]);
				if (subrecordData[offset] >= 32 && subrecordData[offset] < 127)
					dumpLine.append(new String(subrecordData, offset, 1));
				else
					dumpLine.append(".");
			}

			for (; dumpHex.length() < 48; dumpHex.append("   "))
				;
			for (; dumpLine.length() < 16; dumpLine.append(" "))
				;
			dumpData.append("" + i + ":");
			dumpData.append(dumpHex);
			dumpData.append("  *");
			dumpData.append(dumpLine);
			dumpData.append("*");
			if (i + 16 < subrecordData.length)
				dumpData.append("\n");
			dumpHex.delete(0, 48);
			dumpLine.delete(0, 16);
		}

		return dumpData.toString();

	}

}
