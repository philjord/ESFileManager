package esmLoader.loader;

import java.awt.Point;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Map;
import java.util.zip.DataFormatException;

import utils.ESMByteConvert;

import esmLoader.common.PluginException;
import esmLoader.common.data.plugin.PluginGroup;

public class WRLDExtBlock extends PluginGroup
{
	public WRLDExtBlock(byte[] prefix)
	{
		super(prefix);
	}

	public void loadAndIndex(String fileName, RandomAccessFile in, int groupLength, Map<Integer, CELLPointer> WRLDExtBlockCELLByFormId,
			Map<Point, CELLPointer> WRLDExtBlockCELLByXY) throws IOException, DataFormatException, PluginException
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
				int subGroupType = prefix[12] & 0xff;

				if (subGroupType == PluginGroup.EXTERIOR_SUBBLOCK)
				{
					WRLDExtSubblock children = new WRLDExtSubblock(prefix);
					children.loadAndIndex(fileName, in, length, WRLDExtBlockCELLByFormId, WRLDExtBlockCELLByXY);
				}
				else
				{
					System.out.println("Group Type " + subGroupType + " not allowed as child of WRLD ext block group");
				}
			}
			else
			{
				System.out.println("What the hell is a type " + type + " doing in the WRLD ext block group?");
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
