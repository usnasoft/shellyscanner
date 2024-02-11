package it.usna.shellyscan.view;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.undo.UndoManager;

import it.usna.shellyscan.controller.UsnaAction;
import it.usna.swing.TextLineNumber;
import it.usna.swing.dialog.FindReplaceDialog;

/**
 * A small text editor where "load" and "save" relies on GhostDevice notes
 * @author usna
 */
public class BasicEditorPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	
	protected JTextArea textArea = new JTextArea();
	private Action cutAction;
	private Action copyAction;
	private Action pasteAction;
	private Action undoAction;
	private Action redoAction;
	private Action findAction;
	
	public BasicEditorPanel(Window owner, String text, boolean lines) {
		setLayout(new BorderLayout());
		JScrollPane scrollPane = new JScrollPane();
		add(scrollPane, BorderLayout.CENTER);

		textArea.setText(text);
		scrollPane.setViewportView(textArea);
		if(lines) {
			TextLineNumber lineNum = new TextLineNumber(textArea);
			lineNum.setBorder(BorderFactory.createEmptyBorder(0, 1, 0, 3));
			scrollPane.setRowHeaderView(lineNum);
		}
		
		// actions
		UndoManager manager = new UndoManager();
		textArea.getDocument().addUndoableEditListener(manager);
		
		cutAction = new DefaultEditorKit.CutAction();
		cutAction.putValue(Action.SHORT_DESCRIPTION, LABELS.getString("btnCut"));
		cutAction.putValue(Action.SMALL_ICON, new ImageIcon(BasicEditorPanel.class.getResource("/images/Clipboard_Cut24.png")));
		
		copyAction = new DefaultEditorKit.CopyAction();
		copyAction.putValue(Action.SHORT_DESCRIPTION, LABELS.getString("btnCopy"));
		copyAction.putValue(Action.SMALL_ICON, new ImageIcon(BasicEditorPanel.class.getResource("/images/Clipboard_Copy24.png")));

		pasteAction = new DefaultEditorKit.PasteAction();
		pasteAction.putValue(Action.SHORT_DESCRIPTION, LABELS.getString("btnPaste"));
		pasteAction.putValue(Action.SMALL_ICON, new ImageIcon(BasicEditorPanel.class.getResource("/images/Clipboard_Paste24.png")));
		
		undoAction = new UsnaAction(null, "btnUndo", "/images/Undo24.png", e -> {
			try {manager.undo();} catch(RuntimeException ex) {}
		});
		textArea.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, MainView.SHORTCUT_KEY), "undo_usna");
		textArea.getActionMap().put("undo_usna", undoAction);
		
		redoAction = new UsnaAction(null, "btnRedo", "/images/Redo24.png", e -> {
			try {manager.redo();} catch(RuntimeException ex) {}
		});
		textArea.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, MainView.SHORTCUT_KEY), "redo_usna");
		textArea.getActionMap().put("redo_usna", redoAction);
		
		findAction = new UsnaAction(null, "btnFind", "/images/Search24.png", e -> {
			FindReplaceDialog f = new FindReplaceDialog(owner, textArea, true);
			f.setLocationRelativeTo(BasicEditorPanel.this);
			f.setVisible(true);
		});
		textArea.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F, MainView.SHORTCUT_KEY), "find_usna");
		textArea.getActionMap().put("find_usna", findAction);
		
		// toolbar
		JToolBar toolBar = createToolbar(new JToolBar());
		toolBar.setFloatable(false);
		add(toolBar, BorderLayout.NORTH);
	}
	
	protected JToolBar createToolbar(JToolBar toolBar) {
		toolBar.add(cutAction);
		toolBar.add(copyAction);
		toolBar.add(pasteAction);
		toolBar.addSeparator();
		toolBar.add(undoAction);
		toolBar.add(redoAction);
		toolBar.addSeparator();
		toolBar.add(findAction);
		return toolBar;
	}
	
	public void addCaretListener(CaretListener listener) {
		textArea.addCaretListener(listener);
	}
	
	public int getCaretRow() {
		try {
			int caretpos = textArea.getCaretPosition();
			return textArea.getLineOfOffset(caretpos) + 1;
		} catch (BadLocationException e1) {
			return 0;
		}
	}
	
	public int getCaretColumn() {
		try {
			int caretpos = textArea.getCaretPosition();
			int row = textArea.getLineOfOffset(caretpos);
			return caretpos - textArea.getLineStartOffset(row) + 1;
		} catch (BadLocationException e1) {
			return 0;
		}
	}
	
	public String getText() {
		return textArea.getText();
	}
	
	@Override
	public void requestFocus() {
		textArea.requestFocus();
	}
}