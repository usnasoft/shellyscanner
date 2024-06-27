package it.usna.shellyscan.controller;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.event.ActionEvent;
import java.util.function.BiConsumer;

import javax.swing.ImageIcon;
import javax.swing.text.JTextComponent;
import javax.swing.text.TextAction;

/**
 * javax.swing.text.TextAction with resourced attributes (label and icons)
 */
public class UsnaTextAction extends TextAction {
	private static final long serialVersionUID = 1L;
//	protected final Component w;
	protected BiConsumer<JTextComponent, ActionEvent> consumer;

	public UsnaTextAction(/*Component w,*/ String actionName, String tooltipId, String icon, final BiConsumer<JTextComponent, ActionEvent> a) {
		this(/*w,*/ actionName, icon, tooltipId);
		this.consumer = a;
	}
	
	/**
	 * This constructor must be followed by a setActionListener call
	 */
	protected UsnaTextAction(/*Component w,*/ String actionName, String icon, String tooltipId) {
		super(actionName);
//		this.w = w;
		if(icon != null) {
			putValue(LARGE_ICON_KEY, new ImageIcon(UsnaTextAction.class.getResource(icon)));
		}
		if(tooltipId != null) {
			putValue(SHORT_DESCRIPTION, LABELS.getString(tooltipId));
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
//		try {
//			if(w != null) {
//				w.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)); // usefull if SwingUtilities.invokeLater(...) is not used inside "onActionPerformed"
//			}
		consumer.accept(getTextComponent(e), e);
//		} finally {
//			if(w != null) {
//				w.setCursor(Cursor.getDefaultCursor());
//			}
//		}
	}
	
	public void setName(String nameId) {
		putValue(NAME, LABELS.getString(nameId));
	}
	
	public void setSmallIcon(String icon) {
		putValue(SMALL_ICON, new ImageIcon(UsnaTextAction.class.getResource(icon)));
	}
	
	@Override
	public String toString() {
		return (String)getValue(NAME);
	}
}