package it.usna.shellyscan.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
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
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import it.usna.mvc.singlewindow.MainWindow;
import it.usna.shellyscan.Main;
import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.Devices.EventType;
import it.usna.shellyscan.model.device.BatteryDeviceInterface;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.ShellyAbstractDevice.Status;
import it.usna.shellyscan.model.device.g2.AbstractBatteryG2Device;
import it.usna.shellyscan.view.util.UsnaTextPane;
import it.usna.shellyscan.view.util.UtilMiscellaneous;
import it.usna.swing.dialog.FindReplaceDialog;
import it.usna.util.UsnaEventListener;

public class DialogDeviceInfo extends JDialog implements UsnaEventListener<Devices.EventType, Integer> {
	private static final long serialVersionUID = 1L;
	private final static Logger LOG = LoggerFactory.getLogger(MainWindow.class);
	private final static Style DEF_STYLE = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
	private final static Pattern DELIMITERS_PATTERN = Pattern.compile("(\\{\n)|(\\n\\s*\\})");
	private final Devices devicesModel;
	private final int deviceIndex;
	private final ScheduledExecutorService executor;
	
	private JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
	private boolean pleaseUpdate = false;

	public DialogDeviceInfo(final MainView owner, Devices devicesModel, int modelIndex) {
		super(owner, false);
		this.devicesModel = devicesModel;
		this.deviceIndex = modelIndex;
		ShellyAbstractDevice device = devicesModel.get(modelIndex);
		setTitle(UtilMiscellaneous.getExtendedHostName(device));
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		this.setSize(530, 650);
		setLocationRelativeTo(owner);
		
		// too many concurrent requests are dangerous (device reboot - G1) or cause websocket disconnection (G2)
		executor = Executors.newScheduledThreadPool(device instanceof AbstractBatteryG2Device ? 5 : 2);

		getContentPane().add(tabbedPane, BorderLayout.CENTER);

		JPanel buttonsPanel = new JPanel();
		getContentPane().add(buttonsPanel, BorderLayout.SOUTH);

		JButton btnRefresh = new JButton(new UsnaAction("labelRefresh", e -> {
			fill();
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
		fill();
		devicesModel.addListener(this);
	}

	private synchronized void fill() {
		ShellyAbstractDevice device = devicesModel.get(deviceIndex);
		try {
			int selected = tabbedPane.getSelectedIndex();
			tabbedPane.removeAll();
			for (String info : device.getInfoRequests()) {
				String name = info.replaceFirst("^/", "").replaceFirst("^rpc/", "").replaceFirst("^Shelly\\.", "");
				tabbedPane.add(name, getJsonPanel(info, device));
			}
			if(selected >= 0) {
				tabbedPane.setSelectedIndex(selected);
			}
		} catch(RuntimeException e) {
			LOG.error("", e);
		}
	}

	private JPanel getJsonPanel(String info, ShellyAbstractDevice device) {
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout(0, 0));
		UsnaTextPane textPane = new UsnaTextPane();
		Style redStyle = textPane.addStyle("red", null);
		StyleConstants.setForeground(redStyle, Color.RED);
		StyleConstants.setBold(redStyle, true);
		textPane.setEditable(false);
		textPane.setForeground(Color.BLUE);
		textPane.addCaretListener(e -> markDelimiters(e.getDot(), textPane.getStyledDocument(), redStyle));
		
		final ObjectWriter writer = new ObjectMapper().writerWithDefaultPrettyPrinter();
		JsonNode storedVal;
		final boolean preview;
		if (device instanceof BatteryDeviceInterface && (storedVal = ((BatteryDeviceInterface) device).getStoredJSON(info)) != null) {
			try {
				String json = writer.writeValueAsString(storedVal);
				textPane.setText(json, DEF_STYLE);
				textPane.setCaretPosition(0);
			} catch (JsonProcessingException e) {}
			preview = true;
		} else {
			textPane.setText(Main.LABELS.getString("lblLoading"));
			preview = false;
		}
		panel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		executor.schedule(() -> {
			try {
				if (Thread.interrupted() == false) {
					// Retrive current data
					JsonNode val = device.getJSON(info);
					final String json = val.isNull() ? "" : writer.writeValueAsString(val);
					textPane.setForeground(Color.BLACK);
					textPane.setText(json, DEF_STYLE);
					if (device instanceof BatteryDeviceInterface) {
						((BatteryDeviceInterface) device).setStoredJSON(info, val);
					}
//					textPane.setCaretPosition(0);
					markDelimiters(0, textPane.getStyledDocument(), redStyle);
				}
			} catch (Exception e) {
				if (Thread.interrupted() == false) {
					String msg;
					if (device.getStatus() == Status.OFF_LINE || device.getStatus() == Status.GHOST) {
						msg = "<" + Main.LABELS.getString("Status-OFFLINE") + ">";
						pleaseUpdate = true;
					} else if (device.getStatus() == Status.NOT_LOOGGED) {
						msg = "<" + Main.LABELS.getString("Status-PROTECTED") + ">";
						pleaseUpdate = true;
					} else {
						msg = e.getMessage();
					}
					if (preview) {
						textPane.insert(msg + "\n\n", 0);
					} else {
						textPane.setText(msg);
					}
				}
			}
			panel.setCursor(Cursor.getDefaultCursor());
		}, Devices.MULTI_QUERY_DELAY, TimeUnit.MILLISECONDS);
		JScrollPane scrollPane = new JScrollPane(textPane);
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

	@Override
	public void dispose() {
		devicesModel.removeListener(this);
		executor.shutdownNow();
		super.dispose();
	}
	
	private static void markDelimiters(final int pos, StyledDocument doc, Style style) {
		try {
			String str = doc.getText(0, doc.getLength());
			doc.setCharacterAttributes(0, str.length(), DEF_STYLE, true);
			ArrayList<Integer> start = new ArrayList<>();
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
	
	@Override
	public void update(EventType mesgType, Integer msgBody) {
		SwingUtilities.invokeLater(() -> {
			if(pleaseUpdate && msgBody != null && msgBody == deviceIndex && ((ScheduledThreadPoolExecutor)executor).getQueue().size() == 0 && (
					(mesgType == EventType.SUBSTITUTE) ||  (mesgType == EventType.UPDATE && devicesModel.get(deviceIndex).getStatus() == Status.ON_LINE)) ){
				pleaseUpdate = false;
				try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e) {}
				fill();
			} else if(mesgType == Devices.EventType.CLEAR) {
				dispose();
			}
		});
	}
}