package it.usna.shellyscan.view.devsettings;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.view.MainView;
import it.usna.shellyscan.view.util.UtilMiscellaneous;
import it.usna.swing.BusyCursor;
import it.usna.swing.UsnaSwingUtils;
import it.usna.swing.table.ExTooltipTable;
import it.usna.swing.table.UsnaTableModel;
import it.usna.swing.texteditor.TextDocumentListener;
import it.usna.util.UsnaEventListener;

/**
 * Single device selection dialog - only ShellyAbstractDevice.Status.ON_LINE are listed
 * @author usna
 */
public class DialogDeviceSelection extends JDialog {
	private static final long serialVersionUID = 1L;

	private Future<?> updateTaskFuture;

	public DialogDeviceSelection(final DialogDeviceSettings owner, UsnaEventListener<ShellyAbstractDevice, Future<?>> listener, Devices model) {
		super(owner, LABELS.getString("dlgSelectorTitle"));
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		
		UsnaTableModel tModel = new UsnaTableModel(LABELS.getString("col_device"), LABELS.getString("col_ip"));
		ExTooltipTable table = new ExTooltipTable(tModel, true) {
			@Override
			public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
				Component comp = super.prepareRenderer(renderer, row, column);
//				if()
				return comp;
			}
		};

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 3, 0));
		scrollPane.setViewportView(table);
		getContentPane().add(scrollPane, BorderLayout.CENTER);

		for(int i = 0; i < model.size(); i++) {
			ShellyAbstractDevice d = model.get(i);
//			if(d.getStatus() == ShellyAbstractDevice.Status.ON_LINE) {
				tModel.addRow(UtilMiscellaneous.getExtendedHostName(d), d.getAddressAndPort(), d);
//			}
		}
		table.sortByColumn(1, SortOrder.ASCENDING);

		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
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
		
		RowFilter<TableModel, Integer> onLineFilter = new RowFilter<TableModel, Integer>() {
			@Override
			public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
				return ((ShellyAbstractDevice)entry.getModel().getValueAt(entry.getIdentifier(), 2)).getStatus() == ShellyAbstractDevice.Status.ON_LINE;
			}
		};
		textFieldFilter.getDocument().addDocumentListener((TextDocumentListener)e -> {
			final int[] cols = new int[] {0, 1};
			String filter = textFieldFilter.getText();
			TableRowSorter<? extends TableModel> sorter = (TableRowSorter<? extends TableModel>)table.getRowSorter();
			if(filter.isEmpty()) {
//				sorter.setRowFilter(null);
				
				sorter.setRowFilter(onLineFilter);
			} else {
				filter = filter.replace("\\E", "\\e");
//				List<RowFilter<TableModel, Integer>> filters = new ArrayList<>(2);
//				filters.add(onLineFilter);
//				filters.add(RowFilter.regexFilter("(?i).*\\Q" + filter + "\\E.*", cols));
				
//				sorter.setRowFilter(RowFilter.regexFilter("(?i).*\\Q" + filter + "\\E.*", cols));
				List<RowFilter<TableModel, Integer>> filters = List.of(RowFilter.regexFilter("(?i).*\\Q" + filter + "\\E.*", cols), onLineFilter);
				sorter.setRowFilter(RowFilter.andFilter(filters));
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
		
		// Selection
		ExecutorService exeService = Executors.newFixedThreadPool(1);
		table.getSelectionModel().addListSelectionListener(event -> {
			if(!event.getValueIsAdjusting()) {
				if(updateTaskFuture != null) {
					updateTaskFuture.cancel(true);
				}
				updateTaskFuture = exeService.submit(() -> {
					try (var cres = new BusyCursor(this)) {
						int row = table.getSelectedModelRow();
						if(row >= 0) {
							listener.update((ShellyAbstractDevice)tModel.getValueAt(2, row), updateTaskFuture);
						}
					}
				});
			}
		});
		
		// Select & close (first click do select)
		table.addMouseListener(new MouseAdapter() {
			@Override
		    public void mousePressed(MouseEvent evt) {
		        if (evt.getClickCount() == 2 && table.getSelectedRow() != -1) {
		        	dispose();
		        }
		    }
		});
		
		rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escape_close");
		rootPane.getActionMap().put("escape_close", new AbstractAction() {
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});

		setSize(450, owner.getHeight());		UsnaSwingUtils.setLocationRelativeTo(this, owner, SwingConstants.RIGHT, -8, 0);
//		setLocationRelativeTo(owner);
		setVisible(true);
		table.columnsWidthAdapt();
	}
}