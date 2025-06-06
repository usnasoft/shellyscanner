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
	private String toolTipInactive;
	private String toolTipActive;
	private boolean selected = false;
	
	public UsnaToggleAction(Component w, String nameId, String tooltipInactiveId, String tooltipActiveId, String iconInactive, String iconActive, final ActionListener activate, final ActionListener deactivate) {
		super(w, nameId, tooltipInactiveId, iconInactive, null, activate);
		this.deactivate = deactivate;
		imageInactive = (ImageIcon)getValue(SMALL_ICON);
		toolTipInactive = (String)getValue(SHORT_DESCRIPTION);
		imageActive = new ImageIcon(UsnaToggleAction.class.getResource(iconActive));
		toolTipActive =  LABELS.getString(tooltipActiveId);
	}
	
	public UsnaToggleAction(Component w, String nameId, String tooltipInactiveId, String tooltipActiveId, String iconInactive, String iconActive, final ActionListener listener) {
		this(w, nameId, tooltipInactiveId, tooltipActiveId, iconInactive, iconActive, listener, listener);
	}
	
	public UsnaToggleAction(Component w, String iconInactive, String iconActive, final ActionListener activate, final ActionListener deactivate) {
		super(w, null, iconInactive, activate);
		this.deactivate = deactivate;
		imageInactive = (ImageIcon)getValue(LARGE_ICON_KEY);
//		toolTipInactive = (String)getValue(SHORT_DESCRIPTION);
		imageActive = new ImageIcon(UsnaToggleAction.class.getResource(iconActive));
	}
	
	public UsnaToggleAction(Component w, String iconInactive, String iconActive, final ActionListener listener) {
		this(w, iconInactive, iconActive, listener, listener);
	}
	
	public void setTooltip(String tooltipInactiveId, String tooltipActiveId) {
		toolTipInactive = LABELS.getString(tooltipInactiveId);
		toolTipActive =  LABELS.getString(tooltipActiveId);
		putValue(SHORT_DESCRIPTION, selected ? toolTipActive : toolTipInactive);
	}
	
	public void setSelected(boolean sel) {
		if(sel) {
			putValue(LARGE_ICON_KEY, imageActive);
			putValue(SHORT_DESCRIPTION, toolTipActive);
			selected = true;
		} else {
			putValue(LARGE_ICON_KEY, imageInactive);
			putValue(SHORT_DESCRIPTION, toolTipInactive);
			selected = false;
		}
	}
	
	public boolean isSelected() {
		return selected;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		try {
			if(w != null) {
				w.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)); // usefull if SwingUtilities.invokeLater(...) is not used inside "onActionPerformed"
			}
			if(selected) {
				setSelected(false);
				deactivate.actionPerformed(e);
			} else {
				setSelected(true);
				onActionPerformed.actionPerformed(e);
			}
		} finally {
			if(w != null) {
				w.setCursor(Cursor.getDefaultCursor());
			}
		}
	}
}