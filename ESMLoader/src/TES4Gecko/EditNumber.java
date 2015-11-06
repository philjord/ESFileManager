package TES4Gecko;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;

import javax.swing.JFormattedTextField;

public final class EditNumber extends JFormattedTextField.AbstractFormatter
{
	private NumberFormat formatter;

	private boolean integerOnly;

	public EditNumber(boolean integerOnly, boolean useGrouping)
	{
		this.integerOnly = integerOnly;
		this.formatter = NumberFormat.getNumberInstance();
		this.formatter.setParseIntegerOnly(integerOnly);
		this.formatter.setGroupingUsed(useGrouping);
	}

	public Object stringToValue(String string) throws ParseException
	{
		int length = string.length();

		if (length == 0)
		{
			setEditValid(false);
			Number value;

			if (this.integerOnly)
				value = new Integer(0);
			else
			{
				value = new Double(0.0D);
			}
			return value;
		}

		ParsePosition pos = new ParsePosition(0);
		Number value = this.formatter.parse(string, pos);
		int index = pos.getIndex();

		if (value == null)
		{
			setEditValid(false);
			throw new ParseException("Unable to parse number", index);
		}

		if (index == length)
		{
			setEditValid(true);
			if (this.integerOnly)
				value = new Integer(value.intValue());
			else if (!(value instanceof Double))
			{
				value = new Double(value.doubleValue());
			}
			return value;
		}

		double number = value.doubleValue();
		int op = 0;
		while (index < length)
		{
			char c = string.charAt(index);
			if (c == '+')
			{
				op = 0;
			}
			else if (c == '-')
			{
				op = 1;
			}
			else if (c == '*')
			{
				op = 2;
			}
			else if (c == '/')
			{
				op = 3;
			}
			else
			{
				setEditValid(false);
				throw new ParseException("Unrecognized operator", index);
			}

			index++;
			if (index == length)
			{
				setEditValid(false);
				throw new ParseException("Trailing operator", index);
			}

			pos.setIndex(index);
			value = this.formatter.parse(string, pos);
			index = pos.getIndex();
			if (value == null)
			{
				setEditValid(false);
				throw new ParseException("Unable to parse number", index);
			}

			switch (op)
			{
				case 0:
					number += value.doubleValue();
					break;
				case 1:
					number -= value.doubleValue();
					break;
				case 2:
					number *= value.doubleValue();
					break;
				case 3:
					number /= value.doubleValue();
			}

		}

		if (this.integerOnly)
			value = new Integer((int) number);
		else
		{
			value = new Double(number);
		}
		setEditValid(true);
		return value;
	}

	public String valueToString(Object value) throws ParseException
	{
		if (value == null)
		{
			setEditValid(false);
			return new String();
		}

		if (!(value instanceof Number))
		{
			setEditValid(false);
			throw new ParseException("Value is not a Number", 0);
		}

		setEditValid(true);
		return this.formatter.format(value);
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.EditNumber
 * JD-Core Version:    0.6.0
 */