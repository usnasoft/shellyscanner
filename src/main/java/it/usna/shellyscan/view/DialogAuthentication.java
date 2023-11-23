package it.usna.shellyscan.view;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import it.usna.shellyscan.Main;

public class DialogAuthentication extends JDialog {
	private static final long serialVersionUID = 1L;
	
	private JLabel messageLabel = new JLabel();
	private String user = null;
	private char[] pwd = null;
	private final JTextField fieldUser = new JTextField();
	private final JPasswordField fieldPwd = new JPasswordField();
	private final JPasswordField fieldConfirmPwd = new JPasswordField();

	/**
	 * @wbp.parser.constructor
	 */
	public DialogAuthentication(Window owner, String title, String userLabel, String pwdLabel, String confirmPwdLabel, String noPwdLabel) {
		super(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
		init(userLabel, pwdLabel, confirmPwdLabel, noPwdLabel);
	}
	
	public DialogAuthentication(Window owner, String title, String userLabel, String pwdLabel, String confirmPwdLabel) {
		super(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
		init(userLabel, pwdLabel, confirmPwdLabel, null);
	}
	
	public DialogAuthentication(Window owner, String title, String userLabelText, String pwdLabelText) {
		super(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
		init(userLabelText, pwdLabelText, null, null);
	}
	
	public DialogAuthentication(String title, String userLabelText, String pwdLabelText) {
		this(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow(), title, userLabelText, pwdLabelText);
	}
	
	private void init(String userLabelText, String pwdLabelText, String confLabelText, String noPwdLabel) {
		setIconImage(Toolkit.getDefaultToolkit().createImage(Main.ICON));
		BorderLayout borderLayout = (BorderLayout) getContentPane().getLayout();
		borderLayout.setVgap(10);
		borderLayout.setHgap(5);
		((JComponent)getContentPane()).setBorder(BorderFactory.createEmptyBorder(6, 6, 0, 6));
		
		JPanel panelLabels = new JPanel();
		getContentPane().add(panelLabels, BorderLayout.WEST);
		panelLabels.setLayout(new GridLayout(0, 1, 0, 5));
		
		JPanel panelFields = new JPanel();
		getContentPane().add(panelFields, BorderLayout.CENTER);
		panelFields.setLayout(new GridLayout(0, 1, 0, 5));

		if(userLabelText != null) {
			panelLabels.add(new JLabel(userLabelText));
			panelFields.add(fieldUser);
			fieldUser.setColumns(32);
		}
		
		panelLabels.add(new JLabel(pwdLabelText));
		panelFields.add(fieldPwd);
		fieldPwd.setColumns(32);
		final char pwdEchoChar = fieldPwd.getEchoChar();

		if(confLabelText != null) {
			panelLabels.add(new JLabel(confLabelText));
			fieldConfirmPwd.setColumns(32);
			panelFields.add(fieldConfirmPwd);
		}
		
		JPanel chKPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
		JCheckBox chckbxShowPwd = new JCheckBox(Main.LABELS.getString("labelShowPwd"));
		JCheckBox chckbxNoPwd = new JCheckBox();
		panelLabels.add(new JLabel());
		chKPanel.add(chckbxShowPwd);
		if(noPwdLabel != null) {
			chckbxNoPwd.setText(noPwdLabel);
			chKPanel.add(chckbxNoPwd);
		}
		panelFields.add(chKPanel);

		getContentPane().add(messageLabel, BorderLayout.NORTH);
		
		JPanel buttonsPanel = new JPanel();
		getContentPane().add(buttonsPanel, BorderLayout.SOUTH);

		JButton okButton = new JButton(Main.LABELS.getString("dlgOK"));
		okButton.setEnabled(false);
		okButton.addActionListener(event -> {
			user = fieldUser.getText();
			pwd = fieldPwd.getPassword();
			setVisible(false);
		});
		buttonsPanel.add(okButton);

		JButton cancelButton = new JButton(Main.LABELS.getString("dlgClose"));
		cancelButton.addActionListener(event -> {
			user = null;
			pwd = null;
			setVisible(false);
		});
		buttonsPanel.add(cancelButton);
		
		getRootPane().setDefaultButton(okButton);
		
		DocumentListener fieldListener = new DocumentListener() {
			@Override
			public void changedUpdate(DocumentEvent e) {
				manageOK();
			}
			@Override
			public void removeUpdate(DocumentEvent e) {
				manageOK();
			}
			@Override
			public void insertUpdate(DocumentEvent e) {
				manageOK();
			}

			public void manageOK() {
				okButton.setEnabled(
						(userLabelText == null || fieldUser.getText().length() > 0) &&
						(fieldPwd.getPassword().length > 0 || chckbxNoPwd.isSelected()) &&
						(confLabelText == null || Arrays.equals(fieldConfirmPwd.getPassword(), fieldPwd.getPassword()))
						);
			}
		};
		
		fieldUser.getDocument().addDocumentListener(fieldListener);
		fieldPwd.getDocument().addDocumentListener(fieldListener);
		fieldConfirmPwd.getDocument().addDocumentListener(fieldListener);
		
		chckbxShowPwd.addItemListener(e -> {
			fieldPwd.setEchoChar((e.getStateChange() == java.awt.event.ItemEvent.SELECTED) ? '\0' : pwdEchoChar);
			fieldConfirmPwd.setEchoChar((e.getStateChange() == java.awt.event.ItemEvent.SELECTED) ? '\0' : pwdEchoChar);
		});
		chckbxNoPwd.addItemListener(e -> {
			boolean noPwd = e.getStateChange() == java.awt.event.ItemEvent.SELECTED;
			fieldPwd.setEnabled(noPwd == false);
			fieldConfirmPwd.setEnabled(noPwd == false);
			chckbxShowPwd.setEnabled(noPwd == false);
			fieldPwd.setText("");
			fieldConfirmPwd.setText("");
			fieldListener.changedUpdate(null);
		});
		
		setDefaultCloseOperation(/*EXIT_ON_CLOSE*/DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				cancelButton.doClick();
			}
		});
		
		pack();
		setLocationRelativeTo(getOwner());
	}
	
	public void setUser(String userVal) {
		fieldUser.setText(userVal);
	}
	
	public void editableUser(boolean ed) {
		fieldUser.setEnabled(ed);
	}
	
	@Override
	public void dispose() {
		if(pwd != null) {
			Arrays.fill(pwd, '\0'); // security reasons
		}
		super.dispose();
	}

	public void setMessage(String msg) {
		messageLabel.setText(msg);
		pack();
	}
	
	public String getUser() {
		return user;
	}
	
	public char[] getPassword() {
		return pwd;
	}
}