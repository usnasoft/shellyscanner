package it.usna.shellyscan.view.scripts.ide;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
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

import it.usna.shellyscan.Main;
import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.model.device.g2.modules.Script;
import it.usna.shellyscan.view.MainView;
import it.usna.shellyscan.view.scripts.DialogDeviceScriptsG2;
import it.usna.shellyscan.view.scripts.ScriptsPanel;
import it.usna.shellyscan.view.util.Msg;
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
	private Action runAction;
	private Action uploadAndRunAction;
	private Action cutAction = new DefaultEditorKit.CutAction();
	private Action copyAction = new DefaultEditorKit.CopyAction();;
	private Action pasteAction = new DefaultEditorKit.PasteAction();
	private Action undoAction;
	private Action redoAction;
	private Action findAction;
	private Action gotoAction;

	private File path = null;
	
	private EditorPanel editor;
	private JLabel caretLabel;
	
	public ScriptEditor(ScriptsPanel originatingPanel, Script script) throws IOException {
		super(LABELS.getString("dlgScriptEditorTitle") + " - " + script.getName());
		setIconImage(Main.ICON);
		
		getContentPane().setLayout(new BorderLayout());
		
		JScrollPane scrollPane = new JScrollPane();

		editor = new EditorPanel(script.getCode());
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
		editor.mapAction(KeyStroke.getKeyStroke(KeyEvent.VK_Z, MainView.SHORTCUT_KEY), undoAction, "undo_usna");
		
		redoAction = editor.getRedoAction();
		redoAction.putValue(Action.SHORT_DESCRIPTION, LABELS.getString("btnRedo"));
		redoAction.putValue(Action.SMALL_ICON, new ImageIcon(ScriptEditor.class.getResource("/images/Redo24.png")));
		editor.mapAction(KeyStroke.getKeyStroke(KeyEvent.VK_Y, MainView.SHORTCUT_KEY), redoAction, "redo_usna");
		
		findAction = new UsnaAction(null, "btnFind", "/images/Search24.png", e -> {
			FindReplaceDialog f = new FindReplaceDialog(this, editor, true);
			f.setLocationRelativeTo(this);
			f.setVisible(true);
		});
		editor.mapAction(KeyStroke.getKeyStroke(KeyEvent.VK_F, MainView.SHORTCUT_KEY), findAction, "find_usna");
		
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

		uploadAction = new UsnaAction(this, "btnUploadTooltip", "/images/Upload24.png", e -> {
			String res = script.putCode(editor.getText());
			if(res != null) {
				Msg.errorMsg(this, res);
			}
		});
		
		runAction = new UsnaAction(null, "btnRun", "/images/PlayerPlayGreen24.png", e -> {
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
		
		uploadAndRunAction = new UsnaAction(this, "btnRun", "/images/PlayerUploadPlay24.png", e -> {
			String res = script.putCode(editor.getText()); // uploadAction.actionPerformed(e);
			if(res != null) {
				Msg.errorMsg(this, res);
			} else {
				runAction.actionPerformed(e);
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

			JOptionPane optionPane = new JOptionPane(msg, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION) {
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
					editor.gotoLine(Integer.parseInt(input.getText()));
				} catch(RuntimeException ex) {}
			}
			editor.requestFocus();
		});
		editor.mapAction(KeyStroke.getKeyStroke(KeyEvent.VK_G, MainView.SHORTCUT_KEY), gotoAction, "goto_usna");
		
		caretLabel = new JLabel(editor.getCaretRow() + " : " + editor.getCaretColumn());
		editor.addCaretListener(e -> {
			caretLabel.setText(editor.getCaretRow() + " : " + editor.getCaretColumn());
		});
		caretLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 30));
		
		getContentPane().add(getToolBar(script), BorderLayout.NORTH);
		
		setSize(800, 600);
		setVisible(true);
		editor.requestFocus();
		setLocationRelativeTo(originatingPanel);
	}
	
	private JToolBar getToolBar(Script script) {
		JToolBar toolBar = new JToolBar();

		JToggleButton btnRun = new JToggleButton(runAction);
		btnRun.setSelected(script.isRunning());
		btnRun.setRolloverIcon(btnRun.getIcon());
		ImageIcon stopIcon = new ImageIcon(ScriptEditor.class.getResource("/images/PlayerStopRed24.png"));
		btnRun.setRolloverSelectedIcon(stopIcon);
		btnRun.setSelectedIcon(stopIcon);
		btnRun.setHideActionText(true);
		
		toolBar.add(openAction);
		toolBar.add(saveAsAction);
		toolBar.add(uploadAction);
		toolBar.addSeparator();
		toolBar.add(btnRun);
		toolBar.add(uploadAndRunAction);
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
		
		return toolBar;
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