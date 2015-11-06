package esmmanager.loader;

import java.awt.Point;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;

import tools.io.ESMByteConvert;
import esmmanager.common.PluginException;
import esmmanager.common.data.plugin.PluginGroup;
import esmmanager.common.data.plugin.PluginRecord;
import esmmanager.common.data.plugin.PluginSubrecord;

public class WRLDExtSubblock extends PluginGroup
{
	public WRLDExtSubblock(byte[] prefix)
	{
		super(prefix);
	}

	public void loadAndIndex(String fileName, RandomAccessFile in, int groupLength, Map<Integer, CELLPointer> WRLDExtBlockCELLByFormId,
			Map<Point, CELLPointer> WRLDExtBlockCELLByXY) throws IOException, DataFormatException, PluginException
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
				int gt = prefix[12] & 0xff;

				if (gt == PluginGroup.CELL)
				{
					cellPointer.cellChildrenFilePointer = filePositionPointer;

					// now skip the group
					in.skipBytes(length);
				}
				else
				{
					System.out.println("Group Type " + gt + " not allowed as child of WRLD ext sub block group");
				}
			}
			else if (type.equals("CELL"))
			{
				int formID = ESMByteConvert.extractInt(prefix, 12);

				cellPointer = new CELLPointer(formID, filePositionPointer);

				WRLDExtBlockCELLByFormId.put(new Integer(formID), cellPointer);

				PluginRecord rec = new PluginRecord(prefix);
				rec.load(fileName, in, length);
				// find the x and y
				List<PluginSubrecord> subrecords = rec.getSubrecords();
				int x = 0;
				int y = 0;
				for (int i = 0; i < subrecords.size(); i++)
				{
					PluginSubrecord sr = subrecords.get(i);
					byte[] bs = sr.getSubrecordData();

					if (sr.getSubrecordType().equals("XCLC"))
					{
						x = ESMByteConvert.extractInt(bs, 0);
						y = ESMByteConvert.extractInt(bs, 4);
					}
				}
				Point p = new Point(x, y);

				WRLDExtBlockCELLByXY.put(p, cellPointer);
			}
			else
			{
				System.out.println("What the hell is a type " + type + " doing in the WRLD ext sub block group?");
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
