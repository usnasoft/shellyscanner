package it.usna.shellyscan.view.appsettings;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.view.DevicesTable;
import it.usna.shellyscan.view.MainView;
import it.usna.shellyscan.view.chart.ChartType;
import it.usna.shellyscan.view.util.ScannerProperties;
import it.usna.util.AppProperties;

public class DialogAppSettings extends JDialog {
	private static final long serialVersionUID = 1L;
	
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
		
		PanelNetwork panelNetwork = new PanelNetwork(this, appProp);
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
			panelNetwork.store(appProp, model);
			panelIDE.store(appProp);

			// CSV
			appProp.setProperty(ScannerProperties.PROP_CSV_SEPARATOR, panelGUI.csvTextField.getText());
			
			// Charts
			appProp.setProperty(ScannerProperties.PROP_CHARTS_START, ((ChartType)panelGUI.comboCharts.getSelectedItem()).name());
			appProp.setProperty(ScannerProperties.PROP_CHARTS_EXPORT, panelGUI.comboChartsExport.getSelectedIndex() == 0 ? "H" : "V");
			
			// Double click
			appProp.setProperty(ScannerProperties.PROP_DCLICK_ACTION, panelGUI.detailsButton.isSelected() ? "DET" : "WEB");

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
			
			// Uptime
			if(panelGUI.rdbtnUptimeSeconds.isSelected()) {
				appProp.setProperty(ScannerProperties.PROP_UPTIME_MODE, "SEC");
			} else if(panelGUI.rdbtnUptimeDay.isSelected()) {
				appProp.setProperty(ScannerProperties.PROP_UPTIME_MODE, "DAY");
			} else { // updBetaCHK
				appProp.setProperty(ScannerProperties.PROP_UPTIME_MODE, "FROM");
			}
			
			// Temp unit
			appProp.setProperty(ScannerProperties.PROP_TEMP_UNIT, panelGUI.rdbtnNewRadioButtonTC.isSelected() ? "C" : "F");
			
			// Filter
			appProp.setIntProperty(ScannerProperties.PROP_DEFAULT_FILTER_IDX, panelGUI.comboFilterCol.getSelectedIndex());

			// toolbar
			boolean captions = panelGUI.chckbxToolbarCaptions.isSelected();
			appProp.setBoolProperty(ScannerProperties.PROP_TOOLBAR_CAPTIONS, captions);
			
			// store
//			boolean useStore = panelStore.chckbxUseStore.isSelected();
//			boolean changedUse = appProp.setBoolProperty(ScannerProperties.PROP_USE_ARCHIVE, useStore);
//			String fileName = panelStore.textFieldStoreFileName.getText();
//			boolean changeArcFile = appProp.changeProperty(ScannerProperties.PROP_ARCHIVE_FILE, fileName);
//			if(useStore && (changedUse || changeArcFile)) {
//				try {
//					PanelStore.removeGhosts(model);
//					model.loadFromStore(Paths.get(fileName));
//				} catch(Exception e) {
//					appProp.setBoolProperty(ScannerProperties.PROP_USE_ARCHIVE, false);
//					LOG.error("Archive read", e);
//					Msg.errorMsg(this, String.format(LABELS.getString("dlgAppStoreerrorReadingStore"), fileName));
//				}
//			}
//			boolean autoReload = panelStore.autoReloadCheckBox.isSelected();
//			appProp.setBoolProperty(ScannerProperties.PROP_AUTORELOAD_ARCHIVE, autoReload);
			panelStore.store(appProp, model);
			
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