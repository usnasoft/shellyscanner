package it.usna.shellyscan.view.scripts.ide;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.event.InputEvent;
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
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.DocumentFilter;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.controller.UsnaToggleAction;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.g2.HttpLogsListener;
import it.usna.shellyscan.model.device.g2.modules.Script;
import it.usna.shellyscan.view.MainView;
import it.usna.shellyscan.view.appsettings.DialogAppSettings;
import it.usna.shellyscan.view.scripts.DialogDeviceScripts;
import it.usna.shellyscan.view.scripts.ScriptsPanel;
import it.usna.shellyscan.view.util.Msg;
import it.usna.shellyscan.view.util.UsnaTextPane;
import it.usna.swing.dialog.FindReplaceDialog;
import it.usna.swing.texteditor.TextLineNumber;
import it.usna.util.AppProperties;
import it.usna.util.IOFile;

/**
 * A small text editor for scripts
 * @author usna
 */
public class ScriptFrame extends JFrame {
	private static final long serialVersionUID = 1L;
	public final static String RUN_EVENT = "scriptIsRunning";
	public final static String CLOSE_EVENT = "scriptEditorColose";

	private Action openAction;
	private Action saveAsAction;
	private Action uploadAction;
	private UsnaToggleAction runStopAction;
	private Action uploadAndRunAction;
	private Action cutAction = new DefaultEditorKit.CutAction();
	private Action copyAction = new DefaultEditorKit.CopyAction();;
	private Action pasteAction = new DefaultEditorKit.PasteAction();
	private Action undoAction;
	private Action redoAction;
	private Action findAction;
	private Action gotoAction;
	private Action commentAction;

	private File path = null;
	
	private EditorPanel editor;
	private JLabel caretLabel;

	private UsnaTextPane logsTextArea = new UsnaTextPane();
	
	private final AbstractG2Device device;
	private boolean readLogs;
	
	public ScriptFrame(ScriptsPanel originatingPanel, AbstractG2Device device, Script script, AppProperties appProp) throws IOException {
		super(/*LABELS.getString("dlgScriptEditorTitle") + " - " +*/ script.getName());
		setIconImage(Main.ICON);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		
		this.device = device;
		JSplitPane splitPane = new JSplitPane();
		splitPane.setOneTouchExpandable(true);
		splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);

		add(splitPane, BorderLayout.CENTER);
		
		splitPane.setTopComponent(editorPanel(script, appProp));
		splitPane.setBottomComponent(logPanel());
		
		add(getToolBar(), BorderLayout.NORTH);

		setSize(800, 600);
		setVisible(true);
		splitPane.setDividerLocation(0.75d);
		editor.requestFocus();
		setLocationRelativeTo(originatingPanel);

		runningStatus(script.isRunning());
	}
	
	@Override
	public void dispose() {
		firePropertyChange(CLOSE_EVENT, null, null);
		readLogs = false;
		super.dispose();
	}
	
	private JPanel editorPanel(Script script, AppProperties appProp) throws IOException {
		JPanel mainEditorPanel = new JPanel(new BorderLayout());
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBorder(BorderFactory.createEmptyBorder());

		editor = new EditorPanel(script.getCode());
		editor.setTabSize(appProp.getIntProperty(DialogAppSettings.PROP_IDE_TAB_SIZE, DialogAppSettings.IDE_TAB_SIZE_DEFAULT));
		scrollPane.setViewportView(editor);
		TextLineNumber lineNum = new TextLineNumber(editor);
		lineNum.setBorder(BorderFactory.createEmptyBorder(0, 1, 0, 2));
		scrollPane.setRowHeaderView(lineNum);
		
		mainEditorPanel.add(scrollPane, BorderLayout.CENTER);
		
		// actions
		cutAction.putValue(Action.SHORT_DESCRIPTION, LABELS.getString("btnCut"));
		cutAction.putValue(Action.SMALL_ICON, new ImageIcon(ScriptFrame.class.getResource("/images/Clipboard_Cut24.png")));
		
		copyAction.putValue(Action.SHORT_DESCRIPTION, LABELS.getString("btnCopy"));
		copyAction.putValue(Action.SMALL_ICON, new ImageIcon(ScriptFrame.class.getResource("/images/Clipboard_Copy24.png")));

		pasteAction.putValue(Action.SHORT_DESCRIPTION, LABELS.getString("btnPaste"));
		pasteAction.putValue(Action.SMALL_ICON, new ImageIcon(ScriptFrame.class.getResource("/images/Clipboard_Paste24.png")));
		
		undoAction = editor.getUndoAction();
		undoAction.putValue(Action.SHORT_DESCRIPTION, LABELS.getString("btnUndo"));
		undoAction.putValue(Action.SMALL_ICON, new ImageIcon(ScriptFrame.class.getResource("/images/Undo24.png")));
		editor.mapAction(KeyStroke.getKeyStroke(KeyEvent.VK_Z, MainView.SHORTCUT_KEY), undoAction, "undo_usna");
		
		redoAction = editor.getRedoAction();
		redoAction.putValue(Action.SHORT_DESCRIPTION, LABELS.getString("btnRedo"));
		redoAction.putValue(Action.SMALL_ICON, new ImageIcon(ScriptFrame.class.getResource("/images/Redo24.png")));
		editor.mapAction(KeyStroke.getKeyStroke(KeyEvent.VK_Y, MainView.SHORTCUT_KEY), redoAction, "redo_usna");
		
		findAction = new UsnaAction(null, "btnFind", "/images/Search24.png", e -> {
			FindReplaceDialog f = new FindReplaceDialog(this, editor, true);
			f.setLocationRelativeTo(this);
			f.setVisible(true);
		});
		editor.mapAction(KeyStroke.getKeyStroke(KeyEvent.VK_F, MainView.SHORTCUT_KEY), findAction, "find_usna");
		
		commentAction = new UsnaAction(null, "btnCommentTooltip", "/images/Comment24.png", e -> {
			editor.commentSelection();
			editor.requestFocus();
		});
		editor.mapAction(KeyStroke.getKeyStroke(KeyEvent.VK_7, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), commentAction, "comment_usna");
		editor.mapAction(KeyStroke.getKeyStroke(KeyEvent.VK_SLASH, InputEvent.CTRL_DOWN_MASK), commentAction, "comment_usna2");

		openAction = new UsnaAction(ScriptFrame.this, "dlgOpen", "/images/Open24.png", e -> {
			final JFileChooser fc = new JFileChooser(path);
			fc.setFileFilter(new FileNameExtensionFilter(LABELS.getString("filetype_js_desc"), DialogDeviceScripts.FILE_EXTENSION));
			fc.addChoosableFileFilter(new FileNameExtensionFilter(LABELS.getString("filetype_sbk_desc"), Main.BACKUP_FILE_EXT));
			if(fc.showOpenDialog(ScriptFrame.this) == JFileChooser.APPROVE_OPTION) {
				String text = loadCodeFromFile(fc.getSelectedFile());
				if(text != null) {
					editor.setText(text);
				}
				path = fc.getSelectedFile().getParentFile();
			}
		});
		
		saveAsAction = new UsnaAction(ScriptFrame.this, "dlgSave", "/images/Save24.png", e -> {
			final JFileChooser fc = new JFileChooser(path);
			fc.setFileFilter(new FileNameExtensionFilter(LABELS.getString("filetype_js_desc"), DialogDeviceScripts.FILE_EXTENSION));
			if(fc.showSaveDialog(ScriptFrame.this) == JFileChooser.APPROVE_OPTION) {
				try {
					Path toSave = IOFile.addExtension(fc.getSelectedFile().toPath(), DialogDeviceScripts.FILE_EXTENSION);
					IOFile.writeFile(toSave, editor.getText());
					JOptionPane.showMessageDialog(ScriptFrame.this, LABELS.getString("msgFileSaved"), LABELS.getString("dlgScriptEditorTitle"), JOptionPane.INFORMATION_MESSAGE);
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

		runStopAction = new UsnaToggleAction(this, "btnRun", "btnRunStoppedTooltip", "btnRunRunningTooltip", "/images/PlayerPlayGreen24.png", "/images/PlayerStopRed24.png", e -> {
			try {
				if(script.isRunning()) {
					script.stop();
					runningStatus(false);
					firePropertyChange(RUN_EVENT, true, false);
				} else {
					runningStatus(true);
					script.run();
					firePropertyChange(RUN_EVENT, false, true);
				}
			} catch (IOException ex) {
				if(ex.getMessage() != null && ex.getMessage().endsWith("500")) {
					Msg.errorMsg(this, LABELS.getString("lblScriptExeError"));
				} else {
					Msg.errorMsg(this, ex);
				}
				runningStatus(false);
			}
		});
		
		uploadAndRunAction = new UsnaAction(this, "btnUploadRun", "/images/PlayerUploadPlay24.png", e -> {
			String res = script.putCode(editor.getText()); // uploadAction.actionPerformed(e);
			if(res != null) {
				Msg.errorMsg(this, res);
			} else {
				runStopAction.actionPerformed(e);
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
		caretLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
		
		return mainEditorPanel;
	}
	
	private void runningStatus(boolean running) {
		if(running) {
			uploadAndRunAction.setEnabled(false);
			uploadAction.setEnabled(false);
			runStopAction.setSelected(true);
			activateLogConnection();
		} else {
			uploadAndRunAction.setEnabled(true);
			uploadAction.setEnabled(true);
			runStopAction.setSelected(false);
			readLogs = false; // stop LogConnection
		}
	}
	
	private JPanel logPanel(/*Script script, AbstractG2Device device*/) throws IOException {
		logsTextArea.setEditable(false);
		JPanel mainLogPanel = new JPanel(new BorderLayout());

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 2, 4, 2));
		scrollPane.setViewportView(logsTextArea);

		mainLogPanel.add(scrollPane, BorderLayout.CENTER);
		
		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		
		Action erase = new UsnaAction(this, null, "/images/erase-9-16.png", e -> {
			logsTextArea.setText("");
		});
		toolBar.add(erase);
		
		mainLogPanel.add(toolBar, BorderLayout.NORTH);
		return mainLogPanel;
	}
	
	private JToolBar getToolBar() {
		JToolBar toolBar = new JToolBar();
		toolBar.add(openAction);
		toolBar.add(saveAsAction);
		toolBar.add(uploadAction);
		toolBar.addSeparator();
		toolBar.add(runStopAction);
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
		toolBar.addSeparator();
		toolBar.add(commentAction);
		toolBar.add(Box.createHorizontalGlue());
		toolBar.add(caretLabel);
		return toolBar;
	}
	
	private void activateLogConnection() {
		try {
			readLogs = true;
			device.connectHttpLogs(new HttpLogsListener() {
				@Override
				public void accept(String txt) {
					String[] logLine = txt.split("\\s", 3);
					int level;
					try {
						level = Integer.parseInt(logLine[1]);
					} catch(Exception e) {
						level = Integer.MIN_VALUE;					
					}
					if(logLine.length < 3 || level == Integer.MIN_VALUE) {
						logsTextArea.append(txt.trim() + "\n");
						logsTextArea.setCaretPosition(logsTextArea.getStyledDocument().getLength());
					} else if(level <= /*logLevel*/0) {
						logsTextArea.append(/*logLine[0] + "L" + logLine[1] + ": " +*/ logLine[2].trim() + "\n");
						logsTextArea.setCaretPosition(logsTextArea.getStyledDocument().getLength());
					}
				}
				
				@Override
				public boolean requestNext() {
					return readLogs;
				}
			});
		} catch (RuntimeException e) {
			Msg.errorMsg(this, e);
		}
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
			Msg.errorMsg(this, e1);
		} finally {
			setCursor(Cursor.getDefaultCursor());
		}
		return null;
	}
}

// editor: cmmenti - indent