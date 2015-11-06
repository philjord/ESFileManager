package TES4Gecko;

public class FunctionInfo
{
	private String functionName;

	private int functionCode;

	private boolean firstReference;

	private boolean secondReference;

	public FunctionInfo(String name, int code, boolean firstParam, boolean secondParam)
	{
		this.functionName = name;
		this.functionCode = code;
		this.firstReference = firstParam;
		this.secondReference = secondParam;
	}

	public String getName()
	{
		return this.functionName;
	}

	public int getCode()
	{
		return this.functionCode;
	}

	public boolean isFirstReference()
	{
		return this.firstReference;
	}

	public boolean isSecondReference()
	{
		return this.secondReference;
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.FunctionInfo
 * JD-Core Version:    0.6.0
 */