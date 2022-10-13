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
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.text.JTextComponent;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.swing.dialog.FindReplaceDialog;

public class DialogDeviceInfo extends JDialog implements ClipboardOwner {
	private static final long serialVersionUID = 1L;
	private final static int SHORTCUT_KEY = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
	private ExecutorService executor = Executors.newFixedThreadPool(2); // too many concurrent requests are dangerous (device reboot)

	public DialogDeviceInfo(final MainView owner, boolean json, ShellyAbstractDevice device, String[] infoList) {
		super(owner, false);
		final String dName = device.getName();
		setTitle(device.getHostname() + " - " + (dName.length() > 0 ? dName : device.getTypeName()));
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		this.setSize(530, 650);
		setLocationRelativeTo(owner);
		
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		getContentPane().add(tabbedPane, BorderLayout.CENTER);
		
		JPanel buttonsPanel = new JPanel();
		getContentPane().add(buttonsPanel, BorderLayout.SOUTH);
		
		final Supplier<JTextComponent> ta = () -> (JTextComponent)((JScrollPane)((JPanel)tabbedPane.getSelectedComponent()).getComponent(0)).getViewport().getView();

		final Action findAction = new AbstractAction(Main.LABELS.getString("btnFind")) {
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e) {
				FindReplaceDialog f = new FindReplaceDialog(DialogDeviceInfo.this, ta, false);
				f.setLocationRelativeTo(DialogDeviceInfo.this);
				f.setVisible(true);
			}
		};
		JButton jButtonFind = new JButton(findAction);
		jButtonFind.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F, SHORTCUT_KEY), "find_act");
		jButtonFind.getActionMap().put("find_act", findAction);
		
		JButton jButtonCopyAll = new JButton(Main.LABELS.getString("btnCopyAll"));
		jButtonCopyAll.addActionListener(event -> {
			final String cp = ta.get().getText();
			if(cp != null && cp.length() > 0) {
				final Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
				cb.setContents(new StringSelection(cp), DialogDeviceInfo.this);
			}
		});

		JButton jButtonClose = new JButton(Main.LABELS.getString("dlgClose"));
		jButtonClose.addActionListener(event -> dispose());

		buttonsPanel.add(jButtonFind);
		buttonsPanel.add(jButtonCopyAll);
		buttonsPanel.add(jButtonClose);
		
		setVisible(true);
		
		for(String info: infoList) {
			String name = info.replaceFirst("^/", "").replaceFirst("rpc/", "").replaceFirst("Shelly\\.", "");
			tabbedPane.add(name, getPanel(info, json, device));
		}
	}
	
	public JPanel getPanel(String info, boolean isJson, ShellyAbstractDevice device) {
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout(0, 0));
		JTextArea textArea = new JTextArea();
		textArea.setEditable(false);
		executor.execute(() -> {
			panel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			try (CloseableHttpClient httpClient = HttpClients.createDefault();
					CloseableHttpResponse response = httpClient.execute(device.getHttpHost(), new HttpGet(info), device.getClientContext());
					InputStream in = response.getEntity().getContent()) {
				if(Thread.interrupted() == false) {
					if(isJson) {
						ObjectMapper mapper = new ObjectMapper();
						JsonNode root = mapper.readTree(in);
						final String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
						textArea.setText(json);
					} else {
						try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
							br.lines().forEach(l -> textArea.append(l + "\n"));
						}
					}
					textArea.setCaretPosition(0);
				}
			} catch (java.io.FileNotFoundException e) {
				textArea.setText("Resource not found: " + e.getMessage());
			} catch (Exception e) {
				if(Thread.interrupted() == false) {
					textArea.setText(e.getMessage());
				}
			}
			panel.setCursor(Cursor.getDefaultCursor());
		});
		JScrollPane scrollPane = new JScrollPane(textArea);
		panel.add(scrollPane);
		return panel;
	}
	
	@Override
	public void dispose() {
		executor.shutdownNow();
		super.dispose();
	}
	
	@Override
	public void lostOwnership(Clipboard clipboard, Transferable contents) {
	}
}