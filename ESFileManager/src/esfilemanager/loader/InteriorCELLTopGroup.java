package esfilemanager.loader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

import esfilemanager.common.PluginException;
import esfilemanager.common.data.plugin.PluginGroup;
import tools.io.ESMByteConvert;
import tools.io.FileChannelRAF;

public class InteriorCELLTopGroup extends PluginGroup {
	//Interior Cells are index in a block and sub block system the block Id tells you the final digits of teh cells in it
	// the sub block tell you the pent ultimate digit (there are exactly 10 at each level)
	// e.g. block 4 sub 3 == *34 so 99634 is there and next to 99634 is it's children, with 2 groups 
	// persistent and temporary
	// note if you load the 99634 children group, both persistent and temp will be loaded
	// which is very expensive as persistent need to be loaded up front but temp only at real load time
	// so finding and loading persists goes through a separate system

	private FileChannelRAF		in;
	private InteriorCELLBlock[]	interiorCELLBlocks	= new InteriorCELLBlock[10];

	//public Map<Integer, CELLPointer> interiorCELLByFormId = new LinkedHashMap<Integer, CELLPointer>();

	public InteriorCELLTopGroup(byte[] prefix) {
		super(prefix);

	}

	public FormToFilePointer getInteriorCELL(int cellId) {
		// notice Exception catching here
		try {
			int lastDigit = cellId % 10;
			InteriorCELLBlock interiorCELLBlock = interiorCELLBlocks [lastDigit];
			//Note we can be asked for a cell that does not exist in this ESM/ESP file
			if(interiorCELLBlock != null)
				return interiorCELLBlock.getInteriorCELL(cellId, in);

		} catch (PluginException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	public ArrayList<FormToFilePointer> getAllInteriorCELLFormIds() {
		try {
			ArrayList<FormToFilePointer> ret = new ArrayList<FormToFilePointer>();
			for (InteriorCELLBlock interiorCELLBlock : interiorCELLBlocks) {
				if(interiorCELLBlock != null)
					interiorCELLBlock.getAllInteriorCELLFormIds(ret, in);
			}

			return ret;
		} catch (PluginException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	public void loadAndIndex(String fileName, FileChannelRAF in, int groupLength) throws IOException, PluginException {

		this.in = in;
		int dataLength = groupLength;
		byte prefix[] = new byte[headerByteCount];

		while (dataLength >= headerByteCount) {
			int count = in.read(prefix);
			if (count != headerByteCount)
				throw new PluginException(fileName + ": Record prefix is incomplete");

			dataLength -= headerByteCount;
			String type = new String(prefix, 0, 4);
			int length = ESMByteConvert.extractInt(prefix, 4);

			if (type.equals("GRUP")) {
				length -= headerByteCount;
				int prefixGroupType = prefix [12] & 0xff;

				if (prefixGroupType == PluginGroup.INTERIOR_BLOCK) {
					InteriorCELLBlock cellBlock = new InteriorCELLBlock(prefix, in.getFilePointer(), length);
					interiorCELLBlocks [cellBlock.lastDigit] = cellBlock;

					//children.loadAndIndex(fileName, in, length, interiorCELLByFormId);
					in.skipBytes(length);

				} else {
					System.out.println("Group Type " + prefixGroupType + " not allowed as child of Int CELL");
				}
			} else {
				System.out.println("what the hell is a type " + type + " doing in the Int CELL group?");
			}

			// now take the length of the dataLength ready for the next iteration
			dataLength -= length;
		}

		if (dataLength != 0) {
			if (getGroupType() == 0)
				throw new PluginException(fileName + ": Group " + getGroupRecordType() + " is incomplete");
			else
				throw new PluginException(fileName + ": Subgroup type " + getGroupType() + " is incomplete");
		}
	}
	
	
	public void loadAndIndexch(String fileName, FileChannelRAF in, long pos, int groupLength) throws IOException, PluginException {

		this.in = in;
		FileChannel ch = in.getChannel();
		
		int dataLength = groupLength;
		byte prefix[] = new byte[headerByteCount];

		while (dataLength >= headerByteCount) {
			int count = ch.read(ByteBuffer.wrap(prefix), pos);			
			if (count != headerByteCount)
				throw new PluginException("Record prefix is incomplete");
			
			pos += headerByteCount;			
			dataLength -= headerByteCount;
			String type = new String(prefix, 0, 4);
			int length = ESMByteConvert.extractInt(prefix, 4);

			if (type.equals("GRUP")) {
				length -= headerByteCount;
				int prefixGroupType = prefix [12] & 0xff;

				if (prefixGroupType == PluginGroup.INTERIOR_BLOCK) {
					InteriorCELLBlock cellBlock = new InteriorCELLBlock(prefix, pos, length);
					interiorCELLBlocks [cellBlock.lastDigit] = cellBlock;

					//children.loadAndIndex(fileName, in, length, interiorCELLByFormId);
					pos += length;	
				} else {
					System.out.println("Group Type " + prefixGroupType + " not allowed as child of Int CELL");
				}
			} else {
				System.out.println("what the hell is a type " + type + " doing in the Int CELL group?");
			}

			// now take the length of the dataLength ready for the next iteration
			dataLength -= length;
		}

		if (dataLength != 0) {
			if (getGroupType() == 0)
				throw new PluginException(fileName + ": Group " + getGroupRecordType() + " is incomplete");
			else
				throw new PluginException(fileName + ": Subgroup type " + getGroupType() + " is incomplete");
		}
	}

}
