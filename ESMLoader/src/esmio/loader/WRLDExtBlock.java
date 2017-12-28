package esmio.loader;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.zip.DataFormatException;

import esmio.Point;
import esmio.common.PluginException;
import esmio.common.data.plugin.PluginGroup;
import tools.io.ESMByteConvert;

public class WRLDExtBlock extends PluginGroup
{
	private long fileOffset;
	private int length;
	public int x;
	public int y;

	private HashMap<Point, WRLDExtSubblock> WRLDExtSubblockChildren = null;

	public WRLDExtBlock(byte[] prefix, long fileOffset, int length)
	{
		super(prefix);
		this.fileOffset = fileOffset;
		this.length = length;

		int intValue = ESMByteConvert.extractInt(groupLabel, 0);
		x = intValue >>> 16;
		if ((x & 0x8000) != 0)
			x |= 0xffff0000;
		y = intValue & 0xffff;
		if ((y & 0x8000) != 0)
			y |= 0xffff0000;
	}

	public CELLDIALPointer getWRLDExtBlockCELLByXY(Point point, RandomAccessFile in)
			throws IOException, DataFormatException, PluginException
	{
		// must hold everyone up so a single thread does the load if needed
		synchronized (this)
		{
			if (WRLDExtSubblockChildren == null)
				loadAndIndex(in);
		}

		//pointy point here
		Point extSubPoint = new Point((int) Math.floor(point.x / 8f), (int) Math.floor(point.y / 8f));
		WRLDExtSubblock wrldExtSubblock = WRLDExtSubblockChildren.get(extSubPoint);
		if (wrldExtSubblock != null)
		{
			return wrldExtSubblock.getWRLDExtBlockCELLByXY(point, in);
		}
		else
		{
			return null;
		}
	}

	private void loadAndIndex(RandomAccessFile in) throws IOException, DataFormatException, PluginException
	{
		WRLDExtSubblockChildren = new HashMap<Point, WRLDExtSubblock>();
		synchronized (in)
		{
			in.seek(fileOffset);
			int dataLength = length;
			byte prefix[] = new byte[headerByteCount];

			while (dataLength >= headerByteCount)
			{
				int count = in.read(prefix);
				if (count != headerByteCount)
					throw new PluginException("Record prefix is incomplete");
				dataLength -= headerByteCount;
				String type = new String(prefix, 0, 4);
				int length = ESMByteConvert.extractInt(prefix, 4);

				if (type.equals("GRUP"))
				{
					length -= headerByteCount;
					int subGroupType = prefix[12] & 0xff;

					if (subGroupType == PluginGroup.EXTERIOR_SUBBLOCK)
					{
						WRLDExtSubblock wrldExtSubblock = new WRLDExtSubblock(prefix, in.getFilePointer(), length);
						WRLDExtSubblockChildren.put(new Point(wrldExtSubblock.x, wrldExtSubblock.y), wrldExtSubblock);
						//we DO NOT index now, later
						in.skipBytes(length);
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
					throw new PluginException("Group " + getGroupRecordType() + " is incomplete");
				else
					throw new PluginException("Subgroup type " + getGroupType() + " is incomplete");
			}
		}

	}
}
