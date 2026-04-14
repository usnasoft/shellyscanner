package it.usna.shellyscan.controller;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;

import javax.swing.JOptionPane;

import it.usna.shellyscan.view.util.Msg;

public class UsnaOpenUrlAction extends UsnaAction {
	private static final long serialVersionUID = 1L;
	
	public UsnaOpenUrlAction(Component owner, String nameId, String url) {
		super(owner, nameId, listener(owner, url));
	}
	
	public UsnaOpenUrlAction(Component owner, String nameId, String tooltipId, String largeIcon, String url) {
		super(owner, nameId, tooltipId, null, largeIcon, listener(owner, url));
	}
	
	private static ActionListener listener(Component owner, String url) {
		return e -> {
			if(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
				try {
					Desktop.getDesktop().browse(URI.create(url));
				} catch (IOException | UnsupportedOperationException ex) {
					Msg.errorMsg(owner, ex);
				}
			} else {
				JOptionPane.showMessageDialog(owner, url, "", JOptionPane.PLAIN_MESSAGE);
			}
		};
	}
}
