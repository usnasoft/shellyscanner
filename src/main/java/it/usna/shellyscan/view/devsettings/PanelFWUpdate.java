package it.usna.shellyscan.view.devsettings;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FontMetrics;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.Devices.EventType;
import it.usna.shellyscan.model.device.FirmwareManager;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.g2.FirmwareManagerG2;
import it.usna.shellyscan.model.device.g2.WebSocketDeviceListener;
import it.usna.shellyscan.view.DevicesTable;
import it.usna.shellyscan.view.util.UtilCollecion;
import it.usna.swing.table.ExTooltipTable;
import it.usna.swing.table.UsnaTableModel;
import it.usna.util.UsnaEventListener;

public class PanelFWUpdate extends AbstractSettingsPanel implements UsnaEventListener<Devices.EventType, Integer> {
	private static final long serialVersionUID = 1L;
	private final static int COL_STATUS = 0;
	private final static int COL_CURRENT = 2;
	private final static int COL_STABLE = 3;
	private final static int COL_BETA = 4;
	private ExTooltipTable table;
	private UsnaTableModel tModel = new UsnaTableModel("", LABELS.getString("col_device"), LABELS.getString("dlgSetColCurrentV"), LABELS.getString("dlgSetColLastV"), LABELS.getString("dlgSetColBetaV"));
	private List<DeviceFirmware> devicesFWData;
	
	private JButton btnUnselectAll = new JButton(LABELS.getString("btn_unselectAll"));
	private JButton btnSelectStable = new JButton(LABELS.getString("btn_selectAllSta"));
	private JButton btnSelectBeta = new JButton(LABELS.getString("btn_selectAllbeta"));
	private JLabel lblCount = new JLabel();

	private ExecutorService exeService = Executors.newFixedThreadPool(25);
	private List<Future<Void>> retriveFutures;
	
	private final static Logger LOG = LoggerFactory.getLogger(PanelFWUpdate.class);
	
	/**
	 * @wbp.nonvisual location=61,49
	 */
	public PanelFWUpdate(DialogDeviceSettings parent) {
		super(parent);
		setLayout(new BorderLayout(0, 0));

		table = new ExTooltipTable(tModel) {
			private static final long serialVersionUID = 1L;
			{
				getTableHeader().setReorderingAllowed(false);
				((JCheckBox) getDefaultRenderer(Boolean.class)).setOpaque(true);
				((JCheckBox) getDefaultRenderer(Boolean.class)).setHorizontalAlignment(JCheckBox.LEFT);
				TableCellRenderer fwRendered = new FWCellRendered();
				getColumnModel().getColumn(COL_STABLE).setCellRenderer(fwRendered);
				getColumnModel().getColumn(COL_BETA).setCellRenderer(fwRendered);
				columnModel.getColumn(COL_STATUS).setMaxWidth(DevicesTable.ONLINE_BULLET.getIconWidth() + 4);
			}

			@Override
			public boolean isCellEditable(final int row, final int column) {
				return getValueAt(row, column) instanceof Boolean;
			}

			@Override
			public Component prepareEditor(TableCellEditor editor, int row, int column) {
				JCheckBox comp = (JCheckBox)super.prepareEditor(editor, row, column);
				FirmwareManager fw = devicesFWData.get(table.convertRowIndexToModel(row)).fwModule;
				comp.setText(FirmwareManager.getShortVersion(column == COL_STABLE ? fw.newStable() : fw.newBeta()));
				comp.setBackground(table.getSelectionBackground());
				comp.setForeground(table.getSelectionForeground());
				comp.setHorizontalAlignment(JLabel.LEFT);
				return comp;
			}

			@Override
			public void editingStopped(ChangeEvent e) {
				final int r = getEditingRow();
				final int c = getEditingColumn();
				super.editingStopped(e);
				if(r >= 0 && c >= 0 && getValueAt(r, c) == Boolean.TRUE) {
					final int toOff = (c == COL_STABLE) ? COL_BETA : COL_STABLE;
					if(getValueAt(r, toOff) instanceof Boolean) {
						setValueAt(Boolean.FALSE, r, toOff);
					}
				}
				countSelection();
			}
			
			@Override
			protected String cellTooltipValue(Object value, boolean noSpace, int row, int column) {
				FirmwareManager fw = devicesFWData.get(table.convertRowIndexToModel(row)).fwModule;
				if(column == COL_CURRENT && fw != null) {
					return fw.current();
				} else if(column == COL_STABLE && fw != null) {
					return fw.newStable();
				} else if(column == COL_BETA && fw != null) {
					return fw.newBeta();
				}
				return super.cellTooltipValue(value, noSpace, row, column);
			}
		};

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportView(table);
		add(scrollPane, BorderLayout.CENTER);

		JPanel panel = new JPanel();
		add(panel, BorderLayout.SOUTH);
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		
		panel.add(Box.createHorizontalStrut(2));
		
		btnUnselectAll.setBorder(BorderFactory.createEmptyBorder(4, 7, 4, 7));
		panel.add(btnUnselectAll);
		btnUnselectAll.addActionListener(event -> {
			for(int i= 0; i < tModel.getRowCount(); i++) {
				if(tModel.getValueAt(i, COL_STABLE) instanceof Boolean) {
					tModel.setValueAt(Boolean.FALSE, i, COL_STABLE);
				}
				if(tModel.getValueAt(i, COL_BETA) instanceof Boolean) {
					tModel.setValueAt(Boolean.FALSE, i, COL_BETA);
				}
			}
			countSelection();
		});

		btnSelectStable.setBorder(BorderFactory.createEmptyBorder(4, 7, 4, 7));
		panel.add(btnSelectStable);
		btnSelectStable.addActionListener(event -> {
			for(int i= 0; i < tModel.getRowCount(); i++) {
				if(tModel.getValueAt(i, COL_STABLE) instanceof Boolean) {
					tModel.setValueAt(Boolean.TRUE, i, COL_STABLE);
					if(tModel.getValueAt(i, COL_BETA) instanceof Boolean) {
						tModel.setValueAt(Boolean.FALSE, i, COL_BETA);
					}
				}
			}
			countSelection();
		});
		
		btnSelectBeta.setBorder(BorderFactory.createEmptyBorder(4, 7, 4, 7));
		panel.add(btnSelectBeta);
		btnSelectBeta.addActionListener(event -> {
			for(int i= 0; i < tModel.getRowCount(); i++) {
				if(tModel.getValueAt(i, COL_BETA) instanceof Boolean) {
					tModel.setValueAt(Boolean.TRUE, i, COL_BETA);
					if(tModel.getValueAt(i, COL_STABLE) instanceof Boolean) {
						tModel.setValueAt(Boolean.FALSE, i, COL_STABLE);
					}
				}
			}
			countSelection();
		});
		
		panel.add(Box.createHorizontalStrut(12));
		panel.add(lblCount);
		panel.add(Box.createHorizontalGlue());

		JButton btnCheck = new JButton(LABELS.getString("btn_check"));
		btnCheck.setBorder(BorderFactory.createEmptyBorder(4, 7, 4, 7));
		panel.add(btnCheck);
		
		panel.add(Box.createHorizontalStrut(2));
		
		btnCheck.addActionListener(event -> {
			btnCheck.setEnabled(false);
			exeService.execute(() -> {
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				for(int i = 0; i < parent.getLocalSize(); i++) {
					try {
						tModel.setValueAt(DevicesTable.UPDATING_BULLET, i, COL_STATUS);
					} catch(Exception e) {/* while fill() */}
				}
				try {
					devicesFWData.parallelStream().forEach(dd -> dd.fwModule.chech());
					fill();
				} catch(Exception e) {/* while fill() */
				} finally {
					btnCheck.setEnabled(true);
					setCursor(Cursor.getDefaultCursor());
				}
			});
		});
	}

	private void fill() {
		tModel.clear();
		for(int i = 0; i < parent.getLocalSize(); i++) {
			if(Thread.interrupted() == false) {
				tModel.addRow(createRow(i));
			}
		}
		if(Thread.interrupted() == false) {
			countSelection();
		}
	}
	
	private Object[] createRow(int index) {
		ShellyAbstractDevice d = parent.getLocalDevice(index);
		FirmwareManager fw = devicesFWData.get(index).fwModule;
//		boolean globalStable = false;
//		boolean globalBeta = false;
		if(fw.upadating()) {
			return new Object[] {DevicesTable.UPDATING_BULLET, UtilCollecion.getExtendedHostName(d), FirmwareManager.getShortVersion(fw.current()), LABELS.getString("labelUpdating"), null}; // DevicesTable.UPDATING_BULLET
		} else {
			boolean hasUpdate = fw.newStable() != null;
			boolean hasBeta = fw.newBeta() != null;
//			globalStable |= hasUpdate;
//			globalBeta |= hasBeta;
			return new Object[] {DevicesTable.getStatusIcon(d), UtilCollecion.getExtendedHostName(d), FirmwareManager.getShortVersion(fw.current()), hasUpdate ? Boolean.TRUE : null, hasBeta ? Boolean.FALSE : null};
		}
	}

	@Override
	public String showing() throws InterruptedException {
		lblCount.setText("");
		final int size = parent.getLocalSize();
		devicesFWData = Stream.generate(DeviceFirmware::new).limit(size).collect(Collectors.toList());
//		devicesFWData = Stream.generate(DeviceFirmware::new).limit(size).toArray(DeviceFirmware[]::new);

		tModel.clear();
		try {
			List<Callable<Void>> calls = new ArrayList<>();
			for(int i = 0; i < size; i++) {
				calls.add(new GetFWManagerCaller(i));
			}
			retriveFutures = exeService.invokeAll(calls);
			fill();
			parent.getModel().addListener(this);

			table.columnsWidthAdapt();
			final FontMetrics fm = getGraphics().getFontMetrics();
			TableColumn stableC = table.getColumnModel().getColumn(COL_STABLE);
			stableC.setPreferredWidth(Math.max(SwingUtilities.computeStringWidth(fm, "0.12.0"), stableC.getPreferredWidth()));
			TableColumn betaC = table.getColumnModel().getColumn(COL_BETA);
			betaC.setPreferredWidth(Math.max(SwingUtilities.computeStringWidth(fm, "0.12.0-beta1"), betaC.getPreferredWidth()));
			return null;
		} catch (/*IOException |*/ RuntimeException e) {
			LOG.warn("showing", e);
			return e.toString();
		}
	}

	public void hiding() {
		parent.getModel().removeListener(this);
		if(retriveFutures != null) {
			retriveFutures.forEach(f -> f.cancel(true));
		}
		devicesFWData.parallelStream().map(dd -> dd.wsSession).filter(f -> f != null).forEach(f -> {
			try {
				f.get().close();
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
			final ShellyAbstractDevice d = parent.getLocalDevice(index);
			FirmwareManager fm = d.getFWManager();
			devicesFWData.get(index).fwModule = fm;
			if(fm.upadating()) {
				devicesFWData.get(index).uptime = d.getUptime();
			}
			
			if(d instanceof AbstractG2Device) {
				try {
					devicesFWData.get(index).wsSession = wsEventListener(index, (AbstractG2Device)d);
				} catch (IOException | InterruptedException | ExecutionException e) {
					LOG.debug("PanelFWUpdate ws", e);
				}
			}
			return null;
		}
	}

	@Override
	public String apply() {
		int count = countSelection();
		if(count > 0 && JOptionPane.showConfirmDialog(this,
				String.format(LABELS.getString("dlgSetConfirmUpdate"), count), LABELS.getString("dlgSetFWUpdate"),
				JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
			String res = "";
			for(int i = 0; i < parent.getLocalSize(); i++) {
				Object update = tModel.getValueAt(i, COL_STABLE);
				Object beta = tModel.getValueAt(i, COL_BETA);
				DeviceFirmware fwInfo = devicesFWData.get(i);
				if(update instanceof Boolean && ((Boolean)update) == Boolean.TRUE) {
					fwInfo.uptime = parent.getLocalDevice(i).getUptime();
					String msg = fwInfo.fwModule.update(true);
					if(msg != null && LABELS.containsKey(msg)) {
						msg = LABELS.getString(msg);
					}
					res += UtilCollecion.getFullName(parent.getLocalDevice(i)) + " - " + ((msg == null) ? LABELS.getString("labelUpdating") : LABELS.getString("labelError") + ": " + msg) + "\n";
				} else if(beta instanceof Boolean && ((Boolean)beta) == Boolean.TRUE) {
					fwInfo.uptime = parent.getLocalDevice(i).getUptime();
					String msg = fwInfo.fwModule.update(false);
					if(msg != null && LABELS.containsKey(msg)) {
						msg = LABELS.getString(msg);
					}
					res += UtilCollecion.getFullName(parent.getLocalDevice(i)) + " - " + ((msg == null) ? LABELS.getString("labelUpdatingBeta") : LABELS.getString("labelError") + ": " + msg) + "\n";
				}
			}
			fill();
			return res;
		}
		return null;
	}
	
	private int countSelection() {
		int countS = 0;
		int countB = 0;
		for(int i = 0; i < tModel.getRowCount(); i++) {
			Object update = tModel.getValueAt(i, COL_STABLE);
			Object beta = tModel.getValueAt(i, COL_BETA);
			if(update instanceof Boolean && ((Boolean)update) == Boolean.TRUE) {
				countS++;
			}
			if(beta instanceof Boolean && ((Boolean)beta) == Boolean.TRUE) {
				countB++;
			}
		}
		lblCount.setText(String.format(LABELS.getString("lbl_update_count"), countS, countB));
		return countS + countB;
	}
	
//	private void enableSelectionButtosn(boolean enable) {
//	btnUnselectAll.setEnabled(false);
//	btnSelectStable.setEnabled(false);
//	btnSelectBeta.setEnabled(false);
//	}
	
	private class FWCellRendered implements TableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			if(value == null || value instanceof Boolean) {
				JCheckBox c = (JCheckBox)table.getDefaultRenderer(Boolean.class).getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				if(value == null) {
					c.setEnabled(false);
					c.setText("");
				} else {
					c.setEnabled(true);
					FirmwareManager fw = devicesFWData.get(table.convertRowIndexToModel(row)).fwModule;
					if(fw != null) {
						c.setText(FirmwareManager.getShortVersion(column == COL_STABLE ? fw.newStable() : fw.newBeta()));
					}
				}
				return c;
			} else {
				return table.getDefaultRenderer(String.class).getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			}
		}
	}
	
	private Future<Session> wsEventListener(int index, AbstractG2Device d) throws IOException, InterruptedException, ExecutionException {
		return d.connectWebSocketClient(new WebSocketDeviceListener(json -> json.path("method").asText().equals(WebSocketDeviceListener.NOTIFY_EVENT)) {
			@Override
			public void onMessage(JsonNode msg) {
				try {
					for(JsonNode event: msg.path("params").path("events")) {
						String eventType = event.path("event").asText();
						if(eventType.equals("ota_progress")) { // dowloading
							((FirmwareManagerG2)devicesFWData.get(index).fwModule).upadating(true);
							int progress = event.path("progress_percent").asInt();
							tModel.setValueAt(String.format(Main.LABELS.getString("lbl_downloading"), progress), index, COL_STABLE);
							tModel.setValueAt(DevicesTable.UPDATING_BULLET, index, COL_STATUS);
							break;
						} else if(/*eventType.equals("ota_success") ||*/ eventType.equals("scheduled_restart")) { // rebooting
							tModel.setValueAt(DevicesTable.OFFLINE_BULLET, index, COL_STATUS);
							tModel.setValueAt(LABELS.getString("lbl_rebooting"), index, COL_STABLE);
							devicesFWData.get(index).rebootTime = System.currentTimeMillis();
						}
					}
				} catch(Exception e) {
					LOG.debug("onMessage" + msg, e);
				}
				//System.out.println(index + "-M: " + msg);
			}
		});
	}
	
	private static class DeviceFirmware {
		private FirmwareManager fwModule;
		private Future<Session> wsSession;
		private long rebootTime = Long.MAX_VALUE; // g2
		private int uptime = 0; // g1
	}

	@Override
	public void update(EventType mesgType, Integer pos) {
		if(mesgType == Devices.EventType.UPDATE) {
			SwingUtilities.invokeLater(() -> {
				try {
					final ShellyAbstractDevice device = parent.getModel().get(pos);
					final int index = parent.getLocalIndex(pos);
					if(index >= 0 && device.getStatus() != ShellyAbstractDevice.Status.ERROR) {
						DeviceFirmware fwInfo = devicesFWData.get(index);
						if(device.getStatus() != ShellyAbstractDevice.Status.ON_LINE) {
							tModel.setValueAt(DevicesTable.getStatusIcon(device), index, COL_STATUS);
						} else if(device.getUptime() < fwInfo.uptime || System.currentTimeMillis() - fwInfo.rebootTime > 2500L) {
							// starting uptime bigger than now (g1 - not 100% sure) or after 2.5 seconds since reboot (reboot completed - g2)
							fwInfo.rebootTime = Long.MAX_VALUE; // reset
							fwInfo.uptime = 0; // reset
							tModel.setValueAt(DevicesTable.ONLINE_BULLET, index, COL_STATUS);
//							fwInfo.fwModule.chech();
							fwInfo.fwModule = device.getFWManager();
							tModel.setRow(index, createRow(index));
							countSelection();
							if(device instanceof AbstractG2Device && fwInfo.wsSession.get().isOpen() == false) { // should be (closed on reboot)
								fwInfo.wsSession = wsEventListener(index, (AbstractG2Device)device);
							}
						}
					}
				} catch (Throwable ex) {
					LOG.error("Unexpected", ex);
				}
			});
		}
	}
} // 346 - 362 - 464

//{"src":"shellyplusi4-a8032ab1fe78","dst":"S_Scanner","method":"NotifyEvent","params":{"ts":1677696108.45,"events":[{"component":"sys", "event":"ota_progress", "msg":"Waiting for data", "progress_percent":99, "ts":1677696108.45}]}}
//{"src":"shellyplusi4-a8032ab1fe78","dst":"S_Scanner","method":"NotifyEvent","params":{"ts":1677696109.49,"events":[{"component":"sys", "event":"ota_success", "msg":"Update applied, rebooting", "ts":1677696109.49}]}}
//{"src":"shellyplusi4-a8032ab1fe78","dst":"S_Scanner","method":"NotifyEvent","params":{"ts":1677696109.57,"events":[{"component":"sys", "event":"scheduled_restart", "time_ms": 435, "ts":1677696109.57}]}}