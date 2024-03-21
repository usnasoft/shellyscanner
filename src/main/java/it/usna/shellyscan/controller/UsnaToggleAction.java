package it.usna.shellyscan.controller;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;

public class UsnaToggleAction extends UsnaAction {
	private static final long serialVersionUID = 1L;
	private final ActionListener deactivate;
	private final ImageIcon imageInactive;
	private final ImageIcon imageActive;
	private final String toolTipInactive;
	private final String toolTipActive;
	private boolean active = false;
	
	public UsnaToggleAction(Component w, String nameId, String tooltipId, String iconInactive, String iconActive, final ActionListener activate, final ActionListener deactivate) {
		this(w, nameId, tooltipId, tooltipId, iconInactive, iconActive, activate, deactivate);
	}

	public UsnaToggleAction(Component w, String nameId, String tooltipInactiveId, String tooltipActiveId, String iconInactive, String iconActive, final ActionListener activate, final ActionListener deactivate) {
		super(w, nameId, tooltipInactiveId, iconInactive, null, activate);
		this.deactivate = deactivate;
		imageInactive = (ImageIcon)getValue(SMALL_ICON);
		toolTipInactive = (String)getValue(SHORT_DESCRIPTION);
		imageActive = new ImageIcon(UsnaToggleAction.class.getResource(iconActive));
		toolTipActive =  LABELS.getString(tooltipActiveId);
	}
	
	public void setSelected(boolean sel) {
		if(sel) {
			putValue(SMALL_ICON, imageActive);
			putValue(SHORT_DESCRIPTION, toolTipActive);
			active = true;
		} else {
			putValue(SMALL_ICON, imageInactive);
			putValue(SHORT_DESCRIPTION, toolTipInactive);
			active = false;
		}
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		try {
			if(w != null) {
				w.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)); // usefull if SwingUtilities.invokeLater(...) is not used inside "onActionPerformed"
			}
			if(active) {
				deactivate.actionPerformed(e);
				setSelected(false);
			} else {
				onActionPerformed.actionPerformed(e);
				setSelected(true);
			}
		} finally {
			if(w != null) {
				w.setCursor(Cursor.getDefaultCursor());
			}
		}
	}
}