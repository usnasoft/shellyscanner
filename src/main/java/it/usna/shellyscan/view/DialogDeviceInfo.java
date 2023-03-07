package it.usna.shellyscan.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.BatteryDeviceInterface;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.ShellyAbstractDevice.Status;
import it.usna.shellyscan.model.device.g2.AbstractBatteryG2Device;
import it.usna.shellyscan.view.util.UsnaTextPane;
import it.usna.shellyscan.view.util.UtilCollecion;
import it.usna.swing.dialog.FindReplaceDialog;

public class DialogDeviceInfo extends JDialog {
	private static final long serialVersionUID = 1L;
	private final static Style DEF_STYLE = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
	private ScheduledExecutorService executor;

	public DialogDeviceInfo(final MainView owner, boolean json, ShellyAbstractDevice device, String[] infoList) {
		super(owner, false);
		setTitle(UtilCollecion.getExtendedHostName(device));
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		this.setSize(530, 650);
		setLocationRelativeTo(owner);
		
		// too many concurrent requests are dangerous (device reboot - G1) or cause websocket disconnection (G2)
		executor = Executors.newScheduledThreadPool(device instanceof AbstractBatteryG2Device ? 10 : 2);

		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		getContentPane().add(tabbedPane, BorderLayout.CENTER);

		JPanel buttonsPanel = new JPanel();
		getContentPane().add(buttonsPanel, BorderLayout.SOUTH);

		JButton btnRefresh = new JButton(new UsnaAction("labelRefresh", e -> {
			int s = tabbedPane.getSelectedIndex();
			fill(tabbedPane, json, device, infoList);
			tabbedPane.setSelectedIndex(s);
		}));

		final Supplier<JTextComponent> ta = () -> (JTextComponent) ((JScrollPane) ((JPanel) tabbedPane.getSelectedComponent()).getComponent(0)).getViewport().getView();
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
			if (cp != null && cp.length() > 0) {
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
		for (String info : infoList) {
			String name = info.replaceFirst("^/", "").replaceFirst("rpc/", "").replaceFirst("Shelly\\.", "");
			if (json) {
				tabbedPane.add(name, getJsonPanel(info, device));
			} else {
				tabbedPane.add(name, getPlainPanel(info, device));
			}
		}
	}

	private JPanel getJsonPanel(String info, ShellyAbstractDevice device) {
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout(0, 0));
		UsnaTextPane textArea = new UsnaTextPane();
		Style redStyle = textArea.addStyle("red", null);
		StyleConstants.setForeground(redStyle, Color.RED);
		StyleConstants.setBold(redStyle, true);
		textArea.setEditable(false);
		textArea.setForeground(Color.BLUE);
		textArea.addCaretListener(e -> markDelimiters(e.getDot(), textArea.getStyledDocument(), redStyle));
		
		final ObjectMapper mapper = new ObjectMapper();
		JsonNode storedVal;
		final boolean preview;
		if (device instanceof BatteryDeviceInterface && (storedVal = ((BatteryDeviceInterface) device).getStoredJSON(info)) != null) {
			try {
				String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(storedVal);
				textArea.setText(json, DEF_STYLE);
				textArea.setCaretPosition(0);
			} catch (JsonProcessingException e) {}
			preview = true;
		} else {
			textArea.setText(Main.LABELS.getString("lblLoading"));
			preview = false;
		}
		panel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		executor.schedule(() -> {
			try {
				if (Thread.interrupted() == false) {
					// Retrive current data
					JsonNode val = device.getJSON(info);
					final String json = val.isNull() ? "" : mapper.writerWithDefaultPrettyPrinter().writeValueAsString(val);
					textArea.setForeground(Color.BLACK);
					textArea.setText(json, DEF_STYLE);
					if (device instanceof BatteryDeviceInterface) {
						((BatteryDeviceInterface) device).setStoredJSON(info, val);
					}
//					textArea.setCaretPosition(0);
					markDelimiters(0, textArea.getStyledDocument(), redStyle);
				}
			} catch (Exception e) {
				if (Thread.interrupted() == false) {
					String msg;
					if (device.getStatus() == Status.OFF_LINE) {
						msg = "<" + Main.LABELS.getString("Status-OFFLINE") + ">";
					} else if (device.getStatus() == Status.NOT_LOOGGED) {
						msg = "<" + Main.LABELS.getString("Status-PROTECTED") + ">";
					} else {
						msg = e.getMessage();
					}
					if (preview) {
						textArea.insert(msg + "\n\n", 0);
					} else {
						textArea.setText(msg);
					}
				}
			}
			panel.setCursor(Cursor.getDefaultCursor());
		}, Devices.MULTI_QUERY_DELAY, TimeUnit.MILLISECONDS);
		JScrollPane scrollPane = new JScrollPane(textArea);
		panel.add(scrollPane);
		// final Action topAction = new UsnaAction(e -> {
		// scrollPane.getVerticalScrollBar().setValue(0);
		// }
		// );
		// final Action bottomAction = new UsnaAction(e ->
		// scrollPane.getVerticalScrollBar().setValue(Integer.MAX_VALUE)
		// );
		// final Action downAction = new UsnaAction(e -> {
		// JScrollBar sb = scrollPane.getVerticalScrollBar();
		// sb.setValue(sb.getValue() + sb.getBlockIncrement());
		// });
		// textArea.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME,
		// 0), "top_act");
		// textArea.getActionMap().put("top_act", topAction);
		// textArea.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_END,
		// 0), "bottom_act");
		// textArea.getActionMap().put("bottom_act", bottomAction);
		// textArea.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN,
		// SHORTCUT_KEY), "down_act");
		// textArea.getActionMap().put("down_act", downAction);
		return panel;
	}

	private JPanel getPlainPanel(String info, ShellyAbstractDevice device) {
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout(0, 0));
		JTextArea textArea = new JTextArea();
		textArea.setEditable(false);
		textArea.setForeground(Color.BLUE);
		textArea.setText(Main.LABELS.getString("lblLoading"));

		panel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		executor.schedule(() -> {
			try {
				if (Thread.interrupted() == false) {
					textArea.setForeground(Color.BLACK);
					String log = device.getAsString(info);
					textArea.setText(log);
					textArea.setCaretPosition(0);
				}
			} catch (Exception e) {
				if (Thread.interrupted() == false) {
					String msg;
					if (device.getStatus() == Status.OFF_LINE) {
						msg = "<" + Main.LABELS.getString("Status-OFFLINE") + ">";
					} else if (device.getStatus() == Status.NOT_LOOGGED) {
						msg = "<" + Main.LABELS.getString("Status-PROTECTED") + ">";
					} else {
						msg = e.getMessage();
					}
					textArea.setText(msg);
				}
			}
			panel.setCursor(Cursor.getDefaultCursor());
		}, Devices.MULTI_QUERY_DELAY, TimeUnit.MILLISECONDS);
		JScrollPane scrollPane = new JScrollPane(textArea);
		panel.add(scrollPane);
		return panel;
	}

	@Override
	public void dispose() {
		executor.shutdownNow();
		super.dispose();
	}
	
	private static Pattern DELIMITERS_PATTERN = Pattern.compile("(\\{\n)|(\\n\\s*\\})");
	private static void markDelimiters(final int pos, StyledDocument doc, Style style) {
		try {
			String str = doc.getText(0, doc.getLength());
			doc.setCharacterAttributes(0, str.length(), DEF_STYLE, true);
			List<Integer> start = new ArrayList<>();
			int index = 0;
			int startIndent = 0;
			Matcher matcher = DELIMITERS_PATTERN.matcher(str);
			while(matcher.find(index)) {
				index = matcher.end();
				if (str.charAt(index - 1) == '\n') { // "{\n"
						start.add(index - 1);
						if(index <= pos + 2) {
							startIndent = start.size();
						}
				} else /*if (str.charAt(index - 1) == '}')*/ { // "\\n\\s*\\}"
					int sPos = start.remove(start.size() - 1);
					if(index <= pos) {
						startIndent = start.size();
					} else if(start.size() == startIndent - 1 /*&& index > pos*/) {
						doc.setCharacterAttributes(sPos - 1, 1, style, true);
						doc.setCharacterAttributes(index - 1, 1, style, true);
						break;
					}
				}
			}
		} catch(/*BadLocation*/Exception e) {}
	}
}