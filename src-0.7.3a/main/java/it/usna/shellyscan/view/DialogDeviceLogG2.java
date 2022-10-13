package it.usna.shellyscan.view;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.swing.dialog.FindReplaceDialog;
import java.awt.Component;
import javax.swing.Box;

public class DialogDeviceLogG2 extends JDialog implements ClipboardOwner {
	private static final long serialVersionUID = 1L;
	private final static int SHORTCUT_KEY = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
	private WebSocketClient clientEndPoint = null;

	public DialogDeviceLogG2(final MainView owner, Devices model, int index) {
		super(owner, false);
		AbstractG2Device device = (AbstractG2Device) model.get(index);
		final String dName = device.getName();
		setTitle(device.getHostname() + " - " + (dName.length() > 0 ? dName : device.getTypeName()));
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		JPanel buttonsPanel = new JPanel();
		getContentPane().add(buttonsPanel, BorderLayout.SOUTH);
		
		JTextArea textArea = new JTextArea();
		textArea.setEditable(false);

		final Action findAction = new AbstractAction(Main.LABELS.getString("btnFind")) {
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e) {
				FindReplaceDialog f = new FindReplaceDialog(DialogDeviceLogG2.this, textArea, false);
				f.setLocationRelativeTo(DialogDeviceLogG2.this);
				f.setVisible(true);
			}
		};
		JButton jButtonFind = new JButton(findAction);
		jButtonFind.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F, SHORTCUT_KEY), "find_act");
		jButtonFind.getActionMap().put("find_act", findAction);

		JButton jButtonCopyAll = new JButton(Main.LABELS.getString("btnCopyAll"));
		jButtonCopyAll.addActionListener(event -> {
			final String cp = textArea.getText();
			if(cp != null && cp.length() > 0) {
				final Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
				cb.setContents(new StringSelection(cp), DialogDeviceLogG2.this);
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
		btnActivateLog.addActionListener(event -> {
			try {
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				device.postCommand("Sys.SetConfig", "{\"config\": {\"debug\":{\"websocket\":{\"enable\": true}}}");
				if(clientEndPoint.isClosed()) {
					clientEndPoint.reconnect();
				}
			} finally {
				setCursor(Cursor.getDefaultCursor());
			}
		});

		JButton btnStopLog = new JButton(Main.LABELS.getString("dlgLogG2Deactivate"));
		buttonsPanel.add(btnStopLog);
		btnStopLog.addActionListener(event -> {
			try {
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				device.postCommand("Sys.SetConfig", "{\"config\": {\"debug\":{\"websocket\":{\"enable\": false}}}");
			} finally {
				setCursor(Cursor.getDefaultCursor());
			}
		});
		
		JButton btnsStopAppRefresh = new JButton(Main.LABELS.getString("dlgLogG2PauseRefresh"));
		buttonsPanel.add(btnsStopAppRefresh);
		btnsStopAppRefresh.addActionListener(event -> {
			model.pauseRefresh(index);
			textArea.append(">>>> " + Main.APP_NAME +" refresh process stopped\n");
		});

		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout(0, 0));
		try {
			final ObjectMapper mapper = new ObjectMapper();
			clientEndPoint = new org.java_websocket.client.WebSocketClient(new URI("ws://" + device.getHttpHost().getHostName() + "/debug/log")) {
				@Override
				public void onMessage( String message ) {
					try {
						JsonNode msg = mapper.readTree(message);
						textArea.append(msg.get("ts").asLong() + " - L" + msg.get("level").asText() + ": " + msg.get("data").asText().trim() + "\n");
					} catch (JsonProcessingException ex) {
						textArea.append(">>>> Error: " + ex.toString() + "\n");
					}
					textArea.setCaretPosition(textArea.getText().length());
				}

				@Override
				public void onOpen(ServerHandshake handshakedata) {
//					textArea.append(">>>> Open\n");
				}

				@Override
				public void onClose(int code, String reason, boolean remote) {
					textArea.append(">>>> Close: " + reason  + " (" + code + ")\n");
				}

				@Override
				public void onError(Exception ex) {
					textArea.append(">>>> Error: " + ex.toString() + "\n");
				}
			};
			clientEndPoint.connect();
		} catch (URISyntaxException e) {
			textArea.append(e.toString());
		} finally {
			clientEndPoint.close();
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
				clientEndPoint.close();
				model.refresh(index, false);
			}
		});
		
		this.setSize(680, 650);
		setLocationRelativeTo(owner);
		setVisible(true);
	}

	@Override
	public void lostOwnership(Clipboard clipboard, Transferable contents) {
	}
}