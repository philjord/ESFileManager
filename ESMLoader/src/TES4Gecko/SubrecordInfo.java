package TES4Gecko;

public class SubrecordInfo
{
	private String subrecordType;

	private String[] recordTypes;

	private int[] referenceOffsets;

	private static final String[] allRecordTypes = new String[0];

	public SubrecordInfo(String subrecordType, int[] referenceOffsets, String[] recordTypes)
	{
		this.subrecordType = subrecordType;
		this.referenceOffsets = referenceOffsets;
		this.recordTypes = new String[recordTypes.length];
		for (int i = 0; i < recordTypes.length; i++)
			this.recordTypes[i] = recordTypes[i];
	}

	public SubrecordInfo(String subrecordType, int[] referenceOffsets)
	{
		this.subrecordType = subrecordType;
		this.referenceOffsets = referenceOffsets;
		this.recordTypes = allRecordTypes;
	}

	public String getSubrecordType()
	{
		return this.subrecordType;
	}

	public String[] getRecordTypes()
	{
		return this.recordTypes;
	}

	public int[] getReferenceOffsets()
	{
		return this.referenceOffsets;
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.SubrecordInfo
 * JD-Core Version:    0.6.0
 */