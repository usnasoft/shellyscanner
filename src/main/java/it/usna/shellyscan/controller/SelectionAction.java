package it.usna.shellyscan.controller;

import static it.usna.shellyscan.Main.LABELS;

import java.util.function.Predicate;

import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

/**
 * Action to select rows in a JTable based on a java.util.function.Predicate<Integer> (Integer is the corresponding model index) criteria
 */
public class SelectionAction extends UsnaAction {
	private static final long serialVersionUID = 1L;

	public SelectionAction(JTable table, String nameId, String tooltipId, String smallIcon, Predicate<Integer> predicate) {
		this(table, tooltipId, predicate);
		putValue(NAME, LABELS.getString(nameId));
		if(smallIcon != null) {
			putValue(SMALL_ICON, new ImageIcon(getClass().getResource(smallIcon)));
		}
	}

	public SelectionAction(JTable table, String tooltipId, Predicate<Integer> test) {
		super(null, null, tooltipId, null);
		onActionPerformed = e -> {
			ListSelectionModel lsm = table.getSelectionModel();
			lsm.setValueIsAdjusting(true);
			lsm.clearSelection();
			for(int i = 0; i < table.getRowCount(); i++) {
				if(test.test(table.convertRowIndexToModel(i))) {
					lsm.addSelectionInterval(i, i);
				}
			}
			lsm.setValueIsAdjusting(false);
		};
	}
}