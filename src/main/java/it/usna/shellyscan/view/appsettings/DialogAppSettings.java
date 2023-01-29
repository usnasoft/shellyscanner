package it.usna.shellyscan.view.appsettings;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Base64;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;

import org.apache.hc.client5.http.auth.CredentialsProvider;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.DevicesFactory;
import it.usna.shellyscan.model.device.LoginManager;
import it.usna.shellyscan.view.DevicesTable;
import it.usna.shellyscan.view.MainView;
import it.usna.shellyscan.view.chart.MeasuresChart.ChartType;
import it.usna.util.AppProperties;

public class DialogAppSettings extends JDialog {
	private static final long serialVersionUID = 1L;
	
	private final static String IPV4_REGEX_3 = "^((0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)\\.){2}(0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)$";
	
	public final static String PROP_CSV_SEPARATOR = "CSV_SEPARATOR";
	public final static String PROP_CSV_SEPARATOR_DEFAULT = ",";
	public final static String PROP_SCAN_MODE = "SCAN_MODE";
	public final static String PROP_SCAN_MODE_DEFAULT = "FULL";
	public final static String PROP_DCLICK_ACTION = "DCLICK_ACTION";
	public final static String PROP_DCLICK_ACTION_DEFAULT = "DET";
	public final static String PROP_CHARTS_START = "CHART_DEF";
	
	public final static String PROP_DETAILED_VIEW_SCREEN = "DETAIL_SCREEN";
	public final static String PROP_DETAILED_VIEW_SCREEN_FULL = "FULL";
	public final static String PROP_DETAILED_VIEW_SCREEN_AS_IS = "ASIS";
	public final static String PROP_DETAILED_VIEW_SCREEN_HORIZONTAL = "HOR";
	public final static String PROP_DETAILED_VIEW_SCREEN_ESTIMATE = "COMP";
	public final static String PROP_DETAILED_VIEW_SCREEN_DEFAULT = PROP_DETAILED_VIEW_SCREEN_FULL;
	
	public final static String PROP_LOGIN_USER = "RLUSER";
	public final static String PROP_LOGIN_PWD = "RLPWD";
	
	public final static String PROP_REFRESH_ITERVAL = "REFRESH_INTERVAL";
	public final static int PROP_REFRESH_ITERVAL_DEFAULT = 2;
	public final static String PROP_REFRESH_CONF = "REFRESH_SETTINGS";
	public final static int PROP_REFRESH_CONF_DEFAULT = 5;
	
	public final static String BASE_SCAN_IP = "BASE_SCAN";
	public final static String FIRST_SCAN_IP = "FIRST_SCAN";
	public final static int FIST_SCAN_IP_DEFAULT = 1;
	public final static String LAST_SCAN_IP = "LAST_SCAN";
	public final static int LAST_SCAN_IP_DEFAULT = 254;
	
	public DialogAppSettings(final MainView owner, DevicesTable devTable, Devices model, final AppProperties appProp) {
		super(owner, LABELS.getString("dlgAppSetTitle"), true);

		final AppProperties tempProp = new AppProperties();
		devTable.saveColPos(tempProp, "");
		devTable.saveColWidth(tempProp, "");
		
		BorderLayout borderLayout = new BorderLayout();
		borderLayout.setVgap(5);
		getContentPane().setLayout(borderLayout);
//		((JComponent)getContentPane()).setBorder(BorderFactory.createEmptyBorder(6, 6, 0, 6));

		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		
//		PanelNetwork panelNetwork = new PanelNetwork(appProp);
		PanelGUI panelGUI = new PanelGUI(devTable, appProp);
		panelGUI.setBorder(new EmptyBorder(6, 6, 6, 6));
		tabbedPane.add(LABELS.getString("dlgAppSetTabGuiTitle"), panelGUI);
		
		PanelNetwork panelNetwork = new PanelNetwork(appProp);
		panelNetwork.setBorder(new EmptyBorder(6, 6, 6, 6));
		tabbedPane.add(LABELS.getString("dlgAppSetTabLANTitle"), panelNetwork);
		
		getContentPane().add(tabbedPane, BorderLayout.WEST);
		
		JPanel panelCommands = new JPanel();
		JButton btnClose = new JButton(LABELS.getString("dlgCancel"));
		JButton btnOKButton = new JButton(LABELS.getString("dlgOK"));
		panelCommands.add(btnOKButton);
		panelCommands.add(btnClose);
		btnClose.addActionListener(event -> {
			dispose(devTable, tempProp);
		});
		btnOKButton.addActionListener(event -> {
			// Scan mode
			String scanMode;
			if(panelNetwork.localScanButton.isSelected()) {
				scanMode = "LOCAL";
			} else if(panelNetwork.fullScanButton.isSelected()) {
				scanMode = "FULL";
			} else {
				scanMode = "IP";
				String baseIP = panelNetwork.baseIP.getText();
				if(baseIP.isEmpty() || baseIP.matches(IPV4_REGEX_3) == false) {
					JOptionPane.showMessageDialog(this, LABELS.getString("dlgAppSetScanNetworWrongBase"), LABELS.getString("dlgAppSetTitle"), JOptionPane.ERROR_MESSAGE);
					return;
				}
				appProp.setProperty(BASE_SCAN_IP, baseIP);
				appProp.setProperty(FIRST_SCAN_IP, panelNetwork.firstIP.getText());
				appProp.setProperty(LAST_SCAN_IP, panelNetwork.lastIP.getText());
				model.setIPInterval(appProp.getIntProperty(FIRST_SCAN_IP), appProp.getIntProperty(LAST_SCAN_IP));
			}
			if(appProp.changeProperty(PROP_SCAN_MODE, scanMode)) {
				JOptionPane.showMessageDialog(this, LABELS.getString("dlgAppSetScanNetworMsg"), LABELS.getString("dlgAppSetTitle"), JOptionPane.WARNING_MESSAGE);
			}

			// CVS
			appProp.setProperty(PROP_CSV_SEPARATOR, panelGUI.csvTextField.getText());
			
			// Charts
			appProp.setProperty(PROP_CHARTS_START, ((ChartType)panelGUI.comboCharts.getSelectedItem()).name());
			
			// Login
			String rlUser = panelNetwork.userFieldRL.getText();
			appProp.setProperty(PROP_LOGIN_USER, rlUser);
			String encodedRlp = "";
			if(rlUser.length() > 0) {
				char[] rlp = panelNetwork.passwordFieldRL.getPassword();
				try {
					encodedRlp = (char)(rlp.hashCode() % ('Z' - 'A') + 'A') + Base64.getEncoder().encodeToString(new String(rlp).getBytes());
				} catch(RuntimeException e) {}
				CredentialsProvider cp = LoginManager.getCredentialsProvider(rlUser, rlp);
				DevicesFactory.setCredentialProvider(cp);
			} else {
				DevicesFactory.setCredentialProvider(null);
			}
			appProp.setProperty(PROP_LOGIN_PWD, encodedRlp);
			
			// Double click
			appProp.setProperty(PROP_DCLICK_ACTION, panelGUI.detailsButton.isSelected() ? "DET" : "WEB");
			
			// Refresh
			boolean r0 = appProp.changeProperty(PROP_REFRESH_ITERVAL, panelNetwork.refreshTextField.getText());
			boolean r1 = appProp.changeProperty(PROP_REFRESH_CONF, panelNetwork.confRefreshtextField.getText());
			if(r0 || r1) {
				model.setRefreshTime(appProp.getIntProperty(PROP_REFRESH_ITERVAL) * 1000, appProp.getIntProperty(PROP_REFRESH_CONF));
				for(int i = 0; i < model.size(); i++) {
					model.refresh(i, true);
				}
			}
			
			// Detailed view
			String detaildedScreen;
			if(panelGUI.rdbtnDetailedViewFull.isSelected()) {
				detaildedScreen = PROP_DETAILED_VIEW_SCREEN_FULL;
			} else if(panelGUI.rdbtnDetailedViewAsIs.isSelected()) {
				detaildedScreen = PROP_DETAILED_VIEW_SCREEN_AS_IS;
			} else if(panelGUI.rdbtnNDetailedViewEstimate.isSelected()) {
				detaildedScreen = PROP_DETAILED_VIEW_SCREEN_ESTIMATE;
			} else {
				detaildedScreen = PROP_DETAILED_VIEW_SCREEN_HORIZONTAL;
			}
			appProp.setProperty(PROP_DETAILED_VIEW_SCREEN, detaildedScreen);
			dispose();
		});
		getContentPane().add(panelCommands, BorderLayout.SOUTH);

		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				dispose(devTable, tempProp);
			}
		});
		
		pack();
		setLocationRelativeTo(owner);
		setVisible(true);
	}
	
	private void dispose(DevicesTable devTable, final AppProperties tempProp) {
		devTable.restoreColumns();
		devTable.loadColPos(tempProp, "");
		devTable.loadColWidth(tempProp, "");
		dispose();
	}
}