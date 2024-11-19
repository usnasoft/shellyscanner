package it.usna.shellyscan.view;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
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
import it.usna.shellyscan.model.device.MotionSensor;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.ShellyAbstractDevice.Status;
import it.usna.shellyscan.model.device.blu.AbstractBluDevice;
import it.usna.shellyscan.model.device.g1.ShellyDW;
import it.usna.shellyscan.model.device.g1.ShellyFlood;
import it.usna.shellyscan.model.device.g1.ShellyTRV;
import it.usna.shellyscan.model.device.g1.modules.ThermostatG1;
import it.usna.shellyscan.model.device.g2.ShellyPlusSmoke;
import it.usna.shellyscan.model.device.modules.DeviceModule;
import it.usna.swing.ArrayTableCellRenderer;
import it.usna.swing.DecimalTableCellRenderer;
import it.usna.swing.table.ExTooltipTable;
import it.usna.swing.table.UsnaTableModel;
import it.usna.util.AppProperties;

public class DevicesTable extends ExTooltipTable {
	private static final long serialVersionUID = 1L;
	private final static URL OFFLINEIMG = MainView.class.getResource("/images/bullet_stop.png");
	private final static URL GHOSTIMG = MainView.class.getResource("/images/bullet_ghost.png");
	private final static URL BTHOMEIMG = MainView.class.getResource("/images/bullet_bluetooth.png");
	public final static ImageIcon ONLINE_BULLET = new ImageIcon(MainView.class.getResource("/images/bullet_yes.png"), LABELS.getString("labelDevOnLIne"));
	public final static ImageIcon ONLINE_BULLET_REBOOT = new ImageIcon(MainView.class.getResource("/images/bullet_yes_reboot.png"), LABELS.getString("labelDevOnLIneReboot"));
	public final static ImageIcon OFFLINE_BULLET = new ImageIcon(OFFLINEIMG, LABELS.getString("labelDevOffLIne"));
	public final static ImageIcon LOGIN_BULLET = new ImageIcon(MainView.class.getResource("/images/bullet_star_yellow.png"), LABELS.getString("labelDevNotLogged"));
	public final static ImageIcon UPDATING_BULLET = new ImageIcon(MainView.class.getResource("/images/bullet_refresh.png"), LABELS.getString("labelDevUpdating"));
	public final static ImageIcon ERROR_BULLET = new ImageIcon(MainView.class.getResource("/images/bullet_error.png"), LABELS.getString("labelDevError"));
	private final static String TRUE = LABELS.getString("true_yn");
	private final static String FALSE = LABELS.getString("false_yn");
	private final static String YES = LABELS.getString("true_yna");
	private final static String NO = LABELS.getString("false_yna");
	private final static MessageFormat SWITCH_FORMATTER = new MessageFormat(Main.LABELS.getString("METER_VAL_EX"), Locale.ENGLISH); // tooltip
	
	// model columns indexes
	public final static int COL_STATUS_IDX = 0;
	final static int COL_TYPE = 1;
	final static int COL_DEVICE = 2;
	final static int COL_NAME = 3;
	final static int COL_KEYWORD = 4;
	final static int COL_MAC_IDX = 5;
	final static int COL_IP_IDX = 6;
	final static int COL_SSID_IDX = 7;
	final static int COL_RSSI_IDX = 8;
	final static int COL_CLOUD = 9;
	final static int COL_MQTT = 10;
	final static int COL_UPTIME_IDX = 11;
	final static int COL_INT_TEMP = 12;
	final static int COL_MEASURES_IDX = 13;
	final static int COL_DEBUG = 14;
	final static int COL_SOURCE_IDX = 15;
	final static int COL_COMMAND_IDX = 16;
	
	public final static String STORE_PREFIX = "TAB";
	public final static String STORE_EXT_PREFIX = "TAB_EXT";
	
	private UptimeCellRenderer uptimeRenderer = new UptimeCellRenderer();

	private boolean adaptTooltipLocation = false;
	
	private final static Logger LOG = LoggerFactory.getLogger(DevicesTable.class);

	public DevicesTable(TableModel tm) {
		super(tm, true);
		columnModel.getColumn(COL_STATUS_IDX).setMaxWidth(ONLINE_BULLET.getIconWidth() + 4);
		columnModel.getColumn(COL_MEASURES_IDX).setCellRenderer(new DeviceMetersCellRenderer());
		columnModel.getColumn(COL_INT_TEMP).setCellRenderer(new DecimalTableCellRenderer(2));
		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment(JLabel.CENTER);
		columnModel.getColumn(COL_CLOUD).setCellRenderer(centerRenderer);
		columnModel.getColumn(COL_MQTT).setCellRenderer(centerRenderer);
		columnModel.getColumn(COL_UPTIME_IDX).setCellRenderer(uptimeRenderer);
		columnModel.getColumn(COL_DEBUG).setCellRenderer(centerRenderer);
		columnModel.getColumn(COL_SOURCE_IDX).setCellRenderer(new ArrayTableCellRenderer());
		final TableColumn colCommand = columnModel.getColumn(COL_COMMAND_IDX);
		colCommand.setCellRenderer(new DevicesCommandCellRenderer());
		colCommand.setCellEditor(new DevicesCommandCellEditor(this));

		TableRowSorter<?> sorter = (TableRowSorter<?>)getRowSorter();
		
		sorter.setComparator(COL_COMMAND_IDX, (o1, o2) -> {
			final String s1, s2;
			if(o1 == null) {
				s1 = "";
			} else if (o1 instanceof DeviceModule[] dmArray) {
				s1 = dmArray[0].getLabel();
			} else if (o1 instanceof DeviceModule dm) {
				s1 = dm.getLabel();
			} else {
				s1 = o1.toString();
			}
			if(o2 == null) {
				s2 = "";
			} else if (o2 instanceof DeviceModule[] dmArray) {
				s2 = dmArray[0].getLabel();
			} else if (o2 instanceof DeviceModule dm) {
				s2 = dm.getLabel();
			} else {
				s2 = o2.toString();
			}
			return s1.compareTo(s2);
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
			String s1 = o1 instanceof String[] ? ((String[])o1)[0] : (String)o1;
			String s2 = o2 instanceof String[] ? ((String[])o2)[0] : (String)o2;
			if(s1 == null) {
				return -1;
			}
			if(s2 == null) {
				return 1;
			}
			return s1.compareTo(s2);
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
		Object val = getValueAt(row, column);
		return val instanceof DeviceModule || val instanceof DeviceModule[];
	}

	@Override
	public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
		Component comp = super.prepareRenderer(renderer, row, column);
		if(isRowSelected(row)) {
			comp.setBackground(getSelectionBackground());
		} else {
			comp.setBackground((row % 2 == 0) ? Main.TAB_LINE1 : Main.TAB_LINE2);
		}
		computeRowHeight(row, comp);
		return comp;
	}
	
	public void setUptimeRenderMode(String mode) {
		uptimeRenderer.setMode(mode);
	}

	@Override
	public String getToolTipText(final MouseEvent evt) {
		final int r, c;
		final Object value;
		if(((Component) evt.getSource()).isVisible() && (r = rowAtPoint(evt.getPoint())) >= 0 && (c = columnAtPoint(evt.getPoint())) >= 0 &&
				(value = getValueAt(r, c)) != null && (getEditingColumn() == c && getEditingRow() == r) == false) {
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
				if(Arrays.stream(meters).anyMatch(m -> DeviceMetersCellRenderer.hasHiddenMeasures(m) || m instanceof LabelHolder || m.hasNames()) ||
						getCellRect(r, c, false).width <= getCellRenderer(r, c).getTableCellRendererComponent(this, value, false, false, r, c).getPreferredSize().width) {
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
							if(t == Meters.Type.EX) {
								tt += "<td><i>" + LABELS.getString("METER_LBL_" + t) + tLabel + "</i>&nbsp;</td><td align='right'>" + SWITCH_FORMATTER.format(new Object [] {m.getValue(t)}) + "&nbsp;</td>";
							} else {
								tt += "<td><i>" + LABELS.getString("METER_LBL_" + t) + tLabel + "</i>&nbsp;</td><td align='right'>" + String.format(Locale.ENGLISH, LABELS.getString("METER_VAL_" + t), m.getValue(t)) + "&nbsp;</td>";
							}
						}
						tt += "</tr>";
					}
					return tt + "</table></html>";
				}
			} else {
				adaptTooltipLocation = true;
				return super.getToolTipText(evt);
			}
		}
		return null;
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
		Graphics g = getGraphics();
		if(g != null) {
			final FontMetrics fm = g.getFontMetrics();
			final int columnCount = getColumnCount();
			final int rowCount =  getRowCount();
			for(int c = 0; c < columnCount; c++) {
				TableColumn tc = columnModel.getColumn(c);
				Object val = tc.getHeaderValue();
				int width = (val != null) ? SwingUtilities.computeStringWidth(fm, val.toString()) >> 1 /*/ 2*/ : 1;
				if(c == convertColumnIndexToView(COL_UPTIME_IDX)) {
					width = Math.max(width, uptimeRenderer.getPreferredWidth(fm));
				} else if(c == convertColumnIndexToView(COL_STATUS_IDX)) {
					width = Math.max(width, ONLINE_BULLET.getIconWidth() + 1);
				} else {
					for(int r = 0; r < rowCount; r++) {
						val = getValueAt(r, c);
						if(val != null) {
							if(val instanceof Object[] arr) {
								for(Object v: arr) {
									if(v != null) {
										int w = SwingUtilities.computeStringWidth(fm, v.toString());
										width = Math.max(width, w);
									}
								}
							} else {
								width = Math.max(width, SwingUtilities.computeStringWidth(fm, val.toString()));
							}
						}
					}
				}
				tc.setPreferredWidth(width);
			}
		}
	}

	public void csvExport(BufferedWriter w, String separator) throws IOException {
		Stream.Builder<String> h = Stream.builder();
		for(int col = 0; col < getColumnCount(); col++) {
			String name = getColumnName(col);
			h.accept(name.isEmpty() ? LABELS.getString("col_status_exp") : name); // dirty and fast
		}
		w.write(h.build().collect(Collectors.joining(separator)));
		w.newLine();

		for(int row = 0; row < getRowCount(); row++) {
			try {
				Stream.Builder<String> r = Stream.builder();
				for(int col = 0; col < getColumnCount(); col++) {
					r.accept(cellTooltipValue(getValueAt(row, col), true, row, col));
				}
				w.write(r.build().collect(Collectors.joining(separator)));
				w.newLine();
			} catch(RuntimeException e) {
				LOG.error("csvExport", e);
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
					row[DevicesTable.COL_UPTIME_IDX] = d.getUptime();
				}
				row[DevicesTable.COL_INT_TEMP] = (d instanceof InternalTmpHolder) ? ((InternalTmpHolder)d).getInternalTmp() : null;
				row[DevicesTable.COL_MEASURES_IDX] = d.getMeters();
				row[DevicesTable.COL_DEBUG] = LABELS.getString("debug" + d.getDebugMode().name());
				DeviceModule[] command = null;
				if(d instanceof ModulesHolder mh && mh.getModulesCount() > 0) {
					row[DevicesTable.COL_COMMAND_IDX] = command = mh.getModules();
				} else if(d instanceof ShellyDW dw) {
					row[DevicesTable.COL_COMMAND_IDX] = LABELS.getString("lableStatusOpen") + ": " + (dw.isOpen() ? YES : NO);
				} else if(d instanceof ShellyFlood flood) {
					row[DevicesTable.COL_COMMAND_IDX] = LABELS.getString("lableStatusFlood") + ": " + (flood.flood() ? YES : NO);
				} else if(d instanceof MotionSensor motion) {
					row[DevicesTable.COL_COMMAND_IDX] = String.format(LABELS.getString("lableStatusMotion"), motion.motion() ? YES : NO);
				} else if(d instanceof ShellyPlusSmoke smoke) {
					row[DevicesTable.COL_COMMAND_IDX] = String.format(LABELS.getString("lableStatusSmoke"), smoke.getAlarm() ? YES : NO);
				} else if(d instanceof ShellyTRV trv) { // very specific
					ThermostatG1 thermostat = trv.getThermostat();
					if(thermostat.isEnabled()) {
						row[DevicesTable.COL_COMMAND_IDX] = thermostat;
					} else {
						row[DevicesTable.COL_COMMAND_IDX] = String.format(LABELS.getString("lableStatusTRV"), thermostat.getPosition());
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
				return new ImageIcon(BTHOMEIMG, String.format(LABELS.getString("labelDevOnLIneBTHome"), LocalDateTime.ofInstant(Instant.ofEpochMilli(d.getLastTime()), ZoneId.systemDefault())));
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