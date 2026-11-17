package it.usna.shellyscan.controller;

import java.awt.Component;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;

import javax.swing.JOptionPane;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.view.util.Msg;

public class UsnaOpenUrlAction extends UsnaAction {
	private static final long serialVersionUID = 1L;
	
//	public UsnaOpenUrlAction(Component owner, String nameId, String url) {
//		super(owner, nameId, listener(owner, url));
//	}
	
	public UsnaOpenUrlAction(Component owner, String nameId, String tooltipId, String url) {
		super(owner, nameId, e -> openUrl(owner, url));
		if(tooltipId != null) {
			putValue(SHORT_DESCRIPTION, Main.LABELS.getString(tooltipId));
		}
	}
	
	public UsnaOpenUrlAction(Component owner, String nameId, String tooltipId, String largeIcon, String url) {
		super(owner, nameId, tooltipId, null, largeIcon, e -> openUrl(owner, url));
	}

	public static void openUrl(Component owner, String url) {
		if(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
			try {
				Desktop.getDesktop().browse(URI.create(url));
			} catch (IOException | UnsupportedOperationException ex) {
				Msg.errorMsg(owner, ex);
			}
		} else {
			JOptionPane.showMessageDialog(owner, url, "", JOptionPane.PLAIN_MESSAGE);
		}
	}
}