package it.usna.shellyscan.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

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

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.g1.AbstractBatteryDevice;
import it.usna.swing.dialog.FindReplaceDialog;

public class DialogDeviceInfo extends JDialog {
	private static final long serialVersionUID = 1L;
//	private final static int SHORTCUT_KEY = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(); // from 1.10 getMenuShortcutKeyMaskEx
	private ScheduledExecutorService executor = Executors.newScheduledThreadPool(2); // too many concurrent requests are dangerous (device reboot)

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

		JButton btnRefresh = new JButton(new UsnaAction("labelRefresh", e -> {
			int s = tabbedPane.getSelectedIndex();
			fill(tabbedPane, json, device, infoList);
			tabbedPane.setSelectedIndex(s);
		}));

		final Supplier<JTextComponent> ta = () -> (JTextComponent)((JScrollPane)((JPanel)tabbedPane.getSelectedComponent()).getComponent(0)).getViewport().getView();
		final Action findAction = new UsnaAction("btnFind", e -> {
			FindReplaceDialog f = new FindReplaceDialog(DialogDeviceInfo.this, ta, false);
			f.setLocationRelativeTo(DialogDeviceInfo.this);
			f.setVisible(true);
		});

		JButton jButtonFind = new JButton(findAction);
		jButtonFind.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F, MainView.SHORTCUT_KEY), "find_act");
		jButtonFind.getActionMap().put("find_act", findAction);


		JButton jButtonCopyAll = new JButton(new UsnaAction("btnCopyAll", e -> {
			final String cp = ta.get().getText();
			if(cp != null && cp.length() > 0) {
				final Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
				StringSelection selection = new StringSelection(cp);
				cb.setContents(selection, selection);
			}
		}));

		JButton jButtonClose = new JButton(new UsnaAction("dlgClose", e -> dispose()));

		buttonsPanel.add(btnRefresh);
		buttonsPanel.add(jButtonFind);
		buttonsPanel.add(jButtonCopyAll);
		buttonsPanel.add(jButtonClose);

		setVisible(true);

		fill(tabbedPane, json, device, infoList);
	}

	private void fill(JTabbedPane tabbedPane, boolean json, ShellyAbstractDevice device, String[] infoList) {
		tabbedPane.removeAll();
		for(String info: infoList) {
			String name = info.replaceFirst("^/", "").replaceFirst("rpc/", "").replaceFirst("Shelly\\.", "");
			tabbedPane.add(name, getPanel(info, json, device));
		}
	}

	private JPanel getPanel(String info, boolean isJson, ShellyAbstractDevice device) {
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout(0, 0));
		JTextArea textArea = new JTextArea();
		textArea.setEditable(false);
		textArea.setForeground(Color.BLUE);
		final ObjectMapper mapper = new ObjectMapper();
		JsonNode storedVal;
		final boolean preview;
		if(isJson && device instanceof AbstractBatteryDevice && (storedVal = ((AbstractBatteryDevice)device).getStoredJSON(info)) != null) {
			String json;
			try {
				json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(storedVal);
				textArea.setText(json);
			} catch (JsonProcessingException e) {}
			preview = true;
		} else {
			textArea.setText(Main.LABELS.getString("lblLoading"));
			preview = false;
		}
		panel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		executor.schedule(() -> {
			//			panel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			try {
				if(Thread.interrupted() == false) {
					if(isJson) {
						// Retrive current data
						JsonNode val = device.getJSON(info);
						final String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(val);
						textArea.setForeground(Color.BLACK);
						textArea.setText(json);
						if(device instanceof AbstractBatteryDevice) {
							((AbstractBatteryDevice)device).setStoredJSON(info, val);
						}
					} else { // log
//						try (CloseableHttpClient httpClient = HttpClients.createDefault();
//								CloseableHttpResponse response = httpClient.execute(device.getHttpHost(), new HttpGet(info), device.getClientContext());
//								InputStream in = response.getEntity().getContent();
//								BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
//							textArea.setForeground(Color.BLACK);
//							textArea.setText("");
//							br.lines().forEach(l -> textArea.append(l + "\n"));
//						}
						try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
							httpClient.execute(device.getHttpHost(), new HttpGet(info), device.getClientContext(), response -> {
								try(InputStream in = response.getEntity().getContent(); BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
									textArea.setForeground(Color.BLACK);
									textArea.setText("");
									br.lines().forEach(l -> textArea.append(l + "\n"));
								}
								return null;
							});
						}
					}
					textArea.setCaretPosition(0);
				}
			} catch (java.io.FileNotFoundException e) {
				textArea.setText(Main.LABELS.getString("msgNotFound") + e.getMessage());
			} catch (Exception e) {
				if(Thread.interrupted() == false) {
					if(preview) {
						textArea.insert(e.getMessage() + "\n\n", 0);
					} else {
						textArea.setText(e.getMessage());
					}
				}
			}
			panel.setCursor(Cursor.getDefaultCursor());
		}, Devices.MULTI_QUERY_DELAY, TimeUnit.MILLISECONDS);
		JScrollPane scrollPane = new JScrollPane(textArea);
		panel.add(scrollPane);

//		final Action topAction = new UsnaAction(e -> {
//		scrollPane.getVerticalScrollBar().setValue(0);
//		}
//				);
//		final Action bottomAction = new UsnaAction(e -> 
//		scrollPane.getVerticalScrollBar().setValue(Integer.MAX_VALUE)
//				);
//		final Action downAction = new UsnaAction(e -> {
//			JScrollBar sb = scrollPane.getVerticalScrollBar();
//			sb.setValue(sb.getValue() + sb.getBlockIncrement());
//		});
//		textArea.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0), "top_act");
//		textArea.getActionMap().put("top_act", topAction);
//		textArea.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_END, 0), "bottom_act");
//		textArea.getActionMap().put("bottom_act", bottomAction);
//		textArea.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, SHORTCUT_KEY), "down_act");
//		textArea.getActionMap().put("down_act", downAction);
		return panel;
	}

	@Override
	public void dispose() {
		executor.shutdownNow();
		super.dispose();
	}
}