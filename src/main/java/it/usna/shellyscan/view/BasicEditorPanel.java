package it.usna.shellyscan.view;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.text.DefaultEditorKit;
import javax.swing.undo.UndoManager;

import it.usna.shellyscan.controller.UsnaAction;
import it.usna.swing.dialog.FindReplaceDialog;

/**
 * A small text editor where "load" and "save" relies on GhostDevice notes
 * @author usna
 */
public class BasicEditorPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	
	private JTextArea textArea = new JTextArea();
	
	public BasicEditorPanel(Window owner, String text) {
		setLayout(new BorderLayout());
		JScrollPane scrollPane = new JScrollPane();
		add(scrollPane, BorderLayout.CENTER);

		textArea.setText(text);
		scrollPane.setViewportView(textArea);
		
		// actions
		UndoManager manager = new UndoManager();
		textArea.getDocument().addUndoableEditListener(manager);
		
		Action cutAction = new DefaultEditorKit.CutAction();
		cutAction.putValue(Action.SHORT_DESCRIPTION, LABELS.getString("btnCut"));
		cutAction.putValue(Action.SMALL_ICON, new ImageIcon(BasicEditorPanel.class.getResource("/images/Clipboard Cut_16.png")));
		
		Action copyAction = new DefaultEditorKit.CopyAction();
		copyAction.putValue(Action.SHORT_DESCRIPTION, LABELS.getString("btnCopy"));
		copyAction.putValue(Action.SMALL_ICON, new ImageIcon(BasicEditorPanel.class.getResource("/images/Clipboard_Copy_16.png")));

		Action pasteAction = new DefaultEditorKit.PasteAction();
		pasteAction.putValue(Action.SHORT_DESCRIPTION, LABELS.getString("btnPaste"));
		pasteAction.putValue(Action.SMALL_ICON, new ImageIcon(BasicEditorPanel.class.getResource("/images/Clipboard Paste_16.png")));
		
		Action undoAction = new UsnaAction(null, "/images/Undo_16.png", "btnUndo", e -> {
			try {manager.undo();} catch(RuntimeException ex) {}
		});
		textArea.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, MainView.SHORTCUT_KEY), "undo_usna");
		textArea.getActionMap().put("undo_usna", undoAction);
		
		Action redoAction = new UsnaAction(null, "/images/Redo_16.png", "btnRedo", e -> {
			try {manager.redo();} catch(RuntimeException ex) {}
		});
		textArea.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, MainView.SHORTCUT_KEY), "redo_usna");
		textArea.getActionMap().put("redo_usna", redoAction);
		
		Action findAction = new UsnaAction(null, "/images/Search_16.png", "btnFind", e -> {
			FindReplaceDialog f = new FindReplaceDialog(owner, textArea, true);
			f.setLocationRelativeTo(BasicEditorPanel.this);
			f.setVisible(true);
		});
		textArea.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F, MainView.SHORTCUT_KEY), "find_usna");
		textArea.getActionMap().put("find_usna", findAction);
		
		// toolbar
		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		add(toolBar, BorderLayout.NORTH);
		toolBar.add(cutAction);
		toolBar.add(copyAction);
		toolBar.add(pasteAction);
		toolBar.addSeparator();
		toolBar.add(undoAction);
		toolBar.add(redoAction);
		toolBar.addSeparator();
		toolBar.add(findAction);
	}
	
	public String getText() {
		return textArea.getText();
	}
	
	@Override
	public void requestFocus() {
		textArea.requestFocus();
	}
}