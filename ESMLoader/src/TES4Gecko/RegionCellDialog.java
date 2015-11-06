package TES4Gecko;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class RegionCellDialog extends JDialog implements ActionListener
{
	private JList regionList;

	private JButton doneButton;

	private String regionsToExport = "All";

	public static final String None = "None";

	public static final String All = "All";

	public static final String CancelMerge = "CancelMerge";

	public static final String Some = "Some";

	public static final String Except = "Except";

	public static final String Separator = ":";

	private static final String Title = "Exterior Cells to be Merged";

	private static final String Header = "<html>There are exterior cells in the plugin to be merged. Please select whether to merge all<br>such cells, none of those cells, or select the regions where the cells to be merged are located.<br> Please note the following:<br><br>&#8226 Persistent references are unaffected.<br>&#8226 New regions and worldspaces are still merged; this only affects exterior cells.<br>&#8226 If this window is closed by any method except the <i>Done</i> button, all exterior cells are merged.<br></html>";

	private static final String Unassigned = "<html><b><i>There are exterior cells not assigned to any region in this plugin!<br>Only the first option will merge these cells!</i></b></html>";

	public RegionCellDialog(JFrame parent, Vector<String[]> regionData)
	{
		super(parent, true);
		setDefaultCloseOperation(2);

		boolean hasUnassignedCells = false;
		boolean hasOnlyUnassignedCells = false;
		for (Iterator<String[]> i = regionData.iterator(); i.hasNext();)
		{
			String[] regionArray = i.next();
			if (Integer.parseInt(regionArray[0], 16) != 65535)
				continue;
			hasUnassignedCells = true;
			i.remove();
			break;
		}

		if ((hasUnassignedCells) && (regionData.size() == 0))
		{
			hasOnlyUnassignedCells = true;
		}

		this.regionList = new JList(regionData);

		ListSelectionListener listListener = new ListSelectionListener()
		{
			public void valueChanged(ListSelectionEvent e)
			{
				if (!e.getValueIsAdjusting())
				{
					if (RegionCellDialog.this.regionList.getSelectedIndices().length == 0)
					{
						RegionCellDialog.this.doneButton.setEnabled(false);
					}
					else
					{
						RegionCellDialog.this.doneButton.setEnabled(true);
					}
				}
			}
		};
		this.regionList.setSelectionMode(2);
		this.regionList.addListSelectionListener(listListener);
		this.regionList.setCellRenderer(new cellRenderer());
		JScrollPane listPane = new JScrollPane(this.regionList);
		this.regionList.setEnabled(false);

		JRadioButton allButton = new JRadioButton("Merge all exterior cells; includes cells not assigned to regions", true);
		JRadioButton noneButton = new JRadioButton("Merge no exterior cells", false);
		JRadioButton cancelMergeButton = new JRadioButton("Cancel entire merge", false);
		JRadioButton selectButton = new JRadioButton("Select regions with exterior cells to be merged:", false);
		JRadioButton exceptButton = new JRadioButton("Select regions with exterior cells NOT to be merged:", false);
		allButton.setBackground(Main.backgroundColor);
		noneButton.setBackground(Main.backgroundColor);
		cancelMergeButton.setBackground(Main.backgroundColor);
		selectButton.setBackground(Main.backgroundColor);
		exceptButton.setBackground(Main.backgroundColor);
		ButtonGroup bgroup = new ButtonGroup();
		allButton.setActionCommand("merge all");
		allButton.addActionListener(this);
		bgroup.add(allButton);
		noneButton.setActionCommand("merge none");
		noneButton.addActionListener(this);
		bgroup.add(noneButton);
		cancelMergeButton.setActionCommand("cancel merge");
		cancelMergeButton.addActionListener(this);
		bgroup.add(cancelMergeButton);
		selectButton.setActionCommand("merge some");
		selectButton.addActionListener(this);
		if (!hasOnlyUnassignedCells)
		{
			bgroup.add(selectButton);
		}
		exceptButton.setActionCommand("merge some except");
		exceptButton.addActionListener(this);
		if (!hasOnlyUnassignedCells)
		{
			bgroup.add(exceptButton);
		}

		JPanel radioPane = new JPanel(new GridLayout(5, 1));
		radioPane.setBackground(Main.backgroundColor);
		radioPane.add(allButton);
		radioPane.add(noneButton);
		radioPane.add(cancelMergeButton);
		if (!hasOnlyUnassignedCells)
		{
			radioPane.add(selectButton);
		}
		if (!hasOnlyUnassignedCells)
		{
			radioPane.add(exceptButton);
		}

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
		JLabel headerLabel = new JLabel(
				"<html>There are exterior cells in the plugin to be merged. Please select whether to merge all<br>such cells, none of those cells, or select the regions where the cells to be merged are located.<br> Please note the following:<br><br>&#8226 Persistent references are unaffected.<br>&#8226 New regions and worldspaces are still merged; this only affects exterior cells.<br>&#8226 If this window is closed by any method except the <i>Done</i> button, all exterior cells are merged.<br></html>",
				2);
		headerLabel.setAlignmentX(0.5F);
		contentPane.add(headerLabel);
		contentPane.add(Box.createVerticalStrut(15));
		contentPane.add(radioPane);
		if (hasUnassignedCells)
		{
			JLabel unassignedLabel = new JLabel(
					"<html><b><i>There are exterior cells not assigned to any region in this plugin!<br>Only the first option will merge these cells!</i></b></html>",
					2);
			unassignedLabel.setAlignmentX(0.5F);
			contentPane.add(unassignedLabel);
			contentPane.add(Box.createVerticalStrut(5));
		}
		if (!hasOnlyUnassignedCells)
		{
			contentPane.add(listPane);
		}
		contentPane.add(Box.createVerticalStrut(15));
		contentPane.add(buttonPane);
		setContentPane(contentPane);
		setTitle("Exterior Cells to be Merged");

		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				RegionCellDialog.this.regionsToExport = "All";
				RegionCellDialog.this.setVisible(false);
				RegionCellDialog.this.dispose();
			}
		});
	}

	public static String showDialog(JFrame parent, Vector<String[]> regionData)
	{
		RegionCellDialog dialog = new RegionCellDialog(parent, regionData);
		dialog.pack();
		dialog.setLocationRelativeTo(parent);
		dialog.setVisible(true);
		return dialog.regionsToExport;
	}

	public void actionPerformed(ActionEvent ae)
	{
		String action = ae.getActionCommand();
		try
		{
			if (action.equals("merge all"))
			{
				this.regionList.clearSelection();
				this.regionList.setEnabled(false);
				this.doneButton.setEnabled(true);
				this.regionsToExport = "All";
			}
			else if (action.equals("merge none"))
			{
				this.regionList.clearSelection();
				this.regionList.setEnabled(false);
				this.doneButton.setEnabled(true);
				this.regionsToExport = "None";
			}
			else if (action.equals("cancel merge"))
			{
				this.regionList.clearSelection();
				this.regionList.setEnabled(false);
				this.doneButton.setEnabled(true);
				this.regionsToExport = "CancelMerge";
			}
			else if (action.equals("merge some"))
			{
				this.regionList.setEnabled(true);
				if (this.regionList.getSelectedIndex() == -1)
					this.doneButton.setEnabled(false);
				this.regionsToExport = "Some";
			}
			else if (action.equals("merge some except"))
			{
				this.regionList.setEnabled(true);
				if (this.regionList.getSelectedIndex() == -1)
					this.doneButton.setEnabled(false);
				this.regionsToExport = "Except";
			}
			else if (action.equals("cancel"))
			{
				this.regionsToExport = "All";
				setVisible(false);
				dispose();
			}
			else if (action.equals("done"))
			{
				if ((this.regionsToExport.equals("Some")) || (this.regionsToExport.equals("Except")))
				{
					Object[] valueArray = this.regionList.getSelectedValues();
					for (int i = 0; i < valueArray.length; i++)
					{
						String[] regionValues = (String[]) valueArray[i];
						this.regionsToExport = (this.regionsToExport + ":" + regionValues[0]);
					}
				}
				setVisible(false);
				dispose();
			}
		}
		catch (Throwable exc)
		{
			Main.logException("Exception while processing action event " + action, exc);
		}
	}

	class cellRenderer extends JPanel implements ListCellRenderer
	{
		JPanel testPanel = null;

		JLabel plugin = null;

		JLabel worldspace = null;

		JLabel region = null;

		cellRenderer()
		{
			this.plugin = new JLabel("");
			this.worldspace = new JLabel("");
			this.region = new JLabel("");
			this.testPanel = new JPanel();
			this.testPanel.setLayout(new GridLayout(1, 3));
			this.testPanel.add(this.plugin);
			this.testPanel.add(this.worldspace);
			this.testPanel.add(this.region);
		}

		public Component getListCellRendererComponent(JList list, Object value, int idx, boolean isSel, boolean hasFocus)
		{
			if (value != null)
			{
				String[] regionInfo = (String[]) value;
				this.plugin.setText(regionInfo[1]);
				this.worldspace.setText(regionInfo[2]);
				this.region.setText(regionInfo[3]);
			}
			this.testPanel.setBackground(isSel ? list.getSelectionBackground() : list.getBackground());
			this.testPanel.setForeground(isSel ? list.getSelectionForeground() : list.getForeground());

			return this.testPanel;
		}
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.RegionCellDialog
 * JD-Core Version:    0.6.0
 */