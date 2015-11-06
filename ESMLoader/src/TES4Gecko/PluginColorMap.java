package TES4Gecko;

import java.awt.Color;
import java.util.HashMap;

public final class PluginColorMap
{
	private static HashMap<Integer, Color> colorMap = new HashMap<Integer, Color>();

	private static final Color ThisPlugin = Color.WHITE;

	private static final Color LastESM = Color.YELLOW;

	private static final Color LastMinus1ESM = Color.PINK;

	private static final Color LastMinus2ESM = Color.CYAN;

	private static final Color LastMinus3ESM = Color.ORANGE;

	private static final Color LastMinus4ESM = Color.MAGENTA;

	private static final Color AllOtherESM = Color.GREEN;

	private static Color[] colorArray =
	{ ThisPlugin, LastESM, LastMinus1ESM, LastMinus2ESM, LastMinus3ESM, LastMinus4ESM, AllOtherESM };

	public static void setColorMap(int numMasters)
	{
		colorMap.clear();
		int i = numMasters;
		for (int j = 0; i >= 0; j++)
		{
			colorMap.put(Integer.valueOf(i), j > 6 ? colorArray[6] : colorArray[j]);

			i--;
		}
	}

	public static Color getPluginColor(int modIndex)
	{
		if (colorMap.containsKey(Integer.valueOf(modIndex)))
			return colorMap.get(Integer.valueOf(modIndex));
		return AllOtherESM;
	}

	public static Color getPluginColor(String modIndex)
	{
		Color retColor = null;
		try
		{
			int idx = Integer.parseInt(modIndex);
			retColor = getPluginColor(idx);
		}
		catch (Exception ex)
		{
			retColor = AllOtherESM;
		}
		return retColor;
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.PluginColorMap
 * JD-Core Version:    0.6.0
 */