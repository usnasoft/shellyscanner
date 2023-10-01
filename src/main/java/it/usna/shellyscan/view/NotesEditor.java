package it.usna.shellyscan.view;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.Window;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.model.device.GhostDevice;
import it.usna.shellyscan.view.util.UtilMiscellaneous;

/**
 * A small text editor where "load" and "save" relies on GhostDevice notes
 * @author usna
 */
public class NotesEditor extends JFrame {
	private static final long serialVersionUID = 1L;
	
	public NotesEditor(Window owner, GhostDevice ghost) {
//		super(owner, LABELS.getString("action_notes_tooltip") + " - " + UtilMiscellaneous.getDescName(ghost));
		super(LABELS.getString("action_notes_tooltip") + " - " + UtilMiscellaneous.getDescName(ghost));
		setIconImage(Toolkit.getDefaultToolkit().createImage(getClass().getResource(Main.ICON)));
		
		BasicEditorPanel editor = new BasicEditorPanel(this, ghost.getNote());
		getContentPane().add(editor);

		// bottom buttons
		JPanel buttonsPanel = new JPanel(new FlowLayout());
		getContentPane().add(buttonsPanel, BorderLayout.SOUTH);
		
		JButton okButton = new JButton(LABELS.getString("dlgSave"));
		okButton.addActionListener(e -> {
			ghost.setNote(editor.getText());
			dispose();
		});
		buttonsPanel.add(okButton);
		
		JButton closeButton = new JButton(LABELS.getString("dlgClose"));
		closeButton.addActionListener(e -> dispose());
		buttonsPanel.add(closeButton);
		
		setSize(550, 400);
		setVisible(true);
		editor.requestFocus();
		setLocationRelativeTo(owner);
	}
}