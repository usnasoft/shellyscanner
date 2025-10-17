package it.usna.shellyscan.view;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.Window;
import java.io.IOException;
import java.net.URI;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.html.HTMLDocument;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.view.util.ApplicationUpdateCHK;

public class DialogAbout {
	private DialogAbout() {}
	
	public static void show(JFrame owner) {
		final String build = Main.VERSION_CODE.substring(Main.VERSION_CODE.length() - 2, Main.VERSION_CODE.length());
		JEditorPane ep = new JEditorPane("text/html", "<html><h1><font color=#00005a>" + Main.APP_NAME + " " + Main.VERSION +
				("00".equals(build) ? "" : " r." + build) + " <img src=\"usna16.gif\"></h1></font><p>" + LABELS.getString("aboutApp") + "</html>");
		ep.setEditable(false);
		((HTMLDocument)ep.getDocument()).setBase(DialogAbout.class.getResource("/images/"));
		ep.addHyperlinkListener(ev -> {
			try {
				if(ev.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
					if(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
						Desktop.getDesktop().browse(URI.create(ev.getURL().toString()));
					} else {
						JOptionPane.showMessageDialog(ep, ev.getURL(), "", JOptionPane.PLAIN_MESSAGE);
					}
				}
			} catch (IOException ex) {/* cannot run the browser */}
		});
		
		// JButton Check Updates
		JButton checkButton = new JButton(new UsnaAction("aboutCheckUpdates", e -> {
			Window w = SwingUtilities.getWindowAncestor((Component)e.getSource());
			ApplicationUpdateCHK.checkForUpdates(w);
		}));
		
		// JButton OK (close)
		JButton okButton = new JButton(new UsnaAction("dlgOK", e -> {
			Window w = SwingUtilities.getWindowAncestor((Component)e.getSource());
			if (w != null) {
				w.setVisible(false);
			}
		}));

		JOptionPane.showOptionDialog(owner, ep, Main.APP_NAME, JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE,
				new ImageIcon(DialogAbout.class.getResource("/images/ShSc.png")), new Object[] {checkButton, okButton}, okButton);
	}
}