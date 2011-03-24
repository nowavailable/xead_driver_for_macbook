package xeadDriver;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.*;
import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.text.DecimalFormat;
import org.w3c.dom.*;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.hssf.util.*;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

public class XF300 extends JDialog implements XFExecutable, XFScriptable {
	private static final long serialVersionUID = 1L;
	private static ResourceBundle res = ResourceBundle.getBundle("xeadDriver.Res");
	private org.w3c.dom.Element functionElement_ = null;
	private Session session_ = null;
	private Connection connection = null;
	private boolean instanceIsAvailable = true;
	private boolean isToBeCanceled = false;
	private int instanceArrayIndex_ = -1;
	private int programSequence;
	private StringBuffer processLog = new StringBuffer();
	private XF300_KeyInputDialog keyInputDialog = null;
	private JPanel jPanelMain = new JPanel();
	private JTabbedPane jTabbedPane = new JTabbedPane();
	private Dimension scrSize;
	private JPanel jPanelHeaderFields = new JPanel();
	private JScrollPane jScrollPaneHeaderFields = new JScrollPane();
	private XF300_HeaderTable headerTable_;
	private HashMap<String, Object> parmMap_ = null;
	private HashMap<String, Object> returnMap_ = new HashMap<String, Object>();
	private ArrayList<XF300_HeaderField> headerFieldList = new ArrayList<XF300_HeaderField>();
	private ArrayList<XF300_HeaderReferTable> headerReferTableList = new ArrayList<XF300_HeaderReferTable>();
	private NodeList headerReferElementList;
	private org.w3c.dom.Element headerFunctionElement = null;
	private String headerFunctionID = "";
	private JPanel jPanelCenter = new JPanel();
	private JPanel jPanelTop = new JPanel();
	private JPanel jPanelTopEast = new JPanel();
	private JPanel jPanelTopCenter = new JPanel();
	private JPanel jPanelTopNorthMargin = new JPanel();
	private JPanel jPanelTopSouthMargin = new JPanel();
	private JPanel jPanelTopEastMargin = new JPanel();
	private JPanel jPanelTopWestMargin = new JPanel();
	private JButton jButtonList = new JButton();
	private JPanel jPanelFilter[] = new JPanel[5];
	private double filterWidth[] = new double[5];
	private boolean anyFilterIsEditable[] = new boolean[5];
	@SuppressWarnings("unchecked")
	private ArrayList<XF300_Filter> filterListArray[] = new ArrayList[5];
	@SuppressWarnings("unchecked")
	private ArrayList<XF300_DetailColumn>[] detailColumnListArray = new ArrayList[5];
	@SuppressWarnings("unchecked")
	private ArrayList<XF300_DetailReferTable>[] detailReferTableListArray = new ArrayList[5];
	@SuppressWarnings("unchecked")
	private ArrayList<WorkingRow>[] workingRowListArray = new ArrayList[5];
	private XF300_DetailTable[] detailTableArray = new XF300_DetailTable[5];
	private org.w3c.dom.Element[] detailFunctionElementArray = new org.w3c.dom.Element[5];
	private String[] detailFunctionIDArray = new String[5];
	private TableModelReadOnlyList[] tableModelMainArray = new TableModelReadOnlyList[5];
	private JTable[] jTableMainArray = new JTable[5];
	private String[] initialMsgArray = new String[5];
	private NodeList[] detailReferElementList = new NodeList[5];
	private Bindings[] detailScriptBindingsArray = new Bindings[5];
	private DefaultTableCellRenderer rendererTableHeader = null;
	private DefaultTableCellRenderer rendererAlignmentCenter = new DefaultTableCellRenderer();
	private DefaultTableCellRenderer rendererAlignmentRight = new DefaultTableCellRenderer();
	private DefaultTableCellRenderer rendererAlignmentLeft = new DefaultTableCellRenderer();
	private boolean tablesReadyToUse;
	private JPanel jPanelBottom = new JPanel();
	private JPanel jPanelButtons = new JPanel();
	private JPanel[] jPanelButtonArray = new JPanel[7];
	private JPanel jPanelDummy = new JPanel();
	private JPanel jPanelInfo = new JPanel();
	private GridLayout gridLayoutButtons = new GridLayout();
	private GridLayout gridLayoutInfo = new GridLayout();
	private ArrayList<String> messageList = new ArrayList<String>();
	private JLabel jLabelFunctionID = new JLabel();
	private JLabel jLabelSessionID = new JLabel();
	private JSplitPane jSplitPaneMain = new JSplitPane();
	private JSplitPane jSplitPaneCenter = new JSplitPane();
	private JScrollPane jScrollPaneTable = new JScrollPane();
	private JScrollPane jScrollPaneMessages = new JScrollPane();
	private JTextArea jTextAreaMessages = new JTextArea();
	private JButton[] jButtonArray = new JButton[7];
	private Color selectionColorWithFocus = new Color(49,106,197);
	private Color selectionColorWithoutFocus = new Color(213,213,213);
	private SortableDomElementListModel detailTabSortingList;
	private SortableDomElementListModel workSortingList;
	private Action helpAction = new AbstractAction(){
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent e){
			session_.browseHelp();
		}
	};
	private Action[] actionButtonArray = new Action[7];
	private String[] actionDefinitionArray = new String[7];
	private String actionF6 = "";
	private String actionF8 = "";
	private ScriptEngine scriptEngine;
	private Bindings headerScriptBindings;
	private String scriptNameRunning = "";
	private final int FIELD_HORIZONTAL_MARGIN = 1;
	private final int FIELD_VERTICAL_MARGIN = 5;
	private final int FONT_SIZE = 14;
	private final int ROW_HEIGHT = 18;
	private ByteArrayOutputStream exceptionLog;
	private PrintStream exceptionStream;
	private String exceptionHeader = "";

	public XF300(Session session, int instanceArrayIndex) {
		super(session, "", true);
		try {
			session_ = session;
			instanceArrayIndex_ = instanceArrayIndex;
			//
			initComponentsAndVariants();
			//
		} catch(Exception e) {
			e.printStackTrace(exceptionStream);
		}
	}

	void initComponentsAndVariants() throws Exception {
		//
		scrSize = Toolkit.getDefaultToolkit().getScreenSize();
		//
		jPanelMain.setLayout(new BorderLayout());
		//
		jSplitPaneMain.setOrientation(JSplitPane.VERTICAL_SPLIT);
		jSplitPaneMain.add(jSplitPaneCenter, JSplitPane.TOP);
		jSplitPaneMain.add(jScrollPaneMessages, JSplitPane.BOTTOM);
		jSplitPaneMain.setFocusable(false);
		jScrollPaneHeaderFields.getViewport().add(jPanelHeaderFields, null);
		jScrollPaneHeaderFields.setBorder(null);
		jPanelHeaderFields.setLayout(null);
		jPanelHeaderFields.setFocusable(false);
		//
		jPanelCenter.setLayout(new BorderLayout());
		//
		jPanelTop.setPreferredSize(new Dimension(600, 45));
		jPanelTop.setLayout(new BorderLayout());
		jPanelTopNorthMargin.setPreferredSize(new Dimension(600, 8));
		jPanelTopSouthMargin.setPreferredSize(new Dimension(600, 8));
		jPanelTopEastMargin.setPreferredSize(new Dimension(8, 20));
		jPanelTopWestMargin.setPreferredSize(new Dimension(8, 20));
		jPanelTopEast.setPreferredSize(new Dimension(80, 80));
		jPanelTopEast.setLayout(new BorderLayout());
		jPanelTopEast.add(jButtonList, BorderLayout.CENTER);
		jPanelTopEast.add(jPanelTopEastMargin, BorderLayout.EAST);
		jPanelTopEast.add(jPanelTopWestMargin, BorderLayout.WEST);
		jButtonList.setFont(new java.awt.Font("Dialog", 0, FONT_SIZE));
		jButtonList.setText(res.getString("Search"));
		jButtonList.addActionListener(new XF300_jButtonList_actionAdapter(this));
		jButtonList.addKeyListener(new XF300_keyAdapter(this));
		jPanelTopCenter.setLayout(new BorderLayout());
		//jPanelTopCenter.add(jPanelFilter, BorderLayout.CENTER);
		jPanelTop.setBorder(BorderFactory.createEtchedBorder());
		jPanelTop.add(jPanelTopCenter, BorderLayout.CENTER);
		jPanelTop.add(jPanelTopNorthMargin, BorderLayout.NORTH);
		jPanelTop.add(jPanelTopSouthMargin, BorderLayout.SOUTH);
		jPanelTop.add(jPanelTopEast, BorderLayout.EAST);
		jPanelCenter.add(jPanelTop, BorderLayout.NORTH);
		jPanelCenter.add(jScrollPaneTable, BorderLayout.CENTER);
		//
		jTextAreaMessages.setEditable(false);
		jTextAreaMessages.setBorder(BorderFactory.createEtchedBorder());
		jTextAreaMessages.setFont(new java.awt.Font("SansSerif", 0, FONT_SIZE));
		jTextAreaMessages.setFocusable(false);
		jTextAreaMessages.setLineWrap(true);
		jScrollPaneMessages.getViewport().add(jTextAreaMessages, null);
		jSplitPaneCenter.setOrientation(JSplitPane.VERTICAL_SPLIT);
		jSplitPaneCenter.add(jScrollPaneHeaderFields, JSplitPane.TOP);
		jSplitPaneCenter.add(jTabbedPane, JSplitPane.BOTTOM);
		jSplitPaneCenter.setFocusable(false);
		jTabbedPane.setFont(new java.awt.Font("SansSerif", 0, FONT_SIZE));
		jTabbedPane.addKeyListener(new XF300_jTabbedPane_keyAdapter(this));
		jTabbedPane.addChangeListener(new XF300_jTabbedPane_changeAdapter(this));
		//
		rendererAlignmentCenter.setHorizontalAlignment(SwingConstants.CENTER);
		rendererAlignmentRight.setHorizontalAlignment(SwingConstants.RIGHT);
		rendererAlignmentLeft.setHorizontalAlignment(SwingConstants.LEFT);
		for (int i = 0; i < 5; i++) {
			detailTableArray[i] = null;
			detailReferElementList[i] = null;
			filterListArray[i] = new ArrayList<XF300_Filter>();
			workingRowListArray[i] = new ArrayList<WorkingRow>();
			detailColumnListArray[i] = new ArrayList<XF300_DetailColumn>();
			detailReferTableListArray[i] = new ArrayList<XF300_DetailReferTable>();
			jPanelFilter[i] = new JPanel();
			jTableMainArray[i] = new JTable();
			jTableMainArray[i].setFont(new java.awt.Font("SansSerif", 0, FONT_SIZE));
			jTableMainArray[i].setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			jTableMainArray[i].setRowHeight(ROW_HEIGHT);
			jTableMainArray[i].setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			jTableMainArray[i].setRowSelectionAllowed(true);
			jTableMainArray[i].addKeyListener(new XF300_jTableMain_keyAdapter(this));
			jTableMainArray[i].addMouseListener(new XF300_jTableMain_mouseAdapter(this));
			jTableMainArray[i].addFocusListener(new XF300_jTableMain_focusAdapter(this));
			jTableMainArray[i].setAutoCreateRowSorter(true);
			jTableMainArray[i].getTableHeader().setFont(new java.awt.Font("SansSerif", 0, FONT_SIZE));
			rendererTableHeader = (DefaultTableCellRenderer)jTableMainArray[i].getTableHeader().getDefaultRenderer();
			rendererTableHeader.setHorizontalAlignment(SwingConstants.LEFT);
		}
		jScrollPaneTable.getViewport().add(jTableMainArray[0], null);
		jScrollPaneTable.addMouseListener(new XF300_jScrollPaneTable_mouseAdapter(this));
		//
		jPanelBottom.setPreferredSize(new Dimension(10, 35));
		jPanelBottom.setLayout(new BorderLayout());
		jPanelBottom.setBorder(null);
		jLabelFunctionID.setFont(new java.awt.Font("Dialog", 0, FONT_SIZE));
		jLabelFunctionID.setHorizontalAlignment(SwingConstants.RIGHT);
		jLabelFunctionID.setForeground(Color.gray);
		jLabelFunctionID.setFocusable(false);
		jLabelSessionID.setFont(new java.awt.Font("Dialog", 0, FONT_SIZE));
		jLabelSessionID.setHorizontalAlignment(SwingConstants.RIGHT);
		jLabelSessionID.setForeground(Color.gray);
		jLabelSessionID.setFocusable(false);
		jPanelButtons.setBorder(null);
		jPanelButtons.setLayout(gridLayoutButtons);
		jPanelButtons.setFocusable(false);
		gridLayoutInfo.setColumns(1);
		gridLayoutInfo.setRows(2);
		gridLayoutInfo.setVgap(4);
		jPanelInfo.setLayout(gridLayoutInfo);
		jPanelInfo.add(jLabelSessionID);
		jPanelInfo.add(jLabelFunctionID);
		jPanelInfo.setFocusable(false);
		gridLayoutButtons.setColumns(7);
		gridLayoutButtons.setRows(1);
		gridLayoutButtons.setHgap(2);
		//
		for (int i = 0; i < 7; i++) {
			jButtonArray[i] = new JButton();
			jButtonArray[i].setBounds(new Rectangle(0, 0, 90, 30));
			jButtonArray[i].setFocusable(false);
			jButtonArray[i].addActionListener(new XF300_FunctionButton_actionAdapter(this));
			jPanelButtonArray[i] = new JPanel();
			jPanelButtonArray[i].setLayout(new BorderLayout());
			jPanelButtonArray[i].add(jButtonArray[i], BorderLayout.CENTER);
			actionButtonArray[i] = new ButtonAction(jButtonArray[i]);
			jPanelButtons.add(jPanelButtonArray[i]);
		}
		//
		jPanelMain.add(jSplitPaneMain, BorderLayout.CENTER);
		jPanelMain.add(jPanelBottom, BorderLayout.SOUTH);
		jPanelBottom.add(jPanelInfo, BorderLayout.EAST);
		this.getContentPane().add(jPanelMain, BorderLayout.CENTER);
		this.setSize(new Dimension(scrSize.width, scrSize.height));
		this.addWindowFocusListener(new XF300_WindowAdapter(this));
		this.addKeyListener(new XF300_keyAdapter(this));
	}

	public HashMap<String, Object> execute(org.w3c.dom.Element functionElement, HashMap<String, Object> parmMap) {
		org.w3c.dom.Element workElement;
		String workStr, workAlias, workTableID, workFieldID;
		StringTokenizer workTokenizer;
		org.w3c.dom.Element fieldElement;

		try {
			setCursor(new Cursor(Cursor.WAIT_CURSOR));

			///////////////////
			// Process parms //
			///////////////////
			parmMap_ = parmMap;
			if (parmMap_ == null) {
				parmMap_ = new HashMap<String, Object>();
			}
			returnMap_.clear();
			returnMap_.putAll(parmMap_);
			returnMap_.put("RETURN_CODE", "00");

			///////////////////////////
			// Initializing variants //
			///////////////////////////
			instanceIsAvailable = false;
			isToBeCanceled = false;
			exceptionLog = new ByteArrayOutputStream();
			exceptionStream = new PrintStream(exceptionLog);
			exceptionHeader = "";
			processLog.delete(0, processLog.length());
			messageList.clear();
			functionElement_ = functionElement;
			connection = session_.getConnection();
			programSequence = session_.writeLogOfFunctionStarted(functionElement_.getAttribute("ID"), functionElement_.getAttribute("Name"));
			keyInputDialog = null;

			//////////////////////////////////////
			// Setup Script Engine and Bindings //
			//////////////////////////////////////
			scriptEngine = session_.getScriptEngineManager().getEngineByName("js");
			headerScriptBindings = scriptEngine.getBindings(ScriptContext.ENGINE_SCOPE);
			headerScriptBindings.clear();
			headerScriptBindings.put("instance", (XFScriptable)this);
		
			//////////////////////////////
			// Set panel configurations //
			//////////////////////////////
			jLabelSessionID.setText(session_.getSessionID());
			jLabelFunctionID.setText("300" + "-" + instanceArrayIndex_ + "-" + functionElement_.getAttribute("ID"));
			FontMetrics metrics = jLabelFunctionID.getFontMetrics(new java.awt.Font("Dialog", 0, FONT_SIZE));
			jPanelInfo.setPreferredSize(new Dimension(metrics.stringWidth(jLabelFunctionID.getText()), 35));
			this.setTitle(functionElement_.getAttribute("Name"));
			if (functionElement_.getAttribute("Size").equals("")) {
				this.setPreferredSize(new Dimension(scrSize.width, scrSize.height));
				this.setLocation(0, 0);
			} else {
				workTokenizer = new StringTokenizer(functionElement_.getAttribute("Size"), ";" );
				int width = Integer.parseInt(workTokenizer.nextToken());
				int height = Integer.parseInt(workTokenizer.nextToken());
				this.setPreferredSize(new Dimension(width, height));
				int posX = (scrSize.width - width) / 2;
				int posY = (scrSize.height - height) / 2;
				this.setLocation(posX, posY);
			}
			this.pack();

			/////////////////////////////////////////////////
			// Setup information of Header Table and Lists //
			/////////////////////////////////////////////////
			headerTable_ = new XF300_HeaderTable(functionElement_, this);
			headerReferTableList.clear();
			headerReferElementList = headerTable_.getTableElement().getElementsByTagName("Refer");
			workSortingList = XFUtility.getSortedListModel(headerReferElementList, "Order");
			for (int i = 0; i < workSortingList.getSize(); i++) {
				headerReferTableList.add(new XF300_HeaderReferTable((org.w3c.dom.Element)workSortingList.getElementAt(i), this));
			}

			/////////////////////////////////////////////////
			// Setup Header Fields and Fetch Header Record //
			/////////////////////////////////////////////////
			jPanelHeaderFields.removeAll();
			headerFieldList.clear();
			Dimension dimOfPriviousField = new Dimension(0,0);
			Dimension dim = new Dimension(0,0);
			int posX = 0;
			int posY = 0;
			int biggestWidth = 300;
			int biggestHeight = 30;
			boolean firstVisibleField = true;
			//Rectangle rec;
			//
			NodeList headerFieldElementList = functionElement_.getElementsByTagName("Field");
			workSortingList = XFUtility.getSortedListModel(headerFieldElementList, "Order");
			for (int i = 0; i < workSortingList.getSize(); i++) {
				//
				headerFieldList.add(new XF300_HeaderField((org.w3c.dom.Element)workSortingList.getElementAt(i), this));
				if (headerFieldList.get(i).isVisibleOnPanel()) {
					if (firstVisibleField) {
						posX = 0;
						posY = this.FIELD_VERTICAL_MARGIN + 3;
						firstVisibleField = false;
					} else {
						if (headerFieldList.get(i).isHorizontal()) {
							posX = posX + dimOfPriviousField.width + headerFieldList.get(i).getPositionMargin() + this.FIELD_HORIZONTAL_MARGIN;
						} else {
							posX = 0;
							posY = posY + dimOfPriviousField.height+ headerFieldList.get(i).getPositionMargin() + this.FIELD_VERTICAL_MARGIN;
						}
					}
					dim = headerFieldList.get(i).getPreferredSize();
					headerFieldList.get(i).setBounds(posX, posY, dim.width, dim.height);
					jPanelHeaderFields.add(headerFieldList.get(i));
					//
					if (posX + dim.width > biggestWidth) {
						biggestWidth = posX + dim.width;
					}
					//
					if (posY + dim.height > biggestHeight) {
						biggestHeight = posY + dim.height;
					}
					//
					if (headerFieldList.get(i).isHorizontal()) {
						dimOfPriviousField = new Dimension(dim.width, XFUtility.FIELD_UNIT_HEIGHT);
					} else {
						dimOfPriviousField = new Dimension(dim.width, dim.height);
					}
				}
			}
			//
			// Add primary table keys as HIDDEN fields if they are not on the header field list //
			for (int i = 0; i < headerTable_.getKeyFieldIDList().size(); i++) {
				if (!containsHeaderField(headerTable_.getTableID(), "", headerTable_.getKeyFieldIDList().get(i))) {
					headerFieldList.add(new XF300_HeaderField(headerTable_.getTableID(), "", headerTable_.getKeyFieldIDList().get(i), this));
				}
			}
			//
			// Analyze fields in script and add them as HIDDEN columns if necessary //
			for (int i = 0; i < headerTable_.getScriptList().size(); i++) {
				if	(headerTable_.getScriptList().get(i).isToBeRunAtEvent("BR", "")
						|| headerTable_.getScriptList().get(i).isToBeRunAtEvent("AR", "")) {
					for (int j = 0; j < headerTable_.getScriptList().get(i).getFieldList().size(); j++) {
						workTokenizer = new StringTokenizer(headerTable_.getScriptList().get(i).getFieldList().get(j), "." );
						workAlias = workTokenizer.nextToken();
						workTableID = getTableIDOfTableAlias(workAlias, -1);
						workFieldID = workTokenizer.nextToken();
						if (!containsHeaderField(workTableID, workAlias, workFieldID)) {
							fieldElement = session_.getFieldElement(workTableID, workFieldID);
							if (fieldElement == null) {
								String msg = res.getString("FunctionError1") + headerTable_.getTableID() + res.getString("FunctionError2") + headerTable_.getScriptList().get(i).getName() + res.getString("FunctionError3") + workAlias + "_" + workFieldID + res.getString("FunctionError4");
								JOptionPane.showMessageDialog(null, msg);
								throw new Exception(msg);
							} else {
								if (headerTable_.isValidDataSource(workTableID, workAlias, workFieldID)) {
									headerFieldList.add(new XF300_HeaderField(workTableID, workAlias, workFieldID, this));
								}
							}
						}
					}
				}
			}
			//
			// Analyze header refer tables and add their fields as HIDDEN header fields if necessary //
			for (int i = headerReferTableList.size()-1; i > -1; i--) {
				for (int j = 0; j < headerReferTableList.get(i).getFieldIDList().size(); j++) {
					if (containsHeaderField(headerReferTableList.get(i).getTableID(), headerReferTableList.get(i).getTableAlias(), headerReferTableList.get(i).getFieldIDList().get(j))) {
						headerReferTableList.get(i).setToBeExecuted(true);
						break;
					}
				}
				if (headerReferTableList.get(i).isToBeExecuted()) {
					for (int j = 0; j < headerReferTableList.get(i).getFieldIDList().size(); j++) {
						if (!containsHeaderField(headerReferTableList.get(i).getTableID(), headerReferTableList.get(i).getTableAlias(), headerReferTableList.get(i).getFieldIDList().get(j))) {
							headerFieldList.add(new XF300_HeaderField(headerReferTableList.get(i).getTableID(), headerReferTableList.get(i).getTableAlias(), headerReferTableList.get(i).getFieldIDList().get(j), this));
						}
					}
					for (int j = 0; j < headerReferTableList.get(i).getWithKeyFieldIDList().size(); j++) {
						workTokenizer = new StringTokenizer(headerReferTableList.get(i).getWithKeyFieldIDList().get(j), "." );
						workAlias = workTokenizer.nextToken();
						workTableID = getTableIDOfTableAlias(workAlias, -1);
						workFieldID = workTokenizer.nextToken();
						if (!containsHeaderField(workTableID, workAlias, workFieldID)) {
							headerFieldList.add(new XF300_HeaderField(workTableID, workAlias, workFieldID, this));
						}
					}
				}
			}
			//
			// Add detail fields on header table as HIDDEN fields if they are not on the header fields list //
			NodeList columnFieldList = functionElement_.getElementsByTagName("Column");
			for (int i = 0; i < columnFieldList.getLength(); i++) {
				workElement = (org.w3c.dom.Element)columnFieldList.item(i);
				workStr = workElement.getAttribute("DataSource");
				if (workStr.substring(0, workStr.indexOf(".")).equals(headerTable_.getTableID())) {
					if (!containsHeaderField(headerTable_.getTableID(), "", workStr.substring(workStr.indexOf(".") + 1, workStr.length()))) {
						headerFieldList.add(new XF300_HeaderField(headerTable_.getTableID(), "", workStr.substring(workStr.indexOf(".") + 1, workStr.length()), this));
					}
				}
			}
			//
			jSplitPaneMain.setDividerLocation(this.getPreferredSize().height - 118);
			this.pack();
			jPanelHeaderFields.setPreferredSize(new Dimension(biggestWidth, biggestHeight));
			jSplitPaneCenter.setDividerLocation(biggestHeight + this.FIELD_VERTICAL_MARGIN + 13);
			//this.pack();

			/////////////////////////////////////////////////
			// Setup information of header function called //
			/////////////////////////////////////////////////
			NodeList functionList = session_.getFunctionList();
			headerFunctionID = functionElement_.getAttribute("HeaderFunction");
			headerFunctionElement = null;
			if (!functionElement_.getAttribute("HeaderFunction").equals("")) {
				for (int k = 0; k < functionList.getLength(); k++) {
					workElement = (org.w3c.dom.Element)functionList.item(k);
					if (workElement.getAttribute("ID").equals(functionElement_.getAttribute("HeaderFunction"))) {
						headerFunctionElement = workElement;
						break;
					}
				}
			}

			///////////////////////////////////////
			// Setup information of detail table //
			///////////////////////////////////////
			tablesReadyToUse = false;
			jPanelBottom.remove(jPanelButtons);
			jPanelBottom.remove(jPanelDummy);
			int tabCount = jTabbedPane.getTabCount();
			for (int i = 0; i < tabCount; i++) {
				jTabbedPane.removeTabAt(0);
			}
			boolean firstTab = true;
			NodeList detailElementList = functionElement_.getElementsByTagName("Detail");
			detailTabSortingList = XFUtility.getSortedListModel(detailElementList, "Order");
			for (int i = 0; i < detailTabSortingList.getSize(); i++) {
				workElement = (org.w3c.dom.Element)detailTabSortingList.getElementAt(i);
				//
				if (firstTab) {
					//jTabbedPane.addTab(workElement.getAttribute("Caption"), null, jScrollPaneTable);
					jTabbedPane.addTab(workElement.getAttribute("Caption"), null, jPanelCenter);
					firstTab = false;
				} else {
					jTabbedPane.addTab(workElement.getAttribute("Caption"), null, null);
				}
				//
				detailTableArray[i] = new XF300_DetailTable(workElement, i, this);
				//
				detailReferTableListArray[i].clear();
				detailReferElementList[i] = detailTableArray[i].getTableElement().getElementsByTagName("Refer");
				workSortingList = XFUtility.getSortedListModel(detailReferElementList[i], "Order");
				for (int j = 0; j < workSortingList.getSize(); j++) {
					detailReferTableListArray[i].add(new XF300_DetailReferTable((org.w3c.dom.Element)workSortingList.getElementAt(j), i, this));
				}
				//
				initialMsgArray[i] = workElement.getAttribute("InitialMsg");
				//
				// Add header table keys as HIDDEN fields if they are not on the header field list //
				for (int j = 0; j < detailTableArray[i].getHeaderKeyFieldIDList().size(); j++) {
					if (!containsHeaderField(headerTable_.getTableID(), "", detailTableArray[i].getHeaderKeyFieldIDList().get(j))) {
						headerFieldList.add(new XF300_HeaderField(headerTable_.getTableID(), "", detailTableArray[i].getHeaderKeyFieldIDList().get(j), this));
					}
				}
				//
				detailScriptBindingsArray[i] = scriptEngine.createBindings();
				detailScriptBindingsArray[i].putAll(headerScriptBindings);
				//detailScriptBindingsArray[i].put("instance", (XFScriptable)this);

				///////////////////////////
				// Setup Filter fields ////
				///////////////////////////
				filterListArray[i].clear();
				jPanelFilter[i].removeAll();
				anyFilterIsEditable[i] = false;
				GridLayout gridLayoutFilter = new GridLayout();
				NodeList FilterFieldList = workElement.getElementsByTagName("Filter");
				workSortingList = XFUtility.getSortedListModel(FilterFieldList, "Order");
				if (workSortingList.getSize() <= 2) {
					gridLayoutFilter.setColumns(2);
					gridLayoutFilter.setRows(1);
					filterWidth[i] = (this.getPreferredSize().getWidth() - 90) / 2;
				}
				if (workSortingList.getSize() == 3) {
					gridLayoutFilter.setColumns(3);
					gridLayoutFilter.setRows(1);
					filterWidth[i] = (this.getPreferredSize().getWidth() - 90) / 3;
				}
				if (workSortingList.getSize() == 4) {
					gridLayoutFilter.setColumns(4);
					gridLayoutFilter.setRows(1);
					filterWidth[i] = (this.getPreferredSize().getWidth() - 90) / 4;
				}
				jPanelFilter[i].setLayout(gridLayoutFilter);
				for (int j = 0; j < workSortingList.getSize() && j < 4; j++) {
					filterListArray[i].add(new XF300_Filter((org.w3c.dom.Element)workSortingList.getElementAt(j), this, i));
					if (filterListArray[i].get(j).isEditable()) {
						anyFilterIsEditable[i] = true;
					}
					jPanelFilter[i].add(filterListArray[i].get(j));
				}

				///////////////////////////////////////////
				// Setup components for JTable of Detail //
				///////////////////////////////////////////
				tableModelMainArray[i] = new TableModelReadOnlyList();
				jTableMainArray[i].setModel(tableModelMainArray[i]);
				TableColumn column = null;
				detailColumnListArray[i].clear();
				//
				// Add Column on table model //
				tableModelMainArray[i].addColumn("NO."); //column index:0 //
				int columnIndex = 0;
				columnFieldList = workElement.getElementsByTagName("Column");
				workSortingList = XFUtility.getSortedListModel(columnFieldList, "Order");
				for (int j = 0; j < workSortingList.getSize(); j++) {
					detailColumnListArray[i].add(new XF300_DetailColumn(workElement.getAttribute("Table"), (org.w3c.dom.Element)workSortingList.getElementAt(j), this, i));
					if (detailColumnListArray[i].get(j).isVisibleOnPanel()) {
						columnIndex++;
						detailColumnListArray[i].get(j).setColumnIndex(columnIndex);
						tableModelMainArray[i].addColumn(detailColumnListArray[i].get(j).getCaption());
					}
				}
				//
				// Set attributes to each column //
				column = jTableMainArray[i].getColumnModel().getColumn(0);
				column.setPreferredWidth(38);
				column.setCellRenderer(new XF300_DetailRowNumberRenderer());
				for (int j = 0; j < workSortingList.getSize(); j++) {
					if (detailColumnListArray[i].get(j).isVisibleOnPanel()) {
						column = jTableMainArray[i].getColumnModel().getColumn(detailColumnListArray[i].get(j).getColumnIndex());
						column.setPreferredWidth(detailColumnListArray[i].get(j).getWidth());
						column.setCellRenderer(detailColumnListArray[i].get(j).getCellRenderer());
					}
				}
				//
				// Add detail table key fields as HIDDEN column if they are not on detail field list //
				for (int j = 0; j < detailTableArray[i].getKeyFieldIDList().size(); j++) {
					if (!containsDetailField(i, detailTableArray[i].getTableID(), "", detailTableArray[i].getKeyFieldIDList().get(j))) {
						detailColumnListArray[i].add(new XF300_DetailColumn(detailTableArray[i].getTableID(), detailTableArray[i].getTableID(), "", detailTableArray[i].getKeyFieldIDList().get(j), this, i));
					}
				}
				//
				// Add OrderBy fields as HIDDEN column if they are not on detail field list //
				for (int j = 0; j < detailTableArray[i].getOrderByFieldIDList().size(); j++) {
					workStr = detailTableArray[i].getOrderByFieldIDList().get(j).replace("(D)", "");
					workStr = workStr.replace("(A)", "");
					workTokenizer = new StringTokenizer(workStr, "." );
					workAlias = workTokenizer.nextToken();
					workTableID = getTableIDOfTableAlias(workAlias, i);
					workFieldID = workTokenizer.nextToken();
					if (!containsDetailField(i, workTableID, workAlias, workFieldID)) {
						detailColumnListArray[i].add(new XF300_DetailColumn(detailTableArray[i].getTableID(), workTableID, workAlias, workFieldID, this, i));
					}
				}
				//
				// Add filter fields as HIDDEN columns if they are not on the column list //
				for (int j = 0; j < filterListArray[i].size(); j++) {
					if (!containsDetailField(i, filterListArray[i].get(j).getTableID(), filterListArray[i].get(j).getTableAlias(), filterListArray[i].get(j).getFieldID())) {
						detailColumnListArray[i].add(new XF300_DetailColumn(detailTableArray[i].getTableID(), filterListArray[i].get(j).getTableID(), filterListArray[i].get(j).getTableAlias(), filterListArray[i].get(j).getFieldID(), this, i));
					}
				}
				//
				// Analyze fields in script and add them as HIDDEN columns if necessary //
				for (int j = 0; j < detailTableArray[i].getScriptList().size(); j++) {
					if	(detailTableArray[i].getScriptList().get(j).isToBeRunAtEvent("BR", "")
							|| detailTableArray[i].getScriptList().get(j).isToBeRunAtEvent("AR", "")) {
						for (int k = 0; k < detailTableArray[i].getScriptList().get(j).getFieldList().size(); k++) {
							workTokenizer = new StringTokenizer(detailTableArray[i].getScriptList().get(j).getFieldList().get(k), "." );
							workAlias = workTokenizer.nextToken();
							workTableID = getTableIDOfTableAlias(workAlias, i);
							workFieldID = workTokenizer.nextToken();
							if (!containsDetailField(i, workTableID, workAlias, workFieldID)) {
								fieldElement = session_.getFieldElement(workTableID, workFieldID);
								if (fieldElement == null) {
									String msg = res.getString("FunctionError1") + detailTableArray[i].getTableID() + res.getString("FunctionError2") + detailTableArray[i].getScriptList().get(j).getName() + res.getString("FunctionError3") + workAlias + "_" + workFieldID + res.getString("FunctionError4");
									JOptionPane.showMessageDialog(null, msg);
									throw new Exception(msg);
								} else {
									if (detailTableArray[i].isValidDataSource(i, workTableID, workAlias, workFieldID)) {
										detailColumnListArray[i].add(new XF300_DetailColumn(detailTableArray[i].getTableID(), workTableID, workAlias, workFieldID, this, i));
									}
								}
							}
						}
					}
				}
				//
				// Analyze detail refer tables and add their fields as HIDDEN column fields if necessary //
				for (int j = detailReferTableListArray[i].size()-1; j > -1; j--) {
					for (int k = 0; k < detailReferTableListArray[i].get(j).getFieldIDList().size(); k++) {
						if (containsDetailField(i, detailReferTableListArray[i].get(j).getTableID(), detailReferTableListArray[i].get(j).getTableAlias(), detailReferTableListArray[i].get(j).getFieldIDList().get(k))) {
							detailReferTableListArray[i].get(j).setToBeExecuted(true);
							break;
						}
					}
					if (detailReferTableListArray[i].get(j).isToBeExecuted()) {
						for (int k = 0; k < detailReferTableListArray[i].get(j).getFieldIDList().size(); k++) {
							if (!containsDetailField(i, detailReferTableListArray[i].get(j).getTableID(), detailReferTableListArray[i].get(j).getTableAlias(), detailReferTableListArray[i].get(j).getFieldIDList().get(k))) {
								detailColumnListArray[i].add(new XF300_DetailColumn(detailTableArray[i].getTableID(), detailReferTableListArray[i].get(j).getTableID(), detailReferTableListArray[i].get(j).getTableAlias(), detailReferTableListArray[i].get(j).getFieldIDList().get(k), this, i));
							}
						}
						for (int k = 0; k < detailReferTableListArray[i].get(j).getWithKeyFieldIDList().size(); k++) {
							workTokenizer = new StringTokenizer(detailReferTableListArray[i].get(j).getWithKeyFieldIDList().get(k), "." );
							workAlias = workTokenizer.nextToken();
							workTableID = getTableIDOfTableAlias(workAlias, i);
							workFieldID = workTokenizer.nextToken();
							if (!containsDetailField(i, workTableID, workAlias, workFieldID)) {
								detailColumnListArray[i].add(new XF300_DetailColumn(detailTableArray[i].getTableID(), workTableID, workAlias, workFieldID, this, i));
							}
						}
					}
				}

				/////////////////////////////////////////////////
				// Setup information of detail function called //
				/////////////////////////////////////////////////
				org.w3c.dom.Element workElement2;
				detailFunctionIDArray[i] = workElement.getAttribute("DetailFunction");
				detailFunctionElementArray[i] = null;
				for (int j = 0; j < functionList.getLength(); j++) {
					workElement2 = (org.w3c.dom.Element)functionList.item(j);
					if (workElement2.getAttribute("ID").equals(workElement.getAttribute("DetailFunction"))) {
						detailFunctionElementArray[i] = workElement2;
						break;
					}
				}
			}
			tablesReadyToUse = true;


			/////////////////////////
			// Fetch Header Record //
			/////////////////////////
			fetchHeaderRecord(false);
			if (!isToBeCanceled) {

				/////////////////////////////////////////////
				// Select Detail records and setup JTables //
				/////////////////////////////////////////////
				for (int i = 0; i < detailTabSortingList.getSize(); i++) {
					if (filterListArray[i].size() == 0 || !anyFilterIsEditable[i]) {
						selectDetailRecordsAndSetupTableRows(i, true);
					}
				}

				//////////////////////////////////////////////
				// Set first tab to be selected and focused //
				//////////////////////////////////////////////
				if (jTabbedPane.getTabCount() == 0) {
					jPanelBottom.add(jPanelDummy, BorderLayout.CENTER);
					jTextAreaMessages.setText(res.getString("FunctionMessage30"));
				} else {
					jPanelBottom.add(jPanelButtons, BorderLayout.CENTER);
					jTabbedPane.setSelectedIndex(0);
					jTabbedPane_stateChanged(null);
					jTabbedPane.requestFocus();
				}
				if (parmMap_.containsKey("INITIAL_MESSAGE")) {
					jTextAreaMessages.setText((String)parmMap_.get("INITIAL_MESSAGE"));
				}

				////////////////
				// Show Panel //
				////////////////
				setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				this.setVisible(true);
			}

		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, res.getString("FunctionError5"));
			e.printStackTrace(exceptionStream);
			setErrorAndCloseFunction();
		} finally {
			setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		}

		///////////////////////////////
		// Release instance and exit //
		///////////////////////////////
		return returnMap_;
	}

	public boolean isAvailable() {
		return instanceIsAvailable;
	}
	
	protected void processWindowEvent(WindowEvent e) {
		super.processWindowEvent(e);
		if (e.getID() == WindowEvent.WINDOW_CLOSING) {
			closeFunction();
		}
	}

	void windowGainedFocus(WindowEvent e) {
		if (jTabbedPane.getTabCount() > 0) {
			if (jTableMainArray[jTabbedPane.getSelectedIndex()].getRowCount() >= 1) {
				jTableMainArray[jTabbedPane.getSelectedIndex()].requestFocus();
			} else {
				jTabbedPane.requestFocus();
			}
		}
	}

	void setErrorAndCloseFunction() {
		returnMap_.put("RETURN_CODE", "99");
		closeFunction();
	}

	void closeFunction() {
		instanceIsAvailable = true;
		String wrkStr;
		if (exceptionLog.size() > 0 || !exceptionHeader.equals("")) {
			wrkStr = processLog.toString() + "\nERROR LOG:\n" + exceptionHeader + exceptionLog.toString();
		} else {
			wrkStr = processLog.toString();
		}
		wrkStr = wrkStr.replace("'", "\"");
		session_.writeLogOfFunctionClosed(programSequence, returnMap_.get("RETURN_CODE").toString(), wrkStr);
		this.setVisible(false);
	}
	
	public void cancelWithMessage(String message) {
		if (!message.equals("")) {
			JOptionPane.showMessageDialog(null, message);
		}
		returnMap_.put("RETURN_CODE", "01");
		isToBeCanceled = true;
	}

	public void callFunction(String functionID) {
		try {
			returnMap_ = XFUtility.callFunction(session_, functionID, parmMap_);
		} catch (Exception e) {
			String message = res.getString("FunctionError9") + functionID + res.getString("FunctionError10");
			JOptionPane.showMessageDialog(null,message);
			exceptionHeader = message;
			setErrorAndCloseFunction();
		}
	}

	void fetchHeaderRecord(boolean cancelWithoutMessage) {
		try {
			ResultSet resultOfHeaderTable;
			Statement statementForHeaderTable = connection.createStatement();
			String sql;
			//
			boolean recordNotFound = true;
			String inputDialogMessage = "";
			boolean keyInputRequired = false;
			for (int i = 0; i < headerFieldList.size(); i++) {
				if (headerFieldList.get(i).isKey()) {
					if (parmMap_.containsKey(headerFieldList.get(i).getFieldID())) {
						if (parmMap_.get(headerFieldList.get(i).getFieldID()).equals(headerFieldList.get(i).getNullValue())) {
							keyInputRequired = true;
						}
					} else {
						keyInputRequired = true;
					}
				} else {
					headerFieldList.get(i).setValue(headerFieldList.get(i).getNullValue());
				}
			}
			//
			while (recordNotFound) {
				if (keyInputRequired) {
					if (keyInputDialog == null) {
						keyInputDialog = new XF300_KeyInputDialog(this);
					}
					parmMap_ = keyInputDialog.requestKeyValues(inputDialogMessage);
					if (parmMap_.size() == 0) {
						isToBeCanceled = true;
						returnMap_.put("RETURN_CODE", "01");
						closeFunction();
						return;
					}
				}
				//
				headerTable_.runScript("BR", ""); /* Script to be run BEFORE READ */
				//
				sql = headerTable_.getSelectSQL();
				XFUtility.appendLog(sql, processLog);
				resultOfHeaderTable = statementForHeaderTable.executeQuery(sql);
				if (resultOfHeaderTable.next()) {
					//
					recordNotFound = false;
					//
					for (int i = 0; i < headerFieldList.size(); i++) {
						if (headerFieldList.get(i).getTableID().equals(headerTable_.getTableID())) {
							headerFieldList.get(i).setValueOfResultSet(resultOfHeaderTable);
						}
					}
					//
					headerTable_.runScript("AR", "BR()"); /* Script to be run AFETR READ primary table*/
					//
					resultOfHeaderTable.close();
					//
				} else {
					//
					resultOfHeaderTable.close();
					//
					if (keyInputRequired) {
						inputDialogMessage = res.getString("FunctionError37");
					} else {
						if (!cancelWithoutMessage) {
							JOptionPane.showMessageDialog(this, res.getString("FunctionError38"));
						}
						isToBeCanceled = true;
						returnMap_.put("RETURN_CODE", "01");
						closeFunction();
						return;
					}
				}
			}
			//
			ResultSet resultOfReferTable;
			Statement statementForReferTable = connection.createStatement();
			for (int i = 0; i < headerReferTableList.size(); i++) {
				//
				if (headerReferTableList.get(i).isToBeExecuted()) {
					//
					headerTable_.runScript("AR", "BR(" + headerReferTableList.get(i).getTableAlias() + ")"); /* Script to be run BEFORE READ */
					//
					sql = headerReferTableList.get(i).getSelectSQL();
					XFUtility.appendLog(sql, processLog);
					resultOfReferTable = statementForReferTable.executeQuery(sql);
					while (resultOfReferTable.next()) {
						//
						if (headerReferTableList.get(i).isRecordToBeSelected(resultOfReferTable)) {
							//
							for (int j = 0; j < headerFieldList.size(); j++) {
								if (headerFieldList.get(j).getTableAlias().equals(headerReferTableList.get(i).getTableAlias())) {
									headerFieldList.get(j).setValueOfResultSet(resultOfReferTable);
								}
							}
							//
							headerTable_.runScript("AR", "AR(" + headerReferTableList.get(i).getTableAlias() + ")"); /* Script to be run AFTER READ */
						}
					}
					//
					resultOfReferTable.close();
				}
			}
			//
			headerTable_.runScript("AR", "AR()"); /* Script to be run AFTER READ */
			//
		} catch (SQLException e) {
			JOptionPane.showMessageDialog(this, res.getString("FunctionError6"));
			e.printStackTrace(exceptionStream);
			setErrorAndCloseFunction();
		} catch(ScriptException e) {
			JOptionPane.showMessageDialog(this, res.getString("FunctionError7") + this.getScriptNameRunning() + res.getString("FunctionError8"));
			exceptionHeader = "'" + this.getScriptNameRunning() + "' Script error\n";
			e.printStackTrace(exceptionStream);
			setErrorAndCloseFunction();
		}
	}

	void setupFunctionKeysAndButtonsForTabIndex(int tabIndex) {
		//
		InputMap inputMap  = jPanelMain.getInputMap(JPanel.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		inputMap.clear();
		ActionMap actionMap = jPanelMain.getActionMap();
		actionMap.clear();
		//
		for (int i = 0; i < 7; i++) {
			jButtonArray[i].setText("");
			jButtonArray[i].setVisible(false);
			actionDefinitionArray[i] = "";
		}
		actionF6 = "";
		actionF8 = "";
		//
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), "HELP");
		actionMap.put("HELP", helpAction);
		//
		int workIndex;
		org.w3c.dom.Element element;
		NodeList buttonList = detailTableArray[tabIndex].getDetailTableElement().getElementsByTagName("Button");
		for (int i = 0; i < buttonList.getLength(); i++) {
			element = (org.w3c.dom.Element)buttonList.item(i);
			//
			workIndex = Integer.parseInt(element.getAttribute("Position"));
			actionDefinitionArray[workIndex] = element.getAttribute("Action");
			XFUtility.setCaptionToButton(jButtonArray[workIndex], element, "");
			jButtonArray[workIndex].setVisible(true);
			inputMap.put(XFUtility.getKeyStroke(element.getAttribute("Number")), "actionButton" + workIndex);
			actionMap.put("actionButton" + workIndex, actionButtonArray[workIndex]);
			//
			if (element.getAttribute("Number").equals("6")) {
				actionF6 = element.getAttribute("Position");
			}
			if (element.getAttribute("Number").equals("8")) {
				actionF8 = element.getAttribute("Position");
			}
		}
	}

	void selectDetailRecordsAndSetupTableRows(int index, boolean isToOverrideMessage) {
		HashMap<String, Object> keyMap;
		ArrayList<Object> columnValueList;
		ArrayList<Object> orderByValueList;
		String workStr;
		boolean readyToValidate;
		boolean toBeSelected;
		// 
		try {
			//
			int rowCount = tableModelMainArray[index].getRowCount();
			for (int i = 0; i < rowCount; i++) {
				tableModelMainArray[index].removeRow(0);
			}
			workingRowListArray[index].clear();
			//
			ResultSet resultOfDetailTable;
			Statement statementForDetailTable = connection.createStatement();
			ResultSet resultOfDetailReferTable;
			Statement statementForReferTable = connection.createStatement();
			//
			int countOfRows = 0;
			String sql = detailTableArray[index].getSelectSQL();
			XFUtility.appendLog(sql, processLog);
			resultOfDetailTable = statementForDetailTable.executeQuery(sql);
			while (resultOfDetailTable.next()) {
				//
				for (int i = 0; i < detailColumnListArray[index].size(); i++) {
					detailColumnListArray[index].get(i).setReadyToValidate(false);
					detailColumnListArray[index].get(i).initialize();
				}
				//
				detailTableArray[index].runScript(index, "BR", ""); /* Script to be run BEFORE READ */
				//
				for (int i = 0; i < detailColumnListArray[index].size(); i++) {
					if (detailColumnListArray[index].get(i).getTableID().equals(detailTableArray[index].getTableID())) {
						readyToValidate = detailColumnListArray[index].get(i).setValueOfResultSet(resultOfDetailTable);
						detailColumnListArray[index].get(i).setReadyToValidate(readyToValidate);
					}
				}
				//
				detailTableArray[index].runScript(index, "AR", "BR()"); /* Script to be run AFTER READ primary table */
				//
				if (isTheRowToBeSelected(index)) {
					//
					toBeSelected = true;
					//
					for (int i = 0; i < detailReferTableListArray[index].size(); i++) {
						//
						if (detailReferTableListArray[index].get(i).isToBeExecuted()) {
							//
							detailTableArray[index].runScript(index, "AR", "BR(" + detailReferTableListArray[index].get(i).getTableAlias() + ")"); /* Script to be run BEFORE READ */
							//
							sql = detailReferTableListArray[index].get(i).getSelectSQL();
							if (!sql.equals("")) {
								XFUtility.appendLog(sql, processLog);
								resultOfDetailReferTable = statementForReferTable.executeQuery(sql);
								while (resultOfDetailReferTable.next()) {
									//
									if (detailReferTableListArray[index].get(i).isRecordToBeSelected(index, resultOfDetailReferTable)) {
										//
										for (int j = 0; j < detailColumnListArray[index].size(); j++) {
											if (detailColumnListArray[index].get(j).getTableAlias().equals(detailReferTableListArray[index].get(i).getTableAlias())) {
												detailColumnListArray[index].get(j).setValueOfResultSet(resultOfDetailReferTable);
											}
										}
										//
										detailTableArray[index].runScript(index, "AR", "AR(" + detailReferTableListArray[index].get(i).getTableAlias() + ")"); /* Script to be run AFTER READ */
										//
										if (!isTheRowToBeSelected(index)) {
											toBeSelected = false;
											break;
										}
									}
								}
								//
								resultOfDetailReferTable.close();
							}
							//
							if (!toBeSelected) {
								break;
							}
						}
					}
					//
					detailTableArray[index].runScript(index, "AR", "AR()"); /* Script to be run AFTER READ */
					//
					if (toBeSelected) {
						//
						for (int i = 0; i < detailColumnListArray[index].size(); i++) {
							detailColumnListArray[index].get(i).setReadyToValidate(true);
						}
						//
						if (isTheRowToBeSelected(index)) {
							//
							keyMap = new HashMap<String, Object>();
							for (int i = 0; i < detailTableArray[index].getKeyFieldIDList().size(); i++) {
								keyMap.put(detailTableArray[index].getKeyFieldIDList().get(i), resultOfDetailTable.getObject(detailTableArray[index].getKeyFieldIDList().get(i)));
							}
							//
							if (detailTableArray[index].hasOrderByAsItsOwnFields()) {
								Object[] Cell = new Object[detailColumnListArray[index].size() + 1];
								Cell[0] = new XF300_DetailRowNumber(countOfRows + 1, keyMap);
								for (int i = 0; i < detailColumnListArray[index].size(); i++) {
									if (detailColumnListArray[index].get(i).isVisibleOnPanel()) {
										Cell[detailColumnListArray[index].get(i).getColumnIndex()] = detailColumnListArray[index].get(i).getCellObject();
									}
								}
								tableModelMainArray[index].addRow(Cell);
							} else {
								columnValueList = new ArrayList<Object>();
								for (int i = 0; i < detailColumnListArray[index].size(); i++) {
									if (detailColumnListArray[index].get(i).isVisibleOnPanel()) {
										columnValueList.add(detailColumnListArray[index].get(i).getCellObject());
									}
								}
								orderByValueList = new ArrayList<Object>();
								for (int i = 0; i < detailTableArray[index].getOrderByFieldIDList().size(); i++) {
									workStr = detailTableArray[index].getOrderByFieldIDList().get(i).replace("(D)", "");
									workStr = workStr.replace("(A)", "");
									for (int j = 0; j < detailColumnListArray[index].size(); j++) {
										if (detailColumnListArray[index].get(j).getDataSourceName().equals(workStr)) {
											orderByValueList.add(detailColumnListArray[index].get(j).getExternalValue());
											break;
										}
									}
								}
								workingRowListArray[index].add(new WorkingRow(index, columnValueList, keyMap, orderByValueList));
							}
							//
							countOfRows++;
						}
					}
				}
			}
			//
			resultOfDetailTable.close();
			//
			headerTable_.runScript("AR", "AS()"); /* Script to be run AFTER READ and AFTER SUMMARY */
			//
			if (!detailTableArray[index].hasOrderByAsItsOwnFields()) {
				WorkingRow[] workingRowArray = workingRowListArray[index].toArray(new WorkingRow[0]);
				Arrays.sort(workingRowArray, new WorkingRowComparator());
				for (int i = 0; i < workingRowArray.length; i++) {
					Object[] Cell = new Object[workingRowArray[i].getColumnValueList().size() + 1];
					Cell[0] = new XF300_DetailRowNumber(i + 1, workingRowArray[i].getKeyMap());
					for (int j = 0; j < workingRowArray[i].getColumnValueList().size(); j++) {
						Cell[j+1] = workingRowArray[i].getColumnValueList().get(j);
					}
					tableModelMainArray[index].addRow(Cell);
				}
			}
			//
			if (isToOverrideMessage) {
				//
				messageList.clear();
				//
				if (countOfRows > 0) {
					jTableMainArray[index].setRowSelectionInterval(0, 0);
					jTableMainArray[index].requestFocus();
					//
					if (detailFunctionIDArray[index].equals("")) {
						messageList.add(res.getString("FunctionMessage2"));
					} else {
						messageList.add(res.getString("FunctionMessage3"));
					}
				} else {
					messageList.add(res.getString("FunctionMessage4"));
				}
				//
				setMessagesOnPanel();
			}
			//
		} catch (SQLException e) {
			JOptionPane.showMessageDialog(this, res.getString("FunctionError6"));
			e.printStackTrace(exceptionStream);
			setErrorAndCloseFunction();
		} catch(ScriptException e) {
			JOptionPane.showMessageDialog(this, res.getString("FunctionError7") + this.getScriptNameRunning() + res.getString("FunctionError8"));
			exceptionHeader = "'" + this.getScriptNameRunning() + "' Script error\n";
			e.printStackTrace(exceptionStream);
			setErrorAndCloseFunction();
		}
	}

	void jFunctionButton_actionPerformed(ActionEvent e) {
		Component com = (Component)e.getSource();
		for (int i = 0; i < 7; i++) {
			if (com.equals(jButtonArray[i])) {
				doButtonAction(actionDefinitionArray[i]);
				break;
			}
		}
	}

	void doButtonAction(String action) {
		int pos1, pos2;
		HashMap<String, Object> returnMap;
		try {
			setCursor(new Cursor(Cursor.WAIT_CURSOR));
			//
			messageList.clear();
			//
			if (action.equals("EXIT")) {
				closeFunction();
			}
			//
			if (action.equals("HEADER")) {
				if (headerFunctionElement == null) {
					JOptionPane.showMessageDialog(this, res.getString("FunctionError41") + headerFunctionID + res.getString("FunctionError42"));
					messageList.add(res.getString("FunctionError15"));
				} else {
					returnMap = session_.getFunction().execute(headerFunctionElement, parmMap_);
					if (returnMap.get("RETURN_CODE").equals("30")) {
						returnMap_.put("RETURN_CODE", "30");
						closeFunction();
					} else {
						if (returnMap.get("RETURN_MESSAGE") == null) {
							messageList.add(XFUtility.getMessageOfReturnCode(returnMap.get("RETURN_CODE").toString()));
						} else {
							messageList.add(returnMap.get("RETURN_MESSAGE").toString());
						}
						if (returnMap.get("RETURN_CODE").equals("20")) {
							fetchHeaderRecord(true);
							for (int i = 0; i < jTabbedPane.getTabCount(); i++) {
								selectDetailRecordsAndSetupTableRows(i, false);
							}
							returnMap_.put("RETURN_CODE", returnMap.get("RETURN_CODE"));
						}
					}
				}
			}
			//
			if (action.equals("ADD")) {
				if (detailFunctionElementArray[jTabbedPane.getSelectedIndex()] == null) {
					JOptionPane.showMessageDialog(this, res.getString("FunctionError13") + detailFunctionIDArray[jTabbedPane.getSelectedIndex()] + res.getString("FunctionError14"));
					messageList.add(res.getString("FunctionError15"));
				} else {
					returnMap = session_.getFunction().execute(detailFunctionElementArray[jTabbedPane.getSelectedIndex()], parmMap_);
					if (returnMap.get("RETURN_MESSAGE") == null) {
						messageList.add(XFUtility.getMessageOfReturnCode(returnMap.get("RETURN_CODE").toString()));
					} else {
						messageList.add(returnMap.get("RETURN_MESSAGE").toString());
					}
					if (returnMap.get("RETURN_CODE").equals("10")) {
						selectDetailRecordsAndSetupTableRows(jTabbedPane.getSelectedIndex(), false);
						returnMap_.put("RETURN_CODE", returnMap.get("RETURN_CODE"));
					}
				}
			}
			//
			if (action.equals("OUTPUT")) {
				session_.browseFile(getExcellBookURI());
			}
			//
			pos1 = action.indexOf("CALL(", 0);
			if (pos1 >= 0) {
				pos2 = action.indexOf(")", pos1);
				String functionID = action.substring(pos1+5, pos2);
				//
				org.w3c.dom.Element workElement1, elementOfFunction = null;
				NodeList functionList = session_.getFunctionList();
				for (int k = 0; k < functionList.getLength(); k++) {
					workElement1 = (org.w3c.dom.Element)functionList.item(k);
					if (workElement1.getAttribute("ID").equals(functionID)) {
						elementOfFunction = workElement1;
						break;
					}
				}
				//
				if (elementOfFunction == null) {
					JOptionPane.showMessageDialog(this, res.getString("FunctionError17") + functionID + res.getString("FunctionError18"));
					messageList.add(res.getString("FunctionError15"));
				} else {
					returnMap = session_.getFunction().execute(elementOfFunction, parmMap_);
					if (returnMap.get("RETURN_CODE").equals("30")) {
						returnMap_.put("RETURN_CODE", "30");
						closeFunction();
					} else {
						if (returnMap.get("RETURN_MESSAGE") == null) {
							messageList.add(XFUtility.getMessageOfReturnCode(returnMap.get("RETURN_CODE").toString()));
						} else {
							messageList.add(returnMap.get("RETURN_MESSAGE").toString());
						}
						if (returnMap.get("RETURN_CODE").equals("10")
								|| returnMap.get("RETURN_CODE").equals("20")) {
							fetchHeaderRecord(true);
							for (int i = 0; i < jTabbedPane.getTabCount(); i++) {
								selectDetailRecordsAndSetupTableRows(i, false);
							}
							returnMap_.put("RETURN_CODE", returnMap.get("RETURN_CODE"));
						}
					}
				}
			}
			//
			setMessagesOnPanel();
			//
		} finally {
			setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		}
	}
	
	 class WorkingRow extends Object {
		private ArrayList<Object> columnValueList_ = new ArrayList<Object>();
		private ArrayList<Object> orderByValueList_ = new ArrayList<Object>();
		private HashMap<String, Object> keyMap_ = new HashMap<String, Object>();
		private int index_;
		//
		public WorkingRow(int index, ArrayList<Object> columnValueList, HashMap<String, Object> keyMap, ArrayList<Object> orderByValueList) {
			columnValueList_ = columnValueList;
			keyMap_ = keyMap;
			orderByValueList_ = orderByValueList;
			index_ = index;
		}
		public HashMap<String, Object> getKeyMap() {
			return keyMap_;
		}
		public ArrayList<Object> getColumnValueList() {
			return columnValueList_;
		}
		public ArrayList<Object> getOrderByValueList() {
			return orderByValueList_;
		}
		public int getIndex() {
			return index_;
		}
	}
		
	 class WorkingRowComparator implements java.util.Comparator<WorkingRow>{
		 public int compare(WorkingRow row1, WorkingRow row2){
			 int compareResult = 0;
			 for (int i = 0; i < row1.getOrderByValueList().size(); i++) {
				 compareResult = row1.getOrderByValueList().get(i).toString().compareTo(row2.getOrderByValueList().get(i).toString());
				 if (detailTableArray[row1.getIndex()].getOrderByFieldIDList().get(i).contains("(D)")) {
					 compareResult = compareResult * -1;
				 }
				 if (compareResult != 0) {
					 break;
				 }
			 }
			 return compareResult;
		 }
	 }

	URI getExcellBookURI() {
		File xlsFile = null;
		String xlsFileName = "";
		FileOutputStream fileOutputStream = null;
		HSSFFont font = null;
		XF300_DetailCell cellObject = null;
		//
		HSSFWorkbook workBook = new HSSFWorkbook();
		HSSFSheet workSheet = workBook.createSheet(functionElement_.getAttribute("Name"));
		workSheet.setDefaultRowHeight( (short) 300);
		HSSFFooter workSheetFooter = workSheet.getFooter();
		workSheetFooter.setRight(functionElement_.getAttribute("Name") + "  Page " + HSSFFooter.page() + " / " + HSSFFooter.numPages() );
		//
		HSSFFont fontHeader = workBook.createFont();
		fontHeader = workBook.createFont();
		fontHeader.setFontName(res.getString("XLSFontHDR"));
		fontHeader.setFontHeightInPoints((short)11);
		//
		HSSFFont fontDataBlack = workBook.createFont();
		fontDataBlack.setFontName(res.getString("XLSFontDTL"));
		fontDataBlack.setFontHeightInPoints((short)11);
		HSSFFont fontDataRed = workBook.createFont();
		fontDataRed.setFontName(res.getString("XLSFontDTL"));
		fontDataRed.setFontHeightInPoints((short)11);
		fontDataRed.setColor(HSSFColor.RED.index);
		HSSFFont fontDataBlue = workBook.createFont();
		fontDataBlue.setFontName(res.getString("XLSFontDTL"));
		fontDataBlue.setFontHeightInPoints((short)11);
		fontDataBlue.setColor(HSSFColor.BLUE.index);
		HSSFFont fontDataGreen = workBook.createFont();
		fontDataGreen.setFontName(res.getString("XLSFontDTL"));
		fontDataGreen.setFontHeightInPoints((short)11);
		fontDataGreen.setColor(HSSFColor.GREEN.index);
		HSSFFont fontDataOrange = workBook.createFont();
		fontDataOrange.setFontName(res.getString("XLSFontDTL"));
		fontDataOrange.setFontHeightInPoints((short)11);
		fontDataOrange.setColor(HSSFColor.ORANGE.index);
		//
		HSSFCellStyle styleHeaderLabel = workBook.createCellStyle();
		styleHeaderLabel.setBorderBottom(HSSFCellStyle.BORDER_THIN);
		styleHeaderLabel.setBorderLeft(HSSFCellStyle.BORDER_THIN);
		styleHeaderLabel.setBorderRight(HSSFCellStyle.BORDER_THIN);
		styleHeaderLabel.setBorderTop(HSSFCellStyle.BORDER_THIN);
		styleHeaderLabel.setFillForegroundColor(HSSFColor.GREY_25_PERCENT.index);
		styleHeaderLabel.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
		styleHeaderLabel.setAlignment(HSSFCellStyle.ALIGN_RIGHT);
		styleHeaderLabel.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
		styleHeaderLabel.setFont(fontHeader);
		//
		HSSFCellStyle styleDetailLabel = workBook.createCellStyle();
		styleDetailLabel.setBorderBottom(HSSFCellStyle.BORDER_THIN);
		styleDetailLabel.setBorderLeft(HSSFCellStyle.BORDER_THIN);
		styleDetailLabel.setBorderRight(HSSFCellStyle.BORDER_THIN);
		styleDetailLabel.setBorderTop(HSSFCellStyle.BORDER_THIN);
		styleDetailLabel.setFillForegroundColor(HSSFColor.GREY_25_PERCENT.index);
		styleDetailLabel.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
		styleDetailLabel.setAlignment(HSSFCellStyle.ALIGN_LEFT);
		styleDetailLabel.setFont(fontHeader);
		//
		HSSFCellStyle styleDetailNumberLabel = workBook.createCellStyle();
		styleDetailNumberLabel.setBorderBottom(HSSFCellStyle.BORDER_THIN);
		styleDetailNumberLabel.setBorderLeft(HSSFCellStyle.BORDER_THIN);
		styleDetailNumberLabel.setBorderRight(HSSFCellStyle.BORDER_THIN);
		styleDetailNumberLabel.setBorderTop(HSSFCellStyle.BORDER_THIN);
		styleDetailNumberLabel.setFillForegroundColor(HSSFColor.GREY_25_PERCENT.index);
		styleDetailNumberLabel.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
		styleDetailNumberLabel.setAlignment(HSSFCellStyle.ALIGN_LEFT);
		styleDetailNumberLabel.setFont(fontHeader);
		//
		HSSFCellStyle styleDataInteger = workBook.createCellStyle();
		styleDataInteger.setBorderBottom(HSSFCellStyle.BORDER_THIN);
		styleDataInteger.setBorderLeft(HSSFCellStyle.BORDER_THIN);
		styleDataInteger.setBorderRight(HSSFCellStyle.BORDER_THIN);
		styleDataInteger.setBorderTop(HSSFCellStyle.BORDER_THIN);
		styleDataInteger.setAlignment(HSSFCellStyle.ALIGN_RIGHT);
		styleDataInteger.setVerticalAlignment(HSSFCellStyle.VERTICAL_TOP);
		styleDataInteger.setFont(fontDataBlack);
		styleDataInteger.setDataFormat(HSSFDataFormat.getBuiltinFormat("#,##0"));
		//
		int currentRowNumber = -1;
		int mergeRowNumberFrom = -1;
		//
		try {
			xlsFile = session_.createTempFile(functionElement_.getAttribute("ID"), ".xls");
			xlsFileName = xlsFile.getPath();
			fileOutputStream = new FileOutputStream(xlsFileName);
			int columnIndex;
			//
			// Header Lines //
			for (int i = 0; i < headerFieldList.size() && i < 24; i++) {
				if (headerFieldList.get(i).isVisibleOnPanel()) {
					for (int j = 0; j < headerFieldList.get(i).getRows(); j++) {
						//
						currentRowNumber++;
						HSSFRow rowData = workSheet.createRow(currentRowNumber);
						//
						// Cells for header field label //
						HSSFCell cellHeader = rowData.createCell(0);
						cellHeader.setCellStyle(styleHeaderLabel);
						if (j==0) {
							mergeRowNumberFrom = currentRowNumber;
							cellHeader.setCellValue(new HSSFRichTextString(headerFieldList.get(i).getCaption()));
						}
						rowData.createCell(1).setCellStyle(styleHeaderLabel);
						//
						// Cells for header field data //
						if (headerFieldList.get(i).getColor().equals("black")) {
							font = fontDataBlack;
						}
						if (headerFieldList.get(i).getColor().equals("red")) {
							font = fontDataRed;
						}
						if (headerFieldList.get(i).getColor().equals("blue")) {
							font = fontDataBlue;
						}
						if (headerFieldList.get(i).getColor().equals("green")) {
							font = fontDataGreen;
						}
						if (headerFieldList.get(i).getColor().equals("orange")) {
							font = fontDataOrange;
						}
						setupCellAttributesForHeaderField(rowData, workBook, workSheet, headerFieldList.get(i), currentRowNumber, j, font);
					}
					workSheet.addMergedRegion(new CellRangeAddress(mergeRowNumberFrom, currentRowNumber, 0, 1));
					workSheet.addMergedRegion(new CellRangeAddress(mergeRowNumberFrom, currentRowNumber, 2, 6));
				}
			}
			//
			// Spacer //
			currentRowNumber++;
			workSheet.createRow(currentRowNumber);
			//
			// Detail Lines //
			currentRowNumber++;
			HSSFRow rowCaption = workSheet.createRow(currentRowNumber);
			for (int i = 0; i < tableModelMainArray[jTabbedPane.getSelectedIndex()].getColumnCount(); i++) {
				HSSFCell cell = rowCaption.createCell(i);
				if (i == 0) {
					cell.setCellStyle(styleDetailNumberLabel);
				} else {
					cell.setCellStyle(styleDetailLabel);
				}
				Rectangle rect = jTableMainArray[jTabbedPane.getSelectedIndex()].getCellRect(0, i, true);
				workSheet.setColumnWidth(i, rect.width * 40);
				cell.setCellValue(new HSSFRichTextString(tableModelMainArray[jTabbedPane.getSelectedIndex()].getColumnName(i)));
			}
			//
			for (int i = 0; i < tableModelMainArray[jTabbedPane.getSelectedIndex()].getRowCount(); i++) {
				currentRowNumber++;
				HSSFRow rowData = workSheet.createRow(currentRowNumber);
				//
				HSSFCell cell = rowData.createCell(0); //Column of Sequence Number
				cell.setCellType(HSSFCell.CELL_TYPE_NUMERIC);
				cell.setCellStyle(styleDataInteger);
				cell.setCellValue(i + 1);
				//
				columnIndex = 0;
				for (int j = 0; j < detailColumnListArray[jTabbedPane.getSelectedIndex()].size(); j++) {
					if (detailColumnListArray[jTabbedPane.getSelectedIndex()].get(j).isVisibleOnPanel()) {
						columnIndex++;
						cellObject = (XF300_DetailCell)tableModelMainArray[jTabbedPane.getSelectedIndex()].getValueAt(i,columnIndex);
						if (cellObject.getColor().equals(Color.black)) {
							font = fontDataBlack;
						}
						if (cellObject.getColor().equals(Color.red)) {
							font = fontDataRed;
						}
						if (cellObject.getColor().equals(Color.blue)) {
							font = fontDataBlue;
						}
						if (cellObject.getColor().equals(Color.green)) {
							font = fontDataGreen;
						}
						if (cellObject.getColor().equals(Color.orange)) {
							font = fontDataOrange;
						}
						setupCellAttributesForDetailColumn(rowData.createCell(columnIndex), workBook, detailColumnListArray[jTabbedPane.getSelectedIndex()].get(j).getBasicType(), cellObject, font, detailColumnListArray[jTabbedPane.getSelectedIndex()].get(j).getDecimalSize());
					}
				}
			}
			//
			workBook.write(fileOutputStream);
			fileOutputStream.close();
			//
			messageList.add(res.getString("XLSComment1"));
			//
		} catch (Exception e1) {
			messageList.add(res.getString("XLSErrorMessage"));
			e1.printStackTrace(exceptionStream);
			try {
				fileOutputStream.close();
			} catch (Exception e2) {
				e2.printStackTrace(exceptionStream);
			}
		}
		return xlsFile.toURI();
	}

	private void setupCellAttributesForHeaderField(HSSFRow rowData, HSSFWorkbook workBook, HSSFSheet workSheet, XF300_HeaderField object, int currentRowNumber, int rowIndexInCell, HSSFFont font) {
		String wrk;
		//
		HSSFCellStyle style = workBook.createCellStyle();
		style.setBorderBottom(HSSFCellStyle.BORDER_THIN);
		style.setBorderLeft(HSSFCellStyle.BORDER_THIN);
		style.setBorderRight(HSSFCellStyle.BORDER_THIN);
		style.setBorderTop(HSSFCellStyle.BORDER_THIN);
		style.setFont(font);
		style.setAlignment(HSSFCellStyle.ALIGN_LEFT);
		style.setVerticalAlignment(HSSFCellStyle.VERTICAL_TOP);
		style.setWrapText(true);
		style.setDataFormat(HSSFDataFormat.getBuiltinFormat("text"));
		//
		HSSFCell cellValue = rowData.createCell(2);
		//
		String basicType = object.getBasicType();
		if (basicType.equals("INTEGER")) {
			wrk = XFUtility.getStringNumber(object.getExternalValue().toString());
			if (wrk.equals("")) {
				cellValue.setCellType(HSSFCell.CELL_TYPE_STRING);
				cellValue.setCellStyle(style);
				cellValue.setCellValue(new HSSFRichTextString(object.getExternalValue().toString()));
				//if (rowIndexInCell==0) {
				//	cellValue.setCellValue(new HSSFRichTextString(""));
				//}
			} else {
				cellValue.setCellType(HSSFCell.CELL_TYPE_NUMERIC);
				style.setAlignment(HSSFCellStyle.ALIGN_RIGHT);
				style.setVerticalAlignment(HSSFCellStyle.VERTICAL_TOP);
				style.setDataFormat(HSSFDataFormat.getBuiltinFormat("#,##0"));
				cellValue.setCellStyle(style);
				if (rowIndexInCell==0) {
					cellValue.setCellValue(Double.parseDouble(wrk));
				}
			}
		} else {
			if (basicType.equals("FLOAT")) {
				wrk = XFUtility.getStringNumber(object.getExternalValue().toString());
				if (wrk.equals("")) {
					cellValue.setCellType(HSSFCell.CELL_TYPE_STRING);
					cellValue.setCellStyle(style);
					if (rowIndexInCell==0) {
						cellValue.setCellValue(new HSSFRichTextString(""));
					}
				} else {
					cellValue.setCellType(HSSFCell.CELL_TYPE_NUMERIC);
					style.setAlignment(HSSFCellStyle.ALIGN_RIGHT);
					style.setVerticalAlignment(HSSFCellStyle.VERTICAL_TOP);
					style.setDataFormat(XFUtility.getFloatFormat(workBook, object.getDecimalSize()));
					cellValue.setCellStyle(style);
					if (rowIndexInCell==0) {
						cellValue.setCellValue(Double.parseDouble(wrk));
					}
				}
			} else {
				cellValue.setCellType(HSSFCell.CELL_TYPE_STRING);
				cellValue.setCellStyle(style);
				if (rowIndexInCell==0) {
					if (basicType.equals("STRING")) {
						if (object.isImage()) {
							style.setAlignment(HSSFCellStyle.ALIGN_RIGHT);
							style.setVerticalAlignment(HSSFCellStyle.VERTICAL_BOTTOM);
							style.setDataFormat(HSSFDataFormat.getBuiltinFormat("text"));
							HSSFPatriarch patriarch = workSheet.createDrawingPatriarch();
							HSSFClientAnchor anchor = null;
							cellValue.setCellStyle(style);
							cellValue.setCellValue(new HSSFRichTextString((String)object.getInternalValue()));
							int imageType = -1;
							String fileName = (String)object.getExternalValue();
							File imageFile = new File(fileName);
							if (imageFile.exists()) {
								boolean isValidFileType = false;
								if (fileName.contains(".png") || fileName.contains(".PNG")) {
									imageType = HSSFWorkbook.PICTURE_TYPE_PNG;
									isValidFileType = true;
								}
								if (fileName.contains(".jpg") || fileName.contains(".JPG") || fileName.contains(".jpeg") || fileName.contains(".JPEG")) {
									imageType = HSSFWorkbook.PICTURE_TYPE_JPEG;
									isValidFileType = true;
								}
								if (isValidFileType) {
									int pictureIndex;
									FileInputStream fis = null;
									ByteArrayOutputStream bos = null;
									try {
										// read in the image file //
										fis = new FileInputStream(imageFile);
										bos = new ByteArrayOutputStream( );
										int c;
										// copy the image bytes into the ByteArrayOutputStream //
										while ((c = fis.read()) != -1) {
											bos.write(c);
										}
										// add the image bytes to the workbook //
										pictureIndex = workBook.addPicture( bos.toByteArray(), imageType);
										anchor = new HSSFClientAnchor(0,0,0,0,(short)2,currentRowNumber,(short)6,currentRowNumber + object.getRows());
										anchor.setAnchorType(0);
										anchor.setDx1(30);
										anchor.setDy1(30);
										anchor.setDx2(-30);
										anchor.setDy2(-250);
										patriarch.createPicture(anchor, pictureIndex);
										//
									} catch(Exception e) {
										e.printStackTrace(exceptionStream);
									} finally {
										try {
											if (fis != null) {
												fis.close();
											}
											if (bos != null) {
												bos.close();
											}
										} catch (IOException e) {
											e.printStackTrace(exceptionStream);
										}
									}
								}
							}
						} else {
							cellValue.setCellStyle(style);
							cellValue.setCellValue(new HSSFRichTextString((String)object.getExternalValue()));
						}
					} else {
						if (basicType.equals("DATE")) {
							java.util.Date utilDate = XFUtility.convertDateFromStringToUtil((String)object.getInternalValue());
							String text = XFUtility.getUserExpressionOfUtilDate(utilDate, session_.getDateFormat(), false);
							cellValue.setCellValue(new HSSFRichTextString(text));
						}
						if (object.getBasicType().equals("DATETIME") || object.getBasicType().equals("TIME")) {
							cellValue.setCellValue(new HSSFRichTextString(object.getInternalValue().toString()));
						}
					}
				}
			}
		}
		rowData.createCell(3).setCellStyle(style);
		rowData.createCell(4).setCellStyle(style);
		rowData.createCell(5).setCellStyle(style);
		rowData.createCell(6).setCellStyle(style);
	}

	private void setupCellAttributesForDetailColumn(HSSFCell cell, HSSFWorkbook workBook, String basicType, XF300_DetailCell object, HSSFFont font, int decimalSize) {
		String wrk;
		//
		HSSFCellStyle style = workBook.createCellStyle();
		style.setBorderBottom(HSSFCellStyle.BORDER_THIN);
		style.setBorderLeft(HSSFCellStyle.BORDER_THIN);
		style.setBorderRight(HSSFCellStyle.BORDER_THIN);
		style.setBorderTop(HSSFCellStyle.BORDER_THIN);
		style.setFont(font);
		style.setAlignment(HSSFCellStyle.ALIGN_LEFT);
		style.setVerticalAlignment(HSSFCellStyle.VERTICAL_TOP);
		style.setWrapText(true);
		style.setDataFormat(HSSFDataFormat.getBuiltinFormat("text"));
		//
		Object value = object.getValue();
		if (basicType.equals("INTEGER")) {
			if (value == null) {
				wrk = "";
			} else {
				wrk = XFUtility.getStringNumber(value.toString());
			}
			if (wrk.equals("")) {
				cell.setCellType(HSSFCell.CELL_TYPE_STRING);
				cell.setCellStyle(style);
				//cell.setCellValue(new HSSFRichTextString(wrk));
				cell.setCellValue(new HSSFRichTextString(value.toString()));
			} else {
				cell.setCellType(HSSFCell.CELL_TYPE_NUMERIC);
				style.setAlignment(HSSFCellStyle.ALIGN_RIGHT);
				style.setVerticalAlignment(HSSFCellStyle.VERTICAL_TOP);
				style.setDataFormat(HSSFDataFormat.getBuiltinFormat("#,##0"));
				cell.setCellStyle(style);
				cell.setCellValue(Double.parseDouble(wrk));
			}
		} else {
			if (basicType.equals("FLOAT")) {
				if (value == null) {
					wrk = "";
				} else {
					wrk = XFUtility.getStringNumber(value.toString());
				}
				if (wrk.equals("")) {
					cell.setCellType(HSSFCell.CELL_TYPE_STRING);
					cell.setCellStyle(style);
					cell.setCellValue(new HSSFRichTextString(wrk));
				} else {
					cell.setCellType(HSSFCell.CELL_TYPE_NUMERIC);
					style.setAlignment(HSSFCellStyle.ALIGN_RIGHT);
					style.setVerticalAlignment(HSSFCellStyle.VERTICAL_TOP);
					//style.setDataFormat(HSSFDataFormat.getBuiltinFormat("#,##0.000"));
					style.setDataFormat(XFUtility.getFloatFormat(workBook, decimalSize));
					cell.setCellStyle(style);
					cell.setCellValue(Double.parseDouble(wrk));
				}
			} else {
				cell.setCellType(HSSFCell.CELL_TYPE_STRING);
				cell.setCellStyle(style);
				if (value == null) {
					wrk = "";
				} else {
					wrk = value.toString();
				}
				cell.setCellValue(new HSSFRichTextString(wrk));
			}
		}
	}
	
	void jTableMain_focusGained(FocusEvent e) {
		if (jTableMainArray[jTabbedPane.getSelectedIndex()].getRowCount() > 0) {
			jTableMainArray[jTabbedPane.getSelectedIndex()].setSelectionBackground(selectionColorWithFocus);
			jTableMainArray[jTabbedPane.getSelectedIndex()].setSelectionForeground(Color.white);
		} else {
			jTabbedPane.requestFocus();
		}
	}

	void jTableMain_focusLost(FocusEvent e) {
		jTableMainArray[jTabbedPane.getSelectedIndex()].setSelectionBackground(selectionColorWithoutFocus);
		jTableMainArray[jTabbedPane.getSelectedIndex()].setSelectionForeground(Color.black);
	}

	void jScrollPaneTable_mousePressed(MouseEvent e) {
		jTableMainArray[jTabbedPane.getSelectedIndex()].requestFocus();
	}

	void jTabbedPane_keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_DOWN) {
			jTableMainArray[jTabbedPane.getSelectedIndex()].requestFocus();
			if (jTableMainArray[jTabbedPane.getSelectedIndex()].getRowCount() > 0) {
				jTableMainArray[jTabbedPane.getSelectedIndex()].setRowSelectionInterval(0, 0);
			}
		}
		//
		// Steps to hack F6 and F8 of jSplitPane //
		if (e.getKeyCode() == KeyEvent.VK_F6) {
			if (!actionF6.equals("")) {
				int workIndex = Integer.parseInt(actionF6);
				jButtonArray[workIndex].doClick();
			}
		}
		if (e.getKeyCode() == KeyEvent.VK_F8) {
			if (!actionF8.equals("")) {
				int workIndex = Integer.parseInt(actionF8);
				jButtonArray[workIndex].doClick();
			}
		}
	}

	void jTabbedPane_stateChanged(ChangeEvent e) {
		if (tablesReadyToUse) {
			//
			int index = jTabbedPane.getSelectedIndex();
			//
			jPanelTopCenter.removeAll();
			jPanelTop.setVisible(false);
			jPanelTopEast.setVisible(false);
			if (filterListArray[index].size() > 0) {
				jPanelTop.setVisible(true);
				jPanelTopCenter.add(jPanelFilter[index], BorderLayout.CENTER);
				//filterListArray[index].get(0).requestFocus();
				jPanelTopEast.setVisible(anyFilterIsEditable[index]);
				//
				if (anyFilterIsEditable[index]) {
					jPanelTopEast.setVisible(true);
				}
			}
			//
			jScrollPaneTable.getViewport().add(jTableMainArray[index], null);
			//
			messageList.clear();
			if (jTableMainArray[index].getRowCount() > 0) {
				if (initialMsgArray[index].equals("")) {
					messageList.add(res.getString("FunctionMessage3"));
				} else {
					messageList.add(initialMsgArray[index]);
				}
			} else {
				if (initialMsgArray[index].equals("")) {
					if (filterListArray[index].size() > 0 && anyFilterIsEditable[index]) {
						messageList.add(res.getString("FunctionMessage1"));
					} else {
						messageList.add(res.getString("FunctionMessage31"));
					}
				} else {
					messageList.add(initialMsgArray[index]);
				}
			}
			setMessagesOnPanel();
			//
			setupFunctionKeysAndButtonsForTabIndex(index);
		}
	}
	
	void dialog_keyPressed(KeyEvent e) {
	}

	void jTableMain_mouseClicked(MouseEvent e) {
		if (e.getClickCount() >= 2 && tableModelMainArray[jTabbedPane.getSelectedIndex()].getRowCount() > 0) {
			processRow(false);
		}
	}

	void jTableMain_keyPressed(KeyEvent e) {
		//
		if (e.getKeyCode() == KeyEvent.VK_UP && jTableMainArray[jTabbedPane.getSelectedIndex()].getSelectedRow() == 0) {
			jTabbedPane.requestFocus();
		}
		//
		if (e.getKeyCode() == KeyEvent.VK_ENTER && tableModelMainArray[jTabbedPane.getSelectedIndex()].getRowCount() > 0) {
			processRow(true);
		}
		//
		// Steps to hack F6 and F8 of component //
		if (e.getKeyCode() == KeyEvent.VK_F6) {
			if (!actionF6.equals("")) {
				int workIndex = Integer.parseInt(actionF6);
				jButtonArray[workIndex].doClick();
			}
		}
		if (e.getKeyCode() == KeyEvent.VK_F8 && !actionF8.equals("")) {
			if (!actionF8.equals("")) {
				int workIndex = Integer.parseInt(actionF8);
				jButtonArray[workIndex].doClick();
			}
		}
	}
	
	void processRow(boolean enterKeyPressed) {
		messageList.clear();
		//
		if (detailFunctionElementArray[jTabbedPane.getSelectedIndex()] == null) {
			JOptionPane.showMessageDialog(this, res.getString("FunctionError13") + detailFunctionIDArray[jTabbedPane.getSelectedIndex()] + res.getString("FunctionError14"));
			messageList.add(res.getString("FunctionError15"));
		} else {
			if (jTableMainArray[jTabbedPane.getSelectedIndex()].getSelectedRow() > -1) {
				try {
					setCursor(new Cursor(Cursor.WAIT_CURSOR));
					//
					int rowNumber = jTableMainArray[jTabbedPane.getSelectedIndex()].convertRowIndexToModel(jTableMainArray[jTabbedPane.getSelectedIndex()].getSelectedRow());
					if (rowNumber > -1) {
						XF300_DetailRowNumber detailRowNumber = (XF300_DetailRowNumber)tableModelMainArray[jTabbedPane.getSelectedIndex()].getValueAt(rowNumber,0);
						HashMap<String, Object> keyMap = detailRowNumber.getKeyMap();
						keyMap.putAll(parmMap_);
						HashMap<String, Object> returnMap = session_.getFunction().execute(detailFunctionElementArray[jTabbedPane.getSelectedIndex()], keyMap);
						if (returnMap.get("RETURN_MESSAGE") == null) {
							messageList.add(XFUtility.getMessageOfReturnCode(returnMap.get("RETURN_CODE").toString()));
						} else {
							messageList.add(returnMap.get("RETURN_MESSAGE").toString());
						}
						//
						if (returnMap.get("RETURN_CODE").equals("20") || returnMap.get("RETURN_CODE").equals("30")) {
							selectDetailRecordsAndSetupTableRows(jTabbedPane.getSelectedIndex(), false);
						}
					}
				} finally {
					setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				}
			}
		}
		//
		if (enterKeyPressed && jTableMainArray[jTabbedPane.getSelectedIndex()].getSelectedRow() >= 0) {
			if (jTableMainArray[jTabbedPane.getSelectedIndex()].getSelectedRow() == 0) {
				jTableMainArray[jTabbedPane.getSelectedIndex()].setRowSelectionInterval(jTableMainArray[jTabbedPane.getSelectedIndex()].getRowCount() - 1, jTableMainArray[jTabbedPane.getSelectedIndex()].getRowCount() - 1);
			} else {
				jTableMainArray[jTabbedPane.getSelectedIndex()].setRowSelectionInterval(jTableMainArray[jTabbedPane.getSelectedIndex()].getSelectedRow() - 1, jTableMainArray[jTabbedPane.getSelectedIndex()].getSelectedRow() - 1);
			}
		}
		//
		setMessagesOnPanel();
	}

	void setMessagesOnPanel() {
		//
		jTextAreaMessages.setText("");
		//
		StringBuffer sb = new StringBuffer("");
		for (int i = 0; i < messageList.size(); i++) {
			if (i > 0) {
				sb.append("\n");
			}
			if (messageList.get(i).equals("")) {
				return;
			} else {
				sb.append(messageList.get(i));
			}
		}
		//
		jTextAreaMessages.setText(sb.toString());
	}

	public String getFunctionID() {
		return functionElement_.getAttribute("ID");
	}

	public String getScriptNameRunning() {
		return scriptNameRunning;
	}

	public StringBuffer getProcessLog() {
		return processLog;
	}

	public org.w3c.dom.Element getFunctionElement() {
		return functionElement_;
	}

	public String getFunctionInfo() {
		return jLabelFunctionID.getText();
	}

	public Session getSession() {
		return session_;
	}

	public PrintStream getExceptionStream() {
		return exceptionStream;
	}

	public Bindings getHeaderScriptBindings() {
		return 	headerScriptBindings;
	}

	public void evalHeaderTableScript(String scriptName, String scriptText) throws ScriptException {
		if (!scriptText.equals("")) {
			scriptNameRunning = scriptName;
			scriptEngine.eval(scriptText);
		}
	}

	public void evalDetailTableScript(String scriptName, String scriptText, int index) throws ScriptException {
		if (!scriptText.equals("")) {
			scriptNameRunning = scriptName;
			scriptEngine.eval(scriptText, detailScriptBindingsArray[index]);
		}
	}

	public HashMap<String, Object> getParmMap() {
		return parmMap_;
	}

	public HashMap<String, Object> getReturnMap() {
		return returnMap_;
	}
	
	public XF300_HeaderTable getHeaderTable() {
		return headerTable_;
	}
	
	public ArrayList<XF300_HeaderReferTable> getHeaderReferTableList() {
		return headerReferTableList;
	}

	public ArrayList<XF300_HeaderField> getHeaderFieldList() {
		return headerFieldList;
	}

	public XF300_HeaderField getHeaderFieldObjectByID(String tableID, String tableAlias, String fieldID) {
		XF300_HeaderField headerField = null;
		for (int i = 0; i < headerFieldList.size(); i++) {
			if (tableID.equals("")) {
				if (headerFieldList.get(i).getTableAlias().equals(tableAlias) && headerFieldList.get(i).getFieldID().equals(fieldID)) {
					headerField = headerFieldList.get(i);
					break;
				}
			}
			if (tableAlias.equals("")) {
				if (headerFieldList.get(i).getTableID().equals(tableID) && headerFieldList.get(i).getFieldID().equals(fieldID)) {
					headerField = headerFieldList.get(i);
					break;
				}
			}
			if (!tableID.equals("") && !tableAlias.equals("")) {
				if (headerFieldList.get(i).getTableID().equals(tableID) && headerFieldList.get(i).getTableAlias().equals(tableAlias) && headerFieldList.get(i).getFieldID().equals(fieldID)) {
					headerField = headerFieldList.get(i);
					break;
				}
			}
		}
		return headerField;
	}

	public boolean containsHeaderField(String tableID, String tableAlias, String fieldID) {
		boolean result = false;
		for (int i = 0; i < headerFieldList.size(); i++) {
			if (tableID.equals("")) {
				if (headerFieldList.get(i).getTableAlias().equals(tableAlias)) {
					result = true;
				}
			}
			if (tableAlias.equals("")) {
				if (headerFieldList.get(i).getTableID().equals(tableID) && headerFieldList.get(i).getFieldID().equals(fieldID)) {
					result = true;
				}
			}
			if (!tableID.equals("") && !tableAlias.equals("")) {
				if (headerFieldList.get(i).getTableID().equals(tableID) && headerFieldList.get(i).getTableAlias().equals(tableAlias) && headerFieldList.get(i).getFieldID().equals(fieldID)) {
					result = true;
				}
			}
		}
		return result;
	}

	public boolean containsDetailField(int tabIndex, String tableID, String tableAlias, String fieldID) {
		boolean result = false;
		for (int i = 0; i < detailColumnListArray[tabIndex].size(); i++) {
			if (tableID.equals("")) {
				if (detailColumnListArray[tabIndex].get(i).getTableAlias().equals(tableAlias)) {
					result = true;
				}
			}
			if (tableAlias.equals("")) {
				if (detailColumnListArray[tabIndex].get(i).getTableID().equals(tableID) && detailColumnListArray[tabIndex].get(i).getFieldID().equals(fieldID)) {
					result = true;
				}
			}
			if (!tableID.equals("") && !tableAlias.equals("")) {
				if (detailColumnListArray[tabIndex].get(i).getTableID().equals(tableID) && detailColumnListArray[tabIndex].get(i).getTableAlias().equals(tableAlias) && detailColumnListArray[tabIndex].get(i).getFieldID().equals(fieldID)) {
					result = true;
				}
			}
		}
		return result;
	}

	public XF300_DetailTable getDetailTable(int tabIndex) {
		return detailTableArray[tabIndex];
	}

	public ArrayList<XF300_DetailColumn> getDetailColumnList(int tabIndex) {
		return detailColumnListArray[tabIndex];
	}

	public ArrayList<XF300_DetailReferTable> getDetailReferTableList(int tabIndex) {
		return detailReferTableListArray[tabIndex];
	}

	public Bindings getDetailScriptBindings(int tabIndex) {
		return detailScriptBindingsArray[tabIndex];
	}

	public XF300_Filter getFilterObjectByName(int index, String dataSourceName) {
		XF300_Filter filter = null;
		for (int i = 0; i < filterListArray[index].size(); i++) {
			if (filterListArray[index].get(i).getDataSourceName().equals(dataSourceName)) {
				filter = filterListArray[index].get(i);
				break;
			}
		}
		return filter;
	}
	
	public ArrayList<XF300_Filter> getFilterList(int tabIndex) {
		return filterListArray[tabIndex];
	}
	
	public double getFilterWidth(int tabIndex) {
		return filterWidth[tabIndex];
	}

	public XF300_DetailColumn getDetailColumnObjectByID(int tabIndex, String tableID, String tableAlias, String fieldID) {
		XF300_DetailColumn detailColumnField = null;
		for (int i = 0; i < detailColumnListArray[tabIndex].size(); i++) {
			if (tableID.equals("")) {
				if (detailColumnListArray[tabIndex].get(i).getTableAlias().equals(tableAlias) && detailColumnListArray[tabIndex].get(i).getFieldID().equals(fieldID)) {
					detailColumnField = detailColumnListArray[tabIndex].get(i);
					break;
				}
			}
			if (tableAlias.equals("")) {
				if (detailColumnListArray[tabIndex].get(i).getTableID().equals(tableID) && detailColumnListArray[tabIndex].get(i).getFieldID().equals(fieldID)) {
					detailColumnField = detailColumnListArray[tabIndex].get(i);
					break;
				}
			}
			if (!tableID.equals("") && !tableAlias.equals("")) {
				if (detailColumnListArray[tabIndex].get(i).getTableID().equals(tableID) && detailColumnListArray[tabIndex].get(i).getTableAlias().equals(tableAlias) && detailColumnListArray[tabIndex].get(i).getFieldID().equals(fieldID)) {
					detailColumnField = detailColumnListArray[tabIndex].get(i);
					break;
				}
			}
		}
		return detailColumnField;
	}

	public String getTableIDOfTableAlias(String tableAlias, int index) {
		String tableID = tableAlias;
		org.w3c.dom.Element workElement;
		//
		for (int j = 0; j < headerReferElementList.getLength(); j++) {
			workElement = (org.w3c.dom.Element)headerReferElementList.item(j);
			if (workElement.getAttribute("TableAlias").equals(tableAlias)) {
				tableID = workElement.getAttribute("ToTable");
				break;
			}
		}
		//
		if (index > -1) {
			for (int j = 0; j < detailReferElementList[index].getLength(); j++) {
				workElement = (org.w3c.dom.Element)detailReferElementList[index].item(j);
				if (workElement.getAttribute("TableAlias").equals(tableAlias)) {
					tableID = workElement.getAttribute("ToTable");
					break;
				}
			}
		}
		//
		return tableID;
	}
	
	public int getSelectedTabIndex() {
		return jTabbedPane.getSelectedIndex();
	}

	boolean isTheRowToBeSelected(int index) {
		boolean toBeSelected = true;
		//
		for (int i = 0; i < filterListArray[index].size(); i++) {
			toBeSelected = filterListArray[index].get(i).isValidated();
			if (!toBeSelected) {
				break;
			}
		}
		//
		return toBeSelected;
	}
}

class XF300_HeaderField extends JPanel implements XFScriptableField {
	private static final long serialVersionUID = 1L;
	private static ResourceBundle res = ResourceBundle.getBundle("xeadDriver.Res");
	org.w3c.dom.Element functionFieldElement_ = null;
	private org.w3c.dom.Element tableElement = null;
	private String tableID_ = "";
	private String tableAlias_ = "";
	private String fieldID_ = "";
	private String dataType = "";
	private String dataTypeOptions = "";
	private ArrayList<String> dataTypeOptionList;
	private String fieldCaption = "";
	private int dataSize = 5;
	private int decimalSize = 0;
	private String fieldOptions = "";
	private ArrayList<String> fieldOptionList;
	private int fieldRows = 1;
	private JPanel jPanelField = new JPanel();
	private JLabel jLabelField = new JLabel();
	private JLabel jLabelFieldComment = new JLabel();
	private XFTextField xFTextField = null;
	private XFDateField xFDateField = null;
	private XFTextArea xFTextArea = null;
	private XFYMonthBox xFYMonthBox = null;
	private XFMSeqBox xFMSeqBox = null;
	private XFFYearBox xFFYearBox = null;
	private XFUrlField xFUrlField = null;
	private XFImageField xFImageField = null;
	private XFEditableField component = null;
	private ArrayList<String> keyValueList = new ArrayList<String>();
	private String keyValue = "";
	private ArrayList<String> textValueList = new ArrayList<String>();
	private boolean isKey = false;
	private boolean isFieldOnPrimaryTable = false;
	private boolean isVisibleOnPanel = true;
	private boolean isHorizontal = false;
	private boolean isVirtualField = false;
	private boolean isRangeKeyFieldValid = false;
	private boolean isRangeKeyFieldExpire = false;
	private boolean isImage = false;
	private int positionMargin = 0;
	private Color foreground = Color.black;
	private XF300 dialog_;

	public XF300_HeaderField(org.w3c.dom.Element functionFieldElement, XF300 dialog){
		super();
		//
		String wrkStr;
		dialog_ = dialog;
		functionFieldElement_ = functionFieldElement;
		fieldOptions = functionFieldElement_.getAttribute("FieldOptions");
		fieldOptionList = XFUtility.getOptionList(fieldOptions);
		//
		StringTokenizer workTokenizer = new StringTokenizer(functionFieldElement_.getAttribute("DataSource"), "." );
		tableAlias_ = workTokenizer.nextToken();
		tableID_ = dialog.getTableIDOfTableAlias(tableAlias_, -1);
		fieldID_ =workTokenizer.nextToken();
		//
		if (tableID_.equals(dialog_.getHeaderTable().getTableID()) && tableAlias_.equals(tableID_)) {
			isFieldOnPrimaryTable = true;
			//
			ArrayList<String> keyFieldList = dialog_.getHeaderTable().getKeyFieldIDList();
			for (int i = 0; i < keyFieldList.size(); i++) {
				if (keyFieldList.get(i).equals(fieldID_)) {
					isKey = true;
					break;
				}
			}
		}
		//
		if (fieldOptionList.contains("HORIZONTAL")) {
			isHorizontal = true;
		}
		wrkStr = XFUtility.getOptionValueWithKeyword(fieldOptions, "HORIZONTAL");
		if (!wrkStr.equals("")) {
			isHorizontal = true;
			positionMargin = Integer.parseInt(wrkStr);
		}
		wrkStr = XFUtility.getOptionValueWithKeyword(fieldOptions, "VERTICAL");
		if (!wrkStr.equals("")) {
			positionMargin = Integer.parseInt(wrkStr);
		}
		//
		org.w3c.dom.Element workElement = dialog.getSession().getFieldElement(tableID_, fieldID_);
		if (workElement == null) {
			JOptionPane.showMessageDialog(this, tableID_ + "." + fieldID_ + res.getString("FunctionError11"));
		}
		dataType = workElement.getAttribute("Type");
		dataTypeOptions = workElement.getAttribute("TypeOptions");
		dataTypeOptionList = XFUtility.getOptionList(dataTypeOptions);
		if (workElement.getAttribute("Name").equals("")) {
			fieldCaption = workElement.getAttribute("ID");
		} else {
			fieldCaption = workElement.getAttribute("Name");
		}
		dataSize = Integer.parseInt(workElement.getAttribute("Size"));
		if (dataSize > 50) {
			dataSize = 50;
		}
		if (!workElement.getAttribute("Decimal").equals("")) {
			decimalSize = Integer.parseInt(workElement.getAttribute("Decimal"));
		}
		//
		tableElement = (org.w3c.dom.Element)workElement.getParentNode();
		if (!tableElement.getAttribute("RangeKey").equals("")) {
			workTokenizer = new StringTokenizer(tableElement.getAttribute("RangeKey"), ";" );
			if (workTokenizer.nextToken().equals(fieldID_)) {
				isRangeKeyFieldValid = true;
			}
			if (workTokenizer.nextToken().equals(fieldID_)) {
				isRangeKeyFieldExpire = true;
			}
		}
		//
		wrkStr = XFUtility.getOptionValueWithKeyword(fieldOptions, "CAPTION");
		if (!wrkStr.equals("")) {
			fieldCaption = wrkStr;
		}
		jLabelField.setText(fieldCaption);
		jLabelField.setPreferredSize(new Dimension(120, 24));
		jLabelField.setHorizontalAlignment(SwingConstants.RIGHT);
		jLabelField.setVerticalAlignment(SwingConstants.TOP);
		FontMetrics metrics = jLabelField.getFontMetrics(new java.awt.Font("Dialog", 0, 14));
		if (metrics.stringWidth(fieldCaption) > 120) {
			jLabelField.setFont(new java.awt.Font("Dialog", 0, 12));
			metrics = jLabelField.getFontMetrics(new java.awt.Font("Dialog", 0, 12));
			if (metrics.stringWidth(fieldCaption) > 120) {
				jLabelField.setFont(new java.awt.Font("Dialog", 0, 10));
			} else {
				jLabelField.setFont(new java.awt.Font("Dialog", 0, 12));
			}
		} else {
			jLabelField.setFont(new java.awt.Font("Dialog", 0, 14));
		}
		//
		if (dataType.equals("VARCHAR") || dataType.equals("LONG VARCHAR")) {
			xFTextArea = new XFTextArea(dataSize, "", fieldOptions);
			xFTextArea.setLocation(5, 0);
			xFTextArea.setEditable(false);
			component = xFTextArea;
		} else {
			if (dataTypeOptionList.contains("URL")) {
				xFUrlField = new XFUrlField(dataSize);
				xFUrlField.setLocation(5, 0);
				xFUrlField.setEditable(false);
				component = xFUrlField;
			} else {
				if (dataTypeOptionList.contains("IMAGE")) {
					xFImageField = new XFImageField(fieldOptions, dialog_.getSession().getImageFileFolder());
					xFImageField.setLocation(5, 0);
					component = xFImageField;
					isImage = true;
				} else {
					if (dataType.equals("DATE")) {
						xFDateField = new XFDateField(dialog_.getSession());
						xFDateField.setLocation(5, 0);
						xFDateField.setEditable(false);
						component = xFDateField;
					} else {
						xFTextField = new XFTextField(this.getBasicType(), dataSize, decimalSize, dataTypeOptions, fieldOptions);
						xFTextField.setLocation(5, 0);
						xFTextField.setEditable(false);
						component = xFTextField;
						if (dataTypeOptionList.contains("YMONTH")) {
							xFYMonthBox = new XFYMonthBox(dialog_.getSession());
							xFYMonthBox.setLocation(5, 0);
							xFYMonthBox.setEditable(false);
							component = xFYMonthBox;
						} else {
							if (dataTypeOptionList.contains("MSEQ")) {
								xFMSeqBox = new XFMSeqBox(dialog_.getSession());
								xFMSeqBox.setLocation(5, 0);
								xFMSeqBox.setEditable(false);
								component = xFMSeqBox;
							} else {
								if (dataTypeOptionList.contains("FYEAR")) {
									xFFYearBox = new XFFYearBox(dialog_.getSession());
									xFFYearBox.setLocation(5, 0);
									xFFYearBox.setEditable(false);
									component = xFFYearBox;
								} else {
									xFTextField = new XFTextField(this.getBasicType(), dataSize, decimalSize, dataTypeOptions, fieldOptions);
									xFTextField.setLocation(5, 0);
									xFTextField.setEditable(false);
									//
									wrkStr = XFUtility.getOptionValueWithKeyword(dataTypeOptions, "KUBUN");
									if (!wrkStr.equals("")) {
										//
										String wrk;
										int fieldWidth = 50;
										JLabel jLabel = new JLabel();
										metrics = jLabel.getFontMetrics(new java.awt.Font("Dialog", 0, 14));
										try {
											Statement statement = dialog_.getSession().getConnection().createStatement();
											ResultSet result = statement.executeQuery("select KBUSERKUBUN,TXUSERKUBUN  from " + dialog_.getSession().getTableNameOfUserVariants() + " where IDUSERKUBUN = '" + wrkStr + "' order by SQLIST");
											while (result.next()) {
												keyValueList.add(result.getString("KBUSERKUBUN").trim());
												wrk = result.getString("TXUSERKUBUN").trim();
												textValueList.add(wrk);
												if (metrics.stringWidth(wrk) > fieldWidth) {
													fieldWidth = metrics.stringWidth(wrk);
												}
											}
											xFTextField.setSize(fieldWidth + 10, xFTextField.getHeight());
											result.close();
										} catch (SQLException e) {
											e.printStackTrace(dialog_.getExceptionStream());
											dialog_.setErrorAndCloseFunction();
										}
									}
									//
									component = xFTextField;
								}
							}
						}
					}
				}
			}
		}
		fieldRows = component.getRows();
		jPanelField.setLayout(null);
		jPanelField.setPreferredSize(new Dimension(component.getWidth() + 5, component.getHeight()));
		jPanelField.add((JComponent)component);
		//
		this.setOpaque(false);
		this.setLayout(new BorderLayout());
		if (fieldOptionList.contains("NO_CAPTION")) {
			this.setPreferredSize(new Dimension(component.getWidth() + 5, component.getHeight()));
		} else {
			this.setPreferredSize(new Dimension(component.getWidth() + 130, component.getHeight()));
			this.add(jLabelField, BorderLayout.WEST);
		}
		this.add(jPanelField, BorderLayout.CENTER);
		//
		wrkStr = XFUtility.getOptionValueWithKeyword(fieldOptions, "COMMENT");
		if (!wrkStr.equals("")) {
			jLabelFieldComment.setText(wrkStr);
			jLabelFieldComment.setForeground(Color.gray);
			jLabelFieldComment.setFont(new java.awt.Font("Dialog", 0, 12));
			jLabelFieldComment.setVerticalAlignment(SwingConstants.TOP);
			metrics = jLabelFieldComment.getFontMetrics(new java.awt.Font("Dialog", 0, 12));
			this.setPreferredSize(new Dimension(this.getPreferredSize().width + metrics.stringWidth(wrkStr), this.getPreferredSize().height));
			this.add(jLabelFieldComment, BorderLayout.EAST);
		}
		//
		if (dataTypeOptionList.contains("VIRTUAL")) {
			isVirtualField = true;
		}
		//
		dialog_.getHeaderScriptBindings().put(this.getFieldIDInScript(), (XFScriptableField)this);
	}

	public XF300_HeaderField(String tableID, String tableAlias, String fieldID, XF300 dialog){
		super();
		//
		String wrkStr;
		dialog_ = dialog;
		functionFieldElement_ = null;
		fieldOptions = "";
		fieldOptionList = new ArrayList<String>();
		//
		isVisibleOnPanel = false;
		//
		tableID_ = tableID;
		fieldID_ = fieldID;
		dialog_ = dialog;
		if (tableAlias.equals("")) {
			tableAlias_ = tableID;
		} else {
			tableAlias_ = tableAlias;
		}
		//
		if (tableID_.equals(dialog_.getHeaderTable().getTableID()) && tableID_.equals(tableAlias_)) {
			isFieldOnPrimaryTable = true;
			//
			ArrayList<String> keyFieldList = dialog_.getHeaderTable().getKeyFieldIDList();
			for (int i = 0; i < keyFieldList.size(); i++) {
				if (keyFieldList.get(i).equals(fieldID_)) {
					isKey = true;
					break;
				}
			}
		}
		//
		org.w3c.dom.Element workElement = dialog.getSession().getFieldElement(tableID_, fieldID_);
		if (workElement == null) {
			JOptionPane.showMessageDialog(this, tableID_ + "." + fieldID_ + res.getString("FunctionError11"));
		}
		dataType = workElement.getAttribute("Type");
		dataTypeOptions = workElement.getAttribute("TypeOptions");
		dataTypeOptionList = XFUtility.getOptionList(dataTypeOptions);
		if (workElement.getAttribute("Name").equals("")) {
			fieldCaption = workElement.getAttribute("ID");
		} else {
			fieldCaption = workElement.getAttribute("Name");
		}
		dataSize = Integer.parseInt(workElement.getAttribute("Size"));
		if (dataSize > 50) {
			dataSize = 50;
		}
		if (!workElement.getAttribute("Decimal").equals("")) {
			decimalSize = Integer.parseInt(workElement.getAttribute("Decimal"));
		}
		//
		tableElement = (org.w3c.dom.Element)workElement.getParentNode();
		if (!tableElement.getAttribute("RangeKey").equals("")) {
			StringTokenizer workTokenizer = new StringTokenizer(tableElement.getAttribute("RangeKey"), ";" );
			if (workTokenizer.nextToken().equals(fieldID_)) {
				isRangeKeyFieldValid = true;
			}
			if (workTokenizer.nextToken().equals(fieldID_)) {
				isRangeKeyFieldExpire = true;
			}
		}
		//
		xFTextField = new XFTextField(this.getBasicType(), dataSize, decimalSize, dataTypeOptions, fieldOptions);
		xFTextField.setLocation(5, 0);
		xFTextField.setEditable(false);
		//
		wrkStr = XFUtility.getOptionValueWithKeyword(dataTypeOptions, "KUBUN");
		if (!wrkStr.equals("")) {
			//
			String wrk;
			int fieldWidth = 50;
			JLabel jLabel = new JLabel();
			FontMetrics metrics = jLabel.getFontMetrics(new java.awt.Font("Dialog", 0, 14));
			try {
				Statement statement = dialog_.getSession().getConnection().createStatement();
				ResultSet result = statement.executeQuery("select KBUSERKUBUN,TXUSERKUBUN  from " + dialog_.getSession().getTableNameOfUserVariants() + " where IDUSERKUBUN = '" + wrkStr + "' order by SQLIST");
				while (result.next()) {
					keyValueList.add(result.getString("KBUSERKUBUN").trim());
					wrk = result.getString("TXUSERKUBUN").trim();
					textValueList.add(wrk);
					if (metrics.stringWidth(wrk) > fieldWidth) {
						fieldWidth = metrics.stringWidth(wrk);
					}
				}
				xFTextField.setSize(fieldWidth, xFTextField.getHeight());
				result.close();
			} catch (SQLException e) {
				e.printStackTrace(dialog_.getExceptionStream());
				dialog_.setErrorAndCloseFunction();
			}
		}
		//
		component = xFTextField;
		//
		fieldRows = component.getRows();
		jPanelField.setLayout(null);
		jPanelField.setPreferredSize(new Dimension(component.getWidth() + 5, component.getHeight()));
		jPanelField.add((JComponent)component);
		//
		this.setLayout(new BorderLayout());
		this.setPreferredSize(new Dimension(component.getWidth() + 130, component.getHeight()));
		this.add(jPanelField, BorderLayout.CENTER);
		//
		if (dataTypeOptionList.contains("VIRTUAL")) {
			isVirtualField = true;
		}
		//
		dialog_.getHeaderScriptBindings().put(this.getFieldIDInScript(), (XFScriptableField)this);
	}

	public String getTableID(){
		return tableID_;
	}

	public String getTableAlias(){
		return tableAlias_;
	}

	public String getFieldID(){
		return fieldID_;
	}

	public String getDataSourceName(){
		return tableAlias_ + "." + fieldID_;
	}

	public int getDecimalSize(){
		return decimalSize;
	}

	public String getCaption(){
		return jLabelField.getText();
	}

	public boolean isVisibleOnPanel(){
		return isVisibleOnPanel;
	}

	public boolean isHorizontal(){
		return isHorizontal;
	}

	public int getPositionMargin(){
		return positionMargin;
	}

	public boolean isVirtualField(){
		return isVirtualField;
	}

	public boolean isRangeKeyFieldValid(){
		return isRangeKeyFieldValid;
	}

	public boolean isRangeKeyFieldExpire(){
		return isRangeKeyFieldExpire;
	}

	public org.w3c.dom.Element getTableElement(){
		return tableElement;
	}

	public boolean isImage(){
		return isImage;
	}

	public int getRows(){
		return fieldRows;
	}

	public String getBasicType(){
		return XFUtility.getBasicTypeOf(dataType);
	}

	public boolean isFieldOnPrimaryTable(){
		return isFieldOnPrimaryTable;
	}

	public boolean isKey(){
		return isKey;
	}

	public void setValueOfResultSet(ResultSet result){
		try {
			if (this.isVirtualField) {
				if (this.isRangeKeyFieldExpire()) {
					component.setValue(XFUtility.calculateExpireValue(this.getTableElement(), result, dialog_.getSession()));
				}
			} else {
				Object value = result.getObject(this.getFieldID()); 
				if (this.getBasicType().equals("INTEGER")) {
					if (value == null || value.equals("")) {
						component.setValue("");
					} else {
						String wrkStr = value.toString();
						int pos = wrkStr.indexOf(".");
						if (pos >= 0) {
							wrkStr = wrkStr.substring(0, pos);
						}
						component.setValue(Integer.parseInt(wrkStr));
					}
				} else {
					if (this.getBasicType().equals("FLOAT")) {
						if (value == null || value.equals("")) {
							component.setValue("");
						} else {
							component.setValue(Float.parseFloat(value.toString()));
						}
					} else {
						if (result == null) {
							component.setValue("");
						} else {
							String stringValue = result.getString(this.getFieldID());
							if (stringValue == null) {
								component.setValue("");
							} else {
								stringValue = stringValue.trim();
								String wrkStr = XFUtility.getOptionValueWithKeyword(dataTypeOptions, "KUBUN");
								if (!wrkStr.equals("")) {
									for (int i = 0; i < keyValueList.size(); i++) {
										if (keyValueList.get(i).equals(stringValue)) {
											keyValue = stringValue;
											component.setValue(textValueList.get(i));
											break;
										}
									}

								} else {
									component.setValue(stringValue);
								}
							}
						}
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace(dialog_.getExceptionStream());
			dialog_.setErrorAndCloseFunction();
		}
	}

	public void setValue(Object object){
		String wrkStr = XFUtility.getOptionValueWithKeyword(dataTypeOptions, "KUBUN");
		if (wrkStr.equals("")) {
			XFUtility.setValueToEditableField(this.getBasicType(), object, component);
		} else {
			for (int i = 0; i < keyValueList.size(); i++) {
				if (keyValueList.get(i).equals(object)) {
					keyValue = (String)object;
					component.setValue(textValueList.get(i));
					break;
				}
			}
		}
	}

	public Object getInternalValue(){
		Object returnObj = null;
		//
		String wrkStr = XFUtility.getOptionValueWithKeyword(dataTypeOptions, "KUBUN");
		if (!wrkStr.equals("")) {
			returnObj = keyValue;
		} else {
			returnObj = (String)component.getInternalValue();
			//returnObj = component.getInternalValue();
		}
		//
		return returnObj;
	}

	public Object getExternalValue(){
		Object returnObj = (String)component.getExternalValue();
		return returnObj;
	}

	public Object getNullValue(){
		return XFUtility.getNullValueOfBasicType(this.getBasicType());
	}

	public Object getValue() {
		Object returnObj = null;
		//
		String wrkStr = XFUtility.getOptionValueWithKeyword(dataTypeOptions, "KUBUN");
		if (!wrkStr.equals("")) {
			returnObj = keyValue;
		} else {
			if (this.getBasicType().equals("INTEGER")) {
				returnObj = Integer.parseInt((String)component.getInternalValue());
			} else {
				if (this.getBasicType().equals("FLOAT")) {
					returnObj = Float.parseFloat((String)component.getInternalValue());
				} else {
					returnObj = (String)component.getInternalValue();
				}
			}
		}
		//
		return returnObj;
	}

	public void setOldValue(Object object){
	}

	public Object getOldValue() {
		return getInternalValue();
	}

	public void setEditable(boolean editable){
		component.setEditable(editable);
	}

	public boolean isEditable() {
		return false;
	}

	public void setError(String message) {
	}

	public String getError() {
		return "";
	}

	public void setColor(String color) {
		foreground = XFUtility.convertStringToColor(color);
		component.setForeground(foreground);
	}

	public String getColor() {
		return XFUtility.convertColorToString(foreground);
	}

	public String getFieldIDInScript(){
		return tableAlias_ + "_" + fieldID_;
	}
}

class XF300_DetailCellRenderer extends DefaultTableCellRenderer {
	private static final long serialVersionUID = 1L;
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column){
		XF300_DetailCell cell = (XF300_DetailCell)value;
		//
		setText((String)cell.getValue());
		setFont(new java.awt.Font("SansSerif", 0, 14));
		//
		if (isSelected) {
			setBackground(table.getSelectionBackground());
			setForeground(table.getSelectionForeground());
		} else {
			if (row%2==0) {
				setBackground(table.getBackground());
			} else {
				setBackground(XFUtility.ODD_ROW_COLOR);
			}
			setForeground(table.getForeground());
		}
		//
		if (!cell.getColor().equals(table.getForeground())) {
			setForeground(cell.getColor());
		}
		//
		validate();
		return this;
	}
}

class XF300_DetailRowNumberRenderer extends DefaultTableCellRenderer {
	private static final long serialVersionUID = 1L;
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column){
		XF300_DetailRowNumber cell = (XF300_DetailRowNumber)value;
		//
		setText(cell.toString());
		setFont(new java.awt.Font("SansSerif", 0, 14));
		setHorizontalAlignment(SwingConstants.RIGHT);
		//
		if (isSelected) {
			setBackground(table.getSelectionBackground());
			setForeground(table.getSelectionForeground());
		} else {
			if (row%2==0) {
				setBackground(table.getBackground());
			} else {
				setBackground(XFUtility.ODD_ROW_COLOR);
			}
			setForeground(table.getForeground());
		}
		//
		validate();
		return this;
	}
}

class XF300_DetailCell extends Object {
	private static final long serialVersionUID = 1L;
	private Object value_ = null;
	private Color color_ = null;
	public XF300_DetailCell(Object value, Color color) {
		value_ = value;
		color_ = color;
	}
	public Object getValue() {
		return value_;
	}
	public Color getColor() {
		return color_;
	}
}

class XF300_DetailRowNumber extends Object {
	private int number_;
	private HashMap<String, Object> keyMap_ = new HashMap<String, Object>();
	private DecimalFormat integerFormat = new DecimalFormat("0000");
	//
	public XF300_DetailRowNumber(int num, HashMap<String, Object> keyMap) {
		number_ = num;
		keyMap_ = keyMap;
	}
	public String toString() {
		return integerFormat.format(number_);
	}
	public HashMap<String, Object> getKeyMap() {
		return keyMap_;
	}
}

class XF300_DetailColumn extends Object implements XFScriptableField {
	private static final long serialVersionUID = 1L;
	private static ResourceBundle res = ResourceBundle.getBundle("xeadDriver.Res");
	private org.w3c.dom.Element functionColumnElement_ = null;
	private org.w3c.dom.Element tableElement = null;
	private XF300 dialog_ = null;
	private String tableID_ = "";
	private String tableAlias_ = "";
	private String fieldID_ = "";
	private String dataType = "";
	private int dataSize = 5;
	private int decimalSize = 0;
	private String dataTypeOptions = "";
	private ArrayList<String> dataTypeOptionList;
	private String fieldOptions = "";
	private String fieldCaption = "";
	private int fieldWidth = 50;
	private int columnIndex = -1;
	private boolean isVisibleOnPanel = true;
	private boolean isVirtualField = false;
	private boolean isRangeKeyFieldValid = false;
	private boolean isRangeKeyFieldExpire = false;
	private boolean isReadyToValidate = false;
	private DecimalFormat integerFormat = new DecimalFormat("#,##0");
	private DecimalFormat floatFormat0 = new DecimalFormat("#,##0");
	private DecimalFormat floatFormat1 = new DecimalFormat("#,##0.0");
	private DecimalFormat floatFormat2 = new DecimalFormat("#,##0.00");
	private DecimalFormat floatFormat3 = new DecimalFormat("#,##0.000");
	private DecimalFormat floatFormat4 = new DecimalFormat("#,##0.0000");
	private DecimalFormat floatFormat5 = new DecimalFormat("#,##0.00000");
	private DecimalFormat floatFormat6 = new DecimalFormat("#,##0.000000");
	private ArrayList<String> kubunValueList = new ArrayList<String>();
	private ArrayList<String> kubunTextList = new ArrayList<String>();
	private Object value_ = null;
	private Color foreground = Color.black;

	public XF300_DetailColumn(String detailTableID, org.w3c.dom.Element functionColumnElement, XF300 dialog, int index){
		super();
		//
		String wrkStr;
		functionColumnElement_ = functionColumnElement;
		dialog_ = dialog;
		fieldOptions = functionColumnElement_.getAttribute("FieldOptions");
		//
		StringTokenizer workTokenizer = new StringTokenizer(functionColumnElement_.getAttribute("DataSource"), "." );
		tableAlias_ = workTokenizer.nextToken();
		tableID_ = dialog.getTableIDOfTableAlias(tableAlias_, index);
		fieldID_ =workTokenizer.nextToken();
		//
		org.w3c.dom.Element workElement = dialog.getSession().getFieldElement(tableID_, fieldID_);
		if (workElement == null) {
			JOptionPane.showMessageDialog(null, tableID_ + "." + fieldID_ + res.getString("FunctionError11"));
		}
		dataType = workElement.getAttribute("Type");
		dataTypeOptions = workElement.getAttribute("TypeOptions");
		dataTypeOptionList = XFUtility.getOptionList(dataTypeOptions);
		if (workElement.getAttribute("Name").equals("")) {
			fieldCaption = workElement.getAttribute("ID");
		} else {
			fieldCaption = workElement.getAttribute("Name");
		}
		wrkStr = XFUtility.getOptionValueWithKeyword(fieldOptions, "CAPTION");
		if (!wrkStr.equals("")) {
			fieldCaption = wrkStr;
		}
		dataSize = Integer.parseInt(workElement.getAttribute("Size"));
		if (dataSize > 50) {
			dataSize = 50;
		}
		if (!workElement.getAttribute("Decimal").equals("")) {
			decimalSize = Integer.parseInt(workElement.getAttribute("Decimal"));
		}
		//
		tableElement = (org.w3c.dom.Element)workElement.getParentNode();
		if (!tableElement.getAttribute("RangeKey").equals("")) {
			workTokenizer = new StringTokenizer(tableElement.getAttribute("RangeKey"), ";" );
			if (workTokenizer.nextToken().equals(fieldID_)) {
				isRangeKeyFieldValid = true;
			}
			if (workTokenizer.nextToken().equals(fieldID_)) {
				isRangeKeyFieldExpire = true;
			}
		}
		//
		if (dataTypeOptionList.contains("VIRTUAL")) {
			isVirtualField = true;
		}
		//
		JLabel jLabel = new JLabel();
		FontMetrics metrics = jLabel.getFontMetrics(new java.awt.Font("Dialog", 0, 14));
		int captionWidth = metrics.stringWidth(fieldCaption) + 18;
		//
		String basicType = this.getBasicType();
		wrkStr = XFUtility.getOptionValueWithKeyword(dataTypeOptions, "KUBUN");
		if (!wrkStr.equals("")) {
			try {
				String wrk = "";
				Statement statement = dialog_.getSession().getConnection().createStatement();
				ResultSet result = statement.executeQuery("select * from " + dialog_.getSession().getTableNameOfUserVariants() + " where IDUSERKUBUN = '" + wrkStr + "'");
				while (result.next()) {
					//
					kubunValueList.add(result.getString("KBUSERKUBUN").trim());
					wrk = result.getString("TXUSERKUBUN").trim();
					if (metrics.stringWidth(wrk) + 10 > fieldWidth) {
						fieldWidth = metrics.stringWidth(wrk) + 10;
					}
					kubunTextList.add(wrk);
				}
				result.close();
			} catch (SQLException e) {
				e.printStackTrace(dialog_.getExceptionStream());
				dialog_.setErrorAndCloseFunction();
			}
		} else {
			if (dataTypeOptionList.contains("KANJI")) {
				fieldWidth = dataSize * 14 + 5;
			} else {
				if (dataTypeOptionList.contains("FYEAR")) {
					fieldWidth = 80;
				} else {
					if (dataTypeOptionList.contains("YMONTH")) {
						fieldWidth = 85;
					} else {
						if (dataTypeOptionList.contains("MSEQ")) {
							fieldWidth = 50;
						} else {
							if (basicType.equals("INTEGER") || basicType.equals("FLOAT")) {
								fieldWidth = XFUtility.getLengthOfEdittedNumericValue(dataSize, decimalSize, dataTypeOptionList.contains("ACCEPT_MINUS")) * 7 + 21;
							} else {
								if (basicType.equals("DATE")) {
									fieldWidth = XFUtility.getWidthOfDateValue(dialog_.getSession().getDateFormat());
								} else {
									fieldWidth = dataSize * 7 + 12;
								}
							}
						}
					}
				}
			}
		}
		//
		if (fieldWidth > 320) {
			fieldWidth = 320;
		}
		//
		wrkStr = XFUtility.getOptionValueWithKeyword(fieldOptions, "WIDTH");
		if (!wrkStr.equals("")) {
			fieldWidth = Integer.parseInt(wrkStr);
		}
		//
		if (captionWidth > fieldWidth) {
			fieldWidth = captionWidth;
		}
		//
		dialog_.getDetailScriptBindings(index).put(this.getFieldIDInScript(), (XFScriptableField)this);
	}

	public XF300_DetailColumn(String detailTableID, String tableID, String tableAlias, String fieldID, XF300 dialog, int index){
		super();
		//
		functionColumnElement_ = null;
		dialog_ = dialog;
		fieldOptions = "";
		//
		isVisibleOnPanel = false;
		//
		tableID_ = tableID;
		if (tableAlias.equals("")) {
			tableAlias_ = tableID;
		} else {
			tableAlias_ = tableAlias;
		}
		fieldID_ = fieldID;
		//
		org.w3c.dom.Element workElement = dialog.getSession().getFieldElement(tableID_, fieldID_);
		if (workElement == null) {
			JOptionPane.showMessageDialog(null, tableID_ + "." + fieldID_ + res.getString("FunctionError11"));
		}
		dataType = workElement.getAttribute("Type");
		dataTypeOptions = workElement.getAttribute("TypeOptions");
		dataTypeOptionList = XFUtility.getOptionList(dataTypeOptions);
		if (workElement.getAttribute("Name").equals("")) {
			fieldCaption = workElement.getAttribute("ID");
		} else {
			fieldCaption = workElement.getAttribute("Name");
		}
		dataSize = Integer.parseInt(workElement.getAttribute("Size"));
		if (dataSize > 50) {
			dataSize = 50;
		}
		//
		tableElement = (org.w3c.dom.Element)workElement.getParentNode();
		if (!tableElement.getAttribute("RangeKey").equals("")) {
			StringTokenizer workTokenizer = new StringTokenizer(tableElement.getAttribute("RangeKey"), ";" );
			if (workTokenizer.nextToken().equals(fieldID_)) {
				isRangeKeyFieldValid = true;
			}
			if (workTokenizer.nextToken().equals(fieldID_)) {
				isRangeKeyFieldExpire = true;
			}
		}
		//
		if (dataTypeOptionList.contains("VIRTUAL")) {
			isVirtualField = true;
		}
		//
		dialog_.getDetailScriptBindings(index).put(this.getFieldIDInScript(), (XFScriptableField)this);
	}

	public boolean isVisibleOnPanel(){
		return isVisibleOnPanel;
	}

	public boolean isVirtualField(){
		return isVirtualField;
	}

	public boolean isRangeKeyFieldValid(){
		return isRangeKeyFieldValid;
	}

	public boolean isRangeKeyFieldExpire(){
		return isRangeKeyFieldExpire;
	}

	public boolean isReadyToValidate(){
		return isReadyToValidate;
	}

	public void setReadyToValidate(boolean isReady) {
		isReadyToValidate = isReady;
	}

	public org.w3c.dom.Element getTableElement(){
		return tableElement;
	}

	public String getBasicType(){
		return XFUtility.getBasicTypeOf(dataType);
	}

	public Object getNullValue(){
		return XFUtility.getNullValueOfBasicType(this.getBasicType());
	}

	public String getTableID(){
		return tableID_;
	}

	public String getFieldID(){
		return fieldID_;
	}

	public String getFieldIDInScript(){
		return tableAlias_ + "_" + fieldID_;
	}

	public String getTableAlias(){
		return tableAlias_;
	}

	public String getDataSourceName(){
		return tableAlias_ + "." + fieldID_;
	}

	public int getDecimalSize(){
		return decimalSize;
	}

	public Object getInternalValue(){
		return value_;
	}

	public Object getExternalValue(){
		Object value = null;
		String basicType = this.getBasicType();
		//
		if (basicType.equals("INTEGER") && !dataTypeOptionList.contains("MSEQ") && !dataTypeOptionList.contains("FYEAR")) {
			if (value_ == null || value_.toString().equals("")) {
				value = "";
			} else {
				String wrkStr = value_.toString();
				int pos = wrkStr.indexOf(".");
				if (pos >= 0) {
					wrkStr = wrkStr.substring(0, pos);
				}
				value = integerFormat.format(Integer.parseInt(wrkStr));
			}
		} else {
			if (basicType.equals("FLOAT")) {
				double doubleWrk = 0;
				if (value_ != null && !value_.toString().equals("")) {
					doubleWrk = Double.parseDouble(value_.toString());
				}
				if (decimalSize == 0) {
					value = floatFormat0.format(doubleWrk);
				}
				if (decimalSize == 1) {
					value = floatFormat1.format(doubleWrk);
				}
				if (decimalSize == 2) {
					value = floatFormat2.format(doubleWrk);
				}
				if (decimalSize == 3) {
					value = floatFormat3.format(doubleWrk);
				}
				if (decimalSize == 4) {
					value = floatFormat4.format(doubleWrk);
				}
				if (decimalSize == 5) {
					value = floatFormat5.format(doubleWrk);
				}
				if (decimalSize == 6) {
					value = floatFormat6.format(doubleWrk);
				}

			} else {
				if (basicType.equals("DATE")) {
					if (value_ == null || value_.equals("")) {
						value = "";
					} else {
						value = XFUtility.getUserExpressionOfUtilDate(XFUtility.convertDateFromSqlToUtil(java.sql.Date.valueOf(value_.toString())), dialog_.getSession().getDateFormat(), false);
					}
				} else {
					if (basicType.equals("DATETIME")) {
						if (value_ != null) {
							value = value_.toString().replace("-", "/");
						}
					} else {
						if (!XFUtility.getOptionValueWithKeyword(dataTypeOptions, "KUBUN").equals("")) {
							String wrkStr = value_.toString().trim();
							for (int i = 0; i < kubunValueList.size(); i++) {
								if (kubunValueList.get(i).equals(wrkStr)) {
									value = kubunTextList.get(i);
									break;
								}
							}
						} else {
							if (value_ != null) {
								value = value_.toString().trim();
								//
								if (dataTypeOptionList.contains("YMONTH") || dataTypeOptionList.contains("FYEAR")) {
									String wrkStr = (String)value.toString();
									if (!wrkStr.equals("")) {
										value = XFUtility.getUserExpressionOfYearMonth(wrkStr, dialog_.getSession().getDateFormat());
									}
								}
								//
								if (dataTypeOptionList.contains("MSEQ")) {
									String wrkStr = (String)value.toString();
									if (!wrkStr.equals("")) {
										value = XFUtility.getUserExpressionOfMSeq(Integer.parseInt(wrkStr), dialog_.getSession());
									}
								}
							}
						}
					}
				}
			}
		}
		//
		return value;
	}

	public XF300_DetailCell getCellObject() {
		return new XF300_DetailCell(this.getExternalValue(), this.getForeground());
	}

	public XF300_DetailCellRenderer getCellRenderer(){
		XF300_DetailCellRenderer renderer = new XF300_DetailCellRenderer();
		//
		if (this.getBasicType().equals("INTEGER") || this.getBasicType().equals("FLOAT")) {
			renderer.setHorizontalAlignment(SwingConstants.RIGHT);
		} else {
			renderer.setHorizontalAlignment(SwingConstants.LEFT);
		}
		return renderer;
	}

	public String getCaption(){
		return fieldCaption;
	}

	public int getWidth(){
		return fieldWidth;
	}

	public int getColumnIndex(){
		return columnIndex;
	}

	public void setColumnIndex(int index){
		columnIndex = index;
	}

	public boolean setValueOfResultSet(ResultSet result) {
		String basicType = this.getBasicType();
		boolean isFoundInResultSet = false;
		this.setColor("");
		//
		try {
			if (this.isVirtualField) {
				if (this.isRangeKeyFieldExpire()) {
					value_ = XFUtility.calculateExpireValue(this.getTableElement(), result, dialog_.getSession());
				}
			} else {
				//
				Object value = result.getObject(this.getFieldID()); 
				isFoundInResultSet = true;
				//
//				if (this.isFieldOnDetailTable) {
//					value_ = value;
//				} else {
					if (basicType.equals("INTEGER")) {
						if (value == null || value.equals("")) {
							value_ = "";
						} else {
							String wrkStr = value.toString();
							int pos = wrkStr.indexOf(".");
							if (pos >= 0) {
								wrkStr = wrkStr.substring(0, pos);
							}
							value_ = Integer.parseInt(wrkStr);
						}
					} else {
						if (basicType.equals("FLOAT")) {
							if (value == null || value.equals("")) {
								value_ = "";
							} else {
								value_ = Float.parseFloat(value.toString());
							}
						} else {
							if (result == null) {
								value_ = "";
							} else {
								String stringValue = result.getString(this.getFieldID());
								if (stringValue == null) {
									value_ = "";
								} else {
									value_ = stringValue.trim();
								}
							}
						}
					}
//				}
			}
		} catch (SQLException e) {
			e.printStackTrace(dialog_.getExceptionStream());
			dialog_.setErrorAndCloseFunction();
		}
		return isFoundInResultSet;
	}

	public void initialize() {
		value_ = XFUtility.getNullValueOfBasicType(this.getBasicType());
	}

	public void setValue(Object value){
		value_ = XFUtility.getValueAccordingToBasicType(this.getBasicType(), value);
	}

	public Object getValue() {
		return getInternalValue();
	}

	public void setOldValue(Object value){
	}

	public Object getOldValue() {
		return getInternalValue();
	}

	public void setEditable(boolean editable){
	}

	public boolean isEditable() {
		return false;
	}

	public void setError(String message) {
	}

	public String getError() {
		return "";
	}

	public void setColor(String color) {
		foreground = XFUtility.convertStringToColor(color);
	}

	public String getColor() {
		return XFUtility.convertColorToString(foreground);
	}

	Color getForeground() {
		return foreground;
	}
}


class XF300_Filter extends JPanel {
	private static final long serialVersionUID = 1L;
	private static ResourceBundle res = ResourceBundle.getBundle("xeadDriver.Res");
	private org.w3c.dom.Element fieldElement_ = null;
	private XF300 dialog_ = null;
	private String dataType = "";
	private String dataTypeOptions = "";
	private ArrayList<String> dataTypeOptionList;
	private String tableID = "";
	private String tableAlias = "";
	private String fieldID = "";
	private String fieldCaption = "";
	private String fieldOptions = "";
	private ArrayList<String> fieldOptionList;
	private String componentType = "";
	private String operandType = "";
	private int dataSize = 5;
	private int decimalSize = 0;
	private JPanel jPanelField = new JPanel();
	private JLabel jLabelField = new JLabel();
	private XFTextField xFTextField = null;
	private XFDateField xFDateField = null;
	private XFYMonthBox xFYMonthBox = null;
	private XFMSeqBox xFMSeqBox = null;
	private XFFYearBox xFFYearBox = null;
	private JComboBox jComboBox = null;
	private XF300_PromptCallField xFPromptCall = null;
	private ArrayList<String> keyValueList = new ArrayList<String>();
	private JComponent component = null;
	private boolean isReflect = false;
	private boolean isEditable_ = true;
	private int index_ = 0;
	//
	public XF300_Filter(org.w3c.dom.Element fieldElement, XF300 dialog, int index){
		super();
		//
		String wrkStr;
		fieldElement_ = fieldElement;
		dialog_ = dialog;
		index_ = index;
		//
		fieldOptions = fieldElement_.getAttribute("FieldOptions");
		fieldOptionList = XFUtility.getOptionList(fieldOptions);
		StringTokenizer workTokenizer1 = new StringTokenizer(fieldElement_.getAttribute("DataSource"), "." );
		tableAlias = workTokenizer1.nextToken();
		tableID = dialog_.getTableIDOfTableAlias(tableAlias, index);
		fieldID =workTokenizer1.nextToken();
		//
		org.w3c.dom.Element workElement = dialog_.getSession().getFieldElement(tableID, fieldID);
		if (workElement == null) {
			JOptionPane.showMessageDialog(this, tableID + "." + fieldID + res.getString("FunctionError11"));
		}
		dataType = workElement.getAttribute("Type");
		dataTypeOptions = workElement.getAttribute("TypeOptions");
		dataTypeOptionList = XFUtility.getOptionList(dataTypeOptions);
		if (workElement.getAttribute("Name").equals("")) {
			fieldCaption = workElement.getAttribute("ID");
		} else {
			fieldCaption = workElement.getAttribute("Name");
		}
		dataSize = Integer.parseInt(workElement.getAttribute("Size"));
		if (dataSize > 50) {
			dataSize = 50;
		}
		if (!workElement.getAttribute("Decimal").equals("")) {
			decimalSize = Integer.parseInt(workElement.getAttribute("Decimal"));
		}
		//
		operandType = "EQ";
		if (fieldOptionList.contains("GE")) {
			operandType = "GE";
		}
		if (fieldOptionList.contains("GT")) {
			operandType = "GT";
		}
		if (fieldOptionList.contains("LE")) {
			operandType = "LE";
		}
		if (fieldOptionList.contains("LT")) {
			operandType = "LT";
		}
		if (fieldOptionList.contains("SCAN")) {
			operandType = "SCAN";
		}
		if (fieldOptionList.contains("GENERIC")) {
			operandType = "GENERIC";
		}
		if (fieldOptionList.contains("REFLECT")) {
			operandType = "REFLECT";
		}
		//
		if (operandType.equals("REFLECT")) {
			isReflect = true;
		}
		//
		wrkStr = XFUtility.getOptionValueWithKeyword(fieldOptions, "CAPTION");
		if (!wrkStr.equals("")) {
			fieldCaption = wrkStr;
		}
		jLabelField = new JLabel(fieldCaption);
		jLabelField.setPreferredSize(new Dimension(90, 20));
		jLabelField.setHorizontalAlignment(SwingConstants.RIGHT);
		FontMetrics metrics1 = jLabelField.getFontMetrics(new java.awt.Font("Dialog", 0, 14));
		if (metrics1.stringWidth(fieldCaption) > 90) {
			jLabelField.setFont(new java.awt.Font("Dialog", 0, 12));
			metrics1 = jLabelField.getFontMetrics(new java.awt.Font("Dialog", 0, 12));
			if (metrics1.stringWidth(fieldCaption) > 90) {
				jLabelField.setFont(new java.awt.Font("Dialog", 0, 10));
			} else {
				jLabelField.setFont(new java.awt.Font("Dialog", 0, 12));
			}
		} else {
			jLabelField.setFont(new java.awt.Font("Dialog", 0, 14));
		}
		//
		jPanelField.setLayout(null);
		this.setLayout(new BorderLayout());
		this.add(jLabelField, BorderLayout.WEST);
		this.add(jPanelField, BorderLayout.CENTER);
		//
		componentType = "TEXTFIELD";
		xFTextField = new XFTextField(this.getBasicType(), dataSize, decimalSize, dataTypeOptions, fieldOptions);
		int fieldWidthMax = (int)dialog_.getFilterWidth(index_) - 100;
		if (xFTextField.getWidth() > fieldWidthMax) {
			xFTextField.setBounds(new Rectangle(5, 0, fieldWidthMax, xFTextField.getHeight()));
		} else {
			xFTextField.setLocation(5, 0);
		}
		xFTextField.setValue(getDefaultValue());
		component = xFTextField;
		//
		// PROMPT_LIST1 is the list with blank row, PROMPT_LIST2 is without blank row
		FontMetrics metrics2 = jLabelField.getFontMetrics(new java.awt.Font("Dialog", 0, 12));
		int valueIndex = -1;
		int selectIndex = 0;
		String wrkText, wrkKey;
		if (fieldOptionList.contains("PROMPT_LIST1") || fieldOptionList.contains("PROMPT_LIST2")) {
			//
			wrkStr = XFUtility.getOptionValueWithKeyword(dataTypeOptions, "KUBUN");
			if (!wrkStr.equals("")) {
				//
				componentType = "KUBUN_LIST";
				Object defaultValue = getDefaultValue();
				jComboBox = new JComboBox();
				component = jComboBox;
				//
				fieldWidthMax = fieldWidthMax - 28;
				int fieldWidth = 20;
				//
				if (fieldOptionList.contains("PROMPT_LIST1")) {
					valueIndex++;
					keyValueList.add("");
					jComboBox.addItem("");
				}
				//
				try {
					Statement statement = dialog_.getSession().getConnection().createStatement();
					ResultSet result = statement.executeQuery("select * from " + dialog_.getSession().getTableNameOfUserVariants() + " where IDUSERKUBUN = '" + wrkStr + "' order by SQLIST");
					while (result.next()) {
						valueIndex++;
						//
						wrkKey = result.getString("KBUSERKUBUN").trim();
						keyValueList.add(wrkKey);
						if (wrkKey.equals(defaultValue)) {
							selectIndex = valueIndex;
						}
						wrkText = result.getString("TXUSERKUBUN").trim();
						jComboBox.addItem(wrkText);
						if (metrics2.stringWidth(wrkText) > fieldWidth) {
							fieldWidth = metrics2.stringWidth(wrkText);
						}
					}
					result.close();
					//
					if (fieldWidth > fieldWidthMax) {
						fieldWidth = fieldWidthMax;
					}
					jComboBox.setBounds(new Rectangle(5, 0, fieldWidth + 28, 24));
					jComboBox.setFont(new java.awt.Font("Dialog", 0, 12));
					jComboBox.setSelectedIndex(selectIndex);
				} catch(SQLException e) {
					e.printStackTrace(dialog_.getExceptionStream());
					dialog_.setErrorAndCloseFunction();
				}
				//
			} else {
				//
				wrkStr = XFUtility.getOptionValueWithKeyword(dataTypeOptions, "VALUES");
				if (!wrkStr.equals("")) {
					//
					componentType = "VALUES_LIST";
					Object defaultValue = getDefaultValue();
					jComboBox = new JComboBox();
					component = jComboBox;
					//
					fieldWidthMax = fieldWidthMax - 28;
					int fieldWidth = 20;
					//
					if (fieldOptionList.contains("PROMPT_LIST1")) {
						valueIndex++;
						jComboBox.addItem("");
					}
					//
					StringTokenizer workTokenizer = new StringTokenizer(wrkStr, ";" );
					while (workTokenizer.hasMoreTokens()) {
						valueIndex++;
						wrkKey = workTokenizer.nextToken();
						jComboBox.addItem(wrkKey);
						if (wrkKey.equals(defaultValue)) {
							selectIndex = valueIndex;
						}
						if (metrics2.stringWidth(wrkKey) > fieldWidth) {
							fieldWidth = metrics2.stringWidth(wrkKey);
						}
					}
					//
					if (fieldWidth > fieldWidthMax) {
						fieldWidth = fieldWidthMax;
					}
					jComboBox.setBounds(new Rectangle(5, 0, fieldWidth + 28, 24));
					jComboBox.setFont(new java.awt.Font("Dialog", 0, 12));
					jComboBox.setSelectedIndex(selectIndex);
					//
				} else {
					//
					componentType = "RECORDS_LIST";
					Object defaultValue = getDefaultValue();
					jComboBox = new JComboBox();
					component = jComboBox;
					//
					fieldWidthMax = fieldWidthMax - 28;
					int fieldWidth = 20;
					//
					ArrayList<XF300_DetailReferTable> referTableList = dialog_.getDetailReferTableList(index);
					for (int i = 0; i < referTableList.size(); i++) {
						if (referTableList.get(i).getTableID().equals(tableID)) {
							if (referTableList.get(i).getTableAlias().equals("") || referTableList.get(i).getTableAlias().equals(tableAlias)) {
								//
								if (fieldOptionList.contains("PROMPT_LIST1")) {
									valueIndex++;
									jComboBox.addItem("");
								}
								//
								try {
									Statement statement = dialog_.getSession().getConnection().createStatement();
									StringBuffer buf = new StringBuffer();
									buf.append("select ");
									buf.append(fieldID);
									buf.append(" from ");
									buf.append(tableID);
									buf.append(" order by ");
									buf.append(fieldID);
									ResultSet result = statement.executeQuery(buf.toString());
									while (result.next()) {
										valueIndex++;
										wrkKey = result.getString(fieldID).trim();
										jComboBox.addItem(wrkKey);
										if (wrkKey.equals(defaultValue)) {
											selectIndex = valueIndex;
										}
										if (metrics2.stringWidth(wrkKey) > fieldWidth) {
											fieldWidth = metrics2.stringWidth(wrkKey);
										}
									}
									result.close();
									//
									//
									if (fieldWidth > fieldWidthMax) {
										fieldWidth = fieldWidthMax;
									}
									jComboBox.setBounds(new Rectangle(5, 0, fieldWidth + 28, 24));
									jComboBox.setFont(new java.awt.Font("Dialog", 0, 12));
									jComboBox.setSelectedIndex(selectIndex);
									//
								} catch(SQLException e) {
									e.printStackTrace(dialog_.getExceptionStream());
									dialog_.setErrorAndCloseFunction();
								}
								break;
							}
						}
					}
				}
			}
			//
		} else {
			wrkStr = XFUtility.getOptionValueWithKeyword(fieldOptions, "PROMPT_CALL");
			if (!wrkStr.equals("")) {
				componentType = "PROMPT_CALL";
				org.w3c.dom.Element promptFunctionElement = null;
				NodeList functionList = dialog_.getSession().getFunctionList();
				for (int k = 0; k < functionList.getLength(); k++) {
					workElement = (org.w3c.dom.Element)functionList.item(k);
					if (workElement.getAttribute("ID").equals(wrkStr)) {
						promptFunctionElement = workElement;
						break;
					}
				}
				xFPromptCall = new XF300_PromptCallField(fieldElement, promptFunctionElement, dialog_, index_);
				xFPromptCall.setLocation(5, 0);
				xFPromptCall.setValue(getDefaultValue());
				component = xFPromptCall;
			} else {
				//
				if (dataType.equals("DATE")) {
					componentType = "DATE";
					xFDateField = new XFDateField(dialog_.getSession());
					xFDateField.setLocation(5, 0);
					xFDateField.setEditable(true);
					xFDateField.setValue(getDefaultValue());
					component = xFDateField;
				} else {
					//
					if (dataTypeOptionList.contains("YMONTH")) {
						componentType = "YMONTH";
						xFYMonthBox = new XFYMonthBox(dialog_.getSession());
						xFYMonthBox.setLocation(5, 0);
						xFYMonthBox.setEditable(true);
						xFYMonthBox.setValue(getDefaultValue());
						component = xFYMonthBox;
					} else {
						if (dataTypeOptionList.contains("MSEQ")) {
							componentType = "MSEQ";
							xFMSeqBox = new XFMSeqBox(dialog_.getSession());
							xFMSeqBox.setLocation(5, 0);
							xFMSeqBox.setEditable(true);
							xFMSeqBox.setValue(getDefaultValue());
							component = xFMSeqBox;
						} else {
							if (dataTypeOptionList.contains("FYEAR")) {
								componentType = "FYEAR";
								xFFYearBox = new XFFYearBox(dialog_.getSession());
								xFFYearBox.setLocation(5, 0);
								xFFYearBox.setEditable(true);
								xFFYearBox.setValue(getDefaultValue());
								component = xFFYearBox;
							}
						}
					}
				}
			}
		}
		//
		if (fieldOptionList.contains("NON_EDITABLE")) {
			this.setEditable(false);
		}
		//
		component.addKeyListener(new XF300_keyAdapter(dialog));
		jPanelField.add(component);
	}

	Object getDefaultValue() {
		Object value = "";
		//
		String wrkStr = XFUtility.getOptionValueWithKeyword(fieldOptions, "VALUE");
		if (!wrkStr.equals("")) {
			value = XFUtility.getDefaultValueOfFilterField(wrkStr, dialog_.getSession());
			if (value == null) {
				JOptionPane.showMessageDialog(this, tableID + "." + fieldID + res.getString("FunctionError46") + "\n" + wrkStr);
			}
		}
		//
		return value;
	}

	public String getBasicType(){
		return XFUtility.getBasicTypeOf(dataType);
	}

	public String getCaptionAndValue(){
		String value = fieldCaption + ":";
		String wrk = "";
		//
		if (componentType.equals("TEXTFIELD")) {
			wrk = (String)xFTextField.getExternalValue();
		}
		if (componentType.equals("KUBUN_LIST") || componentType.equals("VALUES_LIST") || componentType.equals("RECORDS_LIST")) {
			wrk = jComboBox.getSelectedItem().toString();
		}
		if (componentType.equals("YMONTH")) {
			wrk = (String)xFYMonthBox.getExternalValue();
		}
		if (componentType.equals("MSEQ")) {
			wrk = (String)xFMSeqBox.getExternalValue();
		}
		if (componentType.equals("FYEAR")) {
			wrk = (String)xFFYearBox.getExternalValue();
		}
		if (componentType.equals("DATE")) {
			wrk = (String)xFDateField.getExternalValue();
		}
		if (componentType.equals("PROMPT_CALL")) {
			wrk = (String)xFPromptCall.getExternalValue();
		}
		//
		if (wrk.equals("")) {
			value = value + "* ";
		} else {
			value = value + wrk + " ";
		}
		//
		return value;
	}

	public String getDataType(){
		return dataType;
	}
	
	public void requestFocus(){
		super.requestFocus();
		component.requestFocus();
	}
	
	public String getFieldID(){
		return fieldID;
	}
	
	public String getTableAlias(){
		return tableAlias;
	}
	
	public String getTableID(){
		return tableID;
	}
	
	public String getDataSourceName(){
		return tableAlias + "." + fieldID;
	}

	public void setParmMapValue(HashMap<String, Object> parmMap){
		String mapKey = this.getTableID() + "." + this.getFieldID();
		//
		if (parmMap != null && parmMap.containsKey(mapKey)) {
			//
			Object mapValue = parmMap.get(mapKey);
			//
			if (componentType.equals("TEXTFIELD")) {
				xFTextField.setText(mapValue.toString().trim());
			}
			//
			if (componentType.equals("DATE")) {
				xFDateField.setValue(mapValue.toString());
				xFDateField.setEditable(false);
			}
			//
			if (componentType.equals("YMONTH")) {
				xFYMonthBox.setValue(mapValue.toString());
			}
			//
			if (componentType.equals("MSEQ")) {
				xFMSeqBox.setValue(mapValue.toString());
			}
			//
			if (componentType.equals("FYEAR")) {
				xFFYearBox.setValue(mapValue.toString());
			}
			//
			if (componentType.equals("KUBUN_LIST")) {
				jComboBox.setSelectedIndex(keyValueList.indexOf(mapValue.toString()));
			}
			//
			if (componentType.equals("VALUES_LIST") || componentType.equals("RECORDS_LIST")) {
				for (int i = 0; i < jComboBox.getItemCount(); i++) {
					if (jComboBox.getItemAt(i).equals(mapValue.toString())) {
							jComboBox.setSelectedIndex(i);
							break;
					}
				}
			}
			//
			if (componentType.equals("PROMPT_CALL")) {
				xFPromptCall.setValue(mapValue.toString());
			}
			//
			//this.setEditable(false);
			//isEditable_ = false;
		}
	}

	public void setValue(Object value){
		//
		if (componentType.equals("TEXTFIELD")) {
			xFTextField.setText(value.toString().trim());
		}
		//
		if (componentType.equals("DATE")) {
			xFDateField.setValue(value.toString());
			xFDateField.setEditable(false);
		}
		//
		if (componentType.equals("YMONTH")) {
			xFYMonthBox.setValue(value.toString());
		}
		//
		if (componentType.equals("MSEQ")) {
			xFMSeqBox.setValue(value.toString());
		}
		//
		if (componentType.equals("FYEAR")) {
			xFFYearBox.setValue(value.toString());
		}
		//
		if (componentType.equals("KUBUN_LIST")) {
			jComboBox.setSelectedIndex(keyValueList.indexOf(value.toString()));
		}
		//
		if (componentType.equals("VALUES_LIST") || componentType.equals("RECORDS_LIST")) {
			for (int i = 0; i < jComboBox.getItemCount(); i++) {
				if (jComboBox.getItemAt(i).equals(value.toString())) {
					jComboBox.setSelectedIndex(i);
					break;
				}
			}
		}
		//
		if (componentType.equals("PROMPT_CALL")) {
			xFPromptCall.setValue(value.toString());
		}
	}
	
	public void setEditable(boolean isEditable) {
		isEditable_ = isEditable;
		//
		if (componentType.equals("TEXTFIELD")) {
			xFTextField.setEditable(isEditable_);
		}
		//
		if (componentType.equals("DATE")) {
			xFDateField.setEditable(isEditable_);
		}
		//
		if (componentType.equals("YMONTH")) {
			xFYMonthBox.setEditable(isEditable_);
		}
		//
		if (componentType.equals("MSEQ")) {
			xFMSeqBox.setEditable(isEditable_);
		}
		//
		if (componentType.equals("FYEAR")) {
			xFFYearBox.setEditable(isEditable_);
		}
		//
		if (componentType.equals("KUBUN_LIST") || componentType.equals("VALUES_LIST") || componentType.equals("RECORDS_LIST")) {
			jComboBox.setEditable(isEditable_);
		}
		//
		if (componentType.equals("PROMPT_CALL")) {
			xFPromptCall.setEditable(isEditable_);
		}
	}
	
	public boolean isEditable() {
		return isEditable_;
	}

	public boolean isReflect(){
		return isReflect;
	}
	
	public boolean isValidated(){
		boolean validated = false;
		//
		XF300_DetailColumn columnField = dialog_.getDetailColumnObjectByID(index_, this.getTableID(), this.getTableAlias(), this.getFieldID());
		//
		if (this.isReflect) {
			//
			if (componentType.equals("TEXTFIELD")) {
				columnField.setValue((String)xFTextField.getInternalValue());
			}
			//
			if (componentType.equals("KUBUN_LIST") || componentType.equals("RECORDS_LIST") || componentType.equals("VALUES_LIST")) {
				columnField.setValue((String)jComboBox.getSelectedItem());
			}
			//
			if (componentType.equals("DATE")) {
				columnField.setValue(xFDateField.getInternalValue());
			}
			//
			if (componentType.equals("YMONTH")) {
				columnField.setValue(xFYMonthBox.getInternalValue());
			}
			//
			if (componentType.equals("MSEQ")) {
				columnField.setValue(xFMSeqBox.getInternalValue());
			}
			//
			if (componentType.equals("FYEAR")) {
				columnField.setValue(xFFYearBox.getInternalValue());
			}
			//
			if (componentType.equals("PROMPT_CALL")) {
				columnField.setValue(xFPromptCall.getInternalValue());
			}
			//
			validated = true;
			//
		} else {
			//
			String stringResultValue = "";
			String stringFilterValue = "";
			double doubleResultValue = 0;
			double doubleFilterValue = 0;
			//
			if (columnField.isReadyToValidate()) {
				if (componentType.equals("TEXTFIELD")) {
					if (columnField.getInternalValue() != null) {
						stringResultValue = columnField.getInternalValue().toString().trim();
					} 
					stringFilterValue = (String)xFTextField.getInternalValue();
					//
					if (this.getBasicType().equals("INTEGER") || this.getBasicType().equals("FLOAT")) {
						if (stringFilterValue.equals("")) {
							validated = true;
						} else {
							doubleResultValue = Double.parseDouble(stringResultValue);
							String wrk = XFUtility.getStringNumber(stringFilterValue);
							if (!wrk.equals("")) {
								doubleFilterValue = Double.parseDouble(wrk);
							}
							if (operandType.equals("EQ")) {
								if (doubleResultValue == doubleFilterValue) {
									validated = true;
								}
							}
							if (operandType.equals("GE")) {
								if (doubleResultValue >= doubleFilterValue) {
									validated = true;
								}
							}
							if (operandType.equals("GT")) {
								if (doubleResultValue > doubleFilterValue) {
									validated = true;
								}
							}
							if (operandType.equals("LE")) {
								if (doubleResultValue <= doubleFilterValue) {
									validated = true;
								}
							}
							if (operandType.equals("LT")) {
								if (doubleResultValue < doubleFilterValue) {
									validated = true;
								}
							}
						}
					} else {
						if (this.getBasicType().equals("DATE") || this.getBasicType().equals("TIME") || this.getBasicType().equals("DATETIME")) {
							if (stringFilterValue.equals("")) {
								validated = true;
							} else {
								stringResultValue = XFUtility.getStringNumber(stringResultValue).replace("-", "");
								doubleResultValue = Double.parseDouble(stringResultValue);
								stringFilterValue = XFUtility.getStringNumber(stringFilterValue).replace("-", "");
								if (!stringFilterValue.equals("")) {
									doubleFilterValue = Double.parseDouble(stringFilterValue);
								}
								if (operandType.equals("EQ")) {
									if (doubleResultValue == doubleFilterValue) {
										validated = true;
									}
								}
								if (operandType.equals("GE") && !operandType.equals("GENERIC")) {
									if (doubleResultValue >= doubleFilterValue) {
										validated = true;
									}
								}
								if (operandType.equals("GT")) {
									if (doubleResultValue > doubleFilterValue) {
										validated = true;
									}
								}
								if (operandType.equals("LE")) {
									if (doubleResultValue <= doubleFilterValue) {
										validated = true;
									}
								}
								if (operandType.equals("LT")) {
									if (doubleResultValue < doubleFilterValue) {
										validated = true;
									}
								}
								if (operandType.equals("GENERIC")) {
									int lengthResultValue = stringResultValue.length();
									int lengthFieldValue = stringFilterValue.length();
									if (lengthResultValue >= lengthFieldValue) {
										String wrk = stringResultValue.substring(0, lengthFieldValue);
										if (wrk.equals(stringFilterValue)) {
											validated = true;
										}
									}
								}
							}
						} else {
							if (stringFilterValue.equals("")) {
								validated = true;
							} else {
								if (operandType.equals("EQ")) {
									if (stringResultValue.equals(stringFilterValue)) {
										validated = true;
									}
								}
								if (operandType.equals("SCAN")) {
									if (stringResultValue.contains(stringFilterValue)) {
										validated = true;
									}
								}
								if (operandType.equals("GENERIC")) {
									int lengthResultValue = stringResultValue.length();
									int lengthFieldValue = stringFilterValue.length();
									if (lengthResultValue >= lengthFieldValue) {
										String wrk = stringResultValue.substring(0, lengthFieldValue);
										if (wrk.equals(stringFilterValue)) {
											validated = true;
										}
									}
								}
							}
						}
					}
				}
				//
				if (componentType.equals("KUBUN_LIST") || componentType.equals("RECORDS_LIST")) {
					String fieldValue = (String)jComboBox.getSelectedItem();
					if (fieldValue.equals("")) {
						validated = true;
					} else {
						String resultValue = (String)columnField.getExternalValue();
						if (resultValue.equals(fieldValue)) {
							validated = true;
						} else {
							if (fieldValue.equals("NULL") && resultValue.equals("")) {
								validated = true;
							}
							if (fieldValue.equals("!NULL") && !resultValue.equals("")) {
								validated = true;
							}
						}
					}
				}
				//
				if (componentType.equals("VALUES_LIST")) {
					String fieldValue = (String)jComboBox.getSelectedItem();
					if (fieldValue.equals("")) {
						validated = true;
					} else {
						String resultValue = (String)columnField.getInternalValue();
						if (resultValue.equals(fieldValue)) {
							validated = true;
						}
					}
				}
				//
				if (componentType.equals("DATE")) {
					stringFilterValue = (String)xFDateField.getInternalValue();
					if (stringFilterValue == null) {
						validated = true;
					} else {
						if (columnField.getInternalValue() != null) {
							stringResultValue = columnField.getInternalValue().toString().trim();
							stringResultValue = XFUtility.getStringNumber(stringResultValue).replace("-", "");
						} 
						doubleResultValue = Double.parseDouble(stringResultValue);
						stringFilterValue = XFUtility.getStringNumber(stringFilterValue).replace("-", "");
						if (!stringFilterValue.equals("")) {
							doubleFilterValue = Double.parseDouble(stringFilterValue);
						}
						if (operandType.equals("EQ")) {
							if (doubleResultValue == doubleFilterValue) {
								validated = true;
							}
						}
						if (operandType.equals("GE")) {
							if (doubleResultValue >= doubleFilterValue) {
								validated = true;
							}
						}
						if (operandType.equals("GT")) {
							if (doubleResultValue > doubleFilterValue) {
								validated = true;
							}
						}
						if (operandType.equals("LE")) {
							if (doubleResultValue <= doubleFilterValue) {
								validated = true;
							}
						}
						if (operandType.equals("LT")) {
							if (doubleResultValue < doubleFilterValue) {
								validated = true;
							}
						}

					}
				}
				//
				if (componentType.equals("YMONTH")) {
					stringFilterValue = (String)xFYMonthBox.getInternalValue();
					if (stringFilterValue.equals("")) {
						validated = true;
					} else {
						if (columnField.getInternalValue() != null) {
							stringResultValue = columnField.getInternalValue().toString().trim();
							stringResultValue = XFUtility.getStringNumber(stringResultValue).replace("/", "");
						} 
						doubleResultValue = Double.parseDouble(stringResultValue);
						if (!stringFilterValue.equals("")) {
							doubleFilterValue = Double.parseDouble(stringFilterValue);
						}
						if (operandType.equals("EQ")) {
							if (doubleResultValue == doubleFilterValue) {
								validated = true;
							}
						}
						if (operandType.equals("GE") && !operandType.equals("GENERIC")) {
							if (doubleResultValue >= doubleFilterValue) {
								validated = true;
							}
						}
						if (operandType.equals("GT")) {
							if (doubleResultValue > doubleFilterValue) {
								validated = true;
							}
						}
						if (operandType.equals("LE")) {
							if (doubleResultValue <= doubleFilterValue) {
								validated = true;
							}
						}
						if (operandType.equals("LT")) {
							if (doubleResultValue < doubleFilterValue) {
								validated = true;
							}
						}
						if (operandType.equals("GENERIC")) {
							int lengthResultValue = stringResultValue.length();
							int lengthFieldValue = stringFilterValue.length();
							if (lengthResultValue >= lengthFieldValue) {
								String wrk = stringResultValue.substring(0, lengthFieldValue);
								if (wrk.equals(stringFilterValue)) {
									validated = true;
								}
							}
						}

					}
				}
				//
				if (componentType.equals("MSEQ")) {
					stringFilterValue = (String)xFMSeqBox.getInternalValue();
					if (stringFilterValue.equals("0")) {
						validated = true;
					} else {
						if (columnField.getInternalValue() != null) {
							stringResultValue = columnField.getInternalValue().toString().trim();
							stringResultValue = XFUtility.getStringNumber(stringResultValue).replace("/", "");
						} 
						doubleResultValue = Double.parseDouble(stringResultValue);
						if (!stringFilterValue.equals("")) {
							doubleFilterValue = Double.parseDouble(stringFilterValue);
						}
						if (operandType.equals("EQ")) {
							if (doubleResultValue == doubleFilterValue) {
								validated = true;
							}
						}
						if (operandType.equals("GE") && !operandType.equals("GENERIC")) {
							if (doubleResultValue >= doubleFilterValue) {
								validated = true;
							}
						}
						if (operandType.equals("GT")) {
							if (doubleResultValue > doubleFilterValue) {
								validated = true;
							}
						}
						if (operandType.equals("LE")) {
							if (doubleResultValue <= doubleFilterValue) {
								validated = true;
							}
						}
						if (operandType.equals("LT")) {
							if (doubleResultValue < doubleFilterValue) {
								validated = true;
							}
						}
						if (operandType.equals("GENERIC")) {
							int lengthResultValue = stringResultValue.length();
							int lengthFieldValue = stringFilterValue.length();
							if (lengthResultValue >= lengthFieldValue) {
								String wrk = stringResultValue.substring(0, lengthFieldValue);
								if (wrk.equals(stringFilterValue)) {
									validated = true;
								}
							}
						}

					}
				}
				//
				if (componentType.equals("FYEAR")) {
					stringFilterValue = (String)xFFYearBox.getInternalValue();
					if (stringFilterValue.equals("0")) {
						validated = true;
					} else {
						if (columnField.getInternalValue() != null) {
							stringResultValue = columnField.getInternalValue().toString().trim();
							stringResultValue = XFUtility.getStringNumber(stringResultValue).replace("/", "");
						} 
						doubleResultValue = Double.parseDouble(stringResultValue);
						if (!stringFilterValue.equals("")) {
							doubleFilterValue = Double.parseDouble(stringFilterValue);
						}
						if (operandType.equals("EQ")) {
							if (doubleResultValue == doubleFilterValue) {
								validated = true;
							}
						}
						if (operandType.equals("GE") && !operandType.equals("GENERIC")) {
							if (doubleResultValue >= doubleFilterValue) {
								validated = true;
							}
						}
						if (operandType.equals("GT")) {
							if (doubleResultValue > doubleFilterValue) {
								validated = true;
							}
						}
						if (operandType.equals("LE")) {
							if (doubleResultValue <= doubleFilterValue) {
								validated = true;
							}
						}
						if (operandType.equals("LT")) {
							if (doubleResultValue < doubleFilterValue) {
								validated = true;
							}
						}
						if (operandType.equals("GENERIC")) {
							int lengthResultValue = stringResultValue.length();
							int lengthFieldValue = stringFilterValue.length();
							if (lengthResultValue >= lengthFieldValue) {
								String wrk = stringResultValue.substring(0, lengthFieldValue);
								if (wrk.equals(stringFilterValue)) {
									validated = true;
								}
							}
						}

					}
				}
				//
				if (componentType.equals("PROMPT_CALL")) {
					if (columnField.getInternalValue() != null) {
						stringResultValue = columnField.getInternalValue().toString().trim();
					} 
					stringFilterValue = (String)xFPromptCall.getInternalValue();
					//
					if (this.getBasicType().equals("INTEGER") || this.getBasicType().equals("FLOAT")) {
						if (stringFilterValue.equals("")) {
							validated = true;
						} else {
							doubleResultValue = Double.parseDouble(stringResultValue);
							String wrk = XFUtility.getStringNumber(stringFilterValue);
							if (!wrk.equals("")) {
								doubleFilterValue = Double.parseDouble(wrk);
							}
							if (operandType.equals("EQ")) {
								if (doubleResultValue == doubleFilterValue) {
									validated = true;
								}
							}
							if (operandType.equals("GE")) {
								if (doubleResultValue >= doubleFilterValue) {
									validated = true;
								}
							}
							if (operandType.equals("GT")) {
								if (doubleResultValue > doubleFilterValue) {
									validated = true;
								}
							}
							if (operandType.equals("LE")) {
								if (doubleResultValue <= doubleFilterValue) {
									validated = true;
								}
							}
							if (operandType.equals("LT")) {
								if (doubleResultValue < doubleFilterValue) {
									validated = true;
								}
							}
						}
					} else {
						if (this.getBasicType().equals("DATE") || this.getBasicType().equals("TIME") || this.getBasicType().equals("DATETIME")) {
							if (stringFilterValue.equals("")) {
								validated = true;
							} else {
								stringResultValue = XFUtility.getStringNumber(stringResultValue).replace("-", "");
								doubleResultValue = Double.parseDouble(stringResultValue);
								stringFilterValue = XFUtility.getStringNumber(stringFilterValue).replace("-", "");
								if (!stringFilterValue.equals("")) {
									doubleFilterValue = Double.parseDouble(stringFilterValue);
								}
								if (operandType.equals("EQ")) {
									if (doubleResultValue == doubleFilterValue) {
										validated = true;
									}
								}
								if (operandType.equals("GE") && !operandType.equals("GENERIC")) {
									if (doubleResultValue >= doubleFilterValue) {
										validated = true;
									}
								}
								if (operandType.equals("GT")) {
									if (doubleResultValue > doubleFilterValue) {
										validated = true;
									}
								}
								if (operandType.equals("LE")) {
									if (doubleResultValue <= doubleFilterValue) {
										validated = true;
									}
								}
								if (operandType.equals("LT")) {
									if (doubleResultValue < doubleFilterValue) {
										validated = true;
									}
								}
								if (operandType.equals("GENERIC")) {
									int lengthResultValue = stringResultValue.length();
									int lengthFieldValue = stringFilterValue.length();
									if (lengthResultValue >= lengthFieldValue) {
										String wrk = stringResultValue.substring(0, lengthFieldValue);
										if (wrk.equals(stringFilterValue)) {
											validated = true;
										}
									}
								}
							}
						} else {
							if (stringFilterValue.equals("")) {
								validated = true;
							} else {
								if (operandType.equals("EQ")) {
									if (stringResultValue.equals(stringFilterValue)) {
										validated = true;
									}
								}
								if (operandType.equals("SCAN")) {
									if (stringResultValue.contains(stringFilterValue)) {
										validated = true;
									}
								}
								if (operandType.equals("GENERIC")) {
									int lengthResultValue = stringResultValue.length();
									int lengthFieldValue = stringFilterValue.length();
									if (lengthResultValue >= lengthFieldValue) {
										String wrk = stringResultValue.substring(0, lengthFieldValue);
										if (wrk.equals(stringFilterValue)) {
											validated = true;
										}
									}
								}
							}
						}
					}
				}
			} else {
				validated = true;
			}
		}
		//
		return validated;
	}
}

class XF300_PromptCallField extends JPanel implements XFEditableField {
	private static final long serialVersionUID = 1L;
	private static ResourceBundle res = ResourceBundle.getBundle("xeadDriver.Res");
	private String tableID = "";
	private String tableAlias = "";
	private String fieldID = "";
	private int rows_ = 1;
	private XFTextField xFTextField;
	private JButton jButton = new JButton();
	private boolean isEditable = true;
    private XF300 dialog_;
    private org.w3c.dom.Element fieldElement_;
    private org.w3c.dom.Element functionElement_;
    private ArrayList<XF300_DetailReferTable> referTableList_;
    private String oldValue = "";
    //private Color normalColor;
    private ArrayList<String> fieldsToPutList_ = new ArrayList<String>();
    private ArrayList<String> fieldsToPutToList_ = new ArrayList<String>();
    private ArrayList<String> fieldsToGetList_ = new ArrayList<String>();
    private ArrayList<String> fieldsToGetToList_ = new ArrayList<String>();
    private int index_;

	public XF300_PromptCallField(org.w3c.dom.Element fieldElement, org.w3c.dom.Element functionElement, XF300 dialog, int index){
		//
		super();
		//
		fieldElement_ = fieldElement;
		functionElement_ = functionElement;
		dialog_ = dialog;
		index_ = index;
		//
		String fieldOptions = fieldElement_.getAttribute("FieldOptions");
		StringTokenizer workTokenizer = new StringTokenizer(fieldElement_.getAttribute("DataSource"), "." );
		tableAlias = workTokenizer.nextToken();
		fieldID =workTokenizer.nextToken();
		//
		tableID = tableAlias;
		referTableList_ = dialog_.getDetailReferTableList(index);
		for (int i = 0; i < referTableList_.size(); i++) {
			if (referTableList_.get(i).getTableAlias().equals(tableAlias)) {
				tableID = referTableList_.get(i).getTableID();
				break;
			}
		}
		//
		org.w3c.dom.Element workElement = dialog_.getSession().getFieldElement(tableID, fieldID);
		if (workElement == null) {
			JOptionPane.showMessageDialog(this, tableID + "." + fieldID + res.getString("FunctionError11"));
		}
		String dataType = workElement.getAttribute("Type");
		String dataTypeOptions = workElement.getAttribute("TypeOptions");
		int dataSize = Integer.parseInt(workElement.getAttribute("Size"));
		if (dataSize > 50) {
			dataSize = 50;
		}
		int decimalSize = 0;
		if (!workElement.getAttribute("Decimal").equals("")) {
			decimalSize = Integer.parseInt(workElement.getAttribute("Decimal"));
		}
		//
		xFTextField = new XFTextField(XFUtility.getBasicTypeOf(dataType), dataSize, decimalSize, dataTypeOptions, fieldOptions);
		xFTextField.setLocation(5, 0);
		xFTextField.setEditable(false);
		xFTextField.addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent event) {
				xFTextField.selectAll();
			} 
			public void focusLost(FocusEvent event) {
				xFTextField.select(0, 0);
			} 
		});
		xFTextField.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent event) {
				if (isEditable && event.getKeyCode() == KeyEvent.VK_DELETE) {
					xFTextField.setText("");
					xFTextField.setFocusable(false);
				}
			} 
		});
		//
		String wrkStr = XFUtility.getOptionValueWithKeyword(fieldOptions, "PROMPT_CALL_TO_PUT");
		if (!wrkStr.equals("")) {
			workTokenizer = new StringTokenizer(wrkStr, ";" );
			while (workTokenizer.hasMoreTokens()) {
				fieldsToPutList_.add(workTokenizer.nextToken());
			}
		}
		wrkStr = XFUtility.getOptionValueWithKeyword(fieldOptions, "PROMPT_CALL_TO_PUT_TO");
		if (!wrkStr.equals("")) {
			workTokenizer = new StringTokenizer(wrkStr, ";" );
			while (workTokenizer.hasMoreTokens()) {
				fieldsToPutToList_.add(workTokenizer.nextToken());
			}
		}
		wrkStr = XFUtility.getOptionValueWithKeyword(fieldOptions, "PROMPT_CALL_TO_GET");
		if (!wrkStr.equals("")) {
			workTokenizer = new StringTokenizer(wrkStr, ";" );
			while (workTokenizer.hasMoreTokens()) {
				fieldsToGetList_.add(workTokenizer.nextToken());
			}
		}
		wrkStr = XFUtility.getOptionValueWithKeyword(fieldOptions, "PROMPT_CALL_TO_GET_TO");
		if (!wrkStr.equals("")) {
			workTokenizer = new StringTokenizer(wrkStr, ";" );
			while (workTokenizer.hasMoreTokens()) {
				fieldsToGetToList_.add(workTokenizer.nextToken());
			}
		}
		//
		jButton.setText("...");
		jButton.setFont(new java.awt.Font("Dialog", 0, 11));
		jButton.setPreferredSize(new Dimension(26, XFUtility.FIELD_UNIT_HEIGHT));
		jButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Object value;
				//
				HashMap<String, Object> fieldValuesMap = new HashMap<String, Object>();
				for (int i = 0; i < fieldsToPutList_.size(); i++) {
					value = dialog_.getFilterObjectByName(index_, fieldsToPutList_.get(i));
					if (value != null) {
						fieldValuesMap.put(fieldsToPutToList_.get(i), value);
					}
				}
				//
				HashMap<String, Object> returnMap = dialog_.getSession().getFunction().execute(functionElement_, fieldValuesMap);
				if (!returnMap.get("RETURN_CODE").equals("99")) {
					//
					HashMap<String, Object> fieldsToGetMap = new HashMap<String, Object>();
					for (int i = 0; i < fieldsToGetList_.size(); i++) {
						value = returnMap.get(fieldsToGetList_.get(i));
						if (value != null) {
							fieldsToGetMap.put(fieldsToGetToList_.get(i), value);
						}
					}
					for (int i = 0; i < dialog_.getFilterList(index_).size(); i++) {
						value = fieldsToGetMap.get(dialog_.getFilterList(index_).get(i).getDataSourceName());
						if (value != null && dialog_.getFilterList(index_).get(i).isEditable()) {
							dialog_.getFilterList(index_).get(i).setValue(value);
						}
					}
					//
					if (!xFTextField.getText().equals("")) {
						xFTextField.setFocusable(true);
					}
				}
			}
		});
		jButton.addKeyListener(new java.awt.event.KeyAdapter() {
			public void keyPressed(KeyEvent e)  {
			    if ((e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0){
					if (e.getKeyCode() == KeyEvent.VK_ENTER) {
						jButton.doClick();
					}
				}
			}
		});
		//
		int fieldWidth = (int)dialog_.getFilterWidth(index_) - 100 - 27;
		if (fieldWidth < xFTextField.getWidth()) {
			this.setSize(new Dimension(fieldWidth + 27, XFUtility.FIELD_UNIT_HEIGHT));
		} else {
			this.setSize(new Dimension(xFTextField.getWidth() + 27, XFUtility.FIELD_UNIT_HEIGHT));
		}
		this.setLayout(new BorderLayout());
		this.add(xFTextField, BorderLayout.CENTER);
		this.add(jButton, BorderLayout.EAST);
	}

	public void setEditable(boolean editable) {
		jButton.setEnabled(editable);
		isEditable = editable;
	}

	public void requestFocus() {
		xFTextField.requestFocus();
	}

	public Object getInternalValue() {
		return xFTextField.getText();
	}

	public Object getExternalValue() {
		return xFTextField.getText();
	}
	
	public void setOldValue(Object obj) {
		oldValue = (String)obj;
	}

	public Object getOldValue() {
		return oldValue;
	}

	public String getValueClass() {
		return "String";
	}
	
	public void setValue(Object obj) {
		if (obj == null) {
			xFTextField.setText("");
		} else {
			xFTextField.setText(obj.toString());
		}
	}

	public void setBackground(Color color) {
		if (xFTextField != null) {
			if (color.equals(XFUtility.ACTIVE_COLOR)) {
				//xFTextField.setBackground(normalColor);
				if (xFTextField.isEditable()) {
					xFTextField.setBackground(XFUtility.ACTIVE_COLOR);
				} else {
					xFTextField.setBackground(XFUtility.INACTIVE_COLOR);
				}
			}
//			if (color.equals(XFUtility.ERROR_COLOR)) {
//				xFTextField.setBackground(XFUtility.ERROR_COLOR);
//			}
		}
	}

	public int getRows() {
		return rows_;
	}

	public boolean isEditable() {
		return isEditable;
	}
}


class XF300_HeaderTable extends Object {
	private static final long serialVersionUID = 1L;
	private org.w3c.dom.Element tableElement = null;
	private org.w3c.dom.Element functionElement_ = null;
	private String tableID = "";
	private String activeWhere = "";
	private String fixedWhere = "";
	private ArrayList<String> keyFieldIDList = new ArrayList<String>();
	private ArrayList<XFScript> scriptList = new ArrayList<XFScript>();
	private XF300 dialog_;
	private StringTokenizer workTokenizer;

	public XF300_HeaderTable(org.w3c.dom.Element functionElement, XF300 dialog){
		super();
		//
		functionElement_ = functionElement;
		dialog_ = dialog;
		//
		tableID = functionElement_.getAttribute("HeaderTable");
		tableElement = dialog_.getSession().getTableElement(tableID);
		activeWhere = tableElement.getAttribute("ActiveWhere");
		fixedWhere = functionElement_.getAttribute("FixedWhere");
		//
		String workString;
		org.w3c.dom.Element workElement;
		//
		if (functionElement_.getAttribute("HeaderKeyFields").equals("")) {
			NodeList nodeList = tableElement.getElementsByTagName("Key");
			for (int i = 0; i < nodeList.getLength(); i++) {
				workElement = (org.w3c.dom.Element)nodeList.item(i);
				if (workElement.getAttribute("Type").equals("PK")) {
					workTokenizer = new StringTokenizer(workElement.getAttribute("Fields"), ";" );
					while (workTokenizer.hasMoreTokens()) {
						workString = workTokenizer.nextToken();
						keyFieldIDList.add(workString);
					}
					break;
				}
			}
		} else {
			workTokenizer = new StringTokenizer(functionElement_.getAttribute("HeaderKeyFields"), ";" );
			while (workTokenizer.hasMoreTokens()) {
				keyFieldIDList.add(workTokenizer.nextToken());
			}
		}
		//
		org.w3c.dom.Element element;
		NodeList workList = tableElement.getElementsByTagName("Script");
		SortableDomElementListModel sortList = XFUtility.getSortedListModel(workList, "Order");
		for (int i = 0; i < sortList.size(); i++) {
	        element = (org.w3c.dom.Element)sortList.getElementAt(i);
	        scriptList.add(new XFScript(tableID, element));
		}
	}

	public String getSelectSQL(){
		int count;
		StringBuffer buf = new StringBuffer();
		//
		buf.append("select ");
		//
		count = -1;
		for (int i = 0; i < dialog_.getHeaderFieldList().size(); i++) {
			if (dialog_.getHeaderFieldList().get(i).isFieldOnPrimaryTable() && !dialog_.getHeaderFieldList().get(i).isVirtualField()) {
				count++;
				if (count > 0) {
					buf.append(",");
				}
				buf.append(dialog_.getHeaderFieldList().get(i).getFieldID());
			}
		}
		//
		buf.append(" from ");
		buf.append(tableID);
		//
		buf.append(" where ") ;
		count = -1;
		for (int i = 0; i < dialog_.getHeaderFieldList().size(); i++) {
			if (dialog_.getHeaderFieldList().get(i).isKey()) {
				count++;
				if (count > 0) {
					buf.append(" and ") ;
				}
				buf.append(dialog_.getHeaderFieldList().get(i).getFieldID()) ;
				buf.append("=") ;
				//if (dialog_.getHeaderFieldList().get(i).isLiteralRequired()) {
				if (XFUtility.isLiteralRequiredBasicType(dialog_.getHeaderFieldList().get(i).getBasicType())) {
					buf.append("'") ;
					buf.append(dialog_.getParmMap().get(dialog_.getHeaderFieldList().get(i).getFieldID()));
					buf.append("'") ;
				} else {
					buf.append(dialog_.getParmMap().get(dialog_.getHeaderFieldList().get(i).getFieldID()));
				}
			}
		}
		//
		if (!activeWhere.equals("")) {
			buf.append(" and (");
			buf.append(activeWhere);
			buf.append(") ");
		}
		//
		if (!fixedWhere.equals("")) {
			buf.append(" and (");
			buf.append(fixedWhere);
			buf.append(") ");
		}
		//
		return buf.toString();
	}

	public String getTableID(){
		return tableID;
	}

	public org.w3c.dom.Element getTableElement(){
		return tableElement;
	}
	
	public ArrayList<String> getKeyFieldIDList(){
		return keyFieldIDList;
	}
	
	public ArrayList<XFScript> getScriptList(){
		return scriptList;
	}
	
	public boolean isValidDataSource(String tableID, String tableAlias, String fieldID) {
		boolean isValid = false;
		XF300_HeaderReferTable referTable;
		org.w3c.dom.Element workElement;
		//
		if (this.getTableID().equals(tableID) && this.getTableID().equals(tableAlias)) {
			NodeList nodeList = tableElement.getElementsByTagName("Field");
			for (int i = 0; i < nodeList.getLength(); i++) {
				workElement = (org.w3c.dom.Element)nodeList.item(i);
				if (workElement.getAttribute("ID").equals(fieldID)) {
					isValid = true;
					break;
				}
			}
		} else {
			for (int i = 0; i < dialog_.getHeaderReferTableList().size(); i++) {
				referTable = dialog_.getHeaderReferTableList().get(i);
				if (referTable.getTableID().equals(tableID) && referTable.getTableAlias().equals(tableAlias)) {
					for (int j = 0; j < referTable.getFieldIDList().size(); j++) {
						if (referTable.getFieldIDList().get(j).equals(fieldID)) {
							isValid = true;
							break;
						}
					}
				}
				if (isValid) {
					break;
				}
			}
		}
		//
		return isValid;
	}

	public void runScript(String event1, String event2) throws ScriptException  {
		XFScript script;
		ArrayList<XFScript> validScriptList = new ArrayList<XFScript>();
		//
		for (int i = 0; i < scriptList.size(); i++) {
			script = scriptList.get(i);
			if (script.isToBeRunAtEvent(event1, event2)) {
				validScriptList.add(script);
			}
		}
		//
		for (int i = 0; i < validScriptList.size(); i++) {
			dialog_.evalHeaderTableScript(validScriptList.get(i).getName(), validScriptList.get(i).getScriptText());
		}
	}
}

class XF300_HeaderReferTable extends Object {
	private static final long serialVersionUID = 1L;
	private org.w3c.dom.Element referElement_ = null;
	private org.w3c.dom.Element tableElement = null;
	private XF300 dialog_ = null;
	private String tableID = "";
	private String tableAlias = "";
	private String activeWhere = "";
	private ArrayList<String> fieldIDList = new ArrayList<String>();
	private ArrayList<String> toKeyFieldIDList = new ArrayList<String>();
	private ArrayList<String> withKeyFieldIDList = new ArrayList<String>();
	private ArrayList<String> orderByFieldIDList = new ArrayList<String>();
	private boolean isToBeExecuted = false;
	private int rangeKeyType = 0;
	private String rangeKeyFieldValid = "";
	private String rangeKeyFieldExpire = "";
	private String rangeKeyFieldSearch = "";
	private boolean rangeValidated;
	//
	public XF300_HeaderReferTable(org.w3c.dom.Element referElement, XF300 dialog){
		super();
		//
		referElement_ = referElement;
		dialog_ = dialog;
		//
		tableID = referElement_.getAttribute("ToTable");
		tableElement = dialog_.getSession().getTableElement(tableID);
		//
		StringTokenizer workTokenizer;
		String wrkStr = tableElement.getAttribute("RangeKey");
		if (!wrkStr.equals("")) {
			workTokenizer = new StringTokenizer(wrkStr, ";" );
			rangeKeyFieldValid =workTokenizer.nextToken();
			rangeKeyFieldExpire =workTokenizer.nextToken();
			org.w3c.dom.Element workElement = dialog_.getSession().getFieldElement(tableID, rangeKeyFieldExpire);
			if (XFUtility.getOptionList(workElement.getAttribute("TypeOptions")).contains("VIRTUAL")) {
				rangeKeyType = 1;
			} else {
				rangeKeyType = 2;
			}
		}
		//
		activeWhere = tableElement.getAttribute("ActiveWhere");
		//
		tableAlias = referElement_.getAttribute("TableAlias");
		if (tableAlias.equals("")) {
			tableAlias = tableID;
		}
		//
		workTokenizer = new StringTokenizer(referElement_.getAttribute("Fields"), ";" );
		while (workTokenizer.hasMoreTokens()) {
			fieldIDList.add(workTokenizer.nextToken());
		}
		//
		if (referElement_.getAttribute("ToKeyFields").equals("")) {
			org.w3c.dom.Element workElement = dialog.getSession().getTablePKElement(tableID);
			workTokenizer = new StringTokenizer(workElement.getAttribute("Fields"), ";" );
			while (workTokenizer.hasMoreTokens()) {
				toKeyFieldIDList.add(workTokenizer.nextToken());
			}
		} else {
			workTokenizer = new StringTokenizer(referElement_.getAttribute("ToKeyFields"), ";" );
			while (workTokenizer.hasMoreTokens()) {
				toKeyFieldIDList.add(workTokenizer.nextToken());
			}
		}
		//
		workTokenizer = new StringTokenizer(referElement_.getAttribute("WithKeyFields"), ";" );
		while (workTokenizer.hasMoreTokens()) {
			withKeyFieldIDList.add(workTokenizer.nextToken());
		}
		//
		workTokenizer = new StringTokenizer(referElement_.getAttribute("OrderBy"), ";" );
		while (workTokenizer.hasMoreTokens()) {
			orderByFieldIDList.add(workTokenizer.nextToken());
		}
	}

	public String getSelectSQL(){
		int count;
		org.w3c.dom.Element workElement;
		StringBuffer buf = new StringBuffer();
		//
		buf.append("select ");
		//
		count = 0;
		for (int i = 0; i < fieldIDList.size(); i++) {
			workElement = dialog_.getSession().getFieldElement(tableID, fieldIDList.get(i));
			if (!XFUtility.getOptionList(workElement.getAttribute("TypeOptions")).contains("VIRTUAL")) {
				if (count > 0) {
					buf.append(",");
				}
				count++;
				buf.append(fieldIDList.get(i));
			}
		}
		if (count == 0) {
			for (int i = 0; i < toKeyFieldIDList.size(); i++) {
				if (count > 0) {
					buf.append(",");
				}
				count++;
				buf.append(toKeyFieldIDList.get(i));
			}
		}
		if (!rangeKeyFieldValid.equals("")) {
			if (count > 0) {
				buf.append(",");
			}
			buf.append(rangeKeyFieldValid);
			//
			workElement = dialog_.getSession().getFieldElement(tableID, rangeKeyFieldExpire);
			if (!XFUtility.getOptionList(workElement.getAttribute("TypeOptions")).contains("VIRTUAL")) {
				buf.append(",");
				buf.append(rangeKeyFieldExpire);
			}
		}
		//
		buf.append(" from ");
		buf.append(tableID);
		buf.append(" where ");
		//
		count = 0;
		for (int i = 0; i < toKeyFieldIDList.size(); i++) {
			if (toKeyFieldIDList.get(i).equals(rangeKeyFieldValid)) {
				rangeKeyFieldSearch = withKeyFieldIDList.get(i);
			} else {
				if (count > 0) {
					buf.append(" and ");
				}
				buf.append(toKeyFieldIDList.get(i));
				buf.append("=");
				for (int j = 0; j < dialog_.getHeaderFieldList().size(); j++) {
					if (withKeyFieldIDList.get(i).equals(dialog_.getHeaderFieldList().get(j).getTableAlias() + "." + dialog_.getHeaderFieldList().get(j).getFieldID())) {
						//if (dialog_.getHeaderFieldList().get(j).isLiteralRequired()) {
						if (XFUtility.isLiteralRequiredBasicType(dialog_.getHeaderFieldList().get(j).getBasicType())) {
							buf.append("'") ;
							buf.append(dialog_.getHeaderFieldList().get(j).getInternalValue());
							buf.append("'") ;
						} else {
							buf.append(dialog_.getHeaderFieldList().get(j).getInternalValue());
						}
						break;
					}
				}
				count++;
			}
		}
		//
		if (!activeWhere.equals("")) {
			buf.append(" and ");
			buf.append(activeWhere);
		}
		//
		if (this.rangeKeyType != 0) {
			buf.append(" order by ");
			buf.append(rangeKeyFieldValid);
			buf.append(" DESC ");
		} else {
			if (orderByFieldIDList.size() > 0) {
				int pos0,pos1;
				buf.append(" order by ");
				for (int i = 0; i < orderByFieldIDList.size(); i++) {
					if (i > 0) {
						buf.append(",");
					}
					pos0 = orderByFieldIDList.get(i).indexOf(".");
					pos1 = orderByFieldIDList.get(i).indexOf("(A)");
					if (pos1 >= 0) {
						buf.append(orderByFieldIDList.get(i).substring(pos0+1, pos1));
					} else {
						pos1 = orderByFieldIDList.get(i).indexOf("(D)");
						if (pos1 >= 0) {
							buf.append(orderByFieldIDList.get(i).substring(pos0+1, pos1));
							buf.append(" DESC ");
						} else {
							buf.append(orderByFieldIDList.get(i).substring(pos0+1, orderByFieldIDList.get(i).length()));
						}
					}
				}
			}
		}
		//
		rangeValidated = false;
		//
		return buf.toString();
	}

	public String getTableID(){
		return tableID;
	}
	
	public String getTableAlias(){
		return tableAlias;
	}
	
	public ArrayList<String> getWithKeyFieldIDList(){
		return withKeyFieldIDList;
	}

	public ArrayList<String> getFieldIDList(){
		return  fieldIDList;
	}
	
	public void setToBeExecuted(boolean executed){
		isToBeExecuted = executed;
	}
	
	public boolean isToBeExecuted(){
		return isToBeExecuted;
	}
	
	public boolean isRecordToBeSelected(ResultSet result){
		boolean returnValue = false;
		//
		if (rangeKeyType == 0) {
			returnValue = true;
		}
		//
		if (rangeKeyType == 1) {
			try {
				if (!rangeValidated) { 
					// Note that result set is ordered by rangeKeyFieldValue DESC //
					StringTokenizer workTokenizer = new StringTokenizer(rangeKeyFieldSearch, "." );
					String workTableAlias = workTokenizer.nextToken();
					String workFieldID = workTokenizer.nextToken();
					Object valueKey = dialog_.getHeaderFieldObjectByID("", workTableAlias, workFieldID).getInternalValue();
					Object valueFrom = result.getObject(rangeKeyFieldValid);
					int comp1 = valueKey.toString().compareTo(valueFrom.toString());
					if (comp1 >= 0) {
						returnValue = true;
						rangeValidated = true;
					}
				}
			} catch (SQLException e) {
				e.printStackTrace(dialog_.getExceptionStream());
				dialog_.setErrorAndCloseFunction();
			}
		}
		//
		if (rangeKeyType == 2) {
			try {
				StringTokenizer workTokenizer = new StringTokenizer(rangeKeyFieldSearch, "." );
				String workTableAlias = workTokenizer.nextToken();
				String workFieldID = workTokenizer.nextToken();
				Object valueKey = dialog_.getHeaderFieldObjectByID("", workTableAlias, workFieldID).getInternalValue();
				Object valueFrom = result.getObject(rangeKeyFieldValid);
				Object valueThru = result.getObject(rangeKeyFieldExpire);
				int comp1 = valueKey.toString().compareTo(valueFrom.toString());
				int comp2 = valueKey.toString().compareTo(valueThru.toString());
				if (comp1 >= 0 && comp2 < 0) {
					returnValue = true;
				}
			} catch (SQLException e) {
				e.printStackTrace(dialog_.getExceptionStream());
				dialog_.setErrorAndCloseFunction();
			}
		}
		//
		return returnValue;
	}
}

class XF300_DetailTable extends Object {
	private static final long serialVersionUID = 1L;
	private org.w3c.dom.Element tableElement = null;
	private org.w3c.dom.Element detailTableElement_ = null;
	private String tableID = "";
	private String activeWhere = "";
	private String fixedWhere = "";
	private ArrayList<String> headerKeyFieldIDList = new ArrayList<String>();
	private ArrayList<String> keyFieldIDList = new ArrayList<String>();
	private ArrayList<String> orderByFieldIDList = new ArrayList<String>();
	private ArrayList<XFScript> scriptList = new ArrayList<XFScript>();
	private XF300 dialog_;
	private int tabIndex_;
	private StringTokenizer workTokenizer;
	private boolean hasOrderByAsItsOwnFields = true;

	public XF300_DetailTable(org.w3c.dom.Element detailTableElement, int tabIndex, XF300 dialog){
		super();
		//
		detailTableElement_ = detailTableElement;
		tabIndex_ = tabIndex;
		dialog_ = dialog;
		//
		tableID = detailTableElement.getAttribute("Table");
		tableElement = dialog_.getSession().getTableElement(tableID);
		activeWhere = tableElement.getAttribute("ActiveWhere");
		fixedWhere = detailTableElement.getAttribute("FixedWhere");
		//
		int pos1;
		String wrkStr1, wrkStr2;
		org.w3c.dom.Element workElement;
		//
		if (detailTableElement_.getAttribute("HeaderKeyFields").equals("")) {
			org.w3c.dom.Element headerTableElement = dialog_.getSession().getTableElement(dialog_.getHeaderTable().getTableID());
			NodeList nodeList = headerTableElement.getElementsByTagName("Key");
			for (int i = 0; i < nodeList.getLength(); i++) {
				workElement = (org.w3c.dom.Element)nodeList.item(i);
				if (workElement.getAttribute("Type").equals("PK")) {
					workTokenizer = new StringTokenizer(workElement.getAttribute("Fields"), ";" );
					while (workTokenizer.hasMoreTokens()) {
						wrkStr1 = workTokenizer.nextToken();
						headerKeyFieldIDList.add(wrkStr1);
					}
					break;
				}
			}
		} else {
			workTokenizer = new StringTokenizer(detailTableElement_.getAttribute("HeaderKeyFields"), ";" );
			while (workTokenizer.hasMoreTokens()) {
				headerKeyFieldIDList.add(workTokenizer.nextToken());
			}
		}
		//
		if (detailTableElement_.getAttribute("KeyFields").equals("")) {
			NodeList nodeList = tableElement.getElementsByTagName("Key");
			for (int i = 0; i < nodeList.getLength(); i++) {
				workElement = (org.w3c.dom.Element)nodeList.item(i);
				if (workElement.getAttribute("Type").equals("PK")) {
					workTokenizer = new StringTokenizer(workElement.getAttribute("Fields"), ";" );
					while (workTokenizer.hasMoreTokens()) {
						wrkStr1 = workTokenizer.nextToken();
						keyFieldIDList.add(wrkStr1);
					}
					break;
				}
			}
		} else {
			workTokenizer = new StringTokenizer(detailTableElement_.getAttribute("KeyFields"), ";" );
			while (workTokenizer.hasMoreTokens()) {
				keyFieldIDList.add(workTokenizer.nextToken());
			}
		}
		//
		workTokenizer = new StringTokenizer(detailTableElement_.getAttribute("OrderBy"), ";" );
		while (workTokenizer.hasMoreTokens()) {
			wrkStr1 = workTokenizer.nextToken();
			//
			pos1 = wrkStr1.indexOf(".");
			if (pos1 > -1) { 
				wrkStr2 = wrkStr1.substring(0, pos1);
				if (!wrkStr2.equals(this.tableID)) {
					hasOrderByAsItsOwnFields = false;
				}
			}
			//
			orderByFieldIDList.add(wrkStr1);
		}
		//
		org.w3c.dom.Element element;
		NodeList workList = tableElement.getElementsByTagName("Script");
		SortableDomElementListModel sortList = XFUtility.getSortedListModel(workList, "Order");
		for (int i = 0; i < sortList.size(); i++) {
	        element = (org.w3c.dom.Element)sortList.getElementAt(i);
	        scriptList.add(new XFScript(tableID, element));
		}
	}
	
	public String getSelectSQL(){
		int count;
		StringBuffer buf = new StringBuffer();
		XF300_HeaderField headerField;
		//
		buf.append("select ");
		//
		count = -1;
		for (int i = 0; i < keyFieldIDList.size(); i++) {
			count++;
			if (count > 0) {
				buf.append(",");
			}
			buf.append(keyFieldIDList.get(i));
		}
		for (int i = 0; i < dialog_.getDetailColumnList(tabIndex_).size(); i++) {
			if (dialog_.getDetailColumnList(tabIndex_).get(i).getTableID().equals(tableID) && !dialog_.getDetailColumnList(tabIndex_).get(i).isVirtualField()) {
				count++;
				if (count > 0) {
					buf.append(",");
				}
				buf.append(dialog_.getDetailColumnList(tabIndex_).get(i).getFieldID());
			}
		}
		//
		buf.append(" from ");
		buf.append(tableID);
		//
		buf.append(" where ") ;
		count = -1;
//		for (int i = 0; i < dialog_.getHeaderTable().getKeyFieldIDList().size(); i++) {
//			count++;
//			if (count > 0) {
//				buf.append(" and ") ;
//			}
//			buf.append(keyFieldIDList.get(i)) ;
//			buf.append("=") ;
//			headerField = dialog_.getHeaderFieldObjectByID(dialog_.getHeaderTable().getTableID(), "", dialog_.getHeaderTable().getKeyFieldIDList().get(i));
//			if (XFUtility.isLiteralRequiredBasicType(headerField.getBasicType())) {
//				buf.append("'");
//				buf.append(dialog_.getParmMap().get(dialog_.getHeaderTable().getKeyFieldIDList().get(i)));
//				buf.append("'");
//			} else {
//				buf.append(dialog_.getParmMap().get(dialog_.getHeaderTable().getKeyFieldIDList().get(i)));
//			}
//		}
		for (int i = 0; i < headerKeyFieldIDList.size(); i++) {
			count++;
			if (count > 0) {
				buf.append(" and ") ;
			}
			buf.append(headerKeyFieldIDList.get(i)) ;
			buf.append("=") ;
			headerField = dialog_.getHeaderFieldObjectByID(dialog_.getHeaderTable().getTableID(), "", headerKeyFieldIDList.get(i));
			if (XFUtility.isLiteralRequiredBasicType(headerField.getBasicType())) {
				buf.append("'");
				buf.append(headerField.getInternalValue());
				buf.append("'");
			} else {
				buf.append(headerField.getInternalValue());
			}
		}
		//
		if (!activeWhere.equals("")) {
			buf.append(" and (");
			buf.append(activeWhere);
			buf.append(") ");
		}
		//
		if (!fixedWhere.equals("")) {
			buf.append(" and (");
			buf.append(fixedWhere);
			buf.append(") ");
		}
		//
		if (this.hasOrderByAsItsOwnFields) {
			if (orderByFieldIDList.size() > 0) {
				int pos0,pos1;
				buf.append(" order by ");
				for (int i = 0; i < orderByFieldIDList.size(); i++) {
					if (i > 0) {
						buf.append(",");
					}
					pos0 = orderByFieldIDList.get(i).indexOf(".");
					pos1 = orderByFieldIDList.get(i).indexOf("(A)");
					if (pos1 >= 0) {
						buf.append(orderByFieldIDList.get(i).substring(pos0+1, pos1));
					} else {
						pos1 = orderByFieldIDList.get(i).indexOf("(D)");
						if (pos1 >= 0) {
							buf.append(orderByFieldIDList.get(i).substring(pos0+1, pos1));
							buf.append(" DESC ");
						} else {
							buf.append(orderByFieldIDList.get(i).substring(pos0+1, orderByFieldIDList.get(i).length()));
						}
					}
				}
			} else {
				buf.append(" order by ");
				for (int i = 0; i < keyFieldIDList.size(); i++) {
					if (i > 0) {
						buf.append(",");
					}
					buf.append(keyFieldIDList.get(i));
				}
			}
		}
		//
		return buf.toString();
	}
	
	public String getTableID(){
		return tableID;
	}
	
	public ArrayList<String> getHeaderKeyFieldIDList(){
		return headerKeyFieldIDList;
	}
	
	public ArrayList<String> getKeyFieldIDList(){
		return keyFieldIDList;
	}
	
	public ArrayList<XFScript> getScriptList(){
		return scriptList;
	}

	public org.w3c.dom.Element getDetailTableElement() {
		return detailTableElement_;
	}

	public org.w3c.dom.Element getTableElement() {
		return tableElement;
	}
	
	public ArrayList<String> getOrderByFieldIDList(){
		return orderByFieldIDList;
	}
	
	public boolean hasOrderByAsItsOwnFields(){
		return hasOrderByAsItsOwnFields;
	}
	
	public boolean isValidDataSource(int index, String tableID, String tableAlias, String fieldID) {
		boolean isValid = false;
		XF300_DetailReferTable referTable;
		org.w3c.dom.Element workElement;
		//
		if (this.getTableID().equals(tableID) && this.getTableID().equals(tableAlias)) {
			NodeList nodeList = tableElement.getElementsByTagName("Field");
			for (int i = 0; i < nodeList.getLength(); i++) {
				workElement = (org.w3c.dom.Element)nodeList.item(i);
				if (workElement.getAttribute("ID").equals(fieldID)) {
					isValid = true;
					break;
				}
			}
		} else {
			for (int i = 0; i < dialog_.getDetailReferTableList(index).size(); i++) {
				referTable = dialog_.getDetailReferTableList(index).get(i);
				if (referTable.getTableID().equals(tableID) && referTable.getTableAlias().equals(tableAlias)) {
					for (int j = 0; j < referTable.getFieldIDList().size(); j++) {
						if (referTable.getFieldIDList().get(j).equals(fieldID)) {
							isValid = true;
							break;
						}
					}
				}
				if (isValid) {
					break;
				}
			}
		}
		//
		return isValid;
	}

	public void runScript(int index, String event1, String event2) throws ScriptException  {
		XFScript script;
		ArrayList<XFScript> validScriptList = new ArrayList<XFScript>();
		//
		for (int i = 0; i < scriptList.size(); i++) {
			script = scriptList.get(i);
			if (script.isToBeRunAtEvent(event1, event2)) {
				validScriptList.add(script);
			}
		}
		//
		for (int i = 0; i < validScriptList.size(); i++) {
			dialog_.evalDetailTableScript(validScriptList.get(i).getName(), validScriptList.get(i).getScriptText(), index);
		}
	}
}

class XF300_DetailReferTable extends Object {
	private static final long serialVersionUID = 1L;
	private org.w3c.dom.Element referElement_ = null;
	private org.w3c.dom.Element tableElement = null;
	private int tabIndex_;
	private XF300 dialog_ = null;
	private String tableID = "";
	private String tableAlias = "";
	private String activeWhere = "";
	private ArrayList<String> fieldIDList = new ArrayList<String>();
	private ArrayList<String> toKeyFieldIDList = new ArrayList<String>();
	private ArrayList<String> withKeyFieldIDList = new ArrayList<String>();
	private ArrayList<String> orderByFieldIDList = new ArrayList<String>();
	private boolean isToBeExecuted = false;
	private int rangeKeyType = 0;
	private String rangeKeyFieldValid = "";
	private String rangeKeyFieldExpire = "";
	private String rangeKeyFieldSearch = "";
	private boolean rangeValidated;

	public XF300_DetailReferTable(org.w3c.dom.Element referElement, int tabIndex, XF300 dialog){
		super();
		//
		referElement_ = referElement;
		tabIndex_ = tabIndex;
		dialog_ = dialog;
		//
		tableID = referElement_.getAttribute("ToTable");
		tableElement = dialog_.getSession().getTableElement(tableID);
		//
		StringTokenizer workTokenizer;
		String wrkStr = tableElement.getAttribute("RangeKey");
		if (!wrkStr.equals("")) {
			workTokenizer = new StringTokenizer(wrkStr, ";" );
			rangeKeyFieldValid =workTokenizer.nextToken();
			rangeKeyFieldExpire =workTokenizer.nextToken();
			org.w3c.dom.Element workElement = dialog_.getSession().getFieldElement(tableID, rangeKeyFieldExpire);
			if (XFUtility.getOptionList(workElement.getAttribute("TypeOptions")).contains("VIRTUAL")) {
				rangeKeyType = 1;
			} else {
				rangeKeyType = 2;
			}
		}
		//
		activeWhere = tableElement.getAttribute("ActiveWhere");
		//
		tableAlias = referElement_.getAttribute("TableAlias");
		if (tableAlias.equals("")) {
			tableAlias = tableID;
		}
		//
		workTokenizer = new StringTokenizer(referElement_.getAttribute("Fields"), ";" );
		while (workTokenizer.hasMoreTokens()) {
			fieldIDList.add(workTokenizer.nextToken());
		}
		//
		if (referElement_.getAttribute("ToKeyFields").equals("")) {
			org.w3c.dom.Element workElement = dialog.getSession().getTablePKElement(tableID);
			workTokenizer = new StringTokenizer(workElement.getAttribute("Fields"), ";" );
			while (workTokenizer.hasMoreTokens()) {
				toKeyFieldIDList.add(workTokenizer.nextToken());
			}
		} else {
			workTokenizer = new StringTokenizer(referElement_.getAttribute("ToKeyFields"), ";" );
			while (workTokenizer.hasMoreTokens()) {
				toKeyFieldIDList.add(workTokenizer.nextToken());
			}
		}
		//
		workTokenizer = new StringTokenizer(referElement_.getAttribute("WithKeyFields"), ";" );
		while (workTokenizer.hasMoreTokens()) {
			withKeyFieldIDList.add(workTokenizer.nextToken());
		}
		//
		workTokenizer = new StringTokenizer(referElement_.getAttribute("OrderBy"), ";" );
		while (workTokenizer.hasMoreTokens()) {
			orderByFieldIDList.add(workTokenizer.nextToken());
		}
	}

	public String getSelectSQL(){
		int count;
		StringBuffer buf = new StringBuffer();
		boolean validWhereKeys = false;
		//
		buf.append("select ");
		//
		org.w3c.dom.Element workElement;
		count = 0;
		for (int i = 0; i < fieldIDList.size(); i++) {
			workElement = dialog_.getSession().getFieldElement(tableID, fieldIDList.get(i));
			if (!XFUtility.getOptionList(workElement.getAttribute("TypeOptions")).contains("VIRTUAL")) {
				if (count > 0) {
					buf.append(",");
				}
				count++;
				buf.append(fieldIDList.get(i));
			}
		}
		if (count == 0) {
			for (int i = 0; i < toKeyFieldIDList.size(); i++) {
				if (count > 0) {
					buf.append(",");
				}
				count++;
				buf.append(toKeyFieldIDList.get(i));
			}
		}
		if (!rangeKeyFieldValid.equals("")) {
			if (count > 0) {
				buf.append(",");
			}
			buf.append(rangeKeyFieldValid);
			//
			workElement = dialog_.getSession().getFieldElement(tableID, rangeKeyFieldExpire);
			if (!XFUtility.getOptionList(workElement.getAttribute("TypeOptions")).contains("VIRTUAL")) {
				buf.append(",");
				buf.append(rangeKeyFieldExpire);
			}
		}
		//
		buf.append(" from ");
		buf.append(tableID);
		buf.append(" where ");
		//
		count = 0;
		for (int i = 0; i < toKeyFieldIDList.size(); i++) {
			if (toKeyFieldIDList.get(i).equals(rangeKeyFieldValid)) {
				rangeKeyFieldSearch = withKeyFieldIDList.get(i);
			} else {
				if (count > 0) {
					buf.append(" and ");
				}
				buf.append(toKeyFieldIDList.get(i));
				buf.append("=");
				for (int j = 0; j < dialog_.getDetailColumnList(tabIndex_).size(); j++) {
					if (withKeyFieldIDList.get(i).equals(dialog_.getDetailColumnList(tabIndex_).get(j).getDataSourceName())) {
						//if (dialog_.getDetailColumnList(tabIndex_).get(j).isLiteralRequired()) {
						if (XFUtility.isLiteralRequiredBasicType(dialog_.getDetailColumnList(tabIndex_).get(j).getBasicType())) {
							buf.append("'");
							buf.append(dialog_.getDetailColumnList(tabIndex_).get(j).getInternalValue());
							buf.append("'");
							if (!dialog_.getDetailColumnList(tabIndex_).get(j).getInternalValue().equals("")) {
								validWhereKeys = true;
							}
						} else {
							buf.append(dialog_.getDetailColumnList(tabIndex_).get(j).getInternalValue());
							validWhereKeys = true;
						}
						break;
					}
				}
				count++;
			}
		}
		//
		if (!activeWhere.equals("")) {
			buf.append(" and ");
			buf.append(activeWhere);
		}
		//
		if (this.rangeKeyType != 0) {
			buf.append(" order by ");
			buf.append(rangeKeyFieldValid);
			buf.append(" DESC ");
		} else {
			if (orderByFieldIDList.size() > 0) {
				int pos0,pos1;
				buf.append(" order by ");
				for (int i = 0; i < orderByFieldIDList.size(); i++) {
					if (i > 0) {
						buf.append(",");
					}
					pos0 = orderByFieldIDList.get(i).indexOf(".");
					pos1 = orderByFieldIDList.get(i).indexOf("(A)");
					if (pos1 >= 0) {
						buf.append(orderByFieldIDList.get(i).substring(pos0+1, pos1));
					} else {
						pos1 = orderByFieldIDList.get(i).indexOf("(D)");
						if (pos1 >= 0) {
							buf.append(orderByFieldIDList.get(i).substring(pos0+1, pos1));
							buf.append(" DESC ");
						} else {
							buf.append(orderByFieldIDList.get(i).substring(pos0+1, orderByFieldIDList.get(i).length()));
						}
					}
				}
			}
		}
		//
		rangeValidated = false;
		//
		if (validWhereKeys) {
			return buf.toString();
		} else {
			return "";
		}
	}

	public String getTableID(){
		return tableID;
	}

	public String getTableAlias(){
		return tableAlias;
	}
	
	public ArrayList<String> getWithKeyFieldIDList(){
		return withKeyFieldIDList;
	}

	public ArrayList<String> getFieldIDList(){
		return  fieldIDList;
	}
	
	public void setToBeExecuted(boolean executed){
		isToBeExecuted = executed;
	}
	
	public boolean isToBeExecuted(){
		return isToBeExecuted;
	}
	
	public boolean isRecordToBeSelected(int index, ResultSet result){
		boolean returnValue = false;
		//
		if (rangeKeyType == 0) {
			returnValue = true;
		}
		//
		if (rangeKeyType == 1) {
			try {
				// Note that result set is ordered by rangeKeyFieldValue DESC //
				if (!rangeValidated) { 
					StringTokenizer workTokenizer = new StringTokenizer(rangeKeyFieldSearch, "." );
					String workTableAlias = workTokenizer.nextToken();
					String workFieldID = workTokenizer.nextToken();
					Object valueKey = dialog_.getDetailColumnObjectByID(index, "", workTableAlias, workFieldID).getInternalValue();
					Object valueFrom = result.getObject(rangeKeyFieldValid);
					int comp1 = valueKey.toString().compareTo(valueFrom.toString());
					if (comp1 >= 0) {
						returnValue = true;
						rangeValidated = true;
					}
				}
			} catch (SQLException e) {
				e.printStackTrace(dialog_.getExceptionStream());
				dialog_.setErrorAndCloseFunction();
			}
		}
		//
		if (rangeKeyType == 2) {
			try {
				StringTokenizer workTokenizer = new StringTokenizer(rangeKeyFieldSearch, "." );
				String workTableAlias = workTokenizer.nextToken();
				String workFieldID = workTokenizer.nextToken();
				Object valueKey = dialog_.getDetailColumnObjectByID(index, "", workTableAlias, workFieldID).getInternalValue();
				Object valueFrom = result.getObject(rangeKeyFieldValid);
				Object valueThru = result.getObject(rangeKeyFieldExpire);
				int comp1 = valueKey.toString().compareTo(valueFrom.toString());
				int comp2 = valueKey.toString().compareTo(valueThru.toString());
				if (comp1 >= 0 && comp2 < 0) {
					returnValue = true;
				}
			} catch (SQLException e) {
				e.printStackTrace(dialog_.getExceptionStream());
				dialog_.setErrorAndCloseFunction();
			}
		}
		//
		return returnValue;
	}
}

class XF300_KeyInputDialog extends JDialog {
	private static final long serialVersionUID = 1L;
	private static ResourceBundle res = ResourceBundle.getBundle("xeadDriver.Res");
	private JPanel jPanelMain = new JPanel();
	private Dimension scrSize;
	private JPanel jPanelKeyFields = new JPanel();
	private JPanel jPanelTop = new JPanel();
	private JPanel jPanelBottom = new JPanel();
	private JPanel jPanelButtons = new JPanel();
	private JPanel jPanelInfo = new JPanel();
	private GridLayout gridLayoutInfo = new GridLayout();
	private JLabel jLabelFunctionID = new JLabel();
	private JLabel jLabelSessionID = new JLabel();
	private JScrollPane jScrollPaneMessages = new JScrollPane();
	private JTextArea jTextAreaMessages = new JTextArea();
	private JButton jButtonCancel = new JButton();
	private JButton jButtonOK = new JButton();
	private XF300 dialog_;
	private final int FIELD_VERTICAL_MARGIN = 5;
	private final int FONT_SIZE = 14;
	private ArrayList<XF300_HeaderField> fieldList = new ArrayList<XF300_HeaderField>();
	private HashMap<String, Object> keyMap_ = new HashMap<String, Object>();
	
	public XF300_KeyInputDialog(XF300 dialog) {
		super(dialog, "", true);
		dialog_ = dialog;
		initComponentsAndVariants();
	}

	void initComponentsAndVariants() {
		//
		scrSize = Toolkit.getDefaultToolkit().getScreenSize();
		//
		jPanelMain.setLayout(new BorderLayout());
		jPanelTop.setLayout(new BorderLayout());
		jPanelKeyFields.setLayout(null);
		jPanelKeyFields.setFocusable(false);
		jTextAreaMessages.setEditable(false);
		jTextAreaMessages.setBorder(BorderFactory.createEtchedBorder());
		jTextAreaMessages.setFont(new java.awt.Font("SansSerif", 0, FONT_SIZE));
		jTextAreaMessages.setFocusable(false);
		jTextAreaMessages.setLineWrap(true);
		jScrollPaneMessages.getViewport().add(jTextAreaMessages, null);
		jScrollPaneMessages.setPreferredSize(new Dimension(10, 35));
		jPanelBottom.setPreferredSize(new Dimension(10, 35));
		jPanelBottom.setLayout(new BorderLayout());
		jPanelBottom.setBorder(null);
		jLabelFunctionID.setFont(new java.awt.Font("Dialog", 0, FONT_SIZE));
		jLabelFunctionID.setHorizontalAlignment(SwingConstants.RIGHT);
		jLabelFunctionID.setForeground(Color.gray);
		jLabelFunctionID.setFocusable(false);
		jLabelSessionID.setFont(new java.awt.Font("Dialog", 0, FONT_SIZE));
		jLabelSessionID.setHorizontalAlignment(SwingConstants.RIGHT);
		jLabelSessionID.setForeground(Color.gray);
		jLabelSessionID.setFocusable(false);
		jPanelButtons.setBorder(null);
		jPanelButtons.setLayout(null);
		jPanelButtons.setFocusable(false);
		gridLayoutInfo.setColumns(1);
		gridLayoutInfo.setRows(2);
		gridLayoutInfo.setVgap(4);
		jPanelInfo.setLayout(gridLayoutInfo);
		jPanelInfo.add(jLabelSessionID);
		jPanelInfo.add(jLabelFunctionID);
		jPanelInfo.setFocusable(false);
		jPanelMain.add(jPanelTop, BorderLayout.CENTER);
		jPanelMain.add(jPanelBottom, BorderLayout.SOUTH);
		jPanelTop.add(jPanelKeyFields, BorderLayout.CENTER);
		jPanelTop.add(jScrollPaneMessages, BorderLayout.SOUTH);
		jPanelBottom.add(jPanelInfo, BorderLayout.EAST);
		jPanelBottom.add(jPanelButtons, BorderLayout.CENTER);
		jButtonCancel.setFont(new java.awt.Font("Dialog", 0, FONT_SIZE));
		jButtonCancel.setText(res.getString("Cancel"));
		jButtonCancel.setBounds(new Rectangle(5, 2, 80, 32));
		jButtonCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				keyMap_.clear();
				setVisible(false);
			}
		});
		jButtonOK.setFont(new java.awt.Font("Dialog", 0, FONT_SIZE));
		jButtonOK.setText("OK");
		jButtonOK.setBounds(new Rectangle(270, 2, 80, 32));
		jButtonOK.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean anyOfFieldsAreNull = false;
				for (int i = 0; i < fieldList.size(); i++) {
					if (fieldList.get(i).getInternalValue().equals(fieldList.get(i).getNullValue())) {
						anyOfFieldsAreNull = true;
						break;
					} else {
						keyMap_.put(fieldList.get(i).getFieldID(), fieldList.get(i).getInternalValue());
					}
				}
				if (anyOfFieldsAreNull) {
					jTextAreaMessages.setText(res.getString("FunctionError23"));
				} else {
					setVisible(false);
				}
			}
		});
		jPanelButtons.add(jButtonCancel);
		jPanelButtons.add(jButtonOK);
		this.getRootPane().setDefaultButton(jButtonOK);
		this.getContentPane().add(jPanelMain, BorderLayout.CENTER);
		this.setSize(new Dimension(scrSize.width, scrSize.height));
		//
		StringTokenizer workTokenizer;
		String tableAlias, tableID, fieldID;
		org.w3c.dom.Element element;
		int posX = 0;
		int posY = 0;
		int biggestWidth = 0;
		int biggestHeight = 0;
		Dimension dim = new Dimension();
		Dimension dimOfPriviousField = new Dimension();
		boolean topField = true;
		XF300_HeaderField field;
		//
		this.setTitle(dialog_.getTitle());
		jLabelSessionID.setText(dialog_.getSession().getSessionID());
		jLabelFunctionID.setText(dialog_.getFunctionInfo());
		FontMetrics metrics = jLabelFunctionID.getFontMetrics(new java.awt.Font("Dialog", 0, FONT_SIZE));
		jPanelInfo.setPreferredSize(new Dimension(metrics.stringWidth(jLabelFunctionID.getText()), 35));
		//
		NodeList headerFieldElementList = dialog_.getFunctionElement().getElementsByTagName("Field");
		SortableDomElementListModel sortingList1 = XFUtility.getSortedListModel(headerFieldElementList, "Order");
		for (int i = 0; i < sortingList1.getSize(); i++) {
			element = (org.w3c.dom.Element)sortingList1.getElementAt(i);
			workTokenizer = new StringTokenizer(element.getAttribute("DataSource"), "." );
			tableAlias = workTokenizer.nextToken();
			tableID = dialog_.getTableIDOfTableAlias(tableAlias, -1);
			fieldID =workTokenizer.nextToken();
			//
			if (tableID.equals(dialog_.getHeaderTable().getTableID())) {
				ArrayList<String> keyFieldList = dialog_.getHeaderTable().getKeyFieldIDList();
				for (int j = 0; j < keyFieldList.size(); j++) {
					if (keyFieldList.get(j).equals(fieldID)) {
						field = new XF300_HeaderField((org.w3c.dom.Element)sortingList1.getElementAt(i), dialog_);
						field.setEditable(true);
						fieldList.add(field);
						//
						if (topField) {
							posX = 0;
							posY = this.FIELD_VERTICAL_MARGIN + 8;
							topField = false;
						} else {
							posX = 0;
							posY = posY + dimOfPriviousField.height+ field.getPositionMargin() + this.FIELD_VERTICAL_MARGIN;
						}
						dim = field.getPreferredSize();
						field.setBounds(posX, posY, dim.width, dim.height);
						jPanelKeyFields.add(field);
						//
						if (posX + dim.width > biggestWidth) {
							biggestWidth = posX + dim.width;
						}
						if (posY + dim.height > biggestHeight) {
							biggestHeight = posY + dim.height;
						}
						dimOfPriviousField = new Dimension(dim.width, dim.height);
					}
				}
			}
		}
		//
		int width = 450;
		if (biggestWidth > 430) {
			width = biggestWidth + 20;
		}
		int height = biggestHeight + 117;
		this.setPreferredSize(new Dimension(width, height));
		posX = (scrSize.width - width) / 2;
		posY = (scrSize.height - height) / 2;
		this.setLocation(posX, posY);
		this.pack();
	}
	
	public HashMap<String, Object> requestKeyValues(String message) {
		keyMap_.clear();
		if (message.equals("")) {
			jTextAreaMessages.setText(res.getString("FunctionMessage29"));
		} else {
			jTextAreaMessages.setText(message);
		}
		this.setVisible(true);
		//
		return keyMap_;
	}
}

class XF300_jTabbedPane_keyAdapter extends java.awt.event.KeyAdapter {
	XF300 adaptee;
	XF300_jTabbedPane_keyAdapter(XF300 adaptee) {
		this.adaptee = adaptee;
	}
	public void keyPressed(KeyEvent e) {
		adaptee.jTabbedPane_keyPressed(e);
	}
}

class XF300_jTabbedPane_changeAdapter  implements ChangeListener {
	XF300 adaptee;
	XF300_jTabbedPane_changeAdapter(XF300 adaptee) {
		this.adaptee = adaptee;
	}
	public void stateChanged(ChangeEvent e) {
		adaptee.jTabbedPane_stateChanged(e);
	}
}

class XF300_jTableMain_keyAdapter extends java.awt.event.KeyAdapter {
	XF300 adaptee;
	XF300_jTableMain_keyAdapter(XF300 adaptee) {
		this.adaptee = adaptee;
	}
	public void keyPressed(KeyEvent e) {
		adaptee.jTableMain_keyPressed(e);
	}
}

class XF300_jTableMain_focusAdapter extends java.awt.event.FocusAdapter {
	XF300 adaptee;
	XF300_jTableMain_focusAdapter(XF300 adaptee) {
		this.adaptee = adaptee;
	}
	public void focusGained(FocusEvent e) {
		adaptee.jTableMain_focusGained(e);
	}
	public void focusLost(FocusEvent e) {
		adaptee.jTableMain_focusLost(e);
	}
}

class XF300_jTableMain_mouseAdapter extends java.awt.event.MouseAdapter {
	XF300 adaptee;
	XF300_jTableMain_mouseAdapter(XF300 adaptee) {
		this.adaptee = adaptee;
	}
	public void mouseClicked(MouseEvent e) {
		adaptee.jTableMain_mouseClicked(e);
	}
}

class XF300_jScrollPaneTable_mouseAdapter extends java.awt.event.MouseAdapter {
	XF300 adaptee;
	XF300_jScrollPaneTable_mouseAdapter(XF300 adaptee) {
		this.adaptee = adaptee;
	}
	public void mousePressed(MouseEvent e) {
		adaptee.jScrollPaneTable_mousePressed(e);
	}
}

class XF300_FunctionButton_actionAdapter implements java.awt.event.ActionListener {
	XF300 adaptee;
	XF300_FunctionButton_actionAdapter(XF300 adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.jFunctionButton_actionPerformed(e);
	}
}

class XF300_WindowAdapter extends java.awt.event.WindowAdapter {
	XF300 adaptee;
	XF300_WindowAdapter(XF300 adaptee) {
		this.adaptee = adaptee;
	}
	public void windowGainedFocus(WindowEvent e) {
		adaptee.windowGainedFocus(e);
	}
}

class XF300_keyAdapter extends java.awt.event.KeyAdapter {
	XF300 adaptee;
	XF300_keyAdapter(XF300 adaptee) {
		this.adaptee = adaptee;
	}
	public void keyPressed(KeyEvent e) {
		adaptee.dialog_keyPressed(e);
	}
}

class XF300_jButtonList_actionAdapter implements java.awt.event.ActionListener {
	XF300 adaptee;
	XF300_jButtonList_actionAdapter(XF300 adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.selectDetailRecordsAndSetupTableRows(adaptee.getSelectedTabIndex(), true);
	}
}
