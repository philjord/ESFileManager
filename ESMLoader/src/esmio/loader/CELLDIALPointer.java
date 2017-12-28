package esmio.loader;

public class CELLDIALPointer
{
	public int formId = -1;

	public long cellFilePointer = -1;

	public long cellChildrenFilePointer = -1;

	public CELLDIALPointer(int formId, long cellFilePointer)
	{
		this.formId = formId;
		this.cellFilePointer = cellFilePointer;
	}

}
