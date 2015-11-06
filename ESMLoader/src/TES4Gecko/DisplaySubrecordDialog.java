package TES4Gecko;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class DisplaySubrecordDialog extends JDialog implements ActionListener
{
	public DisplaySubrecordDialog(JDialog parent, PluginSubrecord subrecord)
	{
		super(parent, "Subrecord Data: " + subrecord.getSubrecordType(), true);
		setDefaultCloseOperation(2);

		JPanel contentPane = new JPanel();
		contentPane.setLayout(new BoxLayout(contentPane, 1));
		contentPane.setOpaque(true);
		contentPane.setBackground(Main.backgroundColor);
		contentPane.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

		JLabel displayTypeLabel = new JLabel("<html><b>Subrecord data displayed as " + subrecord.getDisplayDataTypeLabel() + "</b></html>");
		displayTypeLabel.setAlignmentX(0.0F);
		JPanel displayTypePane = new JPanel();
		displayTypePane.setBackground(Main.backgroundColor);
		displayTypePane.add(displayTypeLabel);

		String displayData = subrecord.getDisplayData();
		int numRows = 0;
		if ((displayData.contains("\n")) || (displayData.contains("\r")))
		{
			String[] dummyArray = displayData.split("[\n\r]");
			boolean horizScroll = false;
			for (int i = 0; i < dummyArray.length; i++)
			{
				if (!dummyArray[i].equals(""))
				{
					numRows++;
				}
				else
				{
					if (dummyArray[i].length() <= 79)
						continue;
					horizScroll = true;
				}
			}
			if (horizScroll)
				numRows++;
		}
		else
		{
			numRows = displayData.length() > 79 ? 2 : 1;
		}
		JTextArea textArea = new JTextArea(displayData);
		textArea.setRows(numRows);
		textArea.setEditable(false);
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportView(textArea);
		scrollPane.setVerticalScrollBarPolicy(20);
		scrollPane.setHorizontalScrollBarPolicy(30);

		JPanel buttonPane = new JPanel();
		buttonPane.setBackground(Main.backgroundColor);

		JButton button = new JButton("Done");
		button.setActionCommand("done");
		button.setHorizontalAlignment(0);
		button.addActionListener(this);
		buttonPane.add(button);

		contentPane.add(displayTypePane);
		contentPane.add(scrollPane);
		contentPane.add(Box.createVerticalStrut(15));
		contentPane.add(buttonPane);
		setContentPane(contentPane);
	}

	public DisplaySubrecordDialog(JDialog parent, PluginSubrecord subrecord, boolean alwaysByte)
	{
		super(parent, "Subrecord Data: Byte Array", true);
		setDefaultCloseOperation(2);

		JPanel contentPane = new JPanel();
		contentPane.setLayout(new BoxLayout(contentPane, 1));
		contentPane.setOpaque(true);
		contentPane.setBackground(Main.backgroundColor);
		contentPane.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

		JLabel displayTypeLabel = new JLabel("<html><b>Subrecord data displayed as Byte Array</b></html>");

		displayTypeLabel.setAlignmentX(0.0F);
		JPanel displayTypePane = new JPanel();
		displayTypePane.setBackground(Main.backgroundColor);
		displayTypePane.add(displayTypeLabel);

		String displayData = subrecord.getDisplayDataAsBytes();
		int numRows = 0;
		if ((displayData.contains("\n")) || (displayData.contains("\r")))
		{
			String[] dummyArray = displayData.split("[\n\r]");
			boolean horizScroll = false;
			for (int i = 0; i < dummyArray.length; i++)
			{
				if (!dummyArray[i].equals(""))
				{
					numRows++;
				}
				else
				{
					if (dummyArray[i].length() <= 79)
						continue;
					horizScroll = true;
				}
			}
			if (horizScroll)
				numRows++;
		}
		else
		{
			numRows = displayData.length() > 79 ? 2 : 1;
		}
		JTextArea textArea = new JTextArea(displayData);
		textArea.setRows(numRows);
		textArea.setEditable(false);
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportView(textArea);
		scrollPane.setVerticalScrollBarPolicy(20);
		scrollPane.setHorizontalScrollBarPolicy(30);

		JPanel buttonPane = new JPanel();
		buttonPane.setBackground(Main.backgroundColor);

		JButton button = new JButton("Done");
		button.setActionCommand("done");
		button.setHorizontalAlignment(0);
		button.addActionListener(this);
		buttonPane.add(button);

		contentPane.add(displayTypePane);
		contentPane.add(scrollPane);
		contentPane.add(Box.createVerticalStrut(15));
		contentPane.add(buttonPane);
		setContentPane(contentPane);
	}

	public static void showDialog(JDialog parent, PluginSubrecord subrecord)
	{
		DisplaySubrecordDialog dialog = new DisplaySubrecordDialog(parent, subrecord);
		dialog.pack();
		Dimension resizeDim = new Dimension(Math.min(dialog.getSize().width, parent.getSize().width * 2 / 3), Math.min(
				dialog.getSize().height, parent.getSize().height * 2 / 3));
		dialog.setPreferredSize(resizeDim);
		dialog.pack();
		dialog.setLocationRelativeTo(parent);
		dialog.setVisible(true);
	}

	public static void showDialog(JDialog parent, PluginSubrecord subrecord, boolean alwaysByte)
	{
		DisplaySubrecordDialog dialog = new DisplaySubrecordDialog(parent, subrecord, alwaysByte);
		dialog.pack();
		Dimension resizeDim = new Dimension(Math.min(dialog.getSize().width, parent.getSize().width * 2 / 3), Math.min(
				dialog.getSize().height, parent.getSize().height * 2 / 3));
		dialog.setPreferredSize(resizeDim);
		dialog.pack();
		dialog.setLocationRelativeTo(parent);
		dialog.setVisible(true);
	}

	public void actionPerformed(ActionEvent ae)
	{
		try
		{
			String action = ae.getActionCommand();
			if (action.equals("done"))
			{
				setVisible(false);
				dispose();
			}
		}
		catch (Throwable exc)
		{
			Main.logException("Exception while processing action event", exc);
		}
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.DisplaySubrecordDialog
 * JD-Core Version:    0.6.0
 */