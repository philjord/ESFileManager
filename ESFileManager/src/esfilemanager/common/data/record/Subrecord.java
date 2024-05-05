package esfilemanager.common.data.record;

public class Subrecord
{
	protected String subrecordType;

	protected byte subrecordData[];

	public Subrecord()
	{
		
	}
	
	public Subrecord(String subrecordType, byte subrecordData[])
	{
		// memory saving mechanism  https://www.baeldung.com/java-string-pool
		this.subrecordType = subrecordType.intern();
		this.subrecordData = subrecordData;
		
		
		
		// some random seeming chars, swap first non printable char with a number or _ 
		// Note only the last many subs of IMAD image space modifiers
		if (subrecordType.endsWith("IAD")) {
			this.subrecordType = "" + "_" + subrecordType.substring(1);
			//if(subrecordType.charAt(0) < 0x20 || subrecordType.charAt(0) >= 0x80) {
				//int asciiCode = (byte)subrecordType.charAt(0);
				//this.subrecordType = "" + asciiCode + subrecordType.substring(1);
			//}
		}
		
		//WTHR in FO4 at least
		if (subrecordType.endsWith("0TX")) {
			this.subrecordType = "" + "_" + subrecordType.substring(1);
			//if(subrecordType.charAt(0) < 0x20 || subrecordType.charAt(0) >= 0x80) {
				//int asciiCode = (byte)subrecordType.charAt(0);
				//this.subrecordType = "" + asciiCode + subrecordType.substring(1);
			//}
		}
		 
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

	@Override
	public String toString()
	{
		return subrecordType + " subrecord";
	}
}
