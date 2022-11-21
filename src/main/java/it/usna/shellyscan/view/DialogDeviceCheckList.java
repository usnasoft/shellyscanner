package it.usna.shellyscan.view;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Component;
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
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;

import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.view.util.IPv4Comparator;
import it.usna.shellyscan.view.util.UtilCollecion;
import it.usna.swing.table.ExTooltipTable;
import it.usna.swing.table.UsnaTableModel;

public class DialogDeviceCheckList extends JDialog {
	private static final long serialVersionUID = 1L;

	public DialogDeviceCheckList(final Window owner, List<ShellyAbstractDevice> model, Boolean ipSort) {
		super(owner, LABELS.getString("dlgSelectorTitle"));
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		
		UsnaTableModel tModel = new UsnaTableModel(LABELS.getString("col_device"), LABELS.getString("col_ip"));
		ExTooltipTable table = new ExTooltipTable(tModel, true) {
			private static final long serialVersionUID = 1L;
			{
				columnModel.getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
					private static final long serialVersionUID = 1L;
					@Override
					public void setValue(Object value) {
						setText(((InetAddress)value).getHostAddress());
					}
				});
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

		model.forEach(d -> {
			tModel.addRow(UtilCollecion.getExtendedHostName(d), d.getHttpHost().getAddress());
		});

		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		// Find panel
		JPanel panelBottom = new JPanel();
		getContentPane().add(panelBottom, BorderLayout.SOUTH);
		
		
		panelBottom.setLayout(new BorderLayout(0, 0));
		
		JButton btnClose = new JButton(LABELS.getString("dlgClose"));
		btnClose.setAlignmentX(Component.RIGHT_ALIGNMENT);
		panelBottom.add(btnClose, BorderLayout.EAST);
		btnClose.addActionListener(e -> dispose());
		
		JPanel panelFind = new JPanel();
		panelBottom.add(panelFind, BorderLayout.WEST);
		
		JLabel label = new JLabel("Filter:");
		panelFind.add(label);
		
		JTextField textFieldFilter = new JTextField();
		textFieldFilter.setColumns(24);
		textFieldFilter.setBorder(BorderFactory.createEmptyBorder(2, 1, 2, 1));
		panelFind.add(textFieldFilter);
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

		// Sort
//		TableRowSorter<?> sorter = (TableRowSorter<?>)table.getRowSorter();
//		sorter.setComparator(1, new IPv4Comparator());

		setSize(560, 400);
		setVisible(true);
		setLocationRelativeTo(owner);
		table.columnsWidthAdapt();
	}
}