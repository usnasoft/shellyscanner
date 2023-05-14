package it.usna.shellyscan.controller;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.Window;
import java.util.function.Consumer;

import javax.swing.ImageIcon;

import it.usna.swing.table.ExTooltipTable;

public class UsnaSelectedAction extends UsnaAction {
	private static final long serialVersionUID = 1L;

	public UsnaSelectedAction(Window w, ExTooltipTable table, String nameId, String tooltipId, String smallIcon, String largeIcon, Consumer<Integer> c) {
		this(w, table, largeIcon, tooltipId, c);
		putValue(NAME, LABELS.getString(nameId));
		if(smallIcon != null) {
			putValue(SMALL_ICON, new ImageIcon(getClass().getResource(smallIcon)));
		}
	}
	
	public UsnaSelectedAction(Window w, ExTooltipTable table, String nameId, Consumer<Integer> c) {
		this(w, table, null, null, c);
		putValue(NAME, LABELS.getString(nameId));
	}

	public UsnaSelectedAction(Window w, ExTooltipTable table, String icon, String tooltipId, Consumer<Integer> c) {
		super(w, icon, tooltipId, null);
		onActionPerformed = e -> {
			for(int ind: table.getSelectedRows()) {
				int modelRow = table.convertRowIndexToModel(ind);
				c.accept(modelRow);
			}
		};
	}
}

//private class ViewSelectedAction extends UsnaAction {
//private static final long serialVersionUID = 1L;
//
//public ViewSelectedAction(String nameId, String tooltipId, String smallIcon, String largeIcon, BiConsumer<Integer, ShellyAbstractDevice> c) {
//	this(largeIcon, tooltipId, c);
//	putValue(NAME, LABELS.getString(nameId));
//	if(smallIcon != null) {
//		putValue(SMALL_ICON, new ImageIcon(getClass().getResource(smallIcon)));
//	}
//}
//
//public ViewSelectedAction(String icon, String tooltipId, BiConsumer<Integer, ShellyAbstractDevice> c) {
//	super(MainView.this, icon, tooltipId, null);
//	onActionPerformed = e -> {
//		for(int ind: devicesTable.getSelectedRows()) {
//			int modelRow = devicesTable.convertRowIndexToModel(ind);
//			c.accept(modelRow, model.get(modelRow));
//		}
//	};
//}
//}