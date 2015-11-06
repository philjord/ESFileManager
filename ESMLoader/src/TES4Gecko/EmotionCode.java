package TES4Gecko;

public final class EmotionCode
{
	public static final int Invalid = -1;

	public static final int Neutral = 0;

	public static final int Anger = 1;

	public static final int Disgust = 2;

	public static final int Fear = 3;

	public static final int Sad = 4;

	public static final int Happy = 5;

	public static final int Surprise = 6;

	public static boolean isValid(int param)
	{
		return (param == 0) || (param == 1) || (param == 2) || (param == 3) || (param == 4) || (param == 5) || (param == 6);
	}

	public static String getString(int param)
	{
		switch (param)
		{
			case 0:
				return "Neutral";
			case 1:
				return "Anger";
			case 2:
				return "Disgust";
			case 3:
				return "Fear";
			case 4:
				return "Sad";
			case 5:
				return "Happy";
			case 6:
				return "Surprise";
		}
		return "Invalid";
	}

	public static int getCode(String param)
	{
		if (param.equalsIgnoreCase("Neutral"))
			return 0;
		if (param.equalsIgnoreCase("Anger"))
			return 1;
		if (param.equalsIgnoreCase("Disgust"))
			return 2;
		if (param.equalsIgnoreCase("Fear"))
			return 3;
		if (param.equalsIgnoreCase("Sad"))
			return 4;
		if (param.equalsIgnoreCase("Happy"))
			return 5;
		if (param.equalsIgnoreCase("Surprise"))
			return 6;
		return -1;
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.EmotionCode
 * JD-Core Version:    0.6.0
 */