package display;

import java.awt.Color;
import java.awt.Dialog;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import esmmanager.common.data.plugin.PluginSubrecord;

public class DisplaySubrecordDialog extends JDialog implements ActionListener
{

	public DisplaySubrecordDialog(Window parent, PluginSubrecord subrecord)
	{
		super(parent, "Subrecord Data", Dialog.ModalityType.MODELESS);
		setDefaultCloseOperation(2);

		JPanel contentPane = getTextArea(subrecord);

		JPanel buttonPane = new JPanel();
		buttonPane.setBackground(new Color(240, 240, 240));
		JButton button = new JButton("Done");
		button.setActionCommand("done");
		button.setHorizontalAlignment(0);
		button.addActionListener(this);
		buttonPane.add(button);
		contentPane.add(buttonPane);
		setContentPane(contentPane);
	}

	public static JPanel getTextArea(PluginSubrecord subrecord)
	{
		byte[] subrecordData = subrecord.getSubrecordData();

		StringBuffer dumpData = new StringBuffer(128 + 3 * subrecordData.length + 6 * (subrecordData.length / 16));
		/*dumpData.append(String.format("%s subrecord: Data length x'%X'\n", new Object[] {
		    subrecord.getSubrecordType(), Integer.valueOf(subrecordData.length)
		}));*/
		dumpData.append("" + subrecord.getSubrecordType() + " subrecord: Data length x'" + subrecordData.length + "'\n");
		dumpData.append("\n       0  1  2  3  4  5  6  7  8  9  A  B  C  D  E  F\n");
		StringBuffer dumpHex = new StringBuffer(48);
		StringBuffer dumpLine = new StringBuffer(16);
		for (int i = 0; i < subrecordData.length; i += 16)
		{
			for (int j = 0; j < 16; j++)
			{
				int offset = i + j;
				if (offset == subrecordData.length)
					break;
				//dumpHex.append(String.format(" %02X", new Object[] { Byte.valueOf(subrecordData[offset])}));
				dumpHex.append(" " + subrecordData[offset]);
				if (subrecordData[offset] >= 32 && subrecordData[offset] < 127)
					dumpLine.append(new String(subrecordData, offset, 1));
				else
					dumpLine.append(".");
			}

			for (; dumpHex.length() < 48; dumpHex.append("   "))
				;
			for (; dumpLine.length() < 16; dumpLine.append(" "))
				;
			//dumpData.append(String.format("%04X:", new Object[] { Integer.valueOf(i)}));
			dumpData.append("" + i + ":");
			dumpData.append(dumpHex);
			dumpData.append("  *");
			dumpData.append(dumpLine);
			dumpData.append("*");
			if (i + 16 < subrecordData.length)
				dumpData.append("\n");
			dumpHex.delete(0, 48);
			dumpLine.delete(0, 16);
		}

		JTextArea textArea = new JTextArea(dumpData.toString());
		JScrollPane scrollPane = new JScrollPane(textArea);
		scrollPane.setVerticalScrollBarPolicy(22);

		JPanel contentPane = new JPanel();
		contentPane.setLayout(new BoxLayout(contentPane, 1));
		contentPane.setOpaque(true);
		contentPane.setBackground(new Color(240, 240, 240));
		contentPane.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
		contentPane.add(scrollPane);
		contentPane.add(Box.createVerticalStrut(15));

		return contentPane;
	}

	public static void showDialog(Window parent, PluginSubrecord subrecord)
	{
		DisplaySubrecordDialog dialog = new DisplaySubrecordDialog(parent, subrecord);
		dialog.pack();
		dialog.setLocationRelativeTo(parent);
		dialog.setVisible(true);
	}

	public void actionPerformed(ActionEvent ae)
	{

		String action = ae.getActionCommand();
		if (action.equals("done"))
		{
			setVisible(false);
			dispose();
		}

	}
}
