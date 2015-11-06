package TES4Gecko;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

public class CellMusicDialog extends JDialog implements ActionListener
{
	private JButton doneButton;

	public String cellMusicType = "Default";

	public static final String Default = "Default";

	public static final String Public = "Public";

	public static final String Dungeon = "Dungeon";

	public static final String Cancel = "Cancel";

	//private static final String Title = "Exterior Cell Music";

	public CellMusicDialog(JDialog parent)
	{
		super(parent, true);
		setDefaultCloseOperation(2);

		JRadioButton defaultButton = new JRadioButton("Default", true);
		JRadioButton publicButton = new JRadioButton("Public", false);
		JRadioButton dungeonButton = new JRadioButton("Dungeon", false);
		defaultButton.setBackground(Main.backgroundColor);
		publicButton.setBackground(Main.backgroundColor);
		dungeonButton.setBackground(Main.backgroundColor);
		ButtonGroup bgroup = new ButtonGroup();
		defaultButton.setActionCommand("set default music");
		defaultButton.addActionListener(this);
		bgroup.add(defaultButton);
		publicButton.setActionCommand("set public music");
		publicButton.addActionListener(this);
		bgroup.add(publicButton);
		dungeonButton.setActionCommand("set dungeon music");
		dungeonButton.addActionListener(this);
		bgroup.add(dungeonButton);
		JPanel radioPane = new JPanel(new GridLayout(3, 1));
		radioPane.setBackground(Main.backgroundColor);
		radioPane.add(defaultButton);
		radioPane.add(publicButton);
		radioPane.add(dungeonButton);

		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, 0));
		buttonPane.setBackground(Main.backgroundColor);

		this.doneButton = new JButton("Done");
		this.doneButton.setActionCommand("done");
		this.doneButton.addActionListener(this);
		buttonPane.add(this.doneButton);

		buttonPane.add(Box.createHorizontalStrut(45));
		JButton button = new JButton("Cancel");
		button.setActionCommand("cancel");
		button.addActionListener(this);
		buttonPane.add(button);

		JPanel contentPane = new JPanel();
		contentPane.setLayout(new BoxLayout(contentPane, 1));
		contentPane.setOpaque(true);
		contentPane.setBackground(Main.backgroundColor);
		contentPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		contentPane.add(Box.createVerticalStrut(15));
		contentPane.add(radioPane);
		contentPane.add(Box.createVerticalStrut(15));
		contentPane.add(buttonPane);
		setContentPane(contentPane);
		setTitle("Exterior Cell Music");

		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				CellMusicDialog.this.cellMusicType = "Cancel";
				CellMusicDialog.this.setVisible(false);
				CellMusicDialog.this.dispose();
			}
		});
	}

	public static String showDialog(JDialog parent)
	{
		CellMusicDialog dialog = new CellMusicDialog(parent);
		dialog.pack();
		dialog.setLocationRelativeTo(parent);
		dialog.setVisible(true);
		return dialog.cellMusicType;
	}

	public void actionPerformed(ActionEvent ae)
	{
		String action = ae.getActionCommand();
		try
		{
			if (action.equals("set default music"))
			{
				this.cellMusicType = "Default";
			}
			else if (action.equals("set public music"))
			{
				this.cellMusicType = "Public";
			}
			else if (action.equals("set dungeon music"))
			{
				this.cellMusicType = "Dungeon";
			}
			else if (action.equals("cancel"))
			{
				this.cellMusicType = "Cancel";
				setVisible(false);
				dispose();
			}
			else if (action.equals("done"))
			{
				setVisible(false);
				dispose();
			}
		}
		catch (Throwable exc)
		{
			Main.logException("Exception while processing action event " + action, exc);
		}
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.CellMusicDialog
 * JD-Core Version:    0.6.0
 */