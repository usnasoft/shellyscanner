package it.usna.shellyscan.view;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
import javax.swing.table.TableRowSorter;

import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.view.util.UtilMiscellaneous;
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

	public DialogDeviceSelection(final Window owner, UsnaEventListener<ShellyAbstractDevice, Future<?>> listener, Devices model) {
		super(owner, LABELS.getString("dlgSelectorTitle"));
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		
		UsnaTableModel tModel = new UsnaTableModel(LABELS.getString("col_device"), LABELS.getString("col_ip"));
		ExTooltipTable table = new ExTooltipTable(tModel, true);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportView(table);
		getContentPane().add(scrollPane, BorderLayout.CENTER);

		ArrayList<ShellyAbstractDevice> modelMap = new ArrayList<>();
		for(int i = 0; i < model.size(); i++) {
			ShellyAbstractDevice d = model.get(i);
			if(d.getStatus() == ShellyAbstractDevice.Status.ON_LINE) {
				tModel.addRow(UtilMiscellaneous.getExtendedHostName(d), d.getAddressAndPort());
				modelMap.add(d);
			}
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
		
		// Selection
		ExecutorService exeService = Executors.newFixedThreadPool(1);
		table.getSelectionModel().addListSelectionListener(event -> {
			if(event.getValueIsAdjusting() == false) {
				if(updateTaskFuture != null) {
					updateTaskFuture.cancel(true);
				}
				updateTaskFuture = exeService.submit(() -> {
					setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					try {
						int row = table.getSelectedModelRow();
						if(row >= 0) {
							listener.update(modelMap.get(row), updateTaskFuture);
						}
					} finally {
						setCursor(Cursor.getDefaultCursor());
					}
				});
			}
		});
		
		// Select & close (first click do select)
		table.addMouseListener(new MouseAdapter() {
		    public void mousePressed(MouseEvent evt) {
		        if (evt.getClickCount() == 2 && table.getSelectedRow() != -1) {
		        	dispose();
		        }
		    }
		});

		setSize(450, owner.getHeight());		UsnaSwingUtils.setLocationRelativeTo(this, owner, SwingConstants.RIGHT, -8, 0);
//		setLocationRelativeTo(owner);
		setVisible(true);
		table.columnsWidthAdapt();
	}
}