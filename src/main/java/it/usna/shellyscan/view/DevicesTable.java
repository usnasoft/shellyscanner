package it.usna.shellyscan.view;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.ImageIcon;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.model.device.GhostDevice;
import it.usna.shellyscan.model.device.InternalTmpHolder;
import it.usna.shellyscan.model.device.LabelHolder;
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.ModulesHolder;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.ShellyAbstractDevice.Status;
import it.usna.shellyscan.model.device.blu.AbstractBluDevice;
import it.usna.shellyscan.model.device.g1.ShellyDW;
import it.usna.shellyscan.model.device.g1.ShellyFlood;
import it.usna.shellyscan.model.device.g1.ShellyTRV;
import it.usna.shellyscan.model.device.g1.modules.ThermostatG1;
import it.usna.shellyscan.model.device.g2.ShellyPlusSmoke;
import it.usna.shellyscan.model.device.modules.DeviceModule;
import it.usna.shellyscan.view.util.ScannerProperties;
import it.usna.shellyscan.view.util.UtilMiscellaneous;
import it.usna.swing.ArrayTableCellRenderer;
import it.usna.swing.DecimalTableCellRenderer;
import it.usna.swing.table.ExTooltipTable;
import it.usna.swing.table.UsnaTableModel;
import it.usna.util.AppProperties;

public class DevicesTable extends ExTooltipTable {
	private static final long serialVersionUID = 1L;
	private static final Image OFFLINEIMG =Toolkit.getDefaultToolkit().getImage(DevicesTable.class.getResource("/images/bullet_stop.png"));
	private static final Image GHOSTIMG = Toolkit.getDefaultToolkit().getImage(DevicesTable.class.getResource("/images/bullet_ghost.png"));
	private static final Image BLUIMG = Toolkit.getDefaultToolkit().getImage(DevicesTable.class.getResource("/images/bullet_bluetooth.png"));
	public static final ImageIcon ONLINE_BULLET = new ImageIcon(DevicesTable.class.getResource("/images/bullet_yes.png"), LABELS.getString("labelDevOnLIne"));
	public static final ImageIcon ONLINE_BULLET_REBOOT = new ImageIcon(DevicesTable.class.getResource("/images/bullet_yes_reboot.png"), LABELS.getString("labelDevOnLIneReboot"));
	public static final ImageIcon OFFLINE_BULLET = new ImageIcon(OFFLINEIMG, LABELS.getString("labelDevOffLIne"));
	public static final ImageIcon LOGIN_BULLET = new ImageIcon(DevicesTable.class.getResource("/images/bullet_star_yellow.png"), LABELS.getString("labelDevNotLogged"));
	public static final ImageIcon UPDATING_BULLET = new ImageIcon(DevicesTable.class.getResource("/images/bullet_refresh.png"), LABELS.getString("labelDevUpdating"));
	public static final ImageIcon ERROR_BULLET = new ImageIcon(DevicesTable.class.getResource("/images/bullet_error.png"), LABELS.getString("labelDevError"));
	private static final String TRUE = LABELS.getString("true_yn");
	private static final String FALSE = LABELS.getString("false_yn");
	private static final String YES = LABELS.getString("true_yna");
	private static final String NO = LABELS.getString("false_yna");
	
	// model columns indexes
	public static final int COL_STATUS_IDX = 0;
	static final int COL_TYPE = 1;
	static final int COL_DEVICE = 2;
	static final int COL_NAME = 3;
	static final int COL_KEYWORD = 4;
	static final int COL_MAC_IDX = 5;
	static final int COL_IP_IDX = 6;
	static final int COL_SSID_IDX = 7;
	static final int COL_RSSI_IDX = 8;
	static final int COL_CLOUD = 9;
	static final int COL_MQTT = 10;
	static final int COL_UPTIME_IDX = 11;
	static final int COL_INT_TEMP = 12;
	static final int COL_MEASURES_IDX = 13;
	static final int COL_DEBUG = 14;
	static final int COL_SOURCE_IDX = 15;
	static final int COL_COMMAND_IDX = 16;
	
	public static final String STORE_PREFIX = "TAB";
	public static final String STORE_EXT_PREFIX = "TAB_EXT";
	
	private UptimeCellRenderer uptimeRenderer = new UptimeCellRenderer();

	private boolean adaptTooltipLocation = false;
	
	private boolean tempUnitCelsius;
	
	private static final Logger LOG = LoggerFactory.getLogger(DevicesTable.class);

	public DevicesTable(TableModel tm) {
		super(tm, true);
		tempUnitCelsius = ScannerProperties.instance().getProperty(ScannerProperties.PROP_TEMP_UNIT).equals("C");
		columnModel.getColumn(COL_STATUS_IDX).setMaxWidth(ONLINE_BULLET.getIconWidth() + 2);
		columnModel.getColumn(COL_STATUS_IDX).setMinWidth(ONLINE_BULLET.getIconWidth() + 2);
		columnModel.getColumn(COL_MEASURES_IDX).setCellRenderer(new DeviceMetersCellRenderer(tempUnitCelsius));
		columnModel.getColumn(COL_INT_TEMP).setCellRenderer(tempUnitCelsius ? new DecimalTableCellRenderer(2) : new FahrenheitTableCellRenderer());
		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment(DefaultTableCellRenderer.CENTER);
		columnModel.getColumn(COL_CLOUD).setCellRenderer(centerRenderer);
		columnModel.getColumn(COL_MQTT).setCellRenderer(centerRenderer);
		columnModel.getColumn(COL_UPTIME_IDX).setCellRenderer(uptimeRenderer);
		columnModel.getColumn(COL_DEBUG).setCellRenderer(centerRenderer);
		columnModel.getColumn(COL_SOURCE_IDX).setCellRenderer(new ArrayTableCellRenderer());
		final TableColumn colCommand = columnModel.getColumn(COL_COMMAND_IDX);
		colCommand.setCellRenderer(new DevicesCommandCellRenderer(tempUnitCelsius));
		colCommand.setCellEditor(new DevicesCommandCellEditor(this, tempUnitCelsius));

		TableRowSorter<?> sorter = (TableRowSorter<?>)getRowSorter();

		sorter.setComparator(COL_COMMAND_IDX, (o1, o2) -> {
			final String s1, s2;
			if(o1 == null) {
				s1 = null;
			} else if (o1 instanceof DeviceModule[] dmArray) {
				s1 = dmArray[0].getLabel();
			} else if (o1 instanceof DeviceModule dm) {
				s1 = dm.getLabel();
			} else {
				s1 = o1.toString();
			}
			if(o2 == null) {
				s2 = null;
			} else if (o2 instanceof DeviceModule[] dmArray) {
				s2 = dmArray[0].getLabel();
			} else if (o2 instanceof DeviceModule dm) {
				s2 = dm.getLabel();
			} else {
				s2 = o2.toString();
			}
			if(s1 == null) {
				return (s2 == null) ? 0 : -1;
			}
			if(s2 == null) {
				return 1;
			}
			return s1.compareToIgnoreCase(s2);
		});
		
		sorter.setComparator(COL_MEASURES_IDX, (Meters[] o1, Meters[] o2) -> {
			if(o1 == null) {
				return -1;
			}
			if(o2 == null) {
				return 1;
			}
//			return ((Comparable<Object>[])o1)[0].compareTo(((Comparable<Object>[])o2)[0]);
			return (o1[0]).compareTo(o2[0]);
		});
		
		sorter.setComparator(COL_SOURCE_IDX, (o1, o2) -> {
			String s1 = o1 instanceof String[] arr ? arr[0] : (String)o1;
			String s2 = o2 instanceof String[] arr ? arr[0] : (String)o2;
			if(s1 == null) {
				return (s2 == null) ? 0 : -1;
			}
			if(s2 == null) {
				return 1;
			}
			return s1.compareToIgnoreCase(s2);
		});
		
		activateSingleCellStringCopy();
		
		String[] headerTips = new String[COL_MQTT + 1];
		headerTips[COL_STATUS_IDX] = LABELS.getString("col_status_exp");
		headerTips[COL_CLOUD] = LABELS.getString("col_cloud_exp");
		headerTips[COL_MQTT] = LABELS.getString("col_mqtt_exp");
		setHeadersTooltip(headerTips);

		// sorter.setSortsOnUpdates(true); // messes rows heights and selection
        // setUpdateSelectionOnSort(true); // default
	}

	@Override
	public boolean isCellEditable(final int row, final int column) {
//		Object val = getValueAt(row, column); return val instanceof DeviceModule || val instanceof DeviceModule[];
		return convertColumnIndexToModel(column) == COL_COMMAND_IDX;
	}

	@Override
	public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
		Component comp = super.prepareRenderer(renderer, row, column);
		if(isRowSelected(row)) {
			comp.setBackground(getSelectionBackground());
		} else {
			comp.setBackground((row % 2 == 0) ? Main.TAB_LINE1_COLOR : Main.TAB_LINE2_COLOR);
		}
		computeRowHeight(row, comp);
		return comp;
	}
	
	public void setUptimeRenderMode(String mode) {
		uptimeRenderer.setMode(mode);
		((UsnaTableModel)dataModel).fireTableDataChanged();
		columnsWidthAdapt();
	}
	
	public void setTempRenderMode(boolean celsius) {
		this.tempUnitCelsius = celsius;
		columnModel.getColumn(convertColumnIndexToView(COL_INT_TEMP)).setCellRenderer(celsius ? new DecimalTableCellRenderer(2) : new FahrenheitTableCellRenderer());
		((DeviceMetersCellRenderer)columnModel.getColumn(convertColumnIndexToView(COL_MEASURES_IDX)).getCellRenderer()).setTempUnit(celsius);
		((DevicesCommandCellRenderer)columnModel.getColumn(convertColumnIndexToView(COL_COMMAND_IDX)).getCellRenderer()).setTempUnit(celsius);
		((DevicesCommandCellEditor)columnModel.getColumn(convertColumnIndexToView(COL_COMMAND_IDX)).getCellEditor()).setTempUnit(celsius);
		((UsnaTableModel)dataModel).fireTableDataChanged();
	}

	@Override
	protected String getToolTipText(Object value, boolean cellTooSmall, int r, int c) {
		if(value != null) {
			final int modelCol = convertColumnIndexToModel(c);
			if(modelCol == COL_UPTIME_IDX) {
				adaptTooltipLocation = false;
				long s = ((Number)value).longValue();
				final int gg = (int)(s / (3600 * 24));
				s = s % (3600 * 24);
				int hh = (int)(s / 3600);
				s = s % 3600;
				int mm = (int)(s / 60);
				s = s % 60;
				LocalDateTime since = LocalDateTime.now().minusSeconds(((Number)value).longValue());
				return String.format(LABELS.getString("col_uptime_tooltip"), gg, hh, mm, s, since);
			} else if (value instanceof ImageIcon icon) {
				adaptTooltipLocation = false;
				return icon.getDescription();
			} else if(value instanceof ThermostatG1 therm) { // TRV G1
				adaptTooltipLocation = false;
				return String.format(Locale.ENGLISH, LABELS.getString("col_command_therm_tooltip"), therm.getCurrentProfile(), therm.getTargetTemp(), therm.getPosition());
			} else if(value instanceof Meters[] meters) {
				if(Arrays.stream(meters).anyMatch(m -> DeviceMetersCellRenderer.hasHiddenMeasures(m) || m instanceof LabelHolder || m.hasNames()) || cellTooSmall) {
					adaptTooltipLocation = true;
					String tt = "<html><table border='0' cellspacing='0' cellpadding='0'>";
					boolean labelHolder = false;
					for(Meters m: meters) {
						tt += "<tr>";
						if(m instanceof LabelHolder) {
							tt += "<td><b>" + ((LabelHolder)m).getLabel() + "</b>&nbsp;</td>";
							labelHolder = true;
						} else if(labelHolder) { // skip first cell for alignment
							tt += "<td></td>";
						}
						for(Meters.Type t: m.getTypes()) {
							final String name = m.getName(t);
							final String tLabel = (name != null && name.isEmpty() == false) ? " (" + name + ")": "";
							if(t == Meters.Type.T || t == Meters.Type.T1 || t == Meters.Type.T2 || t == Meters.Type.T3 || t == Meters.Type.T4) {
								if(tempUnitCelsius) {
									tt += "<td><i>" + LABELS.getString("METER_LBL_" + t) + tLabel + "</i>&nbsp;</td><td align='right'>" + String.format(Locale.ENGLISH, LABELS.getString("METER_VAL_T"), m.getValue(t)) + "&nbsp;</td>";
								} else { // fahrenheit 
									tt += "<td><i>" + LABELS.getString("METER_LBL_" + t) + tLabel + "</i>&nbsp;</td><td align='right'>" + String.format(Locale.ENGLISH, LABELS.getString("METER_VAL_T_F"), m.getValue(t) * 1.8f + 32f) + "&nbsp;</td>";
								}
							} else if(t == Meters.Type.EX) {
								tt += "<td><i>" + LABELS.getString("METER_LBL_" + t) + tLabel + "</i>&nbsp;</td><td align='right'>" + LABELS.getString((m.getValue(t) == 0f) ? "METER_VAL_EX_0" : "METER_VAL_EX_1") + "&nbsp;</td>";
							} else {
								tt += "<td><i>" + LABELS.getString("METER_LBL_" + t) + tLabel + "</i>&nbsp;</td><td align='right'>" + String.format(Locale.ENGLISH, LABELS.getString("METER_VAL_" + t), m.getValue(t)) + "&nbsp;</td>";
							}
						}
						tt += "</tr>";
					}
					return tt + "</table></html>";
				} // else return null;
			} else {
				adaptTooltipLocation = true;
				return super.getToolTipText(value, cellTooSmall, r, c);
			}
		}
		return null;
	}
	
	@Override
	public String cellValueAsString(Object value, int row, int column) {
		if(value != null) {
			final int modelCol = convertColumnIndexToModel(column);
			if(modelCol == COL_INT_TEMP && tempUnitCelsius == false) {
				return UtilMiscellaneous.celsiusToFahrenheit((Float)value);
			} else if(modelCol == COL_MEASURES_IDX) {
				String ret = "";
				for(Meters m: (Meters[])value) {
					if(m instanceof LabelHolder lh) {
						ret += lh.getLabel() + " ";
					}
					for(Meters.Type t: m.getTypes()) {
						final String name = m.getName(t);
						final String tLabel = (name != null && name.isEmpty() == false) ? " (" + name + ")": "";
						if(t == Meters.Type.T || t == Meters.Type.T1 || t == Meters.Type.T2 || t == Meters.Type.T3 || t == Meters.Type.T4) {
							if(tempUnitCelsius) {
								ret += LABELS.getString("METER_LBL_" + t) + tLabel + " " + String.format(Locale.ENGLISH, LABELS.getString("METER_VAL_T"), m.getValue(t)) + " ";
							} else { // fahrenheit 
								ret += LABELS.getString("METER_LBL_" + t) + tLabel + " " + String.format(Locale.ENGLISH, LABELS.getString("METER_VAL_T_F"), m.getValue(t) * 1.8f + 32f) + " ";
							}
						} else if(t.isBoolean()) {
							ret += LABELS.getString("METER_LBL_" + t) + tLabel + " " + LABELS.getString((m.getValue(t) == 0f) ? "METER_VAL_" + t + "_0" : "METER_VAL_" + t + "_NOT0") + " ";
						} else {
							ret += LABELS.getString("METER_LBL_" + t) + tLabel + " " + String.format(Locale.ENGLISH, LABELS.getString("METER_VAL_" + t), m.getValue(t)) + " ";
						}
					}
					ret += "+ ";
				}
				return ret.replaceAll("[ +]+$", "");
			} else if(modelCol == COL_COMMAND_IDX && value instanceof DeviceModule[] dm) {
				return Stream.of(dm).filter(d -> d != null).map(d -> d.getLabel()).filter(label -> label != null && label.isEmpty() == false).collect(Collectors.joining(" + "));
			} else if(value instanceof Object[]) {
				return Stream.of((Object[])value).filter(v -> v != null).map(v -> v.toString()).collect(Collectors.joining(" + "));
			} else {
				return value.toString();
			}
		}
		return "";
	}

	@Override
	public Point getToolTipLocation(final MouseEvent evt) {
		return (adaptTooltipLocation) ? super.getToolTipLocation(evt) : null;
	}
	
	public void loadColPos(final AppProperties appProp) {
		if(loadColPos(appProp, STORE_PREFIX) == false) { // no configuration -> default
			hideColumn(COL_KEYWORD);
			hideColumn(COL_MAC_IDX);
			hideColumn(COL_SSID_IDX);
			hideColumn(COL_DEBUG);
		}
	}

	@Override
	public void columnsWidthAdapt() {
		final FontMetrics fm = getFontMetrics(getFont());
		final int columnCount = getColumnCount();
		final int rowCount = getRowCount();
		for (int c = 0; c < columnCount; c++) {
			int modelCol = convertColumnIndexToModel(c);
			if (modelCol != COL_STATUS_IDX) { // COL_STATUS_IDX has fixed width
				TableColumn tc = columnModel.getColumn(c);
				if (modelCol == COL_UPTIME_IDX) {
					tc.setPreferredWidth(uptimeRenderer.getPreferredWidth(fm));
				} else {
					int width = SwingUtilities.computeStringWidth(fm, tc.getHeaderValue().toString()) >> 1;
					for (int r = 0; r < rowCount; r++) {
						Object val = getValueAt(r, c);
						if (val != null) {
							if (val instanceof Object[] arr) {
								for (Object v : arr) {
									if (v != null) {
										width = Math.max(width, SwingUtilities.computeStringWidth(fm, v.toString()));
									}
								}
							} else {
								width = Math.max(width, SwingUtilities.computeStringWidth(fm, val.toString()));
							}
						}
					}
					tc.setPreferredWidth(width);
				}
			}
		}
	}

	public void setRowFilter(String filter, int ... cols) {
		TableRowSorter<?> sorter = (TableRowSorter<?>)getRowSorter();
		if(filter.isEmpty()) {
			sorter.setRowFilter(null);
		} else {
			RowFilter<TableModel, Integer> regexFilter = RowFilter.regexFilter("(?i).*\\Q" + filter.replace("\\E", "\\e") + "\\E.*", cols);
			sorter.setRowFilter(regexFilter);
//			ArrayList<RowFilter<TableModel, Integer>> filters = new ArrayList<>();
//			filters.add(regexFilter);
//			if(cols.length > 1) {
//				filters.add(new RowFilter<TableModel, Integer>() {
//					@Override
//					public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
//						Object val = entry.getValue(COL_COMMAND_IDX);
//						if(val instanceof String) {
//							return ((String)val).toUpperCase().contains(filter.toUpperCase());
//						} else if(val instanceof LabelHolder) {
//							return ((LabelHolder)val).getLabel().toUpperCase().contains(filter.toUpperCase());
//						} else if(val instanceof LabelHolder[]) {
//							for(LabelHolder lh: (LabelHolder[])val) {
//								if(lh.getLabel().toUpperCase().contains(filter.toUpperCase())) {
//									return true;
//								}
//							}
//							return false;
//						}
//						return false;
//					}
//				});
//			}
//			sorter.setRowFilter(RowFilter.orFilter(filters));
		}
	}

	public void resetRowsComputedHeight() {
		for(int i = 0; i < getRowCount(); i++) {
			setRowHeight(i, 1);
		}
	}
	
	public void resetRowComputedHeight(int modelIndex) {
		setRowHeight(convertRowIndexToView(modelIndex), ONLINE_BULLET.getIconHeight());
	}

	// adapt row height
	private void computeRowHeight(int rowIndex, Component callVal) {
		int thisH = callVal.getPreferredSize().height;
		if(getRowHeight(rowIndex) < thisH) {
			setRowHeight(rowIndex, thisH);
		}
	}
	
//	public void setDevicesModel() {
//		// todo
//	}
	
	public void addRow(ShellyAbstractDevice device, GhostDevice ghost) {
		((UsnaTableModel)dataModel).addRow(generateRow(device, ghost, new Object[DevicesTable.COL_COMMAND_IDX + 1]));
		columnsWidthAdapt();
		getRowSorter().allRowsChanged();
	}
	
	public void updateRow(ShellyAbstractDevice device, GhostDevice ghost, int modelIndex) {
		generateRow(device, ghost, ((UsnaTableModel)dataModel).getRow(modelIndex));
		((UsnaTableModel)dataModel).fireTableRowsUpdated(modelIndex, modelIndex);
		final int i1 = selectionModel.getAnchorSelectionIndex(); // getRowSorter().allRowsChanged() do not preserve the selected cell; this mess the selection dragging the mouse
//		final int i2 = lsm.getLeadSelectionIndex();
		getRowSorter().allRowsChanged();
		selectionModel.setAnchorSelectionIndex(i1);
//		lsm.setLeadSelectionIndex(i2);
	}
	
	private static Object[] generateRow(ShellyAbstractDevice d, GhostDevice g, final Object row[]) {
		try {
			row[DevicesTable.COL_STATUS_IDX] = getStatusIcon(d);
			row[DevicesTable.COL_TYPE] = d.getTypeName();
			row[DevicesTable.COL_DEVICE] = d.getHostname();
			row[DevicesTable.COL_NAME] = d.getName();
			row[DevicesTable.COL_KEYWORD] = g.getKeyNote();
			row[DevicesTable.COL_MAC_IDX] = d.getMacAddress();
			row[DevicesTable.COL_IP_IDX] = d.getAddressAndPort();
			row[DevicesTable.COL_SSID_IDX] = d.getSSID();
			Status status = d.getStatus();
			if(status != Status.NOT_LOOGGED && status != Status.ERROR && status != Status.GHOST /*&&(d instanceof ShellyUnmanagedDevice == false || ((ShellyUnmanagedDevice)d).geException() == null)*/) {
				row[DevicesTable.COL_RSSI_IDX] = d.getRssi();
				if(d instanceof AbstractBluDevice == false) {
					row[DevicesTable.COL_CLOUD] = (d.getCloudEnabled() ? TRUE : FALSE) + " " + (d.getCloudConnected() ? TRUE : FALSE);
					row[DevicesTable.COL_MQTT] = (d.getMQTTEnabled() ? TRUE : FALSE) + " " + (d.getMQTTConnected() ? TRUE : FALSE);
				}
				int uptime = d.getUptime();
				if(uptime >= 0) {
					row[DevicesTable.COL_UPTIME_IDX] = uptime;
				}
				row[DevicesTable.COL_INT_TEMP] = (d instanceof InternalTmpHolder) ? ((InternalTmpHolder)d).getInternalTmp() : null;
				row[DevicesTable.COL_MEASURES_IDX] = d.getMeters();
				row[DevicesTable.COL_DEBUG] = LABELS.getString("debug" + d.getDebugMode().name());
				DeviceModule[] command = null;
				if(d instanceof ModulesHolder mh && mh.getModulesCount() > 0) {
					row[DevicesTable.COL_COMMAND_IDX] = command = mh.getModules();
				} else if(d instanceof ShellyDW dw) {
					row[DevicesTable.COL_COMMAND_IDX] = LABELS.getString("labelStatusOpen") + ": " + (dw.isOpen() ? YES : NO);
				} else if(d instanceof ShellyFlood flood) {
					row[DevicesTable.COL_COMMAND_IDX] = LABELS.getString("labelStatusFlood") + ": " + (flood.flood() ? YES : NO);
				} else if(d instanceof ShellyPlusSmoke smoke) {
					row[DevicesTable.COL_COMMAND_IDX] = String.format(LABELS.getString("labelStatusSmoke"), smoke.getAlarm() ? YES : NO);
				} else if(d instanceof ShellyTRV trv) { // very specific
					ThermostatG1 thermostat = trv.getThermostat();
					if(thermostat.isEnabled()) {
						row[DevicesTable.COL_COMMAND_IDX] = thermostat;
					} else {
						row[DevicesTable.COL_COMMAND_IDX] = String.format(LABELS.getString("labelStatusTRV"), thermostat.getPosition());
					}
				}
				if(command != null) {
					if(command.length == 1) {
						row[DevicesTable.COL_SOURCE_IDX] = command[0].getLastSource();
					} else {
						if(row[DevicesTable.COL_SOURCE_IDX] instanceof String[] res && res.length == command.length) {
							for(int i = 0; i < command.length; i++) {
								res[i] = command[i].getLastSource();
							}
						} else {
							String res[] = new String[command.length]; // Arrays.setAll(res, i -> m[i].getLastSource()); // slower for 2 elements
							for(int i = 0; i < command.length; i++) {
								res[i] = command[i].getLastSource();
							}
							row[DevicesTable.COL_SOURCE_IDX] = res;
						}
					}
				}
			} else {
				row[DevicesTable.COL_RSSI_IDX] = row[DevicesTable.COL_CLOUD] = row[DevicesTable.COL_UPTIME_IDX] = row[DevicesTable.COL_INT_TEMP] =
						row[DevicesTable.COL_MEASURES_IDX] = row[DevicesTable.COL_DEBUG] = row[DevicesTable.COL_SOURCE_IDX] = row[DevicesTable.COL_COMMAND_IDX] = null;
			}
		} catch(Exception e) {
			LOG.error("", e);
		}
		return row;
	}
	
	public static ImageIcon getStatusIcon(ShellyAbstractDevice d) {
		if(d.getStatus() == Status.ON_LINE) {
			if(d instanceof AbstractBluDevice) {
				return new ImageIcon(BLUIMG, String.format(LABELS.getString("labelDevOnLIneBTHome"), LocalDateTime.ofInstant(Instant.ofEpochMilli(d.getLastTime()), ZoneId.systemDefault())));
			} else {
				return d.rebootRequired() ? ONLINE_BULLET_REBOOT : ONLINE_BULLET;
			}
		} else if(d.getStatus() == Status.OFF_LINE) {
			long lastOnline = d.getLastTime();
			if(lastOnline > 0) {
				return new ImageIcon(OFFLINEIMG, String.format(LABELS.getString("labelDevOffLIneTime"), LocalDateTime.ofInstant(Instant.ofEpochMilli(lastOnline), ZoneId.systemDefault())));
			} else {
				return OFFLINE_BULLET;
			}
		} else if(d.getStatus() == Status.READING) {
			return UPDATING_BULLET;
		} else if(d.getStatus() == Status.GHOST) {
			return new ImageIcon(GHOSTIMG, String.format(LABELS.getString("labelDevGhostTime"), LocalDateTime.ofInstant(Instant.ofEpochMilli(d.getLastTime()), ZoneId.systemDefault())));
		} else if(d.getStatus() == Status.ERROR) {
			return ERROR_BULLET;
		} else { // Status.NOT_LOOGGED
			return LOGIN_BULLET;
		}
	}
} // 462 - 472 - 513 - 505 - 518 - 499 - 507