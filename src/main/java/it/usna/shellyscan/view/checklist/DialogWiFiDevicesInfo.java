package it.usna.shellyscan.view.checklist;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.RowFilter;
import javax.swing.SortOrder;
import javax.swing.table.TableRowSorter;

import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.view.MainView;
import it.usna.shellyscan.view.util.UtilMiscellaneous;
import it.usna.swing.table.ExTooltipTable;
import it.usna.swing.table.UsnaTableModel;
import it.usna.swing.texteditor.TextDocumentListener;

/**
 * List of BLE devices for this gateway
 * @author usna
 */
public class DialogWiFiDevicesInfo extends JDialog {
	private static final long serialVersionUID = 1L;
	//test: BLE.ListPairedDevices

//	private Future<?> updateTaskFuture;

	public DialogWiFiDevicesInfo(final Window owner, Object bleVal) {
		super(owner, LABELS.getString("dlgWiFiDevInfoTitle"));
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		
		UsnaTableModel tModel = new UsnaTableModel(LABELS.getString("col_device"), LABELS.getString("col_mac"));
		ExTooltipTable table = new ExTooltipTable(tModel, true);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportView(table);
		scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 3, 0));
		getContentPane().add(scrollPane, BorderLayout.CENTER);
		
		((List<?>)bleVal).stream().forEach(blu -> {
			if(blu instanceof ShellyAbstractDevice bth) {
				tModel.addRow(UtilMiscellaneous.getDescName(bth), bth.getMacAddress());
			} else { // mac (String)
				tModel.addRow(null, blu);
			}
		});

		table.sortByColumn(1, SortOrder.ASCENDING);
		table.activateSingleCellStringCopy();

//		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
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
			TableRowSorter<?> sorter = (TableRowSorter<?>)table.getRowSorter();
			if(filter.isEmpty()) {
				sorter.setRowFilter(null);
			} else {
				filter = filter.replace("\\E", "\\e");
				sorter.setRowFilter(RowFilter.regexFilter("(?i).*\\Q" + filter + "\\E.*", cols));
			}
		});
		textFieldFilter.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F, MainView.SHORTCUT_KEY), "find_focus_sel");
		textFieldFilter.getActionMap().put("find_focus_sel", new UsnaAction(e -> textFieldFilter.requestFocus()));
		
		final UsnaAction eraseFilterAction = new UsnaAction(this, null, "/images/erase-9-16.png", e -> {
			textFieldFilter.setText("");
			textFieldFilter.requestFocusInWindow();
			table.clearSelection();
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

		setSize(450, 300);//		UsnaSwingUtils.setLocationRelativeTo(this, owner, SwingConstants.RIGHT, -8, 0);
		setLocationRelativeTo(owner);
		setVisible(true);
		table.columnsWidthAdapt();
	}
}