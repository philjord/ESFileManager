package esfilemanager.loader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;

import esfilemanager.Point;
import esfilemanager.common.PluginException;
import esfilemanager.common.data.plugin.PluginGroup;
import esfilemanager.common.data.plugin.PluginRecord;
import esfilemanager.common.data.record.Subrecord;
import tools.io.ESMByteConvert;
import tools.io.FileChannelRAF;

public class WRLDExtSubblock extends PluginGroup {
	public long							fileOffset;
	public int							length;
	public int							x;
	public int							y;

	private Map<Point, FormToFilePointer>	CELLByXY	= null;

	public WRLDExtSubblock(byte[] prefix, long fileOffset, int length) {
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

	public FormToFilePointer getWRLDExtBlockCELLByXY(Point point, FileChannelRAF in)
			throws IOException, DataFormatException, PluginException {
		// must hold everyone up so a single thread does the load if needed, otherwise we'll create to hashmaps and ruin everything
		synchronized (this) {
			if (CELLByXY == null)
				loadAndIndex(in);
		}

		return CELLByXY.get(point);
	}

	private void loadAndIndex(FileChannelRAF in) throws IOException, DataFormatException, PluginException {
		CELLByXY = new HashMap<Point, FormToFilePointer>();
				
		filePositionPointer = fileOffset;
		long pos = filePositionPointer;
		FileChannel ch = in.getChannel();
		int dataLength = length;
		byte[] prefix = new byte[headerByteCount];

		FormToFilePointer formToFilePointer = null;

		while (dataLength >= headerByteCount) {
			long filePositionPointer = pos;// record start before header read so we can record a simple index
			
			int count = ch.read(ByteBuffer.wrap(prefix), pos);	
			pos += headerByteCount;
			if (count != headerByteCount)
				throw new PluginException("Record prefix is incomplete");
			

			dataLength -= headerByteCount;
			String type = new String(prefix, 0, 4);
			int length = ESMByteConvert.extractInt(prefix, 4);

			if (type.equals("GRUP")) {
				length -= headerByteCount;
				int subGroupType = prefix [12] & 0xff;

				if (subGroupType == PluginGroup.CELL) {
					formToFilePointer.cellChildrenFilePointer = filePositionPointer;// note not current pos but pos with header
					// don't load, pos moved forward below
				} else {
					System.out.println("Group Type " + subGroupType + " not allowed as child of WRLD ext sub block group");
				}
			} else if (type.equals("CELL")) {
				int formID = ESMByteConvert.extractInt3(prefix, 12);

				formToFilePointer = new FormToFilePointer(formID, filePositionPointer);// note not current pos but pos with header

				PluginRecord rec = new PluginRecord(prefix);
				rec.load(in, pos);
				// find the x and y
				List<Subrecord> subrecords = rec.getSubrecords();
				int x = 0;
				int y = 0;
				for (int i = 0; i < subrecords.size(); i++) {
					Subrecord sr = subrecords.get(i);
					byte[] bs = sr.getSubrecordData();

					if (sr.getSubrecordType().equals("XCLC")) {
						x = ESMByteConvert.extractInt(bs, 0);
						y = ESMByteConvert.extractInt(bs, 4);
					}
				}
				//we do index now
				CELLByXY.put(new Point(x, y), formToFilePointer);
			} else {
				System.out.println("What the hell is a type " + type + " doing in the WRLD ext sub block group?");
			}

			pos += length;
			//prep for next iter
			dataLength -= length;
		}

		if (dataLength != 0) {
			if (getGroupType() == 0)
				throw new PluginException("Group " + getGroupRecordType() + " is incomplete");
			else
				throw new PluginException("Subgroup type " + getGroupType() + " is incomplete");
		}
	}
}
