package it.usna.shellyscan.view.scheduler.walldisplay;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.io.IOException;
import java.util.List;

import javax.swing.BoxLayout;
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
import javax.swing.table.TableCellRenderer;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.model.device.g2.WallDisplay;
import it.usna.shellyscan.model.device.g2.modules.ScheduleManagerThermWD;
import it.usna.shellyscan.model.device.g2.modules.ScheduleManagerThermWD.ThermProfile;
import it.usna.shellyscan.view.util.Msg;
import it.usna.swing.table.ExTooltipTable;
import it.usna.swing.table.UsnaTableModel;

public class ProfilesPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private final JDialog parent;
	private final ScheduleManagerThermWD wdSceduleManager;
	private List<ThermProfile> profiles;
	private JTable profilesTable;
	private UsnaTableModel tModel;
	
	public ProfilesPanel(JDialog parent, WallDisplay device) {
		this.parent = parent;
		setLayout(new BorderLayout(0, 0));
		this.wdSceduleManager = (device != null) ? new ScheduleManagerThermWD(device) : null; // device == null -> design
		
		JScrollPane scrollPane = new JScrollPane();
		add(scrollPane, BorderLayout.WEST);
		
		tModel = new UsnaTableModel(Main.LABELS.getString("schLblProfiles"));
		profilesTable = new ExTooltipTable(tModel, true) {
			private static final long serialVersionUID = 1L;
			{
				setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				sortByColumn(0, SortOrder.ASCENDING);
				((DefaultRowSorter<?, ?>)getRowSorter()).setSortsOnUpdates(true);
			}
			
			@Override
			public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
				Component comp = super.prepareRenderer(renderer, row, column);
				if(isRowSelected(row) == false) {
					comp.setBackground((row % 2 == 0) ? Main.TAB_LINE1_COLOR : Main.TAB_LINE2_COLOR);
				}
				return comp;
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
					if(value.isEmpty()) {
						getCellEditor().cancelCellEditing();
						return;
					}
					if(mRow < profiles.size()) {
						ThermProfile oldProfile = profiles.get(mRow);
						if(oldProfile.name().equals(value) == false) {
							String ret = wdSceduleManager.renameProfiles(mRow, value);
							if(ret != null) {
								Msg.errorMsg(parent, ret);
								return;
							} else {
								profiles.set(mRow, new ThermProfile(oldProfile.id(), value));
								tModel.setRow(mRow, value);
							}
						}
					} else {
						try {
							int newId = wdSceduleManager.addProfiles(value);
							profiles.add(new ThermProfile(newId, value));
							tModel.setRow(mRow, value);
						} catch (IOException ex) {
							Msg.errorMsg(parent, ex);
							return;
						}
					}
				} finally {
					parent.setCursor(Cursor.getDefaultCursor());
				}
				super.editingStopped(e);
			}
		};
		
		fill();

		scrollPane.setViewportView(profilesTable);
		
//		JPanel buttonsPanel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.LEFT, VerticalFlowLayout.LEFT));
		JPanel buttonsPanel = new JPanel();
		add(buttonsPanel, BorderLayout.EAST);
		buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));
		
		JButton newProfileButton = new JButton(new UsnaAction("schAddProfile", e -> {
			TableCellEditor editor = profilesTable.getCellEditor();
			if(editor != null) {
				editor.stopCellEditing();
			}
			int newRow = tModel.addRow(Main.LABELS.getString("schDefaultprofileName"));
			profilesTable.editCellAt(profilesTable.convertRowIndexToView(newRow), 0);
			profilesTable.getEditorComponent().requestFocus();
		}));
		buttonsPanel.add(newProfileButton);
		
		JButton duplicateProfileButton = new JButton(new UsnaAction("labelDuplicate", e -> {
			int sel = profilesTable.getSelectedRow();
			if(sel >= 0) {
				// todo
			} else {
				Msg.errorMsg(parent, "msgDuplicateProfileSelect");
			}
		}));
		buttonsPanel.add(duplicateProfileButton);

		JButton deleteProfileButton = new JButton(new UsnaAction("schDelProfile", e -> {
			int sel = profilesTable.getSelectedRow();
			if(sel >= 0) {
				int mRow = profilesTable.convertRowIndexToModel(sel);
				TableCellEditor editor = profilesTable.getCellEditor();
				if(editor != null) {
					editor.stopCellEditing();
				}
				ThermProfile oldProfile = profiles.get(mRow);
				String ret = wdSceduleManager.deleteProfiles(oldProfile.id());
				if(ret != null) {
					Msg.errorMsg(parent, ret);
				} else {
					profiles.remove(mRow);
					tModel.removeRow(mRow);
				}
			}
		}));
		buttonsPanel.add(deleteProfileButton);
	}
	
	private void fill() {
		try {
			profiles = wdSceduleManager.getProfiles();
			profiles.forEach(p -> {
				tModel.addRow(p.name());
			});
		} catch (/*IO*/Exception e) {
			Msg.errorMsg(parent, e);
		}
	}
	
	public void refresh() {
		tModel.clear();
		fill();
	}
	
	@Override
	public void setVisible(boolean v) {
		if(v == false && profilesTable.isEditing()) {
			profilesTable.getCellEditor().stopCellEditing();
		}
		super.setVisible(v);
	}
}