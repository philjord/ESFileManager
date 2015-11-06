package TES4Gecko;

import java.text.ParseException;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.UIManager;

public final class EditInputVerifier extends InputVerifier
{
	private boolean optionalField;

	public EditInputVerifier(boolean optionalField)
	{
		this.optionalField = optionalField;
	}

	public boolean verify(JComponent input)
	{
		boolean allow = true;
		if ((input instanceof JFormattedTextField))
		{
			JFormattedTextField textField = (JFormattedTextField) input;
			JFormattedTextField.AbstractFormatter formatter = textField.getFormatter();
			if (formatter != null)
			{
				String value = textField.getText();
				if (value.length() != 0)
					try
					{
						formatter.stringToValue(value);
					}
					catch (ParseException exc)
					{
						allow = false;
					}
				else if (!this.optionalField)
				{
					allow = false;
				}
			}
		}

		return allow;
	}

	public boolean shouldYieldFocus(JComponent input)
	{
		boolean allow = verify(input);
		if (!allow)
		{
			UIManager.getLookAndFeel().provideErrorFeedback(input);
		}
		return allow;
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.EditInputVerifier
 * JD-Core Version:    0.6.0
 */