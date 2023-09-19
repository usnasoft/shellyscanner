package it.usna.shellyscan.controller;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.Window;
import java.util.function.Consumer;

import javax.swing.ImageIcon;
import javax.swing.JTable;

public class UsnaSelectedAction extends UsnaAction {
	private static final long serialVersionUID = 1L;

	public UsnaSelectedAction(Window w, JTable table, String nameId, String tooltipId, String smallIcon, String largeIcon, Consumer<Integer> c) {
		this(w, table, largeIcon, tooltipId, c);
		putValue(NAME, LABELS.getString(nameId));
		if(smallIcon != null) {
			putValue(SMALL_ICON, new ImageIcon(getClass().getResource(smallIcon)));
		}
	}
	
	public UsnaSelectedAction(Window w, JTable table, String nameId, Consumer<Integer> c) {
		this(w, table, null, null, c);
		putValue(NAME, LABELS.getString(nameId));
	}

	public UsnaSelectedAction(Window w, JTable table, String icon, String tooltipId, Consumer<Integer> c) {
		super(w, icon, tooltipId, null);
		setConsumer(table, c);
	}
	
	/**
	 * This constructor must be followed by a setConsumer call
	 * @param w
	 * @param nameId
	 * @param tooltipId
	 * @param smallIcon
	 * @param largeIcon
	 */
	protected UsnaSelectedAction(Window w, String nameId, String tooltipId, String smallIcon, String largeIcon) {
		this(w,  largeIcon, tooltipId);
		putValue(NAME, LABELS.getString(nameId));
		if(smallIcon != null) {
			putValue(SMALL_ICON, new ImageIcon(getClass().getResource(smallIcon)));
		}
	}
	
	/**
	 * This constructor must be followed by a setConsumer call
	 * @param w
	 * @param icon
	 * @param tooltipId
	 */
	protected UsnaSelectedAction(Window w, String icon, String tooltipId) {
		super(w, icon, tooltipId, null);
	}
	
	protected void setConsumer(JTable table, Consumer<Integer> c) {
		onActionPerformed = e -> {
			for(int ind: table.getSelectedRows()) {
				int modelRow = table.convertRowIndexToModel(ind);
				c.accept(modelRow);
			}
		};
	}
}