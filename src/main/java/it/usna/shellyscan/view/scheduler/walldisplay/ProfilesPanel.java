package it.usna.shellyscan.view.scheduler.walldisplay;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.swing.BorderFactory;
import javax.swing.Box;
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
import it.usna.shellyscan.controller.UsnaToggleAction;
import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.g2.WallDisplay;
import it.usna.shellyscan.model.device.g2.modules.ScheduleManagerThermWD;
import it.usna.shellyscan.model.device.g2.modules.ScheduleManagerThermWD.ThermProfile;
import it.usna.shellyscan.view.util.Msg;
import it.usna.swing.table.ExTooltipTable;
import it.usna.swing.table.UsnaTableModel;

/**
 * Thermostat profiles as a sortable and editable (names) list + create, delete, duplicate buttons
 */
class ProfilesPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	public static final String SELECTION_EVENT = "usna_profile_selection";
	public static final String DELETE_EVENT = "usna_profile_delete";
	public static final String DUPLICATE_EVENT = "usna_profile_duplicate";
	private final JDialog parentDlg;
	private final ScheduleManagerThermWD wdSceduleManager;
	private List<ThermProfile> profiles;
	
//	private JComboBox<ThermProfile> activeProfile;
	private ExTooltipTable profilesTable;
	private UsnaTableModel tModel;
	private UsnaToggleAction enableprofilesAction;

	public ProfilesPanel(JDialog parent, WallDisplay device, ScheduleManagerThermWD wdSceduleManager) {
		this.parentDlg = parent;
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
							String ret = wdSceduleManager.renameProfiles(oldProfile.id(), value);
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

		scrollPane.setViewportView(profilesTable);

		JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 10));
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
							refresh();
							ProfilesPanel.this.firePropertyChange(DELETE_EVENT, oldProfile.id(), null);
						}
					}
				} catch (IOException ex) {
					Msg.errorMsg(parent, ex);
				}
			}
		}));
		buttonsPanel.add(deleteProfileButton);
		
		buttonsPanel.add(Box.createHorizontalStrut(30));
		
		JButton enableButton = new JButton();
		enableButton.setContentAreaFilled(false);
		enableButton.setBorder(BorderFactory.createEmptyBorder());
		enableprofilesAction = new UsnaToggleAction(this, "/images/Standby24.png", "/images/StandbyOn24.png",
				e -> {
					String ret = wdSceduleManager.enableProfiles(true);
					if(ret != null) {
						Msg.errorMsg(parent, ret);
					}
				}, e -> {
					String ret = wdSceduleManager.enableProfiles(false);
					if(ret != null) {
						Msg.errorMsg(parent, ret);
					}
				});
		enableprofilesAction.setTooltip("lblDisabled", "lblEnabled");
		enableButton.setAction(enableprofilesAction);
		buttonsPanel.add(enableButton);
//		add(enableButton, BorderLayout.EAST);
		
		fill();
	}
	
	private void fill() {
		try {
			profiles = wdSceduleManager.getProfiles();
			profiles.forEach(p -> tModel.addRow(p.name()) );
			
			try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e1) {}
			ThermProfile current = wdSceduleManager.getCurrentProfile();
			enableprofilesAction.setSelected(current != null);
		} catch (/*IO*/Exception e) {
			Msg.errorMsg(parentDlg, e);
		}
	}
	
	public void refresh() {
		int sel = profilesTable.getSelectedRow();
		tModel.clear();
		fill();
		if(sel >= 0 && profilesTable.getRowCount() > sel) {
			profilesTable.setRowSelectionInterval(sel, sel);
		}
	}
	
	@Override
	public void setVisible(boolean v) {
		if(v == false && profilesTable.isEditing()) {
			profilesTable.getCellEditor().stopCellEditing();
		}
		super.setVisible(v);
	}
}