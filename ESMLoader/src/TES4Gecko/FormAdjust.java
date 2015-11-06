package TES4Gecko;

import java.util.Map;

public class FormAdjust
{
	private int[] masterMap;

	private int masterCount;

	private Map<Integer, FormInfo> formMap;

	public FormAdjust()
	{
	}

	public FormAdjust(int[] masterMap, int masterCount)
	{
		this.masterMap = masterMap;
		this.masterCount = masterCount;
	}

	public FormAdjust(int[] masterMap, int masterCount, Map<Integer, FormInfo> formMap)
	{
		this.masterMap = masterMap;
		this.masterCount = masterCount;
		this.formMap = formMap;
	}

	public int[] getMasterMap()
	{
		return this.masterMap;
	}

	public int getMasterCount()
	{
		return this.masterCount;
	}

	public Map<Integer, FormInfo> getFormMap()
	{
		return this.formMap;
	}

	public int adjustFormID(int formID)
	{
		if ((this.masterMap == null) || (formID == 0))
		{
			return formID;
		}

		int masterID = formID >>> 24;
		if (masterID < this.masterMap.length)
		{
			masterID = this.masterMap[masterID];
			return formID & 0xFFFFFF | masterID << 24;
		}

		int newFormID = formID & 0xFFFFFF;
		if (this.formMap == null)
		{
			newFormID |= this.masterCount << 24;
		}
		else
		{
			FormInfo formInfo = this.formMap.get(new Integer(formID));
			if (formInfo == null)
				newFormID |= this.masterCount << 24;
			else
			{
				newFormID = formInfo.getMergedFormID();
			}
		}
		return newFormID;
	}

	public String adjustEditorID(int formID)
	{
		String editorID = null;
		if (this.formMap != null)
		{
			FormInfo formInfo = this.formMap.get(new Integer(formID));
			if (formInfo != null)
			{
				editorID = formInfo.getMergedEditorID();
			}
		}
		return editorID;
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.FormAdjust
 * JD-Core Version:    0.6.0
 */