package it.usna.shellyscan.view.checklist;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.RowFilter;
import javax.swing.SortOrder;
import javax.swing.table.TableRowSorter;

import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.blu.BLEGateway;
import it.usna.shellyscan.model.device.blu.BTHomeDevice;
import it.usna.shellyscan.model.device.blu.BluInetAddressAndPort;
import it.usna.shellyscan.view.MainView;
import it.usna.shellyscan.view.util.UtilMiscellaneous;
import it.usna.swing.table.ExTooltipTable;
import it.usna.swing.table.UsnaTableModel;
import it.usna.swing.texteditor.TextDocumentListener;

/**
 * List of gateways and bthpme hosts for this BLE device
 * @author usna
 */
public class DialogBluDevicesInfo extends JDialog {
	private static final long serialVersionUID = 1L;
	private ExTooltipTable btHomeTable;
	private ExTooltipTable gatewaysTable;

	public DialogBluDevicesInfo(final Window owner, BTHomeDevice d, Object bleVal, Devices appModel) {
		super(owner, LABELS.getString("dlgBLEInfoTitle"));
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		
		var bth = btHomePanel(d, bleVal, appModel);
		tabbedPane.add(LABELS.getString("dlgBLEInfoBTHomeHosts"), bth);
		
		var gw = gatewaysPanel(bleVal);
		tabbedPane.add(LABELS.getString("dlgBLEInfoBTHomeGW"), gw);
		
		getContentPane().add(tabbedPane, BorderLayout.CENTER);

		JButton btnClose = new JButton(LABELS.getString("dlgClose"));
		btnClose.setBorder(BorderFactory.createEmptyBorder(2, 7, 2, 8));
		btnClose.addActionListener(e -> dispose());
		
		JPanel panelFind = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
		panelFind.setBorder(BorderFactory.createEmptyBorder(1, 0, 3, 0));
		getContentPane().add(panelFind, BorderLayout.SOUTH);
		
		JLabel label = new JLabel(LABELS.getString("lblFilter"));
		panelFind.add(label);
		
		JTextField textFieldFilter = new JTextField();
		textFieldFilter.setColumns(18);
		textFieldFilter.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		panelFind.add(textFieldFilter);
		
		textFieldFilter.getDocument().addDocumentListener((TextDocumentListener)e -> {
			final int[] cols = new int[] {0, 1};
			String filter = textFieldFilter.getText();
			TableRowSorter<?> sorterBth = (TableRowSorter<?>)btHomeTable.getRowSorter();
			TableRowSorter<?> sorterGw = (TableRowSorter<?>)gatewaysTable.getRowSorter();
			if(filter.isEmpty()) {
				sorterBth.setRowFilter(null);
				sorterGw.setRowFilter(null);
			} else {
				filter = filter.replace("\\E", "\\e");
				sorterBth.setRowFilter(RowFilter.regexFilter("(?i).*\\Q" + filter + "\\E.*", cols));
				sorterGw.setRowFilter(RowFilter.regexFilter("(?i).*\\Q" + filter + "\\E.*", cols));
			}
		});
		textFieldFilter.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F, MainView.SHORTCUT_KEY), "find_focus_sel");
		textFieldFilter.getActionMap().put("find_focus_sel", new UsnaAction(e -> textFieldFilter.requestFocus()));
		
		final UsnaAction eraseFilterAction = new UsnaAction(this, null, "/images/erase-9-16.png", e -> {
			textFieldFilter.setText("");
			textFieldFilter.requestFocusInWindow();
			btHomeTable.clearSelection();
			gatewaysTable.clearSelection();
		});
		JButton eraseFilterButton = new JButton(eraseFilterAction);
		eraseFilterButton.setContentAreaFilled(false);
		eraseFilterButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_E, MainView.SHORTCUT_KEY), "find_erase_sel");
		eraseFilterButton.getActionMap().put("find_erase_sel", eraseFilterAction);
		eraseFilterButton.setBorder(BorderFactory.createEmptyBorder(1, 2, 1, 2));
		
		panelFind.add(eraseFilterButton);
		panelFind.add(Box.createHorizontalStrut(12));
		panelFind.add(btnClose);

		rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escape_close");
		rootPane.getActionMap().put("escape_close", new AbstractAction() {
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});

		setSize(450, 300);
		setLocationRelativeTo(owner);
		setVisible(true);
		
	}
	
	private JComponent btHomePanel(BTHomeDevice d, Object bleVal, Devices appModel) {
		UsnaTableModel tModel = new UsnaTableModel(LABELS.getString("col_device"), LABELS.getString("col_ip"));
		btHomeTable = new ExTooltipTable(tModel, true);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 3, 0));
		scrollPane.setViewportView(btHomeTable);

		var addr = (BluInetAddressAndPort)d.getAddressAndPort();
		int idxParent = appModel.indexByIP(addr.getParent());
//		if(idx >= 0) {
//			System.out.println(UtilMiscellaneous.getDescName(appModel.get(idxParent)) + " - " + addr.getRepresentation());
			tModel.addRow(UtilMiscellaneous.getDescName(appModel.get(idxParent)), addr.getParent());
//		} else {
//		}
		addr.getAlternativeParents().forEach(p -> {
			int idx = appModel.indexByIP(p);
//			if(idx >= 0) {
//				System.out.println(UtilMiscellaneous.getDescName(appModel.get(idx)) + " - " + p);
				tModel.addRow(UtilMiscellaneous.getDescName(appModel.get(idx)), p);
//			} else {
//			}
		});
		btHomeTable.sortByColumn(1, SortOrder.ASCENDING);
		btHomeTable.activateSingleCellStringCopy();
		btHomeTable.columnsWidthAdapt();
		return scrollPane;
	}
	
	private JComponent gatewaysPanel(Object bleVal) {
		UsnaTableModel tModel = new UsnaTableModel(LABELS.getString("col_device"), LABELS.getString("col_ip"), "Last seen"); // TODO
		gatewaysTable = new ExTooltipTable(tModel, true);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 3, 0));
		scrollPane.setViewportView(gatewaysTable);
		getContentPane().add(scrollPane, BorderLayout.CENTER);

		if(bleVal instanceof Collection<?> coll) {
			coll.stream().map(w -> (BLEGateway) w)/*.sorted(Comparator.reverseOrder())*/.forEach(gw -> {
//				System.out.println(UtilMiscellaneous.getDescName(gw.gw())  + " - " +  gw.gw().getAddressAndPort().getRepresentation() + " - " + gw.lastSeen());
				tModel.addRow(UtilMiscellaneous.getDescName(gw.gw()), gw.gw().getAddressAndPort().getRepresentation(), System.currentTimeMillis()/1000 - gw.lastSeen());
			});
		}
		
		gatewaysTable.sortByColumn(1, SortOrder.ASCENDING);
		gatewaysTable.activateSingleCellStringCopy();
		gatewaysTable.columnsWidthAdapt();
		return scrollPane;
	}
}