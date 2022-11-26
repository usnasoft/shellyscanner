package it.usna.shellyscan.view;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
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

import javax.swing.Action;
import javax.swing.BorderFactory;
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
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
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

	public DialogDeviceCheckList(final Window owner, final List<ShellyAbstractDevice> devices, final Boolean ipSort) {
		super(owner, LABELS.getString("dlgChecklistTitle"));
		BorderLayout borderLayout = (BorderLayout) getContentPane().getLayout();
		borderLayout.setVgap(2);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		UsnaTableModel tModel = new UsnaTableModel(
				LABELS.getString("col_device"), LABELS.getString("col_ip"), LABELS.getString("col_eco"), LABELS.getString("col_ledoff"),
				LABELS.getString("col_AP"), LABELS.getString("col_logs"), LABELS.getString("col_blt"));
		ExTooltipTable table = new ExTooltipTable(tModel, true) {
			private static final long serialVersionUID = 1L;
			{
				setHeadersTooltip(null, null, LABELS.getString("col_eco_tooltip"), LABELS.getString("col_ledoff_tooltip"),
						LABELS.getString("col_AP_tooltip"),  LABELS.getString("col_logs_tooltip"), LABELS.getString("col_blt_tooltip"));

				columnModel.getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
					private static final long serialVersionUID = 1L;
					@Override
					public void setValue(Object value) {
						setText(((InetAddress)value).getHostAddress());
					}
				});
				TableCellRenderer rendTrueOk = new CheckRenderer(true);
				TableCellRenderer rendFalseOk = new CheckRenderer(false);
				columnModel.getColumn(2).setCellRenderer(rendTrueOk);
				columnModel.getColumn(3).setCellRenderer(rendTrueOk);
				columnModel.getColumn(4).setCellRenderer(rendFalseOk);
				columnModel.getColumn(5).setCellRenderer(rendFalseOk);
				columnModel.getColumn(6).setCellRenderer(rendFalseOk);

				TableRowSorter<?> rowSorter = ((TableRowSorter<?>)getRowSorter());
				Comparator<?> oc =  (o1, o2) -> {return o1 == null ? -1 : o1.toString().compareTo(o2.toString());};
				rowSorter.setComparator(1, new IPv4Comparator());
				rowSorter.setComparator(2, oc);
				rowSorter.setComparator(3, oc);
				rowSorter.setComparator(4, oc);
				rowSorter.setComparator(5, oc);
				rowSorter.setComparator(6, oc);

				if(ipSort != null) {
					sortByColumn(1, ipSort);
				}
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

		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		JPanel panelBottom = new JPanel(new BorderLayout(0, 0));
		getContentPane().add(panelBottom, BorderLayout.SOUTH);

		// Find panel
		JPanel panelFind = new JPanel();
		FlowLayout flowLayout_1 = (FlowLayout) panelFind.getLayout();
		flowLayout_1.setVgap(0);
		panelBottom.add(panelFind, BorderLayout.EAST);

		JLabel label = new JLabel("Filter:");
		panelFind.add(label);

		JTextField textFieldFilter = new JTextField();
		textFieldFilter.setColumns(20);
		textFieldFilter.setBorder(BorderFactory.createEmptyBorder(2, 1, 2, 1));
		panelFind.add(textFieldFilter);
		textFieldFilter.getDocument().addDocumentListener(new DocumentListener() {
			private final int[] cols = new int[] {0, 1};
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
			fill(tModel, devices);
		});
		panelButtons.add(btnRefresh);
		
		JButton btnEdit = new JButton(LABELS.getString("edit"));
		btnEdit.setEnabled(false);
		btnEdit.addActionListener(ev -> {
			ShellyAbstractDevice d = devices.get(table.convertRowIndexToModel(table.getSelectedRow()));
			try {
				Desktop.getDesktop().browse(new URI(d.getHttpHost().getSchemeName() + "://" + d.getHttpHost().getAddress().getHostAddress()));
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

		setSize(680, 420);
		setVisible(true);
		setLocationRelativeTo(owner);
		table.columnsWidthAdapt();
	}

	private void fill(UsnaTableModel tModel, List<ShellyAbstractDevice> model) {
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		try {
			model.forEach(d -> {
				try {
					if(d instanceof AbstractG1Device) {
						JsonNode settings = d.getJSON("/settings");
						Boolean eco = boolVal(settings.path("eco_mode_enabled"));
						Boolean ledOff = boolVal(settings.path("led_status_disable"));
						boolean debug = d.getDebugMode() != ShellyAbstractDevice.LogMode.NO;
						tModel.addRow(UtilCollecion.getExtendedHostName(d), d.getHttpHost().getAddress(), eco, ledOff, "-", debug, "-");
					} else { // G2
						JsonNode settings = d.getJSON("/rpc/Shelly.GetConfig");
						Boolean eco = boolVal(settings.at("/sys/device/eco_mode"));
						Object ap = boolVal(settings.at("/wifi/ap/enable"));
						if(ap != null && ap == Boolean.TRUE && settings.at("/wifi/ap/is_open").asBoolean(true) == false) {
							ap = TRUE; // AP active but protected with pwd
						}
						Object debug = (d.getDebugMode() == ShellyAbstractDevice.LogMode.NO) ? Boolean.FALSE : LABELS.getString("debug" + d.getDebugMode());
						Boolean ble = boolVal(settings.at("/ble/enable"));
						tModel.addRow(UtilCollecion.getExtendedHostName(d), d.getHttpHost().getAddress(), eco, "-", ap, debug, ble);
					}
				} catch (Exception e) {
					tModel.addRow(UtilCollecion.getExtendedHostName(d), d.getHttpHost().getAddress());
					LOG.error("{}", d, e);
				}
			});
		} finally {
			setCursor(Cursor.getDefaultCursor());
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