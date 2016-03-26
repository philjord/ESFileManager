package esmmanager.loader;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.DataFormatException;

import tools.io.ESMByteConvert;
import esmmanager.Point;
import esmmanager.common.PluginException;
import esmmanager.common.data.plugin.PluginGroup;
import esmmanager.common.data.plugin.PluginRecord;

public class WRLDTopGroup extends PluginGroup
{
	//top level WRLD records
	public Map<Integer, PluginRecord> WRLDByFormId = new LinkedHashMap<Integer, PluginRecord>();

	public Map<Integer, WRLDChildren> WRLDChildrenByFormId = new LinkedHashMap<Integer, WRLDChildren>();

	public WRLDTopGroup(byte[] prefix)
	{
		super(prefix);
	}

	public CELLPointer getWRLDExtBlockCELLByXY(int wrldFormId, Point point)
	{
		WRLDChildren wrldChildren = WRLDChildrenByFormId.get(wrldFormId);
		if (wrldChildren != null)
			return wrldChildren.getWRLDExtBlockCELLByXY(point);
		
		return null;
	}

	public void loadAndIndex(String fileName, RandomAccessFile in, int groupLength) throws IOException, DataFormatException, PluginException
	{
		int dataLength = groupLength;
		byte prefix[] = new byte[headerByteCount];

		while (dataLength >= headerByteCount)
		{
			int count = in.read(prefix);
			if (count != headerByteCount)
				throw new PluginException(fileName + ": Record prefix is incomplete");
			dataLength -= headerByteCount;
			//String type = new String(prefix, 0, 4);
			int length = ESMByteConvert.extractInt(prefix, 4);

			PluginRecord wrldRecord = new PluginRecord(prefix);
			wrldRecord.load("", in, length);
			WRLDByFormId.put(new Integer(wrldRecord.getFormID()), wrldRecord);

			dataLength -= length;

			count = in.read(prefix);
			if (count != headerByteCount)
				throw new PluginException(fileName + ": Record prefix is incomplete");
			dataLength -= headerByteCount;
			//type = new String(prefix, 0, 4);
			length = ESMByteConvert.extractInt(prefix, 4);

			length -= headerByteCount;

			int subGroupType = prefix[12] & 0xff;

			if (subGroupType == PluginGroup.WORLDSPACE)
			{
				WRLDChildren children = new WRLDChildren(prefix);
				children.loadAndIndex(in, length);
				WRLDChildrenByFormId.put(wrldRecord.getFormID(), children);
			}
			else
			{
				System.out.println("Group Type " + subGroupType + " not allowed as child of WRLD");
			}
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
