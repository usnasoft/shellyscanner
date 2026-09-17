package it.usna.shellyscan.controller;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.swing.ImageIcon;
import javax.swing.JTable;

/**
 * As snaSelectedAction but there is a value returning for any element; a list of this values is the processed at the end
 */
public class UsnaSelectedAction2 extends UsnaAction {
	private static final long serialVersionUID = 1L;
	private BooleanSupplier test = null;

	public <R>UsnaSelectedAction2(Component w, JTable table, String nameId, String tooltipId, String smallIcon, String largeIcon, BooleanSupplier test, Function<Integer, R> func, Consumer<List<R>> c) {
		this(w, table, nameId, tooltipId, smallIcon, largeIcon, func, c);
		this.test = test;
	}

	public <R>UsnaSelectedAction2(Component w, JTable table, String nameId, String tooltipId, String smallIcon, String largeIcon, Function<Integer, R> func, Consumer<List<R>> c) {
		this(w, table, tooltipId, largeIcon, func, c);
		putValue(NAME, LABELS.getString(nameId));
		if(smallIcon != null) {
			putValue(SMALL_ICON, new ImageIcon(UsnaSelectedAction2.class.getResource(smallIcon)));
		}
	}

	public <R>UsnaSelectedAction2(Component w, JTable table, String nameId, Function<Integer, R> func, Consumer<List<R>> c) {
		this(w, table, null, null, func, c);
		putValue(NAME, LABELS.getString(nameId));
	}

	public <R>UsnaSelectedAction2(Component w, JTable table, String tooltipId, String icon, Function<Integer, R> func, Consumer<List<R>> c) {
		super(w, tooltipId, icon, null);
		setFunction(table, func, c);
	}

	private <R> void setFunction(JTable table, Function<Integer, R> func, Consumer<List<R>> c) {
		onActionPerformed = e -> {
			ArrayList<R> acc = new ArrayList<>();
			if(test == null || test.getAsBoolean()) {
				for(int ind: table.getSelectedRows()) {
					acc.add(func.apply(table.convertRowIndexToModel(ind)));
				}
			}
			c.accept(acc);
		};
	}
}