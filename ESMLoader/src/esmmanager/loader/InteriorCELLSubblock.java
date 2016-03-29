package esmmanager.loader;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import esmmanager.common.PluginException;
import esmmanager.common.data.plugin.PluginGroup;
import tools.io.ESMByteConvert;

public class InteriorCELLSubblock extends PluginGroup
{

	private long fileOffset;
	private int length;
	public int secondLastDigit;

	private Map<Integer, CELLPointer> CELLByFormID = null;

	public InteriorCELLSubblock(byte[] prefix, long fileOffset, int length)
	{
		super(prefix);
		this.fileOffset = fileOffset;
		this.length = length;

		secondLastDigit = ESMByteConvert.extractInt(groupLabel, 0);
	}

	public CELLPointer getInteriorCELL(int cellId, RandomAccessFile in) throws IOException, PluginException
	{
		if (CELLByFormID == null)
			loadAndIndex(in);

		return CELLByFormID.get(cellId);
	}

	public void getAllInteriorCELLFormIds(ArrayList<CELLPointer> ret, RandomAccessFile in) throws IOException, PluginException
	{
		if (CELLByFormID == null)
			loadAndIndex(in);

		ret.addAll(CELLByFormID.values());
	}

	public void loadAndIndex(RandomAccessFile in) throws IOException, PluginException
	{
		CELLByFormID = new HashMap<Integer, CELLPointer>();
		in.seek(fileOffset);
		int dataLength = length;
		byte prefix[] = new byte[headerByteCount];

		CELLPointer cellPointer = null;

		while (dataLength >= headerByteCount)
		{
			long filePositionPointer = in.getFilePointer();

			int count = in.read(prefix);
			if (count != headerByteCount)
				throw new PluginException(": Record prefix is incomplete");
			dataLength -= headerByteCount;
			String type = new String(prefix, 0, 4);
			int length = ESMByteConvert.extractInt(prefix, 4);

			if (type.equals("GRUP"))
			{
				length -= headerByteCount;
				int subGroupType = prefix[12] & 0xff;

				if (subGroupType == PluginGroup.CELL)
				{
					cellPointer.cellChildrenFilePointer = filePositionPointer;

					// now skip the group
					in.skipBytes(length);
				}
				else
				{
					System.out.println("Group Type " + subGroupType + " not allowed as child of Int CELL sub block group");
				}
			}
			else if (type.equals("CELL"))
			{
				int formID = ESMByteConvert.extractInt(prefix, 12);
				cellPointer = new CELLPointer(formID, filePositionPointer);
				CELLByFormID.put(new Integer(formID), cellPointer);
				in.skipBytes(length);
			}
			else
			{
				System.out.println("What the hell is a type " + type + " doing in the Int CELL sub block group?");
			}

			//prep for next iter
			dataLength -= length;
		}

		if (dataLength != 0)
		{
			if (getGroupType() == 0)
				throw new PluginException(": Group " + getGroupRecordType() + " is incomplete");
			else
				throw new PluginException(": Subgroup type " + getGroupType() + " is incomplete");
		}

	}

}
