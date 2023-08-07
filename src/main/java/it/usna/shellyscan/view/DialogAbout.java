package it.usna.shellyscan.view;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.Window;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

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

public class DialogAbout {
	public static void show(JFrame owner) {
		JEditorPane ep = new JEditorPane("text/html", "<html><h1><font color=#00005a>" + Main.APP_NAME + " " + Main.VERSION + " <img src=\"usna16.gif\"></h1></font><p>" + LABELS.getString("aboutApp") + "</html>");
		ep.setEditable(false);
		((HTMLDocument)ep.getDocument()).setBase(DialogAbout.class.getResource("/images/"));
		ep.addHyperlinkListener(ev -> {
			try {
				if(ev.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
					if(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
						Desktop.getDesktop().browse(new URI(ev.getURL().toString()));
					} else {
						JOptionPane.showMessageDialog(owner, ev.getURL(), "", JOptionPane.PLAIN_MESSAGE);
					}
				}
			} catch (IOException | URISyntaxException ex) {}
		});
		JButton checkButton = new JButton(new UsnaAction("aboutCheckUpdates", e -> {
			//todo
		}));
		JButton okButton = new JButton(new UsnaAction("dlgOK", e -> {
			Window w = SwingUtilities.getWindowAncestor((Component)e.getSource());
			if (w != null) {
				w.setVisible(false);
			}
		}));
		Object[] options = new Object[] {checkButton, okButton};
		JOptionPane.showOptionDialog(owner, ep, Main.APP_NAME, JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE, new ImageIcon(DialogAbout.class.getResource("/images/ShSc.png")), options, okButton);
	}
}