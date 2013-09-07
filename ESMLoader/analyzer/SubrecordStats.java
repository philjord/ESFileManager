package analyzer;

import java.util.HashSet;
import java.util.regex.Pattern;

import tools.io.ESMByteConvert;
import esmLoader.common.data.record.Subrecord;

public class SubrecordStats
{
	public String subrecordType;

	public int count = 0;

	public int minLength = Integer.MAX_VALUE;

	public int maxLength = Integer.MIN_VALUE;

	public boolean isString = true; // any false leaves it as false only all trues leave it

	public boolean couldBeFormId = true;

	public HashSet<String> appearsIn = new HashSet<String>();

	public HashSet<Integer> hasOrderOf = new HashSet<Integer>();

	public SubrecordStats(String st)
	{
		this.subrecordType = st;
	}

	public void applySub(Subrecord sub, String inRec, int orderNo)
	{


		appearsIn.add(inRec);
		hasOrderOf.add(orderNo);

		count++;
		byte[] bs = sub.getSubrecordData();
		if (minLength > bs.length)
		{
			minLength = bs.length;
		}

		if (maxLength < bs.length)
		{
			maxLength = bs.length;
		}

		if (bs.length == 4)
		{
			int possFormId = ESMByteConvert.extractInt(bs, 0);
			if (possFormId < 0 || possFormId > EsmFormatAnalyzer.maxFormId)
			{
				couldBeFormId = false;
			}
		}
		else
		{
			couldBeFormId = false;
		}

		//oblivion has lots of massive DESC recos and FULLs don't show either
		if (bs.length > 0 && bs[bs.length - 1] == 0 )//&& bs.length < 2048)
		{
			// only update is string if it is not yet false
			if (isString)
			{
				String s = new String(bs, 0, bs.length - 1);
				isString = Pattern.matches("[^\\p{C}[\\s]]*", s);
				//if (!Pattern.matches("[\\p{Graph}\\p{Space}]+.", str))
			}
		}
		else
		{
			isString = false;
		}

	}
}
