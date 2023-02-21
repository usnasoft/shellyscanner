package it.usna.shellyscan.view;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URI;
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
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.view.util.UtilCollecion;
import it.usna.swing.dialog.FindReplaceDialog;

public class DialogDeviceLogsG2 extends JDialog {
	private static final long serialVersionUID = 1L;
	private final static Logger LOG = LoggerFactory.getLogger(DialogDeviceLogsG2.class);

	public DialogDeviceLogsG2(final MainView owner, Devices model, int index) {
		super(owner, false);
		AbstractG2Device device = (AbstractG2Device) model.get(index);
		setTitle(UtilCollecion.getExtendedHostName(device));
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		activateLog(device);

		JPanel buttonsPanel = new JPanel();
		getContentPane().add(buttonsPanel, BorderLayout.SOUTH);

		JTextPane textArea = new JTextPane();
		StyledDocument document = textArea.getStyledDocument();
		Style bluStyle = textArea.addStyle("blue", null);
		StyleConstants.setForeground(bluStyle, Color.BLUE);
		textArea.setEditable(false);

		final Action findAction = new AbstractAction(Main.LABELS.getString("btnFind")) {
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e) {
				FindReplaceDialog f = new FindReplaceDialog(DialogDeviceLogsG2.this, textArea, false);
				f.setLocationRelativeTo(DialogDeviceLogsG2.this);
				f.setVisible(true);
			}
		};
		JButton jButtonFind = new JButton(findAction);
		jButtonFind.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F, MainView.SHORTCUT_KEY), "find_act");
		jButtonFind.getActionMap().put("find_act", findAction);

		JButton jButtonCopyAll = new JButton(Main.LABELS.getString("btnCopyAll"));
		jButtonCopyAll.addActionListener(event -> {
			final String cp = textArea.getText();
			if(cp != null && cp.length() > 0) {
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
			model.pauseRefresh(index);
			try {
				document.insertString(document.getLength(), ">>>> " + Main.APP_NAME +" refresh process stopped\n", bluStyle);
			} catch (BadLocationException e1) {}
		});

		JLabel lblNewLabel = new JLabel(Main.LABELS.getString("dlgLogG2Level"));
		buttonsPanel.add(lblNewLabel);

		JComboBox<String> comboBox = new JComboBox<>();
		comboBox.addItem(LABELS.getString("dlgLogG2Lev0")); // error
		comboBox.addItem(LABELS.getString("dlgLogG2Lev1")); // warn
		comboBox.addItem(LABELS.getString("dlgLogG2Lev2")); // info
		comboBox.addItem(LABELS.getString("dlgLogG2Lev3")); // debug
		comboBox.addItem(LABELS.getString("dlgLogG2Lev4")); // verbose
		comboBox.setSelectedIndex(4);
		buttonsPanel.add(comboBox);

		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout(0, 0));

		WebSocketClient webSocketClient = new WebSocketClient(model.getHttpClient());
		try {
			WebSocketListener wsListener = new WebSocketListener() {
				private final ObjectMapper mapper = new ObjectMapper();

				@Override
				public void onWebSocketConnect(Session session) {
					try {
						document.insertString(document.getLength(), ">>>> Open\n", bluStyle);
					} catch (BadLocationException e) {}
				}

				@Override
				public void onWebSocketClose(int statusCode, String reason) {
					try {
						document.insertString(document.getLength(), ">>>> Close: " + reason  + " (" + statusCode + ")\n", bluStyle);
					} catch (BadLocationException e) {}
				}

				@Override
				public void onWebSocketError(Throwable cause) {
					LOG.warn("onWebSocketError: {}", cause.toString());
				}

				@Override
				public void onWebSocketText(String message) {
					try {
						int logLevel = comboBox.getSelectedIndex();
						JsonNode msg = mapper.readTree(message);
						int level = msg.get("level").asInt(0);
						if(level <= logLevel) {
							try {
								document.insertString(document.getLength(), msg.get("ts").asLong() + " - L" + level + ": " + msg.get("data").asText().trim() + "\n", null);
							} catch (BadLocationException e) {}
						}
					} catch (JsonProcessingException ex) {
						try {
							document.insertString(document.getLength(), ">>>> Error: " + ex.toString() + "\n", bluStyle);
						} catch (BadLocationException e) {}
					}
//					textArea.setCaretPosition(textArea.getText().length());
					textArea.setCaretPosition(document.getLength());
				}

				@Override
				public void onWebSocketBinary(byte[] payload, int offset, int length) {}
			};

			webSocketClient.start();
			final Future<Session> session = webSocketClient.connect(wsListener, URI.create("ws://" + device.getAddress().getHostAddress() + "/debug/log"));

//			test(webSocketClient, device);
			btnActivateLog.addActionListener(event -> {
				try {
					setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					activateLog(device);
					if(session.get().isOpen() == false) {
						webSocketClient.connect(wsListener, URI.create("ws://" + device.getAddress().getHostAddress() + "/debug/log"));
					}
				} catch (Exception e1) {
					LOG.error("webSocketClient.connect", e1);
				} finally {
					setCursor(Cursor.getDefaultCursor());
				}
			});

			btnStopLog.addActionListener(event -> {
				try {
					setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					try {
						if(session.get().isOpen()) {
							session.get().disconnect();
						}
					} catch (IOException | InterruptedException | ExecutionException e1) {
						LOG.error("webSocketClient.connect", e1);
					}
					device.postCommand("Sys.SetConfig", "{\"config\": {\"debug\":{\"websocket\":{\"enable\": false}}}");
				} finally {
					setCursor(Cursor.getDefaultCursor());
				}
			});
		} catch(Exception e) {
			LOG.error("webSocketClient.start", e);
		}

		JScrollPane scrollPane = new JScrollPane(textArea);
		panel.add(scrollPane);

		getContentPane().add(panel, BorderLayout.CENTER);

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				dispose();
			}
			@Override
			public void windowClosed(WindowEvent e) {
				try {
					webSocketClient.stop();
				} catch (Exception e1) {
					LOG.error("webSocketClient.stop", e1);
				}
				model.refresh(index, false);
			}
		});

		this.setSize(700, 650);
		setLocationRelativeTo(owner);
		setVisible(true);
	}

	private static void activateLog(AbstractG2Device device) {
		if(device.getDebugMode() != AbstractG2Device.LogMode.SOCKET) {
			device.postCommand("Sys.SetConfig", "{\"config\": {\"debug\":{\"websocket\":{\"enable\": true}}}");
		}
	}
	
	private void test(WebSocketClient webSocketClient, AbstractG2Device device) {
		try {
			Future<Session> session = webSocketClient.connect(new WebSocketListener() {
				@Override
				public void onWebSocketConnect(Session session) {
					System.out.println(">>>> Open");
				}

				@Override
				public void onWebSocketClose(int statusCode, String reason) {
					System.out.println(">>>> Close: " + reason  + " (" + statusCode + ")");
				}

				@Override
				public void onWebSocketError(Throwable cause) {
					System.out.println("onWebSocketError: " + cause);
				}

				@Override
				public void onWebSocketText(String message) {
					System.out.println(message);
				}

				@Override
				public void onWebSocketBinary(byte[] payload, int offset, int length) {}
			}, URI.create("ws://" + device.getAddress().getHostAddress() + "/rpc"));
			
			RemoteEndpoint remote = session.get().getRemote();
//			remote.sendStringByFuture("{\"id\":2, \"src\":\"user_1\", \"method\":\"Switch.Set\", \"params\":{\"id\":0, \"on\":true}}");
			remote.sendStringByFuture("{\"id\":2, \"S_Scanner\":\"user\", \"method\":\"Shelly.GetStatus\"}");
		} catch (IOException | InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}