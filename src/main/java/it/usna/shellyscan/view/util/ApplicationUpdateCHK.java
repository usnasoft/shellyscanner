package it.usna.shellyscan.view.util;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Window;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.usna.shellyscan.Main;

public class ApplicationUpdateCHK {
	private static final Logger LOG = LoggerFactory.getLogger(ApplicationUpdateCHK.class);
	
	public static void checkForUpdates(final Window w) {
		w.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		try {
			List<Release> rel = remoteCheck(true, "000");
			if(rel.size() == 0) {
				Msg.showMsg(w, LABELS.getString("aboutCheckUpdatesNone"), currentVersion(), JOptionPane.INFORMATION_MESSAGE);
			} else {	
				String msg = rel.stream().map(Release::msg).collect(Collectors.joining("\n"));
				
				Object[] options = new Object[] {LABELS.getString("aboutCheckUpdatesDownload"), changelogBtn(w), LABELS.getString("dlgClose")};
				int choice = JOptionPane.showOptionDialog(w, msg, currentVersion(), JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, null);
				if(choice == 0) { // download
					try {
						Desktop.getDesktop().browse(URI.create(LABELS.getString("downloadURL")));
					} catch (IOException | UnsupportedOperationException ex) {
						Msg.errorMsg(w, LABELS.getString("downloadURL"));
					}
				}
			}
		} catch(IOException e) {
			LOG.warn("CheckUpdate", e);
			Msg.errorMsg(w, LABELS.getString("aboutCheckUpdatesError"));
		} finally {
			w.setCursor(Cursor.getDefaultCursor());
		}
	}

	/**
	 * This call manages (read/write) IGNORE_VERION_DOWNLOAD parameter
	 */
	public static void checkForUpdates(final Window w, final ScannerProperties appProp) {
		String mode = appProp.getProperty(ScannerProperties.PROP_UPDATECHK_ACTION);
		if(mode.equals("NEVER") == false) {
			w.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			try {
				String ignoreRel = appProp.getProperty(ScannerProperties.VERSION_IGNORE, "000");
				List<Release> rel = remoteCheck(mode.endsWith("BETA"), ignoreRel);
				if(rel.size() > 0) {
					String msg = rel.stream().map(Release::msg).collect(Collectors.joining("\n"));
					final Object[] options = new Object[] {LABELS.getString("aboutCheckUpdatesDownload"), changelogBtn(w), LABELS.getString("aboutCheckUpdatesSkip"), LABELS.getString("dlgClose")};
					int choice = JOptionPane.showOptionDialog(w, msg, currentVersion(), JOptionPane.YES_NO_CANCEL_OPTION , JOptionPane.INFORMATION_MESSAGE, null, options, null);
					if(choice == 0) { // download
						try {
							Desktop.getDesktop().browse(URI.create(LABELS.getString("downloadURL")));
						} catch (IOException | UnsupportedOperationException ex) {
							Msg.errorMsg(w, LABELS.getString("downloadURL"));
						}
					} else if(choice == 2) { // skip
						appProp.setProperty(ScannerProperties.VERSION_IGNORE, rel.stream().map(r -> r.relId()).collect(Collectors.maxBy(String.CASE_INSENSITIVE_ORDER)).get()); // new ignore
					}
				}
			} catch(IOException e) {
				LOG.warn("CheckUpdate", e);
			} finally {
				w.setCursor(Cursor.getDefaultCursor());
			}
		}
	}
	
	// This will not automatically dispose the OptionDialog
	private static JButton changelogBtn(final Window w) {
		JButton changelog = new JButton(LABELS.getString("aboutCheckUpdatesChangelog"));
		changelog.addActionListener(e -> {
			try {
				Desktop.getDesktop().browse(URI.create(LABELS.getString("changelogURL")));
			} catch (IOException | UnsupportedOperationException ex) {
				Msg.errorMsg(w, LABELS.getString("changelogURL"));
			}
		});
		return changelog;
	}
	
	private static String currentVersion() {
		return String.format(LABELS.getString("aboutCheckUpdatesTitle"), Main.VERSION + " r." + Main.VERSION_CODE.substring(Main.VERSION_CODE.length() - 2, Main.VERSION_CODE.length()));
	}
	
	private static List<Release> remoteCheck(final boolean checkDev, final String ignoreRel) throws MalformedURLException, IOException {
		List<Release> rel = new ArrayList<>(2);
		final URLConnection con = new URL(LABELS.getString("aboutCheckUpdatesPath")).openConnection(); // http://www.usna.it/shellyscanner/last_verion.txt
		final JsonNode updateNode = new ObjectMapper().readTree(con.getInputStream());
		final JsonNode stable = updateNode.path("stable");
		String id = null;
		if(stable.isNull() == false && (id = stable.path("id").asText()).compareTo(Main.VERSION_CODE) > 0 && id.compareTo(ignoreRel) > 0) {
			String devMsg = String.format(LABELS.getString("aboutCheckUpdatesYes"), stable.path("version").asText());
			String note = stable.path("note").asText();
			if(note.length() > 0) {
				devMsg += " - " + note;
			}
			rel.add(new Release(devMsg, id));
		}
		final JsonNode dev = updateNode.path("dev");
		if(checkDev && dev.isNull() == false && (id = dev.path("id").asText()).compareTo(Main.VERSION_CODE) > 0 && id.compareTo(ignoreRel) > 0) {
			String stableMsg = String.format(LABELS.getString("aboutCheckUpdatesYes"), dev.path("version").asText());
			String note = dev.path("note").asText();
			if(note.length() > 0) {
				stableMsg += " - " + note;
			}
			rel.add(new Release(stableMsg, id));
		}
		return rel;
	}
	
	private record Release(String msg, String relId) {}
}

/*
{
"stable": {"id": "001.000.000r200", "version": "1.0.0", "note": ""},
"dev": {"id": "001.000.000r100", "version": "1.0.0 beta", "note": "beta version"},
}

"stable" & "dev" are optional; "note" fields are optional 
*/