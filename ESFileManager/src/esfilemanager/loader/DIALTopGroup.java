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

public class DIALTopGroup extends PluginGroup {
	private SparseArray<FormToFilePointer> DIALByFormID = null;

	public DIALTopGroup(byte[] prefix) {
		super(prefix);
	}

	public FormToFilePointer getDIAL(int dialId) throws IOException, PluginException {
		return DIALByFormID.get(dialId);
	}

	public void getAllInteriorCELLFormIds(ArrayList<FormToFilePointer> ret) throws IOException, PluginException {
		for (int i = 0; i < DIALByFormID.size(); i++)
			ret.add(DIALByFormID.get(DIALByFormID.keyAt(i)));
	}
	
	public void loadAndIndex(String fileName, FileChannelRAF in, long pos, int groupLength) throws IOException, PluginException {
		
		FileChannel ch = in.getChannel();
		
		DIALByFormID = new SparseArray<FormToFilePointer>();
		int dataLength = groupLength;
		byte prefix[] = new byte[headerByteCount];

		FormToFilePointer formToFilePointer = null;

		while (dataLength >= headerByteCount) {
			long filePositionPointer = pos; // grab so it can be used for the pointer below

			int count = ch.read(ByteBuffer.wrap(prefix), pos);			
			if (count != headerByteCount)
				throw new PluginException("File " + fileName + ": Record prefix is incomplete");
			
			pos += prefix.length;
			dataLength -= headerByteCount;
			String type = new String(prefix, 0, 4);
			int length = ESMByteConvert.extractInt(prefix, 4);

			if (type.equals("GRUP")) {
				length -= headerByteCount;
				int subGroupType = prefix [12] & 0xff;

				if (subGroupType == PluginGroup.TOPIC) {
					formToFilePointer.cellChildrenFilePointer = filePositionPointer; // not pos, but the pointer including the header
					// now skip the group
					pos += length;
				} else {
					System.out.println("File " + fileName + ": Group Type " + subGroupType + " not allowed as child of DIAL group");
				}
			} else if (type.equals("DIAL")) {
				int formID = ESMByteConvert.extractInt3(prefix, 12);
				formToFilePointer = new FormToFilePointer(formID, filePositionPointer);
				DIALByFormID.put(formID, formToFilePointer);
				pos += length;
			} else {
				System.out.println("File " + fileName + ": What the hell is a type " + type + " doing in the Int CELL sub block group?");
			}

			//prep for next iter
			dataLength -= length;
		}

		if (dataLength != 0) {
			if (getGroupType() == 0)
				throw new PluginException("File " + fileName + ": Group " + getGroupRecordType() + " is incomplete");
			else
				throw new PluginException("File " + fileName + ": Subgroup type " + getGroupType() + " is incomplete");
		}

	}

}
