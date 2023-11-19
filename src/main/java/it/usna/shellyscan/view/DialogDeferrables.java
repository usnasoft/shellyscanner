package it.usna.shellyscan.view;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Toolkit;
import java.awt.Window;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.controller.DeferrableAction;
import it.usna.shellyscan.controller.DeferrableAction.Status;
import it.usna.shellyscan.controller.DeferrablesContainer;
import it.usna.shellyscan.controller.DeferrablesContainer.DeferrableRecord;
import it.usna.shellyscan.model.Devices;
import it.usna.swing.table.ExTooltipTable;
import it.usna.swing.table.UsnaTableModel;
import it.usna.util.UsnaEventListener;

public class DialogDeferrables extends JFrame implements UsnaEventListener<DeferrableAction.Status, Integer> {
	private static final long serialVersionUID = 1L;
//	private final static Logger LOG = LoggerFactory.getLogger(RestoreAction.class);

	private final UsnaTableModel tModel = new UsnaTableModel(LABELS.getString("col_time"), LABELS.getString("col_devName"), LABELS.getString("col_actionDesc"), LABELS.getString("col_status"));
	private final ExTooltipTable table = new ExTooltipTable(tModel);
	private final DeferrablesContainer deferrables;
	//	private final static int COL_TIME = 0;

	public DialogDeferrables(Window owner, Devices model) {
		super(LABELS.getString("labelShowDeferrables"));
		setIconImage(Toolkit.getDefaultToolkit().createImage(DialogDeferrables.class.getResource(Main.ICON)));

		deferrables = DeferrablesContainer.getInstance(model);
//		LOG.error("{}", deferrables);

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

		setSize(600, 300);
	}

	@Override
	public void setVisible(boolean v) {
		super.setVisible(v);
		if(v) {
			fill();
			table.columnsWidthAdapt();
			deferrables.addListener(this);
		} else {
			deferrables.removeListener(this);
		}
	}

	private void fill() {
		SwingUtilities.invokeLater(() -> {
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			tModel.clear();
			for(int i = 0; i < deferrables.size(); i++) {
				tModel.addRow(getRow(deferrables.get(i)));
			}
			setCursor(Cursor.getDefaultCursor());
		});
	}
	
	private static Object[] getRow(DeferrableRecord def) {
		Object retMsg = def.getRetMsg();
		String msg;
		String status = def.getStatus().name();
		if(retMsg != null && (msg = retMsg.toString()).length() > 0) {
			status += " - " + msg.replace("\n", "; ");
		}
		return new String[] {
				String.format(LABELS.getString("formatDataTime"), def.getTime()),
				def.getDeviceName(),
				def.getDescription(),
				status};
	}

	@Override
	public void update(Status mesgType, Integer msgBody) {
//		System.out.println(mesgType + "-" + msgBody);
		if(mesgType == Status.WAITING) {
			tModel.addRow(getRow(deferrables.get(msgBody)));
		} else {
			tModel.setRow(msgBody, getRow(deferrables.get(msgBody)));
		}
	}
}