package it.usna.shellyscan.view.devsettings;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FontMetrics;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import it.usna.shellyscan.model.device.FirmwareManager;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.view.DevicesTable;
import it.usna.shellyscan.view.util.Msg;
import it.usna.shellyscan.view.util.UtilCollecion;
import it.usna.swing.table.ExTooltipTable;
import it.usna.swing.table.UsnaTableModel;

public class PanelFWUpdate extends AbstractSettingsPanel {
	private static final long serialVersionUID = 1L;
	private final static int COL_STATUS = 0;
	private final static int COL_CURRENT = 2;
	private final static int COL_STABLE = 3;
	private final static int COL_BETA = 4;
	private ExTooltipTable table;
	private UsnaTableModel tModel = new UsnaTableModel("", LABELS.getString("col_device"), LABELS.getString("dlgSetColCurrentV"), LABELS.getString("dlgSetColLastV"), LABELS.getString("dlgSetColBetaV"));
	private List<FirmwareManager> fwModule = new ArrayList<>();
	
	private JButton btnUnselectAll = new JButton(LABELS.getString("btn_unselectAll"));
	private JButton btnSelectStable = new JButton(LABELS.getString("btn_selectAllSta"));
	private JButton btnSelectBeta = new JButton(LABELS.getString("btn_selectAllbeta"));
	private JLabel lblCount = new JLabel();
	
	/**
	 * @wbp.nonvisual location=61,49
	 */
	private ExecutorService exeService = Executors.newFixedThreadPool(25);
	private List<Future<Void>> retriveFutures;
	public PanelFWUpdate(List<ShellyAbstractDevice> devices) {
		super(devices);
		setLayout(new BorderLayout(0, 0));

		table = new ExTooltipTable(tModel) {
			private static final long serialVersionUID = 1L;
			{
				getTableHeader().setReorderingAllowed(false);
				((JCheckBox) getDefaultRenderer(Boolean.class)).setOpaque(true);
				((JCheckBox) getDefaultRenderer(Boolean.class)).setHorizontalAlignment(JCheckBox.LEFT);
				TableCellRenderer fwRendered = new FWCellRendered();
				getColumnModel().getColumn(COL_STABLE).setCellRenderer(fwRendered);
				getColumnModel().getColumn(COL_BETA).setCellRenderer(fwRendered);
				columnModel.getColumn(COL_STATUS).setMaxWidth(DevicesTable.ONLINE_BULLET.getIconWidth() + 4);
			}

			@Override
			public boolean isCellEditable(final int row, final int column) {
				return getValueAt(row, column) instanceof Boolean;
			}

			@Override
			public Component prepareEditor(TableCellEditor editor, int row, int column) {
				JCheckBox comp = (JCheckBox)super.prepareEditor(editor, row, column);
				FirmwareManager fw = fwModule.get(table.convertRowIndexToModel(row));
				comp.setText(FirmwareManager.getShortVersion(column == COL_STABLE ? fw.newStable() : fw.newBeta()));
				comp.setBackground(table.getSelectionBackground());
				comp.setForeground(table.getSelectionForeground());
				comp.setHorizontalAlignment(JLabel.LEFT);
				return comp;
			}

			@Override
			public void editingStopped(ChangeEvent e) {
				final int r = getEditingRow();
				final int c = getEditingColumn();
				super.editingStopped(e);
				if(r >= 0 && c >= 0 && getValueAt(r, c) == Boolean.TRUE) {
					final int toOff = (c == COL_STABLE) ? COL_BETA : COL_STABLE;
					if(getValueAt(r, toOff) instanceof Boolean) {
						setValueAt(Boolean.FALSE, r, toOff);
					}
				}
				countSelection();
			}
			
			@Override
			protected String cellTooltipValue(Object value, boolean noSpace, int row, int column) {
				FirmwareManager fw = fwModule.get(convertRowIndexToModel(row));
				if(column == COL_CURRENT && fw != null) {
					return fw.current();
				} else if(column == COL_STABLE && fw != null) {
					return fw.newStable();
				} else if(column == COL_BETA && fw != null) {
					return fw.newBeta();
				}
				return super.cellTooltipValue(value, noSpace, row, column);
			}
		};

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportView(table);
		add(scrollPane, BorderLayout.CENTER);

		JPanel panel = new JPanel();
		add(panel, BorderLayout.SOUTH);
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		
		panel.add(Box.createHorizontalStrut(2));
		
		btnUnselectAll.setBorder(BorderFactory.createEmptyBorder(4, 7, 4, 7));
		panel.add(btnUnselectAll);
		btnUnselectAll.addActionListener(event -> {
			for(int i= 0; i < tModel.getRowCount(); i++) {
				if(tModel.getValueAt(i, COL_STABLE) instanceof Boolean) {
					tModel.setValueAt(Boolean.FALSE, i, COL_STABLE);
				}
				if(tModel.getValueAt(i, COL_BETA) instanceof Boolean) {
					tModel.setValueAt(Boolean.FALSE, i, COL_BETA);
				}
			}
			countSelection();
		});

		btnSelectStable.setBorder(BorderFactory.createEmptyBorder(4, 7, 4, 7));
		panel.add(btnSelectStable);
		btnSelectStable.addActionListener(event -> {
			for(int i= 0; i < tModel.getRowCount(); i++) {
				if(tModel.getValueAt(i, COL_STABLE) instanceof Boolean) {
					tModel.setValueAt(Boolean.TRUE, i, COL_STABLE);
					if(tModel.getValueAt(i, COL_BETA) instanceof Boolean) {
						tModel.setValueAt(Boolean.FALSE, i, COL_BETA);
					}
				}
			}
			countSelection();
		});
		
		btnSelectBeta.setBorder(BorderFactory.createEmptyBorder(4, 7, 4, 7));
		panel.add(btnSelectBeta);
		btnSelectBeta.addActionListener(event -> {
			for(int i= 0; i < tModel.getRowCount(); i++) {
				if(tModel.getValueAt(i, COL_BETA) instanceof Boolean) {
					tModel.setValueAt(Boolean.TRUE, i, COL_BETA);
					if(tModel.getValueAt(i, COL_STABLE) instanceof Boolean) {
						tModel.setValueAt(Boolean.FALSE, i, COL_STABLE);
					}
				}
			}
			countSelection();
		});
		
		panel.add(lblCount);
		panel.add(Box.createHorizontalGlue());

		JButton btnCheck = new JButton(LABELS.getString("btn_check"));
		btnCheck.setBorder(BorderFactory.createEmptyBorder(4, 7, 4, 7));
		panel.add(btnCheck);
		
		panel.add(Box.createHorizontalStrut(2));
		
		btnCheck.addActionListener(event -> {
			btnCheck.setEnabled(false);
			btnUnselectAll.setEnabled(false);
			btnSelectStable.setEnabled(false);
			btnSelectBeta.setEnabled(false);
			exeService.execute(() -> {
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				for(int i = 0; i < devices.size(); i++) {
					tModel.setValueAt(DevicesTable.UPDATING_BULLET, i, COL_STATUS);
				}
				try {
					fwModule.parallelStream()./*filter(fw-> fw != null).*/forEach(fw -> {
						try {
							fw.chech();
						} catch (IOException e1) {
							Msg.errorMsg(e1);
						}
					});
					fill();
					btnCheck.setEnabled(true);
				} finally {
					setCursor(Cursor.getDefaultCursor());
				}
			});
		});
	}

	private void fill() {
		tModel.clear();
		boolean globalStable = false;
		boolean globalBeta = false;
		for(int i = 0; i < devices.size(); i++) {
			if(Thread.interrupted() == false) {
				ShellyAbstractDevice d = devices.get(i);
				FirmwareManager fu = fwModule.get(i);
				final String id = UtilCollecion.getExtendedHostName(d);
				if(fu.upadating()) {
					tModel.addRow(DevicesTable.getStatusIcon(d), id, FirmwareManager.getShortVersion(fu.current()), LABELS.getString("labelUpdating"), null);
				} else {
					boolean hasUpdate = fu.newStable() != null;
					boolean hasBeta = fu.newBeta() != null;
					globalStable |= hasUpdate;
					globalBeta |= hasBeta;
//					if(fu.isValid()) {
						tModel.addRow(DevicesTable.getStatusIcon(d), id, FirmwareManager.getShortVersion(fu.current()), hasUpdate ? Boolean.TRUE : null, hasBeta ? Boolean.FALSE : null);
//					}
				}
			}
			btnUnselectAll.setEnabled(globalStable || globalBeta);
			btnSelectStable.setEnabled(globalStable);
			btnSelectBeta.setEnabled(globalBeta);
			countSelection();
		}
	}

	@Override
	public String showing() throws InterruptedException {
		lblCount.setText("");
		btnUnselectAll.setEnabled(false);
		btnSelectStable.setEnabled(false);
		btnSelectBeta.setEnabled(false);
		final int size = devices.size();
		fwModule = Arrays.asList(new FirmwareManager[size]);
		tModel.clear();
		try {
			List<Callable<Void>> calls = new ArrayList<>();
			for(int i = 0; i < size; i++) {
				calls.add(new GetFWManagerCaller(devices, fwModule, i));
			}
			retriveFutures = exeService.invokeAll(calls);
			fill();

			table.columnsWidthAdapt();
			final FontMetrics fm = getGraphics().getFontMetrics();
			TableColumn stableC = table.getColumnModel().getColumn(COL_STABLE);
			stableC.setPreferredWidth(Math.max(SwingUtilities.computeStringWidth(fm, "0.12.0"), stableC.getPreferredWidth()));
			TableColumn betaC = table.getColumnModel().getColumn(COL_BETA);
			betaC.setPreferredWidth(Math.max(SwingUtilities.computeStringWidth(fm, "0.12.0-beta1"), betaC.getPreferredWidth()));
		} catch (/*IOException |*/ RuntimeException e) {
			return e.toString();
		}
		return null;
	}

	public void hiding() {
		if(retriveFutures != null) {
			retriveFutures.forEach(f -> f.cancel(true));
		}
	}
	
	private static class GetFWManagerCaller implements Callable<Void> {
		private final int index;
		private final List<FirmwareManager> fwModule;
		private final List<ShellyAbstractDevice> devices;
		
		private GetFWManagerCaller(List<ShellyAbstractDevice> devices, List<FirmwareManager> fwModule, int index) {
			this.index = index;
			this.fwModule = fwModule;
			this.devices = devices;
		}

		@Override
		public Void call() {
			final ShellyAbstractDevice d = devices.get(index);
			FirmwareManager fm = d.getFWManager();
			fwModule.set(index, fm);
			return null;
		}
	}

	@Override
	public String apply() {
		int count = countSelection();
		if(count > 0 && JOptionPane.showConfirmDialog(this,
				String.format(LABELS.getString("dlgSetConfirmUpdate"), count), LABELS.getString("dlgSetFWUpdate"),
				JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
			String res = "";
			for(int i = 0; i < devices.size(); i++) {
				Object update = tModel.getValueAt(i, COL_STABLE);
				Object beta = tModel.getValueAt(i, COL_BETA);
				if(update instanceof Boolean && ((Boolean)update) == Boolean.TRUE) {
					String msg = fwModule.get(i).update(true);
					if(msg != null && LABELS.containsKey(msg)) {
						msg = LABELS.getString(msg);
					}
					res += UtilCollecion.getFullName(devices.get(i)) + " - " + ((msg == null) ? LABELS.getString("labelUpdating") : LABELS.getString("labelError") + ": " + msg) + "\n";
				} else if(beta instanceof Boolean && ((Boolean)beta) == Boolean.TRUE) {
					String msg = fwModule.get(i).update(false);
					if(msg != null && LABELS.containsKey(msg)) {
						msg = LABELS.getString(msg);
					}
					res += UtilCollecion.getFullName(devices.get(i)) + " - " + ((msg == null) ? LABELS.getString("labelUpdatingBeta") : LABELS.getString("labelError") + ": " + msg) + "\n";
				}
			}
			fill();
			return res;
		}
		return null;
	}
	
	private int countSelection() {
		int countS = 0;
		int countB = 0;
		for(int i = 0; i < tModel.getRowCount(); i++) {
			Object update = tModel.getValueAt(i, COL_STABLE);
			Object beta = tModel.getValueAt(i, COL_BETA);
			if(update instanceof Boolean && ((Boolean)update) == Boolean.TRUE) {
				countS++;
			}
			if(beta instanceof Boolean && ((Boolean)beta) == Boolean.TRUE) {
				countB++;
			}
		}
		lblCount.setText(String.format(LABELS.getString("lbl_update_count"), countS, countB));
		return countS + countB;
	}
	
	private class FWCellRendered implements TableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			if(value == null || value instanceof Boolean) {
				JCheckBox c = (JCheckBox)table.getDefaultRenderer(Boolean.class).getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				if(value == null) {
					c.setEnabled(false);
					c.setText("");
				} else {
					c.setEnabled(true);
					FirmwareManager fw = fwModule.get(table.convertRowIndexToModel(row));
					if(fw != null) {
						c.setText(FirmwareManager.getShortVersion(column == COL_STABLE ? fw.newStable() : fw.newBeta()));
					}
				}
				return c;
			} else {
				return table.getDefaultRenderer(String.class).getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			}
		}
	}
}
// 346 - 362