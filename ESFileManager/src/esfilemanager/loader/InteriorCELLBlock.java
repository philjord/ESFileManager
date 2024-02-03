package esfilemanager.loader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

import esfilemanager.common.PluginException;
import esfilemanager.common.data.plugin.PluginGroup;
import tools.io.ESMByteConvert;
import tools.io.FileChannelRAF;

public class InteriorCELLBlock extends PluginGroup {
	private long					fileOffset;
	private int						length;
	public int						lastDigit;

	private InteriorCELLSubblock[]	interiorCELLSubblocks	= null;

	public InteriorCELLBlock(byte[] prefix, long fileOffset, int length) {
		super(prefix);
		this.fileOffset = fileOffset;
		this.length = length;

		lastDigit = ESMByteConvert.extractInt(groupLabel, 0);
	}

	public FormToFilePointer getInteriorCELL(int cellId, FileChannelRAF in) throws IOException, PluginException {
		synchronized(this) {
			if (interiorCELLSubblocks == null)
				loadAndIndex(in);
		}
		int secondLastDigit = (cellId / 10) % 10;
		InteriorCELLSubblock interiorCELLSubblock = interiorCELLSubblocks [secondLastDigit];
		//Note we can be asked for a cell that does not exist in this ESM/ESP file
		if (interiorCELLSubblock != null) {
			return interiorCELLSubblock.getInteriorCELL(cellId, in);
		}

		return null;
	}

	public void getAllInteriorCELLFormIds(ArrayList<FormToFilePointer> ret, FileChannelRAF in)
			throws IOException, PluginException {
		synchronized(this) {
			if (interiorCELLSubblocks == null)
				loadAndIndex(in);
		}
		for (InteriorCELLSubblock interiorCELLSubblock : interiorCELLSubblocks) {
			if(interiorCELLSubblock != null)  
				interiorCELLSubblock.getAllInteriorCELLFormIds(ret, in);
		}
	}

	public void loadAndIndex(FileChannelRAF in) throws IOException, PluginException {
		interiorCELLSubblocks = new InteriorCELLSubblock[10];
		
		
		filePositionPointer = fileOffset;
		long pos = filePositionPointer;
		FileChannel ch = in.getChannel();
		int dataLength = length;
		byte[] prefix = new byte[headerByteCount];
		
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
				int subGroupType = prefix [12] & 0xff;

				if (subGroupType == PluginGroup.INTERIOR_SUBBLOCK) {
					InteriorCELLSubblock subblock = new InteriorCELLSubblock(prefix, pos, length);
					interiorCELLSubblocks [subblock.secondLastDigit] = subblock;

					//children.loadAndIndex(fileName, in, length, interiorCELLByFormId);
					pos += length;
				} else {
					System.out.println("Group Type " + subGroupType + " not allowed as child of Int CELL block group");
				}
			} else {
				System.out.println("What the hell is a type " + type + " doing in the Int CELL block group?");
			}

			//prep for next iter
			dataLength -= length;
		}

		if (dataLength != 0) {
			if (getGroupType() == 0)
				throw new PluginException(": Group " + getGroupRecordType() + " is incomplete");
			else
				throw new PluginException(": Subgroup type " + getGroupType() + " is incomplete");
		}

	}

}
