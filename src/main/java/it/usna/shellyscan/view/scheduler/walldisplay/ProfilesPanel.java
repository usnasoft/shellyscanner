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
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SortOrder;
import javax.swing.event.ChangeEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.controller.UsnaToggleAction;
import it.usna.shellyscan.model.DeviceAPIException;
import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
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
	private ThermProfile currentProfile;

	private ExTooltipTable profilesTable;
	private UsnaTableModel tModel;
	private UsnaToggleAction enableProfilesAction;
	
	private JButton deleteProfileButton;
	private JButton duplicateProfileButton;
	private JButton selectProfileButton;

	public ProfilesPanel(JDialog parent, AbstractG2Device device, ScheduleManagerThermWD wdSceduleManager) {
		this.parentDlg = parent;
		setLayout(new BorderLayout(20, 0));
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
						manageSelection();
					}
				});
				
				columnModel.getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
					@Override
					public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
						super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
						if(currentProfile != null && profiles.get(convertRowIndexToModel(row)).id() == currentProfile.id()) {
							this.setText("<html>" + this.getText() + " <b>\u2713");
						} else {
							this.setText("<html>" + this.getText());
						}
						if(isSelected == false) {
							setBackground((row % 2 == 0) ? Main.TAB_LINE1_COLOR : Main.TAB_LINE2_COLOR);
						}
						return this;
					}
				});
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
					// No empty profile names allowed -> go back to original value
					if(value.isEmpty()) {
						getCellEditor().cancelCellEditing();
						// Overridden removeEditor() will remove the row in case of aborted new row
					} else {
						// Update existing profile
						if(mRow < profiles.size() && tModel.getValueAt(mRow, 0).equals(value) == false) {
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
						} else if(mRow >= profiles.size()) {
							try {
								int newId = wdSceduleManager.addProfiles(value);
								profiles.add(new ThermProfile(newId, value));
								tModel.setRow(mRow, value);
								setRowSelectionInterval(mRow, mRow);
							} catch (DeviceAPIException ex) {
								if(ex.getErrorCode() == DeviceAPIException.FAILED_PRECONDITION) {
									Msg.errorMsg(parent, "schAddProfileError");
									getCellEditor().cancelCellEditing();
								} else {
									Msg.errorMsg(parent, ex);
								}
							} catch (IOException ex) {
								Msg.errorMsg(parent, ex);
//								return;
							}
						}
						currentProfile = wdSceduleManager.getCurrentProfile();
					}
				} finally {
					parent.setCursor(Cursor.getDefaultCursor());
				}
				super.editingStopped(e);
			}
			
			@Override
			public void removeEditor() {
				final int mRow = convertRowIndexToModel(getEditingRow());
				super.removeEditor();
				if(mRow >= profiles.size()) { // esc (?)
					tModel.removeRow(mRow);
				}
			}
		};

		scrollPane.setViewportView(profilesTable);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());

		JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 10));
		add(buttonsPanel, BorderLayout.CENTER);

		JButton newProfileButton = new JButton(new UsnaAction(null, "schAddProfile", null, "/images/plus_transp16.png", null,  e -> {
			TableCellEditor editor = profilesTable.getCellEditor();
			if(editor != null) {
				editor.stopCellEditing();
			}
			int newRow = tModel.addRow(Main.LABELS.getString("schDefaultprofileName"));
			profilesTable.editCellAt(profilesTable.convertRowIndexToView(newRow), 0);
			profilesTable.getEditorComponent().requestFocus();
		}));
		buttonsPanel.add(newProfileButton);
		
		duplicateProfileButton = new JButton(new UsnaAction(parent, "schDuplicateProfile", null, "/images/duplicate_trasp16.png", null, e -> {
			int mRow = profilesTable.getSelectedModelRow();
			if(mRow >= 0) {
				String name = profiles.get(mRow).name() + "-new";
				try {
					int newId = wdSceduleManager.addProfiles(name);
					tModel.addRow(name);
					profiles.add(new ThermProfile(newId, name));
					ProfilesPanel.this.firePropertyChange(DUPLICATE_EVENT, profiles.get(mRow).id(), newId);
				} catch (DeviceAPIException ex) {
					if(ex.getErrorCode() == DeviceAPIException.FAILED_PRECONDITION) {
						Msg.errorMsg(parent, "schAddProfileError");
					} else {
						Msg.errorMsg(parent, ex);
					}
				} catch (IOException ex) {
					Msg.errorMsg(parent, ex);
				}
			} else {
				Msg.errorMsg(parent, "msgDuplicateProfileSelect");
			}
		}));
		duplicateProfileButton.setEnabled(false);
		buttonsPanel.add(duplicateProfileButton);

		deleteProfileButton = new JButton(new UsnaAction(parent, "schDelProfile", null, "/images/erase-9-16.png", null, e -> {
			int mRow = profilesTable.getSelectedModelRow();
			if(mRow >= 0) {
				TableCellEditor editor = profilesTable.getCellEditor();
				if(editor != null) {
					editor.stopCellEditing();
				}
				ThermProfile oldProfile = profiles.get(mRow);

				try {
					if(wdSceduleManager.getRules(oldProfile.id()).isEmpty() ||
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
		deleteProfileButton.setEnabled(false);
		buttonsPanel.add(deleteProfileButton);
		
		selectProfileButton = new JButton(new UsnaAction(parent, "schSelectProfile", null, "/images/OK_trasp.png", null, e -> {
			try {
				ThermProfile selectedProfile = profiles.get(profilesTable.getSelectedModelRow());
				wdSceduleManager.setCurrentProfile(selectedProfile.id());
				currentProfile = selectedProfile;
				selectProfileButton.setEnabled(false);
			} catch (IOException ex) {
				Msg.errorMsg(parent, ex);
				currentProfile = null;
			}
			profilesTable.repaint();
		}));
		selectProfileButton.setEnabled(false);
		buttonsPanel.add(selectProfileButton);
		
		buttonsPanel.add(Box.createHorizontalStrut(20));
		
		JButton enableButton = new JButton();
		enableButton.setContentAreaFilled(false);
		enableButton.setBorder(BorderFactory.createEmptyBorder());
		enableProfilesAction = new UsnaToggleAction(this, "/images/Standby24.png", "/images/StandbyOn24.png",
				e -> {
					String ret = wdSceduleManager.enableProfiles(true);
					if(ret != null) {
						Msg.errorMsg(parent, ret);
					}
					currentProfile = wdSceduleManager.getCurrentProfile();
					manageSelection();
					profilesTable.repaint();
				}, e -> {
					String ret = wdSceduleManager.enableProfiles(false);
					if(ret != null) {
						Msg.errorMsg(parent, ret);
					}
					currentProfile = null;
					manageSelection();
					profilesTable.repaint();
				});
		enableProfilesAction.setTooltip("lblDisabled", "lblEnabled");
		enableButton.setAction(enableProfilesAction);
		buttonsPanel.add(enableButton);
		
		fill();
	}
	
	private void manageSelection() {
		int sel = profilesTable.getSelectedModelRow();
		boolean validSelection = (sel >= 0);
		firePropertyChange(SELECTION_EVENT, null, validSelection ? profiles.get(sel).id() : -1);

		duplicateProfileButton.setEnabled(validSelection);
		deleteProfileButton.setEnabled(validSelection);
		selectProfileButton.setEnabled(validSelection && currentProfile != null && profiles.get(sel).id() != currentProfile.id());
	}
	
	private void fill() {
		try {
			profiles = wdSceduleManager.getProfiles();
			profiles.forEach(p -> tModel.addRow(p.name()) );
			
			try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e1) {}
			currentProfile = wdSceduleManager.getCurrentProfile();
			enableProfilesAction.setSelected(currentProfile != null);
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