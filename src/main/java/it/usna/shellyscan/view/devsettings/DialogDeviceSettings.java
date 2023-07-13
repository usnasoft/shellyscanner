package it.usna.shellyscan.view.devsettings;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.Devices.EventType;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.WIFIManager;
import it.usna.shellyscan.model.device.g1.AbstractG1Device;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.view.MainView;
import it.usna.shellyscan.view.util.Msg;
import it.usna.shellyscan.view.util.UtilCollecion;
import it.usna.util.UsnaEventListener;

public class DialogDeviceSettings extends JDialog implements UsnaEventListener<Devices.EventType, Integer> {
	public enum Gen {G1, G2, ALL};
	private static final long serialVersionUID = 1L;
	
	private JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
	private JButton btnClose = new JButton(LABELS.getString("dlgClose"));
	private JButton btnOKButton = new JButton(LABELS.getString("dlgApply"));
	private JButton btnApplyClose = new JButton(LABELS.getString("dlgApplyClose"));
	private Thread showCurrentThread;
	private AbstractSettingsPanel currentPanel = null;
	
	private Devices model;
	private int[] devicesInd;

	public DialogDeviceSettings(final MainView owner, Devices model, int[] devicesInd) {
		super(owner, false);
		this.model = model;
		this.devicesInd = devicesInd;
		model.addListener(this);

		setTitle(LABELS.getString("dlgSetTitle") + " - " + (devicesInd.length == 1 ? UtilCollecion.getDescName(getLocalDevice(0)) : devicesInd.length));
		
		BorderLayout borderLayout = (BorderLayout) getContentPane().getLayout();
		borderLayout.setVgap(5);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		this.setSize(610, 420);
		setLocationRelativeTo(owner);

		JPanel panel_1 = new JPanel();
		getContentPane().add(panel_1, BorderLayout.SOUTH);
		Gen devTypes = getTypes();
		PanelFWUpdate panelFW = new PanelFWUpdate(this);
		tabbedPane.add(LABELS.getString("dlgSetFWUpdate"), panelFW);
		PanelWIFI panelWIFI1 = new PanelWIFI(this, WIFIManager.Network.PRIMARY);
		tabbedPane.add(LABELS.getString("dlgSetWIFI1"), panelWIFI1);
		PanelWIFI panelWIFI2 = new PanelWIFI(this, WIFIManager.Network.SECONDARY);
		tabbedPane.add(LABELS.getString("dlgSetWIFIBackup"), panelWIFI2);
		PanelResLogin panelResLogin = new PanelResLogin(this, devTypes);
		tabbedPane.add(LABELS.getString("dlgSetRestrictedLogin"), panelResLogin);
		AbstractSettingsPanel panelMQTT;
		if(devTypes == Gen.G1) {
			panelMQTT = new PanelMQTTG1(this);
		} else if(devTypes == Gen.G2) {
			panelMQTT = new PanelMQTTG2(this);
		} else {
			panelMQTT = new PanelMQTTAll(this);
		}
		tabbedPane.add(LABELS.getString("dlgSetMQTT"), panelMQTT);

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
		model.removeListener(this);
		if(currentPanel != null) {
			currentPanel.hiding();
		}
		super.dispose();
	}
	
//	static Gen getTypes(List<ShellyAbstractDevice> devices) {
//		Gen r = null;
//		for(ShellyAbstractDevice d: devices) {
//			if(r == null) {
//				r = d instanceof AbstractG2Device ? Gen.G2 : Gen.G1;
//			} else if(d instanceof AbstractG2Device) {
//				if(r != Gen.G2) {
//					return Gen.ALL;
//				}
//			} else if(d instanceof AbstractG1Device) {
//				if(r != Gen.G1) {
//					return Gen.ALL;
//				}
//			}
//		}
//		return r;
//	}

	private synchronized void showCurrent() {
		if(showCurrentThread != null) {
			DialogDeviceSettings.this.setCursor(Cursor.getDefaultCursor());
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
//					DialogDeviceSettings.this.setCursor(Cursor.getDefaultCursor());
					if(isVisible() && currentPanel.isVisible() && msg != null && msg.length() > 0) {
						JOptionPane.showMessageDialog(this, msg, LABELS.getString("errorTitle"), JOptionPane.ERROR_MESSAGE);
					} else { //if(msg == null) { // "" -> panel displayed his own message
						btnOKButton.setEnabled(true);
						btnApplyClose.setEnabled(true);
					}
				}
			} catch (InterruptedException e) {
			} finally {
				DialogDeviceSettings.this.setCursor(Cursor.getDefaultCursor());
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
			Msg.errorMsg(this, e.getMessage());
			return false;
		} finally {
			setCursor(Cursor.getDefaultCursor());
		}
	}
	
	ShellyAbstractDevice getLocalDevice(int index) {
		return model.get(devicesInd[index]);
	}
	
	int getLocalIndex(int ind) {
		for(int i = 0; i < devicesInd.length; i++) {
			if(devicesInd[i] == ind) {
				return i;
			}
		}
		return -1;
	}
	
	int getLocalSize() {
		return devicesInd.length;
	}
	
	Devices getModel() {
		return model;
	}
	
	private Gen getTypes() {
		Gen r = null;
		for(int index: devicesInd) {
			ShellyAbstractDevice d =  model.get(index);
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
	
	@Override
	public void update(EventType mesgType, Integer pos) {
		if(mesgType == Devices.EventType.CLEAR) {
			SwingUtilities.invokeLater(() -> dispose()); // devicesInd changes
		}
	}
} // 166 - 223