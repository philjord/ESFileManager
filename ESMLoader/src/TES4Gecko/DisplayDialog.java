package TES4Gecko;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.zip.DataFormatException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

public class DisplayDialog extends DisplayPlugin implements ActionListener, TreeExpansionListener
{
	private Plugin plugin;

	private PluginNode pluginNode;

	private File pluginFile;

	private DefaultTreeModel pluginTreeModel;

	private JTree pluginTree;

	private boolean pluginModified = false;

	private int masterCount;

	private JTextField searchField;

	private FormInfo searchFormInfo;

	private JCheckBox copyReferencesField;

	private JButton savePluginBtn;

	private JButton saveClipboardBtn;

	private JButton saveClipboardAsBtn;

	private JButton clearClipboardBtn;

	private JButton findBtn;

	private JButton findNextBtn;

	private JLabel clipboardFileLabel;

	private int pluginHighFormID = 0;

	private int clipboardHighFormID = 0;

	private final String dumpDialogueHeader = "TES4GECKO DIALOGUE DUMP";

	private final String commentStart = "//";

	private final String masterModReportHeader = "TES4GECKO MASTER ALTERATION REPORT";

	private final String formIDReportHeader = "TES4GECKO FORM ID REPORT";

	private final String noDialogueStr = "[No dialogue]";

	private final String HELLOSeparatorStr = "ABOVE HERE ARE GREETINGS THAT CAN LINK TO OTHER GREETINGS";

	private List<PluginRecord> raceList = null;

	private MouseListener mlClipboard;

	private MouseListener mlPlugin;

	public DisplayDialog(JFrame parent, File pluginFile, PluginNode pluginNode)
	{
		super(parent, "Display " + pluginFile.getName());

		this.pluginFile = pluginFile;
		this.pluginNode = pluginNode;
		this.plugin = pluginNode.getPlugin();

		List<FormInfo> formList = this.plugin.getFormList();

		pluginNode.setFormAdjust(new FormAdjust());

		List<String> masterList = this.plugin.getMasterList();
		List<String> clipboardMasterList = new ArrayList<String>(this.plugin.getMasterList());
		this.masterCount = masterList.size();
		String pluginName = pluginFile.getName();
		if (this.plugin.isMaster())
		{
			clipboardMasterList.add(pluginName);
		}

		this.clipboardFile = new File(pluginFile.getParent() + Main.fileSeparator + "Gecko Clipboard.esp");
		this.clipboard = new Plugin(this.clipboardFile, this.plugin.getCreator(), this.plugin.getSummary(), clipboardMasterList);
		this.clipboard.setVersion(this.plugin.getVersion());
		this.clipboard.createInitialGroups();

		int formCount = 0;
		for (FormInfo formInfo : formList)
		{
			if (formInfo.getFormID() >>> 24 >= this.masterCount)
			{
				formCount++;
			}
		}
		this.pluginHighFormID = highestFormID(this.plugin);
		this.clipboardHighFormID = this.pluginHighFormID;

		String highFormID = String.format("%08X", new Object[]
		{ Integer.valueOf(this.pluginHighFormID) });
		JLabel countLabel = new JLabel("<html>Plugin record count: " + formList.size() + "<br>Plugin form ID count: " + formCount + " ["
				+ String.format("%X", new Object[]
				{ Integer.valueOf(formCount) }) + " hex]" + "<br>Plugin high form ID (hex): " + highFormID + "</html>");
		JPanel countPane = new JPanel();
		countPane.setBackground(Main.backgroundColor);
		countPane.setBorder(BorderFactory.createEtchedBorder(Color.WHITE, Color.BLACK));
		countPane.add(countLabel);
		countPane.setPreferredSize(new Dimension(200, 30));

		Object[] masterNames = new Object[2];
		masterNames[0] = "Index";
		masterNames[1] = "Master";

		Object[][] masterData = new Object[this.masterCount][2];
		for (int i = 0; i < this.masterCount; i++)
		{
			masterData[i][0] = String.format("%02X", new Object[]
			{ Integer.valueOf(i) });
			masterData[i][1] = masterList.get(i);
		}

		PluginColorMap.setColorMap(masterList.size());

		JTable masterTable = new JTable(masterData, masterNames);
		masterTable.setColumnSelectionAllowed(false);
		masterTable.setRowSelectionAllowed(false);
		masterTable.getColumnModel().getColumn(0).setMaxWidth(45);
		masterTable.setAutoResizeMode(3);
		masterTable.setPreferredScrollableViewportSize(new Dimension(200, masterTable.getRowHeight() * Math.max(this.masterCount, 1)));
		masterTable.setDefaultRenderer(Object.class, new PluginColorTableRenderer());

		JScrollPane masterScrollPane = new JScrollPane(masterTable);
		masterScrollPane.getViewport().setBackground(Main.backgroundColor);

		JLabel searchLabel = new JLabel("General Search");
		searchLabel.setHorizontalAlignment(0);
		searchLabel.setAlignmentX(0.5F);

		this.searchField = new JTextField(20);
		this.searchField.setActionCommand("find editor id");
		this.searchField.addActionListener(this);

		JPanel buttonPane = new JPanel();
		buttonPane.setBackground(Main.backgroundColor);
		buttonPane.add(Box.createGlue());

		this.findBtn = new JButton("Find");
		this.findBtn.setActionCommand("find editor id");
		this.findBtn.addActionListener(this);
		buttonPane.add(this.findBtn);

		buttonPane.add(Box.createHorizontalStrut(10));

		this.findNextBtn = new JButton("Find Next");
		this.findNextBtn.setActionCommand("find next editor id");
		this.findNextBtn.addActionListener(this);
		buttonPane.add(this.findNextBtn);

		this.searchField.addKeyListener(new KeyAdapter()
		{
			public void keyReleased(KeyEvent evt)
			{
				int key = evt.getKeyCode();
				if (key != 10)
				{
					DisplayDialog.this.findNextBtn.setEnabled(false);
				}
				else
				{
					DisplayDialog.this.findBtn.doClick();
				}
			}
		});
		buttonPane.add(Box.createGlue());

		JRadioButton editIDBtn = new JRadioButton("Editor ID", true);
		JRadioButton formIDBtn = new JRadioButton("Form ID", false);
		JRadioButton nameIDBtn = new JRadioButton("Item Name", false);
		JRadioButton XYCoordBtn = new JRadioButton("XY Coordinates", false);
		JRadioButton ownerBtn = new JRadioButton("Ownership", false);
		JRadioButton refBaseBtn = new JRadioButton("Ref Base ID", false);
		JRadioButton responseTextBtn = new JRadioButton("Response Text", false);
		JRadioButton scriptTextBtn = new JRadioButton("Script Text", false);
		JRadioButton questRefBtn = new JRadioButton("Quest Ref ID", false);
		JRadioButton landTexBtn = new JRadioButton("Land Tex ID", false);
		formIDBtn.setBackground(Main.backgroundColor);
		editIDBtn.setBackground(Main.backgroundColor);
		nameIDBtn.setBackground(Main.backgroundColor);
		XYCoordBtn.setBackground(Main.backgroundColor);
		ownerBtn.setBackground(Main.backgroundColor);
		refBaseBtn.setBackground(Main.backgroundColor);
		responseTextBtn.setBackground(Main.backgroundColor);
		scriptTextBtn.setBackground(Main.backgroundColor);
		questRefBtn.setBackground(Main.backgroundColor);
		landTexBtn.setBackground(Main.backgroundColor);
		ButtonGroup bgroup = new ButtonGroup();
		editIDBtn.setActionCommand("set editor id search");
		editIDBtn.addActionListener(this);
		bgroup.add(editIDBtn);
		formIDBtn.setActionCommand("set form id search");
		formIDBtn.addActionListener(this);
		bgroup.add(formIDBtn);
		nameIDBtn.setActionCommand("set name id search");
		nameIDBtn.addActionListener(this);
		bgroup.add(nameIDBtn);
		XYCoordBtn.setActionCommand("set XY coordinate search");
		XYCoordBtn.addActionListener(this);
		bgroup.add(XYCoordBtn);
		ownerBtn.setActionCommand("set owner id search");
		ownerBtn.addActionListener(this);
		bgroup.add(ownerBtn);
		refBaseBtn.setActionCommand("set ref base id search");
		refBaseBtn.addActionListener(this);
		bgroup.add(refBaseBtn);
		responseTextBtn.setActionCommand("set response text search");
		responseTextBtn.addActionListener(this);
		bgroup.add(responseTextBtn);
		scriptTextBtn.setActionCommand("set script text search");
		scriptTextBtn.addActionListener(this);
		bgroup.add(scriptTextBtn);
		questRefBtn.setActionCommand("set quest ref id search");
		questRefBtn.addActionListener(this);
		bgroup.add(questRefBtn);
		landTexBtn.setActionCommand("set land tex id search");
		landTexBtn.addActionListener(this);
		bgroup.add(landTexBtn);

		JPanel searchChoicePane = new JPanel(new GridLayout(5, 2));
		searchChoicePane.setBackground(Main.backgroundColor);
		searchChoicePane.add(editIDBtn);
		searchChoicePane.add(formIDBtn);
		searchChoicePane.add(nameIDBtn);
		searchChoicePane.add(XYCoordBtn);
		searchChoicePane.add(ownerBtn);
		searchChoicePane.add(refBaseBtn);
		searchChoicePane.add(responseTextBtn);
		searchChoicePane.add(scriptTextBtn);
		searchChoicePane.add(questRefBtn);
		searchChoicePane.add(landTexBtn);

		JPanel searchFieldPane = new JPanel();
		searchFieldPane.setLayout(new BoxLayout(searchFieldPane, 0));
		searchFieldPane.setBackground(Main.backgroundColor);
		searchFieldPane.add(Box.createHorizontalStrut(7));
		searchFieldPane.add(this.searchField);
		searchFieldPane.add(Box.createHorizontalStrut(7));

		JPanel searchPane = new JPanel();
		searchPane.setLayout(new BoxLayout(searchPane, 1));
		searchPane.setBackground(Main.backgroundColor);
		searchPane.setBorder(BorderFactory.createEtchedBorder(Color.WHITE, Color.BLACK));
		searchPane.add(searchLabel);
		searchPane.add(searchChoicePane);
		searchPane.add(searchFieldPane);
		searchPane.add(buttonPane);
		searchPane.setMaximumSize(new Dimension(200, 80));

		this.copyReferencesField = new JCheckBox("Copy referenced items", this.copyReferences);
		this.copyReferencesField.setBackground(Main.backgroundColor);
		this.copyReferencesField.setActionCommand("copy references");
		this.copyReferencesField.addActionListener(this);
		JPanel copyReferencesPanel = new JPanel(new GridLayout(2, 1));
		copyReferencesPanel.setBackground(Main.backgroundColor);
		copyReferencesPanel.add(this.copyReferencesField);

		JPanel sidePane = new JPanel();
		sidePane.setLayout(new BoxLayout(sidePane, 1));
		sidePane.setBackground(Main.backgroundColor);
		sidePane.add(Box.createGlue());
		sidePane.add(countPane);
		sidePane.add(Box.createVerticalStrut(25));
		sidePane.add(masterScrollPane);
		sidePane.add(Box.createVerticalStrut(25));
		sidePane.add(searchPane);
		sidePane.add(Box.createVerticalStrut(25));
		sidePane.add(Box.createVerticalStrut(25));
		sidePane.add(copyReferencesPanel);
		sidePane.add(Box.createGlue());

		this.pluginTreeModel = new DefaultTreeModel(pluginNode);
		this.pluginTree = new JTree(this.pluginTreeModel);
		this.pluginTree.setCellRenderer(new DisplayCellRenderer());
		this.pluginTree.setScrollsOnExpand(true);
		this.pluginTree.setExpandsSelectedPaths(true);
		this.pluginTree.addTreeExpansionListener(this);
		this.pluginTree.setLargeModel(true);

		JScrollPane pluginScrollPane = new JScrollPane(this.pluginTree);
		pluginScrollPane.setHorizontalScrollBarPolicy(32);
		pluginScrollPane.setVerticalScrollBarPolicy(22);
		pluginScrollPane.setPreferredSize(new Dimension(300, 500));

		JPanel labelPane = new JPanel();
		labelPane.setBackground(Main.backgroundColor);
		labelPane.add(new JLabel(pluginFile.getName()));

		buttonPane = new JPanel(new GridLayout(3, 2, 5, 5));
		buttonPane.setBackground(Main.backgroundColor);

		JButton button = new JButton("Toggle Ignore");
		button.setActionCommand("toggle ignore");
		button.addActionListener(this);
		buttonPane.add(button);

		button = new JButton("Copy to Clipboard");
		button.setActionCommand("copy record");
		button.addActionListener(this);
		buttonPane.add(button);

		button = new JButton("Display Subrecord");
		button.setActionCommand("display subrecord");
		button.addActionListener(this);
		buttonPane.add(button);

		button = new JButton("Delete Subrecord");
		button.setActionCommand("delete subrecord");
		button.addActionListener(this);
		buttonPane.add(button);

		button = new JButton("Display Subrecord As Bytes");
		button.setActionCommand("display subrecord as bytes");
		button.addActionListener(this);
		buttonPane.add(button);

		this.savePluginBtn = new JButton("Save Plugin");
		this.savePluginBtn.setActionCommand("save plugin");
		this.savePluginBtn.addActionListener(this);
		buttonPane.add(this.savePluginBtn);

		JPanel pluginPane = new JPanel();
		pluginPane.setLayout(new BoxLayout(pluginPane, 1));
		pluginPane.setBackground(Main.backgroundColor);
		pluginPane.setBorder(BorderFactory.createEtchedBorder(Color.WHITE, Color.BLACK));
		pluginPane.add(labelPane);
		pluginPane.add(pluginScrollPane);
		pluginPane.add(Box.createVerticalStrut(10));
		pluginPane.add(buttonPane);

		setPluginModified(false);

		PluginNode rootNode = new PluginNode(this.clipboard);
		List<PluginGroup> groupList = this.clipboard.getGroupList();
		for (PluginGroup group : groupList)
		{
			GroupNode groupNode = new GroupNode(group);
			rootNode.insert(groupNode);
		}

		this.clipboardTreeModel = new DefaultTreeModel(rootNode);
		this.clipboardTree = new JTree(this.clipboardTreeModel);
		this.clipboardTree.setScrollsOnExpand(true);
		this.clipboardTree.setSelectionModel(null);
		this.clipboardTree.addTreeExpansionListener(this);

		JScrollPane clipboardScrollPane = new JScrollPane(this.clipboardTree);
		clipboardScrollPane.setHorizontalScrollBarPolicy(32);
		clipboardScrollPane.setVerticalScrollBarPolicy(22);
		clipboardScrollPane.setPreferredSize(new Dimension(300, 500));

		labelPane = new JPanel();
		labelPane.setBackground(Main.backgroundColor);
		this.clipboardFileLabel = new JLabel(this.clipboardFile.getName());
		labelPane.add(this.clipboardFileLabel);

		buttonPane = new JPanel(new GridLayout(3, 2, 5, 5));
		buttonPane.setBackground(Main.backgroundColor);

		this.saveClipboardBtn = new JButton("Save Clipboard To Default");
		this.saveClipboardBtn.setActionCommand("save clipboard to default");
		this.saveClipboardBtn.addActionListener(this);
		buttonPane.add(this.saveClipboardBtn);
		this.clearClipboardBtn = new JButton("Clear Clipboard");
		this.clearClipboardBtn.setActionCommand("clear clipboard");
		this.clearClipboardBtn.addActionListener(this);
		buttonPane.add(this.clearClipboardBtn);
		this.saveClipboardAsBtn = new JButton("Save Clipboard To New");
		this.saveClipboardAsBtn.setActionCommand("save clipboard to new");
		this.saveClipboardAsBtn.addActionListener(this);
		buttonPane.add(this.saveClipboardAsBtn);
		buttonPane.add(Box.createGlue());
		buttonPane.add(Box.createGlue());
		buttonPane.add(Box.createGlue());

		JPanel clipboardPane = new JPanel();
		clipboardPane.setLayout(new BoxLayout(clipboardPane, 1));
		clipboardPane.setBackground(Main.backgroundColor);
		clipboardPane.setBorder(BorderFactory.createEtchedBorder(Color.WHITE, Color.BLACK));
		clipboardPane.add(labelPane);
		clipboardPane.add(clipboardScrollPane);
		clipboardPane.add(Box.createVerticalStrut(10));
		clipboardPane.add(buttonPane);

		setClipboardModified(false);
		setClipboardCleared(true);

		JPanel treePane = new JPanel();
		treePane.setLayout(new BoxLayout(treePane, 0));
		treePane.setBackground(Main.backgroundColor);
		treePane.add(sidePane);
		treePane.add(Box.createHorizontalStrut(15));
		treePane.add(pluginPane);
		treePane.add(Box.createHorizontalStrut(15));
		treePane.add(clipboardPane);

		buttonPane = new JPanel();
		buttonPane.setBackground(Main.backgroundColor);

		button = new JButton("Done");
		button.setActionCommand("done");
		button.addActionListener(this);
		buttonPane.add(button);

		JPanel contentPane = new JPanel();
		contentPane.setLayout(new BoxLayout(contentPane, 1));
		contentPane.setOpaque(true);
		contentPane.setBackground(Main.backgroundColor);
		contentPane.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		contentPane.add(treePane);
		contentPane.add(Box.createVerticalStrut(15));
		contentPane.add(buttonPane);
		contentPane.setPreferredSize(new Dimension(975, 650));
		setContentPane(contentPane);

		addWindowListener(new DialogWindowListener());

		this.mlClipboard = new MouseAdapter()
		{
			public void mouseReleased(MouseEvent e)
			{
				if ((e.isAltDown()) || (e.isAltGraphDown()) || (e.isControlDown()) || (e.isShiftDown()))
					return;
				if (e.getButton() != 3)
					return;
				int selRow = DisplayDialog.this.clipboardTree.getRowForLocation(e.getX(), e.getY());
				if (selRow == -1)
					return;
				TreePath selPath = DisplayDialog.this.clipboardTree.getPathForLocation(e.getX(), e.getY());
				if ((selPath.getLastPathComponent() instanceof PluginNode))
				{
					PluginNode plNode = (PluginNode) selPath.getLastPathComponent();
					if (plNode == null)
						return;
					Plugin thisPlugin = (Plugin) plNode.getUserObject();
					if (thisPlugin == null)
						return;
					String actionString = "Popup:Clipboard:PLUG:";
					JPopupMenu popup = new JPopupMenu();
					JMenuItem item = new JMenuItem("Prepare for Lip Synch");
					item.addActionListener(DisplayDialog.this);
					item.setActionCommand(actionString + ":PrepareLipSynch");
					popup.add(item);
					popup.show(DisplayDialog.this.clipboardTree, e.getX(), e.getY());
				}
				if ((selPath.getLastPathComponent() instanceof GroupNode))
				{
					String recordType = "NotHandled";
					GroupNode groupNode = (GroupNode) selPath.getLastPathComponent();
					if (groupNode == null)
						return;
					PluginGroup pluginGroup = (PluginGroup) groupNode.getUserObject();
					if (pluginGroup == null)
						return;
					if (pluginGroup.getGroupType() == 0)
						recordType = pluginGroup.getGroupRecordType();
					if (pluginGroup.getGroupType() == 7)
					{
						recordType = "INFO";
					}
					String actionString = "Popup:Clipboard:GRUP:" + recordType;
					if (recordType.equals("DIAL"))
					{
						JPopupMenu popup = new JPopupMenu();
						JMenuItem item = new JMenuItem("Remove selected conditions from responses");
						item.addActionListener(DisplayDialog.this);
						item.setActionCommand(actionString + ":RemoveCondition");
						popup.add(item);
						popup.addSeparator();
						item = new JMenuItem("Remove extraneous quest references");
						item.addActionListener(DisplayDialog.this);
						item.setActionCommand(actionString + ":RemoveExcessQSTIs");
						popup.add(item);
						popup.show(DisplayDialog.this.clipboardTree, e.getX(), e.getY());
					}
				}
				if ((selPath.getLastPathComponent() instanceof RecordNode))
				{
					RecordNode recNode = (RecordNode) selPath.getLastPathComponent();
					if (recNode == null)
						return;
					PluginRecord pluginRec = (PluginRecord) recNode.getUserObject();
					if (pluginRec == null)
						return;
					String pluginRecType = pluginRec.getRecordType();
					String actionString = "Popup:Clipboard:" + pluginRec.getRecordType() + ":" + pluginRec.getFormID();
					if (pluginRecType.equals("QUST"))
					{
						JPopupMenu popup = new JPopupMenu();
						JMenuItem item = new JMenuItem("Change quest editor ID");
						item.addActionListener(DisplayDialog.this);
						item.setActionCommand(actionString + ":ChangeEditorID");
						popup.add(item);
						popup.addSeparator();
						item = new JMenuItem("Change quest form ID (Quest only)");
						item.addActionListener(DisplayDialog.this);
						item.setActionCommand(actionString + ":ChangeFormIDs:QuestOnly");
						popup.add(item);
						popup.addSeparator();
						item = new JMenuItem("Change quest form ID (with related INFOS)");
						item.addActionListener(DisplayDialog.this);
						item.setActionCommand(actionString + ":ChangeFormIDs:RelatedInfos");
						popup.add(item);
						popup.addSeparator();
						item = new JMenuItem("Change quest form ID (with INFOS & unshared DIALs)");
						item.addActionListener(DisplayDialog.this);
						item.setActionCommand(actionString + ":ChangeFormIDs:UnsharedDials");
						popup.add(item);
						popup.show(DisplayDialog.this.clipboardTree, e.getX(), e.getY());
					}
					if (pluginRecType.equals("REGN"))
					{
						JPopupMenu popup = new JPopupMenu();
						JMenuItem item = new JMenuItem("Change region editor ID");
						item.addActionListener(DisplayDialog.this);
						item.setActionCommand(actionString + ":ChangeEditorID");
						popup.add(item);
						popup.show(DisplayDialog.this.clipboardTree, e.getX(), e.getY());
					}
					if (pluginRecType.equals("WRLD"))
					{
						JPopupMenu popup = new JPopupMenu();
						JMenuItem item = new JMenuItem("Change worldspace editor ID");
						item.addActionListener(DisplayDialog.this);
						item.setActionCommand(actionString + ":ChangeEditorID");
						popup.add(item);
						popup.addSeparator();
						item = new JMenuItem("Change worldspace form IDs");
						item.addActionListener(DisplayDialog.this);
						item.setActionCommand(actionString + ":ChangeFormIDs");
						popup.add(item);
						popup.show(DisplayDialog.this.clipboardTree, e.getX(), e.getY());
					}
					if ((pluginRecType.equals("CELL")) || (pluginRecType.equals("REFR")) || (pluginRecType.equals("ACRE"))
							|| (pluginRecType.equals("ACHR")))
					{
						if (pluginRec.hasSubrecordOfType("EDID"))
						{
							String recDesc = "";
							if (pluginRecType.equals("CELL"))
								recDesc = "cell";
							if (pluginRecType.equals("REFR"))
								recDesc = "reference";
							if (pluginRecType.equals("ACRE"))
								recDesc = "NPC reference";
							if (pluginRecType.equals("ACHR"))
								recDesc = "creature reference";
							JPopupMenu popup = new JPopupMenu();
							JMenuItem item = new JMenuItem("Change " + recDesc + " editor ID");
							item.addActionListener(DisplayDialog.this);
							item.setActionCommand(actionString + ":ChangeEditorID");
							popup.add(item);
							popup.show(DisplayDialog.this.clipboardTree, e.getX(), e.getY());
						}
					}
				}
			}
		};
		this.clipboardTree.addMouseListener(this.mlClipboard);

		this.mlPlugin = new MouseAdapter()
		{
			public void mouseReleased(MouseEvent e)
			{
				if ((e.isAltDown()) || (e.isAltGraphDown()) || (e.isControlDown()) || (e.isShiftDown()))
					return;
				if (e.getButton() != 3)
					return;
				int selRow = DisplayDialog.this.pluginTree.getRowForLocation(e.getX(), e.getY());
				if (selRow == -1)
					return;
				TreePath selPath = DisplayDialog.this.pluginTree.getPathForLocation(e.getX(), e.getY());
				if ((selPath.getLastPathComponent() instanceof PluginNode))
				{
					PluginNode plNode = (PluginNode) selPath.getLastPathComponent();
					if (plNode == null)
						return;
					Plugin thisPlugin = (Plugin) plNode.getUserObject();
					if (thisPlugin == null)
						return;
					String actionString = "Popup:Plugin:PLUG:";
					JPopupMenu popup = new JPopupMenu();
					JMenuItem item = new JMenuItem("New Form ID Report");
					item.addActionListener(DisplayDialog.this);
					item.setActionCommand(actionString + ":FormIDReport");
					popup.add(item);
					popup.addSeparator();
					item = new JMenuItem("Master Alteration Report (append)");
					item.addActionListener(DisplayDialog.this);
					item.setActionCommand(actionString + ":MasterModReport:Append");
					popup.add(item);
					popup.addSeparator();
					item = new JMenuItem("Master Alteration Report (replace)");
					item.addActionListener(DisplayDialog.this);
					item.setActionCommand(actionString + ":MasterModReport:Replace");
					popup.add(item);
					popup.addSeparator();
					item = new JMenuItem("Toggle ignore on references with given base IDs");
					item.addActionListener(DisplayDialog.this);
					item.setActionCommand(actionString + ":ToggleRefs:" + selRow);
					popup.add(item);
					popup.addSeparator();
					item = new JMenuItem("Replace reference base IDs");
					item.addActionListener(DisplayDialog.this);
					item.setActionCommand(actionString + ":ReplaceBaseRefs:" + selRow);
					popup.add(item);
					popup.addSeparator();
					item = new JMenuItem("Replace landscape texture IDs");
					item.addActionListener(DisplayDialog.this);
					item.setActionCommand(actionString + ":ReplaceLTEXRefs:" + selRow);
					popup.add(item);
					popup.addSeparator();
					item = new JMenuItem("Execute NVIDIA/ATI \"fog fix\"");
					item.addActionListener(DisplayDialog.this);
					item.setActionCommand(actionString + ":FogFix:" + selRow);
					popup.add(item);
					popup.addSeparator();
					item = new JMenuItem("Change music type for all exterior cells");
					item.addActionListener(DisplayDialog.this);
					item.setActionCommand(actionString + ":ChangeMusic:" + selRow);
					popup.add(item);
					popup.addSeparator();
					item = new JMenuItem("Dump dialogue for entire plugin");
					item.addActionListener(DisplayDialog.this);
					item.setActionCommand(actionString + ":DumpPluginDialogue:" + selRow);
					popup.add(item);

					popup.show(DisplayDialog.this.pluginTree, e.getX(), e.getY());
				}
				if ((selPath.getLastPathComponent() instanceof GroupNode))
				{
					String recordType = "NotHandled";
					GroupNode groupNode = (GroupNode) selPath.getLastPathComponent();
					if (groupNode == null)
						return;
					PluginGroup pluginGroup = (PluginGroup) groupNode.getUserObject();
					if (pluginGroup == null)
						return;
					if (pluginGroup.getGroupType() == 0)
						recordType = pluginGroup.getGroupRecordType();
					if (pluginGroup.getGroupType() == 7)
						recordType = "INFO";
					if (pluginGroup.getGroupType() == 1)
						recordType = "WORLDSPACE";
					if (pluginGroup.getGroupType() == 6)
					{
						recordType = "CELLCONTENTS";
					}
					String actionString = "Popup:Plugin:GRUP:" + recordType;
					if (recordType.equals("DIAL"))
					{
						JPopupMenu popup = new JPopupMenu();
						JMenuItem item = new JMenuItem("Read dialogue from file");
						item.addActionListener(DisplayDialog.this);
						item.setActionCommand(actionString + ":ReadDialogue");
						popup.add(item);
						popup.addSeparator();
						item = new JMenuItem("Remove extraneous quest references");
						item.addActionListener(DisplayDialog.this);
						item.setActionCommand(actionString + ":RemoveExcessQSTIs");
						popup.add(item);
						popup.show(DisplayDialog.this.pluginTree, e.getX(), e.getY());
					}
					if (recordType.equals("CELL"))
					{
						JPopupMenu popup = new JPopupMenu();
						JMenuItem item = new JMenuItem("Show all interior cells");
						item.addActionListener(DisplayDialog.this);
						item.setActionCommand(actionString + ":ShowCells:" + selRow);
						popup.add(item);
						popup.addSeparator();
						item = new JMenuItem("Toggle ignore on references with given base IDs");
						item.addActionListener(DisplayDialog.this);
						item.setActionCommand(actionString + ":ToggleRefs:" + selRow);
						popup.add(item);
						popup.addSeparator();
						item = new JMenuItem("Replace reference base IDs");
						item.addActionListener(DisplayDialog.this);
						item.setActionCommand(actionString + ":ReplaceBaseRefs:" + selRow);
						popup.add(item);
						popup.show(DisplayDialog.this.pluginTree, e.getX(), e.getY());
					}
					if (recordType.equals("WORLDSPACE"))
					{
						JPopupMenu popup = new JPopupMenu();
						JMenuItem item = new JMenuItem("Show all exterior cells in worldspace");
						item.addActionListener(DisplayDialog.this);
						item.setActionCommand(actionString + ":ShowCells:" + selRow);
						popup.add(item);
						popup.addSeparator();
						item = new JMenuItem("Toggle ignore on references with given base IDs");
						item.addActionListener(DisplayDialog.this);
						item.setActionCommand(actionString + ":ToggleRefs:" + selRow);
						popup.add(item);
						popup.addSeparator();
						item = new JMenuItem("Replace reference base IDs");
						item.addActionListener(DisplayDialog.this);
						item.setActionCommand(actionString + ":ReplaceBaseRefs:" + selRow);
						popup.add(item);
						popup.addSeparator();
						item = new JMenuItem("Replace landscape texture IDs");
						item.addActionListener(DisplayDialog.this);
						item.setActionCommand(actionString + ":ReplaceLTEXRefs:" + selRow);
						popup.add(item);
						popup.show(DisplayDialog.this.pluginTree, e.getX(), e.getY());
					}
					if (recordType.equals("CELLCONTENTS"))
					{
						JPopupMenu popup = new JPopupMenu();
						JMenuItem item = new JMenuItem("Toggle ignore on references with given base IDs");
						item.addActionListener(DisplayDialog.this);
						item.setActionCommand(actionString + ":ToggleRefs:" + selRow);
						popup.add(item);
						popup.addSeparator();
						item = new JMenuItem("Replace reference base IDs");
						item.addActionListener(DisplayDialog.this);
						item.setActionCommand(actionString + ":ReplaceBaseRefs:" + selRow);
						popup.add(item);
						popup.addSeparator();
						item = new JMenuItem("Replace landscape texture IDs");
						item.addActionListener(DisplayDialog.this);
						item.setActionCommand(actionString + ":ReplaceLTEXRefs:" + selRow);
						popup.add(item);
						popup.show(DisplayDialog.this.pluginTree, e.getX(), e.getY());
					}
				}
				if ((selPath.getLastPathComponent() instanceof RecordNode))
				{
					RecordNode recNode = (RecordNode) selPath.getLastPathComponent();
					if (recNode == null)
						return;
					PluginRecord pluginRec = (PluginRecord) recNode.getUserObject();
					if (pluginRec == null)
						return;
					boolean isSelected = DisplayDialog.this.pluginTree.isPathSelected(selPath);
					String actionString = "Popup:Plugin:" + pluginRec.getRecordType() + ":" + pluginRec.getFormID();
					if (pluginRec.getRecordType().equals("NPC_"))
					{
						JPopupMenu popup = new JPopupMenu();
						JMenuItem item = new JMenuItem("Dump NPC dialogue to file (append)");
						item.addActionListener(DisplayDialog.this);
						item.setActionCommand(actionString + ":DumpDialogue:Append");
						popup.add(item);
						popup.addSeparator();
						item = new JMenuItem("Dump NPC dialogue to file (replace)");
						item.addActionListener(DisplayDialog.this);
						item.setActionCommand(actionString + ":DumpDialogue:Replace");
						popup.add(item);
						popup.show(DisplayDialog.this.pluginTree, e.getX(), e.getY());
					}
					if (pluginRec.getRecordType().equals("QUST"))
					{
						JPopupMenu popup = new JPopupMenu();
						String itemString = isSelected ? "Deselect" : "Select";
						itemString = itemString + " quest and associated INFOs";
						JMenuItem item = new JMenuItem(itemString);
						item.addActionListener(DisplayDialog.this);
						item.setActionCommand(actionString + ":SelectInfos:" + (isSelected ? "Deselect" : "Select"));
						popup.add(item);
						popup.addSeparator();
						item = new JMenuItem("Dump quest dialogue to file (append)");
						item.addActionListener(DisplayDialog.this);
						item.setActionCommand(actionString + ":DumpDialogue:Append");
						popup.add(item);
						popup.addSeparator();
						item = new JMenuItem("Dump Quest dialogue to file (replace)");
						item.addActionListener(DisplayDialog.this);
						item.setActionCommand(actionString + ":DumpDialogue:Replace");
						popup.add(item);

						popup.show(DisplayDialog.this.pluginTree, e.getX(), e.getY());
					}
					if (pluginRec.getRecordType().equals("REGN"))
					{
						JPopupMenu popup = new JPopupMenu();
						JMenuItem item = new JMenuItem("Toggle ignore on references with given base IDs");
						item.addActionListener(DisplayDialog.this);
						item.setActionCommand(actionString + ":ToggleRefs:");
						popup.add(item);
						popup.addSeparator();
						item = new JMenuItem("Replace reference base IDs");
						item.addActionListener(DisplayDialog.this);
						item.setActionCommand(actionString + ":ReplaceBaseRefs:" + selRow);
						popup.add(item);
						popup.addSeparator();
						item = new JMenuItem("Replace landscape texture IDs");
						item.addActionListener(DisplayDialog.this);
						item.setActionCommand(actionString + ":ReplaceLTEXRefs:" + selRow);
						popup.add(item);
						popup.show(DisplayDialog.this.pluginTree, e.getX(), e.getY());
					}
					if (pluginRec.getRecordType().equals("WRLD"))
					{
						JPopupMenu popup = new JPopupMenu();
						String itemString = isSelected ? "Deselect" : "Select";
						itemString = itemString + " worldspace and associated " + (isSelected ? "objects" : "regions");
						JMenuItem item = new JMenuItem(itemString);
						item.addActionListener(DisplayDialog.this);
						item.setActionCommand(actionString + ":SelectRegions:" + (isSelected ? "Deselect" : "Select"));
						popup.add(item);
						if (!isSelected)
						{
							popup.addSeparator();
							itemString = isSelected ? "Deselect" : "Select";
							itemString = itemString + " worldspace and associated " + (isSelected ? "objects" : "child WLRDs");
							item = new JMenuItem(itemString);
							item.addActionListener(DisplayDialog.this);
							item.setActionCommand(actionString + ":SelectWRLDs:" + (isSelected ? "Deselect" : "Select"));
							popup.add(item);
						}

						popup.show(DisplayDialog.this.pluginTree, e.getX(), e.getY());
					}
					if (pluginRec.getRecordType().equals("CELL"))
					{
						JPopupMenu popup = new JPopupMenu();
						if (pluginRec.hasSubrecordOfType("XCLC"))
						{
							if (selPath.getPath().length == 6)
							{
								GroupNode groupNode = (GroupNode) selPath.getPath()[2];
								if (groupNode == null)
									return;
								PluginGroup pluginGroup = (PluginGroup) groupNode.getUserObject();
								if (pluginGroup.getGroupType() != 1)
									return;
								String WSNodeStr = String.format("%08X", new Object[]
								{ Integer.valueOf(pluginGroup.getGroupParentID()) });
								String itemString = isSelected ? "Deselect" : "Select";
								itemString = itemString + " cell with persistent references";
								JMenuItem item = new JMenuItem(itemString);
								item.addActionListener(DisplayDialog.this);
								item.setActionCommand(actionString + ":SelectPersistentRefs:" + (isSelected ? "Deselect:" : "Select:")
										+ WSNodeStr);
								popup.add(item);
								popup.show(DisplayDialog.this.pluginTree, e.getX(), e.getY());
							}
						}
					}
				}
			}
		};
		this.pluginTree.addMouseListener(this.mlPlugin);

		if (Main.debugMode)
		{
			System.out.printf(pluginFile.getName() + ": Version " + this.plugin.getVersion() + " loaded with highest FormID used = "
					+ String.format("%08X", new Object[]
					{ Integer.valueOf(this.pluginHighFormID) }) + "\n", new Object[0]);
		}
	}

	public static void showDialog(JFrame parent, File pluginFile, PluginNode pluginNode)
	{
		DisplayDialog dialog = new DisplayDialog(parent, pluginFile, pluginNode);
		dialog.pack();
		dialog.setLocationRelativeTo(parent);
		dialog.setVisible(true);
	}

	public void actionPerformed(ActionEvent ae)
	{
		try
		{
			String action = ae.getActionCommand();
			if (action.startsWith("Popup"))
			{
				popupEventHandler(action);
				return;
			}
			if (action.equals("save plugin"))
			{
				if ((this.pluginModified) && (SaveTask.savePlugin(this, this.pluginFile, this.plugin)))
				{
					setPluginModified(false);

					this.pluginNode = new PluginNode(this.plugin);
					this.pluginNode.setFormAdjust(new FormAdjust());
					this.pluginNode.buildNodes(null);
					this.pluginTree.setModel(new DefaultTreeModel(this.pluginNode));
				}
			}
			else if (action.equals("save clipboard to default"))
			{
				if ((this.clipboardModified) && (SaveTask.savePlugin(this, this.clipboardFile, this.clipboard)))
				{
					setClipboardModified(false);
					validateTree(this.clipboardTree);
					this.clipboardFileLabel.setText(this.clipboardFile.getName());
				}
			}
			else if (action.equals("save clipboard to new"))
			{
				if (this.clipboardModified)
				{
					File saveFile = getClipboardSaveFile();
					if ((saveFile != null) && (SaveTask.savePlugin(this, saveFile, this.clipboard)))
					{
						setClipboardModified(false);
						validateTree(this.clipboardTree);

						((DefaultTreeModel) this.clipboardTree.getModel()).nodeStructureChanged((PluginNode) this.clipboardTree.getModel()
								.getRoot());
						this.clipboardFileLabel.setText(saveFile.getName());
					}
				}
			}
			else if (action.equals("clear clipboard"))
			{
				if (removeAllObjects(this.clipboard) != 0)
				{
					setClipboardModified(false);
					setClipboardCleared(true);
					this.clipboardHighFormID = this.pluginHighFormID;
					this.clipboard.setPluginFile(this.clipboardFile);
					validateTree(this.clipboardTree);

					((DefaultTreeModel) this.clipboardTree.getModel()).nodeStructureChanged((PluginNode) this.clipboardTree.getModel()
							.getRoot());
					this.clipboardFileLabel.setText(this.clipboardFile.getName());
				}
			}
			else if (action.equals("copy record"))
			{
				setCursor(Cursor.getPredefinedCursor(3));
				copyRecords(this.pluginTree);
				setCursor(Cursor.getPredefinedCursor(0));
				setClipboardModified(this.clipboardModified);
				if (this.clipboardModified)
				{
					setClipboardCleared(false);
				}
			}
			else if (action.equals("display subrecord"))
			{
				displaySubrecordData();
			}
			else if (action.equals("display subrecord as bytes"))
			{
				displaySubrecordDataAsBytes();
			}
			else if (action.equals("delete subrecord"))
			{
				deleteSubrecords();
			}
			else if (action.equals("toggle ignore"))
			{
				if (toggleIgnore(this.pluginTree))
				{
					setPluginModified(true);
				}
			}
			else if (action.equals("set form id search"))
			{
				this.findBtn.setActionCommand("find form id");
				this.searchField.setActionCommand("find form id");
				this.findNextBtn.setEnabled(false);
			}
			else if (action.equals("set editor id search"))
			{
				this.findBtn.setActionCommand("find editor id");
				this.searchField.setActionCommand("find editor id");
				this.findNextBtn.setEnabled(false);
				this.searchFormInfo = null;
				this.findNextBtn.setActionCommand("find next editor id");
			}
			else if (action.equals("set name id search"))
			{
				this.findBtn.setActionCommand("find name id");
				this.searchField.setActionCommand("find name id");
				this.findNextBtn.setEnabled(false);
				this.searchFormInfo = null;
				this.findNextBtn.setActionCommand("find next name id");
			}
			else if (action.equals("set owner id search"))
			{
				this.findBtn.setActionCommand("find owner id");
				this.searchField.setActionCommand("find owner id");
				this.findNextBtn.setEnabled(false);
				this.searchFormInfo = null;
				this.findNextBtn.setActionCommand("find next owner id");
			}
			else if (action.equals("set ref base id search"))
			{
				this.findBtn.setActionCommand("find ref base id");
				this.searchField.setActionCommand("find ref base id");
				this.findNextBtn.setEnabled(false);
				this.searchFormInfo = null;
				this.findNextBtn.setActionCommand("find next ref base id");
			}
			else if (action.equals("set XY coordinate search"))
			{
				this.findBtn.setActionCommand("find XY coordinates");
				this.searchField.setActionCommand("find XY coordinates");
				this.findNextBtn.setEnabled(false);
				this.searchFormInfo = null;
				this.findNextBtn.setActionCommand("find next XY coordinates");
			}
			else if (action.equals("set response text search"))
			{
				this.findBtn.setActionCommand("find response text");
				this.searchField.setActionCommand("find response text");
				this.findNextBtn.setEnabled(false);
				this.searchFormInfo = null;
				this.findNextBtn.setActionCommand("find next response text");
			}
			else if (action.equals("set script text search"))
			{
				this.findBtn.setActionCommand("find script text");
				this.searchField.setActionCommand("find script text");
				this.findNextBtn.setEnabled(false);
				this.searchFormInfo = null;
				this.findNextBtn.setActionCommand("find next script text");
			}
			else if (action.equals("set quest ref id search"))
			{
				this.findBtn.setActionCommand("find quest ref id");
				this.searchField.setActionCommand("find quest ref id");
				this.findNextBtn.setEnabled(false);
				this.searchFormInfo = null;
				this.findNextBtn.setActionCommand("find next quest ref id");
			}
			else if (action.equals("set land tex id search"))
			{
				this.findBtn.setActionCommand("find land tex id");
				this.searchField.setActionCommand("find land tex id");
				this.findNextBtn.setEnabled(false);
				this.searchFormInfo = null;
				this.findNextBtn.setActionCommand("find next land tex id");
			}
			else if (action.equals("find editor id"))
			{
				setCursor(Cursor.getPredefinedCursor(3));
				this.findNextBtn.setEnabled(findEditorID(false));
				setCursor(Cursor.getPredefinedCursor(0));
			}
			else if (action.equals("find next editor id"))
			{
				setCursor(Cursor.getPredefinedCursor(3));
				this.findNextBtn.setEnabled(findEditorID(true));
				setCursor(Cursor.getPredefinedCursor(0));
			}
			else if (action.equals("find name id"))
			{
				setCursor(Cursor.getPredefinedCursor(3));
				this.findNextBtn.setEnabled(findNameID(false));
				setCursor(Cursor.getPredefinedCursor(0));
			}
			else if (action.equals("find next name id"))
			{
				setCursor(Cursor.getPredefinedCursor(3));
				this.findNextBtn.setEnabled(findNameID(true));
				setCursor(Cursor.getPredefinedCursor(0));
			}
			else if (action.equals("find response text"))
			{
				setCursor(Cursor.getPredefinedCursor(3));
				this.findNextBtn.setEnabled(findResponseText(false));
				setCursor(Cursor.getPredefinedCursor(0));
			}
			else if (action.equals("find next response text"))
			{
				setCursor(Cursor.getPredefinedCursor(3));
				this.findNextBtn.setEnabled(findResponseText(true));
				setCursor(Cursor.getPredefinedCursor(0));
			}
			else if (action.equals("find script text"))
			{
				setCursor(Cursor.getPredefinedCursor(3));
				this.findNextBtn.setEnabled(findScriptText(false));
				setCursor(Cursor.getPredefinedCursor(0));
			}
			else if (action.equals("find next script text"))
			{
				setCursor(Cursor.getPredefinedCursor(3));
				this.findNextBtn.setEnabled(findScriptText(true));
				setCursor(Cursor.getPredefinedCursor(0));
			}
			else if (action.equals("find owner id"))
			{
				setCursor(Cursor.getPredefinedCursor(3));
				this.findNextBtn.setEnabled(findOwnerID(false));
				setCursor(Cursor.getPredefinedCursor(0));
			}
			else if (action.equals("find next owner id"))
			{
				setCursor(Cursor.getPredefinedCursor(3));
				this.findNextBtn.setEnabled(findOwnerID(true));
				setCursor(Cursor.getPredefinedCursor(0));
			}
			else if (action.equals("find ref base id"))
			{
				setCursor(Cursor.getPredefinedCursor(3));
				this.findNextBtn.setEnabled(findRefBaseID(false));
				setCursor(Cursor.getPredefinedCursor(0));
			}
			else if (action.equals("find next ref base id"))
			{
				setCursor(Cursor.getPredefinedCursor(3));
				this.findNextBtn.setEnabled(findRefBaseID(true));
				setCursor(Cursor.getPredefinedCursor(0));
			}
			else if (action.equals("find quest ref id"))
			{
				setCursor(Cursor.getPredefinedCursor(3));
				this.findNextBtn.setEnabled(findQuestRefID(false));
				setCursor(Cursor.getPredefinedCursor(0));
			}
			else if (action.equals("find next quest ref id"))
			{
				setCursor(Cursor.getPredefinedCursor(3));
				this.findNextBtn.setEnabled(findQuestRefID(true));
				setCursor(Cursor.getPredefinedCursor(0));
			}
			else if (action.equals("find land tex id"))
			{
				setCursor(Cursor.getPredefinedCursor(3));
				this.findNextBtn.setEnabled(findLandTexID(false));
				setCursor(Cursor.getPredefinedCursor(0));
			}
			else if (action.equals("find next land tex id"))
			{
				setCursor(Cursor.getPredefinedCursor(3));
				this.findNextBtn.setEnabled(findLandTexID(true));
				setCursor(Cursor.getPredefinedCursor(0));
			}
			else if (action.equals("find XY coordinates"))
			{
				setCursor(Cursor.getPredefinedCursor(3));
				this.findNextBtn.setEnabled(findXYCoordinates(false));
				setCursor(Cursor.getPredefinedCursor(0));
			}
			else if (action.equals("find next XY coordinates"))
			{
				setCursor(Cursor.getPredefinedCursor(3));
				this.findNextBtn.setEnabled(findXYCoordinates(true));
				setCursor(Cursor.getPredefinedCursor(0));
			}
			else if (action.equals("find form id"))
			{
				setCursor(Cursor.getPredefinedCursor(3));
				findFormID();
				setCursor(Cursor.getPredefinedCursor(0));
			}
			else if (action.equals("copy references"))
			{
				this.copyReferences = this.copyReferencesField.isSelected();
			}
			else if (action.equals("done"))
			{
				closeDialog();
				setVisible(false);
				this.pluginTree.clearSelection();
				this.clipboardTree.clearSelection();
				removeAllObjects(this.clipboard);
				removeAllObjects(this.plugin);
				EditDialog.removeAllComponents(this);
				this.clipboardTree.removeMouseListener(this.mlClipboard);
				this.pluginTree.removeMouseListener(this.mlPlugin);
				removeAllUserObjects(this.clipboardTree);
				removeAllUserObjects(this.pluginTree);
				this.mlClipboard = null;
				this.mlPlugin = null;
				this.clipboardTree.removeTreeExpansionListener(this);
				this.clipboardTree.setModel(null);
				this.clipboardTree = null;
				this.pluginTree.removeTreeExpansionListener(this);
				this.pluginTree.setModel(null);
				this.pluginTree = null;
				this.pluginTreeModel = null;
				dispose();
			}
		}
		catch (Throwable exc)
		{
			Main.logException("Exception while processing action event", exc);
		}
	}

	private void removeAllUserObjects(JTree tree)
	{
		TreeNode root = (TreeNode) tree.getModel().getRoot();
		removeAllUserObjects(root);
	}

	private void removeAllUserObjects(TreeNode node)
	{
		((DefaultMutableTreeNode) node).setUserObject(null);
		if (node.getChildCount() >= 0)
		{
			for (Enumeration<?> e = node.children(); e.hasMoreElements();)
			{
				TreeNode n = (TreeNode) e.nextElement();
				removeAllUserObjects(n);
			}
		}
	}

	private void findFormID()
	{
		String text = this.searchField.getText().trim();
		if ((text == null) || (text.length() == 0))
		{
			JOptionPane.showMessageDialog(this, "You must enter a search term", "Enter search term", 0);
			return;
		}
		int formID;
		try
		{
			formID = Integer.parseInt(text, 16);
		}
		catch (NumberFormatException exc)
		{

			JOptionPane.showMessageDialog(this, "You must enter a hex number for the form ID", "Enter search term", 0);
			return;
		}

		FormInfo formInfo = null;
		formInfo = this.plugin.getFormMap().get(new Integer(formID));

		if (formInfo == null)
		{
			JOptionPane.showMessageDialog(this, "No match found for '" + text + "'", "No match found", 0);
		}
		else
		{
			this.pluginTree.clearSelection();
			RecordNode recordNode = formInfo.getRecordNode();
			TreePath treePath = new TreePath(recordNode.getPath());
			this.pluginTree.setSelectionPath(treePath);
			this.pluginTree.scrollPathToVisible(treePath);
		}
	}

	private boolean findEditorID(boolean resume)
	{
		if (!resume)
		{
			this.searchFormInfo = null;
		}

		String text = this.searchField.getText().trim();
		String asteriskLikeWindows = "([\\w\\s])(\\*)";
		String text2 = text.replaceAll(asteriskLikeWindows, "$1.$2");
		if (text2.startsWith("*"))
			text2 = "." + text2;
		if ((text2 == null) || (text2.length() == 0))
		{
			JOptionPane.showMessageDialog(this, "You must enter a search term", "Enter search term", 0);
			return false;
		}

		Pattern p = null;
		try
		{
			p = Pattern.compile(text2, 2);
		}
		catch (PatternSyntaxException exc)
		{
			JOptionPane.showMessageDialog(this, "'" + text2 + "' is not a valid regular expression", "Invalid regular expression", 0);
		}

		if (p == null)
		{
			return false;
		}

		List<FormInfo> formList = this.plugin.getFormList();
		for (FormInfo formInfo : formList)
		{
			if (this.searchFormInfo != null)
			{
				if (this.searchFormInfo == formInfo)
					this.searchFormInfo = null;
			}
			else
			{
				String editorID = formInfo.getEditorID();
				if ((editorID != null) && (editorID.length() > 0))
				{
					Matcher m = p.matcher(editorID);
					if (m.matches())
					{
						this.searchFormInfo = formInfo;
						break;
					}

				}

			}

		}

		if (this.searchFormInfo == null)
		{
			JOptionPane.showMessageDialog(this, "No match found for '" + text + "'", "No match found", 0);
			return false;
		}
		this.pluginTree.clearSelection();
		RecordNode recordNode = this.searchFormInfo.getRecordNode();
		TreePath treePath = new TreePath(recordNode.getPath());
		this.pluginTree.setSelectionPath(treePath);
		this.pluginTree.scrollPathToVisible(treePath);

		return true;
	}

	private boolean findNameID(boolean resume)
	{
		if (!resume)
		{
			this.searchFormInfo = null;
		}

		String text = this.searchField.getText().trim();
		String asteriskLikeWindows = "([\\w\\s])(\\*)";
		String text2 = text.replaceAll(asteriskLikeWindows, "$1.$2");
		if (text2.startsWith("*"))
			text2 = "." + text2;
		if ((text2 == null) || (text2.length() == 0))
		{
			JOptionPane.showMessageDialog(this, "You must enter a search term", "Enter search term", 0);
			return false;
		}

		Pattern p = null;
		try
		{
			p = Pattern.compile(text2, 2);
		}
		catch (PatternSyntaxException exc)
		{
			JOptionPane.showMessageDialog(this, "'" + text2 + "' is not a valid regular expression", "Invalid regular expression", 0);
		}

		if (p == null)
		{
			return false;
		}

		List<FormInfo> formList = this.plugin.getFormList();
		for (FormInfo formInfo : formList)
		{
			if (this.searchFormInfo != null)
			{
				if (this.searchFormInfo == formInfo)
					this.searchFormInfo = null;
			}
			else
			{
				String nameID = "";
				try
				{
					Object plugRec = formInfo.getSource();
					if ((plugRec != null) && ((plugRec instanceof PluginRecord)))
					{
						nameID = ((PluginRecord) plugRec).getSubrecord("FULL").getDisplayData();
					}
				}
				catch (Exception localException)
				{
				}
				if ((nameID != null) && (nameID.length() > 0))
				{
					Matcher m = p.matcher(nameID);
					if (m.matches())
					{
						this.searchFormInfo = formInfo;
						break;
					}

				}

			}

		}

		if (this.searchFormInfo == null)
		{
			JOptionPane.showMessageDialog(this, "No match found for '" + text + "'", "No match found", 0);
			return false;
		}
		this.pluginTree.clearSelection();
		RecordNode recordNode = this.searchFormInfo.getRecordNode();
		TreePath treePath = new TreePath(recordNode.getPath());
		this.pluginTree.setSelectionPath(treePath);
		this.pluginTree.scrollPathToVisible(treePath);

		return true;
	}

	private boolean findResponseText(boolean resume)
	{
		if (!resume)
		{
			this.searchFormInfo = null;
		}

		String text = this.searchField.getText().trim();
		String asteriskLikeWindows = "([\\w\\s])(\\*)";
		String text2 = text.replaceAll(asteriskLikeWindows, "$1.$2");
		if (text2.startsWith("*"))
			text2 = "." + text2;
		if ((text2 == null) || (text2.length() == 0))
		{
			JOptionPane.showMessageDialog(this, "You must enter a search term", "Enter search term", 0);
			return false;
		}

		Pattern p = null;
		try
		{
			p = Pattern.compile(text2, 2);
		}
		catch (PatternSyntaxException exc)
		{
			JOptionPane.showMessageDialog(this, "'" + text2 + "' is not a valid regular expression", "Invalid regular expression", 0);
		}

		if (p == null)
		{
			return false;
		}

		List<FormInfo> formList = this.plugin.getFormList();
		for (FormInfo formInfo : formList)
		{
			if (this.searchFormInfo != null)
			{
				if (this.searchFormInfo == formInfo)
					this.searchFormInfo = null;
			}
			else
			{
				String responseText = "";
				if (!formInfo.getRecordType().equalsIgnoreCase("INFO"))
					continue;
				try
				{
					Object plugRec = formInfo.getSource();
					if ((plugRec == null) || (!(plugRec instanceof PluginRecord)))
						continue;
					List<PluginSubrecord> responseList = ((PluginRecord) plugRec).getAllSubrecords("NAM1");
					for (PluginSubrecord response : responseList)
					{
						responseText = response.getDisplayData();
						if ((responseText != null) && (responseText.length() > 0))
						{
							Matcher m = p.matcher(responseText);
							if (m.matches())
							{
								this.searchFormInfo = formInfo;
								break;
							}
						}
					}
				}
				catch (Exception localException)
				{
				}
			}

		}

		if (this.searchFormInfo == null)
		{
			JOptionPane.showMessageDialog(this, "No match found for '" + text + "'", "No match found", 0);
			return false;
		}
		this.pluginTree.clearSelection();
		RecordNode recordNode = this.searchFormInfo.getRecordNode();
		TreePath treePath = new TreePath(recordNode.getPath());
		this.pluginTree.setSelectionPath(treePath);
		this.pluginTree.scrollPathToVisible(treePath);

		return true;
	}

	private boolean findScriptText(boolean resume)
	{
		if (!resume)
		{
			this.searchFormInfo = null;
		}

		String text = this.searchField.getText().trim();
		String asteriskLikeWindows = "([\\w\\s])(\\*)";
		String text2 = text.replaceAll(asteriskLikeWindows, "$1.$2");
		if (text2.startsWith("*"))
			text2 = "." + text2;
		if ((text2 == null) || (text2.length() == 0))
		{
			JOptionPane.showMessageDialog(this, "You must enter a search term", "Enter search term", 0);
			return false;
		}

		Pattern p = null;
		try
		{
			p = Pattern.compile(text2, 2);
		}
		catch (PatternSyntaxException exc)
		{
			JOptionPane.showMessageDialog(this, "'" + text2 + "' is not a valid regular expression", "Invalid regular expression", 0);
		}

		if (p == null)
		{
			return false;
		}

		List<FormInfo> formList = this.plugin.getFormList();
		for (FormInfo formInfo : formList)
		{
			if (this.searchFormInfo != null)
			{
				if (this.searchFormInfo == formInfo)
					this.searchFormInfo = null;
			}
			else
			{
				String responseText = "";
				if ((!formInfo.getRecordType().equalsIgnoreCase("SCPT")) && (!formInfo.getRecordType().equalsIgnoreCase("QUST"))
						&& (!formInfo.getRecordType().equalsIgnoreCase("INFO")))
					continue;
				try
				{
					Object plugRec = formInfo.getSource();
					if ((plugRec == null) || (!(plugRec instanceof PluginRecord)))
						continue;
					List<PluginSubrecord> responseList = ((PluginRecord) plugRec).getAllSubrecords("SCTX");
					for (PluginSubrecord response : responseList)
					{
						responseText = response.getDisplayData();
						if ((responseText != null) && (responseText.length() > 0))
						{
							Matcher m = p.matcher(responseText);
							if (m.matches())
							{
								this.searchFormInfo = formInfo;
								break;
							}
						}
					}
				}
				catch (Exception localException)
				{
				}
			}

		}

		if (this.searchFormInfo == null)
		{
			JOptionPane.showMessageDialog(this, "No match found for '" + text + "'", "No match found", 0);
			return false;
		}
		this.pluginTree.clearSelection();
		RecordNode recordNode = this.searchFormInfo.getRecordNode();
		TreePath treePath = new TreePath(recordNode.getPath());
		this.pluginTree.setSelectionPath(treePath);
		this.pluginTree.scrollPathToVisible(treePath);

		return true;
	}

	private boolean findOwnerID(boolean resume)
	{
		if (!resume)
		{
			this.searchFormInfo = null;
		}

		String text = this.searchField.getText().trim();
		if ((text == null) || (text.length() == 0))
		{
			JOptionPane.showMessageDialog(this, "You must enter a search term", "Enter search term", 0);
			return false;
		}
		int ownerID;
		try
		{
			ownerID = Integer.parseInt(text, 16);
		}
		catch (NumberFormatException exc)
		{

			JOptionPane.showMessageDialog(this, "You must enter a hex number for the owner ID", "Enter search term", 0);
			return false;
		}

		List<FormInfo> formList = this.plugin.getFormList();
		for (FormInfo formInfo : formList)
		{
			if (this.searchFormInfo != null)
			{
				if (this.searchFormInfo == formInfo)
					this.searchFormInfo = null;
			}
			else
			{
				String ownerIDstr = "";
				Integer ownerIDint = null;
				try
				{
					Object plugRec = formInfo.getSource();
					if ((plugRec != null) && ((plugRec instanceof PluginRecord)))
					{
						ownerIDstr = ((PluginRecord) plugRec).getSubrecord("XOWN").getDisplayData();
						ownerIDint = Integer.valueOf(Integer.parseInt(ownerIDstr, 16));
					}
				}
				catch (Exception localException)
				{
				}
				if ((ownerIDint == null) || (ownerIDint.intValue() <= 0) || (ownerIDint.intValue() != ownerID))
					continue;
				this.searchFormInfo = formInfo;
				break;
			}

		}

		if (this.searchFormInfo == null)
		{
			JOptionPane.showMessageDialog(this, "No match found for '" + text + "'", "No match found", 0);
			return false;
		}
		this.pluginTree.clearSelection();
		RecordNode recordNode = this.searchFormInfo.getRecordNode();
		TreePath treePath = new TreePath(recordNode.getPath());
		this.pluginTree.setSelectionPath(treePath);
		this.pluginTree.scrollPathToVisible(treePath);

		return true;
	}

	private boolean findRefBaseID(boolean resume)
	{
		if (!resume)
		{
			this.searchFormInfo = null;
		}

		String text = this.searchField.getText().trim();
		if ((text == null) || (text.length() == 0))
		{
			JOptionPane.showMessageDialog(this, "You must enter a search term", "Enter search term", 0);
			return false;
		}
		int refBaseID;
		try
		{
			refBaseID = Integer.parseInt(text, 16);
		}
		catch (NumberFormatException exc)
		{

			JOptionPane.showMessageDialog(this, "You must enter a hex number for the ref base ID", "Enter search term", 0);
			return false;
		}

		List<FormInfo> formList = this.plugin.getFormList();
		for (FormInfo formInfo : formList)
		{
			if (this.searchFormInfo != null)
			{
				if (this.searchFormInfo == formInfo)
					this.searchFormInfo = null;
			}
			else
			{
				String refBaseIDstr = "";
				Integer refBaseIDint = null;
				if ((!formInfo.getRecordType().equalsIgnoreCase("REFR")) && (!formInfo.getRecordType().equalsIgnoreCase("ACRE"))
						&& (!formInfo.getRecordType().equalsIgnoreCase("ACHR")))
					continue;
				try
				{
					Object plugRec = formInfo.getSource();
					if ((plugRec != null) && ((plugRec instanceof PluginRecord)))
					{
						refBaseIDstr = ((PluginRecord) plugRec).getSubrecord("NAME").getDisplayData();
						refBaseIDint = Integer.valueOf(Integer.parseInt(refBaseIDstr, 16));
					}
				}
				catch (Exception localException)
				{
				}
				if ((refBaseIDint == null) || (refBaseIDint.intValue() <= 0) || (refBaseIDint.intValue() != refBaseID))
					continue;
				this.searchFormInfo = formInfo;
				break;
			}

		}

		if (this.searchFormInfo == null)
		{
			JOptionPane.showMessageDialog(this, "No match found for '" + text + "'", "No match found", 0);
			return false;
		}
		this.pluginTree.clearSelection();
		RecordNode recordNode = this.searchFormInfo.getRecordNode();
		TreePath treePath = new TreePath(recordNode.getPath());
		this.pluginTree.setSelectionPath(treePath);
		this.pluginTree.scrollPathToVisible(treePath);

		return true;
	}

	private boolean findLandTexID(boolean resume)
	{
		if (!resume)
		{
			this.searchFormInfo = null;
		}

		String text = this.searchField.getText().trim();
		if ((text == null) || (text.length() == 0))
		{
			JOptionPane.showMessageDialog(this, "You must enter a search term", "Enter search term", 0);
			return false;
		}
		int landTexID;
		try
		{
			landTexID = Integer.parseInt(text, 16);
		}
		catch (NumberFormatException exc)
		{

			JOptionPane.showMessageDialog(this, "You must enter a hex number for the landscape texture ID", "Enter search term", 0);
			return false;
		}

		List<FormInfo> formList = this.plugin.getFormList();
		for (FormInfo formInfo : formList)
		{
			if (this.searchFormInfo != null)
			{
				if (this.searchFormInfo == formInfo)
					this.searchFormInfo = null;
			}
			else if (formInfo.getRecordType().equalsIgnoreCase("LAND"))
			{
				Object plugRec = formInfo.getSource();
				if ((plugRec == null) || (!(plugRec instanceof PluginRecord)))
					continue;
				if (!findLandTexIDInLANDRec((PluginRecord) plugRec, landTexID))
					continue;
				this.searchFormInfo = formInfo;
				break;
			}

		}

		if (this.searchFormInfo == null)
		{
			JOptionPane.showMessageDialog(this, "No match found for '" + text + "'", "No match found", 0);
			return false;
		}
		this.pluginTree.clearSelection();
		RecordNode recordNode = this.searchFormInfo.getRecordNode();
		TreePath treePath = new TreePath(recordNode.getPath());
		this.pluginTree.setSelectionPath(treePath);
		this.pluginTree.scrollPathToVisible(treePath);

		return true;
	}

	private boolean findQuestRefID(boolean resume)
	{
		if (!resume)
		{
			this.searchFormInfo = null;
		}

		String text = this.searchField.getText().trim();
		if ((text == null) || (text.length() == 0))
		{
			JOptionPane.showMessageDialog(this, "You must enter a search term", "Enter search term", 0);
			return false;
		}
		int refBaseID;
		try
		{
			refBaseID = Integer.parseInt(text, 16);
		}
		catch (NumberFormatException exc)
		{

			JOptionPane.showMessageDialog(this, "You must enter a hex number for the quest ref ID", "Enter search term", 0);
			return false;
		}

		List<FormInfo> formList = this.plugin.getFormList();
		for (FormInfo formInfo : formList)
		{
			if (this.searchFormInfo != null)
			{
				if (this.searchFormInfo == formInfo)
					this.searchFormInfo = null;
			}
			else
			{
				String refBaseIDstr = "";
				Integer refBaseIDint = null;
				if ((!formInfo.getRecordType().equalsIgnoreCase("DIAL")) && (!formInfo.getRecordType().equalsIgnoreCase("INFO")))
					continue;
				try
				{
					Object plugRec = formInfo.getSource();
					if ((plugRec == null) || (!(plugRec instanceof PluginRecord)))
						continue;
					boolean foundQuestID = false;
					List<PluginSubrecord> subList = ((PluginRecord) plugRec).getAllSubrecords("QSTI");
					for (PluginSubrecord subrec : subList)
					{
						refBaseIDstr = subrec.getDisplayData();
						refBaseIDint = Integer.valueOf(Integer.parseInt(refBaseIDstr, 16));
						if ((refBaseIDint == null) || (refBaseIDint.intValue() <= 0))
							continue;
						if (refBaseIDint.intValue() != refBaseID)
							continue;
						this.searchFormInfo = formInfo;
						foundQuestID = true;
						break;
					}

					if (!foundQuestID)
					{
						continue;
					}
				}
				catch (Exception localException)
				{
				}
			}
		}
		if (this.searchFormInfo == null)
		{
			JOptionPane.showMessageDialog(this, "No match found for '" + text + "'", "No match found", 0);
			return false;
		}
		this.pluginTree.clearSelection();
		RecordNode recordNode = this.searchFormInfo.getRecordNode();
		TreePath treePath = new TreePath(recordNode.getPath());
		this.pluginTree.setSelectionPath(treePath);
		this.pluginTree.scrollPathToVisible(treePath);

		return true;
	}

	private boolean findXYCoordinates(boolean resume)
	{
		if (!resume)
		{
			this.searchFormInfo = null;
		}

		String text = this.searchField.getText().trim();
		if ((text == null) || (text.length() == 0))
		{
			JOptionPane.showMessageDialog(this, "You must enter a search term", "Enter search term", 0);
			return false;
		}

		Integer xcoord = null;
		Integer ycoord = null;
		try
		{
			String[] coords = text.split(",");
			if (coords.length != 2)
				throw new NumberFormatException("Wrong number of coordinates");
			xcoord = Integer.valueOf(Integer.parseInt(coords[0].trim()));
			ycoord = Integer.valueOf(Integer.parseInt(coords[1].trim()));
		}
		catch (NumberFormatException exc)
		{
			xcoord = ycoord = null;
		}
		if ((xcoord == null) || (ycoord == null))
		{
			JOptionPane.showMessageDialog(this, "You must enter two integers in the form X, Y; the comma is required.",
					"Enter search term", 0);
			return false;
		}

		String XYSearch = xcoord + ", " + ycoord;

		List<FormInfo> formList = this.plugin.getFormList();
		for (FormInfo formInfo : formList)
		{
			if (this.searchFormInfo != null)
			{
				if (this.searchFormInfo == formInfo)
					this.searchFormInfo = null;
			}
			else
			{
				String XY = "";
				if (!formInfo.getRecordType().equalsIgnoreCase("CELL"))
					continue;
				try
				{
					Object plugRec = formInfo.getSource();
					if ((plugRec != null) && ((plugRec instanceof PluginRecord)))
					{
						XY = ((PluginRecord) plugRec).getSubrecord("XCLC").getDisplayData();
					}
				}
				catch (Exception localException)
				{
				}
				if ((XY == null) || (XY.length() <= 0) || (!XYSearch.equalsIgnoreCase(XY)))
					continue;
				this.searchFormInfo = formInfo;
				break;
			}

		}

		if (this.searchFormInfo == null)
		{
			JOptionPane.showMessageDialog(this, "No match found for '" + text + "'", "No match found", 0);
			return false;
		}
		this.pluginTree.clearSelection();
		RecordNode recordNode = this.searchFormInfo.getRecordNode();
		TreePath treePath = new TreePath(recordNode.getPath());
		this.pluginTree.setSelectionPath(treePath);
		this.pluginTree.scrollPathToVisible(treePath);

		return true;
	}

	private void closeDialog()
	{
		if (this.pluginModified)
		{
			int selection = JOptionPane.showConfirmDialog(this, "The plugin has been modified. Do you want to save the changes?",
					"Plugin Modified", 0, 3);
			if (selection == 0)
			{
				SaveTask.savePlugin(this, this.pluginFile, this.plugin);
			}
		}
		if (this.clipboardModified)
		{
			int selection = JOptionPane.showConfirmDialog(this, "The clipboard has been modified. Do you want to save the changes?",
					"Clipboard Modified", 0, 3);
			if (selection == 0)
				SaveTask.savePlugin(this, this.clipboardFile, this.clipboard);
		}
	}

	private void setPluginModified(boolean isModified)
	{
		this.pluginModified = isModified;
		this.savePluginBtn.setEnabled(isModified);
	}

	private void setClipboardModified(boolean isModified)
	{
		this.clipboardModified = isModified;
		this.saveClipboardBtn.setEnabled(isModified);
		this.saveClipboardAsBtn.setEnabled(isModified);
	}

	private void setClipboardCleared(boolean isCleared)
	{
		this.clipboardCleared = isCleared;

		this.clearClipboardBtn.setEnabled(!isCleared);
	}

	private void displaySubrecordData()
	{
		TreePath[] treePaths = this.pluginTree.getSelectionPaths();
		if (treePaths == null)
		{
			JOptionPane.showMessageDialog(this, "You must select a subrecord to display.", "Error", 0);
			return;
		}

		for (TreePath treePath : treePaths)
		{
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
			Object userObject = node.getUserObject();
			if (!(userObject instanceof PluginSubrecord))
			{
				JOptionPane.showMessageDialog(this, "Only subrecords may be displayed.", "Error", 0);
				return;
			}

		}

		for (TreePath treePath : treePaths)
		{
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
			Object userObject = node.getUserObject();
			DisplaySubrecordDialog.showDialog(this, (PluginSubrecord) userObject);
		}
	}

	private void displaySubrecordDataAsBytes()
	{
		TreePath[] treePaths = this.pluginTree.getSelectionPaths();
		if (treePaths == null)
		{
			JOptionPane.showMessageDialog(this, "You must select a subrecord to display.", "Error", 0);
			return;
		}

		for (TreePath treePath : treePaths)
		{
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
			Object userObject = node.getUserObject();
			if (!(userObject instanceof PluginSubrecord))
			{
				JOptionPane.showMessageDialog(this, "Only subrecords may be displayed.", "Error", 0);
				return;
			}

		}

		for (TreePath treePath : treePaths)
		{
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
			Object userObject = node.getUserObject();
			DisplaySubrecordDialog.showDialog(this, (PluginSubrecord) userObject, true);
		}
	}

	private void deleteSubrecords() throws DataFormatException, IOException, PluginException
	{
		TreePath[] treePaths = this.pluginTree.getSelectionPaths();
		if (treePaths == null)
		{
			JOptionPane.showMessageDialog(this, "You must select a subrecord to delete.", "Error", 0);
			return;
		}

		for (TreePath treePath : treePaths)
		{
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
			Object userObject = node.getUserObject();
			if (!(userObject instanceof PluginSubrecord))
			{
				JOptionPane.showMessageDialog(this, "Only subrecords may be deleted.", "Error", 0);
				return;
			}

		}

		for (TreePath treePath : treePaths)
		{
			DefaultMutableTreeNode subrecordNode = (DefaultMutableTreeNode) treePath.getLastPathComponent();
			RecordNode recordNode = (RecordNode) subrecordNode.getParent();
			PluginRecord record = recordNode.getRecord();
			PluginSubrecord subrecord = (PluginSubrecord) subrecordNode.getUserObject();

			List<PluginSubrecord> subrecords = record.getSubrecords();
			ListIterator<PluginSubrecord> lit = subrecords.listIterator();
			while (lit.hasNext())
			{
				PluginSubrecord checkSubrecord = lit.next();
				if (checkSubrecord.equals(subrecord))
				{
					lit.remove();
					break;
				}
			}

			record.setSubrecords(subrecords);
			setPluginModified(true);

			recordNode.remove(subrecordNode);
			DefaultTreeModel model = (DefaultTreeModel) this.pluginTree.getModel();
			model.nodeStructureChanged(recordNode);
		}
	}

	public void treeExpanded(TreeExpansionEvent event)
	{
		JTree tree = (JTree) event.getSource();
		TreePath treePath = event.getPath();
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();

		if ((node instanceof RecordNode))
		{
			RecordNode recordNode = (RecordNode) node;
			DefaultMutableTreeNode subrecordNode = (DefaultMutableTreeNode) recordNode.getFirstChild();
			if (subrecordNode.getUserObject() == null)
				try
				{
					recordNode.removeAllChildren();
					createRecordChildren(recordNode);
					DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
					model.nodeStructureChanged(recordNode);
				}
				catch (Throwable exc)
				{
					Main.logException("Exception while creating subrecords", exc);
				}
		}
	}

	public void treeCollapsed(TreeExpansionEvent event)
	{
	}

	private List<FormInfo> findAllInfos(Plugin pl)
	{
		ArrayList<FormInfo> allInfos = new ArrayList<FormInfo>();
		List<FormInfo> allForms = pl.getFormList();

		for (FormInfo form : allForms)
		{
			if (!form.getRecordType().equals("INFO"))
				continue;
			allInfos.add(form);
		}
		return allInfos;
	}

	private List<FormInfo> findQuestInfos(Plugin pl, int questID)
	{
		ArrayList<FormInfo> questInfos = new ArrayList<FormInfo>();
		List<FormInfo> allForms = pl.getFormList();

		for (FormInfo form : allForms)
		{
			if (!form.getRecordType().equals("INFO"))
			{
				continue;
			}
			PluginRecord pluginRec = (PluginRecord) form.getSource();
			if (pluginRec == null)
				continue;

			List<PluginSubrecord> pluginSubrecs;
			try
			{
				pluginSubrecs = pluginRec.getSubrecords();
			}
			catch (Exception ex)
			{

				continue;
			}

			for (PluginSubrecord pluginSubrec : pluginSubrecs)
			{
				if (!pluginSubrec.getSubrecordType().equals("QSTI"))
				{
					continue;
				}

				byte[] subrecordData;
				try
				{
					subrecordData = pluginSubrec.getSubrecordData();
				}
				catch (Exception ex)
				{

					continue;
				}

				int infoQuestID = SerializedElement.getInteger(subrecordData, 0);
				if (questID != infoQuestID)
					continue;
				questInfos.add(form);
			}

		}

		return questInfos;
	}

	private List<FormInfo> findWRLDChildren(Plugin pl, int WSID, boolean selected, boolean regions)
	{
		ArrayList<FormInfo> WSInfos = new ArrayList<FormInfo>();
		List<FormInfo> allForms = pl.getFormList();

		for (FormInfo form : allForms)
		{
			if ((!form.getRecordType().equals("REGN")) && (!form.getRecordType().equals("WRLD")))
				continue;
			if (selected)
			{
				if ((regions) && (!form.getRecordType().equals("REGN")))
					continue;
				if ((!regions) && (!form.getRecordType().equals("WRLD")))
				{
					continue;
				}
			}
			PluginRecord pluginRec = (PluginRecord) form.getSource();
			if (pluginRec == null)
				continue;
			try
			{
				if (!pluginRec.hasSubrecordWithData("WNAM", new Integer(WSID)))
					continue;
				WSInfos.add(form);
			}
			catch (Exception localException)
			{
			}

		}

		return WSInfos;
	}

	private List<FormInfo> findInfosWithCondition(Plugin pl, int funcCode, int compCode, Object param1, Object param2, float compValue)
	{
		ArrayList<FormInfo> condInfos = new ArrayList<FormInfo>();
		byte[] byteArray1 = null;
		byte[] byteArray2 = null;

		if ((!FunctionCode.isValid(funcCode)) || (!ComparisonCode.isValid(compCode)))
			return condInfos;
		try
		{
			if (param1 != null)
				byteArray1 = PluginRecord.convertToByteArray(param1);
			if (param2 != null)
				byteArray2 = PluginRecord.convertToByteArray(param2);

		}
		catch (Exception ex)
		{
			return condInfos;
		}
		FunctionInfo funcInfo = PluginSubrecord.getFunctionInfo(funcCode);
		if (funcInfo == null)
			return condInfos;
		if ((funcInfo.isFirstReference()) && (param1 == null))
			return condInfos;
		if ((funcInfo.isSecondReference()) && (param2 == null))
			return condInfos;

		boolean usesFirst = funcInfo.isFirstReference();
		boolean usesSecond = funcInfo.isSecondReference();
		List<FormInfo> allForms = pl.getFormList();

		for (FormInfo form : allForms)
		{
			if (!form.getRecordType().equals("INFO"))
			{
				continue;
			}
			PluginRecord pluginRec = (PluginRecord) form.getSource();
			if (pluginRec == null)
				continue;

			List<PluginSubrecord> pluginSubrecs;
			try
			{
				pluginSubrecs = pluginRec.getSubrecords();
			}
			catch (Exception ex)
			{

				continue;
			}

			for (PluginSubrecord pluginSubrec : pluginSubrecs)
			{
				if (!pluginSubrec.getSubrecordType().equals("CTDA"))
				{
					continue;
				}

				try
				{
					byte[] subrecordData = pluginSubrec.getSubrecordData();
					int subCompCode = (subrecordData[0] & 0xF0) >>> 4;
					int subCompValueInt = SerializedElement.getInteger(subrecordData, 4);
					float subCompValue = Float.intBitsToFloat(subCompValueInt);
					int subFuncCode = SerializedElement.getInteger(subrecordData, 8);
					if ((subCompCode != compCode) || (subFuncCode != funcCode) || (subCompValue != compValue)
							|| ((usesFirst) && (SerializedElement.compareArrays(byteArray1, 0, subrecordData, 12, 4) != 0)))
						continue;
					if ((usesSecond) && (SerializedElement.compareArrays(byteArray2, 0, subrecordData, 16, 4) != 0))
						continue;
					condInfos.add(form);
				}
				catch (Exception localException1)
				{
				}

			}

		}

		return condInfos;
	}

	private List<Integer> getNPCsInDialogue(Plugin pl)
	{
		List<Integer> NPCList = new ArrayList<Integer>();
		List<FormInfo> allForms = pl.getFormList();

		for (FormInfo form : allForms)
		{
			if ((!form.getRecordType().equals("INFO")) && (!form.getRecordType().equals("QUST")))
			{
				continue;
			}
			PluginRecord pluginRec = (PluginRecord) form.getSource();
			if ((pluginRec == null) || (pluginRec.isIgnored()))
				continue;

			List<PluginSubrecord> pluginSubrecs;
			try
			{
				pluginSubrecs = pluginRec.getSubrecords();
			}
			catch (Exception ex)
			{

				continue;
			}

			for (PluginSubrecord pluginSubrec : pluginSubrecs)
			{
				if (!pluginSubrec.getSubrecordType().equals("CTDA"))
				{
					continue;
				}

				try
				{
					byte[] subrecordData = pluginSubrec.getSubrecordData();
					int subFuncCode = SerializedElement.getInteger(subrecordData, 8);
					int subParam1 = SerializedElement.getInteger(subrecordData, 12);
					if ((subFuncCode != 72) || (subParam1 == 20) || (NPCList.contains(Integer.valueOf(subParam1))))
						continue;
					NPCList.add(Integer.valueOf(subParam1));
				}
				catch (Exception localException1)
				{
				}
			}

		}

		return NPCList;
	}

	private List<Integer> getNPCsInFactions(Plugin pl, List<Integer> factList)
	{
		List<Integer> NPCList = new ArrayList<Integer>();
		PluginGroup npcGroup = pl.getTopGroup("NPC_");
		if (npcGroup == null)
			return NPCList;

		List<PluginRecord> allNPCs = npcGroup.getAllPluginRecords();

		for (PluginRecord pluginRec : allNPCs)
		{
			if ((pluginRec == null) || (pluginRec.isIgnored()))
				continue;

			List<PluginSubrecord> pluginSubrecs;
			try
			{
				pluginSubrecs = pluginRec.getSubrecords();
			}
			catch (Exception ex)
			{

				continue;
			}

			for (PluginSubrecord pluginSubrec : pluginSubrecs)
			{
				if (!pluginSubrec.getSubrecordType().equals("SNAM"))
				{
					continue;
				}

				try
				{
					byte[] subrecordData = pluginSubrec.getSubrecordData();
					int factID = SerializedElement.getInteger(subrecordData, 0);
					if (!factList.contains(Integer.valueOf(factID)))
						continue;
					NPCList.add(Integer.valueOf(pluginRec.getFormID()));
				}
				catch (Exception localException1)
				{
				}

			}

		}

		return NPCList;
	}

	private List<Integer> getFactionsInDialogue(Plugin pl)
	{
		List<Integer> factionList = new ArrayList<Integer>();
		List<Integer> factionsInPlugin = new ArrayList<Integer>();
		List<FormInfo> allForms = pl.getFormList();

		PluginGroup factionGroup = pl.getTopGroup("FACT");
		if (factionGroup == null)
			return factionList;
		List<PluginRecord> allFactions = factionGroup.getAllPluginRecords();
		if (allFactions.size() == 0)
			return factionList;
		for (PluginRecord fact : allFactions)
		{
			factionsInPlugin.add(Integer.valueOf(fact.getFormID()));
		}

		for (FormInfo form : allForms)
		{
			if ((!form.getRecordType().equals("INFO")) && (!form.getRecordType().equals("QUST")))
			{
				continue;
			}
			PluginRecord pluginRec = (PluginRecord) form.getSource();
			if ((pluginRec == null) || (pluginRec.isIgnored()))
				continue;

			List<PluginSubrecord> pluginSubrecs;
			try
			{
				pluginSubrecs = pluginRec.getSubrecords();
			}
			catch (Exception ex)
			{

				continue;
			}

			for (PluginSubrecord pluginSubrec : pluginSubrecs)
			{
				if (!pluginSubrec.getSubrecordType().equals("CTDA"))
				{
					continue;
				}

				try
				{
					byte[] subrecordData = pluginSubrec.getSubrecordData();
					int subFuncCode = SerializedElement.getInteger(subrecordData, 8);
					int subParam1 = SerializedElement.getInteger(subrecordData, 12);
					if ((subFuncCode != 71) || (!factionsInPlugin.contains(Integer.valueOf(subParam1))))
						continue;
					factionList.add(Integer.valueOf(subParam1));
				}
				catch (Exception localException1)
				{
				}
			}

		}

		return factionList;
	}

	private int purgeFactions(Plugin pl, List<Integer> factList)
	{
		int formsAltered = 0;
		List<FormInfo> allForms = pl.getFormList();

		for (FormInfo form : allForms)
		{
			if ((!form.getRecordType().equals("INFO")) && (!form.getRecordType().equals("QUST")) && (!form.getRecordType().equals("NPC_")))
			{
				continue;
			}
			PluginRecord pluginRec = (PluginRecord) form.getSource();
			if ((pluginRec == null) || (pluginRec.isIgnored()))
				continue;

			List<PluginSubrecord> pluginSubrecs;
			try
			{
				pluginSubrecs = pluginRec.getSubrecords();
			}
			catch (Exception ex)
			{

				continue;
			}

			boolean altered = false;
			ListIterator<PluginSubrecord> lit = pluginSubrecs.listIterator();
			while (lit.hasNext())
			{
				PluginSubrecord subrec = lit.next();

				if ((subrec.getSubrecordType().equals("CTDA")) || (subrec.getSubrecordType().equals("SNAM")))
				{
					byte[] subrecordData;
					try
					{
						subrecordData = subrec.getSubrecordData();
					}
					catch (Exception ex)
					{

						continue;
					}

					if (subrec.getSubrecordType().equals("CTDA"))
					{
						int subFuncCode = SerializedElement.getInteger(subrecordData, 8);
						int subParam1 = SerializedElement.getInteger(subrecordData, 12);
						if ((subFuncCode == 71) && (!factList.contains(Integer.valueOf(subParam1))))
						{
							lit.remove();
							altered = true;
						}
					}
					if (!subrec.getSubrecordType().equals("SNAM"))
					{
						continue;
					}

					int faction = SerializedElement.getInteger(subrecordData, 0);
					if (factList.contains(Integer.valueOf(faction)))
						continue;
					lit.remove();
					altered = true;
				}
			}

			if (!altered)
				continue;
			try
			{
				pluginRec.setSubrecords(pluginSubrecs);
			}
			catch (Exception ex)
			{
				continue;
			}
			formsAltered++;
		}

		return formsAltered;
	}

	private int removeConditionFromInfos(Plugin pl, int funcCode, Integer compCode, Object param1, Object param2, Float compValue)
	{
		int formsAltered = 0;
		byte[] byteArray1 = null;
		byte[] byteArray2 = null;

		if ((!FunctionCode.isValid(funcCode)) || ((compCode != null) && (!ComparisonCode.isValid(compCode.intValue()))))
			return formsAltered;
		try
		{
			if (param1 != null)
				byteArray1 = PluginRecord.convertToByteArray(param1);
			if (param2 != null)
				byteArray2 = PluginRecord.convertToByteArray(param2);

		}
		catch (Exception ex)
		{
			return formsAltered;
		}
		FunctionInfo funcInfo = PluginSubrecord.getFunctionInfo(funcCode);
		if (funcInfo == null)
			return formsAltered;

		boolean usesFirst = (funcInfo.isFirstReference()) && (param1 != null);
		boolean usesSecond = (funcInfo.isSecondReference()) && (param2 != null);
		List<FormInfo> allForms = pl.getFormList();

		for (FormInfo form : allForms)
		{
			if (!form.getRecordType().equals("INFO"))
				continue;
			PluginRecord pluginRec = (PluginRecord) form.getSource();
			if (pluginRec == null)
				continue;

			List<PluginSubrecord> pluginSubrecs;
			try
			{
				pluginSubrecs = pluginRec.getSubrecords();
			}
			catch (Exception ex)
			{

				continue;
			}

			boolean subrecAltered = false;
			for (Iterator<PluginSubrecord> i = pluginSubrecs.iterator(); i.hasNext();)
			{
				PluginSubrecord pluginSubrec = i.next();
				if (!pluginSubrec.getSubrecordType().equals("CTDA"))
				{
					continue;
				}

				try
				{
					byte[] subrecordData = pluginSubrec.getSubrecordData();
					int subCompCode = (subrecordData[0] & 0xF0) >>> 4;
					int subCompValueInt = SerializedElement.getInteger(subrecordData, 4);
					float subCompValue = Float.intBitsToFloat(subCompValueInt);
					int subFuncCode = SerializedElement.getInteger(subrecordData, 8);
					if ((subFuncCode != funcCode) || ((compCode != null) && (subCompCode != compCode.intValue()))
							|| ((compValue != null) && (subCompValue != compValue.floatValue()))
							|| ((usesFirst) && (SerializedElement.compareArrays(byteArray1, 0, subrecordData, 12, 4) != 0)))
						continue;
					if ((usesSecond) && (SerializedElement.compareArrays(byteArray2, 0, subrecordData, 16, 4) != 0))
						continue;
					i.remove();
					subrecAltered = true;
				}
				catch (Exception localException1)
				{
				}

			}

			if (!subrecAltered)
				continue;
			try
			{
				pluginRec.setSubrecords(pluginSubrecs);
			}
			catch (Exception ex)
			{
				continue;
			}
			formsAltered++;
		}

		return formsAltered;
	}

	private int removeQuestsFromDIALs(Plugin pl, HashSet<Integer> questsToKeep)
	{
		int formsAltered = 0;
		if (questsToKeep == null)
			return formsAltered;
		List<PluginGroup> topGroups = pl.getGroupList();

		PluginGroup DIALGroup = null;
		String groupRecordType;
		for (PluginGroup group : topGroups)
		{
			groupRecordType = group.getGroupRecordType();
			if (groupRecordType.equals("DIAL"))
			{
				DIALGroup = group;
				break;
			}
		}
		if (DIALGroup == null)
			return formsAltered;
		List<PluginRecord> recordList = DIALGroup.getRecordList();
		for (PluginRecord rec : recordList)
		{
			if (((rec instanceof PluginGroup)) || (!rec.getRecordType().equals("DIAL")))
				continue;

			List<PluginSubrecord> subrecs;
			try
			{
				subrecs = rec.getSubrecords();
			}
			catch (Exception ex)
			{

				continue;
			}

			boolean subrecAltered = false;
			for (Iterator<PluginSubrecord> i = subrecs.iterator(); i.hasNext();)
			{
				PluginSubrecord subrec = i.next();
				if (!subrec.getSubrecordType().equals("QSTI"))
				{
					continue;
				}
				try
				{
					byte[] subrecordData = subrec.getSubrecordData();
					int questID = SerializedElement.getInteger(subrecordData, 0);
					if (questsToKeep.contains(Integer.valueOf(questID)))
						continue;
					i.remove();
					subrecAltered = true;
				}
				catch (Exception localException1)
				{
				}

			}

			if (!subrecAltered)
				continue;
			try
			{
				rec.setSubrecords(subrecs);
			}
			catch (Exception ex)
			{
				continue;
			}
			formsAltered++;
		}

		return formsAltered;
	}

	private int cleanINFOsForLipSynch(Plugin pl, HashSet<String> fieldsToKeep, HashSet<Integer> keptCTDAFuncs)
	{
		int formsAltered = 0;
		List<PluginGroup> topGroups = pl.getGroupList();
		PluginGroup groupNeeded = null;
		String groupRecordType;
		for (PluginGroup group : topGroups)
		{
			groupRecordType = group.getGroupRecordType();
			if (groupRecordType.equals("DIAL"))
			{
				groupNeeded = group;
				break;
			}
		}
		if (groupNeeded == null)
			return formsAltered;

		List<PluginRecord> groupList = groupNeeded.getRecordList();
		for (PluginRecord dialOrInfo : groupList)
		{
			if ((dialOrInfo instanceof PluginGroup))
			{
				List<PluginRecord> infoGroup = ((PluginGroup) dialOrInfo).getRecordList();
				for (PluginRecord rec : infoGroup)
				{
					try
					{
						boolean bool1 = rec.removeSubrecords(fieldsToKeep, true);
						boolean bool2 = rec.removeCTDASubrecords(keptCTDAFuncs, true);
						if ((!bool1) && (!bool2))
							continue;
						formsAltered++;
					}
					catch (Exception localException)
					{
					}
				}
			}

		}
		return formsAltered;
	}

	private int cleanQUSTsForLipSynch(Plugin pl, HashSet<String> fieldsToKeep, HashSet<Integer> keptCTDAFuncs)
	{
		int formsAltered = 0;
		PluginGroup groupNeeded = pl.getTopGroup("QUST");
		if (groupNeeded == null)
			return formsAltered;

		List<PluginRecord> questList = groupNeeded.getRecordList();
		for (PluginRecord quest : questList)
		{
			try
			{
				boolean bool1 = quest.removeSubrecords(fieldsToKeep, true);
				boolean bool2 = quest.removeCTDASubrecords(keptCTDAFuncs, true);
				if ((!bool1) && (!bool2))
					continue;
				formsAltered++;
			}
			catch (Exception localException)
			{
			}
		}
		return formsAltered;
	}

	private int cleanGroupForLipSynch(Plugin pl, String groupName, HashSet<String> fieldsToKeep)
	{
		int formsAltered = 0;
		List<PluginGroup> topGroups = pl.getGroupList();
		PluginGroup groupNeeded = null;
		String groupRecordType;
		for (PluginGroup group : topGroups)
		{
			groupRecordType = group.getGroupRecordType();
			if (groupRecordType.equals(groupName))
			{
				groupNeeded = group;
				break;
			}
		}
		if (groupNeeded == null)
			return formsAltered;

		List<PluginRecord> groupList = groupNeeded.getRecordList();
		for (PluginRecord rec : groupList)
		{
			if ((rec instanceof PluginGroup))
			{
				continue;
			}

			try
			{
				if (!rec.removeSubrecords(fieldsToKeep, true))
					continue;
				formsAltered++;
			}
			catch (Exception localException)
			{
			}
		}
		return formsAltered;
	}

	private List<PluginRecord> getRaceList(Plugin pl)
	{
		List<PluginRecord> raceList = new ArrayList<PluginRecord>();
		PluginGroup raceGroup = pl.getTopGroup("RACE");

		if (raceGroup != null)
		{
			List<PluginRecord> recordList = raceGroup.getRecordList();
			for (PluginRecord record : recordList)
			{
				if ((!record.getRecordType().equals("RACE")) || (record.isIgnored()))
					continue;
				raceList.add(record);
			}

		}

		try
		{
			List<String> masterListInCaps = pl.getMasterList();
			Master[] masters = new Master[masterListInCaps.size()];

			int index = 0;
			for (String masterName : masterListInCaps)
			{
				File masterFile = new File(this.pluginFile.getParent() + Main.fileSeparator + masterName);
				Master master = new Master(masterFile);
				master.load(null);
				masters[(index++)] = master;
			}

			ListIterator<String> lit = masterListInCaps.listIterator();
			while (lit.hasNext())
			{
				String tmpMaster = lit.next();
				lit.set(tmpMaster.toUpperCase());
			}
			for (int masterID = masters.length - 1; masterID > -1; masterID--)
			{
				Master master = masters[masterID];
				List<FormInfo> formList = master.getFormList();
				List<String> masterListforMaster = master.getMasterList();

				for (FormInfo formInfo : formList)
				{
					String recordType = formInfo.getRecordType();
					if (!recordType.equals("RACE"))
					{
						continue;
					}

					int formMasterID = (formInfo.getFormID() & 0xFF000000) >> 24;
					int newMasterID = formMasterID;
					if (formMasterID <= masterListforMaster.size())
					{
						String formMasterName = formMasterID == masterListforMaster.size() ? master.getName()
								: (String) masterListforMaster.get(formMasterID);
						String raceFormID = String.format("%08X", new Object[]
						{ Integer.valueOf(formInfo.getFormID()) });
						int pluginMasterIdx = masterListInCaps.indexOf(formMasterName.toUpperCase());
						if (pluginMasterIdx == -1)
						{
							if (Main.debugMode)
							{
								System.out
										.printf("GenerateTask: Form ID %08X is modified in <%s> from the original in <%s>;  but <%s> is not in the master list for plugin <%s>.\n",
												new Object[]
												{ Integer.valueOf(formInfo.getFormID()), master.getName(), formMasterName, formMasterName,
														this.plugin.getName() });
							}
						}
						else
							newMasterID = pluginMasterIdx;
					}
					PluginRecord record = master.getRecord(formInfo.getFormID());

					if (newMasterID != formMasterID)
					{
						int newFormID = record.getFormID() & 0xFFFFFF | newMasterID << 24;
						record.setFormID(newFormID);
					}

					if (raceList.contains(record))
						continue;
					raceList.add(record);
				}
			}
		}
		catch (Throwable exc)
		{
			Main.logException("Exception while generating race map", exc);
		}
		return raceList;
	}

	private List<PluginRecord> getRecordList(Plugin pl, List<Integer> recIDList, String recType)
	{
		List<PluginRecord> retRecList = new ArrayList<PluginRecord>();
		if ((recIDList == null) || (recIDList.isEmpty()))
			return retRecList;
		List<Integer> dupeRecIDList = new ArrayList<Integer>();
		dupeRecIDList.addAll(recIDList);
		PluginGroup recGroup = pl.getTopGroup(recType);

		if (recGroup != null)
		{
			List<PluginRecord> recordList = recGroup.getRecordList();
			for (PluginRecord record : recordList)
			{
				if ((!record.getRecordType().equals(recType)) || (record.isIgnored()))
					continue;
				if (!dupeRecIDList.contains(Integer.valueOf(record.getFormID())))
					continue;
				retRecList.add(record);
				dupeRecIDList.remove(new Integer(record.getFormID()));

				if (dupeRecIDList.isEmpty())
					return retRecList;

			}

		}

		try
		{
			List<String> masterListInCaps = pl.getMasterList();
			Master[] masters = new Master[masterListInCaps.size()];

			int index = 0;
			for (String masterName : masterListInCaps)
			{
				File masterFile = new File(this.pluginFile.getParent() + Main.fileSeparator + masterName);
				Master master = new Master(masterFile);
				master.load(null);
				masters[(index++)] = master;
			}

			ListIterator<String> lit = masterListInCaps.listIterator();
			while (lit.hasNext())
			{
				String tmpMaster = lit.next();
				lit.set(tmpMaster.toUpperCase());
			}
			for (int masterID = masters.length - 1; masterID > -1; masterID--)
			{
				Master master = masters[masterID];
				List<FormInfo> formList = master.getFormList();
				List<String> masterListforMaster = master.getMasterList();

				for (FormInfo formInfo : formList)
				{
					String recordType = formInfo.getRecordType();
					if (!recordType.equals(recType))
					{
						continue;
					}

					int formMasterID = (formInfo.getFormID() & 0xFF000000) >> 24;
					int newMasterID = formMasterID;
					String npcFormID = String.format("%08X", new Object[]
					{ Integer.valueOf(formInfo.getFormID()) });
					if (formMasterID <= masterListforMaster.size())
					{
						String formMasterName = formMasterID == masterListforMaster.size() ? master.getName() : masterListforMaster
								.get(formMasterID);
						int pluginMasterIdx = masterListInCaps.indexOf(formMasterName.toUpperCase());
						if (pluginMasterIdx == -1)
						{
							if (Main.debugMode)
							{
								System.out
										.printf("GenerateTask: Form ID %08X is modified in <%s> from the original in <%s>;  but <%s> is not in the master list for plugin <%s>.\n",
												new Object[]
												{ Integer.valueOf(formInfo.getFormID()), master.getName(), formMasterName, formMasterName,
														this.plugin.getName() });
							}
						}
						else
							newMasterID = pluginMasterIdx;
					}
					PluginRecord record = master.getRecord(formInfo.getFormID());

					if (newMasterID != formMasterID)
					{
						int newFormID = record.getFormID() & 0xFFFFFF | newMasterID << 24;
						record.setFormID(newFormID);
					}
					if (retRecList.contains(record))
						continue;
					if (!recIDList.contains(Integer.valueOf(record.getFormID())))
						continue;
					retRecList.add(record);
					dupeRecIDList.remove(new Integer(record.getFormID()));

					if (dupeRecIDList.isEmpty())
						return retRecList;
				}
			}

		}
		catch (Throwable exc)
		{
			Main.logException("Exception while generating NPC list", exc);
		}
		return retRecList;
	}

	private int addFakeVoiceRaces(JTree plTree)
	{
		int ARGONIAN_ID = 2049;
		int BRETON_ID = 2050;
		int DREMORA_ID = 2064;
		int HIGHELF_ID = 2062;
		int IMPERIAL_ID = 2063;
		int NORD_ID = 2066;
		int REDGUARD_ID = 2068;
		int ARGONIAN_SI_ID = 147433;
		int BRETON_SI_ID = 140540;
		int DREMORA_SI_ID = 229392;
		int HIGHELF_SI_ID = 102916;
		int IMPERIAL_SI_ID = 2311;
		int NORD_SI_ID = 140541;
		int REDGUARD_SI_ID = 3395;
		int DARKSEDUCER_SI_ID = 73870;
		int GOLDENSAINT_SI_ID = 73871;
		int SHEOGORATH_SI_ID = 340110;

		int[] IDArray =
		{ ARGONIAN_ID, BRETON_ID, DREMORA_ID, HIGHELF_ID, IMPERIAL_ID, NORD_ID, REDGUARD_ID, ARGONIAN_SI_ID, BRETON_SI_ID, DREMORA_SI_ID,
				HIGHELF_SI_ID, IMPERIAL_SI_ID, NORD_SI_ID, REDGUARD_SI_ID, DARKSEDUCER_SI_ID, GOLDENSAINT_SI_ID, SHEOGORATH_SI_ID };
		HashMap<Integer, String> EDIDMap = new HashMap<Integer, String>();
		HashMap<Integer, String> FULLMap = new HashMap<Integer, String>();
		HashMap<Integer, String> DESCMap = new HashMap<Integer, String>();

		EDIDMap.put(Integer.valueOf(ARGONIAN_ID), "ArgonianFakeVoice");
		EDIDMap.put(Integer.valueOf(BRETON_ID), "BretonFakeVoice");
		EDIDMap.put(Integer.valueOf(DREMORA_ID), "DremoraFakeVoice");
		EDIDMap.put(Integer.valueOf(HIGHELF_ID), "HighElfFakeVoice");
		EDIDMap.put(Integer.valueOf(IMPERIAL_ID), "ImperialFakeVoice");
		EDIDMap.put(Integer.valueOf(NORD_ID), "NordFakeVoice");
		EDIDMap.put(Integer.valueOf(REDGUARD_ID), "RedguardFakeVoice");
		EDIDMap.put(Integer.valueOf(ARGONIAN_SI_ID), "ArgonianSIFakeVoice");
		EDIDMap.put(Integer.valueOf(BRETON_SI_ID), "BretonSIFakeVoice");
		EDIDMap.put(Integer.valueOf(DREMORA_SI_ID), "DremoraSIFakeVoice");
		EDIDMap.put(Integer.valueOf(HIGHELF_SI_ID), "HighElfSIFakeVoice");
		EDIDMap.put(Integer.valueOf(IMPERIAL_SI_ID), "ImperialSIFakeVoice");
		EDIDMap.put(Integer.valueOf(NORD_SI_ID), "NordSIFakeVoice");
		EDIDMap.put(Integer.valueOf(REDGUARD_SI_ID), "RedguardSIFakeVoice");
		EDIDMap.put(Integer.valueOf(DARKSEDUCER_SI_ID), "DarkSeducerSIFakeVoice");
		EDIDMap.put(Integer.valueOf(GOLDENSAINT_SI_ID), "GoldenSaintSIFakeVoice");
		EDIDMap.put(Integer.valueOf(SHEOGORATH_SI_ID), "SheogorathSIFakeVoice");
		FULLMap.put(Integer.valueOf(ARGONIAN_ID), "Argonian");
		FULLMap.put(Integer.valueOf(BRETON_ID), "Breton");
		FULLMap.put(Integer.valueOf(DREMORA_ID), "Dremora");
		FULLMap.put(Integer.valueOf(HIGHELF_ID), "High Elf");
		FULLMap.put(Integer.valueOf(IMPERIAL_ID), "Imperial");
		FULLMap.put(Integer.valueOf(NORD_ID), "Nord");
		FULLMap.put(Integer.valueOf(REDGUARD_ID), "Redguard");
		FULLMap.put(Integer.valueOf(ARGONIAN_SI_ID), "Argonian");
		FULLMap.put(Integer.valueOf(BRETON_SI_ID), "Breton");
		FULLMap.put(Integer.valueOf(DREMORA_SI_ID), "Dremora");
		FULLMap.put(Integer.valueOf(HIGHELF_SI_ID), "High Elf");
		FULLMap.put(Integer.valueOf(IMPERIAL_SI_ID), "Imperial");
		FULLMap.put(Integer.valueOf(NORD_SI_ID), "Nord");
		FULLMap.put(Integer.valueOf(REDGUARD_SI_ID), "Redguard");
		FULLMap.put(Integer.valueOf(DARKSEDUCER_SI_ID), "Dark Seducer");
		FULLMap.put(Integer.valueOf(GOLDENSAINT_SI_ID), "Golden Saint");
		FULLMap.put(Integer.valueOf(SHEOGORATH_SI_ID), "Sheogorath");
		DESCMap.put(Integer.valueOf(ARGONIAN_ID), "Argonian Fake Voice. ONLY FOR LIP FILE GENERATION");
		DESCMap.put(Integer.valueOf(BRETON_ID), "Breton Fake Voice. ONLY FOR LIP FILE GENERATION");
		DESCMap.put(Integer.valueOf(DREMORA_ID), "Dremora Fake Voice. ONLY FOR LIP FILE GENERATION");
		DESCMap.put(Integer.valueOf(HIGHELF_ID), "High Elf Fake Voice. ONLY FOR LIP FILE GENERATION");
		DESCMap.put(Integer.valueOf(IMPERIAL_ID), "Imperial Fake Voice. ONLY FOR LIP FILE GENERATION");
		DESCMap.put(Integer.valueOf(NORD_ID), "Nord Fake Voice. ONLY FOR LIP FILE GENERATION");
		DESCMap.put(Integer.valueOf(REDGUARD_ID), "Redguard Fake Voice. ONLY FOR LIP FILE GENERATION");
		DESCMap.put(Integer.valueOf(ARGONIAN_SI_ID), "Argonian Fake Voice (SI Only). ONLY FOR LIP FILE GENERATION");
		DESCMap.put(Integer.valueOf(BRETON_SI_ID), "Breton Fake Voice (SI Only). ONLY FOR LIP FILE GENERATION");
		DESCMap.put(Integer.valueOf(DREMORA_SI_ID), "Dremora Fake Voice (SI Only). ONLY FOR LIP FILE GENERATION");
		DESCMap.put(Integer.valueOf(HIGHELF_SI_ID), "High Elf Fake Voice (SI Only). ONLY FOR LIP FILE GENERATION");
		DESCMap.put(Integer.valueOf(IMPERIAL_SI_ID), "Imperial Fake Voice (SI Only). ONLY FOR LIP FILE GENERATION");
		DESCMap.put(Integer.valueOf(NORD_SI_ID), "Nord Fake Voice (SI Only). ONLY FOR LIP FILE GENERATION");
		DESCMap.put(Integer.valueOf(REDGUARD_SI_ID), "Redguard Fake Voice (SI Only). ONLY FOR LIP FILE GENERATION");
		DESCMap.put(Integer.valueOf(DARKSEDUCER_SI_ID), "Dark Seducer Fake Voice (SI Only). ONLY FOR LIP FILE GENERATION");
		DESCMap.put(Integer.valueOf(GOLDENSAINT_SI_ID), "Golden Saint Fake Voice (SI Only). ONLY FOR LIP FILE GENERATION");
		DESCMap.put(Integer.valueOf(SHEOGORATH_SI_ID), "Sheogorath Fake Voice (SI Only). ONLY FOR LIP FILE GENERATION");

		ArrayList<PluginRecord> fakeVoiceRecords = new ArrayList<PluginRecord>();

		for (int i = 0; i < IDArray.length; i++)
		{
			List<PluginSubrecord> subrecords = new ArrayList<PluginSubrecord>(4);
			byte[] FULLbytes = FULLMap.get(Integer.valueOf(IDArray[i])).getBytes();
			byte[] FULLsubrecordData = new byte[FULLbytes.length + 1];
			System.arraycopy(FULLbytes, 0, FULLsubrecordData, 0, FULLbytes.length);
			FULLsubrecordData[FULLbytes.length] = 0;
			byte[] DESCbytes = DESCMap.get(Integer.valueOf(IDArray[i])).getBytes();
			byte[] DESCsubrecordData = new byte[DESCbytes.length + 1];
			System.arraycopy(DESCbytes, 0, DESCsubrecordData, 0, DESCbytes.length);
			DESCsubrecordData[DESCbytes.length] = 0;
			PluginSubrecord FULLSubrecord = new PluginSubrecord("RACE", "FULL", FULLsubrecordData);
			subrecords.add(FULLSubrecord);
			PluginSubrecord DESCSubrecord = new PluginSubrecord("RACE", "DESC", DESCsubrecordData);
			subrecords.add(DESCSubrecord);

			if (IDArray[i] == BRETON_ID)
			{
				byte[] VNAMData = new byte[8];
				SerializedElement.setInteger(0, VNAMData, 0);
				SerializedElement.setInteger(IMPERIAL_ID, VNAMData, 4);
				PluginSubrecord VNAMSubrecord = new PluginSubrecord("RACE", "VNAM", VNAMData);
				subrecords.add(VNAMSubrecord);
			}
			if (IDArray[i] == BRETON_SI_ID)
			{
				byte[] VNAMData = new byte[8];
				SerializedElement.setInteger(0, VNAMData, 0);
				SerializedElement.setInteger(IMPERIAL_SI_ID, VNAMData, 4);
				PluginSubrecord VNAMSubrecord = new PluginSubrecord("RACE", "VNAM", VNAMData);
				subrecords.add(VNAMSubrecord);
			}
			PluginRecord record = new PluginRecord("RACE", IDArray[i]);
			try
			{
				record.setSubrecords(subrecords);
				record.setEditorID(EDIDMap.get(Integer.valueOf(IDArray[i])));
			}
			catch (Exception ex)
			{
				continue;
			}
			fakeVoiceRecords.add(record);
		}

		int formsAltered = 0;
		PluginNode rootNode = (PluginNode) plTree.getModel().getRoot();
		List<FormInfo> formList = rootNode.getPlugin().getFormList();

		int childCount = rootNode.getChildCount();
		List<PluginRecord> groupList = new ArrayList<PluginRecord>();
		PluginGroup groupRace = null;
		GroupNode raceNode = null;
		for (int i = 0; i < childCount; i++)
		{
			GroupNode parentNode = (GroupNode) rootNode.getChildAt(i);
			if (!parentNode.getGroup().getGroupRecordType().equals("RACE"))
				continue;
			raceNode = parentNode;
			groupRace = parentNode.getGroup();
			groupList = parentNode.getGroup().getRecordList();
		}

		if ((groupRace == null) || (raceNode == null))
			return formsAltered;

		int raceCount = raceNode.getChildCount();
		HashSet<Integer> customRaceIDs = new HashSet<Integer>();
		for (PluginRecord rec : groupList)
		{
			if ((rec instanceof PluginGroup))
			{
				continue;
			}

			customRaceIDs.add(Integer.valueOf(rec.getFormID()));
		}

		for (PluginRecord svRec : fakeVoiceRecords)
		{
			if (customRaceIDs.contains(Integer.valueOf(svRec.getFormID())))
				continue;
			RecordNode recordNode = new RecordNode(svRec);
			try
			{
				createRecordChildren(recordNode);
			}
			catch (Exception ex)
			{
				continue;
			}
			raceNode.add(recordNode);
			groupList.add(svRec);
			formList.add(new FormInfo(svRec, svRec.getRecordType(), svRec.getFormID(), svRec.getEditorID()));
			formsAltered++;
		}

		if (formsAltered != 0)
		{
			int[] indices = new int[formsAltered];
			for (int i = 0; i < formsAltered; i++)
			{
				indices[i] = (raceCount + i);
			}
			((DefaultTreeModel) plTree.getModel()).nodesWereInserted(raceNode, indices);
		}
		return formsAltered;
	}

	private int addRecordsToGroup(JTree plTree, String topGroup, List<PluginRecord> records)
	{
		int formsAltered = 0;
		PluginNode rootNode = (PluginNode) plTree.getModel().getRoot();
		List<FormInfo> formList = rootNode.getPlugin().getFormList();

		int childCount = rootNode.getChildCount();
		List<PluginRecord> groupList = new ArrayList<PluginRecord>();
		PluginGroup groupNeeded = null;
		GroupNode neededNode = null;
		for (int i = 0; i < childCount; i++)
		{
			GroupNode parentNode = (GroupNode) rootNode.getChildAt(i);
			if (!parentNode.getGroup().getGroupRecordType().equals(topGroup))
				continue;
			neededNode = parentNode;
			groupNeeded = parentNode.getGroup();
			groupList = parentNode.getGroup().getRecordList();
		}

		if ((groupNeeded == null) || (neededNode == null))
			return formsAltered;

		int recCount = neededNode.getChildCount();

		for (PluginRecord svRec : records)
		{
			RecordNode recordNode = new RecordNode(svRec);
			try
			{
				createRecordChildren(recordNode);
			}
			catch (Exception ex)
			{
				continue;
			}
			neededNode.add(recordNode);
			if (groupList.contains(svRec))
				continue;
			groupList.add(svRec);
			formList.add(new FormInfo(svRec, svRec.getRecordType(), svRec.getFormID(), svRec.getEditorID()));
			formsAltered++;
		}

		if (formsAltered != 0)
		{
			int[] indices = new int[formsAltered];
			for (int i = 0; i < formsAltered; i++)
			{
				indices[i] = (recCount + i);
			}
			((DefaultTreeModel) plTree.getModel()).nodesWereInserted(neededNode, indices);
		}
		return formsAltered;
	}

	private int removeNonLipSynchObjects(Plugin pl)
	{
		int formsRemoved = 0;
		HashSet<String> keepThese = new HashSet<String>();
		HashSet<Integer> questFormIDs = new HashSet<Integer>();
		HashSet<Integer> keptDIALs = new HashSet<Integer>();
		HashSet<Integer> keptINFOs = new HashSet<Integer>();
		keepThese.add("DIAL");
		keepThese.add("INFO");
		keepThese.add("QUST");
		keepThese.add("RACE");
		keepThese.add("NPC_");
		keepThese.add("FACT");
		List<FormInfo> allForms = pl.getFormList();

		for (FormInfo form : allForms)
		{
			if (form.getRecordType().equals("QUST"))
			{
				PluginRecord pluginRec = (PluginRecord) form.getSource();
				if ((pluginRec != null) && (!pluginRec.isIgnored()))
					questFormIDs.add(Integer.valueOf(form.getFormID()));
			}
			if (keepThese.contains(form.getRecordType()))
				continue;
			PluginRecord pluginRec = (PluginRecord) form.getSource();
			if (pluginRec == null)
				continue;
			pluginRec.setIgnore(true);
			formsRemoved++;
		}
		List<FormInfo> questDIALs;
		for (Iterator<Integer> it = questFormIDs.iterator(); it.hasNext();)
		{
			int questID = it.next().intValue();

			questDIALs = findQuestDials(pl, questID);
			for (FormInfo form2 : questDIALs)
			{
				keptDIALs.add(Integer.valueOf(form2.getFormID()));
			}
			List<FormInfo> questINFOs = findQuestInfos(pl, questID);
			for (FormInfo form3 : questINFOs)
			{
				keptINFOs.add(Integer.valueOf(form3.getFormID()));
			}
		}

		for (FormInfo form : allForms)
		{
			if ((form.getRecordType().equals("DIAL")) && (!keptDIALs.contains(Integer.valueOf(form.getFormID()))))
			{
				PluginRecord pluginRec = (PluginRecord) form.getSource();
				if ((pluginRec == null) || (pluginRec.isIgnored()))
					continue;
				pluginRec.setIgnore(true);
				formsRemoved++;
			}
			else
			{
				if ((!form.getRecordType().equals("INFO")) || (keptINFOs.contains(Integer.valueOf(form.getFormID()))))
					continue;
				PluginRecord pluginRec = (PluginRecord) form.getSource();
				if ((pluginRec == null) || (pluginRec.isIgnored()))
					continue;
				pluginRec.setIgnore(true);
				formsRemoved++;
			}

		}

		List<PluginGroup> groupList = pl.getGroupList();
		for (PluginGroup group : groupList)
		{
			group.removeIgnoredRecords();
		}

		return formsRemoved;
	}

	private int removeRecordsNotOnList(Plugin pl, List<Integer> NPCIDList, String recType)
	{
		int formsRemoved = 0;
		List<FormInfo> allForms = pl.getFormList();

		for (FormInfo form : allForms)
		{
			if (!form.getRecordType().equals(recType))
				continue;
			if (form.getFormID() == 20)
				continue;
			PluginRecord pluginRec = (PluginRecord) form.getSource();
			if ((pluginRec == null) || (pluginRec.isIgnored()) || (NPCIDList.contains(Integer.valueOf(form.getFormID()))))
				continue;
			pluginRec.setIgnore(true);
			formsRemoved++;
		}

		List<PluginGroup> groupList = pl.getGroupList();
		for (PluginGroup group : groupList)
		{
			group.removeIgnoredRecords();
		}
		return formsRemoved;
	}

	private int removeAllObjects(Plugin pl)
	{
		int formsRemoved = 0;
		List<FormInfo> allForms = pl.getFormList();

		for (FormInfo form : allForms)
		{
			PluginRecord pluginRec = (PluginRecord) form.getSource();
			if (pluginRec == null)
				continue;
			pluginRec.setIgnore(true);
			formsRemoved++;
		}
		List<PluginGroup> groupList = pl.getGroupList();
		for (PluginGroup group : groupList)
		{
			group.removeIgnoredRecords();
		}
		pl.resetFormList();
		pl.resetFormMap();

		return formsRemoved;
	}

	private int reduceLipSynchObjects(Plugin pl)
	{
		int formsAltered = 0;
		HashSet<String> keptRACEFields = new HashSet<String>();
		HashSet<String> keptINFOFields = new HashSet<String>();
		HashSet<String> keptQUSTFields = new HashSet<String>();
		HashSet<String> keptNPCFields = new HashSet<String>();
		HashSet<String> keptFACTFields = new HashSet<String>();
		HashSet<Integer> questFormIDs = new HashSet<Integer>();
		ArrayList<Integer> factionFormIDs = new ArrayList<Integer>();
		HashSet<Integer> keptCTDAFuncs = new HashSet<Integer>();
		keptRACEFields.add("EDID");
		keptRACEFields.add("FULL");
		keptRACEFields.add("DESC");
		keptRACEFields.add("VNAM");
		keptQUSTFields.add("EDID");
		keptQUSTFields.add("FULL");
		keptQUSTFields.add("CTDA");
		keptINFOFields.add("QSTI");
		keptINFOFields.add("PNAM");
		keptINFOFields.add("TRDT");
		keptINFOFields.add("NAM1");
		keptINFOFields.add("NAM2");
		keptINFOFields.add("CTDA");
		keptNPCFields.add("EDID");
		keptNPCFields.add("FULL");
		keptNPCFields.add("ACBS");
		keptNPCFields.add("RNAM");
		keptNPCFields.add("SNAM");
		keptFACTFields.add("EDID");
		keptFACTFields.add("FULL");
		keptCTDAFuncs.add(Integer.valueOf(72));
		keptCTDAFuncs.add(Integer.valueOf(70));
		keptCTDAFuncs.add(Integer.valueOf(69));
		keptCTDAFuncs.add(Integer.valueOf(71));
		PluginGroup QUSTGroup = null;
		PluginGroup FACTGroup = null;

		List<PluginGroup> groupList = pl.getGroupList();
		for (PluginGroup group : groupList)
		{
			String groupRecordType = group.getGroupRecordType();
			if (groupRecordType.equals("QUST"))
			{
				QUSTGroup = group;
			}
			if (!groupRecordType.equals("FACT"))
				continue;
			FACTGroup = group;
		}

		if (QUSTGroup == null)
			return formsAltered;
		List<PluginRecord> recordList = QUSTGroup.getRecordList();
		List<PluginRecord> factList = FACTGroup == null ? new ArrayList<PluginRecord>() : FACTGroup.getRecordList();
		for (PluginRecord rec : recordList)
		{
			if (((rec instanceof PluginGroup)) || (!rec.getRecordType().equals("QUST")))
				continue;
			questFormIDs.add(Integer.valueOf(rec.getFormID()));
		}
		for (PluginRecord rec : factList)
		{
			if (((rec instanceof PluginGroup)) || (!rec.getRecordType().equals("FACT")))
				continue;
			factionFormIDs.add(Integer.valueOf(rec.getFormID()));
		}
		formsAltered += removeQuestsFromDIALs(pl, questFormIDs);
		formsAltered += cleanINFOsForLipSynch(pl, keptINFOFields, keptCTDAFuncs);
		formsAltered += cleanQUSTsForLipSynch(pl, keptQUSTFields, keptCTDAFuncs);
		formsAltered += cleanGroupForLipSynch(pl, "RACE", keptRACEFields);
		formsAltered += cleanGroupForLipSynch(pl, "NPC_", keptNPCFields);
		formsAltered += cleanGroupForLipSynch(pl, "FACT", keptFACTFields);
		formsAltered += purgeFactions(pl, factionFormIDs);

		return formsAltered;
	}

	private int changeQuestTopics(Plugin pl, int oldQID, int newQID, int lastFormID, boolean cloneDIALs) throws DataFormatException,
			IOException, PluginException
	{
		List<FormInfo> allQuestDials = findQuestDials(pl, oldQID);
		int dialFormIDsUsed = 0;
		int infoFormIDsUsed = 0;
		int lastFormIDUsed = lastFormID;
		String oldName;
		for (FormInfo form : allQuestDials)
		{
			PluginRecord pluginRec = (PluginRecord) form.getSource();
			if ((cloneDIALs) && (dialToBeCloned(pluginRec, oldQID)))
			{
				int oldDialID = pluginRec.getFormID();
				PluginGroup topicGroup = findTopicGroup(pl, oldDialID);
				if (topicGroup == null)
				{
					if (!Main.debugMode)
						continue;
					System.out.printf("changeQuestTopics: No INFO group found for DIAL record [%08X]\n", new Object[]
					{ Integer.valueOf(oldDialID) });
				}
				else
				{
					lastFormIDUsed++;
					pluginRec.setFormID(lastFormIDUsed);
					oldName = pluginRec.getEditorID();
					try
					{
						FormInfo dialFormInfo = pl.getFormMap().get(Integer.valueOf(oldDialID));
						pluginRec.setFormID(lastFormIDUsed);
						dialFormInfo.setFormID(lastFormIDUsed);
						dialFormInfo.setMergedFormID(lastFormIDUsed);
						pluginRec.setEditorID(oldName + "GECKO");
						dialFormInfo.setEditorID(oldName + "GECKO");
						dialFormInfo.setMergedEditorID(oldName + "GECKO");
						pl.getFormMap().remove(Integer.valueOf(oldDialID));
						pl.getFormMap().put(new Integer(lastFormIDUsed), dialFormInfo);
						pluginRec.setEditorID(oldName + "GECKO");
					}
					catch (Exception ex)
					{
						ex.printStackTrace(System.out);
					}
					if (Main.debugMode)
					{
						System.out.printf("Dialogue Form ID Change: Changed DIAL record with form ID %08X and name " + oldName
								+ " to ID %08X and name %s \n", new Object[]
						{ Integer.valueOf(oldDialID), Integer.valueOf(pluginRec.getFormID()), pluginRec.getEditorID() });
					}
					int oldGroupID = topicGroup.getGroupParentID();
					topicGroup.setGroupParentID(lastFormIDUsed);
					if (Main.debugMode)
					{
						System.out.printf("INFO Group Parent Change: Changed parent form ID %08X to parent form ID %08X \n", new Object[]
						{ Integer.valueOf(oldGroupID), Integer.valueOf(topicGroup.getGroupParentID()) });
					}
					boolean changed = pluginRec.changeSubrecord("QSTI", new Integer(oldQID), new Integer(newQID));
					if ((changed) && (Main.debugMode))
					{
						System.out.printf(
								"changeQuestTopics: Changed QSTI Subrecord with value %08X to value %08X in DIAL record [%08X]\n",
								new Object[]
								{ Integer.valueOf(oldQID), Integer.valueOf(newQID), Integer.valueOf(pluginRec.getFormID()) });
					}
					dialFormIDsUsed++;
				}
			}
			else
			{
				boolean changed = pluginRec.addAdditionalSubrecord("QSTI", new Integer(newQID));
				if ((!changed) || (!Main.debugMode))
					continue;
				System.out.printf("changeQuestTopics: Added QSTI Subrecord with value %08X to DIAL record " + pluginRec.getEditorID()
						+ " [%08X]\n", new Object[]
				{ Integer.valueOf(newQID), Integer.valueOf(pluginRec.getFormID()) });
			}
		}

		List<FormInfo> allQuestInfos = findQuestInfos(pl, oldQID);
		ArrayList<Integer> oldForms = new ArrayList<Integer>(allQuestInfos.size());
		ArrayList<Integer> newForms = new ArrayList<Integer>(allQuestInfos.size());
		int oldFormID;
		for (FormInfo form : allQuestInfos)
		{
			lastFormIDUsed++;
			oldFormID = form.getFormID();
			oldForms.add(new Integer(form.getFormID()));
			newForms.add(new Integer(lastFormIDUsed));
			PluginRecord pluginRec = (PluginRecord) form.getSource();
			pluginRec.setFormID(lastFormIDUsed);
			form.setFormID(lastFormIDUsed);
			form.setMergedFormID(lastFormIDUsed);
			pl.getFormMap().remove(Integer.valueOf(oldFormID));
			pl.getFormMap().put(new Integer(lastFormIDUsed), form);
			boolean changed = pluginRec.changeSubrecord("QSTI", new Integer(oldQID), new Integer(newQID));
			if ((changed) && (Main.debugMode))
			{
				System.out.printf("changeQuestTopics: Changed QSTI Subrecord with value %08X to value %08X in INFO record [%08X]\n",
						new Object[]
						{ Integer.valueOf(oldQID), Integer.valueOf(newQID), Integer.valueOf(pluginRec.getFormID()) });
			}

			infoFormIDsUsed++;
		}

		List<FormInfo> allInfos = findAllInfos(pl);

		for (FormInfo form : allInfos)
		{
			PluginRecord pluginRec = (PluginRecord) form.getSource();

			for (int i = 0; i < infoFormIDsUsed; i++)
			{
				boolean changed = pluginRec.changeSubrecord("PNAM", oldForms.get(i), newForms.get(i));
				if ((changed) && (Main.debugMode))
				{
					System.out.printf("changeQuestTopics: Changed PNAM Subrecord with value %08X to value %08X in INFO record [%08X]\n",
							new Object[]
							{ oldForms.get(i), newForms.get(i), Integer.valueOf(pluginRec.getFormID()) });
				}

				if (changed)
					break;
			}
		}
		return dialFormIDsUsed + infoFormIDsUsed;
	}

	private List<FormInfo> findQuestDials(Plugin pl, int questID)
	{
		ArrayList<FormInfo> questDials = new ArrayList<FormInfo>();
		List<FormInfo> allForms = pl.getFormList();

		for (FormInfo form : allForms)
		{
			if (!form.getRecordType().equals("DIAL"))
			{
				continue;
			}
			PluginRecord pluginRec = (PluginRecord) form.getSource();
			if (pluginRec == null)
				continue;

			List<PluginSubrecord> pluginSubrecs;
			try
			{
				pluginSubrecs = pluginRec.getSubrecords();
			}
			catch (Exception ex)
			{

				continue;
			}

			for (PluginSubrecord pluginSubrec : pluginSubrecs)
			{
				if (!pluginSubrec.getSubrecordType().equals("QSTI"))
				{
					continue;
				}

				byte[] subrecordData;
				try
				{
					subrecordData = pluginSubrec.getSubrecordData();
				}
				catch (Exception ex)
				{

					continue;
				}

				int infoQuestID = SerializedElement.getInteger(subrecordData, 0);
				if (questID != infoQuestID)
					continue;
				questDials.add(form);
			}

		}

		return questDials;
	}

	private int highestFormID(Plugin pl)
	{
		List<FormInfo> allForms = pl.getFormList();
		int highFormID = allForms.get(0).getFormID();
		for (FormInfo form : allForms)
		{
			int formID = form.getFormID();
			if (formID <= highFormID)
				continue;
			highFormID = formID;
		}
		return highFormID;
	}

	private int modifyWorldspace(Plugin pl, PluginRecord pluginRec, int highFormID)
	{
		int tmpHighFormID = highFormID;
		int numNewFormIDs = 0;
		int oldWSID = pluginRec.getFormID();
		String oldName = pluginRec.getEditorID();
		tmpHighFormID++;
		numNewFormIDs++;
		PluginGroup WRLDGroup = pl.getTopGroup("WRLD");
		if (WRLDGroup == null)
			return 0;
		List<PluginRecord> recList = WRLDGroup.getRecordList();
		PluginRecord worldspace = null;
		PluginGroup worldGroup = null;
		boolean wsFound = false;
		for (int i = 0; i < recList.size(); i += 2)
		{
			worldspace = recList.get(i);
			worldGroup = (PluginGroup) recList.get(i + 1);
			if (worldspace.getFormID() != pluginRec.getFormID())
				continue;
			wsFound = true;
			break;
		}

		if (!wsFound)
			return 0;
		HashMap<Integer, Integer> formIDsChanged = new HashMap<Integer, Integer>();
		try
		{
			oldWSID = worldspace.getFormID();
			worldspace.setFormID(tmpHighFormID);
			worldspace.setEditorID(oldName + "GECKO");
			formIDsChanged.put(Integer.valueOf(oldWSID), Integer.valueOf(tmpHighFormID));
			worldGroup.setGroupParentID(tmpHighFormID++);
			numNewFormIDs++;
		}
		catch (Exception ex)
		{
			ex.printStackTrace(System.out);
		}
		if (Main.debugMode)
		{
			System.out.printf("Worldspace Form ID Change: Changed WRLD record with form ID %08X and name " + oldName
					+ " to ID %08X and name %s \n", new Object[]
			{ Integer.valueOf(oldWSID), Integer.valueOf(worldspace.getFormID()), worldspace.getEditorID() });
		}

		List<PluginRecord> regionsInWS = pl.getRegionsInWorldspace(oldWSID);
		HashMap<Integer, Integer> regionIDsChanged = new HashMap<Integer, Integer>();
		String oldRegionName;
		for (PluginRecord region : regionsInWS)
		{
			try
			{
				int oldRegionID = region.getFormID();
				region.setFormID(tmpHighFormID);
				oldRegionName = region.getEditorID();
				region.setEditorID(oldRegionName + "GECKO");
				region.changeSubrecord("WNAM", Integer.valueOf(oldWSID), Integer.valueOf(worldspace.getFormID()));
				formIDsChanged.put(Integer.valueOf(oldRegionID), Integer.valueOf(tmpHighFormID));
				regionIDsChanged.put(Integer.valueOf(oldRegionID), Integer.valueOf(tmpHighFormID++));
				numNewFormIDs++;
				if (!Main.debugMode)
					continue;
				System.out.printf("Region Form ID Change: Changed REGN record with form ID %08X and name " + oldRegionName
						+ " to ID %08X and name %s \n", new Object[]
				{ Integer.valueOf(oldRegionID), Integer.valueOf(region.getFormID()), region.getEditorID() });
			}
			catch (Exception ex)
			{
				ex.printStackTrace(System.out);
			}
		}
		List<PluginRecord> worldGroupList = worldGroup.getRecordList();
		List<PluginGroup> blockList = new ArrayList<PluginGroup>();
		for (PluginRecord wgList : worldGroupList)
		{
			if (!(wgList instanceof PluginGroup))
			{
				int oldID = wgList.getFormID();
				wgList.setFormID(tmpHighFormID);
				formIDsChanged.put(Integer.valueOf(oldID), Integer.valueOf(tmpHighFormID));
				numNewFormIDs++;
			}
			else
			{
				switch (((PluginGroup) wgList).getGroupType())
				{
					case 6:
						HashMap<Integer, Integer> formIDMap = modifyCellGroup((PluginGroup) wgList, tmpHighFormID);
						tmpHighFormID += formIDMap.size() + 1;
						numNewFormIDs += formIDMap.size();
						formIDsChanged.putAll(formIDMap);
						break;
					case 4:
						blockList.add((PluginGroup) wgList);
					case 5:
				}
			}
		}
		List<PluginRecord> subBlockList;
		for (PluginGroup block : blockList)
		{
			subBlockList = block.getRecordList();
			for (PluginRecord subBlock : subBlockList)
			{
				List<PluginRecord> cellList = ((PluginGroup) subBlock).getRecordList();
				for (PluginRecord cell : cellList)
				{
					if ((cell instanceof PluginGroup))
					{
						HashMap<Integer, Integer> formIDMap = modifyCellGroup((PluginGroup) cell, tmpHighFormID);
						tmpHighFormID += formIDMap.size() + 1;
						numNewFormIDs += formIDMap.size();
						formIDsChanged.putAll(formIDMap);
					}
					if (!cell.getRecordType().equals("CELL"))
						continue;
					int oldID = cell.getFormID();
					cell.setFormID(tmpHighFormID);
					formIDsChanged.put(Integer.valueOf(oldID), Integer.valueOf(tmpHighFormID));
					numNewFormIDs++;
					PluginSubrecord editorID = null;
					try
					{
						editorID = cell.getSubrecord("EDID");
					}
					catch (Exception localException1)
					{
					}

					if (editorID != null)
					{
						String newEDID = editorID.getDisplayData();
						try
						{
							cell.setEditorID(newEDID + "GECKO");
						}
						catch (Exception localException2)
						{
						}
					}
					try
					{
						PluginSubrecord regionData = cell.getSubrecord("XCLR");
						if (regionData != null)
						{
							byte[] oldSubData = regionData.getSubrecordData();
							int[] regionArray = SerializedElement.getIntegerArray(oldSubData, 0);
							ArrayList<Integer> newRegionArray = new ArrayList<Integer>();
							for (int i = 0; i < regionArray.length; i++)
							{
								Integer newRegionID = regionIDsChanged.get(new Integer(regionArray[i]));
								if (newRegionID == null)
									continue;
								newRegionArray.add(newRegionID);
							}
							if (newRegionArray.size() > 0)
							{
								byte[] newSubData = new byte[4 * newRegionArray.size()];
								int[] intArray = new int[newRegionArray.size()];
								for (int i = 0; i < newRegionArray.size(); i++)
									intArray[i] = newRegionArray.get(i).intValue();
								SerializedElement.setIntegerArray(intArray, newSubData, 0);
								cell.changeSubrecord("XCLR", oldSubData, newSubData);
							}
							else
							{
								cell.removeSubrecords("XCLR");
							}
						}
					}
					catch (Exception localException3)
					{
					}
				}
			}
		}

		List<PluginRecord> childWSs = pl.getChildWorldspaces(oldWSID);
		for (PluginRecord world : childWSs)
		{
			try
			{
				world.changeSubrecord("WNAM", Integer.valueOf(oldWSID), Integer.valueOf(worldspace.getFormID()));
			}
			catch (Exception localException4)
			{
			}
			numNewFormIDs += modifyWorldspace(pl, world, tmpHighFormID);
		}

		Set<Integer> oldIDSet = formIDsChanged.keySet();
		for (Integer oldID : oldIDSet)
		{
			try
			{
				FormInfo changedFormInfo = pl.getFormMap().get(oldID);
				String oldIDstr = String.format("%08X", new Object[]
				{ oldID });
				if (changedFormInfo == null)
				{
					if (Main.debugMode)
					{
						System.out.printf("No form info for former ID " + oldIDstr + "\n", new Object[0]);
					}
				}
				PluginRecord changedRec = (PluginRecord) changedFormInfo.getSource();
				if (changedRec == null)
				{
					if (Main.debugMode)
					{
						System.out.printf("No source record for former ID " + oldIDstr, new Object[0]);
					}
				}
				changedFormInfo.setFormID(changedRec.getFormID());
				changedFormInfo.setMergedFormID(changedRec.getFormID());
				changedFormInfo.setEditorID(changedRec.getEditorID());
				changedFormInfo.setMergedEditorID(changedRec.getEditorID());
				pl.getFormMap().remove(oldID);
				pl.getFormMap().put(new Integer(formIDsChanged.get(oldID).intValue()), changedFormInfo);
			}
			catch (Exception ex)
			{
				ex.printStackTrace(System.out);
			}
		}

		return numNewFormIDs;
	}

	private HashMap<Integer, Integer> modifyCellGroup(PluginGroup cellGroup, int startFormID)
	{
		HashMap<Integer, Integer> formIDMap = new HashMap<Integer, Integer>();
		if (cellGroup.getGroupType() != 6)
			return formIDMap;
		int subGroupID = startFormID;
		cellGroup.setGroupParentID(startFormID++);
		List<PluginRecord> cellList = cellGroup.getRecordList();
		for (PluginRecord cell : cellList)
		{
			if ((cell instanceof PluginGroup))
			{
				((PluginGroup) cell).setGroupParentID(subGroupID);
				List<PluginRecord> itemList = ((PluginGroup) cell).getAllPluginRecords();
				for (PluginRecord item : itemList)
				{
					int oldID = item.getFormID();
					item.setFormID(startFormID);
					formIDMap.put(Integer.valueOf(oldID), Integer.valueOf(startFormID++));
					PluginSubrecord editorID = null;
					try
					{
						editorID = item.getSubrecord("EDID");
					}
					catch (Exception localException)
					{
					}

					if (editorID == null)
						continue;
					String newEDID = editorID.getDisplayData();
					try
					{
						item.setEditorID(newEDID + "GECKO");
					}
					catch (Exception localException1)
					{
					}
				}
			}
			else
			{
				int oldID = cell.getFormID();
				cell.setFormID(startFormID);
				formIDMap.put(Integer.valueOf(oldID), Integer.valueOf(startFormID++));
				PluginSubrecord editorID = null;
				try
				{
					editorID = cell.getSubrecord("EDID");
				}
				catch (Exception localException4)
				{
				}

				if (editorID == null)
					continue;
				String newEDID = editorID.getDisplayData();
				try
				{
					cell.setEditorID(newEDID + "GECKO");
				}
				catch (Exception localException3)
				{
				}
			}
		}
		return formIDMap;
	}

	private int modifyQuestFormID(Plugin pl, PluginRecord pluginRec, int highFormID, String howMany)
	{
		int tmpHighFormID = highFormID;
		int numNewFormIDs = 0;
		int oldQID = pluginRec.getFormID();
		String oldName = pluginRec.getEditorID();

		tmpHighFormID++;
		numNewFormIDs++;
		try
		{
			FormInfo questFormInfo = pl.getFormMap().get(Integer.valueOf(oldQID));
			pluginRec.setFormID(tmpHighFormID);
			questFormInfo.setFormID(tmpHighFormID);
			questFormInfo.setMergedFormID(tmpHighFormID);
			pluginRec.setEditorID(oldName + "GECKO");
			questFormInfo.setEditorID(oldName + "GECKO");
			questFormInfo.setMergedEditorID(oldName + "GECKO");
			pl.getFormMap().remove(Integer.valueOf(oldQID));
			pl.getFormMap().put(new Integer(tmpHighFormID), questFormInfo);
		}
		catch (Exception ex)
		{
			ex.printStackTrace(System.out);
		}
		if (Main.debugMode)
		{
			System.out.printf("Quest Form ID Change: Changed QUST record with form ID %08X and name " + oldName
					+ " to ID %08X and name %s \n", new Object[]
			{ Integer.valueOf(oldQID), Integer.valueOf(pluginRec.getFormID()), pluginRec.getEditorID() });
		}
		if ((howMany.equals("RelatedInfos")) || (howMany.equals("UnsharedDials")))
		{
			boolean cloneDIALs = howMany.equals("UnsharedDials");
			try
			{
				int numNewDialIDs = changeQuestTopics(pl, oldQID, tmpHighFormID, tmpHighFormID, cloneDIALs);
				numNewFormIDs += numNewDialIDs;
			}
			catch (Exception ex)
			{
				ex.printStackTrace(System.out);
			}
		}
		return numNewFormIDs;
	}

	private boolean dialToBeCloned(PluginRecord pluginRec, int oldQID)
	{
		if (((pluginRec instanceof PluginGroup)) || (!pluginRec.getRecordType().equals("DIAL")))
			return false;
		int dialMod = pluginRec.getFormID() >>> 24;
		int questMod = oldQID >>> 24;
		if (dialMod != questMod)
			return false;
		boolean foundOnlyOne = false;

		List<PluginSubrecord> pluginSubrecs;
		try
		{
			pluginSubrecs = pluginRec.getSubrecords();
		}
		catch (Exception ex)
		{

			return false;
		}

		for (PluginSubrecord pluginSubrec : pluginSubrecs)
		{
			if (!pluginSubrec.getSubrecordType().equals("QSTI"))
				continue;
			if (foundOnlyOne)
			{
				foundOnlyOne = false;
				break;
			}
			foundOnlyOne = true;

			byte[] subrecordData;
			try
			{
				subrecordData = pluginSubrec.getSubrecordData();
			}
			catch (Exception ex)
			{

				break;
			}

			int infoQuestID = SerializedElement.getInteger(subrecordData, 0);
			if (oldQID == infoQuestID)
				continue;
			foundOnlyOne = false;
			break;
		}

		return foundOnlyOne;
	}

	private PluginGroup findTopicGroup(Plugin pl, int dialID)
	{
		List<PluginGroup> topList = pl.getGroupList();
		PluginGroup returnGroup = null;
		PluginGroup dialGroup = null;
		for (PluginGroup topGroup : topList)
		{
			if ((topGroup.getGroupType() != 0) || (!topGroup.getGroupRecordType().equals("DIAL")))
				continue;
			dialGroup = topGroup;
			break;
		}

		if (dialGroup == null)
			return null;
		List<PluginRecord> dialList = dialGroup.getRecordList();
		for (PluginRecord infoGroup : dialList)
		{
			if ((!(infoGroup instanceof PluginGroup)) || (((PluginGroup) infoGroup).getGroupType() != 7)
					|| (((PluginGroup) infoGroup).getGroupParentID() != dialID))
				continue;
			returnGroup = (PluginGroup) infoGroup;
			break;
		}

		return returnGroup;
	}

	private List<PluginGroup> findAllTopicGroups(Plugin pl)
	{
		List<PluginGroup> topList = pl.getGroupList();
		List<PluginGroup> returnGroups = new ArrayList<PluginGroup>();
		PluginGroup dialGroup = null;
		for (PluginGroup topGroup : topList)
		{
			if ((topGroup.getGroupType() != 0) || (!topGroup.getGroupRecordType().equals("DIAL")))
				continue;
			dialGroup = topGroup;
			break;
		}

		if (dialGroup == null)
			return returnGroups;
		List<PluginRecord> dialList = dialGroup.getRecordList();
		for (PluginRecord infoGroup : dialList)
		{
			if ((!(infoGroup instanceof PluginGroup)) || (((PluginGroup) infoGroup).getGroupType() != 7))
				continue;
			returnGroups.add((PluginGroup) infoGroup);
			break;
		}

		return returnGroups;
	}

	private void dumpNPCDialogue(Plugin pl, PluginRecord plRec, boolean append)
	{
		if (!plRec.getRecordType().equals("NPC_"))
			return;
		File file = getDialogueDumpFile("NPC_");
		if (file == null)
			return;
		if (!append)
		{
			int dontLeave = JOptionPane.showConfirmDialog(this,
					"This will overwrite whatever data exists in the file.\n Do you wish to continue?", "Possible Content Overwrite", 2, 2);
			if (dontLeave != 0)
				return;

		}

		if (this.raceList == null)
		{
			this.raceList = getRaceList(pl);
		}
		PluginSubrecord nameRec = null;
		PluginSubrecord raceRec = null;
		PluginSubrecord configRec = null;
		byte[] raceBytes = null;
		byte[] configBytes = null;
		try
		{
			nameRec = plRec.getSubrecord("FULL");
			raceRec = plRec.getSubrecord("RNAM");
			raceBytes = raceRec.getSubrecordData();
			configRec = plRec.getSubrecord("ACBS");
			configBytes = configRec.getSubrecordData();
		}
		catch (Exception localException1)
		{
		}
		String NPCName = nameRec == null ? "Unknown" : nameRec.getDisplayData();
		String NPCRaceFormID = raceRec == null ? "of unknown race" : raceRec.getDisplayData();
		String NPCRaceEditorID = "Unknown";
		String NPCRaceName = "Unknown";
		String gender = (SerializedElement.getInteger(configBytes, 0) & 0x1) == 0 ? "male" : configRec == null ? "gender-unknown"
				: "female";
		if (raceRec != null)
		{
			int raceID = SerializedElement.getInteger(raceBytes, 0);
			for (PluginRecord tmpRaceRec : this.raceList)
			{
				if (tmpRaceRec.getFormID() != raceID)
					continue;
				NPCRaceEditorID = tmpRaceRec.getEditorID();
				try
				{
					PluginSubrecord raceNameRec = tmpRaceRec.getSubrecord("FULL");
					if (raceNameRec == null)
						break;
					NPCRaceName = raceNameRec.getDisplayData();
				}
				catch (Exception localException2)
				{
				}
			}
		}
		String headerLine = "TES4GECKO DIALOGUE DUMP for NPC named " + NPCName + ", a " + gender + " " + NPCRaceName + " [race ID "
				+ NPCRaceEditorID + "] with editor ID " + plRec.getEditorID() + " and form ID " + String.format("%08X", new Object[]
				{ new Integer(plRec.getFormID()) }) + "\n";
		List<FormInfo> condInfos = findInfosWithCondition(pl, 72, 0, Integer.valueOf(plRec.getFormID()), null, 1.0F);
		if (condInfos.size() == 0)
		{
			JOptionPane.showMessageDialog(this, "No dialogue data was found for this NPC");
			return;
		}

		FileOutputStream outFile = null;
		try
		{
			outFile = new FileOutputStream(file, append);
			outFile.write(headerLine.getBytes());
			for (FormInfo form : condInfos)
			{
				outFile.write(dialogueForInfo(pl, form).getBytes());
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			return;
		}
		finally
		{
			try
			{
				if (outFile != null)
					outFile.close();
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
				return;
			}
		}
		JOptionPane.showMessageDialog(this, "Dialogue data for NPC " + plRec.getEditorID() + "\nwritten to file " + file.getName());
	}

	private void masterModReport(Plugin pl, boolean append)
	{
		File file = getMasterModFile();
		if (file == null)
			return;
		if (!append)
		{
			int dontLeave = JOptionPane.showConfirmDialog(this,
					"This will overwrite whatever data exists in the file.\n Do you wish to continue?", "Possible Content Overwrite", 2, 2);
			if (dontLeave != 0)
				return;
		}
		String headerLine = "TES4GECKO MASTER ALTERATION REPORT for plugin <" + pl.getName() + ">\n";
		List<String> alteredList = getAlteredList(pl);
		if (alteredList.size() == 0)
		{
			JOptionPane.showMessageDialog(this, "No master objects were altered in this plugin");
			return;
		}

		FileOutputStream outFile = null;
		try
		{
			outFile = new FileOutputStream(file, append);

			outFile.write(headerLine.getBytes());

			for (String line : alteredList)
			{

				outFile.write(line.getBytes());
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			return;
		}
		finally
		{
			try
			{
				if (outFile != null)
					outFile.close();
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
				return;
			}
		}
		JOptionPane.showMessageDialog(this, "Master alteration data for plugin " + pl.getName() + "\nwritten to file " + file.getName());
	}

	private void formIDReport(Plugin pl)
	{
		File file = getNewFormIDFile(pl.getName());
		if (file == null)
			return;
		if (Main.debugMode)
			System.out.printf("File <" + file.getAbsolutePath() + "> selected for form ID report", new Object[0]);
		String headerLine = "TES4GECKO FORM ID REPORT" + " for plugin <" + pl.getName() + ">\n";
		List<String> newList = getNewList(pl);
		if (newList.size() == 0)
		{
			JOptionPane.showMessageDialog(this, "No new form IDs were introduced in this plugin");
			return;
		}

		FileOutputStream outFile = null;
		try
		{
			outFile = new FileOutputStream(file);

			outFile.write(headerLine.getBytes());

			for (String line : newList)
			{
				outFile.write(line.getBytes());
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			return;
		}
		finally
		{
			try
			{
				if (outFile != null)
					outFile.close();
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
				return;
			}
		}

		JOptionPane.showMessageDialog(this, "New form ID data for plugin " + pl.getName() + "\nwritten to file " + file.getName());
	}

	private List<String> getAlteredList(Plugin pl)
	{
		ArrayList<String> alteredList = new ArrayList<String>();
		List<PluginGroup> topGroups = pl.getGroupList();
		List<String> masterList = pl.getMasterList();

		for (int i = 0; i < masterList.size(); i++)
		{
			String masterHeader = "Altered objects for mod index " + i + " [" + masterList.get(i) + "]:\n";
			boolean alteredFound = false;
			for (PluginGroup currGroup : topGroups)
			{
				String groupType = currGroup.getGroupRecordType();
				String groupHeader = "Object type: " + currGroup.getTypeMap().get(groupType) + "\n";
				List<String> alteredGroup = new ArrayList<String>();
				if (groupType.equals("DIAL"))
				{
					alteredGroup = findAlteredDIAL(currGroup, i);
				}
				else if (groupType.equals("CELL"))
				{
					alteredGroup = findAlteredCELL(currGroup, i);
				}
				else if (groupType.equals("WRLD"))
				{
					alteredGroup = findAlteredWRLD(currGroup, i);
				}
				else
				{
					alteredGroup = findAlteredOther(currGroup, i);
				}
				if (alteredGroup.size() <= 0)
					continue;
				if (!alteredFound)
				{
					alteredFound = true;
					alteredList.add(masterHeader);
				}
				alteredList.add(groupHeader);
				alteredList.addAll(alteredGroup);
			}

		}

		return alteredList;
	}

	private List<String> getNewList(Plugin pl)
	{
		ArrayList<String> newList = new ArrayList<String>();
		List<PluginGroup> topGroups = pl.getGroupList();
		int currentModIndex = pl.getMasterList().size();
		boolean newFound = false;
		for (PluginGroup currGroup : topGroups)
		{
			String groupType = currGroup.getGroupRecordType();

			boolean isLight = groupType.equalsIgnoreCase("LIGH");
			String groupHeader = "Object type: " + currGroup.getTypeMap().get(groupType) + " [" + groupType + "]";
			List<String> newGroup = new ArrayList<String>();
			List<PluginRecord> groupList = currGroup.getRecordList();
			for (PluginRecord rec : groupList)
			{
				if (!(rec instanceof PluginGroup))
				{
					int idx = rec.getFormID() >>> 24;

					if (idx != currentModIndex)
						continue;
					String MODBValue = ", MODB not present";
					String MODLValue = ", MODL not present";
					if (isLight)
					{
						try
						{
							MODBValue = ", MODB: " + rec.getSubrecord("MODB").getDisplayData();
						}
						catch (Exception localException)
						{
						}
					}
					try
					{
						MODLValue = ", MODL: " + rec.getSubrecord("MODL").getDisplayData();
					}
					catch (Exception localException1)
					{
					}
					String entry = String.format("%08X", new Object[]
					{ Integer.valueOf(rec.getFormID()) }) + "\t\t" + rec.getEditorID() + (isLight ? MODLValue : "")
							+ (isLight ? MODBValue : "") + "\n";
					newGroup.add(entry);
				}
			}
			if (newGroup.size() <= 0)
				continue;
			groupHeader = groupHeader + ", " + newGroup.size() + " new form IDs found.\n";
			newList.add(groupHeader);
			newList.addAll(newGroup);
		}

		return newList;
	}

	private List<String> findAlteredOther(PluginGroup group, int modIndex)
	{
		ArrayList<String> alteredList = new ArrayList<String>();
		String groupType = group.getGroupRecordType();
		if ((groupType.equals("CELL")) || (groupType.equals("DIAL")) || (groupType.equals("WRLD")))
			return alteredList;
		List<PluginRecord> groupList = group.getRecordList();
		for (PluginRecord rec : groupList)
		{
			int idx = rec.getFormID() >>> 24;
			if (idx != modIndex)
				continue;
			String entry = "\tForm ID = " + String.format("%08X", new Object[]
			{ Integer.valueOf(rec.getFormID()) }) + "; editor ID = " + rec.getEditorID() + "\n";
			alteredList.add(entry);
		}

		return alteredList;
	}

	private List<String> findAlteredDIAL(PluginGroup group, int modIndex)
	{
		ArrayList<String> alteredList = new ArrayList<String>();
		String groupType = group.getGroupRecordType();
		if (!groupType.equals("DIAL"))
			return alteredList;
		List<PluginRecord> groupList = group.getRecordList();
		String dialFormID = "";
		String dialEditorID = "";
		for (PluginRecord dialOrInfo : groupList)
		{
			if ((dialOrInfo instanceof PluginGroup))
			{
				List<PluginRecord> infoGroup = ((PluginGroup) dialOrInfo).getRecordList();
				for (PluginRecord rec : infoGroup)
				{
					int idx2 = rec.getFormID() >>> 24;
					if (idx2 != modIndex)
						continue;
					String entry = "\t\tResponse form ID = " + String.format("%08X", new Object[]
					{ Integer.valueOf(rec.getFormID()) }) + " for topic " + dialEditorID + " [" + dialFormID + "]\n";
					alteredList.add(entry);
				}
			}
			else
			{

				dialFormID = String.format("%08X", new Object[]
				{ Integer.valueOf(dialOrInfo.getFormID()) });
				dialEditorID = dialOrInfo.getEditorID();
				int idx1 = dialOrInfo.getFormID() >>> 24;
				if (idx1 != modIndex)
					continue;
				String entry = "\tTopic form ID = " + dialFormID + "; editor ID = " + dialEditorID + "\n";
				alteredList.add(entry);
			}
		}

		return alteredList;
	}

	private List<String> findAlteredCELL(PluginGroup group, int modIndex)
	{
		ArrayList<String> alteredList = new ArrayList<String>();
		String groupType = group.getGroupRecordType();
		if (!groupType.equals("CELL"))
			return alteredList;
		List<PluginRecord> blockList = group.getRecordList();
		for (PluginRecord block : blockList)
		{
			List<PluginRecord> subBlockList = ((PluginGroup) block).getRecordList();
			for (PluginRecord subBlock : subBlockList)
			{
				List<PluginRecord> cellList = ((PluginGroup) subBlock).getRecordList();
				for (PluginRecord cell : cellList)
				{
					if (((cell instanceof PluginGroup)) || (!cell.getRecordType().equals("CELL")))
						continue;
					int idx1 = cell.getFormID() >>> 24;
					if (idx1 != modIndex)
						continue;
					String entry = "\tInterior cell form ID = " + String.format("%08X", new Object[]
					{ Integer.valueOf(cell.getFormID()) }) + "; editor ID = " + cell.getEditorID() + "\n";
					alteredList.add(entry);
				}
			}
		}

		return alteredList;
	}

	private List<String> findAlteredWRLD(PluginGroup group, int modIndex)
	{
		ArrayList<String> alteredList = new ArrayList<String>();
		String groupType = group.getGroupRecordType();
		if (!groupType.equals("WRLD"))
			return alteredList;
		List<PluginRecord> recList = group.getRecordList();
		for (int i = 0; i < recList.size(); i += 2)
		{
			PluginRecord world = recList.get(i);
			PluginGroup worldGroup = (PluginGroup) recList.get(i + 1);
			List<PluginRecord> worldGroupList = worldGroup.getRecordList();

			int idx = world.getFormID() >>> 24;
			if (idx == modIndex)
			{
				String entry = "\tWorldspace form ID = " + String.format("%08X", new Object[]
				{ Integer.valueOf(world.getFormID()) }) + "; editor ID = " + world.getEditorID() + "\n";
				alteredList.add(entry);
			}

			List<PluginRecord> blockList = new ArrayList<PluginRecord>();
			PluginGroup persistentCellGroup = null;

			for (PluginRecord block : worldGroupList)
			{
				if (!(block instanceof PluginGroup))
					continue;
				switch (((PluginGroup) block).getGroupType())
				{
					case 6:
						persistentCellGroup = (PluginGroup) ((PluginGroup) block).getRecordList().get(0);
						break;
					case 4:
						blockList.add(block);
					case 5:
				}
			}
			String refType;

			if (persistentCellGroup != null)
			{
				boolean atLeastOne = false;
				List<PluginRecord> refGroup = persistentCellGroup.getRecordList();
				for (PluginRecord ref : refGroup)
				{
					int idx2 = ref.getFormID() >>> 24;
					if (idx2 != modIndex)
						continue;
					refType = "R";
					String recordType = ref.getRecordType();
					if (recordType.equals("ACHR"))
						refType = "NPC r";
					if (recordType.equals("ACRE"))
						refType = "Creature r";
					String editIDStr = "; editor ID = " + ref.getEditorID();
					String entry = "\t\t" + refType + "eference form ID = " + String.format("%08X", new Object[]
					{ Integer.valueOf(ref.getFormID()) }) + editIDStr + "\n";
					if (!atLeastOne)
					{
						atLeastOne = true;
						alteredList.add("\tPersistent references altered:\n");
					}
					alteredList.add(entry);
				}
			}

			for (Iterator<PluginRecord> refGroup = blockList.iterator(); refGroup.hasNext();)
			{
				PluginGroup block = (PluginGroup) refGroup.next();

				List<PluginRecord> subBlockList = block.getRecordList();
				boolean atLeastOne = false;
				for (PluginRecord subBlock : subBlockList)
				{
					List<PluginRecord> cellList = ((PluginGroup) subBlock).getRecordList();
					for (PluginRecord cell : cellList)
					{
						if (((cell instanceof PluginGroup)) || (!cell.getRecordType().equals("CELL")))
							continue;
						int idx1 = cell.getFormID() >>> 24;
						String editIDStr = "; editor ID = " + cell.getEditorID();
						if (idx1 != modIndex)
							continue;
						String entry = "\tExterior cell form ID = " + String.format("%08X", new Object[]
						{ Integer.valueOf(cell.getFormID()) }) + editIDStr + "; coordinates = (" + getXCLCString(cell) + ")\n";
						if (!atLeastOne)
						{
							atLeastOne = true;
							alteredList.add("\tExterior cells altered:\n");
						}
						alteredList.add(entry);
					}
				}
			}
		}

		return alteredList;
	}

	private String getXCLCString(PluginRecord rec)
	{
		String retStr = "Error";
		if ((rec instanceof PluginGroup))
			return retStr;

		List<PluginSubrecord> plSubrecs;
		try
		{
			plSubrecs = rec.getSubrecords();
		}
		catch (Exception ex)
		{

			return retStr;
		}

		for (PluginSubrecord plSubrec : plSubrecs)
		{
			if (!plSubrec.getSubrecordType().equals("XCLC"))
			{
				continue;
			}

			byte[] subrecordData;
			try
			{
				subrecordData = plSubrec.getSubrecordData();
			}
			catch (Exception ex)
			{

				return retStr;
			}

			int x = SerializedElement.getInteger(subrecordData, 0);
			int y = SerializedElement.getInteger(subrecordData, 4);
			retStr = x + ", " + y;
			break;
		}

		return retStr;
	}

	private float getXCLLFogNear(PluginRecord rec)
	{
		float retVal = (0.0F / 0.0F);
		if ((rec instanceof PluginGroup))
			return retVal;

		List<PluginSubrecord> plSubrecs;
		try
		{
			plSubrecs = rec.getSubrecords();
		}
		catch (Exception ex)
		{

			return retVal;
		}

		for (PluginSubrecord plSubrec : plSubrecs)
		{
			if (!plSubrec.getSubrecordType().equals("XCLL"))
			{
				continue;
			}

			byte[] subrecordData;
			try
			{
				subrecordData = plSubrec.getSubrecordData();
			}
			catch (Exception ex)
			{

				return retVal;
			}

			int fogNearBits = SerializedElement.getInteger(subrecordData, 12);
			retVal = Float.intBitsToFloat(fogNearBits);
			break;
		}

		return retVal;
	}

	private boolean setXCLLFogNear(PluginRecord rec, float val)
	{
		boolean retBool = false;
		if ((rec instanceof PluginGroup))
			return retBool;

		try
		{
			PluginSubrecord subXCLL = rec.getSubrecord("XCLL");
			if (subXCLL == null)
				return retBool;
			byte[] oldData = subXCLL.getSubrecordData();
			byte[] newData = new byte[oldData.length];
			System.arraycopy(oldData, 0, newData, 0, oldData.length);
			int valBits = Float.floatToIntBits(val);
			SerializedElement.setInteger(valBits, newData, 12);
			rec.changeSubrecord("XCLL", oldData, newData);
			retBool = true;
		}
		catch (Exception ex)
		{
			return retBool;
		}

		return retBool;
	}

	private int readDialogue(Plugin pl)
	{
		int linesChanged = 0;
		File file = getDialogueReadFile();
		if (file == null)
			return 0;
		int dontLeave = JOptionPane.showConfirmDialog(this, "This will replace dialogue data in this plugin.\n Do you wish to continue?",
				"Possible Content Overwrite", 2, 2);
		if (dontLeave != 0)
			return 0;
		if (Main.debugMode)
			System.out.printf("File <" + file.getAbsolutePath() + "> selected", new Object[0]);
		BufferedReader inputStream = null;
		try
		{
			inputStream = new BufferedReader(new FileReader(file));
		}
		catch (Exception ex)
		{
			return 0;
		}
		String inLine = null;
		try
		{
			while ((inLine = inputStream.readLine()) != null)
			{
				if ((inLine.startsWith("TES4GECKO DIALOGUE DUMP")) || (inLine.endsWith("TAB:")) || (inLine.startsWith("//")))
					continue;
				String[] lineParts = inLine.split("\t");
				String[] fileParts = lineParts[0].split("_");
				String[] emoParts = lineParts[1].split(":");

				int plModIndex = pl.getMasterList().size();
				String dialogueLine = lineParts.length > 2 ? lineParts[2] : " ";
				String dialogueNotes = lineParts.length == 5 ? lineParts[4] : "";

				for (int i = plModIndex; i >= 0; i--)
				{
					int formid = 0;
					int emoLevel = 0;
					int respNum = 0;
					int emoType = EmotionCode.getCode(emoParts[0]);
					try
					{
						formid = Integer.parseInt(fileParts[2], 16);
						emoLevel = Integer.parseInt(emoParts[1]);
						respNum = Integer.parseInt(fileParts[3]);
					}
					catch (Exception ex)
					{
						if (!Main.debugMode)
							break;

						System.out.printf("One of the numerical components in line <" + inLine + "> is not a number.", new Object[0]);
						break;
					}

					FormInfo info = pl.getFormMap().get(Integer.valueOf(formid & 0xFFFFFF | i << 24));
					if ((info == null) || (!info.getRecordType().equals("INFO")))
						continue;
					if (!changeResponseInINFO(info, emoType, emoLevel, respNum, dialogueLine, dialogueNotes))
					{
						if (!Main.debugMode)
							break;
						System.out.printf("Error in changing response in line <" + inLine + ">.", new Object[0]);
						break;
					}

					linesChanged++;

					break;
				}
			}

			inputStream.close();
		}
		catch (Exception localException1)
		{
		}
		return linesChanged;
	}

	private void dumpQuestDialogue(Plugin pl, PluginRecord plRec, boolean append)
	{
		dumpQuestDialogue(pl, plRec, append, null);
	}

	private void dumpQuestDialogue(Plugin pl, PluginRecord plRec, boolean append, File dumpFile)
	{
		if (!plRec.getRecordType().equals("QUST"))
			return;
		File file;
		if (dumpFile == null)
		{
			file = getDialogueDumpFile("QUST");
			if (file == null)
				return;
		}
		else
		{
			file = dumpFile;
		}
		if (!append)
		{
			int dontLeave = JOptionPane.showConfirmDialog(this,
					"This will overwrite whatever data exists in the file.\n Do you wish to continue?", "Possible Content Overwrite", 2, 2);
			if (dontLeave != 0)
				return;
		}
		String headerLine = "TES4GECKO DIALOGUE DUMP for Quest " + plRec.getEditorID() + " [" + String.format("%08X", new Object[]
		{ new Integer(plRec.getFormID()) }) + "]\n";
		List<FormInfo> questInfos = findQuestInfos(pl, plRec.getFormID());
		if ((questInfos.size() == 0) && (dumpFile == null))
		{
			JOptionPane.showMessageDialog(this, "No dialogue data was found for this quest");
			return;
		}

		FileOutputStream outFile = null;
		try
		{
			outFile = new FileOutputStream(file, append);
			outFile.write(headerLine.getBytes());

			for (int dialType = 0; dialType <= 6; dialType++)
			{
				String dialTypeHeader = DialogueTypeCode.getString(dialType).toUpperCase() + " TAB:\n";
				boolean firstOneFound = true;
				for (FormInfo form : questInfos)
				{
					if (getInfoDialogueType(form) != dialType)
						continue;

					if (firstOneFound)
					{
						outFile.write(dialTypeHeader.getBytes());
						firstOneFound = false;
					}
					outFile.write(dialogueForInfo(pl, form).getBytes());

				}
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			return;
		}
		finally
		{
			try
			{
				if (outFile != null)
					outFile.close();
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
				return;
			}
		}
		if (dumpFile == null)
			JOptionPane.showMessageDialog(this, "Dialogue data for quest " + plRec.getEditorID() + "\nwritten to file " + file.getName());
	}

	private File getDialogueDumpFile(String recType)
	{
		FileOutputStream outFile = null;
		FileInputStream inFile = null;
		JFileChooser chooser = new JFileChooser(Main.pluginDirectory);
		chooser.setFileSelectionMode(2);
		String recName = "";
		if (recType.equals("NPC_"))
			recName = "NPC";
		if (recType.equals("QUST"))
			recName = "Quest";
		chooser.setDialogTitle("Select File to Dump " + recName + " Dialogue");
		chooser.setFileFilter(new TextFileFilter());
		if (chooser.showOpenDialog(this) != 0)
			return null;
		File file = chooser.getSelectedFile();
		if (!file.getName().contains("."))
		{
			File newFile = new File(file.getAbsolutePath() + ".txt");
			file = newFile;
		}
		if (!file.getName().toUpperCase().endsWith(".TXT"))
		{
			JOptionPane.showMessageDialog(this, "Dialogue dump file \"" + file.getName() + "\" must be a text file.", "File Type Error", 0);
			return null;
		}
		if ((file.exists()) && ((file.isDirectory()) || (!file.canWrite())))
		{
			JOptionPane.showMessageDialog(this, "Dialogue dump file \"" + file.getName() + "\" must be a writable file.",
					"File Type Error", 0);
			return null;
		}
		if (!file.exists())
		{
			try
			{
				file.createNewFile();
			}
			catch (Exception ex)
			{
				return null;
			}
		}

		try
		{
			outFile = new FileOutputStream(file, true);
			outFile.close();
			inFile = new FileInputStream(file);
			byte[] headerTest = new byte["TES4GECKO DIALOGUE DUMP".length()];
			int bytesRead = inFile.read(headerTest);
			inFile.close();
			if ((bytesRead != 0) && (bytesRead != -1))
			{
				String tmp = new String(headerTest);
				if (!tmp.equals("TES4GECKO DIALOGUE DUMP"))
				{
					JOptionPane.showMessageDialog(this, "Dialogue dump file \"" + file.getName() + "\" has non-dialogue dump content.",
							"File Type Error", 0);
					return null;
				}
			}
		}
		catch (Exception ex)
		{
			JOptionPane.showMessageDialog(this, "Dialogue dump file \"" + file.getName() + "\" cannot be opened.", "File Type Error", 0);
			return null;
		}
		return file;
	}

	private File getMasterModFile()
	{
		FileOutputStream outFile = null;
		FileInputStream inFile = null;
		JFileChooser chooser = new JFileChooser(Main.pluginDirectory);
		chooser.setFileSelectionMode(2);
		String recName = "";
		chooser.setDialogTitle("Select Master Alteration Report");
		chooser.setFileFilter(new TextFileFilter());
		if (chooser.showOpenDialog(this) != 0)
			return null;
		File file = chooser.getSelectedFile();
		if (!file.getName().contains("."))
		{
			File newFile = new File(file.getAbsolutePath() + ".txt");
			file = newFile;
		}
		if (!file.getName().toUpperCase().endsWith(".TXT"))
		{
			JOptionPane.showMessageDialog(this, "Master alteration report \"" + file.getName() + "\" must be a text file.",
					"File Type Error", 0);
			return null;
		}
		if ((file.exists()) && ((file.isDirectory()) || (!file.canWrite())))
		{
			JOptionPane.showMessageDialog(this, "Master alteration report \"" + file.getName() + "\" must be a writable file.",
					"File Type Error", 0);
			return null;
		}
		if (!file.exists())
		{
			try
			{
				file.createNewFile();
			}
			catch (Exception ex)
			{
				return null;
			}
		}

		try
		{
			outFile = new FileOutputStream(file, true);
			outFile.close();
			inFile = new FileInputStream(file);
			byte[] headerTest = new byte["TES4GECKO MASTER ALTERATION REPORT".length()];
			int bytesRead = inFile.read(headerTest);
			inFile.close();
			if ((bytesRead != 0) && (bytesRead != -1))
			{
				String tmp = new String(headerTest);
				if (!tmp.equals("TES4GECKO MASTER ALTERATION REPORT"))
				{
					JOptionPane.showMessageDialog(this, "Master alteration report \"" + file.getName() + "\" has unrelated content.",
							"File Type Error", 0);
					return null;
				}
			}
		}
		catch (Exception ex)
		{
			JOptionPane.showMessageDialog(this, "Master alteration report \"" + file.getName() + "\" cannot be opened.", "File Type Error",
					0);
			return null;
		}
		return file;
	}

	private File getNewFormIDFile(String pluginName)
	{
		String formIDFileName = Main.pluginDirectory + Main.fileSeparator + pluginName + ".IDList";
		File file = new File(formIDFileName);
		if ((file.exists()) && ((file.isDirectory()) || (!file.canWrite())))
		{
			JOptionPane.showMessageDialog(this, "Form ID report \"" + file.getName() + "\" must be a writable file.", "File Type Error", 0);
			return null;
		}
		if (!file.exists())
		{
			try
			{
				file.createNewFile();
			}
			catch (Exception ex)
			{
				return null;
			}
		}
		else
		{
			int selection = JOptionPane.showConfirmDialog(this, "<html>The file <i>" + file.getName()
					+ "</i> already exists. Do you wish to overwrite?", "Form ID Report File", 0, 3);
			if (selection != 0)
				return null;
		}

		return file;
	}

	private File getDialogueReadFile()
	{
		FileInputStream inFile = null;
		JFileChooser chooser = new JFileChooser(Main.pluginDirectory);
		chooser.setFileSelectionMode(2);
		chooser.setDialogTitle("Select File to Read Dialogue");
		chooser.setFileFilter(new TextFileFilter());
		if (chooser.showOpenDialog(this) != 0)
			return null;
		File file = chooser.getSelectedFile();
		if (!file.getName().toUpperCase().endsWith(".TXT"))
		{
			JOptionPane.showMessageDialog(this, "Dialogue read file \"" + file.getName() + "\" must be a text file.", "File Type Error", 0);
			return null;
		}
		if ((file.exists()) && ((file.isDirectory()) || (!file.canRead())))
		{
			JOptionPane.showMessageDialog(this, "Dialogue read file \"" + file.getName() + "\" must be a readable file.",
					"File Type Error", 0);
			return null;
		}
		if (!file.exists())
		{
			JOptionPane.showMessageDialog(this, "Dialogue read file \"" + file.getName() + "\" must exist.", "File Type Error", 0);
			return null;
		}

		try
		{
			inFile = new FileInputStream(file);
			byte[] headerTest = new byte["TES4GECKO DIALOGUE DUMP".length()];
			int bytesRead = inFile.read(headerTest);
			inFile.close();
			if ((bytesRead == 0) || (bytesRead == -1))
			{
				JOptionPane.showMessageDialog(this, "Dialogue read file \"" + file.getName() + "\" is empty.", "File Type Error", 0);
				return null;
			}

			String tmp = new String(headerTest);
			if (!tmp.equals("TES4GECKO DIALOGUE DUMP"))
			{
				JOptionPane.showMessageDialog(this, "Dialogue read file \"" + file.getName() + "\" has non-dialogue read content.",
						"File Type Error", 0);
				return null;
			}

		}
		catch (Exception ex)
		{
			JOptionPane.showMessageDialog(this, "Dialogue read file \"" + file.getName() + "\" cannot be opened.", "File Type Error", 0);
			return null;
		}
		return file;
	}

	private File getClipboardSaveFile()
	{
		FileOutputStream outFile = null;
		JFileChooser chooser = new JFileChooser(Main.pluginDirectory);
		chooser.setFileSelectionMode(2);
		chooser.setDialogTitle("Select Clipboard Save Destination");
		chooser.setFileFilter(new ESPFileFilter());
		if (chooser.showOpenDialog(this) != 0)
			return null;
		File file = chooser.getSelectedFile();
		if (!file.getName().contains("."))
		{
			File newFile = new File(file.getAbsolutePath() + ".esp");
			file = newFile;
		}
		if (!file.getName().toUpperCase().endsWith(".ESP"))
		{
			JOptionPane
					.showMessageDialog(this, "Clipboard save file \"" + file.getName() + "\" must be an ESP file.", "File Type Error", 0);
			return null;
		}
		if ((file.exists()) && ((file.isDirectory()) || (!file.canRead())))
		{
			JOptionPane.showMessageDialog(this, "Clipboard save file \"" + file.getName() + "\" must be a readable file.",
					"File Type Error", 0);
			return null;
		}
		if ((file.exists()) && (!file.getName().toUpperCase().equals("GECKO CLIPBOARD.ESP")))
		{
			JOptionPane.showMessageDialog(this, "Clipboard save file \"" + file.getName() + "\" is not named\n"
					+ "\"Gecko Clipboard.esp\" and therefore will not be overwritten.", "File Type Error", 0);
			return null;
		}

		try
		{
			outFile = new FileOutputStream(file, true);
			outFile.close();
		}
		catch (Exception ex)
		{
			JOptionPane.showMessageDialog(this, "Clipboard save file \"" + file.getName() + "\" cannot be opened.", "File Type Error", 0);
			return null;
		}
		return file;
	}

	private String dialogueForInfo(Plugin pl, FormInfo form)
	{
		String retStr = "";
		String questEditorID = "";
		String dialEditorID = "";
		if (!form.getRecordType().equals("INFO"))
		{
			return retStr;
		}
		PluginRecord plRec = (PluginRecord) form.getSource();
		if (plRec == null)
		{
			return retStr;
		}

		PluginGroup plGroup = (PluginGroup) plRec.getParent();
		if (plGroup == null)
			return retStr;
		int dialFormID = SerializedElement.getInteger(plGroup.getGroupLabel(), 0);
		FormInfo parentDial = pl.getFormMap().get(Integer.valueOf(dialFormID));
		if (parentDial == null)
		{
			dialEditorID = "DID" + String.format("%08X", new Object[]
			{ new Integer(plGroup.getFormID()) });
		}
		else
		{
			dialEditorID = parentDial.getEditorID();
		}

		List<PluginSubrecord> plSubrecs;
		try
		{
			plSubrecs = plRec.getSubrecords();
		}
		catch (Exception ex)
		{

			return retStr;
		}

		for (PluginSubrecord plSubrec : plSubrecs)
		{
			if (plSubrec.getSubrecordType().equals("QSTI"))
			{
				byte[] subrecordData;
				try
				{
					subrecordData = plSubrec.getSubrecordData();
				}
				catch (Exception ex)
				{

					return retStr;
				}

				int parentQuestID = SerializedElement.getInteger(subrecordData, 0);
				FormInfo parentQuest = pl.getFormMap().get(new Integer(parentQuestID));
				if (parentQuest == null)
				{
					questEditorID = "QID" + String.format("%08X", new Object[]
					{ Integer.valueOf(parentQuestID) });
				}
				else
				{
					questEditorID = parentQuest.getEditorID();
				}
			}
			else if (plSubrec.getSubrecordType().equals("TRDT"))
			{

				byte[] subrecordData;
				try
				{
					subrecordData = plSubrec.getSubrecordData();
				}
				catch (Exception ex)
				{

					return retStr;
				}

				int emotionCode = SerializedElement.getInteger(subrecordData, 0);
				int emotionValue = SerializedElement.getInteger(subrecordData, 4);
				int responseNum = subrecordData[12];

				retStr = retStr + questEditorID + "_" + dialEditorID + "_" + String.format("%08X", new Object[]
				{ Integer.valueOf(form.getFormID() & 0xFFFFFF) }) + "_" + responseNum + "\t" + EmotionCode.getString(emotionCode) + ":"
						+ emotionValue + "\t";
			}
			else if (plSubrec.getSubrecordType().equals("NAM1"))
			{

				byte[] subrecordData;
				try
				{
					subrecordData = plSubrec.getSubrecordData();
				}
				catch (Exception ex)
				{

					return retStr;
				}

				String dialogueLine = "";
				if (subrecordData.length > 1)
					dialogueLine = new String(subrecordData, 0, subrecordData.length - 1);
				if (dialogueLine.trim().equals(""))
					retStr = retStr + "\"[No dialogue]\"";
				else
					retStr = retStr + "\"" + hideWhitespace(dialogueLine).trim() + "\"";
			}
			else
			{
				if (!plSubrec.getSubrecordType().equals("NAM2"))
				{
					continue;
				}

				byte[] subrecordData;
				try
				{
					subrecordData = plSubrec.getSubrecordData();
				}
				catch (Exception ex)
				{

					return retStr;
				}

				String prodNote = "";
				if (subrecordData.length > 1)
					prodNote = new String(subrecordData, 0, subrecordData.length - 1);
				if (prodNote.trim().equals(""))
					retStr = retStr + "\n";
				else
					retStr = retStr + "\tPRODNOTE:\t\"" + hideWhitespace(prodNote).trim() + "\"\n";
			}
		}
		return retStr;
	}

	private boolean changeResponseInINFO(FormInfo form, int emoType, int emoLevel, int respNum, String dialogueLine, String dialogueNotes)
	{
		boolean retVal = false;
		if (!form.getRecordType().equals("INFO"))
			return retVal;
		PluginRecord plRec = (PluginRecord) form.getSource();
		if (plRec == null)
		{
			return retVal;
		}

		List<PluginSubrecord> plSubrecs;
		try
		{
			plSubrecs = plRec.getSubrecords();
		}
		catch (Exception ex)
		{

			return retVal;
		}

		int TRDTRspNum = 0;
		for (PluginSubrecord plSubrec : plSubrecs)
		{
			if (plSubrec.getSubrecordType().equals("TRDT"))
			{
				byte[] subrecordData;
				try
				{
					subrecordData = plSubrec.getSubrecordData();
				}
				catch (Exception ex)
				{

					return retVal;
				}

				TRDTRspNum = subrecordData[12];
				if (TRDTRspNum == respNum)
				{
					SerializedElement.setInteger(emoType == -1 ? 0 : emoType, subrecordData, 0);
					SerializedElement.setInteger((emoLevel < 0) || (emoLevel > 100) ? 50 : emoLevel, subrecordData, 4);
					try
					{
						plSubrec.setSubrecordData(subrecordData);
					}
					catch (Exception ex)
					{
						return false;
					}
				}
			}
			else if (plSubrec.getSubrecordType().equals("NAM1"))
			{
				if (TRDTRspNum != respNum)
				{
					continue;
				}
				String newStr = dialogueLine.replace('"', ' ').trim();

				if (newStr.equals("[No dialogue]"))
					newStr = " ";
				String cleanLine = unhideWhitespace(newStr);
				byte[] cleanLineBytes = cleanLine.getBytes();
				byte[] subrecordData = new byte[cleanLineBytes.length + 1];
				System.arraycopy(cleanLineBytes, 0, subrecordData, 0, cleanLineBytes.length);
				subrecordData[cleanLineBytes.length] = 0;
				try
				{
					plSubrec.setSubrecordData(subrecordData);
				}
				catch (Exception ex)
				{
					return false;
				}
				retVal = true;
			}
			else
			{
				if (!plSubrec.getSubrecordType().equals("NAM2"))
					continue;
				if (TRDTRspNum == respNum)
				{
					String newStr = dialogueNotes.replace('"', ' ').trim();
					String cleanNote = unhideWhitespace(newStr);
					byte[] cleanNoteBytes = cleanNote.getBytes();
					byte[] subrecordData = new byte[cleanNoteBytes.length + 1];
					System.arraycopy(cleanNoteBytes, 0, subrecordData, 0, cleanNoteBytes.length);
					subrecordData[cleanNoteBytes.length] = 0;
					try
					{
						plSubrec.setSubrecordData(subrecordData);
					}
					catch (Exception ex)
					{
						return false;
					}
				}
			}
		}
		if (retVal)
			try
			{
				plRec.setSubrecords(plSubrecs);
			}
			catch (Exception ex)
			{
				return false;
			}
		return retVal;
	}

	private String hideWhitespace(String param)
	{
		return param.replaceAll("\t", "[TAB]").replaceAll("\r", "[RET]").replaceAll("\n", "[NL]").replaceAll("\"", "[DQ]");
	}

	private String unhideWhitespace(String param)
	{
		return param.replaceAll("\\[TAB\\]", "\t").replaceAll("\\[RET\\]", "\r").replaceAll("\\[NL\\]", "\n").replaceAll("\\[DQ\\]", "\"");
	}

	private int getInfoDialogueType(FormInfo form)
	{
		int retVal = -1;
		if (!form.getRecordType().equals("INFO"))
		{
			return retVal;
		}
		PluginRecord plRec = (PluginRecord) form.getSource();
		if (plRec == null)
		{
			return retVal;
		}

		List<PluginSubrecord> plSubrecs;
		try
		{
			plSubrecs = plRec.getSubrecords();
		}
		catch (Exception ex)
		{

			return retVal;
		}

		for (PluginSubrecord plSubrec : plSubrecs)
		{
			if (!plSubrec.getSubrecordType().equals("DATA"))
			{
				continue;
			}
			byte[] subrecordData;
			try
			{
				subrecordData = plSubrec.getSubrecordData();
			}
			catch (Exception ex)
			{

				return retVal;
			}

			retVal = subrecordData[0];
		}

		return retVal;
	}

	private void selectQuestInfos(Plugin pl, PluginRecord plRec, JTree plTree, FormInfo questForm, boolean selected)
	{
		if (!plRec.getRecordType().equals("QUST"))
			return;
		ArrayList<TreePath> pathList = new ArrayList<TreePath>();
		pathList.add(new TreePath(questForm.getRecordNode().getPath()));
		List<FormInfo> questInfos = findQuestInfos(pl, questForm.getFormID());
		for (FormInfo INFOFormInfo : questInfos)
		{
			RecordNode recordNode = INFOFormInfo.getRecordNode();
			TreePath treePath = new TreePath(recordNode.getPath());
			pathList.add(treePath);
		}
		if (pathList.size() > 0)
		{
			setCursor(Cursor.getPredefinedCursor(3));
			TreePath[] pathArray = new TreePath[pathList.size()];
			if (selected)
				this.pluginTree.addSelectionPaths(pathList.toArray(pathArray));
			else
				this.pluginTree.removeSelectionPaths(pathList.toArray(pathArray));
			setCursor(Cursor.getPredefinedCursor(0));
			this.pluginTree.scrollPathToVisible(pathArray[0]);
		}
	}

	private void selectWRLDData(Plugin pl, PluginRecord plRec, JTree plTree, FormInfo WSForm, boolean selected, boolean regions)
	{
		if (!plRec.getRecordType().equals("WRLD"))
			return;
		ArrayList<TreePath> pathList = new ArrayList<TreePath>();
		pathList.add(new TreePath(WSForm.getRecordNode().getPath()));
		List<FormInfo> WRLDChildren = findWRLDChildren(pl, WSForm.getFormID(), selected, regions);
		for (FormInfo WRLDChild : WRLDChildren)
		{
			RecordNode recordNode = WRLDChild.getRecordNode();
			TreePath treePath = new TreePath(recordNode.getPath());
			pathList.add(treePath);
		}
		if (pathList.size() > 0)
		{
			setCursor(Cursor.getPredefinedCursor(3));
			TreePath[] pathArray = new TreePath[pathList.size()];
			if (selected)
				this.pluginTree.addSelectionPaths(pathList.toArray(pathArray));
			else
				this.pluginTree.removeSelectionPaths(pathList.toArray(pathArray));
			setCursor(Cursor.getPredefinedCursor(0));
			this.pluginTree.scrollPathToVisible(pathArray[0]);
		}
	}

	private void selectPersistentRefs(Plugin pl, PluginRecord plRec, JTree plTree, FormInfo cellForm, boolean selected, int WSID)
	{
		if (!plRec.getRecordType().equals("CELL"))
			return;
		ArrayList<TreePath> pathList = new ArrayList<TreePath>();
		pathList.add(new TreePath(cellForm.getRecordNode().getPath()));
		List<FormInfo> refInfos = findPersistentRefs(pl, plRec, WSID);
		for (FormInfo refFormInfo : refInfos)
		{
			RecordNode recordNode = refFormInfo.getRecordNode();
			TreePath treePath = new TreePath(recordNode.getPath());
			pathList.add(treePath);
		}
		if (pathList.size() > 0)
		{
			setCursor(Cursor.getPredefinedCursor(3));
			TreePath[] pathArray = new TreePath[pathList.size()];
			if (selected)
				this.pluginTree.addSelectionPaths(pathList.toArray(pathArray));
			else
				this.pluginTree.removeSelectionPaths(pathList.toArray(pathArray));
			setCursor(Cursor.getPredefinedCursor(0));
			this.pluginTree.scrollPathToVisible(pathArray[0]);
		}
	}

	private List<FormInfo> findPersistentRefs(Plugin pl, PluginRecord plRec, int WSID)
	{
		ArrayList<FormInfo> refList = new ArrayList<FormInfo>();
		PluginGroup WRLDGroup = pl.getTopGroup("WRLD");
		if (WRLDGroup == null)
			return refList;
		List<PluginRecord> recList = WRLDGroup.getRecordList();
		PluginGroup worldGroup = null;
		for (int i = 0; i < recList.size(); i += 2)
		{
			worldGroup = (PluginGroup) recList.get(i + 1);
			if (worldGroup.getGroupParentID() == WSID)
				break;
		}
		if (worldGroup == null)
			return refList;

		List<PluginRecord> worldGroupList = worldGroup.getRecordList();

		PluginGroup persistentCellGroup = null;

		for (PluginRecord block : worldGroupList)
		{
			if (!(block instanceof PluginGroup))
				continue;
			switch (((PluginGroup) block).getGroupType())
			{
				case 6:
					persistentCellGroup = (PluginGroup) ((PluginGroup) block).getRecordList().get(0);
			}

		}

		if (persistentCellGroup == null)
			return refList;

		String[] cellXY = getXCLCString(plRec).split(",");
		float cellX = Float.parseFloat(cellXY[0].trim());
		float cellY = Float.parseFloat(cellXY[1].trim());
		List<PluginRecord> refGroup = persistentCellGroup.getRecordList();
		for (PluginRecord ref : refGroup)
		{
			if ((ref instanceof PluginGroup))
				continue;
			PluginSubrecord positionRotation = null;
			try
			{
				positionRotation = ref.getSubrecord("DATA");
			}
			catch (Exception ex)
			{
				continue;
			}
			if (positionRotation != null)
			{
				String[] coords = positionRotation.getDisplayData().split("[ \n:(),]+");
				float refX = Float.parseFloat(coords[1].trim());
				float refY = Float.parseFloat(coords[2].trim());
				if ((refX < cellX * 4096.0F) || (refX >= (cellX + 1.0F) * 4096.0F) || (refY < cellY * 4096.0F)
						|| (refY >= (cellY + 1.0F) * 4096.0F))
					continue;
				FormInfo refInfo = pl.getFormMap().get(Integer.valueOf(ref.getFormID()));
				refList.add(refInfo);
			}
		}
		return refList;
	}

	private int toggleRefsBaseIDCellGroup(Plugin pl, PluginGroup plGroup, JTree plTree, List<Integer> baseIDList)
	{
		int retVal = 0;
		if (plGroup.getGroupType() != 6)
			return retVal;
		ArrayList<TreePath> pathList = new ArrayList<TreePath>();
		List<FormInfo> refInfos = findRefsBaseIDCellGroup(pl, plGroup, baseIDList);
		for (FormInfo refFormInfo : refInfos)
		{
			RecordNode recordNode = refFormInfo.getRecordNode();
			TreePath treePath = new TreePath(recordNode.getPath());
			if (!toggleRecordIgnore(plTree, treePath, recordNode))
				continue;
			retVal++;
		}
		return retVal;
	}

	private int replaceRefsBaseIDCellGroup(Plugin pl, PluginGroup plGroup, JTree plTree, int oldBaseID, int newBaseID)
	{
		int retVal = 0;
		List<Integer> singleBaseID = new ArrayList<Integer>(1);
		singleBaseID.add(Integer.valueOf(oldBaseID));
		if (plGroup.getGroupType() != 6)
			return retVal;
		List<FormInfo> refInfos = findRefsBaseIDCellGroup(pl, plGroup, singleBaseID);
		for (FormInfo refFormInfo : refInfos)
		{
			PluginRecord record = (PluginRecord) refFormInfo.getSource();
			try
			{
				if (!record.changeSubrecord("NAME", Integer.valueOf(oldBaseID), Integer.valueOf(newBaseID)))
					continue;
				retVal++;
			}
			catch (Exception localException)
			{
			}
		}
		return retVal;
	}

	private boolean replaceLandTexIDCellGroup(Plugin pl, PluginGroup plGroup, JTree plTree, int oldLTEXID, int newLTEXID)
	{
		boolean retVal = false;
		if (plGroup.getGroupType() != 6)
			return retVal;
		FormInfo LANDInfo = findLANDRecCellGroup(pl, plGroup);
		if (LANDInfo != null)
		{
			PluginRecord record = (PluginRecord) LANDInfo.getSource();
			retVal = replaceLandTexIDInLANDRec(record, oldLTEXID, newLTEXID);
		}
		return retVal;
	}

	private boolean replaceLandTexIDInLANDRec(PluginRecord plRec, int oldLTEXID, int newLTEXID)
	{
		boolean retVal = false;
		if (((plRec instanceof PluginGroup)) || (!plRec.getRecordType().equals("LAND")))
			return retVal;
		try
		{
			List<PluginSubrecord> subrecordList = plRec.getSubrecords();
			ListIterator<PluginSubrecord> lit = subrecordList.listIterator();
			while (lit.hasNext())
			{
				PluginSubrecord checkSubrecord = lit.next();
				String subType = checkSubrecord.getSubrecordType();
				if ((subType.equals("ATXT")) || (subType.equals("BTXT")))
				{
					byte[] subrecData = checkSubrecord.getSubrecordData();
					int currLTEXID = SerializedElement.getInteger(subrecData, 0);
					if (currLTEXID != oldLTEXID)
						continue;
					SerializedElement.setInteger(newLTEXID, subrecData, 0);
					checkSubrecord.setSubrecordData(subrecData);
					retVal = true;
				}
			}
			if (retVal)
				plRec.setSubrecords(subrecordList);
		}
		catch (Exception localException)
		{
		}
		return retVal;
	}

	private boolean findLandTexIDInLANDRec(PluginRecord plRec, int landTexID)
	{
		boolean retVal = false;
		if (((plRec instanceof PluginGroup)) || (!plRec.getRecordType().equals("LAND")))
			return retVal;
		try
		{
			List<PluginSubrecord> subrecordList = plRec.getSubrecords();
			ListIterator<PluginSubrecord> lit = subrecordList.listIterator();
			while (lit.hasNext())
			{
				PluginSubrecord checkSubrecord = lit.next();
				String subType = checkSubrecord.getSubrecordType();
				if ((subType.equals("ATXT")) || (subType.equals("BTXT")))
				{
					byte[] subrecData = checkSubrecord.getSubrecordData();
					int currLTEXID = SerializedElement.getInteger(subrecData, 0);
					if (currLTEXID == landTexID)
					{
						retVal = true;
						break;
					}
				}
			}
		}
		catch (Exception localException)
		{
		}
		return retVal;
	}

	private int toggleRefsBaseIDAllInteriorCells(Plugin pl, PluginGroup plGroup, JTree plTree, List<Integer> baseIDList)
	{
		int retVal = 0;
		if ((plGroup.getGroupType() != 0) || (!plGroup.getGroupRecordType().equals("CELL")))
			return retVal;
		List<PluginRecord> intBlockList = plGroup.getRecordList();
		for (PluginRecord rec : intBlockList)
		{
			if ((!(rec instanceof PluginGroup)) || (((PluginGroup) rec).getGroupType() != 2))
				continue;
			PluginGroup intBlock = (PluginGroup) rec;
			List<PluginRecord> intSubBlockList = intBlock.getRecordList();
			for (PluginRecord rec2 : intSubBlockList)
			{
				if ((!(rec2 instanceof PluginGroup)) || (((PluginGroup) rec2).getGroupType() != 3))
					continue;
				PluginGroup intSubBlock = (PluginGroup) rec2;
				List<PluginRecord> cellList = intSubBlock.getRecordList();
				for (PluginRecord rec3 : cellList)
				{
					if ((!(rec3 instanceof PluginGroup)) || (((PluginGroup) rec3).getGroupType() != 6))
						continue;
					PluginGroup cellGroup = (PluginGroup) rec3;
					retVal += toggleRefsBaseIDCellGroup(pl, cellGroup, plTree, baseIDList);
				}
			}
		}
		return retVal;
	}

	private int toggleRefsBaseIDAllCellsInWRLD(Plugin pl, PluginGroup plGroup, JTree plTree, List<Integer> baseIDList, int regionID)
	{
		int retVal = 0;
		if (plGroup.getGroupType() != 1)
			return retVal;
		int WSID = plGroup.getGroupParentID();
		List<PluginRecord> intBlockList = plGroup.getRecordList();
		for (PluginRecord rec : intBlockList)
		{
			if ((rec instanceof PluginGroup))
				if (((PluginGroup) rec).getGroupType() == 6)
				{
					if (regionID <= 0)
					{
						retVal += toggleRefsBaseIDCellGroup(pl, (PluginGroup) rec, plTree, baseIDList);
					}
				}
				else if (((PluginGroup) rec).getGroupType() == 4)
				{
					PluginGroup extBlock = (PluginGroup) rec;
					List<PluginRecord> extSubBlockList = extBlock.getRecordList();
					for (PluginRecord rec2 : extSubBlockList)
					{
						if ((!(rec2 instanceof PluginGroup)) || (((PluginGroup) rec2).getGroupType() != 5))
							continue;
						PluginGroup extSubBlock = (PluginGroup) rec2;
						List<PluginRecord> cellList = extSubBlock.getRecordList();
						for (int i = 0; i < cellList.size(); i += 2)
						{
							PluginRecord cell = cellList.get(i);
							PluginGroup cellGroup = (PluginGroup) cellList.get(i + 1);
							if (regionID <= 0)
							{
								retVal += toggleRefsBaseIDCellGroup(pl, cellGroup, plTree, baseIDList);
							}
							else
							{
								if (!cell.hasSubrecordOfType("XCLR"))
									continue;
								try
								{
									String cellRegionStr = cell.getSubrecord("XCLR").getDisplayData();
									boolean inRegion = false;
									String[] cellRegionArray = cellRegionStr.split(",");
									for (int j = 0; j < cellRegionArray.length; j++)
									{
										int cellRegionID = Integer.parseInt(cellRegionArray[j].trim(), 16);
										if (cellRegionID != regionID)
											continue;
										inRegion = true;
										break;
									}

									if (!inRegion)
										continue;
									retVal += toggleRefsBaseIDCellGroup(pl, cellGroup, plTree, baseIDList);

									List<FormInfo> persistentRefs = findPersistentRefs(pl, cell, WSID);
									for (FormInfo form : persistentRefs)
									{
										RecordNode recordNode = form.getRecordNode();
										PluginRecord refRec = recordNode.getRecord();
										String recType = refRec.getRecordType();
										if ((!recType.equals("REFR")) && (!recType.equals("ACRE")) && (!recType.equals("ACHR")))
											continue;
										try
										{
											int refBaseID = Integer.parseInt(rec.getSubrecord("NAME").getDisplayData(), 16);
											if (!baseIDList.contains(Integer.valueOf(refBaseID)))
												continue;
											TreePath treePath = new TreePath(recordNode.getPath());
											if (!toggleRecordIgnore(plTree, treePath, recordNode))
												continue;
											retVal++;
										}
										catch (Exception localException)
										{
										}
									}
								}
								catch (Exception localException1)
								{
								}
							}
						}
					}
				}
		}
		return retVal;
	}

	private List<FormInfo> findRefsBaseIDCellGroup(Plugin pl, PluginGroup plGroup, List<Integer> baseIDList)
	{
		ArrayList<FormInfo> refList = new ArrayList<FormInfo>();
		if (plGroup.getGroupType() != 6)
			return refList;
		List<PluginRecord> cellGroupList = plGroup.getRecordList();
		List<PluginRecord> cellRefList = new ArrayList<PluginRecord>();

		for (PluginRecord block : cellGroupList)
		{
			if (!(block instanceof PluginGroup))
				continue;
			List<PluginRecord> tmpList = ((PluginGroup) block).getRecordList();
			for (PluginRecord ref : tmpList)
			{
				String refType = ref.getRecordType();
				if ((!refType.equals("REFR")) && (!refType.equals("ACRE")) && (!refType.equals("ACHR")))
					continue;
				cellRefList.add(ref);
			}

		}

		for (PluginRecord ref : cellRefList)
		{
			if ((ref instanceof PluginGroup))
				continue;
			int refID = ref.getFormID();
			int baseID = -1;
			try
			{
				baseID = Integer.parseInt(ref.getSubrecord("NAME").getDisplayData(), 16);
			}
			catch (Exception ex)
			{
				continue;
			}
			if (!baseIDList.contains(Integer.valueOf(baseID)))
				continue;
			FormInfo refInfo = pl.getFormMap().get(Integer.valueOf(refID));
			refList.add(refInfo);
		}

		return refList;
	}

	private FormInfo findLANDRecCellGroup(Plugin pl, PluginGroup plGroup)
	{
		FormInfo LANDRec = null;
		if (plGroup.getGroupType() != 6)
			return LANDRec;
		List<PluginRecord> cellGroupList = plGroup.getRecordList();

		for (PluginRecord block : cellGroupList)
		{
			if (!(block instanceof PluginGroup))
				continue;
			List<PluginRecord> tmpList = ((PluginGroup) block).getRecordList();
			for (PluginRecord ref : tmpList)
			{
				String refType = ref.getRecordType();
				if (!refType.equals("LAND"))
					continue;
				LANDRec = pl.getFormMap().get(Integer.valueOf(ref.getFormID()));
				break;
			}

		}

		return LANDRec;
	}

	private int toggleRefsBaseIDPlugin(Plugin pl, JTree plTree, List<Integer> baseIDList)
	{
		int retVal = 0;
		List<FormInfo> allForms = pl.getFormList();
		setCursor(Cursor.getPredefinedCursor(3));
		long startTime = System.currentTimeMillis();
		for (FormInfo refFormInfo : allForms)
		{
			PluginRecord plRec = (PluginRecord) refFormInfo.getSource();
			String refType = plRec.getRecordType();
			if ((!refType.equals("REFR")) && (!refType.equals("ACRE")) && (!refType.equals("ACHR")))
				continue;
			int baseID = -1;
			try
			{
				baseID = Integer.parseInt(plRec.getSubrecord("NAME").getDisplayData(), 16);
			}
			catch (Exception ex)
			{
				continue;
			}
			if (!baseIDList.contains(Integer.valueOf(baseID)))
				continue;
			RecordNode recordNode = refFormInfo.getRecordNode();
			TreePath treePath = new TreePath(recordNode.getPath());
			if (!toggleRecordIgnore(plTree, treePath, recordNode))
				continue;
			retVal++;
		}

		long medTime = System.currentTimeMillis();
		if (Main.debugMode)
		{
			System.out.printf("Part 1 of selectRefsBaseIDPlugin() completed in %.2f seconds.\n", new Object[]
			{ Float.valueOf((medTime - startTime) / 1000.0F) });
		}
		setCursor(Cursor.getPredefinedCursor(0));
		long endTime = System.currentTimeMillis();
		return retVal;
	}

	private int replaceRefsBaseIDPlugin(Plugin pl, JTree plTree, int oldBaseID, int newBaseID)
	{
		int retVal = 0;
		List<FormInfo> allForms = pl.getFormList();
		for (FormInfo refFormInfo : allForms)
		{
			PluginRecord plRec = (PluginRecord) refFormInfo.getSource();
			String refType = plRec.getRecordType();
			if ((!refType.equals("REFR")) && (!refType.equals("ACRE")) && (!refType.equals("ACHR")))
				continue;
			try
			{
				if (!plRec.changeSubrecord("NAME", Integer.valueOf(oldBaseID), Integer.valueOf(newBaseID)))
					continue;
				retVal++;
			}
			catch (Exception localException)
			{
			}
		}
		return retVal;
	}

	private int replaceLandTexIDPlugin(Plugin pl, JTree plTree, int oldLTEXID, int newLTEXID)
	{
		int retVal = 0;
		List<FormInfo> allForms = pl.getFormList();
		for (FormInfo refFormInfo : allForms)
		{
			PluginRecord plRec = (PluginRecord) refFormInfo.getSource();
			String refType = plRec.getRecordType();
			if (!refType.equals("LAND"))
				continue;
			if (!replaceLandTexIDInLANDRec(plRec, oldLTEXID, newLTEXID))
				continue;
			retVal++;
		}

		return retVal;
	}

	private int replaceRefsBaseIDAllInteriorCells(Plugin pl, PluginGroup plGroup, JTree plTree, int oldBaseID, int newBaseID)
	{
		int retVal = 0;
		if ((plGroup.getGroupType() != 0) || (!plGroup.getGroupRecordType().equals("CELL")))
			return retVal;
		List<PluginRecord> intBlockList = plGroup.getRecordList();
		for (PluginRecord rec : intBlockList)
		{
			if ((!(rec instanceof PluginGroup)) || (((PluginGroup) rec).getGroupType() != 2))
				continue;
			PluginGroup intBlock = (PluginGroup) rec;
			List<PluginRecord> intSubBlockList = intBlock.getRecordList();
			for (PluginRecord rec2 : intSubBlockList)
			{
				if ((!(rec2 instanceof PluginGroup)) || (((PluginGroup) rec2).getGroupType() != 3))
					continue;
				PluginGroup intSubBlock = (PluginGroup) rec2;
				List<PluginRecord> cellList = intSubBlock.getRecordList();
				for (PluginRecord rec3 : cellList)
				{
					if ((!(rec3 instanceof PluginGroup)) || (((PluginGroup) rec3).getGroupType() != 6))
						continue;
					PluginGroup cellGroup = (PluginGroup) rec3;
					retVal += replaceRefsBaseIDCellGroup(pl, cellGroup, plTree, oldBaseID, newBaseID);
				}
			}
		}
		return retVal;
	}

	private int replaceRefsBaseIDAllCellsInWRLD(Plugin pl, PluginGroup plGroup, JTree plTree, int oldBaseID, int newBaseID, int regionID)
	{
		int retVal = 0;
		if (plGroup.getGroupType() != 1)
			return retVal;
		int WSID = plGroup.getGroupParentID();
		List<PluginRecord> intBlockList = plGroup.getRecordList();
		for (PluginRecord rec : intBlockList)
		{
			if ((rec instanceof PluginGroup))
				if (((PluginGroup) rec).getGroupType() == 6)
				{
					if (regionID <= 0)
					{
						retVal += replaceRefsBaseIDCellGroup(pl, (PluginGroup) rec, plTree, oldBaseID, newBaseID);
					}
				}
				else if (((PluginGroup) rec).getGroupType() == 4)
				{
					PluginGroup extBlock = (PluginGroup) rec;
					List<PluginRecord> extSubBlockList = extBlock.getRecordList();
					for (PluginRecord rec2 : extSubBlockList)
					{
						if ((!(rec2 instanceof PluginGroup)) || (((PluginGroup) rec2).getGroupType() != 5))
							continue;
						PluginGroup extSubBlock = (PluginGroup) rec2;
						List<PluginRecord> cellList = extSubBlock.getRecordList();
						for (int i = 0; i < cellList.size(); i += 2)
						{
							PluginRecord cell = cellList.get(i);
							PluginGroup cellGroup = (PluginGroup) cellList.get(i + 1);
							if (regionID <= 0)
							{
								retVal += replaceRefsBaseIDCellGroup(pl, cellGroup, plTree, oldBaseID, newBaseID);
							}
							else
							{
								if (!cell.hasSubrecordOfType("XCLR"))
									continue;
								try
								{
									String cellRegionStr = cell.getSubrecord("XCLR").getDisplayData();
									boolean inRegion = false;
									String[] cellRegionArray = cellRegionStr.split(",");
									for (int j = 0; j < cellRegionArray.length; j++)
									{
										int cellRegionID = Integer.parseInt(cellRegionArray[j].trim(), 16);
										if (cellRegionID != regionID)
											continue;
										inRegion = true;
										break;
									}

									if (!inRegion)
										continue;
									retVal += replaceRefsBaseIDCellGroup(pl, cellGroup, plTree, oldBaseID, newBaseID);

									List<FormInfo> persistentRefs = findPersistentRefs(pl, cell, WSID);
									for (FormInfo form : persistentRefs)
									{
										RecordNode recordNode = form.getRecordNode();
										PluginRecord refRec = recordNode.getRecord();
										String recType = refRec.getRecordType();
										if ((!recType.equals("REFR")) && (!recType.equals("ACRE")) && (!recType.equals("ACHR")))
											continue;
										try
										{
											if (!refRec.changeSubrecord("NAME", Integer.valueOf(oldBaseID), Integer.valueOf(newBaseID)))
												continue;
											retVal++;
										}
										catch (Exception localException)
										{
										}
									}
								}
								catch (Exception localException1)
								{
								}
							}
						}
					}
				}
		}
		return retVal;
	}

	private int replaceLandTexIDAllCellsInWRLD(Plugin pl, PluginGroup plGroup, JTree plTree, int oldLTEXID, int newLTEXID, int regionID)
	{
		int retVal = 0;
		if (plGroup.getGroupType() != 1)
			return retVal;
		int WSID = plGroup.getGroupParentID();
		List<PluginRecord> intBlockList = plGroup.getRecordList();
		for (PluginRecord rec : intBlockList)
		{
			if ((!(rec instanceof PluginGroup)) || (((PluginGroup) rec).getGroupType() == 6))
			{
				continue;
			}
			if (((PluginGroup) rec).getGroupType() == 4)
			{
				PluginGroup extBlock = (PluginGroup) rec;
				List<PluginRecord> extSubBlockList = extBlock.getRecordList();
				for (PluginRecord rec2 : extSubBlockList)
				{
					if ((!(rec2 instanceof PluginGroup)) || (((PluginGroup) rec2).getGroupType() != 5))
						continue;
					PluginGroup extSubBlock = (PluginGroup) rec2;
					List<PluginRecord> cellList = extSubBlock.getRecordList();
					for (int i = 0; i < cellList.size(); i += 2)
					{
						PluginRecord cell = cellList.get(i);
						PluginGroup cellGroup = (PluginGroup) cellList.get(i + 1);
						if (regionID <= 0)
						{
							retVal += (replaceLandTexIDCellGroup(pl, cellGroup, plTree, oldLTEXID, newLTEXID) ? 1 : 0);
						}
						else
						{
							if (!cell.hasSubrecordOfType("XCLR"))
								continue;
							try
							{
								String cellRegionStr = cell.getSubrecord("XCLR").getDisplayData();
								boolean inRegion = false;
								String[] cellRegionArray = cellRegionStr.split(",");
								for (int j = 0; j < cellRegionArray.length; j++)
								{
									int cellRegionID = Integer.parseInt(cellRegionArray[j].trim(), 16);
									if (cellRegionID != regionID)
										continue;
									inRegion = true;
									break;
								}

								if (!inRegion)
									continue;
								retVal += (replaceLandTexIDCellGroup(pl, cellGroup, plTree, oldLTEXID, newLTEXID) ? 1 : 0);
							}
							catch (Exception localException)
							{
							}
						}
					}
				}
			}
		}
		return retVal;
	}

	private int getStartFormID(int baseID)
	{
		int retVal = -1;
		String inputID = (String) JOptionPane.showInputDialog(this,
				"<html>Please enter the starting form ID <i>in hex</i>\n(Numbering will start at this number plus one):",
				"New Starting Form ID", -1, null, null, String.format("%08X", new Object[]
				{ Integer.valueOf(baseID) }));
		if (inputID == null)
			return retVal;
		try
		{
			retVal = Integer.parseInt(inputID, 16);
		}
		catch (Exception ex)
		{
			JOptionPane.showMessageDialog(this, "Value entered: \"" + inputID + "\" is not a valid number.", "Entry Error", 0);
			return -1;
		}
		if (retVal < baseID)
		{
			JOptionPane.showMessageDialog(this, "Number entered: \"" + String.format("%08X", new Object[]
			{ Integer.valueOf(retVal) }) + "\" is too small.", "Entry Error", 0);
			return -1;
		}
		return retVal;
	}

	private int enterFormID(int defaultID, String caption)
	{
		int retVal = -1;
		String defCaption = "<html>Please enter the desired form ID <i>in hex</i>:";
		if ((caption == null) || (caption.equals("")))
			caption = defCaption;
		String inputID = (String) JOptionPane.showInputDialog(this, caption, "Please Enter Form ID", -1, null, null, defaultID < 0 ? ""
				: String.format("%08X", new Object[]
				{ Integer.valueOf(defaultID) }));
		if (inputID == null)
			return retVal;
		try
		{
			retVal = Integer.parseInt(inputID.trim(), 16);
		}
		catch (Exception ex)
		{
			JOptionPane.showMessageDialog(this, "Value entered: \"" + inputID + "\" is not a valid number.", "Entry Error", 0);
			return -1;
		}
		return retVal;
	}

	private List<Integer> getFormIDs()
	{
		List<Integer> retList = new ArrayList<Integer>();
		String inputIDs = (String) JOptionPane.showInputDialog(this,
				"<html>Please enter the form IDs to use. These must be in hex,\nseparated by commas with optional spaces:",
				"Initial Form ID List", -1, null, null, null);
		if (inputIDs == null)
			return retList;
		try
		{
			String[] formArray = inputIDs.split(",");
			for (int i = 0; i < formArray.length; i++)
			{
				int formID = Integer.parseInt(formArray[i].trim(), 16);
				retList.add(Integer.valueOf(formID));
			}
		}
		catch (Exception ex)
		{
			JOptionPane.showMessageDialog(this, "Value entered: \"" + inputIDs + "\" has invalid characters.", "Entry Error", 0);
			return new ArrayList<Integer>();
		}
		return retList;
	}

	private void popupEventHandler(String action)
	{
		if ((action == null) || (!action.startsWith("Popup")) || (!action.contains(":")))
			return;
		String[] argList = null;
		try
		{
			argList = action.split(":");
		}
		catch (Exception ex)
		{
			return;
		}
		if (argList.length < 5)
			return;
		Plugin pl = null;
		JTree plTree = null;
		String whichJTree = argList[1];
		String recType = argList[2];
		int formID = 0;
		String groupType = "";
		if (!recType.equals("PLUG"))
			if (recType.equals("GRUP"))
				groupType = argList[3];
			else
				formID = Integer.parseInt(argList[3]);
		String cmd = argList[4];
		if (whichJTree.equals("Plugin"))
		{
			pl = this.plugin;
			plTree = this.pluginTree;
		}
		else
		{
			pl = this.clipboard;
			plTree = this.clipboardTree;
		}

		FormInfo formInfo = null;
		PluginRecord pluginRec = null;
		if (formID != 0)
		{
			formInfo = pl.getFormMap().get(new Integer(formID));
			pluginRec = (PluginRecord) formInfo.getSource();
		}

		if (whichJTree.equals("Clipboard"))
		{
			List<Integer> NPCFactionList;
			if (recType.equals("PLUG"))
			{
				if (cmd.equals("PrepareLipSynch"))
				{
					int selection = JOptionPane
							.showConfirmDialog(
									this,
									"<html>This operation will reduce the clipboard plugin to the bare minimum\nrequired to load successfully in version 1.0 of the CS in order to generate\nLIP files for the dialogue under the quests copied to the clipboard. This plugin\n<html>will <b>not</b> be playable or moddable and <b><i>UNDER NO CIRCUMSTANCES</i></b> should be \nrenamed back to the name of the original plugin ("
											+ this.plugin.getName() + ").\n" + "Do you still want to do this?", "Prepare Lip Synch Plugin",
									0, 3);
					if (selection != 0)
						return;

					setCursor(Cursor.getPredefinedCursor(3));
					int formIDsRemoved = removeNonLipSynchObjects(pl);

					List<Integer> NPCIDList = getNPCsInDialogue(pl);

					List<Integer> factionIDList = getFactionsInDialogue(pl);

					NPCFactionList = getNPCsInFactions(pl, factionIDList);
					if (!NPCFactionList.isEmpty())
					{
						for (Integer factNPCID : NPCFactionList)
						{
							if (NPCIDList.contains(factNPCID))
								continue;
							NPCIDList.add(factNPCID);
						}
					}
					List<PluginRecord> raceList = getRaceList(pl);
					List<PluginRecord> NPCList = null;

					if (!NPCIDList.isEmpty())
					{
						NPCList = getRecordList(pl, NPCIDList, "NPC_");
					}

					formIDsRemoved += removeRecordsNotOnList(pl, NPCIDList, "NPC_");
					List<PluginRecord> factionList = null;

					if (!factionIDList.isEmpty())
					{
						factionList = getRecordList(pl, factionIDList, "FACT");
					}

					formIDsRemoved += removeRecordsNotOnList(pl, factionIDList, "FACT");
					int racesAdded = addRecordsToGroup(plTree, "RACE", raceList);
					int NPCsAdded = NPCList == null ? 0 : addRecordsToGroup(plTree, "NPC_", NPCList);
					int formIDsAltered = reduceLipSynchObjects(pl);

					if ((formIDsRemoved > 0) || (formIDsAltered > 0) || (racesAdded > 0) || (NPCsAdded > 0))
					{
						pl.setMasterList(new ArrayList<String>());
						pl.setVersion(0.8F);
						pl.setCreator("TES4Gecko LIP file-friendly ESP generation");
						pl.setSummary("This plugin is derived from " + this.plugin.getName()
								+ ". It can ONLY be used for LIP file generation.");
						setClipboardModified(true);

						PluginNode newPluginNode = new PluginNode(pl);
						try
						{
							newPluginNode.buildNodes(null);
						}
						catch (Exception localException1)
						{
						}
						plTree.setModel(new DefaultTreeModel(newPluginNode));
					}
					setCursor(Cursor.getPredefinedCursor(0));
					if ((formIDsRemoved == 0) && (formIDsAltered == 0) && (racesAdded == 0) && (NPCsAdded == 0))
					{
						JOptionPane.showMessageDialog(this, "No objects were removed, altered, or added", "Object Cleaning Result", 0);
					}
					else
					{
						JOptionPane.showMessageDialog(this, formIDsRemoved + " objects were removed" + "; " + formIDsAltered
								+ " objects were altered, " + racesAdded + " races were added, and " + NPCsAdded + " NPCs were added.",
								"Object Cleaning Result", 1);
					}
				}
			}
			else if (recType.equals("GRUP"))
			{
				if (groupType.equals("DIAL"))
				{
					if (cmd.equals("RemoveCondition"))
					{
						String inputID = (String) JOptionPane
								.showInputDialog(
										this,
										"Please select the condition function to remove\nNote: ALL conditions that use this function will be removed:",
										"Function to Remove", -1, null, FunctionCode.funcCodeList, FunctionCode.funcCodeList[0]);
						if (inputID == null)
							return;
						int funcCode = FunctionCode.funcCodeMap.get(inputID).intValue();
						int formsAltered = removeConditionFromInfos(pl, funcCode, null, null, null, null);
						if (formsAltered == 0)
						{
							JOptionPane.showMessageDialog(this, "No responses were changed", "Condition Removal Result", 0);
						}
						else
						{
							JOptionPane.showMessageDialog(this, formsAltered + " responses were changed", "Condition Removal Result", 1);
							setClipboardModified(true);
						}
					}
					if (cmd.equals("RemoveExcessQSTIs"))
					{
						HashSet<Integer> questFormIDs = new HashSet<Integer>();
						PluginGroup QUSTGroup = null;
						List<PluginGroup> groupList = pl.getGroupList();
						String groupRecordType;
						for (PluginGroup group : groupList)
						{
							groupRecordType = group.getGroupRecordType();
							if (!groupRecordType.equals("QUST"))
								continue;
							QUSTGroup = group;
							break;
						}

						if (QUSTGroup == null)
						{
							JOptionPane.showMessageDialog(this, "No DIALs were changed", "Quest Reference Removal Result", 0);
						}
						else
						{
							List<PluginRecord> recordList = QUSTGroup.getRecordList();
							for (PluginRecord rec : recordList)
							{
								if (((rec instanceof PluginGroup)) || (!rec.getRecordType().equals("QUST")))
									continue;
								questFormIDs.add(Integer.valueOf(rec.getFormID()));
							}
							int formsAltered = removeQuestsFromDIALs(pl, questFormIDs);
							if (formsAltered == 0)
							{
								JOptionPane.showMessageDialog(this, "No DIALs were changed", "Quest Reference Removal Result", 0);
							}
							else
							{
								JOptionPane.showMessageDialog(this, formsAltered + " DIALs were changed", "Quest Reference Removal Result",
										1);
								setClipboardModified(true);
							}
						}
					}

				}

			}
			else if (cmd.equals("ChangeEditorID"))
			{
				String inputID = (String) JOptionPane.showInputDialog(this, "Please enter the new editor ID for " + pluginRec.getEditorID()
						+ ":", "New Editor ID", -1, null, null, pluginRec.getEditorID());
				if ((inputID == null) || (inputID.equals("")))
					return;
				try
				{
					pluginRec.setEditorID(inputID);
					setClipboardModified(true);
					((DefaultTreeModel) this.clipboardTree.getModel()).reload();
				}
				catch (Exception ex)
				{
					return;
				}
			}
			else
			{
				if (recType.equals("QUST"))
				{
					if (cmd.equals("ChangeFormIDs"))
					{
						int baseID = getStartFormID(this.clipboardHighFormID);
						if (baseID == -1)
							return;
						setCursor(Cursor.getPredefinedCursor(3));
						int formIDsAdded = modifyQuestFormID(pl, pluginRec, baseID, argList[5]);
						if (formIDsAdded > 0)
						{
							this.clipboardHighFormID = (baseID + formIDsAdded);
							setClipboardModified(true);
							((DefaultTreeModel) this.clipboardTree.getModel()).reload();
							pl.repopulateFormList();
							pl.repopulateFormMap();
						}
						setCursor(Cursor.getPredefinedCursor(0));
					}
				}
				if (recType.equals("WRLD"))
				{
					if (cmd.equals("ChangeFormIDs"))
					{
						int baseID = getStartFormID(this.clipboardHighFormID);
						if (baseID == -1)
							return;
						setCursor(Cursor.getPredefinedCursor(3));
						int formIDsModified = modifyWorldspace(pl, pluginRec, baseID);
						if (formIDsModified > 0)
						{
							this.clipboardHighFormID = (baseID + formIDsModified);
							setClipboardModified(true);
							DefaultTreeModel model = (DefaultTreeModel) plTree.getModel();
							model.reload();
							JOptionPane.showMessageDialog(this, formIDsModified + " form IDs were changed", "Worldspace Form ID Result", 1);
						}

						setCursor(Cursor.getPredefinedCursor(0));
					}
				}
			}

		}
		else if (whichJTree.equals("Plugin"))
		{
			if (recType.equals("QUST"))
			{
				if (cmd.equals("SelectInfos"))
				{
					boolean selected = argList[5].equals("Select");
					long startTime = System.currentTimeMillis();
					selectQuestInfos(pl, pluginRec, plTree, formInfo, selected);
					if (Main.debugMode)
					{
						System.out.printf(
								"Quest %s %s completed in %.2f seconds.\n",
								new Object[]
								{ pluginRec.getEditorID(), selected ? "selection" : "deselection",
										Float.valueOf((System.currentTimeMillis() - startTime) / 1000.0F) });
					}
				}
				if (cmd.equals("DumpDialogue"))
				{
					boolean append = argList[5].equals("Append");
					dumpQuestDialogue(pl, pluginRec, append);
				}
			}
			if (recType.equals("NPC_"))
			{
				if (cmd.equals("DumpDialogue"))
				{
					boolean append = argList[5].equals("Append");
					dumpNPCDialogue(pl, pluginRec, append);
				}
			}

			if (recType.equals("REGN"))
			{
				if (cmd.equals("ToggleRefs"))
				{
					PluginGroup plGroup = pl.getWorldspaceGroupForRegion(pluginRec);
					List<Integer> baseIDList = getFormIDs();
					if (baseIDList.size() == 0)
						return;
					setCursor(Cursor.getPredefinedCursor(3));
					int numToggled = toggleRefsBaseIDAllCellsInWRLD(pl, plGroup, plTree, baseIDList, formID);
					if (numToggled > 0)
					{
						JOptionPane.showMessageDialog(this, numToggled + " references had their ignore status changed",
								"Toggle Reference Ignore Result", 1);
						setPluginModified(true);
					}
					setCursor(Cursor.getPredefinedCursor(0));
				}
				if (cmd.equals("ReplaceBaseRefs"))
				{
					PluginGroup plGroup = pl.getWorldspaceGroupForRegion(pluginRec);
					int oldBaseID = enterFormID(-1, "<html> Please enter the reference base ID to be replaced <i>in hex</i>:");
					if (oldBaseID < 0)
						return;
					int newBaseID = enterFormID(-1, "<html> Please enter the reference base ID replacement value <i>in hex</i>:");
					if (newBaseID < 0)
						return;
					if (newBaseID == oldBaseID)
						return;
					setCursor(Cursor.getPredefinedCursor(3));
					int numChanged = replaceRefsBaseIDAllCellsInWRLD(pl, plGroup, plTree, oldBaseID, newBaseID, formID);
					if (numChanged > 0)
					{
						PluginNode newPluginNode = new PluginNode(pl);
						try
						{
							newPluginNode.buildNodes(null);
						}
						catch (Exception localException2)
						{
						}
						plTree.setModel(new DefaultTreeModel(newPluginNode));
						JOptionPane.showMessageDialog(this, numChanged + " references had their base IDs replaced",
								"Replace Reference Base ID", 1);
						setPluginModified(true);
					}
					setCursor(Cursor.getPredefinedCursor(0));
				}
				if (cmd.equals("ReplaceLTEXRefs"))
				{
					PluginGroup plGroup = pl.getWorldspaceGroupForRegion(pluginRec);
					int oldLTEXID = enterFormID(-1, "<html> Please enter the landscape texture ID to be replaced <i>in hex</i>:");
					if (oldLTEXID < 0)
						return;
					int newLTEXID = enterFormID(-1, "<html> Please enter the landscape texture ID replacement value <i>in hex</i>:");
					if (newLTEXID < 0)
						return;
					if (newLTEXID == oldLTEXID)
						return;
					setCursor(Cursor.getPredefinedCursor(3));
					int numChanged = replaceLandTexIDAllCellsInWRLD(pl, plGroup, plTree, oldLTEXID, newLTEXID, formID);
					if (numChanged > 0)
					{
						PluginNode newPluginNode = new PluginNode(pl);
						try
						{
							newPluginNode.buildNodes(null);
						}
						catch (Exception localException3)
						{
						}
						plTree.setModel(new DefaultTreeModel(newPluginNode));
						String plural = numChanged > 1 ? " cells" : " cell";
						JOptionPane.showMessageDialog(this,
								numChanged + plural + " in region had landscape texture ID " + String.format("%08X", new Object[]
								{ Integer.valueOf(oldLTEXID) }) + " replaced with " + String.format("%08X", new Object[]
								{ Integer.valueOf(newLTEXID) }) + ".", "Replace Landscape Texture ID", 1);
						setPluginModified(true);
					}
					setCursor(Cursor.getPredefinedCursor(0));
				}
			}
			if (recType.equals("WRLD"))
			{
				if ((cmd.equals("SelectRegions")) || (cmd.equals("SelectWRLDs")))
				{
					boolean regions = cmd.equals("SelectRegions");
					boolean selected = argList[5].equals("Select");
					long startTime = System.currentTimeMillis();
					selectWRLDData(pl, pluginRec, plTree, formInfo, selected, regions);
					if (Main.debugMode)
					{
						System.out.printf(
								"WRLD %s %s completed in %.2f seconds.\n",
								new Object[]
								{ pluginRec.getEditorID(), selected ? "selection" : "deselection",
										Float.valueOf((System.currentTimeMillis() - startTime) / 1000.0F) });
					}
				}
			}
			if (recType.equals("CELL"))
			{
				if (cmd.equals("SelectPersistentRefs"))
				{
					boolean selected = argList[5].equals("Select");
					int worldID = Integer.parseInt(argList[6], 16);
					long startTime = System.currentTimeMillis();
					selectPersistentRefs(pl, pluginRec, plTree, formInfo, selected, worldID);
					if (Main.debugMode)
					{
						System.out.printf(
								"Persistent refs for CELL %s %s completed in %.2f seconds.\n",
								new Object[]
								{ pluginRec.getEditorID(), selected ? "selection" : "deselection",
										Float.valueOf((System.currentTimeMillis() - startTime) / 1000.0F) });
					}
				}
			}

			if (recType.equals("GRUP"))
			{
				if (groupType.equals("DIAL"))
				{
					if (cmd.equals("ReadDialogue"))
					{
						int linesRead = readDialogue(pl);
						if (linesRead == 0)
						{
							JOptionPane.showMessageDialog(this, "No lines of dialogue were changed", "Read Dialogue Result", 0);
						}
						else
						{
							JOptionPane.showMessageDialog(this, linesRead + " lines of dialogue were changed", "Read Dialogue Result", 1);
							setPluginModified(true);
						}
					}
					if (cmd.equals("RemoveExcessQSTIs"))
					{
						HashSet<Integer> questFormIDs = new HashSet<Integer>();
						PluginGroup QUSTGroup = null;
						List<PluginGroup> groupList = pl.getGroupList();
						String groupRecordType;
						for (PluginGroup group : groupList)
						{
							groupRecordType = group.getGroupRecordType();
							if (!groupRecordType.equals("QUST"))
								continue;
							QUSTGroup = group;
							break;
						}

						if (QUSTGroup == null)
						{
							JOptionPane.showMessageDialog(this, "No DIALs were changed", "Quest Reference Removal Result", 0);
						}
						else
						{
							List<PluginRecord> recordList = QUSTGroup.getRecordList();
							for (PluginRecord rec : recordList)
							{
								if (((rec instanceof PluginGroup)) || (!rec.getRecordType().equals("QUST")))
									continue;
								questFormIDs.add(Integer.valueOf(rec.getFormID()));
							}
							int formsAltered = removeQuestsFromDIALs(pl, questFormIDs);
							if (formsAltered == 0)
							{
								JOptionPane.showMessageDialog(this, "No DIALs were changed", "Quest Reference Removal Result", 0);
							}
							else
							{
								JOptionPane.showMessageDialog(this, formsAltered + " DIALs were changed", "Quest Reference Removal Result",
										1);
								setPluginModified(true);
							}
						}
					}
				}
				if (groupType.equals("CELL"))
				{
					if (cmd.equals("ShowCells"))
					{
						int selRow = Integer.parseInt(argList[5]);
						TreePath topCellPath = this.pluginTree.getPathForRow(selRow);
						TreeNode node = (TreeNode) topCellPath.getLastPathComponent();
						if (node.getChildCount() >= 0)
						{
							for (Enumeration<?> e1 = node.children(); e1.hasMoreElements();)
							{
								TreeNode n1 = (TreeNode) e1.nextElement();
								TreePath path1 = topCellPath.pathByAddingChild(n1);
								for (Enumeration<?> e2 = n1.children(); e2.hasMoreElements();)
								{
									TreeNode n2 = (TreeNode) e2.nextElement();
									TreePath path2 = path1.pathByAddingChild(n2);
									this.pluginTree.expandPath(path2);
								}
							}
						}
					}
					if (cmd.equals("ToggleRefs"))
					{
						int selRow = Integer.parseInt(argList[5]);
						TreePath topCellPath = this.pluginTree.getPathForRow(selRow);
						TreeNode node = (TreeNode) topCellPath.getLastPathComponent();
						PluginGroup plGroup = (PluginGroup) ((GroupNode) node).getUserObject();
						List<Integer> baseIDList = getFormIDs();
						if (baseIDList.size() == 0)
							return;
						setCursor(Cursor.getPredefinedCursor(3));
						int numToggled = toggleRefsBaseIDAllInteriorCells(pl, plGroup, plTree, baseIDList);
						if (numToggled > 0)
						{
							JOptionPane.showMessageDialog(this, numToggled + " references had their ignore status changed",
									"Toggle Reference Ignore Result", 1);
							setPluginModified(true);
						}
						setCursor(Cursor.getPredefinedCursor(0));
					}
					if (cmd.equals("ReplaceBaseRefs"))
					{
						int selRow = Integer.parseInt(argList[5]);
						TreePath topCellPath = this.pluginTree.getPathForRow(selRow);
						TreeNode node = (TreeNode) topCellPath.getLastPathComponent();
						PluginGroup plGroup = (PluginGroup) ((GroupNode) node).getUserObject();
						int oldBaseID = enterFormID(-1, "<html> Please enter the reference base ID to be replaced <i>in hex</i>:");
						if (oldBaseID < 0)
							return;
						int newBaseID = enterFormID(-1, "<html> Please enter the reference base ID replacement value <i>in hex</i>:");
						if (newBaseID < 0)
							return;
						if (newBaseID == oldBaseID)
							return;
						setCursor(Cursor.getPredefinedCursor(3));
						int numChanged = replaceRefsBaseIDAllInteriorCells(pl, plGroup, plTree, oldBaseID, newBaseID);
						if (numChanged > 0)
						{
							PluginNode newPluginNode = new PluginNode(pl);
							try
							{
								newPluginNode.buildNodes(null);
							}
							catch (Exception localException4)
							{
							}
							plTree.setModel(new DefaultTreeModel(newPluginNode));
							JOptionPane.showMessageDialog(this, numChanged + " references had their base IDs replaced",
									"Replace Reference Base ID", 1);
							setPluginModified(true);
						}
						setCursor(Cursor.getPredefinedCursor(0));
					}
				}
				if (groupType.equals("CELLCONTENTS"))
				{
					if (cmd.equals("ToggleRefs"))
					{
						int selRow = Integer.parseInt(argList[5]);
						TreePath topCellPath = this.pluginTree.getPathForRow(selRow);
						TreeNode node = (TreeNode) topCellPath.getLastPathComponent();
						if (((node instanceof GroupNode)) && (((GroupNode) node).getUserObject() != null)
								&& (((PluginGroup) ((GroupNode) node).getUserObject()).getGroupType() == 6))
						{
							PluginGroup plGroup = (PluginGroup) ((GroupNode) node).getUserObject();
							List<Integer> baseIDList = getFormIDs();
							if (baseIDList.size() == 0)
								return;
							setCursor(Cursor.getPredefinedCursor(3));
							int numToggled = toggleRefsBaseIDCellGroup(pl, plGroup, plTree, baseIDList);
							if (numToggled > 0)
							{
								JOptionPane.showMessageDialog(this, numToggled + " references had their ignore status changed",
										"Toggle Reference Ignore Result", 1);
								setPluginModified(true);
							}
							setCursor(Cursor.getPredefinedCursor(0));
						}
					}
					if (cmd.equals("ReplaceBaseRefs"))
					{
						int selRow = Integer.parseInt(argList[5]);
						TreePath topCellPath = this.pluginTree.getPathForRow(selRow);
						TreeNode node = (TreeNode) topCellPath.getLastPathComponent();
						if (((node instanceof GroupNode)) && (((GroupNode) node).getUserObject() != null)
								&& (((PluginGroup) ((GroupNode) node).getUserObject()).getGroupType() == 6))
						{
							PluginGroup plGroup = (PluginGroup) ((GroupNode) node).getUserObject();
							int oldBaseID = enterFormID(-1, "<html> Please enter the reference base ID to be replaced <i>in hex</i>:");
							if (oldBaseID < 0)
								return;
							int newBaseID = enterFormID(-1, "<html> Please enter the reference base ID replacement value <i>in hex</i>:");
							if (newBaseID < 0)
								return;
							if (newBaseID == oldBaseID)
								return;
							setCursor(Cursor.getPredefinedCursor(3));
							int numChanged = replaceRefsBaseIDCellGroup(pl, plGroup, plTree, oldBaseID, newBaseID);
							if (numChanged > 0)
							{
								PluginNode newPluginNode = new PluginNode(pl);
								try
								{
									newPluginNode.buildNodes(null);
								}
								catch (Exception localException5)
								{
								}
								plTree.setModel(new DefaultTreeModel(newPluginNode));
								JOptionPane.showMessageDialog(this, numChanged + " references had their base IDs replaced",
										"Replace Reference Base ID", 1);
								setPluginModified(true);
							}
							setCursor(Cursor.getPredefinedCursor(0));
						}
					}
					if (cmd.equals("ReplaceLTEXRefs"))
					{
						int selRow = Integer.parseInt(argList[5]);
						TreePath topCellPath = this.pluginTree.getPathForRow(selRow);
						TreeNode node = (TreeNode) topCellPath.getLastPathComponent();
						if (((node instanceof GroupNode)) && (((GroupNode) node).getUserObject() != null)
								&& (((PluginGroup) ((GroupNode) node).getUserObject()).getGroupType() == 6))
						{
							PluginGroup plGroup = (PluginGroup) ((GroupNode) node).getUserObject();
							int oldLTEXID = enterFormID(-1, "<html> Please enter the landscape texture ID to be replaced <i>in hex</i>:");
							if (oldLTEXID < 0)
								return;
							int newLTEXID = enterFormID(-1, "<html> Please enter the landscape texture ID replacement value <i>in hex</i>:");
							if (newLTEXID < 0)
								return;
							if (newLTEXID == oldLTEXID)
								return;
							setCursor(Cursor.getPredefinedCursor(3));
							boolean changed = replaceLandTexIDCellGroup(pl, plGroup, plTree, oldLTEXID, newLTEXID);
							if (changed)
							{
								PluginNode newPluginNode = new PluginNode(pl);
								try
								{
									newPluginNode.buildNodes(null);
								}
								catch (Exception localException6)
								{
								}
								plTree.setModel(new DefaultTreeModel(newPluginNode));
								JOptionPane.showMessageDialog(this, "Landscape texture ID " + String.format("%08X", new Object[]
								{ Integer.valueOf(oldLTEXID) }) + " changed to " + String.format("%08X", new Object[]
								{ Integer.valueOf(newLTEXID) }) + " in this cell.", "Replace Landscape Texture ID", 1);
								setPluginModified(true);
							}
							setCursor(Cursor.getPredefinedCursor(0));
						}
					}
				}
				if (groupType.equals("WORLDSPACE"))
				{
					if (cmd.equals("ShowCells"))
					{
						int selRow = Integer.parseInt(argList[5]);
						TreePath topCellPath = this.pluginTree.getPathForRow(selRow);
						TreeNode node = (TreeNode) topCellPath.getLastPathComponent();
						if (node.getChildCount() >= 0)
						{
							for (Enumeration<?> e1 = node.children(); e1.hasMoreElements();)
							{
								TreeNode n1 = (TreeNode) e1.nextElement();
								if ((!(n1 instanceof GroupNode)) || (((GroupNode) n1).getUserObject() == null)
										|| (((PluginGroup) ((GroupNode) n1).getUserObject()).getGroupType() != 4))
								{
									continue;
								}
								TreePath path1 = topCellPath.pathByAddingChild(n1);
								for (Enumeration<?> e2 = n1.children(); e2.hasMoreElements();)
								{
									TreeNode n2 = (TreeNode) e2.nextElement();
									TreePath path2 = path1.pathByAddingChild(n2);
									this.pluginTree.expandPath(path2);
								}
							}
						}
					}

					if (cmd.equals("ToggleRefs"))
					{
						int selRow = Integer.parseInt(argList[5]);
						TreePath topCellPath = this.pluginTree.getPathForRow(selRow);
						TreeNode node = (TreeNode) topCellPath.getLastPathComponent();
						PluginGroup plGroup = (PluginGroup) ((GroupNode) node).getUserObject();
						List<Integer> baseIDList = getFormIDs();
						if (baseIDList.size() == 0)
							return;
						setCursor(Cursor.getPredefinedCursor(3));
						int numToggled = toggleRefsBaseIDAllCellsInWRLD(pl, plGroup, plTree, baseIDList, -1);
						if (numToggled > 0)
						{
							JOptionPane.showMessageDialog(this, numToggled + " references had their ignore status changed",
									"Toggle Reference Ignore Result", 1);
							setPluginModified(true);
						}
						setCursor(Cursor.getPredefinedCursor(0));
					}
					if (cmd.equals("ReplaceBaseRefs"))
					{
						int selRow = Integer.parseInt(argList[5]);
						TreePath topCellPath = this.pluginTree.getPathForRow(selRow);
						TreeNode node = (TreeNode) topCellPath.getLastPathComponent();
						PluginGroup plGroup = (PluginGroup) ((GroupNode) node).getUserObject();
						int oldBaseID = enterFormID(-1, "<html> Please enter the reference base ID to be replaced <i>in hex</i>:");
						if (oldBaseID < 0)
							return;
						int newBaseID = enterFormID(-1, "<html> Please enter the reference base ID replacement value <i>in hex</i>:");
						if (newBaseID < 0)
							return;
						if (newBaseID == oldBaseID)
							return;
						setCursor(Cursor.getPredefinedCursor(3));
						int numChanged = replaceRefsBaseIDAllCellsInWRLD(pl, plGroup, plTree, oldBaseID, newBaseID, -1);
						if (numChanged > 0)
						{
							PluginNode newPluginNode = new PluginNode(pl);
							try
							{
								newPluginNode.buildNodes(null);
							}
							catch (Exception localException7)
							{
							}
							plTree.setModel(new DefaultTreeModel(newPluginNode));
							JOptionPane.showMessageDialog(this, numChanged + " references had their base IDs replaced",
									"Replace Reference Base ID", 1);
							setPluginModified(true);
						}
						setCursor(Cursor.getPredefinedCursor(0));
					}
					if (cmd.equals("ReplaceLTEXRefs"))
					{
						int selRow = Integer.parseInt(argList[5]);
						TreePath topCellPath = this.pluginTree.getPathForRow(selRow);
						TreeNode node = (TreeNode) topCellPath.getLastPathComponent();
						PluginGroup plGroup = (PluginGroup) ((GroupNode) node).getUserObject();
						int oldLTEXID = enterFormID(-1, "<html> Please enter the landscape texture ID to be replaced <i>in hex</i>:");
						if (oldLTEXID < 0)
							return;
						int newLTEXID = enterFormID(-1, "<html> Please enter the landscape texture ID replacement value <i>in hex</i>:");
						if (newLTEXID < 0)
							return;
						if (newLTEXID == oldLTEXID)
							return;
						setCursor(Cursor.getPredefinedCursor(3));
						int numChanged = replaceLandTexIDAllCellsInWRLD(pl, plGroup, plTree, oldLTEXID, newLTEXID, -1);
						if (numChanged > 0)
						{
							PluginNode newPluginNode = new PluginNode(pl);
							try
							{
								newPluginNode.buildNodes(null);
							}
							catch (Exception localException8)
							{
							}
							plTree.setModel(new DefaultTreeModel(newPluginNode));
							String plural = numChanged > 1 ? " cells" : " cell";
							JOptionPane.showMessageDialog(this,
									numChanged + plural + " in world space had landscape texture ID " + String.format("%08X", new Object[]
									{ Integer.valueOf(oldLTEXID) }) + " replaced with " + String.format("%08X", new Object[]
									{ Integer.valueOf(newLTEXID) }) + ".", "Replace Landscape Texture ID", 1);
							setPluginModified(true);
						}
						setCursor(Cursor.getPredefinedCursor(0));
					}
				}
			}
			if (recType.equals("PLUG"))
			{
				if (cmd.equals("MasterModReport"))
				{
					boolean append = argList[5].equals("Append");
					masterModReport(pl, append);
				}
				if (cmd.equals("FormIDReport"))
				{
					formIDReport(pl);
				}
				if (cmd.equals("ToggleRefs"))
				{
					List<Integer> baseIDList = getFormIDs();
					if (baseIDList.size() == 0)
						return;
					setCursor(Cursor.getPredefinedCursor(3));
					int numToggled = toggleRefsBaseIDPlugin(pl, plTree, baseIDList);
					if (numToggled > 0)
					{
						JOptionPane.showMessageDialog(this, numToggled + " references had their ignore status changed",
								"Toggle Reference Ignore Result", 1);
						setPluginModified(true);
					}
					setCursor(Cursor.getPredefinedCursor(0));
				}
				if (cmd.equals("ReplaceBaseRefs"))
				{
					int oldBaseID = enterFormID(-1, "<html> Please enter the reference base ID to be replaced <i>in hex</i>:");
					if (oldBaseID < 0)
						return;
					int newBaseID = enterFormID(-1, "<html> Please enter the reference base ID replacement value <i>in hex</i>:");
					if (newBaseID < 0)
						return;
					if (newBaseID == oldBaseID)
						return;
					setCursor(Cursor.getPredefinedCursor(3));
					int numChanged = replaceRefsBaseIDPlugin(pl, plTree, oldBaseID, newBaseID);
					if (numChanged > 0)
					{
						PluginNode newPluginNode = new PluginNode(pl);
						try
						{
							newPluginNode.buildNodes(null);
						}
						catch (Exception localException9)
						{
						}
						plTree.setModel(new DefaultTreeModel(newPluginNode));
						JOptionPane.showMessageDialog(this, numChanged + " references had their base IDs replaced",
								"Replace Reference Base ID", 1);
						setPluginModified(true);
					}
					setCursor(Cursor.getPredefinedCursor(0));
				}
				if (cmd.equals("ReplaceLTEXRefs"))
				{
					int oldLTEXID = enterFormID(-1, "<html> Please enter the landscape texture ID to be replaced <i>in hex</i>:");
					if (oldLTEXID < 0)
						return;
					int newLTEXID = enterFormID(-1, "<html> Please enter the landscape texture ID replacement value <i>in hex</i>:");
					if (newLTEXID < 0)
						return;
					if (newLTEXID == oldLTEXID)
						return;
					setCursor(Cursor.getPredefinedCursor(3));
					int numChanged = replaceLandTexIDPlugin(pl, plTree, oldLTEXID, newLTEXID);
					if (numChanged > 0)
					{
						PluginNode newPluginNode = new PluginNode(pl);
						try
						{
							newPluginNode.buildNodes(null);
						}
						catch (Exception localException10)
						{
						}
						plTree.setModel(new DefaultTreeModel(newPluginNode));
						JOptionPane.showMessageDialog(this,
								numChanged + " cells in plugin had landscape texture ID " + String.format("%08X", new Object[]
								{ Integer.valueOf(oldLTEXID) }) + " replaced with " + String.format("%08X", new Object[]
								{ Integer.valueOf(newLTEXID) }) + ".", "Replace Landscape Texture ID", 1);
						setPluginModified(true);
					}
					setCursor(Cursor.getPredefinedCursor(0));
				}
				PluginRecord quest;
				if (cmd.equals("DumpPluginDialogue"))
				{
					PluginGroup questGroup = pl.getTopGroup("QUST");
					if ((questGroup == null) || (questGroup.isEmpty()))
					{
						JOptionPane.showMessageDialog(this, "No quests in plugin, so no dialogue to dump", "Dump Plugin Dialogue", 1);
					}
					File file = getDialogueDumpFile("QUST");
					if (file == null)
						return;
					List<PluginRecord> allQuests = questGroup.getAllPluginRecords();
					boolean firstQuestDone = false;
					setCursor(Cursor.getPredefinedCursor(3));
					for (Iterator<PluginRecord> newLTEXID = allQuests.iterator(); newLTEXID.hasNext();)
					{
						quest = newLTEXID.next();

						dumpQuestDialogue(pl, quest, firstQuestDone, file);
						firstQuestDone = true;
					}
					setCursor(Cursor.getPredefinedCursor(0));
					JOptionPane.showMessageDialog(this, "All dialogue in " + pl.getName() + " dumped to " + file.getName() + " ["
							+ allQuests.size() + " quests total]", "Dump Plugin Dialogue", 1);
				}

				if (cmd.equals("FogFix"))
				{
					int numCellsChanged = 0;
					int numCellsWithXCLL = 0;
					setCursor(Cursor.getPredefinedCursor(3));
					List<FormInfo> allForms = pl.getFormList();
					for (FormInfo recFormInfo : allForms)
					{
						if (recFormInfo.getFormID() == 20)
							continue;
						PluginRecord plRec = (PluginRecord) recFormInfo.getSource();
						if (plRec == null)
						{
							if (Main.debugMode)
								System.out.printf("FormInfo %08X has no record\n", new Object[]
								{ Integer.valueOf(recFormInfo.getFormID()) });
						}
						else
						{
							String recordType = plRec.getRecordType();
							if (recordType == null)
							{
								if (Main.debugMode)
									System.out.printf("Record %08X has no record type\n", new Object[]
									{ Integer.valueOf(plRec.getFormID()) });
							}
							if ((recordType == null) || (!recordType.equals("CELL")))
								continue;
							try
							{
								if (!plRec.hasSubrecordOfType("XCLL"))
									continue;
								if (Main.debugMode)
								{
									if (plRec.hasSubrecordOfType("XCLC"))
										System.out.printf("Record %08X has both XCLL & XCLC subrecords\n", new Object[]
										{ Integer.valueOf(plRec.getFormID()) });
								}
								numCellsWithXCLL++;
								if (getXCLLFogNear(plRec) != 0.0F)
									continue;
								if (!setXCLLFogNear(plRec, 1.0E-004F))
									continue;
								if (Main.debugMode)
								{
									System.out.printf("Cell ID %08X with name %s had fog fix applied.\n", new Object[]
									{ Integer.valueOf(plRec.getFormID()), plRec.getEditorID() });
								}
								numCellsChanged++;
							}
							catch (Exception localException11)
							{
							}
						}

					}

					setCursor(Cursor.getPredefinedCursor(0));
					String plural = numCellsWithXCLL == 1 ? " cell was" : " cells were";
					if (numCellsChanged > 0)
					{
						String plural2 = numCellsChanged == 1 ? " cell" : " cells";
						JOptionPane.showMessageDialog(this, numCellsWithXCLL + plural + " inspected and " + numCellsChanged + plural2
								+ " had the fog fix applied.", "NVIDIA/ATI Fog Fix Result", 1);
						setPluginModified(true);
					}
					else
					{
						JOptionPane.showMessageDialog(this, numCellsWithXCLL + plural + " inspected but no cells were altered.",
								"NVIDIA/ATI Fog Fix Result", 1);
					}
				}

				if (cmd.equals("ChangeMusic"))
				{
					int numCellsChanged = 0;
					int numCellsWithXCLC = 0;
					String musicTypeStr = CellMusicDialog.showDialog(this);
					if (musicTypeStr.equals("Cancel"))
						return;
					byte musicType = 0;
					if (musicTypeStr.equals("Public"))
						musicType = 1;
					if (musicTypeStr.equals("Dungeon"))
						musicType = 2;

					setCursor(Cursor.getPredefinedCursor(3));
					List<FormInfo> allForms = pl.getFormList();
					for (Iterator<FormInfo> recordTypeIt = allForms.iterator(); recordTypeIt.hasNext();)
					{
						FormInfo recFormInfo = recordTypeIt.next();

						if (recFormInfo.getFormID() == 20)
							continue;
						PluginRecord plRec = (PluginRecord) recFormInfo.getSource();
						if (plRec == null)
						{
							if (Main.debugMode)
								System.out.printf("FormInfo %08X has no record\n", new Object[]
								{ Integer.valueOf(recFormInfo.getFormID()) });
						}
						else
						{
							String recordType = plRec.getRecordType();
							if (recordType == null)
							{
								if (Main.debugMode)
									System.out.printf("Record %08X has no record type\n", new Object[]
									{ Integer.valueOf(plRec.getFormID()) });
							}
							if ((recordType == null) || (!recordType.equals("CELL")))
								continue;
							try
							{
								if (!plRec.hasSubrecordOfType("XCLC"))
									continue;
								if (Main.debugMode)
								{
									if (plRec.hasSubrecordOfType("XCLL"))
										System.out.printf("Record %08X has both XCLL & XCLC subrecords\n", new Object[]
										{ Integer.valueOf(plRec.getFormID()) });
								}
								numCellsWithXCLC++;
								if ((musicType == 0) && (plRec.hasSubrecordOfType("XCMT")))
								{
									if (!plRec.removeSubrecords("XCMT"))
										continue;
									if (Main.debugMode)
									{
										System.out.printf("Cell ID %08X had XCMT removed.\n", new Object[]
										{ Integer.valueOf(plRec.getFormID()) });
									}
									numCellsChanged++;
								}
								else
								{
									if (musicType == 0)
										continue;
									if (plRec.hasSubrecordOfType("XCMT"))
									{
										byte currMusic = Byte.parseByte(plRec.getSubrecord("XCMT").getDisplayData(), 16);
										if (currMusic == musicType)
											continue;
										if (plRec.changeSubrecord("XCMT", Byte.valueOf(currMusic), Byte.valueOf(musicType)))
										{
											numCellsChanged++;
										}

									}
									else if (plRec.insertSubrecordAfter("XCMT", Byte.valueOf(musicType), "XCLC"))
									{
										numCellsChanged++;
									}
								}
							}
							catch (Exception localException12)
							{
							}
						}
					}
					setCursor(Cursor.getPredefinedCursor(0));
					String plural = numCellsWithXCLC == 1 ? " cell was" : " cells were";
					if (numCellsChanged > 0)
					{
						String plural2 = numCellsChanged == 1 ? " cell" : " cells";
						JOptionPane.showMessageDialog(this, numCellsWithXCLC + plural + " inspected and " + numCellsChanged + plural2
								+ " had the music type changed.", "Music Type Change Result", 1);
						setPluginModified(true);
					}
					else
					{
						JOptionPane.showMessageDialog(this, numCellsWithXCLC + plural + " inspected but no cells were altered.",
								"Music Type Change Result", 1);
					}
				}
			}
		}
	}

	private class DialogWindowListener extends WindowAdapter
	{
		public DialogWindowListener()
		{
		}

		public void windowClosing(WindowEvent we)
		{
			DisplayDialog.this.closeDialog();
		}
	}

	private class DisplayCellRenderer extends DefaultTreeCellRenderer
	{
		public DisplayCellRenderer()
		{
			setTextSelectionColor(Color.WHITE);
			setTextNonSelectionColor(Color.BLACK);
			setBackgroundSelectionColor(Color.BLUE);
			setBackgroundNonSelectionColor(Color.WHITE);
		}

		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean isSelected, boolean isExpanded, boolean isLeaf,
				int row, boolean hasFocus)
		{
			Component component = super.getTreeCellRendererComponent(tree, value, isSelected, isExpanded, isLeaf, row, hasFocus);

			if ((value instanceof RecordNode))
			{
				int modIdx = ((RecordNode) value).getRecord().getFormID() >>> 24;
				Color bkgnd = PluginColorMap.getPluginColor(modIdx);
				setBackgroundNonSelectionColor(bkgnd);
			}
			else
			{
				setBackgroundNonSelectionColor(Color.WHITE);
			}
			return component;
		}
	}

	private class PluginColorTableRenderer extends DefaultTableCellRenderer
	{
		private PluginColorTableRenderer()
		{
		}

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col)
		{
			Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);

			String s = table.getModel().getValueAt(row, 0).toString();
			comp.setBackground(PluginColorMap.getPluginColor(s));

			return comp;
		}
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.DisplayDialog
 * JD-Core Version:    0.6.0
 */