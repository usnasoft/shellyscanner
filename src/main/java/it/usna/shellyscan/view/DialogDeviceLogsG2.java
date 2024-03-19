package it.usna.shellyscan.view;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.g2.HttpLogsListener;
import it.usna.shellyscan.view.util.Msg;
import it.usna.shellyscan.view.util.UsnaTextPane;
import it.usna.shellyscan.view.util.UtilMiscellaneous;
import it.usna.swing.dialog.FindReplaceDialog;

public class DialogDeviceLogsG2 extends JDialog {
	private static final long serialVersionUID = 1L;
//	private final static Logger LOG = LoggerFactory.getLogger(DialogDeviceLogsG2.class);
	private UsnaTextPane textArea = new UsnaTextPane();
	private Style bluStyle = textArea.addStyle("blue", null);
	private JComboBox<String> comboBox = new JComboBox<>();
	private JButton btnActivateLog;
	private JButton btnStopLog;
	private boolean readLogs;

	public DialogDeviceLogsG2(final Window owner, Devices devicesModel, int modelIndex, int initLlogLevel) {
		super(owner, ModalityType.MODELESS);
		AbstractG2Device device = (AbstractG2Device) devicesModel.get(modelIndex);
		setTitle(UtilMiscellaneous.getExtendedHostName(device));
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		JPanel buttonsPanel = new JPanel();
		getContentPane().add(buttonsPanel, BorderLayout.SOUTH);
		
		StyledDocument document = textArea.getStyledDocument();
		StyleConstants.setForeground(bluStyle, Color.BLUE);
		textArea.setEditable(false);

		final Action findAction = new UsnaAction("btnFind", e-> {
			FindReplaceDialog f = new FindReplaceDialog(DialogDeviceLogsG2.this, textArea, false);
			f.setLocationRelativeTo(DialogDeviceLogsG2.this);
			f.setVisible(true);
		});

		JButton jButtonFind = new JButton(findAction);
		jButtonFind.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F, MainView.SHORTCUT_KEY), "find_act");
		jButtonFind.getActionMap().put("find_act", findAction);

		JButton jButtonCopyAll = new JButton(LABELS.getString("btnCopyAll"));
		jButtonCopyAll.addActionListener(event -> {
			final String cp = textArea.getText();
			if (cp != null && cp.length() > 0) {
				final Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
				StringSelection selection = new StringSelection(cp);
				cb.setContents(selection, selection);
			}
		});

		JButton jButtonClose = new JButton(LABELS.getString("dlgClose"));
		jButtonClose.addActionListener(event -> dispose());

		buttonsPanel.add(jButtonFind);
		buttonsPanel.add(jButtonCopyAll);
		buttonsPanel.add(jButtonClose);

		Component horizontalStrut = Box.createHorizontalStrut(25);
		buttonsPanel.add(horizontalStrut);

		btnActivateLog = new JButton(LABELS.getString("dlgLogG2Activate"));
		buttonsPanel.add(btnActivateLog);

		btnStopLog = new JButton(LABELS.getString("dlgLogG2Deactivate"));
		buttonsPanel.add(btnStopLog);

		JButton btnsStopAppRefresh = new JButton(LABELS.getString("dlgLogG2PauseRefresh"));
		buttonsPanel.add(btnsStopAppRefresh);
		btnsStopAppRefresh.addActionListener(event -> {
			devicesModel.pauseRefresh(modelIndex);
			try {
				document.insertString(document.getLength(), ">>>> " + Main.APP_NAME + " refresh process stopped\n", bluStyle);
			} catch (BadLocationException e1) {}
		});

		JLabel lblNewLabel = new JLabel(LABELS.getString("dlgLogG2Level"));
		buttonsPanel.add(lblNewLabel);

		comboBox.addItem(LABELS.getString("dlgLogG2Lev0")); // error
		comboBox.addItem(LABELS.getString("dlgLogG2Lev1")); // warn
		comboBox.addItem(LABELS.getString("dlgLogG2Lev2")); // info
		comboBox.addItem(LABELS.getString("dlgLogG2Lev3")); // debug
		comboBox.addItem(LABELS.getString("dlgLogG2Lev4")); // verbose
		comboBox.setSelectedIndex(initLlogLevel);
		buttonsPanel.add(comboBox);

		activateLogConnection(device);

		btnActivateLog.addActionListener(event -> {
			try {
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				activateLogConnection(device);
			} finally {
				setCursor(Cursor.getDefaultCursor());
			}
		});

		btnStopLog.addActionListener(event -> {
			readLogs = false;
			btnActivateLog.setEnabled(true);
			btnStopLog.setEnabled(false);
			textArea.append(">>>> Pause\n", bluStyle);
		});

		JPanel panel = new JPanel(new BorderLayout());
		JScrollPane scrollPane = new JScrollPane(textArea);
		panel.add(scrollPane);

		getContentPane().add(panel, BorderLayout.CENTER);

		this.setSize(700, 650);
		setLocationRelativeTo(owner);
		setVisible(true);
	}
	
	@Override
	public void dispose() {
		readLogs = false;
		super.dispose();
	}
	
	private void activateLogConnection(AbstractG2Device device) {
		try {
			textArea.append(">>>> Connect\n", bluStyle);
			readLogs = true;
			device.connectHttpLogs(new HttpLogsListener() {
				@Override
				public void accept(String txt) {
					int logLevel = comboBox.getSelectedIndex();
					String[] logLine = txt.split("\\s", 3);
					int level;
					try {
						level = Integer.parseInt(logLine[1]);
					} catch(Exception e) {
						level = Integer.MIN_VALUE;					
					}
					if(logLine.length < 3 || level == Integer.MIN_VALUE) {
						textArea.append(txt.trim() + "\n");
						textArea.setCaretPosition(textArea.getStyledDocument().getLength());
					} else if(level <= logLevel) {
						textArea.append(logLine[0] + "-L" + logLine[1] + ": " + logLine[2].trim( )+ "\n");
						textArea.setCaretPosition(textArea.getStyledDocument().getLength());
					}
				}
				
				@Override
				public boolean requestNext() {
					return readLogs;
				}
			});
			btnActivateLog.setEnabled(false);
			btnStopLog.setEnabled(true);
		} catch (RuntimeException e) {
			Msg.errorMsg(this, e);
		}
	}
}