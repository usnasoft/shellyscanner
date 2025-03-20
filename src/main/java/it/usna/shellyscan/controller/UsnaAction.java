package it.usna.shellyscan.controller;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

/**
 * javax.swing.AbstractAction with resourced attributes (label and icons)
 */
public class UsnaAction extends AbstractAction {
	private static final long serialVersionUID = 1L;
	protected final Component w;
	protected ActionListener onActionPerformed;
	
	public UsnaAction(final ActionListener a) {
		this.onActionPerformed = a;
		w = null;
	}
	
	public UsnaAction(String nameId, final ActionListener a) {
		putValue(NAME, LABELS.getString(nameId));
		this.onActionPerformed = a;
		w = null;
	}
	
	public UsnaAction(Component w, String nameId, final ActionListener a) {
		this.w = w;
		putValue(NAME, LABELS.getString(nameId));
		this.onActionPerformed = a;
	}

	public UsnaAction(Component w, String nameId, String tooltipId, String smallIcon, String largeIcon, final ActionListener a) {
		this(w, nameId, tooltipId, smallIcon, largeIcon);
		this.onActionPerformed = a;
	}

	public UsnaAction(Component w, String tooltipId, String icon, final ActionListener a) {
		this(w, icon, tooltipId);
		this.onActionPerformed = a;
	}
	
	/**
	 * This constructor must be followed by a setActionListener call
	 */
	public UsnaAction(Component w, String nameId, String tooltipId, String smallIcon, String largeIcon) {
		this(w, largeIcon, tooltipId);
		putValue(NAME, LABELS.getString(nameId));
		if(smallIcon != null) {
			putValue(SMALL_ICON, new ImageIcon(UsnaAction.class.getResource(smallIcon)));
		}
	}
	
	/**
	 * This constructor must be followed by a setActionListener call
	 */
	public UsnaAction(Component w, String icon, String tooltipId) {
		this.w = w;
		if(icon != null) {
			putValue(LARGE_ICON_KEY, new ImageIcon(UsnaAction.class.getResource(icon)));
		}
		if(tooltipId != null) {
			putValue(SHORT_DESCRIPTION, LABELS.getString(tooltipId));
		}
	}
	
	public void setActionListener(ActionListener a) {
		onActionPerformed = a;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		try {
			if(w != null) {
				w.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)); // usefull if SwingUtilities.invokeLater(...) is not used inside "onActionPerformed"
			}
			onActionPerformed.actionPerformed(e);
		} finally {
			if(w != null) {
				w.setCursor(Cursor.getDefaultCursor());
			}
		}
	}
	
	public void setName(String nameId) {
		putValue(NAME, LABELS.getString(nameId));
	}
	
	public void setSmallIcon(String icon) {
		putValue(SMALL_ICON, new ImageIcon(UsnaAction.class.getResource(icon)));
	}
	
	@Override
	public String toString() {
		return (String)getValue(NAME);
	}
}