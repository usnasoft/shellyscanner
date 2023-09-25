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
import it.usna.shellyscan.view.util.UpplicationUpdateCHK;

public class DialogAbout {
//	private final static Logger LOG = LoggerFactory.getLogger(DialogAbout.class);
	
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
						JOptionPane.showMessageDialog(ep, ev.getURL(), "", JOptionPane.PLAIN_MESSAGE);
					}
				}
			} catch (IOException | URISyntaxException ex) {}
		});
		
		// JButton Check Updates
		JButton checkButton = new JButton(new UsnaAction("aboutCheckUpdates", e -> {
//			String title = LABELS.getString("aboutCheckUpdates") + " - " + Main.VERSION + " r." + Main.REVISION;
			Window w = SwingUtilities.getWindowAncestor((Component)e.getSource());
			UpplicationUpdateCHK.chechForUpdates(w);
//			try {
//				w.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
////				String msg = chechForUpdates(w, true);
////				if(msg != null) {
////					Msg.showMsg(w, msg, title, JOptionPane.INFORMATION_MESSAGE);
////				}
//				
//			} finally {
//				w.setCursor(Cursor.getDefaultCursor());
//			}
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
	
//	private static String chechForUpdates(final Window w, final boolean checkDev) {
//		try {
//			final String title = LABELS.getString("aboutCheckUpdates") + " - " + Main.VERSION + " r." + Main.REVISION;
//			final URLConnection con = new URL(LABELS.getString("aboutCheckUpdatesPath")).openConnection(); // https://www.usna.it/shellyscanner/last_verion.txt
//			final JsonNode updateNode = new ObjectMapper().readTree(con.getInputStream());
//			String msg = "";
//			final JsonNode stable = updateNode.path("stable");
//			if(stable.isNull() == false && stable.path("id").asText().compareTo(Main.VERSION_CODE) > 0) {
//				msg = "\n" + String.format(LABELS.getString("aboutCheckUpdatesYes"), stable.path("version").asText());
//				String note = stable.path("note").asText();
//				if(note.length() > 0) {
//					msg += " - " + note;
//				}
//			}
//			final JsonNode dev;
//			if(checkDev && (dev = updateNode.path("dev")).isNull() == false && dev.path("id").asText().compareTo(Main.VERSION_CODE) > 0) {
//				msg += "\n" + String.format(LABELS.getString("aboutCheckUpdatesYes"), dev.path("version").asText());
//				String note = dev.path("note").asText();
//				if(note.length() > 0) {
//					msg += " - " + note;
//				}
//			}
//			if(msg.length() > 0) {
//				Object[] options = new Object[] {LABELS.getString("aboutCheckUpdatesDownload"), LABELS.getString("dlgClose")};
//				if(JOptionPane.showOptionDialog(w, msg, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, null) == 0) {
//					try {
//						Desktop.getDesktop().browse(new URI(LABELS.getString("aboutCheckUpdatesDownloadURL")));
//					} catch (IOException | URISyntaxException ex) {
//						JOptionPane.showMessageDialog(w, LABELS.getString("aboutCheckUpdatesDownloadURL"), "", JOptionPane.PLAIN_MESSAGE);
//					}
//				}
//				return null;
//			} else {
//				String msgNone = updateNode.path("none").asText();
//				return msgNone.length() > 0 ? msgNone : LABELS.getString("aboutCheckUpdatesNone");
//			}
//		} catch (IOException ex) {
//			LOG.warn("CheckUpdate", ex);
//			Msg.errorMsg(w, LABELS.getString("aboutCheckUpdatesError"));
//			return null;
//		}
//	}
}