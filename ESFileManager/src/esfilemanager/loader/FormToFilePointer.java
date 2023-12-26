package esfilemanager.loader;

/**
 * Pointer used by CELL and DIALS so a bit confusing
 * @author pjnz
 *
 */
public class FormToFilePointer
{
	public int formId = -1;

	public long cellFilePointer = -1;

	public long cellChildrenFilePointer = -1;

	public FormToFilePointer(int formId, long cellFilePointer)
	{
		this.formId = formId;
		this.cellFilePointer = cellFilePointer;
	}

}
