package it.usna.shellyscan.view.scheduler.walldisplay;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.swing.DefaultRowSorter;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SortOrder;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.g2.WallDisplay;
import it.usna.shellyscan.model.device.g2.modules.ScheduleManagerThermWD;
import it.usna.shellyscan.model.device.g2.modules.ScheduleManagerThermWD.ThermProfile;
import it.usna.shellyscan.view.util.Msg;
import it.usna.swing.table.ExTooltipTable;
import it.usna.swing.table.UsnaTableModel;

class ProfilesPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	public final static String SELECTION_EVENT = "usna_profile_selection";
	public final static String DELETE_EVENT = "usna_profile_delete";
	public final static String DUPLICATE_EVENT = "usna_profile_duplicate";
	private final JDialog parent;
	private final ScheduleManagerThermWD wdSceduleManager;
	private List<ThermProfile> profiles;
	private ExTooltipTable profilesTable;
	private UsnaTableModel tModel;

	public ProfilesPanel(JDialog parent, WallDisplay device, ScheduleManagerThermWD wdSceduleManager) {
		this.parent = parent;
		setLayout(new BorderLayout(40, 0));
		this.wdSceduleManager = wdSceduleManager;
		
		JScrollPane scrollPane = new JScrollPane();
		add(scrollPane, BorderLayout.WEST);
		
		tModel = new UsnaTableModel(Main.LABELS.getString("schLblProfiles"));
		profilesTable = new ExTooltipTable(tModel, true) {
			private static final long serialVersionUID = 1L;
			{
				setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				sortByColumn(0, SortOrder.ASCENDING);
				((DefaultRowSorter<?, ?>)getRowSorter()).setSortsOnUpdates(true);
				
				getSelectionModel().addListSelectionListener(e -> {
					if(e.getValueIsAdjusting() == false) {
						int sel = getSelectedModelRow();
						ProfilesPanel.this.firePropertyChange(SELECTION_EVENT, null, (sel >= 0) ? profiles.get(sel).id() : -1);
					}
				});
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
					// No empty profile names -> go back to previous
					if(value.isEmpty()) {
						getCellEditor().cancelCellEditing();
						return;
					}
					// Update
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
					// New profile
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

		JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 10));
		add(buttonsPanel, BorderLayout.CENTER);

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
		
		JButton duplicateProfileButton = new JButton(new UsnaAction(parent, "schDuplicateProfile", e -> {
			int mRow = profilesTable.getSelectedModelRow();
			if(mRow >= 0) {
				String name = profiles.get(mRow).name() + "-new";
				try {
					int newId = wdSceduleManager.addProfiles(name);
					tModel.addRow(name);
					profiles.add(new ThermProfile(newId, name));
					ProfilesPanel.this.firePropertyChange(DUPLICATE_EVENT, profiles.get(mRow).id(), newId);
				} catch (IOException ex) {
					Msg.errorMsg(parent, ex);
				}
			} else {
				Msg.errorMsg(parent, "msgDuplicateProfileSelect");
			}
		}));
		buttonsPanel.add(duplicateProfileButton);

		JButton deleteProfileButton = new JButton(new UsnaAction(parent, "schDelProfile", e -> {
			int mRow = profilesTable.getSelectedModelRow();
			if(mRow >= 0) {
				TableCellEditor editor = profilesTable.getCellEditor();
				if(editor != null) {
					editor.stopCellEditing();
				}
				ThermProfile oldProfile = profiles.get(mRow);

				try {
					if(wdSceduleManager.getRules(oldProfile.id()).size() == 0 ||
							JOptionPane.showConfirmDialog(parent, LABELS.getString("msgSchDelProfile"), LABELS.getString("schDelProfile"),
							JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION) {
						try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e1) {}
						String ret = wdSceduleManager.deleteProfiles(oldProfile.id());
						if(ret != null) {
							Msg.errorMsg(parent, ret);
						} else {
							profiles.remove(mRow);
							tModel.removeRow(mRow);
							ProfilesPanel.this.firePropertyChange(DELETE_EVENT, oldProfile.id(), null);
						}
					}
				} catch (IOException ex) {
					Msg.errorMsg(parent, ex);
				}
			}
		}));
		buttonsPanel.add(deleteProfileButton);
	}
	
	private void fill() {
		try {
			profiles = wdSceduleManager.getProfiles();
			profiles.forEach(p -> tModel.addRow(p.name()) );
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
	
	public void loadFromBackup() {
		// todo
	}
}