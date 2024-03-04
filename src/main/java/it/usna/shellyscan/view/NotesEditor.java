package it.usna.shellyscan.view;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Element;
import javax.swing.undo.UndoManager;

import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.model.device.GhostDevice;
import it.usna.shellyscan.view.util.UtilMiscellaneous;
import it.usna.swing.dialog.FindReplaceDialog;

/**
 * A small text editor where "load" and "save" relies on GhostDevice notes
 * @author usna
 */
public class NotesEditor extends JFrame {
	private static final long serialVersionUID = 1L;
	
	public NotesEditor(Window owner, GhostDevice ghost) {
		super(LABELS.getString("action_notes_tooltip") + " - " + UtilMiscellaneous.getDescName(ghost));
		setIconImages(owner.getIconImages());
		
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

class BasicEditorPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	
	protected JTextArea textArea = new JTextArea();
	private Action cutAction;
	private Action copyAction;
	private Action pasteAction;
	private Action undoAction;
	private Action redoAction;
	private Action findAction;
	protected JScrollPane scrollPane = new JScrollPane();
	
	public BasicEditorPanel(Window owner, String text) {
		setLayout(new BorderLayout());
		add(scrollPane, BorderLayout.CENTER);

		textArea.setText(text);
		scrollPane.setViewportView(textArea);
		
		// actions
		UndoManager undoManager = new UndoManager();
		textArea.getDocument().addUndoableEditListener(undoManager);
		
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
			try {undoManager.undo();} catch(RuntimeException ex) {}
		});
		mapAction(KeyStroke.getKeyStroke(KeyEvent.VK_Z, MainView.SHORTCUT_KEY), undoAction, "undo_usna");
		
		redoAction = new UsnaAction(null, "btnRedo", "/images/Redo24.png", e -> {
			try {undoManager.redo();} catch(RuntimeException ex) {}
		});
		mapAction(KeyStroke.getKeyStroke(KeyEvent.VK_Y, MainView.SHORTCUT_KEY), redoAction, "redo_usna");
		
		findAction = new UsnaAction(null, "btnFind", "/images/Search24.png", e -> {
			FindReplaceDialog f = new FindReplaceDialog(owner, textArea, true);
			f.setLocationRelativeTo(BasicEditorPanel.this);
			f.setVisible(true);
		});
		mapAction(KeyStroke.getKeyStroke(KeyEvent.VK_F, MainView.SHORTCUT_KEY), findAction, "find_usna");
		
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
	
	public void gotoLine(int line) {
		Element el = textArea.getDocument().getDefaultRootElement().getElement(line - 1);
		if(el != null) {
			textArea.setCaretPosition(el.getStartOffset());
		}
	}
	
	public void mapAction(KeyStroke k, Action action, String name) {
		textArea.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(k, name);
		textArea.getActionMap().put(name, action);
	}
	
	public String getText() {
		return textArea.getText();
	}
	
	@Override
	public void requestFocus() {
		textArea.requestFocus();
	}
}