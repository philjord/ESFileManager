package TES4Gecko;

public final class DialogueTypeCode
{
	public static final int Invalid = -1;

	public static final int Topic = 0;

	public static final int Conversation = 1;

	public static final int Combat = 2;

	public static final int Persuasion = 3;

	public static final int Detection = 4;

	public static final int Service = 5;

	public static final int Miscellaneous = 6;

	public static boolean isValid(int param)
	{
		return (param == 0) || (param == 1) || (param == 2) || (param == 3) || (param == 4) || (param == 5) || (param == 6);
	}

	public static String getString(int param)
	{
		switch (param)
		{
			case 0:
				return "Topic";
			case 1:
				return "Conversation";
			case 2:
				return "Combat";
			case 3:
				return "Persuasion";
			case 4:
				return "Detection";
			case 5:
				return "Service";
			case 6:
				return "Miscellaneous";
		}
		return "Invalid";
	}

	public static int getCode(String param)
	{
		if (param.equalsIgnoreCase("Topic"))
			return 0;
		if (param.equalsIgnoreCase("Conversation"))
			return 1;
		if (param.equalsIgnoreCase("Combat"))
			return 2;
		if (param.equalsIgnoreCase("Persuasion"))
			return 3;
		if (param.equalsIgnoreCase("Detection"))
			return 4;
		if (param.equalsIgnoreCase("Service"))
			return 5;
		if (param.equalsIgnoreCase("Miscellaneous"))
			return 6;
		return -1;
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.DialogueTypeCode
 * JD-Core Version:    0.6.0
 */