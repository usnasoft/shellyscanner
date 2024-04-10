package it.usna.shellyscan.view.appsettings;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Paths;
import java.util.Base64;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.DevicesFactory;
import it.usna.shellyscan.view.DevicesTable;
import it.usna.shellyscan.view.MainView;
import it.usna.shellyscan.view.chart.ChartType;
import it.usna.shellyscan.view.util.Msg;
import it.usna.shellyscan.view.util.ScannerProperties;
import it.usna.util.AppProperties;

public class DialogAppSettings extends JDialog {
	private static final long serialVersionUID = 1L;
	
	private final static String IPV4_REGEX_3 = "^((0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)\\.){2}(0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)$";
	
//	public final static String PROP_TOOLBAR_CAPTIONS = "T_CAPTIONS";
//	public final static String PROP_CSV_SEPARATOR = "CSV_SEPARATOR";
//	public final static String PROP_CSV_SEPARATOR_DEFAULT = ",";
//	public final static String PROP_SCAN_MODE = "SCAN_MODE";
//	public final static String PROP_SCAN_MODE_DEFAULT = "FULL";
//	public final static String PROP_DCLICK_ACTION = "DCLICK_ACTION";
//	public final static String PROP_DCLICK_ACTION_DEFAULT = "DET";
//	
//	public final static String PROP_UPDATECHK_ACTION = "UPDATE_CHK";
//	public final static String PROP_UPDATECHK_ACTION_DEFAULT = "STABLE";
//	
//	public final static String PROP_CHARTS_START = "CHART_DEF";
//	public final static String PROP_CHARTS_EXPORT = "CHART_EXPORT";
//	
//	public final static String PROP_DETAILED_VIEW_SCREEN = "DETAIL_SCREEN";
//	public final static String PROP_DETAILED_VIEW_SCREEN_FULL = "FULL";
//	public final static String PROP_DETAILED_VIEW_SCREEN_AS_IS = "ASIS";
//	public final static String PROP_DETAILED_VIEW_SCREEN_HORIZONTAL = "HOR";
//	public final static String PROP_DETAILED_VIEW_SCREEN_ESTIMATE = "COMP";
//	public final static String PROP_DETAILED_VIEW_SCREEN_DEFAULT = PROP_DETAILED_VIEW_SCREEN_FULL;
//	
//	public final static String PROP_LOGIN_USER = "RLUSER";
//	public final static String PROP_LOGIN_PWD = "RLPWD";
//	
//	public final static String PROP_REFRESH_ITERVAL = "REFRESH_INTERVAL";
//	public final static int PROP_REFRESH_ITERVAL_DEFAULT = 2;
//	public final static String PROP_REFRESH_CONF = "REFRESH_SETTINGS";
//	public final static int PROP_REFRESH_CONF_DEFAULT = 5;
//	
//	public final static String PROP_USE_ARCHIVE = "USE_ARCHIVE";
//	public final static String PROP_ARCHIVE_FILE = "USE_ARCHIVE_FILENAME";
//	public final static String PROP_ARCHIVE_FILE_DEFAULT = Paths.get(System.getProperty("user.home"), "ShellyStore.arc").toString();
//	public final static String PROP_AUTORELOAD_ARCHIVE = "AUTORELOAD";
//	
//	public final static String BASE_SCAN_IP = "BASE_SCAN";
//	public final static String FIRST_SCAN_IP = "FIRST_SCAN";
//	public final static int FIST_SCAN_IP_DEFAULT = 1;
//	public final static String LAST_SCAN_IP = "LAST_SCAN";
//	public final static int LAST_SCAN_IP_DEFAULT = 254;
//	
//	public final static String PROP_IDE_TAB_SIZE = "IDE_TAB_SIZE";
//	public final static int IDE_TAB_SIZE_DEFAULT = 4;
	
	private final static Logger LOG = LoggerFactory.getLogger(DialogAppSettings.class);
	
	public DialogAppSettings(final MainView mainView, DevicesTable devTable, Devices model, boolean extendedView, final AppProperties appProp) {
		super(mainView, LABELS.getString("dlgAppSetTitle"), true);

		final AppProperties tempProp = new AppProperties();
		devTable.saveColPos(tempProp, "");
		devTable.saveColWidth(tempProp, "");
		
		BorderLayout borderLayout = new BorderLayout();
		borderLayout.setVgap(5);
		getContentPane().setLayout(borderLayout);

		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);

		PanelGUI panelGUI = new PanelGUI(devTable, extendedView, appProp);
		panelGUI.setBorder(new EmptyBorder(6, 6, 6, 6));
		tabbedPane.add(LABELS.getString("dlgAppSetTabGuiTitle"), panelGUI);
		
		PanelNetwork panelNetwork = new PanelNetwork(appProp);
		panelNetwork.setBorder(new EmptyBorder(6, 6, 6, 6));
		tabbedPane.add(LABELS.getString("dlgAppSetTabLANTitle"), panelNetwork);
		
		PanelIDE panelIDE = new PanelIDE(appProp);
		panelIDE.setBorder(new EmptyBorder(6, 6, 6, 6));
		tabbedPane.add(LABELS.getString("dlgAppSetTabIDETitle"), panelIDE);
		
		PanelStore panelStore = new PanelStore(model, appProp);
		panelStore.setBorder(new EmptyBorder(6, 6, 6, 6));
		tabbedPane.add(LABELS.getString("dlgAppSetTabStoreTitle"), panelStore);
		
		getContentPane().add(tabbedPane, BorderLayout.WEST);
		
		JPanel panelCommands = new JPanel();
		JButton btnClose = new JButton(LABELS.getString("dlgCancel"));
		JButton btnOKButton = new JButton(LABELS.getString("dlgOK"));
		panelCommands.add(btnOKButton);
		panelCommands.add(btnClose);
		btnClose.addActionListener(event -> revertAndDispose(devTable, tempProp));
		btnOKButton.addActionListener(event -> {
			// Scan mode
			String scanMode;
			if(panelNetwork.localScanButton.isSelected()) {
				scanMode = "LOCAL";
			} else if(panelNetwork.fullScanButton.isSelected()) {
				scanMode = "FULL";
			} else if(panelNetwork.ipScanButton.isSelected()) {
				scanMode = "IP";
				String baseIP = panelNetwork.baseIP.getText();
				if(baseIP.isEmpty() || baseIP.matches(IPV4_REGEX_3) == false) {
					JOptionPane.showMessageDialog(this, LABELS.getString("dlgAppSetScanNetworWrongBase"), LABELS.getString("dlgAppSetTitle"), JOptionPane.ERROR_MESSAGE);
					return;
				}
				appProp.setProperty(ScannerProperties.BASE_SCAN_IP, baseIP);
				appProp.setProperty(ScannerProperties.FIRST_SCAN_IP, panelNetwork.firstIP.getText());
				appProp.setProperty(ScannerProperties.LAST_SCAN_IP, panelNetwork.lastIP.getText());
				model.setIPInterval(appProp.getIntProperty(ScannerProperties.FIRST_SCAN_IP), appProp.getIntProperty(ScannerProperties.LAST_SCAN_IP));
			} else { // Offline
				scanMode = "OFFLINE";
			}
			if(appProp.changeProperty(ScannerProperties.PROP_SCAN_MODE, scanMode)) {
				JOptionPane.showMessageDialog(this, LABELS.getString("dlgAppSetScanNetworMsg"), LABELS.getString("dlgAppSetTitle"), JOptionPane.WARNING_MESSAGE);
			}

			// CSV
			appProp.setProperty(ScannerProperties.PROP_CSV_SEPARATOR, panelGUI.csvTextField.getText());
			
			// Charts
			appProp.setProperty(ScannerProperties.PROP_CHARTS_START, ((ChartType)panelGUI.comboCharts.getSelectedItem()).name());
			appProp.setProperty(ScannerProperties.PROP_CHARTS_EXPORT, panelGUI.comboChartsExport.getSelectedIndex() == 0 ? "H" : "V");
			
			// Login
			String rlUser = panelNetwork.userFieldRL.getText();
			appProp.setProperty(ScannerProperties.PROP_LOGIN_USER, rlUser);
			String encodedRlp = "";
			if(rlUser.length() > 0) {
				char[] rlp = panelNetwork.passwordFieldRL.getPassword();
				try {
					encodedRlp = (char)(rlp.hashCode() % ('Z' - 'A') + 'A') + Base64.getEncoder().encodeToString(new String(rlp).getBytes());
				} catch(RuntimeException e) {}
//				CredentialsProvider cp = LoginManager.getCredentialsProvider(rlUser, rlp);
				DevicesFactory.setCredential(rlUser, rlp);
			} else {
				DevicesFactory.setCredential(null, null);
			}
			appProp.setProperty(ScannerProperties.PROP_LOGIN_PWD, encodedRlp);
			
			// Double click
			appProp.setProperty(ScannerProperties.PROP_DCLICK_ACTION, panelGUI.detailsButton.isSelected() ? "DET" : "WEB");
			
			// Refresh
			boolean r0 = appProp.changeProperty(ScannerProperties.PROP_REFRESH_ITERVAL, panelNetwork.refreshTextField.getText());
			boolean r1 = appProp.changeProperty(ScannerProperties.PROP_REFRESH_CONF, panelNetwork.confRefreshtextField.getText());
			if(r0 || r1) {
				model.setRefreshTime(appProp.getIntProperty(ScannerProperties.PROP_REFRESH_ITERVAL) * 1000, appProp.getIntProperty(ScannerProperties.PROP_REFRESH_CONF));
				for(int i = 0; i < model.size(); i++) {
					model.refresh(i, true);
				}
			}
			
			// Detailed view
			if(panelGUI.rdbtnDetailedViewFull.isSelected()) {
				appProp.setProperty(ScannerProperties.PROP_DETAILED_VIEW_SCREEN, ScannerProperties.PROP_DETAILED_VIEW_SCREEN_FULL);
			} else if(panelGUI.rdbtnDetailedViewAsIs.isSelected()) {
				appProp.setProperty(ScannerProperties.PROP_DETAILED_VIEW_SCREEN, ScannerProperties.PROP_DETAILED_VIEW_SCREEN_AS_IS);
			} else if(panelGUI.rdbtnNDetailedViewEstimate.isSelected()) {
				appProp.setProperty(ScannerProperties.PROP_DETAILED_VIEW_SCREEN, ScannerProperties.PROP_DETAILED_VIEW_SCREEN_ESTIMATE);
			} else {
				appProp.setProperty(ScannerProperties.PROP_DETAILED_VIEW_SCREEN, ScannerProperties.PROP_DETAILED_VIEW_SCREEN_HORIZONTAL);
			}
			
			// Check for new Shelly Scanner release
			if(panelGUI.updNoCHK.isSelected()) {
				appProp.setProperty(ScannerProperties.PROP_UPDATECHK_ACTION, "NEVER");
			} else if(panelGUI.updStableCHK.isSelected()) {
				appProp.setProperty(ScannerProperties.PROP_UPDATECHK_ACTION, "STABLE");
			} else { // updBetaCHK
				appProp.setProperty(ScannerProperties.PROP_UPDATECHK_ACTION, "BETA");
			}
			
			// toolbar
			boolean captions = panelGUI.chckbxToolbarCaptions.isSelected();
			appProp.setBoolProperty(ScannerProperties.PROP_TOOLBAR_CAPTIONS, captions);
			mainView.hideCaptions(captions == false);
			
			// IDE
			appProp.setIntProperty(ScannerProperties.PROP_IDE_TAB_SIZE, panelIDE.tabSize.getIntValue());
			if(panelIDE.rdbtnIndentNone.isSelected()) {
				appProp.setProperty(ScannerProperties.IDE_AUTOINDENT, "NO");
			} else if(panelIDE.rdbtnIndentYes.isSelected()) {
				appProp.setProperty(ScannerProperties.IDE_AUTOINDENT, "YES");
			} else if(panelIDE.rdbtnIndentSmart.isSelected()) {
				appProp.setProperty(ScannerProperties.IDE_AUTOINDENT, "SMART");
			}
			appProp.setBoolProperty(ScannerProperties.IDE_AUTOCLOSE_CURLY, panelIDE.chckbxCloseCurly.isSelected());
			appProp.setBoolProperty(ScannerProperties.PROP_IDE_DARK, panelIDE.chcDarkMode.isSelected());
			
			// store
			boolean useStore = panelStore.chckbxUseStore.isSelected();
			boolean changedUse = appProp.setBoolProperty(ScannerProperties.PROP_USE_ARCHIVE, useStore);
			String fileName = panelStore.textFieldStoreFileName.getText();
			boolean changeArcFile = appProp.changeProperty(ScannerProperties.PROP_ARCHIVE_FILE, fileName);
			if(useStore && (changedUse || changeArcFile)) {
				try {
					PanelStore.removeGhosts(model);
					model.loadFromStore(Paths.get(fileName));
				} catch(Exception e) {
					appProp.setBoolProperty(ScannerProperties.PROP_USE_ARCHIVE, false);
					LOG.error("Archive read", e);
					Msg.errorMsg(this, String.format(LABELS.getString("dlgAppStoreerrorReadingStore"), fileName));
				}
			}
			boolean autoReload = panelStore.autoReloadCheckBox.isSelected();
			appProp.setBoolProperty(ScannerProperties.PROP_AUTORELOAD_ARCHIVE, autoReload);
			
			dispose();
		});
		getContentPane().add(panelCommands, BorderLayout.SOUTH);

		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				revertAndDispose(devTable, tempProp);
			}
		});
		
		pack();
		setLocationRelativeTo(mainView);
		setVisible(true);
	}
	
	private void revertAndDispose(DevicesTable devTable, final AppProperties tempProp) {
		devTable.restoreColumns();
		devTable.loadColPos(tempProp, "");
		devTable.loadColWidth(tempProp, "");
		dispose();
	}
}