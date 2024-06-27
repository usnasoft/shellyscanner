package it.usna.shellyscan.view;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.text.DefaultEditorKit;
import javax.swing.undo.UndoManager;

import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.controller.UsnaTextAction;
import it.usna.shellyscan.model.device.GhostDevice;
import it.usna.shellyscan.view.util.UtilMiscellaneous;
import it.usna.swing.dialog.FindReplaceDialog;

/**
 * A small text editor where "load" and "save" relies on GhostDevice notes
 * @author usna
 */
public class NotesEditor extends JFrame {
	private static final long serialVersionUID = 1L;
	private final static int MAX_KEYWORD = 24;
	private JPanel centerPanel = new JPanel(new BorderLayout());

	public NotesEditor(Window owner, GhostDevice ghost) {
		super(LABELS.getString("action_notes_tooltip") + " - " + UtilMiscellaneous.getDescName(ghost));
		setIconImages(owner.getIconImages());

		// Notes editor
		JTextArea notesEditor = new JTextArea();
		JScrollPane editorScrollPane = new JScrollPane();
		notesEditor.setText(ghost.getNote());
		editorScrollPane.setViewportView(notesEditor);
		UndoManager notesUndoManager = new UndoManager();
		notesEditor.getDocument().addUndoableEditListener(notesUndoManager);
		
		// Keyword
		JTextField textFieldKeyword = new JTextField(ghost.getKeyNote());
		textFieldKeyword.setColumns(MAX_KEYWORD);
		centerPanel.add(editorScrollPane, BorderLayout.CENTER);
		UndoManager keywordUndoManager = new UndoManager();
		textFieldKeyword.getDocument().addUndoableEditListener(keywordUndoManager);

		// Actions
		Action cutAction = new DefaultEditorKit.CutAction();
		cutAction.putValue(Action.SHORT_DESCRIPTION, LABELS.getString("btnCut"));
		cutAction.putValue(Action.SMALL_ICON, new ImageIcon(NotesEditor.class.getResource("/images/Clipboard_Cut24.png")));

		Action copyAction = new DefaultEditorKit.CopyAction();
		copyAction.putValue(Action.SHORT_DESCRIPTION, LABELS.getString("btnCopy"));
		copyAction.putValue(Action.SMALL_ICON, new ImageIcon(NotesEditor.class.getResource("/images/Clipboard_Copy24.png")));

		Action pasteAction = new DefaultEditorKit.PasteAction();
		pasteAction.putValue(Action.SHORT_DESCRIPTION, LABELS.getString("btnPaste"));
		pasteAction.putValue(Action.SMALL_ICON, new ImageIcon(NotesEditor.class.getResource("/images/Clipboard_Paste24.png")));

		Action undoAction = new UsnaTextAction("notesUno", "btnUndo", "/images/Undo24.png", (textComponent, e) -> {
			try {
				if(textComponent == textFieldKeyword) {
					keywordUndoManager.undo();
				} else {
					notesUndoManager.undo();
				}
			} catch(RuntimeException ex) {}
		});
		mapAction(KeyStroke.getKeyStroke(KeyEvent.VK_Z, MainView.SHORTCUT_KEY), undoAction, "undo_usna");

		Action redoAction = new UsnaTextAction("notesRedo", "btnRedo", "/images/Redo24.png", (textComponent, e) -> {
			try {
				if(textComponent == textFieldKeyword) {
					keywordUndoManager.redo();
				} else {
					notesUndoManager.redo();
				}
			} catch(RuntimeException ex) {}
		});
		mapAction(KeyStroke.getKeyStroke(KeyEvent.VK_Y, MainView.SHORTCUT_KEY), redoAction, "redo_usna");

		Action findAction = new UsnaTextAction("btnFindNote", "btnFind", "/images/Search24.png", (textComponent, e) -> {
			FindReplaceDialog f = new FindReplaceDialog(this, textComponent, true);
			f.setLocationRelativeTo(this);
			f.setVisible(true);
		});
		mapAction(KeyStroke.getKeyStroke(KeyEvent.VK_F, MainView.SHORTCUT_KEY), findAction, "find_usna");

		// Toolbar
		JToolBar toolBar = new JToolBar();
		toolBar.add(cutAction);
		toolBar.add(copyAction);
		toolBar.add(pasteAction);
		toolBar.addSeparator();
		toolBar.add(undoAction);
		toolBar.add(redoAction);
		toolBar.addSeparator();
		toolBar.add(findAction);
		toolBar.setBorder(BorderFactory.createEmptyBorder());
		toolBar.setFloatable(false);
		getContentPane().add(toolBar, BorderLayout.NORTH);
		
		// bottom buttons
		JPanel buttonsPanel = new JPanel(new FlowLayout());
		getContentPane().add(buttonsPanel, BorderLayout.SOUTH);

		JButton okButton = new JButton(LABELS.getString("dlgSave"));
		okButton.addActionListener(e -> {
			ghost.setNote(notesEditor.getText());
			String keyNote = textFieldKeyword.getText();
			ghost.setKeyNote(keyNote.substring(0, Math.min(MAX_KEYWORD, keyNote.length())));
			dispose();
		});
		buttonsPanel.add(okButton);

		JButton closeButton = new JButton(LABELS.getString("dlgClose"));
		closeButton.addActionListener(e -> dispose());
		buttonsPanel.add(closeButton);

		JPanel keyPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		keyPanel.add(new JLabel(LABELS.getString("labelKeynote")));
		keyPanel.add(textFieldKeyword);

		centerPanel.add(keyPanel, BorderLayout.SOUTH);

		getContentPane().add(centerPanel, BorderLayout.CENTER);

		mapAction(KeyStroke.getKeyStroke(KeyEvent.VK_K, MainView.SHORTCUT_KEY), new UsnaAction(e -> textFieldKeyword.requestFocus()), "focusKeyword");
		mapAction(KeyStroke.getKeyStroke(KeyEvent.VK_N, MainView.SHORTCUT_KEY), new UsnaAction(e -> notesEditor.requestFocus()), "focusNote");
		mapAction(KeyStroke.getKeyStroke(KeyEvent.VK_S, MainView.SHORTCUT_KEY), new UsnaAction(e -> okButton.doClick()), "saveNote");

		setSize(550, 400);
		setVisible(true);
		notesEditor.requestFocus();
		setLocationRelativeTo(owner);
	}

	private void mapAction(KeyStroke k, Action action, String name) {
		centerPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(k, name);
		centerPanel.getActionMap().put(name, action);
	}
}