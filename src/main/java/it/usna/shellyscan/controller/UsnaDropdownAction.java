package it.usna.shellyscan.controller;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.function.Supplier;

import it.usna.swing.UsnaPopupMenu;

public class UsnaDropdownAction extends UsnaAction {
	private static final long serialVersionUID = 1L;
	private Supplier<Object[]> actionsSupplier;
	
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
	}
	
	public UsnaDropdownAction(Component w, String tooltipId, String largeIcon, Supplier<Object[]> supplier) {
		super(w, tooltipId, largeIcon);
		this.actionsSupplier = supplier;
		
		setActionListener(e -> {
			UsnaPopupMenu popup = new UsnaPopupMenu(actionsSupplier.get());
			popup.show(w, 0, 0);
		});
	}
	
	public UsnaDropdownAction(Component w, String tooltipId, String largeIcon, Object[] actions) {
		this(w, tooltipId, largeIcon, () -> actions);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		onActionPerformed.actionPerformed(e);
	}
	
	public Object[] getActions() {
		return actionsSupplier.get();
	}
	
	public void setActions(Object[] actions) {
		this.actionsSupplier = () -> actions;
	}
}