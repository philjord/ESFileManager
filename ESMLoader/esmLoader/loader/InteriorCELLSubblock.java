package esmLoader.loader;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Map;

import utils.ESMByteConvert;

import esmLoader.common.PluginException;
import esmLoader.common.data.plugin.PluginGroup;

public class InteriorCELLSubblock extends PluginGroup
{
	public InteriorCELLSubblock(byte[] prefix)
	{
		super(prefix);
	}

	public void loadAndIndex(String fileName, RandomAccessFile in, int groupLength, Map<Integer, CELLPointer> interiorCELLByFormId)
			throws IOException, PluginException
	{
		int dataLength = groupLength;
		byte prefix[] = new byte[headerByteCount];

		CELLPointer cellPointer = null;

		while (dataLength >= headerByteCount)
		{
			long filePositionPointer = in.getFilePointer();

			int count = in.read(prefix);
			if (count != headerByteCount)
				throw new PluginException(fileName + ": Record prefix is incomplete");
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
				interiorCELLByFormId.put(new Integer(formID), cellPointer);
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
				throw new PluginException(fileName + ": Group " + getGroupRecordType() + " is incomplete");
			else
				throw new PluginException(fileName + ": Subgroup type " + getGroupType() + " is incomplete");
		}

	}
}
