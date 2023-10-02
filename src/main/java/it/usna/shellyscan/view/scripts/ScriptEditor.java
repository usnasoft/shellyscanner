package it.usna.shellyscan.view.scripts;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.Window;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.model.device.g2.modules.Script;
import it.usna.shellyscan.view.BasicEditorPanel;
import it.usna.shellyscan.view.util.Msg;

/**
 * A small text editor where "load" and "save" relies on "scipts" notes
 * @author usna
 */
public class ScriptEditor extends JFrame {
	private static final long serialVersionUID = 1L;
	
	public ScriptEditor(Window owner, Script script) throws IOException {
//		super(owner, LABELS.getString("dlgScriptEditorTitle") + " - " + scipt.getName());
		super(LABELS.getString("dlgScriptEditorTitle") + " - " + script.getName());
		setIconImage(Toolkit.getDefaultToolkit().createImage(getClass().getResource(Main.ICON)));
		
		BasicEditorPanel editor = new BasicEditorPanel(this, script.getCode());
		getContentPane().add(editor);

		// bottom buttons
		JPanel southPanel = new JPanel(new GridLayout(1, 3, 0, 0));
		southPanel.add(new JLabel());
		
		JPanel buttonsPanel = new JPanel(new FlowLayout());
		getContentPane().add(southPanel, BorderLayout.SOUTH);
		
		JButton saveButton = new JButton(LABELS.getString("btnUpload"));
		saveButton.addActionListener(e -> {
			try {
				ScriptEditor.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				String res = script.putCode(editor.getText());
				if(res != null) {
					Msg.errorMsg(this, res);
				}
			} finally {
				ScriptEditor.this.setCursor(Cursor.getDefaultCursor());
			}
		});
		buttonsPanel.add(saveButton);
		
		JButton closeButton = new JButton(LABELS.getString("dlgClose"));
		closeButton.addActionListener(e -> dispose());
		buttonsPanel.add(closeButton);
		
		southPanel.add(buttonsPanel);
		
		JLabel caretLabel = new JLabel(editor.getCaretRow() + " : " + editor.getCaretColumn() + " ", SwingConstants.RIGHT);
		southPanel.add(caretLabel);
		
		editor.addCaretListener(e -> {
			caretLabel.setText(editor.getCaretRow() + " : " + editor.getCaretColumn() + " ");
		});
		
		setSize(800, 600);
		setVisible(true);
		editor.requestFocus();
		setLocationRelativeTo(owner);
	}
}