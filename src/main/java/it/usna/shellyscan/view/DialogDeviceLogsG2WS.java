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
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.swing.AbstractAction;
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

import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.ShellyAbstractDevice.LogMode;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.g2.WebSocketDeviceListener;
import it.usna.shellyscan.view.util.Msg;
import it.usna.shellyscan.view.util.UsnaTextPane;
import it.usna.shellyscan.view.util.UtilMiscellaneous;
import it.usna.swing.dialog.FindReplaceDialog;

public class DialogDeviceLogsG2WS extends JDialog {
	private static final long serialVersionUID = 1L;
	private final static Logger LOG = LoggerFactory.getLogger(DialogDeviceLogsG2WS.class);
	private boolean logWasActive;
	private Future<Session> wsSession;
	private UsnaTextPane textArea = new UsnaTextPane();
	private Style bluStyle = textArea.addStyle("blue", null);
	private JComboBox<String> comboBox = new JComboBox<>();

	public DialogDeviceLogsG2WS(final Window owner, Devices devicesModel, int modelIndex, int initLlogLevel) {
		super(owner, ModalityType.MODELESS);
		AbstractG2Device device = (AbstractG2Device) devicesModel.get(modelIndex);
		setTitle(UtilMiscellaneous.getExtendedHostName(device));
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		logWasActive = device.getDebugMode() == LogMode.SOCKET;
		if(logWasActive == false) {
			device.setDebugMode(LogMode.SOCKET, true);
		}

		JPanel buttonsPanel = new JPanel();
		getContentPane().add(buttonsPanel, BorderLayout.SOUTH);
		
		StyledDocument document = textArea.getStyledDocument();
		StyleConstants.setForeground(bluStyle, Color.BLUE);
		textArea.setEditable(false);

		final Action findAction = new AbstractAction(Main.LABELS.getString("btnFind")) {
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e) {
				FindReplaceDialog f = new FindReplaceDialog(DialogDeviceLogsG2WS.this, textArea, false);
				f.setLocationRelativeTo(DialogDeviceLogsG2WS.this);
				f.setVisible(true);
			}
		};
		JButton jButtonFind = new JButton(findAction);
		jButtonFind.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F, MainView.SHORTCUT_KEY), "find_act");
		jButtonFind.getActionMap().put("find_act", findAction);

		JButton jButtonCopyAll = new JButton(Main.LABELS.getString("btnCopyAll"));
		jButtonCopyAll.addActionListener(event -> {
			final String cp = textArea.getText();
			if (cp != null && cp.length() > 0) {
				final Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
				StringSelection selection = new StringSelection(cp);
				cb.setContents(selection, selection);
			}
		});

		JButton jButtonClose = new JButton(Main.LABELS.getString("dlgClose"));
		jButtonClose.addActionListener(event -> dispose());

		buttonsPanel.add(jButtonFind);
		buttonsPanel.add(jButtonCopyAll);
		buttonsPanel.add(jButtonClose);

		Component horizontalStrut = Box.createHorizontalStrut(25);
		buttonsPanel.add(horizontalStrut);

		JButton btnActivateLog = new JButton(Main.LABELS.getString("dlgLogG2Activate"));
		buttonsPanel.add(btnActivateLog);

		JButton btnStopLog = new JButton(Main.LABELS.getString("dlgLogG2Deactivate"));
		buttonsPanel.add(btnStopLog);

		JButton btnsStopAppRefresh = new JButton(Main.LABELS.getString("dlgLogG2PauseRefresh"));
		buttonsPanel.add(btnsStopAppRefresh);
		btnsStopAppRefresh.addActionListener(event -> {
			devicesModel.pauseRefresh(modelIndex);
			try {
				document.insertString(document.getLength(), ">>>> " + Main.APP_NAME + " refresh process stopped\n", bluStyle);
			} catch (BadLocationException e1) {}
		});

		JLabel lblNewLabel = new JLabel(Main.LABELS.getString("dlgLogG2Level"));
		buttonsPanel.add(lblNewLabel);

		comboBox.addItem(LABELS.getString("dlgLogG2Lev0")); // error
		comboBox.addItem(LABELS.getString("dlgLogG2Lev1")); // warn
		comboBox.addItem(LABELS.getString("dlgLogG2Lev2")); // info
		comboBox.addItem(LABELS.getString("dlgLogG2Lev3")); // debug
		comboBox.addItem(LABELS.getString("dlgLogG2Lev4")); // verbose
		comboBox.setSelectedIndex(initLlogLevel);
		buttonsPanel.add(comboBox);
		
		try {
			WebSocketDeviceListener wsListener = new LogWebSocketDeviceListener();
			wsSession = device.connectWebSocketLogs(wsListener);
			wsSession.get().setIdleTimeout(Duration.ofMinutes(30));
			btnActivateLog.setEnabled(false);

			btnActivateLog.addActionListener(event -> {
				try {
					setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					if (wsSession.get().isOpen() == false) {
						wsSession = device.connectWebSocketLogs(wsListener);
						wsSession.get().setIdleTimeout(Duration.ofMinutes(30));
					}
					btnActivateLog.setEnabled(false);
					btnStopLog.setEnabled(true);
				} catch (Exception e1) {
					LOG.error("webSocketClient.connect", e1);
				} finally {
					setCursor(Cursor.getDefaultCursor());
				}
			});
			
			btnActivateLog.doClick();

			btnStopLog.addActionListener(event -> {
				try {
					setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					try {
						if (wsSession.get().isOpen()) {
							wsSession.get().close(StatusCode.NORMAL, "bye", Callback.NOOP);
						}
						btnActivateLog.setEnabled(true);
						btnStopLog.setEnabled(false);
					} catch (/*IOException |*/ InterruptedException | ExecutionException e1) {
						LOG.error("webSocketClient.close", e1);
					}
				} finally {
					setCursor(Cursor.getDefaultCursor());
				}
			});

			JPanel panel = new JPanel(new BorderLayout());
			JScrollPane scrollPane = new JScrollPane(textArea);
			panel.add(scrollPane);

			getContentPane().add(panel, BorderLayout.CENTER);

			addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					try {
//						wsSession.get().disconnect();
						wsSession.get().close(StatusCode.NORMAL, "bye", Callback.NOOP);
					} catch (Exception e1) {
						LOG.error("webSocketClient.disconnect", e1);
					}
					dispose();
				}

				@Override
				public void windowClosed(WindowEvent e) {
					if(logWasActive == false) {
						device.setDebugMode(LogMode.SOCKET, false);
					}
				}
			});

			this.setSize(700, 650);
			setLocationRelativeTo(owner);
			setVisible(true);
		} catch (Exception e) {
			Msg.errorMsg(owner, e); // Msg.errorMsg(...) do log
		}
	}

	public class LogWebSocketDeviceListener extends WebSocketDeviceListener {
		@Override
		public void onWebSocketOpen(Session session) {
			textArea.append(">>>> Open\n", bluStyle);
		}

		@Override
		public void onWebSocketClose(int statusCode, String reason) {
			textArea.append(">>>> Close: " + reason + " (" + statusCode + ")\n", bluStyle);
		}

		@Override
		public void onMessage(JsonNode msg) {
			int logLevel = comboBox.getSelectedIndex();
			int level = msg.get("level").asInt(0);
			if (level <= logLevel) {
				textArea.append(msg.get("ts").asLong() + " - L" + level + " - fd" + msg.path("fd").asText() + ": " + msg.get("data").asText().trim() + "\n");
			}
			textArea.setCaretPosition(textArea.getStyledDocument().getLength());
		}
	}
}