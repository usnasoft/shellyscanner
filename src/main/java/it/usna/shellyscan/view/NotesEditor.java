package it.usna.shellyscan.view;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.text.DefaultEditorKit;
import javax.swing.undo.UndoManager;

import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.view.util.UtilMiscellaneous;
import it.usna.swing.dialog.FindReplaceDialog;

public class NotesEditor extends JDialog {
	private static final long serialVersionUID = 1L;
	
	public NotesEditor(Window owner, ShellyAbstractDevice device) {
		super(owner, LABELS.getString("action_notes_tooltip") + " - " + UtilMiscellaneous.getFullName(device));
		
		JScrollPane scrollPane = new JScrollPane();
		getContentPane().add(scrollPane, BorderLayout.CENTER);
		
		JTextArea textArea = new JTextArea();
		scrollPane.setViewportView(textArea);
		
		UndoManager manager = new UndoManager();
		textArea.getDocument().addUndoableEditListener(manager);
		
		Action cutAction = new DefaultEditorKit.CutAction();
		cutAction.putValue(Action.SHORT_DESCRIPTION, LABELS.getString("btnCut"));
		cutAction.putValue(Action.SMALL_ICON, new ImageIcon(NotesEditor.class.getResource("/images/Clipboard Cut_16.png")));
		
		Action copyAction = new DefaultEditorKit.CopyAction();
		copyAction.putValue(Action.SHORT_DESCRIPTION, LABELS.getString("btnCopy"));
		copyAction.putValue(Action.SMALL_ICON, new ImageIcon(NotesEditor.class.getResource("/images/Clipboard_Copy_16.png")));

		Action pasteAction = new DefaultEditorKit.PasteAction();
		pasteAction.putValue(Action.SHORT_DESCRIPTION, LABELS.getString("btnPaste"));
		pasteAction.putValue(Action.SMALL_ICON, new ImageIcon(NotesEditor.class.getResource("/images/Clipboard Paste_16.png")));
		
		Action findAction = new UsnaAction(this, "/images/Search_16.png", "btnFind", e -> {
			FindReplaceDialog f = new FindReplaceDialog(NotesEditor.this, textArea, true);
			f.setLocationRelativeTo(NotesEditor.this);
			f.setVisible(true);
		});
		
		Action undoAction = new UsnaAction(this, "/images/Undo_16.png", "btnUndo", e -> {
			try {manager.undo();} catch(RuntimeException ex) {}
		});
		textArea.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, MainView.SHORTCUT_KEY), "undo_usna");
		textArea.getActionMap().put("undo_usna", undoAction);
		
		Action redoAction = new UsnaAction(this, "/images/Redo_16.png", "btnRedo", e -> {
			try {manager.redo();} catch(RuntimeException ex) {}
		});
		textArea.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, MainView.SHORTCUT_KEY), "redo_usna");
		textArea.getActionMap().put("redo_usna", redoAction);
		
		JToolBar toolBar = new JToolBar();
		getContentPane().add(toolBar, BorderLayout.NORTH);
		toolBar.add(cutAction);
		toolBar.add(copyAction);
		toolBar.add(pasteAction);
		toolBar.addSeparator();
		toolBar.add(undoAction);
		toolBar.add(redoAction);
		toolBar.addSeparator();
		toolBar.add(findAction);
		
		setSize(550, 400);
		setVisible(true);
		setLocationRelativeTo(owner);
	}
}