package esmio.loader;

import java.io.IOException;
import java.util.HashMap;
import java.util.zip.DataFormatException;

import esmio.Point;
import esmio.common.PluginException;
import esmio.common.data.plugin.PluginGroup;
import esmio.common.data.plugin.PluginRecord;
import tools.io.ESMByteConvert;
import tools.io.FileChannelRAF;

public class WRLDChildren extends PluginGroup {
	// x and y is by the WRLD area and is used by several wrlds again (eg tamriel and anvil)
	// note extblock and extsubblock may not be in order
	// extblock 0,0 has extsubblock 0-3 by 0-3 that is 16 sub blocks
	// 0,1 has 0-3,4-7   so extsubblock 04 (in extblock 01) has
	// many cells with XCLC values of 7,39-0,39 then 7,38-0,38 to the last at 0,32 8x8 in all, (in order possibly)
	// not all cell have to be there
	// each cell is followed by it's group of temp children

	//so incoming value of say 7,41 = extsub of x7/4*8 = 1 and y41/4*8 = 10 so sub of x1/8 = 0 and y10/8 = 2

	private FileChannelRAF					in;

	// should be a single road
	private PluginRecord					road;

	// should be a single cell
	private PluginRecord					wrldCell;

	private PluginGroup						wrldCellChildren;

	private HashMap<Point, WRLDExtBlock>	WRLDExtBlockChildren	= new HashMap<Point, WRLDExtBlock>();

	public WRLDChildren(byte[] prefix) {
		super(prefix);
	}

	public CELLDIALPointer getWRLDExtBlockCELLByXY(Point point) {
		// notice children already loaded at construct time
		// notice Exception catching here
		try {
			//pointy point here
			Point extPoint = new Point((int)Math.floor(point.x / 32f), (int)Math.floor(point.y / 32f));
			WRLDExtBlock wrldExtBlock = WRLDExtBlockChildren.get(extPoint);
			if (wrldExtBlock != null)
				return wrldExtBlock.getWRLDExtBlockCELLByXY(point, in);
		} catch (PluginException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (DataFormatException e) {
			e.printStackTrace();
		}

		return null;
	}

	public void loadAndIndex(FileChannelRAF in, int groupLength)
			throws IOException, DataFormatException, PluginException {
		this.in = in;

		int dataLength = groupLength;
		byte prefix[] = new byte[headerByteCount];
		while (dataLength >= headerByteCount) {
			int count = in.read(prefix);
			if (count != headerByteCount)
				throw new PluginException("Record prefix is incomplete");
			dataLength -= headerByteCount;
			String type = new String(prefix, 0, 4);
			int length = ESMByteConvert.extractInt(prefix, 4);

			if (type.equals("GRUP")) {
				length -= headerByteCount;

				int subGroupType = prefix [12] & 0xff;

				if (subGroupType == PluginGroup.EXTERIOR_BLOCK) {
					WRLDExtBlock wrldExtBlock = new WRLDExtBlock(prefix, in.getFilePointer(), length);
					WRLDExtBlockChildren.put(new Point(wrldExtBlock.x, wrldExtBlock.y), wrldExtBlock);
					//we DO NOT index now, later
					in.skipBytes(length);
				} else if (subGroupType == PluginGroup.CELL) {
					wrldCellChildren = new PluginGroup(prefix);
					wrldCellChildren.load("", in, length);
				} else {
					System.out.println("Group Type " + subGroupType + " not allowed as child of WRLD children group");
				}
			} else if (type.equals("ROAD")) {
				PluginRecord record = new PluginRecord(prefix);
				record.load("", in, length);
				road = record;
			} else if (type.equals("CELL")) {
				PluginRecord record = new PluginRecord(prefix);
				record.load("", in, length);
				wrldCell = record;
			} else {
				System.out.println("What the hell is a type " + type + " doing in the WRLD children group?");
			}

			//prep for next iter;
			dataLength -= length;
		}

		if (dataLength != 0) {
			if (getGroupType() == 0)
				throw new PluginException("Group " + getGroupRecordType() + " is incomplete");
			else
				throw new PluginException(" Subgroup type " + getGroupType() + " is incomplete");
		}

	}

	public PluginRecord getRoad() {
		return road;
	}

	public PluginRecord getCell() {
		return wrldCell;
	}

	public PluginGroup getCellChildren() {
		return wrldCellChildren;
	}

}
