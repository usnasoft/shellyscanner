package it.usna.shellyscan.view;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.model.device.BatteryDeviceInterface;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.ShellyAbstractDevice.Status;
import it.usna.shellyscan.model.device.g1.AbstractG1Device;
import it.usna.shellyscan.view.util.IPv4Comparator;
import it.usna.shellyscan.view.util.UtilCollecion;
import it.usna.swing.table.ExTooltipTable;
import it.usna.swing.table.UsnaTableModel;

public class DialogDeviceCheckList extends JDialog {
	private static final long serialVersionUID = 1L;
	private final static Logger LOG = LoggerFactory.getLogger(AbstractG1Device.class);
	private final static String TRUE = LABELS.getString("true_yn");
	private final static String FALSE = LABELS.getString("false_yn");
	private final static int COL_STATUS = 0;
	private final static int COL_NAME = 1;
	private final static int COL_IP = 2;
//	private final 
	private ExecutorService exeService /*= Executors.newFixedThreadPool(20)*/;

	public DialogDeviceCheckList(final Window owner, final List<ShellyAbstractDevice> devices, final Boolean ipSort) {
		super(owner, LABELS.getString("dlgChecklistTitle"));
		BorderLayout borderLayout = (BorderLayout) getContentPane().getLayout();
		borderLayout.setVgap(2);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		UsnaTableModel tModel = new UsnaTableModel("",
				LABELS.getString("col_device"), LABELS.getString("col_ip"), LABELS.getString("col_eco"), LABELS.getString("col_ledoff"), LABELS.getString("col_logs"),
				LABELS.getString("col_blt"), LABELS.getString("col_AP"), LABELS.getString("col_roaming"), LABELS.getString("col_wifi1"), LABELS.getString("col_wifi2"));
		
		ExTooltipTable table = new ExTooltipTable(tModel, true) {
			private static final long serialVersionUID = 1L;
			{
				columnModel.getColumn(COL_STATUS).setMaxWidth(DevicesTable.ONLINE_BULLET.getIconWidth() + 4);
				setHeadersTooltip(LABELS.getString("col_status_exp"), null, null, LABELS.getString("col_eco_tooltip"), LABELS.getString("col_ledoff_tooltip"), LABELS.getString("col_logs_tooltip"), 
						LABELS.getString("col_blt_tooltip"), LABELS.getString("col_AP_tooltip"), LABELS.getString("col_roaming_tooltip"), LABELS.getString("col_wifi1_tooltip"), LABELS.getString("col_wifi2_tooltip"));

				columnModel.getColumn(COL_IP).setCellRenderer(new DefaultTableCellRenderer() {
					private static final long serialVersionUID = 1L;
					@Override
					public void setValue(Object value) {
						setText(((InetAddress)value).getHostAddress());
					}
				});
				TableCellRenderer rendTrueOk = new CheckRenderer(true);
				TableCellRenderer rendFalseOk = new CheckRenderer(false);
				columnModel.getColumn(3).setCellRenderer(rendTrueOk); // eco
				columnModel.getColumn(4).setCellRenderer(rendTrueOk); // led
				columnModel.getColumn(5).setCellRenderer(rendFalseOk); // logs
				columnModel.getColumn(6).setCellRenderer(rendFalseOk); // bluetooth
				columnModel.getColumn(7).setCellRenderer(rendFalseOk); // AP
				columnModel.getColumn(8).setCellRenderer(rendFalseOk); // roaming
				columnModel.getColumn(9).setCellRenderer(rendTrueOk); // wifi1 null -> "-"
				columnModel.getColumn(10).setCellRenderer(rendTrueOk); // wifi2 null -> "-"

				TableRowSorter<?> rowSorter = ((TableRowSorter<?>)getRowSorter());
				Comparator<?> oc = (o1, o2) -> {return o1 == null ? -1 : o1.toString().compareTo(o2.toString());};
				rowSorter.setComparator(COL_IP, new IPv4Comparator());
				rowSorter.setComparator(3, oc);
				rowSorter.setComparator(4, oc);
				rowSorter.setComparator(5, oc);
				rowSorter.setComparator(6, oc);
				rowSorter.setComparator(7, oc);

				if(ipSort != null) {
					sortByColumn(COL_IP, ipSort);
				}
				rowSorter.setSortsOnUpdates(true);
			}

			@Override
			protected String cellTooltipValue(Object value, boolean cellTooSmall, int row, int column) {
				if(cellTooSmall && value instanceof InetAddress) {
					return ((InetAddress)value).getHostAddress();
				} else {
					return super.cellTooltipValue(value, cellTooSmall, row, column);
				}
			}
		};

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportView(table);
		getContentPane().add(scrollPane, BorderLayout.CENTER);

		fill(tModel, devices);

		table.setRowHeight(table.getRowHeight() + 2);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		JPanel panelBottom = new JPanel(new BorderLayout(0, 0));
		getContentPane().add(panelBottom, BorderLayout.SOUTH);

		// Find panel
		JPanel panelFind = new JPanel();
		FlowLayout flowLayout_1 = (FlowLayout) panelFind.getLayout();
		flowLayout_1.setVgap(0);
		panelBottom.add(panelFind, BorderLayout.EAST);

		JLabel label = new JLabel(LABELS.getString("lblFilter"));
		panelFind.add(label);

		JTextField textFieldFilter = new JTextField();
		textFieldFilter.setColumns(20);
		textFieldFilter.setBorder(BorderFactory.createEmptyBorder(2, 1, 2, 1));
		panelFind.add(textFieldFilter);
		textFieldFilter.getDocument().addDocumentListener(new DocumentListener() {
			private final int[] cols = new int[] {COL_NAME, COL_IP};
			@Override
			public void changedUpdate(DocumentEvent e) {
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				setRowFilter(textFieldFilter.getText());
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				setRowFilter(textFieldFilter.getText());
			}

			public void setRowFilter(String filter) {
				TableRowSorter<?> sorter = (TableRowSorter<?>)table.getRowSorter();
				if(filter.length() > 0) {
					filter = filter.replace("\\E", "\\e");
					sorter.setRowFilter(RowFilter.regexFilter("(?i).*\\Q" + filter + "\\E.*", cols));
				} else {
					sorter.setRowFilter(null);
				}
			}
		});
		getRootPane().registerKeyboardAction(e -> textFieldFilter.requestFocus(), KeyStroke.getKeyStroke(KeyEvent.VK_F, MainView.SHORTCUT_KEY), JComponent.WHEN_IN_FOCUSED_WINDOW);

		final Action eraseFilterAction = new UsnaAction(this, "/images/erase-9-16.png", null, e -> {
			textFieldFilter.setText("");
			textFieldFilter.requestFocusInWindow();
			table.clearSelection();
		});

		JButton eraseFilterButton = new JButton(eraseFilterAction);
		eraseFilterButton.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
		eraseFilterButton.setContentAreaFilled(false);
		eraseFilterButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_E, MainView.SHORTCUT_KEY), "find_erase");
		eraseFilterButton.getActionMap().put("find_erase", eraseFilterAction);
		panelFind.add(eraseFilterButton);

		JPanel panelButtons = new JPanel();
		panelBottom.add(panelButtons, BorderLayout.WEST);

		JButton btnClose = new JButton(LABELS.getString("dlgClose"));
		btnClose.addActionListener(e -> dispose());
		panelButtons.setLayout(new GridLayout(0, 3, 0, 0));
		panelButtons.add(btnClose);

		JButton btnRefresh = new JButton(LABELS.getString("labelRefresh"));
		btnRefresh.addActionListener(e -> {
			tModel.clear();
			exeService.shutdownNow();
			fill(tModel, devices);
			try {
				Thread.sleep(250); // too many call disturb some devices at least (2.5)
			} catch (InterruptedException e1) {}
		});
		panelButtons.add(btnRefresh);
		
		JButton btnEdit = new JButton(LABELS.getString("edit"));
		btnEdit.setEnabled(false);
		btnEdit.addActionListener(ev -> {
			ShellyAbstractDevice d = devices.get(table.convertRowIndexToModel(table.getSelectedRow()));
			try {
				Desktop.getDesktop().browse(new URI("http://" + d.getAddress().getHostAddress()));
			} catch (IOException | URISyntaxException e) {
				Main.errorMsg(e);
			}
		});
		panelButtons.add(btnEdit);
		
		table.getSelectionModel().addListSelectionListener(l -> btnEdit.setEnabled(table.getSelectedRow() >= 0));
		
		table.addMouseListener(new MouseAdapter() {
		    public void mousePressed(MouseEvent evt) {
		        if (evt.getClickCount() == 2 && table.getSelectedRow() != -1) {
		        	btnEdit.doClick();
		        }
		    }
		});

		setSize(750, 420);
		setVisible(true);
		setLocationRelativeTo(owner);
		table.columnsWidthAdapt();
	}

	@Override
	public void dispose() {
		exeService.shutdownNow();
		super.dispose();
	}
	
	private void fill(UsnaTableModel tModel, List<ShellyAbstractDevice> model) {
		exeService = Executors.newFixedThreadPool(20);
		model.forEach(d -> {
			final int row = tModel.addRow(DevicesTable.UPDATING_BULLET, UtilCollecion.getExtendedHostName(d), d.getAddress());
			exeService.execute(() -> {
				try {
					if(d instanceof AbstractG1Device) {
						tModel.setRow(row, g1Row(d, d.getJSON("/settings")));
					} else { // G2
						tModel.setRow(row, g2Row(d, d.getJSON("/rpc/Shelly.GetConfig")));
					}
				} catch (/*IO*/Exception e) {
					if(d instanceof BatteryDeviceInterface) {
						if(d instanceof AbstractG1Device) {
							tModel.setRow(row, g1Row(d, ((BatteryDeviceInterface)d).getStoredJSON("/settings")));
						} else {
							tModel.setRow(row, g2Row(d, ((BatteryDeviceInterface)d).getStoredJSON("/rpc/Shelly.GetConfig")));
						}
					} else {
						tModel.setRow(row, getStatusIcon(d), UtilCollecion.getExtendedHostName(d), d.getAddress());
					}
					if(/*e.getCause().getCause() instanceof java.net.SocketTimeoutException*/d.getStatus() == Status.OFF_LINE || d.getStatus() == Status.NOT_LOOGGED) {
						LOG.debug("{}", d, e);
					} else {
						LOG.error("{}", d, e);
					}
				}
			});
		});
	}
	
	private static Object[] g1Row(ShellyAbstractDevice d, JsonNode settings) {
		Boolean eco = boolVal(settings.path("eco_mode_enabled"));
		Boolean ledOff = boolVal(settings.path("led_status_disable"));
		boolean debug = d.getDebugMode() != ShellyAbstractDevice.LogMode.NO;
		String roaming;
		if(settings.path("ap_roaming").isMissingNode()) {
			roaming = "-";
		} else if(settings.at("/ap_roaming/enabled").asBoolean()) {
			roaming = settings.at("/ap_roaming/threshold").asText();
		} else {
			roaming = FALSE;
		}
		String wifi1;
		if(settings.at("/wifi_sta/enabled").asBoolean()) {
			wifi1 = "static".equals(settings.at("/wifi_sta/ipv4_method").asText()) ? TRUE : FALSE;
		} else {
			wifi1 = "-";
		}
		String wifi2;
		if(settings.at("/wifi_sta1/enabled").asBoolean()) {
			wifi2 = "static".equals(settings.at("/wifi_sta1/ipv4_method").asText()) ? TRUE : FALSE;
		} else {
			wifi2 = "-";
		}
		return new Object[] {getStatusIcon(d), UtilCollecion.getExtendedHostName(d), d.getAddress(), eco, ledOff, debug, "-", "-", roaming, wifi1, wifi2};
	}
	
	private static Object[] g2Row(ShellyAbstractDevice d, JsonNode settings) {
		Boolean eco = boolVal(settings.at("/sys/device/eco_mode"));
		Object ap = boolVal(settings.at("/wifi/ap/enable"));
		if(ap != null && ap == Boolean.TRUE && settings.at("/wifi/ap/is_open").asBoolean(true) == false) {
			ap = TRUE; // AP active but protected with pwd
		}
		Object debug = (d.getDebugMode() == ShellyAbstractDevice.LogMode.NO) ? Boolean.FALSE : LABELS.getString("debug" + d.getDebugMode());
		Boolean ble = boolVal(settings.at("/ble/enable"));
		String roaming;
		if(settings.at("/wifi/roam").isMissingNode()) {
			roaming = "-";
		} else if(settings.at("/wifi/roam/interval").asInt() > 0) {
			roaming = settings.at("/wifi/roam/rssi_thr").asText();
		} else {
			roaming = FALSE;
		}
		String wifi1;
		if(settings.at("/wifi/sta/enable").asBoolean()) {
			wifi1 = "static".equals(settings.at("/wifi/sta/ipv4mode").asText()) ? TRUE : FALSE;
		} else {
			wifi1 = "-";
		}
		String wifi2;
		if(settings.at("/wifi/sta1/enable").asBoolean()) {
			wifi2 = "static".equals(settings.at("/wifi/sta1/ipv4mode").asText()) ? TRUE : FALSE;
		} else {
			wifi2 = "-";
		}
		return new Object[] {getStatusIcon(d), UtilCollecion.getExtendedHostName(d), d.getAddress(), eco, "-", debug, ble, ap, roaming, wifi1, wifi2};
	}
	
	private static ImageIcon getStatusIcon(ShellyAbstractDevice d) {
		if(d.getStatus() == Status.ON_LINE) {
			return DevicesTable.ONLINE_BULLET;
		} else if(d.getStatus() == Status.OFF_LINE) {
			return DevicesTable.OFFLINE_BULLET;
		} else if(d.getStatus() == Status.READING) {
			return DevicesTable.UPDATING_BULLET;
		} else if(d.getStatus() == Status.ERROR) {
			return DevicesTable.ERROR_BULLET;
		} else { // Status.NOT_LOOGGED
			return DevicesTable.LOGIN_BULLET;
		}
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
			if(value instanceof Boolean) {
				ret = super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
				if((Boolean)value) {
					((JLabel)ret).setText(TRUE);
					if(isSelected == false) {
						ret.setForeground(goodVal ? Color.green : Color.red);
					}
				} else {
					((JLabel)ret).setText(FALSE);
					if(isSelected == false) {
						ret.setForeground(goodVal ? Color.red : Color.green);
					}
				}
			} else {
				ret = super.getTableCellRendererComponent(table, value == null ? "-" : value, isSelected, hasFocus, row, column);
				if(isSelected == false) {
					ret.setForeground(table.getForeground());
				}
			}
			return ret;
		}
	}
}