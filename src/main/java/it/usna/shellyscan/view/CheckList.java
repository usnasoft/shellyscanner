package it.usna.shellyscan.view;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.controller.UsnaSelectedAction;
import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.Devices.EventType;
import it.usna.shellyscan.model.device.BatteryDeviceInterface;
import it.usna.shellyscan.model.device.InetAddressAndPort;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.ShellyAbstractDevice.LogMode;
import it.usna.shellyscan.model.device.ShellyAbstractDevice.Status;
import it.usna.shellyscan.model.device.blu.AbstractBluDevice;
import it.usna.shellyscan.model.device.blu.BluInetAddressAndPort;
import it.usna.shellyscan.model.device.g1.AbstractG1Device;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.g2.RangeExtenderManager;
import it.usna.shellyscan.model.device.g2.modules.WIFIManagerG2;
import it.usna.shellyscan.view.util.Msg;
import it.usna.shellyscan.view.util.ScannerProperties;
import it.usna.shellyscan.view.util.ScannerProperties.PropertyEvent;
import it.usna.shellyscan.view.util.UtilMiscellaneous;
import it.usna.swing.UsnaPopupMenu;
import it.usna.swing.table.ExTooltipTable;
import it.usna.swing.table.UsnaTableModel;
import it.usna.swing.texteditor.TextDocumentListener;
import it.usna.util.UsnaEventListener;

public class CheckList extends JDialog implements UsnaEventListener<Devices.EventType, Integer>, ScannerProperties.AppPropertyListener {
	private static final long serialVersionUID = 1L;
	private final static Logger LOG = LoggerFactory.getLogger(AbstractG1Device.class);
	private final static String TRUE = LABELS.getString("true_yn");
	private final static String FALSE = LABELS.getString("false_yn");
	private final static int COL_STATUS = 0;
	private final static int COL_NAME = 1;
	private final static int COL_IP = 2;
	private final static int COL_ECO = 3;
	private final static int COL_LED = 4;
	private final static int COL_LOGS = 5;
	private final static int COL_BLE = 6;
	private final static int COL_AP = 7;
	private final static int COL_EXTENDER = 11;

	private final ScannerProperties properties = ScannerProperties.get();
	private final Devices appModel;
	private final int[] devicesInd;
	private final JToolBar toolBar = new JToolBar();
	private final ExTooltipTable table;
	private final UsnaTableModel tModel;
	private ScheduledExecutorService exeService /* = Executors.newFixedThreadPool(20) */;

	public CheckList(final Window owner, Devices appModel, int[] devicesInd, final SortOrder ipSort) {
		super(owner, LABELS.getString("dlgChecklistTitle"));
		this.appModel = appModel;
		this.devicesInd = devicesInd;

		BorderLayout borderLayout = (BorderLayout) getContentPane().getLayout();
		borderLayout.setVgap(2);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		tModel = new UsnaTableModel("",
				LABELS.getString("col_device"), LABELS.getString("col_ip"), LABELS.getString("col_eco"), LABELS.getString("col_ledoff"), LABELS.getString("col_logs"), LABELS.getString("col_blt"),
				LABELS.getString("col_AP"), LABELS.getString("col_roaming"), LABELS.getString("col_wifi1"), LABELS.getString("col_wifi2"), LABELS.getString("col_extender")) {
			private static final long serialVersionUID = 1L;

			@Override
			public Class<?> getColumnClass(final int c) {
				return (c == COL_IP) ? InetAddressAndPort.class : super.getColumnClass(c);
			}
		};

		table = new ExTooltipTable(tModel, true) {
			private static final long serialVersionUID = 1L;
			{
				columnModel.getColumn(COL_STATUS).setMaxWidth(DevicesTable.ONLINE_BULLET.getIconWidth() + 4);
				setHeadersTooltip(LABELS.getString("col_status_exp"), null, null, LABELS.getString("col_eco_tooltip"), LABELS.getString("col_ledoff_tooltip"), LABELS.getString("col_logs_tooltip"), LABELS.getString("col_blt_tooltip"),
						LABELS.getString("col_AP_tooltip"), LABELS.getString("col_roaming_tooltip"), LABELS.getString("col_wifi1_tooltip"), LABELS.getString("col_wifi2_tooltip"), LABELS.getString("col_extender_tooltip"));

				TableCellRenderer rendTrueOk = new CheckRenderer(true);
				TableCellRenderer rendFalseOk = new CheckRenderer(false);
				columnModel.getColumn(COL_IP).setCellRenderer(new InetAddressAndPortRenderer());
				columnModel.getColumn(COL_ECO).setCellRenderer(rendTrueOk);
				columnModel.getColumn(COL_LED).setCellRenderer(rendTrueOk);
				columnModel.getColumn(COL_LOGS).setCellRenderer(rendFalseOk);
				columnModel.getColumn(COL_BLE).setCellRenderer(rendFalseOk);
				columnModel.getColumn(COL_AP).setCellRenderer(rendFalseOk);
				columnModel.getColumn(8).setCellRenderer(rendFalseOk); // roaming
				columnModel.getColumn(9).setCellRenderer(rendTrueOk); // wifi1 null -> "-"
				columnModel.getColumn(10).setCellRenderer(rendTrueOk); // wifi2 null -> "-"
				columnModel.getColumn(COL_EXTENDER).setCellRenderer(rendTrueOk); // extender null -> "-"

				((TableRowSorter<?>) getRowSorter()).setSortsOnUpdates(true);

				if (ipSort != SortOrder.UNSORTED) {
					sortByColumn(COL_IP, ipSort);
				}
			}
		};

		Action ecoModeAction = new UsnaSelectedAction(this, table, "setEcoMode_action", "setEcoMode_action_tooletip", null, "/images/leaf24.png", localRow -> {
			Boolean eco = (Boolean) tModel.getValueAt(localRow, COL_ECO);
			ShellyAbstractDevice d = getLocalDevice(localRow);
			d.setEcoMode(!eco);
			try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e1) {}
			updateRow(d, localRow);
		});

		Action ledAction = new UsnaSelectedAction(this, table, "setLED_action", "setLED_action_tooletip", null, "/images/Light24.png", localRow -> { // AbstractG1Device
			Boolean led = (Boolean) tModel.getValueAt(localRow, COL_LED);
			AbstractG1Device d = (AbstractG1Device) getLocalDevice(localRow);
			d.setLEDMode(!led);
			try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e1) {}
			updateRow(d, localRow);
		});

		Action logsAction = new UsnaSelectedAction(this, table, "setLogs_action", "setLogs_action_tooletip", null, "/images/Document2_24.png", localRow -> { // AbstractG1Device
			Boolean logs = (Boolean) tModel.getValueAt(localRow, COL_LOGS);
			AbstractG1Device d = (AbstractG1Device) getLocalDevice(localRow);
			d.setDebugMode(logs ? LogMode.NO : LogMode.FILE);
			try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e1) {}
			updateRow(d, localRow);
		});

		Action bleAction = new UsnaSelectedAction(this, table, "setBLE_action", "setBLE_action_tooletip", null, "/images/Bluetooth24.png", localRow -> { // AbstractG2Device
			Boolean ble = (Boolean) tModel.getValueAt(localRow, COL_BLE);
			AbstractG2Device d = (AbstractG2Device) getLocalDevice(localRow);
			d.setBLEMode(!ble);
			try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e1) {}
			updateRow(d, localRow);
		});

		Action apModeAction = new UsnaSelectedAction(this, table, "setAPMode_action", "setAPMode_action_tooletip", null, "/images/Rss24.png", localRow -> { // AbstractG2Device
			Object ap = tModel.getValueAt(localRow, COL_AP);
			AbstractG2Device d = (AbstractG2Device) getLocalDevice(localRow);
			WIFIManagerG2.enableAP(d, !((ap instanceof Boolean && ap == Boolean.TRUE) || (ap instanceof String && TRUE.equals(ap))));
			try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e1) {}
			updateRow(d, localRow);
		});

		Action rangeExtenderAction = new UsnaSelectedAction(this, table, "setExtender_action", "setExtender_action_tooletip", null, "/images/Extender24.png", localRow -> { // AbstractG2Device
			Object ext = tModel.getValueAt(localRow, COL_EXTENDER);
			AbstractG2Device d = (AbstractG2Device) getLocalDevice(localRow);
			RangeExtenderManager.enable(d, "-".equals(ext));
			try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e1) {}
			updateRow(d, localRow);
		});

		Action rebootAction = new UsnaSelectedAction(this, table, "action_reboot_name", "action_reboot_tooltip", null, "/images/Nuke24.png",
				() -> {
					final String cancel = UIManager.getString("OptionPane.cancelButtonText");
					return JOptionPane.showOptionDialog(
							CheckList.this, LABELS.getString("action_reboot_confirm"), LABELS.getString("action_reboot_tooltip"),
							JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
							new Object[] {LABELS.getString("action_reboot_name"), cancel}, cancel) == 0;},
				modelRow -> {
					ShellyAbstractDevice d = getLocalDevice(modelRow);
					d.setStatus(Status.READING);
					tModel.setValueAt(DevicesTable.UPDATING_BULLET, modelRow, DevicesTable.COL_STATUS_IDX);
					SwingUtilities.invokeLater(() -> appModel.reboot(devicesInd[modelRow]));
				});

		Action browseAction = new UsnaSelectedAction(this, table, "action_web_name", "action_web_tooltip", null, "/images/Computer24.png", i -> {
			try {
				Desktop.getDesktop().browse(URI.create("http://" + getLocalDevice(i).getAddressAndPort().toString()));
			} catch (IOException ex) {
				Msg.errorMsg(ex);
			}
		});
		
		UsnaAction refreshAction = new UsnaAction(this, "labelRefresh", "labelRefresh", null, "/images/Refresh24.png");
		refreshAction.setActionListener(i -> {
			refreshAction.setEnabled(false);
			tModel.clear();
			fill();
			exeService.schedule(() -> refreshAction.setEnabled(true), 600, TimeUnit.MILLISECONDS);
		});
		
		Action helpAction = new UsnaAction(this, "helpBtnTooltip", "helpBtnTooltip", null, "/images/Question24.png", i -> {
			try {
				Desktop.getDesktop().browse(URI.create(LABELS.getString("dlgChecklistManualUrl")));
			} catch (IOException | UnsupportedOperationException ex) {
				Msg.errorMsg(this, ex);
			}
		});

		fill();

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportView(table);
		getContentPane().add(scrollPane, BorderLayout.CENTER);

		table.setRowHeight(table.getRowHeight() + 3);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		table.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent evt) {
				if (evt.getClickCount() == 2 /*&& table.getSelectedRow() != -1*/) {
					browseAction.actionPerformed(null);
				}
			}
		});

		ListSelectionListener selListener = e -> {
			if(e.getValueIsAdjusting() == false) {
				int modelRow = table.getSelectedModelRow();
				if (modelRow >= 0) {
					ShellyAbstractDevice d = getLocalDevice(modelRow);
					Object eco = tModel.getValueAt(modelRow, COL_ECO);
					ecoModeAction.setEnabled(eco instanceof Boolean);
					Object led = tModel.getValueAt(modelRow, COL_LED);
					ledAction.setEnabled(led instanceof Boolean);
					Object logs = tModel.getValueAt(modelRow, COL_LOGS);
					logsAction.setEnabled(logs instanceof Boolean && d instanceof AbstractG1Device);
					Object ble = tModel.getValueAt(modelRow, COL_BLE);
					bleAction.setEnabled(ble instanceof Boolean);
					apModeAction.setEnabled(d instanceof AbstractG2Device);
					rangeExtenderAction.setEnabled(d instanceof AbstractG2Device);
					browseAction.setEnabled(true);
					rebootAction.setEnabled(true);
				} else {
					ecoModeAction.setEnabled(false);
					ledAction.setEnabled(false);
					logsAction.setEnabled(false);
					bleAction.setEnabled(false);
					apModeAction.setEnabled(false);
					rangeExtenderAction.setEnabled(false);
					browseAction.setEnabled(false);
					rebootAction.setEnabled(false);
				}
			}
		};
		table.getSelectionModel().addListSelectionListener(selListener);
		selListener.valueChanged(new ListSelectionEvent(table.getSelectionModel(), -1, -1, false));

		UsnaPopupMenu tablePopup = new UsnaPopupMenu(ecoModeAction, ledAction, logsAction, bleAction, apModeAction, rangeExtenderAction, null, browseAction, rebootAction) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doPopup(MouseEvent evt) {
				final int r = table.rowAtPoint(evt.getPoint());
				if (r >= 0) {
					table.setRowSelectionInterval(r, r);
					show(table, evt.getX(), evt.getY());
				}
			}
		};
		table.addMouseListener(tablePopup.getMouseListener());
		
		toolBar.setBorder(BorderFactory.createEmptyBorder());
		toolBar.add(refreshAction);
		toolBar.addSeparator();
		toolBar.add(ecoModeAction);
		toolBar.add(ledAction);
		toolBar.add(logsAction);
		toolBar.add(bleAction);
		toolBar.add(apModeAction);
		toolBar.add(rangeExtenderAction);
		toolBar.addSeparator();
		toolBar.add(browseAction);
		toolBar.add(rebootAction);
		toolBar.add(Box.createHorizontalGlue());
		toolBar.add(helpAction);
		updateHideCaptions();
		getContentPane().add(toolBar, BorderLayout.NORTH);

		JPanel panelBottom = new JPanel(new BorderLayout(0, 0));
		getContentPane().add(panelBottom, BorderLayout.SOUTH);

		// Find panel
		JPanel panelRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
		panelBottom.add(panelRight, BorderLayout.EAST);

		panelRight.add(new JLabel(LABELS.getString("lblFilter")));
		JTextField textFieldFilter = new JTextField();
		textFieldFilter.setColumns(20);
		textFieldFilter.setBorder(BorderFactory.createEmptyBorder(2, 1, 2, 1));
		panelRight.add(textFieldFilter);
		textFieldFilter.getDocument().addDocumentListener((TextDocumentListener)e -> {
			final int[] cols = new int[] { COL_NAME, COL_IP };
			TableRowSorter<?> sorter = (TableRowSorter<?>) table.getRowSorter();
			String filter = textFieldFilter.getText();
			if (filter.length() > 0) {
				filter = filter.replace("\\E", "\\e");
				sorter.setRowFilter(RowFilter.regexFilter("(?i).*\\Q" + filter + "\\E.*", cols));
			} else {
				sorter.setRowFilter(null);
			}
		});

		textFieldFilter.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F, MainView.SHORTCUT_KEY), "find_focus_mw");
		textFieldFilter.getActionMap().put("find_focus_mw", new UsnaAction(e -> textFieldFilter.requestFocus()));

		final Action eraseFilterAction = new UsnaAction(this, null, "/images/erase-9-16.png", e -> {
			textFieldFilter.setText("");
			textFieldFilter.requestFocusInWindow();
			table.clearSelection();
		});

		JButton eraseFilterButton = new JButton(eraseFilterAction);
		eraseFilterButton.setBorder(BorderFactory.createEmptyBorder(1, 2, 1, 2));
		eraseFilterButton.setContentAreaFilled(false);
		eraseFilterButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_E, MainView.SHORTCUT_KEY), "find_erase");
		eraseFilterButton.getActionMap().put("find_erase", eraseFilterAction);
		panelRight.add(eraseFilterButton);
		panelRight.add(Box.createHorizontalStrut(12));

		JButton btnClose = new JButton(new UsnaAction("dlgClose", e -> dispose()));
		panelRight.add(btnClose);

		setSize(900, 490);
		setVisible(true);
		setLocationRelativeTo(owner);
		table.columnsWidthAdapt();
		appModel.addListener(this);
		properties.addListener(this);
	}

	@Override
	public void dispose() {
		properties.removeListener(this);
		appModel.removeListener(this);
		exeService.shutdownNow();
		super.dispose();
	}

	private void fill() {
		if(exeService != null && exeService.isShutdown() == false) {
			exeService.shutdownNow();
		}
		exeService = Executors.newScheduledThreadPool(20);
		for (int devicesInd : devicesInd) {
			ShellyAbstractDevice d = appModel.get(devicesInd);
			final int row = tModel.addRow(DevicesTable.UPDATING_BULLET, UtilMiscellaneous.getExtendedHostName(d), d.getAddressAndPort());
			updateRow(d, row);
		}
	}
	
	private void updateHideCaptions() {
		boolean en = properties.getBoolProperty(ScannerProperties.PROP_TOOLBAR_CAPTIONS, true) == false;
		Stream.of(toolBar.getComponents()).filter(c -> c instanceof AbstractButton).forEach(b -> ((AbstractButton)b).setHideActionText(en));
	}

	private void updateRow(ShellyAbstractDevice d, int row) {
		exeService.execute(() -> {
			try {
				if (d instanceof AbstractG1Device) {
					tModel.setRow(row, g1Row(d, d.getJSON("/settings")));
				} else if (d instanceof AbstractG2Device) { // G2-G3
					tModel.setRow(row, g2Row(d, d.getJSON("/rpc/Shelly.GetConfig"), d.getJSON("/rpc/Shelly.GetStatus")));
				} else if (d instanceof AbstractBluDevice blu) { // G2-G3
					tModel.setRow(row, bluRow(blu));
				}
			} catch (/* IO */Exception e) {
				if (d instanceof BatteryDeviceInterface) {
					if (d instanceof AbstractG1Device) {
						tModel.setRow(row, g1Row(d, ((BatteryDeviceInterface) d).getStoredJSON("/settings")));
					} else {
						tModel.setRow(row, g2Row(d, ((BatteryDeviceInterface) d).getStoredJSON("/rpc/Shelly.GetConfig"), null));
					}
				} else {
					tModel.setRow(row, DevicesTable.getStatusIcon(d), UtilMiscellaneous.getExtendedHostName(d), d.getAddressAndPort());
				}
				if (d.getStatus() == Status.OFF_LINE || d.getStatus() == Status.NOT_LOOGGED) {
					// LOG.debug("{}", d, e);
				} else {
					LOG.error("{}", d, e);
				}
			}
			table.columnsWidthAdapt();
		});
	}

	private static Object[] g1Row(ShellyAbstractDevice d, JsonNode settings) {
		Boolean eco = boolVal(settings.path("eco_mode_enabled"));
		Boolean ledOff = boolVal(settings.path("led_status_disable"));
		Object debug = d.getDebugMode() == LogMode.UNDEFINED ? "-" : d.getDebugMode() != LogMode.NO;
		String roaming;
		if (settings.path("ap_roaming").isMissingNode()) {
			roaming = "-";
		} else if (settings.at("/ap_roaming/enabled").asBoolean()) {
			roaming = settings.at("/ap_roaming/threshold").asText();
		} else {
			roaming = FALSE;
		}
		String wifi1;
		if (settings.at("/wifi_sta/enabled").asBoolean()) {
			wifi1 = "static".equals(settings.at("/wifi_sta/ipv4_method").asText()) ? TRUE : FALSE;
		} else {
			wifi1 = "-";
		}
		String wifi2;
		if (settings.at("/wifi_sta1/enabled").asBoolean()) {
			wifi2 = "static".equals(settings.at("/wifi_sta1/ipv4_method").asText()) ? TRUE : FALSE;
		} else {
			wifi2 = "-";
		}
		return new Object[] { DevicesTable.getStatusIcon(d), UtilMiscellaneous.getExtendedHostName(d), d.getAddressAndPort(), eco, ledOff, debug, "-", "-", roaming, wifi1, wifi2, "-" };
	}

	private static Object[] g2Row(ShellyAbstractDevice d, JsonNode settings, JsonNode status) {
		Boolean eco = boolVal(settings.at("/sys/device/eco_mode"));
		Object ap = boolVal(settings.at("/wifi/ap/enable"));
		if (ap != null && ap == Boolean.TRUE && settings.at("/wifi/ap/is_open").asBoolean(true) == false) {
			ap = TRUE; // AP active but protected with pwd
		}
		ArrayList<LogMode> logModes = new ArrayList<>();
		if(settings.at("/sys/debug/websocket/enable").booleanValue()) {
			logModes.add(LogMode.SOCKET);
		}
		if(settings.at("/sys/debug/mqtt/enable").booleanValue()) {
			logModes.add(LogMode.MQTT);
		}
		if(settings.at("/sys/debug/udp").isMissingNode() == false && settings.at("/sys/debug/udp/addr").isNull() == false) {
			logModes.add(LogMode.UDP);
		}
		Object debug = (logModes.size() == 0) ? Boolean.FALSE : logModes.stream().map(log -> LABELS.getString("debug" + log.name())).collect(Collectors.joining(", "));
		Object ble = boolVal(settings.at("/ble/enable"));
		if (ble == Boolean.TRUE && boolVal(settings.at("/ble/observer/enable")) == Boolean.TRUE) {
			ble = "OBS"; // used as observer
		}
		String roaming;
		if (settings.at("/wifi/roam").isMissingNode()) {
			roaming = "-";
		} else if (settings.at("/wifi/roam/interval").asInt() > 0) {
			roaming = settings.at("/wifi/roam/rssi_thr").asText();
		} else {
			roaming = FALSE;
		}
		String wifi1;
		if (settings.at("/wifi/sta/enable").asBoolean()) {
			wifi1 = "static".equals(settings.at("/wifi/sta/ipv4mode").asText()) ? TRUE : FALSE;
		} else {
			wifi1 = "-";
		}
		String wifi2;
		if (settings.at("/wifi/sta1/enable").asBoolean()) {
			wifi2 = "static".equals(settings.at("/wifi/sta1/ipv4mode").asText()) ? TRUE : FALSE;
		} else {
			wifi2 = "-";
		}
		JsonNode extClient;
		String extender = (status == null || (extClient = status.at("/wifi/ap_client_count")).isMissingNode()) ? "-" : extClient.asInt() + "";
		return new Object[] { DevicesTable.getStatusIcon(d), UtilMiscellaneous.getExtendedHostName(d), d.getAddressAndPort(), eco, "-", debug, ble, ap, roaming, wifi1, wifi2, extender };
	}
	
	private static Object[] bluRow(AbstractBluDevice d) {
//		String parents = d.getAddressAndPort().toString() +
//		d.getAlternativeParents().stream().map(InetAddressAndPort::toString).collect(Collectors.joining("/"));
		
		return new Object[] { DevicesTable.getStatusIcon(d), UtilMiscellaneous.getExtendedHostName(d), d.getAddressAndPort() };
	}

	private static Boolean boolVal(JsonNode node) {
		return node.isMissingNode() ? null : node.asBoolean();
	}

	private static class CheckRenderer extends DefaultTableCellRenderer {
		private static final long serialVersionUID = 1L;
		private final boolean goodVal;

		private CheckRenderer(boolean goodVal) {
			this.goodVal = goodVal;
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			Component ret;
			if (value instanceof Boolean val) {
				if (val) {
					ret = super.getTableCellRendererComponent(table, TRUE, isSelected, hasFocus, row, column);
					ret.setForeground(goodVal ? Color.green : Color.red);
				} else {
					ret = super.getTableCellRendererComponent(table, FALSE, isSelected, hasFocus, row, column);
					ret.setForeground(goodVal ? Color.red : Color.green);
				}
				if (isSelected) {
					ret.setFont(ret.getFont().deriveFont(Font.BOLD));
				}
			} else {
				ret = super.getTableCellRendererComponent(table, value == null ? "-" : value, isSelected, hasFocus, row, column);
				if (isSelected == false) {
					ret.setForeground(table.getForeground());
				}
			}
			return ret;
		}
	}
	
	private static class InetAddressAndPortRenderer extends DefaultTableCellRenderer {
		private static final long serialVersionUID = 1L;

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			JLabel ret = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			List<InetAddressAndPort> parents;
			if(value instanceof BluInetAddressAndPort bluAddr && (parents = bluAddr.getAlternativeParents()).size() > 0) {
				ret.setText(bluAddr.getRepresentation() + parents.stream().map(InetAddressAndPort::toString).collect(Collectors.joining(" \\ ", " \\ ", "")));
				ret.setForeground(Color.red);
				if (isSelected) {
					ret.setFont(ret.getFont().deriveFont(Font.BOLD));
				};
			} else if (isSelected == false) {
				ret.setForeground(table.getForeground());
			}
			return ret;
		}
	}

	private ShellyAbstractDevice getLocalDevice(int ind) {
		return appModel.get(devicesInd[ind]);
	}

	private int getLocalIndex(int ind) {
		for (int i = 0; i < devicesInd.length; i++) {
			if (devicesInd[i] == ind) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public void update(EventType mesgType, Integer pos) {
		if (mesgType == Devices.EventType.CLEAR) {
			SwingUtilities.invokeLater(() -> dispose()); // devicesInd changes
		} else if (mesgType == Devices.EventType.UPDATE) {
			try {
				final int index = getLocalIndex(pos);
				if (index >= 0 /*&& tModel.getValueAt(index, COL_STATUS) !=  DevicesTable.UPDATING_BULLET*/) {
					tModel.setValueAt(DevicesTable.getStatusIcon(appModel.get(pos)), index, COL_STATUS);
				}
			} catch (RuntimeException e) { } // on "refresh" table row could non exists
		}
	}

	@Override
	public void update(PropertyEvent e, String propKey) {
		if(ScannerProperties.PROP_TOOLBAR_CAPTIONS.equals(propKey)) {
			updateHideCaptions();
		}
	}
} // 534 - 560

// g1 "factory_reset_from_switch" : true, "pon_wifi_reset" : false,