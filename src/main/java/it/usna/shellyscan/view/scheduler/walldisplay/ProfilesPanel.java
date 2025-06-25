package it.usna.shellyscan.view.scheduler.walldisplay;

import java.awt.BorderLayout;
import java.awt.Cursor;

import javax.swing.DefaultRowSorter;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SortOrder;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.model.device.g2.WallDisplay;
import it.usna.shellyscan.model.device.g2.modules.ScheduleManagerThermWD;
import it.usna.swing.VerticalFlowLayout;
import it.usna.swing.table.ExTooltipTable;
import it.usna.swing.table.UsnaTableModel;

public class ProfilesPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private final ScheduleManagerThermWD wdSceduleManager;
	private JTable table;
	
	public ProfilesPanel(JDialog parent, WallDisplay device) {
		setLayout(new BorderLayout(0, 0));
		this.wdSceduleManager = new ScheduleManagerThermWD(device);
		
		JScrollPane scrollPane = new JScrollPane();
		add(scrollPane, BorderLayout.WEST);
		
		UsnaTableModel tModel = new UsnaTableModel(Main.LABELS.getString("schLblProfiles"));
		table = new ExTooltipTable(tModel, true) {
			private static final long serialVersionUID = 1L;
			{
				setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				sortByColumn(0, SortOrder.ASCENDING);
				((DefaultRowSorter<?, ?>)getRowSorter()).setSortsOnUpdates(true);
			}
		
			@Override
			public boolean isCellEditable(int r, int c) {
				return true;
			}
			
			@Override
			public void editingStopped(ChangeEvent e) {
				parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				try {
					final int mRow = convertRowIndexToModel(getEditingRow());
					String value = (String) getCellEditor().getCellEditorValue();
					// todo
					tModel.setRow(mRow, value);
				} finally {
					parent.setCursor(Cursor.getDefaultCursor());
				}
			}
		};
		scrollPane.setViewportView(table);
		
		JPanel buttonsPanel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.LEFT, VerticalFlowLayout.LEFT));
		add(buttonsPanel, BorderLayout.EAST);
		
		JButton newProfileButton = new JButton(new UsnaAction("schAddProfile", e -> {
			TableCellEditor editor = table.getCellEditor();
			if(editor != null) {
				editor.stopCellEditing();
			}
			int newRow = tModel.addRow(Main.LABELS.getString("schDefaultprofileName"));
			table.editCellAt(table.convertRowIndexToView(newRow), 0);
			table.getEditorComponent().requestFocus();
		}));
		buttonsPanel.add(newProfileButton);
		JButton deleteProfileButton = new JButton(new UsnaAction("schDelProfile", e -> {
			int sel = table.getSelectedRow();
			if(sel >= 0) {
				TableCellEditor editor = table.getCellEditor();
				if(editor != null) {
					editor.stopCellEditing();
				}
				tModel.removeRow(table.convertRowIndexToModel(sel));
			}
		}));
		buttonsPanel.add(deleteProfileButton);
	}
}
