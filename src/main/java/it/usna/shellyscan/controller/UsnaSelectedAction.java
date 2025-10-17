package it.usna.shellyscan.controller;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.Component;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import javax.swing.ImageIcon;
import javax.swing.JTable;

public class UsnaSelectedAction extends UsnaAction {
	private static final long serialVersionUID = 1L;
	private BooleanSupplier test = null;

	public UsnaSelectedAction(Component w, JTable table, String nameId, String tooltipId, String smallIcon, String largeIcon, BooleanSupplier test, Consumer<Integer> c) {
		this(w, table, nameId, tooltipId, smallIcon, largeIcon, c);
		this.test = test;
	}

	public UsnaSelectedAction(Component w, JTable table, String nameId, String tooltipId, String smallIcon, String largeIcon, Consumer<Integer> c) {
		this(w, table, tooltipId, largeIcon, c);
		putValue(NAME, LABELS.getString(nameId));
		if(smallIcon != null) {
			putValue(SMALL_ICON, new ImageIcon(UsnaSelectedAction.class.getResource(smallIcon)));
		}
	}

	public UsnaSelectedAction(Component w, JTable table, String nameId, Consumer<Integer> c) {
		this(w, table, null, null, c);
		putValue(NAME, LABELS.getString(nameId));
	}

	public UsnaSelectedAction(Component w, JTable table, String tooltipId, String icon, Consumer<Integer> c) {
		super(w, tooltipId, icon, null);
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
	protected UsnaSelectedAction(Component w, String nameId, String tooltipId, String smallIcon, String largeIcon) {
		this(w,  largeIcon, tooltipId);
		putValue(NAME, LABELS.getString(nameId));
		if(smallIcon != null) {
			putValue(SMALL_ICON, new ImageIcon(UsnaSelectedAction.class.getResource(smallIcon)));
		}
	}

	/**
	 * This constructor must be followed by a setConsumer call
	 * @param w
	 * @param icon
	 * @param tooltipId
	 */
	protected UsnaSelectedAction(Component w, String icon, String tooltipId) {
		super(w, tooltipId, icon, null);
	}

	protected void setConsumer(JTable table, Consumer<Integer> c) {
		onActionPerformed = e -> {
			if(test == null || test.getAsBoolean()) {
				for(int ind: table.getSelectedRows()) {
					c.accept(table.convertRowIndexToModel(ind));
				}
			}
		};
	}
}