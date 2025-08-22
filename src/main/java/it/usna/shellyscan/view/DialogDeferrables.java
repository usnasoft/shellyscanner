package it.usna.shellyscan.view;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.controller.DeferrableTask;
import it.usna.shellyscan.controller.DeferrableTask.Status;
import it.usna.shellyscan.controller.DeferrablesContainer;
import it.usna.shellyscan.controller.DeferrablesContainer.DeferrableRecord;
import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.controller.UsnaSelectedAction;
import it.usna.swing.UsnaPopupMenu;
import it.usna.swing.table.ExTooltipTable;
import it.usna.swing.table.UsnaTableModel;
import it.usna.util.UsnaEventListener;

public class DialogDeferrables extends JFrame implements UsnaEventListener<DeferrableTask.Status, Integer> {
	private static final long serialVersionUID = 1L;
	private static final Logger LOG = LoggerFactory.getLogger(DialogDeferrables.class);
	private final UsnaTableModel tModel = new UsnaTableModel(LABELS.getString("col_time"), LABELS.getString("col_devName"), LABELS.getString("col_actionDesc"), LABELS.getString("col_status"));
	private final ExTooltipTable table = new ExTooltipTable(tModel);
	private final DeferrablesContainer deferrables;

	public DialogDeferrables(/*Window owner,*/) {
		super(LABELS.getString("labelShowDeferrables"));
		setIconImage(Main.ICON);

		deferrables = DeferrablesContainer.getInstance();

		table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
			private static final long serialVersionUID = 1L;
			{
				setIconTextGap(6);
			}

			@Override
			public JLabel getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
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
		
		Action abortAction = new UsnaSelectedAction(null, table, "btnAbort", row -> {
			deferrables.cancel(table.convertRowIndexToModel(row));
			table.clearSelection();
		});
		abortAction.setEnabled(false);
		JButton abortButton = new JButton(abortAction);
		JButton closeButton = new JButton(new UsnaAction("dlgClose", e -> dispose()));
		JPanel buttonsPanel = new JPanel();
		buttonsPanel.add(abortButton);
		buttonsPanel.add(closeButton);

		getContentPane().add(buttonsPanel, BorderLayout.SOUTH);

		table.getSelectionModel().addListSelectionListener(e -> {
			abortAction.setEnabled(false);
			for(int idx: table.getSelectedRows()) {
				if(deferrables.get(idx).getStatus() == Status.WAITING) {
					abortAction.setEnabled(true);
					break;
				}
			}
		});
		
		UsnaPopupMenu tablePopup = new UsnaPopupMenu(abortAction);
		table.addMouseListener(tablePopup.getMouseListener(table));

		setSize(700, 300);
	}

	@Override
	public void setVisible(boolean v) {
		super.setVisible(v);
		if(v) {
			SwingUtilities.invokeLater(() -> {
//				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				tModel.clear();
				for(int i = 0; i < deferrables.count(); i++) {
					tModel.addRow(generateRow(deferrables.get(i)));
				}
				table.columnsWidthAdapt();
				deferrables.addUniqueListener(this);
//				setCursor(Cursor.getDefaultCursor());
			});
//			deferrables.addUniqueListener(this);
		} else {
			deferrables.removeListener(this);
		}
	}

//	private void fill() {
//		SwingUtilities.invokeLater(() -> {
//			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
//			tModel.clear();
//			for(int i = 0; i < deferrables.count(); i++) {
//				tModel.addRow(generateRow(deferrables.get(i)));
//			}
//			table.columnsWidthAdapt();
//			setCursor(Cursor.getDefaultCursor());
//		});
//	}

	private static Object[] generateRow(DeferrableRecord def) {
		String defStatus = LABELS.getString("defStatus_" + def.getStatus().name());
		String retMsg = def.getRetMsg();
		if(retMsg != null && retMsg.length() > 0) {
			if(LABELS.containsKey(retMsg)) {
				retMsg = LABELS.getString(retMsg);
			}
			defStatus += " - " + retMsg.replace("\n", "; ");
		}
		return new Object[] {
				String.format(LABELS.getString("formatDataTime"), def.getTime()),
				def.getDeviceName(),
				def.getDescription(),
				defStatus};
	}

	@Override
	public void update(Status mesgType, Integer msgBody) {
		try {
			if(mesgType == Status.WAITING) { // added
				tModel.addRow(generateRow(deferrables.get(msgBody)));
			} else {
				tModel.setRow(msgBody, generateRow(deferrables.get(msgBody)));
			}
		} catch(RuntimeException e) {
			LOG.error("update", e);
		}
	}
}