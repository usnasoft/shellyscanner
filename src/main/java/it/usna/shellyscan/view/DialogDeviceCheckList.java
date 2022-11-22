package it.usna.shellyscan.view;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.net.InetAddress;
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

import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.g1.AbstractG1Device;
import it.usna.shellyscan.view.util.IPv4Comparator;
import it.usna.shellyscan.view.util.UtilCollecion;
import it.usna.swing.table.ExTooltipTable;
import it.usna.swing.table.UsnaTableModel;
import java.awt.FlowLayout;

public class DialogDeviceCheckList extends JDialog {
	private static final long serialVersionUID = 1L;
	private final static Logger LOG = LoggerFactory.getLogger(AbstractG1Device.class);

	public DialogDeviceCheckList(final Window owner, List<ShellyAbstractDevice> model, Boolean ipSort) {
		super(owner, LABELS.getString("dlgChecklistTitle"));
		BorderLayout borderLayout = (BorderLayout) getContentPane().getLayout();
		borderLayout.setVgap(2);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		
		UsnaTableModel tModel = new UsnaTableModel(
				LABELS.getString("col_device"), LABELS.getString("col_ip"), LABELS.getString("col_eco"), LABELS.getString("col_ledoff"), LABELS.getString("col_AP"), LABELS.getString("col_logs"));
		ExTooltipTable table = new ExTooltipTable(tModel, true) {
			private static final long serialVersionUID = 1L;
			{
				String[] headerTips = new String[6];
				headerTips[2] = LABELS.getString("col_eco_tooltip");
				headerTips[3] = LABELS.getString("col_ledoff_tooltip");
				headerTips[4] = LABELS.getString("col_AP_tooltip");
				headerTips[5] = LABELS.getString("col_logs_tooltip");
				setHeadersTooltip(headerTips);
				
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
				
				((TableRowSorter<?>)getRowSorter()).setComparator(1, new IPv4Comparator());
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

		fill(tModel, model);

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
		
		JPanel panelButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		panelBottom.add(panelButtons, BorderLayout.WEST);
		
		JButton btnClose = new JButton(LABELS.getString("dlgClose"));
		btnClose.addActionListener(e -> dispose());
		panelButtons.add(btnClose);
		
		JButton btnNewButton = new JButton(LABELS.getString("labelRefresh"));
		btnNewButton.addActionListener(e -> {
			tModel.clear();
			fill(tModel, model);
		});
		panelButtons.add(btnNewButton);

		setSize(650, 420);
		setVisible(true);
		setLocationRelativeTo(owner);
		table.columnsWidthAdapt();
	}
	
	private void fill(UsnaTableModel tModel, List<ShellyAbstractDevice> model) {
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		try {
			model.forEach(d -> {
				if(d instanceof AbstractG1Device) {
					try {
						JsonNode settings = d.getJSON("/settings");
						Boolean eco = boolVal(settings.path("eco_mode_enabled"));
						Boolean ledOff = boolVal(settings.path("led_status_disable"));
						boolean debug = d.getDebugMode() != ShellyAbstractDevice.LogMode.NO;
						tModel.addRow(UtilCollecion.getExtendedHostName(d), d.getHttpHost().getAddress(), eco, ledOff, "-", debug);
					} catch (Exception e) {
						tModel.addRow(UtilCollecion.getExtendedHostName(d), d.getHttpHost().getAddress());
						LOG.error("{}", d, e);
					}
				} else { // G2
					try {
						JsonNode settings = d.getJSON("/rpc/Shelly.GetConfig");
						Boolean eco = boolVal(settings.at("/sys/device/eco_mode"));
						Boolean ap = boolVal(settings.at("/wifi/ap/enable"));
						String debug = LABELS.getString("debug" + d.getDebugMode());
						tModel.addRow(UtilCollecion.getExtendedHostName(d), d.getHttpHost().getAddress(), eco, "-", ap, debug);
					} catch (Exception e) {
						tModel.addRow(UtilCollecion.getExtendedHostName(d), d.getHttpHost().getAddress());
						LOG.error("{}", d, e);
					}
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
		private final static String TRUE = LABELS.getString("true_yn");
		private final static String FALSE = LABELS.getString("false_yn");
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