package it.usna.shellyscan.view.scheduler.walldisplay;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.io.IOException;
import java.util.List;

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
	public final static String SELECTION_EVENT = "usna_device_selection";
	private final JDialog parent;
	private final ScheduleManagerThermWD wdSceduleManager;
	private List<ThermProfile> profiles;
//	private HashMap<Integer, ArrayList<ScheduleData>> rules = new HashMap<>();
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
		
//		JPanel buttonsPanel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.LEFT, VerticalFlowLayout.LEFT));
		JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 10));
		add(buttonsPanel, BorderLayout.EAST);
//		buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));
		
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

//				try {
//					ArrayList<ScheduleData> list = new ArrayList<>();
//					Iterator<JsonNode> scIt = wdSceduleManager.getRules(p.id()).iterator();
//					while(scIt.hasNext()) {
//						JsonNode node = scIt.next();
//						// {"rule_id":"1751118368455","enable":true,"target_C":21,"profile_id":0,"timespec":"* 0 0 * * MON,TUE,WED,THU,FRI,SAT,SUN"
//						ScheduleData thisRule = new ScheduleData(node.get("rule_id").intValue(), node.get("target_C").floatValue(), CronUtils.fragStrToNum(node.get("timespec").textValue()));
//						list.add(thisRule);
//					}
//					rules.put(p.id(), list);
//				} catch (IOException e) {
//					Msg.errorMsg(parent, e);
//				}
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
	
	public void loadFromBackup() {
		// todo
	}
	
//	public boolean apply() {
//		return false;
//	}
	
//	private record ScheduleData(int ruleId, float target, String timespec) {}
}