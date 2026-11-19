package it.usna.shellyscan.view.devsettings;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumn;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.exceptions.WebSocketTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.usna.shellyscan.controller.DeferrableTask;
import it.usna.shellyscan.controller.DeferrablesContainer;
import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.controller.UsnaOpenUrlAction;
import it.usna.shellyscan.controller.UsnaSelectedAction;
import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.Devices.EventType;
import it.usna.shellyscan.model.device.GhostDevice;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.ShellyAbstractDevice.Status;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.g2.WebSocketDeviceListener;
import it.usna.shellyscan.model.device.g2.modules.FirmwareManagerG2;
import it.usna.shellyscan.model.device.modules.FirmwareManager;
import it.usna.shellyscan.view.DevicesTable;
import it.usna.shellyscan.view.TableFilterPanel;
import it.usna.shellyscan.view.util.UtilMiscellaneous;
import it.usna.swing.UsnaPopupMenu;
import it.usna.swing.table.UsnaTableModel;
import it.usna.util.UsnaEventListener;
import tools.jackson.databind.JsonNode;

public class PanelAlternativeFW extends AbstractSettingsPanel implements UsnaEventListener<Devices.EventType, Integer> {
	private static final long serialVersionUID = 1L;
	private FWAlternativeTable table;
	private UsnaTableModel tModel = new UsnaTableModel("", LABELS.getString("col_device"), LABELS.getString("alternateDesciption"), LABELS.getString("dlgSetColLastV"), LABELS.getString("dlgSetColBetaV")) {
		private static final long serialVersionUID = 1L;
		@Override
		public Class<?> getColumnClass(final int c) { // Booolean is comparable; see TableStringConverter
			return (c == FWAlternativeTable.COL_STABLE  || c == FWAlternativeTable.COL_BETA) ? Object.class : super.getColumnClass(c);
		}
	};
	private List<DeviceFirmware> devicesFWData;

	private JLabel lblCount = new JLabel();
	private JButton btnCheck;

	private ScheduledExecutorService exeService = Executors.newScheduledThreadPool(35);
	private List<Future<Void>> retriveFutures;

	private static final Logger LOG = LoggerFactory.getLogger(PanelAlternativeFW.class);

	public PanelAlternativeFW(DialogDeviceSettingsMain parent) {
		super(parent);
		setLayout(new BorderLayout(0, 0));
		
		table = new FWAlternativeTable(tModel, this);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 3, 0));
		scrollPane.setViewportView(table);
		add(scrollPane, BorderLayout.CENTER);

		JPanel btnPanel = new JPanel(new BorderLayout());
		JPanel btnPanelLeft = new JPanel(new FlowLayout(FlowLayout.CENTER, 1, 0));
		btnPanel.add(btnPanelLeft, BorderLayout.WEST);
		JPanel btnPanelRight = new JPanel(new FlowLayout(FlowLayout.CENTER, 1, 0));
		btnPanel.add(btnPanelRight, BorderLayout.EAST);
		add(btnPanel, BorderLayout.SOUTH);

		JButton btnUnselectAll = new JButton(new UsnaAction("btn_unselectAll", event -> {
			IntStream sel = (table.getSelectedRowCount() > 1) ? table.getSelectedModelRowsStream() : IntStream.range(0, tModel.getRowCount());
			sel.forEach(i -> {
				if(tModel.getValueAt(i, FWAlternativeTable.COL_STABLE) instanceof Boolean) {
					tModel.setValueAt(Boolean.FALSE, i, FWAlternativeTable.COL_STABLE);
				}
				if(tModel.getValueAt(i, FWAlternativeTable.COL_BETA) instanceof Boolean) {
					tModel.setValueAt(Boolean.FALSE, i, FWAlternativeTable.COL_BETA);
				}
			});
			countSelection();
		}));
		btnUnselectAll.setBorder(BorderFactory.createEmptyBorder(4, 7, 4, 7));
		btnPanelLeft.add(btnUnselectAll);
		
		JButton btnSelectStable = new JButton(new UsnaAction("btn_selectAllSta", event -> {
			IntStream sel = (table.getSelectedRowCount() > 1) ? table.getSelectedModelRowsStream() : IntStream.range(0, tModel.getRowCount());
			sel.forEach(i -> {
				if(tModel.getValueAt(i, FWAlternativeTable.COL_STABLE) instanceof Boolean) {
					tModel.setValueAt(Boolean.TRUE, i, FWAlternativeTable.COL_STABLE);
					if(tModel.getValueAt(i, FWAlternativeTable.COL_BETA) instanceof Boolean) {
						tModel.setValueAt(Boolean.FALSE, i, FWAlternativeTable.COL_BETA);
					}
				}
			});
			countSelection();
		}));
		btnSelectStable.setBorder(BorderFactory.createEmptyBorder(4, 7, 4, 7));
		btnPanelLeft.add(btnSelectStable);

		JButton btnSelectBeta = new JButton(new UsnaAction("btn_selectAllbeta", event -> {
			IntStream sel = (table.getSelectedRowCount() > 1) ? table.getSelectedModelRowsStream() : IntStream.range(0, tModel.getRowCount());
			sel.forEach(i -> {
				if(tModel.getValueAt(i, FWAlternativeTable.COL_BETA) instanceof Boolean) {
					tModel.setValueAt(Boolean.TRUE, i, FWAlternativeTable.COL_BETA);
					if(tModel.getValueAt(i, FWAlternativeTable.COL_STABLE) instanceof Boolean) {
						tModel.setValueAt(Boolean.FALSE, i, FWAlternativeTable.COL_STABLE);
					}
				}
			});
			countSelection();
		}));
		btnSelectBeta.setBorder(BorderFactory.createEmptyBorder(4, 7, 4, 7));
		btnPanelLeft.add(btnSelectBeta);

		btnPanelLeft.add(Box.createHorizontalStrut(6));
		btnPanelLeft.add(lblCount);
		
		TableFilterPanel panelFind = new TableFilterPanel(12, false, filter -> {
			table.setRowFilter(filter);
			countSelection();
		});
		btnPanelRight.add(panelFind);
		
		btnCheck = new JButton(new UsnaAction("btn_check", event -> {
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			btnCheck.setEnabled(false);
			int i;
			for(i = 0; i < devicesFWData.size(); i++) {
				FirmwareManager fwModule = devicesFWData.get(i).fwModule;
				if(fwModule != null && tModel.getValueAt(i, FWAlternativeTable.COL_STATUS) != DevicesTable.UPDATING_BULLET) {
					tModel.setValueAt(DevicesTable.UPDATING_BULLET, i, FWAlternativeTable.COL_STATUS);
					final int row = i;
					exeService.schedule(() -> {
						fwModule.chech();
						tModel.setRow(row, createTableRow(row));
						countSelection();
					}, i , TimeUnit.MILLISECONDS);
				}
			}
			exeService.schedule(() -> {
				btnCheck.setEnabled(true);
				setCursor(Cursor.getDefaultCursor());
			}, i + 1000, TimeUnit.MILLISECONDS);
		}));
		btnCheck.setBorder(BorderFactory.createEmptyBorder(4, 7, 4, 7));
		btnPanelRight.add(Box.createHorizontalStrut(3));
		btnPanelRight.add(btnCheck);

		Action browseAction = new UsnaSelectedAction(parentDlg, table, "action_web_name", null, "/images/Computer16.png", null, () ->
		table.getSelectedRowCount() <= 8 ||
		JOptionPane.showConfirmDialog(parentDlg, LABELS.getString("action_web_confirm"), LABELS.getString("action_web_name"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION,
		i -> UsnaOpenUrlAction.openUrl(this, "http://" + parentDlg.getLocalDevice(i).getAddressAndPort().getRepresentation()));

		Action nosortAction = new UsnaAction("lblNoSort", e -> table.clearSort());

		UsnaPopupMenu popup = new UsnaPopupMenu(browseAction, nosortAction);
		table.addMouseListener(popup.getMouseListener(table));

		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent evt) {
				if (evt.getClickCount() == 2 /*&& table.getSelectedRow() != -1*/) {
					browseAction.actionPerformed(null);
				}
			}
		});
	}
	
	FirmwareManagerG2 getFirmwareManager(int index) {
		return (FirmwareManagerG2)devicesFWData.get(index).fwModule;
	}

	private void fillTable() {
		tModel.clear();
		for(int i = 0; i < devicesFWData.size(); i++) {
			if(Thread.interrupted() == false) {
				tModel.addRow(createTableRow(i));
			}
		}
		if(Thread.interrupted() == false) {
			countSelection();
		}
	}

	private Object[] createTableRow(int localIndex/*, boolean select*/) {
		ShellyAbstractDevice d = parentDlg.getLocalDevice(localIndex);
		DeviceFirmware info = devicesFWData.get(localIndex);
		if(info != null) {
			FirmwareManagerG2 fw = (FirmwareManagerG2)info.fwModule;
			if(fw != null) {
				if(fw.upadating()) {
					return new Object[] {DevicesTable.UPDATING_BULLET, UtilMiscellaneous.getExtendedHostName(d), fw.alternativeDescription(),
							(d instanceof AbstractG2Device) ? String.format(LABELS.getString("lbl_downloading"), 0) : LABELS.getString("labelUpdating"), ""};
				} else {
					Boolean stableCell = (fw.altStable() != null) ? Boolean.FALSE : null;
					Boolean betaCell = (fw.altBeta() != null) ? Boolean.FALSE : null;
					return new Object[] {DevicesTable.getStatusIcon(d), UtilMiscellaneous.getExtendedHostName(d), fw.alternativeDescription(), stableCell, betaCell};
				}
			} else {
				DeferrablesContainer dc = DeferrablesContainer.getInstance();
				if(dc.indexOf(parentDlg.getModelIndex(localIndex), DeferrableTask.Type.FW_UPDATE) < 0) {
					return new Object[] {DevicesTable.getStatusIcon(d), UtilMiscellaneous.getExtendedHostName(d), "" /*description unknown*/, null};
				} else {
					return new Object[] {DevicesTable.getStatusIcon(d), UtilMiscellaneous.getExtendedHostName(d), "",  LABELS.getString("labelRequested")};
				}
			}
		} else {
			return new Object[] {DevicesTable.getStatusIcon(d), UtilMiscellaneous.getExtendedHostName(d), "", null, null};
		}
	}

	@Override
	String showing() throws InterruptedException {
		lblCount.setText("");
		btnCheck.setEnabled(false);
		final int size = parentDlg.getLocalSize();
		devicesFWData = new ArrayList<>(size);
		tModel.clear();
		try {
			List<Callable<Void>> calls = new ArrayList<>();
			for(int i = 0; i < size; i++) {
				if(parentDlg.getLocalDevice(i) instanceof AbstractG2Device) { // 3 & 4 & ... extends 2
					devicesFWData.add(new DeviceFirmware());
					calls.add(new GetFWManagerCaller(i));
				} else {
					devicesFWData.add(null);
				}
			}
			retriveFutures = exeService.invokeAll(calls);
			fillTable();
			parentDlg.getModel().addListener(this);

			table.columnsWidthAdapt();
			final FontMetrics fm = getGraphics().getFontMetrics();
			TableColumn stableColumn = table.getColumnModel().getColumn(FWAlternativeTable.COL_STABLE);
			stableColumn.setPreferredWidth(Math.max(SwingUtilities.computeStringWidth(fm, "0.00.0"), stableColumn.getPreferredWidth()));
			TableColumn betaColumn = table.getColumnModel().getColumn(FWAlternativeTable.COL_BETA);
			betaColumn.setPreferredWidth(Math.max(SwingUtilities.computeStringWidth(fm, "0.00.0-beta1"), betaColumn.getPreferredWidth()));
			btnCheck.setEnabled(true);
			return null;
		} catch (RuntimeException e) {
			LOG.warn("showing", e);
			return e.toString();
		}
	}

	@Override
	void hiding() {
		parentDlg.getModel().removeListener(this);
		if(retriveFutures != null) {
			retriveFutures.forEach(f -> f.cancel(true));
		}
		devicesFWData.stream().filter(Objects::nonNull).map(dd -> dd.wsSession).filter(Objects::nonNull).forEach(futureSession -> {
			try {
				futureSession.get().close();
			} catch (InterruptedException | ExecutionException e) {
				LOG.error("ws-close", e);
			}
		});
	}

	private class GetFWManagerCaller implements Callable<Void> {
		private final int index;

		private GetFWManagerCaller(int index) {
			this.index = index;
		}

		@Override
		public Void call() {
			initDevice(index);
			return null;
		}
	}

	private void initDevice(final int index) {
		final ShellyAbstractDevice d = parentDlg.getLocalDevice(index);
		FirmwareManager fm = d.getFWManager();
		DeviceFirmware fwInfo = devicesFWData.get(index);
		fwInfo.fwModule = fm;
		fwInfo.status = d.getStatus();
		if(fm.upadating()) {
			fwInfo.uptime = d.getUptime();
		}
		try {
			fwInfo.wsSession = wsEventListener(index, d);
		} catch (IOException | InterruptedException | ExecutionException e) {
			LOG.debug("PanelFWUpdate ws: {}", d, e);
		}
	}

	@Override
	String apply() {
		int count = countSelection();
		if(count > 0 && JOptionPane.showConfirmDialog(this,
				String.format(LABELS.getString("dlgSetConfirmUpdate"), count), LABELS.getString("dlgSetFWUpdate"),
				JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
			String res = "";
			for(int i = 0; i < tModel.getRowCount(); i++) {
				if(table.convertRowIndexToView(i) >= 0) { // included in filter
					Object update = tModel.getValueAt(i, FWAlternativeTable.COL_STABLE);
					Object beta = tModel.getValueAt(i, FWAlternativeTable.COL_BETA);
					if(update == Boolean.TRUE) {
						res += updateDeviceFW(i, true);
					} else if(beta == Boolean.TRUE) {
						res += updateDeviceFW(i, false);
					}
				}
			}
			fillTable();
			return res;
		}
		return null;
	}

	private String updateDeviceFW(int i, boolean toStable) {
		ShellyAbstractDevice device = parentDlg.getLocalDevice(i);
		if(device instanceof GhostDevice == false) {
			DeviceFirmware fwInfo = devicesFWData.get(i);
			fwInfo.uptime = device.getUptime();
			String msg = ((FirmwareManagerG2)fwInfo.fwModule).updateAlternate(toStable);
			if(msg != null) {
				if(device.getStatus() == Status.OFF_LINE) {
					createDeferrable(parentDlg.getModelIndex(i), toStable);
					return UtilMiscellaneous.getFullName(device) + " - " + LABELS.getString("msgFWUpdateQueue") + "\n";
				} else {
					if(LABELS.containsKey(msg)) {
						msg = LABELS.getString(msg);
					}
					return UtilMiscellaneous.getFullName(device) + " - " + LABELS.getString("labelError") + ": " + msg + "\n";
				}
			} else { // success
				try {
					if(fwInfo.wsSession != null && fwInfo.wsSession.get().isOpen() == false) {
						fwInfo.wsSession = wsEventListener(i, device);
					}
				} catch (InterruptedException | ExecutionException | IOException e) {}
				return "";
			}
		} else {
			createDeferrable(parentDlg.getModelIndex(i), true);
			return UtilMiscellaneous.getFullName(device) + " - " + LABELS.getString("msgFWUpdateQueue") + "\n";
		}
	}
	
	private static void createDeferrable(int modelIndex, boolean toStable) {
		DeferrablesContainer dc = DeferrablesContainer.getInstance();
		dc.addOrUpdate(modelIndex, DeferrableTask.Type.FW_UPDATE, LABELS.getString(toStable ? "dlgSetFWUpdateStable" : "dlgSetFWUpdateBeta"), (def, dev) -> {
			return ((FirmwareManagerG2)dev.getFWManager()).updateAlternate(toStable);
		});
	}

	int countSelection() {
		int countS = 0;
		int countB = 0;
		for(int i = 0; i < tModel.getRowCount(); i++) {
			if(table.convertRowIndexToView(i) >= 0) {
				Object update = tModel.getValueAt(i, FWAlternativeTable.COL_STABLE);
				Object beta = tModel.getValueAt(i, FWAlternativeTable.COL_BETA);
				if(update == Boolean.TRUE) countS++;
				if(beta == Boolean.TRUE) countB++;
			}
		}
		lblCount.setText(String.format(LABELS.getString("lbl_update_count"), countS, countB));
		return countS + countB;
	}

	private Future<Session> wsEventListener(int index, ShellyAbstractDevice d) throws IOException, InterruptedException, ExecutionException {
		return ((AbstractG2Device)d).connectWebSocketClient(new FMUpdateListener(index, "sys"));
	}
	
	// "Jetty uses MethodHandles to instantiate WebSocket endpoints and invoke WebSocket event methods, so WebSocket endpoint classes and WebSocket event methods must be public"
	// -> no anonymous class
	public class FMUpdateListener extends WebSocketDeviceListener {
		private final int index;
		private final String component;
		
		public FMUpdateListener(int index, String component) {
			super(json -> json.path("method").asString("").equals(WebSocketDeviceListener.NOTIFY_EVENT));
			this.index = index;
			this.component = component;
		}

		@Override
		public void onMessage(JsonNode msg) {
			try {
				for(JsonNode event: msg.path("params").path("events")) {
					final String comp = event.path("component").asString("");
					if(component.equals(comp)) {
						final String eventType = event.path("event").asString("");
						if(eventType.equals("ota_progress")) { // dowloading
							FirmwareManager fm = getFirmwareManager(index);
							if(fm != null) {
								fm.upadating(true);
							}
							int progress = event.path("progress_percent").asInt();
							tModel.setValueAt(DevicesTable.UPDATING_BULLET, index, FWAlternativeTable.COL_STATUS);
							tModel.setValueAt(String.format(LABELS.getString("lbl_downloading"), progress), index, FWAlternativeTable.COL_STABLE);
							tModel.setValueAt("", index, FWAlternativeTable.COL_BETA);
							break;
						} else if(eventType.equals("ota_success") || eventType.equals("scheduled_restart")) { // rebooting
							tModel.setValueAt(DevicesTable.OFFLINE_BULLET, index, FWAlternativeTable.COL_STATUS);
							tModel.setValueAt(LABELS.getString("lbl_rebooting"), index, FWAlternativeTable.COL_STABLE);
							tModel.setValueAt("", index, FWAlternativeTable.COL_BETA);
							devicesFWData.get(index).rebootTime = System.currentTimeMillis();
							break;
						} else if(eventType.equals("ota_error")) {
							tModel.setValueAt(DevicesTable.ERROR_BULLET, index, FWAlternativeTable.COL_STATUS);
							tModel.setValueAt(LABELS.getString("lbl_error"), index, FWAlternativeTable.COL_STABLE);
							tModel.setValueAt("", index, FWAlternativeTable.COL_BETA);
							LOG.error("ota_error: {}", event.path("msg").asString("no msg"));
							break;
						}
					}
				}
			} catch(Exception e) {
				LOG.debug("onMessage {}", msg, e);
			}
		}
		
		@Override
		public void onWebSocketError(Throwable cause) {
			if(cause instanceof WebSocketTimeoutException) {
				LOG.trace("ws-timeout -> reopen");
				try {
					devicesFWData.get(index).wsSession = wsEventListener(index, parentDlg.getLocalDevice(index));
				} catch (IOException | InterruptedException | ExecutionException e) {
					LOG.debug("ws-timeout -> reopen error", e);
				}
			} else {
				super.onWebSocketError(cause);
			}
		}
	}

	private static class DeviceFirmware {
		private FirmwareManager fwModule;
		private Future<Session> wsSession;
		private long rebootTime = Long.MAX_VALUE; // g2+
		private int uptime = -1; // g1
		private ShellyAbstractDevice.Status status;
	}

	@Override
	public void update(EventType mesgType, Integer pos) {
		if(mesgType == Devices.EventType.UPDATE || mesgType == Devices.EventType.SUBSTITUTE) {
			final int localIndex = parentDlg.getLocalIndex(pos);
			if(localIndex >= 0 && devicesFWData.get(localIndex) != null && tModel.getValueAt(localIndex, FWAlternativeTable.COL_STATUS) != DevicesTable.ERROR_BULLET) {
				final ShellyAbstractDevice device = parentDlg.getModel().get(pos);
				ShellyAbstractDevice.Status newStatus = device.getStatus();

				if(newStatus == ShellyAbstractDevice.Status.ON_LINE && devicesFWData.get(localIndex).fwModule == null) {
					// Awakened
					retriveFutures.set(localIndex, exeService.submit(() -> {
						initDevice(localIndex);
						tModel.setRow(localIndex, createTableRow(localIndex));
					}, null));
				} else if(newStatus != ShellyAbstractDevice.Status.ERROR) {
					// Updating?
					DeviceFirmware fwInfo = devicesFWData.get(localIndex);
					// status changes to ON_LINE -> maybe reboot after fw update
					// System.currentTimeMillis() - fwInfo.rebootTime > 3000L && status == ON_LINE -> maybe sampling too slow and missed OFF_LINE (gen2)
					// device.getUptime() < fwInfo.uptime ("apply" time) -> && status == ON_LINE -> maybe sampling too slow and missed OFF_LINE (gen1)
					if(newStatus != fwInfo.status || System.currentTimeMillis() - fwInfo.rebootTime > 3000L || device.getUptime() < fwInfo.uptime) {
						if(newStatus == Status.ON_LINE) {
							exeService.submit(() -> {
								try {
									tModel.setValueAt(DevicesTable.UPDATING_BULLET, localIndex, FWAlternativeTable.COL_STATUS);
									Thread.sleep(Devices.MULTI_QUERY_DELAY);
									fwInfo.fwModule = device.getFWManager();
									tModel.setRow(localIndex, createTableRow(localIndex));
									countSelection();
//									if(device instanceof AbstractG2Device && fwInfo.wsSession.get().isOpen() == false) { // should be (closed on reboot)
//										fwInfo.wsSession = wsEventListener(localIndex, (AbstractG2Device)device);
//									}
									fwInfo.rebootTime = Long.MAX_VALUE; // reset value
									fwInfo.uptime = -1; // reset value
								} catch (Exception ex) {
									LOG.error("Unexpected", ex);
								}
							});
						} else if(fwInfo.fwModule.upadating() == false) {
							tModel.setValueAt(DevicesTable.getStatusIcon(device), localIndex, FWAlternativeTable.COL_STATUS);
						}
						fwInfo.status = newStatus;
					}
				}
			}
		}
	}
}