package it.usna.shellyscan.view.devsettingspanel;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.model.device.FirmwareManager;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.swing.table.ExTooltipTable;
import it.usna.swing.table.TooltipTable;
import it.usna.swing.table.UsnaTableModel;

public class PanelFWUpdate extends AbstractSettingsPanel {
	private static final long serialVersionUID = 1L;
	private final static int COL_UPDATE_IND = 0;
	private final static int COL_BETA_IND = 1;
	private ExTooltipTable table;
	private UsnaTableModel tModel = new UsnaTableModel(LABELS.getString("dlgSetColUpdate"), LABELS.getString("dlgSetColBeta"),
			LABELS.getString("col_device"), LABELS.getString("dlgSetColCurrentV"), LABELS.getString("dlgSetColLastV"), LABELS.getString("dlgSetColBetaV"));
	private List<FirmwareManager> fwModule = new ArrayList<>();
	/**
	 * @wbp.nonvisual location=61,49
	 */
	public PanelFWUpdate(List<ShellyAbstractDevice> devices, ExecutorService exeService) {
		super(devices);
		setLayout(new BorderLayout(0, 0));

		table = new ExTooltipTable(tModel) {
			private static final long serialVersionUID = 1L;
			{
				((JComponent) getDefaultRenderer(Boolean.class)).setOpaque(true);
				getColumnModel().getColumn(COL_UPDATE_IND).setCellRenderer(getDefaultRenderer(Boolean.class));
				getColumnModel().getColumn(COL_BETA_IND).setCellRenderer(getDefaultRenderer(Boolean.class));
			}

			@Override
			public boolean isCellEditable(final int row, final int column) {
				return getValueAt(row, column) instanceof Boolean;
			}

			@Override
			public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
				Component comp = super.prepareRenderer(renderer, row, column);
				comp.setEnabled(getValueAt(row, column) != null);
				return comp;
			}

			@Override
			public Component prepareEditor(TableCellEditor editor, int row, int column) {
				JComponent comp = (JComponent)super.prepareEditor(editor, row, column);
				//comp.setOpaque(true);
				comp.setBackground(table.getSelectionBackground());
				return comp;
			}

			@Override
			public void editingStopped(ChangeEvent e) {
				final int r = getEditingRow();
				final int c = getEditingColumn();
				super.editingStopped(e);
				Object v;
				if(r >= 0 && c>= 0 && (v = getValueAt(r, c)) instanceof Boolean && ((Boolean)v) == Boolean.TRUE) {
					final int spegnere = (c == COL_UPDATE_IND) ? COL_BETA_IND : COL_UPDATE_IND;
					if(getValueAt(r, spegnere) instanceof Boolean) {
						setValueAt(Boolean.FALSE, r, spegnere);
					}
				}
			}
		};

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportView(table);
		add(scrollPane, BorderLayout.CENTER);

		JPanel panel = new JPanel();
		FlowLayout flowLayout = (FlowLayout) panel.getLayout();
		flowLayout.setVgap(1);
		flowLayout.setAlignment(FlowLayout.RIGHT);
		add(panel, BorderLayout.SOUTH);

		JButton btnCheck = new JButton(LABELS.getString("btn_check"));
		btnCheck.setHorizontalAlignment(SwingConstants.RIGHT);
		panel.add(btnCheck);
		btnCheck.addActionListener(event -> {
			exeService.execute(() -> {
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				try {
					for(FirmwareManager fu: fwModule) {
						try {
							fu.chech();
						} catch (IOException e1) {
							Main.errorMsg(e1);
						}
					}
					fill();
				} finally {
					setCursor(Cursor.getDefaultCursor());
				}
			});
		});
	}

	private void fill() {
		try {
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			tModel.clear();
			for(int i = 0; i < devices.size(); i++) {
				if(Thread.interrupted() == false /*&& fu != null*/) {
					ShellyAbstractDevice d = devices.get(i);
					FirmwareManager fu = fwModule.get(i);
					final String dName = d.getName();
					final String id = d.getHostname() + " - " + (dName.length() > 0 ? dName : LABELS.getString("dlgSetUnknownName"));
					if(fu.upadating()) {
						tModel.addRow(null, null, id, new FWCell(fu.current()), LABELS.getString("labelUpdating"), null);
					} else {
						boolean hasUpdate = fu.newStable() != null;
						boolean hasBeta = fu.newBeta() != null;
						tModel.addRow(hasUpdate ? Boolean.TRUE : null, hasBeta ? Boolean.FALSE : null, id, new FWCell(fu.current()), hasUpdate ? new FWCell(fu.newStable()) : null, hasBeta ? new FWCell(fu.newBeta()) : null);
					}

				}
			}
		} finally {
			setCursor(Cursor.getDefaultCursor());
		}
	}

	@Override
	public String showing() {
		if(tModel.getRowCount() == 0) {
			fwModule.clear();
			ShellyAbstractDevice d = null;
			try {
				for(int i = 0; i < devices.size(); i++) {
					if(Thread.interrupted() == false) {
						d = devices.get(i);
						fwModule.add(d.getFWManager());
					}
				}
				fill();
				table.columnsWidthAdapt();
			} catch (IOException | RuntimeException e) {
				return getExtendedName(d) + ": " + e.getMessage();
			}/* catch(Exception e) {
				Main.errorMsg(e);
				return "";
			}*/
		}
		return null;
	}

	@Override
	public String apply() {
		int count = 0;
		for(int i=0; i < devices.size(); i++) {
			Object update = tModel.getValueAt(i, COL_UPDATE_IND);
			Object beta = tModel.getValueAt(i, COL_BETA_IND);
			if(update instanceof Boolean && ((Boolean)update) == Boolean.TRUE || beta instanceof Boolean && ((Boolean)beta) == Boolean.TRUE) {
				count++;
			}
		}
		if(count > 0 && JOptionPane.showConfirmDialog(this,
				String.format(LABELS.getString("dlgSetConfirmUpdate"), count), LABELS.getString("dlgSetFWUpdate"),
				JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
			String res = "";
			for(int i=0; i < devices.size(); i++) {
				Object update = tModel.getValueAt(i, COL_UPDATE_IND);
				Object beta = tModel.getValueAt(i, COL_BETA_IND);
				if(update instanceof Boolean && ((Boolean)update) == Boolean.TRUE) {
					String msg = fwModule.get(i).update(true);
					if(msg != null && LABELS.containsKey(msg)) {
						msg = LABELS.getString(msg);
					}
					res += getExtendedName(devices.get(i)) + " - " + ((msg == null) ? LABELS.getString("labelUpdating") : LABELS.getString("labelError") + ": " + msg) + "\n";
				} else if(beta instanceof Boolean && ((Boolean)beta) == Boolean.TRUE) {
					String msg = fwModule.get(i).update(false);
					if(msg != null && LABELS.containsKey(msg)) {
						msg = LABELS.getString(msg);
					}
					res += getExtendedName(devices.get(i)) + " - " + ((msg == null) ? LABELS.getString("labelUpdatingBeta") : LABELS.getString("labelError") + ": " + msg) + "\n";
				}
			}
			fill();
			return res;
		}
		return null;
	}

	private static class FWCell implements TooltipTable.Cell {
		private final static Pattern versionPattern1 = Pattern.compile(".*/v?([\\.\\d]+(-beta.*)?)-.*");
		private final String fw; // 20210429-100340/v1.10.4-g3f94cd7 - 20211222-144927/0.9.2-beta2-gc538a83

		public FWCell(String fw) {
			this.fw = fw;
		}

		@Override
		public String getDescription() {
			return fw;
		}

		@Override
		public String toString() {
			Matcher m = versionPattern1.matcher(fw);
			return m.find() ? m.group(1) : fw;
		}
		
//		public static void main(String ...strings) {
//			Matcher m = versionPattern1.matcher("20211222-144927/0.9.2-beta2-gc538a83");
//			System.out.println(m.find() ? m.group(1) : "x");
//		}
	}
}