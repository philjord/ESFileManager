package esfilemanager.loader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

import com.frostwire.util.SparseArray;

import esfilemanager.common.PluginException;
import esfilemanager.common.data.plugin.PluginGroup;
import tools.io.ESMByteConvert;
import tools.io.FileChannelRAF;

public class InteriorCELLSubblock extends PluginGroup {

	private long							fileOffset;
	private int								length;
	public int								secondLastDigit;

	private SparseArray<FormToFilePointer>	CELLByFormID	= null;

	public InteriorCELLSubblock(byte[] prefix, long fileOffset, int length) {
		super(prefix);
		this.fileOffset = fileOffset;
		this.length = length;

		secondLastDigit = ESMByteConvert.extractInt(groupLabel, 0);
	}

	public FormToFilePointer getInteriorCELL(int cellId, FileChannelRAF in) throws IOException, PluginException {
		synchronized(this ) {
			if (CELLByFormID == null)
				loadAndIndex(in);
		}

		return CELLByFormID.get(cellId);
	}

	public void getAllInteriorCELLFormIds(ArrayList<FormToFilePointer> ret, FileChannelRAF in)
			throws IOException, PluginException {
		synchronized(this ) {
			if (CELLByFormID == null)
				loadAndIndex(in);
		}

		for (int i = 0; i < CELLByFormID.size(); i++)
			ret.add(CELLByFormID.get(CELLByFormID.keyAt(i)));
	}

	public void loadAndIndex(FileChannelRAF in) throws IOException, PluginException {
		CELLByFormID = new SparseArray<FormToFilePointer>();
		
		filePositionPointer = fileOffset;
		long pos = filePositionPointer;
		FileChannel ch = in.getChannel();
		int dataLength = length;
		byte[] prefix = new byte[headerByteCount];
		ByteBuffer pbb = ByteBuffer.wrap(prefix); //reused to avoid allocation of object, all bytes of array are refilled or error thrown
		
		FormToFilePointer formToFilePointer = null;

		while (dataLength >= headerByteCount) {
			long filePositionPointer = pos;// record start before header read so we can record a simple index

			int count = ch.read((ByteBuffer)pbb.rewind(), pos);	
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
					System.out.println("Group Type " + subGroupType + " not allowed as child of Int CELL sub block group");
				}
			} else if (type.equals("CELL")) {
				int formID = ESMByteConvert.extractInt3(prefix, 12);
				formToFilePointer = new FormToFilePointer(formID, filePositionPointer);
				CELLByFormID.put(formID, formToFilePointer);
			} else {
				System.out.println("What the hell is a type " + type + " doing in the Int CELL sub block group?");
			}
			
			pos += length;
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
