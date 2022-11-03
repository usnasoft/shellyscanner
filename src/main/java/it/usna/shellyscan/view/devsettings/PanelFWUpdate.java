package it.usna.shellyscan.view.devsettings;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
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
import it.usna.swing.table.UsnaTableModel;
import javax.swing.JLabel;

public class PanelFWUpdate extends AbstractSettingsPanel {
	private static final long serialVersionUID = 1L;
	private final static int COL_UPDATE_IND = 0;
	private final static int COL_BETA_IND = 1;
	private ExTooltipTable table;
	private UsnaTableModel tModel = new UsnaTableModel(LABELS.getString("dlgSetColUpdate"), LABELS.getString("dlgSetColBeta"),
			LABELS.getString("col_device"), LABELS.getString("dlgSetColCurrentV"), LABELS.getString("dlgSetColLastV"), LABELS.getString("dlgSetColBetaV"));
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
//				((JComponent) getDefaultRenderer(Boolean.class)).setOpaque(true);
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
				if(column == COL_UPDATE_IND || column == COL_BETA_IND) {
					comp.setEnabled(getValueAt(row, column) != null);
				}
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
				if(r >= 0 && c >= 0 && getValueAt(r, c) == Boolean.TRUE) {
					final int toOff = (c == COL_UPDATE_IND) ? COL_BETA_IND : COL_UPDATE_IND;
					if(getValueAt(r, toOff) instanceof Boolean) {
						setValueAt(Boolean.FALSE, r, toOff);
					}
				}
				countSelection();
			}
			
			@Override
			protected String cellTooltipValue(Object value, boolean noSpace, int row, int column) {
				if(value instanceof FWCell) {
					return ((FWCell)value).getDescription();
				} else {
					return super.cellTooltipValue(value, noSpace, row, column);
				}
			}
		};

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportView(table);
		add(scrollPane, BorderLayout.CENTER);

		JPanel panel = new JPanel();
		add(panel, BorderLayout.SOUTH);
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		
		btnUnselectAll.setBorder(BorderFactory.createEmptyBorder(4, 7, 4, 7));
		panel.add(btnUnselectAll);
		btnUnselectAll.addActionListener(event -> {
			for(int i= 0; i < tModel.getRowCount(); i++) {
				if(tModel.getValueAt(i, COL_UPDATE_IND) instanceof Boolean) {
					tModel.setValueAt(Boolean.FALSE, i, COL_UPDATE_IND);
				}
				if(tModel.getValueAt(i, COL_BETA_IND) instanceof Boolean) {
					tModel.setValueAt(Boolean.FALSE, i, COL_BETA_IND);
				}
			}
			countSelection();
		});

		btnSelectStable.setBorder(BorderFactory.createEmptyBorder(4, 7, 4, 7));
		panel.add(btnSelectStable);
		btnSelectStable.addActionListener(event -> {
			for(int i= 0; i < tModel.getRowCount(); i++) {
				if(tModel.getValueAt(i, COL_UPDATE_IND) instanceof Boolean) {
					tModel.setValueAt(Boolean.TRUE, i, COL_UPDATE_IND);
					if(tModel.getValueAt(i, COL_BETA_IND) instanceof Boolean) {
						tModel.setValueAt(Boolean.FALSE, i, COL_BETA_IND);
					}
				}
			}
			countSelection();
		});
		
		btnSelectBeta.setBorder(BorderFactory.createEmptyBorder(4, 7, 4, 7));
		panel.add(btnSelectBeta);
		btnSelectBeta.addActionListener(event -> {
			for(int i= 0; i < tModel.getRowCount(); i++) {
				if(tModel.getValueAt(i, COL_BETA_IND) instanceof Boolean) {
					tModel.setValueAt(Boolean.TRUE, i, COL_BETA_IND);
					if(tModel.getValueAt(i, COL_UPDATE_IND) instanceof Boolean) {
						tModel.setValueAt(Boolean.FALSE, i, COL_UPDATE_IND);
					}
				}
			}
			countSelection();
		});
		
		panel.add(lblCount);
		panel.add(Box.createHorizontalGlue());

		JButton btnCheck = new JButton(LABELS.getString("btn_check"));
		btnCheck.setBorder(BorderFactory.createEmptyBorder(4, 7, 4, 7));
		btnCheck.setHorizontalAlignment(SwingConstants.RIGHT);
		panel.add(btnCheck);
		btnCheck.addActionListener(event -> {
			btnUnselectAll.setEnabled(false);
			btnSelectStable.setEnabled(false);
			btnSelectBeta.setEnabled(false);
			exeService.execute(() -> {
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				fwModule.parallelStream().filter(fw-> fw != null).forEach(fw -> {
					try {
						fw.chech();
					} catch (IOException e1) {
						Main.errorMsg(e1);
					}
				});
				fill();
				setCursor(Cursor.getDefaultCursor());
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
				final String id = getExtendedName(d);
				if(fu == null) {
					tModel.addRow(null, null, id, LABELS.getString("labelDevOffLIne"), null, null);
				} else if(fu.upadating()) {
					tModel.addRow(null, null, id, new FWCell(fu.current()), LABELS.getString("labelUpdating"), null);
				} else {
					boolean hasUpdate = fu.newStable() != null;
					boolean hasBeta = fu.newBeta() != null;
					globalStable |= hasUpdate;
					globalBeta |= hasBeta;
					tModel.addRow(hasUpdate ? Boolean.TRUE : null, hasBeta ? Boolean.FALSE : null, id, new FWCell(fu.current()), hasUpdate ? new FWCell(fu.newStable()) : null, hasBeta ? new FWCell(fu.newBeta()) : null);
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
		try {
			List<Callable<Void>> calls = new ArrayList<>();
			for(int i = 0; i < size; i++) {
				calls.add(new GetFWManagerCaller(devices, fwModule, i));
			}
			retriveFutures = exeService.invokeAll(calls);
			fill();
			table.columnsWidthAdapt();
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
			try {
				FirmwareManager fm = d.getFWManager();
				fwModule.set(index, fm);
			} catch (IOException e) {
				fwModule.set(index, null);
			}
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
	
	private int countSelection() {
		int countS = 0;
		int countB = 0;
		for(int i=0; i < tModel.getRowCount(); i++) {
			Object update = tModel.getValueAt(i, COL_UPDATE_IND);
			Object beta = tModel.getValueAt(i, COL_BETA_IND);
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

	private static class FWCell {
//		private final static Pattern versionPattern1 = Pattern.compile(".*/v?([\\.\\d]+(-beta.*)?)-.*");
		private final static Pattern versionPattern1 = Pattern.compile(".*/v?([\\.\\d]+(-beta.*)?)(-|@).*");
		private final String fw; // 20210429-100340/v1.10.4-g3f94cd7 - 20211222-144927/0.9.2-beta2-gc538a83 - 20211223-144928/v2.0.5@3f0fcbbe		

		public FWCell(String fw) {
			this.fw = fw;
		}

		public String getDescription() {
			return fw;
		}

		@Override
		public String toString() {
			Matcher m = versionPattern1.matcher(fw);
			return m.find() ? m.group(1) : fw;
		}
		
//		public static void main(String ...strings) {
//			Matcher m = versionPattern1.matcher("20211223-144928/v2.0.5@3f0fcbb");
//			System.out.println(m.find() ? m.group(1) : "x");
//		}
	}
}
// 346