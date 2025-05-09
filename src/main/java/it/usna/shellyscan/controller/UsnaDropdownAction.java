package it.usna.shellyscan.controller;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.function.Supplier;

import it.usna.swing.UsnaPopupMenu;

public class UsnaDropdownAction extends UsnaAction {
	private static final long serialVersionUID = 1L;
//	private final Object[] actions;
	private final Supplier<Object[]> actionsSupplier;
	
	public UsnaDropdownAction(Component w, String nameId, String tooltipId/*, String smallIcon*/, String largeIcon, Supplier<Object[]> supplier) {
		super(w, nameId, tooltipId, /*smallIcon*/null, largeIcon);
		this.actionsSupplier = supplier;
		
		setActionListener(e -> {
			UsnaPopupMenu popup = new UsnaPopupMenu(supplier.get());
			popup.show(w, w.getMousePosition().x, w.getMousePosition().y);
		});
	}

	public UsnaDropdownAction(Component w, String nameId, String tooltipId/*, String smallIcon*/, String largeIcon, Object[] actions) {
		this(w, nameId, tooltipId, largeIcon, () -> actions);
//		super(w, nameId, tooltipId, /*smallIcon*/null, largeIcon);
//		this.actionsSupplier = () -> actions;
//		
//		setActionListener(e -> {
//			UsnaPopupMenu popup = new UsnaPopupMenu((Object[])actions);
//			popup.show(w, w.getMousePosition().x, w.getMousePosition().y);
//		});
	}
	
	public UsnaDropdownAction(Component w, String largeIcon, String tooltipId, Supplier<Object[]> supplier) {
		super(w, largeIcon, tooltipId);
		this.actionsSupplier = supplier;
		
		setActionListener(e -> {
			UsnaPopupMenu popup = new UsnaPopupMenu(actionsSupplier.get());
			popup.show(w, 0, 0);
		});
	}
	
	public UsnaDropdownAction(Component w, String largeIcon, String tooltipId, Object[] actions) {
		this(w, largeIcon, tooltipId, () -> actions);
//		super(w, largeIcon, tooltipId);
////		this.actions = actions;
//		this.actionsSupplier = () -> {return actions;};
//		
//		setActionListener(e -> {
//			UsnaPopupMenu popup = new UsnaPopupMenu((Object[])actions);
//			popup.show(w, 0, 0);
//		});
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		onActionPerformed.actionPerformed(e);
	}
	
	public Object[] getActions() {
		return actionsSupplier.get();
	}
}