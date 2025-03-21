package it.usna.shellyscan.controller;

import java.awt.Component;
import java.awt.event.ActionEvent;

import it.usna.swing.UsnaPopupMenu;

public class UsnaDropdownAction extends UsnaAction {
	private static final long serialVersionUID = 1L;
	private final Object[] actions;

	public UsnaDropdownAction(Component w, String nameId, String tooltipId/*, String smallIcon*/, String largeIcon, Object[] actions) {
		super(w, nameId, tooltipId, /*smallIcon*/null, largeIcon);
		this.actions = actions;
		
		setActionListener(e -> {
			UsnaPopupMenu popup = new UsnaPopupMenu((Object[])actions);
			popup.show(w, w.getMousePosition().x, w.getMousePosition().y);
		});
	}
	
	public UsnaDropdownAction(Component w, String largeIcon, String tooltipId, Object[] actions) {
		super(w, largeIcon, tooltipId);
		this.actions = actions;
		
		setActionListener(e -> {
			UsnaPopupMenu popup = new UsnaPopupMenu((Object[])actions);
			popup.show(w, 0, 0);
		});
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		onActionPerformed.actionPerformed(e);
	}
	
	public Object[] getActions() {
		return actions;
	}
}