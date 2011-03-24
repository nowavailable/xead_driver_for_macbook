package xeadDriver;

import java.awt.*;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.PlainDocument;
import java.awt.event.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ResourceBundle;

public class ModifyPasswordDialog extends JDialog {
	private static final long serialVersionUID = 1L;
	private static ResourceBundle res = ResourceBundle.getBundle("xeadDriver.Res");
	private JPanel jPanelMain = new JPanel();
	private JButton jButtonOK = new JButton();
	private JButton jButtonClose = new JButton();
	private JLabel jLabelUserName = new JLabel();
	private JTextField jTextFieldUserName = new JTextField();
	private JLabel jLabelPasswordCurrent = new JLabel();
	private JPasswordField jPasswordCurrent = new JPasswordField();
	private JLabel jLabelPasswordNew = new JLabel();
	private JPasswordField jPasswordNew = new JPasswordField();
	private Session session = null;
	private String userID = "";
	private Connection connection = null;
	private Statement statement = null;
	private boolean modified = false;
	private Image imageTitle;

	public ModifyPasswordDialog(Session session) {
		super(session, "", true);
		try {
			//
		 	imageTitle = Toolkit.getDefaultToolkit().createImage(xeadDriver.ModifyPasswordDialog.class.getResource("ikey.png"));
		 	this.setIconImage(imageTitle);
			this.session = session;
			this.connection = session.getConnection();
			this.setTitle(res.getString("ModifyPassword"));
			jPanelMain.setBorder(BorderFactory.createEtchedBorder());
			jPanelMain.setPreferredSize(new Dimension(270, 163));
			jPanelMain.setLayout(null);
			//
			jLabelUserName.setHorizontalAlignment(SwingConstants.RIGHT);
			jLabelUserName.setBounds(new Rectangle(10, 17, 80, 25));
			jLabelUserName.setFont(new java.awt.Font("Dialog", 0, 12));
			jLabelUserName.setText(res.getString("UserName"));
			jTextFieldUserName.setFont(new java.awt.Font("Dialog", 0, 12));
			jTextFieldUserName.setBounds(new Rectangle(95, 17, 155, 25));
			jTextFieldUserName.setEditable(false);
			//
			jLabelPasswordCurrent.setHorizontalAlignment(SwingConstants.RIGHT);
			jLabelPasswordCurrent.setBounds(new Rectangle(10, 50, 80, 25));
			jLabelPasswordCurrent.setFont(new java.awt.Font("Dialog", 0, 12));
			jLabelPasswordCurrent.setText(res.getString("PasswordCurrent"));
			jPasswordCurrent.setFont(new java.awt.Font("Dialog", 0, 12));
			jPasswordCurrent.setBounds(new Rectangle(95, 50, 130, 25));
			jPasswordCurrent.setDocument(new LimitedDocument(10));
			//
			jLabelPasswordNew.setHorizontalAlignment(SwingConstants.RIGHT);
			jLabelPasswordNew.setBounds(new Rectangle(10, 83, 80, 25));
			jLabelPasswordNew.setFont(new java.awt.Font("Dialog", 0, 12));
			jLabelPasswordNew.setText(res.getString("PasswordNew"));
			jPasswordNew.setFont(new java.awt.Font("Dialog", 0, 12));
			jPasswordNew.setBounds(new Rectangle(95, 83, 130, 25));
			jPasswordNew.setDocument(new LimitedDocument(10));
			//
			jButtonClose.setBounds(new Rectangle(20, 125, 90, 25));
			jButtonClose.setFont(new java.awt.Font("Dialog", 0, 12));
			jButtonClose.setText(res.getString("Close"));
			jButtonClose.addActionListener(new ModifyPasswordDialog_jButtonClose_actionAdapter(this));
			jButtonOK.setBounds(new Rectangle(160, 125, 90, 25));
			jButtonOK.setFont(new java.awt.Font("Dialog", 0, 12));
			jButtonOK.setText(res.getString("Modify"));
			jButtonOK.addActionListener(new ModifyPasswordDialog_jButtonOK_actionAdapter(this));
			//
			this.getContentPane().add(jPanelMain,  BorderLayout.CENTER);
			jPanelMain.add(jButtonClose, null);
			jPanelMain.add(jButtonOK, null);
			jPanelMain.add(jLabelUserName, null);
			jPanelMain.add(jTextFieldUserName, null);
			jPanelMain.add(jLabelPasswordCurrent, null);
			jPanelMain.add(jPasswordCurrent, null);
			jPanelMain.add(jLabelPasswordNew, null);
			jPanelMain.add(jPasswordNew, null);
			pack();
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	public boolean passwordModified() {
		userID = session.getUserID();
		jTextFieldUserName.setText(session.getUserName());
		jPanelMain.getRootPane().setDefaultButton(jButtonOK);
		Dimension dlgSize = this.getPreferredSize();
		Dimension scrSize = Toolkit.getDefaultToolkit().getScreenSize();
		this.setLocation((scrSize.width - dlgSize.width) / 2, (scrSize.height - dlgSize.height) / 2);
		this.pack();
		//
		jPasswordCurrent.setText("");
		jPasswordNew.setText("");
		jPasswordCurrent.requestFocus();
		super.setVisible(true);
		//
		return modified;
	}

	void jButtonOK_actionPerformed(ActionEvent e) {
		try {
			setCursor(new Cursor(Cursor.WAIT_CURSOR));
			if (passwordValidated()) {
				modified = true;
				this.setVisible(false);
			} else {
				jPasswordCurrent.setText("");
				jPasswordNew.setText("");
				jPasswordCurrent.requestFocus();
			}
		} finally {
			setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		}
	}

	boolean passwordValidated() {
		String passwordNew = "";
		String passwordCurrent = "";
		boolean validated = false;
		//
		passwordCurrent = new String(jPasswordCurrent.getPassword());
		String passwordCurrentDigested = session.getDigestAdapter().digest(passwordCurrent);
		//
		passwordNew = new String(jPasswordNew.getPassword());
		String passwordNewDigested = session.getDigestAdapter().digest(passwordNew);
		//
		if (passwordNew.length() < 5) {
			JOptionPane.showMessageDialog(this, res.getString("ModifyPasswordError1"));
		} else {
			try {
				StringBuffer statementBuf = new StringBuffer();
				statementBuf.append("update ");
				statementBuf.append(session.getTableNameOfUser());
				statementBuf.append(" set TXPASSWORD = '") ;
				statementBuf.append(passwordNewDigested) ;
				statementBuf.append("' where IDUSER = '") ;
				statementBuf.append(userID) ;
				statementBuf.append("' and TXPASSWORD = '") ;
				statementBuf.append(passwordCurrentDigested);
				statementBuf.append("'") ;
				String sql = statementBuf.toString();
				//
				statement = connection.createStatement();
				int count = statement.executeUpdate(sql);
				if (count == 1) {
					validated = true;
				} else {
					JOptionPane.showMessageDialog(this, res.getString("ModifyPasswordError2"));
				}
				//
				connection.commit();
				//
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		//
		return validated;
	}

	void jButtonClose_actionPerformed(ActionEvent e) {
		this.setVisible(false);
	}

	class LimitedDocument extends PlainDocument {
		private static final long serialVersionUID = 1L;
		int limit;
		LimitedDocument(int limit) {
			this.limit = limit; 
		}
		public void insertString(int offset, String str, AttributeSet a) {
			if (offset >= limit ) {
				return;
			}
			try {
				super.insertString( offset, str, a );
			} catch(Exception e ) {
			}
		}
	}
}

class ModifyPasswordDialog_jButtonOK_actionAdapter implements java.awt.event.ActionListener {
	ModifyPasswordDialog adaptee;
	ModifyPasswordDialog_jButtonOK_actionAdapter(ModifyPasswordDialog adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.jButtonOK_actionPerformed(e);
  }
}

class ModifyPasswordDialog_jButtonClose_actionAdapter implements java.awt.event.ActionListener {
	ModifyPasswordDialog adaptee;
	ModifyPasswordDialog_jButtonClose_actionAdapter(ModifyPasswordDialog adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.jButtonClose_actionPerformed(e);
  }
}