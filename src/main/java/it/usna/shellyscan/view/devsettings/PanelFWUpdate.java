package it.usna.shellyscan.view.devsettings;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FontMetrics;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumn;

import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.controller.DeferrableTask;
import it.usna.shellyscan.controller.DeferrablesContainer;
import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.Devices.EventType;
import it.usna.shellyscan.model.device.FirmwareManager;
import it.usna.shellyscan.model.device.GhostDevice;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.ShellyAbstractDevice.Status;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.g2.FirmwareManagerG2;
import it.usna.shellyscan.model.device.g2.WebSocketDeviceListener;
import it.usna.shellyscan.view.DevicesTable;
import it.usna.shellyscan.view.util.UtilMiscellaneous;
import it.usna.swing.table.ExTooltipTable;
import it.usna.swing.table.UsnaTableModel;
import it.usna.util.UsnaEventListener;

public class PanelFWUpdate extends AbstractSettingsPanel implements UsnaEventListener<Devices.EventType, Integer> {
	private static final long serialVersionUID = 1L;
	private ExTooltipTable table;
	private UsnaTableModel tModel = new UsnaTableModel("", LABELS.getString("col_device"), LABELS.getString("dlgSetColCurrentV"), LABELS.getString("dlgSetColLastV"), LABELS.getString("dlgSetColBetaV"));
	private List<DeviceFirmware> devicesFWData;

	private JLabel lblCount = new JLabel();
	private JButton btnCheck;

	private ScheduledExecutorService exeService = Executors.newScheduledThreadPool(35);
	private List<Future<Void>> retriveFutures;

	private final static Logger LOG = LoggerFactory.getLogger(PanelFWUpdate.class);

	public PanelFWUpdate(DialogDeviceSettings parent) {
		super(parent);
		setLayout(new BorderLayout(0, 0));
		
		table = new FWUpdateTable(tModel, this);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportView(table);
		add(scrollPane, BorderLayout.CENTER);

		JPanel panel = new JPanel();
		add(panel, BorderLayout.SOUTH);
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

		panel.add(Box.createHorizontalStrut(2));

		JButton btnUnselectAll = new JButton(new UsnaAction("btn_unselectAll", event -> {
			for(int i= 0; i < tModel.getRowCount(); i++) {
				if(tModel.getValueAt(i, FWUpdateTable.COL_STABLE) instanceof Boolean) {
					tModel.setValueAt(Boolean.FALSE, i, FWUpdateTable.COL_STABLE);
				}
				if(tModel.getValueAt(i, FWUpdateTable.COL_BETA) instanceof Boolean) {
					tModel.setValueAt(Boolean.FALSE, i, FWUpdateTable.COL_BETA);
				}
			}
			countSelection();
		}));
		btnUnselectAll.setBorder(BorderFactory.createEmptyBorder(4, 7, 4, 7));
		panel.add(btnUnselectAll);

		JButton btnSelectStable = new JButton(new UsnaAction("btn_selectAllSta", event -> {
			for(int i= 0; i < tModel.getRowCount(); i++) {
				if(tModel.getValueAt(i, FWUpdateTable.COL_STABLE) instanceof Boolean) {
					tModel.setValueAt(Boolean.TRUE, i, FWUpdateTable.COL_STABLE);
					if(tModel.getValueAt(i, FWUpdateTable.COL_BETA) instanceof Boolean) {
						tModel.setValueAt(Boolean.FALSE, i, FWUpdateTable.COL_BETA);
					}
				}
			}
			countSelection();
		}));
		btnSelectStable.setBorder(BorderFactory.createEmptyBorder(4, 7, 4, 7));
		panel.add(btnSelectStable);

		JButton btnSelectBeta = new JButton(new UsnaAction("btn_selectAllbeta", event -> {
			for(int i= 0; i < tModel.getRowCount(); i++) {
				if(tModel.getValueAt(i, FWUpdateTable.COL_BETA) instanceof Boolean) {
					tModel.setValueAt(Boolean.TRUE, i, FWUpdateTable.COL_BETA);
					if(tModel.getValueAt(i, FWUpdateTable.COL_STABLE) instanceof Boolean) {
						tModel.setValueAt(Boolean.FALSE, i, FWUpdateTable.COL_STABLE);
					}
				}
			}
			countSelection();
		}));
		btnSelectBeta.setBorder(BorderFactory.createEmptyBorder(4, 7, 4, 7));
		panel.add(btnSelectBeta);

		panel.add(Box.createHorizontalStrut(12));
		panel.add(lblCount);
		panel.add(Box.createHorizontalGlue());

		btnCheck = new JButton(new UsnaAction("btn_check", event -> {
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			btnCheck.setEnabled(false);
			int i;
			for(i = 0; i < devicesFWData.size(); i++) {
				FirmwareManager fwModule = devicesFWData.get(i).fwModule;
				if(fwModule != null && tModel.getValueAt(i, FWUpdateTable.COL_STATUS) != DevicesTable.UPDATING_BULLET) {
					tModel.setValueAt(DevicesTable.UPDATING_BULLET, i, FWUpdateTable.COL_STATUS);
					final int row = i;
					exeService.schedule(() -> {
						fwModule.chech();
						tModel.setRow(row, createTableRow(row));
					}, i , TimeUnit.MILLISECONDS);
				}
			}
			exeService.schedule(() -> {
				btnCheck.setEnabled(true);
				setCursor(Cursor.getDefaultCursor());
			}, i + 1000, TimeUnit.MILLISECONDS);
		}));
		btnCheck.setBorder(BorderFactory.createEmptyBorder(4, 7, 4, 7));
		panel.add(btnCheck);

		panel.add(Box.createHorizontalStrut(2));
	}
	
	FirmwareManager getFirmwareManager(int index) {
		return devicesFWData.get(index).fwModule;
	}

	private void fillTable() {
		tModel.clear();
		for(int i = 0; i < parent.getLocalSize(); i++) {
			if(Thread.interrupted() == false) {
				tModel.addRow(createTableRow(i));
			}
		}
		if(Thread.interrupted() == false) {
			countSelection();
		}
	}

	private Object[] createTableRow(int localIndex) {
		ShellyAbstractDevice d = parent.getLocalDevice(localIndex);
		FirmwareManager fw = getFirmwareManager(localIndex);
		if(fw != null) {
			if(fw.upadating()) {
				return new Object[] {DevicesTable.UPDATING_BULLET, UtilMiscellaneous.getExtendedHostName(d), FirmwareManager.getShortVersion(fw.current()), LABELS.getString("labelUpdating"), null}; // DevicesTable.UPDATING_BULLET
			} else {
				Boolean stableCell = (fw != null && fw.newStable() != null) ? Boolean.TRUE : null;
				Boolean betaCell = (fw != null && fw.newBeta() != null) ? Boolean.FALSE : null;
				return new Object[] {DevicesTable.getStatusIcon(d), UtilMiscellaneous.getExtendedHostName(d), FirmwareManager.getShortVersion(fw.current()), stableCell, betaCell};
			}
		} else {
			DeferrablesContainer dc = DeferrablesContainer.getInstance();
			if(dc.indexOf(parent.getModelIndex(localIndex), DeferrableTask.Type.FW_UPDATE) < 0) {
				return new Object[] {DevicesTable.getStatusIcon(d), UtilMiscellaneous.getExtendedHostName(d), null /*current fw unknown*/, Boolean.FALSE /*any*/};
			} else {
				return new Object[] {DevicesTable.getStatusIcon(d), UtilMiscellaneous.getExtendedHostName(d), null,  LABELS.getString("labelRequested")};
			}
		}
	}

	@Override
	String showing() throws InterruptedException {
		lblCount.setText("");
		btnCheck.setEnabled(false);
		final int size = parent.getLocalSize();
		devicesFWData = Stream.generate(DeviceFirmware::new).limit(size).collect(Collectors.toList());
		tModel.clear();
		try {
			List<Callable<Void>> calls = new ArrayList<>(size);
			for(int i = 0; i < size; i++) {
				calls.add(new GetFWManagerCaller(i));
			}
			retriveFutures = exeService.invokeAll(calls);
			fillTable();
			parent.getModel().addListener(this);

			table.columnsWidthAdapt();
			final FontMetrics fm = getGraphics().getFontMetrics();
			TableColumn stableColumn = table.getColumnModel().getColumn(FWUpdateTable.COL_STABLE);
			stableColumn.setPreferredWidth(Math.max(SwingUtilities.computeStringWidth(fm, "0.12.0"), stableColumn.getPreferredWidth()));
			TableColumn betaColumn = table.getColumnModel().getColumn(FWUpdateTable.COL_BETA);
			betaColumn.setPreferredWidth(Math.max(SwingUtilities.computeStringWidth(fm, "0.12.0-beta1"), betaColumn.getPreferredWidth()));
			btnCheck.setEnabled(true);
			return null;
		} catch (RuntimeException e) {
			LOG.warn("showing", e);
			return e.toString();
		}
	}

	@Override
	void hiding() {
		parent.getModel().removeListener(this);
		if(retriveFutures != null) {
			retriveFutures.forEach(f -> f.cancel(true));
		}
		devicesFWData.stream().map(dd -> dd.wsSession).filter(futureSession -> futureSession != null).forEach(futureSession -> {
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
		final ShellyAbstractDevice d = parent.getLocalDevice(index);
		FirmwareManager fm = d.getFWManager();
		DeviceFirmware fwInfo = devicesFWData.get(index);
		fwInfo.fwModule = fm;
		fwInfo.status = d.getStatus();
		if(fm.upadating()) {
			fwInfo.uptime = d.getUptime();
		}
		if(d instanceof AbstractG2Device) {
			try {
				fwInfo.wsSession = wsEventListener(index, (AbstractG2Device)d);
			} catch (IOException | InterruptedException | ExecutionException e) {
				LOG.debug("PanelFWUpdate ws: {}", d, e);
			}
		}
	}

	@Override
	String apply() {
		int count = countSelection();
		if(count > 0 && JOptionPane.showConfirmDialog(this,
				String.format(LABELS.getString("dlgSetConfirmUpdate"), count), LABELS.getString("dlgSetFWUpdate"),
				JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
			String res = "";
			for(int i = 0; i < parent.getLocalSize(); i++) {
				Object update = tModel.getValueAt(i, FWUpdateTable.COL_STABLE);
				Object beta = tModel.getValueAt(i, FWUpdateTable.COL_BETA);
				if(update instanceof Boolean && ((Boolean)update) == Boolean.TRUE) {
					res += updateDeviceFW(i, true);
				} else if(beta instanceof Boolean && ((Boolean)beta) == Boolean.TRUE) {
					res += updateDeviceFW(i, false);
				}
			}
			fillTable();
			return res;
		}
		return null;
	}

	private String updateDeviceFW(int i, boolean toStable) {
		ShellyAbstractDevice device = parent.getLocalDevice(i);
		if(device instanceof GhostDevice == false) {
			DeviceFirmware fwInfo = devicesFWData.get(i);
			fwInfo.uptime = device.getUptime();
			String msg = fwInfo.fwModule.update(toStable);
			if(msg != null) {
				if(device.getStatus() == Status.OFF_LINE) {
					createDeferrable(parent.getModelIndex(i), fwInfo.fwModule, toStable);
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
						fwInfo.wsSession = wsEventListener(i, (AbstractG2Device)device);
					}
				} catch (InterruptedException | ExecutionException | IOException e) {}
				return "";
			}
		} else {
			createDeferrable(parent.getModelIndex(i), null, true);
			return UtilMiscellaneous.getFullName(device) + " - " + LABELS.getString("msgFWUpdateQueue") + "\n";
		}
	}
	
	private static void createDeferrable(int modelIndex, FirmwareManager fm, boolean toStable) {
		DeferrablesContainer dc = DeferrablesContainer.getInstance();
		dc.addOrUpdate(modelIndex, DeferrableTask.Type.FW_UPDATE, LABELS.getString(toStable ? "dlgSetFWUpdateStable" : "dlgSetFWUpdateBeta"), (def, dev) -> {
			return dev.getFWManager().update(toStable);
		});
	}

	int countSelection() {
		int countS = 0;
		int countB = 0;
		for(int i = 0; i < tModel.getRowCount(); i++) {
			Object update = tModel.getValueAt(i, FWUpdateTable.COL_STABLE);
			Object beta = tModel.getValueAt(i, FWUpdateTable.COL_BETA);
			if(update == Boolean.TRUE) countS++;
			if(beta == Boolean.TRUE) countB++;
		}
		lblCount.setText(String.format(LABELS.getString("lbl_update_count"), countS, countB));
		return countS + countB;
	}

	private Future<Session> wsEventListener(int index, AbstractG2Device d) throws IOException, InterruptedException, ExecutionException {
		return d.connectWebSocketClient(new FMUpdateListener(index));
	}
	
	// "Jetty uses MethodHandles to instantiate WebSocket endpoints and invoke WebSocket event methods, so WebSocket endpoint classes and WebSocket event methods must be public"
	// -> no anonymous class
	public class FMUpdateListener extends WebSocketDeviceListener {
		private final int index;
		public FMUpdateListener(int index) {
			super(json -> json.path("method").asText().equals(WebSocketDeviceListener.NOTIFY_EVENT));
			this.index = index;
		}

		@Override
		public void onMessage(JsonNode msg) {
			try {
				for(JsonNode event: msg.path("params").path("events")) {
					String eventType = event.path("event").asText();
					if(eventType.equals("ota_progress")) { // dowloading
						((FirmwareManagerG2)getFirmwareManager(index)).upadating(true);
						int progress = event.path("progress_percent").asInt();
						tModel.setValueAt(DevicesTable.UPDATING_BULLET, index, FWUpdateTable.COL_STATUS);
						tModel.setValueAt(String.format(Main.LABELS.getString("lbl_downloading"), progress), index, FWUpdateTable.COL_STABLE);
						tModel.setValueAt(null, index, FWUpdateTable.COL_BETA);
						break;
					} else if(/*eventType.equals("ota_success") ||*/ eventType.equals("scheduled_restart")) { // rebooting
						tModel.setValueAt(DevicesTable.OFFLINE_BULLET, index, FWUpdateTable.COL_STATUS);
						tModel.setValueAt(LABELS.getString("lbl_rebooting"), index, FWUpdateTable.COL_STABLE);
						tModel.setValueAt(null, index, FWUpdateTable.COL_BETA);
						devicesFWData.get(index).rebootTime = System.currentTimeMillis();
						break;
					}
				}
			} catch(Exception e) {
				LOG.debug("onMessage {}", msg, e);
			}
		}
	}

	private static class DeviceFirmware {
		private FirmwareManager fwModule;
		private Future<Session> wsSession;
		private long rebootTime = Long.MAX_VALUE; // g2
		private int uptime = -1; // g1
		private ShellyAbstractDevice.Status status;
	}

	@Override
	public void update(EventType mesgType, Integer pos) {
		if(mesgType == Devices.EventType.UPDATE || mesgType == Devices.EventType.SUBSTITUTE) {
			final int localIndex = parent.getLocalIndex(pos);
			if(localIndex >= 0) {
				final ShellyAbstractDevice device = parent.getModel().get(pos);
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
									tModel.setValueAt(DevicesTable.ONLINE_BULLET, localIndex, FWUpdateTable.COL_STATUS);
									Thread.sleep(Devices.MULTI_QUERY_DELAY);
									fwInfo.fwModule = device.getFWManager();
									tModel.setRow(localIndex, createTableRow(localIndex));
									countSelection();
									if(device instanceof AbstractG2Device && fwInfo.wsSession.get().isOpen() == false) { // should be (closed on reboot)
										fwInfo.wsSession = wsEventListener(localIndex, (AbstractG2Device)device);
									}
									fwInfo.rebootTime = Long.MAX_VALUE; // reset
									fwInfo.uptime = -1; // reset
								} catch (Throwable ex) {
									LOG.error("Unexpected", ex);
								}
							});
						} else if(fwInfo.fwModule.upadating() == false) {
							tModel.setValueAt(DevicesTable.getStatusIcon(device), localIndex, FWUpdateTable.COL_STATUS);
						}
						fwInfo.status = newStatus;
					}
				}
			}
		}
	}
} // 346 - 362 - 462 - 476 - 509 - 418 - 438

//{"src":"shellyplusi4-a8032ab1fe78","dst":"S_Scanner","method":"NotifyEvent","params":{"ts":1677696108.45,"events":[{"component":"sys", "event":"ota_progress", "msg":"Waiting for data", "progress_percent":99, "ts":1677696108.45}]}}
//{"src":"shellyplusi4-a8032ab1fe78","dst":"S_Scanner","method":"NotifyEvent","params":{"ts":1677696109.49,"events":[{"component":"sys", "event":"ota_success", "msg":"Update applied, rebooting", "ts":1677696109.49}]}}
//{"src":"shellyplusi4-a8032ab1fe78","dst":"S_Scanner","method":"NotifyEvent","params":{"ts":1677696109.57,"events":[{"component":"sys", "event":"scheduled_restart", "time_ms": 435, "ts":1677696109.57}]}}