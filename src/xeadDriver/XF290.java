package xeadDriver;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.swing.*;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import org.w3c.dom.*;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

public class XF290 extends Component implements XFExecutable, XFScriptable {
	private static final long serialVersionUID = 1L;
	private static ResourceBundle res = ResourceBundle.getBundle("xeadDriver.Res");
	private org.w3c.dom.Element functionElement_ = null;
	private HashMap<String, Object> parmMap_ = null;
	private HashMap<String, Object> returnMap_ = new HashMap<String, Object>();
	private XF290_PrimaryTable primaryTable_ = null;
	private Session session_ = null;
	private boolean instanceIsAvailable_ = true;
	private boolean isToBeCanceled = false;
	private int programSequence;
	private StringBuffer processLog = new StringBuffer();
	private XF290_Phrase headerPhrase = null;
	private ArrayList<XF290_Phrase> paragraphList = new ArrayList<XF290_Phrase>();
	private ArrayList<XF290_ReferTable> referTableList = new ArrayList<XF290_ReferTable>();
	private ArrayList<XF290_Field> fieldList = new ArrayList<XF290_Field>();
	private Connection connection = null;
	private ScriptEngine scriptEngine;
	private Bindings engineScriptBindings;
	private String scriptNameRunning = "";
	private ByteArrayOutputStream exceptionLog;
	private PrintStream exceptionStream;
	private String exceptionHeader = "";

	public XF290(Session session, int instanceArrayIndex) {
		super();
		try {
			session_ = session;
			//
			initComponentsAndVariants();
			//
		} catch(Exception e) {
			e.printStackTrace(exceptionStream);
		}
	}

	void initComponentsAndVariants() throws Exception {
	}

	public boolean isAvailable() {
		return instanceIsAvailable_;
	}

	public HashMap<String, Object> execute(org.w3c.dom.Element functionElement, HashMap<String, Object> parmMap) {
		SortableDomElementListModel sortedList;
		String workAlias, workTableID, workFieldID;
		StringTokenizer workTokenizer;
		org.w3c.dom.Element workElement;

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
			instanceIsAvailable_ = false;
			isToBeCanceled = false;
			exceptionLog = new ByteArrayOutputStream();
			exceptionStream = new PrintStream(exceptionLog);
			exceptionHeader = "";
			functionElement_ = functionElement;
			processLog.delete(0, processLog.length());
			connection = session_.getConnection();
			programSequence = session_.writeLogOfFunctionStarted(functionElement_.getAttribute("ID"), functionElement_.getAttribute("Name"));

			//////////////////////////////////////
			// Setup Script Engine and Bindings //
			//////////////////////////////////////
			scriptEngine = session_.getScriptEngineManager().getEngineByName("js");
			engineScriptBindings = scriptEngine.getBindings(ScriptContext.ENGINE_SCOPE);
			engineScriptBindings.clear();
			engineScriptBindings.put("instance", (XFScriptable)this);
			
			//////////////////////////////////////////////
			// Setup the primary table and refer tables //
			//////////////////////////////////////////////
			primaryTable_ = new XF290_PrimaryTable(functionElement_, this);
			referTableList.clear();
			NodeList referNodeList = primaryTable_.getTableElement().getElementsByTagName("Refer");
			sortedList = XFUtility.getSortedListModel(referNodeList, "Order");
			for (int i = 0; i < sortedList.getSize(); i++) {
				org.w3c.dom.Element element = (org.w3c.dom.Element)sortedList.getElementAt(i);
				referTableList.add(new XF290_ReferTable(element, this));
			}

			/////////////////////////////////
			// Setup phrase and field List //
			/////////////////////////////////
			XF290_Phrase phrase;
			String lastBlock = "";
			headerPhrase = null;
			fieldList.clear();
			paragraphList.clear();
			NodeList nodeList = functionElement_.getElementsByTagName("Phrase");
			SortableDomElementListModel sortableList = XFUtility.getSortedListModel(nodeList, "Order");
			for (int i = 0; i < sortableList.getSize(); i++) {
				phrase = new XF290_Phrase((org.w3c.dom.Element)sortableList.getElementAt(i));
				if (phrase.getBlock().equals("PHRASE")) {
					if (lastBlock.equals("PARAGRAPH")) {
						paragraphList.add(phrase);
					}
				} else {
					if (phrase.getBlock().equals("HEADER")) {
						headerPhrase = phrase;
					}
					if (phrase.getBlock().equals("PARAGRAPH")) {
						paragraphList.add(phrase);
					}
					lastBlock = phrase.getBlock();
				}
			}
			//
			// Add fields on phrases if they are not on the column list //
			if (headerPhrase != null) {
				for (int j = 0; j < headerPhrase.getDataSourceNameList().size(); j++) {
					if (!existsInFieldList(headerPhrase.getDataSourceNameList().get(j))) {
						fieldList.add(new XF290_Field(headerPhrase.getDataSourceNameList().get(j), this));
					}
				}
			}
			for (int i = 0; i < paragraphList.size(); i++) {
				for (int j = 0; j < paragraphList.get(i).getDataSourceNameList().size(); j++) {
					if (!existsInFieldList(paragraphList.get(i).getDataSourceNameList().get(j))) {
						fieldList.add(new XF290_Field(paragraphList.get(i).getDataSourceNameList().get(j), this));
					}
				}
			}
			//
			// Add primary table key fields if they are not on the column list //
			for (int i = 0; i < primaryTable_.getKeyFieldList().size(); i++) {
				if (!existsInFieldList(primaryTable_.getTableID(), "", primaryTable_.getKeyFieldList().get(i))) {
					fieldList.add(new XF290_Field(primaryTable_.getTableID(), "", primaryTable_.getKeyFieldList().get(i), this));
				}
			}
			//
			// Analyze fields in script and add them if necessary //
			for (int i = 0; i < primaryTable_.getScriptList().size(); i++) {
				if	(primaryTable_.getScriptList().get(i).isToBeRunAtEvent("BR", "")
						|| primaryTable_.getScriptList().get(i).isToBeRunAtEvent("AR", "")) {
					for (int j = 0; j < primaryTable_.getScriptList().get(i).getFieldList().size(); j++) {
						workTokenizer = new StringTokenizer(primaryTable_.getScriptList().get(i).getFieldList().get(j), "." );
						workAlias = workTokenizer.nextToken();
						workTableID = getTableIDOfTableAlias(workAlias);
						workFieldID = workTokenizer.nextToken();
						if (!existsInFieldList(workTableID, workAlias, workFieldID)) {
							workElement = session_.getFieldElement(workTableID, workFieldID);
							if (workElement == null) {
								String msg = res.getString("FunctionError1") + primaryTable_.getTableID() + res.getString("FunctionError2") + primaryTable_.getScriptList().get(i).getName() + res.getString("FunctionError3") + workAlias + "_" + workFieldID + res.getString("FunctionError4");
								JOptionPane.showMessageDialog(null, msg);
								throw new Exception(msg);
							} else {
								if (primaryTable_.isValidDataSource(workTableID, workAlias, workFieldID)) {
									fieldList.add(new XF290_Field(workTableID, workAlias, workFieldID, this));
								}
							}
						}
					}
				}
			}
			//
			// Analyze refer tables and add their fields if necessary //
			for (int i = referTableList.size()-1; i > -1; i--) {
				for (int j = 0; j < referTableList.get(i).getFieldIDList().size(); j++) {
					if (existsInFieldList(referTableList.get(i).getTableID(), referTableList.get(i).getTableAlias(), referTableList.get(i).getFieldIDList().get(j))) {
						referTableList.get(i).setToBeExecuted(true);
						break;
					}
				}
				if (referTableList.get(i).isToBeExecuted()) {
					for (int j = 0; j < referTableList.get(i).getFieldIDList().size(); j++) {
						if (!existsInFieldList(referTableList.get(i).getTableID(), referTableList.get(i).getTableAlias(), referTableList.get(i).getFieldIDList().get(j))) {
							fieldList.add(new XF290_Field(referTableList.get(i).getTableID(), referTableList.get(i).getTableAlias(), referTableList.get(i).getFieldIDList().get(j), this));
						}
					}
					for (int j = 0; j < referTableList.get(i).getWithKeyFieldIDList().size(); j++) {
						workTokenizer = new StringTokenizer(referTableList.get(i).getWithKeyFieldIDList().get(j), "." );
						workAlias = workTokenizer.nextToken();
						workTableID = getTableIDOfTableAlias(workAlias);
						workFieldID = workTokenizer.nextToken();
						if (!existsInFieldList(workTableID, workAlias, workFieldID)) {
							fieldList.add(new XF290_Field(workTableID, workAlias, workFieldID, this));
						}
					}
				}
			}

			//////////////////////////////////////////////////
			// Fetch the record and set values on the panel //
			//////////////////////////////////////////////////
			fetchTableRecord();

			//////////////////////////////////
			// Generate and browse PDF file //
			//////////////////////////////////
			if (!this.isToBeCanceled) {
				session_.browseFile(createPDFFileAndGetURI());
			}

		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, res.getString("FunctionError5"));
			e.printStackTrace(exceptionStream);
			setErrorAndCloseFunction();
		} finally {
			setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		}

		///////////////////
		// CloseFunction //
		///////////////////
		closeFunction();

		return returnMap_;
	}

	void setErrorAndCloseFunction() {
		returnMap_.put("RETURN_CODE", "99");
		closeFunction();
	}

	void closeFunction() {
		instanceIsAvailable_ = true;
		//
		String wrkStr;
		if (exceptionLog.size() > 0 || !exceptionHeader.equals("")) {
			wrkStr = processLog.toString() + "\nERROR LOG:\n" + exceptionHeader + exceptionLog.toString();
		} else {
			wrkStr = processLog.toString();
		}
		wrkStr = wrkStr.replace("'", "\"");
		session_.writeLogOfFunctionClosed(programSequence, returnMap_.get("RETURN_CODE").toString(), wrkStr);
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

	private URI createPDFFileAndGetURI() {
		File pdfFile = null;
		String pdfFileName = "";
		FileOutputStream fileOutputStream = null;
		com.lowagie.text.Font font;
		Chunk chunk;
		Phrase phrase;
		//
		//Setup margins//
		float leftMargin = 50;
		float rightMargin = 50;
		float topMargin = 50;
		float bottomMargin = 50;
		StringTokenizer workTokenizer = new StringTokenizer(functionElement_.getAttribute("Margins"), ";" );
		try {
			leftMargin = Float.parseFloat(workTokenizer.nextToken());
			rightMargin = Float.parseFloat(workTokenizer.nextToken());
			topMargin = Float.parseFloat(workTokenizer.nextToken());
			bottomMargin = Float.parseFloat(workTokenizer.nextToken());
		} catch (Exception e) {
		}
		//
		//Generate PDF file and PDF writer//
		com.lowagie.text.Rectangle pageSize = XFUtility.getPageSize(functionElement_.getAttribute("PageSize"), functionElement_.getAttribute("Direction"));
		com.lowagie.text.Document pdfDoc = new com.lowagie.text.Document(pageSize, leftMargin, rightMargin, topMargin, bottomMargin); 
		//
		try {
			pdfFile = session_.createTempFile(functionElement_.getAttribute("ID"), ".pdf");
			pdfFileName = pdfFile.getPath();
			fileOutputStream = new FileOutputStream(pdfFileName);
			PdfWriter writer = PdfWriter.getInstance(pdfDoc, fileOutputStream);
			//
			//Set document attributes//
			pdfDoc.addTitle(functionElement_.getAttribute("Name"));
			pdfDoc.addAuthor(session_.getUserName()); 
			pdfDoc.addCreator("XEAD Driver");
			//
			//Add header to the document//
			if (headerPhrase != null && headerPhrase.getValueKeywordList().size() > 0) {
				font = new com.lowagie.text.Font(session_.getBaseFontWithID(headerPhrase.getFontID()), headerPhrase.getFontSize(), headerPhrase.getFontStyle());
				phrase = new Phrase("", font);
				for (int i = 0; i < headerPhrase.getValueKeywordList().size(); i++) {
					chunk = getChunkForKeyword(headerPhrase.getValueKeywordList().get(i), null);
					if (chunk != null) {
						phrase.add(chunk);
					}
				}
				HeaderFooter header = new HeaderFooter(phrase,	false);
				header.setAlignment(headerPhrase.getAlignment());
				pdfDoc.setHeader(header);
			}
			//
			//Add page-number-footer to the document//
			if (functionElement_.getAttribute("WithPageNumber").equals("T")) {
				HeaderFooter footer = new HeaderFooter(
						new Phrase("--"), new Phrase("--"));
				footer.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
				footer.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
				pdfDoc.setFooter(footer);
			}
			//
			//Add phrases to the document//
			pdfDoc.open();
			PdfContentByte cb = writer.getDirectContent();
			Paragraph paragraph = null;
			String keyword = "";
			for (int i = 0; i < paragraphList.size(); i++) {
				//
				if (paragraphList.get(i).getBlock().equals("PARAGRAPH")) {
					paragraph = new Paragraph(new Phrase(""));
					paragraph.setAlignment(paragraphList.get(i).getAlignment());
					if (paragraph.getAlignment() == Paragraph.ALIGN_RIGHT) {
						paragraph.setIndentationRight(paragraphList.get(i).getAlignmentMargin());
					}
					if (paragraph.getAlignment() == Paragraph.ALIGN_LEFT) {
						paragraph.setIndentationLeft(paragraphList.get(i).getAlignmentMargin());
					}
					if (paragraph.getAlignment() == Paragraph.ALIGN_CENTER) {
						paragraph.setIndentationRight(paragraphList.get(i).getAlignmentMargin());
						paragraph.setIndentationLeft(paragraphList.get(i).getAlignmentMargin());
					}
					paragraph.setSpacingAfter(paragraphList.get(i).getSpacingAfter());
					//
					font = new com.lowagie.text.Font(session_.getBaseFontWithID(paragraphList.get(i).getFontID()), paragraphList.get(i).getFontSize(), paragraphList.get(i).getFontStyle());
					phrase = new Phrase("", font);
					for (int j = 0; j < paragraphList.get(i).getValueKeywordList().size(); j++) {
						keyword = paragraphList.get(i).getValueKeywordList().get(j);
						chunk = getChunkForKeyword(keyword, cb);
						if (chunk != null) {
							phrase.add(chunk);
						}
					}
					paragraph.add(phrase);
					//
					pdfDoc.add(paragraph);
				}
			}
			//
			pdfDoc.close();
			//
		} catch (DocumentException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		//
		return pdfFile.toURI();
	}

	private Chunk getChunkForKeyword(String keyword, PdfContentByte cb) {
		StringTokenizer workTokenizer;
		String wrkStr;
		com.lowagie.text.Image image;
		int pos, x, y, w;
		Chunk chunk = null;
		//
		try {
			//
			if (keyword.contains("&Text(")) {
				pos = keyword.lastIndexOf(")");
				chunk = new Chunk(keyword.substring(6, pos));
			}
			//
			if (keyword.contains("&DataSource(")) {
				pos = keyword.lastIndexOf(")");
				wrkStr = getExternalStringValueOfFieldByName(keyword.substring(12, pos));
				chunk = new Chunk(wrkStr);
			}
			//
			if (keyword.contains("&Image(")) {
				pos = keyword.lastIndexOf(")");
				workTokenizer = new StringTokenizer(keyword.substring(7, pos), ";" );
				wrkStr = workTokenizer.nextToken().trim();
				x = Integer.parseInt(workTokenizer.nextToken().trim());
				y = Integer.parseInt(workTokenizer.nextToken().trim());
				w = Integer.parseInt(workTokenizer.nextToken().trim());
				image = XFUtility.getImage(getExternalStringValueOfFieldByName(wrkStr), w);
				chunk = new Chunk(image, x, y, true);
			}
			//
			if (keyword.contains("&Barcode(") && cb != null) {
				pos = keyword.lastIndexOf(")");
				workTokenizer = new StringTokenizer(keyword.substring(9, pos), ";" );
				wrkStr = workTokenizer.nextToken().trim();
				String type = workTokenizer.nextToken().trim();
				chunk = XFUtility.getBarcodeChunkOfValue(getExternalStringValueOfFieldByName(wrkStr), type, cb);
			}
			//
			if (keyword.contains("&SystemVariant(")) {
				pos = keyword.lastIndexOf(")");
				chunk = new Chunk(session_.getSystemVariantString(keyword.substring(15, pos)));
			}
			//
			if (keyword.contains("&DateTime(")) {
				pos = keyword.lastIndexOf(")");
				chunk = new Chunk(session_.getTodayTime(keyword.substring(10, pos)));
			}
			//
			if (keyword.contains("&Date(")) {
				pos = keyword.lastIndexOf(")");
				chunk = new Chunk(session_.getToday(keyword.substring(6, pos)));
			}
			//
			if (keyword.contains("&Logo(")) {
				pos = keyword.lastIndexOf(")");
				workTokenizer = new StringTokenizer(keyword.substring(6, pos), ";" );
				x = Integer.parseInt(workTokenizer.nextToken().trim());
				y = Integer.parseInt(workTokenizer.nextToken().trim());
				w = Integer.parseInt(workTokenizer.nextToken().trim());
				image = XFUtility.getImage(session_.getSystemVariantString("COMP_LOGO"), w);
				chunk = new Chunk(image, x, y, true);
			}
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, res.getString("FunctionError39") + keyword + res.getString("FunctionError40") + e.toString());
		}
		//
		return chunk;
	}
	
	void fetchTableRecord() {
		try {
			//
			for (int i = 0; i < fieldList.size(); i++) {
				if (parmMap_.containsKey(fieldList.get(i).getFieldID())) {
					fieldList.get(i).setValue(parmMap_.get(fieldList.get(i).getFieldID()));
				}
			}
			//
			primaryTable_.runScript("BR", "");
			//
			Statement statementForPrimaryTable = connection.createStatement();
			String sql = primaryTable_.getSQLToSelect();
			XFUtility.appendLog(sql, processLog);
			ResultSet resultOfPrimaryTable = statementForPrimaryTable.executeQuery(sql);
			if (resultOfPrimaryTable.next()) {
				//
				for (int i = 0; i < fieldList.size(); i++) {
					if (fieldList.get(i).getTableID().equals(primaryTable_.getTableID())) {
						fieldList.get(i).setValueOfResultSet(resultOfPrimaryTable);
					}
				}
				//
				fetchReferTableRecords("AR", "");
				//
			} else {
				JOptionPane.showMessageDialog(null, res.getString("FunctionError30"));
				returnMap_.put("RETURN_CODE", "01");
				isToBeCanceled = true;
			}
			//
			resultOfPrimaryTable.close();
			//
		} catch (SQLException e) {
			JOptionPane.showMessageDialog(null, res.getString("FunctionError6"));
			e.printStackTrace(exceptionStream);
			setErrorAndCloseFunction();
		} catch(ScriptException e) {
			JOptionPane.showMessageDialog(null, res.getString("FunctionError7") + this.getScriptNameRunning() + res.getString("FunctionError8"));
			exceptionHeader = "'" + this.getScriptNameRunning() + "' Script error\n";
			e.printStackTrace(exceptionStream);
			setErrorAndCloseFunction();
		}
	}

	protected void fetchReferTableRecords(String event, String specificReferTable) {
		ResultSet resultOfReferTable = null;
		String sql = "";
		//
		try {
			//
			Statement statementForReferTable = connection.createStatement();
			//
			primaryTable_.runScript(event, "BR()");
			//
			for (int i = 0; i < referTableList.size(); i++) {
				if (specificReferTable.equals("") || specificReferTable.equals(referTableList.get(i).getTableAlias())) {
					//
					if (referTableList.get(i).isToBeExecuted()) {
						//
						primaryTable_.runScript(event, "BR(" + referTableList.get(i).getTableAlias() + ")");
						//
						for (int j = 0; j < fieldList.size(); j++) {
							if (fieldList.get(j).getTableAlias().equals(referTableList.get(i).getTableAlias())) {
								fieldList.get(j).setValue(null);
							}
						}
						//
						if (!referTableList.get(i).isKeyNullable() || !referTableList.get(i).isKeyNull()) {
							//
							sql = referTableList.get(i).getSelectSQL(false);
							XFUtility.appendLog(sql, processLog);
							resultOfReferTable = statementForReferTable.executeQuery(sql);
							while (resultOfReferTable.next()) {
								//
								if (referTableList.get(i).isRecordToBeSelected(resultOfReferTable)) {
									//
									for (int j = 0; j < fieldList.size(); j++) {
										if (fieldList.get(j).getTableAlias().equals(referTableList.get(i).getTableAlias())) {
											fieldList.get(j).setValueOfResultSet(resultOfReferTable);
										}
									}
									//
									primaryTable_.runScript(event, "AR(" + referTableList.get(i).getTableAlias() + ")");
								}
							}
							//
							resultOfReferTable.close();
						}
					}
				}
			}
			//
			primaryTable_.runScript(event, "AR()");
			//
		} catch (SQLException e) {
			JOptionPane.showMessageDialog(null, res.getString("FunctionError6"));
			e.printStackTrace(exceptionStream);
			setErrorAndCloseFunction();
		} catch(ScriptException e) {
			JOptionPane.showMessageDialog(null, res.getString("FunctionError7") + this.getScriptNameRunning() + res.getString("FunctionError8"));
			exceptionHeader = "'" + this.getScriptNameRunning() + "' Script error\n";
			e.printStackTrace(exceptionStream);
			setErrorAndCloseFunction();
		}
	}

	public String getFunctionID() {
		return functionElement_.getAttribute("ID");
	}

	public HashMap<String, Object> getParmMap() {
		return parmMap_;
	}

	public HashMap<String, Object> getReturnMap() {
		return returnMap_;
	}

	public String getScriptNameRunning() {
		return scriptNameRunning;
	}

	public StringBuffer getProcessLog() {
		return processLog;
	}

	public Session getSession() {
		return session_;
	}

	public PrintStream getExceptionStream() {
		return exceptionStream;
	}

	public Bindings getEngineScriptBindings() {
		return 	engineScriptBindings;
	}
	
	public void evalScript(String scriptName, String scriptText) throws ScriptException {
		if (!scriptText.equals("")) {
			scriptNameRunning = scriptName;
			scriptEngine.eval(scriptText);
		}
	}

	public XF290_PrimaryTable getPrimaryTable() {
		return primaryTable_;
	}

	public ArrayList<XF290_Field> getFieldList() {
		return fieldList;
	}

	public ArrayList<String> getKeyFieldList() {
		return primaryTable_.getKeyFieldList();
	}

	public ArrayList<XF290_ReferTable> getReferTableList() {
		return referTableList;
	}

	public boolean existsInFieldList(String tableID, String tableAlias, String fieldID) {
		boolean result = false;
		for (int i = 0; i < fieldList.size(); i++) {
			if (tableID.equals("")) {
				if (fieldList.get(i).getTableAlias().equals(tableAlias)) {
					result = true;
				}
			}
			if (tableAlias.equals("")) {
				if (fieldList.get(i).getTableID().equals(tableID) && fieldList.get(i).getFieldID().equals(fieldID)) {
					result = true;
				}
			}
			if (!tableID.equals("") && !tableAlias.equals("")) {
				if (fieldList.get(i).getTableID().equals(tableID) && fieldList.get(i).getTableAlias().equals(tableAlias) && fieldList.get(i).getFieldID().equals(fieldID)) {
					result = true;
				}
			}
		}
		return result;
	}

	public boolean existsInFieldList(String dataSourceName) {
		boolean result = false;
		for (int i = 0; i < fieldList.size(); i++) {
			if (fieldList.get(i).getDataSourceName().equals(dataSourceName)) {
				result = true;
			}
		}
		return result;
	}

	public String getTableIDOfTableAlias(String tableAlias) {
		String tableID = tableAlias;
		XF290_ReferTable referTable;
		for (int j = 0; j < referTableList.size(); j++) {
			referTable = referTableList.get(j);
			referTable.getTableAlias();
			if (referTable.getTableAlias().equals(tableAlias)) {
				tableID = referTable.getTableID();
				break;
			}
		}
		return tableID;
	}

	public Object getInternalValueOfFieldByName(String dataSourceName) {
		Object obj = null;
		for (int i = 0; i < fieldList.size(); i++) {
			if (fieldList.get(i).getDataSourceName().equals(dataSourceName)) {
				obj = fieldList.get(i).getInternalValue();
				break;
			}
		}
		return obj;
	}

	public String getExternalStringValueOfFieldByName(String dataSourceName) {
		String value = "";
		String wrkStr, basicType, fmt = "";
		//
		StringTokenizer workTokenizer = new StringTokenizer(dataSourceName, ";");
		wrkStr = workTokenizer.nextToken().trim();
		if (workTokenizer.hasMoreTokens()) {
			fmt = workTokenizer.nextToken().trim();
		}
		for (int i = 0; i < fieldList.size(); i++) {
			if (fieldList.get(i).getDataSourceName().equals(wrkStr)) {
				if (fieldList.get(i).isKubunField()) {
					value = fieldList.get(i).getExternalValue().toString();
				} else {
					value = fieldList.get(i).getInternalValue().toString();
					//
					basicType = fieldList.get(i).getBasicType();
					if (basicType.equals("DATE")) {
						if (fmt.equals("")) {
							fmt = session_.getDateFormat();
						}
						value = XFUtility.getUserExpressionOfUtilDate(XFUtility.convertDateFromSqlToUtil(java.sql.Date.valueOf(value)), fmt, false);
					}
					if (basicType.equals("INTEGER")) {
						value = XFUtility.getEditValueOfInteger(Integer.parseInt(value), fmt, fieldList.get(i).getDataSize());
					}
					if (basicType.equals("FLOAT")) {
						value = XFUtility.getEditValueOfFloat(Float.parseFloat(value), fmt, fieldList.get(i).getDecimalSize());
					}
					if (fieldList.get(i).getDataTypeOptionList().contains("YMONTH") || fieldList.get(i).getDataTypeOptionList().contains("FYEAR")) {
						if (fmt.equals("")) {
							fmt = session_.getDateFormat();
						}
						value = XFUtility.getUserExpressionOfYearMonth(value.toString(), fmt);
					}
					if (fieldList.get(i).getDataTypeOptionList().contains("MSEQ")) {
						wrkStr = (String)value.toString();
						if (!wrkStr.equals("")) {
							value = XFUtility.getUserExpressionOfMSeq(Integer.parseInt(wrkStr), session_);
						}
					}

				}
				break;
			}
		}
		return value;
	}
}

class XF290_Field extends Object implements XFScriptableField {
	private static final long serialVersionUID = 1L;
	private static ResourceBundle res = ResourceBundle.getBundle("xeadDriver.Res");
	org.w3c.dom.Element tableElement = null;
	private String dataSourceName_ = "";
	private String tableID_ = "";
	private String tableAlias_ = "";
	private String fieldID_ = "";
	private String dataType = "";
	private int dataSize = 5;
	private int decimalSize = 0;
	private String dataTypeOptions = "";
	private ArrayList<String> dataTypeOptionList;
	private XFTextField xFTextField = null;
	private XFEditableField component = null;
	private boolean isNullable = true;
	private boolean isKey = false;
	private boolean isFieldOnPrimaryTable = false;
	private boolean isVirtualField = false;
	private boolean isRangeKeyFieldValid = false;
	private boolean isRangeKeyFieldExpire = false;
	private boolean isImage = false;
	private boolean isKubunField = false;
	private XF290 dialog_;
	private ArrayList<String> kubunValueList = new ArrayList<String>();
	private ArrayList<String> kubunTextList = new ArrayList<String>();

	public XF290_Field(String dataSourceName, XF290 dialog){
		super();
		//
		dataSourceName_ = dataSourceName;
		dialog_ = dialog;
		//
		StringTokenizer workTokenizer = new StringTokenizer(dataSourceName_, ";" );
		dataSourceName_ = workTokenizer.nextToken().trim();
		workTokenizer = new StringTokenizer(dataSourceName_, "." );
		tableAlias_ = workTokenizer.nextToken();
		tableID_ = dialog_.getTableIDOfTableAlias(tableAlias_);
		fieldID_ =workTokenizer.nextToken();
		//
		setupVariants();
		//
		dialog_.getEngineScriptBindings().put(this.getFieldIDInScript(), (XFScriptableField)this);
	}

	public XF290_Field(String tableID, String tableAlias, String fieldID, XF290 dialog){
		super();
		//
		tableID_ = tableID;
		fieldID_ = fieldID;
		dialog_ = dialog;
		//
		if (tableAlias.equals("")) {
			tableAlias_ = tableID;
		} else {
			tableAlias_ = tableAlias;
		}
		dataSourceName_ = tableAlias_ + "." + fieldID_;
		//
		setupVariants();
		//
		dialog_.getEngineScriptBindings().put(this.getFieldIDInScript(), (XFScriptableField)this);
	}

	public void setupVariants(){
		//
		if (tableID_.equals(dialog_.getPrimaryTable().getTableID()) && tableAlias_.equals(tableID_)) {
			isFieldOnPrimaryTable = true;
			//
			ArrayList<String> keyNameList = dialog_.getKeyFieldList();
			for (int i = 0; i < keyNameList.size(); i++) {
				if (keyNameList.get(i).equals(fieldID_)) {
					isKey = true;
					break;
				}
			}
		}
		//
		org.w3c.dom.Element workElement = dialog_.getSession().getFieldElement(tableID_, fieldID_);
		if (workElement == null) {
			JOptionPane.showMessageDialog(null, tableID_ + "." + fieldID_ + res.getString("FunctionError11"));
		}
		dataType = workElement.getAttribute("Type");
		dataTypeOptions = workElement.getAttribute("TypeOptions");
		dataTypeOptionList = XFUtility.getOptionList(dataTypeOptions);
		dataSize = Integer.parseInt(workElement.getAttribute("Size"));
		if (dataSize > 50) {
			dataSize = 50;
		}
		if (!workElement.getAttribute("Decimal").equals("")) {
			decimalSize = Integer.parseInt(workElement.getAttribute("Decimal"));
		}
		if (workElement.getAttribute("Nullable").equals("F")) {
			isNullable = false;
		}
		if (dataTypeOptionList.contains("VIRTUAL")) {
			isVirtualField = true;
		}
		//
		String wrkStr = XFUtility.getOptionValueWithKeyword(dataTypeOptions, "KUBUN");
		if (!wrkStr.equals("")) {
			try {
				isKubunField = true;
				String wrk = "";
				Statement statement = dialog_.getSession().getConnection().createStatement();
				ResultSet result = statement.executeQuery("select * from " + dialog_.getSession().getTableNameOfUserVariants() + " where IDUSERKUBUN = '" + wrkStr + "'");
				while (result.next()) {
					//
					kubunValueList.add(result.getString("KBUSERKUBUN").trim());
					wrk = result.getString("TXUSERKUBUN").trim();
					kubunTextList.add(wrk);
				}
				result.close();
			} catch (SQLException e) {
				e.printStackTrace(dialog_.getExceptionStream());
				dialog_.setErrorAndCloseFunction();
			}
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
		xFTextField = new XFTextField(this.getBasicType(), dataSize, decimalSize, dataTypeOptions, "");
		xFTextField.setLocation(5, 0);
		component = xFTextField;
	}

	public String getDataSourceName(){
		return dataSourceName_;
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

	public String getTableID(){
		return tableID_;
	}

	public int getDataSize(){
		return dataSize;
	}

	public int getDecimalSize(){
		return decimalSize;
	}

	public ArrayList<String> getDataTypeOptionList() {
		return dataTypeOptionList;
	}

	public boolean isNullable(){
		return isNullable;
	}

	public boolean isNull(){
		return XFUtility.isNullValue(this.getBasicType(), component.getInternalValue());
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

	public String getBasicType(){
		return XFUtility.getBasicTypeOf(dataType);
	}

	public boolean isFieldOnPrimaryTable(){
		return isFieldOnPrimaryTable;
	}

	public boolean isKey(){
		return isKey;
	}

	public boolean isKubunField(){
		return isKubunField;
	}

	public void setValueOfResultSet(ResultSet result){
		try {
			if (this.isVirtualField) {
				if (this.isRangeKeyFieldExpire()) {
					component.setValue(XFUtility.calculateExpireValue(this.getTableElement(), result, dialog_.getSession()));
				}
			} else {
				String basicType = this.getBasicType();
				//
				if (basicType.equals("INTEGER") || basicType.equals("FLOAT")) {
					String value = result.getString(this.getFieldID());
					if (this.isFieldOnPrimaryTable) {
						if (value == null) {
							component.setValue("0");
						} else {
							component.setValue(result.getString(this.getFieldID()));
						}
					} else {
						if (basicType.equals("INTEGER")) {
							if (value == null || value.equals("")) {
								component.setValue("");
							} else {
								int pos = value.indexOf(".");
								if (pos >= 0) {
									value = value.substring(0, pos);
								}
								component.setValue(Integer.parseInt(value));
							}
						}
						if (basicType.equals("FLOAT")) {
							if (value == null || value.equals("")) {
								component.setValue("");
							} else {
								component.setValue(Float.parseFloat(value));
							}
						}
					}
				}
				//
				if (basicType.equals("STRING") || basicType.equals("TIME") || basicType.equals("DATETIME")) {
					String value = result.getString(this.getFieldID());
					if (value == null) {
						component.setValue("");
					} else {
						component.setValue(value.trim());
					}
				}
				//
				if (basicType.equals("DATE")) {
					component.setValue(result.getDate(this.getFieldID()));
				}
				//
				component.setOldValue(component.getInternalValue());
			}
		} catch (SQLException e) {
			e.printStackTrace(dialog_.getExceptionStream());
			dialog_.setErrorAndCloseFunction();
		}
	}

	public void setValue(Object object){
		XFUtility.setValueToEditableField(this.getBasicType(), object, component);
	}

	public Object getNullValue(){
		return XFUtility.getNullValueOfBasicType(this.getBasicType());
	}

	public Object getInternalValue(){
		Object returnObj = null;
		String basicType = this.getBasicType();
		//
		if (basicType.equals("INTEGER") || basicType.equals("FLOAT") || basicType.equals("STRING")) {
			String value = (String)component.getInternalValue();
			if (value == null) {
				value = "";
			}
			returnObj = value;
		}
		//
		if (basicType.equals("DATE")) {
			returnObj = (String)component.getInternalValue();
		}
		//
		if (basicType.equals("DATETIME")) {
			returnObj = (String)component.getInternalValue();
		}
		//
		return returnObj;
	}

	public Object getExternalValue() {
		Object returnObj = getInternalValue();
		//
		if (this.isKubunField) {
			if (returnObj == null) {
				returnObj = "";
			} else {
				String wrkStr = returnObj.toString().trim();
				for (int i = 0; i < kubunValueList.size(); i++) {
					if (kubunValueList.get(i).equals(wrkStr)) {
						returnObj = kubunTextList.get(i);
						break;
					}
				}
			}
		} else {
			if (dataTypeOptionList.contains("MSEQ")) {
				returnObj = XFUtility.getUserExpressionOfMSeq(Integer.parseInt((String)component.getInternalValue()), dialog_.getSession());
			}
			if (dataTypeOptionList.contains("YMONTH") || dataTypeOptionList.contains("FYEAR")) {
				returnObj = XFUtility.getUserExpressionOfYearMonth(component.getInternalValue().toString(), dialog_.getSession().getDateFormat());
			}
		}
		//
		return returnObj;
	}

	public Object getValue() {
		Object returnObj = null;
		//
		if (this.getBasicType().equals("INTEGER")) {
			returnObj = Integer.parseInt((String)component.getInternalValue());
		} else {
			if (this.getBasicType().equals("FLOAT")) {
				returnObj = Float.parseFloat((String)component.getInternalValue());
			} else {
				if (component.getInternalValue() == null) {
					returnObj = "";
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
		return getValue();
	}

	public boolean isEditable() {
		return false;
	}

	public void setEditable(boolean isEditable) {
	}

	public void setError(String message) {
	}

	public String getError() {
		return "";
	}

	public void setColor(String color) {
	}

	public String getColor() {
		return "";
	}
}

class XF290_Phrase extends Object {
	private static final long serialVersionUID = 1L;
	org.w3c.dom.Element phraseElement_ = null;
	private String block = "";
	private String alignment = "";
	private ArrayList<String> valueKeywordList = new ArrayList<String>();
	private ArrayList<String> dataSourceNameList = new ArrayList<String>();
	private String fontID = "";
	private int fontSize = 0;
	private float alignmentMargin;
	private float spacingAfter;
	private String fontStyle = "";

	public XF290_Phrase(org.w3c.dom.Element phraseElement){
		super();
		//
		phraseElement_ = phraseElement;
		//
		block = phraseElement_.getAttribute("Block");
		alignment = phraseElement_.getAttribute("Alignment");
		try {
			alignmentMargin = Float.parseFloat(phraseElement_.getAttribute("AlignmentMargin"));
		} catch (NumberFormatException e) {
			alignmentMargin = 0f;
		}
		//
		try {
			spacingAfter = Float.parseFloat(phraseElement_.getAttribute("SpacingAfter"));
		} catch (NumberFormatException e) {
			spacingAfter = 0f;
		}
		//
		StringTokenizer workTokenizer;
		String wrkStr;
		String phraseValue = phraseElement_.getAttribute("Value");
		int pos1 = phraseValue.indexOf("&", 0);
		int pos2;
		while (pos1 > -1) {
			pos2 = phraseValue.indexOf("&", pos1+1);
			if (pos2 == -1) {
				valueKeywordList.add(phraseValue.substring(pos1, phraseValue.length()).trim());
				break;
			} else {
				valueKeywordList.add(phraseValue.substring(pos1, pos2).trim());
			}
			pos1 = pos2;
		}
		for (int i = 0; i < valueKeywordList.size(); i++) {
			if (valueKeywordList.get(i).contains("&DataSource(")) {
				pos1 = valueKeywordList.get(i).indexOf(")", 0);
				workTokenizer = new StringTokenizer(valueKeywordList.get(i).substring(12, pos1), ";");
				wrkStr = workTokenizer.nextToken().trim();
				dataSourceNameList.add(wrkStr);
			}
			if (valueKeywordList.get(i).contains("&Image(")) {
				pos1 = valueKeywordList.get(i).indexOf(")", 0);
				workTokenizer = new StringTokenizer(valueKeywordList.get(i).substring(7, pos1), ";");
				wrkStr = workTokenizer.nextToken().trim();
				dataSourceNameList.add(wrkStr);
			}
			if (valueKeywordList.get(i).contains("&Barcode(")) {
				pos1 = valueKeywordList.get(i).indexOf(")", 0);
				workTokenizer = new StringTokenizer(valueKeywordList.get(i).substring(9, pos1), ";");
				wrkStr = workTokenizer.nextToken().trim();
				dataSourceNameList.add(wrkStr);
			}
		}
		//
		fontID = phraseElement_.getAttribute("FontID");
		fontSize = Integer.parseInt(phraseElement_.getAttribute("FontSize"));
		fontStyle = phraseElement_.getAttribute("FontStyle");
	}

	public String getBlock(){
		return block;
	}

	public float getAlignmentMargin(){
		return alignmentMargin;
	}

	public float getSpacingAfter(){
		return spacingAfter;
	}

	public int getAlignment(){
		int result = 0;
		if (alignment.equals("LEFT")) {
			result = com.lowagie.text.Element.ALIGN_LEFT;
		}
		if (alignment.equals("CENTER")) {
			result = com.lowagie.text.Element.ALIGN_CENTER;
		}
		if (alignment.equals("RIGHT")) {
			result = com.lowagie.text.Element.ALIGN_RIGHT;
		}
		return result;
	}

	public ArrayList<String> getValueKeywordList(){
		return valueKeywordList;
	}

	public ArrayList<String> getDataSourceNameList(){
		return dataSourceNameList;
	}

	public String getFontID(){
		return fontID;
	}

	public int getFontSize(){
		return fontSize;
	}

	public int getFontStyle(){
		int result = com.lowagie.text.Font.NORMAL;
		//
		if (fontStyle.equals("BOLD")) {
			result = com.lowagie.text.Font.BOLD;
		}
		if (fontStyle.equals("ITALIC")) {
			result = com.lowagie.text.Font.ITALIC;
		}
		if (fontStyle.contains("BOLDITALIC")) {
			result = com.lowagie.text.Font.BOLDITALIC;
		}
		if (fontStyle.equals("UNDERLINE")) {
			result = com.lowagie.text.Font.UNDERLINE;
		}
		//
		return result;
	}
}

class XF290_PrimaryTable extends Object {
	private static final long serialVersionUID = 1L;
	private org.w3c.dom.Element tableElement = null;
	private org.w3c.dom.Element functionElement_ = null;
	private String tableID = "";
	private String activeWhere = "";
	private ArrayList<String> keyFieldList = new ArrayList<String>();
	private ArrayList<String> orderByFieldList = new ArrayList<String>();
	private ArrayList<XFScript> scriptList = new ArrayList<XFScript>();
	private XF290 dialog_;
	private StringTokenizer workTokenizer;
	private String updateCounterID = "";

	public XF290_PrimaryTable(org.w3c.dom.Element functionElement, XF290 dialog){
		super();
		//
		functionElement_ = functionElement;
		dialog_ = dialog;
		//
		tableID = functionElement_.getAttribute("PrimaryTable");
		tableElement = dialog_.getSession().getTableElement(tableID);
		activeWhere = tableElement.getAttribute("ActiveWhere");
		updateCounterID = tableElement.getAttribute("UpdateCounter");
		if (updateCounterID.equals("")) {
			updateCounterID = XFUtility.DEFAULT_UPDATE_COUNTER;
		}
		//
		String wrkStr1;
		org.w3c.dom.Element workElement;
		//
		if (functionElement_.getAttribute("KeyFields").equals("")) {
			NodeList nodeList = tableElement.getElementsByTagName("Key");
			for (int i = 0; i < nodeList.getLength(); i++) {
				workElement = (org.w3c.dom.Element)nodeList.item(i);
				if (workElement.getAttribute("Type").equals("PK")) {
					workTokenizer = new StringTokenizer(workElement.getAttribute("Fields"), ";" );
					while (workTokenizer.hasMoreTokens()) {
						wrkStr1 = workTokenizer.nextToken();
						keyFieldList.add(wrkStr1);
					}
				}
			}
		} else {
			workTokenizer = new StringTokenizer(functionElement_.getAttribute("KeyFields"), ";" );
			while (workTokenizer.hasMoreTokens()) {
				keyFieldList.add(workTokenizer.nextToken());
			}
		}
		//
		workTokenizer = new StringTokenizer(functionElement_.getAttribute("OrderBy"), ";" );
		while (workTokenizer.hasMoreTokens()) {
			orderByFieldList.add(workTokenizer.nextToken());
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
	
	public String getName() {
		return tableElement.getAttribute("Name");
	}
	
	public String getSQLToSelect(){
		StringBuffer buf = new StringBuffer();
		//
		buf.append("select ");
		//
		boolean firstField = true;
		for (int i = 0; i < dialog_.getFieldList().size(); i++) {
			if (dialog_.getFieldList().get(i).isFieldOnPrimaryTable()
			&& !dialog_.getFieldList().get(i).isVirtualField()) {
				if (!firstField) {
					buf.append(",");
				}
				buf.append(dialog_.getFieldList().get(i).getFieldID());
				firstField = false;
			}
		}
		buf.append(",");
		buf.append(updateCounterID);
		//
		buf.append(" from ");
		buf.append(tableID);
		//
		if (orderByFieldList.size() > 0) {
			buf.append(" order by ");
			for (int i = 0; i < orderByFieldList.size(); i++) {
				if (i > 0) {
					buf.append(",");
				}
				buf.append(orderByFieldList.get(i));
			}
		}
		//
		buf.append(" where ") ;
		//
		int orderOfFieldInKey = 0;
		for (int i = 0; i < dialog_.getFieldList().size(); i++) {
			if (dialog_.getFieldList().get(i).isKey()) {
				if (orderOfFieldInKey > 0) {
					buf.append(" and ") ;
				}
				buf.append(dialog_.getFieldList().get(i).getFieldID()) ;
				buf.append("=") ;
				if (XFUtility.isLiteralRequiredBasicType(dialog_.getFieldList().get(i).getBasicType())) {
					buf.append("'") ;
					buf.append(dialog_.getFieldList().get(i).getInternalValue()) ;
					buf.append("'") ;
				} else {
					buf.append(dialog_.getFieldList().get(i).getInternalValue()) ;
				}
				orderOfFieldInKey++;
			}
		}
		//
		if (!activeWhere.equals("")) {
			buf.append(" and (");
			buf.append(activeWhere);
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
	
	public ArrayList<String> getKeyFieldList(){
		return keyFieldList;
	}
	
	public ArrayList<XFScript> getScriptList(){
		return scriptList;
	}
	
	public boolean isValidDataSource(String tableID, String tableAlias, String fieldID) {
		boolean isValid = false;
		XF290_ReferTable referTable;
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
			for (int i = 0; i < dialog_.getReferTableList().size(); i++) {
				referTable = dialog_.getReferTableList().get(i);
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

	public void runScript(String event1, String event2) throws ScriptException {
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
			dialog_.evalScript(validScriptList.get(i).getName(), validScriptList.get(i).getScriptText());
		}
	}
}

class XF290_ReferTable extends Object {
	private static final long serialVersionUID = 1L;
	private org.w3c.dom.Element referElement_ = null;
	private org.w3c.dom.Element tableElement = null;
	private XF290 dialog_ = null;
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

	public XF290_ReferTable(org.w3c.dom.Element referElement, XF290 dialog){
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
			org.w3c.dom.Element workElement = dialog_.getSession().getTablePKElement(tableID);
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

	public String getSelectSQL(boolean isToGetRecordsForComboBox){
		org.w3c.dom.Element workElement;
		int count;
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
		for (int i = 0; i < toKeyFieldIDList.size(); i++) {
			if (count > 0) {
				buf.append(",");
			}
			count++;
			buf.append(toKeyFieldIDList.get(i));
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
		//
		StringTokenizer workTokenizer;
		String keyFieldID, keyFieldTableID;
		count = 0;
		boolean isToBeWithValue;
		for (int i = 0; i < toKeyFieldIDList.size(); i++) {
			if (toKeyFieldIDList.get(i).equals(rangeKeyFieldValid)) {
				rangeKeyFieldSearch = withKeyFieldIDList.get(i);
			} else {
				if (isToGetRecordsForComboBox) {
					//
					// If the field is part of PK of the primary table or one of other join table, value of field should be within WHERE to SELECT records. //
					for (int j = 0; j < dialog_.getFieldList().size(); j++) {
						if (withKeyFieldIDList.get(i).equals(dialog_.getFieldList().get(j).getDataSourceName())) {
							isToBeWithValue = false;
							//
							workTokenizer = new StringTokenizer(withKeyFieldIDList.get(i), "." );
							keyFieldTableID = workTokenizer.nextToken();
							keyFieldID = workTokenizer.nextToken();
							if (keyFieldTableID.equals(dialog_.getPrimaryTable().getTableID())) {
								for (int k = 0; k < dialog_.getKeyFieldList().size(); k++) {
									if (keyFieldID.equals(dialog_.getKeyFieldList().get(k))) {
										isToBeWithValue = true;
									}
								}
							} else {
								if (!keyFieldTableID.equals(this.tableAlias)) {
									isToBeWithValue = true;
								}
							}
							//
							if (isToBeWithValue) {
								if (count == 0) {
									buf.append(" where ");
								} else {
									buf.append(" and ");
								}
								buf.append(toKeyFieldIDList.get(i));
								buf.append("=");
								buf.append(XFUtility.getTableOperationValue(dialog_.getFieldList().get(j).getBasicType(), dialog_.getFieldList().get(j).getInternalValue())) ;
								count++;
								break;
							}
						}
					}
				} else {
					if (count == 0) {
						buf.append(" where ");
					} else {
						buf.append(" and ");
					}
					buf.append(toKeyFieldIDList.get(i));
					buf.append("=");
					for (int j = 0; j < dialog_.getFieldList().size(); j++) {
						if (withKeyFieldIDList.get(i).equals(dialog_.getFieldList().get(j).getDataSourceName())) {
							buf.append(XFUtility.getTableOperationValue(dialog_.getFieldList().get(j).getBasicType(), dialog_.getFieldList().get(j).getInternalValue())) ;
							break;
						}
					}
					count++;
				}
			}
		}
		//
		if (!activeWhere.equals("")) {
			if (count == 0) {
				buf.append(" where ");
			} else {
				buf.append(" and ");
			}
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

	public boolean isKeyNullable() {
		boolean isKeyNullable = false;
		//
		for (int i = 0; i < withKeyFieldIDList.size(); i++) {
			for (int j = 0; j < dialog_.getFieldList().size(); j++) {
				if (withKeyFieldIDList.get(i).equals(dialog_.getFieldList().get(j).getTableAlias() + "." + dialog_.getFieldList().get(j).getFieldID())) {
					if (dialog_.getFieldList().get(j).isNullable()) {
						isKeyNullable = true;
						break;
					}
				}
			}
		}
		//
		return isKeyNullable;
	}

	public boolean isKeyNull() {
		boolean isKeyNull = false;
		//
		for (int i = 0; i < withKeyFieldIDList.size(); i++) {
			for (int j = 0; j < dialog_.getFieldList().size(); j++) {
				if (withKeyFieldIDList.get(i).equals(dialog_.getFieldList().get(j).getTableAlias() + "." + dialog_.getFieldList().get(j).getFieldID())) {
					if (dialog_.getFieldList().get(j).isNull()) {
						isKeyNull = true;
						break;
					}
				}
			}
		}
		//
		return isKeyNull;
	}

	public ArrayList<String> getKeyFieldIDList(){
		return toKeyFieldIDList;
	}

	public ArrayList<String> getFieldIDList(){
		return fieldIDList;
	}

	public ArrayList<String> getWithKeyFieldIDList(){
		return withKeyFieldIDList;
	}

	public void setKeyFieldValues(XFHashMap keyValues){
		for (int i = 0; i < withKeyFieldIDList.size(); i++) {
			for (int j = 0; j < dialog_.getFieldList().size(); j++) {
				if (dialog_.getFieldList().get(j).getDataSourceName().equals(withKeyFieldIDList.get(i))) {
					dialog_.getFieldList().get(j).setValue(keyValues.getValue(withKeyFieldIDList.get(i)));
					break;
				}
			}
		}
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
					Object valueKey = dialog_.getInternalValueOfFieldByName((rangeKeyFieldSearch));
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
				Object valueKey = dialog_.getInternalValueOfFieldByName((rangeKeyFieldSearch));
				Object valueFrom = result.getObject(rangeKeyFieldValid);
				Object valueThru = result.getObject(rangeKeyFieldExpire);
				if (valueThru == null) {
					valueThru = "9999-12-31";
				}
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