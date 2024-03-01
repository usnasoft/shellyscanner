package it.usna.shellyscan.view.scripts;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.DocumentFilter;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.model.device.g2.modules.Script;
import it.usna.shellyscan.view.MainView;
import it.usna.shellyscan.view.util.Msg;
import it.usna.swing.SyntaxEditor;
import it.usna.swing.TextLineNumber;
import it.usna.swing.dialog.FindReplaceDialog;
import it.usna.util.IOFile;

/**
 * A small text editor where "load" and "save" relies on "scipts" notes
 * @author usna
 */
public class ScriptEditor extends JFrame {
	private static final long serialVersionUID = 1L;

	private Action openAction;
	private Action saveAsAction;
	private Action uploadAction;
	private Action cutAction = new DefaultEditorKit.CutAction();
	private Action copyAction = new DefaultEditorKit.CopyAction();;
	private Action pasteAction = new DefaultEditorKit.PasteAction();
	private Action undoAction;
	private Action redoAction;
//	private Action runAction;
	private Action findAction;
	private Action gotoAction;
	
	private JToggleButton btnRun;

	private File path = null;
	
	private SyntaxEditor editor;
	private JLabel caretLabel;
	
	public ScriptEditor(ScriptsPanel originatingPanel, Script script) throws IOException {
		super(LABELS.getString("dlgScriptEditorTitle") + " - " + script.getName());
		setIconImage(Main.ICON);
		
		getContentPane().setLayout(new BorderLayout());
		
		JScrollPane scrollPane = new JScrollPane();
		
		editor = getEditorPanel(script.getCode());
		scrollPane.setViewportView(editor);
		TextLineNumber lineNum = new TextLineNumber(editor);
		lineNum.setBorder(BorderFactory.createEmptyBorder(0, 1, 0, 2));
		scrollPane.setRowHeaderView(lineNum);
		
		getContentPane().add(scrollPane, BorderLayout.CENTER);
		
		// actions
		cutAction.putValue(Action.SHORT_DESCRIPTION, LABELS.getString("btnCut"));
		cutAction.putValue(Action.SMALL_ICON, new ImageIcon(ScriptEditor.class.getResource("/images/Clipboard_Cut24.png")));
		
		copyAction.putValue(Action.SHORT_DESCRIPTION, LABELS.getString("btnCopy"));
		copyAction.putValue(Action.SMALL_ICON, new ImageIcon(ScriptEditor.class.getResource("/images/Clipboard_Copy24.png")));

		pasteAction.putValue(Action.SHORT_DESCRIPTION, LABELS.getString("btnPaste"));
		pasteAction.putValue(Action.SMALL_ICON, new ImageIcon(ScriptEditor.class.getResource("/images/Clipboard_Paste24.png")));
		
		undoAction = editor.getUndoAction();
		undoAction.putValue(Action.SHORT_DESCRIPTION, LABELS.getString("btnUndo"));
		undoAction.putValue(Action.SMALL_ICON, new ImageIcon(ScriptEditor.class.getResource("/images/Undo24.png")));
		mapAction(KeyStroke.getKeyStroke(KeyEvent.VK_Z, MainView.SHORTCUT_KEY), undoAction, "undo_usna");
		
		redoAction = editor.getRedoAction();
		redoAction.putValue(Action.SHORT_DESCRIPTION, LABELS.getString("btnRedo"));
		redoAction.putValue(Action.SMALL_ICON, new ImageIcon(ScriptEditor.class.getResource("/images/Redo24.png")));
		mapAction(KeyStroke.getKeyStroke(KeyEvent.VK_Y, MainView.SHORTCUT_KEY), redoAction, "redo_usna");
		
		UsnaAction runAction = new UsnaAction(null, "btnRun", e -> {
			try {
				if(script.isRunning()) {
					script.stop();
				} else {
					script.run();
				}
			} catch (IOException ex) {
				Msg.errorMsg(this, ex);
			}
		});
		btnRun = new JToggleButton(runAction);
		btnRun.setSelected(script.isRunning());
		
		findAction = new UsnaAction(null, "btnFind", "/images/Search24.png", e -> {
			FindReplaceDialog f = new FindReplaceDialog(this, editor, true);
			f.setLocationRelativeTo(this);
			f.setVisible(true);
		});
		mapAction(KeyStroke.getKeyStroke(KeyEvent.VK_F, MainView.SHORTCUT_KEY), findAction, "find_usna");
		
		openAction = new UsnaAction(ScriptEditor.this, "dlgOpen", "/images/Open24.png", e -> {
			final JFileChooser fc = new JFileChooser(path);
			fc.setFileFilter(new FileNameExtensionFilter(LABELS.getString("filetype_js_desc"), DialogDeviceScriptsG2.FILE_EXTENSION));
			fc.addChoosableFileFilter(new FileNameExtensionFilter(LABELS.getString("filetype_sbk_desc"), Main.BACKUP_FILE_EXT));
			if(fc.showOpenDialog(ScriptEditor.this) == JFileChooser.APPROVE_OPTION) {
				String text = loadCodeFromFile(fc.getSelectedFile());
				if(text != null) {
					editor.setText(text);
				}
				path = fc.getSelectedFile().getParentFile();
			}
		});
		
		saveAsAction = new UsnaAction(ScriptEditor.this, "dlgSave", "/images/Save24.png", e -> {
			final JFileChooser fc = new JFileChooser(path);
			fc.setFileFilter(new FileNameExtensionFilter(LABELS.getString("filetype_js_desc"), DialogDeviceScriptsG2.FILE_EXTENSION));
			if(fc.showSaveDialog(ScriptEditor.this) == JFileChooser.APPROVE_OPTION) {
				try {
					Path toSave = IOFile.addExtension(fc.getSelectedFile().toPath(), DialogDeviceScriptsG2.FILE_EXTENSION);
					IOFile.writeFile(toSave, editor.getText());
					JOptionPane.showMessageDialog(ScriptEditor.this, LABELS.getString("msgFileSaved"), LABELS.getString("dlgScriptEditorTitle"), JOptionPane.INFORMATION_MESSAGE);
				} catch (IOException e1) {
					Msg.errorMsg(e1);
				}
				path = fc.getSelectedFile().getParentFile();
			}
		});

		uploadAction = new UsnaAction(this, "btnUpload", "btnUploadTooltip", "/images/Upload24.png", null, e -> {
			String res = script.putCode(editor.getText());
			if(res != null) {
				Msg.errorMsg(this, res);
			}
		});
		
		gotoAction = new UsnaAction(null, "dlgScriptEditorGotoLineTitle", "/images/goto_line.png", e -> {
			JPanel msg = new JPanel(new FlowLayout());
			JTextField input = new JTextField(10);
			msg.add(new JLabel(LABELS.getString("dlgScriptEditorGotoLineLabel")));
			msg.add(input);

			DocumentFilter filter = new DocumentFilter() {
				@Override
				public void insertString(DocumentFilter.FilterBypass fb, int offset, String text, AttributeSet attr) throws BadLocationException {
					StringBuilder buffer = new StringBuilder(text.length());
					for (int i = 0; i < text.length(); i++) {
						char ch = text.charAt(i);
						if (Character.isDigit(ch)) {
							buffer.append(ch);
						}
					}
					super.insertString(fb, offset, buffer.toString(), attr);
				}

				@Override
				public void replace(DocumentFilter.FilterBypass fb, int offset, int length, String string, AttributeSet attr) throws BadLocationException {
					if (length > 0) {
						fb.remove(offset, length);
					}
					insertString(fb, offset, string, attr);
				}
			};
			((AbstractDocument)input.getDocument()).setDocumentFilter(filter);

			JOptionPane optionPane = new JOptionPane(msg, JOptionPane.PLAIN_MESSAGE,  JOptionPane.OK_CANCEL_OPTION) {
				private static final long serialVersionUID = 1L;
				@Override
				public void selectInitialValue() {
					input.requestFocusInWindow();
				}
			};
			optionPane.createDialog(this, LABELS.getString("dlgScriptEditorGotoLineTitle")).setVisible(true);
			Object ret = optionPane.getValue();
			if(ret != null && ret instanceof Number n && n.intValue() == JOptionPane.OK_OPTION) {
				try {
					gotoLine(Integer.parseInt(input.getText()));
				} catch(RuntimeException ex) {}
			}
			editor.requestFocus();
		});
		mapAction(KeyStroke.getKeyStroke(KeyEvent.VK_G, MainView.SHORTCUT_KEY), gotoAction, "goto_usna");
		
		caretLabel = new JLabel(editor.getCaretRow() + " : " + editor.getCaretColumn() + "  ");
		editor.addCaretListener(e -> {
			caretLabel.setText(editor.getCaretRow() + " : " + editor.getCaretColumn() + "  ");
		});
		
		getContentPane().add(getToolBar(), BorderLayout.NORTH);
		
		setSize(800, 600);
		setVisible(true);
		editor.requestFocus();
		setLocationRelativeTo(originatingPanel);
	}
	
	private void gotoLine(int line) {
		Element el = editor.getDocument().getDefaultRootElement().getElement(line - 1);
		if(el != null) {
			editor.setCaretPosition(el.getStartOffset());
		}
	}
	
	private static SyntaxEditor getEditorPanel(String initText) {
		SimpleAttributeSet style = new SimpleAttributeSet();
		StyleConstants.setFontFamily(style, Font.MONOSPACED);
		SyntaxEditor textArea = new SyntaxEditor(style);
		
		textArea.activateUndo();
		textArea.setTabSize(4);
		textArea.setText(initText);
		textArea.resetUndo();
		
		Style styleComment = textArea.addStyle("usna_red", null);
		StyleConstants.setForeground(styleComment, Color.RED);
		textArea.addSyntaxRule(new SyntaxEditor.BlockSimpleSyntax("//", "\n", styleComment));
		textArea.addSyntaxRule(new SyntaxEditor.BlockSimpleSyntax("/*", "*/", styleComment));
		
		Style styleStr = textArea.addStyle("usna_green", null);
		StyleConstants.setForeground(styleStr, new Color(0, 120, 0));
		textArea.addSyntaxRule(new SyntaxEditor.BlockSimpleSyntax("\"", "\"", "\\", styleStr));
		textArea.addSyntaxRule(new SyntaxEditor.BlockSimpleSyntax("'", "'", "\\", styleStr));
		
		Style styleBrachets = textArea.addStyle("usna_brachets", null);
		StyleConstants.setBold(styleBrachets, true);
		textArea.addSyntaxRule(new SyntaxEditor.Keywords(new String[] {"{", "}", "[", "]"}, styleBrachets));
		
		Style styleOperators = textArea.addStyle("usna_brachets", null);
		StyleConstants.setForeground(styleOperators, new Color(150, 0, 0));
		textArea.addSyntaxRule(new SyntaxEditor.Keywords(new String[] {"=", "+", "-", "*", "/", "%", "<", ">", "&", "|", "!"}, styleOperators));
		
		Style styleReserved = textArea.addStyle("usna_styleReserved", null);
		StyleConstants.setBold(styleReserved, true);
		StyleConstants.setForeground(styleReserved, Color.blue);
		textArea.addSyntaxRule(new SyntaxEditor.DelimitedKeywords(new String[] {
				"abstract", "arguments", "await*", "boolean", "break", "byte", "case", "catch",
				"char", "class", "const*", "continue", "debugger", "default", "delete", "do",
				"double", "else", "enum", "eval", "export", "extends", "false", "final",
				"finally", "float", "for", "function", "goto", "if", "implements", "import",
				"in", "instanceof", "int", "interface", "let", "long", "native", "new",
				"null", "package", "private", "protected", "public", "return", "short", "static",
				"super", "switch", "synchronized", "this", "throw", "throws", "transient", "true",
				"try", "typeof", "var", "void", "volatile", "while", "with", "yield"}, styleReserved));
		
		Style styleImplemented = textArea.addStyle("usna_styleReserved", null);
		StyleConstants.setBold(styleImplemented, true);
		StyleConstants.setForeground(styleImplemented, new Color(153, 0, 153));
		textArea.addSyntaxRule(new SyntaxEditor.DelimitedKeywords(new String[] {
				"String", "Number", "Function", "Array", "Math", "Date", "Object", "Exceptions"}, styleImplemented));
		
		Style styleShelly = textArea.addStyle("usna_shellyReserved", null);
		StyleConstants.setBold(styleShelly, true);
		StyleConstants.setItalic(styleShelly, true);
		StyleConstants.setForeground(styleShelly, new Color(102, 0, 204));
		textArea.addSyntaxRule(new SyntaxEditor.DelimitedKeywords(new String[] {
				"Shelly", "JSON", "Timer", "MQTT", "BLE", "HTTPServer"}, styleShelly));

		return textArea;
	}
	
	private JToolBar getToolBar() {
		JToolBar toolBar = new JToolBar();
		JButton uploadBtn = new JButton(uploadAction);
		uploadBtn.setHideActionText(false);
		
		ImageIcon runIcon = new ImageIcon(ScriptEditor.class.getResource("/images/PlayerPlayGreen24.png"));
//		JToggleButton btnRun = new JToggleButton(runIcon);
		btnRun.setIcon(runIcon);
		btnRun.setRolloverIcon(runIcon);
		ImageIcon stopIcon = new ImageIcon(ScriptEditor.class.getResource("/images/PlayerStopRed24.png"));
		btnRun.setRolloverSelectedIcon(stopIcon);
		btnRun.setSelectedIcon(stopIcon);
		btnRun.setHideActionText(true);
		
		toolBar.add(openAction);
		toolBar.add(saveAsAction);
		toolBar.add(uploadBtn);
		toolBar.add(btnRun);
		toolBar.addSeparator();
		toolBar.add(cutAction);
		toolBar.add(copyAction);
		toolBar.add(pasteAction);
		toolBar.addSeparator();
		toolBar.add(undoAction);
		toolBar.add(redoAction);
		toolBar.addSeparator();
		toolBar.add(findAction);
		toolBar.add(gotoAction);
		toolBar.add(Box.createHorizontalGlue());
		toolBar.add(caretLabel);
		
		toolBar.setFloatable(false);
		return toolBar;
	}
	
	private void mapAction(KeyStroke k, Action action, String name) {
		editor.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(k, name);
		editor.getActionMap().put(name, action);
	}
	
	private String loadCodeFromFile(File in) {
		try {
			try (ZipFile inZip = new ZipFile(in, StandardCharsets.UTF_8)) { // backup
				String[] scriptList = inZip.stream().filter(z -> z.getName().endsWith(".mjs")).map(z -> z.getName().substring(0, z.getName().length() - 4)).toArray(String[]::new);
				if(scriptList.length > 0) {
					Object sName = JOptionPane.showInputDialog(this, LABELS.getString("scrSelectionMsg"), LABELS.getString("scrSelectionTitle"), JOptionPane.PLAIN_MESSAGE, null, scriptList, null);
					if(sName != null) {
						try (InputStream is = inZip.getInputStream(inZip.getEntry(sName + ".mjs"))) {
							return new BufferedReader(new InputStreamReader(is)).lines().collect(Collectors.joining("\n"));
						}
					}
				} else {
					JOptionPane.showMessageDialog(this, LABELS.getString("scrNoneInZipFile"), LABELS.getString("btnUpload"), JOptionPane.INFORMATION_MESSAGE);
				}
			} catch (ZipException e1) { // no zip (backup) -> text file
				return IOFile.readFile(in);
			}
		} catch (/*IO*/Exception e1) {
			Msg.errorMsg(e1);
		} finally {
			setCursor(Cursor.getDefaultCursor());
		}
		return null;
	}
}