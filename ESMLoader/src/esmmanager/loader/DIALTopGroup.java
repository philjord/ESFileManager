package esmmanager.loader;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import esmmanager.common.PluginException;
import esmmanager.common.data.plugin.PluginGroup;
import tools.io.ESMByteConvert;

public class DIALTopGroup extends PluginGroup
{
	private Map<Integer, CELLDIALPointer> DIALByFormID = null;

	public DIALTopGroup(byte[] prefix)
	{
		super(prefix);
	}

	public CELLDIALPointer getDIAL(int dialId) throws IOException, PluginException
	{
		return DIALByFormID.get(dialId);
	}

	public void getAllInteriorCELLFormIds(ArrayList<CELLDIALPointer> ret) throws IOException, PluginException
	{
		ret.addAll(DIALByFormID.values());
	}

	public void loadAndIndex(RandomAccessFile in, int groupLength) throws IOException, PluginException
	{
		DIALByFormID = new HashMap<Integer, CELLDIALPointer>();
		int dataLength = groupLength;
		byte prefix[] = new byte[headerByteCount];

		CELLDIALPointer cellPointer = null;

		while (dataLength >= headerByteCount)
		{
			long filePositionPointer = in.getFilePointer();

			int count = in.read(prefix);
			if (count != headerByteCount)
				throw new PluginException(": Record prefix is incomplete");
			dataLength -= headerByteCount;
			String type = new String(prefix, 0, 4);
			int length = ESMByteConvert.extractInt(prefix, 4);

			if (type.equals("GRUP"))
			{
				length -= headerByteCount;
				int subGroupType = prefix[12] & 0xff;

				if (subGroupType == PluginGroup.TOPIC)
				{
					cellPointer.cellChildrenFilePointer = filePositionPointer;
					// now skip the group
					in.skipBytes(length);
				}
				else
				{
					System.out.println("Group Type " + subGroupType + " not allowed as child of DIAL group");
				}
			}
			else if (type.equals("DIAL"))
			{
				int formID = ESMByteConvert.extractInt(prefix, 12);
				cellPointer = new CELLDIALPointer(formID, filePositionPointer);
				DIALByFormID.put(new Integer(formID), cellPointer);
				in.skipBytes(length);
			}
			else
			{
				System.out.println("What the hell is a type " + type + " doing in the Int CELL sub block group?");
			}

			//prep for next iter
			dataLength -= length;
		}

		if (dataLength != 0)
		{
			if (getGroupType() == 0)
				throw new PluginException(": Group " + getGroupRecordType() + " is incomplete");
			else
				throw new PluginException(": Subgroup type " + getGroupType() + " is incomplete");
		}

	}

}
