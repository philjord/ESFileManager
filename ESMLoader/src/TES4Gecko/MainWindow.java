package TES4Gecko;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class MainWindow extends JFrame implements ActionListener
{
	private boolean windowMinimized = false;

	public MainWindow()
	{
		super("TES4Gecko Plugin Utility");
		setDefaultCloseOperation(2);

		String propValue = Main.properties.getProperty("window.main.position");
		if (propValue != null)
		{
			int frameX = 0;
			int frameY = 0;
			int sep = propValue.indexOf(',');
			frameX = Integer.parseInt(propValue.substring(0, sep));
			frameY = Integer.parseInt(propValue.substring(sep + 1));
			setLocation(frameX, frameY);
		}

		JPanel contentPane = new JPanel(new GridLayout(0, 2, 20, 20));
		contentPane.setOpaque(true);
		contentPane.setBackground(Main.backgroundColor);
		contentPane.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

		JButton button = new JButton("Merge Plugins");
		button.setActionCommand("merge plugins");
		button.setHorizontalAlignment(0);
		button.addActionListener(this);
		contentPane.add(button);

		button = new JButton("Merge To Master");
		button.setActionCommand("merge master");
		button.setHorizontalAlignment(0);
		button.addActionListener(this);
		contentPane.add(button);

		button = new JButton("Split Plugin");
		button.setActionCommand("split plugin");
		button.setHorizontalAlignment(0);
		button.addActionListener(this);
		contentPane.add(button);

		button = new JButton("Compare Plugins");
		button.setActionCommand("compare plugins");
		button.setHorizontalAlignment(0);
		button.addActionListener(this);
		contentPane.add(button);

		button = new JButton("Display/Copy");
		button.setActionCommand("display records");
		button.setHorizontalAlignment(0);
		button.addActionListener(this);
		contentPane.add(button);

		button = new JButton("Edit Description");
		button.setActionCommand("edit description");
		button.setHorizontalAlignment(0);
		button.addActionListener(this);
		contentPane.add(button);

		button = new JButton("Create Patch");
		button.setActionCommand("create patch");
		button.setHorizontalAlignment(0);
		button.addActionListener(this);
		contentPane.add(button);

		button = new JButton("Apply Patch");
		button.setActionCommand("apply patch");
		button.setHorizontalAlignment(0);
		button.addActionListener(this);
		contentPane.add(button);

		button = new JButton("Convert to Master");
		button.setActionCommand("convert master");
		button.setHorizontalAlignment(0);
		button.addActionListener(this);
		contentPane.add(button);

		button = new JButton("Convert to Plugin");
		button.setActionCommand("convert plugin");
		button.setHorizontalAlignment(0);
		button.addActionListener(this);
		contentPane.add(button);

		button = new JButton("Edit Master List");
		button.setActionCommand("edit master list");
		button.setHorizontalAlignment(0);
		button.addActionListener(this);
		contentPane.add(button);

		button = new JButton("Create Silent Voice Files");
		button.setActionCommand("generate responses");
		button.setHorizontalAlignment(0);
		button.addActionListener(this);
		contentPane.add(button);

		button = new JButton("Clean Plugin");
		button.setActionCommand("clean plugin");
		button.setHorizontalAlignment(0);
		button.addActionListener(this);
		contentPane.add(button);

		button = new JButton("Move Worldspaces");
		button.setActionCommand("move worldspaces");
		button.setHorizontalAlignment(0);
		button.addActionListener(this);
		contentPane.add(button);

		button = new JButton("Set Directory");
		button.setActionCommand("set directory");
		button.setHorizontalAlignment(0);
		button.addActionListener(this);
		contentPane.add(button);

		setContentPane(contentPane);

		JMenuBar menuBar = new JMenuBar();
		menuBar.setOpaque(true);
		menuBar.setBackground(new Color(230, 230, 230));

		JMenu menu = new JMenu("File");
		menu.setMnemonic(77);

		JMenuItem menuItem = new JMenuItem("Exit");
		menuItem.setActionCommand("exit");
		menuItem.addActionListener(this);
		menu.add(menuItem);

		menuBar.add(menu);

		menu = new JMenu("Help");
		menu.setMnemonic(72);

		menuItem = new JMenuItem("About");
		menuItem.setActionCommand("about");
		menuItem.addActionListener(this);
		menu.add(menuItem);

		menuBar.add(menu);

		setJMenuBar(menuBar);

		addWindowListener(new MainWindowListener());
	}

	public void actionPerformed(ActionEvent ae)
	{
		try
		{
			String action = ae.getActionCommand();
			if (Main.debugMode)
			{
				System.out.printf("There are " + Runtime.getRuntime().freeMemory() + " bytes available\n", new Object[0]);
			}

			if (action.equals("merge plugins"))
				mergePlugins();
			else if (action.equals("merge master"))
				mergeToMaster();
			else if (action.equals("split plugin"))
				splitRecords();
			else if (action.equals("compare plugins"))
				comparePlugins();
			else if (action.equals("display records"))
				displayRecords();
			else if (action.equals("convert master"))
				convertToMaster();
			else if (action.equals("convert plugin"))
				convertToPlugin();
			else if (action.equals("create patch"))
				createPatch();
			else if (action.equals("apply patch"))
				applyPatch();
			else if (action.equals("edit master list"))
				editMasterList();
			else if (action.equals("edit description"))
				editDescription();
			else if (action.equals("generate responses"))
				generateResponses();
			else if (action.equals("clean plugin"))
				cleanPlugin();
			else if (action.equals("move worldspaces"))
				moveWorldspaces();
			else if (action.equals("set directory"))
				setDirectory();
			else if (action.equals("exit"))
				exitProgram();
			else if (action.equals("about"))
			{
				aboutTES4Plugin();
			}

			Main.pluginSpill.reset();
		}
		catch (Throwable exc)
		{
			Main.logException("Exception while processing action event", exc);
		}
	}

	private void applyPatch()
	{
		JFileChooser chooser = new JFileChooser(Main.pluginDirectory);
		chooser.setDialogTitle("Select Plugin File");
		chooser.setFileFilter(new PluginFileFilter(true, true, false));
		if (chooser.showOpenDialog(this) != 0)
		{
			return;
		}
		File pluginFile = chooser.getSelectedFile();

		chooser = new JFileChooser(Main.pluginDirectory);
		chooser.setDialogTitle("Select Patch File");
		chooser.setFileFilter(new PluginFileFilter(false, false, true));
		if (chooser.showOpenDialog(this) != 0)
		{
			return;
		}
		File patchFile = chooser.getSelectedFile();

		ApplyPatchTask.applyPatch(this, pluginFile, patchFile);
	}

	private void cleanPlugin()
	{
		JFileChooser chooser = new JFileChooser(Main.pluginDirectory);
		chooser.setDialogTitle("Select Plugin File");
		chooser.setFileFilter(new PluginFileFilter(true, true, false));
		if (chooser.showOpenDialog(this) != 0)
		{
			return;
		}
		File pluginFile = chooser.getSelectedFile();

		CleanTask.cleanPlugin(this, pluginFile);
	}

	private void comparePlugins()
	{
		JFileChooser chooser = new JFileChooser(Main.pluginDirectory);
		chooser.setDialogTitle("Select First File");
		chooser.setFileFilter(new PluginFileFilter(true, true, false));
		if (chooser.showOpenDialog(this) != 0)
		{
			return;
		}
		File pluginFileA = chooser.getSelectedFile();

		chooser.setDialogTitle("Select Second File");
		if (chooser.showOpenDialog(this) != 0)
		{
			return;
		}
		File pluginFileB = chooser.getSelectedFile();

		PluginNode pluginNodeA = CreateTreeTask.createTree(this, pluginFileA);
		if (pluginNodeA != null)
		{
			PluginNode pluginNodeB = CreateTreeTask.createTree(this, pluginFileB);
			if ((pluginNodeB != null) && (CompareTask.comparePlugins(this, pluginNodeA, pluginNodeB)))
				CompareDialog.showDialog(this, pluginFileA, pluginFileB, pluginNodeA, pluginNodeB);
		}
	}

	private void convertToMaster()
	{
		JFileChooser chooser = new JFileChooser(Main.pluginDirectory);
		chooser.setDialogTitle("Select Plugin File");
		chooser.setFileFilter(new PluginFileFilter(false));
		if (chooser.showOpenDialog(this) != 0)
		{
			return;
		}
		File pluginFile = chooser.getSelectedFile();

		String inputName = pluginFile.getName();
		int sep = inputName.lastIndexOf('.');
		if (sep <= 0)
		{
			JOptionPane.showMessageDialog(this, "'" + inputName + "' is not a valid plugin file name", "Error", 0);
			return;
		}

		String outputName = String.format("%s%s%s.esm", new Object[]
		{ pluginFile.getParent(), Main.fileSeparator, inputName.substring(0, sep) });
		File masterFile = new File(outputName);

		if (masterFile.exists())
		{
			int selection = JOptionPane.showConfirmDialog(this, "'" + masterFile.getName()
					+ "' already exists.  Do you want to overwrite it?", "File exists", 0);
			if (selection != 0)
			{
				return;
			}
			masterFile.delete();
		}

		ConvertTask.convertFile(this, pluginFile, masterFile);
	}

	private void convertToPlugin()
	{
		JFileChooser chooser = new JFileChooser(Main.pluginDirectory);
		chooser.setDialogTitle("Select Master File");
		chooser.setFileFilter(new PluginFileFilter(true));
		if (chooser.showOpenDialog(this) != 0)
		{
			return;
		}
		File masterFile = chooser.getSelectedFile();

		String inputName = masterFile.getName();
		int sep = inputName.lastIndexOf('.');
		if (sep <= 0)
		{
			JOptionPane.showMessageDialog(this, "'" + inputName + "' is not a valid master file name", "Error", 0);
			return;
		}

		String outputName = String.format("%s%s%s.esp", new Object[]
		{ masterFile.getParent(), Main.fileSeparator, inputName.substring(0, sep) });
		File pluginFile = new File(outputName);

		if (pluginFile.exists())
		{
			int selection = JOptionPane.showConfirmDialog(this, "'" + pluginFile.getName()
					+ "' already exists.  Do you want to overwrite it?", "File exists", 0);
			if (selection != 0)
			{
				return;
			}
			pluginFile.delete();
		}

		ConvertTask.convertFile(this, masterFile, pluginFile);
	}

	private void createPatch()
	{
		JFileChooser chooser = new JFileChooser(Main.pluginDirectory);
		chooser.setDialogTitle("Select Original File");
		chooser.setFileFilter(new PluginFileFilter(true, true, false));
		if (chooser.showOpenDialog(this) != 0)
		{
			return;
		}
		File baseFile = chooser.getSelectedFile();

		chooser = new JFileChooser(Main.pluginDirectory);
		chooser.setDialogTitle("Select Modified File");
		chooser.setFileFilter(new PluginFileFilter(true, true, false));
		if (chooser.showOpenDialog(this) != 0)
		{
			return;
		}
		File modifiedFile = chooser.getSelectedFile();

		String patchName = baseFile.getName();
		int sep = patchName.lastIndexOf('.');
		if (sep <= 0)
		{
			JOptionPane.showMessageDialog(this, "'" + patchName + "' is not a valid plugin file name", "Error", 0);
			return;
		}

		patchName = String.format("%s%s%s.esu", new Object[]
		{ baseFile.getParent(), Main.fileSeparator, patchName.substring(0, sep) });
		File patchFile = new File(patchName);

		if (patchFile.exists())
		{
			int selection = JOptionPane.showConfirmDialog(this, "'" + patchFile.getName()
					+ "' already exists.  Do you want to overwrite it?", "File exists", 0);
			if (selection != 0)
			{
				return;
			}
			patchFile.delete();
		}

		CreatePatchTask.createPatch(this, baseFile, modifiedFile, patchFile);
	}

	private void displayRecords()
	{
		JFileChooser chooser = new JFileChooser(Main.pluginDirectory);
		chooser.setDialogTitle("Select Plugin File");
		chooser.setFileFilter(new PluginFileFilter());
		if (chooser.showOpenDialog(this) != 0)
		{
			return;
		}
		File pluginFile = chooser.getSelectedFile();

		PluginNode pluginNode = CreateTreeTask.createTree(this, pluginFile);
		if (pluginNode != null)
			DisplayDialog.showDialog(this, pluginFile, pluginNode);
	}

	private void editDescription()
	{
		JFileChooser chooser = new JFileChooser(Main.pluginDirectory);
		chooser.setDialogTitle("Select Plugin File");
		chooser.setFileFilter(new PluginFileFilter(true, true, false));
		if (chooser.showOpenDialog(this) != 0)
		{
			return;
		}
		File pluginFile = chooser.getSelectedFile();

		RandomAccessFile in = null;
		float version = 0.0F;
		String creator = null;
		String summary = null;
		boolean descriptionSet = false;
		try
		{
			if ((!pluginFile.exists()) || (!pluginFile.isFile()))
			{
				throw new IOException("'" + pluginFile.getName() + "' does not exist");
			}
			in = new RandomAccessFile(pluginFile, "r");
			PluginHeader header = new PluginHeader(pluginFile);
			header.read(in);
			version = header.getVersion();
			creator = header.getCreator();
			summary = header.getSummary();
			descriptionSet = true;
		}
		catch (PluginException exc)
		{
			JOptionPane.showMessageDialog(this, exc.getMessage(), "Format Error", 0);
		}
		catch (IOException exc)
		{
			JOptionPane.showMessageDialog(this, exc.getMessage(), "I/O Error", 0);
		}
		catch (Throwable exc)
		{
			Main.logException("Unable to read plugin header", exc);
		}
		try
		{
			if (in != null)
				in.close();
		}
		catch (IOException exc)
		{
			JOptionPane.showMessageDialog(this, exc.getMessage(), "I/O Error", 0);
		}

		if (!descriptionSet)
		{
			return;
		}

		PluginInfo pluginInfo = new PluginInfo(pluginFile.getName(), creator, summary, version);
		boolean descriptionUpdated = EditDialog.showDialog(this, pluginInfo);

		if (descriptionUpdated)
			EditTask.editFile(this, pluginFile, pluginInfo);
	}

	private void editMasterList()
	{
		JFileChooser chooser = new JFileChooser(Main.pluginDirectory);
		chooser.setDialogTitle("Select Plugin File");
		chooser.setFileFilter(new PluginFileFilter(true, true, false));
		if (chooser.showOpenDialog(this) != 0)
		{
			return;
		}
		File pluginFile = chooser.getSelectedFile();

		Plugin plugin = LoadTask.loadPlugin(this, pluginFile);
		if (plugin != null)
			MasterDialog.showDialog(this, pluginFile, plugin);
	}

	private void generateResponses()
	{
		JFileChooser chooser = new JFileChooser(Main.pluginDirectory);
		chooser.setDialogTitle("Select Plugin File");
		chooser.setFileFilter(new PluginFileFilter(true, true, false));
		if (chooser.showOpenDialog(this) != 0)
		{
			return;
		}
		File pluginFile = chooser.getSelectedFile();

		GenerateTask.generateResponses(this, pluginFile);
	}

	private void mergePlugins()
	{
		String[] pluginNames = PluginDialog.showDialog(this);
		if (pluginNames == null)
		{
			return;
		}

		RandomAccessFile in = null;
		boolean descriptionSet = false;
		String creator = null;
		String summary = null;
		try
		{
			File pluginFile = new File(Main.pluginDirectory + Main.fileSeparator + pluginNames[0]);
			if ((!pluginFile.exists()) || (!pluginFile.isFile()))
			{
				throw new IOException("'" + pluginFile.getName() + "' does not exist");
			}
			in = new RandomAccessFile(pluginFile, "r");
			PluginHeader header = new PluginHeader(pluginFile);
			header.read(in);
			creator = header.getCreator();
			summary = header.getSummary();
			descriptionSet = true;
		}
		catch (PluginException exc)
		{
			JOptionPane.showMessageDialog(this, exc.getMessage(), "Format Error", 0);
		}
		catch (IOException exc)
		{
			JOptionPane.showMessageDialog(this, exc.getMessage(), "I/O Error", 0);
		}
		catch (Throwable exc)
		{
			Main.logException("Unable to read plugin header", exc);
		}
		try
		{
			if (in != null)
			{
				in.close();
				in = null;
			}
		}
		catch (IOException exc)
		{
			JOptionPane.showMessageDialog(this, exc.getMessage(), "I/O Error", 0);
		}

		if (!descriptionSet)
		{
			return;
		}

		PluginInfo pluginInfo = MergeDialog.showDialog(this, creator, summary);
		if (pluginInfo == null)
		{
			return;
		}

		File mergedFile = new File(Main.pluginDirectory + Main.fileSeparator + pluginInfo.getName());
		if (mergedFile.exists())
		{
			int selection = JOptionPane.showConfirmDialog(this, "'" + mergedFile.getName()
					+ "' already exists.  Do you want to overwrite it?", "File exists", 0);
			if (selection != 0)
			{
				return;
			}
			mergedFile.delete();
		}

		MergeTask.mergePlugins(this, pluginNames, pluginInfo);
	}

	private void mergeToMaster()
	{
		List<String[]> testVec = new ArrayList<String[]>();
		String[] reg1 =
		{ "0008DFD0", "Oblivion.ESM", "Tamriel", "NibenayBasinValleySubRegion06" };
		String[] reg2 =
		{ "0007B864", "Oblivion.ESM", "Tamriel", "CyrodiilWeatherRegion" };
		String[] reg3 =
		{ "0007B8CE", "Oblivion.ESM", "Tamriel", "TowerofFathisArenRegion" };
		testVec.add(reg1);
		testVec.add(reg2);
		testVec.add(reg3);

		JFileChooser chooser = new JFileChooser(Main.pluginDirectory);
		chooser.setDialogTitle("Select Master File");
		chooser.setFileFilter(new PluginFileFilter(true));
		if (chooser.showOpenDialog(this) != 0)
		{
			return;
		}
		File masterFile = chooser.getSelectedFile();

		chooser = new JFileChooser(Main.pluginDirectory);
		chooser.setDialogTitle("Select Plugin File");
		chooser.setFileFilter(new PluginFileFilter(false));
		if (chooser.showOpenDialog(this) != 0)
		{
			return;
		}
		File pluginFile = chooser.getSelectedFile();

		MergeTask.mergeToMaster(this, masterFile, pluginFile);
	}

	private void moveWorldspaces()
	{
		JFileChooser chooser = new JFileChooser(Main.pluginDirectory);
		chooser.setDialogTitle("Select Plugin File");
		chooser.setFileFilter(new PluginFileFilter(true, true, false));
		if (chooser.showOpenDialog(this) != 0)
		{
			return;
		}
		File pluginFile = chooser.getSelectedFile();

		int option = WorldspaceDialog.showDialog(this);
		if (option >= 0)
			WorldspaceTask.moveWorldspaces(this, pluginFile, option);
	}

	private void setDirectory()
	{
		JFileChooser chooser = new JFileChooser(Main.pluginDirectory);
		chooser.setDialogTitle("Select Plugin Directory");
		chooser.setFileFilter(new PluginDirectoryFilter());
		chooser.setFileSelectionMode(1);
		if (chooser.showDialog(this, "Select") == 0)
		{
			Main.pluginDirectory = chooser.getSelectedFile().getPath();
			Main.properties.setProperty("plugin.directory", Main.pluginDirectory);
		}
	}

	private void splitRecords()
	{
		JFileChooser chooser = new JFileChooser(Main.pluginDirectory);
		chooser.setDialogTitle("Select Plugin File to Split");
		chooser.setFileFilter(new PluginFileFilter(false));
		if (chooser.showOpenDialog(this) != 0)
		{
			return;
		}
		File pluginFile = chooser.getSelectedFile();

		PluginNode pluginNode = CreateTreeTask.createTree(this, pluginFile);
		if (pluginNode != null)
			SplitDialog.showDialog(this, pluginFile, pluginNode);
	}

	private void exitProgram()
	{
		if (!this.windowMinimized)
		{
			Point p = Main.mainWindow.getLocation();
			Main.properties.setProperty("window.main.position", p.x + "," + p.y);
		}

		Main.saveProperties();

		if (Main.pluginSpill != null)
		{
			try
			{
				Main.pluginSpill.close();
			}
			catch (IOException exc)
			{
				Main.logException("Unable to close spill file", exc);
			}

		}

		System.exit(0);
	}

	private void aboutTES4Plugin()
	{
		StringBuilder info = new StringBuilder(256);
		info.append("<html>TES4Gecko Version 15.2<br>");
		info.append("<br>Created by TeamGecko<ul><li>ScripterRon (Ron Hoffman)<li>KomodoDave (N David Brown)<li>SACarrow (Steven A Carrow)<li>dev_akm (Aubrey K McAuley)</ul><br>");

		info.append("See the included ReadMe file for usage details.<br>");

		info.append("<br>User name: ");
		info.append(System.getProperty("user.name"));

		info.append("<br>Home directory: ");
		info.append(System.getProperty("user.home"));

		info.append("<br><br>OS: ");
		info.append(System.getProperty("os.name"));

		info.append("<br>OS version: ");
		info.append(System.getProperty("os.version"));

		info.append("<br>OS patch level: ");
		info.append(System.getProperty("sun.os.patch.level"));

		info.append("<br><br>Java vendor: ");
		info.append(System.getProperty("java.vendor"));

		info.append("<br>Java version: ");
		info.append(System.getProperty("java.version"));

		info.append("<br>Java home directory: ");
		info.append(System.getProperty("java.home"));

		info.append("<br>Java class path: ");
		info.append(System.getProperty("java.class.path"));

		info.append("</html>");
		JOptionPane.showMessageDialog(this, info.toString(), "About Gecko", 1);
	}

	private class MainWindowListener extends WindowAdapter
	{
		public MainWindowListener()
		{
		}

		public void windowIconified(WindowEvent we)
		{
			MainWindow.this.windowMinimized = true;
		}

		public void windowDeiconified(WindowEvent we)
		{
			MainWindow.this.windowMinimized = false;
		}

		public void windowClosing(WindowEvent we)
		{
			MainWindow.this.exitProgram();
		}
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.MainWindow
 * JD-Core Version:    0.6.0
 */