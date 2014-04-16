package esmLoader.loader;

public class CELLPointer
{
	public int formId = -1;

	public long cellFilePointer = -1;

	public long cellChildrenFilePointer = -1;

	public CELLPointer(int formId, long cellFilePointer)
	{
		this.formId = formId;
		this.cellFilePointer = cellFilePointer;
	}

}
