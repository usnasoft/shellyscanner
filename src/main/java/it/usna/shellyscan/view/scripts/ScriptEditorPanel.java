package it.usna.shellyscan.view.scripts;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.event.CaretListener;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.undo.UndoManager;

import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.view.MainView;
import it.usna.swing.SyntaxEditor;
import it.usna.swing.dialog.FindReplaceDialog;

/**
 * A small text editor where "load" and "save" relies on GhostDevice notes
 * @author usna
 */
public class ScriptEditorPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	
	protected SyntaxEditor textArea;
	private Action cutAction;
	private Action copyAction;
	private Action pasteAction;
	private Action undoAction;
	private Action redoAction;
	private Action findAction;
	protected JScrollPane scrollPane = new JScrollPane();
	
	public ScriptEditorPanel(Window owner, String text) {
		setLayout(new BorderLayout());
		add(scrollPane, BorderLayout.CENTER);
		
		SimpleAttributeSet style = new SimpleAttributeSet();
		StyleConstants.setFontFamily(style, Font.MONOSPACED);
		textArea = new SyntaxEditor(style);
		
		Style styleComment = textArea.addStyle("usna_red", null);
		StyleConstants.setForeground(styleComment, Color.RED);
		textArea.addBlockSyntax(new SyntaxEditor.BlockSyntax("//", "\n", styleComment));
		textArea.addBlockSyntax(new SyntaxEditor.BlockSyntax("/*", "*/", styleComment));
		
		Style styleStr = textArea.addStyle("usna_green", null);
		StyleConstants.setForeground(styleStr, new Color(0, 120, 0));
		textArea.addBlockSyntax(new SyntaxEditor.BlockSyntax("\"", "\"", "\\", styleStr));
		textArea.addBlockSyntax(new SyntaxEditor.BlockSyntax("'", "'", "\\", styleStr));
		
		Style styleBrachets = textArea.addStyle("usna_brachets", null);
		StyleConstants.setBold(styleBrachets, true);
		textArea.addKeywords(new SyntaxEditor.Keywords(new String[] {"{", "}", "[", "]"}, styleBrachets));
		
		Style styleOperators = textArea.addStyle("usna_brachets", null);
		StyleConstants.setForeground(styleOperators, new Color(150, 0, 0));
		textArea.addKeywords(new SyntaxEditor.Keywords(new String[] {"=", "+", "-", "*", "/", "%", "<", ">", "&", "|", "!"}, styleOperators));
		
		Style styleReserved = textArea.addStyle("usna_styleReserved", null);
		StyleConstants.setBold(styleReserved, true);
		StyleConstants.setForeground(styleReserved, Color.blue);
		textArea.addDelimitedKeywords(new SyntaxEditor.DelimitedKeywords(new String[] {
				"abstract",	"continue",	"for", "new", "switch",
				"assert",	"default",	"goto",	"package", "synchronized",
				"boolean", "do", "if", "private", "this",
				"break", "double", "implements", "protected", "throw",
				"byte", "else", "import", "public", "throws",
				"case", "enum", "instanceof", "return", "transient",
				"catch", "extends", "int", "short", "try",
				"char", "final", "interface", "static", "void",
				"class", "finally", "long", "strictfp", "volatile",
				"const", "float", "native", "super", "while"}, styleReserved/*, null, null*/));

		textArea.activateUndo();
		textArea.setTabSize(4);

		textArea.setText(text);
		scrollPane.setViewportView(textArea);
		
		// actions
		UndoManager undoManager = new UndoManager();
		textArea.getDocument().addUndoableEditListener(undoManager);
		
		cutAction = new DefaultEditorKit.CutAction();
		cutAction.putValue(Action.SHORT_DESCRIPTION, LABELS.getString("btnCut"));
		cutAction.putValue(Action.SMALL_ICON, new ImageIcon(ScriptEditorPanel.class.getResource("/images/Clipboard_Cut24.png")));
		
		copyAction = new DefaultEditorKit.CopyAction();
		copyAction.putValue(Action.SHORT_DESCRIPTION, LABELS.getString("btnCopy"));
		copyAction.putValue(Action.SMALL_ICON, new ImageIcon(ScriptEditorPanel.class.getResource("/images/Clipboard_Copy24.png")));

		pasteAction = new DefaultEditorKit.PasteAction();
		pasteAction.putValue(Action.SHORT_DESCRIPTION, LABELS.getString("btnPaste"));
		pasteAction.putValue(Action.SMALL_ICON, new ImageIcon(ScriptEditorPanel.class.getResource("/images/Clipboard_Paste24.png")));
		
		undoAction = textArea.getUndoAction();
		undoAction.putValue(Action.SHORT_DESCRIPTION, LABELS.getString("btnUndo"));
		undoAction.putValue(Action.SMALL_ICON, new ImageIcon(ScriptEditorPanel.class.getResource("/images/Undo24.png")));
		mapAction(KeyStroke.getKeyStroke(KeyEvent.VK_Z, MainView.SHORTCUT_KEY), undoAction, "undo_usna");
		
		redoAction = textArea.getRedoAction();
		redoAction.putValue(Action.SHORT_DESCRIPTION, LABELS.getString("btnRedo"));
		redoAction.putValue(Action.SMALL_ICON, new ImageIcon(ScriptEditorPanel.class.getResource("/images/Redo24.png")));
		mapAction(KeyStroke.getKeyStroke(KeyEvent.VK_Y, MainView.SHORTCUT_KEY), redoAction, "redo_usna");
		
		findAction = new UsnaAction(null, "btnFind", "/images/Search24.png", e -> {
			FindReplaceDialog f = new FindReplaceDialog(owner, textArea, true);
			f.setLocationRelativeTo(ScriptEditorPanel.this);
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
		return textArea.getCaretRow();
	}
	
	public int getCaretColumn() {
		return textArea.getCaretColumn();
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