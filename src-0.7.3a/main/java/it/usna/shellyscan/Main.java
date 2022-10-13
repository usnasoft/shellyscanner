package it.usna.shellyscan;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.view.DialogAppSettings;
import it.usna.shellyscan.view.MainView;
import it.usna.util.AppProperties;

public class Main {
	public final static String APP_NAME = "Shelly Scanner";
	public final static String VERSION = "0.7.3a";
	public final static String ICON = "/images/ShSc24.png";
	public final static ResourceBundle LABELS = ResourceBundle.getBundle("LabelsBundle");
	public static Color BG_COLOR = new Color(50, 60, 65);
	public static Color TAB_LINE1 = new Color(240, 240, 240);
	public static Color TAB_LINE2 = new Color(160, 180, 255)/*Color.lightGray*/;
	public static Color STATUS_LINE = new Color(200, 220, 255);
	
	private final static String PROP_FILE = System.getProperty("user.home") + File.separator + ".shellyScanner";
	private final static AppProperties appProp = new AppProperties(PROP_FILE);
	
	private final static Logger LOG = LoggerFactory.getLogger(Main.class);
	
	public static void main(final String ... args) {
		//System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "TRACE");
		LOG.info(APP_NAME + " " + VERSION);

		//		Package mainPackage = Main.class.getPackage();
		//		String version = mainPackage.getImplementationVersion();
		try {
			for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					UIManager.setLookAndFeel(info.getClassName());
					break;
				}
			}
		} catch (Exception e) {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (Exception ex) {
				Main.errorMsg(ex);
			}
		}

		try {
			appProp.load(true);
			final Devices model = new Devices();
			MainView view = new MainView(model, appProp);
			view.setVisible(true);
			view.requestFocus(); // remove random focus on toolbar button 
			SwingUtilities.invokeLater(() -> {
				try {
				 view.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					boolean fullScan = appProp.getProperty(DialogAppSettings.PROP_SCAN_MODE, DialogAppSettings.PROP_SCAN_MODE_DEFAULT).equals("FULL");

					for(int i = 0; i < args.length; i++) {
						if(args[0].equals("-fullscan")) {
							fullScan = true;
						} else if(args[0].equals("-localscan")) {
							fullScan = false;
//						} else if(args[0].equals("-log")) {
//							System.setProperty("org.slf4j.simpleLogger.log.it.usna", "trace");
//							System.setProperty("org.slf4j.simpleLogger.showDateTime", "true");
//							System.setProperty("org.slf4j.simpleLogger.log.javax.jmdns", "error");
						} else {
							LOG.warn("Unknown parameter: {}", args[0]);
						}
					}
					model.scannerInit(fullScan,
							appProp.getIntProperty(DialogAppSettings.PROP_REFRESH_ITERVAL, DialogAppSettings.PROP_REFRESH_ITERVAL_DEFAULT) * 1000,
							appProp.getIntProperty(DialogAppSettings.PROP_REFRESH_CONF, DialogAppSettings.PROP_REFRESH_CONF_DEFAULT));
					view.setCursor(Cursor.getDefaultCursor());
				} catch (IOException e) {
					Main.errorMsg(e);
					System.exit(1);
				}
			});
		} catch (Throwable ex) {
			Main.errorMsg(ex);
			System.exit(1);
		}
	}

	public static void errorMsg(final Throwable t) {
		if(t instanceof IOException) {
			LOG.debug("Connection error", t);
		} else {
			LOG.error("Unexpected", t);
		}
		final String message = t.getMessage();
		final Window win = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
		JOptionPane.showMessageDialog(win, splitLine((message != null && message.length() > 0) ? message : t.toString(), 128), LABELS.getString("errorTitle"), JOptionPane.ERROR_MESSAGE);
	}
	
	public static void errorMsg(final String msg) {
		final Window win = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
		JOptionPane.showMessageDialog(win, msg, LABELS.getString("errorTitle"), JOptionPane.ERROR_MESSAGE);
	}
	
	private static String splitLine(String str, int maxLine) {
		final String lines[] = str.split("\\R");
		String newStr = "";
		for(String line: lines) {
			while(line.length() > maxLine) {
				newStr += line.substring(0, maxLine) + "\n";
				line = line.substring(maxLine);
			}
			newStr += line + '\n';
		}
		return newStr;
	}
}

// https://www.softicons.com/web-icons/web-grey-buttons-by-axialis-team
// https://www.softicons.com/web-icons/circular-icons-by-pro-theme-design/

//-D"org.slf4j.simpleLogger.log.it.usna=debug" -D"org.slf4j.simpleLogger.showDateTime=true"

//color weel https://stackoverflow.com/questions/36252778/how-to-draw-a-rgb-color-wheel-in-java

//0.5.5
// aggiunto Plug
// riscontro "restricted login" e "wi-fi bu" come per backup
// errore cloud per alcuni dispositivi (motion)

//0.6.0
//stato dispositivi (on-line, off-line, login, reading, error)
//colonna "nome"
//doppio click "Full device info" (non su command)
//login al singolo dispositivi protetto
//edit fine rgbw
//duo
//i3
//print table

//0.6.1
//flood

//0.7.0
//refresh parallelo continuo
//shelly 2, UNI
//export CSV

//0.7.2
//nome colonna stato (csv)
//restore mqtt (no pwd)
//doppia scheda di rete (Luk McFagnan)
//settings
// -localscan
//supporto parziale plus
//flag "defaul" su mqtt prefix

//0.7.3
//separazione fill status - file settings (che potrebbe essere chiamato meno)
//ShellyBulb
//supporto parziale

//todo
// test 3EM
// filtro per nome
//restore wi-fi backup (no pwd)
// impedire apertura di piu' editor uguali
//motion / EM3 / 
//ssdi (tooltip IP?)
//selezione colonne

//gruppo misure con definizione blocchi generici

//jmDNS meno petulamte
//setup: separatore csv, fullscan, **colonne**, azione doppio click (info/web interface) 

//https://shelly-api-docs.shelly.cloud/gen2/Overview/CommonDeviceTraits#authentication


//colonna misure
/*
1) per chi, come me, ha molti Shelly sarebbe comodo visualizzare in un colpo solo la versione di firmware installata su ciascuno Shelly.
Si potrebbe implementare l'applicazione in modo che l'utente possa scegliere le colonne da visualizzare aggiungendo, a quelle già presenti, almeno la versione di firmware installata ed il mac-address.
In alternativa, visto che far scegliere le colonne da visualizzare potrebbe essere più complicato da realizzare nel breve, in un primo momento, si potrebbero solo aggiungere quelle due info all'attuale lista dei devices.

3) sarebbe comodo poter esportare quello che c'è sulla lista (o anche altre info di dettaglio) in un file csv/xlsx;

4) sarebbe utile aprire la "web interface" non solo utilizzando il menu contestuale, ma anche cliccando direttamente su un link/icona da aggiungere alla lista.

5) si potrebbe implementare questo "fullscan" che ho fatto io adesso da command line direttamente dall'interfaccia.
*/