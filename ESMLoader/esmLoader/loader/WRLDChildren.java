package esmLoader.loader;

import java.awt.Point;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.DataFormatException;

import tools.io.ESMByteConvert;
import esmLoader.common.PluginException;
import esmLoader.common.data.plugin.PluginGroup;
import esmLoader.common.data.plugin.PluginRecord;

public class WRLDChildren extends PluginGroup
{
	// x and y is by the WRLD area and is used by several wrlds again (eg tamriel and anvil)
	public Map<Point, CELLPointer> WRLDExtBlockCELLByXY = new LinkedHashMap<Point, CELLPointer>();

	// should be a single road
	private PluginRecord road;

	// should be a single cell
	private PluginRecord wrldCell;

	private PluginGroup wrldCellChildren;

	public WRLDChildren(byte[] prefix)
	{
		super(prefix);
	}

	public void loadAndIndex(String fileName, RandomAccessFile in, int groupLength, Map<Integer, CELLPointer> WRLDExtBlockCELLByFormId)
			throws IOException, DataFormatException, PluginException
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

				if (subGroupType == PluginGroup.EXTERIOR_BLOCK)
				{
					WRLDExtBlock children = new WRLDExtBlock(prefix);
					children.loadAndIndex(fileName, in, length, WRLDExtBlockCELLByFormId, WRLDExtBlockCELLByXY);
				}
				else if (subGroupType == PluginGroup.CELL)
				{
					wrldCellChildren = new PluginGroup(prefix);
					wrldCellChildren.load(fileName, in, length);
				}
				else
				{
					System.out.println("Group Type " + subGroupType + " not allowed as child of WRLD children group");
				}
			}
			else if (type.equals("ROAD"))
			{
				PluginRecord record = new PluginRecord(prefix);
				record.load(fileName, in, length);
				road = record;
			}
			else if (type.equals("CELL"))
			{
				PluginRecord record = new PluginRecord(prefix);
				record.load(fileName, in, length);
				wrldCell = record;
			}
			else
			{
				System.out.println("What the hell is a type " + type + " doing in the WRLD children group?");
			}

			//prep for next iter;
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

	public PluginRecord getRoad()
	{
		return road;
	}

	public PluginRecord getCell()
	{
		return wrldCell;
	}

	public PluginGroup getCellChildren()
	{
		return wrldCellChildren;
	}

}
