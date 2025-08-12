package it.usna.shellyscan.view.scripts.ide;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.ProviderNotFoundException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import javax.swing.AbstractAction;
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
import javax.swing.JTextArea;
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

import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.controller.UsnaTextAction;
import it.usna.shellyscan.controller.UsnaToggleAction;
import it.usna.shellyscan.model.device.ShellyAbstractDevice.LogMode;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.g2.WebSocketDeviceListener;
import it.usna.shellyscan.model.device.g2.modules.Script;
import it.usna.shellyscan.view.MainView;
import it.usna.shellyscan.view.scripts.DialogDeviceScripts;
import it.usna.shellyscan.view.scripts.ScriptsPanel;
import it.usna.shellyscan.view.util.Msg;
import it.usna.shellyscan.view.util.ScannerProperties;
import it.usna.swing.dialog.FindReplaceDialog;
import it.usna.swing.texteditor.TextLineNumber;
import it.usna.util.IOFile;

/**
 * A small text editor for scripts
 * @author usna
 */
public class ScriptFrame extends JFrame {
	private static final long serialVersionUID = 1L;
	private static final Logger LOG = LoggerFactory.getLogger(ScriptFrame.class);
	public static final String RUN_EVENT = "scriptIsRunning";
	public static final String CLOSE_EVENT = "scriptEditorColose";

	public static final Color DARK_BACKGOUND_COLOR = new Color(40, 40, 40);

	private Action openAction;
	private Action saveAction;
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

	private Path path = null;
	private boolean canOverwriteFile = false;
	
	private EditorPanel editor;
	private JLabel caretLabel;

	private JTextArea logsTextArea = new JTextArea();
	
	private boolean darkMode = ScannerProperties.instance().getBoolProperty(ScannerProperties.PROP_IDE_DARK);
	private final AbstractG2Device device;
	private boolean logWasActive;
	private Future<Session> wsSession;
	
	private final int scriptId;
	
	public ScriptFrame(ScriptsPanel originatingPanel, AbstractG2Device device, Script script) throws IOException {
		super(script.getName());
		setIconImage(Main.ICON);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		
		this.scriptId = script.getId();
		logWasActive = (device.getDebugMode() == LogMode.SOCKET);
		
		this.device = device;
		JSplitPane splitPane = new JSplitPane();
		splitPane.setOneTouchExpandable(true);
		splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);

		add(splitPane, BorderLayout.CENTER);
		
		splitPane.setTopComponent(editorPanel(script));
		splitPane.setBottomComponent(logPanel());
		
		add(getToolBar(), BorderLayout.NORTH);

		setSize(800, 600);
		setVisible(true);
		splitPane.setDividerLocation(0.75d);
		splitPane.setResizeWeight(0.6d);
		editor.requestFocus();
		setLocationRelativeTo(originatingPanel);

		runningStatus(script.isRunning());
	}
	
	private ScriptFrame() throws IOException { // test & design contructor
		super("test");
		this.device = null;
		this.scriptId = 0;
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
	
		JSplitPane splitPane = new JSplitPane();
		splitPane.setOneTouchExpandable(true);
		splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);

		add(splitPane, BorderLayout.CENTER);
		
		splitPane.setTopComponent(editorPanel(null));
		splitPane.setBottomComponent(logPanel());
		
		add(getToolBar(), BorderLayout.NORTH);

		setSize(800, 600);
		setVisible(true);
		splitPane.setDividerLocation(0.75d);
		splitPane.setResizeWeight(0.6d);
		editor.requestFocus();
		setLocationRelativeTo(null);
	}
	
	@Override
	public void dispose() {
		firePropertyChange(CLOSE_EVENT, null, null);
		if(wsSession != null) {
			try {
				wsSession.get().close(StatusCode.NORMAL, "bye", Callback.NOOP);
			} catch (InterruptedException | ExecutionException e) {
				LOG.error("webSocketClient.disconnect", e);
			}
		}
		if(logWasActive == false) {
			device.setDebugMode(LogMode.SOCKET, false);
		}
		super.dispose();
	}
	
	private JPanel editorPanel(Script script) throws IOException {
		JPanel mainEditorPanel = new JPanel(new BorderLayout());
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBorder(BorderFactory.createEmptyBorder());

		editor = new EditorPanel(script == null ? "" : script.getCode()); // script == null -> test
		scrollPane.setViewportView(editor);
		TextLineNumber lineNum = new TextLineNumber(editor);
		if(darkMode) {
			lineNum.setBackground(Color.DARK_GRAY);
			lineNum.setForeground(Color.WHITE);
		}
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
		
		findAction = new UsnaTextAction("btnFind", "btnFind", "/images/Search24.png", (textComponent, e) -> {
			FindReplaceDialog f;
			if(textComponent == logsTextArea || logsTextArea.hasFocus()) {
				f = new FindReplaceDialog(this, logsTextArea, false);
			} else {
				f = new FindReplaceDialog(this, editor, true);
			}
			f.setLocationRelativeTo(this);
			f.setVisible(true);
		});
		editor.mapAction(KeyStroke.getKeyStroke(KeyEvent.VK_F, MainView.SHORTCUT_KEY), findAction, "btnFind");
		
		Action windowFocusAction = new UsnaAction(e -> {
			if(editor.hasFocus() == false) {
				editor.requestFocus();
			} else {
				logsTextArea.requestFocus();
			}
		});
		editor.mapAction(KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK), windowFocusAction, "usna_focus");
		
		Action commentAction = new UsnaAction(e -> {
			editor.commentSelected();
			editor.requestFocus();
		});
		editor.mapAction(KeyStroke.getKeyStroke(KeyEvent.VK_7, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), commentAction, "comment_usna");
		editor.mapAction(KeyStroke.getKeyStroke(KeyEvent.VK_SLASH, InputEvent.CTRL_DOWN_MASK), commentAction, "comment_usna");
		
		Action upperAction = new UsnaAction(e -> editor.replaceSelection(editor.getSelectedText().toUpperCase()));
		editor.mapAction(KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), upperAction, "upper_usna");
		
		Action lowerAction = new UsnaAction(e -> editor.replaceSelection(editor.getSelectedText().toLowerCase()));
		editor.mapAction(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), lowerAction, "lower_usna");
		
		editor.mapAction(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_DOWN_MASK), new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				editor.autoIndentSelected();
			}
		}, "autoindent_usna");
		
		editor.mapAction(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, InputEvent.CTRL_DOWN_MASK), new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				editor.autocomplete();
			}
		}, "autocomplete_usna");

		final boolean closeCurly = ScannerProperties.instance().getBoolProperty(ScannerProperties.IDE_AUTOCLOSE_CURLY, false);
		final boolean closeBracket = ScannerProperties.instance().getBoolProperty(ScannerProperties.IDE_AUTOCLOSE_BRACKET, false);
		final boolean closeSquare = ScannerProperties.instance().getBoolProperty(ScannerProperties.IDE_AUTOCLOSE_SQUARE, false);
		final boolean closeString = ScannerProperties.instance().getBoolProperty(ScannerProperties.IDE_AUTOCLOSE_STRING, false);
		final String indentMode = ScannerProperties.instance().getProperty(ScannerProperties.IDE_AUTOINDENT, "SMART");
		final boolean indent = "NO".equals(indentMode) == false;
		final boolean smartAutoIndent = "SMART".equals(indentMode);
		editor.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_TAB) {
					if(editor.indentSelected(e.isShiftDown())) {
						e.consume();
					}
				} else if(e.getKeyCode() == KeyEvent.VK_ENTER) {
					if(indent) {
						if(e.isShiftDown()) {
							editor.replaceSelection("\n");
						} else {
							editor.newIndentedLine(smartAutoIndent, closeCurly);
						}
						e.consume();
					}
				}
			}

			@Override
			public void keyTyped(KeyEvent e) {
				if(closeBracket && e.getKeyChar() == '(') {
					editor.blockLimitsInsert("(", ")");
					e.consume();
				} else if(closeSquare && e.getKeyChar() == '[') {
					editor.blockLimitsInsert("[", "]");
					e.consume();
				} else if(closeCurly && e.getKeyChar() == '{') {
					editor.blockLimitsInsert("{", "}");
					e.consume();
				} else if(closeString && e.getKeyChar() == '"') {
					editor.stringBlockLimitsInsert();
					e.consume();
				} else if(e.getKeyChar() == '}') {
					if(smartAutoIndent) {
						editor.removeIndentlevel();
						e.consume();
					}
				}
			}
		});
		
		openAction = new UsnaAction(ScriptFrame.this, "dlgOpen", "/images/Open24.png", e -> {
			final JFileChooser fc = (path == null) ? new JFileChooser() : new JFileChooser(path.getParent().toFile());
			fc.setFileFilter(new FileNameExtensionFilter(LABELS.getString("filetype_js_desc"), DialogDeviceScripts.FILE_EXTENSION));
			fc.addChoosableFileFilter(new FileNameExtensionFilter(LABELS.getString("filetype_sbk_desc"), Main.BACKUP_FILE_EXT));
			if(fc.showOpenDialog(ScriptFrame.this) == JFileChooser.APPROVE_OPTION) {
				Path thisFile = fc.getSelectedFile().toPath();
				String text = loadCodeFromFile(thisFile);
				if(text != null) {
					editor.setText(text);
					editor.resetUndo();
					editor.setCaretPosition(0);
					editor.requestFocus();
					setTitle(script.getName() + " - " + thisFile.getFileName());
				}
				path = thisFile;
			}
		});
		
		saveAsAction = new UsnaAction(ScriptFrame.this, "dlgSaveAs", "/images/SaveAs24.png", e -> {
			final JFileChooser fc = (path == null) ? new JFileChooser() : new JFileChooser(path.getParent().toFile());
			fc.setFileFilter(new FileNameExtensionFilter(LABELS.getString("filetype_js_desc"), DialogDeviceScripts.FILE_EXTENSION));
			if(fc.showSaveDialog(ScriptFrame.this) == JFileChooser.APPROVE_OPTION) {
				try {
					Path toSave = IOFile.addExtension(fc.getSelectedFile().toPath(), DialogDeviceScripts.FILE_EXTENSION);
					Files.writeString(toSave, editor.getText());
					JOptionPane.showMessageDialog(ScriptFrame.this, LABELS.getString("msgFileSaved"), LABELS.getString("dlgScriptEditorTitle"), JOptionPane.INFORMATION_MESSAGE);
					setTitle(script.getName() + " - " + toSave.getFileName());
					canOverwriteFile = true;
				} catch (IOException e1) {
					Msg.errorMsg(e1);
				}
				path = fc.getSelectedFile().toPath();
			}
		});
		
		saveAction = new UsnaAction(ScriptFrame.this, "dlgSave", "/images/Save24.png", e -> {
			if(canOverwriteFile) {
				try {
					Files.writeString(path, editor.getText());
					JOptionPane.showMessageDialog(ScriptFrame.this, LABELS.getString("msgFileSaved"), LABELS.getString("dlgScriptEditorTitle"), JOptionPane.INFORMATION_MESSAGE);
				} catch (IOException e1) {
					Msg.errorMsg(e1);
				}
			} else {
				saveAsAction.actionPerformed(e);
			}
		});
		editor.mapAction(KeyStroke.getKeyStroke(KeyEvent.VK_S, MainView.SHORTCUT_KEY), saveAction, "save_usna");

		uploadAction = new UsnaAction(this, "btnUploadTooltip", "/images/Upload24.png", e -> {
			if(editor.getText().isEmpty()) {
				Msg.errorMsg(this, "dlgScriptEditorMsgEmpty");
			} else {
				String res = script.putCode(editor.getText());
				if(res != null) {
					Msg.errorMsg(this, res);
				}
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
			String res = script.putCode(editor.getText());
			if(res != null) {
				Msg.errorMsg(this, res);
			} else {
				runStopAction.actionPerformed(e);
			}
		});
		
		gotoAction = new UsnaAction(null, "dlgScriptEditorGotoLineTitle", "/images/goto_line.png", e -> {
			JPanel msg = new JPanel(new FlowLayout());
			JTextField input = new JTextField(8);
			msg.add(new JLabel(LABELS.getString("dlgScriptEditorGotoLineLabel")));
			msg.add(input);

			DocumentFilter filter = new DocumentFilter() {
				@Override
				public void insertString(DocumentFilter.FilterBypass fb, int offset, String text, AttributeSet attr) throws BadLocationException {
					super.insertString(fb, offset, text.replaceAll("\\D", ""), attr);
				}

				@Override
				public void replace(DocumentFilter.FilterBypass fb, int offset, int length, String text, AttributeSet attr) throws BadLocationException {
					if (length > 0) {
						fb.remove(offset, length);
					}
					insertString(fb, offset, text, attr);
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
		caretLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
		editor.addCaretListener(e -> caretLabel.setText(editor.getCaretRow() + " : " + editor.getCaretColumn()));
		scrollPane.getVerticalScrollBar().setUnitIncrement(editor.getFont().getSize());
		
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
			try {
				if(wsSession != null && wsSession.get().isOpen()) {
					wsSession.get().close(StatusCode.NORMAL, "bye", Callback.NOOP);
				}
			} catch (Exception e1) {
				LOG.error("webSocketClient.disconnect", e1);
			}
		}
	}
	
	private JPanel logPanel() throws IOException {
		logsTextArea.setEditable(false);
		JPanel mainLogPanel = new JPanel(new BorderLayout());
		
		if(darkMode) {
			logsTextArea.setBackground(DARK_BACKGOUND_COLOR);
			logsTextArea.setForeground(Color.WHITE);
		}

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 2, 4, 2));
		scrollPane.setViewportView(logsTextArea);

		mainLogPanel.add(scrollPane, BorderLayout.CENTER);
		
		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		
		Action erase = new UsnaAction(this, null, "/images/erase-9-16.png", e -> logsTextArea.setText("") );
		toolBar.add(erase).setBorder(BorderFactory.createEmptyBorder(1, 2, 0, 2));
		
		mainLogPanel.add(toolBar, BorderLayout.NORTH);
		scrollPane.getVerticalScrollBar().setUnitIncrement(editor.getFont().getSize());
		
		return mainLogPanel;
	}
	
	private JToolBar getToolBar() {
		UsnaAction btnHelp = new UsnaAction(null, "helpBtnTooltip", "/images/Question24.png", e -> {
			String close = LABELS.getString("dlgClose");
			if(JOptionPane.showOptionDialog(this, LABELS.getString("dlgScriptEditorHelp"), LABELS.getString("dlgScriptEditorTitle"),
					JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, new Object[] {close, LABELS.getString("lblManual")}, close) == 1) {
				try {
					Desktop.getDesktop().browse(URI.create(LABELS.getString("dlgIDEManualUrl")));
				} catch (IOException | UnsupportedOperationException ex) {
					Msg.errorMsg(this, ex);
				}
			}
		});
		
		JToolBar toolBar = new JToolBar();
		toolBar.add(openAction);
		toolBar.add(saveAction);
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
		toolBar.add(Box.createHorizontalGlue());
		toolBar.add(caretLabel);
		toolBar.add(btnHelp);
		return toolBar;
	}
	
	private void activateLogConnection() {
		try {
			if(device.getDebugMode() == LogMode.SOCKET == false) {
				device.setDebugMode(LogMode.SOCKET, true);
			}
			WebSocketDeviceListener wsListener = new LogWebSocketDeviceListener();
			wsSession = device.connectWebSocketLogs(wsListener);
			wsSession.get().setIdleTimeout(Duration.ofMinutes(30));
		} catch(IOException | InterruptedException | ExecutionException e) {
			LOG.error("activateLogConnection", e);
		}
	}
	
	public class LogWebSocketDeviceListener extends WebSocketDeviceListener {
		public LogWebSocketDeviceListener() {
			super(node -> node.path("level").intValue() == 2 && node.path("fd").asInt(100 + scriptId) == 100 + scriptId); // Info
		}
		
		@Override
		public void onWebSocketError(Throwable cause) {
			if(cause instanceof java.nio.channels.ClosedChannelException == false) {
				logsTextArea.append("Shelly Scanner error: " + cause.getMessage() + "\n");
				LOG.debug("ws-error", cause);
			}
		}

		@Override
		public void onMessage(JsonNode msg) {
			logsTextArea.append(msg.path("data").asText() + "\n");
			logsTextArea.setCaretPosition(logsTextArea.getDocument().getLength());
		}
	}
	
	private String loadCodeFromFile(Path in) {
		try {
			try(FileSystem fs = FileSystems.newFileSystem(URI.create("jar:" + in.toUri()), Map.of()); Stream<Path> pathStream = Files.list(fs.getPath("/"))) {
				String[] scriptList = pathStream.filter(p -> p.getFileName().toString().endsWith(".mjs")).map(p -> p.getFileName().toString().substring(0, p.getFileName().toString().length() - 4)).toArray(String[]::new);
				if(scriptList.length > 0) {
					Object sName = JOptionPane.showInputDialog(this, LABELS.getString("scrSelectionMsg"), LABELS.getString("scrSelectionTitle"), JOptionPane.PLAIN_MESSAGE, null, scriptList, null);
					if(sName != null) {
						canOverwriteFile = false;
						return Files.readString(fs.getPath(sName + ".mjs")).replaceAll("\\r+\\n", "\n");
					}
				} else {
					JOptionPane.showMessageDialog(this, LABELS.getString("scrNoneInZipFile"), LABELS.getString("btnUpload"), JOptionPane.INFORMATION_MESSAGE);
				}
			} catch (ProviderNotFoundException e) { // no zip (backup) -> text file
				canOverwriteFile = true;
				return Files.readString(in).replaceAll("\\r+\\n", "\n");
			}
		} catch (FileNotFoundException | NoSuchFileException e) {
			canOverwriteFile = false;
			Msg.errorMsg(this, String.format(LABELS.getString("msgFileNotFound"), in.getFileName().toString()));
		} catch (/*IO*/Exception e) {
			canOverwriteFile = false;
			Msg.errorMsg(this, e);
		} finally {
			setCursor(Cursor.getDefaultCursor());
		}
		return null;
	}
	
	public static void main(String ...strings) throws IOException {
		ScannerProperties.init(Path.of(System.getProperty("user.home"), ".shellyScanner")).load(true);
		new ScriptFrame();
	}
}