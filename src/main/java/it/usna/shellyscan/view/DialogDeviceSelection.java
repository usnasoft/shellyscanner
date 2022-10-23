package it.usna.shellyscan.view;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.InetAddress;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.view.devsettings.AbstractSettingsPanel;
import it.usna.shellyscan.view.util.IPv4Comparator;
import it.usna.swing.table.ExTooltipTable;
import it.usna.swing.table.UsnaTableModel;
import it.usna.util.UsnaEventListener;

public class DialogDeviceSelection extends JDialog {
	private static final long serialVersionUID = 1L;

	private UsnaTableModel tModel = new UsnaTableModel(LABELS.getString("col_device"), LABELS.getString("col_ip"));
	private ExTooltipTable table = new ExTooltipTable(tModel, true);

	public DialogDeviceSelection(final Window owner, UsnaEventListener<ShellyAbstractDevice, Void> listener, Devices model) {
		super(owner, LABELS.getString("dlgSelectorTitle"));
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportView(table);
		getContentPane().add(scrollPane, BorderLayout.CENTER);

		for(int i = 0; i < model.size(); i++) {
			ShellyAbstractDevice d = model.get(i);
//			if(d.getStatus() == ShellyAbstractDevice.Status.ON_LINE) {
				tModel.addRow(AbstractSettingsPanel.getExtendedName(d), d.getHttpHost().getAddress());
//			}
		}

		table.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
			private static final long serialVersionUID = 1L;
			@Override
			public void setValue(Object value) {
				setText(((InetAddress)value).getHostAddress());
			}
		});
		
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		JPanel panel = new JPanel();
		getContentPane().add(panel, BorderLayout.SOUTH);
		
		JButton btnClose = new JButton(LABELS.getString("dlgClose"));
		btnClose.addActionListener(e -> dispose());
		panel.add(btnClose);
		
		TableRowSorter<?> sorter = (TableRowSorter<?>)table.getRowSorter();
		sorter.setComparator(1, new IPv4Comparator());
		table.sortByColumn(0, true);

		table.getSelectionModel().addListSelectionListener(e -> {
			if(e.getValueIsAdjusting() == false) {
				listener.update(model.get(table.convertRowIndexToModel(table.getSelectedRow())), null);
			}
		});
		
		table.addMouseListener(new MouseAdapter() {
		    public void mousePressed(MouseEvent evt) {
		        if (evt.getClickCount() == 2 && table.getSelectedRow() != -1) {
		        	dispose();
		        }
		    }
		});

		setSize(400, 400);
		setVisible(true);
		setLocationRelativeTo(owner);
		table.columnsWidthAdapt();
	}
}