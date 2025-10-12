package esfilemanager.loader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.zip.DataFormatException;

import com.frostwire.util.SparseArray;

import esfilemanager.Point;
import esfilemanager.common.PluginException;
import esfilemanager.common.data.plugin.PluginGroup;
import esfilemanager.common.data.plugin.PluginRecord;
import tools.io.ESMByteConvert;
import tools.io.FileChannelRAF;

public class WRLDTopGroup extends PluginGroup {
	//top level WRLD records
	public SparseArray<PluginRecord>	WRLDByFormId			= new SparseArray<PluginRecord>();

	public SparseArray<WRLDChildren>	WRLDChildrenByFormId	= new SparseArray<WRLDChildren>();

	public WRLDTopGroup(byte[] prefix) {
		super(prefix);
	}

	public FormToFilePointer getWRLDExtBlockCELLByXY(int wrldFormId, Point point) {
		WRLDChildren wrldChildren = WRLDChildrenByFormId.get(wrldFormId);
		if (wrldChildren != null)
			return wrldChildren.getWRLDExtBlockCELLByXY(point);

		return null;
	}

	public void loadAndIndex(String fileName, FileChannelRAF in, long pos, int groupLength)
			throws IOException, DataFormatException, PluginException {

		FileChannel ch = in.getChannel();

		int dataLength = groupLength;
		byte prefix[] = new byte[headerByteCount];
		ByteBuffer pbb = ByteBuffer.wrap(prefix); //reused to avoid allocation of object, all bytes of array are refilled or error thrown

		while (dataLength >= headerByteCount) {

			int count = ch.read((ByteBuffer)pbb.rewind(), pos);
			pos += prefix.length;
			if (count != headerByteCount)
				throw new PluginException(fileName + ": Record prefix is incomplete");
			dataLength -= headerByteCount;
			//String type = new String(prefix, 0, 4);
			int length = ESMByteConvert.extractInt(prefix, 4);

			PluginRecord wrldRecord = new PluginRecord(prefix);
			wrldRecord.load(in, pos);
			pos += length;
			WRLDByFormId.put(wrldRecord.getFormID(), wrldRecord);

			dataLength -= length;

			//Anchorage WRLD 11657 has no children group after the WRLD group, so check to see if we are finished here
			if (dataLength >= headerByteCount) {

				count = ch.read((ByteBuffer)pbb.rewind(), pos);
				pos += prefix.length;
				if (count != headerByteCount)
					throw new PluginException(fileName + ": Record prefix is incomplete");
				dataLength -= headerByteCount;
				//type = new String(prefix, 0, 4);
				length = ESMByteConvert.extractInt(prefix, 4);

				length -= headerByteCount;

				int subGroupType = prefix[12] & 0xff;
				if (subGroupType == PluginGroup.WORLDSPACE) {
					WRLDChildren children = new WRLDChildren(prefix);
					children.loadAndIndex(in, pos, length);
					pos += length;
					WRLDChildrenByFormId.put(wrldRecord.getFormID(), children);
					dataLength -= length;
				} else {
					// It seem the WRLD 60 for FO76 is NOT followed by a GRUP of it's children? It's just the next WRLD along formId 3987
					// so the next record can be NOT a GRUP of children
					// put the channel back to where it was for the next while loop 
					pos -= prefix.length;
					dataLength += headerByteCount;
				}
			}
		}

		if (dataLength != 0) {
			if (getGroupType() == 0)
				throw new PluginException(fileName + ": Group " + getGroupRecordType() + " is incomplete");
			else
				throw new PluginException(fileName + ": Subgroup type " + getGroupType() + " is incomplete");
		}

	}
}
