package it.usna.shellyscan.view;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.g1.AbstractG1Device;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.view.devsettingspanel.AbstractSettingsPanel;
import it.usna.shellyscan.view.devsettingspanel.PanelFWUpdate;
import it.usna.shellyscan.view.devsettingspanel.PanelMQTTAll;
import it.usna.shellyscan.view.devsettingspanel.PanelMQTTG1;
import it.usna.shellyscan.view.devsettingspanel.PanelResLogin;
import it.usna.shellyscan.view.devsettingspanel.PanelWIFI;

public class DialogDeviceSettings extends JDialog {
	public enum Gen {G1, G2, ALL};
	private static final long serialVersionUID = 1L;
	private ExecutorService tp = Executors.newFixedThreadPool(25);
	
	private JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
	private JButton btnClose = new JButton(Main.LABELS.getString("dlgClose"));
	private JButton btnOKButton = new JButton(Main.LABELS.getString("dlgApply"));
	private JButton btnApplyClose = new JButton(Main.LABELS.getString("dlgApplyClose"));
	
	private Future<?> showCurrentFuture;

	public DialogDeviceSettings(final MainView owner, List<ShellyAbstractDevice> devices) {
		super(owner, false);
		if(devices.size() > 1) {
			setTitle(Main.LABELS.getString("dlgSetTitle"));
		} else {
			final String dName = devices.get(0).getName();
			setTitle(Main.LABELS.getString("dlgSetTitle") + " - " + (dName.length() > 0 ? dName : devices.get(0).getTypeName()));
		}
		BorderLayout borderLayout = (BorderLayout) getContentPane().getLayout();
		borderLayout.setVgap(5);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		this.setSize(580, 420);
		setLocationRelativeTo(owner);

		JPanel panel_1 = new JPanel();
		getContentPane().add(panel_1, BorderLayout.SOUTH);
		Gen devTypes = getTypes(devices);
		PanelFWUpdate panelFW = new PanelFWUpdate(devices, tp);
		tabbedPane.add(Main.LABELS.getString("dlgSetFWUpdate"), panelFW);
		PanelWIFI panelWIFI = new PanelWIFI(devices);
		tabbedPane.add(Main.LABELS.getString("dlgSetWIFIBackup"), panelWIFI);
		PanelResLogin panelResLogin = new PanelResLogin(devices);
		tabbedPane.add(Main.LABELS.getString("dlgSetRestrictedLogin"), panelResLogin);
		AbstractSettingsPanel panelMQTT;
		if(devTypes == Gen.G1) {
			panelMQTT = new PanelMQTTG1(devices);
		} else {
			panelMQTT = new PanelMQTTAll(devices);
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
		
		tabbedPane.addChangeListener(i -> {
			showCurrent();
		});
		showCurrent();
		setVisible(true);
	}
	
	@Override
	public void dispose() {
		tp.shutdownNow();
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

	private void showCurrent() {
		if(showCurrentFuture != null) {
			showCurrentFuture.cancel(true);
		}
		showCurrentFuture = tp.submit(() -> {
			btnOKButton.setEnabled(false);
			btnApplyClose.setEnabled(false);
			AbstractSettingsPanel p = ((AbstractSettingsPanel)tabbedPane.getSelectedComponent());
			p.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			String msg = p.showing();
			p.setCursor(Cursor.getDefaultCursor());
			if(msg == null) {
				btnOKButton.setEnabled(true);
				btnApplyClose.setEnabled(true);
			} else {
				if(Thread.interrupted() == false && p.isVisible() && msg.length() > 0) {
					JOptionPane.showMessageDialog(this, msg, LABELS.getString("errorTitle"), JOptionPane.ERROR_MESSAGE);
				}
			}
		});
	}
	
	private boolean apply(AbstractSettingsPanel panel, String name) {
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		try {
			String msg = panel.apply();
			if(msg != null && msg.length() > 0) {
				JOptionPane.showMessageDialog(this, msg, name, JOptionPane.INFORMATION_MESSAGE);
			}
			return true;
		} catch(Exception e) {
			Main.errorMsg(e.getMessage());
			return false;
		} finally {
			setCursor(Cursor.getDefaultCursor());
		}
	}
}