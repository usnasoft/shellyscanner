package it.usna.shellyscan.view.devsettings;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.g1.AbstractG1Device;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.view.MainView;
import it.usna.shellyscan.view.util.Msg;
import it.usna.shellyscan.view.util.UtilCollecion;

public class DialogDeviceSettings extends JDialog {
	public enum Gen {G1, G2, ALL};
	private static final long serialVersionUID = 1L;
	
	private JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
	private JButton btnClose = new JButton(Main.LABELS.getString("dlgClose"));
	private JButton btnOKButton = new JButton(Main.LABELS.getString("dlgApply"));
	private JButton btnApplyClose = new JButton(Main.LABELS.getString("dlgApplyClose"));
	private Thread showCurrentThread;
	
	private AbstractSettingsPanel currentPanel = null;
	private ExecutorService exeService = Executors.newFixedThreadPool(1);

	public DialogDeviceSettings(final MainView owner, Devices model, List<ShellyAbstractDevice> devices) {
		super(owner, false);
		if(devices.size() > 1) {
			setTitle(Main.LABELS.getString("dlgSetTitle"));
		} else {
			setTitle(Main.LABELS.getString("dlgSetTitle") + " - " + UtilCollecion.getDescName(devices.get(0)));
		}
		BorderLayout borderLayout = (BorderLayout) getContentPane().getLayout();
		borderLayout.setVgap(5);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		this.setSize(580, 420);
		setLocationRelativeTo(owner);

		JPanel panel_1 = new JPanel();
		getContentPane().add(panel_1, BorderLayout.SOUTH);
		Gen devTypes = getTypes(devices);
		PanelFWUpdate panelFW = new PanelFWUpdate(devices/*, tp*/);
		tabbedPane.add(Main.LABELS.getString("dlgSetFWUpdate"), panelFW);
		PanelWIFI panelWIFI = new PanelWIFI(this, devices, model);
		tabbedPane.add(Main.LABELS.getString("dlgSetWIFIBackup"), panelWIFI);
		PanelResLogin panelResLogin = new PanelResLogin(devices);
		tabbedPane.add(Main.LABELS.getString("dlgSetRestrictedLogin"), panelResLogin);
		AbstractSettingsPanel panelMQTT;
		if(devTypes == Gen.G1) {
			panelMQTT = new PanelMQTTG1(this, devices, model);
		} else {
			panelMQTT = new PanelMQTTAll(this, devices, model);
		}
		tabbedPane.add(Main.LABELS.getString("dlgSetMQTT"), panelMQTT);

//		if(devTypes != Gen.G1) {
//			JOptionPane.showMessageDialog(owner, "Some function is momentary not available for second generatione devices.", Main.LABELS.getString("dlgSetTitle"), JOptionPane.WARNING_MESSAGE);
////			tabbedPane.setEnabledAt(1, false);
////			tabbedPane.setEnabledAt(2, false);
////			tabbedPane.setEnabledAt(3, false);
//		}

		getContentPane().add(tabbedPane, BorderLayout.CENTER);

		btnClose.addActionListener(event -> dispose());
		btnOKButton.addActionListener(event -> apply(((AbstractSettingsPanel)tabbedPane.getSelectedComponent()), tabbedPane.getTitleAt(tabbedPane.getSelectedIndex())));
		btnApplyClose.addActionListener(event -> {
			if(apply(((AbstractSettingsPanel)tabbedPane.getSelectedComponent()), tabbedPane.getTitleAt(tabbedPane.getSelectedIndex()))) {
				btnClose.doClick();
			}
		});
		panel_1.add(btnOKButton);
		panel_1.add(btnApplyClose);
		panel_1.add(btnClose);

		tabbedPane.addChangeListener(e -> {
			showCurrent();
			try {
				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			} catch (InterruptedException e1) {}
		});
		setVisible(true);
		showCurrent();
	}
	
	@Override
	public void dispose() {
		if(currentPanel != null) {
			currentPanel.hiding();
		}
		exeService.shutdownNow();
		super.dispose();
	}
	
	public static Gen getTypes(List<ShellyAbstractDevice> devices) {
		Gen r = null;
		for(ShellyAbstractDevice d: devices) {
			if(r == null) {
				r = d instanceof AbstractG2Device ? Gen.G2 : Gen.G1;
			} else if(d instanceof AbstractG2Device) {
				if(r != Gen.G2) {
					return Gen.ALL;
				}
			} else if(d instanceof AbstractG1Device) {
				if(r != Gen.G1) {
					return Gen.ALL;
				}
			}
		}
		return r;
	}

	private synchronized void showCurrent() {
		if(showCurrentThread != null) {
			showCurrentThread.interrupt();
		}
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		btnOKButton.setEnabled(false);
		btnApplyClose.setEnabled(false);
		if(currentPanel != null) {
			currentPanel.hiding();
		}
		currentPanel = ((AbstractSettingsPanel)tabbedPane.getSelectedComponent());
		showCurrentThread = new Thread(() -> {
			try {
				btnClose.requestFocus(); // remove focus from form element, it could be disabled now
				String msg = currentPanel.showing();
				if(Thread.interrupted() == false) {
					DialogDeviceSettings.this.setCursor(Cursor.getDefaultCursor());
					if(isVisible() && currentPanel.isVisible() && msg != null && msg.length() > 0) {
						JOptionPane.showMessageDialog(this, msg, LABELS.getString("errorTitle"), JOptionPane.ERROR_MESSAGE);
					} else { //if(msg == null) { // "" -> panel displayed his own message
						btnOKButton.setEnabled(true);
						btnApplyClose.setEnabled(true);
					}
				}
			} catch (InterruptedException e) {
//				e.printStackTrace();
			}
		});
		showCurrentThread.start();
	}
	
	private boolean apply(AbstractSettingsPanel panel, String name) {
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		try {
			String msg = panel.apply();
			if(msg != null && msg.length() > 0) {
				Msg.showHtmlMessageDialog(this, msg, name, JOptionPane.INFORMATION_MESSAGE);
			}
			return true;
		} catch(Exception e) {
//			e.printStackTrace();
			Main.errorMsg(e.getMessage());
			return false;
		} finally {
			setCursor(Cursor.getDefaultCursor());
		}
	}
}