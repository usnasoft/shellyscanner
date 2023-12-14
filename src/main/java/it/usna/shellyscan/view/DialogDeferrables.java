package it.usna.shellyscan.view;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.controller.DeferrableTask;
import it.usna.shellyscan.controller.DeferrableTask.Status;
import it.usna.shellyscan.controller.DeferrablesContainer;
import it.usna.shellyscan.controller.DeferrablesContainer.DeferrableRecord;
import it.usna.shellyscan.model.Devices;
import it.usna.swing.table.ExTooltipTable;
import it.usna.swing.table.UsnaTableModel;
import it.usna.util.UsnaEventListener;

public class DialogDeferrables extends JFrame implements UsnaEventListener<DeferrableTask.Status, Integer> {
	private static final long serialVersionUID = 1L;
	private final UsnaTableModel tModel = new UsnaTableModel(LABELS.getString("col_time"), LABELS.getString("col_devName"), LABELS.getString("col_actionDesc"), LABELS.getString("col_status"));
	private final ExTooltipTable table = new ExTooltipTable(tModel);
	private final DeferrablesContainer deferrables;

	public DialogDeferrables(/*Window owner,*/ Devices model) {
		super(LABELS.getString("labelShowDeferrables"));
		setIconImage(Main.ICON);

		deferrables = DeferrablesContainer.getInstance();

		table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
			private static final long serialVersionUID = 1L;
			{
				setIconTextGap(6);
			}

			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				Status status = deferrables.get(table.convertRowIndexToModel(row)).getStatus();
				if(status == Status.SUCCESS) {
					setIcon(new ImageIcon(DialogDeferrables.class.getResource("/images/def_success.png")));
				} else if(status == Status.WAITING) {
					setIcon(new ImageIcon(DialogDeferrables.class.getResource("/images/def_waiting.png")));
				} else if(status == Status.CANCELLED) {
					setIcon(new ImageIcon(DialogDeferrables.class.getResource("/images/def_cancelled.png")));
				} else if(status == Status.RUNNING) {
					setIcon(new ImageIcon(DialogDeferrables.class.getResource("/images/def_running.png")));
				} else if(status == Status.FAIL) {
					setIcon(new ImageIcon(DialogDeferrables.class.getResource("/images/def_error.png")));
				}
				return this;
			}
		});

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportView(table);
		getContentPane().add(scrollPane, BorderLayout.CENTER);

		JPanel buttonsPanel = new JPanel();
		JButton abortButton = new JButton(LABELS.getString("btnAbort"));
		abortButton.addActionListener(e -> {
			for(int r: table.getSelectedModelRows()) {
				deferrables.cancel(r);
			}
			abortButton.setEnabled(false);
		});
		abortButton.setEnabled(false);

		JButton closeButton = new JButton(LABELS.getString("dlgClose"));
		closeButton.addActionListener(e -> dispose());

		buttonsPanel.add(abortButton);
		buttonsPanel.add(closeButton);

		getContentPane().add(buttonsPanel, BorderLayout.SOUTH);

		table.getSelectionModel().addListSelectionListener(e -> {
			abortButton.setEnabled(false);
			for(int idx: table.getSelectedRows()) {
				if(deferrables.get(idx).getStatus() == Status.WAITING) {
					abortButton.setEnabled(true);
					break;
				}
			}
		});

		setSize(700, 300);
	}

	@Override
	public void setVisible(boolean v) {
		super.setVisible(v);
		if(v) {
			fill();
			table.columnsWidthAdapt();
			deferrables.addUniqueListener(this);
		} else {
			deferrables.removeListener(this);
		}
	}

	private void fill() {
		SwingUtilities.invokeLater(() -> {
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			tModel.clear();
			for(int i = 0; i < deferrables.count(); i++) {
				tModel.addRow(generateRow(deferrables.get(i)));
			}
			setCursor(Cursor.getDefaultCursor());
		});
	}

	private static Object[] generateRow(DeferrableRecord def) {
		String status = LABELS.getString("defStatus_" + def.getStatus().name());
		String retMsg = def.getRetMsg();
		if(retMsg != null && retMsg.length() > 0) {
			if(LABELS.containsKey(retMsg)) {
				retMsg = LABELS.getString(retMsg);
			}
			status += " - " + retMsg.replace("\n", "; ");
		}
		return new Object[] {
				String.format(LABELS.getString("formatDataTime"), def.getTime()),
				def.getDeviceName(),
				def.getDescription(),
				status};
	}

	@Override
	public void update(Status mesgType, Integer msgBody) {
		if(mesgType == Status.WAITING) { // added
			tModel.addRow(generateRow(deferrables.get(msgBody)));
		} else {
			tModel.setRow(msgBody, generateRow(deferrables.get(msgBody)));
		}
	}
}