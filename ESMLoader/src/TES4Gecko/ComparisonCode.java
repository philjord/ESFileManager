package TES4Gecko;

import java.util.HashMap;

public final class ComparisonCode
{
	public static final int EqualTo = 0;

	public static final int NotEqualTo = 2;

	public static final int GreaterThan = 4;

	public static final int GreaterThanOrEqualTo = 6;

	public static final int LessThan = 8;

	public static final int LessThanOrEqualTo = 10;

	public static final HashMap<Integer, String> compCodeSymbolMap = new HashMap<Integer, String>();

	static
	{
		compCodeSymbolMap.put(Integer.valueOf(0), "==");
		compCodeSymbolMap.put(Integer.valueOf(2), "!=");
		compCodeSymbolMap.put(Integer.valueOf(4), ">");
		compCodeSymbolMap.put(Integer.valueOf(6), ">=");
		compCodeSymbolMap.put(Integer.valueOf(8), "<");
		compCodeSymbolMap.put(Integer.valueOf(10), "<=");
	}

	public static boolean isValid(int param)
	{
		return (param == 0) || (param == 2) || (param == 4) || (param == 6) || (param == 8) || (param == 10);
	}

	public static String getCompCodeSymbol(int compCodeType)
	{
		if (!compCodeSymbolMap.containsKey(Integer.valueOf(compCodeType)))
			return "Invalid comparison operator";
		return compCodeSymbolMap.get(Integer.valueOf(compCodeType));
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.ComparisonCode
 * JD-Core Version:    0.6.0
 */