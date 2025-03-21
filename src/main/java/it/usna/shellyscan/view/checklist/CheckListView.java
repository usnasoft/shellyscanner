package it.usna.shellyscan.view.checklist;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.RowFilter;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableRowSorter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.controller.UsnaDropdownAction;
import it.usna.shellyscan.controller.UsnaSelectedAction;
import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.Devices.EventType;
import it.usna.shellyscan.model.device.BatteryDeviceInterface;
import it.usna.shellyscan.model.device.InetAddressAndPort;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.ShellyAbstractDevice.LogMode;
import it.usna.shellyscan.model.device.ShellyAbstractDevice.Status;
import it.usna.shellyscan.model.device.g1.AbstractG1Device;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.g2.RangeExtenderManager;
import it.usna.shellyscan.model.device.g2.modules.Script;
import it.usna.shellyscan.model.device.g2.modules.WIFIManagerG2;
import it.usna.shellyscan.view.DevicesTable;
import it.usna.shellyscan.view.MainView;
import it.usna.shellyscan.view.devsettings.DialogDeviceSettings;
import it.usna.shellyscan.view.scripts.DialogDeviceScripts;
import it.usna.shellyscan.view.util.Msg;
import it.usna.shellyscan.view.util.ScannerProperties;
import it.usna.shellyscan.view.util.ScannerProperties.PropertyEvent;
import it.usna.shellyscan.view.util.UtilMiscellaneous;
import it.usna.swing.UsnaPopupMenu;
import it.usna.swing.table.UsnaTableModel;
import it.usna.swing.texteditor.TextDocumentListener;
import it.usna.util.UsnaEventListener;

public class CheckListView extends JDialog implements UsnaEventListener<Devices.EventType, Integer>, ScannerProperties.AppPropertyListener {
	private static final long serialVersionUID = 1L;
	private final static Logger LOG = LoggerFactory.getLogger(CheckListView.class);
	public final static String TRUE_STR = LABELS.getString("true_yn");
	public final static String FALSE_STR = LABELS.getString("false_yn");
	public final static String NOT_APPLICABLE_STR = "-";
	private final ScannerProperties properties = ScannerProperties.instance();
	private final Devices appModel;
	private final int[] devicesInd;
	private final JToolBar toolBar = new JToolBar();
	private final CheckListTable table;
	private final UsnaTableModel tModel;
	private ScheduledExecutorService exeService /* = Executors.newFixedThreadPool(20) */;

	public CheckListView(final Frame owner, Devices appModel, int[] devicesInd, final SortOrder ipSort) {
		super(owner, LABELS.getString("dlgChecklistTitle"));
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		this.appModel = appModel;
		this.devicesInd = devicesInd;
		
		Container contentPane = getContentPane();
		contentPane.setBackground(Main.STATUS_LINE_COLOR);

		tModel = new UsnaTableModel("" /*status*/,
				LABELS.getString("col_device"), LABELS.getString("col_ip"), LABELS.getString("col_eco"), LABELS.getString("col_ledoff"), LABELS.getString("col_logs"), LABELS.getString("col_blt"),
				LABELS.getString("col_AP"), LABELS.getString("col_roaming"), LABELS.getString("col_wifi1"), LABELS.getString("col_wifi2"), LABELS.getString("col_extender"), LABELS.getString("col_scripts")) {
			private static final long serialVersionUID = 1L;

			@Override
			public Class<?> getColumnClass(final int c) {
				return (c == CheckListTable.COL_IP) ? InetAddressAndPort.class : super.getColumnClass(c);
			}
		};
		table = new CheckListTable(tModel, ipSort);

		Action ecoModeAction = new UsnaSelectedAction(this, table, "setEcoMode_action", "setEcoMode_action_tooletip", null, "/images/leaf24.png", localRow -> {
			Boolean eco = (Boolean) tModel.getValueAt(localRow, CheckListTable.COL_ECO);
			ShellyAbstractDevice d = getLocalDevice(localRow);
			d.setEcoMode(!eco);
			try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e1) {}
			updateRow(d, localRow);
		});

		Action ledAction = new UsnaSelectedAction(this, table, "setLED_action", "setLED_action_tooletip", null, "/images/Light24.png", localRow -> { // AbstractG1Device
			Boolean led = (Boolean) tModel.getValueAt(localRow, CheckListTable.COL_LED);
			AbstractG1Device d = (AbstractG1Device) getLocalDevice(localRow);
			d.setLEDMode(!led);
			try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e1) {}
			updateRow(d, localRow);
		});

		Action logsG1Action = new UsnaSelectedAction(this, table, "setLogs_action", "setLogs_action_tooletip", null, "/images/Document2_24.png", localRow -> { // AbstractG1Device
			Boolean logs = (Boolean) tModel.getValueAt(localRow, CheckListTable.COL_LOGS);
			ShellyAbstractDevice d = getLocalDevice(localRow);
			d.setDebugMode(LogMode.FILE, !logs);
			try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e1) {}
			updateRow(d, localRow);
		});

		Action logsG2Action = new UsnaDropdownAction(this, "setLogs_action", "setLogs_action_tooletip"/*, null*/, "/images/Document2_24.png", new Action[] {
				new UsnaSelectedAction(this, table, "debugSOCKET", localRow -> {
					ShellyAbstractDevice d = getLocalDevice(localRow);
					boolean active = tModel.getValueAt(localRow, CheckListTable.COL_LOGS).toString().contains(LABELS.getString("debugSOCKET"));
					d.setDebugMode (LogMode.SOCKET, !active);
					try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e1) {}
					updateRow(d, localRow);
				}),
				new UsnaSelectedAction(this, table, "debugMQTT", localRow -> {
					ShellyAbstractDevice d = getLocalDevice(localRow);
					boolean active = tModel.getValueAt(localRow, CheckListTable.COL_LOGS).toString().contains(LABELS.getString("debugMQTT"));
					d.setDebugMode (LogMode.MQTT, !active);
					try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e1) {}
					updateRow(d, localRow);
				})
		});
		
		JButton logsButton = new JButton(); // logsG1Action or logsG2Action
		logsButton.setHorizontalTextPosition(SwingConstants.CENTER);
		logsButton.setVerticalTextPosition(SwingConstants.BOTTOM);

		Action bleAction = new UsnaSelectedAction(this, table, "setBLE_action", "setBLE_action_tooletip", null, "/images/Bluetooth24.png", localRow -> { // AbstractG2Device
			Object ble = tModel.getValueAt(localRow, CheckListTable.COL_BLE);
			AbstractG2Device d = (AbstractG2Device) getLocalDevice(localRow);
			d.setBLEMode(FALSE_STR.equals(ble));
			try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e1) {}
			updateRow(d, localRow);
		});

		Action apModeAction = new UsnaSelectedAction(this, table, "setAPMode_action", "setAPMode_action_tooletip", null, "/images/Rss24.png", localRow -> { // AbstractG2Device
			Object ap = tModel.getValueAt(localRow, CheckListTable.COL_AP);
			AbstractG2Device d = (AbstractG2Device) getLocalDevice(localRow);
			WIFIManagerG2.enableAP(d, !((ap instanceof Boolean && ap == Boolean.TRUE) || (ap instanceof String && TRUE_STR.equals(ap))));
			try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e1) {}
			updateRow(d, localRow);
		});
		
		Action roamingAction = new UsnaSelectedAction(this, table, "setRoaming_action", "setRoaming_action_tooletip", null, "/images/Roaming24.png", localRow -> {
			Object roam = tModel.getValueAt(localRow, CheckListTable.COL_ROAMING);
			ShellyAbstractDevice d = getLocalDevice(localRow);
			try {
				d.getWIFIManager(null).enableRoaming(FALSE_STR.equals(roam));
			} catch (IOException e) { }
			try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e1) {}
			updateRow(d, localRow);
		});

		Action rangeExtenderAction = new UsnaSelectedAction(this, table, "setExtender_action", "setExtender_action_tooletip", null, "/images/Extender24.png", localRow -> { // AbstractG2Device
			Object ext = tModel.getValueAt(localRow, CheckListTable.COL_EXTENDER);
			AbstractG2Device d = (AbstractG2Device) getLocalDevice(localRow);
			RangeExtenderManager.enable(d, FALSE_STR.equals(ext));
			try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e1) {}
			updateRow(d, localRow);
		});
		
		Action scriptsEditAction = new UsnaAction("col_scripts", e -> {
			DialogDeviceScripts w = new DialogDeviceScripts(CheckListView.this, appModel, devicesInd[table.getSelectedModelRow()]);
			final int localRow = table.getSelectedModelRow();
			w.addPropertyChangeListener("S_CLOSE", propertyChangeEvent -> {
				try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e1) {}
				tModel.setValueAt(DevicesTable.UPDATING_BULLET, localRow, CheckListTable.COL_STATUS);
				updateRow(getLocalDevice(localRow), localRow);
			});
		});

		Action rebootAction = new UsnaSelectedAction(this, table, "action_reboot_name", "action_reboot_tooltip", null, "/images/Nuke24.png", () -> {
			final String cancel = UIManager.getString("OptionPane.cancelButtonText");
			return JOptionPane.showOptionDialog(
					CheckListView.this, LABELS.getString("action_reboot_confirm"), LABELS.getString("action_reboot_tooltip"),
					JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
					new Object[] {LABELS.getString("action_reboot_name"), cancel}, cancel) == 0;
		}, modelRow -> {
			ShellyAbstractDevice d = getLocalDevice(modelRow);
			d.setStatus(Status.READING);
			tModel.setValueAt(DevicesTable.UPDATING_BULLET, modelRow, CheckListTable.COL_STATUS);
			SwingUtilities.invokeLater(() -> appModel.reboot(devicesInd[modelRow]));
		});

		Action browseAction = new UsnaSelectedAction(this, table, "action_web_name", "action_web_tooltip", null, "/images/Computer24.png", () -> {
			return table.getSelectedRowCount() <= 8 || JOptionPane.showConfirmDialog(this, LABELS.getString("action_web_confirm"), LABELS.getString("action_web_name"),
					JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION;
		}, i -> {
			try {
				Desktop.getDesktop().browse(URI.create("http://" + getLocalDevice(i).getAddressAndPort().getRepresentation()));
			} catch (IOException | UnsupportedOperationException e) {
				Msg.errorMsg(this, e);
			}
		});

		UsnaAction refreshAction = new UsnaAction(this, "labelRefresh", "labelRefresh", null, "/images/Refresh24.png");
		refreshAction.setActionListener(e -> {
			tModel.clear();
			refreshAction.setEnabled(false);
			fill();
			exeService.schedule(() -> refreshAction.setEnabled(true), 600, TimeUnit.MILLISECONDS);
		});

		Action helpAction = new UsnaAction(this, "helpBtnTooltip", "helpBtnTooltip", null, "/images/Question24.png", e -> {
			try {
				Desktop.getDesktop().browse(URI.create(LABELS.getString("dlgChecklistManualUrl")));
			} catch (IOException | UnsupportedOperationException ex) {
				Msg.errorMsg(this, ex);
			}
		});

		fill();

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBorder(BorderFactory.createEmptyBorder(2, 0, 1, 0));
		scrollPane.getViewport().setBackground(Main.BG_COLOR);
		scrollPane.setViewportView(table);
		contentPane.add(scrollPane, BorderLayout.CENTER);

		table.setRowHeight(table.getRowHeight() + 3);

		ListSelectionListener selListener = e -> {
			if(e.getValueIsAdjusting() == false) {
				Object val;
				int modelRow[] = table.getSelectedModelRows();
				if(modelRow.length > 0) {
					ecoModeAction.setEnabled(sameBooleanValues(modelRow, CheckListTable.COL_ECO, null));
					ledAction.setEnabled(sameBooleanValues(modelRow, CheckListTable.COL_LED, AbstractG1Device.class));
					if(sameBooleanValues(modelRow, CheckListTable.COL_LOGS, AbstractG1Device.class)) {
						logsButton.setAction(logsG1Action);
						logsG1Action.setEnabled(true);
					} else if(sameObjectValues(modelRow, CheckListTable.COL_LOGS)) {
						logsButton.setAction(logsG2Action);
						logsG2Action.setEnabled(true);
					} else {
						logsG1Action.setEnabled(false);
						logsG2Action.setEnabled(false);
					}
					bleAction.setEnabled(sameStringValuesOrInt(modelRow, CheckListTable.COL_BLE));
					apModeAction.setEnabled(sameBooleanValues(modelRow, CheckListTable.COL_AP, AbstractG2Device.class));
					roamingAction.setEnabled(sameStringValuesOrInt(modelRow, CheckListTable.COL_ROAMING));
					rangeExtenderAction.setEnabled(sameStringValuesOrInt(modelRow, CheckListTable.COL_EXTENDER));
					scriptsEditAction.setEnabled(modelRow.length == 1 && (val = tModel.getValueAt(modelRow[0], CheckListTable.COL_SCRIPTS)) != null && val.equals(NOT_APPLICABLE_STR) == false);
					browseAction.setEnabled(true);
					rebootAction.setEnabled(true);
				} else {
					ecoModeAction.setEnabled(false);
					ledAction.setEnabled(false);
					logsG1Action.setEnabled(false);
					logsG2Action.setEnabled(false);
					bleAction.setEnabled(false);
					apModeAction.setEnabled(false);
					roamingAction.setEnabled(false);
					rangeExtenderAction.setEnabled(false);
					scriptsEditAction.setEnabled(false);
					browseAction.setEnabled(false);
					rebootAction.setEnabled(false);
				}
			}
		};
		table.getSelectionModel().addListSelectionListener(selListener);
		selListener.valueChanged(new ListSelectionEvent(table.getSelectionModel(), -1, -1, false));

		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent evt) {
				if (evt.getClickCount() == 2 /*&& table.getSelectedRow() != -1*/) {
					browseAction.actionPerformed(null);
				} else {
					doPopup(evt);
				}
			}
			
			@Override
			public void mouseReleased(MouseEvent evt) {
				doPopup(evt);
			}
			
			private void doPopup(MouseEvent evt) {
				final int row;
				if (evt.isPopupTrigger() && (row = table.rowAtPoint(evt.getPoint())) >= 0) {
					if(table.isRowSelected(row) == false) {
						table.setRowSelectionInterval(row, row);
					}
					int col = table.convertColumnIndexToModel(table.columnAtPoint(evt.getPoint()));
					Action colAction = switch(col) {
					case CheckListTable.COL_ECO -> ecoModeAction;
					case CheckListTable.COL_LED -> ledAction;
					case CheckListTable.COL_LOGS -> logsButton.getAction();
					case CheckListTable.COL_BLE -> bleAction;
					case CheckListTable.COL_AP -> apModeAction;
					case CheckListTable.COL_ROAMING -> roamingAction;
					case CheckListTable.COL_WIFI1, CheckListTable.COL_WIFI2 -> new AbstractAction(LABELS.getString("edit") + " (" + tModel.getColumnName(col) + ")") {
						private static final long serialVersionUID = 1L;
						@Override
						public void actionPerformed(ActionEvent e) {
							final int[] modelRows =  table.getSelectedModelRows();
							int devIdx[] = IntStream.of(modelRows).map(i -> devicesInd[i]).toArray();
							DialogDeviceSettings w = new DialogDeviceSettings(CheckListView.this, appModel, devIdx, (col == CheckListTable.COL_WIFI1) ? DialogDeviceSettings.WIFI1 : DialogDeviceSettings.WIFI2);
							w.addPropertyChangeListener("S_APPLY", propertyChangeEvent -> {
								try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e1) {}
								for(int localRow: modelRows) {
									tModel.setValueAt(DevicesTable.UPDATING_BULLET, localRow, CheckListTable.COL_STATUS);
									updateRow(getLocalDevice(localRow), localRow);
								}
							});
						}
					};
					case CheckListTable.COL_EXTENDER -> rangeExtenderAction;
					case CheckListTable.COL_SCRIPTS -> scriptsEditAction;
					default -> null;
					};
					final UsnaPopupMenu tablePopup = new UsnaPopupMenu();
					if(colAction != null) {
						if(colAction instanceof UsnaDropdownAction dda && colAction.isEnabled()) {
							tablePopup.add(dda.getActions());
						} else {
							tablePopup.add(colAction);
						}
						tablePopup.add((Object)null); //separator
					}
					tablePopup.add(browseAction, rebootAction);
					tablePopup.show(table, evt.getX(), evt.getY());
				}
			}
		});
		
		toolBar.setBorder(BorderFactory.createEmptyBorder());
		toolBar.setBackground(Main.STATUS_LINE_COLOR);
		toolBar.add(refreshAction);
		toolBar.addSeparator();
		toolBar.add(ecoModeAction);
		toolBar.add(ledAction);
		toolBar.add(logsButton);
		toolBar.add(bleAction);
		toolBar.add(apModeAction);
		toolBar.add(roamingAction);
		toolBar.add(rangeExtenderAction);
		toolBar.addSeparator();
		toolBar.add(browseAction);
		toolBar.add(rebootAction);
		toolBar.add(Box.createHorizontalGlue());
		toolBar.add(helpAction);
		updateHideCaptions();
		contentPane.add(toolBar, BorderLayout.NORTH);

		JPanel panelBottom = new JPanel(new BorderLayout(0, 0));
		panelBottom.setBorder(BorderFactory.createEmptyBorder(2, 0, 3, 0));
		panelBottom.setBackground(Main.STATUS_LINE_COLOR);
		contentPane.add(panelBottom, BorderLayout.SOUTH);

		// Filter panel
		JPanel panelRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
		panelRight.setOpaque(false);
		panelBottom.add(panelRight, BorderLayout.EAST);

		panelRight.add(new JLabel(LABELS.getString("lblFilter")));
		JTextField textFieldFilter = new JTextField();
		textFieldFilter.setColumns(20);
		textFieldFilter.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		panelRight.add(textFieldFilter);
		textFieldFilter.getDocument().addDocumentListener((TextDocumentListener)e -> {
			TableRowSorter<?> sorter = (TableRowSorter<?>) table.getRowSorter();
			String filter = textFieldFilter.getText();
			sorter.setRowFilter(filter.isEmpty() ? null : RowFilter.regexFilter("(?i).*\\Q" + filter.replace("\\E", "\\e") + "\\E.*", new int[] {CheckListTable.COL_NAME, CheckListTable.COL_IP}));
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
		btnClose.setBorder(BorderFactory.createEmptyBorder(2, 7, 2, 8));
		panelRight.add(btnClose);

		setSize(980, 598);
		setVisible(true);
		setLocationRelativeTo(owner);
		table.columnsWidthAdapt();
		appModel.addListener(this);
		properties.addListener(this);
	}
	
	private boolean sameBooleanValues(int modelRows[], int col, Class<?> allowedDeviceClass) {
		Object val0 = tModel.getValueAt(modelRows[0], col);
		boolean ret = val0 instanceof Boolean && (allowedDeviceClass == null || allowedDeviceClass.isInstance(getLocalDevice(modelRows[0])));
		for(int i = 1; i < modelRows.length && ret; i++) {
			Object val = tModel.getValueAt(modelRows[i], col);
			ret = val instanceof Boolean && /*Objects.equals(val, val0)*/val.equals(val0) && (allowedDeviceClass == null || allowedDeviceClass.isInstance(getLocalDevice(modelRows[i])));
		}
		return ret;
	}
	
//	/** true if all values == FALSE_STR or all values != FALSE_STR but none is NOT_APPLICABLE_STR */
//	private boolean sameStringValues(int modelRows[], int col/*, Class<?> allowedDeviceClass*/) {
//		Object val0 = tModel.getValueAt(modelRows[0], col);
//		boolean ret = val0 instanceof String && val0.equals(NOT_APPLICABLE_STR) == false /*&& (allowedDeviceClass == null || allowedDeviceClass.isInstance(getLocalDevice(modelRows[0])))*/;
//		for(int i = 1; i < modelRows.length && ret; i++) {
//			Object val = tModel.getValueAt(modelRows[i], col);
//			ret = val instanceof String && ((val.equals(FALSE_STR) && val0.equals(FALSE_STR)) || (val.equals(FALSE_STR) == false && val0.equals(FALSE_STR) == false));
//					/*&& (allowedDeviceClass == null || allowedDeviceClass.isInstance(getLocalDevice(modelRows[i])));*/
//		}
//		return ret /*&& val0 != null*/;
//	}
	
	/** true if all values are Integers or all values == FALSE_STR or all values != FALSE_STR but none is NOT_APPLICABLE_STR */
	private boolean sameStringValuesOrInt(int modelRows[], int col) {
		Object val0 = tModel.getValueAt(modelRows[0], col);
		boolean ret = val0 instanceof Integer || val0 instanceof String && val0.equals(NOT_APPLICABLE_STR) == false;
		for(int i = 1; i < modelRows.length && ret; i++) {
			Object val = tModel.getValueAt(modelRows[i], col);
			ret = (val instanceof Integer && val0 instanceof Integer) || (val0 instanceof String && val instanceof String && ((val.equals(FALSE_STR) && val0.equals(FALSE_STR)) || (val.equals(NOT_APPLICABLE_STR) == false && val.equals(FALSE_STR) == false && val0.equals(FALSE_STR) == false)));
		}
		return ret;
	}
	
	/** true if all values are "equals" but none is NOT_APPLICABLE_STR (only AbstractG2Device)*/
	private boolean sameObjectValues(int modelRows[], int col) {
		Object val0 = tModel.getValueAt(modelRows[0], col);
		boolean ret = val0 != null && val0.equals(NOT_APPLICABLE_STR) == false;
		for(int i = 1; i < modelRows.length && ret; i++) {
			ret = val0.equals(tModel.getValueAt(modelRows[i], col)) && AbstractG2Device.class.isInstance(getLocalDevice(modelRows[i]));
		}
		return ret;
	}

	@Override
	public void dispose() {
		properties.removeListener(this);
		appModel.removeListener(this);
		super.dispose();
		exeService.shutdownNow();
	}

	private void fill() {
		if(exeService != null && exeService.isShutdown() == false) {
			exeService.shutdownNow();
		}
		exeService = Executors.newScheduledThreadPool(20);
		for (int devicesInd : devicesInd) {
			final ShellyAbstractDevice d = appModel.get(devicesInd);
			final Object[] tRow = new Object[CheckListTable.COL_LAST + 1];
			tRow[CheckListTable.COL_STATUS] = DevicesTable.UPDATING_BULLET;
			tRow[CheckListTable.COL_NAME] = UtilMiscellaneous.getExtendedHostName(d);
			tRow[CheckListTable.COL_IP] = d.getAddressAndPort();
			final int row = tModel.addRow(tRow);
			updateRow(d, row);
		}
	}
	
	private void updateHideCaptions() {
		boolean en = properties.getBoolProperty(ScannerProperties.PROP_TOOLBAR_CAPTIONS) == false;
		Stream.of(toolBar.getComponents()).filter(c -> c instanceof AbstractButton).forEach(b -> ((AbstractButton)b).setHideActionText(en));
	}

	private void updateRow(ShellyAbstractDevice d, int row) {
		Object[] tRow = tModel.getRow(row);
		exeService.execute(() -> {
			try {
				if (d instanceof AbstractG1Device g1) {
					g1Row(g1, d.getJSON("/settings"), tRow);
				} else if (d instanceof AbstractG2Device g2) { // G2-G3-...
					g2Row(g2, d.getJSON("/rpc/Shelly.GetConfig"), d.getJSON("/rpc/Shelly.GetStatus"), tRow);
				} else /*if (d instanceof AbstractBluDevice blu)*/ {
					//bluRow(blu, tRow);
					tRow[CheckListTable.COL_STATUS] = DevicesTable.getStatusIcon(d);
				}
			} catch (/*IO*/Exception e) {
				if (d instanceof BatteryDeviceInterface) {
					if (d instanceof AbstractG1Device g1) {
						g1Row(g1, ((BatteryDeviceInterface) d).getStoredJSON("/settings"), tRow);
					} else  if (d instanceof AbstractG2Device g2) {
						g2Row(g2, ((BatteryDeviceInterface) d).getStoredJSON("/rpc/Shelly.GetConfig"), null, tRow);
					}
				} else {
					tRow[CheckListTable.COL_STATUS] = DevicesTable.getStatusIcon(d);
					Arrays.fill(tRow, CheckListTable.COL_ECO, CheckListTable.COL_LAST, null);
				}
				if (d.getStatus() != Status.OFF_LINE && d.getStatus() != Status.NOT_LOOGGED) {
					LOG.error("{}", d, e);
				}
			}
			table.columnsWidthAdapt();
			final int i1 = table.getSelectionModel().getAnchorSelectionIndex();
			//table.getRowSorter().allRowsChanged();
			tModel.fireTableRowsUpdated(row, row);
			if(i1 >= 0) {
				table.getSelectionModel().setAnchorSelectionIndex(i1);
			}
		});
	}

	private static void g1Row(AbstractG1Device d, JsonNode settings, Object[] tRow) {
		Boolean eco = boolVal(settings.path("eco_mode_enabled"));
		Boolean ledOff = boolVal(settings.path("led_status_disable"));
		Object debug = d.getDebugMode() == LogMode.UNDEFINED ? "-" : d.getDebugMode() != LogMode.NONE;
		String roaming;
		if (settings.path("ap_roaming").isMissingNode()) {
			roaming = "-";
		} else if (settings.at("/ap_roaming/enabled").asBoolean()) {
			roaming = settings.at("/ap_roaming/threshold").asText();
		} else {
			roaming = FALSE_STR;
		}
		String wifi1;
		if (settings.at("/wifi_sta/enabled").asBoolean()) {
			wifi1 = "static".equals(settings.at("/wifi_sta/ipv4_method").asText()) ? TRUE_STR : FALSE_STR;
		} else {
			wifi1 = "-";
		}
		String wifi2;
		if (settings.at("/wifi_sta1/enabled").asBoolean()) {
			wifi2 = "static".equals(settings.at("/wifi_sta1/ipv4_method").asText()) ? TRUE_STR : FALSE_STR;
		} else {
			wifi2 = "-";
		}
		
		tRow[CheckListTable.COL_STATUS] = DevicesTable.getStatusIcon(d);
		tRow[CheckListTable.COL_ECO] = eco;
		tRow[CheckListTable.COL_LED] = ledOff;
		tRow[CheckListTable.COL_LOGS] = debug;
		tRow[CheckListTable.COL_BLE] = NOT_APPLICABLE_STR;
		tRow[CheckListTable.COL_AP] = NOT_APPLICABLE_STR;
		tRow[CheckListTable.COL_ROAMING] = roaming;
		tRow[CheckListTable.COL_WIFI1] = wifi1;
		tRow[CheckListTable.COL_WIFI2] = wifi2;
		tRow[CheckListTable.COL_EXTENDER] = NOT_APPLICABLE_STR;
		tRow[CheckListTable.COL_SCRIPTS] = NOT_APPLICABLE_STR;
	}

	private static void g2Row(AbstractG2Device d, JsonNode config, JsonNode status, Object[] tRow) {
		Boolean eco = boolVal(config.at("/sys/device/eco_mode"));
		Object ap = boolVal(config.at("/wifi/ap/enable"));
		if (ap != null && ap == Boolean.TRUE && config.at("/wifi/ap/is_open").asBoolean(true) == false) {
			ap = TRUE_STR; // AP active but protected with pwd
		}
		ArrayList<LogMode> logModes = new ArrayList<>();
		if(config.at("/sys/debug/websocket/enable").booleanValue()) {
			logModes.add(LogMode.SOCKET);
		}
		if(config.at("/sys/debug/mqtt/enable").booleanValue()) {
			logModes.add(LogMode.MQTT);
		}
		if(config.at("/sys/debug/udp").hasNonNull("addr")) {
			logModes.add(LogMode.UDP);
		}
		Object debug = (logModes.size() == 0) ? Boolean.FALSE : logModes.stream().map(log -> LABELS.getString("debug" + log.name())).collect(Collectors.joining(", "));
		Object ble;
		JsonNode bleEnableNode = config.at("/ble/enable");
		if(bleEnableNode.isMissingNode()) {
			ble = NOT_APPLICABLE_STR;
		} else if(bleEnableNode.asBoolean()) {
			try {
				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
//				ble = d.getJSON("/rpc/BLE.CloudRelay.ListInfos").get("total").asText(); // fw >= 1.5.0
				ble = d.getJSON("/rpc/BLE.CloudRelay.List").get("addrs").size();
			} catch (/*IO*/Exception e) {
//				if (config.at("/ble/observer/enable").booleanValue()) { // fw < 1.5.0
//					ble = "OBS"; // used as observer
//				} else {
					ble = TRUE_STR;
//				}
			}
		} else {
			ble = FALSE_STR;
		}
		String roaming;
		if (config.at("/wifi/roam").isMissingNode()) {
			roaming = "-";
		} else if (config.at("/wifi/roam/interval").asInt() > 0) {
			roaming = config.at("/wifi/roam/rssi_thr").asText();
		} else {
			roaming = FALSE_STR;
		}
		String wifi1;
		if (config.at("/wifi/sta/enable").asBoolean()) {
			wifi1 = "static".equals(config.at("/wifi/sta/ipv4mode").asText()) ? TRUE_STR : FALSE_STR;
		} else {
			wifi1 = "-";
		}
		String wifi2;
		if (config.at("/wifi/sta1/enable").asBoolean()) {
			wifi2 = "static".equals(config.at("/wifi/sta1/ipv4mode").asText()) ? TRUE_STR : FALSE_STR;
		} else {
			wifi2 = "-";
		}
		Object extender;
		JsonNode extenderEnabled = config.at("/wifi/ap/range_extender/enable");
		if(extenderEnabled.isMissingNode()) {
			extender = NOT_APPLICABLE_STR;
		} else {
			extender = (status == null || extenderEnabled.asBoolean() == false) ? FALSE_STR : status.at("/wifi/ap_client_count").asInt();
		}
		String scripts = NOT_APPLICABLE_STR;
		if (d instanceof BatteryDeviceInterface == false) {
			Status oldStatus = d.getStatus();
			try {
				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
				List<Script> sList = Script.list(d);
				scripts = sList.size() + " / " + sList.stream().filter(Script::isEnabled).count();
			} catch (Exception e) {
				d.setStatus(oldStatus); // restore the old status; now is probably Status.ERROR (some devices do not support scripts)
				LOG.debug("scripts: {}", d, e);
			}
		}
		
		tRow[CheckListTable.COL_STATUS] = DevicesTable.getStatusIcon(d);
		tRow[CheckListTable.COL_ECO] = eco;
		tRow[CheckListTable.COL_LED] = NOT_APPLICABLE_STR;
		tRow[CheckListTable.COL_LOGS] = debug;
		tRow[CheckListTable.COL_BLE] = ble;
		tRow[CheckListTable.COL_AP] = ap;
		tRow[CheckListTable.COL_ROAMING] = roaming;
		tRow[CheckListTable.COL_WIFI1] = wifi1;
		tRow[CheckListTable.COL_WIFI2] = wifi2;
		tRow[CheckListTable.COL_EXTENDER] = extender;
		tRow[CheckListTable.COL_SCRIPTS] = scripts;
	}
	
//	private static void bluRow(AbstractBluDevice d, Object[] tRow) {
//	}

	private static Boolean boolVal(JsonNode node) {
		return node.isMissingNode() ? null : node.asBoolean();
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
				final int localRow = getLocalIndex(pos);
				if (localRow >= 0 /*&& tModel.getValueAt(index, COL_STATUS) !=  DevicesTable.UPDATING_BULLET*/) {
					ShellyAbstractDevice device = appModel.get(pos);
					ImageIcon oldStatusIcon = (ImageIcon)tModel.getValueAt(localRow, CheckListTable.COL_STATUS);
					ImageIcon newStatusIcon = DevicesTable.getStatusIcon(device);
					if(oldStatusIcon != newStatusIcon) {
						final int i1 = table.getSelectionModel().getAnchorSelectionIndex();
						tModel.setValueAt(newStatusIcon, localRow, CheckListTable.COL_STATUS);
						if(device.getStatus() == Status.ON_LINE && oldStatusIcon.getImage() != newStatusIcon.getImage()) { // was not ON_LINE; now is
							updateRow(device, localRow);
						}
						table.getSelectionModel().setAnchorSelectionIndex(i1);
//						table.getRowSorter().allRowsChanged();
					}
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
}

// g1 "factory_reset_from_switch" : true, "pon_wifi_reset" : false,