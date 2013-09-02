package esmLoader.loader;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.LinkedHashMap;
import java.util.Map;

import utils.ESMByteConvert;

import esmLoader.common.PluginException;
import esmLoader.common.data.plugin.PluginGroup;

public class InteriorCELLTopGroup extends PluginGroup
{
	public Map<Integer, CELLPointer> interiorCELLByFormId = new LinkedHashMap<Integer, CELLPointer>();

	public InteriorCELLTopGroup(byte[] prefix)
	{
		super(prefix);
	}

	public void loadAndIndex(String fileName, RandomAccessFile in, int groupLength) throws IOException, PluginException
	{
		int dataLength = groupLength;
		byte prefix[] = new byte[headerByteCount];

		while (dataLength >= headerByteCount)
		{
			int count = in.read(prefix);
			if (count != headerByteCount)
				throw new PluginException(fileName + ": Record prefix is incomplete");

			dataLength -= headerByteCount;
			String type = new String(prefix, 0, 4);
			int length = ESMByteConvert.extractInt(prefix, 4);

			if (type.equals("GRUP"))
			{
				length -= headerByteCount;
				int prefixGroupType = prefix[12] & 0xff;

				if (prefixGroupType == PluginGroup.INTERIOR_BLOCK)
				{
					InteriorCELLBlock children = new InteriorCELLBlock(prefix);
					children.loadAndIndex(fileName, in, length, interiorCELLByFormId);
				}
				else
				{
					System.out.println("Group Type " + prefixGroupType + " not allowed as child of Int CELL");
				}
			}
			else
			{
				System.out.println("what the hell is a type " + type + " doing in the Int CELL group?");
			}

			// now take teh length of the dataLength ready for the next iteration
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
