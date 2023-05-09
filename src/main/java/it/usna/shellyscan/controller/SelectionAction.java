package it.usna.shellyscan.controller;

import static it.usna.shellyscan.Main.LABELS;

import java.util.function.Predicate;

import javax.swing.ImageIcon;
import javax.swing.JTable;

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

	public SelectionAction(JTable devicesTable, String tooltipId, Predicate<Integer> test) {
		super(null, null, tooltipId, null);
		onActionPerformed = e -> {
			devicesTable.clearSelection();
			for(int i = 0; i < devicesTable.getRowCount(); i++) {
				if(test.test(devicesTable.convertRowIndexToModel(i))) {
					devicesTable.addRowSelectionInterval(i, i);
				}
			}
		};
	}
}