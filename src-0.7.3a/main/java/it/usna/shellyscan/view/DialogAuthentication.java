package it.usna.shellyscan.view;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.GridLayout;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import it.usna.shellyscan.Main;
import javax.swing.JCheckBox;

public class DialogAuthentication extends JDialog {
	private static final long serialVersionUID = 1L;
	
	private JLabel messageLabel = new JLabel();
	private String user = null;
	private char[] pwd = null;
	private char pwdEchoChar;
	final JTextField fieldUser;
	JLabel userLabel = new JLabel();

	public DialogAuthentication(String title, String userLabelText, String pwdLabel, String userInitVal) {
		super(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow(), title, Dialog.ModalityType.APPLICATION_MODAL);
		setIconImage(Toolkit.getDefaultToolkit().createImage(getClass().getResource(Main.ICON)));
		BorderLayout borderLayout = (BorderLayout) getContentPane().getLayout();
		borderLayout.setVgap(5);
		borderLayout.setHgap(5);
		((JComponent)getContentPane()).setBorder(BorderFactory.createEmptyBorder(6, 6, 0, 6));

		JPanel panelForm = new JPanel();
		getContentPane().add(panelForm, BorderLayout.CENTER);
		panelForm.setLayout(new GridLayout(3, 1, 0, 5));
		
		fieldUser = new JTextField(userInitVal);
		panelForm.add(fieldUser);
		fieldUser.setColumns(32);
		
		final JPasswordField fieldfPwd = new JPasswordField();
		panelForm.add(fieldfPwd);
		fieldfPwd.setColumns(32);
		pwdEchoChar = fieldfPwd.getEchoChar();
		
		JCheckBox chckbxShowPwd = new JCheckBox(Main.LABELS.getString("labelShowPwd"));
		panelForm.add(chckbxShowPwd);
		chckbxShowPwd.addItemListener(e -> fieldfPwd.setEchoChar((e.getStateChange() == java.awt.event.ItemEvent.SELECTED) ? '\0' : pwdEchoChar));
		
		JPanel panelLabels = new JPanel();
		getContentPane().add(panelLabels, BorderLayout.WEST);
		panelLabels.setLayout(new GridLayout(3, 1, 0, 5));
		
		userLabel.setText(userLabelText);
		panelLabels.add(userLabel);
		panelLabels.add(new JLabel(pwdLabel));
		
		getContentPane().add(messageLabel, BorderLayout.NORTH);
		
		JPanel buttonsPanel = new JPanel();
		getContentPane().add(buttonsPanel, BorderLayout.SOUTH);

		JButton okButton = new JButton(Main.LABELS.getString("dlgOK"));
		okButton.addActionListener(event -> {
			user = fieldUser.getText();
			pwd = fieldfPwd.getPassword();
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
	
	public void showUser(boolean show) {
		fieldUser.setVisible(show);
		userLabel.setVisible(show);
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